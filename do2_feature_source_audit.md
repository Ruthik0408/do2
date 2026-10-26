# DO2 Feature Source Audit

## 1. Executive summary

The notebook file is `do2_status_prediction/src/do2_file.ipynb`. Its training source is `${DB_SCHEMA}.do2_model`; validation and test source is `${DB_SCHEMA}.do2`; the notebook uses `DATE_COLUMN = "do2_date"` and performs no SQL joins. Evidence: `do2_file.ipynb` defines `TRAIN_TABLE = "do2_model"`, `SOURCE_TABLE = "do2"`, and `DATE_COLUMN = "do2_date"`; production extraction instead defaults to `${DB_SCHEMA}.${DB_TABLE}` with `DB_TABLE=do2` and `DB_DATE_COLUMN=created_at` (`do2_status_prediction/src/do2_model/config.py:23-25`, `do2_status_prediction/src/do2_model/database.py:93-125`).

For leakage-safe N/I prediction, current notebook features are not enough. The largest missing signal group is rule-derived history from full `do2` plus current employee state from `employee`. Do not calculate duplicate/overlap history from `do2_model`; it is a sampled/prepared training table and the notebook itself treats `do2` as the original holdout source.

No application/notebook/database code was modified. This report is the only created file.

## 2. Status-N rule mapping

`Status in Employee table is Not Effective`

- Existing code: `dolphin/do2/Do2GenericProcessor.java`, class `Do2GenericProcessor`, method `isValidDo2`, constant `NOT_EFFECTIVE` at line 73 and status assignment at lines 1391-1406.
- Source tables: `do2`, `employee`, `lpc_register`. `do2` entity maps to persistent table `do2` (`dolphin/models/Do2.java:19-25`). `employee` entity maps to persistent table `employee` (`dolphin/models/Employee.java:13-19`). `lpc_register` table is referenced by repository method call at `Do2GenericProcessor.java:1400`; exact entity columns beyond method name were not fully traced.
- Join: `do2.fk_employee = employee.id`, proven by `Do2.employee @JoinColumn(name="fk_employee")` (`dolphin/models/Do2.java:111-113`) and `Employee.id` (`dolphin/models/Employee.java:22-24`).
- Columns: `do2.army_no`, `do2.fk_employee`, `do2.description`, `do2.from_date`, `do2.status`; `employee.id`, `employee.army_no`, `employee.fk_status_code`, `employee.fs_due_date`, `employee.enrolment_date`, `employee.birth_date`; `status_code.id/status_code/status_name` exists (`dolphin/models/StatusCode.java:14-30`).
- Rule logic: `if (!isEffective(do2.getArmyNo()))`, except `DEATH` returns true, and `ATCORR` is allowed when `!do2.fromDate.after(employee.fsDueDate)`. If no final LPC record is found, status is set to `N` and reason to `NOT_EFFECTIVE` (`Do2GenericProcessor.java:1391-1406`).
- Meaning of employee status values: `NOT FOUND` in repository code. Code uses numeric thresholds and exact ids: `employee.fk_status_code > 4` for not-effective repository lists (`dolphin/repositories/Do2Repository.java:1460-1481` from `rg`), id `15` for special death/FS handling (`Do2GenericProcessor.java:1446`), and id `1` for effective/current reports (`dolphin/reports/RankWiseEmolumentsDo2RejectionReport.java:166-175` from `rg`), but the semantic mapping of every status id requires live `status_code` data.
- Employee status history: `NOT FOUND`. I found `employee.current_record` and `employee.qe` references, but no definitive effective-from/effective-to status-history table in the inspected paths.

`From date is after fs due date`

- Existing code: `Do2GenericProcessor.isValidDo2`, lines 1412-1505.
- Rule logic: after not-effective handling, if `do2.from_date > employee.fs_due_date`, status is set to `N` except for CEA-specific allowance-period exceptions and death-LPC exceptions; OPTFIX additionally checks `do2.to_date > employee.fs_due_date` (`Do2GenericProcessor.java:1412-1505`).
- Boundary: strict `after`, equivalent to SQL `>`. Same day is not after.
- Date used: `do2.from_date` primarily; `do2.to_date` only for OPTFIX guard; not `do2.do2_date` or `created_at`.
- Null handling: direct `do2.getFromDate().after(employee.getFsDueDate())` is used inside branches where `employee.fsDueDate` is expected; `employee.fsDueDate == null` can be populated from the first LPC discharge date when an LPC row exists (`Do2GenericProcessor.java:1408-1410`). Other null behavior is not fully guarded in this method.

Proposed N features:

- `n_fs_due_date_missing boolean`: `employee.fs_due_date IS NULL`.
- `n_days_after_fs_due_date integer`: `GREATEST(do2.from_date - employee.fs_due_date, 0)`, null if either date is missing.
- `n_from_date_after_fs_due_date boolean`: `do2.from_date > employee.fs_due_date`.
- `n_from_date_within_fs_month boolean`: `date_trunc('month', do2.from_date) = date_trunc('month', employee.fs_due_date)`.
- `n_employee_status_missing boolean`: `employee.fk_status_code IS NULL`.
- `n_employee_effective_on_from_date boolean`: `employee.fk_status_code = 1` is suggested only as a placeholder; exact status semantics are `NOT FOUND`.
- `n_employee_not_effective_on_from_date boolean`: code-backed approximation is `employee.fk_status_code > 4`; exact semantics are `NOT FOUND`.

## 3. Status-I rule mapping

| Reason family | Existing code | Source tables | Join | Time condition | Rule logic | Aggregation | Proposed feature | Prediction availability | Missing information |
|---|---|---|---|---|---|---|---|---|---|
| Description abolished | `dolphin/controllers/Do2Controller.java`, method around processing, lines 2940-2945 | `do2`; abolished list from `Do2Manager.getAbolishedDescriptionsSevencpc()` | None | Entry-time row only | `do2.from_date >= DateManager.getSeventhCpcAllowanceDate()` and description in abolished list sets `I` | One row | `i_description_abolished_on_from_date boolean` | Yes | Exact abolished description list and date-manager constant value source not fully traced |
| Referenced DO2 cancelled | `Do2GenericProcessor.isValidCancelDo2`, lines 1229-1306 | `do2` self-reference | `Utils.getReferenceDo2List(do2)` plus same `army_no` or same employee, same `from_date`, ref description equals current `column_a` | Must restrict ref rows to `ref.created_at < current.created_at` for ML, although existing code does not show this | If matching ref status is cancel status, current invalid with `REF_DO2_ALREADY_CANCELLED` | `EXISTS`/count per current id | `i_ref_do2_already_cancelled boolean`, `i_ref_do2_match_count integer` | Yes if full historical `do2` is available | Exact SQL inside `Utils.getReferenceDo2List` not found in current audit |
| Duplicate publication | `Do2GenericProcessor.isDuplicatePublication`, lines 267-364 found by `rg`; repository methods at `dolphin/repositories/Do2Repository.java:1207-1225` | `do2` self-reference | Uses employee id, from date, description, with variants on to_date, column_a/b/c, ref_do2_no; excludes current id | Must use `hist.created_at < current.created_at`; existing code likely checks current DB state | Duplicate match sets `DU:DupliCate Publication...` | Count and nearest prior duplicate via aggregate/lateral | `i_prior_duplicate_count integer`, `i_has_prior_duplicate boolean` | Yes from full `do2` | Need full line-level repository query text for each variant if implementing exact family |
| Pay fixation not involved | `dolphin/do2/auditcage/Profix.java:38`, `dolphin/do2/auditcage/Optfix.java:36` constants | `do2`, likely audit/pay history | Employee join | Must use prior valid pay-fix/promotion rows before current `created_at` | Existing constants indicate option invalid when pay fixation is not involved for promotion | `EXISTS` on prior valid pay-fix event | `i_pay_fixation_not_involved boolean` | Maybe | Full method body not traced in this report |
| From date within FS month | `dolphin/do2/auditcage/Posin.java:91` | `do2`, `employee` | employee from current do2 | Entry/current employee FS date | Reason text set when POSIN from-date is inside FS month | One row | `i_from_date_within_fs_month boolean` | Yes | Surrounding POSIN branch not fully traced |
| Previous claim overlap | `dolphin/utils/Utils.java:1115-1121`; CEA constants at `dolphin/do2/accountingcage/independent/Cea.java:76,110` | `do2` self-reference | Employee/army and mutually exclusive descriptions; overlap by period | Must use prior rows only: `hist.created_at < current.created_at` | Overlap reason built when current period overlaps adjusted period, with special mutual-exclusion exceptions (`Utils.java:1081-1121`) | Count overlapping prior rows per current id | `i_prior_overlap_count integer`, `i_has_prior_overlap boolean` | Yes from full `do2` | Full caller and description sets need final tracing |
| Batch verification pending | `ReviewBatchMismatch.java:107`, `Do2ControlImportDo2Xml.java:179,917`, `Do2Controller.java:2628`; repository checks `Do2Repository.java:1282` from `rg` | `do2`, `doiitmp`, enroll XML tables | Batch/unit/do2 number depending path | Existing reason/status is workflow-generated; unsafe as direct feature | Reason `Batch to be cleared after verification` | Count prior pending batch rows, not current reason | `i_prior_batch_verification_pending_count integer` | Maybe | Initial-availability unclear; current reason is leakage |
| Invalid relationship / relationship not mentioned | `Marry.java:42`, `Famdtl.java:184,188`, `Uid.java:207,212`, `Biomet.java:31` | `do2`, `family_detail` likely | Employee id and relation/member fields | Entry-time columns only plus existing family state before current | Missing/invalid col relationship text | Boolean validation result | `i_relationship_missing_or_invalid boolean` | Yes if master relation list is available | Exact relation master table not found |
| Phone/Aadhaar uniqueness | `Uid.java:77,93`; `Biomet.java:82,98`; `Mobile.java:122` commented | `employee`, `family_detail` | Sensitive value match, then employee | Use counts only; historical point-in-time impossible without created dates | Existing code checks same UID/mobile exists for employee/family | Count/boolean only, never raw value | `i_phone_duplicate_count`, `i_aadhaar_duplicate_count` | Maybe | Tables/columns for UID/mobile in family table not fully verified |
| CEA child/class limits | `Cea.java:75,97` | `do2`, family details, CEA history | Employee/child relation | Prior valid CEA rows before current | More than two children; only two classes below Class-I | Counts/booleans | `i_cea_child_limit_exceeded`, `i_cea_class_rule_invalid` | Maybe | Full CEA implementation not traced |
| OPTCPC conflict | `Optfix.java:37`; repository `findOptcpcNotOnIncrDateForEmployeeId` at `Do2Repository.java:2276-2277` | `do2` | `d.employee.id=:empid` | Prior valid rows before current | `description='OPTCPC'`, `column_b='SEVENPC'`, `column_a!='INCR'`, `status='V'` | `EXISTS`/count | `i_prior_optcpc_conflict boolean` | Yes from full `do2` | Whether current created_at is available in old rows |
| Six months / service-age rules | `Do2Controller.java:7081-7082`; `Plbon.java:42`; `Do2GenericProcessor.java:1451-1463` | `employee`, `do2` | `do2.fk_employee=employee.id` | Entry row + employee enrolment/birth dates | Not completed six months / 34 years service / 54 years age | One row | `i_service_months_at_from_date`, `i_age_months_at_from_date` | Yes | Some rule-specific descriptions not fully traced |

## 4. Exact tables and columns

Repository-proven tables:

- `do2`: `id`, `army_no`, `attachment`, `attachment_verified`, `audit_cage`, `batch_no`, `check_digit`, `column_a`, `column_b`, `column_c`, `created_at`, `description`, `do2_date`, `do2_group`, `do2_item_no`, `do2_no`, `do2_type`, `fk_employee`, `fk_unit`, `from_date`, `input_qe`, `sys_reason`, `change_reason`, `lra`, `punishment`, `qe`, `rate`, `reason`, `ref_do2_no`, `rejection_type`, `remarks`, `review_qe`, `reviewed`, `status`, `time_bar_sanction`, `tjra`, `to_date`, `transcription_type`, `unit_code`, `delayed` (`dolphin/models/Do2.java:28-219`).
- `employee`: `id`, `army_no`, `check_digit`, `created_at`, `current_record`, `enrolment_date`, `fk_fs_status_code`, `fk_present_unit`, `fk_status_code`, `fs_due_date`, `birth_date`, multiple rank/date columns (`dolphin/models/Employee.java:22-169`).
- `status_code`: `id`, `created_at`, `status_code`, `status_name`, `fk_task`, `fk_usr` (`dolphin/models/StatusCode.java:14-44`).
- `do2_model`: notebook training table only; exact physical schema `NOT FOUND` without DB inspection. It is selected with `SELECT *`-style generated column list from SQLAlchemy inspector (`do2_file.ipynb`, `make_safe_select_clause`; mirrored behavior in `database.py:44-78`).

## 5. Exact join conditions

- `do2.fk_employee = employee.id`: JPA `Do2.employee @JoinColumn(name="fk_employee")` and `Employee.id` (`Do2.java:111-113`, `Employee.java:22-24`).
- `employee.fk_status_code = status_code.id`: JPA `Employee.statusCode @JoinColumn(name="fk_status_code")` and `StatusCode.id` (`Employee.java:148-150`, `StatusCode.java:19-21`).
- `employee.fk_fs_status_code = status_code.id`: JPA `Employee.fsStatus @JoinColumn(name="fk_fs_status_code")` (`Employee.java:116-118`).
- `do2.fk_unit = unit.id`: JPA `Do2.unit @JoinColumn(name="fk_unit")` (`Do2.java:123-125`); unit table/entity columns not fully traced here.
- `do2` self-history: use `hist.fk_employee = cur.fk_employee` or `hist.army_no = cur.army_no` depending rule. Existing cancel rule allows same army number or same employee object (`Do2GenericProcessor.java:1242-1244`).

## 6. Point-in-time leakage risks

Forbidden current-row fields: `status`, `reason`, `sys_reason`, `change_reason`, `rejection_type`, approval/review/auditor/AAO/AO/HOO fields, MR decision fields, SMS state, `fk_usr`, `remarks`, `audit_cage`; these are configured as leakage (`do2_status_prediction/src/do2_model/config.py:36-76`, `do2_status_prediction/config/feature_config.json`).

For historical features, use full `do2`, not `do2_model`, and enforce:

```sql
hist.created_at < cur.created_at
```

If `created_at` is null, fall back only if the business owner approves a deterministic surrogate such as `(hist.do2_date, hist.id) < (cur.do2_date, cur.id)`; otherwise mark the feature missing. Do not use final status/reason of a row that was not yet available when the current row entered.

## 7. Proposed `do2_rule_features` schema

Primary key: `do2_id bigint primary key references do2(id)`.

Columns:

- `feature_built_at timestamp not null`
- `source_data_missing boolean not null`
- `employee_missing boolean not null`
- `n_fs_due_date_missing boolean`
- `n_days_after_fs_due_date integer`
- `n_from_date_after_fs_due_date boolean`
- `n_from_date_within_fs_month boolean`
- `n_employee_status_missing boolean`
- `n_employee_effective_on_from_date boolean`
- `n_employee_not_effective_on_from_date boolean`
- `i_description_abolished_on_from_date boolean`
- `i_ref_do2_match_count integer`
- `i_ref_do2_already_cancelled boolean`
- `i_prior_duplicate_count integer`
- `i_has_prior_duplicate boolean`
- `i_prior_overlap_count integer`
- `i_has_prior_overlap boolean`
- `i_prior_optcpc_conflict boolean`
- `i_prior_pay_fix_event_count integer`
- `i_pay_fixation_not_involved boolean`
- `i_from_date_within_fs_month boolean`
- `i_relationship_missing_or_invalid boolean`
- `i_phone_duplicate_count integer`
- `i_aadhaar_duplicate_count integer`
- `i_cea_child_limit_exceeded boolean`
- `i_cea_class_rule_invalid boolean`
- `i_service_months_at_from_date integer`
- `i_age_months_at_from_date integer`

Avoid row multiplication with aggregate CTEs keyed by current `do2.id`, `EXISTS`, or `LEFT JOIN LATERAL (...) LIMIT 1`. Never directly join one-to-many history tables into the model rowset without grouping to one row per `do2_id`.

## 8. Existing feature keep/drop assessment

Keep: entry payload and system-at-entry columns from `feature_config.json`: `batch_no`, `unit_code`, `fk_unit`, `army_no`, `fk_employee`, `check_digit`, `do2_group`, `do2_type`, `do2_no`, `do2_item_no`, `do2_date`, `punishment`, `description`, `from_date`, `to_date`, `rate`, `tjra`, `lra`, `column_a`, `column_b`, `column_c`, `ref_do2_no`, `transcription_type`, `qe`, `created_at`, `time_bar_sanction`, `input_qe`, `attachment`, `delayed`, CFA/remarks presence fields.

Replace/add rule features: duplicate counts, overlap counts, reference-cancel flags, employee FS/status flags, abolished-description flag, OPTCPC/pay-fix history, family/relationship validation counts.

Drop as leakage: all fields listed under `excluded_leakage_columns`; production code enforces this during selection (`database.py:65-73`) and feature code checks leakage presence (`features.py:75-77`).

Drop/avoid raw sensitive values: phone, Aadhaar, PAN, raw Army-number-derived identity memorization. Use only counts/booleans for sensitive uniqueness rules.

Candidate for ablation: high-cardinality identifiers `army_no`, `fk_employee`, `do2_no`; current code keeps them, but multi-user production use should test removal or hashing because they can memorize individual/unit behavior.

## 9. Read-only SQL/pseudocode

```sql
WITH cur AS (
  SELECT d.* FROM do2 d
),
emp AS (
  SELECT e.id, e.fk_status_code, e.fs_due_date, e.enrolment_date, e.birth_date
  FROM employee e
)
SELECT
  cur.id AS do2_id,
  emp.id IS NULL AS employee_missing,
  emp.fs_due_date IS NULL AS n_fs_due_date_missing,
  CASE WHEN cur.from_date > emp.fs_due_date THEN cur.from_date - emp.fs_due_date END AS n_days_after_fs_due_date,
  cur.from_date > emp.fs_due_date AS n_from_date_after_fs_due_date,
  date_trunc('month', cur.from_date) = date_trunc('month', emp.fs_due_date) AS n_from_date_within_fs_month,
  emp.fk_status_code IS NULL AS n_employee_status_missing,
  emp.fk_status_code = 1 AS n_employee_effective_on_from_date,
  emp.fk_status_code > 4 AS n_employee_not_effective_on_from_date
FROM cur
LEFT JOIN emp ON emp.id = cur.fk_employee;
```

```sql
-- Duplicate/overlap pattern. Final implementation must branch by description
-- to match Do2GenericProcessor and repository methods exactly.
SELECT cur.id AS do2_id,
       COUNT(hist.id) AS i_prior_duplicate_count,
       COUNT(hist.id) > 0 AS i_has_prior_duplicate
FROM do2 cur
LEFT JOIN do2 hist
  ON hist.fk_employee = cur.fk_employee
 AND hist.id <> cur.id
 AND hist.created_at < cur.created_at
 AND hist.description = cur.description
 AND hist.from_date = cur.from_date
GROUP BY cur.id;
```

```sql
SELECT cur.id AS do2_id,
       COUNT(hist.id) AS i_prior_overlap_count,
       COUNT(hist.id) > 0 AS i_has_prior_overlap
FROM do2 cur
LEFT JOIN do2 hist
  ON hist.fk_employee = cur.fk_employee
 AND hist.id <> cur.id
 AND hist.created_at < cur.created_at
 AND hist.status IN ('V','P')
 AND daterange(hist.from_date, COALESCE(hist.to_date, hist.from_date), '[]')
     && daterange(cur.from_date, COALESCE(cur.to_date, cur.from_date), '[]')
GROUP BY cur.id;
```

## 10. Missing information and questions

- Live PostgreSQL schema/types were not inspected; exact DB column types for `do2_model` and all supporting tables are `NOT FOUND`.
- Complete meaning of every `status_code` value is `NOT FOUND`; query `status_code` read-only before finalizing N status flags.
- `Utils.getReferenceDo2List`, `isEffective`, and full duplicate method bodies should be fully line-read before implementation.
- Exact master tables for Appendix J, allowances, descriptions, city class, station, family relation, UID/mobile storage, and pay fixation need deeper tracing or live schema inspection.
- Confirm whether `employee` is current-state only. If yes, using it for historical training rows may leak future employee status unless an employee snapshot/history table exists.

## 11. Files inspected

- `do2_status_prediction/src/do2_file.ipynb`
- `do2_status_prediction/src/DO2_multiclass_complete_v2.ipynb`
- `do2_status_prediction/src/do2_model/config.py`
- `do2_status_prediction/src/do2_model/database.py`
- `do2_status_prediction/src/do2_model/features.py`
- `do2_status_prediction/config/feature_config.json`
- `do2_status_prediction/README.md`
- `dolphin/models/Do2.java`
- `dolphin/models/Employee.java`
- `dolphin/models/StatusCode.java`
- `dolphin/do2/Do2GenericProcessor.java`
- `dolphin/controllers/Do2Controller.java`
- `dolphin/utils/Utils.java`
- `dolphin/repositories/Do2Repository.java`
- `dolphin/do2/auditcage/Optfix.java`
- `dolphin/do2/auditcage/Profix.java`
- `dolphin/do2/auditcage/Posin.java`
- `dolphin/do2/accountingcage/independent/Cea.java`
- `dolphin/do2/accountingcage/independent/Uid.java`
- `dolphin/do2/accountingcage/independent/Biomet.java`
- `dolphin/do2/accountingcage/independent/Famdtl.java`
- `dolphin/do2/accountingcage/independent/Marry.java`
- `dolphin/do2/ReviewBatchMismatch.java`
- Attached status-I reasons text file, with sensitive values not repeated in this report.

## 12. Continuation audit: exact rule sources

### isEffective()

Proven body: `dolphin/do2/Do2GenericProcessor.java:435-448`. The method returns false when `armyNo == null`, loads `empRepo.findByArmyNoOldArmyNo(armyNo)`, returns false immediately if any returned employee has null `statusCode`, and otherwise marks effective true when the first character of `emp.statusCode.statusCode` is in `['E','N','R','D','M','W','T']`.

Caller found in the DO2 validation path: `Do2GenericProcessor.isValidDo2()` calls `isEffective(do2.getArmyNo())` at `dolphin/do2/Do2GenericProcessor.java:1391`. Separate unrelated FS code has another `isEffective()` in `dolphin/fs/CurrentIrlaGeneration.java:350`; it was not used by this DO2 processor path.

Employee lookup query: `EmployeeRepository.findByArmyNoOldArmyNo` uses `(emp.armyNo=:armyNo OR emp.oldArmyNo=:armyNo) AND emp.currentRecord=true` (`dolphin/repositories/EmployeeRepository.java:49-50`). This means the active DO2 validation checks only current employee rows, not an employee status history snapshot.

### status_code rows

Read-only DB query returned these rows from `public.status_code`:

| id | status_code | status_name |
|---:|---|---|
| 1 | E | EFFECTIVE |
| 2 | N | NEW |
| 3 | R | RECRUIT |
| 4 | D | DESERTER |
| 5 | M | MISSING |
| 6 | W | WAR PRISONER |
| 7 | C | DEATH |
| 8 | G | DISCHARGE |
| 9 | S | DISMIS |
| 10 | B | BATTLE |
| 11 | K | KILLED |
| 12 | I | INVALIDOUT |
| 13 | O | TRANSFEREDOUT |
| 14 | X | COMMISSIONED |
| 15 | 1 | FS-DEATH |
| 16 | 2 | FS-DISCHARGE |
| 17 | 3 | FS-DISMIS |
| 18 | 4 | FS-BATTLE |
| 19 | 5 | FS-KILLED |
| 20 | 6 | FS-DESERTER |
| 21 | 7 | FS-INVALID |
| 22 | 8 | FS-TFROUT |
| 23 | 9 | FS-COMMISSIONED |
| 26 | A | ABSENT |
| 27 | L | DISCH CANCEL |
| 28 | H | DEATH CANCEL |
| 29 | T | TFRDEP CANCEL |
| 30 | U | TOUT CANCEL |
| 31 | F | FS-REMOVAL |

### Utils.getReferenceDo2List()

Proven body: `dolphin/utils/Utils.java:1963-1997`. It gets the current employee from `Utils.getCurrentEmployee(do2.getArmyNo())`, reads `do2.refDo2No`, and only searches when `refDo2No.length() == REF_DO2_NUM_LENGTH`. It splits the reference via `getRefDo2Parts(refDo2No)`.

For non-`CANCEL` descriptions, it calls `do2Repo.findByEmpIdStatusDo2TypeDo2NoItemNoAndYear(emp.id, 'V', refDo2Parts[0], refDo2Parts[1], refDo2Parts[2], refDo2Parts[3])` (`Utils.java:1972-1975`; repository query at `dolphin/repositories/Do2Repository.java:267-270`).

For `CANCEL`, it calls `do2Repo.findByEmpIdDo2TypeDo2NoItemNoAndYear(...)` without the `status='V'` filter (`Utils.java:1976-1979`; exact repository line not re-read in continuation output, still needs final line citation before implementation). If the current description is `CANCEL` and `columnA == "INCR"`, the returned list is filtered in memory to only rows whose `fromDate` equals current `do2.fromDate` (`Utils.java:1986-1994`).

Proven leakage concern: no `created_at < current.created_at` condition exists in this method.

### Duplicate publication

Selector body: `dolphin/do2/Do2GenericProcessor.java:267-373`.

Exclusions that skip duplicate check: descriptions `ATCORR`, `LRA`, `DA`, `CEATWN`, `NOK`; audit-cage ceased rows; and system `TPTL` granted `TDREVIEW` rows (`Do2GenericProcessor.java:268-273`).

Branches:

- `BUNCH`: `findByDescriptionStatusNotEqualAndNotThisIdForBunch(empId, currentId)` (`Do2GenericProcessor.java:277-279`; query `description='BUNCH' AND status='V' AND id!=:thisId` at `Do2Repository.java:2324-2325`).
- Ration-related descriptions from `Do2Manager.getRationRelatedDescriptions()` (`Do2Manager.java:461-470`): match `employee.id`, `fromDate`, `toDate`, `description`, `id != current`, and `status IN ('V','P')` (`Do2GenericProcessor.java:281-282`; `Do2Repository.java:1206-1207`).
- `CEA`, `CEAHC`, `HOSSUB`, `HSBTWN`, `CHNOK`, `CHSRNM`, `CHFMNM`: match `employee.id`, `fromDate`, `description`, `columnA`, `columnB`, `columnC`, `id != current`, `status IN ('V','P')` (`Do2GenericProcessor.java:283-288`; `Do2Repository.java:1209-1212`).
- `CANCEL`: match `employee.id`, `fromDate`, `description`, `columnA`, `refDo2No`, `id != current`, `status IN ('V','P')` (`Do2GenericProcessor.java:290-292`; `Do2Repository.java:1214-1216`).
- `SCCIA` or `MEDAL` with `columnA='GRANTED'`, and `MEDAL` `AWARDED`, `UID`, `BIOMET`: match `employee.id`, `fromDate`, `description`, `columnA`, `columnB`, `id != current`, `status IN ('V','P')` (`Do2GenericProcessor.java:295-303`; `Do2Repository.java:1218-1221`).
- Default: match `employee.id`, `fromDate`, `description`, `columnA`, `id != current`, `status IN ('V','P')` (`Do2GenericProcessor.java:304-305`; `Do2Repository.java:1223-1225`).

AO Estt No.22 fallback: if no duplicate and PAO is `AO_ESTTNO22`, rank abbreviations in `columnA` are normalized for `PROMOT`, `LORANK`, `REVERT`, and `MACPS`, then `findByDescColAStatusValidPendingFromDateEqualDo2IdNotEqual` checks `status IN ('V','P')`, same employee, same description, same from date, normalized `columnA`, id not equal (`Do2GenericProcessor.java:308-347`; repository query `Do2Repository.java:1101-1104`).

Proven leakage concern: none of these duplicate queries uses `created_at < current.created_at`.

### Overlap logic

DO2 entry point: `Do2GenericProcessor.isOverLapping()` uses `toDate = fromDate` when current `toDate` is null; only runs if current description is in `Do2Manager.getMutexNames()`; then calls `Utils.isMutexAllowance(...)` (`dolphin/do2/Do2GenericProcessor.java:255-263`).

Allowed historical rows: `Utils.getValidDo2s()` calls `do2Repo.validDo2List(employee.id, SYSTEM_GENERATED, current.id, VALID, fromDate, toDate)` (`dolphin/utils/Utils.java:815-821`). The repository requires same employee, `transcriptionType != 'S'`, `id != current`, `status='V'`, and inclusive date overlap by four OR conditions (`dolphin/repositories/Do2Repository.java:260-265`).

Active overlap method: `Utils.isMutexAllowance()` at `dolphin/utils/Utils.java:1128-1359`; the earlier same-name method at `Utils.java:985-1125` is inside a block comment and is not active.

Mutex set: `TYOJ`, `TYRJ`, `DRDSRA`, `AL`, `CL`, `SL`, `EXTNAL`, `EXTNCL`, `EXTNSL`, `TJRA`, `RTYOJ`, `EXTNLV`, `RTYRJ`, `RDAILY`, `HATRIP`, `MRA`, `FRA`, `SRA`, `GSRA`, `NRA`, `ISNRA`, `ISMRA`, `ISFRA`, `ISHRA`, `SPLLVE`, `PRPLVE`, `PAL`, `BAL`, `ADVAL`, `ACCMAL`, `RL`, `DAILY`, `POSDA`, `LDAILY` (`Do2Manager.java:420-429`).

Journey overlap descriptions: `TYOJ`, `TYRJ`, `NRA`, `RTYOJ`, `RTYRJ`, `RDAILY`, `DAILY`, `LDAILY` (`Do2Manager.java:391-396`). New journey overlap descriptions: `RTYOJ`, `RTYRJ`, `RDAILY` (`Do2Manager.java:401-405`).

Mutual-exclusion exceptions: active method returns false for many boundary-touch cases using parsed time values from `columnA/columnB` or `tjra/lra` (`Utils.java:1228-1328`), allows `RMAHCO` over `AL/BAL/PAL/CL` (`Utils.java:1150-1156`), skips some ceased current allowances using `findCeasedDo2` (`Utils.java:1176-1184`; query at `Do2Repository.java:1816-1818`), and suppresses overlap on recalled leave when `RECALL` covers the same leave `toDate` (`Utils.java:1330-1340`; query at `Do2Repository.java:1811-1814`). If none of these exceptions applies, it sets reason `OL:Over Lapping with ...` and returns true (`Utils.java:1348-1355`).

### Abolished descriptions and Seventh CPC allowance date

Seventh CPC allowance date is exactly `2017-07-01` from `DateManager.getSeventhCpcAllowanceDate()` (`dolphin/utils/DateManager.java:998-1000`).

Abolished descriptions are exactly: `CILQ`, `SNLQ`, `FAA`, `STRINC`, `HCA`, `FLCHCA`, `ADALLC`, `ACTGA`, `COMNDO`, `INSTAL`, `OUTFIT`, `HSINC`, `HCWA`, `CONDMT`, `BOUNTY`, `RLCA1`, `RLCA2`, `RLCA3`, `RLCA4`, `TYOJ`, `TYRJ`, `DAILY` (`dolphin/do2/Do2Manager.java:1770-1785`).

The active invalidation check is `!do2.fromDate.before(2017-07-01)` and description in that set, then status `I` and reason `VL:Abolished with effect from 01-07-2017` (`dolphin/controllers/Do2Controller.java:2940-2945`).

### PROFIX / OPTFIX pay-fixation logic

OPTFIX top-level validation is `dolphin/do2/auditcage/Optfix.java:45-177`. It validates from date (`>= 2016-01-01`, `Optfix.java:237-250`), to date (`toDate != null`, not after employee `fsDueDate`, no prior discharge type before option date, and after `2016-01-01`, `Optfix.java:253-277`), valid `columnB` values by `columnA` (`Optfix.java:279-312`), duplicate `OPTFIX` by same employee and `toDate` with status `V/P` (`Optfix.java:226-235`; query `Do2Repository.java:570-572`), valid promotion/MACPS row on `fromDate` (`Optfix.java:316-347`; query `Do2Repository.java:385-387`), promotion-beneficial rank change (`Optfix.java:349-390`), no `OPTCPC` on `fromDate` or `toDate` (`Optfix.java:393-400`; query `Do2Repository.java:574-576`), increment due on option date (`Optfix.java:443-577`; helper queries `Do2Repository.java:412-414`, `2042-2043`, `583-587`), and 7CPC pay drawn from effective audit cage (`Optfix.java:580-605`).

OPTFIX side effects, not features: when valid it may create a system-generated `PROFIX`/`ACPFIX`/`HAVFIX` DO2 (`Optfix.java:624-703`) and update promotion `toDate` or employee increment date in process (`Optfix.java:187-204`). These are post-decision workflow effects and must not be current-row model inputs.

PROFIX top-level validation is `dolphin/do2/auditcage/Profix.java:41-119`. It rejects TA pre-7CPC cases (`Profix.java:43-49`), fills missing `refDo2No` (`51-54`), rejects duplicate PROFIX (`55-60`; detail `693-751`; query `Do2Repository.java:393-396`), requires referenced promotion availability (`63-68`; detail `648-690`), validates 7CPC option path (`70-74`; detail `841-870`; queries `Do2Repository.java:661-668`), requires valid increment on PROFIX date in 7CPC (`76-92`; query `Do2Repository.java:385-387`), requires promotion benefit (`95-98`; detail `872-930`), and handles paid-acting-before-substantive (`102-105`; detail `758-839`).

### Employee status/fs_due_date history

DB-proven: no tables matching `%employee%status%`, `%status%history%`, `%fs%due%`, or `%history%` were returned from `information_schema.tables` in the configured schema. `employee` itself includes `qe`, `current_record`, `old_id`, `status_effect_date`, `fk_status_code`, `fk_fs_status_code`, and `fs_due_date` in the live schema. Code-proven current lookup uses only `currentRecord=true` (`EmployeeRepository.java:42-50`).

Assumption to avoid: do not assume current `employee.fs_due_date` and status are point-in-time correct for historical training rows. That must be validated against historical employee rows by `qe/old_id/current_record` before using in training.

### Requested table schemas

Live DB metadata found these requested tables: `do2`, `employee`, `status_code`, `family_detail`, `rank`, `unit`, `unit_master`, `appendix_j`, `allowance_rate`, `audit_cage`, `audit_cage_allowance`, `lpc_register`.

Live DB metadata did not find requested tables named `city`, `station`, `do2_description`, or cancellation-specific tables matching `%cancel%` / `%do2%cancel%` in the configured schema. Cancellation is represented in `do2.status='C'`, `do2.description='CANCEL'`, `do2.ref_do2_no`, and audit-cage columns such as `audit_cage.cancelled` / `fk_cancel_do2`.

Important columns by table:

- `family_detail`: `id`, `fk_do2`, `fk_employee`, `name_of_member`, `date_of_birth`, `relationship`, `gender`, `govt_employee`, `dependant`, `status_of_member`, `adopted`, `from_date`, `to_date`, `created_at`, `record_status`, `reason`, `uid_no`, `pan_no`, `email`, `remarks`.
- `rank`: `id`, `rank_code`, `rank_name`, `color_service`, `service_retention`, `maximum_age`, `maximum_service`, `created_at`, `ahmed_nagar_rank_name`.
- `unit`: `id`, `unit_code`, `task`, `unit_name`, `sus_no`, address fields, `station`, phone/email fields, `fk_pao`, `field_peace`, `raised_date`, `closed_date`, `regular_ta`, `approved`.
- `appendix_j`: `id`, `description`, `do2_group`, `column_a`, `column_b`, `column_c`, `from_date_req`, `to_date_req`, `rate_req`, `tjra_req`, `lra_req`, `oc_cm_req`, `ref_do2_no_req`, `audit_account_cage`, `description_type`, `special`, `sort_priority`.
- `allowance_rate`: `id`, `allowance_name`, `sub_allowance_name`, `fk_rank`, `fk_soldier_group`, `from_date`, `to_date`, `grade_pay`, `city`, `gso_code`, pay range fields, `rate_percentage`, `rate_amount`, `fk_pao`.
- `audit_cage`: `id`, `fk_employee`, `fk_do2`, `from_date`, `to_date`, pay fields, `qe`, `absent_flag`, `deputation_flag`, `record_status`, `created_at`, `fk_cancel_do2`, `cancelled`, `army_no`, `do2no`, `do2descr`, `discharged`, `current_quarter`, `old_id`, `effective`, `deputation_status`, `pay_level`.
- `lpc_register`: `id`, `fk_employee`, `fk_discharge_type`, `discharge_date`, `issue_date`, `created_at`, `army_no`, `check_digit`, `record_status`, `entry_approved`, FS approval actor/date fields, `ecs_generated`, `whether_misc_fs`, `whether_provisional_fs`, `provisional_fs_approved`.

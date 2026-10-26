# Dolphin DO2 Validation Audit

This note maps how Dolphin validates rows in the persistent `do2` table and where each validation stage lives in source.

## Main Processing Stages

0. Insertion/pre-save validation for some UI paths
   - Source: `dolphin/do2/Do2InsertionValidation.java:359`
   - Validates selected employee, employee unit, task-holder authorization, and then calls description-specific `isValid(do2)` by reflection.
   - Important: the generic `Do2GenericProcessor.isValidDo2()` call in this class is currently commented out at `Do2InsertionValidation.java:392-406`; generic validation is therefore mainly enforced during later pending/update processing.

1. `Do2Controller.update(Do2, boolean)`
   - Source: `dolphin/controllers/Do2Controller.java:2816`
   - Rejects empty `armyNo` or `description`.
   - Prevents editing records already `V` or cancel-status `C`.
   - Prevents editing manual-rejection records when protected by `do2_control`.
   - Routes pending/edit records into `process(do2, throughBatch)`.

2. `Do2Controller.process(Do2, boolean)` pre-generic validation
   - Source: `dolphin/controllers/Do2Controller.java:2921`
   - Resolves current employee from `employee`.
   - Handles special PAN lookup for `PFWDL`.
   - Applies abolished-description checks from 7CPC date onward.
   - Applies PAO-specific and description-specific hard stops.
   - Reads `appendix_j` to decide audit cage vs accounting cage.
   - Instantiates `Do2GenericProcessor` and calls `isValidDo2()`.

3. `Do2GenericProcessor.isValidDo2()`
   - Source: `dolphin/do2/Do2GenericProcessor.java:1333`
   - Performs generic structural, date, employee, unit, Appendix-J, duplicate, overlap, reference-DO2, absence, deputation, and back-date checks.
   - Saves invalid/pending reasons directly into `do2.status` and `do2.reason`.

4. Post-generic dispatch
   - Source: `dolphin/controllers/Do2Controller.java:3336`
   - For `CANCEL` accounting-cage records, calls `AccountingCageHelper.processCancel(do2)`.
   - For cease/leave records, may call special processors.
   - If audit-cage, calls `AuditCageProcessor.process(do2)`.
   - If accounting-cage, calls the description-specific `Do2AccountingCageProcessor.isValid(do2)` and then `process(do2)`.

5. Description-specific validation
   - Audit-cage descriptions are handled by classes under `dolphin/do2/auditcage`.
   - Accounting-cage descriptions are dynamically loaded by name from:
     - `dolphin/do2/accountingcage/dependent`
     - `dolphin/do2/accountingcage/independent`
   - Dispatcher source: `dolphin/do2/AccountingCageHelper.java:82`
   - There are 418 Java files under `dolphin/do2/auditcage` and `dolphin/do2/accountingcage`.

## Controller Pre-Generic Rules

Source: `dolphin/controllers/Do2Controller.java:2921-3204`

| Stage | Rule | Status/reason | Main tables/classes |
|---|---|---|---|
| Save/update gate | `armyNo` or `description` blank | `I`, `Cannot save a do2 without Army No. and Description` | `do2` |
| Edit gate | Existing `V` or cancel `C` cannot be edited | no process | `do2` |
| MR edit gate | MR/rejection records protected by `do2_control` cannot be edited | no process | `do2`, `do2_control`, `rejection_detail` |
| Employee resolution | `Utils.getCurrentEmployee(armyNo)` | later `M` if missing | `employee` |
| 7CPC abolished descriptions | `fromDate >= 2017-07-01` and description in abolished list | `I`, `VL:Abolished with effect from 01-07-2017` | `do2`, `Do2Manager` |
| APPT after 7CPC | `APPT` after 7CPC allowance date | `V`, non-actionable | `do2` |
| BOUNTY after TAALL date | BOUNTY with DO2 date after TAALL start | `P`, publish as TAALL | `do2`, `DateManager` |
| Pay matrix CRC | `Utils.crcValueForPayMatrixMatches()` false | `P`, CRC mismatch | `pay_matrix` or reference CRC source |
| Missing employee | non-`CANCEL` with no employee | `M`, `MM:Master Missing` | `employee`, `do2` |
| Missing employee CANCEL | reference found and status `M` can cancel master-missing ref; otherwise master missing | `V` or `M` | `do2` self-reference |
| CONDMT hold | `CONDMT` | `P`, orders awaited | `do2` |
| CILQ age | CILQ GRANTED needs DOB and age >= 25 years | `I` | `employee.birthDate`, `do2` |
| FS future for regular army | Regular employee and FS desc cannot be beyond current quarter | `I` | `employee`, `do2` |
| BUNCH feed source | BUNCH must be transcription `T` | `I` | `do2` |
| REFIXP feed source | Non-DSC REFIXP cannot be direct `D` | `I` | `do2`, PAO |
| APS CEA direct feed | APS CEA direct feed must be edited after pre-audit | `I` | `do2`, PAO |
| DAA | Always inadmissible | `I`, allowance not admissible | `do2` |
| PAO not-valid lists | DSC/Estt22 blocked descriptions | `I` | `Do2Manager`, PAO |
| System-generated-only descriptions | System-generated desc with transcription not `S` | `I` | `Do2Manager`, `do2` |
| Appendix-J lookup | description or `CANCEL.columnA` must exist unless BUNCH | reason only, returns | `appendix_j` |

## Insertion-Time Validation

Source: `dolphin/do2/Do2InsertionValidation.java:359-460`

| Rule | Outcome | Main tables/classes |
|---|---|---|
| Employee must be selected | returns invalid message `Employee not selected.` | `employee`, selected `do2.employee` |
| Employee must have unit | returns invalid message `Unit not defined for Employee.` | `employee.unit` |
| User must hold employee unit task, unless higher level or non-effective exception | returns invalid message `Not authorised task holder.` | `task`, `unit`, `usr`, `employee.statusCode` |
| `CANCEL` and `ATCORR` are insertion-exempt | returns valid without description-specific validation | `do2` |
| Audit/accounting routing | uses `appendix_j` to decide audit vs accounting cage | `appendix_j` |
| Description-specific validation | reflection loads `dolphin.do2.auditcage.<Description>` or accounting processor and calls `isValid(do2)` | 418 validator files |

Batch/XML import also inserts directly into `do2` through repository native insert methods:

- `dolphin/do2/Do2ControlImportDo2Xml.java:608-619`
- `dolphin/repositories/Do2Repository.java:1174-1194`

Those import paths populate `status`, `reason`, `transcription_type`, `qe`, `input_qe`, `fk_employee`, and `fk_unit`; later `/processPendingDo2` or batch jobs re-run the controller/generic/description validation path.

## Generic Processor Rules

Source: `dolphin/do2/Do2GenericProcessor.java:1333-2003`

| Order | Rule family | Status/reason | Main tables/classes |
|---|---|---|---|
| 1 | FS account already closed | false, reason `THIS ACCOUNT ALREADY CLOSED` | FS/account utilities |
| 2 | Employee effective-status check | `N`, `VL:Status in Employee table is Not Effective` | `employee`, `status_code`, `lpc_register` |
| 3 | Non-effective exceptions | `DEATH`, some `ATCORR`, approved LPC/FS death CEA cases may continue | `employee`, `lpc_register`, `do2` |
| 4 | From/to date after FS due date | `N` or `I` for death CEA service/age limits | `employee.fs_due_date`, `lpc_register` |
| 5 | This-month FS discharge descriptions | false, `To be fed in following month` | `employee`, `Do2Manager` |
| 6 | System-generated skip | Some `S` transcription descriptions bypass generic validation | `Do2Manager`, `do2` |
| 7 | Required DO2 no/item no | invalid buffer | `do2.do2_no`, `do2.do2_item_no` |
| 8 | Auto-fill `toDate` | if accounting cage and `tjra == 1`; or valid time in `columnB` | `appendix_j`, `do2` |
| 9 | Contingent bill window | `TFRGTH`, `TFRGTL`, `ENCASH/LTC` between 2013-04-01 and 2014-07-31 | invalid buffer | `do2`, `DateManager` |
| 10 | Allowance birth/death dates | from/to dates must be inside allowance valid period | `allowance_rate`, `appendix_j` |
| 11 | Date validity | invalid from/to, from after to, to beyond current quarter, arrears claim constraints | `do2`, `DateManager`, `Do2Manager` |
| 12 | DO2 group/type | group in `IA, IB, IIA, IIB, ID`; type in `0,1,2,3`, with extra type rules | `do2` |
| 13 | DSC-only SRGRTY/DCRG | PAO code/type restrictions | PAO, `do2` |
| 14 | Record-office publication | descriptions in record-office list must satisfy RO publication rules | `Do2Manager`, `do2` |
| 15 | DO2 date | required, not future, not before enrolment | `do2`, `employee.enrolmentDate` |
| 16 | From date current-quarter/enrolment | not after current quarter except FS/discharge exceptions; not before enrolment | `employee`, `do2` |
| 17 | From date vs DO2 date | from date cannot be later than publication date except listed FS/cancel/leave/pay exceptions | `do2`, `Do2Manager` |
| 18 | Army number exists | current employee must exist | `employee` |
| 19 | Appendix-J format | required/blank columns, ABC combination, ref DO2 format, rate/TJRA/LRA/punishment rules | `appendix_j`, `do2` |
| 20 | Unit code | `unit_code` must exist | `unit` |
| 21 | Time-bar publication | rejects when time barred and no sanction | `do2`, `DateManager` |
| 22 | Back-dated audit-cage cutoffs | PAO/TA/general cutoff dates | `DateManager`, PAO, `employee` |
| 23 | Duplicate publication | duplicate same-employee DO2 found | `I`, `DU:...Duplicate Publication...` | `do2` self-reference |
| 24 | Mutual overlap | overlapping mutually exclusive allowance | `I`, `OL:...` | `do2`, `audit_cage`, `Do2Manager` |
| 25 | Reference DO2 existence | ref not found, invalid format, or invalid status | `I` or `P`, ref reason | `do2` self-reference |
| 26 | CANCEL validation | referenced DO2 valid/pending/cancelled checks; may mark referenced row cancelled | `do2` self-reference, `audit_cage` |
| 27 | Absent/deputation period | if PBOR absent or on deputation | `P` or `I` | `do2`, `audit_cage`, `lpc_deputation`, utilities |

## Duplicate Publication Rule

Source:
- `dolphin/do2/Do2GenericProcessor.java:267`
- `dolphin/repositories/Do2Repository.java:1204`

Dolphin searches existing `do2` rows with same employee, same `fromDate`, same description, status `V` or `P`, and `id != current.id`. Extra equality checks depend on description:

| Description group | Extra duplicate key |
|---|---|
| Ration-related descriptions | `toDate` |
| `CEA`, `CEAHC`, `HOSSUB`, `HSBTWN`, `CHNOK`, `CHSRNM`, `CHFMNM` | `columnA`, `columnB`, `columnC` |
| `CANCEL` | `columnA`, `refDo2No` |
| `SCCIA`, `MEDAL` granted, `MEDAL` awarded, `UID`, `BIOMET` | `columnA`, `columnB` |
| Default | `columnA` |
| `BUNCH` | special bunch duplicate query |
| Estt22 rank aliases | second-pass mapped rank check for `PROMOT`, `LORANK`, `REVERT`, `MACPS` |

Explicit duplicate exclusions:
- `ATCORR`
- `LRA`
- `DA`
- `CEATWN`
- `NOK`
- audit-cage `CEASED` rows
- specific system-generated `TPTL GRANTED TDREVIEW`

## Post-Generic Dispatch Rules

Source: `dolphin/controllers/Do2Controller.java:3336-3538`

| Route | Condition | Processor |
|---|---|---|
| Non-audit `CANCEL` | `description=CANCEL` and `columnA` maps to accounting-cage | `AccountingCageHelper.processCancel(do2)` |
| Cease DO2 | `isCeaseDo2(do2)` and valid | `AuditCageCeaseProcessor` |
| Leave DO2 | `isLeaveDo2(do2)` and valid | `AuditCageLeaveProcessor` |
| HOSPTL admit with `toDate` | may auto-generate HOSPTL DISCH DO2 | `do2` self-insert |
| Audit cage | `appendix_j.auditAccountCage == AT`, BUNCH, REJEOL, most system-generated descs | `AuditCageProcessor.process(do2)` |
| Accounting cage | otherwise | dynamically loaded accounting processor `isValid(do2)` then `process(do2)` |
| RECALL | extra leave processor path after accounting validation | `AuditCageLeaveProcessor` |

## Dynamic Description-Specific Validation

Accounting-cage processor loading is reflection-based:

```java
Class.forName("itsdc.dolphin.do2.accountingcage.dependent." + Utils.getCamelCase(description))
Class.forName("itsdc.dolphin.do2.accountingcage.independent." + Utils.getCamelCase(description))
```

Source: `dolphin/do2/AccountingCageHelper.java:82-100`

Audit-cage processing uses `AuditCageProcessor` plus individual description classes under `dolphin/do2/auditcage`. Most of those classes expose `isValid(Do2 do2)` and then set `do2.status`/`do2.reason`.

To enumerate every per-description rule message:

```bash
rg -n "static public String|public static.*String|static public final String|public static final String" \
  dolphin/do2/Do2GenericProcessor.java \
  dolphin/controllers/Do2Controller.java \
  dolphin/do2/auditcage \
  dolphin/do2/accountingcage
```

To enumerate every per-description validator:

```bash
rg -n "public boolean isValid|boolean isValid|void process\\(Do2|public void process\\(Do2|setStatus\\(|setReason\\(" \
  dolphin/do2/auditcage dolphin/do2/accountingcage
```

## Main Table Dependencies

| Table/model | Used for |
|---|---|
| `do2` | Current row, duplicate publication, overlap checks, reference DO2, cancellation, generated DO2s |
| `employee` | current employee lookup, status, FS due date, enrolment/birth dates, rank/group/TA flags |
| `status_code` | employee effective/non-effective semantics |
| `lpc_register` | FS approved/ECS generated cases, discharge date and death-FS exceptions |
| `unit` | `unit_code` validation and unit linkage |
| `appendix_j` | description existence, audit-vs-accounting routing, required/blank fields, valid column combinations |
| `allowance_rate` | allowance birth/death dates and rate availability |
| `audit_cage` | audit-cage base/drawn/due state, absent/deputation, overlap and pay state |
| `audit_cage_allowance` | allowance drawn/due calculations |
| `arrears` | generated arrears and cancellation reversal |
| `employee_service_detail` | ranks, promotions, classifications, muster/service history |
| `rank` | rank validation |
| `pay_matrix`, `pay_scale`, `class_pay_rate`, `appointment_rate` | pay/rate validation |
| `family_detail` | CEA/HOSSUB/NOK/family-related accounting validations |
| `form16`, `employee_earning`, `employee_deduction`, `fund_summary`, `fund_payment_authority`, `fund_purpose`, `dak`, `section` | accounting-cage financial/fund validations |
| `lpc_deputation` | deputation checks |
| `rh_location` | risk/hardship location validations |
| `do2_control` | edit restrictions for batch/MR records |
| `rejection_detail` | manual rejection/rejection-code behavior |

## Important Implementation Observations

- The validation flow is not centralized. Generic validation is central, but many final decisions happen in 418 description-specific files.
- `do2.reason` prefixes indicate validation family: `VL:` structural/format, `AU:` audit/admissibility, `DU:` duplicate, `OL:` overlap, `MM:` master missing, `MR` manual rejection.
- Some code sets `status` directly to string literals (`"I"`, `"N"`, `"P"`, `"MR"`, `"M"`) instead of `Do2Manager` constants.
- Duplicate-publication reason can contain double `DU:` because `DO2_DUPLICATE_PUBLICATION` already includes `DU:` and the method prepends `rejectionType`.
- Some early returns set `reason` but do not consistently set `status`; final status can depend on caller save behavior.
- Exact field requirements come from live `appendix_j` data plus hard-coded exceptions, so a complete runtime rule catalog must include the database contents of `appendix_j`, `allowance_rate`, `status_code`, PAO configuration, and `Do2Manager` static lists.

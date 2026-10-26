"""Bulk PostgreSQL features derived from Dolphin's generic N validations.

These are model inputs, not final status rules.  Outcome/reason columns are
intentionally absent.  `historical` mode uses the bill QE for quarter-scoped
LPC evidence; `realtime` mode uses the supplied runtime quarter.
"""

from collections.abc import Iterable
from typing import Any


NE_CUTOFF_BY_PAO = {
    "94": "2014-01-01",
    "86": "2016-07-01",
    "89": "2016-07-01",
    "99": "2016-07-01",
    "58": "2016-01-01",
    "97": "2018-07-01",
    "53": "2020-01-01",
}
DEFAULT_NE_CUTOFF = "2014-07-01"


N_FEATURE_SQL = r"""
WITH bills AS (
    SELECT
        d.id,
        d.army_no,
        d.fk_employee,
        UPPER(BTRIM(COALESCE(d.description, ''))) AS description,
        d.from_date,
        d.to_date,
        d.tjra,
        d.lra,
        d.qe AS bill_qe,
        CASE WHEN %(mode)s = 'realtime' THEN %(runtime_qe)s ELSE d.qe END AS feature_qe
    FROM do2 d
    WHERE d.id = ANY(%(do2_ids)s)
),
status_rows AS (
    SELECT
        b.id AS do2_id,
        e.id AS matched_employee_id,
        CASE WHEN e.army_no = b.army_no THEN 0 ELSE 1 END AS matched_old_army_no,
        sc.status_code
    FROM bills b
    LEFT JOIN employee e
      ON (e.army_no = b.army_no OR e.old_army_no = b.army_no)
     AND e.current_record IS TRUE
    LEFT JOIN status_code sc ON sc.id = e.fk_status_code
),
status_features AS (
    SELECT
        do2_id,
        COUNT(matched_employee_id)::integer AS current_employee_match_count,
        COUNT(matched_employee_id) FILTER (WHERE matched_old_army_no = 1)::integer
            AS old_army_match_count,
        COUNT(matched_employee_id) FILTER (WHERE status_code IS NULL)::integer
            AS null_status_count,
        COUNT(matched_employee_id) FILTER (
            WHERE status_code IN ('E','N','R','D','M','W','T')
        )::integer AS effective_status_count,
        COUNT(matched_employee_id) FILTER (
            WHERE status_code IS NOT NULL
              AND status_code NOT IN ('E','N','R','D','M','W','T')
        )::integer AS ineffective_status_count
    FROM status_rows
    GROUP BY do2_id
),
bill_employee AS (
    SELECT
        b.id AS do2_id,
        e.id AS bill_employee_id,
        e.fs_due_date,
        e.enrolment_date,
        e.birth_date,
        e.fk_status_code,
        e.fk_pao
    FROM bills b
    LEFT JOIN employee e ON e.id = b.fk_employee
),
account_lpc AS (
    SELECT
        b.id AS do2_id,
        COUNT(l.id)::integer AS account_closed_lpc_count
    FROM bills b
    LEFT JOIN lpc_register l
      ON l.army_no = b.army_no
     AND l.qe = b.feature_qe
     AND l.entry_approved IS TRUE
     AND l.fs_approved IS TRUE
    GROUP BY b.id
),
final_lpc AS (
    SELECT
        b.id AS do2_id,
        COUNT(l.id)::integer AS finalized_lpc_count,
        MIN(l.discharge_date) AS finalized_lpc_discharge_min,
        MAX(l.discharge_date) AS finalized_lpc_discharge_max
    FROM bills b
    LEFT JOIN lpc_register l
      ON l.fk_employee = b.fk_employee
     AND l.record_status = 'F'
     AND l.fs_approved IS TRUE
     AND l.ecs_generated IS TRUE
     AND l.discharge_date > %(ne_cutoff)s::date
    GROUP BY b.id
),
period_lpc AS (
    SELECT
        b.id AS do2_id,
        COUNT(l.id)::integer AS cea_period_lpc_count
    FROM bills b
    LEFT JOIN lpc_register l
      ON l.fk_employee = b.fk_employee
     AND l.record_status = 'F'
     AND l.fs_approved IS TRUE
     AND l.ecs_generated IS TRUE
     AND b.from_date IS NOT NULL
     AND b.to_date IS NOT NULL
     AND l.discharge_date BETWEEN b.from_date AND b.to_date
    GROUP BY b.id
),
death_lpc AS (
    SELECT
        b.id AS do2_id,
        COUNT(l.id)::integer AS death_irla_lpc_count
    FROM bills b
    LEFT JOIN lpc_register l
      ON l.fk_employee = b.fk_employee
     AND l.record_status = 'F'
     AND l.fs_approved IS TRUE
     AND l.ecs_generated IS TRUE
     AND l.stop_pay_description = 'FSDETH'
     AND l.whether_misc_fs IS TRUE
     AND l.cancelled IS FALSE
    GROUP BY b.id
)
SELECT
    b.id AS do2_id,
    (b.army_no IS NOT NULL AND b.feature_qe IS NOT NULL)::integer
        AS account_closed_check_available,
    COALESCE(al.account_closed_lpc_count, 0) AS account_closed_lpc_count,
    (COALESCE(al.account_closed_lpc_count, 0) > 0)::integer AS account_closed_match,
    (%(mode)s = 'realtime')::integer AS account_closed_uses_current_state,

    (b.army_no IS NOT NULL)::integer AS employee_status_check_available,
    COALESCE(sf.current_employee_match_count, 0) AS current_employee_match_count,
    (COALESCE(sf.current_employee_match_count, 0) = 0)::integer AS current_employee_missing,
    (COALESCE(sf.current_employee_match_count, 0) > 1)::integer AS current_employee_multiple,
    COALESCE(sf.old_army_match_count, 0) AS old_army_match_count,
    COALESCE(sf.null_status_count, 0) AS employee_null_status_count,
    (COALESCE(sf.null_status_count, 0) > 0)::integer AS employee_has_null_status,
    COALESCE(sf.effective_status_count, 0) AS employee_effective_status_count,
    (COALESCE(sf.effective_status_count, 0) > 0
      AND COALESCE(sf.null_status_count, 0) = 0)::integer AS employee_any_effective_status,
    COALESCE(sf.ineffective_status_count, 0) AS employee_ineffective_status_count,

    (be.bill_employee_id IS NOT NULL)::integer AS bill_employee_available,
    (be.fs_due_date IS NOT NULL)::integer AS fs_due_date_original_available,
    COALESCE(fl.finalized_lpc_count, 0) AS finalized_lpc_count,
    (COALESCE(fl.finalized_lpc_count, 0) > 0)::integer AS has_finalized_lpc,
    fl.finalized_lpc_discharge_min,
    fl.finalized_lpc_discharge_max,
    (COALESCE(fl.finalized_lpc_count, 0) > 1)::integer AS lpc_first_row_nondeterministic,
    COALESCE(be.fs_due_date, fl.finalized_lpc_discharge_min) AS resolved_fs_due_date,
    (COALESCE(be.fs_due_date, fl.finalized_lpc_discharge_min) IS NOT NULL)::integer
        AS resolved_fs_due_date_available,

    (b.from_date IS NOT NULL AND COALESCE(be.fs_due_date, fl.finalized_lpc_discharge_min) IS NOT NULL)::integer
        AS fs_date_check_available,
    (b.from_date - COALESCE(be.fs_due_date, fl.finalized_lpc_discharge_min))::integer
        AS days_from_date_after_fs_due_date,
    (b.to_date - COALESCE(be.fs_due_date, fl.finalized_lpc_discharge_min))::integer
        AS days_to_date_after_fs_due_date,
    (b.from_date > COALESCE(be.fs_due_date, fl.finalized_lpc_discharge_min))::integer
        AS from_date_after_fs_due_date,
    (b.to_date > COALESCE(be.fs_due_date, fl.finalized_lpc_discharge_min))::integer
        AS to_date_after_fs_due_date,

    (b.description LIKE 'CEA%')::integer AS is_cea,
    (b.description = 'OPTFIX')::integer AS is_optfix,
    (b.description = 'DEATH')::integer AS is_death,
    (b.description = 'ATCORR')::integer AS is_atcorr,
    COALESCE(pl.cea_period_lpc_count, 0) AS cea_period_lpc_count,
    (COALESCE(pl.cea_period_lpc_count, 0) > 0)::integer AS cea_has_period_lpc,
    COALESCE(dl.death_irla_lpc_count, 0) AS death_irla_lpc_count,
    CASE WHEN be.enrolment_date IS NOT NULL AND b.from_date IS NOT NULL
         THEN EXTRACT(YEAR FROM age(b.from_date, be.enrolment_date)) * 12
            + EXTRACT(MONTH FROM age(b.from_date, be.enrolment_date)) END
        AS service_months_at_claim,
    CASE WHEN be.birth_date IS NOT NULL AND b.from_date IS NOT NULL
         THEN EXTRACT(YEAR FROM age(b.from_date, be.birth_date)) * 12
            + EXTRACT(MONTH FROM age(b.from_date, be.birth_date)) END
        AS age_months_at_claim,
    CASE WHEN be.enrolment_date IS NULL OR b.from_date IS NULL THEN NULL
         ELSE ((EXTRACT(YEAR FROM age(b.from_date, be.enrolment_date)) * 12
              + EXTRACT(MONTH FROM age(b.from_date, be.enrolment_date))) > 408)::integer END
        AS service_over_408_months,
    CASE WHEN be.birth_date IS NULL OR b.from_date IS NULL THEN NULL
         ELSE ((EXTRACT(YEAR FROM age(b.from_date, be.birth_date)) * 12
              + EXTRACT(MONTH FROM age(b.from_date, be.birth_date))) > 648)::integer END
        AS age_over_648_months,

    (%(mode)s = 'historical')::integer AS historical_mode,
    (%(mode)s = 'realtime')::integer AS current_state_feature_mode
FROM bills b
LEFT JOIN status_features sf ON sf.do2_id = b.id
LEFT JOIN bill_employee be ON be.do2_id = b.id
LEFT JOIN account_lpc al ON al.do2_id = b.id
LEFT JOIN final_lpc fl ON fl.do2_id = b.id
LEFT JOIN period_lpc pl ON pl.do2_id = b.id
LEFT JOIN death_lpc dl ON dl.do2_id = b.id
ORDER BY b.id
"""


def ne_cutoff_for_pao(pao_code: str | None) -> str:
    return NE_CUTOFF_BY_PAO.get(str(pao_code or "").strip(), DEFAULT_NE_CUTOFF)


def fetch_n_ml_features(
    connection: Any,
    do2_ids: Iterable[int],
    *,
    mode: str,
    pao_code: str | None,
    runtime_qe: str | None = None,
) -> list[dict[str, Any]]:
    """Fetch N-related ML features for many DO2 ids in one database call."""
    ids = [int(value) for value in do2_ids]
    if not ids:
        return []
    if mode not in {"historical", "realtime"}:
        raise ValueError("mode must be 'historical' or 'realtime'")
    if mode == "realtime" and not runtime_qe:
        raise ValueError("runtime_qe is required in realtime mode")

    params = {
        "do2_ids": ids,
        "mode": mode,
        "runtime_qe": runtime_qe,
        "ne_cutoff": ne_cutoff_for_pao(pao_code),
    }
    with connection.cursor() as cursor:
        cursor.execute(N_FEATURE_SQL, params)
        columns = [item[0] for item in cursor.description]
        return [dict(zip(columns, row)) for row in cursor.fetchall()]


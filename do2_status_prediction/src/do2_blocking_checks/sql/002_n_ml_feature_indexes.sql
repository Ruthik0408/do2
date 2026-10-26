-- Apply only after comparing with existing indexes. CONCURRENTLY cannot run
-- inside a transaction block.

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_employee_current_army_status
    ON employee (army_no, fk_status_code, id)
    WHERE current_record IS TRUE;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_employee_current_old_army_status
    ON employee (old_army_no, fk_status_code, id)
    WHERE current_record IS TRUE AND old_army_no IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_lpc_account_closed_lookup
    ON lpc_register (army_no, qe)
    WHERE entry_approved IS TRUE AND fs_approved IS TRUE;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_lpc_finalized_employee_discharge
    ON lpc_register (fk_employee, discharge_date)
    WHERE record_status = 'F' AND fs_approved IS TRUE AND ecs_generated IS TRUE;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_lpc_death_irla_employee
    ON lpc_register (fk_employee)
    WHERE record_status = 'F'
      AND fs_approved IS TRUE
      AND ecs_generated IS TRUE
      AND stop_pay_description = 'FSDETH'
      AND whether_misc_fs IS TRUE
      AND cancelled IS FALSE;

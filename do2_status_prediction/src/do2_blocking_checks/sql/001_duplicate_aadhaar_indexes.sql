-- Verify equivalent indexes do not already exist before applying.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_employee_uid_current_qe
    ON employee (uid_no, qe, id)
    WHERE current_record IS TRUE AND uid_no IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_family_detail_uid_employee
    ON family_detail (uid_no, fk_employee, relationship)
    WHERE record_status = 'V' AND uid_no IS NOT NULL;

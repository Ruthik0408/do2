# DO2 blocking-check engine (phase 1)

This package predicts whether a known blocking condition applies to a DO2 bill.
It does **not** infer `V` merely because no rule matched.

Final decision policy:

1. Any `BLOCKED` check => return that check's non-V status.
2. No blocked checks, but any applicable check is `UNAVAILABLE` => `REVIEW`.
3. `V` is allowed only when every applicable, required check returned `PASSED`.

The first implemented rule is `I_DUPLICATE_AADHAAR`, based on Dolphin's
`Uid.java` / `Biomet.java` behavior. For reproducible historical feature
generation, it uses the bill's `qe`, rather than the quarter on the machine
running the feature job.

Family-detail parity is different: Dolphin filters `family_detail.record_status
= 'V'` but does not apply a quarter/current-employee filter. A match owned by
the same employee passes only when the relationship also matches; `wife` and
`spouse` are treated as equivalent.

## Run tests

```bash
python -m unittest discover -s tests -v
```

## Database indexes

Apply `sql/001_duplicate_aadhaar_indexes.sql` after checking existing indexes.

## Bulk ML features for N

`do2_checks.n_ml_features.fetch_n_ml_features` enriches a list of DO2 ids in
one PostgreSQL call. It produces raw counts, date differences, condition flags,
and availability indicators derived from Dolphin's account-closed,
employee-status, LPC, FS-date, CEA, death-IRLA, and OPTFIX logic.

```python
rows = fetch_n_ml_features(
    connection,
    do2_ids,
    mode="historical",  # uses each bill's qe for quarter-scoped LPC evidence
    pao_code="94",
)
```

For live scoring, pass `mode="realtime"` and Dolphin's current quarter as
`runtime_qe`. The extractor never selects DO2 status or reason fields.

The current-employee features reproduce Dolphin's current-record lookup and
are explicitly marked as current-state features. Do not assume that these
values reconstruct the employee state that existed for an old training row.

## Next implementation inputs needed

For the two highest-coverage N families, locate the Java code producing:

- `VL:Status in employee table is not effective.`
- `From Date is after FS Due Date.`

The exact Java condition is required before converting them into checks.

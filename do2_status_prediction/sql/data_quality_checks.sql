-- 1. Exact binary target counts for the project.
SELECT
    CASE
        WHEN UPPER(BTRIM(REPLACE(COALESCE(status, ''), '"', ''))) = 'V'
            THEN 'V'
        ELSE 'OTHER'
    END AS target_class,
    COUNT(*) AS row_count
FROM do2
WHERE created_at >= DATE '2021-01-01'
  AND created_at < DATE '2025-01-01'
GROUP BY 1
ORDER BY 1;

-- 2. Counts for the exact train, validation, and test periods.
SELECT
    CASE
        WHEN created_at >= DATE '2021-01-01'
         AND created_at <  DATE '2024-01-01' THEN 'TRAIN_2021_2023'
        WHEN created_at >= DATE '2024-01-01'
         AND created_at <  DATE '2024-07-01' THEN 'VALIDATION_2024_H1'
        WHEN created_at >= DATE '2024-07-01'
         AND created_at <  DATE '2025-01-01' THEN 'TEST_2024_H2'
    END AS split,
    CASE
        WHEN UPPER(BTRIM(REPLACE(COALESCE(status, ''), '"', ''))) = 'V'
            THEN 'V'
        ELSE 'OTHER'
    END AS target_class,
    COUNT(*) AS row_count
FROM do2
WHERE created_at >= DATE '2021-01-01'
  AND created_at < DATE '2025-01-01'
GROUP BY 1, 2
ORDER BY 1, 2;

-- 3. Monthly drift check.
SELECT
    DATE_TRUNC('month', created_at)::date AS month,
    COUNT(*) AS total_rows,
    COUNT(*) FILTER (
        WHERE UPPER(BTRIM(REPLACE(COALESCE(status, ''), '"', ''))) <> 'V'
    ) AS other_rows,
    ROUND(
        100.0 * COUNT(*) FILTER (
            WHERE UPPER(BTRIM(REPLACE(COALESCE(status, ''), '"', ''))) <> 'V'
        ) / COUNT(*),
        2
    ) AS other_pct
FROM do2
WHERE created_at >= DATE '2021-01-01'
  AND created_at < DATE '2025-01-01'
GROUP BY 1
ORDER BY 1;

-- 4. Ensure each evaluation half contains both classes.
SELECT
    CASE
        WHEN created_at < DATE '2024-07-01' THEN '2024_H1'
        ELSE '2024_H2'
    END AS half,
    COUNT(*) FILTER (
        WHERE UPPER(BTRIM(REPLACE(COALESCE(status, ''), '"', ''))) = 'V'
    ) AS valid_rows,
    COUNT(*) FILTER (
        WHERE UPPER(BTRIM(REPLACE(COALESCE(status, ''), '"', ''))) <> 'V'
    ) AS other_rows
FROM do2
WHERE created_at >= DATE '2024-01-01'
  AND created_at < DATE '2025-01-01'
GROUP BY 1
ORDER BY 1;


INSERT INTO notification_setting_entity (
    user_id,
    notification_type,
    is_enabled
)
WITH RECURSIVE numbers AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1
    FROM numbers
    WHERE n < 100
)
SELECT
    CONCAT('user_', LPAD(n, 3, '0')),
    'PAYMENT',
    CASE
        WHEN n <= 80 THEN TRUE
        ELSE FALSE
        END
FROM numbers;
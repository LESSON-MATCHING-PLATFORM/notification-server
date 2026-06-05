INSERT INTO token_entity (user_id, token)
WITH RECURSIVE numbers AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1
    FROM numbers
    WHERE n < 100
)
SELECT
    CONCAT('user_', LPAD(n, 3, '0')),
    CONCAT('token-user-', LPAD(n, 3, '0'), '-web')
FROM numbers

UNION ALL

SELECT
    CONCAT('user_', LPAD(n, 3, '0')),
    CONCAT('token-user-', LPAD(n, 3, '0'), '-mobile')
FROM numbers

UNION ALL

SELECT
    CONCAT('user_', LPAD(n, 3, '0')),
    CONCAT('token-user-', LPAD(n, 3, '0'), '-tablet')
FROM numbers
WHERE MOD(n, 2) = 0;
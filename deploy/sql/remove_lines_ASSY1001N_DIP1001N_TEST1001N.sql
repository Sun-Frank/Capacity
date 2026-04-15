BEGIN;

-- Target lines to remove from production line configuration and related business data.
-- Keep delete order explicit for safety and readability.

DELETE FROM line_realtime
WHERE line_code IN ('ASSY1001N', 'DIP1001N', 'TEST1001N');

DELETE FROM routing_item
WHERE line_code IN ('ASSY1001N', 'DIP1001N', 'TEST1001N');

DELETE FROM product
WHERE line_code IN ('ASSY1001N', 'DIP1001N', 'TEST1001N');

DELETE FROM family_line
WHERE line_code IN ('ASSY1001N', 'DIP1001N', 'TEST1001N');

DELETE FROM product_family
WHERE line_code IN ('ASSY1001N', 'DIP1001N', 'TEST1001N');

DELETE FROM line_profile
WHERE line_code IN ('ASSY1001N', 'DIP1001N', 'TEST1001N');

DELETE FROM line_config
WHERE line_code IN ('ASSY1001N', 'DIP1001N', 'TEST1001N');

COMMIT;

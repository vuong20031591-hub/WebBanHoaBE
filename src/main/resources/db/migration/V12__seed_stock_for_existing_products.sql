-- Ensure existing catalog is purchasable in dev by assigning stock
-- to products that were migrated with default stock_quantity = 0.
UPDATE products
SET stock_quantity = 20
WHERE stock_quantity = 0
  AND deleted_at IS NULL;

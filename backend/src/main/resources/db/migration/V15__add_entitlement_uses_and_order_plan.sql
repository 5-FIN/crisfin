-- CrisFin V15: multi-plan support — remaining uses on entitlements, plan on orders
ALTER TABLE entitlements ADD COLUMN remaining_uses INT;      -- NULL = unlimited
ALTER TABLE payment_orders ADD COLUMN plan VARCHAR(30);

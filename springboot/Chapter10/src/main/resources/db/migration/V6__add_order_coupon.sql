-- orders/migrations/0003_order_coupon_order_discount.py
ALTER TABLE orders_order ADD COLUMN coupon_id BIGINT REFERENCES coupons_coupon (id) ON DELETE SET NULL;
ALTER TABLE orders_order ADD COLUMN discount INTEGER NOT NULL DEFAULT 0 CHECK (discount BETWEEN 0 AND 100);
CREATE INDEX orders_order_coupon_id_idx ON orders_order (coupon_id);

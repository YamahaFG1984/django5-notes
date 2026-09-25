-- orders/migrations/0002_order_stripe_id.py
ALTER TABLE orders_order ADD COLUMN stripe_id VARCHAR(250) NOT NULL DEFAULT '';

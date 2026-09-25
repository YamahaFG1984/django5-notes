-- 示例数据：只在 sample profile 下加载（版本号取得很大，避免和正式迁移冲突）
INSERT INTO shop_category (name, slug) VALUES ('Tea', 'tea'), ('Coffee', 'coffee');

INSERT INTO shop_product (category_id, name, slug, description, price, available, created, updated)
SELECT c.id, v.name, v.slug, v.description, v.price, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM shop_category c
JOIN (VALUES
    ('tea',    'Green tea',   'green-tea',   'A light and refreshing green tea.', 30.00),
    ('tea',    'Red tea',     'red-tea',     'A full-bodied red tea.',            45.50),
    ('tea',    'Tea powder',  'tea-powder',  'Matcha tea powder.',                21.20),
    ('coffee', 'Espresso',    'espresso',    'Dark roasted espresso beans.',      18.90)
) AS v(category, name, slug, description, price) ON v.category = c.slug;

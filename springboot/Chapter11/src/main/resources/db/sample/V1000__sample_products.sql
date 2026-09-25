-- 示例数据（sample profile）：主表 + 英文、西班牙文译文
INSERT INTO shop_category (id) VALUES (1), (2);
INSERT INTO shop_category_translation (master_id, language_code, name, slug) VALUES
    (1, 'en', 'Tea', 'tea'),        (1, 'es', 'Té', 'te'),
    (2, 'en', 'Coffee', 'coffee'),  (2, 'es', 'Café', 'cafe');

INSERT INTO shop_product (id, category_id, price, available, created, updated) VALUES
    (1, 1, 30.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 1, 45.50, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 1, 21.20, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 2, 18.90, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO shop_product_translation (master_id, language_code, name, slug, description) VALUES
    (1, 'en', 'Green tea', 'green-tea', 'A light and refreshing green tea.'),
    (1, 'es', 'Té verde', 'te-verde', 'Un té verde ligero y refrescante.'),
    (2, 'en', 'Red tea', 'red-tea', 'A full-bodied red tea.'),
    (2, 'es', 'Té rojo', 'te-rojo', 'Un té rojo con cuerpo.'),
    (3, 'en', 'Tea powder', 'tea-powder', 'Matcha tea powder.'),
    (4, 'en', 'Espresso', 'espresso', 'Dark roasted espresso beans.');

-- 显式指定了 id，要把自增序列推到后面，否则之后新增的行会主键冲突
ALTER TABLE shop_category ALTER COLUMN id RESTART WITH 100;
ALTER TABLE shop_product ALTER COLUMN id RESTART WITH 100;

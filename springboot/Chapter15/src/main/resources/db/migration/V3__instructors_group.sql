-- 书中在 admin 里手动创建 Instructors 组并勾选权限；这里用数据迁移完成同样的事
-- （≈ Django 的 RunPython 数据迁移），新环境 migrate 之后就有这个组。
INSERT INTO auth_group (name) VALUES ('Instructors');

INSERT INTO auth_group_permissions (group_id, codename)
SELECT g.id, p.codename
FROM auth_group g
CROSS JOIN (VALUES
    ('courses.add_course'), ('courses.change_course'), ('courses.delete_course'), ('courses.view_course'),
    ('courses.add_module'), ('courses.change_module'), ('courses.delete_module'), ('courses.view_module'),
    ('courses.add_content'), ('courses.change_content'), ('courses.delete_content'), ('courses.view_content')
) AS p(codename)
WHERE g.name = 'Instructors';

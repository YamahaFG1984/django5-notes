-- blog/migrations/0005_trigram_ext.py：operations = [TrigramExtension()]
-- 创建扩展需要足够的数据库权限；生产环境通常由 DBA 事先执行
CREATE EXTENSION IF NOT EXISTS pg_trgm;

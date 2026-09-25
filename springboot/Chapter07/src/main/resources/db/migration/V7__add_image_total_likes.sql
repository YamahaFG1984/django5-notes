-- images/migrations/0002_image_total_likes.py（反范式的冗余计数）
ALTER TABLE images_image ADD COLUMN total_likes INTEGER NOT NULL DEFAULT 0;
CREATE INDEX images_image_total_likes_idx ON images_image (total_likes DESC);

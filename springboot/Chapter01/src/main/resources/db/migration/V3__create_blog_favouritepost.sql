-- blog/migrations/0002_favouritepost.py：复合主键 (user_id, post_id)
CREATE TABLE blog_favouritepost (
    user_id  BIGINT NOT NULL REFERENCES auth_user (id) ON DELETE CASCADE,
    post_id  BIGINT NOT NULL REFERENCES blog_post (id) ON DELETE CASCADE,
    created  TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, post_id)
);

CREATE INDEX blog_favouritepost_post_id_idx ON blog_favouritepost (post_id);

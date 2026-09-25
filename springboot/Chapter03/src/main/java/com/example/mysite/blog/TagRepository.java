package com.example.mysite.blog;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findBySlug(String slug);

    Optional<Tag> findByNameIgnoreCase(String name);

    /** post.tags.add('music', 'jazz') 时 taggit 会自动建不存在的标签，这里手动做同样的事。 */
    default Tag getOrCreate(String name) {
        return findByNameIgnoreCase(name).orElseGet(() -> save(new Tag(name)));
    }
}

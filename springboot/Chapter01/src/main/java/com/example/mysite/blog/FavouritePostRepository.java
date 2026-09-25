package com.example.mysite.blog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FavouritePostRepository extends JpaRepository<FavouritePost, FavouritePostId> {
}

package com.example.bookmarks.images;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 对应 images/signals.py：
 * <pre>
 * &#64;receiver(m2m_changed, sender=Image.users_like.through)
 * def users_like_changed(sender, instance, **kwargs):
 *     instance.total_likes = instance.users_like.count()
 *     instance.save()
 * </pre>
 * &#64;EventListener 默认是同步调用：在发布者的线程、同一个事务里执行，
 * 所以这里修改的实体会和点赞一起提交（或一起回滚）。
 */
@Component
public class ImageLikesListener {

    private final ImageRepository images;

    public ImageLikesListener(ImageRepository images) {
        this.images = images;
    }

    @EventListener
    public void onLikesChanged(ImageLikesChangedEvent event) {
        images.findById(event.imageId())
                .ifPresent(image -> image.setTotalLikes(image.getUsersLike().size()));
    }
}

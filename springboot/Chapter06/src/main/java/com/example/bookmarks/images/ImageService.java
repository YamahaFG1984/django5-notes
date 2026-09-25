package com.example.bookmarks.images;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bookmarks.account.Profile;
import com.example.bookmarks.account.ProfileRepository;
import com.example.bookmarks.account.User;
import com.example.bookmarks.account.UserRepository;
import com.example.bookmarks.common.MediaStorage;
import com.example.bookmarks.common.NotFoundException;
import com.example.bookmarks.common.Slugs;

@Service
@Transactional
public class ImageService {

    /** 详情页右侧“谁赞过”的每一项：用户 + 头像路径 */
    public record Liker(User user, String photo) {
    }

    public record ImageDetail(Image image, List<Liker> likers, boolean likedByMe) {
        public int totalLikes() {
            return likers.size();
        }
    }

    private final ImageRepository images;
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final ImageDownloader downloader;
    private final MediaStorage media;

    public ImageService(ImageRepository images, UserRepository users, ProfileRepository profiles,
                        ImageDownloader downloader, MediaStorage media) {
        this.images = images;
        this.users = users;
        this.profiles = profiles;
        this.downloader = downloader;
        this.media = media;
    }

    /**
     * ImageCreateForm.save() + 视图里的 new_image.user = request.user。
     * 下载 → 校验确实是图片 → 按 images/年/月/日/ 保存 → 写库。
     */
    public Image create(Long userId, ImageCreateForm form) {
        byte[] content = downloader.download(form.getUrl());
        String filename = Slugs.slugify(form.getTitle()) + "." + form.extension();
        String path = media.saveImage("images", filename, content);
        Image image = new Image(users.getReferenceById(userId), form.getTitle(), form.getUrl(), path, form.getDescription());
        return images.save(image);
    }

    /**
     * 详情页：图片 + 点赞用户 + 他们的头像。
     * 模板里写 user.profile.photo 会对每个点赞用户查一次资料表（N+1），这里用一条 IN 查询一次取回。
     */
    @Transactional(readOnly = true)
    public ImageDetail detail(Long id, String slug, Long currentUserId) {
        Image image = images.findWithLikesByIdAndSlug(id, slug)
                .orElseThrow(() -> new NotFoundException("No Image matches the given query."));
        List<User> likedBy = image.getUsersLike().stream().sorted(Comparator.comparing(User::getId)).toList();
        Map<Long, String> photos = profiles.findByUserIdIn(likedBy.stream().map(User::getId).toList()).stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), Profile::getPhoto));
        List<Liker> likers = likedBy.stream().map(u -> new Liker(u, photos.getOrDefault(u.getId(), ""))).toList();
        boolean likedByMe = likedBy.stream().anyMatch(u -> u.getId().equals(currentUserId));
        return new ImageDetail(image, likers, likedByMe);
    }

    /** image.users_like.add(request.user) / .remove(request.user)；返回 false 表示图片不存在 */
    public boolean like(Long imageId, Long userId, boolean like) {
        return images.findById(imageId).map(image -> {
            boolean alreadyLiked = image.getUsersLike().stream().anyMatch(u -> u.getId().equals(userId));
            if (like && !alreadyLiked) {          // User 没有重写 equals，按 id 判断，避免重复插入中间表
                image.getUsersLike().add(users.getReferenceById(userId));
            } else if (!like) {
                image.getUsersLike().removeIf(u -> u.getId().equals(userId));
            }
            return true;
        }).orElse(false);
    }

    @Transactional(readOnly = true)
    public long countCreatedBy(Long userId) {
        return images.countByUserId(userId);
    }
}

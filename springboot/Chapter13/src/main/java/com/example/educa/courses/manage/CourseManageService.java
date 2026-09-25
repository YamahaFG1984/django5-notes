package com.example.educa.courses.manage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.educa.account.User;
import com.example.educa.account.UserRepository;
import com.example.educa.common.MediaStorage;
import com.example.educa.common.NotFoundException;
import com.example.educa.courses.Content;
import com.example.educa.courses.ContentRepository;
import com.example.educa.courses.Course;
import com.example.educa.courses.CourseRepository;
import com.example.educa.courses.FileItem;
import com.example.educa.courses.ImageItem;
import com.example.educa.courses.Item;
import com.example.educa.courses.Module;
import com.example.educa.courses.ModuleRepository;
import com.example.educa.courses.SubjectRepository;
import com.example.educa.courses.TextItem;
import com.example.educa.courses.VideoItem;

/**
 * 讲师管理自己课程的业务逻辑。所有查询都带上 ownerId —— 这就是书中 OwnerMixin 和
 * get_object_or_404(..., owner=request.user) 做的事：别人的课程一律当作“不存在”（404）。
 */
@Service
@Transactional
public class CourseManageService {

    /** ≈ ContentCreateUpdateView.get_model() 里允许的 model_name 白名单 */
    public static final List<String> ITEM_TYPES = List.of("text", "video", "image", "file");

    private final CourseRepository courses;
    private final ModuleRepository modules;
    private final ContentRepository contents;
    private final SubjectRepository subjects;
    private final UserRepository users;
    private final MediaStorage storage;

    public CourseManageService(CourseRepository courses, ModuleRepository modules, ContentRepository contents,
                               SubjectRepository subjects, UserRepository users, MediaStorage storage) {
        this.courses = courses;
        this.modules = modules;
        this.contents = contents;
        this.subjects = subjects;
        this.users = users;
        this.storage = storage;
    }

    // ---------------------------------------------------------------- 课程

    @Transactional(readOnly = true)
    public List<Course> myCourses(Long ownerId) {
        return courses.findByOwnerIdOrderByCreatedDesc(ownerId);
    }

    @Transactional(readOnly = true)
    public Course getCourse(Long id, Long ownerId) {
        return courses.findByIdAndOwnerId(id, ownerId).orElseThrow(NotFoundException::new);
    }

    @Transactional(readOnly = true)
    public boolean slugTaken(String slug, Long exceptCourseId) {
        return exceptCourseId == null ? courses.existsBySlug(slug) : courses.existsBySlugAndIdNot(slug, exceptCourseId);
    }

    /** ≈ OwnerEditMixin.form_valid：form.instance.owner = self.request.user */
    public Course createCourse(Long ownerId, CourseForm form) {
        User owner = users.getReferenceById(ownerId);
        Course course = new Course(owner, subjects.getReferenceById(form.getSubjectId()),
                form.getTitle(), form.getSlug(), form.getOverview());
        return courses.save(course);
    }

    public void updateCourse(Long id, Long ownerId, CourseForm form) {
        Course course = getCourse(id, ownerId);
        course.setSubject(subjects.getReferenceById(form.getSubjectId()));
        course.setTitle(form.getTitle());
        course.setSlug(form.getSlug());
        course.setOverview(form.getOverview());
    }

    /** 级联：课程 → 模块 → 内容 → 条目（orphanRemoval/cascade），≈ on_delete=CASCADE */
    public void deleteCourse(Long id, Long ownerId) {
        courses.delete(getCourse(id, ownerId));
    }

    // ---------------------------------------------------------------- 模块

    /**
     * ≈ formset.save()：逐行处理 —— 勾了删除的删除，有 id 的更新，新行插入。
     * 行里的 id 只在“这门课自己的模块”里查找，伪造别的课程的模块 id 不会生效。
     */
    public void saveModules(Long courseId, Long ownerId, ModuleFormSet formSet) {
        Course course = getCourse(courseId, ownerId);
        Map<Long, Module> existing = course.getModules().stream()
                .collect(Collectors.toMap(Module::getId, Function.identity()));
        for (ModuleFormSet.Row row : formSet.getRows()) {
            if (row.isEmptyExtra()) {
                continue;
            }
            if (row.getId() == null) {
                if (!row.isDelete()) {
                    modules.save(new Module(course, row.getTitle(), row.getDescription()));
                }
                continue;
            }
            Module module = existing.get(row.getId());
            if (module == null) {
                throw new NotFoundException();
            }
            if (row.isDelete()) {
                course.getModules().remove(module);   // orphanRemoval=true → DELETE
            } else {
                module.setTitle(row.getTitle());
                module.setDescription(row.getDescription());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Module> modulesOf(Course course) {
        return modules.findByCourseIdOrderByOrderAsc(course.getId());
    }

    @Transactional(readOnly = true)
    public Module getModule(Long moduleId, Long ownerId) {
        return modules.findByIdAndCourseOwnerId(moduleId, ownerId).orElseThrow(NotFoundException::new);
    }

    /** 内容列表页需要的数据一次查好（open-in-view 关闭，模板里不能再懒加载） */
    @Transactional(readOnly = true)
    public ModulePage modulePage(Long moduleId, Long ownerId) {
        Module module = getModule(moduleId, ownerId);
        Course course = module.getCourse();
        return new ModulePage(course, module,
                modules.findByCourseIdOrderByOrderAsc(course.getId()),
                contents.findByModuleIdOrderByOrderAsc(moduleId));
    }

    public record ModulePage(Course course, Module module, List<Module> modules, List<Content> contents) {
    }

    // ---------------------------------------------------------------- 内容

    /** 编辑已有条目：条目必须挂在这个模块下，模块必须属于当前讲师 */
    @Transactional(readOnly = true)
    public Item getItem(Long moduleId, String modelName, Long itemId, Long ownerId) {
        Item item = contents.findByItemIdAndModuleIdAndModuleCourseOwnerId(itemId, moduleId, ownerId)
                .map(Content::getItem)
                .orElseThrow(NotFoundException::new);
        if (!item.getModelName().equals(modelName)) {
            throw new NotFoundException();
        }
        return item;
    }

    /**
     * 新建或更新一个条目。itemId 为空时新建条目并创建 Content 挂到模块上
     * （≈ ContentCreateUpdateView.post 里的 Content.objects.create(module=..., item=obj)）。
     */
    public Item saveItem(Long moduleId, String modelName, Long itemId, Long ownerId, ItemForm form) {
        Module module = getModule(moduleId, ownerId);
        if (itemId != null) {
            Item item = getItem(moduleId, modelName, itemId, ownerId);
            item.setTitle(form.getTitle());
            switch (item) {
                case TextItem text -> text.setContent(form.getContent());
                case VideoItem video -> video.setUrl(form.getUrl());
                case ImageItem image -> {
                    if (form.hasFile()) {
                        image.setFile(storage.saveImage("images", form.getFile().getOriginalFilename(), bytes(form)));
                    }
                }
                case FileItem file -> {
                    if (form.hasFile()) {
                        file.setFile(storage.saveFile("files", form.getFile().getOriginalFilename(), bytes(form)));
                    }
                }
                default -> throw new IllegalStateException("Unknown item " + item);
            }
            return item;
        }
        User owner = users.getReferenceById(ownerId);
        Item item = switch (modelName) {
            case "text" -> new TextItem(owner, form.getTitle(), form.getContent());
            case "video" -> new VideoItem(owner, form.getTitle(), form.getUrl());
            case "image" -> new ImageItem(owner, form.getTitle(),
                    storage.saveImage("images", form.getFile().getOriginalFilename(), bytes(form)));
            case "file" -> new FileItem(owner, form.getTitle(),
                    storage.saveFile("files", form.getFile().getOriginalFilename(), bytes(form)));
            default -> throw new NotFoundException();
        };
        contents.save(new Content(module, item));   // cascade 一起插入条目
        return item;
    }

    /** 删除内容；条目随 orphanRemoval 一起删除（书中要先 content.item.delete() 再 content.delete()） */
    public Long deleteContent(Long contentId, Long ownerId) {
        Content content = contents.findByIdAndModuleCourseOwnerId(contentId, ownerId)
                .orElseThrow(NotFoundException::new);
        Long moduleId = content.getModule().getId();
        contents.delete(content);
        return moduleId;
    }

    // ---------------------------------------------------------------- 排序

    /** ≈ ModuleOrderView：{模块 id: 新序号}，更新语句里带 owner 条件 */
    public void reorderModules(Map<Long, Integer> order, Long ownerId) {
        order.forEach((id, position) -> modules.updateOrder(id, position, ownerId));
    }

    public void reorderContents(Map<Long, Integer> order, Long ownerId) {
        order.forEach((id, position) -> contents.updateOrder(id, position, ownerId));
    }

    private static byte[] bytes(ItemForm form) {
        try {
            return form.getFile().getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

package com.example.myshop.shop;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.example.myshop.i18n.Languages;

/**
 * 可翻译的分类（≈ class Category(TranslatableModel) + TranslatedFields(name=..., slug=...)）。
 * 主表只剩 id；name、slug 按语言存在 shop_category_translation。
 * getName()/getSlug() 返回<b>当前语言</b>的译文，缺失时回退到英文 —— 和 parler 的 fallback 行为一致。
 */
@Entity
@Table(name = "shop_category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 按语言代码索引的译文。EAGER + SUBSELECT：查询出一批分类后，立刻用一条子查询把它们的译文一起取回，
     * 模板里随时访问 name 都不会触发懒加载异常（open-in-view 是关着的）。
     * 注意：EAGER 配 @BatchSize 在 Hibernate 7 里不会立即初始化，事务结束后再访问仍会抛 LazyInitializationException。
     */
    @OneToMany(mappedBy = "master", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @MapKey(name = "languageCode")
    @Fetch(FetchMode.SUBSELECT)
    private Map<String, CategoryTranslation> translations = new HashMap<>();

    protected Category() {
    }

    public Category(String name, String slug) {
        translate(Languages.DEFAULT, name, slug);
    }

    /** ≈ category.set_current_language('es'); category.name = ...; category.save() */
    public void translate(String language, String name, String slug) {
        CategoryTranslation existing = translations.get(language);
        if (existing == null) {
            translations.put(language, new CategoryTranslation(this, language, name, slug));
        } else {
            existing.update(name, slug);
        }
    }

    private CategoryTranslation current() {
        CategoryTranslation t = translations.get(Languages.current());
        return t != null ? t : translations.get(Languages.DEFAULT);
    }

    public CategoryTranslation translation(String language) {
        return translations.get(language);
    }

    /** reverse('shop:product_list_by_category', args=[self.slug]) —— slug 也是当前语言的 */
    public String getAbsoluteUrl() {
        return "/" + getSlug() + "/";
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        CategoryTranslation t = current();
        return t == null ? "" : t.getName();
    }

    public String getSlug() {
        CategoryTranslation t = current();
        return t == null ? "" : t.getSlug();
    }

    @Override
    public String toString() {
        return getName();
    }
}

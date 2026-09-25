package com.example.myshop.shop.admin;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.myshop.common.MediaStorage;
import com.example.myshop.common.MediaStorage.InvalidImageException;
import com.example.myshop.common.Messages;
import com.example.myshop.common.NotFoundException;
import com.example.myshop.i18n.Languages;
import com.example.myshop.shop.Category;
import com.example.myshop.shop.CategoryRepository;
import com.example.myshop.shop.Product;
import com.example.myshop.shop.ProductRepository;

/**
 * 商店后台（shop/admin.py 的 CategoryAdmin、ProductAdmin）。
 * 重点演示 list_editable：在列表页直接修改价格和上架状态，一次提交保存多行。
 */
@Controller
@RequestMapping("/admin")
public class ShopAdminController {

    /** 分类表单：每种语言一组 name + slug */
    public static class CategoryForm {
        @Valid
        private Map<String, TranslationFields> translations = ProductForm.emptyTranslations();

        public Map<String, TranslationFields> getTranslations() {
            return translations;
        }

        public void setTranslations(Map<String, TranslationFields> translations) {
            this.translations = translations;
        }
    }

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final MediaStorage media;

    public ShopAdminController(CategoryRepository categories, ProductRepository products, MediaStorage media) {
        this.categories = categories;
        this.products = products;
        this.media = media;
    }

    @GetMapping({"", "/"})
    public String index() {
        return "redirect:/admin/shop/product/";
    }

    // ---------- 分类 ----------

    @GetMapping("/shop/category/")
    public String categoryList(Model model) {
        model.addAttribute("categories", categories.findAllOrderedByName());
        model.addAttribute("form", new CategoryForm());
        model.addAttribute("languages", Languages.ALL);
        return "admin/shop/category_list";
    }

    @PostMapping("/shop/category/add/")
    public String categoryAdd(@Valid @ModelAttribute("form") CategoryForm form, BindingResult errors,
                              Model model, RedirectAttributes redirect) {
        validateTranslations(form.getTranslations(), errors);
        form.getTranslations().forEach((language, t) -> {
            if (!t.isBlank() && categories.existsTranslatedSlug(language, t.getSlug())) {
                errors.rejectValue("translations[" + language + "].slug", "unique", "Category with this Slug already exists.");
            }
        });
        if (errors.hasErrors()) {
            model.addAttribute("categories", categories.findAllOrderedByName());
            model.addAttribute("languages", Languages.ALL);
            return "admin/shop/category_list";
        }
        TranslationFields en = form.getTranslations().get(Languages.DEFAULT);
        Category category = new Category(en.getName(), en.getSlug());
        form.getTranslations().forEach((language, t) -> {
            if (!t.isBlank()) {
                category.translate(language, t.getName(), t.getSlug());
            }
        });
        categories.save(category);
        Messages.success(redirect, "The category “" + en.getName() + "” was added successfully.");
        return "redirect:/admin/shop/category/";
    }

    // ---------- 商品 ----------

    /** list_display + list_filter = ['available', ...] + list_editable = ['price', 'available'] */
    @GetMapping("/shop/product/")
    public String productList(@RequestParam(required = false) Boolean available, Model model) {
        List<Product> list = ProductRepository.sortedByName(products.findAllBy()).stream()
                .filter(p -> available == null || p.isAvailable() == available)
                .toList();
        ProductListForm form = new ProductListForm();
        list.forEach(p -> form.getRows().add(new ProductListForm.Row(p.getId(), p.getPrice(), p.isAvailable())));
        model.addAttribute("products", list);
        model.addAttribute("form", form);
        model.addAttribute("available", available);
        return "admin/shop/product_list";
    }

    @PostMapping("/shop/product/")
    @Transactional
    public String productListSave(@Valid @ModelAttribute("form") ProductListForm form, BindingResult errors,
                                  RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            Messages.info(redirect, "Please correct the errors below: prices must be non-negative numbers.");
            return "redirect:/admin/shop/product/";
        }
        Map<Long, Product> byId = products.findAllById(form.getRows().stream().map(ProductListForm.Row::getId).toList())
                .stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        int changed = 0;
        for (ProductListForm.Row row : form.getRows()) {
            Product product = byId.get(row.getId());
            if (product != null && (product.getPrice().compareTo(row.getPrice()) != 0 || product.isAvailable() != row.isAvailable())) {
                product.setPrice(row.getPrice());
                product.setAvailable(row.isAvailable());
                changed++;
            }
        }
        Messages.success(redirect, changed + " product(s) were changed successfully.");
        return "redirect:/admin/shop/product/";
    }

    @GetMapping("/shop/product/add/")
    public String productAddForm(Model model) {
        return renderProductForm(model, new ProductForm(), null);
    }

    @PostMapping("/shop/product/add/")
    @Transactional
    public String productAdd(@Valid @ModelAttribute("form") ProductForm form, BindingResult errors,
                             Model model, RedirectAttributes redirect) {
        validateTranslations(form.getTranslations(), errors);
        if (errors.hasErrors()) {
            return renderProductForm(model, form, null);
        }
        TranslationFields en = form.translation(Languages.DEFAULT);
        Product product = new Product(category(form.getCategoryId()), en.getName(), en.getSlug(), form.getPrice());
        if (!apply(form, product, errors)) {
            return renderProductForm(model, form, null);
        }
        products.save(product);
        Messages.success(redirect, "The product “" + product.getName() + "” was added successfully.");
        return "redirect:/admin/shop/product/";
    }

    @GetMapping("/shop/product/{id}/change/")
    public String productChangeForm(@PathVariable Long id, Model model) {
        Product product = product(id);
        return renderProductForm(model, ProductForm.of(product), product);
    }

    @PostMapping("/shop/product/{id}/change/")
    @Transactional
    public String productChange(@PathVariable Long id, @Valid @ModelAttribute("form") ProductForm form,
                                BindingResult errors, Model model, RedirectAttributes redirect) {
        Product product = product(id);
        validateTranslations(form.getTranslations(), errors);
        if (errors.hasErrors() || !apply(form, product, errors)) {
            return renderProductForm(model, form, product);
        }
        Messages.success(redirect, "The product “" + product.getName() + "” was changed successfully.");
        return "redirect:/admin/shop/product/";
    }

    /** 先处理图片：图片不合法时直接返回 false，不去修改受管理的实体（否则事务提交时会把改了一半的数据写进库）。 */
    private boolean apply(ProductForm form, Product product, BindingResult errors) {
        String newImage = null;
        if (form.hasNewImage()) {
            try {
                newImage = media.saveImage("products", form.getImage().getOriginalFilename(), form.getImage().getBytes());
            } catch (InvalidImageException e) {
                errors.rejectValue("image", "invalid_image", e.getMessage());
                return false;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        product.setCategory(category(form.getCategoryId()));
        form.getTranslations().forEach((language, t) -> {
            if (!t.isBlank()) {
                product.translate(language, t.getName(), t.getSlug(), t.getDescription());
            }
        });
        product.setPrice(form.getPrice());
        product.setAvailable(form.isAvailable());
        if (newImage != null) {
            product.setImage(newImage);
        }
        return true;
    }

    private String renderProductForm(Model model, ProductForm form, Product product) {
        model.addAttribute("form", form);
        model.addAttribute("product", product);
        model.addAttribute("categories", categories.findAllOrderedByName());
        model.addAttribute("languages", Languages.ALL);
        return "admin/shop/product_form";
    }

    /** 默认语言（英文）必填；其他语言要么全空（使用回退），要么 name 和 slug 都填 */
    private static void validateTranslations(Map<String, TranslationFields> translations, BindingResult errors) {
        translations.forEach((language, t) -> {
            boolean required = Languages.DEFAULT.equals(language) || !t.isBlank();
            if (required && t.getName().isBlank()) {
                errors.rejectValue("translations[" + language + "].name", "required", "This field is required.");
            }
            if (required && t.getSlug().isBlank()) {
                errors.rejectValue("translations[" + language + "].slug", "required", "This field is required.");
            }
        });
    }

    private Category category(Long id) {
        return categories.findById(id).orElseThrow(() -> new NotFoundException("Category " + id + " not found"));
    }

    private Product product(Long id) {
        return products.findById(id).orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
    }
}

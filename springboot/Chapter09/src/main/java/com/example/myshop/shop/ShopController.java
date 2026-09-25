package com.example.myshop.shop;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.myshop.cart.CartAddProductForm;
import com.example.myshop.common.NotFoundException;

/**
 * shop/views.py + shop/urls.py。商店挂在网站根路径下：/、/{category}/、/{id}/{slug}/。
 * Django 的 urls.py 里商店的 include 必须放在最后，否则 '<slug:category_slug>/' 会“吞掉” cart/、orders/；
 * Spring 按“最具体优先”匹配，/cart/ 这样的字面量路径总是优先于 /{categorySlug}/。
 */
@Controller
public class ShopController {

    private final CategoryRepository categories;
    private final ProductRepository products;

    public ShopController(CategoryRepository categories, ProductRepository products) {
        this.categories = categories;
        this.products = products;
    }

    @GetMapping("/")
    public String productList(Model model) {
        model.addAttribute("category", null);
        model.addAttribute("categories", categories.findAllByOrderByNameAsc());
        model.addAttribute("products", products.findByAvailableTrueOrderByNameAsc());
        return "shop/product/list";
    }

    @GetMapping("/{categorySlug}/")
    public String productListByCategory(@PathVariable String categorySlug, Model model) {
        Category category = categories.findBySlug(categorySlug)
                .orElseThrow(() -> new NotFoundException("No Category matches the given query."));
        model.addAttribute("category", category);
        model.addAttribute("categories", categories.findAllByOrderByNameAsc());
        model.addAttribute("products", products.findByAvailableTrueAndCategoryOrderByNameAsc(category));
        return "shop/product/list";
    }

    @GetMapping("/{id:\\d+}/{slug}/")
    public String productDetail(@PathVariable Long id, @PathVariable String slug, Model model) {
        Product product = products.findByIdAndSlugAndAvailableTrue(id, slug)
                .orElseThrow(() -> new NotFoundException("No Product matches the given query."));
        model.addAttribute("product", product);
        model.addAttribute("cartProductForm", new CartAddProductForm());
        return "shop/product/detail";
    }
}

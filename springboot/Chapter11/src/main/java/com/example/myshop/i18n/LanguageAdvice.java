package com.example.myshop.i18n;

import java.util.List;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 语言选择器需要的数据，≈ 模板里的 {% get_current_language %}、{% get_available_languages %}、
 * {% get_language_info_list %}。
 */
@ControllerAdvice
public class LanguageAdvice {

    @ModelAttribute("languages")
    public List<Languages.Language> languages() {
        return Languages.ALL;
    }

    @ModelAttribute("currentLanguage")
    public String currentLanguage() {
        return Languages.current();
    }
}

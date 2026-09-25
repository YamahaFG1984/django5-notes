package com.example.bookmarks.images;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * 生成拖到书签栏的 javascript: 链接（≈ templates/bookmarklet_launcher.js）。
 * 书中把站点地址 //127.0.0.1:8000 写死在 JS 里；这里用当前请求的地址生成，换域名、换端口都不用改代码。
 */
@Component
public class BookmarkletLauncher {

    public String href() {
        String base = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
        String scriptUrl = base.replaceFirst("^https?:", "") + "/static/js/bookmarklet.js";
        return "javascript:(function(){"
                + "if(!window.bookmarklet){"
                + "var s=document.body.appendChild(document.createElement('script'));"
                + "s.src='" + scriptUrl + "?r='+Math.floor(Math.random()*9999999999999999);"
                + "window.bookmarklet=true;"
                + "}else{bookmarkletLaunch();}"
                + "})();";
    }
}

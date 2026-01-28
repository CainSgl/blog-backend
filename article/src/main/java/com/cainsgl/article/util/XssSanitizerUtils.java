package com.cainsgl.article.util;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

public class XssSanitizerUtils {

    // 预定义策略 - 适用于富文本编辑器内容
    private static final PolicyFactory RICH_TEXT_POLICY = new HtmlPolicyBuilder()
            // 允许常用文本标签
            .allowElements("p", "div", "span", "br", "hr")
            // 允许标题标签
            .allowElements("h1", "h2", "h3", "h4", "h5", "h6")
            // 允许列表
            .allowElements("ul", "ol", "li")
            // 允许文本格式化
            .allowElements("b", "i", "u", "strong", "em", "strike", "del", "ins", "sub", "sup")
            // 允许引用和代码
            .allowElements("blockquote", "code", "pre")
            // 允许表格
            .allowElements("table", "thead", "tbody", "tr", "th", "td")
            // 允许链接（带安全属性）
            .allowElements("a")
            .allowAttributes("href").onElements("a")
            .allowAttributes("title").onElements("a")
            .requireRelNofollowOnLinks()  // 自动添加 rel="nofollow"
            // 允许图片（带安全属性）
            .allowElements("img")
            .allowAttributes("src", "alt", "title", "width", "height").onElements("img")
            // 允许常用样式属性
            .allowAttributes("class").globally()
            .allowAttributes("id").globally()
            .allowStyling()  // 允许 style 属性（会过滤危险样式）
            .toFactory();

    // 简单策略 - 只保留基本格式
    private static final PolicyFactory SIMPLE_POLICY = new HtmlPolicyBuilder()
            .allowElements("p", "br", "b", "i", "u", "strong", "em")
            .toFactory();

    /**
     * 使用富文本策略清理 HTML
     */
    public static String sanitize(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        return RICH_TEXT_POLICY.sanitize(html);
    }

    /**
     * 使用简单策略清理 HTML
     */
    public static String sanitizeSimple(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        return SIMPLE_POLICY.sanitize(html);
    }

    /**
     * 高级清理（保持向后兼容）
     */
    public static String sanitizeAdvanced(String html) {
        return sanitize(html);
    }
}

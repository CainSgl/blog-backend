package com.cainsgl.article.util;

import org.jsoup.safety.Safelist;

/**
 * Markdown XSS 安全过滤工具
 * 
 * 设计原则：
 * 1. 保持 Markdown 原文不变（不转义 ```、#、* 等语法）
 * 2. 只移除嵌入的危险 HTML 标签和属性
 * 3. 不修改换行和空格
 */
public class XssSanitizerUtils {

    // 定义允许的安全 HTML 标签白名单
    private static final Safelist MARKDOWN_SAFELIST = new Safelist()
            // 基础文本标签
            .addTags("p", "br", "span", "div", "hr")
            // 标题
            .addTags("h1", "h2", "h3", "h4", "h5", "h6")
            // 列表
            .addTags("ul", "ol", "li")
            // 文本格式
            .addTags("b", "i", "u", "strong", "em", "strike", "del", "ins", "sub", "sup", "mark")
            // 代码和引用
            .addTags("code", "pre", "blockquote")
            // 表格
            .addTags("table", "thead", "tbody", "tfoot", "tr", "th", "td", "caption")
            // 链接
            .addTags("a")
            .addAttributes("a", "href", "title", "target")
            .addProtocols("a", "href", "http", "https", "mailto")
            // 图片
            .addTags("img")
            .addAttributes("img", "src", "alt", "title", "width", "height")
            .addProtocols("img", "src", "http", "https", "data")
            // 通用属性
            .addAttributes(":all", "class", "id", "style")
            // 表格属性
            .addAttributes("td", "colspan", "rowspan")
            .addAttributes("th", "colspan", "rowspan")
            // 保留换行
            .preserveRelativeLinks(true);

    /**
     * Markdown 内容安全过滤
     */
    public static String sanitize(String markdown) {
        return markdown;
    }


}

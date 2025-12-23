package com.cainsgl.article.util

import org.owasp.html.HtmlPolicyBuilder
import org.owasp.html.PolicyFactory

/**
 * XSS防护工具类
 * 使用OWASP Java HTML Sanitizer进行HTML内容清理
 */
object XssSanitizerUtils {
    
    /**
     * HTML清理策略工厂
     * 允许安全的HTML标签和属性，移除潜在的XSS风险
     */
    private val POLICY_FACTORY: PolicyFactory = HtmlPolicyBuilder()
        // 允许的HTML标签
        .allowElements(
            "a", "b", "br", "blockquote", "caption", "cite", "code", "col",
            "colgroup", "dd", "del", "div", "dl", "dt", "em", "h1", "h2",
            "h3", "h4", "h5", "h6", "hr", "i", "img", "kbd", "li", "ol",
            "p", "pre", "q", "samp", "small", "span", "strike", "strong",
            "sub", "sup", "table", "tbody", "td", "tfoot", "th", "thead",
            "tr", "u", "ul"
        )
        // 允许的链接协议
        .allowUrlProtocols("http", "https", "mailto")
        // 允许的a标签属性
        .allowAttributes("href", "title").onElements("a")
        // 允许的img标签属性
        .allowAttributes("src", "alt", "title", "width", "height").onElements("img")
        // 允许的td/th标签属性
        .allowAttributes("colspan", "rowspan").onElements("td", "th")
        // 允许的col标签属性
        .allowAttributes("span", "width").onElements("col")
        // 允许的table标签属性
        .allowAttributes("summary").onElements("table")
        // 允许的blockquote标签属性
        .allowAttributes("cite").onElements("blockquote")
        // 允许的q标签属性
        .allowAttributes("cite").onElements("q")
        // 允许的div/span标签属性
        .allowAttributes("class").onElements("div", "span")
        // 构建策略
        .toFactory()

    /**
     * 清理HTML内容，移除潜在的XSS攻击代码
     * @param htmlContent 包含HTML的内容
     * @return 清理后的安全HTML内容
     */
    fun sanitize(htmlContent: String): String {
        return POLICY_FACTORY.sanitize(htmlContent)
    }
}
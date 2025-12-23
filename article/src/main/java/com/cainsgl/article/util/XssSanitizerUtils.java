package com.cainsgl.article.util;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;


public class XssSanitizerUtils {

    private final static Safelist safelist = new Safelist()
            .addTags(
                    "p", "br", "strong", "em", "b", "i", "u", "small", "mark",
                    "sub", "sup", "span", "div", "h1", "h2", "h3", "h4", "h5", "h6",
                    "ol", "ul", "li", "dl", "dt", "dd", "blockquote", "cite", "q",
                    "pre", "code", "samp", "kbd", "var", "time", "data", "dfn",
                    "abbr", "ins", "del", "s", "strike", "font", "center"
            )
            .addAttributes("span", "class", "style", "id", "title")
            .addAttributes("div", "class", "style", "id", "title")
            .addAttributes("p", "class", "style", "id", "title")
            .addAttributes("a", "href", "title", "rel", "target", "class", "id")
            .addAttributes("img", "src", "alt", "title", "width", "height", "class", "id", "style")
            .addProtocols("a", "href", "http", "https", "#")
            .addProtocols("img", "src", "http", "https", "data")
            .addAttributes("blockquote", "cite")
            .addAttributes("q", "cite")
            .addAttributes("time", "datetime")
            .preserveRelativeLinks(true);
    private static final Document.OutputSettings outputSettings = new Document.OutputSettings();
    static {
        outputSettings.prettyPrint(false);
    }
    public static  String sanitize( String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        return Jsoup.clean(html, safelist);
    }
}

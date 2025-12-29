package com.cainsgl.article.util;
public class XssSanitizerUtils {

    public static String sanitize(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }

        // First pass: remove dangerous tags and their content
        String result = removeDangerousTags(html);

        // Second pass: remove dangerous attributes
        result = removeDangerousAttributes(result);

        // Third pass: remove dangerous protocols in attributes
        result = removeDangerousProtocols(result);

        // Fourth pass: clean up any remaining dangerous patterns
        result = removeRemainingDangerousPatterns(result);

        return result;
    }

    private static String removeDangerousTags(String html) {
        // Remove script, style, iframe, object, embed, etc. tags and their content
        String result = html.replaceAll("(?i)<script[^>]*>.*?</script>", "");
        result = result.replaceAll("(?i)<style[^>]*>.*?</style>", "");
        result = result.replaceAll("(?i)<iframe[^>]*>.*?</iframe>", "");
        result = result.replaceAll("(?i)<object[^>]*>.*?</object>", "");
        result = result.replaceAll("(?i)<embed[^>]*>.*?</embed>", "");
        result = result.replaceAll("(?i)<form[^>]*>.*?</form>", "");
        result = result.replaceAll("(?i)<meta[^>]*>.*?</meta>", "");
        result = result.replaceAll("(?i)<link[^>]*>.*?</link>", "");

        return result;
    }

    private static String removeDangerousAttributes(String html) {
        // Remove all event handler attributes (on*, etc.)
        return html.replaceAll("(?i)\\s+on[a-z]+\\s*=\\s*([\"']).*?\\1", "");
    }

    private static String removeDangerousProtocols(String html) {
        // Remove javascript:, data:, vbscript: protocols
        String result = html.replaceAll("(?i)(href|src|action)\\s*=\\s*([\"']?)javascript:", "$1=$2#");
        result = result.replaceAll("(?i)(href|src|action)\\s*=\\s*([\"']?)data:", "$1=$2#");
        result = result.replaceAll("(?i)(href|src|action)\\s*=\\s*([\"']?)vbscript:", "$1=$2#");
        result = result.replaceAll("(?i)(href|src|action)\\s*=\\s*([\"']?)file:", "$1=$2#");

        return result;
    }

    private static String removeRemainingDangerousPatterns(String html) {
        // Remove any remaining dangerous patterns
        String result = html.replaceAll("(?i)expression\\s*\\(", "ex-pression(");
        result = result.replaceAll("(?i)javascript\\s*:", "javascript-removed:");
        result = result.replaceAll("(?i)vbscript\\s*:", "vbscript-removed:");

        // Handle multi-line attributes properly
        result = result.replaceAll("(?i)(\\s+on[a-z]+\\s*=)", "");

        return result;
    }

    // Additional method to handle malformed HTML
    public static String sanitizeAdvanced(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }

        StringBuilder result = new StringBuilder();
        int length = html.length();
        int i = 0;

        while (i < length) {
            char c = html.charAt(i);

            if (c == '<') {
                // Look for tag start
                int tagEnd = findTagEnd(html, i, length);
                if (tagEnd != -1) {
                    String tag = html.substring(i, tagEnd + 1);

                    // Check if this is a dangerous tag
                    if (isDangerousTag(tag.toLowerCase())) {
                        // Skip this tag completely
                        i = tagEnd + 1;
                        continue;
                    } else {
                        // Sanitize attributes in the tag
                        String sanitizedTag = sanitizeTagAttributes(tag);
                        result.append(sanitizedTag);
                        i = tagEnd + 1;
                    }
                } else {
                    result.append(c);
                    i++;
                }
            } else {
                result.append(c);
                i++;
            }
        }

        return result.toString();
    }

    private static int findTagEnd(String html, int start, int length) {
        int i = start + 1; // Skip the '<'

        // Handle comments
        if (start + 3 < length && html.substring(start, Math.min(start + 4, length)).equalsIgnoreCase("<!--")) {
            int commentEnd = html.indexOf("-->", start + 4);
            return commentEnd != -1 ? commentEnd + 2 : length - 1;
        }

        // Handle CDATA
        if (start + 8 < length && html.substring(start, Math.min(start + 9, length)).equalsIgnoreCase("<![CDATA[")) {
            int cdataEnd = html.indexOf("]]>", start + 9);
            return cdataEnd != -1 ? cdataEnd + 2 : length - 1;
        }

        boolean inQuotes = false;
        char quoteChar = 0;

        for (; i < length; i++) {
            char c = html.charAt(i);

            if (c == '"' || c == '\'') {
                if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (c == quoteChar) {
                    inQuotes = false;
                }
            } else if (c == '>' && !inQuotes) {
                return i;
            }
        }

        return -1; // No matching end found
    }

    private static boolean isDangerousTag(String tag) {
        String lowerTag = tag.toLowerCase().trim();
        return lowerTag.startsWith("<script") ||
                lowerTag.startsWith("</script") ||
                lowerTag.startsWith("<style") ||
                lowerTag.startsWith("</style") ||
                lowerTag.startsWith("<iframe") ||
                lowerTag.startsWith("</iframe") ||
                lowerTag.startsWith("<object") ||
                lowerTag.startsWith("</object") ||
                lowerTag.startsWith("<embed") ||
                lowerTag.startsWith("</embed") ||
                lowerTag.startsWith("<form") ||
                lowerTag.startsWith("</form") ||
                lowerTag.startsWith("<meta") ||
                lowerTag.startsWith("</meta") ||
                lowerTag.startsWith("<link") ||
                lowerTag.startsWith("</link");
    }

    private static String sanitizeTagAttributes(String tag) {
        // Remove all event handler attributes
        String result = tag.replaceAll("(?i)\\s+on[a-z]+\\s*=\\s*([\"']).*?\\1", "");

        // Remove dangerous protocols in attributes
        result = result.replaceAll("(?i)(href|src|action|background|codebase)\\s*=\\s*([\"']?)javascript:", "$1=$2#");
        result = result.replaceAll("(?i)(href|src|action|background|codebase)\\s*=\\s*([\"']?)data:", "$1=$2#");
        result = result.replaceAll("(?i)(href|src|action|background|codebase)\\s*=\\s*([\"']?)vbscript:", "$1=$2#");

        return result;
    }
}
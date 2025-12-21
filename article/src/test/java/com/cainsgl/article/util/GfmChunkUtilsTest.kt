package com.cainsgl.article.util

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GfmChunkUtilsTest {

    @Test
    fun `test removeHtmlTags`() {
        // 测试基本HTML标签去除
        val input1 = "<p>123</p>"
        val expected1 = "123"
        assertEquals(expected1, GfmChunkUtils.removeHtmlTags(input1))

        // 测试嵌套HTML标签去除
        val input2 = "<div><p>嵌套标签</p></div>"
        val expected2 = "嵌套标签"
        assertEquals(expected2, GfmChunkUtils.removeHtmlTags(input2))

        // 测试混合Markdown和HTML
        val input3 = "# 标题\n<p>段落内容</p>\n**粗体**"
        val expected3 = "# 标题\n段落内容\n**粗体**"
        assertEquals(expected3, GfmChunkUtils.removeHtmlTags(input3))

        // 测试自闭合标签
        val input4 = "<img src=\"image.jpg\" alt=\"图片\" />"
        val expected4 = ""
        assertEquals(expected4, GfmChunkUtils.removeHtmlTags(input4))

        // 测试带属性的标签
        val input5 = "<div class=\"container\">内容</div>"
        val expected5 = "内容"
        assertEquals(expected5, GfmChunkUtils.removeHtmlTags(input5))
    }

    @Test
    fun `test chunk with html tags`() {
        // 测试包含HTML标签的Markdown内容分块
        val markdownWithHtml = """
            # 标题
            
            <p>这是第一段内容。</p>
            
            <div>这是第二段内容，包含<strong>粗体</strong>文本。</div>
            
            ## 子标题
            
            <ul>
                <li>列表项1</li>
                <li>列表项2</li>
            </ul>
        """.trimIndent()

        val chunks = GfmChunkUtils.chunk(markdownWithHtml)
        
        // 验证返回结果不为空
        assertTrue(chunks.isNotEmpty())
        
        // 验证HTML标签已被去除
        chunks.forEach { chunk ->
            assertTrue(!chunk.contains("<") && !chunk.contains(">"))
        }
    }
}
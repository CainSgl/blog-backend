package com.cainsgl.article.util


import org.junit.Test

class GfmChunkUtilsTest {



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
            <script>console.log('xss test')</script>
            #<img src=x onerror=console.log('dom xss')>
        """.trimIndent()
        println(XssSanitizerUtils.sanitize(markdownWithHtml))


    }
}
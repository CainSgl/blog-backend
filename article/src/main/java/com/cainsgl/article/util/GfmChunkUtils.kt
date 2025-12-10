package com.cainsgl.article.util

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.TextCollectingVisitor
import com.vladsch.flexmark.util.data.MutableDataSet

/**
 * GFM切分工具
 * 规则：GFM → 纯文本 → 基础块(500-2000字符) → 子块(3-4句话)
 */
object GfmChunkUtils
{

    private const val BASE_CHUNK_MIN = 500
    private const val BASE_CHUNK_MAX = 2000
    private const val SENTENCES_PER_CHUNK = 3

    private val parser: Parser = Parser.builder(MutableDataSet().apply {
        set(
            Parser.EXTENSIONS, listOf(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                TaskListExtension.create()
            )
        )
    }).build()

    /**
     * 切分GFM内容，返回子块文本列表（用于向量化）
     */
    fun chunk(gfmContent: String): List<String>
    {
        if (gfmContent.isBlank()) return emptyList()

        val document = parser.parse(gfmContent)
        val plainTexts = mutableListOf<String>()

        // 从AST提取各节点纯文本
        var child = document.firstChild
        while (child != null)
        {
            val cleanText = TextCollectingVisitor().collectAndGetText(child).trim()
            if (cleanText.isNotEmpty()) plainTexts.add(cleanText)
            child = child.next
        }

        // 合并为基础块(500-2000字符)
        val baseChunks = mergeToBaseChunks(plainTexts)

        // 拆分为子块(100-200字符)
        return baseChunks.flatMap { splitToSubChunks(it) }
    }

    /**
     * 合并语义单元为基础块
     * - 单元过大(>2000)则强制拆分
     * - 累积达到500-2000区间时输出一个基础块
     */
    private fun mergeToBaseChunks(texts: List<String>): List<String>
    {
        val result = mutableListOf<String>()
        val buffer = StringBuilder()

        for (text in texts)
        {
            //单个语义单元超过最大值，强制拆分
            if (text.length > BASE_CHUNK_MAX)
            {
                if (buffer.isNotEmpty())
                {
                    result.add(buffer.toString()); buffer.clear()
                }
                result.addAll(splitBySize(text, BASE_CHUNK_MAX))
                continue
            }
            //合并后超过最大值，先输出当前缓冲
            if (buffer.length + text.length > BASE_CHUNK_MAX)
            {
                result.add(buffer.toString()); buffer.clear()
            }
            if (buffer.isNotEmpty()) buffer.append(" ")
            buffer.append(text)
            //达到最小值，可以输出一个基础块
            if (buffer.length >= BASE_CHUNK_MIN)
            {
                result.add(buffer.toString()); buffer.clear()
            }
        }
        //吐出剩余内容
        if (buffer.isNotEmpty()) result.add(buffer.toString())
        return result
    }

    /**
     * 基础块按句子拆分为子块（每个子块2-3句话）
     */
    private fun splitToSubChunks(text: String): List<String>
    {
        val sentences = text.split(Regex("(?<=[。！？.!?])")).map { it.trim() }.filter { it.isNotEmpty() }
        if (sentences.isEmpty()) return if (text.isNotBlank()) listOf(text) else emptyList()

        val chunks = mutableListOf<String>()
        var i = 0
        while (i < sentences.size)
        {
            val end = minOf(i + SENTENCES_PER_CHUNK, sentences.size)
            chunks.add(sentences.subList(i, end).joinToString(""))
            i = end
        }
        return chunks
    }

    /** 按大小拆分，优先在标点处切断 */
    private fun splitBySize(text: String, maxSize: Int): List<String>
    {
        val result = mutableListOf<String>()
        var i = 0
        //优先在这些标点处切断
        val punctuation = "，,、：:；;。.!！?？"
            
        while (i < text.length)
        {
            if (i + maxSize >= text.length)
            {
                //剩余部分不超过maxSize，直接取完
                result.add(text.substring(i))
                break
            }
            //在maxSize范围内找最后一个标点符号
            val end = i + maxSize
            var cutPos = -1
            for (j in end - 1 downTo i)
            {
                if (text[j] in punctuation)
                {
                    cutPos = j + 1  //包含标点
                    break
                }
            }
            if (cutPos > i)
            {
                //找到标点，在标点后切断
                result.add(text.substring(i, cutPos))
                i = cutPos
            }
            else
            {
                //找不到标点，强制切断
                result.add(text.substring(i, end))
                i = end
            }
        }
        return result
    }
}

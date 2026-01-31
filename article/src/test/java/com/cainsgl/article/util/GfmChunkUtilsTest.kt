package com.cainsgl.article.util


import org.junit.Test
import kotlin.experimental.and

class GfmChunkUtilsTest {
    fun hasBitFromByte(byteValue: UByte, bitPosition: Int): Boolean {
        val mask = 1 shl bitPosition
        return (byteValue.toInt() and mask) != 0
    }
    @Test
    fun `tt`()
    {
        val mask=1 shl 7;
        val a:UByte=(-128).toUByte();
        val intA:Int = a.toInt()
        val ans= mask and a.toInt()
        println(mask.toFullBinaryString())
        println(a.toFullBinaryString())
        println(ans!=0)
        println(hasBitFromByte(a,7-0))

    }
    fun UByte.toFullBinaryString(): String {
        return String.format("%8s", (this and 0xFF.toUByte()).toString(2)).replace(' ', '0')
    }
    fun Byte.toFullBinaryString(): String {
        return String.format("%8s", (this and 0xFF.toByte()).toString(2)).replace(' ', '0')
    }

    fun Int.toFullBinaryString(): String {
        return String.format("%32s", this.toString(2)).replace(' ', '0')
    }

    @Test
    fun `test chunk with html tags`() {
        // 测试包含HTML标签的Markdown内容分块
        val markdownWithHtml = """
           # Markdown目录树测试文档
## 文档介绍
### 测试目的
验证不同Markdown编辑器/工具生成目录树的准确性，重点测试**多级标题层级识别**、**嵌套结构处理**、**特殊字符标题兼容性**三大核心场景。
### 测试范围
1. 一级到四级标题的层级映射效果
2. 含特殊符号（如`@`、`-`、`_`）的标题处理
3. 跨模块标题的目录树分组逻辑
## 基础语法测试模块
### 标题层级测试
#### 一级标题示例（#）
此处为一级标题的内容测试区，用于验证目录树对最高层级的识别。
#### 二级标题示例（##）
此处为二级标题的内容测试区，重点观察目录树的缩进效果。
#### 三级标题示例（###）
此处为三级标题的内容测试区，测试工具对深层级的支持程度。
#### 四级标题示例（####）
此处为四级标题的内容测试区，验证目录树是否支持四级及以下层级。
### 列表语法测试
#### 无序列表测试
- 无序列表项1
- 无序列表项2
  - 嵌套无序列表项
#### 有序列表测试
1. 有序列表项1
2. 有序列表项2
   1. 嵌套有序列表项
#### 嵌套列表与标题混合测试
## 特殊字符标题测试
### 标题含@符号：用户@测试
此标题包含@符号，验证目录树对特殊符号的识别能力。
### 标题含-符号：层级-嵌套-测试
此标题包含连字符，验证目录树解析时是否会误判层级。
### 标题含_符号：下划线_样式_测试
此标题包含下划线，验证目录树是否受下划线语法影响。
## 跨模块嵌套测试
### 模块A
#### 模块A-子项1
##### 模块A-子项1-子级1
###### 模块A-子项1-子级1-深层1
### 模块B
#### 模块B-子项1
##### 模块B-子项1-子级1
### 模块C
#### 模块C-子项1
#### 模块C-子项2
## 空内容标题测试
### 空内容标题1（仅标题无内容）
### 空内容标题2（仅标题无内容）
## 代码块测试
### 单行代码测试
`val test = "single line code"`
### 多行代码测试
```kotlin
@RestController
@RequestMapping("/api/test")
class TestController {
    @GetMapping
    fun getTest(): String {
        return "directory tree test"
    }
}
<!-- 基础脚本标签攻击 -->
<script>alert('XSS')</script>

<!-- 图像标签事件处理器 -->
<img src="x" onerror="alert('XSS')">

<!-- 事件处理器属性 -->
<div onclick="alert('XSS')">Click me</div>

<!-- JavaScript协议 -->
<a href="javascript:alert('XSS')">Click here</a>

<!-- Data协议 -->
<img src="data:text/html,<script>alert('XSS')</script>">

<!-- 嵌套和编码攻击 -->
<ScRiPt>alert('XSS')</ScRiPt>

<!-- 内联样式攻击 -->
<style>body{background:url("javascript:alert('XSS')")}</style>

<!-- iframe嵌入 -->
<iframe src="javascript:alert('XSS')"></iframe>

<!-- 对象和嵌入标签 -->
<object data="javascript:alert('XSS')"></object>
<embed src="javascript:alert('XSS')">

<!-- 事件处理器变体 -->
<div onload="alert('XSS')">Content</div>
<input type="text" onfocus="alert('XSS')">
<button onmouseover="alert('XSS')">Hover</button>

<!-- HTML5新标签 -->
<video src="x" onerror="alert('XSS')">
<audio src="x" onerror="alert('XSS')">

<!-- 混合攻击 -->
<img src="x" onerror="eval('alert(\"XSS\")')">
<img src="x" onerror="window.location='http://evil.com'">

<!-- 隐蔽的事件处理器 -->
<div ondblclick="alert('XSS')">Double click</div>
<span ontouchstart="alert('XSS')">Touch here</span>

<!-- 带有编码的攻击 -->
<img src="x" onerror="&#97;lert('XSS')">
<img src="x" onerror="alert&#40;'XSS'&#41;">

<!-- 嵌套标签攻击 -->
<svg><script>alert('XSS')</script></svg>

<!-- CSS表达式攻击 -->
<style>#xss{background:url(javascript:alert('XSS'))}</style>

<!-- 评论绕过 -->
<!--><script>alert('XSS')</script>

<!-- Markdown相关的XSS -->
[Click me](javascript:alert('XSS'))
![Image](javascript:alert('XSS'))
<span onclick="alert('XSS')">Safe span</span>

<!-- 复杂的混合攻击 -->
<div><img src="x" onerror="alert('XSS')"><p>Content</p></div>
<script>document.write('<img src="x" onerror="alert(\'XSS\')">')</script>

<!-- 空格和换行绕过 -->
<img src="x" 
onerror="alert('XSS')">

<!-- 多个攻击向量 -->
<script>alert('XSS')</script><img src="x" onerror="alert('XSS')"><div onclick="alert('XSS')">Content</div>
        """.trimIndent()
        println(XssSanitizerUtils.sanitize(markdownWithHtml))


    }
}
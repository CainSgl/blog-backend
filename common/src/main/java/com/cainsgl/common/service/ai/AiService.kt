package com.cainsgl.common.service.ai

interface AiService {
    fun getEmbedding(text: String): FloatArray
    fun getEmbedding(texts: List<String>):List<FloatArray>
}

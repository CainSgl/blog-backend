package com.cainsgl.api.ai

interface AiService {
    fun getEmbedding(text: String): FloatArray
    fun getEmbedding(texts: List<String>):List<FloatArray>
}

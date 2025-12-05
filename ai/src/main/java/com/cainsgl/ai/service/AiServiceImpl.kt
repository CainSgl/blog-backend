package com.cainsgl.ai.service

import com.cainsgl.common.service.ai.AiService
import jakarta.annotation.Resource
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.stereotype.Service

@Service
class AiServiceImpl : AiService {
    @Resource
    lateinit var embeddingModel: EmbeddingModel
    override fun getEmbedding(text: String): FloatArray
    {
       return embeddingModel.embed(text)
    }

    override fun getEmbedding(texts: List<String>): List<FloatArray>
    {
        return embeddingModel.embed(texts)
    }
}

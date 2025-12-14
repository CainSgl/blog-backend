package com.cainsgl.ai.service


import com.cainsgl.api.ai.AiService
import com.cainsgl.common.util.VectorUtils
import jakarta.annotation.Resource
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AiServiceImpl : AiService
{
    @Value("\${dimensions}")
    var dimensions: Int=0
    @Resource
    lateinit var embeddingModel: EmbeddingModel
    override fun getEmbedding(text: String): FloatArray
    {
        //降低到对应的维度，并且归一化
        val embed = embeddingModel.embed(text)
        return VectorUtils.reduceDimension(embed, dimensions)
    }

    override fun getEmbedding(texts: List<String>): List<FloatArray>
    {
        val embeds = embeddingModel.embed(texts)
        return VectorUtils.reduceDimensionBatch(embeds, dimensions)
    }
}

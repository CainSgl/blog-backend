package com.cainsgl.common.util

import kotlin.math.sqrt

object VectorUtils
{

    /**
     * L2归一化：将向量中每个元素除以该向量的L2范数，使归一化后的向量长度为1
     * @param vector 原始向量
     * @return 归一化后的向量
     */
    fun l2Normalize(vector: FloatArray): FloatArray {
        var sumSquares = 0f
        for (v in vector) {
            sumSquares += v * v
        }
        if (sumSquares == 0f) {
            return vector.copyOf()
        }
        val norm = sqrt(sumSquares)
        val result = FloatArray(vector.size)
        for (i in vector.indices) {
            result[i] = vector[i] / norm
        }
        return result
    }

    /**
     * 向量降维：截取前targetDim个维度，然后进行L2归一化
     * @param vector 原始向量
     * @param targetDim 目标维度
     * @return 降维并归一化后的向量
     */
    fun reduceDimension(vector: FloatArray, targetDim: Int): FloatArray {
        require(targetDim > 0) { "目标维度必须大于0" }
        require(targetDim <= vector.size) { "目标维度不能超过原始向量维度" }
        // 截取前targetDim个维度
        val truncated = vector.copyOf(targetDim)
        // L2归一化
        return l2Normalize(truncated)
    }

    /**
     * 批量向量降维：对多个向量进行降维和L2归一化
     * @param vectors 原始向量列表
     * @param targetDim 目标维度
     * @return 降维并归一化后的向量列表
     */
    fun reduceDimensionBatch(vectors: List<FloatArray>, targetDim: Int): List<FloatArray> {
        return vectors.map { reduceDimension(it, targetDim) }
    }
}
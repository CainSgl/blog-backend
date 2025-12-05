package com.cainsgl.common.entity.article

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.cainsgl.common.handler.VectorTypeHandler

@TableName(value = "post_chunk_vectors", autoResultMap = true)
data class PostChunkVectorEntity(
    @TableId(type = IdType.ASSIGN_ID)
    var id: Long? = null,

    @TableField("post_id")
    var postId: Long? = null,

    @TableField(value = "vector", typeHandler = VectorTypeHandler::class)
    var vector: FloatArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PostChunkVectorEntity

        if (id != other.id) return false
        if (postId != other.postId) return false
        if (vector != null) {
            if (other.vector == null) return false
            if (!vector.contentEquals(other.vector)) return false
        } else if (other.vector != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (postId?.hashCode() ?: 0)
        result = 31 * result + (vector?.contentHashCode() ?: 0)
        return result
    }
}

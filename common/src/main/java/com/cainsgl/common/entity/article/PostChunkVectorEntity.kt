package com.cainsgl.common.entity.article

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.cainsgl.common.handler.VectorTypeHandler
import org.apache.commons.codec.digest.DigestUtils

@TableName(value = "post_chunk_vector", autoResultMap = true)
data class PostChunkVectorEntity(
    @TableId(type = IdType.ASSIGN_ID)
    var id: Long? = null,

    @TableField("post_id")
    var postId: Long? = null,

    @TableField(value = "vector", typeHandler = VectorTypeHandler::class)
    var vector: FloatArray? = null,
    @TableField("hash")
    var hash: String? = null,

    @TableField("chunk")
    var chunk: String? = null,
)
{


    override fun equals(other: Any?): Boolean
    {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PostChunkVectorEntity

        if (id != other.id) return false
        if (postId != other.postId) return false
        if (vector != null)
        {
            if (other.vector == null) return false
            if (!vector.contentEquals(other.vector)) return false
        } else if (other.vector != null) return false
        if (hash != other.hash) return false
        if (chunk != other.chunk) return false

        return true
    }

    override fun hashCode(): Int
    {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (postId?.hashCode() ?: 0)
        result = 31 * result + (vector?.contentHashCode() ?: 0)
        result = 31 * result + (hash?.hashCode() ?: 0)
        result = 31 * result + (chunk?.hashCode() ?: 0)
        return result
    }

    fun calculateHash(): PostChunkVectorEntity {
        if(this.hash.isNullOrEmpty())
        {
            this.hash= DigestUtils.sha256Hex(chunk!!)
        }
        return this
    }
}

package com.cainsgl.common.entity.user

import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.cainsgl.common.handler.VectorTypeHandler

//为什么会专门建额外的一张用户信息表，这是为了冷热数据分离，这里的数据都是会频繁更新的，我们会缓存在redis，然后定时同步到数据库
@TableName(value = "user_extra_infos", autoResultMap = true)
data class UserExtraInfoEntity(
    @TableId
    var userId: Long? = null,

    @TableField("follower_count")
    var followerCount: Int = 0,

    @TableField("following_count")
    var followingCount: Int = 0,

    @TableField("like_count")
    var likeCount: Int = 0,

    @TableField("comment_count")
    var commentCount: Int = 0,

    @TableField("post_count")
    var postCount: Int = 0,
    @TableField("article_view_count")
    var articleViewCount: Int = 0,

    @TableField(value = "interest_vector", typeHandler = VectorTypeHandler::class)
    var interestVector: FloatArray? = null

)
{
    override fun equals(other: Any?): Boolean
    {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserExtraInfoEntity

        if (userId != other.userId) return false
        if (followerCount != other.followerCount) return false
        if (followingCount != other.followingCount) return false
        if (likeCount != other.likeCount) return false
        if (commentCount != other.commentCount) return false
        if (postCount != other.postCount) return false
        if (articleViewCount != other.articleViewCount) return false
        if (interestVector != null)
        {
            if (other.interestVector == null) return false
            if (!interestVector.contentEquals(other.interestVector)) return false
        } else if (other.interestVector != null) return false

        return true
    }

    override fun hashCode(): Int
    {
        var result = userId?.hashCode() ?: 0
        result = 31 * result + followerCount
        result = 31 * result + followingCount
        result = 31 * result + likeCount
        result = 31 * result + commentCount
        result = 31 * result + postCount
        result = 31 * result + articleViewCount
        result = 31 * result + (interestVector?.contentHashCode() ?: 0)
        return result
    }

}

package com.cainsgl.common.entity.user

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.cainsgl.common.handler.VectorTypeHandler
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer

//为什么会专门建额外的一张用户信息表，这是为了冷热数据分离，这里的数据都是会频繁更新的，我们会缓存在redis，然后定时同步到数据库
@TableName(value = "user_extra_info", autoResultMap = true)
open class UserExtraInfoEntity(
    @TableId(value = "user_id", type = IdType.INPUT)
    @field:JsonSerialize(using = ToStringSerializer::class)
    @JsonIgnore
    var userId: Long? = null,

    @TableField("follower_count")
    var followerCount: Int? = null,

    @TableField("following_count")
    var followingCount: Int? = null,

    @TableField("like_count")
    var likeCount: Int? = null,

    @TableField("comment_count")
    var commentCount: Int? = null,

    @TableField("post_count")
    var postCount: Int? = null,
    @TableField("article_view_count")
    var articleViewCount: Int? = null,
    @TableField("msg_count")
    var msgCount: Int? = null,
    @TableField("msg_reply_count")
    var msgReplyCount: Int? = null,
    @TableField("msg_like_count")
    var msgLikeCount: Int? = null,
    @TableField("msg_report_count")
    var msgReportCount: Int? = null,
    @TableField("msg_message_count")
    var msgMessageCount: Int? = null,
    @TableField(value = "interest_vector", typeHandler = VectorTypeHandler::class, select = false)
    var interestVector: FloatArray? = null
)
{
    companion object
    {
        const val USER_EXTRA_INFO_REDIS_PREFIX = "user:extraInfo:"
    }


}

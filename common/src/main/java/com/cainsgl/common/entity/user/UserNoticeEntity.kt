package com.cainsgl.common.entity.user

import com.baomidou.mybatisplus.annotation.*
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer

@TableName("user_notice")
data class UserNoticeEntity(
    @TableId(type = IdType.ASSIGN_ID)
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,

    @TableField("type")
    var type: Short? = null,

    @TableField("target_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var targetId: Long? = null,

    @TableField("user_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var userId: Long? = null,

    @TableField("target_user")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var targetUser: Long? = null,

    @TableField("checked")
    var checked: Boolean? = null,
)


enum class UserNoticeType(@JsonValue val str:String, @EnumValue val type: Int)
{
    //TODO
    REPLY("回复",0),
    LIKE_POST("点赞文章",1),
    LIKE_COMMENT("点赞评论",2),
    REPORT("举报",3),
    MSG("消息",4),
    UNKNOW("未知",5);
    @JsonValue
    fun toJSONValue(): Any {
        return str
    }
    companion object {
        private val VALUE_MAP = UserNoticeType.entries.associateBy { it.type }
        private val OPERATE_MAP = UserNoticeType.entries.associateBy { it.str }
        fun getByValue(value: Int?): UserNoticeType {
            if(value==null) return UNKNOW
            return VALUE_MAP[value]?:UNKNOW
        }
        fun getByOperate(operate: String): UserNoticeType {
            return OPERATE_MAP[operate]?:UNKNOW
        }
    }
}

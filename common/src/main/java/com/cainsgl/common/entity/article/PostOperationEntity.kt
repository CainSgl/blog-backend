package com.cainsgl.common.entity.article

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer

@TableName("post_operations")
data class PostOperationEntity(
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,
    @TableField("user_id")
    @JsonSerialize(using = ToStringSerializer::class)
    var userId: Long? = null,
    @TableField("post_id")
    @JsonSerialize(using = ToStringSerializer::class)
    var postId: Long? = null,
    @TableField("operate_type")
    var operateType: Short? = null,
){

    fun toVO():PostOperationVO
    {
      return  PostOperationVO(this.id,this.userId,this.postId,OperateType.getByValue(operateType))
    }
}
data class PostOperationVO(
    @JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,
    @JsonSerialize(using = ToStringSerializer::class)
    var userId: Long? = null,
    @JsonSerialize(using = ToStringSerializer::class)
    var postId: Long? = null,
    var operateType: OperateType,
)
enum class OperateType(val value:Short, private val operate:String) {
    LIKE_TYPE(1,"点赞"),
    HATE_TYPE(2,"讨厌"),
    STAR(3,"收藏"),
    UNKNOWN(-1,"未知");
    @JsonValue
    fun toJSONValue(): Any {
        return operate
    }
    companion object {
        private val VALUE_MAP = entries.associateBy { it.value }
        private val OPERATE_MAP = entries.associateBy { it.operate }
        fun getByValue(value: Short?): OperateType {
           if(value==null) return UNKNOWN
            return VALUE_MAP[value]?:UNKNOWN
        }
        fun getByOperate(operate: String): OperateType {
            return OPERATE_MAP[operate]?:UNKNOWN
        }
    }
}
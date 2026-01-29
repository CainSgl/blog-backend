package com.cainsgl.common.entity.article

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer

@TableName("post_operation")
data class PostOperationEntity(
    @TableId(type = IdType.ASSIGN_ID)
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,
    @TableField("user_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var userId: Long? = null,
    @TableField("target_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var targetId: Long? = null,
    @TableField("operate_type")
    var operateType: Short? = null,
){

    fun toVO():PostOperationVO
    {
      return  PostOperationVO(this.id,this.userId,this.targetId,OperateType.getByValue(operateType))
    }
}
data class PostOperationVO(
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,
    @field:JsonSerialize(using = ToStringSerializer::class)
    var userId: Long? = null,
    @field:JsonSerialize(using = ToStringSerializer::class)
    var postId: Long? = null,
    var operateType: OperateType,
)
enum class OperateType(val value:Short, private val operate:String) {
    LIKE_TYPE(1,"点赞"),
    STAR(3,"收藏文章"),
    UNKNOWN(-1,"未知"),
    //这是为了复用之前的表设计，后续可能会移除
    STAR_KB(4,"收藏知识库");
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
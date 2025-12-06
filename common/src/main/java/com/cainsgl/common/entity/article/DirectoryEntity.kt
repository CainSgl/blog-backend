package com.cainsgl.common.entity.article

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName

@TableName("directories")
data class DirectoryEntity(
    @TableId(type = IdType.ASSIGN_ID)
    var id: Long? = null,

    @TableField("kb_id")
    var kbId: Long? = null,

    @TableField("parent_id")
    var parentId: Long? = null,

    @TableField("name")
    var name: String = "",

    @TableField("post_id")
    var postId: Long? = null,

    @TableField("sort_num")
    var sortNum: Short = 0
)

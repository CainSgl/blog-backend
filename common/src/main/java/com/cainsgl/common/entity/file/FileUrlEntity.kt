package com.cainsgl.common.entity.file

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.cainsgl.common.handler.ByteaTypeHandler
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.time.LocalDate

@TableName("file_url")
data class FileUrlEntity(
    @TableId(type = IdType.ASSIGN_ID)
    @field:JsonSerialize(using = ToStringSerializer::class)
    var shortUrl: Long? = null,

    @TableField("user_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var userId: Long? = null,

    /**
     * 存储 SHA256 hash (64 字符 hex -> 32 字节 bytea)
     */
    @TableField(value = "url", typeHandler = ByteaTypeHandler::class)
    var url: String? = null,
    
    @TableField("name")
    var name: String? = null,
    
    @TableField("file_size")
    var fileSize: Int? = null,
    
    @TableField("created_at")
    var createdAt: LocalDate? = null,
    
    /**
     * 文件状态：0-待验证，1-可用，2-验证失败，3-已删除
     */
    @TableField("status")
    var status: Int? = 0,
) {
    companion object {
        val BASIC_COL = listOf("short_url", "name", "file_size", "created_at", "status")
    }
    
    /**
     * 获取文件状态枚举
     */
    fun getFileStatus(): FileStatus = FileStatus.fromCodeOrDefault(status ?: 0)
    
    /**
     * 设置文件状态
     */
    fun setFileStatus(fileStatus: FileStatus) {
        this.status = fileStatus.code
    }
}
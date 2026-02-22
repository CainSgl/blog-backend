package com.cainsgl.common.entity.file


enum class FileStatus(val code: Int, val description: String) {

    PENDING(0, "待验证"),
    AVAILABLE(1, "可用"),
    FAILED(2, "验证失败"),
    DELETED(3, "已删除");
    
    companion object {
        private val map = entries.associateBy(FileStatus::code)
        fun fromCode(code: Int): FileStatus? = map[code]
        fun fromCodeOrDefault(code: Int, default: FileStatus = PENDING): FileStatus = 
            map[code] ?: default
    }
}

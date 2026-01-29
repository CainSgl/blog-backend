package com.cainsgl.common.handler

import org.apache.ibatis.type.BaseTypeHandler
import org.apache.ibatis.type.JdbcType
import org.apache.ibatis.type.MappedJdbcTypes
import org.apache.ibatis.type.MappedTypes
import java.sql.CallableStatement
import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * 处理 SHA256 hex 字符串和 PostgreSQL bytea 之间的转换
 */
@MappedTypes(String::class)
@MappedJdbcTypes(JdbcType.BINARY)
class ByteaTypeHandler : BaseTypeHandler<String>() {

    override fun setNonNullParameter(ps: PreparedStatement, i: Int, parameter: String, jdbcType: JdbcType?) {
        // 将 hex 字符串转换为字节数组
        val bytes = hexStringToByteArray(parameter)
        ps.setBytes(i, bytes)
    }

    override fun getNullableResult(rs: ResultSet, columnName: String): String? {
        val bytes = rs.getBytes(columnName)
        return bytes?.let { byteArrayToHexString(it) }
    }

    override fun getNullableResult(rs: ResultSet, columnIndex: Int): String? {
        val bytes = rs.getBytes(columnIndex)
        return bytes?.let { byteArrayToHexString(it) }
    }

    override fun getNullableResult(cs: CallableStatement, columnIndex: Int): String? {
        val bytes = cs.getBytes(columnIndex)
        return bytes?.let { byteArrayToHexString(it) }
    }

    /**
     * 将 hex 字符串转换为字节数组
     */
    private fun hexStringToByteArray(hex: String): ByteArray {
        val len = hex.length
        require(len % 2 == 0) { "Hex string must have even length, got: $len" }
        
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            val high = Character.digit(hex[i], 16)
            val low = Character.digit(hex[i + 1], 16)
            
            require(high != -1 && low != -1) { 
                "Invalid hex character at position $i in string: $hex" 
            }
            
            data[i / 2] = ((high shl 4) + low).toByte()
            i += 2
        }
        return data
    }

    /**
     * 将字节数组转换为 hex 字符串
     */
    private fun byteArrayToHexString(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = HEX_ARRAY[v ushr 4]
            hexChars[i * 2 + 1] = HEX_ARRAY[v and 0x0F]
        }
        return String(hexChars)
    }

    companion object {
        private val HEX_ARRAY = "0123456789abcdef".toCharArray()
    }
}

package com.cainsgl.common.handler

import org.apache.ibatis.type.BaseTypeHandler
import org.apache.ibatis.type.JdbcType
import org.apache.ibatis.type.MappedJdbcTypes
import org.apache.ibatis.type.MappedTypes
import org.postgresql.util.PGobject
import java.sql.CallableStatement
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.BitSet

/**
 * PostgreSQL bit(n) 类型的 TypeHandler
 * 用于处理二进制量化向量存储
 * 将 BitSet 映射到 PostgreSQL 的 bit 类型
 */
@MappedTypes(BitSet::class)
@MappedJdbcTypes(JdbcType.OTHER)
class BitTypeHandler : BaseTypeHandler<BitSet>() {

    override fun setNonNullParameter(
        ps: PreparedStatement,
        i: Int,
        parameter: BitSet,
        jdbcType: JdbcType?
    ) {
        val pgObject = PGobject()
        // 使用 varbit 类型，更灵活
        pgObject.type = "varbit"
        pgObject.value = bitSetToBitString(parameter)
        ps.setObject(i, pgObject)
    }

    override fun getNullableResult(rs: ResultSet, columnName: String): BitSet? {
        return convertToBitSet(rs.getString(columnName))
    }

    override fun getNullableResult(rs: ResultSet, columnIndex: Int): BitSet? {
        return convertToBitSet(rs.getString(columnIndex))
    }

    override fun getNullableResult(cs: CallableStatement, columnIndex: Int): BitSet? {
        return convertToBitSet(cs.getString(columnIndex))
    }

    /**
     * 将 BitSet 转换为 bit 字符串（如 "10101100..."）
     */
    private fun bitSetToBitString(bitSet: BitSet): String {
        val sb = StringBuilder(BIT_LENGTH)
        for (i in 0 until BIT_LENGTH) {
            sb.append(if (bitSet.get(i)) '1' else '0')
        }
        return sb.toString()
    }

    /**
     * 将 bit 字符串转换为 BitSet
     */
    private fun convertToBitSet(bitString: String?): BitSet? {
        if (bitString.isNullOrEmpty()) {
            return null
        }
        return try {
            val bitSet = BitSet(bitString.length)
            for (i in bitString.indices) {
                if (bitString[i] == '1') {
                    bitSet.set(i)
                }
            }
            bitSet
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val BIT_LENGTH = 4096
    }
}

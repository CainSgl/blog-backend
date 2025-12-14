package com.cainsgl.common.handler

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.ibatis.type.BaseTypeHandler
import org.apache.ibatis.type.JdbcType
import org.apache.ibatis.type.MappedJdbcTypes
import org.apache.ibatis.type.MappedTypes
import java.sql.CallableStatement
import java.sql.PreparedStatement
import java.sql.ResultSet

private val logger=KotlinLogging.logger{}
@MappedTypes(FloatArray::class)
@MappedJdbcTypes(JdbcType.OTHER)
class VectorTypeHandler : BaseTypeHandler<FloatArray>() {

    override fun setNonNullParameter(ps: PreparedStatement, i: Int, parameter: FloatArray, jdbcType: JdbcType?) {
        // 将 FloatArray 转换为 PostgreSQL vector 类型
        val vectorString = "[${parameter.joinToString(",")}]"
        val pgObject = org.postgresql.util.PGobject()
        pgObject.type = "vector"
        pgObject.value = vectorString
        ps.setObject(i, pgObject)
    }

    override fun getNullableResult(rs: ResultSet, columnName: String): FloatArray? {
        return convertToFloatArray(rs.getObject(columnName))
    }

    override fun getNullableResult(rs: ResultSet, columnIndex: Int): FloatArray? {
        return convertToFloatArray(rs.getObject(columnIndex))
    }

    override fun getNullableResult(cs: CallableStatement, columnIndex: Int): FloatArray? {
        return convertToFloatArray(cs.getObject(columnIndex))
    }

    private fun convertToFloatArray(pgObject: Any?): FloatArray? {
        if (pgObject == null) {
            logger.debug { "vector is null object" }
            return null
        }

        return try {
            // PostgreSQL vector 返回的是 PGobject，其 value 为字符串形式 [1.0,2.0,3.0]
            val vectorString = pgObject.toString()
            if (vectorString.startsWith("[") && vectorString.endsWith("]")) {
                val content = vectorString.substring(1, vectorString.length - 1)
                if (content.isEmpty()) {
                    return FloatArray(0)
                }
                val items = content.split(",")
                FloatArray(items.size) { i -> items[i].trim().toFloat() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

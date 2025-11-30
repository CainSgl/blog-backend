package com.cainsgl.common.handler

import org.apache.ibatis.type.BaseTypeHandler
import org.apache.ibatis.type.JdbcType
import org.apache.ibatis.type.MappedJdbcTypes
import org.apache.ibatis.type.MappedTypes
import java.sql.CallableStatement
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException


@MappedTypes(List::class)
@MappedJdbcTypes(JdbcType.OTHER)
class StringListTypeHandler : BaseTypeHandler<List<String>>()
{

    override fun setNonNullParameter(
        ps: PreparedStatement,
        i: Int,
        parameter: List<String>,
        jdbcType: JdbcType
    )
    {
        // 将 List<String> 转换为 PostgreSQL 数组
        val conn = ps.connection
        val array = parameter.toTypedArray()
        val pgArray = conn.createArrayOf("text", array)
        ps.setArray(i, pgArray)
    }

    override fun getNullableResult(rs: ResultSet, columnName: String): List<String>?
    {
        return convertToArrayList(rs.getArray(columnName))
    }

    override fun getNullableResult(rs: ResultSet, columnIndex: Int): List<String>?
    {
        return convertToArrayList(rs.getArray(columnIndex))
    }

    override fun getNullableResult(cs: CallableStatement, columnIndex: Int): List<String>?
    {
        return convertToArrayList(cs.getArray(columnIndex))
    }

    private fun convertToArrayList(pgArray: java.sql.Array?): List<String>
    {
        if (pgArray == null)
        {
            return ArrayList()
        }

        return try
        {
            // 直接获取数组并转换为 List
            val stringArray = pgArray.array as Array<*>
            val result = ArrayList<String>()
            for (item in stringArray)
            {
                result.add(item.toString())
            }
            result
        } catch (e: SQLException)
        {
            // 备用方案：处理字符串格式 {item1,item2}
            val arrayString = pgArray.toString()
            if (arrayString.startsWith("{") && arrayString.endsWith("}"))
            {
                val content = arrayString.substring(1, arrayString.length - 1)
                if (content.isEmpty())
                {
                    return ArrayList()
                }
                val items = content.split(",").toTypedArray()
                val result = ArrayList<String>()
                for (item in items)
                {
                    result.add(item.trim())
                }
                result
            } else
            {
                ArrayList()
            }
        }
    }
}

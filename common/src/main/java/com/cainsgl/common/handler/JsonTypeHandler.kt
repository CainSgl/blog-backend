package com.cainsgl.common.handler

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.TypeReference
import org.apache.ibatis.type.BaseTypeHandler
import org.apache.ibatis.type.JdbcType
import org.apache.ibatis.type.MappedJdbcTypes
import org.apache.ibatis.type.MappedTypes
import org.postgresql.util.PGobject
import java.sql.CallableStatement
import java.sql.PreparedStatement
import java.sql.ResultSet

@MappedTypes(Map::class)
@MappedJdbcTypes(JdbcType.OTHER)
class JsonTypeHandler : BaseTypeHandler<Map<String, Any>>()
{

    override fun setNonNullParameter(
        ps: PreparedStatement,
        i: Int,
        parameter: Map<String, Any>,
        jdbcType: JdbcType?
    )
    {
        val pgObject = PGobject()
        pgObject.type = "jsonb"
        pgObject.value = JSON.toJSONString(parameter)
        ps.setObject(i, pgObject)
    }

    override fun getNullableResult(rs: ResultSet, columnName: String): Map<String, Any>?
    {
        return parseJson(rs.getString(columnName))
    }

    override fun getNullableResult(rs: ResultSet, columnIndex: Int): Map<String, Any>?
    {
        return parseJson(rs.getString(columnIndex))
    }

    override fun getNullableResult(cs: CallableStatement, columnIndex: Int): Map<String, Any>?
    {
        return parseJson(cs.getString(columnIndex))
    }

    private fun parseJson(json: String?): Map<String, Any>?
    {
        if (json.isNullOrEmpty())
        {
            return null
        }
        return JSON.parseObject(json, object : TypeReference<Map<String, Any>>() {})
    }
}

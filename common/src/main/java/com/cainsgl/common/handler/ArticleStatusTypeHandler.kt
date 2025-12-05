package com.cainsgl.common.handler

import com.cainsgl.common.entity.article.ArticleStatus
import org.apache.ibatis.type.BaseTypeHandler
import org.apache.ibatis.type.JdbcType
import org.apache.ibatis.type.MappedJdbcTypes
import org.apache.ibatis.type.MappedTypes
import java.sql.PreparedStatement
import java.sql.ResultSet

@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(ArticleStatus::class)
class ArticleStatusTypeHandler : BaseTypeHandler<ArticleStatus>() {
    override fun setNonNullParameter(ps: PreparedStatement, i: Int, parameter: ArticleStatus, jdbcType: JdbcType) {
        ps.setString(i, parameter.dbValue)
    }

    override fun getNullableResult(rs: ResultSet, columnName: String): ArticleStatus? =
        rs.getString(columnName)?.let { ArticleStatus.fromDbValue(it) }

    override fun getNullableResult(rs: ResultSet, columnIndex: Int): ArticleStatus? =
        rs.getString(columnIndex)?.let { ArticleStatus.fromDbValue(it) }

    override fun getNullableResult(cs: java.sql.CallableStatement, columnIndex: Int): ArticleStatus? =
        cs.getString(columnIndex)?.let { ArticleStatus.fromDbValue(it) }
}
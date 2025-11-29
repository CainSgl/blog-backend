package com.cainsgl.common.handler;

import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
@MappedTypes(List.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class StringListTypeHandler extends BaseTypeHandler<List<String>>
{

    public StringListTypeHandler() {
        // 无参构造函数
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        // 将 List<String> 转换为 PostgreSQL 数组
        Connection conn = ps.getConnection();
        String[] array = parameter.toArray(new String[0]);
        Array pgArray = conn.createArrayOf("text", array);
        ps.setArray(i, pgArray);
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return convertToArrayList(rs.getArray(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return convertToArrayList(rs.getArray(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return convertToArrayList(cs.getArray(columnIndex));
    }

    private List<String> convertToArrayList(Array pgArray) {
        if (pgArray == null) {
            return new ArrayList<>();
        }

        try {
            // 直接获取数组并转换为 List
            String[] stringArray = (String[]) pgArray.getArray();
            List<String> result = new ArrayList<>();
            for (String item : stringArray) {
                result.add(item);
            }
            return result;
        } catch (SQLException e) {
            // 备用方案：处理字符串格式 {item1,item2}
            String arrayString = pgArray.toString();
            if (arrayString.startsWith("{") && arrayString.endsWith("}")) {
                String content = arrayString.substring(1, arrayString.length() - 1);
                if (content.isEmpty()) {
                    return new ArrayList<>();
                }
                String[] items = content.split(",");
                List<String> result = new ArrayList<>();
                for (String item : items) {
                    result.add(item.trim());
                }
                return result;
            }
            return new ArrayList<>();
        }
    }
}
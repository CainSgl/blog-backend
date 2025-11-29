package com.cainsgl.user.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cainsgl.common.entity.user.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity>
{
    /**
     * 根据用户名或邮箱查询用户
     */
    UserEntity selectByUsernameOrEmail(@Param("account") String account);

    /**
     * 获取用户的extra字段
     */
    String selectExtraById(@Param("id") long id);
}

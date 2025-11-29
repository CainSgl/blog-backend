package com.cainsgl.common.service.user;

import com.cainsgl.common.entity.user.UserEntity;

import java.util.Map;

public interface UserService
{
    UserEntity getUser(long id);
    boolean updateById(UserEntity userEntity);
    /**
     * 根据用户名或邮箱获取用户
     */
    UserEntity getUserByAccount(String account);
    Map getExtra(long id);
}

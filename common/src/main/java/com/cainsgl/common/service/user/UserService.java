package com.cainsgl.common.service.user;

import com.cainsgl.common.entity.user.UserEntity;

import java.util.Map;

public interface UserService
{
    UserEntity getUser(long id);
    boolean updateById(UserEntity userEntity);

}

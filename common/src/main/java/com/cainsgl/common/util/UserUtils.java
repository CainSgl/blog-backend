package com.cainsgl.common.util;

import cn.dev33.satoken.stp.StpUtil;
import com.cainsgl.common.entity.user.UserEntity;

public class UserUtils
{
    private static final String USER_INFO_KEY = "userInfo";

    public static UserEntity getUserInfo(){
        Object o = StpUtil.getSession().get(USER_INFO_KEY);
        if(o!=null)
        {
            return (UserEntity)o;
        }
        return null;
    }
    public static void setUserInfo(UserEntity userEntity){
        StpUtil.getSession().set(USER_INFO_KEY,userEntity);
    }
}

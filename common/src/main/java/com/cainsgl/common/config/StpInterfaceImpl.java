package com.cainsgl.common.config;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.cainsgl.common.entity.user.UserEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * SaToken 权限接口实现
 * 从 Session 中获取用户的角色和权限
 */
@Component
public class StpInterfaceImpl implements StpInterface
{
    private static final String USER_INFO_KEY = "userInfo";
    
    @Override
    public List<String> getPermissionList(Object loginId, String loginType)
    {
        UserEntity user = (UserEntity) StpUtil.getSession().get(USER_INFO_KEY);
        if (user != null && user.getPermissions() != null) {
            return user.getPermissions();
        }
        return new ArrayList<>();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType)
    {
        UserEntity user = (UserEntity) StpUtil.getSession().get(USER_INFO_KEY);
        if (user != null && user.getRoles() != null) {
            return user.getRoles();
        }
        return new ArrayList<>();
    }
}

package com.cainsgl.user.service;


import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cainsgl.common.entity.user.UserEntity;
import com.cainsgl.common.service.user.UserService;
import com.cainsgl.common.util.UserUtils;
import com.cainsgl.user.repository.UserMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService, IService<UserEntity>
{
    @Override
    public UserEntity getUser(long id)
    {
        return baseMapper.selectById(id);
    }

    @Override
    public boolean updateById(UserEntity entity)
    {

        boolean b = super.updateById(entity);
        UserUtils.setUserInfo( entity);
        return b;
    }

    @Override
    public UserEntity getUserByAccount(String account)
    {
        return baseMapper.selectByUsernameOrEmail(account);
    }
    @Override
    public Map getExtra(long id)
    {
        String s = baseMapper.selectExtraById(id);
        return JSON.parseObject(s, Map.class);
    }
}

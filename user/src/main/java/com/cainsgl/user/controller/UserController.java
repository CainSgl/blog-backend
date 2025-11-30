package com.cainsgl.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.cainsgl.common.dto.request.LoginRequest;
import com.cainsgl.common.dto.response.LoginResponse;
import com.cainsgl.common.entity.user.UserEntity;
import com.cainsgl.common.exception.BSystemException;
import com.cainsgl.common.exception.BusinessException;
import com.cainsgl.common.service.user.UserService;
import com.cainsgl.common.util.UserUtils;
import com.cainsgl.user.service.UserServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController
{
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Resource
    private UserServiceImpl userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Object login(@RequestBody LoginRequest loginRequest)
    {
        if (!loginRequest.validate()) {
            throw new BSystemException("参数不全");
        }
        // 查询用户并验证
        UserEntity user = userService.getUserByAccount(loginRequest.getAccount());
        if (user == null) {
            throw new BusinessException("用户不存在或密码错误");
        }
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("用户不存在或密码错误");
        }
        // 检查用户状态
        if (!user.isActive()) {
            UserEntity.Extra extra = userService.getExtra(user.getId());
            if (extra != null) {
                LocalDateTime banedTime = extra.getBanedTime();
                throw new BusinessException("账户已被封禁至 "+banedTime);
            }
            throw new BusinessException("账户已被禁用");
        }
        String device = LoginRequest.getDeviceType();
        //注销所有旧 Token
        List<String> oldTokenList = StpUtil.getTokenValueListByLoginId(user.getId(), device);
        for (String oldToken : oldTokenList) {
            StpUtil.logoutByTokenValue(oldToken);
        }
        StpUtil.login(user.getId(), device);
        String token = StpUtil.getTokenValue();
        UserUtils.setUserInfo(user);
        log.info("用户登录成功: userId={}, device={}", user.getId(), device);
        return new LoginResponse(token, user);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Object logout()
    {
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            StpUtil.logout();
            log.info("用户登出成功: userId={}", userId);
        }
        return "登出成功";
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/current")
    public Object getCurrentUser()
    {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("未登录");
        }
        return UserUtils.getUserInfo();
    }

    @GetMapping
    public Object get()
    {
        UserEntity user = userService.getUser(1994765496125227008L);
        return user;
    }

}

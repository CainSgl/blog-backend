package com.cainsgl.common.dto.response;

import com.cainsgl.common.entity.user.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    
    /**
     * 登录token
     */
    private String token;
    
    /**
     * 用户信息
     */
    private UserEntity userInfo;
}

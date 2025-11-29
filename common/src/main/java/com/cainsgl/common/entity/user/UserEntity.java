package com.cainsgl.common.entity.user;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.FastjsonTypeHandler;
import com.cainsgl.common.handler.StringListTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.type.ArrayTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("users")
public class UserEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("username")
    private String username;

    @TableField("email")
    private String email;

    @TableField("password_hash")
    private String passwordHash;

    @TableField("nickname")
    private String nickname;

    @TableField("avatar_url")
    private String avatarUrl;

    @TableField("bio")
    private String bio;

    @TableField("level")
    private Integer level;

    @TableField("experience")
    private Integer experience;

    @TableField(value = "roles", typeHandler = StringListTypeHandler.class)
    private List<String> roles;

    @TableField(value = "permissions", typeHandler = StringListTypeHandler.class)
    private List<String> permissions;

    @TableField("status")
    private String status;

    @TableField("email_verified")
    private Boolean emailVerified;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
    //到下一级的总经验值
    @TableField(exist = false)
    private Integer nextLevelTotalExp;
    //从现在到下一级需要的经验值
    @TableField(exist = false)
    private Integer expToNextLevel;
    //计算对应的成员变量
    public void calculateLevelInfo() {
        if (this.level != null && this.experience != null) {
            this.nextLevelTotalExp = (int) (Math.pow(2, this.level + 1) + this.level);
            this.expToNextLevel = Math.max(0, this.nextLevelTotalExp - this.experience);
        }
    }
    //去除敏感字段
    public void removeSensitiveFields() {
        this.passwordHash = null;
         this.emailVerified = null;
         this.status = null;
    }
}
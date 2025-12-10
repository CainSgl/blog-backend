package com.cainsgl.common.config

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler
import org.apache.ibatis.reflection.MetaObject
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.OffsetDateTime

/**
 * MyBatis-Plus 配置类
 */
@Configuration
class MyBatisPlusConfig
{

    /**
     * 自动填充处理器
     * 支持以下字段类型的自动填充：
     * - OffsetDateTime: createdAt, updatedAt
     * - LocalDateTime: 任何 LocalDateTime 类型的字段
     */
    @Bean
    fun metaObjectHandler(): MetaObjectHandler
    {
        return object : MetaObjectHandler
        {
            override fun insertFill(metaObject: MetaObject)
            {
                this.strictInsertFill(metaObject, "createdAt", OffsetDateTime::class.java, OffsetDateTime.now())
                this.strictInsertFill(metaObject, "updatedAt", OffsetDateTime::class.java, OffsetDateTime.now())
            }
            override fun updateFill(metaObject: MetaObject)
            {
                this.setFieldValByName("updatedAt", OffsetDateTime.now(), metaObject)
            }
        }
    }
}

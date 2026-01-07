package com.cainsgl.common.config

import com.baomidou.mybatisplus.annotation.DbType
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor
import org.apache.ibatis.reflection.MetaObject
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDateTime

@Configuration
class MyBatisPlusConfig
{
    /**
     * 自动填充处理器
     * 支持以下字段类型的自动填充：
     * - LocalDateTime: createdAt, updatedAt
     */
    @Bean
    fun metaObjectHandler(): MetaObjectHandler
    {
        return object : MetaObjectHandler
        {
            override fun insertFill(metaObject: MetaObject)
            {
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime::class.java, LocalDateTime.now())
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime::class.java, LocalDateTime.now())
            }
            override fun updateFill(metaObject: MetaObject)
            {
                this.setFieldValByName("updatedAt", LocalDateTime.now(), metaObject)
            }
        }
    }

    /**
     * MyBatis-Plus分页插件配置
     */
    @Bean
    fun mybatisPlusInterceptor(): MybatisPlusInterceptor {
        val interceptor = MybatisPlusInterceptor()
        interceptor.addInnerInterceptor(PaginationInnerInterceptor(DbType.POSTGRE_SQL))
        return interceptor
    }
}

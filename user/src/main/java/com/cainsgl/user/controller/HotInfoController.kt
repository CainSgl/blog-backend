package com.cainsgl.user.controller

import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.user.UserExtraInfoEntity
import com.cainsgl.common.util.user.UserHotInfoUtils.Companion.USER_HOT_INFO_COUNT
import com.cainsgl.user.service.UserExtraInfoServiceImpl
import jakarta.annotation.Resource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/user/hotInfo")
class HotInfoController
{
    @Resource
    lateinit var userExtraInfoServiceImpl: UserExtraInfoServiceImpl

    @Resource
    lateinit var redisTemplate: RedisTemplate<String, String>

    @GetMapping
    fun getHotInfo(@RequestParam id: Long?): Any?
    {
        val userId = id ?: StpUtil.getLoginIdAsLong()
        if (userId < 0)
        {
            return ResultCode.RESOURCE_NOT_FOUND
        }
        return userExtraInfoServiceImpl.getBySaveOnNull(userId)
    }
    companion object{
        val ackTypeList = listOf("msgReplyCount","msgLikeCount","msgReportCount","msgMessageCount")
        val ackTypeListToDBField=mapOf(
            "msgReplyCount" to "msg_reply_count",
            "msgLikeCount" to "msg_like_count",
            "msgReportCount" to "msg_report_count",
            "msgMessageCount" to "msg_message_count",
        )
    }
    @GetMapping("/ack")
    fun ack(type:String): Any
    {
        if(ackTypeList.contains(type))
        {
            //合法的
            val userId = StpUtil.getLoginIdAsLong()
            val update = UpdateWrapper<UserExtraInfoEntity>().eq("user_id", userId)
                .set(ackTypeListToDBField[type], 0)
            val key = "${USER_HOT_INFO_COUNT}${userId}"
            redisTemplate.opsForHash<String, Long>().delete(key, type)
            userExtraInfoServiceImpl.update(update)
            return ResultCode.SUCCESS
        }else
        {
            return ResultCode.UNKNOWN_ERROR
        }
    }
}
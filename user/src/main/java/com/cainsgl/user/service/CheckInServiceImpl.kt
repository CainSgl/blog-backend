package com.cainsgl.user.service

import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import com.cainsgl.common.entity.user.UserEntity
import com.cainsgl.user.dto.response.CheckInResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

@Service
class CheckInServiceImpl {

    @Resource
    lateinit var redisTemplate: RedisTemplate<String, Any>

    @Resource
    lateinit var userService: UserServiceImpl

    companion object {
        private const val CHECK_IN_KEY_PREFIX = "user:checkin:"
    }

    /**
     * 生成当月签到的 Redis Key
     */
    private fun getMonthlyCheckInKey(userId: Long): String {
        val today = LocalDate.now()
        val yearMonth = YearMonth.from(today)
        return "${CHECK_IN_KEY_PREFIX}${userId}:${yearMonth.year}${String.format("%02d", yearMonth.monthValue)}"
    }

    /**
     * 获取当月剩余天数
     */
    private fun getDaysUntilMonthEnd(): Long {
        val today = LocalDate.now()
        val lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth())
        return ChronoUnit.DAYS.between(today, lastDayOfMonth) + 1
    }
    fun hasBitFromByte(byteValue: UByte, bitPosition: Int): Boolean {
        val mask = 1 shl bitPosition
        return (byteValue.toInt() and mask) != 0
    }
    /**
     * 用户签到
     * 优化：使用RedisCallback直接获取原始字节数组，绕过FastJSON序列化
     */
    fun checkIn(userId: Long): CheckInResponse {
        val today = LocalDate.now()
        val dayOfMonth = today.dayOfMonth
        val daysInMonth = today.lengthOfMonth()
        val key = getMonthlyCheckInKey(userId)

        // 使用RedisCallback直接获取原始字节数组，绕过序列化器
        val bytes = redisTemplate.execute { connection ->
            connection.stringCommands().get(key.toByteArray())
        }

        // 解析bitmap，获取已签到的日期列表
        val checkInDays = mutableListOf<Int>()
        var alreadyCheckedIn = false
        
        if (bytes != null) {
            for (day in 1..daysInMonth) {
                val bitIndex = day - 1
                val byteIndex = bitIndex / 8
                val bitOffset = bitIndex % 8
                if (byteIndex < bytes.size) {
                    if (hasBitFromByte(bytes[byteIndex].toUByte(),7-bitOffset)) {
                        checkInDays.add(day)
                        if (day == dayOfMonth) {
                            alreadyCheckedIn = true
                        }
                    }
                }
            }
        }

        // 如果今天已签到，直接返回
        if (alreadyCheckedIn) {
            return CheckInResponse(
                success = false,
                expGained = 0,
                checkInDays = checkInDays
            )
        }

        // 执行签到
        redisTemplate.opsForValue().setBit(key, (dayOfMonth - 1).toLong(), true)
        redisTemplate.expire(key, getDaysUntilMonthEnd(), TimeUnit.DAYS)
        // 添加今天到签到列表
        checkInDays.add(dayOfMonth)
        // 获得的经验 = 本月签到总天数
        val expGained = checkInDays.size
        // 增加经验值
        addExperience(userId, expGained)

        log.info { "用户 $userId 签到成功，本月签到 ${checkInDays.size} 天，获得 $expGained 经验" }

        return CheckInResponse(
            success = true,
            expGained = expGained,
            checkInDays = checkInDays
        )
    }

    /**
     * 增加用户经验值
     */
    private fun addExperience(userId: Long, exp: Int) {
        val updateWrapper = KtUpdateWrapper(UserEntity::class.java)
            .eq(UserEntity::id, userId)
            .setSql("experience = experience + $exp")
        
        userService.update(updateWrapper)

        // 清除缓存
        val cacheKey = "${UserServiceImpl.USER_REDIS_PREFIX}${userId}"
        redisTemplate.delete(cacheKey)
    }
}

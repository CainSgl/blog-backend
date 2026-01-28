package com.cainsgl.user.service

import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.common.entity.user.UserNoticeEntity
import com.cainsgl.common.entity.user.UserNoticeType
import com.cainsgl.user.repository.UserNoticeMapper
import org.springframework.stereotype.Service

@Service
class UserNoticeServiceImpl : ServiceImpl<UserNoticeMapper, UserNoticeEntity>(), IService<UserNoticeEntity>
{

    fun getUserNoticeAndMarkChecked(userId: Long, typeStr: String, after: Long?, size: Int): Map<String, Any?>
    {
        val noticeType = UserNoticeType.getByOperate(typeStr)
        val type = if (noticeType != UserNoticeType.UNKNOW) noticeType.type.toShort() else null
        
        // 使用连表查询获取通知及 target_user 的基本信息
        val records = baseMapper.selectUserNoticeWithTargetUserInfo(userId, type, after, size)
        
        // 标记为已读
        if (records.isNotEmpty())
        {
            val ids = records.mapNotNull { it.id }
            if (ids.isNotEmpty())
            {
                val updateWrapper = KtUpdateWrapper(UserNoticeEntity::class.java)
                    .`in`(UserNoticeEntity::id, ids)
                    .eq(UserNoticeEntity::checked, false)
                    .set(UserNoticeEntity::checked, true)
                this.update(updateWrapper)
            }
        }
        
        return mapOf(
            "records" to records,
            "after" to records.lastOrNull()?.id,
            "hasMore" to (records.size >= size)
        )
    }

}

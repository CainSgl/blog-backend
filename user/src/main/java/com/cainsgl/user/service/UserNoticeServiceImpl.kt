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
    fun getUserNoticeAndMarkCheckedByType(userId: Long, types: List<String>, after: Long?, size: Int): Map<String, Any?>
    {
        val typeList= types.mapNotNull {
            val type = UserNoticeType.getByOperate(it)
            if(type== UserNoticeType.UNKNOW)
            {
                return@mapNotNull null
            }
            return@mapNotNull type.type.toShort()
        }
        return getUserNoticeAndMarkChecked(userId, after, size,typeList)

    }
    fun getUserNoticeAndMarkChecked(userId: Long, after: Long?, size: Int, types:List<Short>): Map<String, Any?>
    {

        // 使用连表查询获取通知及 target_user 的基本信息
        val records = baseMapper.selectUserNoticeWithTargetUserInfo(userId, types, after, size)
        
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

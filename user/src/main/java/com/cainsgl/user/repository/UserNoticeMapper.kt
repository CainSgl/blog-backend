package com.cainsgl.user.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.common.entity.user.UserNoticeEntity
import com.cainsgl.user.dto.response.vo.UserNoticeVO
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

@Mapper
interface UserNoticeMapper : BaseMapper<UserNoticeEntity> {
    
    fun selectUserNoticeWithTargetUserInfo(
        @Param("userId") userId: Long,
        @Param("type") type: Short?,
        @Param("after") after: Long?,
        @Param("size") size: Int
    ): List<UserNoticeVO>
}

package com.cainsgl.user.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.common.entity.user.UserExtraInfoEntity
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Update

@Mapper
interface UserExtraInfoMapper : BaseMapper<UserExtraInfoEntity>
{
//    @Select("SELECT interest_vector FROM user_extra_infos WHERE user_id = #{userId}")
//    @Results(
//        Result(
//            //      property = "interestVector",
//            column = "interest_vector",
//            typeHandler = VectorTypeHandler::class
//        )
//    )
//    fun selectInterestVector(userId: Long): FloatArray

    @Update("UPDATE user_extra_info SET interest_vector = #{interestVector, typeHandler=com.cainsgl.common.handler.VectorTypeHandler} WHERE user_id = #{userId}")
    fun updateInterestVector(@Param("userId") userId: Long, @Param("interestVector") interestVector: FloatArray): Int

    /**
     * 批量增量更新用户额外信息
     * 使用 CASE WHEN 实现一次SQL更新多条记录
     */
    fun batchIncrementUserExtraInfo(
        @Param("userIds") userIds: List<Long>,
        @Param("likeCountMap") likeCountMap: Map<Long, Long>,
        @Param("commentCountMap") commentCountMap: Map<Long, Long>,
        @Param("postCountMap") postCountMap: Map<Long, Long>,
        @Param("articleViewCountMap") articleViewCountMap: Map<Long, Long>,
        @Param("followerCountMap") followerCountMap: Map<Long, Long>,
        @Param("followingCountMap") followingCountMap: Map<Long, Long>,
        @Param("msgCountMap") msgCountMap: Map<Long, Long>,
        @Param("msgReplyCountMap") msgReplyCountMap: Map<Long, Long>,
        @Param("msgLikeCountMap") msgLikeCountMap: Map<Long, Long>,
        @Param("msgReportCountMap") msgReportCountMap: Map<Long, Long>,
        @Param("msgMessageCountMap") msgMessageCountMap: Map<Long, Long>
    ): Int
}
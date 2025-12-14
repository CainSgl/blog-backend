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

    @Update("UPDATE user_extra_infos SET interest_vector = #{interestVector, typeHandler=com.cainsgl.common.handler.VectorTypeHandler} WHERE user_id = #{userId}")
    fun updateInterestVector(@Param("userId") userId: Long, @Param("interestVector") interestVector: FloatArray): Int

}
package com.cainsgl.api.user.extra

import com.cainsgl.common.entity.user.UserExtraInfoEntity
import com.cainsgl.grpc.user.GetInterestVectorRequest
import com.cainsgl.grpc.user.SetInterestVectorRequest
import com.cainsgl.grpc.user.UserExtraInfoServiceGrpc
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

@Service
@ConditionalOnMissingBean(type = ["com.cainsgl.user.service.UserExtraInfoServiceImpl"])
class UserExtraInfoGrpcService : UserExtraInfoService
{
    @GrpcClient("UserExtraInfoService")
    lateinit var userExtraInfoServiceGrpc: UserExtraInfoServiceGrpc.UserExtraInfoServiceBlockingStub

    override fun getInterestVector(userId: Long): FloatArray?
    {
        val request = GetInterestVectorRequest.newBuilder()
            .setUserId(userId)
            .build()
        val response = userExtraInfoServiceGrpc.getInterestVector(request)
        return if (response.interestVectorList.isEmpty()) null else response.interestVectorList.toFloatArray()
    }

    override fun setInterestVector(userId: Long, values: FloatArray):Boolean
    {
        val request = SetInterestVectorRequest.newBuilder()
            .setUserId(userId)
            .addAllInterestVector(values.toList())
            .build()
        val response = userExtraInfoServiceGrpc.setInterestVector(request)
        return response.success
    }
    @Deprecated("目前无需定时任务模块调度，直接让该服务直接完成，无需引入其他依赖。性能更高")
    override fun saveCount(userExtraInfoList: List<UserExtraInfoEntity>): Boolean
    {
        throw IllegalArgumentException("该api废弃，不允许调用")
//        val requests = userExtraInfoList.map { userExtraInfo ->
//            val requestBuilder = SaveCountRequest.newBuilder()
//                .setUserId(userExtraInfo.userId!!)
//
//            userExtraInfo.likeCount?.let { requestBuilder.setLikeCount(it) }
//            userExtraInfo.commentCount?.let { requestBuilder.setCommentCount(it) }
//            userExtraInfo.postCount?.let { requestBuilder.setPostCount(it) }
//            userExtraInfo.articleViewCount?.let { requestBuilder.setArticleViewCount(it) }
//            userExtraInfo.followingCount?.let { requestBuilder.setFollowingCount(it) }
//            userExtraInfo.followerCount?.let { requestBuilder.setFollowerCount(it) }
//
//            requestBuilder.build()
//        }
//
//        val batchRequest = BatchSaveCountRequest.newBuilder()
//            .addAllRequests(requests)
//            .build()
//
//        val response = userExtraInfoServiceGrpc.saveCount(batchRequest)
//        return response.success
    }
}

package com.cainsgl.comment.service

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.comment.entity.CommentEntity
import com.cainsgl.comment.repository.CommentMapper
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class CommentServiceImpl : ServiceImpl<CommentMapper, CommentEntity>() {
    fun getByCursor(
        postId: Int,
        version: Int,
        dataId: Int,
        lastCreatedAt: LocalDate?,
        lastLikeCount: Int?
    ): List<CommentEntity>
    {
      return  baseMapper.selectByCursor(postId, version, dataId,lastCreatedAt, lastLikeCount)
    }

}
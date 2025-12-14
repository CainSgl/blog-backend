package com.cainsgl.article.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.api.article.post.PostService
import com.cainsgl.article.repository.PostMapper
import com.cainsgl.common.entity.article.PostEntity
import org.springframework.stereotype.Service

@Service
class PostServiceImpl : ServiceImpl<PostMapper, PostEntity>(), PostService, IService<PostEntity>
{

    /**
     * 获取文章信息
     * @param id
     * @return
     */
    fun getPost(id: Long): PostEntity?
    {
        return baseMapper.selectById(id)
    }

    override fun getById(id: Long): PostEntity?
    {
        return baseMapper.selectById(id)
    }

    override fun getVectorById(id: Long): FloatArray?
    {
        //去数据库查
        val queryWrapper= QueryWrapper<PostEntity>()
        queryWrapper.select("vector")
        queryWrapper.eq("id", id)
        val entity = baseMapper.selectOne(queryWrapper)
        return entity.vecotr
        //    return baseMapper.selectVectorById(id)
    }

    override fun addViewCount(id: Long, count: Int):Boolean
    {
        val wrapper = UpdateWrapper<PostEntity>()
        wrapper.eq("id", id)
        wrapper.setSql("viewCount=viewCount+$count")
        return baseMapper.update(wrapper)>0
    }

}

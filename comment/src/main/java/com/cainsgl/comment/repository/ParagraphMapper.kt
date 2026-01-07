package com.cainsgl.comment.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.comment.entity.ParagraphEntity
import org.apache.ibatis.annotations.Mapper

@Mapper
interface ParagraphMapper : BaseMapper<ParagraphEntity> {
    // 暂时留空，后续根据需要添加方法
}
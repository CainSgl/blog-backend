package com.cainsgl.comment.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.comment.entity.ParagraphEntity
import org.apache.ibatis.annotations.Mapper

@Mapper
interface ParagraphMapper : BaseMapper<ParagraphEntity> {

}
package com.cainsgl.article.system.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.article.system.entity.CarouselEntity
import org.apache.ibatis.annotations.Mapper

@Mapper
interface CarouselMapper : BaseMapper<CarouselEntity>
{

}
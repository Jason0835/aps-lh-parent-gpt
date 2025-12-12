package com.zlt.aps.maindata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.monthplan.api.domain.entity.MpOrderPrediction;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpOrderPredictionEntityMapper.java
 * 描    述：订单预测Mapper接口
 *
 * @author zlt
 * @version 1.0
 */
public interface MpOrderPredictionEntityMapper extends CommBaseMapper<MpOrderPrediction> {

}
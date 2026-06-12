package com.zlt.aps.maindata.mapper;

import com.zlt.aps.mp.api.domain.entity.ProductionModeParam;
import com.zlt.aps.mp.api.domain.vo.MpProductionModeInfoVo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ProductionModeParamMapper.java
 * 描    述：排产模式参数配置项对象Mapper接口
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 20260611
 */
@Mapper
public interface ProductionModeParamMapper extends CommBaseMapper<ProductionModeParam> {
    /**
     * 将排产模式对应的配置项应用到排产参数中
     *
     * @param param
     * @return
     */
    int updateFactoryParamByProductionMode(MpProductionModeInfoVo param);

    /**
     * 更新排产模式参数值
     *
     * @param param
     * @return
     */
    int changeProductionMode(MpProductionModeInfoVo param);
}

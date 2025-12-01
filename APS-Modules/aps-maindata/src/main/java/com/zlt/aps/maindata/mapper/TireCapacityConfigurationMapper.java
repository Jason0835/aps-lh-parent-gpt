package com.zlt.aps.maindata.mapper;

import com.zlt.aps.monthplan.api.domain.entity.TireCapacityConfiguration;
import com.zlt.aps.monthplan.api.domain.vo.TireCapacityConfigurationVo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：TireCapacityConfigurationMapper.java
 * 描    述：轮胎类型产能配置(特殊情况下配置)Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-06-04
 */
@Mapper
public interface TireCapacityConfigurationMapper extends CommBaseMapper<TireCapacityConfiguration> {
    /**
     * 根据查询条件，获取轮胎类型产能配置
     *
     * @param condition
     * @return
     */
    List<TireCapacityConfigurationVo> getConfigurationList(TireCapacityConfiguration condition);

    /**
     * 根据需求版本，轮胎类型，寸口获取需求信息
     *
     * @param condition
     * @return
     */
    TireCapacityConfigurationVo getDemandInfo(TireCapacityConfiguration condition);
}

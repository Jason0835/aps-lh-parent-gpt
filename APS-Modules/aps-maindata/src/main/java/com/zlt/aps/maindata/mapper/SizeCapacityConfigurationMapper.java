package com.zlt.aps.maindata.mapper;

import com.zlt.aps.monthplan.api.domain.entity.SizeCapacityConfiguration;
import com.zlt.aps.monthplan.api.domain.vo.SizeCapacityConfigurationVo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：SizeCapacityConfigurationMapper.java
 * 描    述：寸口产能配置Mapper接口
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
public interface SizeCapacityConfigurationMapper extends CommBaseMapper<SizeCapacityConfiguration> {
    /**
     * 根据查询条件，获取寸口产能配置
     *
     * @param condition
     * @return
     */
    List<SizeCapacityConfigurationVo> getConfigurationList(SizeCapacityConfiguration condition);

    /**
     * 根据查询条件，获取对应的需求信息
     *
     * @param condition
     * @return
     */
    SizeCapacityConfigurationVo getDemandInfo(SizeCapacityConfiguration condition);
}

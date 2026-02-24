package com.zlt.aps.maindata.mapper;

import com.zlt.aps.monthplan.api.domain.entity.FixedPointConfiguration;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FixedPointConfigurationMapper.java
 * 描    述：基础数据-定点机台主Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-21
 */
@Mapper
public interface FixedPointConfigurationMapper extends CommBaseMapper<FixedPointConfiguration> {

    /**
     * 查询基础数据-定点机台主列表
     *
     * @param fixedPointConfiguration 基础数据-定点机台主
     * @return 基础数据-定点机台主集合
     */
    public List<FixedPointConfiguration> selectFixedPointConfigurationList(FixedPointConfiguration fixedPointConfiguration);
}

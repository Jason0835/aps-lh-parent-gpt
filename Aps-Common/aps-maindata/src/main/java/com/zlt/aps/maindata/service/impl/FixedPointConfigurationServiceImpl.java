package com.zlt.aps.maindata.service.impl;

import com.zlt.aps.maindata.mapper.FixedPointConfigurationMapper;
import com.zlt.aps.maindata.mapper.FixedPointMoldingConfigurationMapper;
import com.zlt.aps.maindata.mapper.FixedPointProductConfigurationMapper;
import com.zlt.aps.maindata.mapper.FixedPointVulcanizingConfigurationMapper;
import com.zlt.aps.maindata.service.IFixedPointConfigurationService;
import com.zlt.aps.mp.api.domain.entity.FixedPointConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FixedPointConfigurationServiceImpl.java
 * 描    述：FixedPointConfigurationServiceImpl基础数据-定点机台主业务层处理
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
@Slf4j
@Service
public class FixedPointConfigurationServiceImpl implements IFixedPointConfigurationService {

    private final FixedPointConfigurationMapper fixedPointConfigurationMapper;

    private final FixedPointProductConfigurationMapper fixedPointProductConfigurationMapper;

    private final FixedPointMoldingConfigurationMapper fixedPointMoldingConfigurationMapper;

    private final FixedPointVulcanizingConfigurationMapper fixedPointVulcanizingConfigurationMapper;

    public FixedPointConfigurationServiceImpl(FixedPointConfigurationMapper fixedPointConfigurationMapper,
                                              FixedPointProductConfigurationMapper fixedPointProductConfigurationMapper,
                                              FixedPointMoldingConfigurationMapper fixedPointMoldingConfigurationMapper,
                                              FixedPointVulcanizingConfigurationMapper fixedPointVulcanizingConfigurationMapper) {
        this.fixedPointConfigurationMapper = fixedPointConfigurationMapper;
        this.fixedPointProductConfigurationMapper = fixedPointProductConfigurationMapper;
        this.fixedPointMoldingConfigurationMapper = fixedPointMoldingConfigurationMapper;
        this.fixedPointVulcanizingConfigurationMapper = fixedPointVulcanizingConfigurationMapper;
    }

    @Override
    public List<FixedPointConfiguration> selectFixedPointConfigurationList(FixedPointConfiguration fixedPointConfiguration) {
        return fixedPointConfigurationMapper.selectFixedPointConfigurationList(fixedPointConfiguration);
    }
}

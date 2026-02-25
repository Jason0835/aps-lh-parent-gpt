package com.zlt.aps.maindata.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.maindata.mapper.LocationChannelConfigurationMapper;
import com.zlt.aps.maindata.service.ILocationChannelConfigurationService;
import com.zlt.aps.mp.api.domain.entity.LocationChannelConfiguration;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LocationChannelConfigurationServiceImpl.java
 * 描    述：LocationChannelConfigurationServiceImpl库位类别渠道品牌配置业务层处理
 *@author ZLT
 *@date 2025-02-28
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：ZLT
 *     修改内容：...
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class LocationChannelConfigurationServiceImpl extends AbstractDocService<LocationChannelConfiguration>  implements ILocationChannelConfigurationService {
    @Override
    protected String getDocTypeCode() {
        return "0132";
    }

    private final LocationChannelConfigurationMapper locationChannelConfigurationMapper;

    public LocationChannelConfigurationServiceImpl(LocationChannelConfigurationMapper locationChannelConfigurationMapper) {
        this.locationChannelConfigurationMapper = locationChannelConfigurationMapper;
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("0132");
        return sysDocType;
    }

    @Override
    public String checkUnique(LocationChannelConfiguration docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.column.locationChannelConfiguration.checkUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "locationType", "channel", "brand");
    }
}

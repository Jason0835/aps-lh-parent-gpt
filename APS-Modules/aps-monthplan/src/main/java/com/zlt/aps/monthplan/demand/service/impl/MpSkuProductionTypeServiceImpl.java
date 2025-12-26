package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.monthplan.api.domain.entity.MpSkuProductionType;
import com.zlt.aps.monthplan.demand.mapper.MpSkuProductionTypeEntityMapper;
import com.zlt.aps.monthplan.demand.service.IMpSkuProductionTypeService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpSkuProductionTypeServiceImpl.java
 * 描    述：MpSkuProductionTypeServiceImplSKU排产分类业务层处理
 *@author yelq
 *@date 2025-12-26
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class MpSkuProductionTypeServiceImpl extends AbstractDocService<MpSkuProductionType>  implements IMpSkuProductionTypeService {
    private final MpSkuProductionTypeEntityMapper mpSkuProductionTypeEntityMapper;
    @Override
    protected String getDocTypeCode() {
        return "2025122600";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("2025122600");
        return sysDocType;
    }

    @Override
    public String checkUnique(MpSkuProductionType docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mpSkuProductionType.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public Map<String, String> skuToProductionType() {
        LambdaQueryWrapper<MpSkuProductionType> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MpSkuProductionType::getIsDelete, YesOrNoEnum.NO.getValue());
        List<MpSkuProductionType>  list =   mpSkuProductionTypeEntityMapper.selectList(wrapper);
        if(CollectionUtils.isEmpty(list)) {
            return Collections.emptyMap();
        }
        return list.stream()
            .filter(Objects::nonNull)
            .filter(skuProductionType -> StringUtils.isNotBlank(skuProductionType.getMaterialCode()))
            .collect(Collectors.toMap(MpSkuProductionType::getMaterialCode,
                MpSkuProductionType::getProductionType,
                (existing, replacement) -> existing
            ));
    }
}

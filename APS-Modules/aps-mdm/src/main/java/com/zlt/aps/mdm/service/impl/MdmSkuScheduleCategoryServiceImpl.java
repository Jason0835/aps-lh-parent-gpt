package com.zlt.aps.mdm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mdm.mapper.MdmSkuScheduleCategoryEntityMapper;
import com.zlt.aps.mdm.service.IMdmSkuScheduleCategoryService;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuScheduleCategory;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmSkuScheduleCategoryServiceImpl.java
 * 描    述：MdmSkuScheduleCategoryServiceImplSKU排产分类业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-11
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class MdmSkuScheduleCategoryServiceImpl extends AbstractDocService<MdmSkuScheduleCategory> implements IMdmSkuScheduleCategoryService {
    private final MdmSkuScheduleCategoryEntityMapper skuScheduleCategoryEntityMapper;
    @Override
    protected String getDocTypeCode() {
        return "MDM0146";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0146");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmSkuScheduleCategory docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmSkuScheduleCategory.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public Map<String, String> skuToProductionType(String factoryCode) {
        LambdaQueryWrapper<MdmSkuScheduleCategory> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MdmSkuScheduleCategory::getFactoryCode, factoryCode);
        wrapper.eq(MdmSkuScheduleCategory::getIsDelete, YesOrNoEnum.NO.getValue());
        List<MdmSkuScheduleCategory>  list =   skuScheduleCategoryEntityMapper.selectList(wrapper);
        if(CollectionUtils.isEmpty(list)) {
            return Collections.emptyMap();
        }
        return list.stream()
            .filter(Objects::nonNull)
            .filter(skuScheduleCategory -> StringUtils.isNotBlank(skuScheduleCategory.getMaterialCode()))
            .collect(Collectors.toMap(MdmSkuScheduleCategory::getMaterialCode,
                MdmSkuScheduleCategory::getScheduleType,
                (existing, replacement) -> existing
            ));
    }
}

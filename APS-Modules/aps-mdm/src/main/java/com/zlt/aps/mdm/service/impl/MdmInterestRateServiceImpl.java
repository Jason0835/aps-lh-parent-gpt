package com.zlt.aps.mdm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.mdm.mapper.MdmInterestRateEntityMapper;
import com.zlt.aps.mdm.service.IMdmInterestRateService;
import com.zlt.aps.mdm.api.domain.entity.MdmInterestRate;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmInterestRateServiceImpl.java
 * 描    述：MdmInterestRateServiceImpl利率优先等级配置业务层处理
 *@author zlt
 *@date 2025-03-03
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MdmInterestRateServiceImpl extends AbstractDocService<MdmInterestRate>  implements IMdmInterestRateService {

    @Autowired
    private MdmInterestRateEntityMapper mdmInterestRateMapper;

    @Override
    protected String getDocTypeCode() {
        return "0136";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("0118");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmInterestRate docEntityVO) {
        LambdaQueryWrapper<MdmInterestRate> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(Objects.nonNull(docEntityVO.getId()), BaseEntity::getId, docEntityVO.getId());
        wrapper.lt(MdmInterestRate::getValueMin, docEntityVO.getValueMax());
        wrapper.gt(MdmInterestRate::getValueMax, docEntityVO.getValueMin());
        Long count = mdmInterestRateMapper.selectCount(wrapper);
        if (count > 0) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.column.mdmInterestRate.checkUnique"));
        }
        if (docEntityVO.getValueMin().compareTo(docEntityVO.getValueMax()) > 0) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.column.mdmInterestRate.minCanNotGreaterThanMax"));
        }
        return UserConstants.UNIQUE;
    }
}


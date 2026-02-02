package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlanSum;
import com.zlt.aps.monthplan.api.domain.entity.DpOrderOffsetDetail;
import com.zlt.aps.monthplan.demand.mapper.DpOrderOffsetDetailEntityMapper;
import com.zlt.aps.monthplan.demand.service.IDpOrderOffsetDetailService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.util.CollectionUtils;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DpOrderOffsetDetailServiceImpl.java
 * 描    述：DpOrderOffsetDetailServiceImplS1-0604订单冲减分配业务层处理
 *@author yelq
 *@date 2025-12-30
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class DpOrderOffsetDetailServiceImpl extends AbstractDocService<DpOrderOffsetDetail>  implements IDpOrderOffsetDetailService {
    private final DpOrderOffsetDetailEntityMapper dpOrderOffsetDetailEntityMapper;
    @Override
    protected String getDocTypeCode() {
        return "2025123011";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("2025123011");
        return sysDocType;
    }

    @Override
    public String checkUnique(DpOrderOffsetDetail docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.dpOrderOffsetDetail.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public List<DpOrderOffsetDetail> findPredictOffsetDetail(String monthPlanVersion) {
        LambdaQueryWrapper<DpOrderOffsetDetail> wrapper =
            Wrappers.lambdaQuery(DpOrderOffsetDetail.class)
                .eq(DpOrderOffsetDetail::getMonthPlanVersion, monthPlanVersion)
                .eq(DpOrderOffsetDetail::getIsDelete, ApsConstant.APS_YES_NO_0);
        return dpOrderOffsetDetailEntityMapper.selectList(wrapper);
    }

    /**
     * 获取订单冲减的版本号
     * @param queryCondition 查询条件
     * @return 版本集合
     */
    @Override
    public List<String> getOffsetVersion(DpOrderOffsetDetail queryCondition) {
        return dpOrderOffsetDetailEntityMapper.getOffsetVersion(
                queryCondition.getFactoryCode(),
                queryCondition.getYear(),
                queryCondition.getMonth());
    }
}

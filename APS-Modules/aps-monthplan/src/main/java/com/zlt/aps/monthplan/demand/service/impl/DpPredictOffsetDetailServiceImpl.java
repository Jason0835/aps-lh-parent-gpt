package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.monthplan.api.domain.entity.DpPredictOffsetDetail;
import com.zlt.aps.monthplan.demand.mapper.DpPredictOffsetDetailEntityMapper;
import com.zlt.aps.monthplan.demand.service.IDpPredictOffsetDetailService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;
import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DpPredictOffsetDetailServiceImpl.java
 * 描    述：DpPredictOffsetDetailServiceImpl预测冲减分配业务层处理
 *@author yelq
 *@date 2026-01-20
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
public class DpPredictOffsetDetailServiceImpl extends AbstractDocService<DpPredictOffsetDetail>  implements IDpPredictOffsetDetailService {
    private final DpPredictOffsetDetailEntityMapper dpPredictOffsetDetailMapper;
    @Override
    protected String getDocTypeCode() {
        return "2026012011";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("2026012011");
        return sysDocType;
    }

    @Override
    public String checkUnique(DpPredictOffsetDetail docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.dpPredictOffsetDetail.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public List<DpPredictOffsetDetail> findPredictOffsetDetail(Set<String> monthPlanVersions) {
        if(CollectionUtils.isEmpty(monthPlanVersions)) {
           return Collections.emptyList();
        }
        List<DpPredictOffsetDetail> result = new ArrayList<>();
        if(!CollectionUtils.isEmpty(monthPlanVersions)) {
            final int batchSize = 1000;
            List<String> versionList = new ArrayList<>(monthPlanVersions);
            for (int i = 0; i < versionList.size(); i += batchSize) {
                int end = Math.min(i + batchSize, versionList.size());
                List<String> batchVersions = versionList.subList(i, end);
                LambdaQueryWrapper<DpPredictOffsetDetail> wrapper =
                    Wrappers.lambdaQuery(DpPredictOffsetDetail.class)
                        .in(DpPredictOffsetDetail::getMonthPlanVersion, batchVersions)
                        .eq(DpPredictOffsetDetail::getIsDelete, ApsConstant.APS_YES_NO_0);
                result.addAll(dpPredictOffsetDetailMapper.selectList(wrapper));
            }
        }
        return result;
    }
}

package com.zlt.aps.monthplan.demand.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MpPredictionDetail;
import com.zlt.aps.monthplan.demand.service.IMpPredictionDetailService;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.util.CollectionUtils;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpPredictionDetailServiceImpl.java
 * 描    述：MpPredictionDetailServiceImpl预测明细业务层处理
 *@author yelq
 *@date 2026-01-16
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
public class MpPredictionDetailServiceImpl extends AbstractDocService<MpPredictionDetail>  implements IMpPredictionDetailService {
    @Override
    protected String getDocTypeCode() {
        return "2026011616";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("2026011616");
        return sysDocType;
    }

    @Override
    public String checkUnique(MpPredictionDetail docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mpPredictionDetail.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public void batchInsert(DpDemandPlan tMonthDemandPlan, Map<YearMonth, MpFactoryProductionVersion> productionVersions, List<FactoryMonthPlanProductionFinalResult> list) {
        List<MpPredictionDetail> result = new ArrayList<>();
        Map<String,String> productionVersionMap = this.getProductionVersionMap(list);
        productionVersions.forEach((yearMonth, productionVersion) -> {
            MpPredictionDetail predictionDetail = new MpPredictionDetail();
            predictionDetail.setPredictionVersion(productionVersion.getMonthPlanVersion());
            predictionDetail.setMonthPlanVersion(productionVersion.getMonthPlanVersion());
            predictionDetail.setProductionVersion(productionVersionMap.getOrDefault(predictionDetail.getMonthPlanVersion(), null));
            predictionDetail.setPredictionProductionVersion(predictionDetail.getProductionVersion());
            predictionDetail.setYear(yearMonth.getYear());
            predictionDetail.setPlanType(tMonthDemandPlan.getPlanType());
            predictionDetail.setFactoryCode(tMonthDemandPlan.getFactoryCode());
            predictionDetail.setBaseVale(null);
            predictionDetail.setIsDelete(YesOrNoEnum.NO.getValue());
            predictionDetail.setBatchNumber(tMonthDemandPlan.getMonthPlanVersion());
            result.add(predictionDetail);
        });
        if(!CollectionUtils.isEmpty(result)){
            this.baseDao.insertBatch(result);
        }
    }

    private Map<String, String> getProductionVersionMap(List<FactoryMonthPlanProductionFinalResult> list) {
        if(CollectionUtils.isEmpty(list)){
            return Collections.emptyMap();
        }
        return list.stream()
            .filter(item -> item.getMonthPlanVersion() != null)  // 过滤key为null的
            .collect(Collectors.toMap(
                FactoryMonthPlanProductionFinalResult::getMonthPlanVersion,
                FactoryMonthPlanProductionFinalResult::getProductionVersion,
                (oldValue, newValue) -> newValue  // 重复key时，取新的覆盖旧的
            ));
    }
}

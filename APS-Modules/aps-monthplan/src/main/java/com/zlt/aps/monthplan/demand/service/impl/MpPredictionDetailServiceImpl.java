package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MpPredictionDetail;
import com.zlt.aps.monthplan.common.utils.BatchNumberProcessor;
import com.zlt.aps.monthplan.demand.mapper.MpPredictionDetailEntityMapper;
import com.zlt.aps.monthplan.demand.service.IMpPredictionDetailService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
@RequiredArgsConstructor
public class MpPredictionDetailServiceImpl extends AbstractDocService<MpPredictionDetail>  implements IMpPredictionDetailService {
    private final MpPredictionDetailEntityMapper mpPredictionDetailEntityMapper;
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
    public void batchInsert(DpDemandPlan tMonthDemandPlan,Map<YearMonth, MpFactoryProductionVersion> productionVersions) {
        List<MpPredictionDetail> result = new ArrayList<>();
        productionVersions.forEach((yearMonth, productionVersion) -> {
            MpPredictionDetail predictionDetail = new MpPredictionDetail();
            predictionDetail.setMonthPlanVersion(productionVersion.getMonthPlanVersion());
            predictionDetail.setProductionVersion(productionVersion.getProductionVersion());
            predictionDetail.setPredictionVersion(productionVersion.getMonthPlanVersion());
            predictionDetail.setPredictionProductionVersion(productionVersion.getProductionVersion());
            predictionDetail.setYear(yearMonth.getYear());
            predictionDetail.setMonth(yearMonth.getMonthValue());
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

    @Override
    public Map<String, Map<String, MpPredictionDetail>> fetchVersion(Set<String> batchNumbers) {
        LambdaQueryWrapper<MpPredictionDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(MpPredictionDetail::getBatchNumber,batchNumbers);
        wrapper.eq(MpPredictionDetail::getIsDelete, YesOrNoEnum.NO.getValue());
        List<MpPredictionDetail> list =  this.mpPredictionDetailEntityMapper.selectList(wrapper);
        if(CollectionUtils.isEmpty(list)){
            return Collections.emptyMap();
        }
        Map<String, Map<String, MpPredictionDetail>> result = Maps.newHashMap();
        Map<String,List<MpPredictionDetail>>  map = list.stream().collect(Collectors.groupingBy(MpPredictionDetail::getBatchNumber));
        map.forEach((batchNumber,value)->{
            value.sort(Comparator.comparing(MpPredictionDetail::getYear).thenComparing(MpPredictionDetail::getMonth));
            Map<String, MpPredictionDetail> versionMap = Maps.newHashMap();
            for(int i = 0,size=value.size();i<size;i++){
                String key = "T";
                if(i > 0) {
                    key = key.concat(String.valueOf(i));
                }
                versionMap.put(key,value.get(i));
            }
            result.put(batchNumber,versionMap);
        });
        return result;
    }

    @Override
    public List<String> findSimulatedVersion(Set<String> batchNumbers) {
        if(CollectionUtils.isEmpty(batchNumbers)){
            return Collections.emptyList();
        }
        List<MpPredictionDetail> result = Lists.newArrayList();
        final int batchSize = 1000;
        List<String> versionList = new ArrayList<>(batchNumbers);
        for (int i = 0; i < versionList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, versionList.size());
            List<String> batchVersions = versionList.subList(i, end);
            LambdaQueryWrapper<MpPredictionDetail> wrapper =
                Wrappers.lambdaQuery(MpPredictionDetail.class)
                    .in(MpPredictionDetail::getBatchNumber, batchVersions)
                    .eq(MpPredictionDetail::getIsDelete, ApsConstant.APS_YES_NO_0);
            result.addAll(mpPredictionDetailEntityMapper.selectList(wrapper));
        }
        if(CollectionUtils.isEmpty(result)){
            return Collections.emptyList();
        }
        return BatchNumberProcessor.getLatestBatchNumbers(result);
    }

}

package com.zlt.aps.maindata.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.maindata.mapper.MdmUnqualifiedStockEntityMapper;
import com.zlt.aps.maindata.mapper.MpMonthPlanMonitorEntityMapper;
import com.zlt.aps.maindata.service.IMpMonthPlanMonitorService;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.api.domain.entity.MdmUnqualifiedStock;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthPlanMonitor;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpMonthPlanMonitorServiceImpl.java
 * 描    述：MpMonthPlanMonitorServiceImpl月度硫化监控业务层处理
 *@author zlt
 *@date 2025-12-24
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(rollbackFor = Exception.class)
public class MpMonthPlanMonitorServiceImpl extends AbstractDocService<MpMonthPlanMonitor>  implements IMpMonthPlanMonitorService {

    private final MpMonthPlanMonitorEntityMapper mpMonthPlanMonitorEntityMapper;

    @Autowired
    private MdmUnqualifiedStockEntityMapper unqualifiedStockEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "MONTH0612";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MONTH0612");
        return sysDocType;
    }

    @Override
    public String checkUnique(MpMonthPlanMonitor docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mpMonthPlanMonitor.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public List<MpMonthPlanMonitor> findCompleteQty(MpFactoryProductionVersion finalVersion) {
        LambdaQueryWrapper<MpMonthPlanMonitor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MpMonthPlanMonitor::getFactoryCode, finalVersion.getFactoryCode());
        wrapper.eq(MpMonthPlanMonitor::getYear, finalVersion.getYear());
        wrapper.eq(MpMonthPlanMonitor::getMonth, finalVersion.getMonth());
        wrapper.eq(MpMonthPlanMonitor::getMonthPlanVersion, finalVersion.getMonthPlanVersion());
        wrapper.eq(MpMonthPlanMonitor::getIsDelete, YesOrNoEnum.NO.getValue());
        return this.mpMonthPlanMonitorEntityMapper.selectList(wrapper);
    }

    /**
     * 批量插入月度硫化监控表
     *
     * @param param     参数
     * @param finalList 定稿数据
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void insertMonitorByFinalList(FactoryMonthPlanProductionFinalResult param, List<FactoryMonthPlanProductionFinalResult> finalList) {
        // 查询版本表，取排产周期起始日
        Map<String, Object> versionMap = new HashMap<>(16);
        versionMap.put("year", param.getYear());
        versionMap.put("month", param.getMonth());
        versionMap.put("factory_code", param.getFactoryCode());
        versionMap.put("month_plan_version", param.getMonthPlanVersion());
        versionMap.put("production_version", param.getProductionVersion());
        List<MpFactoryProductionVersion> productionVersionList = baseDao.selectByMap(MpFactoryProductionVersion.class, versionMap);
        if (CollectionUtils.isEmpty(productionVersionList)) {
            log.error("未找到对应的版本数据！，参数：{}", JSON.toJSONString(versionMap));
            throw new RuntimeException("未找到对应的版本数据！，参数：" + JSON.toJSONString(versionMap));
        }
        MpFactoryProductionVersion productionVersion = productionVersionList.get(0);
        Date productionStartDate = productionVersion.getProductionStartDate();
        // 查询不合格库存
        List<MpMonthPlanMonitor> list = new ArrayList<>();
        List<String> materialCodeList = finalList.stream().map(FactoryMonthPlanProductionFinalResult::getMaterialCode).distinct().collect(Collectors.toList());
        List<MdmUnqualifiedStock> mdmUnqualifiedStockList = new ArrayList<>();
        List<List<String>> splitList = ScmListUtils.getSplitList(materialCodeList, 500);
        for (List<String> codeList : splitList) {
            LambdaQueryWrapper<MdmUnqualifiedStock> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MdmUnqualifiedStock::getYear, param.getYear());
            wrapper.eq(MdmUnqualifiedStock::getMonth, param.getMonth());
            wrapper.in(MdmUnqualifiedStock::getMaterialCode, codeList);
            mdmUnqualifiedStockList.addAll(unqualifiedStockEntityMapper.selectList(wrapper));
        }
        Map<String, Integer> unqualifiedStockMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(mdmUnqualifiedStockList)) {
            unqualifiedStockMap = mdmUnqualifiedStockList.stream().collect(Collectors
                    .toMap(item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getYear(), item.getMonth(),
                                    item.getMaterialCode()),
                            MdmUnqualifiedStock::getStockQty, Integer::sum));
        }
        for (FactoryMonthPlanProductionFinalResult finalResult : finalList) {
            MpMonthPlanMonitor monitor = new MpMonthPlanMonitor();
            monitor.setFactoryCode(finalResult.getFactoryCode());
            Integer year = finalResult.getYear();
            monitor.setYear(year);
            Integer month = finalResult.getMonth();
            monitor.setMonth(month);
            monitor.setYearMonth(finalResult.getYearMonth());
            monitor.setMonthPlanVersion(finalResult.getMonthPlanVersion());
            monitor.setProductionVersion(finalResult.getProductionVersion());
            monitor.setProductTypeCode(finalResult.getProductTypeCode());
            monitor.setProductStatus(finalResult.getProductStatus());
            monitor.setStructureName(finalResult.getStructureName());
            monitor.setMainMaterialDesc(finalResult.getMainMaterialDesc());
            monitor.setMesMaterialCode(finalResult.getMesMaterialCode());
            monitor.setMaterialCode(finalResult.getMaterialCode());
            monitor.setMaterialDesc(finalResult.getMaterialDesc());
            monitor.setBrand(finalResult.getBrand());
            monitor.setProSize(finalResult.getProSize());
            monitor.setSpecifications(finalResult.getSpecifications());
            monitor.setMainPattern(finalResult.getMainPattern());
            monitor.setPattern(finalResult.getPattern());
            Integer beginDay = finalResult.getBeginDay();
            Date onboardDate = DateUtils.addDays(productionStartDate, beginDay - 1);
            monitor.setOnboardDate(onboardDate);
            // 根据年月+工厂+物料，取不合格库存数量
            String mapKey = GenerageMapKeyUtils.createMapKey(finalResult.getFactoryCode(), finalResult.getYear(), finalResult.getMonth(),
                    finalResult.getMaterialCode());
            if (unqualifiedStockMap.containsKey(mapKey)) {
                monitor.setUnqualifiedQty(unqualifiedStockMap.get(mapKey));
            }
            monitor.setMouldQty(finalResult.getMouldChangeInfo());
            monitor.setNetDemandQty(finalResult.getProdReqPlan());
//            monitor.setProductionQty(finalResult.getProductionQty());
            Integer productionQty = ObjectUtils.defaultIfNull(monitor.getProductionQty(), 0);
            Integer scheduleQty = ObjectUtils.defaultIfNull(monitor.getScheduleQty(), 0);
            monitor.setLhMargin(scheduleQty - productionQty);
            monitor.setExpectedCloseDay(0);

            Integer endDay = finalResult.getEndDay();
            Date date = DateUtils.parseDate(year + "-" + month + "-" + endDay);
            monitor.setExpectedCloseDate(date);
            monitor.setPlanCloseDate(date);
            long diffMillTime = DateUtils.getDiffMillTime(monitor.getExpectedCloseDate(), monitor.getPlanCloseDate());
            monitor.setDiffDay(Integer.parseInt(String.valueOf(diffMillTime)) / 1000 / 60 / 60 / 24);
            monitor.setFinalResultId(finalResult.getId());
            list.add(monitor);
        }
        baseDao.insertBatch(list);
    }
}

package com.zlt.aps.mp.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.api.domain.entity.MpFinalVersionStatisticsLog;
import com.zlt.aps.mp.api.domain.entity.MpMonthPlanStatistics;
import com.zlt.aps.mp.factory.mapper.MpFinalVersionStatisticsLogEntityMapper;
import com.zlt.aps.mp.factory.mapper.MpMonthPlanStatisticsEntityMapper;
import com.zlt.aps.mp.factory.service.IMpMonthPlanStatisticsService;
import com.zlt.aps.utils.BeanCopyUtils;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpMonthPlanStatisticsServiceImpl.java
 * 描    述：MpMonthPlanStatisticsServiceImplS2-0612.最终排产计划统计业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2026-02-05
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class MpMonthPlanStatisticsServiceImpl extends AbstractDocService<MpMonthPlanStatistics> implements IMpMonthPlanStatisticsService {

    private final MpMonthPlanStatisticsEntityMapper monthPlanStatisticsEntityMapper;

    private final MpFinalVersionStatisticsLogEntityMapper finalVersionStatisticsLogEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "s2-0612";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("s2-0612");
        return sysDocType;
    }

    @Override
    public String checkUnique(MpMonthPlanStatistics docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mpMonthPlanStatistics.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public void deleteMonthPlanStatisticsByCondition(String factoryCode, String year, String month, String productionVersion, String tempFlag, List<String> structureList) {
        monthPlanStatisticsEntityMapper.deleteMonthPlanStatisticsByCondition(factoryCode, year, month, productionVersion, tempFlag, structureList);
    }

    @Override
    public List<MpMonthPlanStatistics> getStatisticsInfo(FactoryMonthPlanMouldDayResult factoryMonthPlanMouldDayResult,
                                                         String productionVersion,
                                                         boolean isFinalAdjust) {
        if (isFinalAdjust) {
            return getStatisticsByLocal(factoryMonthPlanMouldDayResult, productionVersion);
        }
        List<MpMonthPlanStatistics> finalVersionList = getStatisticsByBackup(factoryMonthPlanMouldDayResult, productionVersion);
        if (CollectionUtils.isNotEmpty(finalVersionList)) {
            return finalVersionList;
        }
        return getStatisticsByLocal(factoryMonthPlanMouldDayResult, productionVersion);
    }

    @Override
    public Map<String, MpMonthPlanStatistics> getStatisticsInfo(String factoryCode, String productionVersion, boolean isFinalAdjust) {
        if (StringUtils.isBlank(factoryCode) || StringUtils.isBlank(productionVersion)) {
            return Collections.emptyMap();
        }
        FactoryMonthPlanMouldDayResult query = new FactoryMonthPlanMouldDayResult();
        query.setFactoryCode(factoryCode);
        List<MpMonthPlanStatistics> data = getStatisticsInfo(query, productionVersion, isFinalAdjust);
        if (CollectionUtils.isEmpty(data)) {
            return Collections.emptyMap();
        }
        return data.stream().collect(Collectors.toMap(MpMonthPlanStatistics::getStructureName, Function.identity(), (s1, s2) -> {
            String tempFlag1 = s1.getTempFlag();
            String tempFlag2 = s2.getTempFlag();
            if (!Objects.equals(tempFlag1, tempFlag2) && tempFlag2.equals(YesOrNoEnum.YES.getCode())) {
                return s2;
            }
            return s1;
        }));
    }

    /**
     * 获取排产统计信息，从t_mp_month_plan_statistics本身表获取
     *
     * @param factoryMonthPlanMouldDayResult 查询条件
     * @param productionVersion              排产版本号
     * @return
     */
    private List<MpMonthPlanStatistics> getStatisticsByLocal(FactoryMonthPlanMouldDayResult factoryMonthPlanMouldDayResult,
                                                             String productionVersion) {
        if (StringUtils.isBlank(productionVersion)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<MpMonthPlanStatistics> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MpMonthPlanStatistics::getFactoryCode, factoryMonthPlanMouldDayResult.getFactoryCode());
        queryWrapper.eq(MpMonthPlanStatistics::getIsDelete, YesOrNoEnum.NO.getValue());
        queryWrapper.eq(MpMonthPlanStatistics::getProductionVersion, productionVersion);
        queryWrapper.eq(StringUtils.isNoneEmpty(factoryMonthPlanMouldDayResult.getStructureName()), MpMonthPlanStatistics::getStructureName, factoryMonthPlanMouldDayResult.getStructureName());
        return monthPlanStatisticsEntityMapper.selectList(queryWrapper);
    }

    /**
     * 获取排产统计信息，从定稿备份表获取
     *
     * @param factoryMonthPlanMouldDayResult 查询条件
     * @param productionVersion              排产版本号
     * @return
     */
    private List<MpMonthPlanStatistics> getStatisticsByBackup(FactoryMonthPlanMouldDayResult factoryMonthPlanMouldDayResult,
                                                              String productionVersion) {
        if (StringUtils.isBlank(productionVersion)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<MpFinalVersionStatisticsLog> backUpWrapper = new LambdaQueryWrapper<>();
        backUpWrapper.eq(MpFinalVersionStatisticsLog::getFactoryCode, factoryMonthPlanMouldDayResult.getFactoryCode());
        backUpWrapper.eq(MpFinalVersionStatisticsLog::getIsDelete, YesOrNoEnum.NO.getValue());
        backUpWrapper.eq(MpFinalVersionStatisticsLog::getProductionVersion, productionVersion);
        backUpWrapper.eq(StringUtils.isNoneEmpty(factoryMonthPlanMouldDayResult.getStructureName()), MpFinalVersionStatisticsLog::getStructureName, factoryMonthPlanMouldDayResult.getStructureName());
        List<MpFinalVersionStatisticsLog> backUpList = finalVersionStatisticsLogEntityMapper.selectList(backUpWrapper);
        if (CollectionUtils.isEmpty(backUpList)) {
            return Collections.emptyList();
        }
        return BeanCopyUtils.copyBeanList(backUpList, MpMonthPlanStatistics.class);
    }
}

package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.redis.service.RedisService;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;

import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MpSimulatedResult;
import com.zlt.aps.monthplan.common.utils.AsyncService;
import com.zlt.aps.monthplan.common.utils.MonthCalculator;
import com.zlt.aps.monthplan.common.utils.poi.WorksheetData;
import com.zlt.aps.monthplan.demand.mapper.MpSimulatedResultEntityMapper;
import com.zlt.aps.monthplan.demand.service.IMpSimulatedResultService;

import com.zlt.aps.monthplan.factory.mapper.MpFactoryProductionVersionMapper;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProductionFinalResultService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.YearMonth;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpSimulatedResultServiceImpl.java
 * 描    述：MpSimulatedResultServiceImplS2-1004.实单模拟排产业务层处理
 *@author yelq
 *@date 2025-12-31
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
public class MpSimulatedResultServiceImpl extends AbstractDocService<MpSimulatedResult>  implements IMpSimulatedResultService {
    private final static String SHEET_NAME = "%d年%d月排产";
    private final MpFactoryProductionVersionMapper factoryProductionVersionMapper;
    private final AsyncService asyncService;
    private final MpSimulatedResultEntityMapper entityMapper;
    private final RedisService redisService;
    // 定稿的月度排产计划
    private final IFactoryMonthPlanProductionFinalResultService factoryMonthPlanProductionFinalResultService;

    @Override
    protected String getDocTypeCode() {
        return "2025123114";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("2025123114");
        return sysDocType;
    }

    @Override
    public String checkUnique(MpSimulatedResult docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mpSimulatedResult.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public void createVmMonthPrediction(MpSimulatedResult createCondition) {
      //   (1) 若 不存在T月月度计划，则提示"T月月度生产计划还未定稿，请先生成及定稿！"，系统不做任何处理。
      List<MpFactoryProductionVersion> finalVersions =  validateProductionVersionFinalized(createCondition);
      if (CollectionUtils.isEmpty(finalVersions)) {
        throw new BusinessException(I18nUtil.getMessage("ui.data.alert.productionPrediction.checkFinal"));
      }
      String key = ApsConstant.REDIS_CREATE_VM_MONTH_PREDICTION + createCondition.getFactoryCode()+createCondition.getYear()+createCondition.getMonth();
      if (ApsConstant.TRUE.equals(redisService.getCacheObject(key))) {
        throw new BusinessException(I18nUtil.getMessage("ui.data.alert.createVmMonthPrediction.run"));
      }
      redisService.setCacheObject(key, ApsConstant.TRUE, ApsConstant.EXPIRE_ONE, TimeUnit.HOURS);
      YearMonth tMonth = YearMonth.of(createCondition.getYear(), createCondition.getMonth());
      // 2、得到T月、T+1月、T+2月。T月 = 当前操作日所在年月(当月) +1 ；T+1月 = 在T月的基础上+1个月；T+2月 = 在T月的基础上+2个月
      MonthCalculator.MonthRangeResult monthRange = MonthCalculator.calculateMonthRanges(tMonth);
      MpFactoryProductionVersion finalVersion =  finalVersions.get(0);
      asyncService.executeAsyncTaskForSimulatedProduction(finalVersion,monthRange);
    }

  @Override
  public List<WorksheetData> listExportData(MpSimulatedResult queryVO, String fileName) {
      List<WorksheetData> result = Lists.newArrayList();
      List<MpSimulatedResult> list = this.entityMapper.listExportData(queryVO);
      result.add(this.buildSimulatedResult(fileName,list));
      List<FactoryMonthPlanMouldDayResult> mouldDayResults = null;
      if(CollectionUtils.isNotEmpty(list)){
          Set<String> monthPlanVersions = list.stream().map(MpSimulatedResult::getMonthPlanVersion).collect(Collectors.toSet());
          mouldDayResults = this.factoryMonthPlanProductionFinalResultService.listExportData(monthPlanVersions);
      }
      if(CollectionUtils.isNotEmpty(mouldDayResults)) {
          Map<String, List<FactoryMonthPlanMouldDayResult>> map = this.quickGroup(mouldDayResults);
          map.forEach((yearMonth, value) -> result.add(this.buildSimulatedResult(value)));
      }
      return result;
  }

  private WorksheetData buildSimulatedResult(List<FactoryMonthPlanMouldDayResult> value) {
      WorksheetData worksheetData = new WorksheetData();
      FactoryMonthPlanMouldDayResult mouldDayResult = value.get(0);
      worksheetData.setSheetName(String.format(SHEET_NAME, mouldDayResult.getYear(), mouldDayResult.getMonth()));
      worksheetData.setMouldDayResults(value);
      return worksheetData;
  }

  /**
   * 工具方法：快速获取排序后的分组数据
   */
  public  Map<String, List<FactoryMonthPlanMouldDayResult>> quickGroup(
      List<FactoryMonthPlanMouldDayResult> mouldDayResults) {

    return Optional.ofNullable(mouldDayResults)
        .orElse(Collections.emptyList())
        .stream()
        .sorted(Comparator
            .comparingInt(FactoryMonthPlanMouldDayResult::getYear)
            .thenComparingInt(FactoryMonthPlanMouldDayResult::getMonth))
        .collect(Collectors.groupingBy(
            FactoryMonthPlanMouldDayResult::getExportGroupKey,
            // 保持插入顺序
            java.util.LinkedHashMap::new,
            Collectors.toList()
        ));
  }

  private WorksheetData buildSimulatedResult(String fileName, List<MpSimulatedResult> list) {
    WorksheetData worksheetData = new WorksheetData();
    worksheetData.setSheetName(fileName);
    worksheetData.setSimulatedResults(list);
    return worksheetData;
  }

  /**
     *   3、检查是否已有T月月度计划(定稿)
     *       (1) 若 不存在T月月度计划，则提示"T月月度生产计划还未定稿，请先生成及定稿！"，系统不做任何处理。
     */
    private List<MpFactoryProductionVersion> validateProductionVersionFinalized(MpSimulatedResult createCondition) {
        return factoryProductionVersionMapper.selectList(
            Wrappers.<MpFactoryProductionVersion>lambdaQuery()
                .eq(MpFactoryProductionVersion::getFactoryCode,createCondition.getFactoryCode())
                .eq(MpFactoryProductionVersion::getYear, createCondition.getYear())
                .eq(MpFactoryProductionVersion::getMonth, createCondition.getMonth())
                .eq(MpFactoryProductionVersion::getIsFinal,YesOrNoEnum.YES.getCode())
        );
    }

}

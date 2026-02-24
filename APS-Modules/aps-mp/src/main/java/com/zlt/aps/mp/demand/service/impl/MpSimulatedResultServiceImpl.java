package com.zlt.aps.mp.demand.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.redis.service.RedisService;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.exception.BusinessException;

import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MpSimulatedResult;
import com.zlt.aps.mp.common.utils.AsyncService;
import com.zlt.aps.mp.common.utils.MonthCalculator;
import com.zlt.aps.mp.common.utils.poi.WorksheetData;
import com.zlt.aps.mp.demand.mapper.MpSimulatedResultEntityMapper;
import com.zlt.aps.mp.demand.service.IMpSimulatedResultService;

import com.zlt.aps.mp.factory.mapper.MpFactoryProductionVersionMapper;
import com.zlt.aps.mp.factory.service.IFactoryMonthPlanProductionFinalResultService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.transaction.annotation.Transactional;

import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

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
      log.info("createVmMonthPrediction: factoryCode={},year={},month={}", createCondition.getFactoryCode(), createCondition.getYear(), createCondition.getMonth());
      YearMonth tMonth = YearMonth.of(createCondition.getYear(), createCondition.getMonth());
      //   (1) 若 不存在T月月度计划，则提示"T月月度生产计划还未定稿，请先生成及定稿！"，系统不做任何处理。
      List<MpFactoryProductionVersion> finalVersions =  validateProductionVersionFinalized(tMonth);
      if (CollectionUtils.isEmpty(finalVersions)) {
        throw new BusinessException(I18nUtil.getMessage("ui.data.alert.productionPrediction.checkFinal"));
      }
      String keyForPrediction = ApsConstant.REDIS_CREATE_PRE_MONTH_PREDICTION;
      if (ApsConstant.TRUE.equals(redisService.getCacheObject(keyForPrediction))) {
        throw new BusinessException(I18nUtil.getMessage("ui.data.alert.createMonthPrediction.run"));
      }
      String keyForSimulated =  ApsConstant.REDIS_CREATE_VM_MONTH_PREDICTION;
      if (ApsConstant.TRUE.equals(redisService.getCacheObject(keyForSimulated))) {
        log.info("正在进行实单模拟排产，请稍候,分厂:{},年:{},月:{}",createCondition.getFactoryCode(),createCondition.getYear(),createCondition.getMonth());
        throw new BusinessException(I18nUtil.getMessage("ui.data.alert.createVmMonthPrediction.run"));
      }
      redisService.setCacheObject(keyForSimulated, ApsConstant.TRUE, ApsConstant.EXPIRE_ONE, TimeUnit.HOURS);
      MpFactoryProductionVersion finalVersion =  finalVersions.get(0);
      RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
      // 2、得到T月、T+1月、T+2月。T月 = 当前操作日所在年月(当月) +1 ；T+1月 = 在T月的基础上+1个月；T+2月 = 在T月的基础上+2个月
      MonthCalculator.MonthRangeResult monthRange = MonthCalculator.calculateMonthRanges(tMonth);
      asyncService.executeAsyncTaskForSimulatedProduction(finalVersion,monthRange,requestAttributes,SecurityUtils.getUsername());
    }

  @Override
  public List<WorksheetData> listExportData(MpSimulatedResult queryVO, String fileName) {
      List<WorksheetData> result = Lists.newArrayList();
      String   batchNumber  = getLatestBatchNumber(queryVO);
      queryVO.setMonthPlanVersion(batchNumber);
      List<MpSimulatedResult> list = this.entityMapper.listExportData(queryVO);
      if(CollectionUtils.isNotEmpty(list)){
        result.add(this.buildSimulatedResult(fileName,list));
      }
      if(StringUtils.isNotBlank(batchNumber)){
        this.factoryMonthPlanProductionFinalResultService.listExportData(queryVO,batchNumber,result);
      }
      return result;
  }

  @Override
  public String getLatestBatchNumber(MpSimulatedResult queryCondition) {
    List<MpSimulatedResult> list =  this.entityMapper.selectList(
        Wrappers.<MpSimulatedResult>lambdaQuery()
            .eq(MpSimulatedResult::getFactoryCode, queryCondition.getFactoryCode())
            .eq(MpSimulatedResult::getYear, queryCondition.getYear())
            .eq(MpSimulatedResult::getMonth, queryCondition.getMonth())
            .eq(MpSimulatedResult::getIsDelete,YesOrNoEnum.NO.getCode())
            .orderByDesc(MpSimulatedResult::getCreateTime)
    );
    if(CollectionUtils.isEmpty(list)) {
      return StringUtils.EMPTY;
    }
    return list.get(0).getMonthPlanVersion();
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
   * @param tMonth T月
   */
  private List<MpFactoryProductionVersion> validateProductionVersionFinalized(YearMonth tMonth) {
    return factoryProductionVersionMapper.selectList(
        Wrappers.<MpFactoryProductionVersion>lambdaQuery()
            .eq(MpFactoryProductionVersion::getFactoryCode, FactoryConstant.DEFAULT_FACTORY_CODE)
            .eq(MpFactoryProductionVersion::getYear, tMonth.getYear())
            .eq(MpFactoryProductionVersion::getMonth, tMonth.getMonthValue())
            .eq(MpFactoryProductionVersion::getIsFinal,YesOrNoEnum.YES.getCode())
    );
  }

}

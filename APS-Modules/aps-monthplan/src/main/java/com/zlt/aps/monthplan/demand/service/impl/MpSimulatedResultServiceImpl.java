package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;

import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MpSimulatedResult;
import com.zlt.aps.monthplan.common.utils.AsyncService;
import com.zlt.aps.monthplan.common.utils.MonthCalculator;
import com.zlt.aps.monthplan.demand.service.IMpSimulatedResultService;

import com.zlt.aps.monthplan.factory.mapper.MpFactoryProductionVersionMapper;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

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
    private final MpFactoryProductionVersionMapper factoryProductionVersionMapper;
    private final AsyncService asyncService;



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
    public void createVmMonthPrediction(MpSimulatedResult createCondition) throws Exception {
      YearMonth tMonth = YearMonth.of(createCondition.getYear(), createCondition.getMonth());
      // 2、得到T月、T+1月、T+2月。T月 = 当前操作日所在年月(当月) +1 ；T+1月 = 在T月的基础上+1个月；T+2月 = 在T月的基础上+2个月
      MonthCalculator.MonthRangeResult monthRange = MonthCalculator.calculateMonthRanges(tMonth);
      // 3、检查是否已有T月月度计划(定稿)
      //   (1) 若 不存在T月月度计划，则提示"T月月度生产计划还未定稿，请先生成及定稿！"，系统不做任何处理。
      List<MpFactoryProductionVersion> finalVersions =  validateProductionVersionFinalized(tMonth);
      if (CollectionUtils.isEmpty(finalVersions)) {
        throw new BusinessException(I18nUtil.getMessage("ui.data.alert.productionPrediction.checkFinal"));
      }
      MpFactoryProductionVersion finalVersion =  finalVersions.get(0);
      asyncService.executeAsyncTaskForSimulatedProduction(finalVersion,monthRange);
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

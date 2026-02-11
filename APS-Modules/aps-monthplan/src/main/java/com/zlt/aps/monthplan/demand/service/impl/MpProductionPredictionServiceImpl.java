package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.redis.service.RedisService;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.maindata.mapper.MpProductionPredictionEntityMapper;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.common.utils.AsyncService;
import com.zlt.aps.monthplan.common.utils.MonthCalculator;
import com.zlt.aps.monthplan.demand.service.IMpProductionPredictionService;
import com.zlt.aps.monthplan.factory.mapper.MpFactoryProductionVersionMapper;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpProductionPredictionServiceImpl.java
 * 描    述：MpProductionPredictionServiceImplS2-1002.未来产量预测业务层处理
 *@author yelq
 *@date 2025-12-28
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
public class MpProductionPredictionServiceImpl extends AbstractDocService<MpProductionPrediction>  implements IMpProductionPredictionService {
    private final MpProductionPredictionEntityMapper mpProductionPredictionEntityMapper;
    private final MpFactoryProductionVersionMapper factoryProductionVersionMapper;
    private final AsyncService asyncService;
    private final RedisService redisService;

    @Override
    protected String getDocTypeCode() {
        return "2025122822";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("2025122822");
        return sysDocType;
    }

    @Override
    public String checkUnique(MpProductionPrediction docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mpProductionPrediction.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public void createMonthPrediction(MpProductionPrediction createCondition){
        YearMonth tMonth = YearMonth.of(createCondition.getYear(), createCondition.getMonth());
        // 2、得到T月、T+1月、T+2月。T月 = 当前操作日所在年月(当月) +1 ；T+1月 = 在T月的基础上+1个月；T+2月 = 在T月的基础上+2个月
        MonthCalculator.MonthRangeResult monthRange = MonthCalculator.calculateMonthRanges(tMonth);
        // 3、检查是否已有T月月度计划(定稿)
        //   (1) 若 不存在T月月度计划，则提示"T月月度生产计划还未定稿，请先生成及定稿！"，系统不做任何处理。
        List<MpFactoryProductionVersion> finalVersions =  validateProductionVersionFinalized(tMonth);
        if (CollectionUtils.isEmpty(finalVersions)) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.productionPrediction.checkFinal"));
        }
        String key = ApsConstant.REDIS_CREATE_PRE_MONTH_PREDICTION + createCondition.getFactoryCode()+createCondition.getYear()+createCondition.getMonth();
        if (ApsConstant.TRUE.equals(redisService.getCacheObject(key))) {
            log.info("正在生成订单预测需求，请稍候,分厂:{},年:{},月:{}",createCondition.getFactoryCode(),createCondition.getYear(),createCondition.getMonth());
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.createMonthPrediction.run"));
        }
        redisService.setCacheObject(key, ApsConstant.TRUE, ApsConstant.EXPIRE_ONE, TimeUnit.HOURS);
        MpFactoryProductionVersion finalVersion =  finalVersions.get(0);
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        asyncService.executeAsyncTaskForPredictionProduction(finalVersion,monthRange,requestAttributes);
    }


    @Override
    public List<String> findPredictionVersion(MpProductionPrediction queryCondition) {
        LambdaQueryWrapper<MpProductionPrediction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MpProductionPrediction::getFactoryCode, queryCondition.getFactoryCode());
        wrapper.eq(MpProductionPrediction::getYear, queryCondition.getYear());
        wrapper.eq(MpProductionPrediction::getMonth, queryCondition.getMonth());
        wrapper.eq(MpProductionPrediction::getIsDelete, YesOrNoEnum.NO.getValue());
        wrapper.isNotNull(MpProductionPrediction::getPredictionVersion);
        wrapper.orderByDesc(MpProductionPrediction::getCreateTime);
        List<MpProductionPrediction> list =  this.mpProductionPredictionEntityMapper.selectList(wrapper);
        if(CollectionUtils.isEmpty(list)){
            return Collections.emptyList();
        }
        return list.stream().map(MpProductionPrediction::getPredictionVersion).distinct().collect(Collectors.toList());
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

package com.zlt.aps.mp.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.vo.CycleStructureMinLhMachineQtyVo;
import com.zlt.aps.mp.engine.mapper.FactoryMonthPlanProductLhCapacityMapper;
import com.zlt.aps.mp.engine.mapper.MonthPlanRequireMapper;
import com.zlt.aps.mp.engine.service.DpRequireDataService;
import com.zlt.aps.mp.api.domain.entity.DpDemandPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 排产调用数据获取服务类
 * 需求计划相关业务实现
 *
 * @author ZLT
 * @date 20251208
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DpRequireDataServiceImpl extends AbstractDataService implements DpRequireDataService {

    private final MonthPlanRequireMapper monthPlanRequireMapper;

    private final FactoryMonthPlanProductLhCapacityMapper factoryMonthPlanProductLhCapacityMapper;

    @Override
    public List<DpDemandPlan> getFactoryMonthPlan(Context context) {
        if (isEmptyFactoryAndRequireVersion(context)) {
            return Collections.emptyList();
        }
        QueryWrapper<DpDemandPlan> queryWrapper = new QueryWrapper();
        queryWrapper.eq("FACTORY_CODE", context.getFactoryCode());
        queryWrapper.eq("YEAR", context.getYear());
        queryWrapper.eq("MONTH", context.getMonth());
        if (StringUtils.isNotBlank(context.getMonthPlanVersion())) {
            queryWrapper.eq("MONTH_PLAN_VERSION", context.getMonthPlanVersion());
        }
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return monthPlanRequireMapper.selectList(queryWrapper);
    }

    @Override
    public List<CycleStructureMinLhMachineQtyVo> getCycleLhRatioInfo(Context context) {
        if (isEmptyFactoryAndYearMonth(context)) {
            return Collections.emptyList();
        }
        String factoryCode = context.getFactoryCode();
        Integer year = context.getYear();
        Integer month = context.getMonth();
        return factoryMonthPlanProductLhCapacityMapper.getCycleStructureMinLhRatioInfo(factoryCode, year, month);
    }

}

package com.zlt.aps.monthplan.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.utils.DateUtils;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.monthplan.api.domain.entity.FactoryProductionVersion;
import com.zlt.aps.monthplan.factory.mapper.FactoryProductionVersionMapper;
import com.zlt.aps.monthplan.factory.service.IFactoryProductionVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Date;

/**
 * 分厂排产版本 业务实现
 *
 * @author ZLT
 * @date 20250526
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FactoryProductionVersionServiceImpl implements IFactoryProductionVersionService {

    private final FactoryProductionVersionMapper factoryProductionVersionMapper;

    private final IFactoryParamService factoryParamService;

    @Override
    public void setProductionVersionCycleDate(FactoryProductionVersion factoryProductionVersion) {
        if (null == factoryProductionVersion) {
            return;
        }
        Integer year = factoryProductionVersion.getYear();
        Integer month = factoryProductionVersion.getMonth();
        if (null == year || null == month) {
            return;
        }
        //默认为自然月的起始日
        YearMonth yearMonth = YearMonth.of(year, month);
        factoryProductionVersion.setIsNaturalMonth(YesOrNoEnum.YES.getValue());
        factoryProductionVersion.setProductionStartDate(DateUtils.getDate(yearMonth.atDay(FactoryConstant.MONTH_START_DAY)));
        factoryProductionVersion.setProductionEndDate(DateUtils.getDate(yearMonth.atEndOfMonth()));
        String factoryCode = factoryProductionVersion.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            return;
        }
        Integer startDay = factoryParamService.getMonthStartDay(factoryCode);
        if (null == startDay) {
            return;
        }
        //值在[2,28]之外，则默认为自然月
        if (startDay <= FactoryConstant.MONTH_START_DAY || startDay > FactoryConstant.NO_NATURAL_MONTH_MAX_VALUE) {
            return;
        }
        //值在2~28之间，则为非自然月
        factoryProductionVersion.setIsNaturalMonth(YesOrNoEnum.NO.getValue());
        YearMonth previousMonth = yearMonth.minusMonths(1);
        Date cycleStartDate = DateUtils.getDate(LocalDate.of(previousMonth.getYear(), previousMonth.getMonthValue(), startDay));
        factoryProductionVersion.setProductionStartDate(cycleStartDate);
        Date cycleEndDate = DateUtils.getDate(LocalDate.of(year, month, startDay - 1));
        factoryProductionVersion.setProductionEndDate(cycleEndDate);
    }

    /**
     * 根据分厂编码，及日期，获取定稿版本信息
     *
     * @param factoryCode 分厂编码
     * @param date        日期
     */
    @Override
    public FactoryProductionVersion getFinalVersion(String factoryCode, Date date) {
        if (StringUtils.isBlank(factoryCode) || null == date) {
            return null;
        }
        //根据分厂，及日期确定排产版本计划
        QueryWrapper<FactoryProductionVersion> productionVersionQueryWrapper = new QueryWrapper<>();
        productionVersionQueryWrapper.eq("FACTORY_CODE", factoryCode);
        productionVersionQueryWrapper.le("PRODUCTION_START_DATE", date);
        productionVersionQueryWrapper.ge("PRODUCTION_END_DATE", date);
        productionVersionQueryWrapper.eq("IS_FINAL", YesOrNoEnum.YES.getValue());
        productionVersionQueryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return factoryProductionVersionMapper.selectOne(productionVersionQueryWrapper);
    }

    @Override
    public FactoryProductionVersion getProductionVersion(String productionVersion) {
        if (StringUtils.isBlank(productionVersion)) {
            return null;
        }
        //排产版本号确定排产版本计划
        QueryWrapper<FactoryProductionVersion> productionVersionQueryWrapper = new QueryWrapper<>();
        productionVersionQueryWrapper.eq("PRODUCTION_VERSION", productionVersion);
        productionVersionQueryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return factoryProductionVersionMapper.selectOne(productionVersionQueryWrapper);
    }

    /**
     * 根据分厂编码，及日期，获取定稿版本信息
     *
     * @param factoryCode 分厂编码
     * @param year        年
     * @param month       月
     */
    @Override
    public FactoryProductionVersion getFinalVersionByYearMonth(String factoryCode, Integer year, Integer month) {
        if (StringUtils.isBlank(factoryCode) || null == year || null == month) {
            return null;
        }
        //根据分厂，及年、月确定排产版本计划
        QueryWrapper<FactoryProductionVersion> productionVersionQueryWrapper = new QueryWrapper<>();
        productionVersionQueryWrapper.eq("FACTORY_CODE", factoryCode);
        productionVersionQueryWrapper.eq("YEAR", year);
        productionVersionQueryWrapper.eq("MONTH", month);
        productionVersionQueryWrapper.eq("IS_FINAL", YesOrNoEnum.YES.getValue());
        productionVersionQueryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return factoryProductionVersionMapper.selectOne(productionVersionQueryWrapper);
    }
}

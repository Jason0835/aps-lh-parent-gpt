package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.utils.BeanCopyUtils;
import com.zlt.aps.utils.ProductionCalendarHelper;
import com.zlt.aps.utils.YearMonthUtils;
import com.zlt.aps.maindata.mapper.MdmProductionCalendarEntityMapper;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.service.IMdmProductionCalendarService;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductionCalendar;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmProductionCalendarServiceImpl.java
 * 描    述：MdmProductionCalendarServiceImpl生产日历业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MdmProductionCalendarServiceImpl implements IMdmProductionCalendarService {

    private final MdmProductionCalendarEntityMapper mdmProductionCalendarEntityMapper;

    private final IFactoryParamService factoryParamService;

    /**
     * 查询生产日历
     *
     * @param id 生产日历主键
     * @return 生产日历
     */
    @Override
    public MdmProductionCalendar selectMdmProductionCalendarById(Long id) {
        return mdmProductionCalendarEntityMapper.selectById(id);
    }

    /**
     * 查询生产日历列表
     *
     * @param mdmProductionCalendar 生产日历
     * @return 生产日历
     */
    @Override
    public List<MdmProductionCalendar> selectMdmProductionCalendarList(MdmProductionCalendar mdmProductionCalendar) {
        LambdaQueryWrapper<MdmProductionCalendar> wrapper = buildWrapper(mdmProductionCalendar);
        return mdmProductionCalendarEntityMapper.selectList(wrapper);
    }

    @Override
    public Set<Integer> getStopDays(String factoryCode, Integer year, Integer month) {
        Integer cycleStartDay = factoryParamService.getMonthStartDay(factoryCode, ProductTypeEnum.WHOLE_STEEL);
        boolean isNaturalMonth = YearMonthUtils.isNaturalMonth(cycleStartDay);
        LocalDate monthDate = LocalDate.of(year, month, FactoryConstant.MONTH_START_DAY);
        Integer monthDays = monthDate.with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth();
        LocalDate monthLastDate = LocalDate.of(year, month, monthDays);
        Date cycleStartDate = YearMonthUtils.getDate(monthDate);
        Date cycleEndDate = YearMonthUtils.getDate(monthLastDate);
        //非自然月
        if (!isNaturalMonth) {
            LocalDate previousMonth = YearMonthUtils.getPreviousMonth(year, month);
            LocalDate previousMonthStart = LocalDate.of(previousMonth.getYear(), previousMonth.getMonthValue(), cycleStartDay);
            cycleStartDate = YearMonthUtils.getDate(previousMonthStart);
            LocalDate yearMonthEnd = LocalDate.of(year, month, cycleStartDay - BigDecimal.ONE.intValue());
            cycleEndDate = YearMonthUtils.getDate(yearMonthEnd);
        }
        //停工日配置获取
        List<MdmProductionCalendar> calendarList = getDateRangeCalendarList(factoryCode, cycleStartDate, cycleEndDate);
        if (CollectionUtils.isEmpty(calendarList)) {
            return Collections.emptySet();
        }
        Set<Integer> stopList = YearMonthUtils.calculateStopDays(BeanCopyUtils.copyBeanList(calendarList, ProductionCalendarHelper.class), cycleStartDate, cycleEndDate);
        return stopList;
    }

    @Override
    public Integer getMonthMaxDays(String factoryCode, Integer year, Integer month) {
        Integer cycleStartDay = factoryParamService.getMonthStartDay(factoryCode, ProductTypeEnum.WHOLE_STEEL);
        boolean isNaturalMonth = YearMonthUtils.isNaturalMonth(cycleStartDay);
        LocalDate monthDate = LocalDate.of(year, month, FactoryConstant.MONTH_START_DAY);
        Integer monthDays = monthDate.with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth();
        LocalDate monthLastDate = LocalDate.of(year, month, monthDays);
        Date cycleStartDate = YearMonthUtils.getDate(monthDate);
        Date cycleEndDate = YearMonthUtils.getDate(monthLastDate);
        //非自然月
        if (!isNaturalMonth) {
            LocalDate previousMonth = YearMonthUtils.getPreviousMonth(year, month);
            LocalDate previousMonthStart = LocalDate.of(previousMonth.getYear(), previousMonth.getMonthValue(), cycleStartDay);
            cycleStartDate = YearMonthUtils.getDate(previousMonthStart);
            LocalDate yearMonthEnd = LocalDate.of(year, month, cycleStartDay - BigDecimal.ONE.intValue());
            cycleEndDate = YearMonthUtils.getDate(yearMonthEnd);
        }
        Integer monthMaxDays = YearMonthUtils.getDifferenceDays(cycleStartDate, cycleEndDate);
        //停工日配置获取
        List<MdmProductionCalendar> calendarList = getDateRangeCalendarList(factoryCode, cycleStartDate, cycleEndDate);
        if (CollectionUtils.isEmpty(calendarList)) {
            return monthMaxDays;
        }
        Set<Integer> stopList = YearMonthUtils.calculateStopDays(BeanCopyUtils.copyBeanList(calendarList, ProductionCalendarHelper.class), cycleStartDate, cycleEndDate);
        if (CollectionUtils.isEmpty(stopList)) {
            return monthMaxDays;
        }
        return monthMaxDays - stopList.size();
    }

    @Override
    public Integer getMonthDays(String factoryCode, Integer year, Integer month) {
        Integer cycleStartDay = factoryParamService.getMonthStartDay(factoryCode, ProductTypeEnum.WHOLE_STEEL);
        boolean isNaturalMonth = YearMonthUtils.isNaturalMonth(cycleStartDay);
        LocalDate monthDate = LocalDate.of(year, month, FactoryConstant.MONTH_START_DAY);
        Integer monthDays = monthDate.with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth();
        LocalDate monthLastDate = LocalDate.of(year, month, monthDays);
        Date cycleStartDate = YearMonthUtils.getDate(monthDate);
        Date cycleEndDate = YearMonthUtils.getDate(monthLastDate);
        //非自然月
        if (!isNaturalMonth) {
            LocalDate previousMonth = YearMonthUtils.getPreviousMonth(year, month);
            LocalDate previousMonthStart = LocalDate.of(previousMonth.getYear(), previousMonth.getMonthValue(), cycleStartDay);
            cycleStartDate = YearMonthUtils.getDate(previousMonthStart);
            LocalDate yearMonthEnd = LocalDate.of(year, month, cycleStartDay - BigDecimal.ONE.intValue());
            cycleEndDate = YearMonthUtils.getDate(yearMonthEnd);
        }
        return YearMonthUtils.getDifferenceDays(cycleStartDate, cycleEndDate);
    }

    @Override
    public List<MdmProductionCalendar> getDateRangeCalendarList(String factoryCode, Date startDate, Date endDate) {
        QueryWrapper<MdmProductionCalendar> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        queryWrapper.ge("BEGIN_DATE", startDate);
        queryWrapper.le("END_DATE", endDate);
        return mdmProductionCalendarEntityMapper.selectList(queryWrapper);
    }

    private LambdaQueryWrapper<MdmProductionCalendar> buildWrapper(MdmProductionCalendar mdmProductionCalendar) {
        LambdaQueryWrapper<MdmProductionCalendar> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(StringUtils.isNotBlank(mdmProductionCalendar.getFactoryCode()), MdmProductionCalendar::getFactoryCode, mdmProductionCalendar.getFactoryCode());
        wrapper.eq(mdmProductionCalendar.getYear() != null, MdmProductionCalendar::getYear, mdmProductionCalendar.getYear());
        wrapper.eq(mdmProductionCalendar.getMonth() != null, MdmProductionCalendar::getMonth, mdmProductionCalendar.getMonth());
        return wrapper;
    }

    /**
     * 新增生产日历
     *
     * @param mdmProductionCalendar 生产日历
     * @return 结果
     */
    @Override
    public int insertMdmProductionCalendar(MdmProductionCalendar mdmProductionCalendar) {
        checkCross(mdmProductionCalendar);
        return mdmProductionCalendarEntityMapper.insert(mdmProductionCalendar);
    }

    /**
     * 校验时间交叉
     */
    private void checkCross(MdmProductionCalendar mdmProductionCalendar) {
        // 开始时间和结束时间也不能出现交叉
        Long count = mdmProductionCalendarEntityMapper.selectCount(Wrappers.lambdaQuery(MdmProductionCalendar.class)
                .ne(mdmProductionCalendar.getId() != null, MdmProductionCalendar::getId, mdmProductionCalendar.getId())
                .eq(MdmProductionCalendar::getFactoryCode, mdmProductionCalendar.getFactoryCode())
                .eq(MdmProductionCalendar::getYear, mdmProductionCalendar.getYear())
                .eq(MdmProductionCalendar::getMonth, mdmProductionCalendar.getMonth())
                .ge(MdmProductionCalendar::getEndDate, mdmProductionCalendar.getBeginDate())
                .le(MdmProductionCalendar::getBeginDate, mdmProductionCalendar.getEndDate())
        );
        if (count > 0) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.column.mdmProductionCalendar.crossCheck"));
        }
    }

    /**
     * 修改生产日历
     *
     * @param mdmProductionCalendar 生产日历
     * @return 结果
     */
    @Override
    public int updateMdmProductionCalendar(MdmProductionCalendar mdmProductionCalendar) {
        checkCross(mdmProductionCalendar);
        return mdmProductionCalendarEntityMapper.updateById(mdmProductionCalendar);
    }

    /**
     * 批量删除生产日历
     *
     * @param ids 需要删除的生产日历主键
     * @return 结果
     */
    @Override
    public int deleteMdmProductionCalendarByIds(Long[] ids) {
        return mdmProductionCalendarEntityMapper.deleteBatchIds(Arrays.asList(ids));
    }

    /**
     * 校验生产日历唯一性
     */
    @Override
    public String checkMdmProductionCalendarUnique(MdmProductionCalendar mdmProductionCalendar) {
        if (mdmProductionCalendar == null) {
            return UserConstants.NOT_UNIQUE;
        }
        LambdaQueryWrapper<MdmProductionCalendar> wrapper = Wrappers.lambdaQuery();
        wrapper.ne(mdmProductionCalendar.getId() != null, MdmProductionCalendar::getId, mdmProductionCalendar.getId());
        wrapper.eq(StringUtils.isNotBlank(mdmProductionCalendar.getFactoryCode()), MdmProductionCalendar::getFactoryCode, mdmProductionCalendar.getFactoryCode());
        wrapper.eq(mdmProductionCalendar.getYear() != null, MdmProductionCalendar::getYear, mdmProductionCalendar.getYear());
        wrapper.eq(mdmProductionCalendar.getMonth() != null, MdmProductionCalendar::getMonth, mdmProductionCalendar.getMonth());
        wrapper.eq(mdmProductionCalendar.getBeginDate() != null, MdmProductionCalendar::getBeginDate, mdmProductionCalendar.getBeginDate());
        wrapper.eq(mdmProductionCalendar.getEndDate() != null, MdmProductionCalendar::getEndDate, mdmProductionCalendar.getEndDate());

        if (mdmProductionCalendarEntityMapper.selectCount(wrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

}

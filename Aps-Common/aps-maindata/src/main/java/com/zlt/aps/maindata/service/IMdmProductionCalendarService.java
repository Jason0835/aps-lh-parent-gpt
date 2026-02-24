package com.zlt.aps.maindata.service;

import com.zlt.aps.monthplan.api.domain.entity.MdmProductionCalendar;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmProductionCalendarService.java
 * 描    述：IMdmProductionCalendarService生产日历后端接口
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
public interface IMdmProductionCalendarService {
    /**
     * 查询生产日历
     *
     * @param id 生产日历主键
     * @return 生产日历
     */
    MdmProductionCalendar selectMdmProductionCalendarById(Long id);

    /**
     * 查询生产日历列表
     *
     * @param mdmProductionCalendar 生产日历
     * @return 生产日历集合
     */
    List<MdmProductionCalendar> selectMdmProductionCalendarList(MdmProductionCalendar mdmProductionCalendar);

    /**
     * 获取停开工在某个月份的周期天数
     *
     * @param factoryCode 分厂编码
     * @param year        年
     * @param month       月
     * @return
     */
    Set<Integer> getStopDays(String factoryCode, Integer year, Integer month);

    /**
     * 根据分厂，年、月获取该分厂在指定年、月的最大天数
     * 剔除了停工日
     *
     * @param factoryCode 分厂编码
     * @param year        年
     * @param month       月
     * @return
     */
    Integer getMonthMaxDays(String factoryCode, Integer year, Integer month);

    /**
     * 根据分厂，年、月获取该分厂在指定年、月的最大天数
     *
     *
     * @param factoryCode 分厂编码
     * @param year        年
     * @param month       月
     * @return
     */
    Integer getMonthDays(String factoryCode, Integer year, Integer month);
    /**
     * 查询生产日历列表
     *
     * @param factoryCode 分厂编码
     * @param startDate   开始时间
     * @param endDate     结束时间
     * @return 生产日历集合
     */
    List<MdmProductionCalendar> getDateRangeCalendarList(String factoryCode, Date startDate, Date endDate);

    /**
     * 新增生产日历
     *
     * @param mdmProductionCalendar 生产日历
     * @return 结果
     */
    @Transactional
    int insertMdmProductionCalendar(MdmProductionCalendar mdmProductionCalendar);

    /**
     * 修改生产日历
     *
     * @param mdmProductionCalendar 生产日历
     * @return 结果
     */
    @Transactional
    int updateMdmProductionCalendar(MdmProductionCalendar mdmProductionCalendar);

    /**
     * 批量删除生产日历
     *
     * @param ids 需要删除的生产日历主键集合
     * @return 结果
     */

    @Transactional
    int deleteMdmProductionCalendarByIds(Long[] ids);


    /**
     * 校验生产日历唯一性
     */
    String checkMdmProductionCalendarUnique(MdmProductionCalendar mdmProductionCalendar);
}

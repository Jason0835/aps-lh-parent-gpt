package com.zlt.aps.mp.factory.service;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.dto.FactoryMonthPlanProdFinalQueryDto;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanRequireStock;
import com.zlt.aps.monthplan.api.domain.vo.*;

import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IFactoryMonthPlanProdFinalService.java
 * 描    述：IFactoryMonthPlanProdFinalService分厂月生产计划排产结果-生产计划排产结果后端接口
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-14
 */
@Deprecated
public interface IFactoryMonthPlanProdFinalService {

    /**
     * 根据查询条件，获取列表数据
     * 不对开始日期和结束日期进行处理
     *
     * @param queryWrapper
     * @return
     */
    List<FactoryMonthPlanProdFinal> getList(Wrapper<FactoryMonthPlanProdFinal> queryWrapper);

    /**
     * 根据查询条件，获取列表数据
     * 通过isHandler觉得是否对开始日期和结束日期进行处理
     * true表示需要处理，false表示不处理
     *
     * @param queryWrapper
     * @param isHandler    是否对开始日期和结束日期处理
     * @return
     */
    List<FactoryMonthPlanProdFinal> getList(Wrapper<FactoryMonthPlanProdFinal> queryWrapper, boolean isHandler);

    /**
     * 根据查询条件，获取某日的月计划排产数据
     *
     * @param queryCondition 查询条件
     * @return
     */
    List<FactoryMonthPlanDayProductionInfoVo> getMonthPlanDayProductionInfo(FactoryMonthPlanProdFinalQueryDto queryCondition);

    /**
     * 定稿 - 年月+分厂+需求计划版本+分厂月计划版本
     *
     * @param factoryMonthPlanProdFinal
     * @return
     */
    AjaxResult finalized(FactoryMonthPlanProdFinal factoryMonthPlanProdFinal);

    /**
     * 根据分厂、年份、月份得到分厂版本信息对象
     *
     * @param factoryCode
     * @param year
     * @param month
     * @return
     */
    FactoryMonthPlanFinalVersionInfoVo getFinalVersionInfo(String factoryCode, Integer year, Integer month);

    /**
     * 根据分厂及日期，得到分厂版本信息对象
     *
     * @param factoryCode 分厂编码
     * @param date        日期
     * @return
     */
    FactoryMonthPlanFinalVersionInfoVo getFinalVersionInfoByDate(String factoryCode, Date date);

    /**
     * 导入列表
     *
     * @param list
     * @param updateSupport 是否覆盖更新 true表示更新，false表示先删除后插入
     * @param importLogId   导入日志ID
     * @return
     */
    AjaxResult doImportData(List<FactoryMonthPlanProdFinal> list, boolean updateSupport, long importLogId);

    /**
     * 定稿调整-更新月度外胎汇总
     *
     * @param finalList 定稿的调整记录
     */
    void finalUpdatePlanSurplusList(List<FactoryMonthPlanProdFinal> finalList);

    /**
     * 统计分厂月生产计划排产结果-排产结果列表
     */
    MonthPlanStatisticsVo statistics(FactoryMonthPlanProdFinal prodFinal);

    /**
     * 构建查询条件
     */
    void builderCondition(QueryWrapper<?> queryWrapper, FactoryMonthPlanProdFinal queryVO);

    /**
     * 根据版本计划，统计日排产信息，包含日排产规格数及日排产总量
     *
     * @param query
     * @return
     */
    List<DayProductionTotalVo> statisticsDay(FactoryMonthPlanProdFinal query);

    /**
     * 获取月份排产模式--Date 不为空则表示非自然月排产，Date为空表示自然月排产
     *
     * @param query
     * @return
     */
    FactoryMonthPlanTypeVo getProductionMonthType(FactoryMonthPlanProdFinal query);

    List<MonthPlanRequireStock> getSaleMonthPlanRequireStock(String monthPlanVersion);
}

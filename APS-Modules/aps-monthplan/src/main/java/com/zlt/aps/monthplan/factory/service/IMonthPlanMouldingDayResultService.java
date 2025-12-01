package com.zlt.aps.monthplan.factory.service;


import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.dto.ChangeSpecCodeMouldingDayResultParam;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanMouldingDayResult;
import com.zlt.aps.monthplan.api.domain.vo.DayProductionTotalVo;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanDayResultStatisticsVo;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanMouldingDayResultVo;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanStatisticsVo;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMonthPlanMouldingDayResultService.java
 * 描    述：IMonthPlanMouldingDayResultService分厂月生产计划排产过程-模具排产结果汇总后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-17
 */
public interface IMonthPlanMouldingDayResultService {

    /**
     * 查询列表
     *
     * @param queryVO 查询条件
     * @return
     */
    List<MonthPlanMouldingDayResult> selectList(MonthPlanMouldingDayResult queryVO);

    /**
     * 查询列表
     *
     * @param queryVO   查询条件
     * @param isHandler 是否需要对开始日期和结束日期处理展现 true 表示需要处理 false表示不处理
     * @return
     */
    List<MonthPlanMouldingDayResult> selectList(MonthPlanMouldingDayResult queryVO, boolean isHandler);

    /**
     * 导入列表
     *
     * @param list
     * @param updateSupport 是否覆盖更新 true表示更新，false表示先删除后插入
     * @param importLogId   导入日志ID
     * @return
     */
    AjaxResult doImportData(List<MonthPlanMouldingDayResult> list, boolean updateSupport, long importLogId);

    /**
     * 查询对应年月+分厂+需求计划版本的分厂月计划版本
     *
     * @param query 查询条件
     * @return
     */
    List<String> productionVersionList(MonthPlanMouldingDayResult query);

    /**
     * 对排产计划进行硫化规格切换--背后的业务为切换成型法
     *
     * @param changeParam 需切换的计划
     * @return
     */
    AjaxResult changePlanSpecCode(ChangeSpecCodeMouldingDayResultParam changeParam);

    /**
     * 统计分厂月生产计划排产
     */
    MonthPlanStatisticsVo statistics(MonthPlanMouldingDayResult queryVO);

    /**
     * 根据版本计划，统计日排产信息，包含日排产规格数及日排产总量
     *
     * @param productionVersion
     * @return
     */
    List<DayProductionTotalVo> statisticsDay(String productionVersion);

    /**
     * 查询分厂月生产计划合并SKU-合并SKU
     *
     * @param queryVO 查询条件
     * @return 列表
     */
    List<MonthPlanMouldingDayResultVo> listFacProduct(MonthPlanMouldingDayResult queryVO);

    /**
     * 查询月计划排产统计列表
     *
     * @param queryVO 查询条件
     * @return 列表
     */
    List<MonthPlanDayResultStatisticsVo> listFacProductStatistics(MonthPlanMouldingDayResult queryVO);
}

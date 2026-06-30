package com.zlt.aps.mp.factory.service;


import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mp.api.domain.entity.*;
import com.zlt.aps.mp.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.mp.common.utils.poi.WorksheetData;
import com.zlt.bill.common.service.IDocService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IFactoryMonthPlanProductionFinalResultService.java
 * 描    述：IFactoryMonthPlanProductionFinalResultService工厂月生产计划-最终排产计划定稿后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-23
 */
public interface IFactoryMonthPlanProductionFinalResultService extends IDocService<FactoryMonthPlanProductionFinalResult> {

    /**
     * 根据条件，列表查询
     *
     * @param condition
     * @return
     */
    List<FactoryMonthPlanProductionFinalResult> getDataList(FactoryMonthPlanProductionFinalResult condition);

    /**
     * 8、12个月结构上机频次 = 从定稿的月度排产计划，获取近12个月的已排产的月份个数
     *
     * @return 定稿的月度排产计划
     */
    Map<String, Integer> calculateStructureFrequency();

    /**
     * 根据物料编号,通过月度生产计划表，获取近12个月有排产的月份个数
     *
     * @param materialCode 物料编号
     * @return 近12个月有排产的月份个数
     */
    int calculateStructureFrequency(String materialCode);

    /**
     * 库存抓取日~（同月）月底的月度计划量汇总并保存
     *
     * @param requireVersion        需求版本号
     * @param finishedProductStocks 成品库存
     * @param materialInfoMap 物料信息
     * @return 月度计划量汇总
     */
    Map<String, Integer> calculateMonthSurplus(String requireVersion, List<MdmProductStock> finishedProductStocks,Map<String, MdmMaterialInfo> materialInfoMap);

    /**
     * 库存抓取日~（同月）月底的月度计划量汇总不保存
     *
     * @param finishedProductStocks 成品库存
     * @param yearMonth
     * @param days
     * @return 月度计划量汇总
     */
    Map<String, Integer> calculateMonthSurplusNoSave(List<MdmProductStock> finishedProductStocks, String yearMonth, int days);

    /**
     * 获取最终排产结果
     *
     * @param finalVersion
     * @return
     */
    List<FactoryMonthPlanMouldDayResult> findProductionFinalResult(MpFactoryProductionVersion finalVersion);
    /**
     * 查询最终排产结果
     * @param monthPlanVersions
     * @return
     */
    List<FactoryMonthPlanMouldDayResult> findProductionFinalResult(MpFactoryProductionVersion currentFinalVersion, Set<String> monthPlanVersions);

    /**
     * 定稿
     *
     * @param factoryMonthPlanProdFinal 分厂年月
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    AjaxResult finalized(FactoryMonthPlanProductionFinalResult factoryMonthPlanProdFinal);

    /**
     * 获取定稿排产结果
     * @param finalVersion
     * @return
     */
    List<FactoryMonthPlanMouldDayResult> findFinalProductionResult(MpFactoryProductionVersion finalVersion);

    /**
     * 获取定稿版本的月度计划
     * @param param
     * @return
     */
    List<FactoryMonthPlanProductionFinalResult> listMonthProdFinalPlans(FactoryMonthPlanProductionFinalResult param);
    /**
     * 库存抓取日~（同月）月底的月度计划量汇总不保存
     * @param factoryCode 分厂
     * @param skus 物料
     * @return 月度计划量汇总
     */
    Map<String, Integer> calculateStructureFrequency(String factoryCode, Set<String> skus);
    /**
     *  实单模拟导出排产数据
     */
    void listExportData(MpSimulatedResult queryVO, String   batchNumber,List<WorksheetData> result);

    /**
     * 下发月计划
     *
     * @param factoryMonthPlanProdFinal 参数
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    AjaxResult issueMonthPlan(FactoryMonthPlanProductionFinalResult factoryMonthPlanProdFinal);

    /**
     * 导入定稿
     * @param list 列表数据
     * @param updateSupport 覆盖
     * @param importLogId 导入日志ID
     * @param params 导入参数
     * @return 结果
     */
    AjaxResult importDataFinalResult(List<FactoryMonthPlanProductionFinalResult> list, boolean updateSupport, Long importLogId, FactoryMonthPlanProductionFinalResult params);

    /**
     * 根据条件，列表查询
     *
     * @param condition 查询条件
     * @return 结果
     */
    List<FactoryMonthPlanFinalAdjustVo> list4Adjust(FactoryMonthPlanProductionFinalResult condition);

    /**
     * 定时计算上月超欠产（每月1号凌晨3点触发）
     * 根据上月计划排产量和上月硫化日完成量(合格品)计算超欠产，
     * 并按阈值参数(SYS0206009)判定上月超欠产有效标志：
     * |超欠产值|(绝对值) > 阈值 → 否('0')，否则 → 是('1')
     *
     * @return 计算结果
     */
    AjaxResult calcLastMonthOverProd();

    /**
     * 计算当月超欠产写入下月（月末倒数2天触发）
     * 用当月数据写入下月月计划的"上月超欠产"栏位
     *
     * @return 计算结果
     */
    AjaxResult calcCurrentMonthOverProdForNextMonth();

}

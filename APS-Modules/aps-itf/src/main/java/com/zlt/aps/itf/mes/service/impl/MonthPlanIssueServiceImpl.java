package com.zlt.aps.itf.mes.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.itf.constant.SysCode;
import com.zlt.aps.itf.mes.enums.ItfSyncKeyEnum;
import com.zlt.aps.itf.mes.mapper.MonthPlanIssueEntityMapper;
import com.zlt.aps.itf.mes.service.IMonthPlanIssueService;
import com.zlt.aps.itf.vo.CxMonthPlanIssue;
import com.zlt.aps.itf.vo.MonthPlanIssue;
import com.zlt.aps.itf.vo.SyncDataLogs;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.sync.handle.SyncDataHandle;
import com.zlt.sync.povo.SyncParamsVO;
import com.zlt.sync.service.SyncDataLogsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanIssueServiceImpl.java
 * 描    述：MonthPlanIssueServiceImpl月计划下发业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-24
 */
@Slf4j
@Service
public class MonthPlanIssueServiceImpl implements IMonthPlanIssueService {

    @Autowired
    private SyncDataHandle syncDataHandle;

    @Autowired
    private SyncDataLogsService syncDataLogsService;

    @Autowired
    private MonthPlanIssueEntityMapper monthPlanIssueEntityMapper;

    /**
     * 月计划下发写入MES中间库的单批处理数量，避免单次SQL过长。
     */
    private static final int MONTH_PLAN_ISSUE_BATCH_SIZE = 30;

    /**
     * 根据物料粒度的月计划汇总结果生成成型月计划中间表数据。
     *
     * @param groupMap 按工厂、年月、物料编码汇总的月计划结果
     * @param cxMonthPlanIssuesList 成型月计划下发数据
     * @param dataVersion 接口数据版本号
     * @return 无
     * @throws RuntimeException 月计划字段读取异常时抛出
     */
    private void genCxMonthPlanIssuesList(Map<String, FactoryMonthPlanProductionFinalResult> groupMap,
                                          List<CxMonthPlanIssue> cxMonthPlanIssuesList, String dataVersion) {
        Set<Map.Entry<String, FactoryMonthPlanProductionFinalResult>> entrySet = groupMap.entrySet();
        for (Map.Entry<String, FactoryMonthPlanProductionFinalResult> entry : entrySet) {
            FactoryMonthPlanProductionFinalResult value = entry.getValue();
            CxMonthPlanIssue cxMonthPlanIssue = new CxMonthPlanIssue();
            cxMonthPlanIssue.setYear(value.getYear());
            cxMonthPlanIssue.setMonth(value.getMonth());
            cxMonthPlanIssue.setEmbryoCode(value.getEmbryoCode());
            cxMonthPlanIssue.setEmbryoDesc(value.getMainMaterialDesc());
            cxMonthPlanIssue.setConstructionStage(value.getConstructionStage());
            cxMonthPlanIssue.setDataVersion(dataVersion);
            Map<String, Object> params = value.getParams();
            BigDecimal totalDayResultOld = (BigDecimal) params.get("totalDayResult");
            cxMonthPlanIssue.setDemandQty(totalDayResultOld);
            cxMonthPlanIssuesList.add(cxMonthPlanIssue);
        }
    }

    /**
     * 下发月计划
     *
     * @param monthPlanIssueList 列表
     * @return 列表
     */
    @Override
    public AjaxResult issueMonthPlan(List<FactoryMonthPlanProductionFinalResult> monthPlanIssueList) {
        if (CollectionUtils.isEmpty(monthPlanIssueList)) {
            return AjaxResult.success();
        }
        // 获取下发接口版本号
        String dataVersion = syncDataHandle.getDataVersion(ItfSyncKeyEnum.SYNC_MONTH_PLAN.getCode());

        List<MonthPlanIssue> monthPlanIssues = new ArrayList<>();
        Map<String, FactoryMonthPlanProductionFinalResult> groupMap = new HashMap<>(16);
        this.genMonthPlanIssueList(monthPlanIssueList, groupMap, monthPlanIssues, dataVersion);

        List<CxMonthPlanIssue> cxMonthPlanIssuesList = new ArrayList<>();
        this.genCxMonthPlanIssuesList(groupMap, cxMonthPlanIssuesList, dataVersion);

        this.executeByBatch(monthPlanIssues, monthPlanIssueEntityMapper::batchUpdateMonthPlanIssue);
        this.executeByBatch(monthPlanIssues, monthPlanIssueEntityMapper::batchInsertMonthPlanIssue);
        // 成型月计划
        this.executeByBatch(cxMonthPlanIssuesList, monthPlanIssueEntityMapper::batchUpdateCxMonthPlanIssue);
        this.executeByBatch(cxMonthPlanIssuesList, monthPlanIssueEntityMapper::batchInsertCxMonthPlanIssue);
        // 发送MQ
        AjaxResult ajaxResult = null;
        String factoryCode = monthPlanIssueList.get(0).getFactoryCode();
        String monthPlanVersion = monthPlanIssueList.get(0).getMonthPlanVersion();
        String productionVersion = monthPlanIssueList.get(0).getProductionVersion();
        try {
            // 数据同步到中间库后，往 mq中发送消息通知 MES去取数据
            SyncParamsVO syncParamsVO = new SyncParamsVO();
            syncParamsVO.setSyncKey(ItfSyncKeyEnum.SYNC_MONTH_PLAN.getCode());
            syncParamsVO.setDataVersion(dataVersion);
            // 请求参数
            JSONObject params = new JSONObject();
            params.put("monthPlanVersion", monthPlanVersion);
            params.put("productionVersion", productionVersion);
            params.put("rowCount", monthPlanIssues.size());
            syncParamsVO.setParams(params);
            syncParamsVO.setDataSys(SysCode.APS);
            syncParamsVO.setDockSys(ApsConstant.DOCK_SYS_MES);
            syncParamsVO.setFactoryCode(factoryCode);
            syncParamsVO.setCompanyCode(factoryCode);
            //往消息队列发送消息
            syncDataHandle.syncNotice(syncParamsVO);
            // 取回mes的反馈结果
            SyncDataLogs logs = syncDataLogsService.getSyncDataResult(dataVersion);
            String status = logs.getStatus();
            if (ApsConstant.IS_RELEASE.equals(status)) {
                // 成功
                ajaxResult = AjaxResult.success(I18nUtil.getMessage("ui.data.column.scheduleResult.successPublish"));
            } else {
                // 失败，需要返回异常信息
                ajaxResult = AjaxResult.error(logs.getMsg());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.failedPublish"));
        }
        return ajaxResult;
    }

    /**
     * 按固定批量拆分列表后执行数据库写入，降低单次批量更新或新增SQL长度。
     *
     * @param dataList 数据列表
     * @param batchConsumer 分批处理逻辑
     * @param <T> 数据类型
     * @throws RuntimeException 数据库写入异常时向上抛出，交由事务或上层异常处理
     */
    private <T> void executeByBatch(List<T> dataList, Consumer<List<T>> batchConsumer) {
        if (CollectionUtils.isEmpty(dataList)) {
            return;
        }
        List<List<T>> splitList = ScmListUtils.getSplitList(dataList, MONTH_PLAN_ISSUE_BATCH_SIZE);
        for (List<T> splitItemList : splitList) {
            batchConsumer.accept(splitItemList);
        }
    }

    private void genMonthPlanIssueList(List<FactoryMonthPlanProductionFinalResult> monthPlanIssueList, Map<String, FactoryMonthPlanProductionFinalResult> groupMap, List<MonthPlanIssue> monthPlanIssues, String dataVersion) {
        List<List<FactoryMonthPlanProductionFinalResult>> splitList = ScmListUtils.getSplitList(monthPlanIssueList, 1000);
        for (List<FactoryMonthPlanProductionFinalResult> finalResultList : splitList) {
            for (FactoryMonthPlanProductionFinalResult finalResult : finalResultList) {
                String mapKey = GenerageMapKeyUtils.createMapKey(finalResult.getFactoryCode(), finalResult.getYear(),
                        finalResult.getMonth(), finalResult.getMaterialCode());
                if (groupMap.containsKey(mapKey)) {
                    FactoryMonthPlanProductionFinalResult result = groupMap.get(mapKey);
                    BigDecimal totalDayResult = this.getTotalDayResult(result);
                    Map<String, Object> params = result.getParams();
                    BigDecimal totalDayResultOld = (BigDecimal) params.get("totalDayResult");
                    params.put("totalDayResult", totalDayResult.add(totalDayResultOld));
                } else {
                    BigDecimal totalDayResult = this.getTotalDayResult(finalResult);
                    finalResult.getParams().put("totalDayResult", totalDayResult);
                    groupMap.put(mapKey, finalResult);
                }
                MonthPlanIssue monthPlanIssue = new MonthPlanIssue();
                monthPlanIssue.setMpVersionNo(finalResult.getProductionVersion());
                monthPlanIssue.setLastMonthPlanVersion(finalResult.getLastMonthPlanVersion());
                monthPlanIssue.setMpYear(String.valueOf(finalResult.getYear()));
                monthPlanIssue.setMpMonth(String.valueOf(finalResult.getMonth()));
                monthPlanIssue.setOrderNo(finalResult.getProductionNo());
                monthPlanIssue.setStrucCode(finalResult.getStructureName());
                monthPlanIssue.setMesMaterialCode(finalResult.getMaterialCode());
                monthPlanIssue.setSpecDesc(finalResult.getMaterialDesc());
                monthPlanIssue.setEmbryoSpec(finalResult.getMainMaterialDesc());
                monthPlanIssue.setPattern(finalResult.getPattern());
                monthPlanIssue.setCavity(finalResult.getMouldCavityQty());
                monthPlanIssue.setLiveBlock(finalResult.getTypeBlockQty());
                monthPlanIssue.setNetDemand(finalResult.getTotalQty());
                monthPlanIssue.setAdvNum(finalResult.getHeightProductionQty());
                //monthPlanIssue.setMonthAvgNum(finalResult.getAverageQty());
                monthPlanIssue.setStockSaleRatio(finalResult.getInventorySalesRatio());
                monthPlanIssue.setDayVulQty(finalResult.getDayVulcanizationQty());
                /*monthPlanIssue.setAdjustQty1();
                monthPlanIssue.setAdjustQty2();
                monthPlanIssue.setAdjustQty3();
                monthPlanIssue.setAdjustQty4();*/
                for (int i = 1; i <= 31; i++) {
                    Object fieldValue = ReflectUtils.getFieldValue(finalResult, "day" + i);
                    ReflectUtils.setFieldValue(monthPlanIssue, "day" + i, fieldValue);
                }
                monthPlanIssue.setDataVersion(dataVersion);
                monthPlanIssue.setCompanyCode(finalResult.getFactoryCode());
                monthPlanIssue.setFactoryCode(finalResult.getFactoryCode());
                monthPlanIssue.setConstructionStage(finalResult.getConstructionStage());
                monthPlanIssues.add(monthPlanIssue);
            }
        }
    }

    /**
     * 获取总天数合计
     *
     * @param finalResult 定稿结果
     * @return 结果
     */
    private BigDecimal getTotalDayResult(FactoryMonthPlanProductionFinalResult finalResult) {
        BigDecimal result = BigDecimal.ZERO;
        for (int i = 1; i <= 31; i++) {
            Integer fieldValue = ObjectUtils.defaultIfNull(ReflectUtils.getFieldValue(finalResult, "day" + i), 0);
            result = BigDecimalUtils.add(result, fieldValue);
        }
        return result;
    }
}

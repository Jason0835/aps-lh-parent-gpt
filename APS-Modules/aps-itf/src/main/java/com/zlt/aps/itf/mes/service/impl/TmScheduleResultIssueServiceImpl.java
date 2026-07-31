package com.zlt.aps.itf.mes.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.itf.constant.SysCode;
import com.zlt.aps.itf.mes.enums.ItfSyncKeyEnum;
import com.zlt.aps.itf.mes.mapper.TmScheduleResultIssueMapper;
import com.zlt.aps.itf.mes.service.ITmScheduleResultIssueService;
import com.zlt.aps.itf.vo.MesTmScheduleResult;
import com.zlt.aps.itf.vo.SyncDataLogs;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResultIssue;
import com.zlt.sync.handle.SyncDataHandle;
import com.zlt.sync.povo.SyncParamsVO;
import com.zlt.sync.service.SyncDataLogsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 胎面排程结果下发服务实现类
 *
 * @author APS
 */
@Slf4j
@Service
public class TmScheduleResultIssueServiceImpl implements ITmScheduleResultIssueService {

    /** 日期格式化器 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** SQL Server单次请求参数上限2100，每条记录约40个参数，安全批次大小为50 */
    private static final int BATCH_SIZE = 50;
    @Autowired
    private SyncDataHandle syncDataHandle;
    @Autowired
    private SyncDataLogsService syncDataLogsService;
    @Autowired
    private TmScheduleResultIssueMapper tmScheduleResultIssueMapper;

    /**
     * 下发胎面排程结果到MES
     * 业务规则（与胎圈一致）：
     * 1. D日（今天）：更新中班数据（胎面1班→MES中班），夜班早班已过不下发
     * 2. D+1日（明天）：更新夜早中3班数据（胎面2/3/4班→MES夜/早/中班）
     * 3. D+2日（后天）：先删后插夜早2班数据（胎面5/6班→MES夜/早班），中班尚未排产不下发
     *
     * @param tmScheduleResultIssueList 胎面排程结果下发列表（已按3天拆分）
     * @param factoryCode               厂别
     * @param companyCode               分公司编码
     * @return 下发结果
     */
    @Override
    public AjaxResult issueTmScheduleResult(List<TmScheduleResultIssue> tmScheduleResultIssueList, String factoryCode, String companyCode) {
        if (CollectionUtils.isEmpty(tmScheduleResultIssueList)) {
            return AjaxResult.success();
        }

        // 获取下发接口版本号
        String dataVersion = syncDataHandle.getDataVersion(ItfSyncKeyEnum.SYNC_TM_SCHEDULE_RESULT.getCode());

        // 获取今天、明天、后天的日期
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate dayAfterTomorrow = today.plusDays(2);

        // 按日期分组处理数据
        List<TmScheduleResultIssue> todayList = this.filterByDate(tmScheduleResultIssueList, today);
        List<TmScheduleResultIssue> tomorrowList = this.filterByDate(tmScheduleResultIssueList, tomorrow);
        List<TmScheduleResultIssue> dayAfterTomorrowList = this.filterByDate(tmScheduleResultIssueList, dayAfterTomorrow);

        // 转换为MES实体
        List<MesTmScheduleResult> todayMesList = this.convertToMesList(todayList, dataVersion, companyCode, factoryCode);
        List<MesTmScheduleResult> tomorrowMesList = this.convertToMesList(tomorrowList, dataVersion, companyCode, factoryCode);
        List<MesTmScheduleResult> dayAfterTomorrowMesList = this.convertToMesList(dayAfterTomorrowList, dataVersion, companyCode, factoryCode);

        // D日和D+1日数据：更新（存在则更新，不存在则插入）
        this.upsertTmScheduleResult(todayMesList, dataVersion);
        this.upsertTmScheduleResult(tomorrowMesList, dataVersion);

        // D+2日数据：先删除后插入（确保数据干净）
        this.insertTmScheduleResult(dayAfterTomorrowMesList, dayAfterTomorrow, dataVersion);

        // 合并所有数据用于发送MQ
        List<MesTmScheduleResult> allMesList = new ArrayList<>();
        allMesList.addAll(todayMesList);
        allMesList.addAll(tomorrowMesList);
        allMesList.addAll(dayAfterTomorrowMesList);

        if (CollectionUtils.isEmpty(allMesList)) {
            return AjaxResult.success("没有需要下发的数据");
        }

        // 发送MQ通知MES
        return this.sendMqNotice(allMesList, today, dayAfterTomorrow, dataVersion, factoryCode, companyCode);
    }

    /**
     * 批量更新或插入数据（存在则更新，不存在则插入）
     * 分批处理避免SQL Server参数上限2100的问题
     */
    private void upsertTmScheduleResult(List<MesTmScheduleResult> mesList, String dataVersion) {
        if (CollectionUtils.isEmpty(mesList)) {
            return;
        }
        // 分批查询已有记录，按排程日期+机台编码+胎面编码+版本号匹配
        Set<String> existingKeys = new HashSet<>();
        for (List<MesTmScheduleResult> batch : this.partitionList(mesList)) {
            List<MesTmScheduleResult> existingRecords = tmScheduleResultIssueMapper.selectExistingByScheduleDateAndMachine(batch);
            existingRecords.stream()
                    .map(r -> r.getScheduleDate() + "|" + r.getMachineCode() + "|" + r.getTreadCode() + "|" + r.getDataVersion())
                    .forEach(existingKeys::add);
        }
        // 根据查询结果分组：已有记录走批量更新，不存在记录走批量新增
        List<MesTmScheduleResult> updateList = new ArrayList<>();
        List<MesTmScheduleResult> insertList = new ArrayList<>();
        for (MesTmScheduleResult mesItem : mesList) {
            String key = mesItem.getScheduleDate() + "|" + mesItem.getMachineCode() + "|" + mesItem.getTreadCode() + "|" + mesItem.getDataVersion();
            if (existingKeys.contains(key)) {
                updateList.add(mesItem);
            } else {
                insertList.add(mesItem);
            }
        }
        // 分批更新
        for (List<MesTmScheduleResult> batch : this.partitionList(updateList)) {
            tmScheduleResultIssueMapper.batchUpdateByScheduleDateAndMachine(batch);
        }
        // 分批插入
        for (List<MesTmScheduleResult> batch : this.partitionList(insertList)) {
            tmScheduleResultIssueMapper.batchInsertTmScheduleResult(batch);
        }
    }

    /**
     * 插入数据（先删除指定日期的旧数据，再插入新数据）
     */
    private void insertTmScheduleResult(List<MesTmScheduleResult> mesList, LocalDate scheduleDate, String dataVersion) {
        if (CollectionUtils.isEmpty(mesList)) {
            return;
        }
        String dateStr = scheduleDate.format(DATE_FORMATTER);
        tmScheduleResultIssueMapper.deleteByScheduleDate(dateStr, dataVersion);
        // 分批插入
        for (List<MesTmScheduleResult> batch : this.partitionList(mesList)) {
            tmScheduleResultIssueMapper.batchInsertTmScheduleResult(batch);
        }
    }

    /**
     * 发送MQ通知
     */
    private AjaxResult sendMqNotice(List<MesTmScheduleResult> allMesList, LocalDate today,
                                     LocalDate dayAfterTomorrow, String dataVersion,
                                     String factoryCode, String companyCode) {
        try {
            SyncParamsVO syncParamsVO = new SyncParamsVO();
            syncParamsVO.setSyncKey(ItfSyncKeyEnum.SYNC_TM_SCHEDULE_RESULT.getCode());
            syncParamsVO.setDataVersion(dataVersion);

            // 请求参数
            JSONObject params = new JSONObject();
            params.put("rowCount", allMesList.size());
            params.put("startDate", today.format(DATE_FORMATTER));
            params.put("endDate", dayAfterTomorrow.format(DATE_FORMATTER));
            syncParamsVO.setParams(params);
            syncParamsVO.setDataSys(SysCode.APS);
            syncParamsVO.setDockSys(ApsConstant.DOCK_SYS_MES);
            syncParamsVO.setFactoryCode(factoryCode);
            syncParamsVO.setCompanyCode(companyCode);

            // 往消息队列发送消息
            syncDataHandle.syncNotice(syncParamsVO);

            // 取回mes的反馈结果，按同步日志状态映射 feedbackStatus（对齐胎侧 TcScheduleResultIssueServiceImpl）
            SyncDataLogs logs = syncDataLogsService.getSyncDataResult(dataVersion);
            if (logs != null && ApsConstant.IS_RELEASE.equals(logs.getStatus())) {
                return this.buildIssueResult(true, "SUCCESS",
                        I18nUtil.getMessage("ui.tc.schedule.release.success"));
            }
            if (logs != null && ApsConstant.TIMEOUT_FAILURE.equals(logs.getStatus())) {
                return this.buildIssueResult(false, "TIMEOUT",
                        StrUtil.blankToDefault(logs.getMsg(),
                                I18nUtil.getMessage("ui.tc.schedule.release.timeout")));
            }
            if (logs != null && ApsConstant.FAILURE_RELEASE.equals(logs.getStatus())) {
                return this.buildIssueResult(false, "FAILED",
                        StrUtil.blankToDefault(logs.getMsg(),
                                I18nUtil.getMessage("ui.tc.schedule.release.failed")));
            }
            // 同步日志尚未进入终态时保持发布中，由发布恢复任务继续查询，不能误判为失败。
            return this.buildIssueResult(true, "PENDING",
                    I18nUtil.getMessage("ui.tc.schedule.release.waitFeedbackStage"));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.failedPublish"));
        }
    }

    /**
     * 构造携带MES反馈状态的发布调用结果。
     *
     * @param success 调用是否按成功码返回
     * @param feedbackStatus MES反馈状态
     * @param message 结果说明
     * @return 发布调用结果
     */
    private AjaxResult buildIssueResult(boolean success, String feedbackStatus, String message) {
        AjaxResult result = success ? AjaxResult.success(message) : AjaxResult.error(message);
        result.put("feedbackStatus", feedbackStatus);
        return result;
    }

    /**
     * 根据日期过滤数据
     */
    private List<TmScheduleResultIssue> filterByDate(List<TmScheduleResultIssue> list, LocalDate date) {
        return list.stream()
                .filter(item -> item.getScheduleDate() != null)
                .filter(item -> item.getScheduleDate().equals(date))
                .collect(Collectors.toList());
    }

    /**
     * 转换为MES实体列表
     */
    private List<MesTmScheduleResult> convertToMesList(List<TmScheduleResultIssue> list, String dataVersion,
                                                        String companyCode, String factoryCode) {
        return list.stream().map(item -> this.convertToMesEntity(item, dataVersion, companyCode, factoryCode))
                .collect(Collectors.toList());
    }

    /**
     * 转换为MES中间表实体
     */
    private MesTmScheduleResult convertToMesEntity(TmScheduleResultIssue item, String dataVersion,
                                                    String companyCode, String factoryCode) {
        MesTmScheduleResult mesItem = new MesTmScheduleResult();
        mesItem.setScheduleDate(item.getScheduleDate());
        mesItem.setBatchNo(item.getBatchNo());
        mesItem.setOrderNo(item.getOrderNo());
        mesItem.setTreadCode(item.getTreadCode());
        mesItem.setSapMaterialCode(item.getSapMaterialCode());
        mesItem.setGlueCode(item.getGlueCode());
        mesItem.setBaseGlueCode(item.getBaseGlueCode());
        mesItem.setWholeGlueCode(item.getWholeGlueCode());
        mesItem.setGlueSeq(item.getGlueSeq());
        mesItem.setMouthPlateCode(item.getMouthPlateCode());
        mesItem.setSpecSize(item.getSpecSize());
        mesItem.setMachineCode(item.getMachineCode());
        mesItem.setUnitConsume(item.getUnitConsume());
        mesItem.setStockQty(item.getStockQty());
        mesItem.setSupplyTime(item.getSupplyTime());
        // 班次计划量
        mesItem.setMidPlanQty(item.getMidPlanQty());
        mesItem.setMidProduceOrder(item.getMidProduceOrder());
        mesItem.setMidSysAnalysis(item.getMidSysAnalysis());
        mesItem.setMidHandAnalysis(item.getMidHandAnalysis());
        mesItem.setNightPlanQty(item.getNightPlanQty());
        mesItem.setNightProduceOrder(item.getNightProduceOrder());
        mesItem.setNightSysAnalysis(item.getNightSysAnalysis());
        mesItem.setNightHandAnalysis(item.getNightHandAnalysis());
        mesItem.setDayPlanQty(item.getDayPlanQty());
        mesItem.setDayProduceOrder(item.getDayProduceOrder());
        mesItem.setDaySysAnalysis(item.getDaySysAnalysis());
        mesItem.setDayHandAnalysis(item.getDayHandAnalysis());
        mesItem.setNextMidPlanQty(item.getNextMidPlanQty());
        mesItem.setNextMidProduceOrder(item.getNextMidProduceOrder());
        mesItem.setNextMidSysAnalysis(item.getNextMidSysAnalysis());
        mesItem.setNextMidHandAnalysis(item.getNextMidHandAnalysis());
        // 状态与公共字段
        mesItem.setIsRelease("1");
        mesItem.setMarkCloseOutTip(item.getMarkCloseOutTip());
        mesItem.setTailFlag(item.getTailFlag());
        mesItem.setProductionStatus(item.getProductionStatus());
        mesItem.setRemark(item.getRemark());
        mesItem.setDataVersion(dataVersion);
        mesItem.setCompanyCode(companyCode);
        mesItem.setFactoryCode(factoryCode);
        return mesItem;
    }

    /**
     * 将列表按BATCH_SIZE分批，避免SQL Server参数上限2100的问题
     */
    private <T> List<List<T>> partitionList(List<T> list) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += BATCH_SIZE) {
            partitions.add(list.subList(i, Math.min(i + BATCH_SIZE, list.size())));
        }
        return partitions;
    }
}

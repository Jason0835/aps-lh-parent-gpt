package com.zlt.aps.itf.mes.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.itf.constant.SysCode;
import com.zlt.aps.itf.mes.enums.ItfSyncKeyEnum;
import com.zlt.aps.itf.mes.mapper.TqScheduleResultIssueMapper;
import com.zlt.aps.itf.mes.service.ITqScheduleResultIssueService;
import com.zlt.aps.itf.vo.MesTqScheduleResult;
import com.zlt.aps.itf.vo.SyncDataLogs;
import com.zlt.aps.tq.api.domain.entity.TqScheduleResultIssue;
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
 * 胎圈排程结果下发服务实现类
 *
 * @author APS
 */
@Slf4j
@Service
public class TqScheduleResultIssueServiceImpl implements ITqScheduleResultIssueService {

    @Autowired
    private SyncDataHandle syncDataHandle;

    @Autowired
    private SyncDataLogsService syncDataLogsService;

    @Autowired
    private TqScheduleResultIssueMapper tqScheduleResultIssueMapper;

    /** 日期格式化器 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** SQL Server单次请求参数上限2100，每条记录约40个参数，安全批次大小为50 */
    private static final int BATCH_SIZE = 50;

    /**
     * 下发胎圈排程结果到MES
     * 业务规则：
     * 1. D日（今天）：更新中班数据（胎圈1班→MES中班），夜班早班已过不下发
     * 2. D+1日（明天）：更新夜早中3班数据（胎圈2/3/4班→MES夜/早/中班）
     * 3. D+2日（后天）：先删后插夜早2班数据（胎圈5/6班→MES夜/早班），中班尚未排产不下发
     *
     * @param tqScheduleResultIssueList 胎圈排程结果下发列表（已按3天拆分）
     * @param factoryCode               厂别
     * @param companyCode               分公司编码
     * @return 下发结果
     */
    @Override
    public AjaxResult issueTqScheduleResult(List<TqScheduleResultIssue> tqScheduleResultIssueList, String factoryCode, String companyCode) {
        if (CollectionUtils.isEmpty(tqScheduleResultIssueList)) {
            return AjaxResult.success();
        }

        // 获取下发接口版本号
        String dataVersion = syncDataHandle.getDataVersion(ItfSyncKeyEnum.SYNC_TQ_SCHEDULE_RESULT.getCode());

        // 获取今天、明天、后天的日期
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate dayAfterTomorrow = today.plusDays(2);

        // 按日期分组处理数据
        List<TqScheduleResultIssue> todayList = filterByDate(tqScheduleResultIssueList, today);
        List<TqScheduleResultIssue> tomorrowList = filterByDate(tqScheduleResultIssueList, tomorrow);
        List<TqScheduleResultIssue> dayAfterTomorrowList = filterByDate(tqScheduleResultIssueList, dayAfterTomorrow);

        // 转换为MES实体
        List<MesTqScheduleResult> todayMesList = convertToMesList(todayList, dataVersion, companyCode, factoryCode);
        List<MesTqScheduleResult> tomorrowMesList = convertToMesList(tomorrowList, dataVersion, companyCode, factoryCode);
        List<MesTqScheduleResult> dayAfterTomorrowMesList = convertToMesList(dayAfterTomorrowList, dataVersion, companyCode, factoryCode);

        // D日和D+1日数据：更新（存在则更新，不存在则插入）
        upsertTqScheduleResult(todayMesList, dataVersion);
        upsertTqScheduleResult(tomorrowMesList, dataVersion);

        // D+2日数据：先删除后插入（确保数据干净）
        insertTqScheduleResult(dayAfterTomorrowMesList, dayAfterTomorrow, dataVersion);

        // 合并所有数据用于发送MQ
        List<MesTqScheduleResult> allMesList = new ArrayList<>();
        allMesList.addAll(todayMesList);
        allMesList.addAll(tomorrowMesList);
        allMesList.addAll(dayAfterTomorrowMesList);

        if (CollectionUtils.isEmpty(allMesList)) {
            return AjaxResult.success("没有需要下发的数据");
        }

        // 发送MQ通知MES
        return sendMqNotice(allMesList, today, dayAfterTomorrow, dataVersion, factoryCode, companyCode);
    }

    /**
     * 批量更新或插入数据（存在则更新，不存在则插入）
     * 分批处理避免SQL Server参数上限2100的问题
     */
    private void upsertTqScheduleResult(List<MesTqScheduleResult> mesList, String dataVersion) {
        if (CollectionUtils.isEmpty(mesList)) {
            return;
        }
        // 分批查询已有记录，按排程日期+机台编码+胎圈编码+版本号匹配
        Set<String> existingKeys = new HashSet<>();
        for (List<MesTqScheduleResult> batch : partitionList(mesList)) {
            List<MesTqScheduleResult> existingRecords = tqScheduleResultIssueMapper.selectExistingByScheduleDateAndMachine(batch);
            existingRecords.stream()
                    .map(r -> r.getScheduleDate() + "|" + r.getMachineCode() + "|" + r.getBeadCode() + "|" + r.getDataVersion())
                    .forEach(existingKeys::add);
        }
        // 根据查询结果分组：已有记录走批量更新，不存在记录走批量新增
        List<MesTqScheduleResult> updateList = new ArrayList<>();
        List<MesTqScheduleResult> insertList = new ArrayList<>();
        for (MesTqScheduleResult mesItem : mesList) {
            String key = mesItem.getScheduleDate() + "|" + mesItem.getMachineCode() + "|" + mesItem.getBeadCode() + "|" + mesItem.getDataVersion();
            if (existingKeys.contains(key)) {
                updateList.add(mesItem);
            } else {
                insertList.add(mesItem);
            }
        }
        // 分批更新
        for (List<MesTqScheduleResult> batch : partitionList(updateList)) {
            tqScheduleResultIssueMapper.batchUpdateByScheduleDateAndMachine(batch);
        }
        // 分批插入
        for (List<MesTqScheduleResult> batch : partitionList(insertList)) {
            tqScheduleResultIssueMapper.batchInsertTqScheduleResult(batch);
        }
    }

    /**
     * 插入数据（先删除指定日期的旧数据，再插入新数据）
     */
    private void insertTqScheduleResult(List<MesTqScheduleResult> mesList, LocalDate scheduleDate, String dataVersion) {
        if (CollectionUtils.isEmpty(mesList)) {
            return;
        }
        String dateStr = scheduleDate.format(DATE_FORMATTER);
        tqScheduleResultIssueMapper.deleteByScheduleDate(dateStr, dataVersion);
        // 分批插入
        for (List<MesTqScheduleResult> batch : partitionList(mesList)) {
            tqScheduleResultIssueMapper.batchInsertTqScheduleResult(batch);
        }
    }

    /**
     * 发送MQ通知
     */
    private AjaxResult sendMqNotice(List<MesTqScheduleResult> allMesList, LocalDate today,
                                     LocalDate dayAfterTomorrow, String dataVersion,
                                     String factoryCode, String companyCode) {
        AjaxResult ajaxResult;
        try {
            SyncParamsVO syncParamsVO = new SyncParamsVO();
            syncParamsVO.setSyncKey(ItfSyncKeyEnum.SYNC_TQ_SCHEDULE_RESULT.getCode());
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

            // 取回mes的反馈结果
            SyncDataLogs logs = syncDataLogsService.getSyncDataResult(dataVersion);
            String status = logs.getStatus();
            if (ApsConstant.IS_RELEASE.equals(status)) {
                ajaxResult = AjaxResult.success(I18nUtil.getMessage("ui.data.column.scheduleResult.successPublish"));
            } else {
                ajaxResult = AjaxResult.error(logs.getMsg());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.failedPublish"));
        }

        return ajaxResult;
    }

    /**
     * 根据日期过滤数据
     */
    private List<TqScheduleResultIssue> filterByDate(List<TqScheduleResultIssue> list, LocalDate date) {
        return list.stream()
                .filter(item -> item.getScheduleDate() != null)
                .filter(item -> item.getScheduleDate().equals(date))
                .collect(Collectors.toList());
    }

    /**
     * 转换为MES实体列表
     */
    private List<MesTqScheduleResult> convertToMesList(List<TqScheduleResultIssue> list, String dataVersion,
                                                        String companyCode, String factoryCode) {
        return list.stream().map(item -> convertToMesEntity(item, dataVersion, companyCode, factoryCode))
                .collect(Collectors.toList());
    }

    /**
     * 转换为MES中间表实体
     */
    private MesTqScheduleResult convertToMesEntity(TqScheduleResultIssue item, String dataVersion,
                                                    String companyCode, String factoryCode) {
        MesTqScheduleResult mesItem = new MesTqScheduleResult();
        mesItem.setScheduleDate(item.getScheduleDate());
        mesItem.setCxBatchNo(item.getCxBatchNo());
        mesItem.setBatchNo(item.getBatchNo());
        mesItem.setOrderNo(item.getOrderNo());
        mesItem.setBeadCode(item.getBeadCode());
        mesItem.setSapMaterialCode(item.getSapMaterialCode());
        mesItem.setSteelRingCode(item.getSteelRingCode());
        mesItem.setTriangleGlueCode(item.getTriangleGlueCode());
        mesItem.setGlueCode(item.getGlueCode());
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
        // 成型3~8班计划量
        mesItem.setCxClass3Plan(item.getCxClass3Plan());
        mesItem.setCxClass4Plan(item.getCxClass4Plan());
        mesItem.setCxClass5Plan(item.getCxClass5Plan());
        mesItem.setCxClass6Plan(item.getCxClass6Plan());
        mesItem.setCxClass7Plan(item.getCxClass7Plan());
        mesItem.setCxClass8Plan(item.getCxClass8Plan());
        // 状态与公共字段
        mesItem.setIsRelease("1");
        mesItem.setMarkCloseOutTip(item.getMarkCloseOutTip());
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

package com.zlt.aps.itf.mes.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.gsq.api.domain.entity.GsqScheduleResultIssue;
import com.zlt.aps.itf.constant.SysCode;
import com.zlt.aps.itf.mes.enums.ItfSyncKeyEnum;
import com.zlt.aps.itf.mes.mapper.GsqScheduleResultIssueMapper;
import com.zlt.aps.itf.mes.service.IGsqScheduleResultIssueService;
import com.zlt.aps.itf.vo.MesGsqScheduleResult;
import com.zlt.aps.itf.vo.SyncDataLogs;
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
 * 钢丝圈排程结果下发服务实现类
 *
 * @author APS
 */
@Slf4j
@Service
public class GsqScheduleResultIssueServiceImpl implements IGsqScheduleResultIssueService {

    @Autowired
    private SyncDataHandle syncDataHandle;

    @Autowired
    private SyncDataLogsService syncDataLogsService;

    @Autowired
    private GsqScheduleResultIssueMapper gsqScheduleResultIssueMapper;

    /** 日期格式化器 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** SQL Server单次请求参数上限2100，每条记录约40个参数，安全批次大小为50 */
    private static final int BATCH_SIZE = 50;

    /**
     * 下发钢丝圈排程结果到MES
     * 业务规则：
     * 1. D日（今天）：更新中班数据（钢丝圈1班→MES中班），夜班早班已过不下发
     * 2. D+1日（明天）：更新夜早中3班数据（钢丝圈2/3/4班→MES夜/早/中班）
     * 3. D+2日（后天）：先删后插夜早2班数据（钢丝圈5/6班→MES夜/早班），中班尚未排产不下发
     *
     * @param gsqScheduleResultIssueList 钢丝圈排程结果下发列表（已按3天拆分）
     * @param factoryCode                厂别
     * @param companyCode                分公司编码
     * @return 下发结果
     */
    @Override
    public AjaxResult issueGsqScheduleResult(List<GsqScheduleResultIssue> gsqScheduleResultIssueList, String factoryCode, String companyCode) {
        if (CollectionUtils.isEmpty(gsqScheduleResultIssueList)) {
            return AjaxResult.success();
        }

        // 获取下发接口版本号
        String dataVersion = syncDataHandle.getDataVersion(ItfSyncKeyEnum.SYNC_GSQ_SCHEDULE_RESULT.getCode());

        // 获取今天、明天、后天的日期
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate dayAfterTomorrow = today.plusDays(2);

        // 按日期分组处理数据
        List<GsqScheduleResultIssue> todayList = filterByDate(gsqScheduleResultIssueList, today);
        List<GsqScheduleResultIssue> tomorrowList = filterByDate(gsqScheduleResultIssueList, tomorrow);
        List<GsqScheduleResultIssue> dayAfterTomorrowList = filterByDate(gsqScheduleResultIssueList, dayAfterTomorrow);

        // 转换为MES实体
        List<MesGsqScheduleResult> todayMesList = convertToMesList(todayList, dataVersion, companyCode, factoryCode);
        List<MesGsqScheduleResult> tomorrowMesList = convertToMesList(tomorrowList, dataVersion, companyCode, factoryCode);
        List<MesGsqScheduleResult> dayAfterTomorrowMesList = convertToMesList(dayAfterTomorrowList, dataVersion, companyCode, factoryCode);

        // D日和D+1日数据：更新（存在则更新，不存在则插入）
        upsertGsqScheduleResult(todayMesList, dataVersion);
        upsertGsqScheduleResult(tomorrowMesList, dataVersion);

        // D+2日数据：先删除后插入（确保数据干净）
        insertGsqScheduleResult(dayAfterTomorrowMesList, dayAfterTomorrow, dataVersion);

        // 合并所有数据用于发送MQ
        List<MesGsqScheduleResult> allMesList = new ArrayList<>();
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
     * 匹配键：排程日期+机台编码+钢丝圈编码（不含版本号）
     * 实现方式：delete + insert（而非 update）
     *   - 历史问题：旧版本使用 UPDATE 时，若 MES 表存在同键的多版本残留数据，
     *     一条 UPDATE 会命中多条记录，把它们全部改成新版本，造成重复数据。
     *   - 现在改为：对已存在的键，先批量删除该键的所有历史版本数据，再批量插入本次发布的新版本数据。
     *   - 这样无论历史有多少版本残留，最终每个键只会保留本次发布的 1 条最新版本记录。
     *   - MES 侧无回写字段，删除不会丢失业务数据。
     */
    private void upsertGsqScheduleResult(List<MesGsqScheduleResult> mesList, String dataVersion) {
        if (CollectionUtils.isEmpty(mesList)) {
            return;
        }
        log.info("钢丝圈upsert处理开始，总记录数：{}", mesList.size());
        // 分批查询已有记录，按排程日期+机台编码+钢丝圈编码匹配（不含版本号）
        Set<String> existingKeys = new HashSet<>();
        for (List<MesGsqScheduleResult> batch : partitionList(mesList)) {
            List<MesGsqScheduleResult> existingRecords = gsqScheduleResultIssueMapper.selectExistingByScheduleDateAndMachine(batch);
            existingRecords.stream()
                    .map(r -> r.getScheduleDate() + "|" + r.getMachineCode() + "|" + r.getSteelRingCode())
                    .forEach(existingKeys::add);
        }
        // 根据查询结果分组：已有记录走"删除+插入"覆盖，不存在记录走新增
        List<MesGsqScheduleResult> replaceList = new ArrayList<>();
        List<MesGsqScheduleResult> insertList = new ArrayList<>();
        for (MesGsqScheduleResult mesItem : mesList) {
            String key = mesItem.getScheduleDate() + "|" + mesItem.getMachineCode() + "|" + mesItem.getSteelRingCode();
            if (existingKeys.contains(key)) {
                replaceList.add(mesItem);
            } else {
                insertList.add(mesItem);
            }
        }
        log.info("钢丝圈upsert分组结果：需覆盖{}条，需新增{}条", replaceList.size(), insertList.size());
        // 覆盖处理：先批量删除该键的所有历史版本记录，再批量插入本次发布的新版本数据
        int replaceBatchCount = 0;
        for (List<MesGsqScheduleResult> batch : partitionList(replaceList)) {
            gsqScheduleResultIssueMapper.batchDeleteByScheduleDateAndMachine(batch);
            gsqScheduleResultIssueMapper.batchInsertGsqScheduleResult(batch);
            replaceBatchCount += batch.size();
        }
        // 新增处理
        int insertBatchCount = 0;
        for (List<MesGsqScheduleResult> batch : partitionList(insertList)) {
            gsqScheduleResultIssueMapper.batchInsertGsqScheduleResult(batch);
            insertBatchCount += batch.size();
        }
        log.info("钢丝圈upsert处理完成：实际覆盖{}条，实际新增{}条", replaceBatchCount, insertBatchCount);
    }

    /**
     * 插入数据（先删除指定日期的旧数据，再插入新数据）
     */
    private void insertGsqScheduleResult(List<MesGsqScheduleResult> mesList, LocalDate scheduleDate, String dataVersion) {
        if (CollectionUtils.isEmpty(mesList)) {
            return;
        }
        String dateStr = scheduleDate.format(DATE_FORMATTER);
        gsqScheduleResultIssueMapper.deleteByScheduleDate(dateStr, dataVersion);
        // 分批插入
        for (List<MesGsqScheduleResult> batch : partitionList(mesList)) {
            gsqScheduleResultIssueMapper.batchInsertGsqScheduleResult(batch);
        }
    }

    /**
     * 发送MQ通知
     */
    private AjaxResult sendMqNotice(List<MesGsqScheduleResult> allMesList, LocalDate today,
                                     LocalDate dayAfterTomorrow, String dataVersion,
                                     String factoryCode, String companyCode) {
        AjaxResult ajaxResult;
        try {
            SyncParamsVO syncParamsVO = new SyncParamsVO();
            syncParamsVO.setSyncKey(ItfSyncKeyEnum.SYNC_GSQ_SCHEDULE_RESULT.getCode());
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
            String msg = logs.getMsg();
            if (ApsConstant.IS_RELEASE.equals(status)) {
                // 发布成功：CODE=200，DATA_TAG=IS_RELEASE
                ajaxResult = AjaxResult.success(I18nUtil.getMessage("ui.data.column.scheduleResult.successPublish"));
                ajaxResult.put(AjaxResult.DATA_TAG, ApsConstant.IS_RELEASE);
            } else if (ApsConstant.TIMEOUT_FAILURE.equals(status)) {
                // 超时失败：CODE=500，DATA_TAG=TIMEOUT_FAILURE
                ajaxResult = AjaxResult.error(msg);
                ajaxResult.put(AjaxResult.DATA_TAG, ApsConstant.TIMEOUT_FAILURE);
            } else {
                // 发布失败：CODE=500，DATA_TAG=FAILURE_RELEASE
                ajaxResult = AjaxResult.error(msg);
                ajaxResult.put(AjaxResult.DATA_TAG, ApsConstant.FAILURE_RELEASE);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            ajaxResult = AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.failedPublish"));
            ajaxResult.put(AjaxResult.DATA_TAG, ApsConstant.FAILURE_RELEASE);
        }

        return ajaxResult;
    }

    /**
     * 根据日期过滤数据
     */
    private List<GsqScheduleResultIssue> filterByDate(List<GsqScheduleResultIssue> list, LocalDate date) {
        return list.stream()
                .filter(item -> item.getScheduleDate() != null)
                .filter(item -> item.getScheduleDate().equals(date))
                .collect(Collectors.toList());
    }

    /**
     * 转换为MES实体列表
     */
    private List<MesGsqScheduleResult> convertToMesList(List<GsqScheduleResultIssue> list, String dataVersion,
                                                        String companyCode, String factoryCode) {
        return list.stream().map(item -> convertToMesEntity(item, dataVersion, companyCode, factoryCode))
                .collect(Collectors.toList());
    }

    /**
     * 转换为MES中间表实体
     */
    private MesGsqScheduleResult convertToMesEntity(GsqScheduleResultIssue item, String dataVersion,
                                                    String companyCode, String factoryCode) {
        MesGsqScheduleResult mesItem = new MesGsqScheduleResult();
        mesItem.setScheduleDate(item.getScheduleDate());
        mesItem.setCxBatchNo(item.getCxBatchNo());
        mesItem.setBatchNo(item.getBatchNo());
        mesItem.setOrderNo(item.getOrderNo());
        mesItem.setSteelRingCode(item.getSteelRingCode());
        mesItem.setMaterialCode(item.getMaterialCode());
        mesItem.setSteelType(item.getSteelType());
        mesItem.setEmbryoSpecDesc(item.getEmbryoSpecDesc());
        mesItem.setUnitConsume(item.getUnitConsume());
        mesItem.setTwiningDiscCode(item.getTwiningDiscCode());
        mesItem.setProSize(item.getProSize());
        mesItem.setMachineCode(item.getMachineCode());
        mesItem.setStockQty(item.getStockQty());
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
        // 对应胎圈1~6班消耗量
        mesItem.setTqClass1Plan(item.getTqClass1Plan());
        mesItem.setTqClass2Plan(item.getTqClass2Plan());
        mesItem.setTqClass3Plan(item.getTqClass3Plan());
        mesItem.setTqClass4Plan(item.getTqClass4Plan());
        mesItem.setTqClass5Plan(item.getTqClass5Plan());
        mesItem.setTqClass6Plan(item.getTqClass6Plan());
        // 状态与公共字段
        mesItem.setIsRelease("1");
        mesItem.setCloseOutSpecFlag(item.getCloseOutSpecFlag());
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

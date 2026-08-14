package com.zlt.aps.itf.mes.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
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
import java.util.TreeSet;
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
     * 1. 从下发数据中提取实际排程日期，按日期分组处理（不再依赖LocalDate.now()推导日期，避免发布日期与排程日期不一致导致数据被过滤）
     * 2. 今天及过去日期的数据：upsert（存在则更新，不存在则插入）
     * 3. 未来日期的数据：先删除后插入（确保数据干净）
     * 4. 添加事务保证，任一批次失败则全部回滚
     *
     * @param gsqScheduleResultIssueList 钢丝圈排程结果下发列表（已按3天拆分）
     * @param factoryCode                厂别
     * @param companyCode                分公司编码
     * @return 下发结果
     */
    @Override
    @DSTransactional
    public AjaxResult issueGsqScheduleResult(List<GsqScheduleResultIssue> gsqScheduleResultIssueList, String factoryCode, String companyCode) {
        if (CollectionUtils.isEmpty(gsqScheduleResultIssueList)) {
            return AjaxResult.success();
        }

        log.info("钢丝圈排程结果下发MES开始，传入记录数：{}", gsqScheduleResultIssueList.size());

        // 获取下发接口版本号
        String dataVersion = syncDataHandle.getDataVersion(ItfSyncKeyEnum.SYNC_GSQ_SCHEDULE_RESULT.getCode());

        // 从下发数据中提取所有不重复的排程日期（不再依赖LocalDate.now()推导，避免发布日期与排程日期不一致导致数据被过滤）
        Set<LocalDate> scheduleDates = gsqScheduleResultIssueList.stream()
                .map(GsqScheduleResultIssue::getScheduleDate)
                .filter(date -> date != null)
                .collect(Collectors.toCollection(TreeSet::new));

        if (CollectionUtils.isEmpty(scheduleDates)) {
            return AjaxResult.success("没有需要下发的数据");
        }

        LocalDate today = LocalDate.now();
        List<MesGsqScheduleResult> allMesList = new ArrayList<>();

        // 按实际排程日期分组处理
        for (LocalDate scheduleDate : scheduleDates) {
            List<GsqScheduleResultIssue> dayList = filterByDate(gsqScheduleResultIssueList, scheduleDate);
            if (CollectionUtils.isEmpty(dayList)) {
                continue;
            }

            log.info("处理排程日期{}，记录数：{}", scheduleDate, dayList.size());

            // 转换为MES实体
            List<MesGsqScheduleResult> dayMesList = convertToMesList(dayList, dataVersion, companyCode, factoryCode);
            allMesList.addAll(dayMesList);

            if (scheduleDate.isAfter(today)) {
                // 未来日期：先删除后插入（确保数据干净）
                insertGsqScheduleResult(dayMesList, scheduleDate, dataVersion);
            } else {
                // 今天及过去日期：upsert（存在则更新，不存在则插入）
                upsertGsqScheduleResult(dayMesList, dataVersion);
            }
        }

        if (CollectionUtils.isEmpty(allMesList)) {
            return AjaxResult.success("没有需要下发的数据");
        }

        // 获取日期范围用于MQ通知（scheduleDates已按TreeSet排序）
        LocalDate startDate = scheduleDates.iterator().next();
        LocalDate endDate = scheduleDates.stream()
                .reduce((first, second) -> second)
                .orElse(startDate);

        log.info("钢丝圈排程结果下发MES完成，总记录数：{}，日期范围：{} ~ {}", allMesList.size(), startDate, endDate);

        // 发送MQ通知MES
        return sendMqNotice(allMesList, startDate, endDate, dataVersion, factoryCode, companyCode);
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
    private AjaxResult sendMqNotice(List<MesGsqScheduleResult> allMesList, LocalDate startDate,
                                     LocalDate endDate, String dataVersion,
                                     String factoryCode, String companyCode) {
        AjaxResult ajaxResult;
        try {
            SyncParamsVO syncParamsVO = new SyncParamsVO();
            syncParamsVO.setSyncKey(ItfSyncKeyEnum.SYNC_GSQ_SCHEDULE_RESULT.getCode());
            syncParamsVO.setDataVersion(dataVersion);

            // 请求参数
            JSONObject params = new JSONObject();
            params.put("rowCount", allMesList.size());
            params.put("startDate", startDate.format(DATE_FORMATTER));
            params.put("endDate", endDate.format(DATE_FORMATTER));
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
     * 班次备注(NIGHT_REMARK/DAY_REMARK/MID_REMARK)使用对应班次的原因分析值赋值
     */
    private MesGsqScheduleResult convertToMesEntity(GsqScheduleResultIssue item, String dataVersion,
                                                    String companyCode, String factoryCode) {
        MesGsqScheduleResult mesItem = new MesGsqScheduleResult();
        mesItem.setScheduleDate(item.getScheduleDate());
        mesItem.setTqBatchNo(item.getTqBatchNo());
        mesItem.setBatchNo(item.getBatchNo());
        mesItem.setOrderNo(item.getOrderNo());
        mesItem.setSteelRingCode(item.getSteelRingCode());
        mesItem.setMaterialCode(item.getMaterialCode());
        mesItem.setSteelType(item.getSteelType());
        mesItem.setMachineCode(item.getMachineCode());
        mesItem.setEmbryoSpecDesc(item.getEmbryoSpecDesc());
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
        // 班次备注使用原因分析值赋值
        mesItem.setNightRemark(item.getNightSysAnalysis());
        mesItem.setDayRemark(item.getDaySysAnalysis());
        mesItem.setMidRemark(item.getMidSysAnalysis());
        // 公共字段
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

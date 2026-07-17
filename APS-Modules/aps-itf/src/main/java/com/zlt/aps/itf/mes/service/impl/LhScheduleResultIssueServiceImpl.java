package com.zlt.aps.itf.mes.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.itf.constant.SysCode;
import com.zlt.aps.itf.mes.enums.ItfSyncKeyEnum;
import com.zlt.aps.itf.mes.mapper.LhScheduleResultIssueMapper;
import com.zlt.aps.itf.mes.service.ILhScheduleResultIssueService;
import com.zlt.aps.itf.vo.MesLhScheduleResult;
import com.zlt.aps.itf.vo.SyncDataLogs;
import com.zlt.aps.mp.api.domain.entity.LhScheduleResultIssue;
import com.zlt.sync.handle.SyncDataHandle;
import com.zlt.sync.povo.SyncParamsVO;
import com.zlt.sync.service.SyncDataLogsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 硫化排程结果下发服务实现
 *
 * @author APS Team
 * @since 2.0.0
 */
@Slf4j
@Service
public class LhScheduleResultIssueServiceImpl implements ILhScheduleResultIssueService {

    @Autowired
    private SyncDataHandle syncDataHandle;

    @Autowired
    private SyncDataLogsService syncDataLogsService;

    @Autowired
    private LhScheduleResultIssueMapper lhScheduleResultIssueMapper;

    /**
     * 日期格式化器
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * SQL Server单次请求参数上限2100，每条记录34个参数，安全批次大小为50
     */
    private static final int BATCH_SIZE = 50;

    /**
     * 将列表按BATCH_SIZE分批，避免SQL Server参数上限2100的问题
     *
     * @param list 待分批的列表
     * @return 分批后的列表
     */
    private <T> List<List<T>> partitionList(List<T> list) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += BATCH_SIZE) {
            partitions.add(list.subList(i, Math.min(i + BATCH_SIZE, list.size())));
        }
        return partitions;
    }

    /**
     * 下发硫化排程结果到MES
     * 业务规则：
     * 每条硫化排程结果自带8班数据，覆盖排程窗口T日到T+2日三天（排程日期为T+1日）：
     * 1. 窗口首日（T日，排程日期前一天）数据：更新（仅更新早中班，不覆盖夜班），不存在则插入
     * 2. 窗口次日（T+1日，排程日期当天）数据：更新（存在则更新，不存在则插入），包含夜早中3班
     * 3. 窗口第三日（T+2日，排程日期后一天）数据：先删除后插入，包含夜早中3班
     * 日期从下发数据中推导，不再依赖LocalDate.now()
     *
     * @param lhScheduleResultIssueList 硫化排程结果列表
     * @param factoryCode               厂别
     * @param companyCode               分公司编码
     * @return 下发结果
     */
    @Override
    public AjaxResult issueLhScheduleResult(List<LhScheduleResultIssue> lhScheduleResultIssueList, String factoryCode, String companyCode) {
        if (CollectionUtils.isEmpty(lhScheduleResultIssueList)) {
            return AjaxResult.success();
        }

        // 获取下发接口版本号
        String dataVersion = syncDataHandle.getDataVersion(ItfSyncKeyEnum.SYNC_LH_SCHEDULE_RESULT.getCode());

        // 从数据中提取所有不重复的排程日期并排序
        List<LocalDate> distinctDates = lhScheduleResultIssueList.stream()
                .filter(item -> item.getScheduleDate() != null)
                .map(item -> item.getScheduleDate().toLocalDate())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        if (distinctDates.isEmpty()) {
            return AjaxResult.success("没有需要下发的数据");
        }

        LocalDate firstDate = distinctDates.get(0);
        LocalDate lastDate = distinctDates.get(distinctDates.size() - 1);

        // 按日期分组处理数据
        List<LhScheduleResultIssue> day1List = filterByDate(lhScheduleResultIssueList, distinctDates.get(0));
        List<LhScheduleResultIssue> day2List = distinctDates.size() > 1
                ? filterByDate(lhScheduleResultIssueList, distinctDates.get(1))
                : new ArrayList<>();
        List<LhScheduleResultIssue> day3List = distinctDates.size() > 2
                ? filterByDate(lhScheduleResultIssueList, distinctDates.get(2))
                : new ArrayList<>();

        // 处理窗口首日数据：转换为MES实体
        List<MesLhScheduleResult> day1MesList = convertToMesList(day1List, dataVersion, companyCode, factoryCode);

        // 处理窗口次日数据：转换为MES实体
        List<MesLhScheduleResult> day2MesList = convertToMesList(day2List, dataVersion, companyCode, factoryCode);

        // 处理排程日期当天数据：转换为MES实体
        List<MesLhScheduleResult> day3MesList = convertToMesList(day3List, dataVersion, companyCode, factoryCode);

        // 窗口首日数据：仅更新早中班（不覆盖夜班），不存在则插入
        upsertDay1LhScheduleResult(day1MesList, dataVersion);
        // 窗口次日数据：更新（存在则更新，不存在则插入）
        upsertLhScheduleResult(day2MesList, dataVersion);

        // 排程日期当天数据：先删除后插入（确保数据干净）
        insertLhScheduleResult(day3MesList, lastDate, dataVersion);

        // 合并所有数据用于发送MQ
        List<MesLhScheduleResult> allMesList = new ArrayList<>();
        allMesList.addAll(day1MesList);
        allMesList.addAll(day2MesList);
        allMesList.addAll(day3MesList);

        if (CollectionUtils.isEmpty(allMesList)) {
            return AjaxResult.success("没有需要下发的数据");
        }

        // 发送MQ通知MES
        return sendMqNotice(allMesList, firstDate, lastDate, dataVersion, factoryCode, companyCode);
    }

    /**
     * 按日期过滤数据
     */
    private List<LhScheduleResultIssue> filterByDate(List<LhScheduleResultIssue> list, LocalDate date) {
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        return list.stream()
                .filter(item -> item.getScheduleDate() != null)
                .filter(item -> {
                    java.time.LocalDate itemDate = item.getScheduleDate().toLocalDate();
                    return itemDate.equals(date);
                })
                .collect(Collectors.toList());
    }

    /**
     * 转换为MES实体列表
     */
    private List<MesLhScheduleResult> convertToMesList(List<LhScheduleResultIssue> list, String dataVersion, 
                                                     String companyCode, String factoryCode) {
        List<MesLhScheduleResult> result = new ArrayList<>();
        for (LhScheduleResultIssue item : list) {
            MesLhScheduleResult mesItem = convertToMesEntity(item, dataVersion, companyCode, factoryCode);
            result.add(mesItem);
        }
        return result;
    }

    /**
     * 转换为MES中间表实体
     */
    private MesLhScheduleResult convertToMesEntity(LhScheduleResultIssue item, String dataVersion, 
                                                String companyCode, String factoryCode) {
        MesLhScheduleResult mesItem = new MesLhScheduleResult();
        BeanUtils.copyProperties(item, mesItem);
        mesItem.setDataVersion(dataVersion);
        mesItem.setCompanyCode(companyCode);
        mesItem.setFactoryCode(factoryCode);
        // 将LocalDateTime转换为LocalDate
        if (item.getScheduleDate() != null) {
            mesItem.setScheduleDate(item.getScheduleDate().toLocalDate());
        }
        return mesItem;
    }

    /**
     * 更新或插入数据（存在则更新，不存在则插入）
     * 中间表MES_LH_SCHEDULE_RESULT建在MES分库，Mapper已通过@DS(DataSource.MES)指定数据源
     * 分批处理避免SQL Server参数上限2100的问题
     * 匹配键：排程日期+硫化机台编码+物料编码+工单号（不含版本号）
     * 实现方式：delete + insert（而非逐条 update）
     *   - 历史问题：旧版本逐条UPDATE时WHERE含DATA_VERSION，重新发布因版本号变化导致0条更新、全部走insert，造成多版本残留。
     *   - 现在改为：对已存在的键，先批量删除该键的所有历史版本数据，再批量插入本次发布的新版本数据。
     *   - 这样无论历史有多少版本残留，最终每个键只会保留本次发布的 1 条最新版本记录。
     *   - MES 侧无回写字段，删除不会丢失业务数据。
     */
    private void upsertLhScheduleResult(List<MesLhScheduleResult> mesList, String dataVersion) {
        if (CollectionUtils.isEmpty(mesList)) {
            return;
        }
        log.info("upsert处理开始，总记录数：{}", mesList.size());
        // 分批查询已有记录，按排程日期+硫化机台编码+物料编码+工单号匹配（不含版本号）
        Set<String> existingKeys = new HashSet<>();
        for (List<MesLhScheduleResult> batch : partitionList(mesList)) {
            List<MesLhScheduleResult> existingRecords = lhScheduleResultIssueMapper.selectExistingByScheduleDateAndMachine(batch);
            existingRecords.stream()
                    .map(r -> r.getScheduleDate() + "|" + r.getLhMachineCode() + "|" + r.getMaterialCode() + "|" + r.getOrderNo())
                    .forEach(existingKeys::add);
        }
        // 根据查询结果分组：已有记录走"删除+插入"覆盖，不存在记录走新增
        List<MesLhScheduleResult> replaceList = new ArrayList<>();
        List<MesLhScheduleResult> insertList = new ArrayList<>();
        for (MesLhScheduleResult mesItem : mesList) {
            String key = mesItem.getScheduleDate() + "|" + mesItem.getLhMachineCode() + "|" + mesItem.getMaterialCode() + "|" + mesItem.getOrderNo();
            if (existingKeys.contains(key)) {
                replaceList.add(mesItem);
            } else {
                insertList.add(mesItem);
            }
        }
        log.info("upsert分组结果：需覆盖{}条，需新增{}条", replaceList.size(), insertList.size());
        // 覆盖处理：先批量删除该键的所有历史版本记录，再批量插入本次发布的新版本数据
        int replaceBatchCount = 0;
        for (List<MesLhScheduleResult> batch : partitionList(replaceList)) {
            lhScheduleResultIssueMapper.batchDeleteByScheduleDateAndMachine(batch);
            lhScheduleResultIssueMapper.batchInsertLhScheduleResult(batch);
            replaceBatchCount += batch.size();
        }
        // 新增处理
        int insertBatchCount = 0;
        for (List<MesLhScheduleResult> batch : partitionList(insertList)) {
            lhScheduleResultIssueMapper.batchInsertLhScheduleResult(batch);
            insertBatchCount += batch.size();
        }
        log.info("upsert处理完成：实际覆盖{}条，实际新增{}条", replaceBatchCount, insertBatchCount);
    }

    /**
     * 窗口首日（T日，排程日期前一天）更新或插入数据
     * 仅更新早中班（class2、class3），不覆盖夜班（class1）数据
     * 窗口首日无夜班排产数据，避免将MES已有的夜班数据覆盖为空
     * 中间表MES_LH_SCHEDULE_RESULT建在MES分库，Mapper已通过@DS(DataSource.MES)指定数据源
     * 分批处理避免SQL Server参数上限2100的问题
     *
     * 实现方式：delete + insert（而非逐条 update）
     *   - 先查询已有记录的完整数据（含class1），将class1数据合并到本次下发数据中
     *   - 然后对已存在的键，先删除所有历史版本，再插入含class1的合并数据
     *   - 对不存在的键，直接插入（class1为null，因为窗口首日无夜班数据）
     */
    private void upsertDay1LhScheduleResult(List<MesLhScheduleResult> mesList, String dataVersion) {
        if (CollectionUtils.isEmpty(mesList)) {
            return;
        }
        log.info("Day1 upsert处理开始，总记录数：{}", mesList.size());
        // 分批查询已有记录（返回完整记录，含class1数据），按排程日期+硫化机台编码+物料编码+工单号匹配（不含版本号）
        Map<String, MesLhScheduleResult> existingMap = new HashMap<>();
        for (List<MesLhScheduleResult> batch : partitionList(mesList)) {
            List<MesLhScheduleResult> existingRecords = lhScheduleResultIssueMapper.selectExistingByScheduleDateAndMachine(batch);
            for (MesLhScheduleResult existing : existingRecords) {
                String key = existing.getScheduleDate() + "|" + existing.getLhMachineCode() + "|" + existing.getMaterialCode() + "|" + existing.getOrderNo();
                // 保留最后一个版本的数据（按查询顺序，实际上同一键可能有多版本残留，取最后一个即可）
                existingMap.put(key, existing);
            }
        }
        // 根据查询结果分组：已有记录需合并class1后走"删除+插入"覆盖，不存在记录走新增
        List<MesLhScheduleResult> replaceList = new ArrayList<>();
        List<MesLhScheduleResult> insertList = new ArrayList<>();
        for (MesLhScheduleResult mesItem : mesList) {
            String key = mesItem.getScheduleDate() + "|" + mesItem.getLhMachineCode() + "|" + mesItem.getMaterialCode() + "|" + mesItem.getOrderNo();
            if (existingMap.containsKey(key)) {
                // 合并class1数据：从已有记录中保留class1（夜班）数据，本次下发只更新class2/class3
                MesLhScheduleResult existing = existingMap.get(key);
                mesItem.setClass1PlanQty(existing.getClass1PlanQty());
                mesItem.setClass1PlanQtySeq(existing.getClass1PlanQtySeq());
                mesItem.setClass1AnalysisInput(existing.getClass1AnalysisInput());
                mesItem.setClass1Analysis(existing.getClass1Analysis());
                mesItem.setClass1ExampleType(existing.getClass1ExampleType());
                mesItem.setClass1ExampleNo(existing.getClass1ExampleNo());
                mesItem.setClass1PlanType(existing.getClass1PlanType());
                replaceList.add(mesItem);
            } else {
                insertList.add(mesItem);
            }
        }
        log.info("Day1 upsert分组结果：需覆盖{}条，需新增{}条", replaceList.size(), insertList.size());
        // 覆盖处理：先批量删除该键的所有历史版本记录，再批量插入含class1合并数据的新版本记录
        int replaceBatchCount = 0;
        for (List<MesLhScheduleResult> batch : partitionList(replaceList)) {
            lhScheduleResultIssueMapper.batchDeleteByScheduleDateAndMachine(batch);
            lhScheduleResultIssueMapper.batchInsertLhScheduleResult(batch);
            replaceBatchCount += batch.size();
        }
        // 新增处理
        int insertBatchCount = 0;
        for (List<MesLhScheduleResult> batch : partitionList(insertList)) {
            lhScheduleResultIssueMapper.batchInsertLhScheduleResult(batch);
            insertBatchCount += batch.size();
        }
        log.info("Day1 upsert处理完成：实际覆盖{}条，实际新增{}条", replaceBatchCount, insertBatchCount);
    }

    /**
     * 插入数据（先删除指定日期的旧数据，再插入新数据）
     * 中间表MES_LH_SCHEDULE_RESULT建在MES分库，Mapper已通过@DS(DataSource.MES)指定数据源
     * 分批插入避免SQL Server参数上限2100的问题
     */
    private void insertLhScheduleResult(List<MesLhScheduleResult> mesList, LocalDate scheduleDate, String dataVersion) {
        if (CollectionUtils.isEmpty(mesList)) {
            return;
        }
        String dateStr = scheduleDate.format(DATE_FORMATTER);
        lhScheduleResultIssueMapper.deleteByScheduleDate(dateStr, dataVersion);
        // 分批插入
        for (List<MesLhScheduleResult> batch : partitionList(mesList)) {
            lhScheduleResultIssueMapper.batchInsertLhScheduleResult(batch);
        }
    }

    /**
     * 发送MQ通知
     */
    private AjaxResult sendMqNotice(List<MesLhScheduleResult> allMesList, LocalDate startDate,
                                  LocalDate endDate, String dataVersion,
                                  String factoryCode, String companyCode) {
        AjaxResult ajaxResult;
        try {
            SyncParamsVO syncParamsVO = new SyncParamsVO();
            syncParamsVO.setSyncKey(ItfSyncKeyEnum.SYNC_LH_SCHEDULE_RESULT.getCode());
            syncParamsVO.setDataVersion(dataVersion);

            // 请求参数
            JSONObject params = new JSONObject();
            params.put("rowCount", allMesList.size());
            params.put("startDate", startDate.format(DATE_FORMATTER));
            params.put("endDate", endDate.format(DATE_FORMATTER));
            syncParamsVO.setParams(params);
            syncParamsVO.setDataSys(SysCode.APS);
            syncParamsVO.setDockSys(com.zlt.aps.common.core.constant.ApsConstant.DOCK_SYS_MES);
            syncParamsVO.setFactoryCode(factoryCode);
            syncParamsVO.setCompanyCode(companyCode);

            // 往消息队列发送消息
            syncDataHandle.syncNotice(syncParamsVO);

            // 取回mes的反馈结果
            SyncDataLogs logs = syncDataLogsService.getSyncDataResult(dataVersion);
            String status = logs.getStatus();
            if (com.zlt.aps.common.core.constant.ApsConstant.IS_RELEASE.equals(status)) {
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
}

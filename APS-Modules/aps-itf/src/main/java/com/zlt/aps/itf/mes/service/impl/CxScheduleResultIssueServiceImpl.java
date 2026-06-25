package com.zlt.aps.itf.mes.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.itf.constant.SysCode;
import com.zlt.aps.itf.mes.enums.ItfSyncKeyEnum;
import com.zlt.aps.itf.mes.mapper.CxScheduleResultIssueMapper;
import com.zlt.aps.itf.mes.service.ICxScheduleResultIssueService;
import com.zlt.aps.itf.vo.MesCxScheduleResult;
import com.zlt.aps.itf.vo.SyncDataLogs;
import com.zlt.aps.mp.api.domain.entity.CxScheduleResultIssue;
import com.zlt.sync.handle.SyncDataHandle;
import com.zlt.sync.povo.SyncParamsVO;
import com.zlt.sync.service.SyncDataLogsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 成型排程结果下发服务实现类
 *
 * @author APS Team
 * @since 2.0.0
 */
@Slf4j
@Service
public class CxScheduleResultIssueServiceImpl implements ICxScheduleResultIssueService {

    @Autowired
    private SyncDataHandle syncDataHandle;

    @Autowired
    private SyncDataLogsService syncDataLogsService;

    @Autowired
    private CxScheduleResultIssueMapper cxScheduleResultIssueMapper;

    /**
     * 日期格式化器
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * SQL Server单次请求参数上限2100，每条记录35个参数，安全批次大小为50
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
     * 下发成型排程结果到MES
     * 业务规则：
     * 1. 当天数据：更新（存在则更新，不存在则插入）
     * 2. 隔天数据：更新（存在则更新，不存在则插入）
     * 3. 第三天数据：插入
     *
     * @param cxScheduleResultIssueList 成型排程结果列表
     * @param factoryCode               厂别
     * @param companyCode               分公司编码
     * @return 下发结果
     */
    @Override
    public AjaxResult issueCxScheduleResult(List<CxScheduleResultIssue> cxScheduleResultIssueList, String factoryCode, String companyCode) {
        if (CollectionUtils.isEmpty(cxScheduleResultIssueList)) {
            return AjaxResult.success();
        }

        // 获取下发接口版本号
        String dataVersion = syncDataHandle.getDataVersion(ItfSyncKeyEnum.SYNC_CX_SCHEDULE_RESULT.getCode());

        // 获取今天、明天、后天的日期
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate dayAfterTomorrow = today.plusDays(2);

        // 按日期分组处理数据
        List<CxScheduleResultIssue> todayList = filterByDate(cxScheduleResultIssueList, today);
        List<CxScheduleResultIssue> tomorrowList = filterByDate(cxScheduleResultIssueList, tomorrow);
        List<CxScheduleResultIssue> dayAfterTomorrowList = filterByDate(cxScheduleResultIssueList, dayAfterTomorrow);

        // 处理当天的数据：转换为MES实体
        List<MesCxScheduleResult> todayMesList = convertToMesList(todayList, dataVersion, companyCode, factoryCode);

        // 处理明天的数据：转换为MES实体
        List<MesCxScheduleResult> tomorrowMesList = convertToMesList(tomorrowList, dataVersion, companyCode, factoryCode);

        // 处理后天的数据：转换为MES实体
        List<MesCxScheduleResult> dayAfterTomorrowMesList = convertToMesList(dayAfterTomorrowList, dataVersion, companyCode, factoryCode);

        // 当天和隔天数据：更新（存在则更新，不存在则插入）
        upsertCxScheduleResult(todayMesList, dataVersion);
        upsertCxScheduleResult(tomorrowMesList, dataVersion);

        // 第三天数据：先删除后插入（确保数据干净）
        insertCxScheduleResult(dayAfterTomorrowMesList, dayAfterTomorrow, dataVersion);

        // 合并所有数据用于发送MQ
        List<MesCxScheduleResult> allMesList = new ArrayList<>();
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
     * 中间表MES_CX_SCHEDULE_RESULT建在MES分库，Mapper已通过@DS(DataSource.MES)指定数据源
     * 分批处理避免SQL Server参数上限2100的问题
     */
    private void upsertCxScheduleResult(List<MesCxScheduleResult> mesList, String dataVersion) {
        if (CollectionUtils.isEmpty(mesList)) {
            return;
        }
        // 分批查询已有记录，按排程日期+机台编码+胎胚编码+版本号匹配
        Set<String> existingKeys = new HashSet<>();
        for (List<MesCxScheduleResult> batch : partitionList(mesList)) {
            List<MesCxScheduleResult> existingRecords = cxScheduleResultIssueMapper.selectExistingByScheduleDateAndMachine(batch);
            existingRecords.stream()
                    .map(r -> r.getScheduleDate() + "|" + r.getMachineCode() + "|" + r.getEmbryoCode() + "|" + r.getDataVersion())
                    .forEach(existingKeys::add);
        }
        // 根据查询结果分组：已有记录走批量更新，不存在记录走批量新增
        List<MesCxScheduleResult> updateList = new ArrayList<>();
        List<MesCxScheduleResult> insertList = new ArrayList<>();
        for (MesCxScheduleResult mesItem : mesList) {
            String key = mesItem.getScheduleDate() + "|" + mesItem.getMachineCode() + "|" + mesItem.getEmbryoCode() + "|" + mesItem.getDataVersion();
            if (existingKeys.contains(key)) {
                updateList.add(mesItem);
            } else {
                insertList.add(mesItem);
            }
        }
        // 分批更新
        for (List<MesCxScheduleResult> batch : partitionList(updateList)) {
            cxScheduleResultIssueMapper.batchUpdateByScheduleDateAndMachine(batch);
        }
        // 分批插入
        for (List<MesCxScheduleResult> batch : partitionList(insertList)) {
            cxScheduleResultIssueMapper.batchInsertCxScheduleResult(batch);
        }
    }

    /**
     * 插入数据（先删除指定日期的旧数据，再插入新数据）
     * 中间表MES_CX_SCHEDULE_RESULT建在MES分库，Mapper已通过@DS(DataSource.MES)指定数据源
     * 分批插入避免SQL Server参数上限2100的问题
     */
    private void insertCxScheduleResult(List<MesCxScheduleResult> mesList, LocalDate scheduleDate, String dataVersion) {
        if (CollectionUtils.isEmpty(mesList)) {
            return;
        }
        String dateStr = scheduleDate.format(DATE_FORMATTER);
        cxScheduleResultIssueMapper.deleteByScheduleDate(dateStr, dataVersion);
        // 分批插入
        for (List<MesCxScheduleResult> batch : partitionList(mesList)) {
            cxScheduleResultIssueMapper.batchInsertCxScheduleResult(batch);
        }
    }

    /**
     * 发送MQ通知
     */
    private AjaxResult sendMqNotice(List<MesCxScheduleResult> allMesList, LocalDate today, 
                                     LocalDate dayAfterTomorrow, String dataVersion,
                                     String factoryCode, String companyCode) {
        AjaxResult ajaxResult;
        try {
            SyncParamsVO syncParamsVO = new SyncParamsVO();
            syncParamsVO.setSyncKey(ItfSyncKeyEnum.SYNC_CX_SCHEDULE_RESULT.getCode());
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
    private List<CxScheduleResultIssue> filterByDate(List<CxScheduleResultIssue> list, LocalDate date) {
        return list.stream()
                .filter(item -> item.getScheduleDate() != null)
                .filter(item -> {
                    LocalDate itemDate = item.getScheduleDate().toLocalDate();
                    return itemDate.equals(date);
                })
                .collect(Collectors.toList());
    }

    /**
     * 转换为MES实体列表
     */
    private List<MesCxScheduleResult> convertToMesList(List<CxScheduleResultIssue> list, String dataVersion,
                                                        String companyCode, String factoryCode) {
        List<MesCxScheduleResult> result = new ArrayList<>();
        for (CxScheduleResultIssue item : list) {
            MesCxScheduleResult mesItem = convertToMesEntity(item, dataVersion, companyCode, factoryCode);
            result.add(mesItem);
        }
        return result;
    }

    /**
     * 转换为MES中间表实体
     */
    private MesCxScheduleResult convertToMesEntity(CxScheduleResultIssue item, String dataVersion,
                                                    String companyCode, String factoryCode) {
        MesCxScheduleResult mesItem = new MesCxScheduleResult();
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
}

package com.zlt.aps.itf.mes.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResultIssue;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.itf.constant.SysCode;
import com.zlt.aps.itf.mes.enums.ItfSyncKeyEnum;
import com.zlt.aps.itf.mes.service.ICd15ScheduleResultIssueService;
import com.zlt.aps.itf.vo.SyncDataLogs;
import com.zlt.sync.handle.SyncDataHandle;
import com.zlt.sync.povo.SyncParamsVO;
import com.zlt.sync.service.SyncDataLogsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 斜裁排程结果下发 MES 实现。
 *
 * <p>生成数据版本后先按业务键覆盖 MES 中间表，事务提交成功后再发送
 * MQ 通知，最后根据同步日志反馈发布结果。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd15ScheduleResultIssueServiceImpl
        implements ICd15ScheduleResultIssueService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SyncDataHandle syncDataHandle;
    private final SyncDataLogsService syncDataLogsService;
    private final Cd15ScheduleResultIssueWriter issueWriter;

    @Override
    public AjaxResult issueCd15ScheduleResult(
            List<Cd15ScheduleResultIssue> issueList,
            String factoryCode,
            String companyCode) {
        if (CollectionUtils.isEmpty(issueList)) {
            return AjaxResult.success(I18nUtil.getMessage(
                    "ui.cd15.publish.noIssueData"));
        }
        String traceId = issueList.get(0).getPublishTraceId();
        String dataVersion = syncDataHandle.getDataVersion(
                ItfSyncKeyEnum.SYNC_CD15_SCHEDULE_RESULT.getCode());
        List<LocalDate> scheduleDates = issueList.stream()
                .filter(issue -> issue.getScheduleDate() != null)
                .map(issue -> this.toLocalDate(issue.getScheduleDate()))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        if (scheduleDates.isEmpty()) {
            log.warn("斜裁排程下发无有效班次日期, traceId={}", traceId);
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.publish.noIssueData"));
        }
        LocalDate startDate = scheduleDates.get(0);
        LocalDate endDate = scheduleDates.get(scheduleDates.size() - 1);
        try {
            int rowCount = this.issueWriter.replace(issueList, dataVersion,
                    companyCode, factoryCode);
            log.info("斜裁排程下发 MES, traceId={}, dataVersion={}, "
                            + "factoryCode={}, rowCount={}, dateRange={}~{}",
                    traceId, dataVersion, factoryCode, rowCount,
                    startDate.format(DATE_FORMATTER),
                    endDate.format(DATE_FORMATTER));
            return this.sendMqNotice(rowCount, startDate, endDate,
                    dataVersion, factoryCode, companyCode, traceId);
        } catch (Exception exception) {
            log.error("斜裁排程写入 MES 中间表失败, traceId={}, dataVersion={}",
                    traceId, dataVersion, exception);
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.publish.failed"));
        }
    }

    /** 发送同步通知并读取 MES 反馈。 */
    private AjaxResult sendMqNotice(
            int rowCount,
            LocalDate startDate,
            LocalDate endDate,
            String dataVersion,
            String factoryCode,
            String companyCode,
            String traceId) {
        try {
            SyncParamsVO syncParams = new SyncParamsVO();
            syncParams.setSyncKey(
                    ItfSyncKeyEnum.SYNC_CD15_SCHEDULE_RESULT.getCode());
            syncParams.setDataVersion(dataVersion);
            JSONObject parameters = new JSONObject();
            parameters.put("rowCount", rowCount);
            parameters.put("startDate", startDate.format(DATE_FORMATTER));
            parameters.put("endDate", endDate.format(DATE_FORMATTER));
            parameters.put("publishTraceId", traceId);
            syncParams.setParams(parameters);
            syncParams.setDataSys(SysCode.APS);
            syncParams.setDockSys(ApsConstant.DOCK_SYS_MES);
            syncParams.setFactoryCode(factoryCode);
            syncParams.setCompanyCode(companyCode);
            syncDataHandle.syncNotice(syncParams);

            SyncDataLogs syncResult =
                    syncDataLogsService.getSyncDataResult(dataVersion);
            if (syncResult != null && ApsConstant.IS_RELEASE.equals(
                    syncResult.getStatus())) {
                return AjaxResult.success(I18nUtil.getMessage(
                        "ui.cd15.publish.success"));
            }
            String failureMessage = syncResult == null
                    || syncResult.getMsg() == null
                    ? I18nUtil.getMessage("ui.cd15.publish.failed")
                    : syncResult.getMsg();
            return AjaxResult.error(failureMessage);
        } catch (Exception exception) {
            log.error("斜裁排程下发 MES 通知失败, traceId={}",
                    traceId, exception);
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.publish.failed"));
        }
    }

    /** 转换日期并兼容 java.sql.Date。 */
    private LocalDate toLocalDate(Date value) {
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}

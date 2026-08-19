package com.zlt.aps.itf.mes.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.itf.constant.SysCode;
import com.zlt.aps.itf.mes.enums.ItfSyncKeyEnum;
import com.zlt.aps.itf.mes.service.ICd90ScheduleResultIssueService;
import com.zlt.aps.itf.vo.SyncDataLogs;
import com.zlt.aps.mp.api.domain.entity.Cd90ScheduleResultIssue;
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
 * 直裁排程结果下发 MES 服务实现。
 *
 * <p>生成数据版本后，先把班次级载荷聚合并覆盖写入
 * MES_CD90_SCHEDULE_RESULT，事务提交成功后再发送 MQ 通知 MES 拉取，
 * 最后轮询同步日志获取 MES 反馈。</p>
 *
 * @author APS Team
 * @since 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd90ScheduleResultIssueServiceImpl implements ICd90ScheduleResultIssueService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SyncDataHandle syncDataHandle;
    private final SyncDataLogsService syncDataLogsService;
    private final Cd90ScheduleResultIssueWriter issueWriter;

    @Override
    public AjaxResult issueCd90ScheduleResult(List<Cd90ScheduleResultIssue> cd90ScheduleResultIssueList,
                                              String factoryCode, String companyCode) {
        if (CollectionUtils.isEmpty(cd90ScheduleResultIssueList)) {
            return AjaxResult.success(I18nUtil.getMessage(
                    "ui.data.column.scheduleResult.noIssueData"));
        }

        String publishTraceId = cd90ScheduleResultIssueList.get(0).getPublishTraceId();
        String dataVersion = syncDataHandle.getDataVersion(ItfSyncKeyEnum.SYNC_CD90_SCHEDULE_RESULT.getCode());

        // 按排班日期分组（day1/day2/day3）
        List<LocalDate> distinctDates = cd90ScheduleResultIssueList.stream()
                .filter(item -> item.getScheduleDate() != null)
                .map(item -> toLocalDate(item.getScheduleDate()))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        if (distinctDates.isEmpty()) {
            log.warn("直裁排程下发: 列表无有效排班日期, traceId={}", publishTraceId);
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.data.column.scheduleResult.noIssueData"));
        }

        LocalDate firstDate = distinctDates.get(0);
        LocalDate lastDate = distinctDates.get(distinctDates.size() - 1);

        try {
            int rowCount = this.issueWriter.replace(
                    cd90ScheduleResultIssueList, dataVersion,
                    companyCode, factoryCode);
            log.info("直裁排程下发 MES: traceId={}, dataVersion={}, "
                            + "factoryCode={}, rowCount={}, 日期范围={}~{}",
                    publishTraceId, dataVersion, factoryCode, rowCount,
                    firstDate.format(DATE_FORMATTER),
                    lastDate.format(DATE_FORMATTER));
            return this.sendMqNotice(rowCount, firstDate, lastDate,
                    dataVersion, factoryCode, companyCode,
                    publishTraceId);
        } catch (Exception exception) {
            log.error("直裁排程写入 MES 中间表失败, traceId={}, dataVersion={}",
                    publishTraceId, dataVersion, exception);
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.data.column.scheduleResult.failedPublish"));
        }
    }

    /**
     * 发送 MQ 通知 MES 拉取，并轮询 SyncDataLogs 取回 MES 反馈。
     */
    private AjaxResult sendMqNotice(int rowCount, LocalDate startDate,
                                    LocalDate endDate, String dataVersion,
                                    String factoryCode, String companyCode, String publishTraceId) {
        try {
            SyncParamsVO syncParamsVO = new SyncParamsVO();
            syncParamsVO.setSyncKey(ItfSyncKeyEnum.SYNC_CD90_SCHEDULE_RESULT.getCode());
            syncParamsVO.setDataVersion(dataVersion);

            JSONObject params = new JSONObject();
            params.put("rowCount", rowCount);
            params.put("startDate", startDate.format(DATE_FORMATTER));
            params.put("endDate", endDate.format(DATE_FORMATTER));
            params.put("publishTraceId", publishTraceId);
            syncParamsVO.setParams(params);
            syncParamsVO.setDataSys(SysCode.APS);
            syncParamsVO.setDockSys(ApsConstant.DOCK_SYS_MES);
            syncParamsVO.setFactoryCode(factoryCode);
            syncParamsVO.setCompanyCode(companyCode);

            syncDataHandle.syncNotice(syncParamsVO);

            SyncDataLogs logs = syncDataLogsService.getSyncDataResult(dataVersion);
            if (logs != null && ApsConstant.IS_RELEASE.equals(
                    logs.getStatus())) {
                return AjaxResult.success(I18nUtil.getMessage("ui.data.column.scheduleResult.successPublish"));
            }
            String failureMessage = logs == null || logs.getMsg() == null
                    ? I18nUtil.getMessage(
                    "ui.data.column.scheduleResult.failedPublish")
                    : logs.getMsg();
            return AjaxResult.error(failureMessage);
        } catch (Exception e) {
            log.error("直裁排程下发 MQ 通知失败, traceId={}", publishTraceId, e);
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.failedPublish"));
        }
    }

    private LocalDate toLocalDate(Date date) {
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}

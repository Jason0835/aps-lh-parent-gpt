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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 直裁排程结果下发 MES 服务实现。
 *
 * <p>当前阶段 MES 中间表 MES_CD90_SCHEDULE_RESULT 字段定义需与 MES 团队对齐后建表，
 * 故本实现暂以日志记录 + MQ 通知骨架落地，中间表 Mapper 待表结构确认后补齐。
 * 业务流程已按 LH 模式打通：按排班日期分组 day1/day2/day3，upsert/update+insert 分别处理，
 * 完成后通过 SyncDataHandle.syncNotice 发送 MQ 通知 MES 拉取，并轮询 SyncDataLogs 取回 MES 反馈。</p>
 *
 * @author APS Team
 * @since 2.0.0
 */
@Slf4j
@Service
public class Cd90ScheduleResultIssueServiceImpl implements ICd90ScheduleResultIssueService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private SyncDataHandle syncDataHandle;

    @Autowired
    private SyncDataLogsService syncDataLogsService;

    @Override
    public AjaxResult issueCd90ScheduleResult(List<Cd90ScheduleResultIssue> cd90ScheduleResultIssueList,
                                              String factoryCode, String companyCode) {
        if (CollectionUtils.isEmpty(cd90ScheduleResultIssueList)) {
            return AjaxResult.success("没有需要下发的数据");
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
            return AjaxResult.success("没有需要下发的数据");
        }

        LocalDate firstDate = distinctDates.get(0);
        LocalDate lastDate = distinctDates.get(distinctDates.size() - 1);

        // TODO: MES 中间表 MES_CD90_SCHEDULE_RESULT 字段定义待与 MES 团队对齐后建表，
        //       届时补齐 MesCd90ScheduleResult 实体 + MesCd90ScheduleResultMapper，
        //       按 day1/day2 走 upsert、day3 走 delete+insert 的模式落库。
        //       当前阶段仅以日志记录下发动作，便于联调验证 MQ 链路。
        log.info("直裁排程下发 MES 占位: traceId={}, dataVersion={}, factoryCode={}, 记录数={}, 日期范围={}~{}",
                publishTraceId, dataVersion, factoryCode, cd90ScheduleResultIssueList.size(),
                firstDate.format(DATE_FORMATTER), lastDate.format(DATE_FORMATTER));

        return sendMqNotice(cd90ScheduleResultIssueList, firstDate, lastDate, dataVersion, factoryCode, companyCode, publishTraceId);
    }

    /**
     * 发送 MQ 通知 MES 拉取，并轮询 SyncDataLogs 取回 MES 反馈。
     */
    private AjaxResult sendMqNotice(List<Cd90ScheduleResultIssue> allIssueList, LocalDate startDate,
                                    LocalDate endDate, String dataVersion,
                                    String factoryCode, String companyCode, String publishTraceId) {
        try {
            SyncParamsVO syncParamsVO = new SyncParamsVO();
            syncParamsVO.setSyncKey(ItfSyncKeyEnum.SYNC_CD90_SCHEDULE_RESULT.getCode());
            syncParamsVO.setDataVersion(dataVersion);

            JSONObject params = new JSONObject();
            params.put("rowCount", allIssueList.size());
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
            String status = logs.getStatus();
            if (ApsConstant.IS_RELEASE.equals(status)) {
                return AjaxResult.success(I18nUtil.getMessage("ui.data.column.scheduleResult.successPublish"));
            }
            return AjaxResult.error(logs.getMsg());
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

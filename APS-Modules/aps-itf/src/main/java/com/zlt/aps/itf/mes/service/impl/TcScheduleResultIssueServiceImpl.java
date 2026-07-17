package com.zlt.aps.itf.mes.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.itf.constant.SysCode;
import com.zlt.aps.itf.mes.enums.ItfSyncKeyEnum;
import com.zlt.aps.itf.mes.mapper.TcScheduleResultIssueMapper;
import com.zlt.aps.itf.mes.service.ITcScheduleResultIssueService;
import com.zlt.aps.itf.vo.SyncDataLogs;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResultIssue;
import com.zlt.aps.tc.api.domain.vo.TcReleaseFeedbackItemVo;
import com.zlt.aps.tc.api.domain.vo.TcReleaseFeedbackVo;
import com.zlt.sync.handle.SyncDataHandle;
import com.zlt.sync.povo.SyncParamsVO;
import com.zlt.sync.service.SyncDataLogsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎侧排程结果MES中间表写入、MQ通知和状态查询实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TcScheduleResultIssueServiceImpl implements ITcScheduleResultIssueService {

    private static final int BATCH_SIZE = 50;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final TcScheduleResultIssueMapper issueMapper;
    private final SyncDataHandle syncDataHandle;
    private final SyncDataLogsService syncDataLogsService;

    /**
     * 按D、D+1更新和D+2替换规则下发胎侧排程。
     *
     * @param issueList 下发记录
     * @return 下发结果
     */
    @Override
    public AjaxResult issue(List<TcScheduleResultIssue> issueList) {
        if (CollectionUtils.isEmpty(issueList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.tc.schedule.release.noIssueData"));
        }
        String dataVersion = issueList.stream().map(TcScheduleResultIssue::getDataVersion)
                .filter(StrUtil::isNotBlank).findFirst()
                .orElseGet(() -> this.syncDataHandle.getDataVersion(
                        ItfSyncKeyEnum.SYNC_TC_SCHEDULE_RESULT.getCode()));
        issueList.stream().forEach(item -> item.setDataVersion(dataVersion));
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate dayAfterTomorrow = today.plusDays(2);
        List<TcScheduleResultIssue> todayList = this.filterByDate(issueList, today);
        List<TcScheduleResultIssue> tomorrowList = this.filterByDate(issueList, tomorrow);
        List<TcScheduleResultIssue> dayAfterTomorrowList = this.filterByDate(issueList, dayAfterTomorrow);
        this.upsert(todayList);
        this.upsert(tomorrowList);
        if (CollectionUtils.isNotEmpty(dayAfterTomorrowList)) {
            this.issueMapper.deleteByScheduleDate(dayAfterTomorrow.format(DATE_FORMATTER), dataVersion);
            this.partition(dayAfterTomorrowList).stream().forEach(this.issueMapper::batchInsert);
        }
        List<TcScheduleResultIssue> sentList = new ArrayList<>();
        sentList.addAll(todayList);
        sentList.addAll(tomorrowList);
        sentList.addAll(dayAfterTomorrowList);
        if (sentList.isEmpty()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.tc.schedule.release.noIssueData"));
        }
        this.sendNotice(sentList, dataVersion, today, dayAfterTomorrow);
        SyncDataLogs logs = this.syncDataLogsService.getSyncDataResult(dataVersion);
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
     * 查询MES同步日志并按已写入幂等键生成反馈。
     *
     * @param dataVersion 数据版本
     * @return 发布反馈
     */
    @Override
    public TcReleaseFeedbackVo queryStatus(String dataVersion) {
        TcReleaseFeedbackVo feedback = new TcReleaseFeedbackVo();
        feedback.setDataVersion(dataVersion);
        feedback.setCallbackVersion(dataVersion);
        if (StrUtil.isBlank(dataVersion)) {
            return feedback;
        }
        SyncDataLogs logs = this.syncDataLogsService.getSyncDataResult(dataVersion);
        String status = this.toFeedbackStatus(logs);
        if (status == null) {
            return feedback;
        }
        List<String> idempotencyKeyList = this.issueMapper.selectIdempotencyKeys(dataVersion);
        feedback.setItems(CollectionUtils.emptyIfNull(idempotencyKeyList).stream().map(key -> {
            TcReleaseFeedbackItemVo item = new TcReleaseFeedbackItemVo();
            item.setIdempotencyKey(key);
            item.setFeedbackStatus(status);
            item.setMessage(logs == null ? null : logs.getMsg());
            return item;
        }).collect(Collectors.toList()));
        return feedback;
    }

    /**
     * 更新或插入D、D+1排程记录。
     *
     * @param issueList 下发记录
     */
    private void upsert(List<TcScheduleResultIssue> issueList) {
        if (CollectionUtils.isEmpty(issueList)) {
            return;
        }
        Set<String> existingKeySet = new HashSet<>();
        this.partition(issueList).stream().map(this.issueMapper::selectExisting)
                .filter(Objects::nonNull).flatMap(List::stream)
                .map(this::buildBusinessKey).forEach(existingKeySet::add);
        List<TcScheduleResultIssue> updateList = issueList.stream()
                .filter(item -> existingKeySet.contains(this.buildBusinessKey(item)))
                .collect(Collectors.toList());
        List<TcScheduleResultIssue> insertList = issueList.stream()
                .filter(item -> !existingKeySet.contains(this.buildBusinessKey(item)))
                .collect(Collectors.toList());
        this.partition(updateList).stream().forEach(this.issueMapper::batchUpdate);
        this.partition(insertList).stream().forEach(this.issueMapper::batchInsert);
    }

    /**
     * 发送MES同步通知。
     *
     * @param issueList 实际下发记录
     * @param dataVersion 数据版本
     * @param startDate 开始日期
     * @param endDate 结束日期
     */
    private void sendNotice(List<TcScheduleResultIssue> issueList, String dataVersion,
                            LocalDate startDate, LocalDate endDate) {
        TcScheduleResultIssue firstItem = issueList.get(0);
        SyncParamsVO params = new SyncParamsVO();
        params.setSyncKey(ItfSyncKeyEnum.SYNC_TC_SCHEDULE_RESULT.getCode());
        params.setDataVersion(dataVersion);
        JSONObject requestParams = new JSONObject();
        requestParams.put("rowCount", issueList.size());
        requestParams.put("startDate", startDate.format(DATE_FORMATTER));
        requestParams.put("endDate", endDate.format(DATE_FORMATTER));
        params.setParams(requestParams);
        params.setDataSys(SysCode.APS);
        params.setDockSys(ApsConstant.DOCK_SYS_MES);
        params.setFactoryCode(firstItem.getFactoryCode());
        params.setCompanyCode(StrUtil.blankToDefault(firstItem.getCompanyCode(), firstItem.getFactoryCode()));
        this.syncDataHandle.syncNotice(params);
    }

    /**
     * 按日期过滤发布记录。
     *
     * @param issueList 发布记录
     * @param scheduleDate 日期
     * @return 日期内记录
     */
    private List<TcScheduleResultIssue> filterByDate(List<TcScheduleResultIssue> issueList,
                                                     LocalDate scheduleDate) {
        return issueList.stream().filter(item -> scheduleDate.equals(item.getScheduleDate()))
                .collect(Collectors.toList());
    }

    /**
     * 构造MES中间表业务键。
     *
     * @param item 发布记录
     * @return 业务键
     */
    private String buildBusinessKey(TcScheduleResultIssue item) {
        return item.getScheduleDate() + "|" + item.getMachineCode() + "|"
                + item.getSidewallCode() + "|" + item.getDataVersion();
    }

    /**
     * 分批避免SQL Server参数上限。
     *
     * @param sourceList 原始集合
     * @return 分批集合
     */
    private List<List<TcScheduleResultIssue>> partition(List<TcScheduleResultIssue> sourceList) {
        List<List<TcScheduleResultIssue>> batchList = new ArrayList<>();
        for (int index = 0; index < sourceList.size(); index += BATCH_SIZE) {
            batchList.add(sourceList.subList(index, Math.min(index + BATCH_SIZE, sourceList.size())));
        }
        return batchList;
    }

    /**
     * 将MES同步日志状态转换为发布反馈状态。
     *
     * @param logs 同步日志
     * @return 反馈状态，非终态返回null
     */
    private String toFeedbackStatus(SyncDataLogs logs) {
        if (logs == null || StrUtil.isBlank(logs.getStatus())) {
            return null;
        }
        if (ApsConstant.IS_RELEASE.equals(logs.getStatus())) {
            return "SUCCESS";
        }
        if (ApsConstant.TIMEOUT_FAILURE.equals(logs.getStatus())) {
            return "TIMEOUT";
        }
        if (ApsConstant.FAILURE_RELEASE.equals(logs.getStatus())) {
            return "FAILED";
        }
        return null;
    }
}

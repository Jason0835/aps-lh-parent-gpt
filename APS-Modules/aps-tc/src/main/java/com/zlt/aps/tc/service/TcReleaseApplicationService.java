package com.zlt.aps.tc.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.domain.vo.*;
import com.zlt.aps.tc.api.enums.TcAutoScheduleTaskStatusEnum;
import com.zlt.aps.tc.api.enums.TcBackgroundTaskTypeEnum;
import com.zlt.aps.tc.api.enums.TcReleaseStatusTransition;
import com.zlt.aps.tc.api.enums.TcScheduleReleaseStatusEnum;
import com.zlt.aps.tc.component.TcAutoScheduleExecutionGuard;
import com.zlt.aps.tc.component.TcScheduleResultIssueAssembler;
import com.zlt.aps.tc.domain.TcAutoScheduleTask;
import com.zlt.aps.tc.domain.TcReleaseTaskDetail;
import com.zlt.aps.tc.mapper.TcAutoScheduleTaskMapper;
import com.zlt.aps.tc.mapper.TcScheduleResultMapper;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎侧排程发布校验、任务创建和查询应用服务。
 */
@Service
@RequiredArgsConstructor
public class TcReleaseApplicationService {

    private final TcScheduleResultMapper scheduleResultMapper;
    private final TcAutoScheduleTaskMapper taskMapper;
    private final BaseDao baseDao;
    private final TcBackgroundTaskService backgroundTaskService;
    private final TcReleaseAsyncExecutor asyncExecutor;
    private final TcAutoScheduleExecutionGuard executionGuard;
    private final TcScheduleResultIssueAssembler issueAssembler;
    private final PlatformTransactionManager transactionManager;

    /**
     * 校验发布请求，不改变排程状态。
     *
     * @param request 发布请求
     * @return 校验结果
     */
    public TcReleaseValidateVo validate(TcReleaseRequestVo request) {
        TcReleaseValidateVo response = new TcReleaseValidateVo();
        try {
            List<TcScheduleResult> resultList = this.loadAndValidateResults(request, false);
            response.setAllowed(Boolean.TRUE);
            response.setSelectedCount(resultList.size());
        } catch (ServiceException exception) {
            response.setAllowed(Boolean.FALSE);
            response.setSelectedCount(request == null || request.getItems() == null ? 0 : request.getItems().size());
            TcAutoScheduleIssueVo issue = new TcAutoScheduleIssueVo();
            issue.setLevel("ERROR");
            issue.setStageCode("RELEASE_VALIDATE");
            issue.setStageName(I18nUtil.getMessage("ui.tc.schedule.release.validateStage"));
            issue.setCategory("VALIDATION_FAILED");
            issue.setMessage(exception.getMessage());
            response.setIssues(Collections.singletonList(issue));
        }
        return response;
    }

    /**
     * 创建发布任务并立即返回等待执行状态。
     *
     * @param request 发布请求
     * @return 发布任务
     * @throws ServiceException 并发、版本或状态校验失败时抛出
     */
    public TcReleaseTaskVo publish(TcReleaseRequestVo request) {
        this.validateRequestBase(request);
        String lockToken = this.executionGuard.acquire(request.getFactoryCode(), request.getScheduleDate());
        TcAutoScheduleTask task;
        try {
            TransactionTemplate transactionTemplate = new TransactionTemplate(this.transactionManager);
            task = transactionTemplate.execute(status -> this.createReleaseTask(request));
            if (task == null) {
                throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.release.createFailed"));
            }
        } finally {
            this.executionGuard.release(request.getFactoryCode(), request.getScheduleDate(), lockToken);
        }
        this.asyncExecutor.execute(task.getTaskId());
        return this.backgroundTaskService.toReleaseTaskVo(task);
    }

    /**
     * 查询指定发布任务。
     *
     * @param taskId 任务ID
     * @return 发布任务
     * @throws ServiceException 任务不存在或类型不正确时抛出
     */
    public TcReleaseTaskVo getTask(String taskId) {
        TcAutoScheduleTask task = this.backgroundTaskService.findByTaskId(taskId);
        if (task == null || !TcBackgroundTaskTypeEnum.RELEASE.getCode().equals(task.getTaskType())) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.release.taskNotFound"));
        }
        return this.backgroundTaskService.toReleaseTaskVo(task);
    }

    /**
     * 查询工厂日期最近一次发布任务。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 最近发布任务
     * @throws ServiceException 参数无效或任务不存在时抛出
     */
    public TcReleaseTaskVo getLatestTask(String factoryCode, Date scheduleDate) {
        if (StrUtil.isBlank(factoryCode) || scheduleDate == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.release.argumentsInvalid"));
        }
        TcAutoScheduleTask task = this.backgroundTaskService.findLatest(factoryCode, scheduleDate,
                TcBackgroundTaskTypeEnum.RELEASE.getCode());
        if (task == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.release.taskNotFound"));
        }
        return this.backgroundTaskService.toReleaseTaskVo(task);
    }

    /**
     * 在调用方短事务内创建发布主任务、明细并把结果置为发布中。
     *
     * @param request 发布请求
     * @return 新任务
     */
    private TcAutoScheduleTask createReleaseTask(TcReleaseRequestVo request) {
        if (this.backgroundTaskService.findActive(request.getFactoryCode(), request.getScheduleDate()) != null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.concurrentTask"));
        }
        List<TcScheduleResult> resultList = this.loadAndValidateResults(request, true);
        String taskId = TcScheduleConstants.RELEASE_TASK_ID_PREFIX + IdUtil.fastSimpleUUID().toUpperCase();
        String traceId = IdUtil.fastSimpleUUID().toUpperCase();
        String mesDataVersion = "TCREL-" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + "-"
                + IdUtil.fastSimpleUUID().substring(0, 8).toUpperCase();
        List<String> idempotencyKeyList = resultList.stream()
                .map(this.issueAssembler::buildIdempotencyKey).sorted().collect(Collectors.toList());

        TcAutoScheduleTask task = new TcAutoScheduleTask();
        task.setTaskId(taskId);
        task.setTaskType(TcBackgroundTaskTypeEnum.RELEASE.getCode());
        task.setFactoryCode(request.getFactoryCode());
        task.setScheduleDate(request.getScheduleDate());
        task.setBatchNo(resultList.get(0).getBatchNo());
        task.setTraceId(traceId);
        task.setTaskStatus(TcAutoScheduleTaskStatusEnum.PENDING.getCode());
        task.setProgress(0);
        task.setCurrentStage(TcAutoScheduleTaskStatusEnum.PENDING.getCode());
        task.setCurrentStageName(I18nUtil.getMessage("ui.tc.schedule.release.pending"));
        task.setMesDataVersion(mesDataVersion);
        task.setIdempotencyKey(DigestUtils.sha256Hex(String.join(";", idempotencyKeyList)));
        task.setInputVersion(task.getIdempotencyKey());
        task.setRequestSnapshot(JSON.toJSONString(request));
        task.setSummaryJson(JSON.toJSONString(this.buildReleaseSummary(resultList.size(), 0, 0, 0)));
        task.setCreateBy(this.currentUsername());
        if (this.taskMapper.insert(task) != 1) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.release.createFailed"));
        }

        List<TcReleaseTaskDetail> detailList = resultList.stream()
                .map(result -> this.buildReleaseDetail(task, result)).collect(Collectors.toList());
        this.baseDao.saveBatch(detailList);
        for (TcScheduleResult result : resultList) {
            LambdaUpdateWrapper<TcScheduleResult> updateWrapper = new LambdaUpdateWrapper<TcScheduleResult>()
                    .eq(TcScheduleResult::getId, result.getId())
                    .set(TcScheduleResult::getReleaseStatus, TcScheduleReleaseStatusEnum.RELEASING.getCode())
                    .set(TcScheduleResult::getUpdateBy, task.getCreateBy())
                    .set(TcScheduleResult::getUpdateTime, new Date());
            if (StrUtil.isBlank(result.getReleaseStatus())) {
                updateWrapper.and(wrapper -> wrapper.isNull(TcScheduleResult::getReleaseStatus)
                        .or().eq(TcScheduleResult::getReleaseStatus, ""));
            } else {
                updateWrapper.eq(TcScheduleResult::getReleaseStatus, result.getReleaseStatus());
            }
            if (result.getTaskVersion() == null) {
                updateWrapper.isNull(TcScheduleResult::getTaskVersion);
            } else {
                updateWrapper.eq(TcScheduleResult::getTaskVersion, result.getTaskVersion());
            }
            if (this.scheduleResultMapper.update(null, updateWrapper) != 1) {
                throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.concurrentChanged"));
            }
        }
        return task;
    }

    /**
     * 加载并校验当前批次所选结果。
     *
     * @param request 发布请求
     * @param lockRows 是否加数据库行锁
     * @return 已校验结果
     */
    private List<TcScheduleResult> loadAndValidateResults(TcReleaseRequestVo request, boolean lockRows) {
        this.validateRequestBase(request);
        List<Long> resultIdList = request.getItems().stream().map(TcReleaseItemVo::getResultId)
                .filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());
        if (resultIdList.size() != request.getItems().size()) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.release.itemsInvalid"));
        }
        boolean missingExpectedVersion = request.getItems().stream()
                .anyMatch(item -> item == null || item.getResultId() == null
                        || item.getExpectedTaskVersion() == null);
        if (missingExpectedVersion) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.release.itemsInvalid"));
        }
        LambdaQueryWrapper<TcScheduleResult> queryWrapper = new LambdaQueryWrapper<TcScheduleResult>()
                .in(TcScheduleResult::getId, resultIdList).orderByAsc(TcScheduleResult::getId);
        if (lockRows) {
            queryWrapper.last("FOR UPDATE");
        }
        List<TcScheduleResult> resultList = this.scheduleResultMapper.selectList(queryWrapper);
        if (resultList == null || resultList.size() != resultIdList.size()) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.concurrentChanged"));
        }
        Map<Long, Long> expectedVersionMap = request.getItems().stream().collect(Collectors.toMap(
                TcReleaseItemVo::getResultId, TcReleaseItemVo::getExpectedTaskVersion));
        String batchNo = resultList.get(0).getBatchNo();
        TcScheduleResult latestResult = this.scheduleResultMapper.selectOne(
                new LambdaQueryWrapper<TcScheduleResult>()
                        .eq(TcScheduleResult::getFactoryCode, request.getFactoryCode())
                        .eq(TcScheduleResult::getScheduleDate, request.getScheduleDate())
                        .orderByDesc(TcScheduleResult::getCreateTime)
                        .last("limit 1"));
        if (latestResult == null || !Objects.equals(batchNo, latestResult.getBatchNo())) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.release.batchChanged"));
        }
        for (TcScheduleResult result : resultList) {
            if (!Objects.equals(request.getFactoryCode(), result.getFactoryCode())
                    || !Objects.equals(DateUtil.formatDate(request.getScheduleDate()),
                    DateUtil.formatDate(result.getScheduleDate()))
                    || !Objects.equals(batchNo, result.getBatchNo())) {
                throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.release.rangeInvalid"));
            }
            Long currentVersion = result.getTaskVersion() == null ? 0L : result.getTaskVersion();
            if (!Objects.equals(expectedVersionMap.get(result.getId()), currentVersion)) {
                throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.concurrentChanged"));
            }
            if (!TcReleaseStatusTransition.canTransit(result.getReleaseStatus(),
                    TcScheduleReleaseStatusEnum.RELEASING.getCode())) {
                throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.release.statusInvalid"));
            }
            if (StrUtil.isBlank(result.getMachineCode()) || result.getMachineCode().contains(",")
                    || this.sumPlanQty(result).signum() <= 0) {
                throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.release.resultInvalid"));
            }
        }
        return resultList;
    }

    /**
     * 校验发布请求基础字段。
     *
     * @param request 发布请求
     */
    private void validateRequestBase(TcReleaseRequestVo request) {
        if (request == null || StrUtil.isBlank(request.getFactoryCode()) || request.getScheduleDate() == null
                || CollectionUtils.isEmpty(request.getItems())) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.release.argumentsInvalid"));
        }
    }

    /**
     * 汇总结果六班正计划量。
     *
     * @param result 排程结果
     * @return 六班计划量合计
     */
    private BigDecimal sumPlanQty(TcScheduleResult result) {
        BigDecimal totalQty = BigDecimal.ZERO;
        for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
            totalQty = totalQty.add(BigDecimalUtils.valueOf(result.getFieldValueByFieldName(String.format(
                    TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder))));
        }
        return totalQty;
    }

    /**
     * 构造发布任务明细。
     *
     * @param task 发布任务
     * @param result 排程结果
     * @return 发布明细
     */
    private TcReleaseTaskDetail buildReleaseDetail(TcAutoScheduleTask task, TcScheduleResult result) {
        TcReleaseTaskDetail detail = new TcReleaseTaskDetail();
        detail.setTaskId(task.getTaskId());
        detail.setResultId(result.getId());
        detail.setBatchNo(result.getBatchNo());
        detail.setOrderNo(result.getOrderNo());
        detail.setTaskVersion(result.getTaskVersion() == null ? 0L : result.getTaskVersion());
        detail.setIdempotencyKey(this.issueAssembler.buildIdempotencyKey(result));
        detail.setSourceStatus(StrUtil.blankToDefault(result.getReleaseStatus(),
                TcScheduleReleaseStatusEnum.NOT_RELEASED.getCode()));
        detail.setBeforeStatus(detail.getSourceStatus());
        detail.setAfterStatus(TcScheduleReleaseStatusEnum.RELEASING.getCode());
        detail.setCallbackStatus("PENDING");
        detail.setCreateBy(task.getCreateBy());
        return detail;
    }

    /**
     * 构造发布任务摘要。
     *
     * @param selectedCount 选择数量
     * @param successCount 成功数量
     * @param failedCount 失败数量
     * @param timeoutCount 超时数量
     * @return 发布摘要
     */
    private Map<String, Object> buildReleaseSummary(int selectedCount, int successCount,
                                                     int failedCount, int timeoutCount) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("schemaVersion", 1);
        summary.put("selectedCount", selectedCount);
        summary.put("successCount", successCount);
        summary.put("failedCount", failedCount);
        summary.put("timeoutCount", timeoutCount);
        return summary;
    }

    /**
     * 获取当前登录用户名作为审计人。
     *
     * @return 登录用户名
     */
    private String currentUsername() {
        return StrUtil.blankToDefault(SecurityUtils.getUsername(), "system");
    }
}

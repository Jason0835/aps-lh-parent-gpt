package com.zlt.aps.tm.service;

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
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.vo.*;
import com.zlt.aps.tm.api.enums.TmAutoScheduleTaskStatusEnum;
import com.zlt.aps.tm.api.enums.TmBackgroundTaskTypeEnum;
import com.zlt.aps.tm.api.enums.TmReleaseStatusTransition;
import com.zlt.aps.tm.api.enums.TmScheduleReleaseStatusEnum;
import com.zlt.aps.tm.component.TmScheduleResultIssueAssembler;
import com.zlt.aps.tm.domain.TmAutoScheduleTask;
import com.zlt.aps.tm.domain.TmReleaseTaskDetail;
import com.zlt.aps.tm.mapper.TmAutoScheduleTaskMapper;
import com.zlt.aps.tm.mapper.TmScheduleResultMapper;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎面排程发布校验、任务创建和查询应用服务（对齐胎侧 TcReleaseApplicationService）。
 *
 * <p>tm TmScheduleResult 无 taskVersion 字段，版本校验省略，按 id + 原发布状态做乐观锁；
 * 无独立执行锁，依靠 createReleaseTask 内 findActive 防并发。</p>
 */
@Service
@RequiredArgsConstructor
public class TmReleaseApplicationService {

    private final TmScheduleResultMapper scheduleResultMapper;
    private final TmAutoScheduleTaskMapper taskMapper;
    private final BaseDao baseDao;
    private final TmOperationTaskService operationTaskService;
    private final TmReleaseAsyncExecutor asyncExecutor;
    private final TmScheduleResultIssueAssembler issueAssembler;
    private final PlatformTransactionManager transactionManager;

    /**
     * 校验发布请求，不改变排程状态。
     *
     * @param request 发布请求
     * @return 校验结果
     */
    public TmReleaseValidateVo validate(TmReleaseRequestVo request) {
        TmReleaseValidateVo response = new TmReleaseValidateVo();
        try {
            List<TmScheduleResult> resultList = this.loadAndValidateResults(request, false);
            response.setAllowed(Boolean.TRUE);
            response.setSelectedCount(resultList.size());
        } catch (ServiceException exception) {
            response.setAllowed(Boolean.FALSE);
            response.setSelectedCount(request == null || request.getItems() == null ? 0 : request.getItems().size());
            TmAutoScheduleIssueVo issue = new TmAutoScheduleIssueVo();
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
     * @throws ServiceException 并发、范围或状态校验失败时抛出
     */
    public TmReleaseTaskVo publish(TmReleaseRequestVo request) {
        this.validateRequestBase(request);
        TmAutoScheduleTask task;
        TransactionTemplate transactionTemplate = new TransactionTemplate(this.transactionManager);
        task = transactionTemplate.execute(status -> this.createReleaseTask(request));
        if (task == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.release.createFailed"));
        }
        this.asyncExecutor.execute(task.getTaskId());
        return this.operationTaskService.toReleaseTaskVo(task);
    }

    /**
     * 查询指定发布任务。
     *
     * @param taskId 任务ID
     * @return 发布任务
     * @throws ServiceException 任务不存在或类型不正确时抛出
     */
    public TmReleaseTaskVo getTask(String taskId) {
        TmAutoScheduleTask task = this.operationTaskService.findByTaskId(taskId);
        if (task == null || !TmBackgroundTaskTypeEnum.RELEASE.getCode().equals(task.getTaskType())) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.release.taskNotFound"));
        }
        return this.operationTaskService.toReleaseTaskVo(task);
    }

    /**
     * 查询工厂日期最近一次发布任务。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 最近发布任务
     * @throws ServiceException 参数无效或任务不存在时抛出
     */
    public TmReleaseTaskVo getLatestTask(String factoryCode, Date scheduleDate) {
        if (StrUtil.isBlank(factoryCode) || scheduleDate == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.release.argumentsInvalid"));
        }
        TmAutoScheduleTask task = this.operationTaskService.findLatest(factoryCode, scheduleDate,
                TmBackgroundTaskTypeEnum.RELEASE.getCode());
        if (task == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.release.taskNotFound"));
        }
        return this.operationTaskService.toReleaseTaskVo(task);
    }

    /**
     * 在调用方短事务内创建发布主任务、明细并把结果置为发布中。
     *
     * @param request 发布请求
     * @return 新任务
     */
    private TmAutoScheduleTask createReleaseTask(TmReleaseRequestVo request) {
        if (this.operationTaskService.findActive(request.getFactoryCode(), request.getScheduleDate()) != null) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.operationTaskConcurrent"));
        }
        List<TmScheduleResult> resultList = this.loadAndValidateResults(request, true);
        String taskId = TmScheduleConstants.RELEASE_TASK_ID_PREFIX + IdUtil.fastSimpleUUID().toUpperCase();
        String traceId = IdUtil.fastSimpleUUID().toUpperCase();
        String mesDataVersion = "TMREL-" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + "-"
                + IdUtil.fastSimpleUUID().substring(0, 8).toUpperCase();

        TmAutoScheduleTask task = new TmAutoScheduleTask();
        task.setTaskId(taskId);
        task.setTaskType(TmBackgroundTaskTypeEnum.RELEASE.getCode());
        task.setFactoryCode(request.getFactoryCode());
        task.setScheduleDate(request.getScheduleDate());
        task.setBatchNo(resultList.get(0).getBatchNo());
        task.setTraceId(traceId);
        task.setTaskStatus(TmAutoScheduleTaskStatusEnum.PENDING.getCode());
        task.setProgress(0);
        task.setCurrentStage(TmAutoScheduleTaskStatusEnum.PENDING.getCode());
        task.setCurrentStageName(I18nUtil.getMessage("ui.tc.schedule.release.pending"));
        task.setMesDataVersion(mesDataVersion);
        task.setRequestSnapshot(JSON.toJSONString(request));
        task.setSummaryJson(JSON.toJSONString(this.buildReleaseSummary(resultList.size(), 0, 0, 0)));
        task.setCreateBy(this.currentUsername());
        if (this.taskMapper.insert(task) != 1) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.release.createFailed"));
        }

        List<TmReleaseTaskDetail> detailList = resultList.stream()
                .map(result -> this.buildReleaseDetail(task, result)).collect(Collectors.toList());
        this.baseDao.saveBatch(detailList);
        for (TmScheduleResult result : resultList) {
            LambdaUpdateWrapper<TmScheduleResult> updateWrapper = new LambdaUpdateWrapper<TmScheduleResult>()
                    .eq(TmScheduleResult::getId, result.getId())
                    .set(TmScheduleResult::getReleaseStatus, TmScheduleReleaseStatusEnum.RELEASING.getCode())
                    .set(TmScheduleResult::getUpdateBy, task.getCreateBy())
                    .set(TmScheduleResult::getUpdateTime, new Date());
            if (StrUtil.isBlank(result.getReleaseStatus())) {
                updateWrapper.and(wrapper -> wrapper.isNull(TmScheduleResult::getReleaseStatus)
                        .or().eq(TmScheduleResult::getReleaseStatus, ""));
            } else {
                updateWrapper.eq(TmScheduleResult::getReleaseStatus, result.getReleaseStatus());
            }
            if (this.scheduleResultMapper.update(null, updateWrapper) != 1) {
                throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.operationConcurrentChanged"));
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
    private List<TmScheduleResult> loadAndValidateResults(TmReleaseRequestVo request, boolean lockRows) {
        this.validateRequestBase(request);
        List<Long> resultIdList = request.getItems().stream().map(TmReleaseItemVo::getResultId)
                .filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());
        if (resultIdList.isEmpty()) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.release.itemsInvalid"));
        }
        LambdaQueryWrapper<TmScheduleResult> queryWrapper = new LambdaQueryWrapper<TmScheduleResult>()
                .in(TmScheduleResult::getId, resultIdList).orderByAsc(TmScheduleResult::getId);
        if (lockRows) {
            queryWrapper.last("FOR UPDATE");
        }
        List<TmScheduleResult> resultList = this.scheduleResultMapper.selectList(queryWrapper);
        if (resultList == null || resultList.size() != resultIdList.size()) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.operationConcurrentChanged"));
        }
        String batchNo = resultList.get(0).getBatchNo();
        TmScheduleResult latestResult = this.scheduleResultMapper.selectOne(
                new LambdaQueryWrapper<TmScheduleResult>()
                        .eq(TmScheduleResult::getFactoryCode, request.getFactoryCode())
                        .eq(TmScheduleResult::getScheduleDate, request.getScheduleDate())
                        .orderByDesc(TmScheduleResult::getCreateTime)
                        .last("limit 1"));
        if (latestResult == null || !Objects.equals(batchNo, latestResult.getBatchNo())) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.release.batchChanged"));
        }
        for (TmScheduleResult result : resultList) {
            if (!Objects.equals(request.getFactoryCode(), result.getFactoryCode())
                    || !Objects.equals(DateUtil.formatDate(request.getScheduleDate()),
                    DateUtil.formatDate(result.getScheduleDate()))
                    || !Objects.equals(batchNo, result.getBatchNo())) {
                throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.release.rangeInvalid"));
            }
            if (!TmReleaseStatusTransition.canTransit(result.getReleaseStatus(),
                    TmScheduleReleaseStatusEnum.RELEASING.getCode())) {
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
    private void validateRequestBase(TmReleaseRequestVo request) {
        if (request == null || StrUtil.isBlank(request.getFactoryCode()) || request.getScheduleDate() == null
                || request.getItems() == null || request.getItems().isEmpty()) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.release.argumentsInvalid"));
        }
    }

    /**
     * 汇总结果六班正计划量。
     *
     * @param result 排程结果
     * @return 六班计划量合计
     */
    private BigDecimal sumPlanQty(TmScheduleResult result) {
        BigDecimal totalQty = BigDecimal.ZERO;
        for (int shiftOrder = 1; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
            totalQty = totalQty.add(BigDecimalUtils.valueOf(result.getFieldValueByFieldName(String.format(
                    TmScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder))));
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
    private TmReleaseTaskDetail buildReleaseDetail(TmAutoScheduleTask task, TmScheduleResult result) {
        TmReleaseTaskDetail detail = new TmReleaseTaskDetail();
        detail.setTaskId(task.getTaskId());
        detail.setResultId(result.getId());
        detail.setBatchNo(result.getBatchNo());
        detail.setOrderNo(result.getOrderNo());
        detail.setTaskVersion(0L);
        detail.setIdempotencyKey(this.issueAssembler.buildIdempotencyKey(result));
        detail.setSourceStatus(StrUtil.blankToDefault(result.getReleaseStatus(),
                TmScheduleReleaseStatusEnum.NOT_RELEASED.getCode()));
        detail.setBeforeStatus(detail.getSourceStatus());
        detail.setAfterStatus(TmScheduleReleaseStatusEnum.RELEASING.getCode());
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

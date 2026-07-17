package com.zlt.aps.tc.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.domain.vo.TcAutoScheduleRequestVo;
import com.zlt.aps.tc.api.domain.vo.TcAutoScheduleResponseVo;
import com.zlt.aps.tc.api.enums.TcAutoScheduleTaskStatusEnum;
import com.zlt.aps.tc.component.TcAutoScheduleExecutionGuard;
import com.zlt.aps.tc.domain.TcAutoScheduleTask;
import com.zlt.aps.tc.engine.domain.TcPersistResult;
import com.zlt.aps.tc.engine.domain.TcScheduleContext;
import com.zlt.aps.tc.engine.template.TcScheduleTemplateImpl;
import com.zlt.aps.tc.mapper.TcScheduleResultMapper;
import com.zlt.aps.tc.service.ITcScheduleResultService;
import com.zlt.aps.tc.service.TcAutoScheduleAsyncExecutor;
import com.zlt.aps.tc.service.TcAutoScheduleTaskService;
import com.zlt.aps.tc.service.cache.TcAutoScheduleRedisCacheService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 胎侧自动排程结果业务实现。
 *
 * <p>负责请求校验、异步任务编排、执行锁和结果只读查询，六阶段算法由
 * {@link TcScheduleTemplateImpl} 及各步骤服务承担。</p>
 */
@Slf4j
@Service
public class TcScheduleResultServiceImpl extends AbstractDocService<TcScheduleResult>
        implements ITcScheduleResultService {

    private static final AtomicLong LAST_BATCH_TIME_MILLIS = new AtomicLong(0L);

    private static final Set<String> BLOCK_OVERWRITE_STATUS_SET =
            new HashSet<>(Arrays.asList("1", "3", "5"));

    private static final Set<String> CONFIRM_OVERWRITE_STATUS_SET =
            new HashSet<>(Arrays.asList("0", "2", "4"));

    @Resource
    private TcScheduleResultMapper tcScheduleResultMapper;

    @Resource
    private TcScheduleTemplateImpl tcScheduleTemplate;

    @Resource
    private TcAutoScheduleTaskService tcAutoScheduleTaskService;

    @Lazy
    @Resource
    private TcAutoScheduleAsyncExecutor tcAutoScheduleAsyncExecutor;

    @Resource
    private TcAutoScheduleRedisCacheService tcAutoScheduleRedisCacheService;

    @Resource
    private TcAutoScheduleExecutionGuard tcAutoScheduleExecutionGuard;

    /**
     * 获取单据类型编码。
     *
     * @return 胎侧排程结果单据类型编码
     */
    @Override
    protected String getDocTypeCode() {
        return "TC0815";
    }

    /**
     * 获取胎侧排程结果单据类型。
     *
     * @return 单据类型对象
     */
    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode(this.getDocTypeCode());
        return sysDocType;
    }

    /**
     * 校验排程结果唯一性。
     *
     * @param query 待校验结果
     * @return 唯一性编码
     * @throws ServiceException 数据不唯一时抛出
     */
    @Override
    public String checkUnique(TcScheduleResult query) {
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.resultNotUnique"));
        }
        return unique;
    }

    /**
     * 获取排程结果业务唯一键字段。
     *
     * @return 唯一键字段列表
     */
    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "batchNo", "scheduleDate", "sidewallCode", "machineCode"));
    }

    /**
     * 校验自动排程请求和旧结果覆盖条件。
     *
     * @param request 自动排程请求
     * @return 校验响应
     * @throws ServiceException 请求非法或存在不可覆盖结果时抛出
     */
    @Override
    public TcAutoScheduleResponseVo validateAutoPlan(TcAutoScheduleRequestVo request) {
        this.validateRequest(request);
        TcAutoScheduleResponseVo response = this.buildBaseResponse(request);
        this.validateOverwrite(request, response, this.listForOverwriteCheck(request), false);
        response.setSuccess(Boolean.TRUE);
        response.setMessage(Boolean.TRUE.equals(response.getConfirmRequired())
                ? I18nUtil.getMessage("ui.tc.schedule.confirmOverwriteTip")
                : I18nUtil.getMessage("ui.tc.schedule.validatePassed"));
        return response;
    }

    /**
     * 创建胎侧自动排程异步任务。
     *
     * @param request 自动排程请求
     * @return 待执行任务响应
     * @throws ServiceException 请求非法或未确认覆盖时抛出
     */
    @Override
    public TcAutoScheduleResponseVo autoPlan(TcAutoScheduleRequestVo request) {
        this.validateRequest(request);
        TcAutoScheduleResponseVo response = this.buildBaseResponse(request);
        TcAutoScheduleTask task;
        String lockToken = this.tcAutoScheduleExecutionGuard.acquire(
                request.getFactoryCode(), request.getScheduleDate());
        try {
            this.validateOverwrite(request, response, this.listForOverwriteCheck(request), true);
            TcAutoScheduleTask activeTask = this.tcAutoScheduleTaskService.findActive(
                    request.getFactoryCode(), request.getScheduleDate());
            if (activeTask != null) {
                throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.concurrentTask"));
            }
            task = this.tcAutoScheduleTaskService.createPending(request, response);
        } finally {
            this.tcAutoScheduleExecutionGuard.release(
                    request.getFactoryCode(), request.getScheduleDate(), lockToken);
        }
        TcAutoScheduleResponseVo submitResponse = this.tcAutoScheduleTaskService.toResponse(task);
        submitResponse.setSuccess(Boolean.TRUE);
        submitResponse.setConfirmRequired(Boolean.FALSE);
        submitResponse.setMessage(I18nUtil.getMessage("ui.tc.schedule.taskSubmitted"));
        this.tcAutoScheduleAsyncExecutor.execute(task.getTaskId());
        return submitResponse;
    }

    /**
     * 执行已提交的胎侧自动排程任务。
     *
     * @param taskId 对外任务编号
     * @return 最终排程响应
     * @throws ServiceException 任务不存在或排程失败时抛出
     */
    @Override
    public TcAutoScheduleResponseVo executeTcAutoPlanTask(String taskId) {
        TcAutoScheduleTask task = tcAutoScheduleTaskService.findByTaskId(taskId);
        if (task == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.taskNotFound"));
        }
        TcAutoScheduleRequestVo request = JSON.parseObject(task.getRequestSnapshot(), TcAutoScheduleRequestVo.class);
        this.validateRequest(request);
        String lockToken = tcAutoScheduleExecutionGuard.acquire(request.getFactoryCode(), request.getScheduleDate());
        TcScheduleContext context = null;
        long startMillis = System.currentTimeMillis();
        try {
            TcAutoScheduleResponseVo response = this.buildExecutionResponse(task);
            this.validateOverwrite(request, response, this.listForOverwriteCheck(request), true);
            context = this.buildScheduleContext(taskId, request, response);
            TcScheduleContext finalContext = context;
            context.setProgressListener((progress, stage, stageName) ->
                    tcAutoScheduleTaskService.updateProgress(taskId, progress, stage, stageName));
            tcScheduleTemplate.execute(context);
            TcPersistResult persistResult = Optional.ofNullable(context.getPersistResult()).orElseGet(TcPersistResult::new);
            response.setSuccess(Boolean.TRUE);
            response.setResultCount(persistResult.getResultCount());
            response.setUnplannedCount(persistResult.getUnplannedCount());
            response.setIssues(context.getIssueCollector().getIssues());
            response.setIssueCount(response.getIssues().size());
            response.setSummary(new LinkedHashMap<>(finalContext.getQualitySummary()));
            response.setMessage(I18nUtil.getMessage("ui.tc.schedule.executeFinished"));
            response.setTaskId(taskId);
            response.setTaskStatus(TcAutoScheduleTaskStatusEnum.SUCCESS.getCode());
            response.setProgress(100);
            response.setCurrentStage(TcScheduleConstants.AUTO_SCHEDULE_STAGE_COMPLETE);
            response.setCurrentStageName(I18nUtil.getMessage("ui.tc.schedule.taskCompleted"));
            log.info("{} step=FINISHED factoryCode={}, scheduleDate={}, batchNo={}, resultCount={}, unplannedCount={}, elapsedMs={}",
                    TcScheduleConstants.AUTO_PLAN_LOG_PREFIX, request.getFactoryCode(),
                    DateUtil.formatDate(request.getScheduleDate()), response.getBatchNo(),
                    response.getResultCount(), response.getUnplannedCount(), System.currentTimeMillis() - startMillis);
            return response;
        } catch (RuntimeException exception) {
            tcAutoScheduleTaskService.markFailed(taskId, exception.getMessage(),
                    context == null ? Collections.emptyList() : context.getIssueCollector().getIssues());
            throw exception;
        } finally {
            tcAutoScheduleExecutionGuard.release(request.getFactoryCode(), request.getScheduleDate(), lockToken);
        }
    }

    /**
     * 查询自动排程任务。
     *
     * @param taskId 对外任务编号
     * @return 任务响应
     * @throws ServiceException 任务不存在时抛出
     */
    @Override
    public TcAutoScheduleResponseVo getAutoPlanTask(String taskId) {
        TcAutoScheduleTask task = tcAutoScheduleTaskService.findByTaskId(taskId);
        if (task == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.taskNotFound"));
        }
        return tcAutoScheduleTaskService.toResponse(task);
    }

    /**
     * 查询指定工厂和排程日期最近一次任务。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 最近任务响应
     * @throws ServiceException 参数非法或任务不存在时抛出
     */
    @Override
    public TcAutoScheduleResponseVo getLatestAutoPlanTask(String factoryCode, Date scheduleDate) {
        TcAutoScheduleRequestVo request = new TcAutoScheduleRequestVo();
        request.setFactoryCode(factoryCode);
        request.setScheduleDate(scheduleDate);
        this.validateRequest(request);
        TcAutoScheduleTask task = tcAutoScheduleTaskService.findLatest(factoryCode, scheduleDate);
        if (task == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.taskNotFound"));
        }
        return tcAutoScheduleTaskService.toResponse(task);
    }

    /**
     * 清理胎侧自动排程基础资料缓存。
     *
     * @param factoryCode 工厂编码，为空时清理全部胎侧缓存
     * @param scheduleDate 排程日期
     * @return 删除的缓存键数量
     */
    @Override
    public long clearAutoPlanRedisCache(String factoryCode, Date scheduleDate) {
        return tcAutoScheduleRedisCacheService.clear(factoryCode, scheduleDate);
    }

    /**
     * 查询胎侧排程结果只读列表。
     *
     * @param query 查询条件
     * @return 排程结果列表
     */
    @Override
    public List<TcScheduleResult> listResult(TcScheduleResult query) {
        LambdaQueryWrapper<TcScheduleResult> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            wrapper.eq(StrUtil.isNotBlank(query.getFactoryCode()), TcScheduleResult::getFactoryCode, query.getFactoryCode());
            wrapper.eq(StrUtil.isNotBlank(query.getBatchNo()), TcScheduleResult::getBatchNo, query.getBatchNo());
            wrapper.eq(query.getScheduleDate() != null, TcScheduleResult::getScheduleDate, query.getScheduleDate());
            wrapper.eq(StrUtil.isNotBlank(query.getMachineCode()), TcScheduleResult::getMachineCode, query.getMachineCode());
            wrapper.like(StrUtil.isNotBlank(query.getOrderNo()), TcScheduleResult::getOrderNo, query.getOrderNo());
            wrapper.like(StrUtil.isNotBlank(query.getSidewallCode()), TcScheduleResult::getSidewallCode, query.getSidewallCode());
        }
        wrapper.orderByAsc(TcScheduleResult::getScheduleDate, TcScheduleResult::getMachineCode,
                TcScheduleResult::getClass1Sequence, TcScheduleResult::getClass2Sequence,
                TcScheduleResult::getClass3Sequence, TcScheduleResult::getClass4Sequence,
                TcScheduleResult::getClass5Sequence, TcScheduleResult::getClass6Sequence);
        return tcScheduleResultMapper.selectList(wrapper);
    }

    /**
     * 校验自动排程请求必填字段。
     *
     * @param request 自动排程请求
     * @throws ServiceException 请求、工厂或日期为空时抛出
     */
    private void validateRequest(TcAutoScheduleRequestVo request) {
        if (request == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.requestEmpty"));
        }
        if (StrUtil.isBlank(request.getFactoryCode())) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.factoryCodeEmpty"));
        }
        if (request.getScheduleDate() == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.scheduleDateEmpty"));
        }
    }

    /**
     * 构建自动排程提交基础响应。
     *
     * @param request 自动排程请求
     * @return 基础响应
     */
    private TcAutoScheduleResponseVo buildBaseResponse(TcAutoScheduleRequestVo request) {
        TcAutoScheduleResponseVo response = new TcAutoScheduleResponseVo();
        response.setBatchNo(this.generateBatchNo());
        response.setTraceId(StrUtil.blankToDefault(request.getTraceId(), IdUtil.fastSimpleUUID()));
        response.setResultCount(0);
        response.setUnplannedCount(0);
        response.setConfirmRequired(Boolean.FALSE);
        return response;
    }

    /**
     * 构建后台执行响应。
     *
     * @param task 自动排程任务
     * @return 执行响应
     */
    private TcAutoScheduleResponseVo buildExecutionResponse(TcAutoScheduleTask task) {
        TcAutoScheduleResponseVo response = new TcAutoScheduleResponseVo();
        response.setTaskId(task.getTaskId());
        response.setTaskStatus(TcAutoScheduleTaskStatusEnum.RUNNING.getCode());
        response.setBatchNo(task.getBatchNo());
        response.setTraceId(task.getTraceId());
        response.setResultCount(0);
        response.setUnplannedCount(0);
        response.setConfirmRequired(Boolean.FALSE);
        return response;
    }

    /**
     * 构建排程运行上下文。
     *
     * @param taskId 自动排程任务编号
     * @param request 自动排程请求
     * @param response 执行响应
     * @return 排程运行上下文
     */
    private TcScheduleContext buildScheduleContext(String taskId, TcAutoScheduleRequestVo request,
                                                   TcAutoScheduleResponseVo response) {
        TcScheduleContext context = new TcScheduleContext();
        context.setTaskId(taskId);
        context.setFactoryCode(request.getFactoryCode());
        context.setScheduleDate(request.getScheduleDate());
        context.setOperator(StrUtil.blankToDefault(request.getOperator(), "system"));
        context.setBatchNo(response.getBatchNo());
        context.setTraceId(response.getTraceId());
        return context;
    }

    /**
     * 生成胎侧自动排程批次号。
     *
     * @return TC 加时间戳格式的批次号
     */
    private String generateBatchNo() {
        long currentMillis = System.currentTimeMillis();
        long uniqueMillis = LAST_BATCH_TIME_MILLIS.updateAndGet(lastMillis ->
                currentMillis > lastMillis ? currentMillis : lastMillis + 1);
        return TcScheduleConstants.AUTO_PLAN_BATCH_NO_PREFIX
                + DateUtil.format(new Date(uniqueMillis), "yyyyMMddHHmmssSSS");
    }

    /**
     * 查询覆盖校验所需的旧结果状态。
     *
     * @param request 自动排程请求
     * @return 旧结果精简列表
     */
    private List<TcScheduleResult> listForOverwriteCheck(TcAutoScheduleRequestVo request) {
        return tcScheduleResultMapper.selectList(new LambdaQueryWrapper<TcScheduleResult>()
                .select(TcScheduleResult::getId, TcScheduleResult::getReleaseStatus)
                .eq(TcScheduleResult::getFactoryCode, request.getFactoryCode())
                .eq(TcScheduleResult::getScheduleDate, request.getScheduleDate()));
    }

    /**
     * 校验旧结果覆盖口径。
     *
     * @param request 自动排程请求
     * @param response 自动排程响应
     * @param currentResultList 当前有效旧结果
     * @param executeMode 是否为实际提交模式
     * @throws ServiceException 存在已发布结果或未确认覆盖时抛出
     */
    private void validateOverwrite(TcAutoScheduleRequestVo request, TcAutoScheduleResponseVo response,
                                   List<TcScheduleResult> currentResultList, boolean executeMode) {
        if (CollUtil.isEmpty(currentResultList)) {
            response.setConfirmRequired(Boolean.FALSE);
            return;
        }
        boolean hasBlockedStatus = currentResultList.stream()
                .map(TcScheduleResult::getReleaseStatus)
                .anyMatch(BLOCK_OVERWRITE_STATUS_SET::contains);
        boolean hasUnknownStatus = currentResultList.stream()
                .map(TcScheduleResult::getReleaseStatus)
                .anyMatch(status -> !CONFIRM_OVERWRITE_STATUS_SET.contains(status)
                        && !BLOCK_OVERWRITE_STATUS_SET.contains(status));
        if (hasBlockedStatus || hasUnknownStatus) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.publishedResultExists"));
        }
        response.setConfirmRequired(Boolean.TRUE);
        if (executeMode && !Boolean.TRUE.equals(request.getConfirmOverwrite())) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.confirmOverwriteRequired"));
        }
    }

}

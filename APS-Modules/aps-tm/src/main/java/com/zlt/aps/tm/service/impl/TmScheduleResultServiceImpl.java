package com.zlt.aps.tm.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.domain.dto.TmRollingRecalcRequestDTO;
import com.zlt.aps.tm.api.domain.entity.TmDispatcherLog;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.api.domain.entity.TmParams;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.vo.*;
import com.zlt.aps.tm.api.enums.*;
import com.zlt.aps.tm.component.TmScheduleBatchNoGenerator;
import com.zlt.aps.tm.domain.TmAutoScheduleTask;
import com.zlt.aps.tm.engine.domain.TmPersistResult;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.service.collector.TmAutoScheduleIssueCollector;
import com.zlt.aps.tm.engine.template.TmScheduleTemplateImpl;
import com.zlt.aps.tm.mapper.*;
import com.zlt.aps.tm.service.ITmRollingUpdateService;
import com.zlt.aps.tm.service.ITmScheduleResultService;
import com.zlt.aps.tm.service.TmAutoScheduleAsyncExecutor;
import com.zlt.aps.tm.service.TmAutoScheduleTaskService;
import com.zlt.aps.tm.service.cache.TmAutoScheduleRedisCacheService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎面排程结果表 业务层处理
 */
@Slf4j
@Service
public class TmScheduleResultServiceImpl extends AbstractDocService<TmScheduleResult> implements ITmScheduleResultService {

    @Resource
    private TmScheduleResultMapper tmScheduleResultMapper;

    @Resource
    private TmDispatcherLogMapper tmDispatcherLogMapper;

    @Resource
    private TmMachineInfoMapper tmMachineInfoMapper;

    @Resource
    private TmParamsMapper tmParamsMapper;

    @Resource
    private TmScheduleResultExplainMapper tmScheduleResultExplainMapper;

    @Resource
    private TmScheduleUnplannedMapper tmScheduleUnplannedMapper;

    @Resource
    private TmScheduleTemplateImpl tmScheduleTemplate;

    @Resource
    private TmAutoScheduleRedisCacheService tmAutoScheduleRedisCacheService;

    @Resource
    private TmAutoScheduleTaskService tmAutoScheduleTaskService;

    /** 失败批次过程日志独立保存服务。 */
    @Resource
    private TmFailedProcessLogService tmFailedProcessLogService;

    @Resource
    private TmAutoScheduleAsyncExecutor tmAutoScheduleAsyncExecutor;

    @Resource
    private com.zlt.aps.tm.service.TmReleaseApplicationService tmReleaseApplicationService;

    @Resource
    private TmManualOperationFacade tmManualOperationFacade;

    @Resource
    private TmManualScheduleApplicationService tmManualScheduleApplicationService;

    @Resource
    private ITmRollingUpdateService tmRollingUpdateService;

    @Resource
    private PlatformTransactionManager platformTransactionManager;

    @Resource
    private TmScheduleBatchNoGenerator tmScheduleBatchNoGenerator;

    @Override
    protected String getDocTypeCode() {
        return "TM0815";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TM0815");
        return sysDocType;
    }

    @Override
    public String checkUnique(TmScheduleResult query) {
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.scheduleResult.notUnique"));
        }
        return unique;
    }


    /**
     * 保存非排程字段，禁止通过通用保存入口新增或直接修改排程字段。
     *
     * @param scheduleResult 待保存排程结果
     * @return 更新行数
     * @throws ServiceException 新增或排程字段被直接修改时抛出
     */
    @Override
    public int save(TmScheduleResult scheduleResult) {
        if (scheduleResult == null || scheduleResult.getId() == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.directCreateForbidden"));
        }
        TmScheduleResult persisted = tmScheduleResultMapper.selectById(scheduleResult.getId());
        if (persisted == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.resultNotFound"));
        }
        this.validateDirectSchedulingFields(persisted, scheduleResult);
        scheduleResult.setReleaseStatus(persisted.getReleaseStatus());
        return baseDao.save(scheduleResult);
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "batchNo", "scheduleDate", "treadCode", "machineCode"));
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<TmScheduleResult> list, List<TmScheduleResult> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        // 提取所有非空、去重的机台编码
        List<String> machineCodeList = list.stream()
                .map(TmScheduleResult::getMachineCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        // 分批查询机台基础数据
        List<List<String>> splitList = ListUtil.split(machineCodeList, 500);
        List<TmMachineInfo> machineInfoList = new ArrayList<>();
        for (List<String> codes : splitList) {
            LambdaQueryWrapper<TmMachineInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(TmMachineInfo::getMachineCode, codes);
            machineInfoList.addAll(tmMachineInfoMapper.selectList(wrapper));
        }
        if (CollUtil.isNotEmpty(machineInfoList)) {
            serviceCheckParams.put("tmMachineCodeList",
                    machineInfoList.stream().map(TmMachineInfo::getMachineCode).collect(Collectors.toList()));
        }
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(TmScheduleResult importDocEntity, List<ImportErrorLog> importErrorLogs,
                                                Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        // 校验机台编码是否存在
        if (serviceCheckParams.containsKey("tmMachineCodeList")) {
            List<String> machineCodeList = (List<String>) serviceCheckParams.get("tmMachineCodeList");
            String machineCode = importDocEntity.getMachineCode();
            if (!machineCodeList.contains(machineCode)) {
                String message = I18nUtil.getMessage("ui.data.alert.tm.machineCodeNotExist");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(), errorRowNum, message, importErrorLogs);
                return Boolean.FALSE;
            }
        }
        TmScheduleResult persisted = this.findExistingImportResult(importDocEntity);
        if (persisted != null && this.isSchedulingFieldChanged(persisted, importDocEntity)) {
            String message = I18nUtil.getMessage("ui.data.alert.tm.schedule.directScheduleEditForbidden");
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    errorRowNum, message, importErrorLogs);
            return Boolean.FALSE;
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }

    /**
     * 修改胎面排程结果。
     *
     * @param scheduleResult 胎面排程结果
     * @return 更新行数
     * @throws ServiceException 记录不存在或排程字段被直接修改时抛出
     */
    @Override
    public int updateTmScheduleResult(TmScheduleResult scheduleResult) {
        if (scheduleResult == null || scheduleResult.getId() == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.resultNotFound"));
        }
        TmScheduleResult persisted = tmScheduleResultMapper.selectById(scheduleResult.getId());
        if (persisted == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.resultNotFound"));
        }
        this.validateDirectSchedulingFields(persisted, scheduleResult);
        scheduleResult.setBaseVale(scheduleResult.getId());
        scheduleResult.setReleaseStatus(persisted.getReleaseStatus());
        return tmScheduleResultMapper.updateById(scheduleResult);
    }

    /**
     * 校验通用保存不得修改机台、六班计划量和六班顺序。
     *
     * @param persisted     数据库当前结果
     * @param scheduleResult 待保存结果
     * @throws ServiceException 排程字段发生变化时抛出
     */
    private void validateDirectSchedulingFields(TmScheduleResult persisted, TmScheduleResult scheduleResult) {
        if (this.isSchedulingFieldChanged(persisted, scheduleResult)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.directScheduleEditForbidden"));
        }
    }

    /**
     * 判断机台、六班计划量或六班顺序是否变化。
     *
     * @param persisted     数据库当前结果
     * @param scheduleResult 待比较结果
     * @return true 表示排程字段发生变化
     */
    private boolean isSchedulingFieldChanged(TmScheduleResult persisted, TmScheduleResult scheduleResult) {
        if (!Objects.equals(persisted.getMachineCode(), scheduleResult.getMachineCode())) {
            return true;
        }
        for (int shiftOrder = 1; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
            String planQtyField = String.format(TmScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder);
            String sequenceField = String.format(TmScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder);
            if (!this.isSameFieldValue(persisted.getFieldValueByFieldName(planQtyField),
                    scheduleResult.getFieldValueByFieldName(planQtyField))
                    || !Objects.equals(persisted.getFieldValueByFieldName(sequenceField),
                    scheduleResult.getFieldValueByFieldName(sequenceField))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 比较字段业务值，BigDecimal 忽略小数位差异。
     *
     * @param firstValue  第一个值
     * @param secondValue 第二个值
     * @return true 表示业务值相同
     */
    private boolean isSameFieldValue(Object firstValue, Object secondValue) {
        if (firstValue instanceof java.math.BigDecimal && secondValue instanceof java.math.BigDecimal) {
            return ((java.math.BigDecimal) firstValue).compareTo((java.math.BigDecimal) secondValue) == 0;
        }
        return Objects.equals(firstValue, secondValue);
    }

    /**
     * 按导入结果当前粒度查询已有排程。
     *
     * @param importResult 导入排程结果
     * @return 已有结果；不存在时返回 null
     */
    private TmScheduleResult findExistingImportResult(TmScheduleResult importResult) {
        if (importResult.getId() != null) {
            return tmScheduleResultMapper.selectById(importResult.getId());
        }
        LambdaQueryWrapper<TmScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmScheduleResult::getFactoryCode, importResult.getFactoryCode());
        wrapper.eq(TmScheduleResult::getBatchNo, importResult.getBatchNo());
        wrapper.eq(TmScheduleResult::getScheduleDate, importResult.getScheduleDate());
        wrapper.eq(TmScheduleResult::getTreadCode, importResult.getTreadCode());
        wrapper.eq(TmScheduleResult::getMachineCode, importResult.getMachineCode());
        wrapper.last("LIMIT 1");
        return tmScheduleResultMapper.selectOne(wrapper);
    }

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录数
     * @param ids id数组
     * @return 符合条件的记录数
     */
    @Override
    public int isReleasingOrTimeoutByIds(Long[] ids) {
        return tmScheduleResultMapper.isReleasingOrTimeoutByIds(ids);
    }

    /**
     * 记录调度员操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule 操作后的排程数据
     */
    @Override
    public void insetDispatcherLog(String operType, TmScheduleResult newSchedule) {
        TmScheduleResult oldSchedule = tmScheduleResultMapper.selectById(newSchedule.getId());
        TmDispatcherLog log = new TmDispatcherLog();
        // 基础信息赋值
        log.setScheduleId(newSchedule.getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate());
        log.setTreadCode(newSchedule.getTreadCode());
        log.setFactoryCode(newSchedule.getFactoryCode());
        log.setBatchNo(newSchedule.getBatchNo());
        // 操作前的信息赋值
        if (oldSchedule != null) {
            log.setBeforeMachineCode(oldSchedule.getMachineCode());
            log.setBeforeClass1PlanQty(oldSchedule.getClass1PlanQty());
            log.setBeforeClass2PlanQty(oldSchedule.getClass2PlanQty());
            log.setBeforeClass3PlanQty(oldSchedule.getClass3PlanQty());
            log.setBeforeClass4PlanQty(oldSchedule.getClass4PlanQty());
            log.setBeforeClass5PlanQty(oldSchedule.getClass5PlanQty());
            log.setBeforeClass6PlanQty(oldSchedule.getClass6PlanQty());
        }
        // 操作后的信息赋值
        log.setAfterMachineCode(newSchedule.getMachineCode());
        log.setAfterClass1PlanQty(newSchedule.getClass1PlanQty());
        log.setAfterClass2PlanQty(newSchedule.getClass2PlanQty());
        log.setAfterClass3PlanQty(newSchedule.getClass3PlanQty());
        log.setAfterClass4PlanQty(newSchedule.getClass4PlanQty());
        log.setAfterClass5PlanQty(newSchedule.getClass5PlanQty());
        log.setAfterClass6PlanQty(newSchedule.getClass6PlanQty());
        // 调用插入日志方法
        tmDispatcherLogMapper.insert(log);
    }

    /**
     * 撤销指定的最新人工操作。
     *
     * @param dispatcherLogId 调度日志 ID
     * @return 恢复行数
     * @throws ServiceException 门面安全校验失败时抛出
     */
    @Override
    public int undoLastOperation(Long dispatcherLogId) {
        return tmManualOperationFacade.undoLastOperation(dispatcherLogId);
    }

    /**
     * 按工厂和排程日期逻辑删除当前有效批次数据
     * @param factoryCode 工厂编号
     * @param scheduleDate 排程日期
     */
    @Override
    public void logicDeleteByFactoryCodeAndScheduleDate(String factoryCode, Date scheduleDate) {
        if (StringUtils.isBlank(factoryCode) || scheduleDate == null) {
            return;
        }
        tmScheduleUnplannedMapper.logicDeleteByFactoryCodeAndScheduleDate(factoryCode, scheduleDate);
        tmScheduleResultExplainMapper.logicDeleteByFactoryCodeAndScheduleDate(factoryCode, scheduleDate);
        tmScheduleResultMapper.logicDeleteByFactoryCodeAndScheduleDate(factoryCode, scheduleDate);
    }

    /**
     * 校验胎面自动排程请求。
     *
     * @param request 自动排程请求
     * @return 自动排程校验响应
     * @throws ServiceException 工厂、日期缺失或存在已发布旧结果时抛出
     */
    @Override
    public TmAutoScheduleResponseVo validateTmAutoPlan(TmAutoScheduleRequestVo request) {
        validateAutoScheduleRequest(request);
        TmAutoScheduleResponseVo response = buildAutoScheduleResponse(request);
        List<TmScheduleResult> currentResultList = listForOverwriteCheck(request);
        fillOverwriteCheckResult(request, response, currentResultList, false);
        response.setSuccess(Boolean.TRUE);
        response.setMessage(Boolean.TRUE.equals(response.getConfirmRequired())
                ? resolveTmMessage("ui.data.alert.tm.schedule.confirmOverwriteTip", "当前排程日期已有未发布计划，确认后将重新生成")
                : resolveTmMessage("ui.data.alert.tm.schedule.validatePassed", "自动排程校验通过"));
        return response;
    }

    /**
     * 执行胎面自动排程。
     *
     * @param request 自动排程请求
     * @return 自动排程响应
     * @throws ServiceException 请求非法、旧批次不可覆盖或未确认覆盖时抛出
     */
    @Override
    public TmAutoScheduleResponseVo tmAutoPlan(TmAutoScheduleRequestVo request) {
        validateAutoScheduleRequest(request);
        TmAutoScheduleResponseVo response = buildAutoScheduleResponse(request);
        List<TmScheduleResult> currentResultList = listForOverwriteCheck(request);
        fillOverwriteCheckResult(request, response, currentResultList, true);
        TmAutoScheduleTask activeTask = tmAutoScheduleTaskService.findActive(request.getFactoryCode(), request.getScheduleDate());
        if (activeTask != null) {
            return tmAutoScheduleTaskService.toResponse(activeTask);
        }
        TmAutoScheduleTask task = tmAutoScheduleTaskService.createPending(request, response);
        TmAutoScheduleResponseVo submitResponse = tmAutoScheduleTaskService.toResponse(task);
        submitResponse.setSuccess(Boolean.TRUE);
        submitResponse.setConfirmRequired(Boolean.FALSE);
        submitResponse.setMessage(resolveTmMessage("ui.data.alert.tm.schedule.taskSubmitted", "胎面自动排程任务已提交"));
        tmAutoScheduleAsyncExecutor.execute(task.getTaskId());
        return submitResponse;
    }

    @Override
    public TmAutoScheduleResponseVo executeTmAutoPlanTask(String taskId) {
        TmAutoScheduleTask autoScheduleTask = tmAutoScheduleTaskService.findByTaskId(taskId);
        if (autoScheduleTask == null) {
            throw new ServiceException(resolveTmMessage("ui.data.alert.tm.schedule.taskNotFound", "未找到胎面自动排程任务"));
        }
        TmAutoScheduleRequestVo request = null;
        long startMillis = System.currentTimeMillis();
        TmAutoScheduleResponseVo response = null;
        TmScheduleContext context = null;
        try {
            request = JSON.parseObject(autoScheduleTask.getRequestSnapshot(), TmAutoScheduleRequestVo.class);
            log.info("{} step=REQUEST_RECEIVED factoryCode={}, scheduleDate={}, traceId={}, operator={}, dataSource={}, confirmOverwrite={}",
                    TmScheduleConstants.AUTO_PLAN_LOG_PREFIX, request == null ? null : request.getFactoryCode(),
                    formatAutoPlanDate(request == null ? null : request.getScheduleDate()),
                    request == null ? null : request.getTraceId(),
                    request == null ? null : request.getOperator(), request == null ? null : request.getDataSource(),
                    request == null ? null : request.getConfirmOverwrite());
            validateAutoScheduleRequest(request);
            log.info("{} step=REQUEST_VALIDATED factoryCode={}, scheduleDate={}, traceId={}, operator={}, dataSource={}, confirmOverwrite={}",
                    TmScheduleConstants.AUTO_PLAN_LOG_PREFIX, request.getFactoryCode(),
                    formatAutoPlanDate(request.getScheduleDate()), request.getTraceId(),
                    request.getOperator(), request.getDataSource(), request.getConfirmOverwrite());

            response = new TmAutoScheduleResponseVo();
            response.setBatchNo(autoScheduleTask.getBatchNo());
            response.setTraceId(autoScheduleTask.getTraceId());
            response.setResultCount(0);
            response.setUnplannedCount(0);
            response.setConfirmRequired(Boolean.FALSE);
            log.info("{} step=RESPONSE_INITIALIZED factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, operator={}",
                    TmScheduleConstants.AUTO_PLAN_LOG_PREFIX, request.getFactoryCode(), formatAutoPlanDate(request.getScheduleDate()),
                    response.getBatchNo(), response.getTraceId(), StrUtil.blankToDefault(request.getOperator(), "system"));

            List<TmScheduleResult> currentResultList = listForOverwriteCheck(request);
            log.info("{} step=OLD_RESULT_CHECKED factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, oldResultCount={}, releaseStatusSummary={}",
                    TmScheduleConstants.AUTO_PLAN_LOG_PREFIX, request.getFactoryCode(), formatAutoPlanDate(request.getScheduleDate()),
                    response.getBatchNo(), response.getTraceId(), currentResultList.size(), summarizeReleaseStatus(currentResultList));

            fillOverwriteCheckResult(request, response, currentResultList, true);
            log.info("{} step=OVERWRITE_DECIDED factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, oldResultCount={}, confirmRequired={}, confirmOverwrite={}",
                    TmScheduleConstants.AUTO_PLAN_LOG_PREFIX, request.getFactoryCode(), formatAutoPlanDate(request.getScheduleDate()),
                    response.getBatchNo(), response.getTraceId(), currentResultList.size(), response.getConfirmRequired(),
                    request.getConfirmOverwrite());

            log.info("{} step=OLD_RESULT_REPLACEMENT_DEFERRED factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, oldResultCount={}, reason=replaceInsideFinalTransaction",
                    TmScheduleConstants.AUTO_PLAN_LOG_PREFIX, request.getFactoryCode(), formatAutoPlanDate(request.getScheduleDate()),
                    response.getBatchNo(), response.getTraceId(), currentResultList.size());
            context = buildScheduleContext(request, response);
            context.setProgressListener((progress, stage, stageName) ->
                    tmAutoScheduleTaskService.updateProgress(taskId, progress, stage, stageName));
            log.info("{} step=CONTEXT_BUILT factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, operator={}, taskCount={}, machineCount={}, paramCount={}",
                    TmScheduleConstants.AUTO_PLAN_LOG_PREFIX, context.getFactoryCode(),
                    formatAutoPlanDate(context.getScheduleDate()),
                    context.getBatchNo(), context.getTraceId(), context.getOperator(), context.getTaskDraftList().size(),
                    context.getMachineCandidateList().size(), context.getParamMap().size());

            log.info("{} step=TEMPLATE_STARTED factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, taskCount={}, machineCount={}, stockForecastCount={}, chainCount={}, snapshotCount={}",
                    TmScheduleConstants.AUTO_PLAN_LOG_PREFIX, context.getFactoryCode(),
                    formatAutoPlanDate(context.getScheduleDate()),
                    context.getBatchNo(), context.getTraceId(), context.getTaskDraftList().size(),
                    context.getMachineCandidateList().size(), context.getStockForecastMap().size(),
                    countTaskChain(context), context.getSnapshotMap().size());
            tmScheduleTemplate.execute(context);
            TmPersistResult persistResult = Optional.ofNullable(context.getPersistResult()).orElseGet(TmPersistResult::new);
            log.info("{} step=TEMPLATE_FINISHED factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, taskCount={}, machineCount={}, stockForecastCount={}, chainCount={}, snapshotCount={}, resultCount={}, explainCount={}, unplannedCount={}, errorCount={}, lastErrorMsg={}",
                    TmScheduleConstants.AUTO_PLAN_LOG_PREFIX, context.getFactoryCode(),
                    formatAutoPlanDate(context.getScheduleDate()),
                    context.getBatchNo(), context.getTraceId(), context.getTaskDraftList().size(),
                    context.getMachineCandidateList().size(), context.getStockForecastMap().size(),
                    countTaskChain(context), context.getSnapshotMap().size(), persistResult.getResultCount(),
                    persistResult.getExplainCount(), persistResult.getUnplannedCount(), persistResult.getErrorCount(),
                    persistResult.getLastErrorMsg());

            response.setSuccess(Boolean.TRUE);
            response.setConfirmRequired(Boolean.FALSE);
            response.setBatchNo(context.getBatchNo());
            response.setTraceId(context.getTraceId());
            response.setResultCount(persistResult.getResultCount());
            response.setUnplannedCount(persistResult.getUnplannedCount());
            if (persistResult.getErrorCount() > 0) {
                context.getIssueCollector().addIssue(TmAutoScheduleIssueLevelEnum.ERROR,
                        TmScheduleStepEnum.PERSIST, TmAutoScheduleIssueCategoryEnum.PERSIST_PARTIAL_FAILED,
                        persistResult.getLastErrorMsg());
                response.setMessage(resolveTmMessage("ui.data.alert.tm.schedule.executePartialFailed", "胎面自动排程执行完成，部分记录落库失败，请联系管理员处理"));
                log.warn("{} step=PERSIST_PARTIAL_FAILED factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, errorCount={}, lastErrorMsg={}",
                        TmScheduleConstants.AUTO_PLAN_LOG_PREFIX, context.getFactoryCode(),
                        formatAutoPlanDate(context.getScheduleDate()),
                        context.getBatchNo(), context.getTraceId(), persistResult.getErrorCount(), persistResult.getLastErrorMsg());
            } else if (CollUtil.isEmpty(context.getTaskDraftList())) {
                String emptyTaskMessage = context.getEmptyFormingTaskMessage();
                if (StrUtil.isBlank(emptyTaskMessage)) {
                    emptyTaskMessage = resolveTmMessage("ui.data.alert.tm.schedule.noTaskGenerated",
                            "No schedulable forming demand was loaded");
                }
                response.setMessage(emptyTaskMessage);
                log.warn("{} step=NO_TASK_GENERATED factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, reason=noSchedulableTask",
                        TmScheduleConstants.AUTO_PLAN_LOG_PREFIX, context.getFactoryCode(),
                        formatAutoPlanDate(context.getScheduleDate()), context.getBatchNo(), context.getTraceId());
            } else if (persistResult.getResultCount() == 0 && persistResult.getUnplannedCount() == 0) {
                response.setMessage(resolveTmMessage("ui.data.alert.tm.schedule.noProductionNeeded",
                        "No production is needed because demand has been covered by inventory or scheduling rules"));
                log.info("{} step=NO_PRODUCTION_NEEDED factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, taskCount={}",
                        TmScheduleConstants.AUTO_PLAN_LOG_PREFIX, context.getFactoryCode(),
                        formatAutoPlanDate(context.getScheduleDate()), context.getBatchNo(), context.getTraceId(),
                        context.getTaskDraftList().size());
            } else if (persistResult.getResultCount() == 0) {
                response.setMessage(resolveTmMessage("ui.data.alert.tm.schedule.allUnplanned",
                        "No scheduled result was generated. Please review unplanned tasks and explanation details"));
                log.warn("{} step=ALL_UNPLANNED factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, unplannedCount={}",
                        TmScheduleConstants.AUTO_PLAN_LOG_PREFIX, context.getFactoryCode(),
                        formatAutoPlanDate(context.getScheduleDate()), context.getBatchNo(), context.getTraceId(),
                        persistResult.getUnplannedCount());
            } else {
                response.setMessage(resolveTmMessage("ui.data.alert.tm.schedule.executeFinished", "胎面自动排程执行完成"));
            }
            tmAutoScheduleTaskService.markSuccess(taskId, response, context.getIssueCollector().getIssues());
            log.info("{} step=FINISHED factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, success={}, resultCount={}, unplannedCount={}, message={}, elapsedMs={}",
                    TmScheduleConstants.AUTO_PLAN_LOG_PREFIX, request.getFactoryCode(),
                    formatAutoPlanDate(request.getScheduleDate()),
                    response.getBatchNo(), response.getTraceId(), response.getSuccess(), response.getResultCount(),
                    response.getUnplannedCount(), response.getMessage(), System.currentTimeMillis() - startMillis);
            return response;
        } catch (ServiceException ex) {
            log.warn("{} step=FAILED factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, elapsedMs={}, exceptionType={}, message={}",
                    TmScheduleConstants.AUTO_PLAN_LOG_PREFIX, request == null ? null : request.getFactoryCode(),
                    formatAutoPlanDate(request == null ? null : request.getScheduleDate()),
                    context == null ? response == null ? null : response.getBatchNo() : context.getBatchNo(),
                    context == null ? response == null ? request == null ? null : request.getTraceId() : response.getTraceId() : context.getTraceId(),
                    System.currentTimeMillis() - startMillis, ex.getClass().getSimpleName(), ex.getMessage());
            this.saveFailedProcessLogSafely(context, autoScheduleTask, request, ex);
            tmAutoScheduleTaskService.markFailed(taskId, ex.getMessage(),
                    this.collectFailureIssues(context, ex));
            throw ex;
        } catch (RuntimeException ex) {
            log.error("{} step=FAILED factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, elapsedMs={}, exceptionType={}, message={}",
                    TmScheduleConstants.AUTO_PLAN_LOG_PREFIX, request == null ? null : request.getFactoryCode(),
                    formatAutoPlanDate(request == null ? null : request.getScheduleDate()),
                    context == null ? response == null ? null : response.getBatchNo() : context.getBatchNo(),
                    context == null ? response == null ? request == null ? null : request.getTraceId() : response.getTraceId() : context.getTraceId(),
                    System.currentTimeMillis() - startMillis, ex.getClass().getSimpleName(), ex.getMessage(), ex);
            this.saveFailedProcessLogSafely(context, autoScheduleTask, request, ex);
            tmAutoScheduleTaskService.markFailed(taskId, ex.getMessage(),
                    this.collectFailureIssues(context, ex));
            throw ex;
        }
    }

    /**
     * 保存失败过程日志，保存异常只记录运行日志，不替换原排程异常。
     *
     * @param context          排程上下文，允许为空
     * @param autoScheduleTask 自动排程任务
     * @param request          自动排程请求，允许为空
     * @param originalException 原始排程异常
     */
    private void saveFailedProcessLogSafely(TmScheduleContext context, TmAutoScheduleTask autoScheduleTask,
                                            TmAutoScheduleRequestVo request,
                                            RuntimeException originalException) {
        try {
            tmFailedProcessLogService.saveFailure(context,
                    context == null ? autoScheduleTask.getBatchNo() : context.getBatchNo(),
                    request == null ? null : request.getFactoryCode(),
                    request == null ? null : request.getScheduleDate(), originalException);
        } catch (RuntimeException processLogException) {
            log.error("{} step=FAILED_PROCESS_LOG_SAVE_ERROR batchNo={}, originalExceptionType={}, saveExceptionType={}, saveMessage={}",
                    TmScheduleConstants.AUTO_PLAN_LOG_PREFIX,
                    context == null ? autoScheduleTask.getBatchNo() : context.getBatchNo(),
                    originalException.getClass().getSimpleName(), processLogException.getClass().getSimpleName(),
                    processLogException.getMessage(), processLogException);
        }
    }

    /**
     * 汇总自动排程失败问题，模板执行前失败时补充初始化阶段问题。
     *
     * @param context   排程上下文
     * @param exception 原始异常
     * @return 待写入任务表的问题明细
     */
    private List<TmAutoScheduleIssueVo> collectFailureIssues(TmScheduleContext context,
                                                             RuntimeException exception) {
        TmAutoScheduleIssueCollector issueCollector = context == null
                ? new TmAutoScheduleIssueCollector() : context.getIssueCollector();
        if (!issueCollector.hasErrorIssue()) {
            TmAutoScheduleIssueCategoryEnum category = exception instanceof ServiceException
                    ? TmAutoScheduleIssueCategoryEnum.AUTO_SCHEDULE_BUSINESS_ERROR
                    : TmAutoScheduleIssueCategoryEnum.AUTO_SCHEDULE_SYSTEM_ERROR;
            String message = StrUtil.blankToDefault(exception.getMessage(),
                    I18nUtil.getMessage("ui.data.alert.tm.schedule.taskExecuteFailed"));
            issueCollector.addFailureIssueIfAbsent(TmScheduleStepEnum.BOOTSTRAP, category, message);
        }
        return issueCollector.getIssues();
    }

    /**
     * 构建自动排程运行上下文。
     *
     * @param request  自动排程请求
     * @param response 基础响应，提供批次号和追踪号
     * @return 自动排程上下文
     */
    private TmScheduleContext buildScheduleContext(TmAutoScheduleRequestVo request, TmAutoScheduleResponseVo response) {
        TmScheduleContext context = new TmScheduleContext();
        context.setFactoryCode(request.getFactoryCode());
        context.setScheduleDate(request.getScheduleDate());
        context.setOperator(StrUtil.blankToDefault(request.getOperator(), "system"));
        context.setBatchNo(response.getBatchNo());
        context.setTraceId(response.getTraceId());
        return context;
    }

    /**
     * 格式化自动排程日志中的日期。
     *
     * @param scheduleDate 排程日期
     * @return yyyy-MM-dd 日期文本；入参为空时返回 null
     */
    private String formatAutoPlanDate(Date scheduleDate) {
        return scheduleDate == null ? null : DateUtil.formatDate(scheduleDate);
    }

    /**
     * 汇总旧排程结果的发布状态数量。
     *
     * @param resultList 旧排程结果列表
     * @return 发布状态数量摘要，用于覆盖判断日志
     */
    private String summarizeReleaseStatus(List<TmScheduleResult> resultList) {
        if (CollUtil.isEmpty(resultList)) {
            return "{}";
        }
        return resultList.stream()
                .collect(Collectors.groupingBy(item -> StrUtil.blankToDefault(item.getReleaseStatus(), "EMPTY"), Collectors.counting()))
                .toString();
    }

    /**
     * 统计当前排程上下文中已创建的机台班次任务链数量。
     *
     * @param context 自动排程上下文
     * @return 已创建任务链数量；上下文或任务链集合为空时返回 0
     */
    private int countTaskChain(TmScheduleContext context) {
        if (context == null || context.getTaskChainGroup() == null) {
            return 0;
        }
        return context.getTaskChainGroup().values().size();
    }

    /**
     * 清理胎面自动排程 Redis 基础资料缓存。
     *
     * @param factoryCode 工厂编码，为空时清理全部胎面自动排程缓存
     * @param scheduleDate 排程日期，和工厂同时传入时清理该日期相关缓存
     * @return 实际删除的 Redis key 数量
     */
    @Override
    public long clearAutoPlanRedisCache(String factoryCode, Date scheduleDate) {
        return tmAutoScheduleRedisCacheService.clear(factoryCode, scheduleDate);
    }

    /**
     * 查询胎面排程看板数据。
     *
     * @param query 查询条件
     * @return 看板排程结果
     */
    @Override
    public List<TmScheduleResult> listBoard(TmScheduleResult query) {
        LambdaQueryWrapper<TmScheduleResult> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            wrapper.eq(StrUtil.isNotBlank(query.getFactoryCode()), TmScheduleResult::getFactoryCode, query.getFactoryCode());
            wrapper.eq(StrUtil.isNotBlank(query.getBatchNo()), TmScheduleResult::getBatchNo, query.getBatchNo());
            wrapper.eq(query.getScheduleDate() != null, TmScheduleResult::getScheduleDate, query.getScheduleDate());
            wrapper.eq(StrUtil.isNotBlank(query.getMachineCode()), TmScheduleResult::getMachineCode, query.getMachineCode());
            wrapper.like(StrUtil.isNotBlank(query.getOrderNo()), TmScheduleResult::getOrderNo, query.getOrderNo());
            wrapper.like(StrUtil.isNotBlank(query.getTreadCode()), TmScheduleResult::getTreadCode, query.getTreadCode());
            wrapper.eq(StrUtil.isNotBlank(query.getReleaseStatus()), TmScheduleResult::getReleaseStatus, query.getReleaseStatus());
        }
        wrapper.orderByAsc(TmScheduleResult::getScheduleDate, TmScheduleResult::getMachineCode,
                TmScheduleResult::getClass1Sequence, TmScheduleResult::getClass2Sequence, TmScheduleResult::getClass3Sequence,
                TmScheduleResult::getClass4Sequence, TmScheduleResult::getClass5Sequence, TmScheduleResult::getClass6Sequence);
        return tmScheduleResultMapper.selectList(wrapper);
    }

    /**
     * 按列表同口径汇总胎面排程结果的库存合计与各班次计划量合计。
     *
     * <p>查询条件由 Controller 复用 {@code builderCondition} 构建，与列表查询口径一致；
     * 汇总基于全部匹配行（非仅当前页）。库存取 {@code sixClockStockQty}，
     * 班次计划量通过 {@code getFieldValueByFieldName} 配合班次字段名模板动态读取，避免逐班硬编码。</p>
     *
     * @param wrapper 列表同口径查询条件
     * @return 库存合计与各班次计划量合计
     */
    @Override
    public TmScheduleSummaryVo summarizeScheduleResult(QueryWrapper<TmScheduleResult> wrapper) {
        List<TmScheduleResult> resultList = tmScheduleResultMapper.selectList(wrapper);
        BigDecimal totalStockQty = BigDecimal.ZERO;
        // 各班次计划量合计，下标 0 对应 1 班，长度固定为最大班次序号
        List<BigDecimal> shiftPlanQtyList = new ArrayList<>(
                Collections.nCopies(TmScheduleConstants.TM_MAX_SHIFT_ORDER, BigDecimal.ZERO));
        for (TmScheduleResult result : resultList) {
            totalStockQty = totalStockQty.add(BigDecimalUtils.valueOf(result.getSixClockStockQty()));
            for (int shiftOrder = 1; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
                String planQtyField = String.format(TmScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder);
                Object fieldValue = result.getFieldValueByFieldName(planQtyField);
                BigDecimal shiftPlanQty = fieldValue instanceof BigDecimal ? (BigDecimal) fieldValue : BigDecimal.ZERO;
                shiftPlanQtyList.set(shiftOrder - 1, shiftPlanQtyList.get(shiftOrder - 1).add(shiftPlanQty));
            }
        }
        TmScheduleSummaryVo summaryVo = new TmScheduleSummaryVo();
        summaryVo.setTotalStockQty(totalStockQty);
        summaryVo.setShiftPlanQtyList(shiftPlanQtyList);
        return summaryVo;
    }

    /**
     * 插入人工插单排程结果。
     *
     * @param requestVo 插单请求
     * @return 写入行数
     * @throws ServiceException 门面安全校验失败时抛出
     */
    @Override
    public int insertTask(TmInsertTaskRequestVo requestVo) {
        return tmManualScheduleApplicationService.insertTask(requestVo);
    }

    /**
     * 批量删除未发布排程结果并滚动重排。
     *
     * @param ids 排程结果 ID
     * @return 删除行数
     * @throws ServiceException 门面状态、并发或审计校验失败时抛出
     */
    @Override
    public int removeScheduleResults(List<Long> ids) {
        return tmManualOperationFacade.deleteTasks(ids);
    }

    /**
     * 调整排程计划量。
     *
     * @param scheduleResult 调整后的排程结果
     * @return 更新行数
     * @throws ServiceException 门面安全校验失败时抛出
     */
    @Override
    public int changeQty(TmScheduleResult scheduleResult) {
        return tmManualOperationFacade.changeQty(scheduleResult);
    }
    /**
     * 调整排程机台。
     *
     * @param scheduleResult 转机台后的排程结果
     * @return 更新行数
     * @throws ServiceException 门面安全校验失败时抛出
     */
    @Override
    public int changeMachine(TmScheduleResult scheduleResult) {
        return tmManualOperationFacade.changeMachine(scheduleResult);
    }

    /**
     * 在 aps-tm 单个事务中批量调整排程机台。
     *
     * @param machineCode 目标机台编码
     * @param scheduleResultList 待转机的排程结果
     * @return 更新行数
     * @throws ServiceException 任一记录校验或转机失败时抛出并整批回滚
     */
    @Override
    public int batchChangeMachine(String machineCode, List<TmScheduleResult> scheduleResultList) {
        return tmManualOperationFacade.batchChangeMachine(machineCode, scheduleResultList);
    }

    /**
     * 委托自动滚动服务执行手动重算入口。
     *
     * @param request 滚动重算请求
     * @return 滚动重算统计
     */
    @Override
    public TmRollingRecalcResponseVO rollingRecalc(TmRollingRecalcRequestDTO request) {
        return tmRollingUpdateService.rollingRecalc(request);
    }
    /**
     * 校验排程结果是否允许发布。
     *
     * @param ids 排程结果 ID 列表
     * @return true 表示允许发布
     * @throws ServiceException 参数为空或存在发布中、超时失败记录时抛出
     */
    @Override
    public boolean publishValidate(List<Long> ids) {
        List<Long> normalizedIds = this.normalizePublishIds(ids);
        List<TmScheduleResult> resultList = tmScheduleResultMapper.selectBatchIds(normalizedIds);
        this.validatePublishSourceStatuses(normalizedIds, resultList);
        return true;
    }

    /**
     * 将排程结果标记为待发布。
     *
     * @param ids 排程结果 ID 列表
     * @return 更新行数
     * @throws ServiceException 参数为空或记录不可发布时抛出
     */
    @Override
    public int publish(List<Long> ids) {
        List<Long> normalizedIds = this.normalizePublishIds(ids);
        List<TmScheduleResult> resultList = tmScheduleResultMapper.selectList(
                new LambdaQueryWrapper<TmScheduleResult>().in(TmScheduleResult::getId, normalizedIds)
                        .orderByAsc(TmScheduleResult::getId));
        if (resultList.isEmpty()) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.resultNotFound"));
        }
        TmScheduleResult reference = resultList.get(0);
        com.zlt.aps.tm.api.domain.vo.TmReleaseRequestVo request = new com.zlt.aps.tm.api.domain.vo.TmReleaseRequestVo();
        request.setFactoryCode(reference.getFactoryCode());
        request.setScheduleDate(reference.getScheduleDate());
        request.setItems(normalizedIds.stream().map(resultId -> {
            com.zlt.aps.tm.api.domain.vo.TmReleaseItemVo item = new com.zlt.aps.tm.api.domain.vo.TmReleaseItemVo();
            item.setResultId(resultId);
            item.setExpectedTaskVersion(0L);
            return item;
        }).collect(Collectors.toList()));
        // 委托发布申请服务：建发布任务 + 置发布中 + 异步下发MES（对齐胎侧 TcReleaseApplicationService）
        this.tmReleaseApplicationService.publish(request);
        return normalizedIds.size();
    }

    /**
     * 更改排程结果发布状态。
     *
     * @param ids 排程结果 ID 列表，逗号分隔
     * @param releaseStatus 发布状态
     * @return 更新行数
     */
    @Override
    public int changeReleaseStatus(String ids, String releaseStatus) {
        if (StringUtils.isBlank(ids)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.publishIdsEmpty"));
        }
        if (!TmReleaseStatusTransition.isValidCode(releaseStatus)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.illegalReleaseStatus"));
        }
        Long[] idArray = com.ruoyi.common.text.Convert.toLongArray(ids);
        return this.updateReleaseStatusesAtomically(Arrays.asList(idArray), releaseStatus, false);
    }

    /**
     * 规范化页面发布 ID，去除空值、重复值并固定加锁顺序。
     *
     * @param ids 排程结果 ID
     * @return 规范化后的 ID 列表
     * @throws ServiceException 未提供有效 ID 时抛出
     */
    private List<Long> normalizePublishIds(List<Long> ids) {
        List<Long> normalizedIds = CollUtil.isEmpty(ids) ? Collections.emptyList()
                : ids.stream().filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());
        if (normalizedIds.isEmpty()) {
            throw new ServiceException(resolveTmMessage(
                    "ui.data.alert.tm.schedule.publishIdsEmpty", "发布排程结果不能为空"));
        }
        return normalizedIds;
    }

    /**
     * 校验页面发布入口允许的来源状态。
     *
     * <p>页面发布只允许未发布、发布失败和待发布三种状态。已发布记录只有在人工编辑后
     * 才能通过人工操作链路回退为待发布，禁止在发布按钮入口直接回退。</p>
     *
     * @param ids 请求 ID
     * @param resultList 数据库排程结果
     * @throws ServiceException 记录缺失或任一来源状态非法时抛出
     */
    private void validatePublishSourceStatuses(List<Long> ids, List<TmScheduleResult> resultList) {
        if (resultList == null || resultList.size() != ids.size()) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.resultNotFound"));
        }
        Set<String> publishSourceStatuses = new HashSet<>(Arrays.asList(
                ApsConstant.NO_RELEASE, ApsConstant.FAILURE_RELEASE, ApsConstant.WAIT_RELEASING));
        boolean containsIllegalStatus = resultList.stream()
                .anyMatch(result -> !publishSourceStatuses.contains(result.getReleaseStatus()));
        if (containsIllegalStatus) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.illegalReleaseTransition"));
        }
    }

    /**
     * 在短事务中加行锁、校验并批量修改发布状态。
     *
     * @param ids          排程结果 ID
     * @param targetStatus 目标发布状态
     * @param publishOperation 是否页面发布操作
     * @return 更新行数
     * @throws ServiceException 记录缺失、并发变化或任一状态迁移非法时抛出
     */
    private int updateReleaseStatusesAtomically(List<Long> ids, String targetStatus, boolean publishOperation) {
        List<Long> normalizedIds = ids.stream().filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());
        if (normalizedIds.isEmpty()) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.publishIdsEmpty"));
        }
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        Integer updatedRows = transactionTemplate.execute(transactionStatus -> {
            List<TmScheduleResult> resultList = tmScheduleResultMapper.selectBatchIdsForUpdate(normalizedIds);
            if (resultList == null || resultList.size() != normalizedIds.size()) {
                throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.resultNotFound"));
            }
            boolean invalidTransition;
            if (publishOperation) {
                Set<String> publishSourceStatuses = new HashSet<>(Arrays.asList(
                        ApsConstant.NO_RELEASE, ApsConstant.FAILURE_RELEASE, ApsConstant.WAIT_RELEASING));
                invalidTransition = resultList.stream()
                        .anyMatch(result -> !publishSourceStatuses.contains(result.getReleaseStatus()));
            } else {
                invalidTransition = resultList.stream()
                        .anyMatch(result -> !TmReleaseStatusTransition.canTransit(result.getReleaseStatus(), targetStatus));
            }
            if (invalidTransition) {
                throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.illegalReleaseTransition"));
            }
            LambdaUpdateWrapper<TmScheduleResult> wrapper = new LambdaUpdateWrapper<>();
            wrapper.in(TmScheduleResult::getId, normalizedIds);
            wrapper.set(TmScheduleResult::getReleaseStatus, targetStatus);
            int affectedRows = tmScheduleResultMapper.update(null, wrapper);
            if (affectedRows != normalizedIds.size()) {
                throw new ServiceException(I18nUtil.getMessage(
                        "ui.data.alert.tm.schedule.operationConcurrentChanged"));
            }
            return affectedRows;
        });
        if (updatedRows == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.operationFailed"));
        }
        return updatedRows;
    }

    /**
     * 校验自动排程请求必填字段。
     *
     * @param request 自动排程请求
     * @throws ServiceException 工厂或排程日期为空时抛出
     */
    private void validateAutoScheduleRequest(TmAutoScheduleRequestVo request) {
        if (request == null) {
            throw new ServiceException(resolveTmMessage("ui.data.alert.tm.schedule.autoPlanRequestEmpty", "自动排程请求不能为空"));
        }
        if (StrUtil.isBlank(request.getFactoryCode())) {
            throw new ServiceException(resolveTmMessage("ui.data.alert.tm.schedule.factoryCodeEmpty", "自动排程工厂不能为空"));
        }
        if (request.getScheduleDate() == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.dateEmpty"));
        }
    }

    /**
     * 根据自动排程请求构建基础响应。
     *
     * @param request 自动排程请求
     * @return 自动排程基础响应
     */
    private TmAutoScheduleResponseVo buildAutoScheduleResponse(TmAutoScheduleRequestVo request) {
        TmAutoScheduleResponseVo response = new TmAutoScheduleResponseVo();
        response.setBatchNo(tmScheduleBatchNoGenerator.generate());
        response.setTraceId(StrUtil.blankToDefault(request.getTraceId(), IdUtil.fastSimpleUUID()));
        response.setResultCount(0);
        response.setUnplannedCount(0);
        response.setConfirmRequired(Boolean.FALSE);
        return response;
    }

    /**
     * 按工厂和排程日期查询已有排程结果（仅用于覆盖检查）。
     *
     * <p>只查询 id 和发布状态字段，不施加排序，避免覆盖检查场景下不必要的全字段加载和多列排序开销。</p>
     *
     * @param request 自动排程请求
     * @return 已有排程结果列表
     */
    private List<TmScheduleResult> listForOverwriteCheck(TmAutoScheduleRequestVo request) {
        LambdaQueryWrapper<TmScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(TmScheduleResult::getId, TmScheduleResult::getReleaseStatus);
        wrapper.eq(TmScheduleResult::getFactoryCode, request.getFactoryCode());
        wrapper.eq(TmScheduleResult::getScheduleDate, request.getScheduleDate());
        return tmScheduleResultMapper.selectList(wrapper);
    }

    /**
     * 校验旧批次覆盖口径。
     *
     * @param request           自动排程请求
     * @param response          自动排程响应
     * @param currentResultList 当前排程日期已有结果
     * @param executeMode       是否为执行模式，true 时要求确认后才允许覆盖
     * @throws ServiceException 存在非未发布旧结果，或执行模式未确认覆盖时抛出
     */
    private void fillOverwriteCheckResult(TmAutoScheduleRequestVo request, TmAutoScheduleResponseVo response,
                                          List<TmScheduleResult> currentResultList, boolean executeMode) {
        if (CollUtil.isEmpty(currentResultList)) {
            response.setConfirmRequired(Boolean.FALSE);
            return;
        }
        boolean allNoRelease = currentResultList.stream()
                .allMatch(item -> ApsConstant.NO_RELEASE.equals(item.getReleaseStatus()));
        if (!allNoRelease) {
            throw new ServiceException(resolveTmMessage("ui.data.alert.tm.schedule.generatedPlanExists", "当前排程日期已存在已发布计划，不允许重复生成"));
        }
        response.setConfirmRequired(Boolean.TRUE);
        if (executeMode && !Boolean.TRUE.equals(request.getConfirmOverwrite())) {
            throw new ServiceException(resolveTmMessage("ui.data.alert.tm.schedule.confirmOverwriteRequired", "当前排程日期已有未发布计划，请确认后重新生成"));
        }
    }

    /**
     * 读取胎面排程国际化提示，未命中时回退默认文案。
     *
     * @param messageKey     国际化 key
     * @param defaultMessage 默认提示
     * @return 当前语言环境下的提示文案
     */
    private String resolveTmMessage(String messageKey, String defaultMessage) {
        String message = I18nUtil.getMessage(messageKey);
        return StringUtils.isBlank(message) || messageKey.equals(message) ? defaultMessage : message;
    }

    /**
     * 获取胎面排程班次日期列表
     * 根据工厂参数和排程日期构建6个班次的日期展示列表。
     * 胎面排程6个班次覆盖D日中班、D+1日夜早中、D+2日夜早，
     * D默认等于排程日期减1天，可通过TM_SHIFT_DATE_START_OFFSET按工厂维护：
     * 班次1：D日中班，班次2~4：D+1日夜早中，班次5~6：D+2日夜早
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 班次日期列表
     */
    @Override
    public List<TmScheduleShiftDateVO> listScheduleShiftDates(String factoryCode, Date scheduleDate) {
        if (scheduleDate == null) {
            scheduleDate = DateUtil.offsetDay(new Date(), 1);
        }
        int shiftDateStartOffset = this.resolveShiftDateStartOffset(factoryCode);
        Date dDay = DateUtil.offsetDay(scheduleDate, shiftDateStartOffset);
        Date dPlus1Day = DateUtil.offsetDay(dDay, 1);
        Date dPlus2Day = DateUtil.offsetDay(dDay, 2);
        String dDateStr = DateUtil.format(dDay, "MM/dd");
        String dPlus1DateStr = DateUtil.format(dPlus1Day, "MM/dd");
        String dPlus2DateStr = DateUtil.format(dPlus2Day, "MM/dd");

        List<TmScheduleShiftDateVO> result = new ArrayList<>(6);
        result.add(buildShiftDateVO(1, "afternoon", dDateStr));       // D日中班
        result.add(buildShiftDateVO(2, "night", dPlus1DateStr));      // D+1日夜班
        result.add(buildShiftDateVO(3, "morning", dPlus1DateStr));    // D+1日早班
        result.add(buildShiftDateVO(4, "afternoon", dPlus1DateStr));  // D+1日中班
        result.add(buildShiftDateVO(5, "night", dPlus2DateStr));      // D+2日夜班
        result.add(buildShiftDateVO(6, "morning", dPlus2DateStr));    // D+2日早班
        return result;
    }

    /**
     * 读取一班相对排程日期的偏移天数。
     * 参数未维护、未启用、为空或不是整数时使用兼容旧逻辑的默认值。
     *
     * @param factoryCode 工厂编码
     * @return 一班相对排程日期的偏移天数
     */
    private int resolveShiftDateStartOffset(String factoryCode) {
        if (StringUtils.isBlank(factoryCode)) {
            return TmScheduleConstants.DEFAULT_SHIFT_DATE_START_OFFSET;
        }
        LambdaQueryWrapper<TmParams> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmParams::getFactoryCode, factoryCode);
        wrapper.eq(TmParams::getParamCode, TmScheduleConstants.PARAM_SHIFT_DATE_START_OFFSET);
        wrapper.eq(TmParams::getEnableStatus, TmYesNoEnum.YES.getCode());
        TmParams params = this.tmParamsMapper.selectOne(wrapper);
        if (params == null) {
            return TmScheduleConstants.DEFAULT_SHIFT_DATE_START_OFFSET;
        }
        String effectiveValue = StringUtils.isNotBlank(params.getParamValue())
                ? params.getParamValue() : params.getDefaultValue();
        if (StringUtils.isBlank(effectiveValue)) {
            return TmScheduleConstants.DEFAULT_SHIFT_DATE_START_OFFSET;
        }
        try {
            return Integer.parseInt(effectiveValue.trim());
        } catch (NumberFormatException exception) {
            log.warn("胎面班次表头日期偏移参数格式错误，factoryCode={}, paramCode={}, paramValue={}",
                    factoryCode, TmScheduleConstants.PARAM_SHIFT_DATE_START_OFFSET, effectiveValue);
            return TmScheduleConstants.DEFAULT_SHIFT_DATE_START_OFFSET;
        }
    }

    /**
     * 构建班次日期VO
     *
     * @param shift 班次序号
     * @param shiftType 班次类型
     * @param shiftDate 班次日期
     * @return 班次日期VO
     */
    private TmScheduleShiftDateVO buildShiftDateVO(int shift, String shiftType, String shiftDate) {
        TmScheduleShiftDateVO vo = new TmScheduleShiftDateVO();
        vo.setShift(shift);
        vo.setShiftType(shiftType);
        vo.setShiftDate(shiftDate);
        return vo;
    }
}

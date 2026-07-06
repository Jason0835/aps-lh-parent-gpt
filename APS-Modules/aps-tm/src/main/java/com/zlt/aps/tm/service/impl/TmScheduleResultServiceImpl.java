package com.zlt.aps.tm.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.engine.schedule.ScheduleOperationContext;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.tm.api.domain.entity.TmDispatcherLog;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleRequestVo;
import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleResponseVo;
import com.zlt.aps.tm.api.domain.vo.TmScheduleShiftDateVO;
import com.zlt.aps.tm.api.enums.TmScheduleErrorCodeEnum;
import com.zlt.aps.tm.engine.domain.*;
import com.zlt.aps.tm.engine.service.TmScheduleOperationFacade;
import com.zlt.aps.tm.engine.template.TmScheduleTemplateImpl;
import com.zlt.aps.tm.engine.validator.TmInsertPositionValidator;
import com.zlt.aps.tm.mapper.*;
import com.zlt.aps.tm.service.ITmScheduleResultService;
import com.zlt.aps.tm.service.TmAutoScheduleRedisCacheService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 胎面排程结果表 业务层处理
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TmScheduleResultServiceImpl extends AbstractDocService<TmScheduleResult> implements ITmScheduleResultService {

    private static final String TM_AUTO_PLAN_LOG_PREFIX = "[TM_AUTO_PLAN]";

    /** 胎面自动排程批次号前缀 */
    private static final String TM_AUTO_BATCH_NO_PREFIX = "TM";

    /** 进程内最后一次批次号时间戳，用于避免同一毫秒内连续生成重复批次号 */
    private static final AtomicLong LAST_BATCH_TIME_MILLIS = new AtomicLong(0L);

    @Resource
    private TmScheduleResultMapper tmScheduleResultMapper;

    @Resource
    private TmDispatcherLogMapper tmDispatcherLogMapper;

    @Resource
    private TmMachineInfoMapper tmMachineInfoMapper;

    @Resource
    private TmScheduleResultExplainMapper tmScheduleResultExplainMapper;

    @Resource
    private TmScheduleUnplannedMapper tmScheduleUnplannedMapper;

    @Resource
    private TmScheduleTemplateImpl tmScheduleTemplate;

    @Resource
    private TmScheduleOperationFacade tmScheduleOperationFacade;

    @Resource
    private TmAutoScheduleRedisCacheService tmAutoScheduleRedisCacheService;

    @Resource
    private TmManualInsertRollingService tmManualInsertRollingService;

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
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }

    /**
     * 修改胎面排程结果
     * @param scheduleResult 胎面排程结果
     * @return 结果
     */
    @Override
    public int updateTmScheduleResult(TmScheduleResult scheduleResult) {
        scheduleResult.setBaseVale(scheduleResult.getId());
        // 校验字段是否修改，修改则改状态为未发布
        if (!ApsConstant.RELEASING.equals(scheduleResult.getReleaseStatus())
                && !ApsConstant.TIMEOUT_FAILURE.equals(scheduleResult.getReleaseStatus())) {
            TmScheduleResult old = tmScheduleResultMapper.selectById(scheduleResult.getId());
            if (old != null) {
                boolean flag = compare(old.getMachineCode(), scheduleResult.getMachineCode());
                flag = flag && compare(old.getClass1PlanQty(), scheduleResult.getClass1PlanQty());
                flag = flag && compare(old.getClass2PlanQty(), scheduleResult.getClass2PlanQty());
                flag = flag && compare(old.getClass3PlanQty(), scheduleResult.getClass3PlanQty());
                flag = flag && compare(old.getClass4PlanQty(), scheduleResult.getClass4PlanQty());
                flag = flag && compare(old.getClass5PlanQty(), scheduleResult.getClass5PlanQty());
                flag = flag && compare(old.getClass6PlanQty(), scheduleResult.getClass6PlanQty());
                if (!flag) {
                    scheduleResult.setReleaseStatus(scheduleResult.getReleaseStatus() == null || "".equals(scheduleResult.getReleaseStatus())
                            ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
                }
            }
        }
        return tmScheduleResultMapper.updateById(scheduleResult);
    }

    /**
     * 比较两个值是否相等
     * @param oldVal 旧值
     * @param newVal 新值
     * @return true表示相等
     */
    private boolean compare(Object oldVal, Object newVal) {
        if (oldVal == null && newVal == null) {
            return true;
        }
        if (oldVal != null) {
            return oldVal.equals(newVal);
        }
        return false;
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
        long startMillis = System.currentTimeMillis();
        TmAutoScheduleResponseVo response = null;
        TmScheduleContext context = null;
        log.info("{} step=REQUEST_RECEIVED factoryCode={}, scheduleDate={}, traceId={}, operator={}, dataSource={}, confirmOverwrite={}",
                TM_AUTO_PLAN_LOG_PREFIX, request == null ? null : request.getFactoryCode(),
                formatAutoPlanDate(request == null ? null : request.getScheduleDate()), request == null ? null : request.getTraceId(),
                request == null ? null : request.getOperator(), request == null ? null : request.getDataSource(),
                request == null ? null : request.getConfirmOverwrite());
        try {
            validateAutoScheduleRequest(request);
            log.info("{} step=REQUEST_VALIDATED factoryCode={}, scheduleDate={}, traceId={}, operator={}, dataSource={}, confirmOverwrite={}",
                    TM_AUTO_PLAN_LOG_PREFIX, request.getFactoryCode(), formatAutoPlanDate(request.getScheduleDate()), request.getTraceId(),
                    request.getOperator(), request.getDataSource(), request.getConfirmOverwrite());

            response = buildAutoScheduleResponse(request);
            log.info("{} step=RESPONSE_INITIALIZED factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, operator={}",
                    TM_AUTO_PLAN_LOG_PREFIX, request.getFactoryCode(), formatAutoPlanDate(request.getScheduleDate()),
                    response.getBatchNo(), response.getTraceId(), StrUtil.blankToDefault(request.getOperator(), "system"));

            List<TmScheduleResult> currentResultList = listForOverwriteCheck(request);
            log.info("{} step=OLD_RESULT_CHECKED factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, oldResultCount={}, releaseStatusSummary={}",
                    TM_AUTO_PLAN_LOG_PREFIX, request.getFactoryCode(), formatAutoPlanDate(request.getScheduleDate()),
                    response.getBatchNo(), response.getTraceId(), currentResultList.size(), summarizeReleaseStatus(currentResultList));

            fillOverwriteCheckResult(request, response, currentResultList, true);
            log.info("{} step=OVERWRITE_DECIDED factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, oldResultCount={}, confirmRequired={}, confirmOverwrite={}",
                    TM_AUTO_PLAN_LOG_PREFIX, request.getFactoryCode(), formatAutoPlanDate(request.getScheduleDate()),
                    response.getBatchNo(), response.getTraceId(), currentResultList.size(), response.getConfirmRequired(),
                    request.getConfirmOverwrite());

            if (CollUtil.isNotEmpty(currentResultList)) {
                log.info("{} step=OLD_RESULT_DELETE_STARTED factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, oldResultCount={}",
                        TM_AUTO_PLAN_LOG_PREFIX, request.getFactoryCode(), formatAutoPlanDate(request.getScheduleDate()),
                        response.getBatchNo(), response.getTraceId(), currentResultList.size());
                logicDeleteByFactoryCodeAndScheduleDate(request.getFactoryCode(), request.getScheduleDate());
                log.info("{} step=OLD_RESULT_DELETE_FINISHED factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, oldResultCount={}",
                        TM_AUTO_PLAN_LOG_PREFIX, request.getFactoryCode(), formatAutoPlanDate(request.getScheduleDate()),
                        response.getBatchNo(), response.getTraceId(), currentResultList.size());
            } else {
                log.info("{} step=OLD_RESULT_DELETE_SKIPPED factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, reason=noOldResult",
                        TM_AUTO_PLAN_LOG_PREFIX, request.getFactoryCode(), formatAutoPlanDate(request.getScheduleDate()),
                        response.getBatchNo(), response.getTraceId());
            }

            context = buildScheduleContext(request, response);
            log.info("{} step=CONTEXT_BUILT factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, operator={}, taskCount={}, machineCount={}, paramCount={}",
                    TM_AUTO_PLAN_LOG_PREFIX, context.getFactoryCode(), formatAutoPlanDate(context.getScheduleDate()),
                    context.getBatchNo(), context.getTraceId(), context.getOperator(), context.getTaskDraftList().size(),
                    context.getMachineCandidateList().size(), context.getParamMap().size());

            log.info("{} step=TEMPLATE_STARTED factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, taskCount={}, machineCount={}, stockForecastCount={}, chainCount={}, snapshotCount={}",
                    TM_AUTO_PLAN_LOG_PREFIX, context.getFactoryCode(), formatAutoPlanDate(context.getScheduleDate()),
                    context.getBatchNo(), context.getTraceId(), context.getTaskDraftList().size(),
                    context.getMachineCandidateList().size(), context.getStockForecastMap().size(),
                    countTaskChain(context), context.getSnapshotMap().size());
            tmScheduleTemplate.execute(context);
            TmPersistResult persistResult = Optional.ofNullable(context.getPersistResult()).orElseGet(TmPersistResult::new);
            log.info("{} step=TEMPLATE_FINISHED factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, taskCount={}, machineCount={}, stockForecastCount={}, chainCount={}, snapshotCount={}, resultCount={}, explainCount={}, unplannedCount={}, errorCount={}, lastErrorMsg={}",
                    TM_AUTO_PLAN_LOG_PREFIX, context.getFactoryCode(), formatAutoPlanDate(context.getScheduleDate()),
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
                response.setMessage(resolveTmMessage("ui.data.alert.tm.schedule.executePartialFailed", "胎面自动排程执行完成，部分记录落库失败，请联系管理员处理"));
                log.warn("{} step=PERSIST_PARTIAL_FAILED factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, errorCount={}, lastErrorMsg={}",
                        TM_AUTO_PLAN_LOG_PREFIX, context.getFactoryCode(), formatAutoPlanDate(context.getScheduleDate()),
                        context.getBatchNo(), context.getTraceId(), persistResult.getErrorCount(), persistResult.getLastErrorMsg());
            } else {
                response.setMessage(resolveTmMessage("ui.data.alert.tm.schedule.executeFinished", "胎面自动排程执行完成"));
            }
            log.info("{} step=FINISHED factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, success={}, resultCount={}, unplannedCount={}, message={}, elapsedMs={}",
                    TM_AUTO_PLAN_LOG_PREFIX, request.getFactoryCode(), formatAutoPlanDate(request.getScheduleDate()),
                    response.getBatchNo(), response.getTraceId(), response.getSuccess(), response.getResultCount(),
                    response.getUnplannedCount(), response.getMessage(), System.currentTimeMillis() - startMillis);
            return response;
        } catch (ServiceException ex) {
            log.warn("{} step=FAILED factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, elapsedMs={}, exceptionType={}, message={}",
                    TM_AUTO_PLAN_LOG_PREFIX, request == null ? null : request.getFactoryCode(),
                    formatAutoPlanDate(request == null ? null : request.getScheduleDate()),
                    context == null ? response == null ? null : response.getBatchNo() : context.getBatchNo(),
                    context == null ? response == null ? request == null ? null : request.getTraceId() : response.getTraceId() : context.getTraceId(),
                    System.currentTimeMillis() - startMillis, ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            log.error("{} step=FAILED factoryCode={}, scheduleDate={}, batchNo={}, traceId={}, elapsedMs={}, exceptionType={}, message={}",
                    TM_AUTO_PLAN_LOG_PREFIX, request == null ? null : request.getFactoryCode(),
                    formatAutoPlanDate(request == null ? null : request.getScheduleDate()),
                    context == null ? response == null ? null : response.getBatchNo() : context.getBatchNo(),
                    context == null ? response == null ? request == null ? null : request.getTraceId() : response.getTraceId() : context.getTraceId(),
                    System.currentTimeMillis() - startMillis, ex.getClass().getSimpleName(), ex.getMessage(), ex);
            throw ex;
        }
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
     * 插入人工插单排程结果。
     *
     * @param scheduleResult 插单排程结果
     * @return 写入行数
     * @throws ServiceException 必填字段缺失时抛出
     */
    @Override
    public int insertTask(TmScheduleResult scheduleResult) {
        if (scheduleResult == null) {
            throw new ServiceException(resolveTmMessage("ui.data.alert.tm.schedule.insertTaskEmpty", "插单排程结果不能为空"));
        }
        if (StrUtil.isBlank(scheduleResult.getFactoryCode())) {
            scheduleResult.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        if (scheduleResult.getScheduleDate() == null) {
            throw new ServiceException(resolveTmMessage("ui.data.alert.tm.schedule.insertScheduleDateEmpty", "插单排程日期不能为空"));
        }
        if (StrUtil.isBlank(scheduleResult.getTreadCode())) {
            throw new ServiceException(resolveTmMessage("ui.data.alert.tm.schedule.insertTreadCodeEmpty", "插单胎面不能为空"));
        }
        validateInsertAfterSecondSequence(scheduleResult);
        int insertCount = tmManualInsertRollingService.insertAndRoll(scheduleResult);
        scheduleResult.setBaseVale(scheduleResult.getId());
        insetDispatcherLog(ApsConstant.DISPATCHER_OPER_INSERT_ORDER, scheduleResult);
        return insertCount;
    }

    /**
     * 调整排程计划量。
     *
     * @param scheduleResult 调整后的排程结果
     * @return 更新行数
     * @throws ServiceException 记录不存在或不可调整时抛出
     */
    @Override
    public int changeQty(TmScheduleResult scheduleResult) {
        if (scheduleResult == null || scheduleResult.getId() == null) {
            throw new ServiceException(resolveTmMessage("ui.data.alert.tm.schedule.changeQtyIdEmpty", "调量排程结果不能为空"));
        }
        if (isReleasingOrTimeoutByIds(new Long[]{scheduleResult.getId()}) > 0) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        invokeChangeQtyFacade(scheduleResult);
        int updateCount = tmManualInsertRollingService.changeQtyAndRoll(scheduleResult);
        scheduleResult.setBaseVale(scheduleResult.getId());
        insetDispatcherLog(ApsConstant.DISPATCHER_OPER_PLAN, scheduleResult);
        return updateCount;
    }

    /**
     * 调整排程机台。
     *
     * @param scheduleResult 转机台后的排程结果
     * @return 更新行数
     * @throws ServiceException 记录不存在或不可调整时抛出
     */
    @Override
    public int changeMachine(TmScheduleResult scheduleResult) {
        if (scheduleResult == null || scheduleResult.getId() == null) {
            throw new ServiceException(resolveTmMessage("ui.data.alert.tm.schedule.changeMachineIdEmpty", "转机台排程结果不能为空"));
        }
        if (isReleasingOrTimeoutByIds(new Long[]{scheduleResult.getId()}) > 0) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        invokeTransferMachineFacade(scheduleResult);
        int updateCount = tmManualInsertRollingService.changeMachineAndRoll(scheduleResult);
        scheduleResult.setBaseVale(scheduleResult.getId());
        insetDispatcherLog(ApsConstant.DISPATCHER_OPER_MACHINE, scheduleResult);
        return updateCount;
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
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException(resolveTmMessage("ui.data.alert.tm.schedule.publishIdsEmpty", "发布排程结果不能为空"));
        }
        if (isReleasingOrTimeoutByIds(ids.toArray(new Long[0])) > 0) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
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
        publishValidate(ids);
        LambdaUpdateWrapper<TmScheduleResult> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(TmScheduleResult::getId, ids);
        wrapper.set(TmScheduleResult::getReleaseStatus, ApsConstant.WAIT_RELEASING);
        return tmScheduleResultMapper.update(null, wrapper);
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
            throw new ServiceException(resolveTmMessage("ui.data.alert.tm.schedule.publishIdsEmpty", "请选择要更改发布状态的排程记录"));
        }
        Long[] idArray = com.ruoyi.common.text.Convert.toLongArray(ids);
        LambdaUpdateWrapper<TmScheduleResult> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(TmScheduleResult::getId, Arrays.asList(idArray));
        wrapper.set(TmScheduleResult::getReleaseStatus, releaseStatus);
        return tmScheduleResultMapper.update(null, wrapper);
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
        response.setBatchNo(generateAutoBatchNo());
        response.setTraceId(StrUtil.blankToDefault(request.getTraceId(), IdUtil.fastSimpleUUID()));
        response.setResultCount(0);
        response.setUnplannedCount(0);
        response.setConfirmRequired(Boolean.FALSE);
        return response;
    }

    /**
     * 生成胎面自动排程批次号。
     *
     * <p>批次号按执行时刻生成，同一天多次自动排程可生成不同批次；若同一进程内连续调用落在同一毫秒，
     * 使用递增毫秒兜底，保证本进程内不重复。</p>
     *
     * @return 批次号，格式 TMyyyyMMddHHmmssSSS
     */
    private String generateAutoBatchNo() {
        long currentMillis = System.currentTimeMillis();
        long uniqueMillis = LAST_BATCH_TIME_MILLIS.updateAndGet(lastMillis ->
                currentMillis > lastMillis ? currentMillis : lastMillis + 1);
        return TM_AUTO_BATCH_NO_PREFIX + DateUtil.format(new Date(uniqueMillis), "yyyyMMddHHmmssSSS");
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
     * 校验人工插单只能插到第二个在产规格之后。
     *
     * <p>按同工厂、同日期、同机台、同班次内完成量大于 0 的记录识别在产规格。
     * 若已存在两个及以上在产规格，插单顺序必须大于第二个在产规格顺序。
     * 未传顺序、班次或机台时保持旧接口兼容。</p>
     *
     * @param scheduleResult 插单排程结果
     * @throws ServiceException 插单位置不在第二顺序之后时抛出
     */
    private void validateInsertAfterSecondSequence(TmScheduleResult scheduleResult) {
        Integer shiftOrder = TmInsertPositionValidator.resolveShiftOrder(scheduleResult);
        Integer insertSequence = TmInsertPositionValidator.resolveSequence(scheduleResult, shiftOrder);
        if (shiftOrder == null || insertSequence == null || StrUtil.isBlank(scheduleResult.getMachineCode())) {
            return;
        }
        LambdaQueryWrapper<TmScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmScheduleResult::getFactoryCode, scheduleResult.getFactoryCode());
        wrapper.eq(TmScheduleResult::getScheduleDate, scheduleResult.getScheduleDate());
        wrapper.eq(TmScheduleResult::getMachineCode, scheduleResult.getMachineCode());
        List<TmScheduleResult> resultList = tmScheduleResultMapper.selectList(wrapper);
        List<Integer> inProductionSequenceList = resultList.stream()
                .filter(item -> TmInsertPositionValidator.getFinishQty(item, shiftOrder).compareTo(BigDecimal.ZERO) > 0)
                .map(item -> TmInsertPositionValidator.resolveSequence(item, shiftOrder))
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());
        if (inProductionSequenceList.size() >= 2 && insertSequence <= inProductionSequenceList.get(1)) {
            throw new ServiceException(scheduleErrorMessage(TmScheduleErrorCodeEnum.TM_INSERT_POSITION_INVALID));
        }
    }

    /**
     * 调用排程操作门面处理人工插单任务链。
     *
     * @param scheduleResult 插单排程结果
     */
    private void invokeInsertFacade(TmScheduleResult scheduleResult) {
        Integer shiftOrder = Optional.ofNullable(TmInsertPositionValidator.resolveShiftOrder(scheduleResult)).orElse(1);
        TmTaskDraft taskDraft = buildTaskDraft(scheduleResult, shiftOrder);
        TmInsertPosition position = new TmInsertPosition();
        position.setMachineCode(scheduleResult.getMachineCode());
        position.setShiftOrder(shiftOrder);
        tmScheduleOperationFacade.insertTask(taskDraft, position, buildOperationContext(scheduleResult));
    }

    /**
     * 调用排程操作门面处理调量任务链。
     *
     * @param scheduleResult 调量后的排程结果
     */
    private void invokeChangeQtyFacade(TmScheduleResult scheduleResult) {
        TmScheduleResult oldSchedule = tmScheduleResultMapper.selectById(scheduleResult.getId());
        if (oldSchedule == null) {
            throw new ServiceException(resolveTmMessage("ui.data.alert.tm.schedule.changeQtyResultNotFound", "调量排程结果不存在或已失效"));
        }
        Integer shiftOrder = Optional.ofNullable(TmInsertPositionValidator.resolveShiftOrder(scheduleResult))
                .orElseGet(() -> Optional.ofNullable(TmInsertPositionValidator.resolveShiftOrder(oldSchedule)).orElse(1));
        TmScheduleContext context = buildOperationContext(oldSchedule);
        TmTaskDraft taskDraft = buildTaskDraft(oldSchedule, shiftOrder);
        seedOperationTask(context, taskDraft, oldSchedule.getMachineCode(), shiftOrder);
        tmScheduleOperationFacade.changeQty(taskDraft.getBusinessKey(), resolvePlanQty(scheduleResult, shiftOrder), shiftOrder, context);
    }

    /**
     * 调用排程操作门面处理转机台任务链。
     *
     * @param scheduleResult 转机台后的排程结果
     */
    private void invokeTransferMachineFacade(TmScheduleResult scheduleResult) {
        TmScheduleResult oldSchedule = tmScheduleResultMapper.selectById(scheduleResult.getId());
        if (oldSchedule == null) {
            throw new ServiceException(resolveTmMessage("ui.data.alert.tm.schedule.changeMachineResultNotFound", "转机台排程结果不存在或已失效"));
        }
        Integer shiftOrder = Optional.ofNullable(TmInsertPositionValidator.resolveShiftOrder(scheduleResult))
                .orElseGet(() -> Optional.ofNullable(TmInsertPositionValidator.resolveShiftOrder(oldSchedule)).orElse(1));
        TmScheduleContext context = buildOperationContext(oldSchedule);
        TmTaskDraft taskDraft = buildTaskDraft(oldSchedule, shiftOrder);
        seedOperationTask(context, taskDraft, oldSchedule.getMachineCode(), shiftOrder);
        TmTransferPosition position = new TmTransferPosition();
        position.setShiftOrder(shiftOrder);
        tmScheduleOperationFacade.transferMachine(taskDraft.getBusinessKey(), scheduleResult.getMachineCode(), position, context);
    }

    /**
     * 构造人工操作运行上下文。
     *
     * @param scheduleResult 排程结果
     * @return 排程运行上下文
     */
    private TmScheduleContext buildOperationContext(TmScheduleResult scheduleResult) {
        TmScheduleContext context = new TmScheduleContext();
        context.setFactoryCode(scheduleResult.getFactoryCode());
        context.setScheduleDate(scheduleResult.getScheduleDate());
        context.setBatchNo(scheduleResult.getBatchNo());
        context.setTraceId(IdUtil.fastSimpleUUID());
        context.setOperator("system");
        return context;
    }

    /**
     * 根据排程结果构造任务草稿。
     *
     * @param scheduleResult 排程结果
     * @param shiftOrder     班次顺序
     * @return 任务草稿
     */
    private TmTaskDraft buildTaskDraft(TmScheduleResult scheduleResult, Integer shiftOrder) {
        TmTaskDraft taskDraft = new TmTaskDraft();
        taskDraft.setOrderNo(StrUtil.blankToDefault(scheduleResult.getOrderNo(),
                scheduleResult.getId() == null ? IdUtil.fastSimpleUUID() : String.valueOf(scheduleResult.getId())));
        taskDraft.setTreadCode(StrUtil.blankToDefault(scheduleResult.getTreadCode(), ""));
        taskDraft.setGlueCode(StrUtil.blankToDefault(scheduleResult.getGlueCode(), ""));
        taskDraft.setMouthPlateCode(StrUtil.blankToDefault(scheduleResult.getMouthPlateCode(), ""));
        taskDraft.setMachineCode(scheduleResult.getMachineCode());
        taskDraft.setShiftOrder(shiftOrder);
        taskDraft.setPlanQty(resolvePlanQty(scheduleResult, shiftOrder));
        return taskDraft;
    }

    /**
     * 将当前任务种入操作上下文，供 Facade 后续转机台、调量查找节点。
     *
     * @param context     操作上下文
     * @param taskDraft   任务草稿
     * @param machineCode 机台编码
     * @param shiftOrder  班次顺序
     */
    private void seedOperationTask(TmScheduleContext context, TmTaskDraft taskDraft, String machineCode, Integer shiftOrder) {
        LocalDate localDate = DateUtil.toLocalDateTime(context.getScheduleDate()).toLocalDate();
        ScheduleTaskLinkedList<TmTaskDraft> chain = context.getTaskChainGroup().getOrCreate(machineCode, localDate, shiftOrder);
        ScheduleTaskNode<TmTaskDraft> node = new ScheduleTaskNode<>(taskDraft.getBusinessKey(), taskDraft, machineCode,
                localDate, "CLASS" + shiftOrder, shiftOrder, taskDraft.getPlanQty());
        chain.append(node, new ScheduleOperationContext(context.getOperator(), "MANUAL_SEED", context.getTraceId()));
        context.registerTaskNode(taskDraft.getBusinessKey(), node);
    }

    /**
     * 解析指定班次计划量。
     *
     * @param scheduleResult 排程结果
     * @param shiftOrder     班次顺序
     * @return 计划量
     */
    private BigDecimal resolvePlanQty(TmScheduleResult scheduleResult, Integer shiftOrder) {
        if (Integer.valueOf(1).equals(shiftOrder)) {
            return Optional.ofNullable(scheduleResult.getClass1PlanQty()).orElse(BigDecimal.ZERO);
        }
        if (Integer.valueOf(2).equals(shiftOrder)) {
            return Optional.ofNullable(scheduleResult.getClass2PlanQty()).orElse(BigDecimal.ZERO);
        }
        if (Integer.valueOf(3).equals(shiftOrder)) {
            return Optional.ofNullable(scheduleResult.getClass3PlanQty()).orElse(BigDecimal.ZERO);
        }
        if (Integer.valueOf(4).equals(shiftOrder)) {
            return Optional.ofNullable(scheduleResult.getClass4PlanQty()).orElse(BigDecimal.ZERO);
        }
        if (Integer.valueOf(5).equals(shiftOrder)) {
            return Optional.ofNullable(scheduleResult.getClass5PlanQty()).orElse(BigDecimal.ZERO);
        }
        if (Integer.valueOf(6).equals(shiftOrder)) {
            return Optional.ofNullable(scheduleResult.getClass6PlanQty()).orElse(BigDecimal.ZERO);
        }
        return BigDecimal.ZERO;
    }

    /**
     * 获取胎面排程错误提示。
     *
     * @param errorCode 错误码
     * @return 当前语言环境下的错误提示
     */
    private String scheduleErrorMessage(TmScheduleErrorCodeEnum errorCode) {
        String message = I18nUtil.getMessage(errorCode.getMessageKey());
        return StringUtils.isBlank(message) || errorCode.getMessageKey().equals(message)
                ? errorCode.getDefaultMessage() : message;
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
     * 根据排程日期构建6个班次的日期展示列表
     * 胎面排程6个班次覆盖D日中班、D+1日夜早中、D+2日夜早（D=排程日期-2，即今天）：
     * 班次1：D日中班，班次2~4：D+1日夜早中，班次5~6：D+2日夜早
     *
     * @param scheduleDate 排程日期
     * @return 班次日期列表
     */
    @Override
    public List<TmScheduleShiftDateVO> listScheduleShiftDates(Date scheduleDate) {
        if (scheduleDate == null) {
            scheduleDate = DateUtil.offsetDay(new Date(), 2);
        }
        // D = 排程日期 - 2（即今天）
        Date dDay = DateUtil.offsetDay(scheduleDate, -2);
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

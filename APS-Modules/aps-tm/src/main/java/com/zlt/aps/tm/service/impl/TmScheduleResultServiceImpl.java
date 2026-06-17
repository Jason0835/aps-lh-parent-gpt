package com.zlt.aps.tm.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.tm.api.domain.entity.TmDispatcherLog;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleRequestVo;
import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleResponseVo;
import com.zlt.aps.tm.engine.domain.TmPersistResult;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.template.TmScheduleTemplateImpl;
import com.zlt.aps.tm.mapper.*;
import com.zlt.aps.tm.service.ITmScheduleResultService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎面排程结果表 业务层处理
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TmScheduleResultServiceImpl extends AbstractDocService<TmScheduleResult> implements ITmScheduleResultService {

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
                String message = String.format(I18nUtil.getMessage("ui.data.alert.tm.machineCodeNotExist"), machineCode);
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
    public TmAutoScheduleResponseVo validateAutoPlan(TmAutoScheduleRequestVo request) {
        validateAutoScheduleRequest(request);
        TmAutoScheduleResponseVo response = buildAutoScheduleResponse(request);
        List<TmScheduleResult> currentResultList = listBoard(buildQueryFromRequest(request));
        fillOverwriteCheckResult(request, response, currentResultList, false);
        response.setSuccess(Boolean.TRUE);
        response.setMessage(Boolean.TRUE.equals(response.getConfirmRequired())
                ? "当前排程日期已有未发布计划，确认后将重新生成" : "自动排程校验通过");
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
    public TmAutoScheduleResponseVo autoPlan(TmAutoScheduleRequestVo request) {
        validateAutoScheduleRequest(request);
        TmAutoScheduleResponseVo response = buildAutoScheduleResponse(request);
        List<TmScheduleResult> currentResultList = listBoard(buildQueryFromRequest(request));
        fillOverwriteCheckResult(request, response, currentResultList, true);
        if (CollUtil.isNotEmpty(currentResultList)) {
            logicDeleteByFactoryCodeAndScheduleDate(request.getFactoryCode(), request.getScheduleDate());
        }
        TmScheduleContext context = buildScheduleContext(request, response);
        TmAutoScheduleResponseVo engineResponse = tmScheduleTemplate.execute(context);
        TmPersistResult persistResult = context.getPersistResult() == null ? new TmPersistResult() : context.getPersistResult();
        response.setSuccess(Boolean.TRUE);
        response.setConfirmRequired(Boolean.FALSE);
        response.setBatchNo(context.getBatchNo());
        response.setTraceId(context.getTraceId());
        response.setResultCount(persistResult.getResultCount());
        response.setUnplannedCount(persistResult.getUnplannedCount());
        response.setMessage(engineResponse != null && StrUtil.isNotBlank(engineResponse.getMessage())
                ? engineResponse.getMessage() : "胎面自动排程执行完成");
        return response;
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
            throw new ServiceException("插单排程结果不能为空");
        }
        if (StrUtil.isBlank(scheduleResult.getFactoryCode())) {
            scheduleResult.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        if (scheduleResult.getScheduleDate() == null) {
            throw new ServiceException("插单排程日期不能为空");
        }
        if (StrUtil.isBlank(scheduleResult.getTreadCode())) {
            throw new ServiceException("插单胎面编码不能为空");
        }
        validateInsertAfterSecondSequence(scheduleResult);
        scheduleResult.setReleaseStatus(ApsConstant.NO_RELEASE);
        int insertCount = tmScheduleResultMapper.insert(scheduleResult);
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
            throw new ServiceException("调量排程结果ID不能为空");
        }
        if (isReleasingOrTimeoutByIds(new Long[]{scheduleResult.getId()}) > 0) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        scheduleResult.setBaseVale(scheduleResult.getId());
        insetDispatcherLog(ApsConstant.DISPATCHER_OPER_PLAN, scheduleResult);
        return updateTmScheduleResult(scheduleResult);
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
            throw new ServiceException("发布排程结果ID不能为空");
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
        int updateCount = 0;
        for (Long id : ids) {
            TmScheduleResult updateEntity = new TmScheduleResult();
            updateEntity.setId(id);
            updateEntity.setReleaseStatus(ApsConstant.WAIT_RELEASING);
            updateCount += tmScheduleResultMapper.updateById(updateEntity);
        }
        return updateCount;
    }

    /**
     * 校验自动排程请求必填字段。
     *
     * @param request 自动排程请求
     * @throws ServiceException 工厂或排程日期为空时抛出
     */
    private void validateAutoScheduleRequest(TmAutoScheduleRequestVo request) {
        if (request == null) {
            throw new ServiceException("自动排程请求不能为空");
        }
        if (StrUtil.isBlank(request.getFactoryCode())) {
            throw new ServiceException("自动排程工厂编号不能为空");
        }
        if (request.getScheduleDate() == null) {
            throw new ServiceException("自动排程日期不能为空");
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
        response.setBatchNo("TM-" + DateUtil.format(request.getScheduleDate(), "yyyyMMdd"));
        response.setTraceId(StrUtil.blankToDefault(request.getTraceId(), IdUtil.fastSimpleUUID()));
        response.setResultCount(0);
        response.setUnplannedCount(0);
        response.setConfirmRequired(Boolean.FALSE);
        return response;
    }

    /**
     * 根据自动排程请求构建看板查询条件。
     *
     * @param request 自动排程请求
     * @return 看板查询条件
     */
    private TmScheduleResult buildQueryFromRequest(TmAutoScheduleRequestVo request) {
        TmScheduleResult query = new TmScheduleResult();
        query.setFactoryCode(request.getFactoryCode());
        query.setScheduleDate(request.getScheduleDate());
        return query;
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
            throw new ServiceException(String.format("排程日期：%s已有发布过的生成计划，不可重复生成",
                    DateUtil.formatDate(request.getScheduleDate())));
        }
        response.setConfirmRequired(Boolean.TRUE);
        if (executeMode && !Boolean.TRUE.equals(request.getConfirmOverwrite())) {
            throw new ServiceException("当前排程日期已有未发布计划，请确认后重新生成");
        }
    }

    /**
     * 校验人工插单只能插到第二个在产规格之后。
     *
     * <p>当前实体按 1-6 班横向字段承载顺序，若前端传入任一班次顺序且小于等于 2，
     * 说明插单位置不在第二顺序之后，需要直接拒绝。未传顺序时保持旧接口兼容。</p>
     *
     * @param scheduleResult 插单排程结果
     * @throws ServiceException 插单位置不在第二顺序之后时抛出
     */
    private void validateInsertAfterSecondSequence(TmScheduleResult scheduleResult) {
        List<Integer> sequenceList = Arrays.asList(scheduleResult.getClass1Sequence(), scheduleResult.getClass2Sequence(),
                scheduleResult.getClass3Sequence(), scheduleResult.getClass4Sequence(), scheduleResult.getClass5Sequence(),
                scheduleResult.getClass6Sequence());
        boolean invalid = sequenceList.stream().anyMatch(sequence -> sequence != null && sequence <= 2);
        if (invalid) {
            throw new ServiceException("当前机台班次只能插到第二个在产规格之后");
        }
    }
}

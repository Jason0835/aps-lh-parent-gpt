package com.zlt.aps.lh.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.domain.dto.*;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhScheduleResultTemplateImportVO;
import com.zlt.aps.lh.api.domain.vo.LhScheduleShiftDateVO;
import com.zlt.aps.lh.api.enums.DeleteFlagEnum;
import com.zlt.aps.lh.api.enums.FactoryCodeEnum;
import com.zlt.aps.lh.api.enums.ReleaseStatusEnum;
import com.zlt.aps.lh.component.LhScheduleConfigResolver;
import com.zlt.aps.lh.component.ScheduleExecutionGuard;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.decorator.IScheduleExecutor;
import com.zlt.aps.lh.engine.observer.ScheduleEvent;
import com.zlt.aps.lh.engine.observer.ScheduleEventPublisher;
import com.zlt.aps.lh.exception.ScheduleException;
import com.zlt.aps.lh.mapper.LhScheduleResultMapper;
import com.zlt.aps.lh.service.ILhScheduleService;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.utils.ImportExcelValidatedUtils;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 硫化排程主服务实现
 * <p>排程入口，负责构建上下文并委托给排程执行器</p>
 *
 * @author APS
 */
@Slf4j
@Service
public class LhScheduleServiceImpl extends AbstractDocService<LhScheduleResult> implements ILhScheduleService {

    @Resource
    private IScheduleExecutor scheduleExecutor;

    @Resource
    private LhScheduleConfigResolver scheduleConfigResolver;

    @Resource
    private LhScheduleResultMapper scheduleResultMapper;

    @Resource
    private ScheduleEventPublisher scheduleEventPublisher;

    @Resource
    private ScheduleExecutionGuard scheduleExecutionGuard;

    @Resource
    private LhTextMouldChangePlanGenerator textMouldChangePlanGenerator;

    @Resource
    private LhIncreaseMouldStartPlanService increaseMouldStartPlanService;

    @Override
    public LhScheduleResponseDTO executeSchedule(LhScheduleRequestDTO request) {
        log.info("接收排程请求, 工厂: {}, 日期: {}, 月计划版本: {}, 生产版本: {}",
                request.getFactoryCode(), LhScheduleTimeUtil.formatDate(request.getScheduleDate()),
                request.getMonthPlanVersion(), request.getProductionVersion());
        LhScheduleContext context = buildContext(request);
        String lockToken = null;
        try {
            log.info("准备获取排程执行锁, 工厂: {}, 目标日: {}, T日: {}, 排程天数: {}",
                    context.getFactoryCode(),
                    LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate()),
                    LhScheduleTimeUtil.formatDate(context.getScheduleDate()),
                    context.getScheduleConfig().getScheduleDays());
            lockToken = scheduleExecutionGuard.acquire(context.getFactoryCode(), context.getScheduleTargetDate());
            LhScheduleResponseDTO response = scheduleExecutor.execute(context);
            log.info("排程服务执行完成, 工厂: {}, 批次号: {}, 成功: {}, 排程结果数: {}, 未排产数: {}, 模具计划数: {}",
                    context.getFactoryCode(), response.getBatchNo(), response.isSuccess(),
                    response.getScheduleResultCount(), response.getUnscheduledCount(), response.getMouldChangePlanCount());
            return response;
        } catch (ScheduleException e) {
            log.warn("排程请求被拒绝, 工厂: {}, 日期: {}, 原因: {}",
                    context.getFactoryCode(), LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate()), e.getMessage());
            return LhScheduleResponseDTO.fail(context.getBatchNo(), e.getMessage());
        } catch (Exception e) {
            log.error("排程服务入口异常, 工厂: {}, 日期: {}",
                    context.getFactoryCode(), LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate()), e);
            return LhScheduleResponseDTO.fail(context.getBatchNo(), "排程执行异常: " + e.getMessage());
        } finally {
            scheduleExecutionGuard.release(context.getFactoryCode(), context.getScheduleTargetDate(), lockToken);
            log.debug("排程执行锁释放完成, 工厂: {}, 目标日: {}, 锁令牌是否存在: {}",
                    context.getFactoryCode(), LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate()),
                    StringUtils.isNotEmpty(lockToken));
        }
    }

    @Override
    public LhScheduleResponseDTO publishSchedule(String batchNo) {
        log.info("发布排程结果, 批次号: {}", batchNo);
        try {
            // 1. 查询批次号对应的排程结果
            List<LhScheduleResult> results = scheduleResultMapper.selectList(new LambdaQueryWrapper<LhScheduleResult>()
                    .eq(LhScheduleResult::getBatchNo, batchNo)
                    .eq(LhScheduleResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode()));
            if (results == null || results.isEmpty()) {
                log.warn("发布排程结果失败, 未查询到有效排程结果, 批次号: {}", batchNo);
                return LhScheduleResponseDTO.fail(batchNo, "批次号[" + batchNo + "]对应的排程结果不存在");
            }

            // 2. 更新发布状态为"已发布"（1）
            for (LhScheduleResult result : results) {
                result.setIsRelease(ReleaseStatusEnum.RELEASED.getCode());
            }
            scheduleResultMapper.update(null, new LambdaUpdateWrapper<LhScheduleResult>()
                    .set(LhScheduleResult::getIsRelease, ReleaseStatusEnum.RELEASED.getCode())
                    .eq(LhScheduleResult::getBatchNo, batchNo)
                    .eq(LhScheduleResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode()));

            // 3. 发布排程结果发布事件（通知MES系统）
            LhScheduleContext publishContext = new LhScheduleContext();
            publishContext.setBatchNo(batchNo);
            publishContext.setScheduleResultList(results);
            scheduleEventPublisher.publish(ScheduleEvent.published(publishContext));

            log.info("排程结果发布成功, 批次号: {}, 发布记录数: {}", batchNo, results.size());
            return LhScheduleResponseDTO.success(batchNo, "发布成功，共发布" + results.size() + "条记录");

        } catch (Exception e) {
            log.error("发布排程结果异常, 批次号: {}", batchNo, e);
            return LhScheduleResponseDTO.fail(batchNo, "发布失败: " + e.getMessage());
        }
    }

    /**
     * 构建排程上下文
     * <p>先解析本次排程配置快照，再按 scheduleDays 计算窗口起点 T 日</p>
     *
     * @param request 排程请求
     * @return 排程上下文
     */
    private LhScheduleContext buildContext(LhScheduleRequestDTO request) {
        LhScheduleContext context = new LhScheduleContext();
        String factoryCode = request.getFactoryCode();
        context.setFactoryCode(factoryCode);
        context.setMonthPlanVersion(StringUtils.isNotEmpty(request.getMonthPlanVersion())
                ? request.getMonthPlanVersion().trim() : request.getMonthPlanVersion());
        context.setOperator(StringUtils.isNotEmpty(request.getOperator()) ? request.getOperator().trim() : request.getOperator());
        // 工厂名称来源于工厂枚举：116=越南，117=泰国
        context.setFactoryName(FactoryCodeEnum.getFactoryNameByCode(factoryCode));
        // 请求日期为排程目标日
        Date target = LhScheduleTimeUtil.clearTime(
                request.getScheduleDate() != null ? request.getScheduleDate() : new Date());
        context.setScheduleTargetDate(target);
        scheduleConfigResolver.resolveAndAttach(context);
        int scheduleDays = context.getScheduleConfig().getScheduleDays();
        int offsetDays = Math.max(0, scheduleDays - 1);
        // 引擎使用 T 日 = 目标日 − (连续排程日历跨度 − 1)
        context.setScheduleDate(LhScheduleTimeUtil.addDays(target, -offsetDays));
        log.info("排程上下文构建完成, 工厂: {}, 工厂名称: {}, 目标日: {}, T日: {}, 排程天数: {}, 强制重排: {}, 局部搜索: {}, 定点机台规则: {}",
                context.getFactoryCode(), context.getFactoryDisplayName(),
                LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate()),
                LhScheduleTimeUtil.formatDate(context.getScheduleDate()), scheduleDays,
                context.getScheduleConfig().isForceRescheduleEnabled(),
                context.getScheduleConfig().isLocalSearchEnabled(),
                context.getScheduleConfig().isSpecifyMachineRuleEnabled());
        return context;
    }

    @Override
    public String getDocTypeCode() {
        return "OUT2046";
    }


    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("OUT2046");
        return sysDocType;
    }


    @Override
    public List<LhScheduleShiftDateVO> listScheduleShiftDates(Date scheduleDate) {
        if (Objects.isNull(scheduleDate)) {
            log.warn("listScheduleShiftDates: scheduleDate 为空");
            return new ArrayList<>();
        }
        Date end = DateUtil.beginOfDay(scheduleDate);
        Date start = DateUtil.offsetDay(end, -(LhScheduleConstant.SCHEDULE_DAYS - 1));
        List<LhScheduleShiftDateVO> result = new ArrayList<>(LhScheduleConstant.MAX_SHIFT_SLOT_COUNT);
        int shiftNo = 1;
        for (int dayIndex = 0; dayIndex < LhScheduleConstant.SCHEDULE_DAYS; dayIndex++) {
            int shiftsThisDay = dayIndex == 0
                    ? LhScheduleConstant.SCHEDULE_SHIFT_DATE_WINDOW_FIRST_DAY_SHIFT_COUNT
                    : LhScheduleConstant.SCHEDULE_SHIFT_DATE_WINDOW_OTHER_DAY_SHIFT_COUNT;
            Date current = DateUtil.offsetDay(start, dayIndex);
            String shiftDateStr = DateUtil.format(current, LhScheduleConstant.SCHEDULE_SHIFT_DATE_DISPLAY_PATTERN);
            for (int i = 0; i < shiftsThisDay; i++) {
                LhScheduleShiftDateVO vo = new LhScheduleShiftDateVO();
                vo.setShift(shiftNo);
                vo.setShiftDate(shiftDateStr);
                result.add(vo);
                shiftNo++;
            }
        }
        return result;
    }

    /**
     * 转机台前校验接口
     *
     * @param dto 参数
     * @return 结果
     */
    @Override
    public AjaxResult changeMachinePreCheck(LhTransferDeskDTO dto) {
        if (dto.getId() == null) {
            return AjaxResult.error("请选择需要转机台的记录");
        }
        if (dto.getLhMachineCode() == null) {
            return AjaxResult.error("新机台编码不能为空");
        }

        Long ids = dto.getId();
        String newMachineCode = dto.getLhMachineCode();
        List<LhScheduleResult> existResultList = scheduleResultMapper.changeMachinePreCheck(Collections.singletonList(ids), newMachineCode);

        List<String> errorMessages = new ArrayList<>();
        for (LhScheduleResult existResult : existResultList) {
            String existScheduleDate = DateUtil.format(existResult.getScheduleDate(), "yyyy-MM-dd");
            String existMachineCode = existResult.getLhMachineCode();
            String existMaterialCode = existResult.getMaterialCode();
            errorMessages.add(String.format("排程日期:%s，物料编码：%s，机台编号：%s，已经存在！", existScheduleDate, existMaterialCode, existMachineCode));
        }
        if (CollUtil.isNotEmpty(errorMessages)) {
            return AjaxResult.error(String.join(";", errorMessages));
        }
        return AjaxResult.success();
    }

    /**
     * 转机台操作
     *
     * @param dto 参数
     * @return 结果
     */
    @Override
    public AjaxResult changeMachine(LhTransferDeskDTO dto) {
        if (dto.getId() == null) {
            return AjaxResult.error("请选择需要转机台的记录");
        }
        if (dto.getLhMachineCode() == null) {
            return AjaxResult.error("新机台编码不能为空");
        }

        // 检查所有记录是否已发布
        LhScheduleResult record = scheduleResultMapper.selectById(dto.getId());

        // 更新机台信息
        String oldMachine = record.getLhMachineCode();
        record.setLhMachineCode(dto.getLhMachineCode());
        record.setLhMachineName(dto.getLhMachineName());

        // 更新备注
        String remark = record.getRemark() != null ? record.getRemark() : "";
        record.setRemark(remark + "转机台时间：" + DateUtil.now() + "【原机台：" + oldMachine + ",转入机台：" + dto.getLhMachineCode() + "】");

        // 发布状态是已发布的话，需要更新为待发布
        if (ReleaseStatusEnum.RELEASED.getCode().equals(record.getIsRelease())) {
            record.setIsRelease(ReleaseStatusEnum.PENDING_RELEASE.getCode());
        }

        scheduleResultMapper.updateById(record);
        return AjaxResult.success("转机台成功");
    }

    /**
     * 调量前校验
     *
     * @param dto 参数
     * @return 结果
     */
    @Override
    public AjaxResult adjustQuantityPreCheck(LhScheduleResultUpdateDTO dto) {
        if (Objects.isNull(dto) || Objects.isNull(dto.getId())) {
            return AjaxResult.error("请选择需要调量的排程记录");
        }

        LhScheduleResult record = scheduleResultMapper.selectById(dto.getId());
        if (Objects.isNull(record)) {
            return AjaxResult.error("排程记录不存在或已删除");
        }

        Date now = new Date();
        boolean hasAdjustField = false;
        List<String> errorMessages = new ArrayList<>();
        for (int shiftIndex = 1; shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            Integer adjustPlanQty = getAdjustPlanQty(dto, shiftIndex);
            if (Objects.isNull(adjustPlanQty)) {
                continue;
            }
            hasAdjustField = true;
            if (adjustPlanQty < 0) {
                errorMessages.add(String.format("第%s班计划量不能小于0", shiftIndex));
            }
            /*Date shiftEndTime = ShiftFieldUtil.getShiftEndTime(record, shiftIndex);
            if (Objects.isNull(shiftEndTime)) {
                errorMessages.add(String.format("第%s班结束时间缺失，禁止调量", shiftIndex));
            } else if (!now.before(shiftEndTime)) {
                errorMessages.add(String.format("第%s班已结束，历史班次不可调量", shiftIndex));
            }*/
            Integer finishQty = Optional.ofNullable(ShiftFieldUtil.getShiftFinishQty(record, shiftIndex)).orElse(0);
            if (adjustPlanQty < finishQty) {
                errorMessages.add(String.format("第%s班计划量不能小于完成量%s", shiftIndex, finishQty));
            }
        }

        if (!hasAdjustField) {
            errorMessages.add("未检测到可调量的班次计划量字段");
        }
        if (CollUtil.isNotEmpty(errorMessages)) {
            return AjaxResult.error(String.join("；", errorMessages));
        }
        return AjaxResult.success();
    }

    /**
     * 调量操作
     *
     * @param dto 参数
     * @return 结果
     */
    @Override
    public AjaxResult adjustQuantity(LhScheduleResultUpdateDTO dto) {
        AjaxResult preCheck = adjustQuantityPreCheck(dto);
        if (preCheck.get(AjaxResult.CODE_TAG).equals(AjaxResult.Type.ERROR.value())) {
            return preCheck;
        }

        LhScheduleResult record = scheduleResultMapper.selectById(dto.getId());
        if (Objects.isNull(record)) {
            return AjaxResult.error("排程记录不存在或已删除");
        }

        for (int shiftIndex = 1; shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            Integer adjustPlanQty = getAdjustPlanQty(dto, shiftIndex);
            if (Objects.nonNull(adjustPlanQty)) {
                setAdjustPlanQty(record, shiftIndex, adjustPlanQty);
                setAdjustAnalysis(record, shiftIndex, getAdjustAnalysis(dto, shiftIndex));
            }
        }

        // 调量后同步汇总计划量，并回置为未发布状态，确保后续重新发布
        ShiftFieldUtil.syncDailyPlanQty(record);
        // 发布状态是已发布的话，需要更新为待发布
        if (ReleaseStatusEnum.RELEASED.getCode().equals(record.getIsRelease())) {
            record.setIsRelease(ReleaseStatusEnum.PENDING_RELEASE.getCode());
        }

        int updateCount = scheduleResultMapper.updateById(record);
        if (updateCount <= 0) {
            return AjaxResult.error("调量失败，请稍后重试");
        }
        return AjaxResult.success("调量成功，记录已回置待发布");
    }

    /**
     * 根据单条排程结果生成文字示方换模计划。
     *
     * @param dto 生成入参
     * @return 处理结果
     */
    @Override
    public AjaxResult generateTextMouldChangePlan(LhGenerateTextMouldPlanDTO dto) {
        return textMouldChangePlanGenerator.generate(dto);
    }

    /**
     * 换模开产增加计划。
     *
     * @param scheduleResult 当前硫化排程结果
     * @return 处理结果
     */
    @Override
    public AjaxResult increaseMouldStartPlan(LhScheduleResult scheduleResult) {
        return increaseMouldStartPlanService.increase(scheduleResult);
    }

    /**
     * 获取指定班次的调量计划值。
     *
     * @param dto        调量参数
     * @param shiftIndex 班次索引（1~8）
     * @return 班次计划量，未传入返回null
     */
    private Integer getAdjustPlanQty(LhScheduleResultUpdateDTO dto, int shiftIndex) {
        switch (shiftIndex) {
            case 1:
                return dto.getClass1PlanQty();
            case 2:
                return dto.getClass2PlanQty();
            case 3:
                return dto.getClass3PlanQty();
            case 4:
                return dto.getClass4PlanQty();
            case 5:
                return dto.getClass5PlanQty();
            case 6:
                return dto.getClass6PlanQty();
            case 7:
                return dto.getClass7PlanQty();
            case 8:
                return dto.getClass8PlanQty();
            default:
                return null;
        }
    }

    /**
     * 设置指定班次计划量。
     *
     * @param record     排程结果
     * @param shiftIndex 班次索引（1~8）
     * @param planQty    调整后的计划量
     */
    private void setAdjustPlanQty(LhScheduleResult record, int shiftIndex, Integer planQty) {
        switch (shiftIndex) {
            case 1:
                record.setClass1PlanQty(planQty);
                break;
            case 2:
                record.setClass2PlanQty(planQty);
                break;
            case 3:
                record.setClass3PlanQty(planQty);
                break;
            case 4:
                record.setClass4PlanQty(planQty);
                break;
            case 5:
                record.setClass5PlanQty(planQty);
                break;
            case 6:
                record.setClass6PlanQty(planQty);
                break;
            case 7:
                record.setClass7PlanQty(planQty);
                break;
            case 8:
                record.setClass8PlanQty(planQty);
                break;
            default:
                break;
        }
    }

    /**
     * 获取指定班次的调量原因分析。
     *
     * @param dto        调量参数
     * @param shiftIndex 班次索引（1~8）
     * @return 原因分析，未传入返回null
     */
    private String getAdjustAnalysis(LhScheduleResultUpdateDTO dto, int shiftIndex) {
        switch (shiftIndex) {
            case 1:
                return dto.getClass1Analysis();
            case 2:
                return dto.getClass2Analysis();
            case 3:
                return dto.getClass3Analysis();
            case 4:
                return dto.getClass4Analysis();
            case 5:
                return dto.getClass5Analysis();
            case 6:
                return dto.getClass6Analysis();
            case 7:
                return dto.getClass7Analysis();
            case 8:
                return dto.getClass8Analysis();
            default:
                return null;
        }
    }

    /**
     * 设置指定班次原因分析。
     *
     * @param record     排程结果
     * @param shiftIndex 班次索引（1~8）
     * @param analysis   原因分析，为null时保留原值
     */
    private void setAdjustAnalysis(LhScheduleResult record, int shiftIndex, String analysis) {
        if (Objects.isNull(analysis)) {
            return;
        }
        ShiftFieldUtil.setShiftAnalysis(record, shiftIndex, analysis);
    }
    /**
     * 导出数据
     *
     * @param list 数据列表
     * @param scheduleDate 排程日期
     * @return 导出数据
     */
    @Override
    public byte[] exportData(List<LhScheduleResult> list, Date scheduleDate) {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("excelModel/lhjhtemplate.xlsx");
        if (Objects.isNull(inputStream)) {
            throw new ServiceException("硫化计划导出模板不存在");
        }
        List<LhScheduleResult> exportList = Objects.isNull(list) ? Collections.emptyList() : list;
        Map<String, Object> tableMap = buildExportTableMap(exportList, scheduleDate);
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        excelDataList.add(buildExportDataList(exportList));
        return ExcelUtils.writeMultiList(inputStream, 1, tableMap, excelDataList);
    }


    @Override
    public AjaxResult importScheduleTemplate(List<LhScheduleResultTemplateImportVO> list, LhScheduleResult result, boolean updateSupport, Long id) {
        if (Objects.isNull(result) || StringUtils.isBlank(result.getFactoryCode()) || Objects.isNull(result.getScheduleDate())) {
            return AjaxResult.error("导入条件中的工厂和排程日期不能为空");
        }
        if (Objects.isNull(list) || list.isEmpty()) {
            return AjaxResult.error("导入文件未读取到有效明细行");
        }

        Date scheduleDate = DateUtil.beginOfDay(result.getScheduleDate());
        String factoryCode = result.getFactoryCode().trim();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        int successNum = 0;
        int failureNum = 0;

        // 第一轮：注解必填和Excel内重复校验（模板数据从第9行开始）
        for (int i = 0; i < list.size(); i++) {
            int rowNum = i + 9;
            LhScheduleResultTemplateImportVO row = list.get(i);
            if (Objects.isNull(row)) {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(id, rowNum,
                        buildRequiredMessage(rowNum, "ui.data.column.import.errorRow", "row"), importErrorLogs);
                continue;
            }
            row.setFactoryCode(factoryCode);
            row.setScheduleDate(scheduleDate);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(id, rowNum, row);
            ImportExcelValidatedUtils.validatedRepeat(list, row, i, 9, id, validated, "lhMachineCode", "materialCode");
            if (PubUtil.isNotEmpty(validated)) {
                failureNum++;
                row.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        Map<Integer, Date[]> shiftTimeMap = buildShiftTimeMap(scheduleDate);
        Set<String> importUniqueKeys = new HashSet<>();

        List<String> machineCodes = list.stream()
                .filter(Objects::nonNull)
                .filter(item -> !Objects.equals(item.getId(), -999L))
                .map(LhScheduleResultTemplateImportVO::getLhMachineCode)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        List<String> materialCodes = list.stream()
                .filter(Objects::nonNull)
                .filter(item -> !Objects.equals(item.getId(), -999L))
                .map(LhScheduleResultTemplateImportVO::getMaterialCode)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());

        Map<String, LhScheduleResult> existMap = new HashMap<>(16);
        if (!machineCodes.isEmpty() && !materialCodes.isEmpty()) {
            List<LhScheduleResult> exists = scheduleResultMapper.selectList(new LambdaQueryWrapper<LhScheduleResult>()
                    .eq(LhScheduleResult::getFactoryCode, factoryCode)
                    .eq(LhScheduleResult::getScheduleDate, scheduleDate)
                    .in(LhScheduleResult::getLhMachineCode, machineCodes)
                    .in(LhScheduleResult::getMaterialCode, materialCodes)
                    .eq(LhScheduleResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode()));
            existMap = exists.stream().collect(Collectors.toMap(
                    this::buildImportUniqueKey,
                    item -> item,
                    (oldValue, newValue) -> oldValue,
                    LinkedHashMap::new
            ));
        }

        for (int i = 0; i < list.size(); i++) {
            LhScheduleResultTemplateImportVO row = list.get(i);
            int rowNum = i + 9;
            if (Objects.isNull(row) || Objects.equals(row.getId(), -999L)) {
                continue;
            }
            List<String> rowErrors = validateImportRow(row, factoryCode, scheduleDate, importUniqueKeys, rowNum);
            if (!rowErrors.isEmpty()) {
                failureNum++;
                importErrorLogs.add(new ImportErrorLog(id, rowNum, String.join(";", rowErrors)));
                continue;
            }

            String uniqueKey = buildImportUniqueKey(factoryCode, scheduleDate, row.getLhMachineCode(), row.getMaterialCode());
            LhScheduleResult target = existMap.get(uniqueKey);
            boolean isInsert = Objects.isNull(target);

            if (isInsert) {
                target = new LhScheduleResult();
                target.setIsDelete(DeleteFlagEnum.NORMAL.getCode());
                target.setIsRelease(ReleaseStatusEnum.NOT_RELEASED.getCode());
                target.setDataSource("2");
            }
            target.setFactoryCode(factoryCode);
            target.setScheduleDate(scheduleDate);
            target.setLhMachineCode(row.getLhMachineCode().trim());
            target.setMaterialCode(row.getMaterialCode().trim());

            copyImportRowToEntity(row, target);
            fillShiftTimes(target, shiftTimeMap);

            if (isInsert) {
                scheduleResultMapper.insert(target);
                existMap.put(uniqueKey, target);
            } else {
                scheduleResultMapper.updateById(target);
            }
            successNum++;
        }

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    private List<String> validateImportRow(LhScheduleResultTemplateImportVO row,
                                           String factoryCode,
                                           Date scheduleDate,
                                           Set<String> importUniqueKeys,
                                           Integer rowNum) {
        List<String> errors = new ArrayList<>();
        if (Objects.isNull(row)) {
            errors.add(buildRequiredMessage(rowNum, "ui.data.column.import.errorRow", "row"));
            return errors;
        }
        if (StringUtils.isBlank(row.getLhMachineCode())) {
            errors.add(buildRequiredMessage(rowNum, "ui.data.column.lhScheduleResult.lhMachineCode", "lhMachineCode"));
        }
        if (StringUtils.isBlank(row.getMaterialCode())) {
            errors.add(buildRequiredMessage(rowNum, "ui.data.column.lhScheduleResult.materialCode", "materialCode"));
        }
        if (errors.isEmpty()) {
            String uniqueKey = buildImportUniqueKey(factoryCode, scheduleDate, row.getLhMachineCode(), row.getMaterialCode());
            if (!importUniqueKeys.add(uniqueKey)) {
                errors.add(I18nUtil.getMessage("ui.data.message.lhScheduleResult.import.excel.repeat.machineMaterial"));
            }
        }
        return errors;
    }

    private String buildRequiredMessage(Integer rowNum, String fieldI18nKey, String fallbackFieldName) {
        String requiredMsg = I18nUtil.getMessage("import.validated.required");
        String fieldName = I18nUtil.getMessage(fieldI18nKey);
        if (StringUtils.isBlank(fieldName) || StringUtils.equals(fieldName, fieldI18nKey)) {
            fieldName = fallbackFieldName;
        }
        return String.format(requiredMsg, rowNum, fieldName);
    }

    private String buildImportUniqueKey(LhScheduleResult entity) {
        return buildImportUniqueKey(entity.getFactoryCode(), entity.getScheduleDate(), entity.getLhMachineCode(), entity.getMaterialCode());
    }

    private String buildImportUniqueKey(String factoryCode, Date scheduleDate, String lhMachineCode, String materialCode) {
        return StringUtils.defaultString(factoryCode).trim() + "|"
                + DateUtil.format(DateUtil.beginOfDay(scheduleDate), "yyyy-MM-dd") + "|"
                + StringUtils.defaultString(lhMachineCode).trim() + "|"
                + StringUtils.defaultString(materialCode).trim();
    }

    private void copyImportRowToEntity(LhScheduleResultTemplateImportVO source, LhScheduleResult target) {
        target.setLhMachineName(source.getLhMachineName());
        target.setLeftRightMould(source.getLeftRightMould());
        target.setSpecCode(source.getSpecCode());
        target.setEmbryoCode(source.getEmbryoCode());
        target.setStructureName(source.getStructureName());
        target.setMaterialDesc(source.getMaterialDesc());
        target.setMainMaterialDesc(source.getMainMaterialDesc());
        target.setEmbryoStock(source.getEmbryoStock());
        target.setSpecDesc(source.getSpecDesc());
        target.setLhTime(source.getLhTime());
        target.setMouldSurplusQty(source.getMouldSurplusQty());
        target.setSingleMouldShiftQty(source.getSingleMouldShiftQty());
        target.setMouldMethod(source.getMouldMethod());
        target.setDailyPlanQty(source.getDailyPlanQty());
        target.setTotalDailyPlanQty(source.getDailyPlanQty());
        target.setRemark(source.getRemark());

        target.setClass1PlanQty(source.getClass1PlanQty());
        target.setClass1FinishQty(source.getClass1FinishQty());
        target.setClass1Analysis(source.getClass1Analysis());
        target.setClass2PlanQty(source.getClass2PlanQty());
        target.setClass2FinishQty(source.getClass2FinishQty());
        target.setClass2Analysis(source.getClass2Analysis());
        target.setClass3PlanQty(source.getClass3PlanQty());
        target.setClass3FinishQty(source.getClass3FinishQty());
        target.setClass3Analysis(source.getClass3Analysis());
        target.setClass4PlanQty(source.getClass4PlanQty());
        target.setClass4FinishQty(source.getClass4FinishQty());
        target.setClass4Analysis(source.getClass4Analysis());
        target.setClass5PlanQty(source.getClass5PlanQty());
        target.setClass5FinishQty(source.getClass5FinishQty());
        target.setClass5Analysis(source.getClass5Analysis());
        target.setClass6PlanQty(source.getClass6PlanQty());
        target.setClass6FinishQty(source.getClass6FinishQty());
        target.setClass6Analysis(source.getClass6Analysis());
        target.setClass7PlanQty(source.getClass7PlanQty());
        target.setClass7FinishQty(source.getClass7FinishQty());
        target.setClass7Analysis(source.getClass7Analysis());
        target.setClass8PlanQty(source.getClass8PlanQty());
        target.setClass8FinishQty(source.getClass8FinishQty());
        target.setClass8Analysis(source.getClass8Analysis());

        target.setScheduleType(normalizeScheduleTypeCode(source.getScheduleType()));
        if (StringUtils.isNotBlank(source.getIsFirst())) {
            target.setIsFirst(source.getIsFirst());
        }
        if (StringUtils.isNotBlank(source.getIsEnd())) {
            target.setIsEnd(source.getIsEnd());
        }
        if (StringUtils.isNotBlank(source.getConstructionStage())) {
            target.setConstructionStage(source.getConstructionStage());
        }
        if (StringUtils.isNotBlank(source.getScheduleOrder())) {
            target.setScheduleOrder(source.getScheduleOrder());
        }
    }

    private String normalizeScheduleTypeCode(String scheduleType) {
        if (StringUtils.isBlank(scheduleType)) {
            return scheduleType;
        }
        String value = scheduleType.trim();
        if ("01".equals(value) || "1".equals(value) || "\u7eed\u4f5c".equals(value)) {
            return "01";
        }
        if ("02".equals(value) || "2".equals(value) || "\u65b0\u589e".equals(value)) {
            return "02";
        }
        return convertScheduleTypeCode(value);
    }

    private String convertScheduleTypeCode(String scheduleType) {
        if (StringUtils.isBlank(scheduleType)) {
            return scheduleType;
        }
        String value = scheduleType.trim();
        if ("续作".equals(value)) {
            return "01";
        }
        if ("新增".equals(value)) {
            return "02";
        }
        return value;
    }

    private Map<Integer, Date[]> buildShiftTimeMap(Date scheduleDate) {
        Map<Integer, Date[]> shiftTimeMap = new HashMap<>(16);
        List<LhScheduleShiftDateVO> shiftDates = listScheduleShiftDates(scheduleDate);
        for (LhScheduleShiftDateVO shiftDateVO : shiftDates) {
            if (Objects.isNull(shiftDateVO) || Objects.isNull(shiftDateVO.getShift())) {
                continue;
            }
            int shift = shiftDateVO.getShift();
            Date shiftDate = parseShiftDate(scheduleDate, shiftDateVO.getShiftDate());
            if (Objects.isNull(shiftDate)) {
                continue;
            }
            Date[] shiftRange = resolveShiftRange(shift, shiftDate);
            shiftTimeMap.put(shift, shiftRange);
        }
        return shiftTimeMap;
    }

    private Date parseShiftDate(Date scheduleDate, String monthDay) {
        if (StringUtils.isBlank(monthDay)) {
            return null;
        }
        Date parsed = DateUtil.parse(DateUtil.year(scheduleDate) + "/" + monthDay.trim(), "yyyy/MM/dd");
        while (parsed.after(DateUtil.beginOfDay(scheduleDate))) {
            parsed = DateUtil.offsetYear(parsed, -1);
        }
        return DateUtil.beginOfDay(parsed);
    }

    private Date[] resolveShiftRange(int shift, Date shiftDate) {
        boolean nightShift = shift == 3 || shift == 6;
        boolean morningShift = shift == 1 || shift == 4 || shift == 7;
        boolean afternoonShift = shift == 2 || shift == 5 || shift == 8;

        if (nightShift) {
            Date start = DateUtil.offsetHour(DateUtil.offsetDay(shiftDate, -1), LhScheduleConstant.NIGHT_SHIFT_START_HOUR);
            Date end = DateUtil.offsetHour(shiftDate, LhScheduleConstant.MORNING_SHIFT_START_HOUR);
            return new Date[]{start, end};
        }
        if (morningShift) {
            Date start = DateUtil.offsetHour(shiftDate, LhScheduleConstant.MORNING_SHIFT_START_HOUR);
            Date end = DateUtil.offsetHour(shiftDate, LhScheduleConstant.AFTERNOON_SHIFT_START_HOUR);
            return new Date[]{start, end};
        }
        if (afternoonShift) {
            Date start = DateUtil.offsetHour(shiftDate, LhScheduleConstant.AFTERNOON_SHIFT_START_HOUR);
            Date end = DateUtil.offsetHour(shiftDate, LhScheduleConstant.NIGHT_SHIFT_START_HOUR);
            return new Date[]{start, end};
        }
        return new Date[]{null, null};
    }

    private void fillShiftTimes(LhScheduleResult target, Map<Integer, Date[]> shiftTimeMap) {
        setShiftTime(target, 1, shiftTimeMap.get(1));
        setShiftTime(target, 2, shiftTimeMap.get(2));
        setShiftTime(target, 3, shiftTimeMap.get(3));
        setShiftTime(target, 4, shiftTimeMap.get(4));
        setShiftTime(target, 5, shiftTimeMap.get(5));
        setShiftTime(target, 6, shiftTimeMap.get(6));
        setShiftTime(target, 7, shiftTimeMap.get(7));
        setShiftTime(target, 8, shiftTimeMap.get(8));
    }

    private void setShiftTime(LhScheduleResult target, int shift, Date[] range) {
        Date start = Objects.nonNull(range) ? range[0] : null;
        Date end = Objects.nonNull(range) ? range[1] : null;
        switch (shift) {
            case 1:
                target.setClass1StartTime(start);
                target.setClass1EndTime(end);
                break;
            case 2:
                target.setClass2StartTime(start);
                target.setClass2EndTime(end);
                break;
            case 3:
                target.setClass3StartTime(start);
                target.setClass3EndTime(end);
                break;
            case 4:
                target.setClass4StartTime(start);
                target.setClass4EndTime(end);
                break;
            case 5:
                target.setClass5StartTime(start);
                target.setClass5EndTime(end);
                break;
            case 6:
                target.setClass6StartTime(start);
                target.setClass6EndTime(end);
                break;
            case 7:
                target.setClass7StartTime(start);
                target.setClass7EndTime(end);
                break;
            case 8:
                target.setClass8StartTime(start);
                target.setClass8EndTime(end);
                break;
            default:
                break;
        }
    }
    /**
     * 构建模板表头数据
     *
     * @param list 排程结果列表
     * @param scheduleDate 排程日期
     * @return 模板表头数据
     */
    private Map<String, Object> buildExportTableMap(List<LhScheduleResult> list, Date scheduleDate) {
        Map<String, Object> tableMap = new HashMap<>(16);
        Date exportScheduleDate = Objects.nonNull(scheduleDate) ? scheduleDate : list.stream()
                .map(LhScheduleResult::getScheduleDate)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        List<LhScheduleShiftDateVO> shiftDateList = listScheduleShiftDates(exportScheduleDate);
        for (int i = 0; i < LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; i++) {
            String shiftDate = i < shiftDateList.size() ? shiftDateList.get(i).getShiftDate() : "";
            tableMap.put("shiftDate" + (i + 1), shiftDate);
        }
        tableMap.put("yearmonthday", DateUtil.format(exportScheduleDate, "yyyy年MM月dd日"));
        tableMap.put("productionVersion", PubUtil.isNotEmpty(list) ? list.get(0).getProductionVersion() : "");
        tableMap.put("batchNo", PubUtil.isNotEmpty(list) ? list.get(0).getBatchNo() : "");
        return tableMap;
    }

    /**
     * 构建模板列表数据
     *
     * @param list 排程结果列表
     * @return 模板列表数据
     */
    private List<Map<String, Object>> buildExportDataList(List<LhScheduleResult> list) {
        List<Map<String, Object>> dataList = new ArrayList<>(list.size() + 1);
        if(PubUtil.isNotEmpty(list)){
            dataList.add(buildSummaryRow(list));
        }
        for (LhScheduleResult result : list) {
            Map<String, Object> row = new HashMap<>(96);
            row.put("lhMachineCode", result.getLhMachineCode());
            row.put("materialCode", result.getMaterialCode());
            row.put("materialDesc", result.getMaterialDesc());
            row.put("mainMaterialDesc", result.getMainMaterialDesc());
            row.put("scheduleType", buildScheduleTypeName(result.getScheduleType()));
            row.put("mouldSurplusQty", result.getMouldSurplusQty());
            row.put("embryoStock", result.getEmbryoStock());
            row.put("singleMouldShiftQty", result.getSingleMouldShiftQty());
            row.put("leftRightMould", result.getLeftRightMould());
            row.put("mouldMethod", result.getMouldMethod());
            row.put("totalFinishQty", sumFinishQty(result));
            row.put("dailyPlanQty", result.getDailyPlanQty());
            row.put("totalPlanQty", result.getDailyPlanQty());
            row.put("remark", result.getRemark());
            for (int shift = 1; shift <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shift++) {
                row.put("class" + shift + "Order", buildShiftOrder(result, shift));
                row.put("class" + shift + "PlanQty", getClassPlanQty(result, shift));
                row.put("class" + shift + "FinishQty", getClassFinishQty(result, shift));
                row.put("class" + shift + "Type", buildShiftType(result, shift));
                row.put("class" + shift + "Analysis", getClassAnalysis(result, shift));
                row.put("class" + shift + "Dot", "");
            }
            dataList.add(row);
        }
        return dataList;
    }

    /**
     * 构建排程类型名称
     *
     * @param scheduleType 排程类型编码
     * @return 排程类型名称
     */
    private String buildScheduleTypeName(String scheduleType) {
        if ("01".equals(scheduleType)) {
            return "续作";
        }
        if ("02".equals(scheduleType)) {
            return "新增";
        }
        return scheduleType;
    }

    /**
     * 构建导出首行汇总数据
     *
     * @param list 排程结果列表
     * @return 汇总行数据
     */
    private Map<String, Object> buildSummaryRow(List<LhScheduleResult> list) {
        Map<String, Object> row = new HashMap<>(96);
        int totalPlanQty = 0;
        for (int shift = 1; shift <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shift++) {
            int classPlanQty = sumPlanQtyByShift(list, shift);
            int classFinishQty = sumFinishQtyByShift(list, shift);
            row.put("class" + shift + "PlanQty", classPlanQty);
            row.put("class" + shift + "FinishQty", classFinishQty);
            totalPlanQty += classPlanQty;
        }
        row.put("dailyPlanQty", sumPlanQtyByShift(list, 6)
                + sumPlanQtyByShift(list, 7)
                + sumPlanQtyByShift(list, 8));
        row.put("totalFinishQty", sumFinishQtyByShift(list, 6)
                + sumFinishQtyByShift(list, 7)
                + sumFinishQtyByShift(list, 8));
        row.put("totalPlanQty", totalPlanQty);
        return row;
    }

    /**
     * 汇总指定班次计划量
     *
     * @param list 排程结果列表
     * @param shift 班次序号
     * @return 计划量合计
     */
    private int sumPlanQtyByShift(List<LhScheduleResult> list, int shift) {
        return list.stream()
                .map(result -> getClassPlanQty(result, shift))
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    /**
     * 汇总指定班次完成量
     *
     * @param list 排程结果列表
     * @param shift 班次序号
     * @return 完成量合计
     */
    private int sumFinishQtyByShift(List<LhScheduleResult> list, int shift) {
        return list.stream()
                .map(result -> getClassFinishQty(result, shift))
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    /**
     * 构建班次顺序
     *
     * @param result 排程结果
     * @param shift  班次序号
     * @return 班次顺序
     */
    private Object buildShiftOrder(LhScheduleResult result, int shift) {
        Integer planQty = getClassPlanQty(result, shift);
        return Objects.nonNull(planQty) && planQty > 0 ? result.getScheduleOrder() : "";
    }

    /**
     * 构建班次类型
     *
     * @param result 排程结果
     * @param shift  班次序号
     * @return 班次类型
     */
    private String buildShiftType(LhScheduleResult result, int shift) {
        Integer planQty = getClassPlanQty(result, shift);
        if (Objects.isNull(planQty) || planQty <= 0) {
            return "";
        }
        if ("1".equals(result.getIsFirst())) {
            return "首检";
        }
        if ("01".equals(result.getConstructionStage())) {
            return "试验";
        }
        if ("02".equals(result.getConstructionStage())) {
            return "量试";
        }
        if ("1".equals(result.getIsEnd())) {
            return "收尾";
        }
        return "正常";
    }

    /**
     * 汇总实际完成量
     *
     * @param result 排程结果
     * @return 实际完成量合计
     */
    private int sumFinishQty(LhScheduleResult result) {
        int total = 0;
        for (int shift = 1; shift <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shift++) {
            Integer finishQty = getClassFinishQty(result, shift);
            total += Objects.isNull(finishQty) ? 0 : finishQty;
        }
        return total;
    }

    /**
     * 获取指定班次计划量
     *
     * @param result 排程结果
     * @param shift  班次序号
     * @return 计划量
     */
    private Integer getClassPlanQty(LhScheduleResult result, int shift) {
        switch (shift) {
            case 1:
                return result.getClass1PlanQty();
            case 2:
                return result.getClass2PlanQty();
            case 3:
                return result.getClass3PlanQty();
            case 4:
                return result.getClass4PlanQty();
            case 5:
                return result.getClass5PlanQty();
            case 6:
                return result.getClass6PlanQty();
            case 7:
                return result.getClass7PlanQty();
            case 8:
                return result.getClass8PlanQty();
            default:
                return null;
        }
    }

    /**
     * 获取指定班次完成量
     *
     * @param result 排程结果
     * @param shift  班次序号
     * @return 完成量
     */
    private Integer getClassFinishQty(LhScheduleResult result, int shift) {
        switch (shift) {
            case 1:
                return result.getClass1FinishQty();
            case 2:
                return result.getClass2FinishQty();
            case 3:
                return result.getClass3FinishQty();
            case 4:
                return result.getClass4FinishQty();
            case 5:
                return result.getClass5FinishQty();
            case 6:
                return result.getClass6FinishQty();
            case 7:
                return result.getClass7FinishQty();
            case 8:
                return result.getClass8FinishQty();
            default:
                return null;
        }
    }

    /**
     * 获取指定班次原因分析
     *
     * @param result 排程结果
     * @param shift  班次序号
     * @return 原因分析
     */
    private String getClassAnalysis(LhScheduleResult result, int shift) {
        switch (shift) {
            case 1:
                return result.getClass1Analysis();
            case 2:
                return result.getClass2Analysis();
            case 3:
                return result.getClass3Analysis();
            case 4:
                return result.getClass4Analysis();
            case 5:
                return result.getClass5Analysis();
            case 6:
                return result.getClass6Analysis();
            case 7:
                return result.getClass7Analysis();
            case 8:
                return result.getClass8Analysis();
            default:
                return null;
        }
    }

}

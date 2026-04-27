package com.zlt.aps.lh.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.domain.dto.*;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhScheduleShiftDateVO;
import com.zlt.aps.lh.api.enums.DeleteFlagEnum;
import com.zlt.aps.lh.api.enums.FactoryCodeEnum;
import com.zlt.aps.lh.api.enums.ReleaseStatusEnum;
import com.zlt.aps.lh.component.LhScheduleConfigResolver;
import com.zlt.aps.common.core.utils.ExcelUtils;
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
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import com.ruoyi.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.InputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        log.info("接收排程请求, 工厂: {}, 日期: {}",
                request.getFactoryCode(), LhScheduleTimeUtil.formatDate(request.getScheduleDate()));
        LhScheduleContext context = buildContext(request);
        String lockToken = null;
        try {
            lockToken = scheduleExecutionGuard.acquire(context.getFactoryCode(), context.getScheduleTargetDate());
            return scheduleExecutor.execute(context);
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
        dataList.add(buildSummaryRow(list));
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

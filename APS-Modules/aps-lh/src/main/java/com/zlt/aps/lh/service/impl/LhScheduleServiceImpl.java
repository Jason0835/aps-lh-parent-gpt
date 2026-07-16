package com.zlt.aps.lh.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.ExcelStyleVo;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.service.ICxScheduleResultService;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.domain.dto.*;
import com.zlt.aps.lh.api.domain.entity.*;
import com.zlt.aps.lh.api.domain.vo.LhMouldChangePlanVo;
import com.zlt.aps.lh.api.domain.vo.LhScheduleResultTemplateImportVO;
import com.zlt.aps.lh.api.domain.vo.LhScheduleShiftDateVO;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.DeleteFlagEnum;
import com.zlt.aps.lh.api.enums.FactoryCodeEnum;
import com.zlt.aps.lh.api.enums.ReleaseStatusEnum;
import com.zlt.aps.lh.component.LhScheduleConfigResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.controller.LhMouldChangePlanController;
import com.zlt.aps.lh.engine.decorator.IScheduleExecutor;
import com.zlt.aps.lh.engine.observer.ScheduleEvent;
import com.zlt.aps.lh.engine.observer.ScheduleEventPublisher;
import com.zlt.aps.lh.exception.ScheduleException;
import com.zlt.aps.lh.mapper.*;
import com.zlt.aps.lh.service.ILhScheduleResultService;
import com.zlt.aps.lh.service.ILhScheduleService;
import com.zlt.aps.lh.util.*;
import com.zlt.aps.mdm.api.domain.entity.*;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.utils.AppUtils;
import com.zlt.aps.utils.BeanCopyUtils;
import com.zlt.aps.utils.ImportExcelValidatedUtils;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 硫化排程主服务实现。
 *
 * <p>主要职责：</p>
 * <ul>
 *   <li>接收控制器传入的排程请求，构建本次排程上下文；</li>
 *   <li>解析并固化本次排程参数快照，保证一次排程内规则口径稳定；</li>
 *   <li>同工厂同目标日的并发排程由 Controller 层 {@link com.zlt.aps.redissonLock.annotation.DistributedLock} 注解控制；</li>
 *   <li>委托 {@link IScheduleExecutor} 进入模板链路，执行基础数据初始化、SKU归集、续作、新增和结果校验保存；</li>
 *   <li>按批次号发布已保存的排程结果并触发发布事件。</li>
 * </ul>
 *
 * <p>该类位于整体流程的服务入口层，只做流程编排和边界控制，不直接实现 SKU 排序、机台匹配、
 * 班次排量、换模或换活字块算法。</p>
 *
 * @author APS
 */
@Slf4j
@Service
public class LhScheduleServiceImpl extends AbstractDocService<LhScheduleResult> implements ILhScheduleService {

    /**
     * 硫化排程导入模板首条明细所在 Excel 行号。
     */
    private static final int LH_SCHEDULE_IMPORT_DATA_START_ROW = 6;

    @Resource
    private IScheduleExecutor scheduleExecutor;

    @Resource
    private LhScheduleConfigResolver scheduleConfigResolver;

    @Resource
    private LhScheduleResultMapper scheduleResultMapper;

    @Resource
    private LhRepairCapsuleMapper lhRepairCapsuleMapper;

    @Resource
    private LhDayFinishQtyMapper lhDayFinishQtyMapper;

    @Resource
    private LhScheFinishQtyMapper lhScheFinishQtyMapper;

    @Resource
    private LhMouldChangePlanEntityMapper lhMouldChangePlanMapper;

    @Resource
    private ICxScheduleResultService cxScheduleResultService;

    /**
     * 成型排程结果Mapper（aps-lh模块内直接查询，避免Feign反序列化List失败）
     */
    @Resource
    private CxScheduleResultMapper cxScheduleResultMapper;

    @Resource
    private ScheduleEventPublisher scheduleEventPublisher;

    @Resource
    private LhTextMouldChangePlanGenerator textMouldChangePlanGenerator;

    @Resource
    private LhIncreaseMouldStartPlanService increaseMouldStartPlanService;

    @Autowired
    private LhMouldChangePlanController lhMouldChangePlanController;

    @Autowired
    private ISysDictDataCacheService sysDictDataCacheService;

    @Resource
    private ILhScheduleResultService lhScheduleResultService;

    @Resource
    private MdmSkuMouldRelMapper mdmSkuMouldRelMapper;


    @Resource
    private MdmModelInfoMapper mdmModelInfoMapper;

    @Resource
    private LhMachineInfoMapper lhMachineInfoMapper;

    @Resource
    private MpFactoryProductionVersionMapper mpFactoryProductionVersionMapper;

    @Resource
    private FactoryMonthPlanProductionFinalResultMapper monthPlanMapper;

    @Resource
    private MdmSkuConstructionRefMapper mdmSkuConstructionRefMapper;

    @Resource
    private MdmSkuLhCapacityMapper mdmSkuLhCapacityMapper;

    @Resource
    private MdmWorkCalendarMapper mdmWorkCalendarMapper;

    @Override
    public LhScheduleResponseDTO executeSchedule(LhScheduleRequestDTO request) {
        log.info("接收排程请求, 工厂: {}, 日期: {}, 月计划版本: {}, 生产版本: {}",
                request.getFactoryCode(), LhScheduleTimeUtil.formatDate(request.getScheduleDate()),
                request.getMonthPlanVersion(), request.getProductionVersion());
        LhScheduleContext context = buildContext(request);
        try {
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
            return LhScheduleResponseDTO.fail(context.getBatchNo(),
                    I18nUtil.getMessage("ui.data.column.lhScheduleResult.scheduleExecuteError") + e.getMessage());
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
                return LhScheduleResponseDTO.fail(batchNo,
                        String.format(I18nUtil.getMessage("ui.data.column.lhScheduleResult.batchNoResultNotFound"), batchNo));
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
            return LhScheduleResponseDTO.success(batchNo,
                    String.format(I18nUtil.getMessage("ui.data.column.lhScheduleResult.publishSuccess"), results.size()));

        } catch (Exception e) {
            log.error("发布排程结果异常, 批次号: {}", batchNo, e);
            return LhScheduleResponseDTO.fail(batchNo,
                    I18nUtil.getMessage("ui.data.column.lhScheduleResult.publishFailed") + e.getMessage());
        }
    }

    /**
     * 构建排程上下文。
     *
     * <p>处理流程：</p>
     * <ol>
     *   <li>写入工厂、操作人、月计划版本等请求参数；</li>
     *   <li>将请求日期标准化为排程目标日；</li>
     *   <li>解析硫化参数形成 {@code LhScheduleConfig} 快照；</li>
     *   <li>根据 {@code SCHEDULE_DAYS} 反推出窗口起点 T 日，供班次、日计划 dayN 和基础数据加载使用。</li>
     * </ol>
     *
     * <p>该方法会修改并返回新建的 {@link LhScheduleContext}，不访问排程结果表，也不触发算法计算。</p>
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
        int offsetDays = Math.max(0, scheduleDays - 2);
        // 引擎使用 T 日 = 目标日 − (连续排程日历跨度 − 2)，确保窗口仍为 T～T+2
        context.setScheduleDate(LhScheduleTimeUtil.addDays(target, -offsetDays));
        // 排程窗口结束日期 = T + 2，用于产能计算、加机台、收尾等核心逻辑
        context.setWindowEndDate(LhScheduleTimeUtil.addDays(context.getScheduleDate(), scheduleDays - 1));
        log.info("排程上下文构建完成, 工厂: {}, 工厂名称: {}, 目标日(业务): {}, T日: {}, 窗口结束日: {}, 排程天数: {}, 强制重排: {}, 局部搜索: {}, 定点机台规则: {}",
                context.getFactoryCode(), context.getFactoryDisplayName(),
                LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate()),
                LhScheduleTimeUtil.formatDate(context.getScheduleDate()),
                LhScheduleTimeUtil.formatDate(context.getWindowEndDate()),
                scheduleDays,
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
        // 前端传递 T+1 日期，向前偏移一天使排程窗口覆盖 T、T+1、T+2
        Date end = DateUtil.beginOfDay(DateUtil.offsetDay(scheduleDate, 1));
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
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhScheduleResult.changeMachine.selectRequired"));
        }
        if (dto.getLhMachineCode() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhScheduleResult.changeMachine.newMachineCodeRequired"));
        }

        Long ids = dto.getId();
        String newMachineCode = dto.getLhMachineCode();
        List<LhScheduleResult> existResultList = scheduleResultMapper.changeMachinePreCheck(Collections.singletonList(ids), newMachineCode);

        List<String> errorMessages = new ArrayList<>();
        for (LhScheduleResult existResult : existResultList) {
            String existScheduleDate = DateUtil.format(existResult.getScheduleDate(), "yyyy-MM-dd");
            String existMachineCode = existResult.getLhMachineCode();
            String existMaterialCode = existResult.getMaterialCode();
            errorMessages.add(MessageFormat.format(I18nUtil.getMessage("ui.data.alert.lhScheduleResult.changeMachine.conflictExists"), existScheduleDate, existMaterialCode, existMachineCode));
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
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhScheduleResult.changeMachine.selectRequired"));
        }
        if (dto.getLhMachineCode() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhScheduleResult.changeMachine.newMachineCodeRequired"));
        }

        // 检查所有记录是否已发布
        LhScheduleResult record = scheduleResultMapper.selectById(dto.getId());

        // 更新机台信息
        String oldMachine = record.getLhMachineCode();
        record.setLhMachineCode(dto.getLhMachineCode());
        record.setLhMachineName(dto.getLhMachineName());

        // 更新备注
        String remark = record.getRemark() != null ? record.getRemark() : "";
        record.setRemark(remark + MessageFormat.format(I18nUtil.getMessage("ui.data.alert.lhScheduleResult.changeMachine.remarkTemplate"), DateUtil.now(), oldMachine, dto.getLhMachineCode()));

        // 发布状态是已发布的话，需要更新为待发布
        if (ReleaseStatusEnum.RELEASED.getCode().equals(record.getIsRelease())) {
            record.setIsRelease(ReleaseStatusEnum.PENDING_RELEASE.getCode());
        }

        scheduleResultMapper.updateById(record);
        return AjaxResult.success(I18nUtil.getMessage("ui.data.alert.lhScheduleResult.changeMachine.success"));
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
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhScheduleResult.adjustQuantity.selectRequired"));
        }

        LhScheduleResult record = scheduleResultMapper.selectById(dto.getId());
        if (Objects.isNull(record)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhScheduleResult.adjustQuantity.recordNotFound"));
        }

        Date now = new Date();
        LhScheduleContext shiftContext = buildAdjustQuantityShiftContext(record);
        boolean hasAdjustField = false;
        List<String> errorMessages = new ArrayList<>();
        for (int shiftIndex = 1; shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            Integer adjustPlanQty = getAdjustPlanQty(dto, shiftIndex);
            if (Objects.isNull(adjustPlanQty)) {
                continue;
            }
            hasAdjustField = true;
            if (adjustPlanQty < 0) {
                errorMessages.add(MessageFormat.format(I18nUtil.getMessage("ui.data.alert.lhScheduleResult.adjustQuantity.planQtyNegative"), shiftIndex));
            }
            // 历史班次允许调量低于完成量，非历史班次仍需保护完成量下限。
            boolean historyShift = isHistoryShift(record, shiftIndex, now, shiftContext);
            Integer finishQty = Optional.ofNullable(ShiftFieldUtil.getShiftFinishQty(record, shiftIndex))
                    .orElse(0);
            if (!historyShift && adjustPlanQty < finishQty) {
                errorMessages.add(MessageFormat.format(I18nUtil.getMessage("ui.data.alert.lhScheduleResult.adjustQuantity.planQtyLessThanFinish"), shiftIndex, finishQty));
            }
        }

        if (!hasAdjustField) {
            errorMessages.add(I18nUtil.getMessage("ui.data.alert.lhScheduleResult.adjustQuantity.noAdjustableField"));
        }
        if (CollUtil.isNotEmpty(errorMessages)) {
            return AjaxResult.error(String.join("；", errorMessages));
        }
        return AjaxResult.success();
    }

    /**
     * 判断指定班次是否已经成为历史班次。
     *
     * @param record     排程结果记录
     * @param shiftIndex 班次索引（1~8）
     * @param now        当前校验时间
     * @param context    轻量排程上下文
     * @return true表示班次结束时间已到或已过，false表示无法推导结束时间或班次尚未结束
     */
    private boolean isHistoryShift(LhScheduleResult record, int shiftIndex, Date now, LhScheduleContext context) {
        Date shiftEndTime = resolveAdjustQuantityShiftEndTime(record, shiftIndex, context);
        if (Objects.isNull(shiftEndTime) || Objects.isNull(now)) {
            return false;
        }
        return !now.before(shiftEndTime);
    }

    /**
     * 获取调量校验使用的班次结束时间。
     * <p>优先使用排程结果中已有的班次结束时间；历史数据缺失时，根据排程目标日、硫化参数和默认班次窗口推导结束时间。</p>
     *
     * @param record     排程结果记录
     * @param shiftIndex 班次索引（1~8）
     * @param context    轻量排程上下文
     * @return 班次结束时间，无法获取或推导时返回 null
     */
    private Date resolveAdjustQuantityShiftEndTime(LhScheduleResult record, int shiftIndex, LhScheduleContext context) {
        if (Objects.isNull(record) || shiftIndex < 1 || shiftIndex > LhScheduleConstant.MAX_SHIFT_SLOT_COUNT) {
            return null;
        }
        Date shiftEndTime = ShiftFieldUtil.getShiftEndTime(record, shiftIndex);
        if (Objects.nonNull(shiftEndTime)) {
            return shiftEndTime;
        }
        if (Objects.isNull(record.getScheduleDate())) {
            return null;
        }
        Date scheduleBaseDate = resolveScheduleBaseDate(record.getScheduleDate(), context);
        LhShiftConfigVO shift = LhScheduleTimeUtil.getShiftByIndex(context, scheduleBaseDate, shiftIndex);
        return Objects.nonNull(shift) ? shift.getShiftEndDateTime() : null;
    }

    /**
     * 构建调量历史班次判断所需的轻量排程上下文。
     * <p>仅解析硫化参数配置，用于让班次开始时间、结束时间与当前工厂参数保持一致。</p>
     *
     * @param record 排程结果记录
     * @return 轻量排程上下文
     */
    private LhScheduleContext buildAdjustQuantityShiftContext(LhScheduleResult record) {
        LhScheduleContext context = new LhScheduleContext();
        context.setFactoryCode(record.getFactoryCode());
        if (StringUtils.isNotEmpty(record.getFactoryCode())) {
            scheduleConfigResolver.resolveAndAttach(context);
        }
        return context;
    }

    /**
     * 根据排程目标日反推班次窗口起点 T 日。
     *
     * @param scheduleTargetDate 排程目标日
     * @param context            轻量排程上下文
     * @return 班次窗口起点 T 日
     */
    private Date resolveScheduleBaseDate(Date scheduleTargetDate, LhScheduleContext context) {
        int scheduleDays = LhScheduleTimeUtil.getScheduleDays(context);
        int offsetDays = Math.max(0, scheduleDays - 1);
        return DateUtil.offsetDay(DateUtil.beginOfDay(scheduleTargetDate), -offsetDays);
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
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhScheduleResult.adjustQuantity.recordNotFound"));
        }

        for (int shiftIndex = 1; shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            Integer adjustPlanQty = getAdjustPlanQty(dto, shiftIndex);
            if (Objects.nonNull(adjustPlanQty)) {
                setAdjustPlanQty(record, shiftIndex, adjustPlanQty);
                setAdjustAnalysis(record, shiftIndex, getAdjustAnalysis(dto, shiftIndex));
                if (adjustPlanQty == 0) {
                    ShiftFieldUtil.clearShiftPlanAuxFields(record, shiftIndex);
                }
            }
        }

        // 调量后同步汇总计划量，并回置为未发布状态，确保后续重新发布
        ShiftFieldUtil.syncDailyPlanQty(record);
        // 发布状态不是未发布的话，需要更新为待发布
        if (!ReleaseStatusEnum.NOT_RELEASED.getCode().equals(record.getIsRelease())) {
            record.setIsRelease(ReleaseStatusEnum.PENDING_RELEASE.getCode());
        }

        int updateCount = scheduleResultMapper.updateById(record);
        if (updateCount <= 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhScheduleResult.adjustQuantity.failRetry"));
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.data.alert.lhScheduleResult.adjustQuantity.success"));
    }

    /**
     * 根据单条排程结果文字示方更新。
     *
     * @param dto 生成入参
     * @return 处理结果
     */
    @Override
    public AjaxResult generateTextMouldChangePlan(LhGenerateTextMouldPlanDTO dto) {
        return textMouldChangePlanGenerator.generate(dto);
    }

    /**
     * 计划更新。
     *
     * @param scheduleResult 当前硫化排程结果
     * @return 处理结果
     */
    @Override
    public AjaxResult increaseMouldStartPlan(LhScheduleResult scheduleResult) {
        return increaseMouldStartPlanService.increase(scheduleResult);
    }

    /**
     * 批量计划更新。
     *
     * @param scheduleResultList 硫化排程结果列表
     * @return 处理结果
     */
    @Override
    public AjaxResult batchIncreaseMouldStartPlan(List<LhScheduleResult> scheduleResultList) {
        return increaseMouldStartPlanService.batchIncrease(scheduleResultList);
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
     * @return 导出数据
     */
    @Override
    public byte[] exportData(List<LhScheduleResult> list, LhScheduleResult result) {
        // 节点1：读取固定导出模板。硫化计划导出不再走多语言表头加载，
        // 表头、合并单元格、下拉框、公式位置都由 lhjhtemplate.xlsx 统一维护。
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("excelModel/lhjhtemplate.xlsx");
        if (Objects.isNull(inputStream)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.column.lhScheduleResult.exportTemplateNotFound"));
        }

        // 节点1.5：在 writeMultiList 处理模板之前，先扫描模板占位符行，
        // 建立 占位符名称→列索引 的映射，供后续 fillExportSummaryFormulas 动态定位列位置，
        // 避免因模板列增删（如新增胎胚编码列、调整备注列位置）导致硬编码列索引失效。
        byte[] templateBytes;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            templateBytes = baos.toByteArray();
        } catch (IOException e) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.column.lhScheduleResult.exportTemplateReadFailed"));
        }
        Map<String, Integer> placeholderMap = ExcelUtils.scanTemplateRowPlaceholders(
                new ByteArrayInputStream(templateBytes), 0);

        // 节点2：兼容查询结果为空的场景，后续表头仍可根据 scheduleDate 回填日期，
        // 明细区域则保持为空，避免空指针影响模板导出。
        List<LhScheduleResult> exportList = Objects.isNull(list) ? Collections.emptyList() : list;

        // 节点3：模板表头数据只负责替换普通占位符，例如排程日期、版本、批次号、
        // 以及 8 个班次标题里的 shiftDate1 ~ shiftDate8。
        Map<String, Object> tableMap = buildExportTableMap(exportList, result.getScheduleDate());

        // 换模计划同时用于硫化计划的一班顺序和换模计划Sheet导出，查询一次后复用。
        LhMouldChangePlan mouldChangePlan = BeanCopyUtils.copyBean(result, LhMouldChangePlan.class);
        QueryWrapper<LhMouldChangePlan> wrapper = new QueryWrapper<>();
        lhMouldChangePlanController.builderCondition(wrapper, mouldChangePlan);
        List<LhMouldChangePlan> mouldChangePlanList = lhMouldChangePlanMapper.selectList(wrapper);

        // 节点4：模板第 7 行为 {.xxx} 明细模板行，writeMultiList 会从该行开始复制填充。
        // 当前只有一个明细列表，因此只放入一个 List<Map<String,Object>>。
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        List<Map<String, Object>> exportDataList = buildExportDataList(
                exportList, result.getScheduleDate(), mouldChangePlanList);
        excelDataList.add(exportDataList);

        // 节点5：硫化计划数据位于模板第 0 个 sheet（下标 0）的“硫化计划”页。
        byte[] exportBytes = ExcelUtils.writeMultiList(new ByteArrayInputStream(templateBytes), 0, tableMap, excelDataList);

        //1.获取导出数据
        AppUtils.formatData(mouldChangePlanList, lhMouldChangePlanController.getQueryFormulas());
        List<LhMouldChangePlanVo> mouldChangePlanExportList = lhMouldChangePlanController.buildLhMouldChangePlanVoList(mouldChangePlanList, mouldChangePlan);

        Map<String, Object> mouldChangePlanTableMap = lhMouldChangePlanController.buildExportTableMap(result.getScheduleDate());
        lhMouldChangePlanController.setExportTitleFieldName(mouldChangePlanTableMap);
        List<List<Map<String, Object>>> mouldChangePlanExcelDataList = new ArrayList<>();
        mouldChangePlanExcelDataList.add(lhMouldChangePlanController.buildExportDataList(mouldChangePlanExportList, mouldChangePlan));

        inputStream = new ByteArrayInputStream(exportBytes);
        exportBytes = ExcelUtils.writeMultiList(inputStream, 1, mouldChangePlanTableMap, mouldChangePlanExcelDataList);

        // 排产小结已迁移至成型日计划导出（aps-cx 通过 Feign 调用 buildScheduleSummaryExportData），
        // 硫化日计划导出不再写入排产小结 sheet。
        return fillExportSummaryFormulas(exportBytes, exportDataList.size(), placeholderMap);
    }

    /**
     * 下载导入模板（不含物料描述列）
     * <p>物料描述通过物料编码自动带出，导入时用户无需填写物料描述</p>
     * <p>模板下载只填充表头占位符（日期、班次等），不查询数据库中的业务数据，
     * 确保2个Sheet（硫化计划、硫化换模计划）均为空模板。</p>
     * <p>排产小结已迁移至成型日计划导出，不再包含在硫化导入模板中。</p>
     *
     * @param result 查询条件（工厂、排程日期等）
     * @return 导入模板Excel字节数组
     */
    @Override
    public byte[] downloadImportTemplate(LhScheduleResult result) {
        // 读取固定导出模板
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("excelModel/lhjhtemplate.xlsx");
        if (Objects.isNull(inputStream)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.column.lhScheduleResult.exportTemplateNotFound"));
        }
        byte[] templateBytes;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            templateBytes = baos.toByteArray();
        } catch (IOException e) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.column.lhScheduleResult.exportTemplateReadFailed"));
        }

        // Sheet 0 - 硫化计划：只填充表头占位符，明细数据传空列表
        Map<String, Object> scheduleTableMap = buildExportTableMap(Collections.emptyList(), result.getScheduleDate());
        List<List<Map<String, Object>>> scheduleDataList = new ArrayList<>();
        scheduleDataList.add(new ArrayList<>());
        byte[] exportBytes = ExcelUtils.writeMultiList(new ByteArrayInputStream(templateBytes), 0, scheduleTableMap, scheduleDataList);

        // Sheet 1 - 硫化换模计划：只填充表头占位符，不查询数据库，明细数据传空列表
        Map<String, Object> mouldChangePlanTableMap = new HashMap<>();
        lhMouldChangePlanController.setExportTitleFieldName(mouldChangePlanTableMap);
        List<List<Map<String, Object>>> mouldChangePlanDataList = new ArrayList<>();
        mouldChangePlanDataList.add(new ArrayList<>());
        exportBytes = ExcelUtils.writeMultiList(new ByteArrayInputStream(exportBytes), 1, mouldChangePlanTableMap, mouldChangePlanDataList);

        // 排产小结已迁移至成型日计划导出，硫化导入模板不再包含排产小结 sheet。

        // 重命名 Sheet 0（硫化计划）和 Sheet 1（硫化换模计划）为国际化名称
        // 使导入时能按 i18n sheetName 匹配模板中的工作表
        exportBytes = renameImportSheets(exportBytes);

        // 移除物料描述列
        return removeMaterialDescColumn(exportBytes);
    }

    /**
     * 从Excel中移除物料描述列
     * <p>扫描模板占位符行定位materialDesc列索引，然后删除该列的所有单元格</p>
     *
     * @param excelBytes 原始Excel字节数组
     * @return 移除物料描述列后的Excel字节数组
     */
    private byte[] removeMaterialDescColumn(byte[] excelBytes) {
        if (Objects.isNull(excelBytes) || excelBytes.length == 0) {
            return excelBytes;
        }
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(excelBytes);
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> placeholderMap = ExcelUtils.scanTemplateRowPlaceholders(
                    new ByteArrayInputStream(excelBytes), 0);
            Integer materialDescCol = placeholderMap.get("materialDesc");
            if (Objects.isNull(materialDescCol) || materialDescCol < 0) {
                log.warn("未在模板中找到materialDesc列，跳过移除操作");
                return excelBytes;
            }
            log.info("开始移除导入模板中的物料描述列，列索引: {}", materialDescCol);
            for (Row row : sheet) {
                Cell cell = row.getCell(materialDescCol);
                if (Objects.nonNull(cell)) {
                    row.removeCell(cell);
                }
                shiftCellsLeft(row, materialDescCol);
            }
            workbook.write(outputStream);
            log.info("成功移除导入模板中的物料描述列");
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("移除物料描述列失败", e);
            throw new ServiceException("生成导入模板失败");
        }
    }

    /**
     * 将指定列索引右侧的所有单元格左移一位
     *
     * @param row      当前行
     * @param colIndex 被删除列的索引
     */
    private void shiftCellsLeft(Row row, int colIndex) {
        int lastCellNum = row.getLastCellNum();
        for (int col = colIndex + 1; col < lastCellNum; col++) {
            Cell sourceCell = row.getCell(col);
            Cell targetCell = row.getCell(col - 1);
            if (Objects.nonNull(sourceCell)) {
                if (Objects.isNull(targetCell)) {
                    targetCell = row.createCell(col - 1, sourceCell.getCellType());
                }
                copyCellValue(sourceCell, targetCell);
                row.removeCell(sourceCell);
            } else if (Objects.nonNull(targetCell)) {
                row.removeCell(targetCell);
            }
        }
    }

    /**
     * 复制源单元格的值和样式到目标单元格
     *
     * @param source 源单元格
     * @param target 目标单元格
     */
    private void copyCellValue(Cell source, Cell target) {
        CellStyle style = source.getCellStyle();
        if (Objects.nonNull(style)) {
            target.setCellStyle(style);
        }
        CellType cellType = source.getCellType();
        if (CellType.FORMULA.equals(cellType)) {
            target.setCellFormula(source.getCellFormula());
            return;
        }
        if (CellType.STRING.equals(cellType)) {
            target.setCellValue(source.getRichStringCellValue());
            return;
        }
        if (CellType.NUMERIC.equals(cellType)) {
            target.setCellValue(source.getNumericCellValue());
            return;
        }
        if (CellType.BOOLEAN.equals(cellType)) {
            target.setCellValue(source.getBooleanCellValue());
            return;
        }
        if (CellType.BLANK.equals(cellType)) {
            target.setBlank();
        }
    }

    /**
     * 将 Sheet 0（硫化计划）和 Sheet 1（硫化换模计划）重命名为国际化名称，
     * 使导入时能按 i18n sheetName 匹配模板中的工作表。
     */
    private byte[] renameImportSheets(byte[] excelBytes) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(excelBytes);
             XSSFWorkbook workbook = new XSSFWorkbook(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            workbook.setSheetName(0, I18nUtil.getMessage("ui.data.column.scheduleResult.exportSheetName"));
            workbook.setSheetName(1, I18nUtil.getMessage("ui.data.column.lhMouldChangePlan.import.modelName"));
            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("重命名导入模板Sheet失败", e);
            throw new ServiceException("生成导入模板失败");
        }
    }

    /**
     * 回填导出结果中的明细行公式。
     * <p>通用模板写入工具在复制列表行时，只会识别字符串占位符；遇到公式单元格时不会复制公式，
     * 因此 J/BZ/CA/CB 这几个模板公式在生成阶段会被清掉。这里在最终 xlsx 字节生成后重新打开工作簿，
     * 按实际数据行数逐行写回公式，保证用户下载后的文件仍然保留 Excel 公式。</p>
     * <p>所有列位置均通过 placeholderMap 动态获取，不再硬编码列索引，
     * 模板增删列后只需更新模板中的占位符，代码无需修改。</p>
     *
     * @param exportBytes    Excel 导出字节
     * @param dataRowCount   明细数据行数
     * @param placeholderMap 占位符名称→列索引（0起始）的映射，由 exportData 在模板扫描阶段生成
     * @return 已回填公式的 Excel 导出字节
     */
    private byte[] fillExportSummaryFormulas(byte[] exportBytes, int dataRowCount, Map<String, Integer> placeholderMap) {
        if (Objects.isNull(exportBytes) || exportBytes.length == 0 || dataRowCount <= 0) {
            return exportBytes;
        }

        // 从占位符映射中获取各列的动态索引，替代原先的硬编码列号。
        int dailyPlanQtyCol = placeholderMap.getOrDefault("dailyPlanQty", -1);
        int totalDailyPlanQtyCol = placeholderMap.getOrDefault("totalDailyPlanQty", -1);
        int todayNightFinishQtyCol = placeholderMap.getOrDefault("todayNightFinishQty", -1);
        // 夜班合计计划量 / 夜班合计完成量 / 总计划量公式列，模板中以特殊占位符标记
        int nightPlanQtyTotalCol = placeholderMap.getOrDefault("nightPlanQtyTotal", -1);
        int nightFinishQtyTotalCol = placeholderMap.getOrDefault("nightFinishQtyTotal", -1);
        int totalPlanQtyFormulaCol = placeholderMap.getOrDefault("totalPlanQtyFormula", -1);
        // 夜班 3 个班次（6/7/8）的计划量和完成量列索引
        int class6PlanQtyCol = placeholderMap.getOrDefault("class6PlanQty", -1);
        int class7PlanQtyCol = placeholderMap.getOrDefault("class7PlanQty", -1);
        int class8PlanQtyCol = placeholderMap.getOrDefault("class8PlanQty", -1);
        int class6FinishQtyCol = placeholderMap.getOrDefault("class6FinishQty", -1);
        int class7FinishQtyCol = placeholderMap.getOrDefault("class7FinishQty", -1);
        int class8FinishQtyCol = placeholderMap.getOrDefault("class8FinishQty", -1);
        // 占位符行在模板中的 POI 行号，writeMultiList 处理后数据行从此位置开始
        int startRowIndex = placeholderMap.getOrDefault("_templateRowIndex", 6);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(exportBytes);
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int rowIndex = startRowIndex; rowIndex < startRowIndex + dataRowCount; rowIndex++) {
                int excelRowNum = rowIndex + 1;
                Row row = sheet.getRow(rowIndex);
                if (Objects.isNull(row)) {
                    row = sheet.createRow(rowIndex);
                }

                // 日计划量列（原 J 列，现 F 列）：后续需要参与导入，不能保留公式；
                // 按模板公式逻辑 todayNightFinishQty - totalDailyPlanQty 写入静态数值。
                if (dailyPlanQtyCol >= 0 && todayNightFinishQtyCol >= 0 && totalDailyPlanQtyCol >= 0) {
                    BigDecimal dailyPlanValue = readNumericCell(row.getCell(todayNightFinishQtyCol))
                            .subtract(readNumericCell(row.getCell(totalDailyPlanQtyCol)));
                    setNumericCell(row, dailyPlanQtyCol, dailyPlanValue);
                    // 日计划量 ≤ 400：该单元格标淡橙提示产量偏低
                    if (dailyPlanValue.abs().compareTo(BigDecimal.valueOf(400)) <= 0) {
                        XSSFColor orange = new XSSFColor(new byte[]{(byte) 0xFC, (byte) 0xD5, (byte) 0xB4}, null);
                        Cell cell = row.getCell(dailyPlanQtyCol);
                        if (cell != null) {
                            XSSFCellStyle style = workbook.createCellStyle();
                            style.cloneStyleFrom(cell.getCellStyle());
                            style.setFillForegroundColor(orange);
                            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                            cell.setCellStyle(style);
                        }
                    }
                }

                // 夜班合计计划量 / 夜班合计完成量：后续同样需要导入，直接写入静态合计值。
                if (nightPlanQtyTotalCol >= 0 && class6PlanQtyCol >= 0 && class7PlanQtyCol >= 0 && class8PlanQtyCol >= 0) {
                    setNumericCell(row, nightPlanQtyTotalCol,
                            sumNumericCells(row, class6PlanQtyCol, class7PlanQtyCol, class8PlanQtyCol));
                }
                if (nightFinishQtyTotalCol >= 0 && class6FinishQtyCol >= 0 && class7FinishQtyCol >= 0 && class8FinishQtyCol >= 0) {
                    setNumericCell(row, nightFinishQtyTotalCol,
                            sumNumericCells(row, class7FinishQtyCol, class8FinishQtyCol, class6FinishQtyCol));
                }

                // 总计划量列：8 个班次 PlanQty 之和，以 Excel 公式形式写入。
                // 动态拼接公式，不再硬编码列字母，模板列变动后公式自动适配。
                if (totalPlanQtyFormulaCol >= 0) {
                    StringBuilder formulaBuilder = new StringBuilder();
                    for (int shift = 1; shift <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shift++) {
                        Integer colIndex = placeholderMap.get("class" + shift + "PlanQty");
                        if (colIndex != null && colIndex >= 0) {
                            if (formulaBuilder.length() > 0) {
                                formulaBuilder.append("+");
                            }
                            formulaBuilder.append(columnIndexToLetter(colIndex)).append(excelRowNum);
                        }
                    }
                    if (formulaBuilder.length() > 0) {
                        setFormulaCell(row, totalPlanQtyFormulaCol, formulaBuilder.toString());
                    }
                }
            }
            workbook.setForceFormulaRecalculation(true);
            sheet.setForceFormulaRecalculation(true);
            workbook.setActiveSheet(0);
            workbook.write(outputStream);
            return removeCalcChain(outputStream.toByteArray());
        } catch (Exception e) {
            throw new ServiceException("硫化计划导出公式回填失败");
        }
    }

    /**
     * 将 POI 列索引（0起始）转换为 Excel 列字母（A, B, ..., Z, AA, AB, ...）。
     *
     * @param columnIndex 列索引，0 起始
     * @return Excel 列字母
     */
    private String columnIndexToLetter(int columnIndex) {
        StringBuilder letter = new StringBuilder();
        int col = columnIndex;
        while (col >= 0) {
            letter.insert(0, (char) ('A' + col % 26));
            col = col / 26 - 1;
        }
        return letter.toString();
    }

    /**
     * 移除 xlsx 中的计算链文件及其引用。
     * <p>导出工具先基于模板生成明细，再由 POI 二次回填公式。此时模板或生成结果中残留的
     * calcChain.xml 可能仍指向旧公式单元格，Excel 打开文件时会按旧计算链校验，导致提示
     * “已删除的记录: /xl/calcChain.xml 部分的 公式(计算属性)”。删除计算链后，Excel 会根据
     * 当前单元格公式重新计算并重建计算链，避免修复提示。</p>
     *
     * @param xlsxBytes 已写入公式的 xlsx 字节
     * @return 已清理计算链的 xlsx 字节
     * @throws IOException zip 包读写异常
     */
    private byte[] removeCalcChain(byte[] xlsxBytes) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(xlsxBytes);
             ZipInputStream zipInputStream = new ZipInputStream(inputStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            ZipEntry entry;
            byte[] buffer = new byte[4096];
            while (Objects.nonNull(entry = zipInputStream.getNextEntry())) {
                String entryName = entry.getName();
                if ("xl/calcChain.xml".equals(entryName)) {
                    zipInputStream.closeEntry();
                    continue;
                }
                byte[] entryBytes = readZipEntryBytes(zipInputStream, buffer);
                if ("xl/_rels/workbook.xml.rels".equals(entryName)) {
                    entryBytes = removeCalcChainRelationship(entryBytes);
                } else if ("[Content_Types].xml".equals(entryName)) {
                    entryBytes = removeCalcChainContentType(entryBytes);
                }
                ZipEntry newEntry = new ZipEntry(entryName);
                newEntry.setTime(entry.getTime());
                zipOutputStream.putNextEntry(newEntry);
                zipOutputStream.write(entryBytes);
                zipOutputStream.closeEntry();
                zipInputStream.closeEntry();
            }
            zipOutputStream.finish();
            return outputStream.toByteArray();
        }
    }

    /**
     * 读取 zip 当前条目的完整字节。
     *
     * @param zipInputStream zip 输入流
     * @param buffer         复用缓冲区
     * @return 当前条目字节
     * @throws IOException zip 读取异常
     */
    private byte[] readZipEntryBytes(ZipInputStream zipInputStream, byte[] buffer) throws IOException {
        ByteArrayOutputStream entryOutputStream = new ByteArrayOutputStream();
        int length;
        while ((length = zipInputStream.read(buffer)) > -1) {
            entryOutputStream.write(buffer, 0, length);
        }
        return entryOutputStream.toByteArray();
    }

    /**
     * 从 workbook 关系文件中移除计算链关系。
     *
     * @param entryBytes workbook.xml.rels 字节
     * @return 移除计算链关系后的字节
     */
    private byte[] removeCalcChainRelationship(byte[] entryBytes) {
        String xml = new String(entryBytes, StandardCharsets.UTF_8);
        xml = xml.replaceAll("(?s)<Relationship\\b(?=[^>]*Type=\"http://schemas\\.openxmlformats\\.org/officeDocument/2006/relationships/calcChain\")[^>]*/>", "");
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 从内容类型文件中移除 calcChain.xml 的 Override 声明。
     *
     * @param entryBytes [Content_Types].xml 字节
     * @return 移除计算链内容类型后的字节
     */
    private byte[] removeCalcChainContentType(byte[] entryBytes) {
        String xml = new String(entryBytes, StandardCharsets.UTF_8);
        xml = xml.replaceAll("(?s)<Override\\b(?=[^>]*PartName=\"/xl/calcChain\\.xml\")[^>]*/>", "");
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 设置公式单元格。
     *
     * @param row         当前数据行
     * @param columnIndex 公式所在列下标，POI 从 0 开始计数
     * @param formula     Excel 公式内容，不包含等号
     */
    private void setFormulaCell(Row row, int columnIndex, String formula) {
        Cell cell = row.getCell(columnIndex);
        CellStyle cellStyle = Objects.nonNull(cell) ? cell.getCellStyle() : null;
        if (Objects.nonNull(cell)) {
            row.removeCell(cell);
        }
        cell = row.createCell(columnIndex);
        if (Objects.nonNull(cellStyle)) {
            cell.setCellStyle(cellStyle);
        }
        cell.setCellFormula(formula);
    }

    /**
     * 设置数值单元格。
     * <p>用于导出后还会被系统再次导入的列，避免 Excel 公式在导入端无法计算或公式缓存为空。</p>
     *
     * @param row         当前数据行
     * @param columnIndex 数值所在列下标，POI 从 0 开始计数
     * @param value       写入的数值
     */
    private void setNumericCell(Row row, int columnIndex, BigDecimal value) {
        Cell cell = row.getCell(columnIndex);
        CellStyle cellStyle = Objects.nonNull(cell) ? cell.getCellStyle() : null;
        if (Objects.nonNull(cell)) {
            row.removeCell(cell);
        }
        cell = row.createCell(columnIndex);
        if (Objects.nonNull(cellStyle)) {
            cell.setCellStyle(cellStyle);
        }
        cell.setCellValue(Objects.nonNull(value) ? value.doubleValue() : BigDecimal.ZERO.doubleValue());
    }

    /**
     * 安全读取单元格数值。
     * <p>模板写入后的 CE/CF 可能是数字，也可能因为占位符替换被写成字符串；
     * 这里统一转换为 BigDecimal，转换失败时按 0 处理，保证 J 列静态值可稳定生成。</p>
     *
     * @param cell 待读取的单元格
     * @return 单元格数值，读取不到时返回 0
     */
    private BigDecimal readNumericCell(Cell cell) {
        if (Objects.isNull(cell)) {
            return BigDecimal.ZERO;
        }
        try {
            CellType cellType = cell.getCellType();
            if (CellType.FORMULA.equals(cellType)) {
                cellType = cell.getCachedFormulaResultType();
            }
            if (CellType.NUMERIC.equals(cellType)) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            if (CellType.STRING.equals(cellType)) {
                String value = StringUtils.trimToEmpty(cell.getStringCellValue());
                return StringUtils.isNotBlank(value) ? new BigDecimal(value) : BigDecimal.ZERO;
            }
            if (CellType.BOOLEAN.equals(cellType)) {
                return cell.getBooleanCellValue() ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    /**
     * 汇总同一行多个数字单元格。
     *
     * @param row           当前数据行
     * @param columnIndexes 需要汇总的列下标，POI 从 0 开始计数
     * @return 汇总值
     */
    private BigDecimal sumNumericCells(Row row, int... columnIndexes) {
        if (Objects.isNull(row) || Objects.isNull(columnIndexes)) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (int columnIndex : columnIndexes) {
            total = total.add(readNumericCell(row.getCell(columnIndex)));
        }
        return total;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult importScheduleTemplate(List<LhScheduleResultTemplateImportVO> list, LhScheduleResult result, boolean updateSupport, Long id) {
        log.info(JSONUtil.toJsonStr(list));
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

        list.stream()
                .filter(Objects::nonNull)
                .forEach(this::fillImportChangedTrialStatus);
        // 第一轮：注解必填和Excel内重复校验（模板数据从第6行开始）
        for (int i = 0; i < list.size(); i++) {
            int rowNum = i + LH_SCHEDULE_IMPORT_DATA_START_ROW;
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
            ImportExcelValidatedUtils.validatedRepeat(list, row, i, LH_SCHEDULE_IMPORT_DATA_START_ROW, id, validated, "lhMachineCode", "materialCode", "changedTrialStatus");
            if (PubUtil.isNotEmpty(validated)) {
                failureNum++;
                row.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        Map<Integer, Date[]> shiftTimeMap = buildShiftTimeMap(scheduleDate);
        Set<String> importUniqueKeys = new HashSet<>();

        boolean hasValidBaseRow = list.stream()
                .filter(Objects::nonNull)
                .anyMatch(item -> !Objects.equals(item.getId(), -999L));
        ImportScheduleValidationContext validationContext = new ImportScheduleValidationContext();
        if (hasValidBaseRow) {
            // 业务主数据校验必须在删除旧排程之前执行。
            // 原因：模板导入是按“工厂+排程日期”整体覆盖，如果先删除旧排程，再发现月计划、
            // 工作日历等全局前置条件不满足，会导致原有排程被误清空。
            // 这里先批量加载后续逐行校验所需的主数据上下文；只有全局前置条件通过，并且最终
            // 生成了至少一条可导入记录时，才会在下方执行旧排程逻辑删除。
            validationContext = buildImportScheduleValidationContext(list, factoryCode, scheduleDate);
            if (CollUtil.isNotEmpty(validationContext.globalErrors)) {
                failureNum += (int) list.stream()
                        .filter(Objects::nonNull)
                        .filter(item -> !Objects.equals(item.getId(), -999L))
                        .count();
                ImportExcelValidatedUtils.addImportErrorLog(id, 0,
                        String.join(";", validationContext.globalErrors), importErrorLogs);
                return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
            }
        }
        String batchNo = lhScheduleResultService.generateNextBatchNo(scheduleDate, factoryCode);
        List<LhScheduleResult> insertList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            LhScheduleResultTemplateImportVO row = list.get(i);
            int rowNum = i + LH_SCHEDULE_IMPORT_DATA_START_ROW;
            if (Objects.isNull(row) || Objects.equals(row.getId(), -999L)) {
                continue;
            }
            List<String> rowErrors = validateImportRow(row, factoryCode, scheduleDate, importUniqueKeys, rowNum);
            if (!rowErrors.isEmpty()) {
                failureNum++;
                importErrorLogs.add(new ImportErrorLog(id, rowNum, String.join(";", rowErrors)));
                continue;
            }
            List<String> businessErrors = validateImportBusinessRow(row, validationContext);
            if (!businessErrors.isEmpty()) {
                failureNum++;
                importErrorLogs.add(new ImportErrorLog(id, rowNum, String.join(";", businessErrors)));
                continue;
            }

            LhScheduleResult target = new LhScheduleResult();
            target.setIsRelease(ReleaseStatusEnum.NOT_RELEASED.getCode());
            target.setDataSource("2");
            target.setFactoryCode(factoryCode);
            target.setScheduleDate(scheduleDate);
            target.setLhMachineCode(row.getLhMachineCode().trim());
            target.setMaterialCode(row.getMaterialCode().trim());

            copyImportRowToEntity(row, target);
            fillShiftTimes(target, shiftTimeMap);

            String orderNo = lhScheduleResultService.generateInsertOrderNo(scheduleDate);
            target.setBatchNo(batchNo);
            target.setOrderNo(orderNo);

//            scheduleResultMapper.insert(target);
            insertList.add(target);
            successNum++;
        }

        if (CollUtil.isEmpty(insertList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }

        // 模板导入以指定排程日期为整体覆盖范围；存在可导入记录时，再逻辑删除旧排程并写入本次有效数据。
        // 这样可以满足“只导入存在硫化示方号/班产的 SKU”的要求：失败 SKU 写入错误日志，
        // 成功 SKU 进入 insertList，并作为本次日期的新排程结果。
        scheduleResultMapper.update(null, new LambdaUpdateWrapper<LhScheduleResult>()
                .eq(LhScheduleResult::getFactoryCode, factoryCode)
                .eq(LhScheduleResult::getScheduleDate, scheduleDate)
                .eq(LhScheduleResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                .set(LhScheduleResult::getIsDelete, DeleteFlagEnum.DELETED.getCode()));

        // 复用排程结果服务中的字段补全逻辑，统一填充月计划版本、生产版本、规格、
        // 硫化时间、日计划量、硫化余量、示方号等导入模板没有直接提供的字段。
        lhScheduleResultService.fillScheduleResultFields(insertList, scheduleDate);

        // 从物料表反显胎胚代码和产品结构
        AppUtils.formatData(insertList, getQueryFormulas());

        this.baseDao.insertBatch(insertList);
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    /**
     * 构建导入模板业务校验上下文。
     * <p>
     * 该方法只做批量数据加载和全局前置条件校验，不直接决定某一行是否可导入。
     * 这样可以避免在逐行校验时反复访问数据库，也可以把“月计划是否定稿”“工作日历是否生成”
     * 这类整批导入共用的失败原因提前返回。
     * </p>
     * <p>
     * 当前导入业务要求所有 SKU、物料编码相关匹配都必须叠加 factoryCode。
     * 因此上下文中的 Map/Set key 均使用 factoryCode + 业务编码拼接，避免不同工厂同编码数据串用。
     * </p>
     *
     * @param list         Excel 模板解析后的导入行，包含前一轮注解必填校验结果
     * @param factoryCode  本次导入选择的工厂编码
     * @param scheduleDate 本次导入选择的排程日期
     * @return 导入业务校验上下文，包含全局错误和逐行校验所需的主数据缓存
     */
    private ImportScheduleValidationContext buildImportScheduleValidationContext(List<LhScheduleResultTemplateImportVO> list,
                                                                                 String factoryCode,
                                                                                 Date scheduleDate) {
        ImportScheduleValidationContext context = new ImportScheduleValidationContext();
        // 按自然月解析排程日期所属月份，26 号不算跨月。
        int[] yearMonth = resolveImportPlanYearMonth(scheduleDate);
        context.year = yearMonth[0];
        context.month = yearMonth[1];

        // 排程窗口覆盖 scheduleDate-1 到 scheduleDate+1。
        // 当窗口结束日期与排程日期不在同一自然月时，需同时加载下个月的定稿月计划明细，
        // 否则跨月物料会在"月计划中不存在SKU"校验中误报。
        Date windowEndDate = DateUtil.offsetDay(scheduleDate, LhScheduleConstant.SCHEDULE_DAYS - 2);
        int[] endYearMonth = resolveImportPlanYearMonth(windowEndDate);
        context.nextMonthYear = endYearMonth[0];
        context.nextMonthMonth = endYearMonth[1];
        context.crossMonth = (context.year != context.nextMonthYear) || (context.month != context.nextMonthMonth);
        // 窗口开始日期（scheduleDate-1）也可能与 scheduleDate 不在同一自然月，
        // 此时需同时加载上个月的定稿月计划明细，否则向左跨月的物料会被误报。
        Date windowStartDate = DateUtil.offsetDay(scheduleDate, -(LhScheduleConstant.SCHEDULE_DAYS - 2));
        int[] startYearMonth = resolveImportPlanYearMonth(windowStartDate);
        context.prevMonthYear = startYearMonth[0];
        context.prevMonthMonth = startYearMonth[1];
        context.crossMonthPrev = (context.year != context.prevMonthYear) || (context.month != context.prevMonthMonth);

        // 仅基于第一轮基础校验通过的行提取批量查询条件；
        // 必填缺失、Excel 内重复等基础错误行已经标记为 -999，不参与业务主数据校验。
        List<LhScheduleResultTemplateImportVO> validRows = list.stream()
                .filter(Objects::nonNull)
                .filter(row -> !Objects.equals(row.getId(), -999L))
                .collect(Collectors.toList());
        Set<String> materialCodes = validRows.stream()
                .map(LhScheduleResultTemplateImportVO::getMaterialCode)
                .filter(StringUtils::isNotBlank)
                .map(StringUtils::trim)
                .collect(Collectors.toSet());
        Set<String> machineCodes = validRows.stream()
                .map(LhScheduleResultTemplateImportVO::getLhMachineCode)
                .filter(StringUtils::isNotBlank)
                .map(StringUtils::trim)
                .collect(Collectors.toSet());

        // 全局校验 1：排程日期对应业务月必须存在定稿生产版本。
        // 后续月计划明细和字段补全均依赖这个 productionVersion。
        context.finalVersion = getImportFinalProductionVersion(factoryCode, context.year, context.month);
        if (Objects.isNull(context.finalVersion) || StringUtils.isBlank(context.finalVersion.getProductionVersion())) {
            context.globalErrors.add(String.format("工厂%s，%s年%s月月计划未定稿", factoryCode, context.year, context.month));
            return context;
        }

        // 全局校验 1.1：跨月时窗口结束日期所在月也必须存在定稿生产版本。
        if (context.crossMonth) {
            context.finalVersionNextMonth = getImportFinalProductionVersion(factoryCode, context.nextMonthYear, context.nextMonthMonth);
            if (Objects.isNull(context.finalVersionNextMonth) || StringUtils.isBlank(context.finalVersionNextMonth.getProductionVersion())) {
                context.globalErrors.add(String.format("工厂%s，%s年%s月月计划未定稿（跨月）", factoryCode, context.nextMonthYear, context.nextMonthMonth));
                return context;
            }
        }
        // 全局校验 1.2：向左跨月时窗口开始日期所在月也必须存在定稿生产版本。
        if (context.crossMonthPrev) {
            context.finalVersionPrevMonth = getImportFinalProductionVersion(factoryCode, context.prevMonthYear, context.prevMonthMonth);
            if (Objects.isNull(context.finalVersionPrevMonth) || StringUtils.isBlank(context.finalVersionPrevMonth.getProductionVersion())) {
                context.globalErrors.add(String.format("工厂%s，%s年%s月月计划未定稿（左跨月）", factoryCode, context.prevMonthYear, context.prevMonthMonth));
                return context;
            }
        }

        // 全局校验 2：工作日历中必须已经生成硫化工序日历。
        // 硫化工序编码固定为 02，与 MdmWorkCalendar.procCode 字典保持一致。
        if (!hasLhWorkCalendar(factoryCode, context.year, context.month)) {
            context.globalErrors.add(String.format("工厂%s，%s年%s月工作日历中硫化工序未生成", factoryCode, context.year, context.month));
            return context;
        }
        // 全局校验 2.1：跨月时窗口结束日期所在月也必须已生成硫化工序日历。
        if (context.crossMonth && !hasLhWorkCalendar(factoryCode, context.nextMonthYear, context.nextMonthMonth)) {
            context.globalErrors.add(String.format("工厂%s，%s年%s月工作日历中硫化工序未生成（跨月）", factoryCode, context.nextMonthYear, context.nextMonthMonth));
            return context;
        }
        // 全局校验 2.2：向左跨月时窗口开始日期所在月也必须已生成硫化工序日历。
        if (context.crossMonthPrev && !hasLhWorkCalendar(factoryCode, context.prevMonthYear, context.prevMonthMonth)) {
            context.globalErrors.add(String.format("工厂%s，%s年%s月工作日历中硫化工序未生成（左跨月）", factoryCode, context.prevMonthYear, context.prevMonthMonth));
            return context;
        }

        // 批量加载逐行校验所需数据：
        // 1. 月计划定稿明细：用于确认 SKU 是否在月计划中，并取得产品状态 productStatus；
        // 2. 机台台账：用于确认机台存在且状态启用；
        // 3. 模具台账可用集合：用于确认 SKU 关联的模具号可用；
        // 4. SKU-模具关系：用于从物料编码找到应校验的模具号；
        // 5. SKU-示方关系：用于按物料编码+产品状态确认硫化示方号；
        // 6. SKU 双模日硫化能力：用于确认班产 classCapacity 有值。
        context.monthPlanMap = loadImportMonthPlanMap(factoryCode, context.year, context.month,
                context.finalVersion.getProductionVersion(), materialCodes);
        // 向右跨月时同时加载窗口结束日期所在月份的定稿月计划明细。
        if (context.crossMonth) {
            context.monthPlanMapNextMonth = loadImportMonthPlanMap(factoryCode, context.nextMonthYear, context.nextMonthMonth,
                    context.finalVersionNextMonth.getProductionVersion(), materialCodes);
        }
        // 向左跨月时同时加载窗口开始日期所在月份的定稿月计划明细。
        if (context.crossMonthPrev) {
            context.monthPlanMapPrevMonth = loadImportMonthPlanMap(factoryCode, context.prevMonthYear, context.prevMonthMonth,
                    context.finalVersionPrevMonth.getProductionVersion(), materialCodes);
        }
        context.machineInfoMap = loadImportMachineInfoMap(factoryCode, machineCodes);
        context.availableMouldCodeSet = loadImportAvailableMouldCodeSet(factoryCode, materialCodes);
        context.materialMouldRelMap = loadImportMaterialMouldRelMap(factoryCode, materialCodes);
        context.constructionRefMap = loadImportConstructionRefMap(factoryCode, materialCodes);
        context.classCapacityMaterialSet = loadImportClassCapacityMaterialSet(factoryCode, materialCodes);
        return context;
    }

    /**
     * 校验单行导入数据是否满足硫化排程导入业务前置条件。
     * <p>
     * 该方法只读取 {@link ImportScheduleValidationContext} 中已经批量加载好的缓存，
     * 不再直接访问数据库，避免导入行数较多时产生 N+1 查询。
     * </p>
     * <p>
     * 返回错误时，该行不会进入 insertList；返回空集合时，该行才会被转换为 LhScheduleResult。
     * 这对应需求中的“只导入存在硫化示方号的 SKU”“只导入存在班产的 SKU”。
     * </p>
     *
     * @param row     当前导入行
     * @param context 批量导入校验上下文
     * @return 当前行业务错误列表，空集合表示可以导入
     */
    private List<String> validateImportBusinessRow(LhScheduleResultTemplateImportVO row,
                                                   ImportScheduleValidationContext context) {
        List<String> errors = new ArrayList<>();
        String factoryCode = StringUtils.trim(row.getFactoryCode());
        String materialCode = StringUtils.trim(row.getMaterialCode());
        String machineCode = StringUtils.trim(row.getLhMachineCode());
        String materialKey = buildMaterialFactoryKey(factoryCode, materialCode);

        // 校验机台是否可用：
        // 先按 factoryCode + machineCode 找硫化机台台账，再按 sys_enable_disable 字典判断启用状态。
        LhMachineInfo machineInfo = context.machineInfoMap.get(buildMachineFactoryKey(factoryCode, machineCode));
        if (Objects.isNull(machineInfo)) {
            errors.add(String.format("机台%s不存在", machineCode));
        } else if (!MachineStatusUtil.isEnabled(machineInfo.getStatus())) {
            errors.add(String.format("机台%s状态不可用", machineCode));
        }

        // 校验模具号是否可用：
        // 先通过 SKU-模具关系表按 factoryCode + materialCode 找到所有模具号，
        // 再到模具台账可用集合中确认这些模具号是否存在且 mouldStatus=1。
        List<String> mouldCodes = context.materialMouldRelMap.getOrDefault(materialKey, Collections.emptyList());
        if (CollUtil.isEmpty(mouldCodes)) {
            errors.add(String.format("SKU%s未维护模具关系", materialCode));
        } else {
            List<String> unavailableMouldCodes = mouldCodes.stream()
                    .filter(mouldCode -> !context.availableMouldCodeSet.contains(buildMouldFactoryKey(factoryCode, mouldCode)))
                    .distinct()
                    .collect(Collectors.toList());
            if (CollUtil.isNotEmpty(unavailableMouldCodes)) {
                errors.add(String.format("SKU%s模具号不可用：%s", materialCode, String.join(",", unavailableMouldCodes)));
            }
        }

        // 校验硫化示方号是否存在：
        // 需求要求“通过月计划中物料编码+产品状态到 SKU 与示方关系校验硫化示方号”。
        // 因此这里先从月计划明细拿 productStatus，再通过 SkuConstructionRefResolverUtil 统一降级匹配
        // （正规S→量试T→试制X；量试T→试制X；试制X 不降级），与自动排程和 SkuConstructionValidator 使用同一套规则。
        List<FactoryMonthPlanProductionFinalResult> monthPlanList = context.monthPlanMap.get(materialKey);
        // 向右跨月时，如果主月定稿表中找不到，尝试从窗口结束日期所在月份的定稿表中查找
        if (CollUtil.isEmpty(monthPlanList) && context.crossMonth) {
            monthPlanList = context.monthPlanMapNextMonth.get(materialKey);
        }
        // 向左跨月时，如果主月和右跨月定稿表中都找不到，尝试从窗口开始日期所在月份的定稿表中查找
        if (CollUtil.isEmpty(monthPlanList) && context.crossMonthPrev) {
            monthPlanList = context.monthPlanMapPrevMonth.get(materialKey);
        }
        if (CollUtil.isEmpty(monthPlanList)) {
            errors.add(String.format("月计划中不存在SKU%s", materialCode));
        } else {
            boolean hasValidRef = monthPlanList.stream()
                    .anyMatch(mp -> SkuConstructionRefResolverUtil
                            .resolveCuringRecipeRef(materialCode, mp.getProductStatus(),
                                    context.constructionRefMap) != null);
            if (!hasValidRef) {
                errors.add(String.format("SKU%s不存在硫化示方号", materialCode));
            }
        }

        // 校验班产是否有值：
        // MdmSkuLhCapacity.classCapacity 必须存在且大于 0，否则该 SKU 不导入，仅输出错误提示。
        if (!context.classCapacityMaterialSet.contains(materialKey)) {
            errors.add(String.format("SKU%s未维护班产", materialCode));
        }
        return errors;
    }

    /**
     * 根据排程日期解析对应的自然年月。
     * <p>
     * 按自然月计算，26 号不算跨月。
     * 排程窗口 t-1~t+1 的跨月判断由调用方通过比较 scheduleDate 和 windowEndDate 的自然月实现。
     * </p>
     *
     * @param scheduleDate 排程日期
     * @return int[0]=自然年份，int[1]=自然月份
     */
    private int[] resolveImportPlanYearMonth(Date scheduleDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(scheduleDate);
        return new int[]{calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1};
    }

    /**
     * 查询导入排程日期对应业务月的定稿生产版本。
     * <p>
     * 月计划明细表按 productionVersion 匹配；如果这里没有定稿版本，则整批导入无法继续。
     * 多条定稿数据异常存在时，取更新时间和 ID 最新的一条，与字段补全服务中的查询策略保持一致。
     * </p>
     *
     * @param factoryCode 工厂编码
     * @param year        业务年份
     * @param month       业务月份
     * @return 定稿生产版本，未找到返回 null
     */
    private MpFactoryProductionVersion getImportFinalProductionVersion(String factoryCode, int year, int month) {
        return mpFactoryProductionVersionMapper.selectOne(new LambdaQueryWrapper<MpFactoryProductionVersion>()
                .eq(MpFactoryProductionVersion::getFactoryCode, factoryCode)
                .eq(MpFactoryProductionVersion::getYear, year)
                .eq(MpFactoryProductionVersion::getMonth, month)
                .eq(MpFactoryProductionVersion::getIsFinal, "1")
                .eq(MpFactoryProductionVersion::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                .orderByDesc(MpFactoryProductionVersion::getUpdateTime)
                .orderByDesc(MpFactoryProductionVersion::getId)
                .last("LIMIT 1"));
    }

    /**
     * 判断指定工厂、业务年月是否已经生成硫化工作日历。
     * <p>
     * MdmWorkCalendar.procCode=02 表示硫化工序。这里只校验是否存在有效记录，
     * 不逐日校验开停产标识；导入前置条件关注的是该月该工序日历是否已生成。
     * </p>
     *
     * @param factoryCode 工厂编码
     * @param year        业务年份
     * @param month       业务月份
     * @return true 表示已生成硫化工序日历
     */
    private boolean hasLhWorkCalendar(String factoryCode, int year, int month) {
        return mdmWorkCalendarMapper.selectCount(new LambdaQueryWrapper<MdmWorkCalendar>()
                .eq(MdmWorkCalendar::getFactoryCode, factoryCode)
                .eq(MdmWorkCalendar::getYear, year)
                .eq(MdmWorkCalendar::getMonth, month)
                .eq(MdmWorkCalendar::getProcCode, "02")
                .eq(MdmWorkCalendar::getIsDelete, DeleteFlagEnum.NORMAL.getCode())) > 0;
    }

    /**
     * 加载本次导入 SKU 在定稿月计划中的明细。
     * <p>
     * 返回结果按 factoryCode + materialCode 组织，逐行校验时用于确认 SKU 是否属于定稿月计划，
     * 同时提供 productStatus 给“SKU 与示方关系”校验使用。
     * </p>
     *
     * @param factoryCode       工厂编码
     * @param year              业务年份
     * @param month             业务月份
     * @param productionVersion 定稿生产版本
     * @param materialCodes     本次导入涉及的物料编码集合
     * @return key=factoryCode|materialCode，value=月计划定稿明细
     */
    private Map<String, List<FactoryMonthPlanProductionFinalResult>> loadImportMonthPlanMap(String factoryCode,
                                                                                            int year,
                                                                                            int month,
                                                                                            String productionVersion,
                                                                                            Set<String> materialCodes) {
        if (CollUtil.isEmpty(materialCodes)) {
            return Collections.emptyMap();
        }
        List<FactoryMonthPlanProductionFinalResult> monthPlanList = monthPlanMapper.selectList(new LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult>()
                .eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, factoryCode)
                .eq(FactoryMonthPlanProductionFinalResult::getYear, year)
                .eq(FactoryMonthPlanProductionFinalResult::getMonth, month)
                .eq(FactoryMonthPlanProductionFinalResult::getProductionVersion, productionVersion)
                .in(FactoryMonthPlanProductionFinalResult::getMaterialCode, materialCodes)
                .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode()));
        return monthPlanList.stream()
                .filter(item -> StringUtils.isNotBlank(item.getMaterialCode()))
                .collect(Collectors.groupingBy(item -> buildMaterialFactoryKey(item.getFactoryCode(), item.getMaterialCode())));
    }

    /**
     * 加载本次导入涉及的硫化机台台账。
     * <p>
     * 这里只加载存在的机台记录，是否启用在逐行校验中通过 MachineStatusUtil 判断。
     * </p>
     *
     * @param factoryCode  工厂编码
     * @param machineCodes 本次导入涉及的机台编码集合
     * @return key=factoryCode|machineCode，value=机台台账
     */
    private Map<String, LhMachineInfo> loadImportMachineInfoMap(String factoryCode, Set<String> machineCodes) {
        if (CollUtil.isEmpty(machineCodes)) {
            return Collections.emptyMap();
        }
        List<LhMachineInfo> machineInfoList = lhMachineInfoMapper.selectList(new LambdaQueryWrapper<LhMachineInfo>()
                .eq(LhMachineInfo::getFactoryCode, factoryCode)
                .in(LhMachineInfo::getMachineCode, machineCodes)
                .eq(LhMachineInfo::getIsDelete, DeleteFlagEnum.NORMAL.getCode()));
        return machineInfoList.stream()
                .filter(item -> StringUtils.isNotBlank(item.getMachineCode()))
                .collect(Collectors.toMap(item -> buildMachineFactoryKey(item.getFactoryCode(), item.getMachineCode()),
                        item -> item, (a, b) -> a));
    }

    /**
     * 加载 SKU 与模具号关系。
     * <p>
     * 一个 SKU 可能关联多个模具号，所以返回值是 List。逐行校验时会检查该 SKU 的所有关联模具号
     * 是否均存在于“可用模具集合”中。
     * </p>
     *
     * @param factoryCode   工厂编码
     * @param materialCodes 本次导入涉及的物料编码集合
     * @return key=factoryCode|materialCode，value=该 SKU 关联的模具号列表
     */
    private Map<String, List<String>> loadImportMaterialMouldRelMap(String factoryCode, Set<String> materialCodes) {
        if (CollUtil.isEmpty(materialCodes)) {
            return Collections.emptyMap();
        }
        List<MdmSkuMouldRel> mouldRelList = mdmSkuMouldRelMapper.selectList(new LambdaQueryWrapper<MdmSkuMouldRel>()
                .eq(MdmSkuMouldRel::getFactoryCode, factoryCode)
                .in(MdmSkuMouldRel::getMaterialCode, materialCodes)
                .eq(MdmSkuMouldRel::getIsDelete, DeleteFlagEnum.NORMAL.getCode()));
        return mouldRelList.stream()
                .filter(item -> StringUtils.isNotBlank(item.getMaterialCode()))
                .filter(item -> StringUtils.isNotBlank(item.getMouldCode()))
                .collect(Collectors.groupingBy(item -> buildMaterialFactoryKey(item.getFactoryCode(), item.getMaterialCode()),
                        Collectors.mapping(item -> StringUtils.trim(item.getMouldCode()), Collectors.toList())));
    }

    /**
     * 加载本次导入 SKU 关联的可用模具号集合。
     * <p>
     * 查询步骤：
     * 1. 通过 MdmSkuMouldRel 找出本次导入物料编码关联的所有模具号；
     * 2. 通过 MdmModelInfo 按 factoryCode + mouldCode 查询模具台账；
     * 3. 只保留 mouldStatus=1 的可用模具。
     * </p>
     *
     * @param factoryCode   工厂编码
     * @param materialCodes 本次导入涉及的物料编码集合
     * @return key=factoryCode|mouldCode 的可用模具集合
     */
    private Set<String> loadImportAvailableMouldCodeSet(String factoryCode, Set<String> materialCodes) {
        Map<String, List<String>> materialMouldRelMap = loadImportMaterialMouldRelMap(factoryCode, materialCodes);
        Set<String> mouldCodes = materialMouldRelMap.values().stream()
                .flatMap(Collection::stream)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(mouldCodes)) {
            return Collections.emptySet();
        }
        List<MdmModelInfo> mouldInfoList = mdmModelInfoMapper.selectList(new LambdaQueryWrapper<MdmModelInfo>()
                .eq(MdmModelInfo::getFactoryCode, factoryCode)
                .in(MdmModelInfo::getMouldCode, mouldCodes)
                .eq(MdmModelInfo::getIsDelete, DeleteFlagEnum.NORMAL.getCode()));
        return mouldInfoList.stream()
                .filter(item -> StringUtils.isNotBlank(item.getMouldCode()))
                .filter(item -> MouldStatusUtil.isEnabled(item.getMouldStatus()))
                .map(item -> buildMouldFactoryKey(item.getFactoryCode(), item.getMouldCode()))
                .collect(Collectors.toSet());
    }

    /**
     * 加载存在有效硫化示方号的 SKU 与示方书关系 Map。
     * <p>
     * 只有 MdmSkuConstructionRef.lhNo 不为空的记录才视为“硫化示方号存在”。
     * key 为 materialCode + "::" + trialStatus，与 SkuConstructionRefResolverUtil 约定一致，
     * 供校验阶段统一降级匹配使用。
     * </p>
     *
     * @param factoryCode   工厂编码
     * @param materialCodes 本次导入涉及的物料编码集合
     * @return key=materialCode::trialStatus 的示方关系 Map
     */
    private Map<String, MdmSkuConstructionRef> loadImportConstructionRefMap(String factoryCode, Set<String> materialCodes) {
        if (CollUtil.isEmpty(materialCodes)) {
            return Collections.emptyMap();
        }
        List<MdmSkuConstructionRef> refList = mdmSkuConstructionRefMapper.selectList(new LambdaQueryWrapper<MdmSkuConstructionRef>()
                .eq(MdmSkuConstructionRef::getFactoryCode, factoryCode)
                .in(MdmSkuConstructionRef::getMaterialCode, materialCodes)
                .eq(MdmSkuConstructionRef::getIsDelete, DeleteFlagEnum.NORMAL.getCode()));
        return refList.stream()
                .filter(item -> StringUtils.isNotBlank(item.getMaterialCode()))
                .filter(item -> StringUtils.isNotBlank(item.getLhNo()))
                .collect(Collectors.toMap(
                        item -> item.getMaterialCode() + "::" + StringUtils.trimToEmpty(item.getTrialStatus()),
                        item -> item,
                        (a, b) -> a));
    }

    /**
     * 加载已维护有效班产的 SKU 集合。
     * <p>
     * MdmSkuLhCapacity.classCapacity 为空或小于等于 0 都视为未维护有效班产。
     * 这类 SKU 会写入导入失败日志，不进入本次排程结果。
     * </p>
     *
     * @param factoryCode   工厂编码
     * @param materialCodes 本次导入涉及的物料编码集合
     * @return key=factoryCode|materialCode 的有效班产 SKU 集合
     */
    private Set<String> loadImportClassCapacityMaterialSet(String factoryCode, Set<String> materialCodes) {
        if (CollUtil.isEmpty(materialCodes)) {
            return Collections.emptySet();
        }
        List<MdmSkuLhCapacity> capacityList = mdmSkuLhCapacityMapper.selectList(new LambdaQueryWrapper<MdmSkuLhCapacity>()
                .eq(MdmSkuLhCapacity::getFactoryCode, factoryCode)
                .in(MdmSkuLhCapacity::getMaterialCode, materialCodes)
                .eq(MdmSkuLhCapacity::getIsDelete, DeleteFlagEnum.NORMAL.getCode()));
        return capacityList.stream()
                .filter(item -> StringUtils.isNotBlank(item.getMaterialCode()))
                .filter(item -> Objects.nonNull(item.getClassCapacity()) && item.getClassCapacity() > 0)
                .map(item -> buildMaterialFactoryKey(item.getFactoryCode(), item.getMaterialCode()))
                .collect(Collectors.toSet());
    }

    /**
     * 构建物料维度缓存 key。
     * <p>所有 SKU/物料编码匹配均叠加 factoryCode，避免跨工厂同物料编码误匹配。</p>
     */
    private String buildMaterialFactoryKey(String factoryCode, String materialCode) {
        return StringUtils.trimToEmpty(factoryCode) + "|" + StringUtils.trimToEmpty(materialCode);
    }

    /**
     * 构建机台维度缓存 key。
     * <p>机台编码按工厂隔离，避免不同工厂同机台编码误匹配。</p>
     */
    private String buildMachineFactoryKey(String factoryCode, String machineCode) {
        return StringUtils.trimToEmpty(factoryCode) + "|" + StringUtils.trimToEmpty(machineCode);
    }

    /**
     * 构建模具维度缓存 key。
     * <p>模具号可用性校验必须叠加 factoryCode。</p>
     */
    private String buildMouldFactoryKey(String factoryCode, String mouldCode) {
        return StringUtils.trimToEmpty(factoryCode) + "|" + StringUtils.trimToEmpty(mouldCode);
    }

    /**
     * 导入模板业务校验上下文。
     * <p>
     * 该对象只在一次 importScheduleTemplate 调用内使用，用于保存整批导入共用的校验结果和主数据缓存。
     * globalErrors 表示整批导入无法继续的错误；其余 Map/Set 供逐行校验使用。
     * </p>
     */
    private static class ImportScheduleValidationContext {
        /**
         * 按 26 号切月规则解析得到的月计划业务年份。
         */
        private int year;
        /**
         * 按 26 号切月规则解析得到的月计划业务月份。
         */
        private int month;
        /**
         * 当前工厂、业务年月对应的定稿生产版本。
         */
        private MpFactoryProductionVersion finalVersion;
        /**
         * 排程窗口结束日期对应的业务年份（跨月时与 year 不同）。
         */
        private int nextMonthYear;
        /**
         * 排程窗口结束日期对应的业务月份（跨月时与 month 不同）。
         */
        private int nextMonthMonth;
        /**
         * 排程窗口开始日期对应的业务年份（左跨月时与 year 不同）。
         */
        private int prevMonthYear;
        /**
         * 排程窗口开始日期对应的业务月份（左跨月时与 month 不同）。
         */
        private int prevMonthMonth;
        /**
         * 是否向右跨月（窗口结束日期所在月份与 scheduleDate 所在月份不同）。
         */
        private boolean crossMonth;
        /**
         * 是否向左跨月（窗口开始日期所在月份与 scheduleDate 所在月份不同）。
         */
        private boolean crossMonthPrev;
        /**
         * 向右跨月时窗口结束日期所在月份的定稿生产版本。
         */
        private MpFactoryProductionVersion finalVersionNextMonth;
        /**
         * 向左跨月时窗口开始日期所在月份的定稿生产版本。
         */
        private MpFactoryProductionVersion finalVersionPrevMonth;
        /**
         * 整批导入级错误，例如月计划未定稿、硫化工作日历未生成。
         */
        private List<String> globalErrors = new ArrayList<>();
        /**
         * 定稿月计划明细缓存，key=factoryCode|materialCode，同物料（不同产品状态）全部保留。
         */
        private Map<String, List<FactoryMonthPlanProductionFinalResult>> monthPlanMap = new HashMap<>();
        /**
         * 向右跨月时窗口结束日期所在月份的定稿月计划明细缓存，key=factoryCode|materialCode。
         */
        private Map<String, List<FactoryMonthPlanProductionFinalResult>> monthPlanMapNextMonth = new HashMap<>();
        /**
         * 向左跨月时窗口开始日期所在月份的定稿月计划明细缓存，key=factoryCode|materialCode。
         */
        private Map<String, List<FactoryMonthPlanProductionFinalResult>> monthPlanMapPrevMonth = new HashMap<>();
        /**
         * 硫化机台台账缓存，key=factoryCode|machineCode。
         */
        private Map<String, LhMachineInfo> machineInfoMap = new HashMap<>();
        /**
         * SKU 与模具关系缓存，key=factoryCode|materialCode，value=模具号列表。
         */
        private Map<String, List<String>> materialMouldRelMap = new HashMap<>();
        /**
         * 可用模具号集合，key=factoryCode|mouldCode。
         */
        private Set<String> availableMouldCodeSet = new HashSet<>();
        /**
         * 有效硫化示方关系 Map，key=materialCode::trialStatus，value=MdmSkuConstructionRef。
         */
        private Map<String, MdmSkuConstructionRef> constructionRefMap = new HashMap<>();
        /**
         * 已维护有效班产的 SKU 集合，key=factoryCode|materialCode。
         */
        private Set<String> classCapacityMaterialSet = new HashSet<>();
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
            String uniqueKey = buildImportUniqueKey(factoryCode, scheduleDate, row.getLhMachineCode(), row.getMaterialCode(), row.getChangedTrialStatus());
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


    private String buildImportUniqueKey(String factoryCode, Date scheduleDate, String lhMachineCode, String materialCode, String changedTrialStatus) {
        return StringUtils.defaultString(factoryCode).trim() + "|"
                + DateUtil.format(DateUtil.beginOfDay(scheduleDate), "yyyy-MM-dd") + "|"
                + StringUtils.defaultString(lhMachineCode).trim() + "|"
                + StringUtils.defaultString(materialCode).trim() + "|"
                + StringUtils.defaultString(changedTrialStatus).trim();
    }

    private void fillImportChangedTrialStatus(LhScheduleResultTemplateImportVO row) {
        if (Objects.isNull(row)) {
            return;
        }
        String changedTrialStatus = Stream.of(
                row.getClass1IsEnd(),
                row.getClass2IsEnd(),
                row.getClass3IsEnd(),
                row.getClass4IsEnd(),
                row.getClass5IsEnd(),
                row.getClass6IsEnd(),
                row.getClass7IsEnd(),
                row.getClass8IsEnd()
        ).filter(StringUtils::isNotBlank).findFirst().orElse(null);
        if (StringUtils.isNotBlank(changedTrialStatus)) {
            row.setChangedTrialStatus(changedTrialStatus);
        }
    }

    private void copyImportRowToEntity(LhScheduleResultTemplateImportVO source, LhScheduleResult target) {
        target.setLhMachineName(source.getLhMachineName());
        // 模板按 8 个班次分别维护示方类型，导入时取第一个非空的 classXLhType 作为主业务口径。
        String scheduleType = Stream.of(
                source.getClass1LhType(),
                source.getClass2LhType(),
                source.getClass3LhType(),
                source.getClass4LhType(),
                source.getClass5LhType(),
                source.getClass6LhType(),
                source.getClass7LhType(),
                source.getClass8LhType(),
                source.getTrialStatus()
        ).filter(StringUtils::isNotBlank).findFirst().orElse(null);
        target.setProductStatus(scheduleType);

        // 遍历class1LeftRightMould到class8LeftRightMould，取第一个非空值
        String leftRightMould = Stream.of(
                source.getClass1LeftRightMould(),
                source.getClass2LeftRightMould(),
                source.getClass3LeftRightMould(),
                source.getClass4LeftRightMould(),
                source.getClass5LeftRightMould(),
                source.getClass6LeftRightMould(),
                source.getClass7LeftRightMould(),
                source.getClass8LeftRightMould()
        ).filter(StringUtils::isNotBlank).findFirst().orElse(null);
        target.setLeftRightMould(leftRightMould);

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
        target.setDailyPlanQty(source.getDailyPlanQty());
        target.setTotalDailyPlanQty(source.getDailyPlanQty());
        target.setRemark(source.getRemark());
        target.setChangedTrialStatus(source.getChangedTrialStatus());

        target.setClass1PlanQty(source.getClass1PlanQty());
        target.setClass1FinishQty(source.getClass1FinishQty());
        target.setClass1Analysis(source.getClass1Analysis());
        target.setClass1IsEnd(source.getClass1IsEnd());
        target.setClass1LhType(source.getClass1LhType());
        target.setClass2PlanQty(source.getClass2PlanQty());
        target.setClass2FinishQty(source.getClass2FinishQty());
        target.setClass2Analysis(source.getClass2Analysis());
        target.setClass2IsEnd(source.getClass2IsEnd());
        target.setClass2LhType(source.getClass2LhType());
        target.setClass3PlanQty(source.getClass3PlanQty());
        target.setClass3FinishQty(source.getClass3FinishQty());
        target.setClass3Analysis(source.getClass3Analysis());
        target.setClass3IsEnd(source.getClass3IsEnd());
        target.setClass3LhType(source.getClass3LhType());
        target.setClass4PlanQty(source.getClass4PlanQty());
        target.setClass4FinishQty(source.getClass4FinishQty());
        target.setClass4Analysis(source.getClass4Analysis());
        target.setClass4IsEnd(source.getClass4IsEnd());
        target.setClass4LhType(source.getClass4LhType());
        target.setClass5PlanQty(source.getClass5PlanQty());
        target.setClass5FinishQty(source.getClass5FinishQty());
        target.setClass5Analysis(source.getClass5Analysis());
        target.setClass5IsEnd(source.getClass5IsEnd());
        target.setClass5LhType(source.getClass5LhType());
        target.setClass6PlanQty(source.getClass6PlanQty());
        target.setClass6FinishQty(source.getClass6FinishQty());
        target.setClass6Analysis(source.getClass6Analysis());
        target.setClass6IsEnd(source.getClass6IsEnd());
        target.setClass6LhType(source.getClass6LhType());
        target.setClass7PlanQty(source.getClass7PlanQty());
        target.setClass7FinishQty(source.getClass7FinishQty());
        target.setClass7Analysis(source.getClass7Analysis());
        target.setClass7IsEnd(source.getClass7IsEnd());
        target.setClass7LhType(source.getClass7LhType());
        target.setClass8PlanQty(source.getClass8PlanQty());
        target.setClass8FinishQty(source.getClass8FinishQty());
        target.setClass8Analysis(source.getClass8Analysis());
        target.setClass8IsEnd(source.getClass8IsEnd());
        target.setClass8LhType(source.getClass8LhType());

        target.setScheduleType(normalizeScheduleTypeCode(source.getScheduleType()));
        if (StringUtils.isNotBlank(source.getIsFirst())) {
            target.setIsFirst(source.getIsFirst());
        }
        if (StringUtils.isNotBlank(source.getIsEnd())) {
            target.setIsEnd(source.getIsEnd());
        }
        String constructionStage = Stream.of(
                source.getClass1LhType(),
                source.getClass2LhType(),
                source.getClass3LhType(),
                source.getClass4LhType(),
                source.getClass5LhType(),
                source.getClass6LhType(),
                source.getClass7LhType(),
                source.getClass8LhType()
        ).filter(StringUtils::isNotBlank).findFirst().orElse(null);
        if (constructionStage != null) {
            target.setConstructionStage(constructionStage);
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
     * @param list         排程结果列表
     * @param scheduleDate 排程日期
     * @return 模板表头数据
     */
    private Map<String, Object> buildExportTableMap(List<LhScheduleResult> list, Date scheduleDate) {
        Map<String, Object> tableMap = new HashMap<>(16);

        // 优先使用前端传入的 scheduleDate 作为排程基准日；如果旧入口未传，
        // 再从列表首条数据兜底，保证导出标题和班次日期仍有来源。
        Date exportScheduleDate = Objects.nonNull(scheduleDate) ? scheduleDate : list.stream()
                .map(LhScheduleResult::getScheduleDate)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        // 班次标题不能写死。这里复用 listScheduleShiftDates 的排班日历规则，
        // 将 1~8 班对应的 MM/dd 回填到模板中的 {shiftDate1} ~ {shiftDate8}。
        List<LhScheduleShiftDateVO> shiftDateList = listScheduleShiftDates(exportScheduleDate);
        for (int i = 0; i < LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; i++) {
            String shiftDate = i < shiftDateList.size() ? shiftDateList.get(i).getShiftDate() : "";
            tableMap.put("shiftDate" + (i + 1), shiftDate);
        }

        // 换模次数属于模板表头数据，不随明细行逐行复制。
        // 这里把每个班次“左右模/Khuôn trái phải”列上方需要展示的次数写入 tableMap。
        tableMap.putAll(buildMouldChangeCountTableMap(list, exportScheduleDate, shiftDateList));

        // 固定表头字段沿用模板占位符，避免再通过 loadExportI18nTableName 动态改列名。
        tableMap.put("yearmonthday", DateUtil.format(exportScheduleDate, "yyyy年MM月dd日"));
        tableMap.put("productionVersion", PubUtil.isNotEmpty(list) ? list.get(0).getProductionVersion() : "");
        tableMap.put("batchNo", PubUtil.isNotEmpty(list) ? list.get(0).getBatchNo() : "");
        return tableMap;
    }

    /**
     * 构建模板表头中的换模次数。
     * <p>模板“换模次数”显示在各班次“左右模/Khuôn trái phải”列上方，因此这里按班次生成
     * mouldChangeCount1 ~ mouldChangeCount6，交给 tableMap 替换。换模计划来源按当前导出明细的
     * batchNo + factoryCode 批量查询，再按班次标题日期 + classIndex 统计：</p>
     * <p>只统计早班和中班：第1天早/中、第2天早/中、第3天早/中依次对应 1~6；夜班没有对应 classIndex，不生成占位符。</p>
     *
     * @param list               排程结果列表
     * @param exportScheduleDate 导出排程日期
     * @param shiftDateList      班次标题日期列表
     * @return 换模次数表头Map
     */
    private Map<String, Object> buildMouldChangeCountTableMap(List<LhScheduleResult> list,
                                                              Date exportScheduleDate,
                                                              List<LhScheduleShiftDateVO> shiftDateList) {
        Map<String, Object> resultMap = new HashMap<>(16);

        // 先初始化 6 个占位符，保证没有换模计划时模板不会残留 {mouldChangeCountX}。
        for (int index = 1; index <= LhScheduleConstant.SCHEDULE_DAYS * 2; index++) {
            resultMap.put("mouldChangeCount" + index, "");
        }
        if (PubUtil.isEmpty(list) || Objects.isNull(exportScheduleDate) || PubUtil.isEmpty(shiftDateList)) {
            return resultMap;
        }

        // 查询来源按“导出结果中的批次号+工厂”确定，先拆成 in 条件降低数据库查询范围。
        // 后续仍会用 batchFactoryKeySet 做精确二次过滤，避免 A工厂批次 + B工厂批次交叉误算。
        List<String> batchNos = list.stream()
                .map(LhScheduleResult::getBatchNo)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        List<String> factoryCodes = list.stream()
                .map(LhScheduleResult::getFactoryCode)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        if (batchNos.isEmpty() || factoryCodes.isEmpty()) {
            return resultMap;
        }
        Set<String> batchFactoryKeySet = list.stream()
                .filter(item -> StringUtils.isNotBlank(item.getBatchNo()))
                .filter(item -> StringUtils.isNotBlank(item.getFactoryCode()))
                .map(item -> buildBatchFactoryKey(item.getBatchNo(), item.getFactoryCode()))
                .collect(Collectors.toSet());

        // 班次标题日期来自 listScheduleShiftDates，例如 04/27、04/28。
        // 这里转回完整 Date，用于限定换模计划查询日期范围，减少无关历史数据进入内存聚合。
        List<Date> shiftDates = shiftDateList.stream()
                .map(item -> parseShiftDate(exportScheduleDate, item.getShiftDate()))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (shiftDates.isEmpty()) {
            return resultMap;
        }
        Date minScheduleDate = shiftDates.stream().min(Date::compareTo).orElse(exportScheduleDate);
        Date maxScheduleDate = shiftDates.stream().max(Date::compareTo).orElse(exportScheduleDate);

        // 一次性查出导出窗口内的换模计划。换模次数最终只统计数量，
        // 不区分左右模具体值；展示位置由模板的“左右模”列决定。
        List<LhMouldChangePlan> mouldChangePlanList = lhMouldChangePlanMapper.selectList(
                new LambdaQueryWrapper<LhMouldChangePlan>()
                        .in(LhMouldChangePlan::getLhResultBatchNo, batchNos)
                        .in(LhMouldChangePlan::getFactoryCode, factoryCodes)
                        .ge(LhMouldChangePlan::getPlanDate, DateUtil.beginOfDay(minScheduleDate))
                        .lt(LhMouldChangePlan::getPlanDate, DateUtil.offsetDay(DateUtil.beginOfDay(maxScheduleDate), 1)));
        if (PubUtil.isEmpty(mouldChangePlanList)) {
            return resultMap;
        }

        // 按“计划日期 + 班次编码”聚合换模次数。
        // 业务要求 classIndex 只取 02早班、03中班，夜班没有换模次数口径。
        Map<String, Long> countMap = mouldChangePlanList.stream()
                .filter(item -> batchFactoryKeySet.contains(buildBatchFactoryKey(item.getLhResultBatchNo(), item.getFactoryCode())))
                .filter(item -> Objects.nonNull(item.getScheduleDate()))
                .filter(item -> StringUtils.isNotBlank(item.getClassIndex()))
                .collect(Collectors.groupingBy(
                        item -> buildMouldChangeCountKey(item.getScheduleDate(), item.getClassIndex()),
                        Collectors.counting()
                ));

        // 把聚合结果写回模板占位符：
        // 只为早班/中班生成连续序号，避免第三天早班被写成 mouldChangeCount7。
        int mouldChangeIndex = 1;
        for (int i = 0; i < LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; i++) {
            LhScheduleShiftDateVO shiftDateVO = i < shiftDateList.size() ? shiftDateList.get(i) : null;
            if (Objects.isNull(shiftDateVO)) {
                continue;
            }
            String classIndex = buildMouldChangeClassIndex(shiftDateVO.getShift());
            Date shiftDate = parseShiftDate(exportScheduleDate, shiftDateVO.getShiftDate());
            if (StringUtils.isBlank(classIndex) || Objects.isNull(shiftDate)) {
                continue;
            }
            Long count = countMap.get(buildMouldChangeCountKey(shiftDate, classIndex));
            resultMap.put("mouldChangeCount" + mouldChangeIndex, Objects.nonNull(count) && count > 0 ? count : "");
            mouldChangeIndex++;
        }
        return resultMap;
    }

    /**
     * 根据模板班次序号换算换模计划班次。
     *
     * @param shift 模板班次序号
     * @return 换模计划班次编码，02=早班，03=中班
     */
    private String buildMouldChangeClassIndex(Integer shift) {
        if (Objects.isNull(shift)) {
            return "";
        }
        if (shift == 1 || shift == 4 || shift == 7) {
            return ApsConstant.CLASS_INDEX_MORNING_SHIFT;
        }
        if (shift == 2 || shift == 5 || shift == 8) {
            return ApsConstant.CLASS_INDEX_MIDDLE_SHIFT;
        }
        return "";
    }

    /**
     * 构建换模次数聚合Key。
     *
     * @param scheduleDate 排程日期
     * @param classIndex   班次编码
     * @return 聚合Key
     */
    private String buildMouldChangeCountKey(Date scheduleDate, String classIndex) {
        return DateUtil.formatDate(DateUtil.beginOfDay(scheduleDate)) + "|" + StringUtils.defaultString(classIndex).trim();
    }

    /**
     * 构建批次工厂匹配Key。
     *
     * @param batchNo     批次号
     * @param factoryCode 工厂编号
     * @return 批次工厂匹配Key
     */
    private String buildBatchFactoryKey(String batchNo, String factoryCode) {
        return StringUtils.defaultString(batchNo).trim() + "|" + StringUtils.defaultString(factoryCode).trim();
    }

    /**
     * 构建模板列表数据
     *
     * @param list         排程结果列表
     * @param scheduleDate        导出入口传入的排程日期，用于固定 T 日完成量查询口径
     * @param mouldChangePlanList 模具交替计划，用于按同批次、同排程日、同机台修正一班顺序
     * @return 模板列表数据
     */
    private List<Map<String, Object>> buildExportDataList(List<LhScheduleResult> list, Date scheduleDate,
                                                          List<LhMouldChangePlan> mouldChangePlanList) {
        List<Map<String, Object>> dataList = new ArrayList<>(list.size() + 1);
        Map<String, LhRepairCapsule> capsuleMap = buildRepairCapsuleExportMap(list);
        Map<Long, String> cxMachineCodeMap = buildCxMachineCodeExportMap(list);
        Map<String, Object> todayNightFinishQtyMap = buildTodayNightFinishQtyExportMap(list, scheduleDate);
        Map<String, String> recipeTypeMap = loadLhTrialStatusDictMap();
        Map<String, String> endTypeMap = loadBizEndTypeDictMap();

        // 先按硫化机台升序（忽略大小写），再按同机台不同物料的收尾班次序号升序排（先收尾下机的物料排前面）
        List<LhScheduleResult> sortedList = list.stream()
                .sorted(Comparator
                        .comparing((LhScheduleResult r) -> StringUtils.defaultString(r.getLhMachineCode()), String.CASE_INSENSITIVE_ORDER)
                        .thenComparingInt(r -> findLastPlannedShift(r))
                        .thenComparing(r -> StringUtils.defaultString(r.getMaterialCode())))
                .collect(Collectors.toList());

        // 构建8班顺序值映射：同一物料按班次1~8遍历，每个班次内有计划量的记录按机台编码升序，顺序值从1~n连续编排
        Map<String, Map<Integer, Integer>> shiftOrderMap = buildContinuousShiftOrderMap(sortedList);
        Map<String, Integer> class1MouldChangeOrderMap = buildClass1MouldChangeOrderMap(mouldChangePlanList);
        Map<LhScheduleResult, ExportRowStyleFlag> rowStyleFlagMap = buildMachinePostCloseOutStyleFlagMap(sortedList);

        for (LhScheduleResult result : sortedList) {
            Map<String, Object> row = new HashMap<>(112);
            row.put("height", 17);
            row.put("lhMachineCode", result.getLhMachineCode());
            row.put("materialCode", result.getMaterialCode());
            row.put("embryoCode", result.getEmbryoCode());
            row.put("materialDesc", result.getMaterialDesc());
            row.put("mainMaterialDesc", result.getMainMaterialDesc());
//            row.put("scheduleType", buildScheduleTypeName(result.getScheduleType()));

            // 胶囊使用次数来自 T_LH_REPAIR_CAPSULE，同机台同日期优先；
            // 如果排程日没有采集记录，则取该机台距离排程日最近的一条记录。
            LhRepairCapsule repairCapsule = capsuleMap.get(buildCapsuleExportKey(result));
            row.put("replaceCapsuleCount", Objects.nonNull(repairCapsule) ? repairCapsule.getReplaceCapsuleCount() : "");
            row.put("replaceCapsuleCount2", Objects.nonNull(repairCapsule) ? repairCapsule.getReplaceCapsuleCount2() : "");
            row.put("replaceCapsuleCountLeftRight", buildCapsuleCountLeftRight(repairCapsule));

            // 成型机台号来自成型排程结果的 LH_SCHEDULE_IDS 反查，多个成型机台用分号拼接。
            row.put("cxMachineCode", cxMachineCodeMap.get(result.getId()));
            row.put("todayNightFinishQty", result.getTotalFinishQty());
            row.put("mouldSurplusQty", result.getMouldSurplusQty());
            row.put("embryoStock", result.getEmbryoStock());
            row.put("singleMouldShiftQty", result.getSingleMouldShiftQty());
            row.put("leftRightMould", result.getLeftRightMould());
            row.put("mouldMethod", result.getMouldMethod());
            row.put("structureName", result.getStructureName());
            row.put("totalFinishQty", sumFinishQty(result));
            row.put("isEnd", buildUnifiedShiftType(result, endTypeMap));
            row.put("trialStatus", buildUnifiedLhType(result, recipeTypeMap));

            // 明细行的 dailyPlanQty 已按需求从 totalDailyPlanQty 改为 dailyPlanQty。
            // totalPlanQty 只是模板“总计”占位符，为了明细行保持同一日计划量展示，
            // 当前同步写入 dailyPlanQty；首行汇总会在 buildSummaryRow 中单独计算。
            row.put("dailyPlanQty", (Objects.isNull(result.getTotalFinishQty()) ? 0 : result.getTotalFinishQty()) - (Objects.isNull(result.getTotalDailyPlanQty()) ? 0 : result.getTotalDailyPlanQty()));
            row.put("totalPlanQty", result.getDailyPlanQty());
            row.put("totalDailyPlanQty", result.getTotalDailyPlanQty());
            row.put("monthPlanSumTotal", result.getMonthPlanSumTotal());
            row.put("remark", result.getRemark());

            // 公式列占位符：模板中 BZ/CA/CB 列使用 {.nightPlanQtyTotal} / {.nightFinishQtyTotal} /
            // {.totalPlanQtyFormula} 占位符标记列位置，writeMultiList 会将其替换为空字符串，
            // 后续由 fillExportSummaryFormulas 根据占位符映射动态定位并写入实际值/公式。
            row.put("nightPlanQtyTotal", "");
            row.put("nightFinishQtyTotal", "");
            row.put("totalPlanQtyFormula", "");

            // 8 个班次字段结构完全一致，通过 class{班次号}xxx 统一组装，
            // 与模板中的 {.class1PlanQty}、{.class8Analysis} 等占位符一一对应。
            for (int shift = 1; shift <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shift++) {
                row.put("class" + shift + "LeftRightMould", buildShiftLeftRightMould(result, shift));
                row.put("class" + shift + "Order", resolveExportShiftOrder(
                        shiftOrderMap, class1MouldChangeOrderMap, result, shift));
                row.put("class" + shift + "PlanQty", getClassPlanQty(result, shift));
                row.put("class" + shift + "FinishQty", getClassFinishQty(result, shift));
                Object shiftType = buildShiftType(result, shift, endTypeMap);
                Object shiftLhType = buildShiftMouldMethod(result, shift, recipeTypeMap);
                Object shiftProductStatus = buildShiftProductStatus(result, shift, recipeTypeMap);
                row.put("class" + shift + "IsEnd", shiftType);
                row.put("class" + shift + "Type", shiftType);
                row.put("class" + shift + "LhType", shiftProductStatus);
                row.put("class" + shift + "MouldMethod", shiftLhType);
                row.put("class" + shift + "Analysis", getClassAnalysis(result, shift));
                row.put("class" + shift + "Dot", "");
            }

            // 导出着色规则
            applyExportRowStyle(row, rowStyleFlagMap.get(result));

            dataList.add(row);
        }
        return dataList;
    }

    /**
     * 构建模具交替计划对应的一班顺序映射。
     * 同一硫化结果批次、排程日期和机台内，前规格为1，后规格为2。
     *
     * @param mouldChangePlanList 模具交替计划
     * @return key=批次号|排程日期|机台|物料编码，value=一班顺序
     */
    private Map<String, Integer> buildClass1MouldChangeOrderMap(List<LhMouldChangePlan> mouldChangePlanList) {
        Map<String, Integer> orderMap = new HashMap<>();
        if (CollUtil.isEmpty(mouldChangePlanList)) {
            return orderMap;
        }
        mouldChangePlanList.forEach(plan -> {
            if (StringUtils.isNotBlank(plan.getBeforeMaterialCode())) {
                orderMap.put(buildMouldChangeOrderKey(plan.getLhResultBatchNo(), plan.getScheduleDate(),
                        plan.getLhMachineCode(), plan.getBeforeMaterialCode()), 1);
            }
            if (StringUtils.isNotBlank(plan.getAfterMaterialCode())) {
                orderMap.put(buildMouldChangeOrderKey(plan.getLhResultBatchNo(), plan.getScheduleDate(),
                        plan.getLhMachineCode(), plan.getAfterMaterialCode()), 2);
            }
        });
        return orderMap;
    }

    /**
     * 获取导出班次顺序。仅一班命中模具交替计划时覆盖为前规格1、后规格2，其他情况保持原顺序。
     *
     * @param shiftOrderMap             原连续顺序映射
     * @param class1MouldChangeOrderMap 一班模具交替顺序映射
     * @param result                    排程结果
     * @param shift                     班次序号
     * @return 导出顺序
     */
    private Object resolveExportShiftOrder(Map<String, Map<Integer, Integer>> shiftOrderMap,
                                           Map<String, Integer> class1MouldChangeOrderMap,
                                           LhScheduleResult result, int shift) {
        Object currentOrder = getContinuousShiftOrder(shiftOrderMap, result, shift);
        if (shift != 1 || !(currentOrder instanceof Integer)) {
            return currentOrder;
        }
        String key = buildMouldChangeOrderKey(result.getBatchNo(), result.getScheduleDate(),
                result.getLhMachineCode(), result.getMaterialCode());
        return class1MouldChangeOrderMap.getOrDefault(key, (Integer) currentOrder);
    }

    /**
     * 构建模具交替顺序匹配Key。
     *
     * @param batchNo     硫化结果批次号
     * @param scheduleDate 排程日期
     * @param machineCode 硫化机台编码
     * @param materialCode 物料编码
     * @return 匹配Key
     */
    private String buildMouldChangeOrderKey(String batchNo, Date scheduleDate,
                                            String machineCode, String materialCode) {
        String scheduleDay = Objects.nonNull(scheduleDate) ? DateUtil.formatDate(scheduleDate) : "";
        return StringUtils.defaultString(batchNo).trim() + "|" + scheduleDay + "|"
                + StringUtils.defaultString(machineCode).trim() + "|"
                + StringUtils.defaultString(materialCode).trim();
    }

    /**
     * 构建导出用胶囊使用次数Map。
     * <p>key 按导出明细行维度生成，保证同一机台在不同排程日期导出时可以分别匹配。</p>
     *
     * @param list 排程结果列表
     * @return 胶囊使用次数Map，key=工厂|硫化机台|排程日期
     */
    private Map<String, LhRepairCapsule> buildRepairCapsuleExportMap(List<LhScheduleResult> list) {
        if (PubUtil.isEmpty(list)) {
            return Collections.emptyMap();
        }
        List<String> machineCodes = list.stream()
                .map(LhScheduleResult::getLhMachineCode)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        if (machineCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> factoryCodes = list.stream()
                .map(LhScheduleResult::getFactoryCode)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<LhRepairCapsule> queryWrapper = new LambdaQueryWrapper<LhRepairCapsule>()
                .in(LhRepairCapsule::getLhCode, machineCodes)
                .eq(LhRepairCapsule::getIsDelete, DeleteFlagEnum.NORMAL.getCode());
        if (!factoryCodes.isEmpty()) {
            queryWrapper.in(LhRepairCapsule::getFactoryCode, factoryCodes);
        }
        List<LhRepairCapsule> capsuleList = lhRepairCapsuleMapper.selectList(queryWrapper);
        if (PubUtil.isEmpty(capsuleList)) {
            return Collections.emptyMap();
        }

        Map<String, List<LhRepairCapsule>> machineCapsuleMap = capsuleList.stream()
                .filter(item -> StringUtils.isNotBlank(item.getLhCode()))
                .collect(Collectors.groupingBy(item -> buildCapsuleMachineKey(item.getFactoryCode(), item.getLhCode())));
        Map<String, LhRepairCapsule> resultMap = new HashMap<>(list.size());
        for (LhScheduleResult result : list) {
            List<LhRepairCapsule> sameMachineCapsules = machineCapsuleMap.get(buildCapsuleMachineKey(result.getFactoryCode(), result.getLhMachineCode()));
            LhRepairCapsule matched = matchRepairCapsuleByScheduleDate(sameMachineCapsules, result.getScheduleDate());
            if (Objects.nonNull(matched)) {
                resultMap.put(buildCapsuleExportKey(result), matched);
            }
        }
        return resultMap;
    }

    /**
     * 按排程日期匹配胶囊使用次数。
     * <p>先找获取日期与排程日期同一天的记录；若不存在，则取获取日期距离排程日期最近的一条。</p>
     *
     * @param capsuleList  同一硫化机台下的胶囊使用次数列表
     * @param scheduleDate 排程日期
     * @return 匹配到的胶囊使用次数
     */
    private LhRepairCapsule matchRepairCapsuleByScheduleDate(List<LhRepairCapsule> capsuleList, Date scheduleDate) {
        if (PubUtil.isEmpty(capsuleList)) {
            return null;
        }
        if (Objects.isNull(scheduleDate)) {
            return capsuleList.stream()
                    .filter(item -> Objects.nonNull(item.getObtainTime()))
                    .max(Comparator.comparing(LhRepairCapsule::getObtainTime))
                    .orElse(capsuleList.get(0));
        }
        Date scheduleDay = DateUtil.beginOfDay(scheduleDate);
        return capsuleList.stream()
                .filter(item -> Objects.nonNull(item.getObtainTime()))
                .min(Comparator
                        .comparing((LhRepairCapsule item) -> DateUtil.isSameDay(item.getObtainTime(), scheduleDay) ? 0 : 1)
                        .thenComparingLong(item -> Math.abs(DateUtil.beginOfDay(item.getObtainTime()).getTime() - scheduleDay.getTime())))
                .orElse(null);
    }

    /**
     * 构建胶囊L/R次数展示值。
     * <p>模板只有一个“胶囊次数L/R”单元格，因此导出时组合为“左次数/右次数”。</p>
     *
     * @param repairCapsule 胶囊使用次数
     * @return 胶囊L/R次数展示值
     */
    private String buildCapsuleCountLeftRight(LhRepairCapsule repairCapsule) {
        if (Objects.isNull(repairCapsule)) {
            return "";
        }
        String leftCount = Objects.nonNull(repairCapsule.getReplaceCapsuleCount())
                ? String.valueOf(repairCapsule.getReplaceCapsuleCount()) : "";
        String rightCount = Objects.nonNull(repairCapsule.getReplaceCapsuleCount2())
                ? String.valueOf(repairCapsule.getReplaceCapsuleCount2()) : "";
        if (StringUtils.isBlank(leftCount) && StringUtils.isBlank(rightCount)) {
            return "";
        }
        return leftCount + "/" + rightCount;
    }

    /**
     * 构建导出用成型机台Map。
     * <p>通过成型排程结果 LH_SCHEDULE_IDS 反查当前硫化排程结果ID，一个硫化ID可能关联多个成型机台。</p>
     *
     * @param list 排程结果列表
     * @return 成型机台Map，key=硫化排程结果ID，value=成型机台号
     */
    private Map<Long, String> buildCxMachineCodeExportMap(List<LhScheduleResult> list) {
        List<Long> lhScheduleIds = list.stream()
                .map(LhScheduleResult::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (lhScheduleIds.isEmpty()) {
            log.warn("buildCxMachineCodeExportMap: 导出列表中所有记录的ID为空，无法查询成型机台号");
            return Collections.emptyMap();
        }
        log.debug("buildCxMachineCodeExportMap: 准备查询成型机台号，硫化排程ID数量={}", lhScheduleIds.size());
        List<CxScheduleResult> cxScheduleResults;
        try {
            // 直接通过Mapper查询，避免Feign反序列化List<CxScheduleResult>失败
            cxScheduleResults = queryCxScheduleResultByLhScheduleIds(lhScheduleIds);
        } catch (Exception e) {
            log.error("buildCxMachineCodeExportMap: 查询成型排程结果失败，硫化排程ID数量={}，成型机台号列将为空", lhScheduleIds.size(), e);
            return Collections.emptyMap();
        }
        if (PubUtil.isEmpty(cxScheduleResults)) {
            log.warn("buildCxMachineCodeExportMap: 未查询到关联的成型排程结果，硫化排程ID数量={}，成型机台号列将为空", lhScheduleIds.size());
            return Collections.emptyMap();
        }
        log.debug("buildCxMachineCodeExportMap: 查询到成型排程结果数量={}", cxScheduleResults.size());

        Set<Long> exportIdSet = new HashSet<>(lhScheduleIds);
        Map<Long, LinkedHashSet<String>> machineCodeMap = new HashMap<>(lhScheduleIds.size());
        int skippedBlankMachineCode = 0;
        int skippedBlankLhScheduleIds = 0;
        for (CxScheduleResult cxScheduleResult : cxScheduleResults) {
            if (StringUtils.isBlank(cxScheduleResult.getCxMachineCode())) {
                skippedBlankMachineCode++;
                continue;
            }
            if (StringUtils.isBlank(cxScheduleResult.getLhScheduleIds())) {
                skippedBlankLhScheduleIds++;
                continue;
            }
            List<Long> parsedIds = parseLhScheduleIds(cxScheduleResult.getLhScheduleIds());
            if (parsedIds.isEmpty()) {
                log.warn("buildCxMachineCodeExportMap: 成型排程结果ID={}的LH_SCHEDULE_IDS='{}'解析后为空",
                        cxScheduleResult.getId(), cxScheduleResult.getLhScheduleIds());
                continue;
            }
            for (Long lhScheduleId : parsedIds) {
                if (!exportIdSet.contains(lhScheduleId)) {
                    continue;
                }
                machineCodeMap.computeIfAbsent(lhScheduleId, key -> new LinkedHashSet<>())
                        .add(cxScheduleResult.getCxMachineCode().trim());
            }
        }
        if (skippedBlankMachineCode > 0) {
            log.warn("buildCxMachineCodeExportMap: 跳过{}条成型机台号为空的成型排程结果", skippedBlankMachineCode);
        }
        if (skippedBlankLhScheduleIds > 0) {
            log.warn("buildCxMachineCodeExportMap: 跳过{}条硫化排程ID为空的成型排程结果", skippedBlankLhScheduleIds);
        }
        log.debug("buildCxMachineCodeExportMap: 最终匹配到成型机台号的硫化排程ID数量={}/{}", machineCodeMap.size(), lhScheduleIds.size());
        return machineCodeMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> String.join(";", entry.getValue()),
                        (oldValue, newValue) -> oldValue,
                        HashMap::new
                ));
    }

    /**
     * 解析成型排程结果中的硫化排程ID字符串。
     * <p>兼容逗号、中文逗号、斜杠、分号和空白分隔，避免历史数据分隔符不一致导致反查失败。</p>
     *
     * @param lhScheduleIds 硫化排程ID字符串
     * @return 硫化排程ID列表
     */
    private List<Long> parseLhScheduleIds(String lhScheduleIds) {
        if (StringUtils.isBlank(lhScheduleIds)) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<>();
        for (String token : lhScheduleIds.split("[,，/;；\\s]+")) {
            if (StringUtils.isBlank(token)) {
                continue;
            }
            try {
                result.add(Long.valueOf(token.trim()));
            } catch (NumberFormatException ignored) {
                // 历史脏数据不影响导出，无法解析的片段直接跳过。
            }
        }
        return result;
    }

    /**
     * 根据硫化排程结果ID列表查询关联的成型排程结果。
     * <p>LH_SCHEDULE_IDS 字段保存为逗号分隔字符串，使用 FIND_IN_SET 进行匹配，
     * 兼容历史数据中的中文逗号、斜杠、分号等分隔符。</p>
     * <p>直接通过本地Mapper查询，避免Feign反序列化 List&lt;CxScheduleResult&gt; 时
     * 出现"Cannot deserialize from Object value"错误。</p>
     *
     * @param lhScheduleIds 硫化排程结果ID列表
     * @return 成型排程结果列表
     */
    private List<CxScheduleResult> queryCxScheduleResultByLhScheduleIds(List<Long> lhScheduleIds) {
        if (lhScheduleIds == null || lhScheduleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> queryIds = lhScheduleIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (queryIds.isEmpty()) {
            return Collections.emptyList();
        }
        QueryWrapper<CxScheduleResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("IS_DELETE", "0");
        queryWrapper.and(wrapper -> {
            boolean first = true;
            for (Long lhScheduleId : queryIds) {
                // LH_SCHEDULE_IDS 主要保存为逗号分隔字符串，同时兼容历史数据中的中文逗号、斜杠和分号。
                // 统一转成英文逗号后再使用 FIND_IN_SET，避免 ID=1 误匹配 10、11。
                String findInSetSql = "FIND_IN_SET({0}, REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LH_SCHEDULE_IDS, '，', ','), '/', ','), '；', ','), ';', ','), ' ', ''))";
                if (first) {
                    wrapper.apply(findInSetSql, String.valueOf(lhScheduleId));
                    first = false;
                } else {
                    wrapper.or().apply(findInSetSql, String.valueOf(lhScheduleId));
                }
            }
        });
        return cxScheduleResultMapper.selectList(queryWrapper);
    }

    /**
     * 构建导出用“硫化产量今天夜班”Map。
     * <p>按用户要求，使用 T_LH_DAY_FINISH_QTY 中同工厂、同物料从当月 1 日到排程日当天的
     * DAY_FINISH_QTY 汇总值，再叠加 T_LH_SCHE_FINISH_QTY 中排程日当天同工厂、同物料的
     * CLASS1_FINISH_QTY 之和。两个来源都先一次性查出导出列表涉及的数据，再在内存中按
     * “工厂+物料”聚合，避免逐行查询数据库。</p>
     *
     * @param list 排程结果列表
     * @return 硫化产量今天夜班Map，key=工厂|物料编码
     */
    /**
     * 构建硫化产量今天夜班的Map（供导出和列表展示用）
     * <p>计算逻辑：从本月1日到最大排程日期当天的日产量（LhDayFinishQty.DAY_FINISH_QTY）
     * + 最大排程日期当天的1班完成量（LhScheFinishQty.CLASS1_FINISH_QTY）
     *
     * @param list 排程结果列表，用于提取工厂和物料查询范围
     * @return key=工厂编码|物料编码，value=今天夜班总产量BigDecimal
     */
    // scheduleDate 用于固定 T 日口径；为空时兼容旧调用，回退为列表最大排程日期。
    private Map<String, Object> buildTodayNightFinishQtyExportMap(List<LhScheduleResult> list, Date scheduleDate) {
        if (PubUtil.isEmpty(list)) {
            return Collections.emptyMap();
        }
        List<String> factoryCodes = list.stream()
                .map(LhScheduleResult::getFactoryCode)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        List<String> materialCodes = list.stream()
                .map(LhScheduleResult::getMaterialCode)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        if (factoryCodes.isEmpty() || materialCodes.isEmpty()) {
            log.warn("buildTodayNightFinishQtyExportMap: 工厂编码或物料编码为空，工厂数={}, 物料数={}", factoryCodes.size(), materialCodes.size());
            return Collections.emptyMap();
        }

        // 优先使用入参 scheduleDate，从班次日历取第一个班次的日期作为 T 日
        // 硫化产量今天夜班 = 1号到T-1日的日完成量总和 + T-2日夜班班次完成量
        Date baseScheduleDate = Objects.nonNull(scheduleDate)
                ? DateUtil.beginOfDay(scheduleDate)
                : list.stream()
                .map(LhScheduleResult::getScheduleDate)
                .filter(Objects::nonNull)
                .max(Date::compareTo)
                .map(DateUtil::beginOfDay)
                .orElse(DateUtil.beginOfDay(DateUtil.date()));
        List<LhScheduleShiftDateVO> shiftDateList = listScheduleShiftDates(baseScheduleDate);
        String firstShiftDateStr = PubUtil.isNotEmpty(shiftDateList) ? shiftDateList.get(0).getShiftDate() : null;
        Date targetScheduleDate = null;
        if (StringUtils.isNotBlank(firstShiftDateStr)) {
            // 先解析月日
            Date parsed = DateUtil.parse(firstShiftDateStr, "MM/dd");
            if (parsed != null) {
                int baseYear = DateUtil.year(baseScheduleDate);
                int baseMonth = DateUtil.month(baseScheduleDate) + 1;
                int parsedMonth = DateUtil.month(parsed) + 1;
                int parsedDay = DateUtil.dayOfMonth(parsed);
                // 跨年判断：如果解析的月份小于基准日期的月份，说明是去年
                if (parsedMonth < baseMonth) {
                    targetScheduleDate = DateUtil.parse(baseYear - 1 + "-" + firstShiftDateStr.replace("/", "-"), "yyyy-MM-dd");
                } else {
                    targetScheduleDate = DateUtil.parse(baseYear + "-" + firstShiftDateStr.replace("/", "-"), "yyyy-MM-dd");
                }
            }
        }
        if (targetScheduleDate == null) {
            targetScheduleDate = baseScheduleDate;
        }
        // 硫化产量今天夜班 = 1号到T-1日的日完成量总和 + T日class1FinishQty
        Date nextDayStart = DateUtil.offsetDay(targetScheduleDate, 1);
        Date monthStart = DateUtil.beginOfMonth(targetScheduleDate);
        log.debug("buildTodayNightFinishQtyExportMap: T日={}, T+1日={}, 月初={}, 工厂数={}, 物料数={}",
                DateUtil.formatDate(targetScheduleDate), DateUtil.formatDate(nextDayStart),
                DateUtil.formatDate(monthStart),
                factoryCodes.size(), materialCodes.size());

        // 日完成量查询：1号到T-1日
        List<LhDayFinishQty> finishQtyList = lhDayFinishQtyMapper.selectList(
                new LambdaQueryWrapper<LhDayFinishQty>()
                        .in(LhDayFinishQty::getFactoryCode, factoryCodes)
                        .in(LhDayFinishQty::getMaterialCode, materialCodes)
                        .ge(LhDayFinishQty::getFinishDate, monthStart)
                        .lt(LhDayFinishQty::getFinishDate, targetScheduleDate));
        log.debug("buildTodayNightFinishQtyExportMap: T_LH_DAY_FINISH_QTY查询结果数量={}", finishQtyList.size());
        Map<String, BigDecimal> finishQtyMap = new HashMap<>(16);
        for (LhDayFinishQty finishQty : finishQtyList) {
            if (StringUtils.isBlank(finishQty.getFactoryCode()) || StringUtils.isBlank(finishQty.getMaterialCode())) {
                continue;
            }
            BigDecimal dayFinishQty = Objects.nonNull(finishQty.getDayFinishQty())
                    ? finishQty.getDayFinishQty() : BigDecimal.ZERO;
            finishQtyMap.merge(
                    buildMaterialFactoryExportKey(finishQty.getFactoryCode(), finishQty.getMaterialCode()),
                    dayFinishQty,
                    BigDecimal::add);
        }

        // 排程完成量查询：T日class1FinishQty（夜班完成量）
        LambdaQueryWrapper<LhScheFinishQty> query = new LambdaQueryWrapper<>();
        query.in(LhScheFinishQty::getFactoryCode, factoryCodes)
                .in(LhScheFinishQty::getMaterialCode, materialCodes)
                .eq(LhScheFinishQty::getScheduleDate, targetScheduleDate);
        List<LhScheFinishQty> scheFinishQtyList = lhScheFinishQtyMapper.selectList(query);
        log.debug("buildTodayNightFinishQtyExportMap: T_LH_SCHE_FINISH_QTY查询结果数量={}", scheFinishQtyList.size());
        for (LhScheFinishQty finishQty : scheFinishQtyList) {
            if (StringUtils.isBlank(finishQty.getFactoryCode()) || StringUtils.isBlank(finishQty.getMaterialCode())) {
                continue;
            }
            BigDecimal class1FinishQty = Objects.nonNull(finishQty.getClass1FinishQty())
                    ? finishQty.getClass1FinishQty() : BigDecimal.ZERO;
            finishQtyMap.merge(
                    buildMaterialFactoryExportKey(finishQty.getFactoryCode(), finishQty.getMaterialCode()),
                    class1FinishQty,
                    BigDecimal::add);
        }
        log.debug("buildTodayNightFinishQtyExportMap: 最终聚合结果key数量={}", finishQtyMap.size());
        return new HashMap<>(finishQtyMap);
    }

    @Override
    public Map<String, Object> buildTodayNightFinishQtyMap(List<LhScheduleResult> list) {
        // 供列表接口调用，查询排程页面显示的"硫化产量今天夜班"字段
        return buildTodayNightFinishQtyExportMap(list, null);
    }

    @Override
    public Map<String, Object> buildTodayNightFinishQtyMap(List<LhScheduleResult> list, Date scheduleDate) {
        // 列表接口传入查询条件中的排程日期时，按该日期作为 T 日查询今天夜班完成量。
        return buildTodayNightFinishQtyExportMap(list, scheduleDate);
    }

    /**
     * 构建物料工厂匹配key。
     *
     * @param factoryCode  工厂编号
     * @param materialCode 物料编码
     * @return 物料工厂匹配key
     */
    private String buildMaterialFactoryExportKey(String factoryCode, String materialCode) {
        return StringUtils.defaultString(factoryCode).trim() + "|" + StringUtils.defaultString(materialCode).trim();
    }

    /**
     * 构建胶囊导出明细行key。
     *
     * @param result 排程结果
     * @return 明细行key
     */
    private String buildCapsuleExportKey(LhScheduleResult result) {
        String scheduleDay = Objects.nonNull(result.getScheduleDate()) ? DateUtil.formatDate(result.getScheduleDate()) : "";
        return buildCapsuleMachineKey(result.getFactoryCode(), result.getLhMachineCode()) + "|" + scheduleDay;
    }

    /**
     * 构建胶囊机台匹配key。
     *
     * @param factoryCode   工厂编号
     * @param lhMachineCode 硫化机台编号
     * @return 机台匹配key
     */
    private String buildCapsuleMachineKey(String factoryCode, String lhMachineCode) {
        return StringUtils.defaultString(factoryCode).trim() + "|" + StringUtils.defaultString(lhMachineCode).trim();
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

        // 首行汇总按班次横向展示：每个班的计划量、完成量都取所有明细的纵向合计。
        // 例如 class1PlanQty = 所有明细的 1 班计划量之和。
        for (int shift = 1; shift <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shift++) {
            int classPlanQty = sumPlanQtyByShift(list, shift);
            int classFinishQty = sumFinishQtyByShift(list, shift);
            row.put("class" + shift + "PlanQty", classPlanQty);
            row.put("class" + shift + "FinishQty", classFinishQty);
            totalPlanQty += classPlanQty;
        }

        // 模板最后区域有两个不同口径：
        // dailyPlanQty 对应“合计”的计划量，只统计 6、7、8 班；
        // totalFinishQty 对应“合计”的实际量，也只统计 6、7、8 班；
        // totalPlanQty 对应“总计”，统计 1~8 班的全部计划量。
        row.put("dailyPlanQty", sumPlanQtyByShift(list, 6)
                + sumPlanQtyByShift(list, 7)
                + sumPlanQtyByShift(list, 8));
        row.put("totalFinishQty", sumFinishQtyByShift(list, 6)
                + sumFinishQtyByShift(list, 7)
                + sumFinishQtyByShift(list, 8));
        row.put("totalPlanQty", totalPlanQty);
        row.put("totalDailyPlanQty", sumTotalDailyPlanQty(list));
        return row;
    }

    /**
     * 汇总明细行订单总计划数量。
     * <p>模板 CE 列使用 totalDailyPlanQty，和日计划 dailyPlanQty、总计 totalPlanQty 是不同展示口径，
     * 因此这里单独按明细上的 TOTAL_DAILY_PLAN_QTY 求和。</p>
     *
     * @param list 排程结果列表
     * @return 订单总计划数量合计
     */
    private int sumTotalDailyPlanQty(List<LhScheduleResult> list) {
        return list.stream()
                .map(LhScheduleResult::getTotalDailyPlanQty)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    /**
     * 汇总指定班次计划量
     *
     * @param list  排程结果列表
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
     * @param list  排程结果列表
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
     * @deprecated 已被 {@link #buildContinuousShiftOrderMap} + {@link #getContinuousShiftOrder} 替代，
     * 不再使用数据库字段 scheduleOrder，改为同一物料按班次连续编排
     */
    @Deprecated
    private Object buildShiftOrder(LhScheduleResult result, int shift) {
        Integer planQty = getClassPlanQty(result, shift);
        return Objects.nonNull(planQty) && planQty > 0 ? result.getScheduleOrder() : "";
    }

    /**
     * 构建同一物料下8班连续顺序值映射。
     * <p>编排规则：按物料分组 → 按班次1~8顺序遍历 → 同一SKU+机台的顺序号一旦确定，后续班次沿用不变。
     * 机台首次上机时按机台编码升序分配顺序号，后续班次新增机台从当前最大顺序号+1递增。</p>
     * <p>例如：5台机台K111/K1113/K1206/K1313/K2024排同一物料，
     * 班次1(早班)K1313和K2024有排产，按上机时间+机台编码升序 → K1313=1, K2024=2；
     * 班次2(中班)5台都有排产，K1313和K2024沿用顺序1和2，新增K111=3, K1113=4, K1206=5；
     * 后续班次中5台机台的顺序号始终保持不变。</p>
     *
     * @param sortedList 已按物料编码+机台编码排序的排程结果列表
     * @return key=物料编码|排程结果ID，value=该记录在各班次的顺序值Map（班次号→顺序值）
     */
    private Map<String, Map<Integer, Integer>> buildContinuousShiftOrderMap(List<LhScheduleResult> sortedList) {
        Map<String, Map<Integer, Integer>> resultMap = new HashMap<>(sortedList.size());
        if (PubUtil.isEmpty(sortedList)) {
            return resultMap;
        }

        // 按物料编码分组，保持排序后的顺序
        Map<String, List<LhScheduleResult>> materialGroupMap = new LinkedHashMap<>();
        for (LhScheduleResult r : sortedList) {
            String materialCode = StringUtils.defaultString(r.getMaterialCode());
            materialGroupMap.computeIfAbsent(materialCode, k -> new ArrayList<>()).add(r);
        }

        // 对每个物料组，按班次1~8遍历
        // 机台首次上机时分配顺序号，后续班次沿用不变；新增机台从当前最大顺序号+1递增
        for (Map.Entry<String, List<LhScheduleResult>> entry : materialGroupMap.entrySet()) {
            List<LhScheduleResult> groupList = entry.getValue();

            // 机台编码→已分配的顺序号，跨班次保持
            Map<String, Integer> machineOrderMap = new LinkedHashMap<>();
            // 当前已分配的最大顺序号
            int maxOrder = 0;

            for (int shift = 1; shift <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shift++) {
                // 收集当前班次有计划量的记录，按机台编码升序
                int finalShift = shift;
                List<LhScheduleResult> shiftRecords = groupList.stream()
                        .filter(r -> {
                            Integer planQty = getClassPlanQty(r, finalShift);
                            return Objects.nonNull(planQty) && planQty > 0;
                        })
                        .sorted(Comparator.comparing(r -> StringUtils.defaultString(r.getLhMachineCode()), String.CASE_INSENSITIVE_ORDER))
                        .collect(Collectors.toList());

                for (LhScheduleResult r : shiftRecords) {
                    String machineCode = StringUtils.defaultString(r.getLhMachineCode());
                    int order;
                    if (machineOrderMap.containsKey(machineCode)) {
                        // 已排上机的机台，沿用已有顺序号
                        order = machineOrderMap.get(machineCode);
                    } else {
                        // 新上机台，分配下一个顺序号
                        maxOrder++;
                        order = maxOrder;
                        machineOrderMap.put(machineCode, order);
                    }
                    String key = buildShiftOrderMapKey(r);
                    resultMap.computeIfAbsent(key, k -> new HashMap<>()).put(shift, order);
                }
            }
        }

        return resultMap;
    }

    /**
     * 查找排程结果中最后一个有计划量的班次序号。
     * 遍历班次1~8，返回最后一个有计划量（planQty > 0）的班次序号。
     * 用于同机台不同物料的收尾排序：班次序号越小表示越早收尾下机。
     *
     * @param result 排程结果
     * @return 最后一个有计划量的班次序号（1~8），所有班次均无计划量时返回0
     */
    private int findLastPlannedShift(LhScheduleResult result) {
        int lastShift = 0;
        for (int shift = 1; shift <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shift++) {
            Integer planQty = getClassPlanQty(result, shift);
            if (Objects.nonNull(planQty) && planQty > 0) {
                lastShift = shift;
            }
        }
        return lastShift;
    }

    // ==================== 导出着色规则 ====================

    /**
     * 灰底样式（交替前后标识）。
     */
    private static final ExcelStyleVo GRAY_STYLE = createGrayStyle();

    /**
     * 淡橙底样式（余量不足标识）。
     */
    private static final ExcelStyleVo ORANGE_STYLE = createOrangeStyle();

    private static ExcelStyleVo createGrayStyle() {
        ExcelStyleVo vo = new ExcelStyleVo();
        vo.setRgbColor(new ExcelStyleVo.RgbColor(0xD9, 0xD9, 0xD9));
        return vo;
    }

    private static ExcelStyleVo createOrangeStyle() {
        ExcelStyleVo vo = new ExcelStyleVo();
        vo.setRgbColor(new ExcelStyleVo.RgbColor(0xFC, 0xD5, 0xB4));
        return vo;
    }
    /**
     * 为导出明细行应用着色规则。
     *
     * <p>规则：同一机台发生收尾后，后续再次排产的规格视为收尾后的新开规格或再生产，
     * 机台/物料号/物料描述列涂灰；同时把对应收尾行的机台列涂灰。</p>
     * <p>规则 3（已移至 exportData 方法）：日计划量 ≤ 400，dailyPlanQty 列涂淡橙。</p>
     *
     * @param row       导出行数据
     * @param styleFlag 导出行样式标记
     */
    private void applyExportRowStyle(Map<String, Object> row, ExportRowStyleFlag styleFlag) {
        if (Objects.isNull(styleFlag)) {
            return;
        }
        if (styleFlag.isMachineCodeGray()) {
            row.put("style_lhMachineCode", GRAY_STYLE);
        }
        if (styleFlag.isMaterialInfoGray()) {
            row.put("style_materialCode", GRAY_STYLE);
            row.put("style_materialDesc", GRAY_STYLE);
        }
        if (isHasMorePlanQty(row)) {
            row.put("style_totalDailyPlanQty", GRAY_STYLE);
        }
    }

    /**
     * 构建导出明细行的同机台收尾后涂灰标记。
     *
     * <p>入参需使用最终导出顺序。同一机台出现收尾行时，先暂存该收尾行；
     * 后续同机台仍有计划量的行，标记为收尾后的新开规格或再生产，导出时机台/物料号/物料描述列统一涂灰；
     * 同时回标暂存收尾行的机台列涂灰。若收尾后没有后续计划量，则收尾行不涂灰。</p>
     *
     * @param sortedList 已按导出顺序排序的排程结果
     * @return 每条排程结果的导出样式标记
     */
    private static Map<LhScheduleResult, ExportRowStyleFlag> buildMachinePostCloseOutStyleFlagMap(List<LhScheduleResult> sortedList) {
        if (PubUtil.isEmpty(sortedList)) {
            return Collections.emptyMap();
        }
        Map<LhScheduleResult, ExportRowStyleFlag> styleFlagMap = new IdentityHashMap<>(sortedList.size());
        Map<String, LhScheduleResult> pendingCloseOutRowMap = new HashMap<>(sortedList.size());
        for (LhScheduleResult result : sortedList) {
            ExportRowStyleFlag styleFlag = new ExportRowStyleFlag();
            styleFlagMap.put(result, styleFlag);

            String machineCode = StringUtils.trimToEmpty(result.getLhMachineCode());
            if (StringUtils.isBlank(machineCode)) {
                continue;
            }

            LhScheduleResult pendingCloseOutRow = pendingCloseOutRowMap.get(machineCode);
            if (Objects.nonNull(pendingCloseOutRow) && hasAnyPlanQty(result)) {
                styleFlag.setMachineCodeGray(true);
                styleFlag.setMaterialInfoGray(true);
                ExportRowStyleFlag closeOutStyleFlag = styleFlagMap.get(pendingCloseOutRow);
                if (Objects.nonNull(closeOutStyleFlag)) {
                    closeOutStyleFlag.setMachineCodeGray(true);
                }
            }

            if (isCloseOutSku(result)) {
                pendingCloseOutRowMap.put(machineCode, result);
            }
        }
        return styleFlagMap;
    }

    /**
     * 判断是否为收尾行：任一班次 classXIsEnd 为收尾或试验标记。
     *
     * @param result 排程结果
     * @return true 表示该行存在收尾班次
     */
    private static boolean isCloseOutSku(LhScheduleResult result) {
        for (int shift = 1; shift <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shift++) {
            Object isEndValue = BeanUtil.getProperty(result, "class" + shift + "IsEnd");
            String isEnd = StringUtils.trimToEmpty(Objects.toString(isEndValue, ""));
            if (ApsConstant.APS_STRING_1.equals(isEnd)
                    || ApsConstant.APS_STRING_3.equals(isEnd)
                    || ApsConstant.APS_STRING_4.equals(isEnd)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 20260710+ 是否还有断点后的计划量
     *
     * @param row
     * @return
     */
    private boolean isHasMorePlanQty(Map<String, Object> row) {
        Integer totalDailyPlanQty = (Integer) row.get("totalDailyPlanQty");
        Integer monthPlanSumTotal = (Integer) row.get("monthPlanSumTotal");
        if (null == monthPlanSumTotal || monthPlanSumTotal == BigDecimal.ZERO.intValue()) {
            monthPlanSumTotal = totalDailyPlanQty;
        }
        return !monthPlanSumTotal.equals(totalDailyPlanQty);
    }

    /**
     * 判断排程结果是否存在任一有计划量班次。
     *
     * @param result 排程结果
     * @return true 表示存在计划量大于 0 的班次
     */
    private static boolean hasAnyPlanQty(LhScheduleResult result) {
        for (int shift = 1; shift <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shift++) {
            Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, shift);
            if (Objects.nonNull(planQty) && planQty > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 导出明细行样式标记。
     */
    private static class ExportRowStyleFlag {

        /**
         * 是否涂灰机台列。
         */
        private boolean machineCodeGray;

        /**
         * 是否涂灰物料号和物料描述列。
         */
        private boolean materialInfoGray;

        private boolean isMachineCodeGray() {
            return machineCodeGray;
        }

        private void setMachineCodeGray(boolean machineCodeGray) {
            this.machineCodeGray = machineCodeGray;
        }

        private boolean isMaterialInfoGray() {
            return materialInfoGray;
        }

        private void setMaterialInfoGray(boolean materialInfoGray) {
            this.materialInfoGray = materialInfoGray;
        }
    }
    /**
     * 构建顺序值映射的key。
     *
     * @param result 排程结果
     * @return key = 物料编码|排程结果ID
     */
    private String buildShiftOrderMapKey(LhScheduleResult result) {
        return StringUtils.defaultString(result.getMaterialCode()) + "|" + result.getId();
    }

    /**
     * 从连续顺序值映射中获取指定记录指定班次的顺序值。
     *
     * @param shiftOrderMap 顺序值映射
     * @param result        排程结果
     * @param shift         班次序号
     * @return 顺序值（有计划量时返回数字，无计划量时返回空字符串）
     */
    private Object getContinuousShiftOrder(Map<String, Map<Integer, Integer>> shiftOrderMap,
                                           LhScheduleResult result, int shift) {
        Integer planQty = getClassPlanQty(result, shift);
        if (Objects.isNull(planQty) || planQty <= 0) {
            return "";
        }
        String key = buildShiftOrderMapKey(result);
        Map<Integer, Integer> orderMap = shiftOrderMap.get(key);
        if (Objects.isNull(orderMap)) {
            return "";
        }
        Integer order = orderMap.get(shift);
        return Objects.nonNull(order) ? order : "";
    }

    /**
     * 构建班次维度的左右模导出值。
     * <p>前端列表的 shiftLeftRightMouldFormatter 会按班次判断是否展示左右模：
     * 如果当前班次已经超过收尾位置，或者当前班次没有计划量，则显示为空；只有当前班次确实有排产时，
     * 才展示排程结果上的左右模值。模板导出需要与列表显示保持一致，所以这里也按同一规则生成
     * class1LeftRightMould ~ class8LeftRightMould。</p>
     *
     * @param result 排程结果明细
     * @param shift  班次序号，范围 1~8
     * @return 当前班次左右模导出值
     */
    private Object buildShiftLeftRightMould(LhScheduleResult result, int shift) {
        if (Objects.isNull(result) || isShiftAfterEnding(result, shift)) {
            return "";
        }
        Integer planQty = getClassPlanQty(result, shift);
        if (Objects.isNull(planQty) || planQty <= 0) {
            return "";
        }
        return StringUtils.defaultString(result.getLeftRightMould());
    }

    /**
     * 判断指定班次是否已经超过收尾位置。
     * <p>该逻辑对齐前端 curingSchedule/index.vue 中的 isShiftAfterEnding：
     * 以各班次的 classXIsEnd 字段作为收尾标识，值为 "1" 的班次视为收尾位置，
     * 收尾位置之后的班次返回 true，导出时隐藏左右模等展示值。</p>
     *
     * @param result     排程结果明细
     * @param shiftIndex 当前班次序号
     * @return true 表示当前班次在收尾之后，需要隐藏左右模等班次展示值
     */
    private boolean isShiftAfterEnding(LhScheduleResult result, int shiftIndex) {
        if (Objects.isNull(result)) {
            return false;
        }
        // 以 classXIsEnd = "1" 的班次作为收尾位置，收尾之后的班次返回 true
        for (int shift = 1; shift <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shift++) {
            String isEnd = ShiftFieldUtil.getShiftIsEnd(result, shift);
            if ("1".equals(isEnd)) {
                return shiftIndex > shift;
            }
        }
        return false;
    }

    /**
     * 将空数量按前端空值兜底规则转换为 0。
     *
     * @param value 数量值
     * @return 非空数量，空值返回 0
     */
    private int defaultZero(Integer value) {
        return Objects.isNull(value) ? 0 : value;
    }

    /**
     * 构建班次类型
     *
     * @param result 排程结果
     * @param shift  班次序号
     * @return 班次类型
     */
    private String buildShiftType(LhScheduleResult result, int shift, Map<String, String> endTypeMap) {
        if (isShiftAfterEnding(result, shift)) {
            return "";
        }
        Integer planQty = getClassPlanQty(result, shift);
        if (Objects.isNull(planQty) || planQty <= 0) {
            return "";
        }

        // 对齐前端 curingSchedule/index.vue 的 calcShiftIsEnd：
        // 该列展示 biz_end_type 字典含义，0=正常、1=收尾；空班次和收尾后的班次不展示。
        // 直接使用排程结果中已落库的 classXIsEnd，避免导出时重新推算导致与页面/导入数据不一致。
        String isEnd = StringUtils.defaultIfBlank(ShiftFieldUtil.getShiftIsEnd(result, shift), "0");
        return endTypeMap.getOrDefault(isEnd, isEnd);
    }

    /**
     * 构建班次维度的示方类型导出值。
     * <p>模板当前列占位符是 mouldMethod，使用新字典 lh_trial_status（S=正规示方、T=量试示方、X=试验示方）。
     * 数据库字段 constructionStage 存储的是施工阶段编码（00/01/02/03），
     * 需要先转换为标记值（S/X/T），再通过字典翻译为对应的中文展示文本。
     * 收尾后的班次不展示；没有计划量的班次不展示。</p>
     *
     * @param result 排程结果明细
     * @param shift  班次序号，范围 1~8
     * @param recipeTypeMap lh_trial_status 字典映射，key=标记值(S/X/T)，value=字典标签
     * @return 当前班次示方类型展示值
     */
    /**
     * 构建模板导入行使用的统一收尾类型。
     * <p>模板首行用于反向导入，收尾类型不再按 8 个班次分别导入，因此导出时也提供统一字段。
     * 优先取主字段 isEnd；旧数据未写主字段时，回退取第一个有计划量班次的 classXIsEnd。</p>
     *
     * @param result     排程结果
     * @param endTypeMap biz_end_type 字典映射
     * @return 收尾类型显示值
     */
    private String buildUnifiedShiftType(LhScheduleResult result, Map<String, String> endTypeMap) {
        if (Objects.isNull(result)) {
            return "";
        }
        String isEnd = StringUtils.trimToEmpty(result.getIsEnd());
        if (StringUtils.isBlank(isEnd)) {
            for (int shift = 1; shift <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shift++) {
                if (defaultZero(getClassPlanQty(result, shift)) <= 0) {
                    continue;
                }
                isEnd = ShiftFieldUtil.getShiftIsEnd(result, shift);
                break;
            }
        }
        isEnd = StringUtils.defaultIfBlank(isEnd, "0");
        return endTypeMap.getOrDefault(isEnd, isEnd);
    }

    private Object buildShiftMouldMethod(LhScheduleResult result, int shift, Map<String, String> recipeTypeMap) {
        if (Objects.isNull(result) || isShiftAfterEnding(result, shift)) {
            return "";
        }
        Integer planQty = getClassPlanQty(result, shift);
        if (Objects.isNull(planQty) || planQty <= 0) {
            return "";
        }
        // 直接使用班次维度的硫化示方类型 classXLhType，避免所有班次共用 constructionStage。
        String lhType = getClassLhType(result, shift);
        if (StringUtils.isBlank(lhType)) {
            return "";
        }
        return recipeTypeMap.getOrDefault(lhType, lhType);
    }

    /**
     * 构建班次维度的产品状态展示文本。
     * <p>参照 buildShiftMouldMethod 的判定逻辑：仅当该班次在收尾班次之前且计划量大于 0 时，
     * 才返回产品状态（productStatus）对应的 lh_trial_status 字典中文描述；其余情况返回空字符串。</p>
     *
     * @param result        排程结果
     * @param shift         班次序号
     * @param recipeTypeMap lh_trial_status 字典映射
     * @return 产品状态展示文本
     */
    private Object buildShiftProductStatus(LhScheduleResult result, int shift, Map<String, String> recipeTypeMap) {
        if (Objects.isNull(result) || isShiftAfterEnding(result, shift)) {
            return "";
        }
        Integer planQty = getClassPlanQty(result, shift);
        if (Objects.isNull(planQty) || planQty <= 0) {
            return "";
        }
        // 产品状态（S/T/X）直接对应 lh_trial_status 字典值，转换为中文描述。
        String productStatus = result.getProductStatus();
        if (StringUtils.isBlank(productStatus)) {
            return "";
        }
        return recipeTypeMap.getOrDefault(productStatus, productStatus);
    }

    /**
     * 将施工阶段编码转换为 lh_trial_status 字典的展示文本。
     * <p>constructionStage 编码与 lh_trial_status 标记值的对应关系：
     * 00/0 → 无工艺（不在字典中，直接返回"无工艺"）
     * 01 → X（试验示方）
     * 02 → T（量试示方）
     * 03 → S（正规示方）
     * 先将 constructionStage 转为标记值，再从字典映射中取标签。</p>
     *
     * @param constructionStage 施工阶段字典值
     * @param recipeTypeMap lh_trial_status 字典映射
     * @return 示方类型展示文本
     */
    /**
     * 构建模板导入行使用的统一示方类型。
     * <p>模板首行用于反向导入，示方类型不再按 8 个班次分别导入，因此导出时也提供统一字段。
     * 优先取第一个有计划量班次的 classXLhType；没有班次值时回退使用 constructionStage。</p>
     *
     * @param result        排程结果
     * @param recipeTypeMap lh_trial_status 字典映射
     * @return 示方类型显示值
     */
    private String buildUnifiedLhType(LhScheduleResult result, Map<String, String> recipeTypeMap) {
        if (Objects.isNull(result)) {
            return "";
        }
        String lhType = "";
        for (int shift = 1; shift <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shift++) {
            if (defaultZero(getClassPlanQty(result, shift)) <= 0) {
                continue;
            }
            lhType = getClassLhType(result, shift);
            if (StringUtils.isNotBlank(lhType)) {
                break;
            }
        }
        if (StringUtils.isBlank(lhType)) {
            return buildLhTrialStatusName(result.getConstructionStage(), recipeTypeMap);
        }
        return recipeTypeMap.getOrDefault(lhType, lhType);
    }

    private String buildLhTrialStatusName(String constructionStage, Map<String, String> recipeTypeMap) {
        String dictValue = StringUtils.defaultIfBlank(constructionStage, "00");
        String markFlag = convertConstructionStageToMarkFlag(dictValue);
        if ("00".equals(markFlag)) {
            return "无工艺";
        }
        return recipeTypeMap.getOrDefault(markFlag, markFlag);
    }

    /**
     * 将施工阶段编码转换为 lh_trial_status 字典的标记值。
     * <p>映射关系：00/0→00（无工艺），01→X（试验），02→T（量试），03→S（正规）</p>
     *
     * @param constructionStage 施工阶段编码
     * @return 标记值（S/X/T/00）
     */
    private String convertConstructionStageToMarkFlag(String constructionStage) {
        if ("0".equals(constructionStage) || "00".equals(constructionStage)) {
            return "00";
        }
        if ("01".equals(constructionStage)) {
            return "X";
        }
        if ("02".equals(constructionStage)) {
            return "T";
        }
        if ("03".equals(constructionStage)) {
            return "S";
        }
        return constructionStage;
    }

    /**
     * 加载硫化示方类型字典（lh_trial_status），构建标记值→标签的映射。
     * <p>字典值：S=正规示方、T=量试示方、X=试验示方</p>
     *
     * @return 标记值→标签映射
     */
    /**
     * 获取指定班次的硫化示方类型。
     *
     * @param result 排程结果
     * @param shift  班次序号
     * @return class1LhType~class8LhType 的字段值
     */
    private String getClassLhType(LhScheduleResult result, int shift) {
        if (Objects.isNull(result)) {
            return "";
        }
        switch (shift) {
            case 1:
                return result.getClass1LhType();
            case 2:
                return result.getClass2LhType();
            case 3:
                return result.getClass3LhType();
            case 4:
                return result.getClass4LhType();
            case 5:
                return result.getClass5LhType();
            case 6:
                return result.getClass6LhType();
            case 7:
                return result.getClass7LhType();
            case 8:
                return result.getClass8LhType();
            default:
                return "";
        }
    }

    /**
     * 加载收尾类型字典（biz_end_type），构建字典值→标签的映射。
     *
     * @return 字典值→标签映射
     */
    private Map<String, String> loadBizEndTypeDictMap() {
        List<SysDictData> dictList = sysDictDataCacheService.getType("biz_end_type");
        if (dictList == null || dictList.isEmpty()) {
            Map<String, String> defaultMap = new HashMap<>(4);
            defaultMap.put("0", "正常");
            defaultMap.put("1", "收尾");
            return defaultMap;
        }
        return dictList.stream()
                .filter(d -> StringUtils.isNotEmpty(d.getDictValue()))
                .collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel,
                        (a, b) -> a, LinkedHashMap::new));
    }

    private Map<String, String> loadLhTrialStatusDictMap() {
        List<SysDictData> dictList = sysDictDataCacheService.getType("lh_trial_status");
        if (dictList == null || dictList.isEmpty()) {
            return Collections.emptyMap();
        }
        return dictList.stream()
                .filter(d -> StringUtils.isNotEmpty(d.getDictValue()))
                .collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel,
                        (a, b) -> a, LinkedHashMap::new));
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

    @Override
    public String[] getQueryFormulas() {
        return new String[]{
                "embryoCode,structureName,mainMaterialDesc->getcolsvaluewithcondition(T_MDM_MATERIAL_INFO, [EMBRYO_CODE,STRUCTURE_NAME,EMBRYO_DESC], MATERIAL_CODE, materialCode, IS_DELETE=0)"
        };
    }

}

package com.zlt.aps.gsq.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.engine.service.FactoryService;
import com.zlt.aps.gsq.api.domain.dto.GsqChangeMachineDTO;
import com.zlt.aps.gsq.api.domain.dto.GsqInsertOrderDTO;
import com.zlt.aps.gsq.api.domain.entity.GsqDispatcherLog;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqScheduleResult;
import com.zlt.aps.gsq.api.domain.entity.GsqScheduleResultIssue;
import com.zlt.aps.gsq.api.domain.vo.GsqInsertTaskRequestVo;
import com.zlt.aps.gsq.api.domain.vo.GsqOperationRequestSnapshot;
import com.zlt.aps.gsq.api.domain.vo.GsqScheduleShiftDateVO;
import com.zlt.aps.gsq.constant.GsqScheduleConstants;
import com.zlt.aps.gsq.domain.GsqAutoScheduleTask;
import com.zlt.aps.gsq.engine.service.GsqEngineService;
import com.zlt.aps.gsq.engine.vo.GsqScheduleBaseInfoVo;
import com.zlt.aps.gsq.enums.GsqAutoScheduleTaskStatusEnum;
import com.zlt.aps.gsq.enums.GsqBackgroundTaskTypeEnum;
import com.zlt.aps.gsq.mapper.GsqScheduleResultMapper;
import com.zlt.aps.gsq.service.GsqBackgroundTaskService;
import com.zlt.aps.gsq.service.GsqDispatcherLogService;
import com.zlt.aps.gsq.service.GsqMachineInfoService;
import com.zlt.aps.gsq.service.GsqOperationAsyncExecutor;
import com.zlt.aps.gsq.engine.vo.GsqRollingUpdateResult;
import com.zlt.aps.gsq.service.IGsqRollingUpdateService;
import com.zlt.aps.gsq.service.IGsqScheduleResultService;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.tq.api.domain.entity.TqScheduleResult;
import com.zlt.aps.tq.api.service.ITqScheduleResultService;
import com.zlt.bill.common.service.AbstractDocService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 钢丝圈排程结果Service实现类
 *
 * <p>6班次制：
 * <ul>
 *   <li>1班：D日中班(16:00-24:00)</li>
 *   <li>2班：D+1日夜班(00:00-08:00)</li>
 *   <li>3班：D+1日早班(08:00-16:00)</li>
 *   <li>4班：D+1日中班(16:00-24:00)</li>
 *   <li>5班：D+2日夜班(00:00-08:00)</li>
 *   <li>6班：D+2日早班(08:00-16:00)</li>
 * </ul>
 * D = 排程日期 - 2（即今天），D+1 = 排程日期
 *
 * <p>钢丝圈排程时需查胎圈排程结果数据回填到 TQ_CLASS1~6_PLAN 字段。</p>
 *
 * @author APS
 */
@Slf4j
@Service
public class GsqScheduleResultServiceImpl extends AbstractDocService<GsqScheduleResult> implements IGsqScheduleResultService {

    /**
     * 班次字段名模板常量（遵循动态字段访问规范，配合 String.format 使用）。
     * 用于动态访问 class1~6PlanQty/FinishQty/Sequence/Analysis 等批量字段。
     */
    private static final String CLASS_PLAN_QTY_FIELD_TEMPLATE = "class%dPlanQty";
    private static final String CLASS_FINISH_QTY_FIELD_TEMPLATE = "class%dFinishQty";
    private static final String CLASS_SEQUENCE_FIELD_TEMPLATE = "class%dSequence";
    private static final String CLASS_ANALYSIS_FIELD_TEMPLATE = "class%dAnalysis";

    @Autowired
    private GsqScheduleResultMapper gsqScheduleResultMapper;

    @Autowired
    private IMesItfService mesItfService;

    @Autowired
    private FactoryService factoryService;

    /**
     * 钢丝圈调度员排程操作日志服务（用于记录6班次制操作日志）
     */
    @Autowired
    private GsqDispatcherLogService gsqDispatcherLogService;

    /**
     * 钢丝圈机台信息Feign服务（用于校验新机台是否存在且启用）
     */
    @Autowired
    private GsqMachineInfoService gsqMachineInfoService;

    /**
     * 钢丝圈排程引擎服务（用于自动排程）
     */
    @Autowired
    private GsqEngineService gsqEngineService;

    /**
     * 胎圈排程结果Feign服务（用于查询胎圈排程结果，回填 TQ_CLASS1~6_PLAN 字段）
     */
    @Autowired
    private ITqScheduleResultService tqScheduleResultService;

    /**
     * Redisson分布式锁客户端（用于发布排程时加锁，避免并发下发MES）
     */
    @Autowired
    private RedissonClient redissonClient;

    /**
     * 钢丝圈排程滚动更新服务（用于插单/调量/转机台/删除后触发同班次内时间重算）
     */
    @Resource
    private IGsqRollingUpdateService gsqRollingUpdateService;

    /**
     * 钢丝圈人工滚动应用服务（走任务链路径的插单/调量/转机台/删除统一入口）
     */
    @Autowired
    private GsqManualInsertRollingService gsqManualInsertRollingService;

    @Override
    public String getDocTypeCode() {
        return "GSQ_SCHEDULE_RESULT";
    }

    /**
     * 自动排程
     * 委托给 GsqEngineService.autoGsqSchedule 执行自动排程
     *
     * @param queryVO 排程参数（含排程日期、分厂编码）
     * @return 排程结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult autoPlan(GsqScheduleResult queryVO) {
        if (queryVO == null || queryVO.getScheduleDateQuery() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.gsq.schedule.scheduleDateEmpty"));
        }
        String scheduleDateStr = DateUtil.formatDate(queryVO.getScheduleDateQuery());
        String factoryCode = StringUtils.isBlank(queryVO.getFactoryCode()) ? factoryService.getFactoryCode() : queryVO.getFactoryCode();
        log.info("钢丝圈自动排程开始，排程日期：{}，分厂：{}", scheduleDateStr, factoryCode);
        try {
            gsqEngineService.autoGsqSchedule(scheduleDateStr, factoryCode);
            log.info("钢丝圈自动排程成功，排程日期：{}，分厂：{}", scheduleDateStr, factoryCode);
            return AjaxResult.success(I18nUtil.getMessage("ui.data.column.gsqScheduleResult.autoPlanSuccess"));
        } catch (Exception e) {
            log.error("钢丝圈自动排程失败，排程日期：" + scheduleDateStr, e);
            return AjaxResult.error(MessageFormat.format(I18nUtil.getMessage("ui.gsq.schedule.autoPlanFail"), e.getMessage()));
        }
    }

    // ==================== 插单 ====================

    /**
     * 插单前校验
     * 校验规则：
     * 1. 排程日期不能为空，且需在生产周期内
     * 2. 钢丝圈代码不能为空，施工必须存在（实时提示"钢丝圈规格有误"）
     * 3. 机台编号不能为空
     * 4. 6个班次中至少有一个班次的计划量有值（夜班、中班、早班至少一个有效）
     * 5. 有计划量的班次，顺序也必须有值；反之亦然（双向关联校验）
     * 6. 只能往当前班次或后续班次插单，禁止向历史班次插单
     * 7. 插单只能加到第二个在产规格之后
     * 8. 同一排程日期、机台、钢丝圈不允许重复插单（唯一性校验）
     *
     * @param dto 插单数据
     * @return 校验结果
     */
    @Override
    public AjaxResult validateInsertOrder(GsqInsertOrderDTO dto) {
        // 1. 排程日期校验
        if (dto.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.gsq.schedule.scheduleDateEmpty"));
        }

        // 2. 钢丝圈代码校验
        if (ObjectUtils.isEmpty(dto.getSteelRingCode())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.gsq.schedule.steelRingCodeEmpty"));
        }
        // 校验施工是否存在（查询施工表 T_PRODUCT_CONSTRUCTION_INFO）
        // 规格校验：若施工数据不存在，实时提示"钢丝圈规格有误"
        List<GsqScheduleBaseInfoVo> baseInfoList = gsqEngineService.listGsqScheduleBaseInfo(
                Collections.singletonList(dto.getSteelRingCode()));
        if (CollectionUtils.isEmpty(baseInfoList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.gsq.schedule.constructionNotFound"));
        }

        // 3. 机台编号校验
        if (ObjectUtils.isEmpty(dto.getMachineCode())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.gsq.schedule.machineCodeEmpty"));
        }

        // 4. 至少一个班次有计划量（夜班、中班、早班三个班次中至少有一个班次的计划量必须填写有效值）
        boolean hasAnyPlanQty = false;
        for (int i = 1; i <= 6; i++) {
            Integer planQty = getPlanQtyByClassIndex(dto, i);
            if (planQty != null && planQty > 0) {
                hasAnyPlanQty = true;
                break;
            }
        }
        if (!hasAnyPlanQty) {
            return AjaxResult.error(I18nUtil.getMessage("ui.gsq.schedule.atLeastOneShiftPlan"));
        }

        // 5. 有计划量的班次顺序必须有值，反之亦然（双向关联性校验）
        for (int i = 1; i <= 6; i++) {
            Integer planQty = getPlanQtyByClassIndex(dto, i);
            Integer sequence = getSequenceByClassIndex(dto, i);
            boolean hasPlanQty = planQty != null && planQty > 0;
            boolean hasSequence = sequence != null && sequence > 0;
            if (hasPlanQty && !hasSequence) {
                return AjaxResult.error(MessageFormat.format(I18nUtil.getMessage("ui.gsq.schedule.shiftPlanAndSeqMismatch"), String.valueOf(i)));
            }
            if (hasSequence && !hasPlanQty) {
                return AjaxResult.error(MessageFormat.format(I18nUtil.getMessage("ui.gsq.schedule.shiftSeqAndPlanMismatch"), String.valueOf(i)));
            }
        }

        // 6. 只能往当前班次或后续班次插单，禁止向历史班次插单
        // 示例：早上8点插单时，仅允许插入当天早班、中班以及明天之后的所有日期班次，禁止插入夜班之前的班次
        int currentShiftIndex = resolveCurrentShiftIndex(dto.getScheduleDate());
        for (int i = 1; i < currentShiftIndex; i++) {
            Integer planQty = getPlanQtyByClassIndex(dto, i);
            if (planQty != null && planQty > 0) {
                return AjaxResult.error(MessageFormat.format(I18nUtil.getMessage("ui.gsq.schedule.cannotInsertHistoryShift"), String.valueOf(currentShiftIndex)));
            }
        }

        // 7. 插单只能加到第二个在产规格之后
        LambdaQueryWrapper<GsqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GsqScheduleResult::getScheduleDate, dto.getScheduleDate());
        wrapper.eq(GsqScheduleResult::getMachineCode, dto.getMachineCode());
        wrapper.eq(GsqScheduleResult::getIsDelete, 0);
        List<GsqScheduleResult> existingList = gsqScheduleResultMapper.selectList(wrapper);
        // 按各班次中最小顺序号升序排序
        existingList.sort(Comparator.comparingInt(this::getMinSequenceOfRecord));
        if (existingList.size() >= 2) {
            int secondSpecMinSeq = getMinSequenceFromSecondSpec(existingList);
            for (int i = 1; i <= 6; i++) {
                Integer sequence = getSequenceByClassIndex(dto, i);
                if (sequence != null && sequence < secondSpecMinSeq) {
                    return AjaxResult.error(MessageFormat.format(I18nUtil.getMessage("ui.gsq.schedule.insertAfterSecondSpec"), String.valueOf(secondSpecMinSeq)));
                }
            }
        }

        // 8. 唯一性校验：同一排程日期、机台、钢丝圈不允许重复
        GsqScheduleResult uniqueCheck = new GsqScheduleResult();
        uniqueCheck.setScheduleDate(dto.getScheduleDate());
        uniqueCheck.setSteelRingCode(dto.getSteelRingCode());
        uniqueCheck.setMachineCode(dto.getMachineCode());
        if (UserConstants.NOT_UNIQUE.equals(checkUnique(uniqueCheck))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.gsq.schedule.duplicateRecord"));
        }

        return AjaxResult.success(I18nUtil.getMessage("ui.data.column.gsqScheduleResult.validatePass"));
    }

    /**
     * 插单
     * 业务流程：
     * 1. 调用 validateInsertOrder 执行前置校验
     * 2. 构建排程记录实体，填充6个班次字段（顺序/计划量/原因分析）
     * 3. 数据来源固定为"插单"（DATA_SOURCE=1），发布状态默认"未发布"（IS_RELEASE=0）
     * 4. 生成批次号、工单号（复用自动排程口径）
     * 5. 回填施工字段（英寸 proSize、缠绕盘代码等），从施工表获取
     * 6. 回填胎圈1~6班消耗量到 TQ_CLASS1~6_PLAN 字段
     * 7. 插入数据库
     * 8. 触发滚动更新（待 IGsqRollingUpdateService 实现后启用）
     * 9. 记录调度员操作日志（6班次制）
     *
     * @param dto 插单数据
     * @return 结果
     */
    @Deprecated
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult insertOrder(GsqInsertOrderDTO dto) {
        // 1. 先执行校验
        AjaxResult validateResult = validateInsertOrder(dto);
        if (!validateResult.get(AjaxResult.CODE_TAG).equals(200)) {
            return validateResult;
        }

        // 2. 构建排程记录实体
        GsqScheduleResult entity = new GsqScheduleResult();
        entity.setScheduleDate(dto.getScheduleDate());
        entity.setSteelRingCode(dto.getSteelRingCode());
        entity.setTwiningDiscCode(dto.getTwiningDiscCode());
        entity.setMachineCode(dto.getMachineCode());
        // 数据来源：1-插单
        entity.setDataSource("1");
        // 发布状态：0-未发布
        entity.setIsRelease("0");

        // 3. 填充6个班次字段（顺序/计划量/原因分析）
        entity.setClass1PlanQty(dto.getClass1PlanQty());
        entity.setClass1Sequence(dto.getClass1Sequence());
        entity.setClass1Analysis(dto.getClass1Analysis());
        entity.setClass2PlanQty(dto.getClass2PlanQty());
        entity.setClass2Sequence(dto.getClass2Sequence());
        entity.setClass2Analysis(dto.getClass2Analysis());
        entity.setClass3PlanQty(dto.getClass3PlanQty());
        entity.setClass3Sequence(dto.getClass3Sequence());
        entity.setClass3Analysis(dto.getClass3Analysis());
        entity.setClass4PlanQty(dto.getClass4PlanQty());
        entity.setClass4Sequence(dto.getClass4Sequence());
        entity.setClass4Analysis(dto.getClass4Analysis());
        entity.setClass5PlanQty(dto.getClass5PlanQty());
        entity.setClass5Sequence(dto.getClass5Sequence());
        entity.setClass5Analysis(dto.getClass5Analysis());
        entity.setClass6PlanQty(dto.getClass6PlanQty());
        entity.setClass6Sequence(dto.getClass6Sequence());
        entity.setClass6Analysis(dto.getClass6Analysis());

        entity.setRemark(dto.getRemark());

        // 4. 生成批次号、工单号（复用自动排程口径，规则一致）
        String scheduleDateStr = DateUtil.formatDate(dto.getScheduleDate());
        String[] batchAndOrder = gsqEngineService.generateBatchNoAndOrderNo(scheduleDateStr);
        entity.setBatchNo(batchAndOrder[0]);
        entity.setOrderNo(batchAndOrder[1]);

        // 5. 回填施工字段（英寸 proSize），从施工表 T_PRODUCT_CONSTRUCTION_INFO 获取
        List<GsqScheduleBaseInfoVo> baseInfoList = gsqEngineService.listGsqScheduleBaseInfo(
                Collections.singletonList(dto.getSteelRingCode()));
        if (CollectionUtils.isNotEmpty(baseInfoList)) {
            GsqScheduleBaseInfoVo baseInfo = baseInfoList.get(0);
            // 英寸字段回填（用户输入规格时施工字段实时反显）
            entity.setProSize(baseInfo.getProSize());
        }

        // 6. 回填胎圈排程结果数据到 TQ_CLASS1~6_PLAN 字段
        fillTqPlanQty(Collections.singletonList(entity));

        // 7. 插入数据库
        gsqScheduleResultMapper.insert(entity);

        // 8. 滚动更新：调用标准化插单触发入口，自动处理新增任务后续节点顺序+1与时间重算
        try {
            gsqRollingUpdateService.triggerByInsertOrder(
                    entity.getId(), entity.getScheduleDate(),
                    entity.getMachineCode(), entity.getSteelRingCode());
        } catch (Exception e) {
            // 滚动更新失败不影响插单主操作，仅记录日志
            log.error("钢丝圈插单后触发滚动更新异常，sourceId：{}，原因：{}", entity.getId(), e.getMessage(), e);
        }

        // 9. 记录调度日志（6班次制，操作类型：2-插单，无操作前数据）
        recordDispatcherLog(ApsConstant.DISPATCHER_OPER_INSERT_ORDER, entity, null, entity);

        log.info("钢丝圈排程插单成功，排程日期：{}，钢丝圈代码：{}，机台：{}",
                dto.getScheduleDate(), dto.getSteelRingCode(), dto.getMachineCode());

        return AjaxResult.success(I18nUtil.getMessage("ui.data.column.gsqScheduleResult.insertOrderSuccess"));
    }

    // ==================== 转机台 ====================

    /**
     * 转机台前校验
     *
     * @param dto 转机台数据
     * @return 校验结果
     */
    @Override
    public AjaxResult validateChangeMachine(GsqChangeMachineDTO dto) {
        if (dto.getId() == null) {
            return AjaxResult.error(MessageFormat.format(I18nUtil.getMessage("ui.gsq.schedule.pleaseSelectRecord"), I18nUtil.getMessage("ui.data.column.gsqScheduleResult.changeMachineTitle")));
        }
        if (ObjectUtils.isEmpty(dto.getNewMachineCode())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.gsq.schedule.newMachineCannotBeEmpty"));
        }
        if (dto.getNewMachineCode().equals(dto.getOldMachineCode())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.gsq.schedule.sameMachineNotAllowed"));
        }

        // 校验排程记录是否存在
        GsqScheduleResult record = gsqScheduleResultMapper.selectById(dto.getId());
        if (record == null || Objects.equals(record.getIsDelete(), 1)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gsqScheduleResult.recordNotFound"));
        }

        // 校验新机台是否在钢丝圈机台管理中存在且启用
        GsqMachineInfo queryMachine = new GsqMachineInfo();
        queryMachine.setMachineCode(dto.getNewMachineCode());
        List<GsqMachineInfo> machineList = gsqMachineInfoService.listMachineInfo(queryMachine);
        if (CollectionUtils.isEmpty(machineList)) {
            return AjaxResult.error(MessageFormat.format(I18nUtil.getMessage("ui.gsq.schedule.newMachineNotFound"), dto.getNewMachineCode()));
        }

        return AjaxResult.success(I18nUtil.getMessage("ui.data.column.gsqScheduleResult.validatePass"));
    }

    /**
     * 转机台
     *
     * @param dto 转机台数据
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult changeMachine(GsqChangeMachineDTO dto) {
        // 先执行校验
        AjaxResult validateResult = validateChangeMachine(dto);
        if (!validateResult.get(AjaxResult.CODE_TAG).equals(200)) {
            return validateResult;
        }

        // 查询原记录
        GsqScheduleResult record = gsqScheduleResultMapper.selectById(dto.getId());
        String oldMachineCode = record.getMachineCode();

        // 更新机台编号
        LambdaUpdateWrapper<GsqScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(GsqScheduleResult::getId, dto.getId())
                .set(GsqScheduleResult::getMachineCode, dto.getNewMachineCode());

        // 如果原排程已发布成功，更新发布状态为待发布
        if (ApsConstant.IS_RELEASE.equals(record.getIsRelease())) {
            updateWrapper.set(GsqScheduleResult::getIsRelease, ApsConstant.WAIT_RELEASING);
        }

        gsqScheduleResultMapper.update(null, updateWrapper);

        // 滚动更新：原机台和新机台都需要重新计算同班次内时间
        triggerRollingUpdateForAllShifts("2", dto.getId(), record);
        GsqScheduleResult newMachineRecord = new GsqScheduleResult();
        BeanUtil.copyProperties(record, newMachineRecord);
        newMachineRecord.setMachineCode(dto.getNewMachineCode());
        triggerRollingUpdateForAllShifts("2", dto.getId(), newMachineRecord);

        // 记录调度日志（6班次制，操作类型：0-转机台，操作前=原机台记录，操作后=新机台记录）
        // newMachineRecord已在上方构建
        recordDispatcherLog(ApsConstant.DISPATCHER_OPER_MACHINE, record, record, newMachineRecord);

        log.info("钢丝圈排程转机台成功，id：{}，原机台：{}，新机台：{}",
                dto.getId(), oldMachineCode, dto.getNewMachineCode());

        return AjaxResult.success(I18nUtil.getMessage("ui.data.column.gsqScheduleResult.changeMachineSuccess"));
    }

    // ==================== 调量 ====================

    /**
     * 调量前校验
     * 校验规则：
     * 1. 排程记录必须存在且未删除
     * 2. 至少有一个班次的计划量被修改
     * 3. 计划量不能小于0
     * 4. 历史班次不允许修改计划量（根据当前时间和排程日期判断）
     * 5. 非历史班次的计划量不能小于完成量
     *
     * @param entity 调量数据
     * @return 校验结果
     */
    @Override
    public AjaxResult validateChangeQty(GsqScheduleResult entity) {
        if (entity == null || entity.getId() == null) {
            return AjaxResult.error(MessageFormat.format(I18nUtil.getMessage("ui.gsq.schedule.pleaseSelectRecord"), I18nUtil.getMessage("ui.data.column.gsqScheduleResult.changeQtyTitle")));
        }

        GsqScheduleResult record = gsqScheduleResultMapper.selectById(entity.getId());
        if (record == null || Objects.equals(record.getIsDelete(), 1)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gsqScheduleResult.recordNotFound"));
        }

        Date now = new Date();
        boolean hasAdjustField = false;
        List<String> errorMessages = new ArrayList<>();

        for (int shiftIndex = 1; shiftIndex <= 6; shiftIndex++) {
            // 遵循动态字段访问规范：通过字段名模板动态读取班次计划量
            Integer newPlanQty = this.getPlanQtyByShiftIndex(entity, shiftIndex);
            Integer oldPlanQty = this.getPlanQtyByShiftIndex(record, shiftIndex);

            // 只检查被修改的班次
            if (newPlanQty == null || Objects.equals(newPlanQty, oldPlanQty)) {
                continue;
            }

            hasAdjustField = true;

            // 规则3：计划量不能小于0
            if (newPlanQty < 0) {
                errorMessages.add(String.format(
                        I18nUtil.getMessage("ui.gsq.scheduleResult.changeQty.planQtyLessThanZero"), shiftIndex));
                continue;
            }

            // 判断是否为历史班次
            boolean historyShift = this.isHistoryShift(record, shiftIndex, now);

            if (historyShift) {
                // 规则4：历史班次不允许修改
                errorMessages.add(String.format(
                        I18nUtil.getMessage("ui.gsq.scheduleResult.changeQty.historyShiftForbidden"), shiftIndex));
            } else {
                // 规则5：非历史班次计划量不能小于完成量
                Integer finishQty = this.getFinishQtyByShiftIndex(record, shiftIndex);
                if (finishQty != null && finishQty > 0 && newPlanQty < finishQty) {
                    errorMessages.add(String.format(
                            I18nUtil.getMessage("ui.gsq.scheduleResult.changeQty.planQtyLessThanFinish"),
                            shiftIndex, finishQty));
                }
            }
        }

        if (!hasAdjustField) {
            errorMessages.add(I18nUtil.getMessage("ui.gsq.scheduleResult.changeQty.noAdjustField"));
        }

        if (!errorMessages.isEmpty()) {
            return AjaxResult.error(String.join(
                    I18nUtil.getMessage("ui.gsq.scheduleResult.changeQty.errorSeparator"), errorMessages));
        }

        return AjaxResult.success(I18nUtil.getMessage("ui.data.column.gsqScheduleResult.validatePass"));
    }

    /**
     * 调量
     * 业务逻辑：
     * 1. 前置校验
     * 2. 更新各班次计划量和原因分析
     * 3. 状态更新：检查原排程是否有成功发布给MES的记录
     *    - 若有成功发布记录（IS_RELEASE=1），将发布状态更新为"待发布"（需重新下发MES）
     *    - 若无发布记录或发布未成功，保持原状态不变
     * 4. 保存操作添加事务处理，确保数据一致性
     * 5. 调量保存成功后，触发滚动更新机制（更新当前调量排程及后续所有排产记录）
     * 6. 记录调度员操作日志（6班次制）
     *
     * @param entity 调量数据
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult changeQty(GsqScheduleResult entity) {
        // 1. 前置校验
        AjaxResult validateResult = this.validateChangeQty(entity);
        if (!validateResult.get(AjaxResult.CODE_TAG).equals(200)) {
            return validateResult;
        }

        // 2. 查询原记录
        GsqScheduleResult record = gsqScheduleResultMapper.selectById(entity.getId());
        if (record == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gsqScheduleResult.recordNotFound"));
        }

        // 3. 构建更新实体（携带需要 set 的非 null 字段）和更新条件 wrapper
        // 遵循动态字段访问规范：班次字段通过 setFieldValueByFieldName 设置到 updateEntity，
        // 由 MyBatis-Plus 自动生成 set 子句；公共字段（isRelease/remark）仍用 LambdaUpdateWrapper.set。
        GsqScheduleResult updateEntity = new GsqScheduleResult();
        LambdaUpdateWrapper<GsqScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(GsqScheduleResult::getId, entity.getId());

        boolean hasChange = false;

        // 4. 更新各班次计划量和原因分析（动态字段访问）
        for (int shiftIndex = 1; shiftIndex <= 6; shiftIndex++) {
            Integer newPlanQty = this.getPlanQtyByShiftIndex(entity, shiftIndex);
            Integer oldPlanQty = this.getPlanQtyByShiftIndex(record, shiftIndex);
            String newAnalysis = this.getAnalysisByShiftIndex(entity, shiftIndex);
            String oldAnalysis = this.getAnalysisByShiftIndex(record, shiftIndex);

            // 更新被修改的计划量
            if (newPlanQty != null && !Objects.equals(newPlanQty, oldPlanQty)) {
                this.setPlanQtyToUpdateEntity(updateEntity, shiftIndex, newPlanQty);
                hasChange = true;
            }

            // 更新原因分析（非空且与原值不同时更新）
            if (newAnalysis != null && !newAnalysis.equals(oldAnalysis)) {
                this.setAnalysisToUpdateEntity(updateEntity, shiftIndex, newAnalysis);
                hasChange = true;
            }
        }

        if (!hasChange) {
            return AjaxResult.error(I18nUtil.getMessage("ui.gsq.schedule.noChangesToSave"));
        }

        // 5. 更新备注
        if (entity.getRemark() != null && !entity.getRemark().equals(record.getRemark())) {
            updateWrapper.set(GsqScheduleResult::getRemark, entity.getRemark());
        }

        // 6. 状态更新：如果原排程已发布成功（IS_RELEASE=1），更新为待发布（需重新下发MES）
        //    若无发布记录或发布未成功，保持原状态不变
        if (ApsConstant.IS_RELEASE.equals(record.getIsRelease())) {
            updateWrapper.set(GsqScheduleResult::getIsRelease, ApsConstant.WAIT_RELEASING);
        }

        // 7. 执行更新（updateEntity 携带班次字段的 set 子句，wrapper 携带公共字段和 where 条件）
        gsqScheduleResultMapper.update(updateEntity, updateWrapper);

        log.info("钢丝圈排程调量成功，id：{}，钢丝圈代码：{}，机台：{}",
                entity.getId(), record.getSteelRingCode(), record.getMachineCode());

        // 8. 滚动更新：对每个被修改的班次执行同班次内时间重算（triggerType="3" 表示调量触发）
        //    严格按照【滚动更新后续排程】算法实现更新逻辑，确保更新过程中数据准确性和完整性
        this.triggerRollingUpdateForAllShifts("3", entity.getId(), record);

        // 9. 记录调度日志（6班次制，操作类型：1-调量，操作前=原记录，操作后=更新后记录）
        GsqScheduleResult afterRecord = gsqScheduleResultMapper.selectById(entity.getId());
        this.recordDispatcherLog(ApsConstant.DISPATCHER_OPER_PLAN, record, record, afterRecord);

        return AjaxResult.success(I18nUtil.getMessage("ui.data.column.gsqScheduleResult.changeQtySuccess"));
    }

    // ==================== 逻辑删除 ====================

    /**
     * 逻辑删除前校验
     * 校验规则：
     * 1. 记录必须存在且未删除
     * 2. 发布成功次数必须等于0（已发布成功的计划不允许删除，只能调量）
     * 3. 必须未发送给MES（mesId为空；已发送给MES的计划不允许删除，只能调量）
     *
     * @param ids 需要校验的记录ID列表
     * @return 校验结果（通过返回success，失败返回error及不允许删除的原因）
     */
    @Override
    public AjaxResult validateLogicDelete(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gsqScheduleResult.noSelectRow"));
        }

        for (Long id : ids) {
            GsqScheduleResult record = gsqScheduleResultMapper.selectById(id);
            // 1. 记录必须存在且未删除（框架自动过滤已删除记录，selectById返回null视为不存在）
            if (record == null) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gsqScheduleResult.recordNotFound"));
            }
            // 2. 发布成功次数必须等于0（兼容历史NULL数据：NULL视为0）
            Integer publishSuccessCount = record.getPublishSuccessCount();
            if (publishSuccessCount != null && publishSuccessCount > 0) {
                return AjaxResult.error(MessageFormat.format(I18nUtil.getMessage(
                                "ui.data.column.gsqScheduleResult.deleteFailedPublished"),
                        record.getSteelRingCode()));
            }
            // 3. 必须未发送给MES（mesId为空）
            if (record.getMesId() != null) {
                return AjaxResult.error(MessageFormat.format(I18nUtil.getMessage(
                                "ui.data.column.gsqScheduleResult.deleteFailedMesSent"),
                        record.getSteelRingCode()));
            }
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.data.column.gsqScheduleResult.validatePass"));
    }

    /**
     * 逻辑删除排程记录
     * 只能删除发布成功次数等于0且未发送给MES的计划
     * 业务流程：
     * 1. 执行 validateLogicDelete 前置校验
     * 2. 逻辑删除：更新 is_delete = 1
     * 3. 记录调度员操作日志（6班次制，操作类型：3-删除）
     * 4. 滚动更新：待 IGsqRollingUpdateService 实现后启用
     *
     * @param ids 需要删除的记录ID列表
     * @return 结果
     */
    @Deprecated
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult logicDeleteByIds(List<Long> ids) {
        // 1. 先执行前置校验（发布成功次数=0 且未发送给MES）
        AjaxResult validateResult = validateLogicDelete(ids);
        if (!validateResult.get(AjaxResult.CODE_TAG).equals(200)) {
            return validateResult;
        }

        for (Long id : ids) {
            GsqScheduleResult record = gsqScheduleResultMapper.selectById(id);
            if (record == null) {
                continue;
            }

            // 2. 逻辑删除：更新 is_delete = 1
            LambdaUpdateWrapper<GsqScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(GsqScheduleResult::getId, id)
                    .set(GsqScheduleResult::getIsDelete, 1);
            gsqScheduleResultMapper.update(null, updateWrapper);

            // 3. 滚动更新：调用标准化删除触发入口，自动处理删除任务后续节点顺序-1与时间重算
            try {
                gsqRollingUpdateService.triggerByDelete(
                        id, record.getScheduleDate(),
                        record.getMachineCode(), record.getSteelRingCode());
            } catch (Exception e) {
                // 滚动更新失败不影响删除主操作，仅记录日志
                log.error("钢丝圈删除后触发滚动更新异常，sourceId：{}，原因：{}", id, e.getMessage(), e);
            }

            // 4. 记录调度日志（6班次制，操作类型：3-删除，操作前=原记录，操作后=null）
            recordDispatcherLog(ApsConstant.DISPATCHER_OPER_DELETE, record, record, null);

            log.info("钢丝圈排程逻辑删除成功，id：{}，钢丝圈代码：{}，发布成功次数：{}，MES_ID：{}",
                    id, record.getSteelRingCode(),
                    record.getPublishSuccessCount(), record.getMesId());
        }

        return AjaxResult.success(I18nUtil.getMessage("ui.data.column.gsqScheduleResult.deleteSuccess"));
    }

    // ==================== 发布 ====================

    /**
     * 发布排程到MES
     * 业务流程：
     * 1. 查询排程日期下所有未发布/发布失败/待发布的钢丝圈排程记录
     * 2. 校验机台是否已分配
     * 3. 将6班数据拆分为3天的GsqScheduleResultIssue列表（D日/D+1日/D+2日）
     * 4. 通过Feign调用itf服务的issueGsqScheduleResult接口下发到MES
     * 5. 根据返回结果更新发布状态
     *
     * 6班→3天拆分映射：
     * Day1(D日)：MID=钢丝圈1班, NIGHT=null, DAY=null
     * Day2(D+1日)：NIGHT=钢丝圈2班, DAY=钢丝圈3班, MID=钢丝圈4班
     * Day3(D+2日)：NIGHT=钢丝圈5班, DAY=钢丝圈6班, MID=null
     * TQ_CLASS1~6_PLAN全量传递到每条记录
     *
     * @param queryVO 查询条件（含排程日期等）
     * @return 结果
     */
    @Override
    public AjaxResult publish(GsqScheduleResult queryVO) {
        Date scheduleDate = queryVO.getScheduleDate();
        if (scheduleDate == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gsqScheduleResult.scheduleDateRequired"));
        }
        log.info("钢丝圈排程发布，排程日期：{}", scheduleDate);

        // 加分布式锁，避免并发下发MES造成数据不一致
        String lockKey = "gsq:schedule:publish:" + DateUtil.formatDate(scheduleDate);
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            // 最多等待5秒获取锁，获取到锁后不自动释放（在finally中手动释放）
            locked = lock.tryLock(5, TimeUnit.SECONDS);
            if (!locked) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gsqScheduleResult.publishing"));
            }
            return this.doPublish(scheduleDate, queryVO.getIds());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("钢丝圈排程发布获取锁被中断", e);
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gsqScheduleResult.publishInterrupted"));
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 执行发布排程到MES（实际业务逻辑，由publish加锁后调用）
     *
     * @param scheduleDate 排程日期
     * @param ids          前端选中的记录ID列表（逗号分隔），为空时按日期全量发布
     * @return 结果
     */
    private AjaxResult doPublish(Date scheduleDate, String ids) {
        // 1. 查询该排程日期下所有未发布/发布失败/待发布的记录（框架已自动过滤逻辑删除数据，无需手动追加isDelete条件）
        LambdaQueryWrapper<GsqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GsqScheduleResult::getScheduleDate, scheduleDate);
        wrapper.in(GsqScheduleResult::getIsRelease,
                ApsConstant.NO_RELEASE, ApsConstant.FAILURE_RELEASE, ApsConstant.WAIT_RELEASING);
        // 按前端选中的ID列表过滤，未传ids时全量发布该日期下可发布记录
        if (StringUtils.isNotBlank(ids)) {
            List<Long> idList = Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(idList)) {
                wrapper.in(GsqScheduleResult::getId, idList);
            }
        }
        List<GsqScheduleResult> scheduleList = gsqScheduleResultMapper.selectList(wrapper);

        if (CollectionUtils.isEmpty(scheduleList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gsqScheduleResult.errorPublish"));
        }

        // 2. 校验机台是否已分配
        List<GsqScheduleResult> noMachineList = scheduleList.stream()
                .filter(s -> StringUtils.isBlank(s.getMachineCode()))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(noMachineList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gsqScheduleResult.noMachine"));
        }

        // 3. 回填胎圈排程结果数据
        this.fillTqPlanQty(scheduleList);

        // 3.1 批量查询施工信息（物料编码、钢丝类型、胎胚描述、单耗）
        List<String> steelRingCodes = scheduleList.stream()
                .map(GsqScheduleResult::getSteelRingCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        List<GsqScheduleBaseInfoVo> baseInfoList = gsqEngineService.listGsqScheduleBaseInfo(steelRingCodes);
        Map<String, GsqScheduleBaseInfoVo> baseInfoMap = CollectionUtils.isEmpty(baseInfoList)
                ? Collections.emptyMap()
                : baseInfoList.stream().collect(Collectors.toMap(
                        GsqScheduleBaseInfoVo::getSteelRingCode,
                        vo -> vo,
                        (v1, v2) -> v1));

        // 4. 将6班数据拆分为3天的下发列表
        List<GsqScheduleResultIssue> issueList = new ArrayList<>();
        for (GsqScheduleResult source : scheduleList) {
            // D日 = 排程日期 - 2
            Date dDay = DateUtil.offsetDay(scheduleDate, -2);
            Date dPlus1Day = DateUtil.offsetDay(scheduleDate, -1);
            Date dPlus2Day = scheduleDate;

            // Day1(D日)：钢丝圈1班→MES中班
            GsqScheduleResultIssue day1Issue = this.buildDay1Issue(source, dDay, baseInfoMap);
            issueList.add(day1Issue);

            // Day2(D+1日)：钢丝圈2班→MES夜班, 钢丝圈3班→MES早班, 钢丝圈4班→MES中班
            GsqScheduleResultIssue day2Issue = this.buildDay2Issue(source, dPlus1Day, baseInfoMap);
            issueList.add(day2Issue);

            // Day3(D+2日)：钢丝圈5班→MES夜班, 钢丝圈6班→MES早班
            GsqScheduleResultIssue day3Issue = this.buildDay3Issue(source, dPlus2Day, baseInfoMap);
            issueList.add(day3Issue);
        }

        // 5. 更新发布状态为"发布中"
        List<Long> releaseIds = scheduleList.stream().map(GsqScheduleResult::getId).collect(Collectors.toList());
        LambdaUpdateWrapper<GsqScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(GsqScheduleResult::getId, releaseIds);
        updateWrapper.set(GsqScheduleResult::getIsRelease, ApsConstant.RELEASING);
        gsqScheduleResultMapper.update(null, updateWrapper);

        // 6. 通过Feign调用itf服务下发到MES
        AjaxResult ajaxResult;
        try {
            ajaxResult = mesItfService.issueGsqScheduleResult(issueList);
            // 根据MES反馈状态更新发布状态：三态区分（IS_RELEASE/FAILURE_RELEASE/TIMEOUT_FAILURE）
            // itf 层将状态码放入 AjaxResult.DATA_TAG，doPublish 通过该字段区分三态
            String mesStatus = ajaxResult.get(AjaxResult.DATA_TAG) == null
                    ? ApsConstant.FAILURE_RELEASE
                    : String.valueOf(ajaxResult.get(AjaxResult.DATA_TAG));
            String status;
            if (ApsConstant.IS_RELEASE.equals(mesStatus)) {
                // 发布成功：状态置为已发布，发布成功次数累加1
                status = ApsConstant.IS_RELEASE;
                LambdaUpdateWrapper<GsqScheduleResult> successWrapper = new LambdaUpdateWrapper<>();
                successWrapper.in(GsqScheduleResult::getId, releaseIds);
                successWrapper.set(GsqScheduleResult::getIsRelease, status);
                // publishSuccessCount + 1：原始值若为空按0处理
                successWrapper.setSql("publish_success_count = COALESCE(publish_success_count, 0) + 1");
                gsqScheduleResultMapper.update(null, successWrapper);
            } else if (ApsConstant.TIMEOUT_FAILURE.equals(mesStatus)) {
                // 超时失败：状态置为超时失败
                status = ApsConstant.TIMEOUT_FAILURE;
                LambdaUpdateWrapper<GsqScheduleResult> resultWrapper = new LambdaUpdateWrapper<>();
                resultWrapper.in(GsqScheduleResult::getId, releaseIds);
                resultWrapper.set(GsqScheduleResult::getIsRelease, status);
                gsqScheduleResultMapper.update(null, resultWrapper);
            } else {
                // 发布失败：状态置为发布失败
                status = ApsConstant.FAILURE_RELEASE;
                LambdaUpdateWrapper<GsqScheduleResult> resultWrapper = new LambdaUpdateWrapper<>();
                resultWrapper.in(GsqScheduleResult::getId, releaseIds);
                resultWrapper.set(GsqScheduleResult::getIsRelease, status);
                gsqScheduleResultMapper.update(null, resultWrapper);
            }
        } catch (Exception e) {
            log.error("钢丝圈排程发布失败", e);
            // 发布失败，更新状态为"发布失败"
            LambdaUpdateWrapper<GsqScheduleResult> errorWrapper = new LambdaUpdateWrapper<>();
            errorWrapper.in(GsqScheduleResult::getId, releaseIds);
            errorWrapper.set(GsqScheduleResult::getIsRelease, ApsConstant.FAILURE_RELEASE);
            gsqScheduleResultMapper.update(null, errorWrapper);
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gsqScheduleResult.failedPublish"));
        }

        return ajaxResult;
    }

    /**
     * 构建D日（今天）下发数据
     * 钢丝圈1班(D日中班) → MES中班(MID_PLAN_QTY)
     * 夜班、早班已过不下发，NEXT_MID不下发
     *
     * @param source      钢丝圈排程结果
     * @param dDay        D日日期
     * @param baseInfoMap 施工信息Map
     * @return D日下发对象
     */
    private GsqScheduleResultIssue buildDay1Issue(GsqScheduleResult source, Date dDay,
                                                   Map<String, GsqScheduleBaseInfoVo> baseInfoMap) {
        GsqScheduleResultIssue issue = buildBaseIssue(source, dDay, baseInfoMap);
        // 钢丝圈1班→MES中班
        issue.setMidPlanQty(source.getClass1PlanQty() != null ? source.getClass1PlanQty().doubleValue() : null);
        issue.setMidProduceOrder(source.getClass1Sequence());
        // 夜班、早班已过不下发
        issue.setNightPlanQty(null);
        issue.setNightProduceOrder(null);
        issue.setDayPlanQty(null);
        issue.setDayProduceOrder(null);
        issue.setNextMidPlanQty(null);
        issue.setNextMidProduceOrder(null);
        return issue;
    }

    /**
     * 构建D+1日（明天）下发数据
     * 钢丝圈2班(D+1日夜班) → MES夜班(NIGHT_PLAN_QTY)
     * 钢丝圈3班(D+1日早班) → MES早班(DAY_PLAN_QTY)
     * 钢丝圈4班(D+1日中班) → MES中班(MID_PLAN_QTY)
     * NEXT_MID不下发
     *
     * @param source      钢丝圈排程结果
     * @param dPlus1Day   D+1日日期
     * @param baseInfoMap 施工信息Map
     * @return D+1日下发对象
     */
    private GsqScheduleResultIssue buildDay2Issue(GsqScheduleResult source, Date dPlus1Day,
                                                   Map<String, GsqScheduleBaseInfoVo> baseInfoMap) {
        GsqScheduleResultIssue issue = buildBaseIssue(source, dPlus1Day, baseInfoMap);
        // 钢丝圈2班→MES夜班
        issue.setNightPlanQty(source.getClass2PlanQty() != null ? source.getClass2PlanQty().doubleValue() : null);
        issue.setNightProduceOrder(source.getClass2Sequence());
        // 钢丝圈3班→MES早班
        issue.setDayPlanQty(source.getClass3PlanQty() != null ? source.getClass3PlanQty().doubleValue() : null);
        issue.setDayProduceOrder(source.getClass3Sequence());
        // 钢丝圈4班→MES中班
        issue.setMidPlanQty(source.getClass4PlanQty() != null ? source.getClass4PlanQty().doubleValue() : null);
        issue.setMidProduceOrder(source.getClass4Sequence());
        // NEXT_MID不下发
        issue.setNextMidPlanQty(null);
        issue.setNextMidProduceOrder(null);
        return issue;
    }

    /**
     * 构建D+2日（后天）下发数据
     * 钢丝圈5班(D+2日夜班) → MES夜班(NIGHT_PLAN_QTY)
     * 钢丝圈6班(D+2日早班) → MES早班(DAY_PLAN_QTY)
     * 中班尚未排产不下发，NEXT_MID不下发
     *
     * @param source      钢丝圈排程结果
     * @param dPlus2Day   D+2日日期
     * @param baseInfoMap 施工信息Map
     * @return D+2日下发对象
     */
    private GsqScheduleResultIssue buildDay3Issue(GsqScheduleResult source, Date dPlus2Day,
                                                   Map<String, GsqScheduleBaseInfoVo> baseInfoMap) {
        GsqScheduleResultIssue issue = buildBaseIssue(source, dPlus2Day, baseInfoMap);
        // 钢丝圈5班→MES夜班
        issue.setNightPlanQty(source.getClass5PlanQty() != null ? source.getClass5PlanQty().doubleValue() : null);
        issue.setNightProduceOrder(source.getClass5Sequence());
        // 钢丝圈6班→MES早班
        issue.setDayPlanQty(source.getClass6PlanQty() != null ? source.getClass6PlanQty().doubleValue() : null);
        issue.setDayProduceOrder(source.getClass6Sequence());
        // 中班尚未排产不下发
        issue.setMidPlanQty(null);
        issue.setMidProduceOrder(null);
        issue.setNextMidPlanQty(null);
        issue.setNextMidProduceOrder(null);
        return issue;
    }

    /**
     * 构建基础下发对象（公共字段+胎圈1~6班计划量全量传递）
     *
     * @param source       钢丝圈排程结果
     * @param scheduleDate MES目标日期
     * @param baseInfoMap  施工信息Map（key=钢丝圈代码）
     * @return 基础下发对象
     */
    private GsqScheduleResultIssue buildBaseIssue(GsqScheduleResult source, Date scheduleDate,
                                                   Map<String, GsqScheduleBaseInfoVo> baseInfoMap) {
        GsqScheduleResultIssue issue = new GsqScheduleResultIssue();
        // 日期转换：Date → LocalDate
        issue.setScheduleDate(scheduleDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        // 基础信息
        issue.setTqBatchNo(source.getTqBatchNo());
        issue.setBatchNo(source.getBatchNo());
        issue.setOrderNo(source.getOrderNo());
        // 物料信息（从施工信息表关联获取）
        issue.setSteelRingCode(source.getSteelRingCode());
        GsqScheduleBaseInfoVo baseInfo = baseInfoMap.get(source.getSteelRingCode());
        if (baseInfo != null) {
            issue.setMaterialCode(baseInfo.getMaterialCode());
            issue.setSteelType(baseInfo.getSteelType());
            issue.setEmbryoSpecDesc(baseInfo.getEmbryoSpecDesc());
            // 单耗：SQL中已硬编码为1，暂时使用默认值
            issue.setUnitConsume(baseInfo.getUnitConsume() != null ? baseInfo.getUnitConsume() : 1.0);
        } else {
            // 施工信息查不到时，单耗默认设为1
            issue.setUnitConsume(1.0);
        }
        issue.setTwiningDiscCode(source.getTwiningDiscCode());
        issue.setProSize(source.getProSize());
        issue.setMachineCode(source.getMachineCode());
        // 库存信息
        issue.setStockQty(source.getStockQty() != null ? source.getStockQty().doubleValue() : null);
        // 胎圈1~6班计划量全量传递（MES通过这些字段理解钢丝圈与胎圈的供应关系）
        issue.setTqClass1Plan(source.getTqClass1Plan());
        issue.setTqClass2Plan(source.getTqClass2Plan());
        issue.setTqClass3Plan(source.getTqClass3Plan());
        issue.setTqClass4Plan(source.getTqClass4Plan());
        issue.setTqClass5Plan(source.getTqClass5Plan());
        issue.setTqClass6Plan(source.getTqClass6Plan());
        // 状态
        issue.setProductionStatus(ApsConstant.NO_PRODUNTION);
        issue.setCloseOutSpecFlag(source.getCloseOutSpecFlag());
        issue.setFactoryCode(source.getFactoryCode());
        return issue;
    }

    // ==================== 查询方法 ====================

    /**
     * 查询排程日期是否已发布
     * 判断依据：该排程日期下所有未删除记录的发布状态均为"已发布"
     *
     * @param scheduleDate 排程日期
     * @return 是否已发布
     */
    @Override
    public Boolean isPublish(Date scheduleDate) {
        if (scheduleDate == null) {
            return false;
        }
        // 查询该排程日期下所有未删除的记录
        LambdaQueryWrapper<GsqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GsqScheduleResult::getScheduleDate, scheduleDate);
        wrapper.eq(GsqScheduleResult::getIsDelete, 0);
        List<GsqScheduleResult> list = gsqScheduleResultMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(list)) {
            // 无记录视为未发布
            return false;
        }
        // 所有记录都已发布（IS_RELEASE=1）才返回 true
        return list.stream().allMatch(r -> ApsConstant.IS_RELEASE.equals(r.getIsRelease()));
    }

    /**
     * 唯一性校验
     * 根据排程日期、钢丝圈代码、机台编号校验唯一性
     *
     * @param entity 待校验记录
     * @return UserConstants.UNIQUE="0" 唯一，UserConstants.NOT_UNIQUE="1" 不唯一
     */
    @Override
    public String checkUnique(GsqScheduleResult entity) {
        if (entity == null || entity.getScheduleDate() == null
                || StringUtils.isBlank(entity.getSteelRingCode())
                || StringUtils.isBlank(entity.getMachineCode())) {
            return UserConstants.UNIQUE;
        }
        LambdaQueryWrapper<GsqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GsqScheduleResult::getScheduleDate, entity.getScheduleDate());
        wrapper.eq(GsqScheduleResult::getSteelRingCode, entity.getSteelRingCode());
        wrapper.eq(GsqScheduleResult::getMachineCode, entity.getMachineCode());
        wrapper.eq(GsqScheduleResult::getIsDelete, 0);
        // 编辑时排除自身
        if (entity.getId() != null) {
            wrapper.ne(GsqScheduleResult::getId, entity.getId());
        }
        Long count = gsqScheduleResultMapper.selectCount(wrapper);
        return count != null && count > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录数
     *
     * @param scheduleDate 排程日期
     * @return 记录数
     */
    @Override
    public int isReleasingOrTimeoutByDate(Date scheduleDate) {
        if (scheduleDate == null) {
            return 0;
        }
        LambdaQueryWrapper<GsqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GsqScheduleResult::getScheduleDate, scheduleDate);
        wrapper.eq(GsqScheduleResult::getIsDelete, 0);
        wrapper.in(GsqScheduleResult::getIsRelease, ApsConstant.RELEASING, ApsConstant.FAILURE_RELEASE);
        Long count = gsqScheduleResultMapper.selectCount(wrapper);
        return count == null ? 0 : count.intValue();
    }

    /**
     * 根据排程日期构建6个班次的日期展示列表
     * 钢丝圈排程6个班次覆盖D日中班、D+1日夜早中、D+2日夜早：
     * 班次1：D日中班，班次2~4：D+1日(夜/早/中)，班次5~6：D+2日(夜/早)
     * D = 排程日期 - 2（即今天）
     *
     * @param queryVO 查询条件
     * @return 班次日期列表
     */
    @Override
    public List<GsqScheduleShiftDateVO> listScheduleShiftDates(GsqScheduleResult queryVO) {
        Date scheduleDate = queryVO == null ? null : queryVO.getScheduleDateQuery();
        if (scheduleDate == null) {
            // 默认排程日期 = 今天 + 2（D+2），与前端默认值保持一致
            scheduleDate = DateUtil.offsetDay(new Date(), 2);
        }
        // D = 排程日期 - 2
        Date dDay = DateUtil.offsetDay(scheduleDate, -2);
        Date dPlus1Day = DateUtil.offsetDay(dDay, 1);
        Date dPlus2Day = DateUtil.offsetDay(dDay, 2);
        String dDateStr = DateUtil.format(dDay, "MM/dd");
        String dPlus1DateStr = DateUtil.format(dPlus1Day, "MM/dd");
        String dPlus2DateStr = DateUtil.format(dPlus2Day, "MM/dd");

        List<GsqScheduleShiftDateVO> result = new ArrayList<>(6);
        result.add(buildShiftDateVO(1, "afternoon", dDateStr));        // D日中班
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
     * @param shift     班次序号
     * @param shiftType 班次类型：night=夜班, morning=早班, afternoon=中班
     * @param shiftDate 班次对应日期，格式 MM/dd
     * @return 班次日期VO
     */
    private GsqScheduleShiftDateVO buildShiftDateVO(int shift, String shiftType, String shiftDate) {
        GsqScheduleShiftDateVO vo = new GsqScheduleShiftDateVO();
        vo.setShift(shift);
        vo.setShiftType(shiftType);
        vo.setShiftDate(shiftDate);
        return vo;
    }

    // ==================== 新人工操作入口（走任务链路径） ====================

    /**
     * 人工插单（走任务链路径，支持锚点插入、resequence 重排）。
     *
     * <p>统一走 {@link GsqManualInsertRollingService#insertAndRoll}，由底层服务负责
     * 数据库快照加载、引擎执行和一次性持久化。</p>
     *
     * <p>与旧 insertOrder 的差异：</p>
     * <ul>
     *   <li>支持锚点插入：anchorTaskId 不为空时在锚点之后插入，锚点之后任务 sequence +1</li>
     *   <li>自动重排顺位：resequence 保证同机台同班次 sequence 从1连续递增</li>
     *   <li>滚动重装箱：超额任务自动顺延到下一班次</li>
     * </ul>
     *
     * @param vo 插单请求
     * @return 结果
     */
    @Override
    public AjaxResult insertTask(GsqInsertTaskRequestVo vo) {
        if (vo == null) {
            return AjaxResult.error("插单请求不能为空");
        }
        if (vo.getScheduleDate() == null) {
            return AjaxResult.error("排程日期不能为空");
        }
        if (StringUtils.isBlank(vo.getSteelRingCode())) {
            return AjaxResult.error("钢丝圈代码不能为空");
        }
        if (StringUtils.isBlank(vo.getMachineCode())) {
            return AjaxResult.error("机台编号不能为空");
        }
        // 校验施工是否存在
        List<GsqScheduleBaseInfoVo> baseInfoList = gsqEngineService.listGsqScheduleBaseInfo(
                Collections.singletonList(vo.getSteelRingCode()));
        if (CollectionUtils.isEmpty(baseInfoList)) {
            return AjaxResult.error("钢丝圈规格有误，施工不存在");
        }
        // 构建 GsqScheduleResult 模板
        GsqScheduleResult template = new GsqScheduleResult();
        template.setFactoryCode(vo.getFactoryCode());
        template.setScheduleDate(vo.getScheduleDate());
        template.setSteelRingCode(vo.getSteelRingCode());
        template.setProSize(vo.getProSize());
        template.setMachineCode(vo.getMachineCode());
        template.setClass1PlanQty(vo.getClass1PlanQty());
        template.setClass1Sequence(vo.getClass1Sequence());
        template.setClass1Analysis(vo.getClass1Analysis());
        template.setClass2PlanQty(vo.getClass2PlanQty());
        template.setClass2Sequence(vo.getClass2Sequence());
        template.setClass2Analysis(vo.getClass2Analysis());
        template.setClass3PlanQty(vo.getClass3PlanQty());
        template.setClass3Sequence(vo.getClass3Sequence());
        template.setClass3Analysis(vo.getClass3Analysis());
        template.setClass4PlanQty(vo.getClass4PlanQty());
        template.setClass4Sequence(vo.getClass4Sequence());
        template.setClass4Analysis(vo.getClass4Analysis());
        template.setClass5PlanQty(vo.getClass5PlanQty());
        template.setClass5Sequence(vo.getClass5Sequence());
        template.setClass5Analysis(vo.getClass5Analysis());
        template.setClass6PlanQty(vo.getClass6PlanQty());
        template.setClass6Sequence(vo.getClass6Sequence());
        template.setClass6Analysis(vo.getClass6Analysis());
        template.setRemark(vo.getRemark());
        template.setDataSource("1");
        template.setIsRelease(ApsConstant.NO_RELEASE);
        // 生成批次号、工单号（复用当前排程日期已有批次号，不影响其他记录）
        String scheduleDateStr = DateUtil.formatDate(vo.getScheduleDate());
        String[] batchAndOrder = gsqEngineService.generateBatchNoAndOrderNo(scheduleDateStr);
        template.setBatchNo(batchAndOrder[0]);
        template.setOrderNo(batchAndOrder[1]);
        // 回显施工字段（英寸尺寸），从施工表获取
        GsqScheduleBaseInfoVo baseInfo = baseInfoList.get(0);
        if (StringUtils.isBlank(template.getProSize())) {
            template.setProSize(baseInfo.getProSize());
        }
        // 走人工滚动应用服务统一入口
        try {
            gsqManualInsertRollingService.insertAndRoll(template);
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
        return AjaxResult.success("钢丝圈人工插单成功");
    }

    /**
     * 批量转机台（走任务链路径，支持锚点、目标班次）。
     *
     * <p>批量转机台仅支持同一目标机台。每条请求的 machineCode 即目标机台编码，
     * 源机台由底层服务按 id 从数据库读取并校验，避免请求携带的机台被篡改。</p>
     *
     * @param list 转机台请求列表（每条携带 id 与同一目标 machineCode）
     * @return 结果
     */
    @Override
    public AjaxResult batchChangeMachine(List<GsqScheduleResult> list) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error("转机台请求不能为空");
        }
        // 批量转机台仅支持同一目标机台，避免异构目标静默覆盖为单一目标
        String targetMachineCode = StringUtils.trimToEmpty(list.get(0).getMachineCode());
        if (StringUtils.isBlank(targetMachineCode)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.gsq.schedule.machineCodeEmpty"));
        }
        for (GsqScheduleResult request : list) {
            if (!targetMachineCode.equals(StringUtils.trimToEmpty(request.getMachineCode()))) {
                return AjaxResult.error("批量转机台仅支持同一目标机台");
            }
        }
        try {
            gsqManualInsertRollingService.changeMachineAndRollBatch(list);
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
        return AjaxResult.success("钢丝圈批量转机台成功");
    }

    /**
     * 批量调量（走任务链路径）。
     *
     * @param list 调量请求列表
     * @return 结果
     */
    @Override
    public AjaxResult batchChangeQty(List<GsqScheduleResult> list) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error("调量请求不能为空");
        }
        try {
            gsqManualInsertRollingService.changeQtyAndRollBatch(list);
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
        return AjaxResult.success("钢丝圈批量调量成功");
    }

    /**
     * 批量删除（走任务链路径，删除后 resequence 重排）。
     *
     * <p>底层服务负责：加载待删除记录、行锁、释放状态校验、
     * 局部滚动、逻辑删除和调度日志，任一步失败整批回滚。</p>
     *
     * @param ids 排程记录ID列表
     * @return 结果
     */
    @Override
    public AjaxResult batchDelete(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return AjaxResult.error("删除请求不能为空");
        }
        // 通过 ids 查询出待删除的排程记录列表
        List<GsqScheduleResult> deleteList = new ArrayList<>();
        for (Long id : ids) {
            GsqScheduleResult record = gsqScheduleResultMapper.selectById(id);
            if (record == null || Objects.equals(record.getIsDelete(), 1)) {
                continue;
            }
            deleteList.add(record);
        }
        if (CollectionUtils.isEmpty(deleteList)) {
            return AjaxResult.error("待删除的排程记录不存在或已删除");
        }
        try {
            gsqManualInsertRollingService.deleteAndRollBatch(deleteList);
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
        return AjaxResult.success("钢丝圈批量删除成功");
    }

    // ==================== 私有方法 ====================

    /**
     * 回填胎圈排程结果数据到 TQ_CLASS1~6_PLAN 字段
     * 根据钢丝圈代码和排程日期查询对应的胎圈排程结果，将胎圈6班计划量回填到钢丝圈排程结果
     *
     * @param scheduleList 钢丝圈排程结果列表
     */
    @Override
    public void fillTqPlanQty(List<GsqScheduleResult> scheduleList) {
        if (CollectionUtils.isEmpty(scheduleList)) {
            return;
        }
        // 按排程日期分组
        Map<Date, List<GsqScheduleResult>> dateGroupMap = scheduleList.stream()
                .filter(s -> s.getScheduleDate() != null)
                .collect(Collectors.groupingBy(GsqScheduleResult::getScheduleDate));
        for (Map.Entry<Date, List<GsqScheduleResult>> entry : dateGroupMap.entrySet()) {
            Date scheduleDate = entry.getKey();
            List<GsqScheduleResult> groupList = entry.getValue();
            // 查询胎圈排程结果
            TqScheduleResult tqQuery = new TqScheduleResult();
            tqQuery.setScheduleDate(scheduleDate);
            tqQuery.setIsDelete(0);
            TableDataInfo tqResult;
            try {
                tqResult = tqScheduleResultService.list(tqQuery);
            } catch (Exception e) {
                log.warn("查询胎圈排程结果失败，排程日期：{}", scheduleDate, e);
                continue;
            }
            if (tqResult == null || CollectionUtils.isEmpty(tqResult.getRows())) {
                continue;
            }
            // 远程Feign返回的rows为LinkedHashMap，需转换为TqScheduleResult实体
            List<TqScheduleResult> tqList = new ArrayList<>();
            if (tqResult.getRows() != null) {
                for (Object row : tqResult.getRows()) {
                    if (row instanceof TqScheduleResult) {
                        tqList.add((TqScheduleResult) row);
                    } else if (row instanceof Map) {
                        tqList.add(BeanUtil.toBean((Map<String, Object>) row, TqScheduleResult.class));
                    }
                }
            }
            // 按钢丝圈代码分组
            Map<String, List<TqScheduleResult>> tqCodeMap = tqList.stream()
                    .filter(t -> StringUtils.isNotBlank(t.getSteelRingCode()))
                    .collect(Collectors.groupingBy(TqScheduleResult::getSteelRingCode));
            // 回填
            for (GsqScheduleResult gsq : groupList) {
                List<TqScheduleResult> tqMatchList = tqCodeMap.get(gsq.getSteelRingCode());
                if (CollectionUtils.isEmpty(tqMatchList)) {
                    continue;
                }
                // 取第一条匹配记录（同一钢丝圈代码、排程日期通常只有一条）
                TqScheduleResult tq = tqMatchList.get(0);
                gsq.setTqClass1Plan(tq.getClass1PlanQty());
                gsq.setTqClass2Plan(tq.getClass2PlanQty());
                gsq.setTqClass3Plan(tq.getClass3PlanQty());
                gsq.setTqClass4Plan(tq.getClass4PlanQty());
                gsq.setTqClass5Plan(tq.getClass5PlanQty());
                gsq.setTqClass6Plan(tq.getClass6PlanQty());
            }
        }
    }

    /**
     * 记录调度员排程操作日志（6班次制）
     *
     * <p>统一封装调度日志写入逻辑，覆盖4种操作类型：
     * <ul>
     *   <li>0-转机台：beforeRecord为原机台记录，afterRecord为新机台记录</li>
     *   <li>1-调量：beforeRecord为原记录，afterRecord为更新后记录</li>
     *   <li>2-插单：beforeRecord为null，afterRecord为新增记录</li>
     *   <li>3-删除：beforeRecord为原记录，afterRecord为null</li>
     * </ul>
     * 日志异常不影响主操作，仅记录错误日志。
     *
     * @param operType     操作类型：0-转机台、1-调量、2-插单、3-删除
     * @param baseRecord   基础记录（用于获取排程日期、钢丝圈代码、排程记录ID等公共字段）
     * @param beforeRecord 操作前记录（用于填充before*字段，插单时为null）
     * @param afterRecord  操作后记录（用于填充after*字段，删除时为null）
     */
    private void recordDispatcherLog(String operType, GsqScheduleResult baseRecord,
                                     GsqScheduleResult beforeRecord, GsqScheduleResult afterRecord) {
        try {
            if (baseRecord == null) {
                log.warn("记录调度日志跳过：baseRecord为空");
                return;
            }
            GsqDispatcherLog dispatcherLog = new GsqDispatcherLog();
            dispatcherLog.setOperType(operType);
            dispatcherLog.setScheduleId(baseRecord.getId());
            dispatcherLog.setScheduleDate(baseRecord.getScheduleDate());
            dispatcherLog.setSteelRingCode(baseRecord.getSteelRingCode());
            // 操作前机台编码和6班次计划量
            if (beforeRecord != null) {
                dispatcherLog.setBeforeMachineCode(beforeRecord.getMachineCode());
                dispatcherLog.setBeforeClass1Plan(beforeRecord.getClass1PlanQty());
                dispatcherLog.setBeforeClass2Plan(beforeRecord.getClass2PlanQty());
                dispatcherLog.setBeforeClass3Plan(beforeRecord.getClass3PlanQty());
                dispatcherLog.setBeforeClass4Plan(beforeRecord.getClass4PlanQty());
                dispatcherLog.setBeforeClass5Plan(beforeRecord.getClass5PlanQty());
                dispatcherLog.setBeforeClass6Plan(beforeRecord.getClass6PlanQty());
            }
            // 操作后机台编码和6班次计划量
            if (afterRecord != null) {
                dispatcherLog.setAfterMachineCode(afterRecord.getMachineCode());
                dispatcherLog.setAfterClass1Plan(afterRecord.getClass1PlanQty());
                dispatcherLog.setAfterClass2Plan(afterRecord.getClass2PlanQty());
                dispatcherLog.setAfterClass3Plan(afterRecord.getClass3PlanQty());
                dispatcherLog.setAfterClass4Plan(afterRecord.getClass4PlanQty());
                dispatcherLog.setAfterClass5Plan(afterRecord.getClass5PlanQty());
                dispatcherLog.setAfterClass6Plan(afterRecord.getClass6PlanQty());
            }
            gsqDispatcherLogService.insertGsqDispatcherLog(dispatcherLog);
        } catch (Exception e) {
            // 日志记录失败不影响主操作
            log.error("记录钢丝圈调度日志失败，operType：{}，scheduleId：{}", operType, baseRecord.getId(), e);
        }
    }

    /**
     * 根据班次索引获取DTO中的计划量
     */
    private Integer getPlanQtyByClassIndex(GsqInsertOrderDTO dto, int classIndex) {
        switch (classIndex) {
            case 1: return dto.getClass1PlanQty();
            case 2: return dto.getClass2PlanQty();
            case 3: return dto.getClass3PlanQty();
            case 4: return dto.getClass4PlanQty();
            case 5: return dto.getClass5PlanQty();
            case 6: return dto.getClass6PlanQty();
            default: return null;
        }
    }

    /**
     * 根据班次索引获取DTO中的顺序
     */
    private Integer getSequenceByClassIndex(GsqInsertOrderDTO dto, int classIndex) {
        switch (classIndex) {
            case 1: return dto.getClass1Sequence();
            case 2: return dto.getClass2Sequence();
            case 3: return dto.getClass3Sequence();
            case 4: return dto.getClass4Sequence();
            case 5: return dto.getClass5Sequence();
            case 6: return dto.getClass6Sequence();
            default: return null;
        }
    }

    /**
     * 解析当前班次索引（1~6）
     * 钢丝圈排程6班次时间窗口：
     * 1班：D日中班(16:00-24:00)
     * 2班：D+1日夜班(00:00-08:00)
     * 3班：D+1日早班(08:00-16:00)
     * 4班：D+1日中班(16:00-24:00)
     * 5班：D+2日夜班(00:00-08:00)
     * 6班：D+2日早班(08:00-16:00)
     * D = 排程日期 - 2
     *
     * @param scheduleDate 排程日期
     * @return 当前班次索引
     */
    private int resolveCurrentShiftIndex(Date scheduleDate) {
        if (scheduleDate == null) {
            // 无排程日期时按小时回退判断
            int hour = DateUtil.hour(new Date(), true);
            if (hour >= 16) {
                return 1;
            } else if (hour >= 8) {
                return 3;
            } else {
                return 2;
            }
        }
        Date now = new Date();
        // D日 = 排程日期 - 2
        Date dDay = DateUtil.beginOfDay(DateUtil.offsetDay(scheduleDate, -2));
        Date dPlus1Day = DateUtil.beginOfDay(DateUtil.offsetDay(scheduleDate, -1));
        Date dPlus2Day = DateUtil.beginOfDay(scheduleDate);
        // 各班次时间窗口
        Date shift1Start = DateUtil.offsetHour(dDay, 16);
        Date shift1End = DateUtil.offsetHour(dDay, 24);
        Date shift2Start = dPlus1Day;
        Date shift2End = DateUtil.offsetHour(dPlus1Day, 8);
        Date shift3Start = DateUtil.offsetHour(dPlus1Day, 8);
        Date shift3End = DateUtil.offsetHour(dPlus1Day, 16);
        Date shift4Start = DateUtil.offsetHour(dPlus1Day, 16);
        Date shift4End = DateUtil.offsetHour(dPlus1Day, 24);
        Date shift5Start = dPlus2Day;
        Date shift5End = DateUtil.offsetHour(dPlus2Day, 8);
        Date shift6Start = DateUtil.offsetHour(dPlus2Day, 8);
        Date shift6End = DateUtil.offsetHour(dPlus2Day, 16);
        if (!now.before(shift1Start) && now.before(shift1End)) {
            return 1;
        }
        if (!now.before(shift2Start) && now.before(shift2End)) {
            return 2;
        }
        if (!now.before(shift3Start) && now.before(shift3End)) {
            return 3;
        }
        if (!now.before(shift4Start) && now.before(shift4End)) {
            return 4;
        }
        if (!now.before(shift5Start) && now.before(shift5End)) {
            return 5;
        }
        if (!now.before(shift6Start) && now.before(shift6End)) {
            return 6;
        }
        // 不在任何班次窗口内（排程尚未开始或已结束），默认返回1
        return 1;
    }

    /**
     * 获取一条排程记录6个班次中最小的非空顺序号
     * 遵循动态字段访问规范：通过字段名模板动态读取班次顺序，避免逐字段硬编码。
     *
     * @param record 排程记录
     * @return 最小顺序号，无顺序号时返回 Integer.MAX_VALUE
     */
    private int getMinSequenceOfRecord(GsqScheduleResult record) {
        int minSeq = Integer.MAX_VALUE;
        for (int shiftIndex = 1; shiftIndex <= 6; shiftIndex++) {
            String fieldName = String.format(CLASS_SEQUENCE_FIELD_TEMPLATE, shiftIndex);
            Integer sequence = (Integer) record.getFieldValueByFieldName(fieldName);
            if (sequence != null && sequence > 0) {
                minSeq = Math.min(minSeq, sequence);
            }
        }
        return minSeq;
    }

    /**
     * 获取第二个在产规格的最小顺序号
     * 用于校验插单只能加到第二个在产规格之后
     * 遵循动态字段访问规范：通过字段名模板动态读取班次顺序。
     *
     * @param existingList 已有排程记录列表（按顺序排序）
     * @return 第二个在产规格的最小顺序号
     */
    private int getMinSequenceFromSecondSpec(List<GsqScheduleResult> existingList) {
        if (existingList.size() < 2) {
            return 1;
        }
        // 取第二条记录的顺序号（取所有班次顺序中最小的非空值）
        GsqScheduleResult secondRecord = existingList.get(1);
        int minSeq = Integer.MAX_VALUE;
        for (int shiftIndex = 1; shiftIndex <= 6; shiftIndex++) {
            String fieldName = String.format(CLASS_SEQUENCE_FIELD_TEMPLATE, shiftIndex);
            Integer sequence = (Integer) secondRecord.getFieldValueByFieldName(fieldName);
            if (sequence != null && sequence > 0) {
                minSeq = Math.min(minSeq, sequence);
            }
        }
        return minSeq == Integer.MAX_VALUE ? 1 : minSeq;
    }

    /**
     * 根据班次索引获取实体中的计划量
     * 遵循动态字段访问规范：通过字段名模板动态读取，避免 switch/case 硬编码。
     *
     * @param entity     排程结果实体
     * @param shiftIndex 班次索引（1~6）
     * @return 计划量
     */
    private Integer getPlanQtyByShiftIndex(GsqScheduleResult entity, int shiftIndex) {
        String fieldName = String.format(CLASS_PLAN_QTY_FIELD_TEMPLATE, shiftIndex);
        return (Integer) entity.getFieldValueByFieldName(fieldName);
    }

    /**
     * 根据班次索引获取实体中的完成量
     * 遵循动态字段访问规范：通过字段名模板动态读取，避免 switch/case 硬编码。
     *
     * @param entity     排程结果实体
     * @param shiftIndex 班次索引（1~6）
     * @return 完成量
     */
    private Integer getFinishQtyByShiftIndex(GsqScheduleResult entity, int shiftIndex) {
        String fieldName = String.format(CLASS_FINISH_QTY_FIELD_TEMPLATE, shiftIndex);
        return (Integer) entity.getFieldValueByFieldName(fieldName);
    }

    /**
     * 根据班次索引获取实体中的原因分析
     * 遵循动态字段访问规范：通过字段名模板动态读取，避免 switch/case 硬编码。
     *
     * @param entity     排程结果实体
     * @param shiftIndex 班次索引（1~6）
     * @return 原因分析
     */
    private String getAnalysisByShiftIndex(GsqScheduleResult entity, int shiftIndex) {
        String fieldName = String.format(CLASS_ANALYSIS_FIELD_TEMPLATE, shiftIndex);
        return (String) entity.getFieldValueByFieldName(fieldName);
    }

    /**
     * 设置指定班次计划量到更新实体
     * 遵循动态字段访问规范：通过 setFieldValueByFieldName 设置到更新实体。
     *
     * @param updateEntity 更新实体
     * @param shiftIndex   班次索引（1~6）
     * @param planQty      计划量
     */
    private void setPlanQtyToUpdateEntity(GsqScheduleResult updateEntity,
                                          int shiftIndex, Integer planQty) {
        String fieldName = String.format(CLASS_PLAN_QTY_FIELD_TEMPLATE, shiftIndex);
        updateEntity.setFieldValueByFieldName(fieldName, planQty);
    }

    /**
     * 设置指定班次原因分析到更新实体
     * 遵循动态字段访问规范：通过 setFieldValueByFieldName 设置到更新实体。
     *
     * @param updateEntity 更新实体
     * @param shiftIndex   班次索引（1~6）
     * @param analysis     原因分析
     */
    private void setAnalysisToUpdateEntity(GsqScheduleResult updateEntity,
                                           int shiftIndex, String analysis) {
        String fieldName = String.format(CLASS_ANALYSIS_FIELD_TEMPLATE, shiftIndex);
        updateEntity.setFieldValueByFieldName(fieldName, analysis);
    }

    /**
     * 判断指定班次是否已成为历史班次
     * 钢丝圈排程6班次时间窗口：
     * 1班：D日中班(16:00-24:00)
     * 2班：D+1日夜班(00:00-08:00)
     * 3班：D+1日早班(08:00-16:00)
     * 4班：D+1日中班(16:00-24:00)
     * 5班：D+2日夜班(00:00-08:00)
     * 6班：D+2日早班(08:00-16:00)
     * D = 排程日期 - 2（即今天）
     *
     * @param record     排程结果记录
     * @param shiftIndex 班次索引（1~6）
     * @param now        当前时间
     * @return true表示班次已结束，属于历史班次
     */
    private boolean isHistoryShift(GsqScheduleResult record, int shiftIndex, Date now) {
        if (record.getScheduleDate() == null) {
            return false;
        }
        // D = 排程日期 - 2
        Date dDay = DateUtil.beginOfDay(DateUtil.offsetDay(record.getScheduleDate(), -2));
        Date shiftEndTime = resolveShiftEndTime(dDay, shiftIndex);
        if (shiftEndTime == null) {
            return false;
        }
        return !now.before(shiftEndTime);
    }

    /**
     * 根据D日和班次索引推导班次结束时间
     *
     * @param dDay       D日（排程日期-2）
     * @param shiftIndex 班次索引（1~6）
     * @return 班次结束时间
     */
    private Date resolveShiftEndTime(Date dDay, int shiftIndex) {
        switch (shiftIndex) {
            case 1:
                // 1班：D日中班(16:00-24:00)，结束时间=D日24:00=D+1日00:00
                return DateUtil.endOfDay(dDay);
            case 2:
                // 2班：D+1日夜班(00:00-08:00)，结束时间=D+1日08:00
                return DateUtil.offsetHour(DateUtil.beginOfDay(DateUtil.offsetDay(dDay, 1)), 8);
            case 3:
                // 3班：D+1日早班(08:00-16:00)，结束时间=D+1日16:00
                return DateUtil.offsetHour(DateUtil.beginOfDay(DateUtil.offsetDay(dDay, 1)), 16);
            case 4:
                // 4班：D+1日中班(16:00-24:00)，结束时间=D+1日24:00=D+2日00:00
                return DateUtil.endOfDay(DateUtil.offsetDay(dDay, 1));
            case 5:
                // 5班：D+2日夜班(00:00-08:00)，结束时间=D+2日08:00
                return DateUtil.offsetHour(DateUtil.beginOfDay(DateUtil.offsetDay(dDay, 2)), 8);
            case 6:
                // 6班：D+2日早班(08:00-16:00)，结束时间=D+2日16:00
                return DateUtil.offsetHour(DateUtil.beginOfDay(DateUtil.offsetDay(dDay, 2)), 16);
            default:
                return null;
        }
    }

    /**
     * 触发滚动更新：遍历6个班次，对有计划量的班次触发同班次内时间重算
     *
     * @param triggerType 触发类型：1-插单，2-转机台，3-调量，4-删除
     * @param sourceId    触发源排程记录ID
     * @param record      排程记录（含机台、钢丝圈代码、6班计划量）
     */
    private void triggerRollingUpdateForAllShifts(String triggerType, Long sourceId, GsqScheduleResult record) {
        if (record == null || record.getScheduleDate() == null || StringUtils.isBlank(record.getMachineCode())) {
            log.warn("触发滚动更新跳过：排程记录信息不完整，sourceId={}", sourceId);
            return;
        }

        for (int shiftIndex = 1; shiftIndex <= 6; shiftIndex++) {
            Integer planQty = getPlanQtyByShiftIndex(record, shiftIndex);
            // 仅对有计划量的班次触发滚动更新
            if (planQty == null || planQty <= 0) {
                continue;
            }
            try {
                GsqRollingUpdateResult result = gsqRollingUpdateService.manualRollingUpdate(
                        triggerType, sourceId, record.getScheduleDate(),
                        shiftIndex, record.getMachineCode(), record.getSteelRingCode());
                if (result.isSuccess()) {
                    log.info("钢丝圈滚动更新成功，班次：{}，影响记录数：{}", shiftIndex, result.getAffectedCount());
                } else {
                    log.warn("钢丝圈滚动更新失败，班次：{}，原因：{}", shiftIndex, result.getErrorMsg());
                }
            } catch (Exception e) {
                // 滚动更新失败不影响主操作，仅记录日志
                log.error("钢丝圈滚动更新异常，班次：{}，sourceId：{}，原因：{}", shiftIndex, sourceId, e.getMessage(), e);
            }
        }
    }
}

package com.zlt.aps.gsq.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
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
import com.zlt.aps.gsq.api.domain.vo.GsqScheduleShiftDateVO;
import com.zlt.aps.gsq.engine.service.GsqEngineService;
import com.zlt.aps.gsq.mapper.GsqScheduleResultMapper;
import com.zlt.aps.gsq.service.GsqDispatcherLogService;
import com.zlt.aps.gsq.service.GsqMachineInfoService;
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
            return AjaxResult.error("排程日期不能为空");
        }
        String scheduleDateStr = DateUtil.formatDate(queryVO.getScheduleDateQuery());
        String factoryCode = StringUtils.isBlank(queryVO.getFactoryCode()) ? factoryService.getFactoryCode() : queryVO.getFactoryCode();
        log.info("钢丝圈自动排程开始，排程日期：{}，分厂：{}", scheduleDateStr, factoryCode);
        try {
            gsqEngineService.autoGsqSchedule(scheduleDateStr, factoryCode);
            log.info("钢丝圈自动排程成功，排程日期：{}，分厂：{}", scheduleDateStr, factoryCode);
            return AjaxResult.success("自动排程成功");
        } catch (Exception e) {
            log.error("钢丝圈自动排程失败，排程日期：" + scheduleDateStr, e);
            return AjaxResult.error("自动排程失败：" + e.getMessage());
        }
    }

    // ==================== 插单 ====================

    /**
     * 插单前校验
     * 校验规则：
     * 1. 排程日期不能为空
     * 2. 钢丝圈代码不能为空
     * 3. 机台编号不能为空
     * 4. 6个班次中至少有一个班次的计划量有值
     * 5. 有计划量的班次，顺序也必须有值；反之亦然
     * 6. 只能往当前班次或后续班次插单
     * 7. 插单只能加到第二个在产规格之后
     *
     * @param dto 插单数据
     * @return 校验结果
     */
    @Override
    public AjaxResult validateInsertOrder(GsqInsertOrderDTO dto) {
        // 1. 排程日期校验
        if (dto.getScheduleDate() == null) {
            return AjaxResult.error("排程日期不能为空");
        }

        // 2. 钢丝圈代码校验
        if (ObjectUtils.isEmpty(dto.getSteelRingCode())) {
            return AjaxResult.error("钢丝圈代码不能为空");
        }

        // 3. 机台编号校验
        if (ObjectUtils.isEmpty(dto.getMachineCode())) {
            return AjaxResult.error("机台编号不能为空");
        }

        // 4. 至少一个班次有计划量
        boolean hasAnyPlanQty = false;
        for (int i = 1; i <= 6; i++) {
            Integer planQty = getPlanQtyByClassIndex(dto, i);
            if (planQty != null && planQty > 0) {
                hasAnyPlanQty = true;
                break;
            }
        }
        if (!hasAnyPlanQty) {
            return AjaxResult.error("至少一个班次的计划量必须有值");
        }

        // 5. 有计划量的班次顺序必须有值，反之亦然
        for (int i = 1; i <= 6; i++) {
            Integer planQty = getPlanQtyByClassIndex(dto, i);
            Integer sequence = getSequenceByClassIndex(dto, i);
            boolean hasPlanQty = planQty != null && planQty > 0;
            boolean hasSequence = sequence != null && sequence > 0;
            if (hasPlanQty && !hasSequence) {
                return AjaxResult.error("第" + i + "班有计划量，顺序也必须有值");
            }
            if (hasSequence && !hasPlanQty) {
                return AjaxResult.error("第" + i + "班有顺序，计划量也必须有值");
            }
        }

        // 6. 只能往当前班次或后续班次插单
        int currentShiftIndex = resolveCurrentShiftIndex(dto.getScheduleDate());
        for (int i = 1; i < currentShiftIndex; i++) {
            Integer planQty = getPlanQtyByClassIndex(dto, i);
            if (planQty != null && planQty > 0) {
                return AjaxResult.error("不能往历史班次插单，当前班次为第" + currentShiftIndex + "班");
            }
        }

        // 7. 插单只能加到第二个在产规格之后
        LambdaQueryWrapper<GsqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GsqScheduleResult::getScheduleDate, dto.getScheduleDate());
        wrapper.eq(GsqScheduleResult::getMachineCode, dto.getMachineCode());
        wrapper.eq(GsqScheduleResult::getIsDelete, 0);
        List<GsqScheduleResult> existingList = gsqScheduleResultMapper.selectList(wrapper);
        existingList.sort(Comparator.comparingInt(this::getMinSequenceOfRecord));
        if (existingList.size() >= 2) {
            int secondSpecMinSeq = getMinSequenceFromSecondSpec(existingList);
            for (int i = 1; i <= 6; i++) {
                Integer sequence = getSequenceByClassIndex(dto, i);
                if (sequence != null && sequence < secondSpecMinSeq) {
                    return AjaxResult.error("插单只能加到第二个在产规格之后，顺序号不能小于" + secondSpecMinSeq);
                }
            }
        }

        // 8. 唯一性校验：同一排程日期、机台、钢丝圈不允许重复
        GsqScheduleResult uniqueCheck = new GsqScheduleResult();
        uniqueCheck.setScheduleDate(dto.getScheduleDate());
        uniqueCheck.setSteelRingCode(dto.getSteelRingCode());
        uniqueCheck.setMachineCode(dto.getMachineCode());
        if (UserConstants.NOT_UNIQUE.equals(checkUnique(uniqueCheck))) {
            return AjaxResult.error("同一排程日期、机台、钢丝圈已存在排程记录，不允许重复插单");
        }

        return AjaxResult.success("校验通过");
    }

    /**
     * 插单
     *
     * @param dto 插单数据
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult insertOrder(GsqInsertOrderDTO dto) {
        // 先执行校验
        AjaxResult validateResult = validateInsertOrder(dto);
        if (!validateResult.get(AjaxResult.CODE_TAG).equals(200)) {
            return validateResult;
        }

        // 构建排程记录实体
        GsqScheduleResult entity = new GsqScheduleResult();
        entity.setScheduleDate(dto.getScheduleDate());
        entity.setSteelRingCode(dto.getSteelRingCode());
        entity.setTwiningDiscCode(dto.getTwiningDiscCode());
        entity.setMachineCode(dto.getMachineCode());
        entity.setDataSource("1"); // 插单
        entity.setIsRelease("0"); // 未发布

        // 填充6个班次字段
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

        // TODO 批次号、工单号生成逻辑待 GsqEngineService 提供 public 方法后补充
        // 当前复用同日期已有排程的批次号，工单号留空
        LambdaQueryWrapper<GsqScheduleResult> batchWrapper = new LambdaQueryWrapper<>();
        batchWrapper.eq(GsqScheduleResult::getScheduleDate, dto.getScheduleDate());
        batchWrapper.eq(GsqScheduleResult::getIsDelete, 0);
        batchWrapper.isNotNull(GsqScheduleResult::getBatchNo);
        batchWrapper.last("LIMIT 1");
        GsqScheduleResult existingRecord = gsqScheduleResultMapper.selectOne(batchWrapper);
        if (existingRecord != null) {
            entity.setCxBatchNo(existingRecord.getCxBatchNo());
            entity.setBatchNo(existingRecord.getBatchNo());
        }

        // 回填胎圈排程结果数据到 TQ_CLASS1~6_PLAN
        fillTqPlanQty(Collections.singletonList(entity));

        // 插入数据库
        gsqScheduleResultMapper.insert(entity);

        // TODO 滚动更新：待 IGsqRollingUpdateService 实现后启用
        // triggerRollingUpdateForAllShifts("1", entity.getId(), entity);

        // 记录调度日志（6班次制，操作类型：2-插单，无操作前数据）
        recordDispatcherLog(ApsConstant.DISPATCHER_OPER_INSERT_ORDER, entity, null, entity);

        log.info("钢丝圈排程插单成功，排程日期：{}，钢丝圈代码：{}，机台：{}",
                dto.getScheduleDate(), dto.getSteelRingCode(), dto.getMachineCode());

        return AjaxResult.success("插单成功");
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
            return AjaxResult.error("请选择需要转机台的记录");
        }
        if (ObjectUtils.isEmpty(dto.getNewMachineCode())) {
            return AjaxResult.error("新机台编号不能为空");
        }
        if (dto.getNewMachineCode().equals(dto.getOldMachineCode())) {
            return AjaxResult.error("新机台与原机台不能相同");
        }

        // 校验排程记录是否存在
        GsqScheduleResult record = gsqScheduleResultMapper.selectById(dto.getId());
        if (record == null || Objects.equals(record.getIsDelete(), 1)) {
            return AjaxResult.error("排程记录不存在或已删除");
        }

        // 校验新机台是否在钢丝圈机台管理中存在且启用
        GsqMachineInfo queryMachine = new GsqMachineInfo();
        queryMachine.setMachineCode(dto.getNewMachineCode());
        List<GsqMachineInfo> machineList = gsqMachineInfoService.listMachineInfo(queryMachine);
        if (CollectionUtils.isEmpty(machineList)) {
            return AjaxResult.error("新机台不存在或已停用：" + dto.getNewMachineCode());
        }

        return AjaxResult.success("校验通过");
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

        // TODO 滚动更新：待 IGsqRollingUpdateService 实现后启用
        // triggerRollingUpdateForAllShifts("2", dto.getId(), record);
        // TqScheduleResult newMachineRecord = new TqScheduleResult();
        // BeanUtil.copyProperties(record, newMachineRecord);
        // newMachineRecord.setMachineCode(dto.getNewMachineCode());
        // triggerRollingUpdateForAllShifts("2", dto.getId(), newMachineRecord);

        // 记录调度日志（6班次制，操作类型：0-转机台，操作前=原机台记录，操作后=新机台记录）
        GsqScheduleResult newMachineRecord = new GsqScheduleResult();
        BeanUtil.copyProperties(record, newMachineRecord);
        newMachineRecord.setMachineCode(dto.getNewMachineCode());
        recordDispatcherLog(ApsConstant.DISPATCHER_OPER_MACHINE, record, record, newMachineRecord);

        log.info("钢丝圈排程转机台成功，id：{}，原机台：{}，新机台：{}",
                dto.getId(), oldMachineCode, dto.getNewMachineCode());

        return AjaxResult.success("转机台成功");
    }

    // ==================== 调量 ====================

    /**
     * 调量前校验
     * 校验规则：
     * 1. 排程记录必须存在且未删除
     * 2. 至少有一个班次的计划量被修改
     * 3. 计划量不能小于0
     * 4. 历史班次不允许修改计划量
     * 5. 非历史班次的计划量不能小于完成量
     *
     * @param entity 调量数据
     * @return 校验结果
     */
    @Override
    public AjaxResult validateChangeQty(GsqScheduleResult entity) {
        if (entity == null || entity.getId() == null) {
            return AjaxResult.error("请选择需要调量的排程记录");
        }

        GsqScheduleResult record = gsqScheduleResultMapper.selectById(entity.getId());
        if (record == null || Objects.equals(record.getIsDelete(), 1)) {
            return AjaxResult.error("排程记录不存在或已删除");
        }

        Date now = new Date();
        boolean hasAdjustField = false;
        List<String> errorMessages = new ArrayList<>();

        for (int shiftIndex = 1; shiftIndex <= 6; shiftIndex++) {
            Integer newPlanQty = getPlanQtyByShiftIndex(entity, shiftIndex);
            Integer oldPlanQty = getPlanQtyByShiftIndex(record, shiftIndex);

            // 只检查被修改的班次
            if (newPlanQty == null || Objects.equals(newPlanQty, oldPlanQty)) {
                continue;
            }

            hasAdjustField = true;

            // 规则3：计划量不能小于0
            if (newPlanQty < 0) {
                errorMessages.add(String.format("第%d班计划量不能小于0", shiftIndex));
                continue;
            }

            // 判断是否为历史班次
            boolean historyShift = isHistoryShift(record, shiftIndex, now);

            if (historyShift) {
                // 规则4：历史班次不允许修改
                errorMessages.add(String.format("不能修改历史班次（第%d班）的计划量", shiftIndex));
            } else {
                // 规则5：非历史班次计划量不能小于完成量
                Integer finishQty = getFinishQtyByShiftIndex(record, shiftIndex);
                if (finishQty != null && finishQty > 0 && newPlanQty < finishQty) {
                    errorMessages.add(String.format("第%d班计划量不能小于完成量%d", shiftIndex, finishQty));
                }
            }
        }

        if (!hasAdjustField) {
            errorMessages.add("未检测到需要调整的计划量");
        }

        if (!errorMessages.isEmpty()) {
            return AjaxResult.error(String.join("；", errorMessages));
        }

        return AjaxResult.success("校验通过");
    }

    /**
     * 调量
     * 业务逻辑：
     * 1. 前置校验
     * 2. 更新各班次计划量和原因分析
     * 3. 如果原排程已发布成功，更新发布状态为待发布
     * 4. 记录操作日志
     *
     * @param entity 调量数据
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult changeQty(GsqScheduleResult entity) {
        // 1. 前置校验
        AjaxResult validateResult = validateChangeQty(entity);
        if (!validateResult.get(AjaxResult.CODE_TAG).equals(200)) {
            return validateResult;
        }

        // 2. 查询原记录
        GsqScheduleResult record = gsqScheduleResultMapper.selectById(entity.getId());
        if (record == null) {
            return AjaxResult.error("排程记录不存在或已删除");
        }

        // 3. 构建更新wrapper
        LambdaUpdateWrapper<GsqScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(GsqScheduleResult::getId, entity.getId());

        boolean hasChange = false;

        // 4. 更新各班次计划量和原因分析
        for (int shiftIndex = 1; shiftIndex <= 6; shiftIndex++) {
            Integer newPlanQty = getPlanQtyByShiftIndex(entity, shiftIndex);
            Integer oldPlanQty = getPlanQtyByShiftIndex(record, shiftIndex);
            String newAnalysis = getAnalysisByShiftIndex(entity, shiftIndex);
            String oldAnalysis = getAnalysisByShiftIndex(record, shiftIndex);

            // 更新被修改的计划量
            if (newPlanQty != null && !Objects.equals(newPlanQty, oldPlanQty)) {
                setPlanQtyToUpdateWrapper(updateWrapper, shiftIndex, newPlanQty);
                hasChange = true;
            }

            // 更新原因分析（非空且与原值不同时更新）
            if (newAnalysis != null && !newAnalysis.equals(oldAnalysis)) {
                setAnalysisToUpdateWrapper(updateWrapper, shiftIndex, newAnalysis);
                hasChange = true;
            }
        }

        if (!hasChange) {
            return AjaxResult.error("没有需要保存的修改内容");
        }

        // 5. 更新备注
        if (entity.getRemark() != null && !entity.getRemark().equals(record.getRemark())) {
            updateWrapper.set(GsqScheduleResult::getRemark, entity.getRemark());
        }

        // 6. 状态更新：如果原排程已发布成功，更新为待发布（需重新下发MES）
        if (ApsConstant.IS_RELEASE.equals(record.getIsRelease())) {
            updateWrapper.set(GsqScheduleResult::getIsRelease, ApsConstant.WAIT_RELEASING);
        }

        // 7. 执行更新
        gsqScheduleResultMapper.update(null, updateWrapper);

        log.info("钢丝圈排程调量成功，id：{}，钢丝圈代码：{}，机台：{}",
                entity.getId(), record.getSteelRingCode(), record.getMachineCode());

        // TODO 滚动更新：待 IGsqRollingUpdateService 实现后启用
        // triggerRollingUpdateForAllShifts("3", entity.getId(), record);

        // 8. 记录调度日志（6班次制，操作类型：1-调量，操作前=原记录，操作后=更新后记录）
        GsqScheduleResult afterRecord = gsqScheduleResultMapper.selectById(entity.getId());
        recordDispatcherLog(ApsConstant.DISPATCHER_OPER_PLAN, record, record, afterRecord);

        return AjaxResult.success("调量成功");
    }

    // ==================== 逻辑删除 ====================

    /**
     * 逻辑删除排程记录
     * 只能删除发布成功次数等于0的计划
     *
     * @param ids 需要删除的记录ID列表
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult logicDeleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return AjaxResult.error("请选择需要删除的记录");
        }

        for (Long id : ids) {
            GsqScheduleResult record = gsqScheduleResultMapper.selectById(id);
            if (record == null) {
                continue;
            }
            // 校验：已发布成功的计划不允许删除
            if (ApsConstant.IS_RELEASE.equals(record.getIsRelease())) {
                return AjaxResult.error("已发布成功的计划不允许删除，只能调量。钢丝圈代码：" + record.getSteelRingCode());
            }

            // 逻辑删除：更新 is_delete = 1
            LambdaUpdateWrapper<GsqScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(GsqScheduleResult::getId, id)
                    .set(GsqScheduleResult::getIsDelete, 1);
            gsqScheduleResultMapper.update(null, updateWrapper);

            // TODO 滚动更新：待 IGsqRollingUpdateService 实现后启用
            // triggerRollingUpdateForAllShifts("4", id, record);

            // 记录调度日志（6班次制，操作类型：3-删除，操作前=原记录，操作后=null）
            recordDispatcherLog(ApsConstant.DISPATCHER_OPER_DELETE, record, record, null);

            log.info("钢丝圈排程逻辑删除成功，id：{}，钢丝圈代码：{}", id, record.getSteelRingCode());
        }

        return AjaxResult.success("删除成功");
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

        // 4. 将6班数据拆分为3天的下发列表
        List<GsqScheduleResultIssue> issueList = new ArrayList<>();
        for (GsqScheduleResult source : scheduleList) {
            // D日 = 排程日期 - 2
            Date dDay = DateUtil.offsetDay(scheduleDate, -2);
            Date dPlus1Day = DateUtil.offsetDay(scheduleDate, -1);
            Date dPlus2Day = scheduleDate;

            // Day1(D日)：钢丝圈1班→MES中班
            GsqScheduleResultIssue day1Issue = this.buildDay1Issue(source, dDay);
            issueList.add(day1Issue);

            // Day2(D+1日)：钢丝圈2班→MES夜班, 钢丝圈3班→MES早班, 钢丝圈4班→MES中班
            GsqScheduleResultIssue day2Issue = this.buildDay2Issue(source, dPlus1Day);
            issueList.add(day2Issue);

            // Day3(D+2日)：钢丝圈5班→MES夜班, 钢丝圈6班→MES早班
            GsqScheduleResultIssue day3Issue = this.buildDay3Issue(source, dPlus2Day);
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
            // 根据返回结果更新发布状态：成功→已发布，失败→发布失败
            String status = ajaxResult.get(AjaxResult.CODE_TAG).equals(200)
                    ? ApsConstant.IS_RELEASE
                    : ApsConstant.FAILURE_RELEASE;
            LambdaUpdateWrapper<GsqScheduleResult> resultWrapper = new LambdaUpdateWrapper<>();
            resultWrapper.in(GsqScheduleResult::getId, releaseIds);
            resultWrapper.set(GsqScheduleResult::getIsRelease, status);
            gsqScheduleResultMapper.update(null, resultWrapper);
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
     * @param source 钢丝圈排程结果
     * @param dDay   D日日期
     * @return D日下发对象
     */
    private GsqScheduleResultIssue buildDay1Issue(GsqScheduleResult source, Date dDay) {
        GsqScheduleResultIssue issue = buildBaseIssue(source, dDay);
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
     * @param source    钢丝圈排程结果
     * @param dPlus1Day D+1日日期
     * @return D+1日下发对象
     */
    private GsqScheduleResultIssue buildDay2Issue(GsqScheduleResult source, Date dPlus1Day) {
        GsqScheduleResultIssue issue = buildBaseIssue(source, dPlus1Day);
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
     * @param source    钢丝圈排程结果
     * @param dPlus2Day D+2日日期
     * @return D+2日下发对象
     */
    private GsqScheduleResultIssue buildDay3Issue(GsqScheduleResult source, Date dPlus2Day) {
        GsqScheduleResultIssue issue = buildBaseIssue(source, dPlus2Day);
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
     * @return 基础下发对象
     */
    private GsqScheduleResultIssue buildBaseIssue(GsqScheduleResult source, Date scheduleDate) {
        GsqScheduleResultIssue issue = new GsqScheduleResultIssue();
        // 日期转换：Date → LocalDate
        issue.setScheduleDate(scheduleDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        // 基础信息
        issue.setCxBatchNo(source.getCxBatchNo());
        issue.setBatchNo(source.getBatchNo());
        issue.setOrderNo(source.getOrderNo());
        // 物料信息
        issue.setSteelRingCode(source.getSteelRingCode());
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
            @SuppressWarnings("unchecked")
            List<TqScheduleResult> tqList = (List<TqScheduleResult>) tqResult.getRows();
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
     *
     * @param record 排程记录
     * @return 最小顺序号，无顺序号时返回 Integer.MAX_VALUE
     */
    private int getMinSequenceOfRecord(GsqScheduleResult record) {
        int minSeq = Integer.MAX_VALUE;
        if (record.getClass1Sequence() != null && record.getClass1Sequence() > 0) {
            minSeq = Math.min(minSeq, record.getClass1Sequence());
        }
        if (record.getClass2Sequence() != null && record.getClass2Sequence() > 0) {
            minSeq = Math.min(minSeq, record.getClass2Sequence());
        }
        if (record.getClass3Sequence() != null && record.getClass3Sequence() > 0) {
            minSeq = Math.min(minSeq, record.getClass3Sequence());
        }
        if (record.getClass4Sequence() != null && record.getClass4Sequence() > 0) {
            minSeq = Math.min(minSeq, record.getClass4Sequence());
        }
        if (record.getClass5Sequence() != null && record.getClass5Sequence() > 0) {
            minSeq = Math.min(minSeq, record.getClass5Sequence());
        }
        if (record.getClass6Sequence() != null && record.getClass6Sequence() > 0) {
            minSeq = Math.min(minSeq, record.getClass6Sequence());
        }
        return minSeq;
    }

    /**
     * 获取第二个在产规格的最小顺序号
     * 用于校验插单只能加到第二个在产规格之后
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
        if (secondRecord.getClass1Sequence() != null && secondRecord.getClass1Sequence() > 0) {
            minSeq = Math.min(minSeq, secondRecord.getClass1Sequence());
        }
        if (secondRecord.getClass2Sequence() != null && secondRecord.getClass2Sequence() > 0) {
            minSeq = Math.min(minSeq, secondRecord.getClass2Sequence());
        }
        if (secondRecord.getClass3Sequence() != null && secondRecord.getClass3Sequence() > 0) {
            minSeq = Math.min(minSeq, secondRecord.getClass3Sequence());
        }
        if (secondRecord.getClass4Sequence() != null && secondRecord.getClass4Sequence() > 0) {
            minSeq = Math.min(minSeq, secondRecord.getClass4Sequence());
        }
        if (secondRecord.getClass5Sequence() != null && secondRecord.getClass5Sequence() > 0) {
            minSeq = Math.min(minSeq, secondRecord.getClass5Sequence());
        }
        if (secondRecord.getClass6Sequence() != null && secondRecord.getClass6Sequence() > 0) {
            minSeq = Math.min(minSeq, secondRecord.getClass6Sequence());
        }
        return minSeq == Integer.MAX_VALUE ? 1 : minSeq;
    }

    /**
     * 根据班次索引获取实体中的计划量
     *
     * @param entity     排程结果实体
     * @param shiftIndex 班次索引（1~6）
     * @return 计划量
     */
    private Integer getPlanQtyByShiftIndex(GsqScheduleResult entity, int shiftIndex) {
        switch (shiftIndex) {
            case 1: return entity.getClass1PlanQty();
            case 2: return entity.getClass2PlanQty();
            case 3: return entity.getClass3PlanQty();
            case 4: return entity.getClass4PlanQty();
            case 5: return entity.getClass5PlanQty();
            case 6: return entity.getClass6PlanQty();
            default: return null;
        }
    }

    /**
     * 根据班次索引获取实体中的完成量
     *
     * @param entity     排程结果实体
     * @param shiftIndex 班次索引（1~6）
     * @return 完成量
     */
    private Integer getFinishQtyByShiftIndex(GsqScheduleResult entity, int shiftIndex) {
        switch (shiftIndex) {
            case 1: return entity.getClass1FinishQty();
            case 2: return entity.getClass2FinishQty();
            case 3: return entity.getClass3FinishQty();
            case 4: return entity.getClass4FinishQty();
            case 5: return entity.getClass5FinishQty();
            case 6: return entity.getClass6FinishQty();
            default: return null;
        }
    }

    /**
     * 根据班次索引获取实体中的原因分析
     *
     * @param entity     排程结果实体
     * @param shiftIndex 班次索引（1~6）
     * @return 原因分析
     */
    private String getAnalysisByShiftIndex(GsqScheduleResult entity, int shiftIndex) {
        switch (shiftIndex) {
            case 1: return entity.getClass1Analysis();
            case 2: return entity.getClass2Analysis();
            case 3: return entity.getClass3Analysis();
            case 4: return entity.getClass4Analysis();
            case 5: return entity.getClass5Analysis();
            case 6: return entity.getClass6Analysis();
            default: return null;
        }
    }

    /**
     * 设置指定班次计划量到UpdateWrapper
     *
     * @param updateWrapper 更新条件
     * @param shiftIndex    班次索引（1~6）
     * @param planQty       计划量
     */
    private void setPlanQtyToUpdateWrapper(LambdaUpdateWrapper<GsqScheduleResult> updateWrapper,
                                           int shiftIndex, Integer planQty) {
        switch (shiftIndex) {
            case 1: updateWrapper.set(GsqScheduleResult::getClass1PlanQty, planQty); break;
            case 2: updateWrapper.set(GsqScheduleResult::getClass2PlanQty, planQty); break;
            case 3: updateWrapper.set(GsqScheduleResult::getClass3PlanQty, planQty); break;
            case 4: updateWrapper.set(GsqScheduleResult::getClass4PlanQty, planQty); break;
            case 5: updateWrapper.set(GsqScheduleResult::getClass5PlanQty, planQty); break;
            case 6: updateWrapper.set(GsqScheduleResult::getClass6PlanQty, planQty); break;
            default: break;
        }
    }

    /**
     * 设置指定班次原因分析到UpdateWrapper
     *
     * @param updateWrapper 更新条件
     * @param shiftIndex    班次索引（1~6）
     * @param analysis      原因分析
     */
    private void setAnalysisToUpdateWrapper(LambdaUpdateWrapper<GsqScheduleResult> updateWrapper,
                                            int shiftIndex, String analysis) {
        switch (shiftIndex) {
            case 1: updateWrapper.set(GsqScheduleResult::getClass1Analysis, analysis); break;
            case 2: updateWrapper.set(GsqScheduleResult::getClass2Analysis, analysis); break;
            case 3: updateWrapper.set(GsqScheduleResult::getClass3Analysis, analysis); break;
            case 4: updateWrapper.set(GsqScheduleResult::getClass4Analysis, analysis); break;
            case 5: updateWrapper.set(GsqScheduleResult::getClass5Analysis, analysis); break;
            case 6: updateWrapper.set(GsqScheduleResult::getClass6Analysis, analysis); break;
            default: break;
        }
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
}

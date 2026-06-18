package com.zlt.aps.tq.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tq.api.domain.dto.TqChangeMachineDTO;
import com.zlt.aps.tq.api.domain.dto.TqInsertOrderDTO;
import com.zlt.aps.tq.api.domain.entity.TqNewScheduleResult;
import com.zlt.aps.tq.mapper.TqNewScheduleResultMapper;
import com.zlt.aps.tq.service.ITqNewScheduleResultService;
import com.zlt.bill.common.service.AbstractDocService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎圈排程结果Service实现类（新版）
 *
 * @author APS
 */
@Slf4j
@Service
public class TqNewScheduleResultServiceImpl extends AbstractDocService<TqNewScheduleResult> implements ITqNewScheduleResultService {

    @Autowired
    private TqNewScheduleResultMapper tqNewScheduleResultMapper;

    @Override
    public String getDocTypeCode() {
        return "TQ_NEW_SCHEDULE_RESULT";
    }

    /**
     * 插单前校验
     * 校验规则：
     * 1. 排程日期不能为空，且需在生产周期内
     * 2. 胎圈代码不能为空，施工必须存在
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
    public AjaxResult validateInsertOrder(TqInsertOrderDTO dto) {
        // 1. 排程日期校验
        if (dto.getScheduleDate() == null) {
            return AjaxResult.error("排程日期不能为空");
        }

        // 2. 胎圈代码校验
        if (ObjectUtils.isEmpty(dto.getBeadCode())) {
            return AjaxResult.error("胎圈代码不能为空");
        }
        // TODO 校验施工是否存在（需查询施工表 T_TQ_CONSTRUCTION_INFO）
        // List<TqScheduleBaseInfoVo> baseInfoList = tqEngineMapper.listTqScheduleBaseInfo(
        //     Collections.singletonList(dto.getBeadCode()), "");
        // if (baseInfoList == null || baseInfoList.isEmpty()) {
        //     return AjaxResult.error("胎圈规格有误，施工不存在");
        // }

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
        int currentShiftIndex = resolveCurrentShiftIndex();
        for (int i = 1; i < currentShiftIndex; i++) {
            Integer planQty = getPlanQtyByClassIndex(dto, i);
            if (planQty != null && planQty > 0) {
                return AjaxResult.error("不能往历史班次插单，当前班次为第" + currentShiftIndex + "班");
            }
        }

        // 7. 插单只能加到第二个在产规格之后
        // 查询该机台该日期已有排程记录
        LambdaQueryWrapper<TqNewScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TqNewScheduleResult::getScheduleDate, dto.getScheduleDate());
        wrapper.eq(TqNewScheduleResult::getMachineCode, dto.getMachineCode());
        wrapper.eq(TqNewScheduleResult::getIsDelete, 0);
        wrapper.orderByAsc(TqNewScheduleResult::getClass1Sequence);
        List<TqNewScheduleResult> existingList = tqNewScheduleResultMapper.selectList(wrapper);
        if (existingList.size() >= 2) {
            // 第二个在产规格的最大顺序号
            int secondSpecMaxSeq = getMinSequenceFromSecondSpec(existingList);
            for (int i = 1; i <= 6; i++) {
                Integer sequence = getSequenceByClassIndex(dto, i);
                if (sequence != null && sequence < secondSpecMaxSeq) {
                    return AjaxResult.error("插单只能加到第二个在产规格之后，顺序号不能小于" + secondSpecMaxSeq);
                }
            }
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
    public AjaxResult insertOrder(TqInsertOrderDTO dto) {
        // 先执行校验
        AjaxResult validateResult = validateInsertOrder(dto);
        if (!validateResult.get(AjaxResult.CODE_TAG).equals(200)) {
            return validateResult;
        }

        // 构建排程记录实体
        TqNewScheduleResult entity = new TqNewScheduleResult();
        entity.setScheduleDate(dto.getScheduleDate());
        entity.setBeadCode(dto.getBeadCode());
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

        // TODO 生成批次号、工单号

        // 插入数据库
        tqNewScheduleResultMapper.insert(entity);

        // TODO: 滚动更新后续排程
        log.info("胎圈排程插单成功，排程日期：{}，胎圈代码：{}，机台：{}",
                dto.getScheduleDate(), dto.getBeadCode(), dto.getMachineCode());

        // TODO: 记录排程修改日志

        return AjaxResult.success("插单成功");
    }

    /**
     * 转机台前校验
     *
     * @param dto 转机台数据
     * @return 校验结果
     */
    @Override
    public AjaxResult validateChangeMachine(TqChangeMachineDTO dto) {
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
        TqNewScheduleResult record = tqNewScheduleResultMapper.selectById(dto.getId());
        if (record == null || Objects.equals(record.getIsDelete(), 1)) {
            return AjaxResult.error("排程记录不存在或已删除");
        }

        // TODO 校验新机台是否在胎圈机台管理中存在

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
    public AjaxResult changeMachine(TqChangeMachineDTO dto) {
        // 先执行校验
        AjaxResult validateResult = validateChangeMachine(dto);
        if (!validateResult.get(AjaxResult.CODE_TAG).equals(200)) {
            return validateResult;
        }

        // 查询原记录
        TqNewScheduleResult record = tqNewScheduleResultMapper.selectById(dto.getId());
        String oldMachineCode = record.getMachineCode();

        // 更新机台编号
        LambdaUpdateWrapper<TqNewScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(TqNewScheduleResult::getId, dto.getId())
                .set(TqNewScheduleResult::getMachineCode, dto.getNewMachineCode());

        // 如果原排程已发布成功，更新发布状态为待发布
        if ("1".equals(record.getIsRelease())) {
            updateWrapper.set(TqNewScheduleResult::getIsRelease, "0");
        }

        tqNewScheduleResultMapper.update(null, updateWrapper);

        // TODO: 记录排程修改日志（修改时间、修改人、修改栏位、修改前、修改后）
        log.info("胎圈排程转机台成功，id：{}，原机台：{}，新机台：{}",
                dto.getId(), oldMachineCode, dto.getNewMachineCode());

        // TODO: 滚动更新原机台和新机台的后续排程

        return AjaxResult.success("转机台成功");
    }

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
    public AjaxResult validateChangeQty(TqNewScheduleResult entity) {
        if (entity == null || entity.getId() == null) {
            return AjaxResult.error("请选择需要调量的排程记录");
        }

        TqNewScheduleResult record = tqNewScheduleResultMapper.selectById(entity.getId());
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
    public AjaxResult changeQty(TqNewScheduleResult entity) {
        // 1. 前置校验
        AjaxResult validateResult = validateChangeQty(entity);
        if (!validateResult.get(AjaxResult.CODE_TAG).equals(200)) {
            return validateResult;
        }

        // 2. 查询原记录
        TqNewScheduleResult record = tqNewScheduleResultMapper.selectById(entity.getId());
        if (record == null) {
            return AjaxResult.error("排程记录不存在或已删除");
        }

        // 3. 构建更新wrapper
        LambdaUpdateWrapper<TqNewScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(TqNewScheduleResult::getId, entity.getId());

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
            updateWrapper.set(TqNewScheduleResult::getRemark, entity.getRemark());
        }

        // 6. 状态更新：如果原排程已发布成功（isRelease=1），更新为待发布（isRelease=0）
        // 发布状态：0-未发布，1-已发布，2-发布失败，3-待发布
        if ("1".equals(record.getIsRelease())) {
            updateWrapper.set(TqNewScheduleResult::getIsRelease, "3");
        }

        // 7. 执行更新
        tqNewScheduleResultMapper.update(null, updateWrapper);

        log.info("胎圈排程调量成功，id：{}，胎圈代码：{}，机台：{}",
                entity.getId(), record.getBeadCode(), record.getMachineCode());

        // TODO: 记录排程修改日志
        // TODO: 滚动更新后续排程

        return AjaxResult.success("调量成功");
    }

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
            TqNewScheduleResult record = tqNewScheduleResultMapper.selectById(id);
            if (record == null) {
                continue;
            }
            // 校验：已发布成功的计划不允许删除
            if ("1".equals(record.getIsRelease())) {
                return AjaxResult.error("已发布成功的计划不允许删除，只能调量。胎圈代码：" + record.getBeadCode());
            }

            // 逻辑删除：更新 is_delete = 1
            LambdaUpdateWrapper<TqNewScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(TqNewScheduleResult::getId, id)
                    .set(TqNewScheduleResult::getIsDelete, 1);
            tqNewScheduleResultMapper.update(null, updateWrapper);

            // TODO: 滚动更新后续排程
            // TODO: 记录排程修改日志
            log.info("胎圈排程逻辑删除成功，id：{}，胎圈代码：{}", id, record.getBeadCode());
        }

        return AjaxResult.success("删除成功");
    }

    /**
     * 发布排程到MES
     *
     * @param queryVO 查询条件（含排程日期等）
     * @return 结果
     */
    @Override
    public AjaxResult publish(TqNewScheduleResult queryVO) {
        // TODO 发布业务逻辑待实现
        log.info("胎圈排程发布，排程日期：{}", queryVO.getScheduleDateQuery());
        return AjaxResult.success();
    }

    /**
     * 查询排程日期是否已发布
     *
     * @param scheduleDate 排程日期
     * @return 是否已发布
     */
    @Override
    public Boolean isPublish(Date scheduleDate) {
        // TODO 待实现：查询该排程日期下是否所有记录都已发布
        return false;
    }

    // ==================== 私有方法 ====================

    /**
     * 根据班次索引获取DTO中的计划量
     */
    private Integer getPlanQtyByClassIndex(TqInsertOrderDTO dto, int classIndex) {
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
    private Integer getSequenceByClassIndex(TqInsertOrderDTO dto, int classIndex) {
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
     * 胎圈排程6班次时间窗口：
     * 1班：D日中班(16:00-24:00)
     * 2班：D+1日夜班(00:00-08:00)
     * 3班：D+1日早班(08:00-16:00)
     * 4班：D+1日中班(16:00-24:00)
     * 5班：D+2日夜班(00:00-08:00)
     * 6班：D+2日早班(08:00-16:00)
     * D = 排程日期 - 2（即今天）
     *
     * @return 当前班次索引
     */
    private int resolveCurrentShiftIndex() {
        Date now = new Date();
        int hour = DateUtil.hour(now, true);
        // D日就是今天
        if (hour >= 16) {
            // 16:00-24:00 属于中班（1班或4班）
            // 简化处理：返回1，表示当前处于中班时段
            return 1;
        } else if (hour >= 8) {
            // 08:00-16:00 属于早班（3班或6班）
            return 3;
        } else {
            // 00:00-08:00 属于夜班（2班或5班）
            return 2;
        }
    }

    /**
     * 获取第二个在产规格的最小顺序号
     * 用于校验插单只能加到第二个在产规格之后
     *
     * @param existingList 已有排程记录列表（按顺序排序）
     * @return 第二个在产规格的最小顺序号
     */
    private int getMinSequenceFromSecondSpec(List<TqNewScheduleResult> existingList) {
        if (existingList.size() < 2) {
            return 1;
        }
        // 取第二条记录的顺序号（取所有班次顺序中最小的非空值）
        TqNewScheduleResult secondRecord = existingList.get(1);
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
    private Integer getPlanQtyByShiftIndex(TqNewScheduleResult entity, int shiftIndex) {
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
    private Integer getFinishQtyByShiftIndex(TqNewScheduleResult entity, int shiftIndex) {
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
    private String getAnalysisByShiftIndex(TqNewScheduleResult entity, int shiftIndex) {
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
    private void setPlanQtyToUpdateWrapper(LambdaUpdateWrapper<TqNewScheduleResult> updateWrapper,
                                           int shiftIndex, Integer planQty) {
        switch (shiftIndex) {
            case 1: updateWrapper.set(TqNewScheduleResult::getClass1PlanQty, planQty); break;
            case 2: updateWrapper.set(TqNewScheduleResult::getClass2PlanQty, planQty); break;
            case 3: updateWrapper.set(TqNewScheduleResult::getClass3PlanQty, planQty); break;
            case 4: updateWrapper.set(TqNewScheduleResult::getClass4PlanQty, planQty); break;
            case 5: updateWrapper.set(TqNewScheduleResult::getClass5PlanQty, planQty); break;
            case 6: updateWrapper.set(TqNewScheduleResult::getClass6PlanQty, planQty); break;
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
    private void setAnalysisToUpdateWrapper(LambdaUpdateWrapper<TqNewScheduleResult> updateWrapper,
                                            int shiftIndex, String analysis) {
        switch (shiftIndex) {
            case 1: updateWrapper.set(TqNewScheduleResult::getClass1Analysis, analysis); break;
            case 2: updateWrapper.set(TqNewScheduleResult::getClass2Analysis, analysis); break;
            case 3: updateWrapper.set(TqNewScheduleResult::getClass3Analysis, analysis); break;
            case 4: updateWrapper.set(TqNewScheduleResult::getClass4Analysis, analysis); break;
            case 5: updateWrapper.set(TqNewScheduleResult::getClass5Analysis, analysis); break;
            case 6: updateWrapper.set(TqNewScheduleResult::getClass6Analysis, analysis); break;
            default: break;
        }
    }

    /**
     * 判断指定班次是否已成为历史班次
     * 胎圈排程6班次时间窗口：
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
    private boolean isHistoryShift(TqNewScheduleResult record, int shiftIndex, Date now) {
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

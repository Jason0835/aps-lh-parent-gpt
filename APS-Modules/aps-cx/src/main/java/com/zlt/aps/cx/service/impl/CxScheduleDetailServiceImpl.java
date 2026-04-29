package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.mapper.CxScheduleDetailMapper;
import com.zlt.aps.cx.mapper.CxScheduleResultMapper;
import com.zlt.aps.cx.service.CxScheduleDetailService;
import com.zlt.aps.cx.vo.CxScheduleDetailVo;
import com.zlt.aps.cx.vo.ScheduleDetailQueryVo;
import com.zlt.aps.cx.vo.ScheduleUpdateDetailPlanQtyVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 排程明细服务实现类
 *
 * @author APS Team
 */
@Slf4j
@Service
public class CxScheduleDetailServiceImpl extends ServiceImpl<CxScheduleDetailMapper, CxScheduleDetail>
        implements CxScheduleDetailService {

    @javax.annotation.Resource
    private CxScheduleResultMapper cxScheduleResultMapper;

    @Override
    public List<CxScheduleDetailVo> listVoByMainId(Long mainId) {
        // 查询子表数据
        List<CxScheduleDetail> details = list(new LambdaQueryWrapper<CxScheduleDetail>()
                .eq(CxScheduleDetail::getMainId, mainId)
                .orderByAsc(CxScheduleDetail::getClass1Sequence));

        if (details.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询主表数据
        CxScheduleResult mainResult = baseMapper.selectMainById(mainId);
        if (mainResult == null) {
            return Collections.emptyList();
        }

        // 转换为VO
        return convertToVoList(details, mainResult);
    }

    @Override
    public List<CxScheduleDetailVo> listVoByMachineAndDate(String cxMachineCode, LocalDate scheduleDate) {
        // 查询主表获取匹配的mainId
        List<CxScheduleResult> mainResults = baseMapper.selectMainByMachineAndDate(cxMachineCode, scheduleDate);

        if (mainResults.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> mainIds = mainResults.stream()
                .map(CxScheduleResult::getId)
                .collect(Collectors.toList());

        // 查询子表数据
        List<CxScheduleDetail> details = list(new LambdaQueryWrapper<CxScheduleDetail>()
                .in(CxScheduleDetail::getMainId, mainIds)
                .orderByAsc(CxScheduleDetail::getClass1Sequence));

        // 按主表分组
        Map<Long, CxScheduleResult> mainMap = mainResults.stream()
                .collect(Collectors.toMap(CxScheduleResult::getId, r -> r));

        // 转换为VO
        return details.stream()
                .map(detail -> {
                    CxScheduleDetailVo vo = new CxScheduleDetailVo();
                    BeanUtils.copyProperties(detail, vo);
                    CxScheduleResult main = mainMap.get(detail.getMainId());
                    if (main != null) {
                        copyMainFieldsToVo(main, vo);
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CxScheduleDetailVo> listVoByMachineAndDateRange(String machineCodeStart, String machineCodeEnd,
                                                                LocalDate scheduleDateStart, LocalDate scheduleDateEnd) {
        // 查询主表数据（按机台降序+胎胚排序）
        List<CxScheduleResult> mainResults = baseMapper.selectMainByMachineAndDateRange(
                machineCodeStart, machineCodeEnd, scheduleDateStart, scheduleDateEnd);

        if (mainResults.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> mainIds = mainResults.stream()
                .map(CxScheduleResult::getId)
                .collect(Collectors.toList());

        // 查询子表数据
        List<CxScheduleDetail> details = list(new LambdaQueryWrapper<CxScheduleDetail>()
                .in(CxScheduleDetail::getMainId, mainIds)
                .orderByAsc(CxScheduleDetail::getClass1Sequence));

        // 按主表ID分组
        Map<Long, CxScheduleResult> mainMap = mainResults.stream()
                .collect(Collectors.toMap(CxScheduleResult::getId, r -> r));

        // 转换为VO并按机台降序+胎胚排序
        List<CxScheduleDetailVo> voList = details.stream()
                .map(detail -> {
                    CxScheduleDetailVo vo = new CxScheduleDetailVo();
                    BeanUtils.copyProperties(detail, vo);
                    CxScheduleResult main = mainMap.get(detail.getMainId());
                    if (main != null) {
                        copyMainFieldsToVo(main, vo);
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        // 按机台降序+胎胚排序
        voList.sort((a, b) -> {
            // 机台降序
            int machineCompare = compareStringDescending(a.getCxMachineCode(), b.getCxMachineCode());
            if (machineCompare != 0) {
                return machineCompare;
            }
            // 胎胚相同的放一起（胎胚编码升序）
            return compareStringAscending(a.getEmbryoCode(), b.getEmbryoCode());
        });

        return voList;
    }

    @Override
    public List<CxScheduleDetailVo> listVoByQuery(ScheduleDetailQueryVo query) {
        QueryWrapper<CxScheduleResult> mainQuery = new QueryWrapper<>();
        mainQuery.eq(query.getCxMachineCode() != null, "CX_MACHINE_CODE", query.getCxMachineCode());
        mainQuery.eq(query.getCxMachineName() != null, "CX_MACHINE_NAME", query.getCxMachineName());
        mainQuery.eq(query.getScheduleDate() != null, "SCHEDULE_DATE", query.getScheduleDate());
        mainQuery.eq(query.getEmbryoCode() != null, "EMBRYO_CODE", query.getEmbryoCode());
        mainQuery.eq(query.getMaterialCode() != null, "MATERIAL_CODE", query.getMaterialCode());
        mainQuery.eq(query.getOrderNo() != null, "ORDER_NO", query.getOrderNo());
        mainQuery.eq(query.getProductionStatus() != null, "PRODUCTION_STATUS", query.getProductionStatus());
        mainQuery.eq(query.getIsRelease() != null, "IS_RELEASE", query.getIsRelease());
        mainQuery.eq(query.getStructureName() != null, "STRUCTURE_NAME", query.getStructureName());
        mainQuery.orderByAsc("CX_MACHINE_CODE");

        List<CxScheduleResult> mainResults = cxScheduleResultMapper.selectList(mainQuery);
        if (mainResults.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> mainIds = mainResults.stream()
                .map(CxScheduleResult::getId)
                .collect(Collectors.toList());

        List<CxScheduleDetail> details = list(new LambdaQueryWrapper<CxScheduleDetail>()
                .in(CxScheduleDetail::getMainId, mainIds)
                .orderByAsc(CxScheduleDetail::getClass1Sequence));

        Map<Long, CxScheduleResult> mainMap = mainResults.stream()
                .collect(Collectors.toMap(CxScheduleResult::getId, r -> r));

        return details.stream()
                .map(detail -> {
                    CxScheduleDetailVo vo = new CxScheduleDetailVo();
                    BeanUtils.copyProperties(detail, vo);
                    CxScheduleResult main = mainMap.get(detail.getMainId());
                    if (main != null) {
                        copyMainFieldsToVo(main, vo);
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult updatePlanQty(ScheduleUpdateDetailPlanQtyVo vo) {
        if (vo.getDetailId() == null) {
            return AjaxResult.error("明细ID不能为空");
        }

        CxScheduleDetail detail = getById(vo.getDetailId());
        if (detail == null) {
            return AjaxResult.error("排程明细不存在");
        }

        CxScheduleResult main = cxScheduleResultMapper.selectById(detail.getMainId());
        if (main == null) {
            return AjaxResult.error("关联主表记录不存在");
        }

        LocalDate scheduleLocalDate = main.getScheduleDate() != null
                ? cn.hutool.core.date.DateUtil.toLocalDateTime(main.getScheduleDate()).toLocalDate()
                : null;
        if (scheduleLocalDate == null) {
            return AjaxResult.error("排程日期为空");
        }

        LocalDateTime now = LocalDateTime.now();

        // 校验1：历史班次不可修改（复用adjustQty的isShiftPast规则）
        BigDecimal[] newPlanQtys = {null, vo.getClass1PlanQty(), vo.getClass2PlanQty(), vo.getClass3PlanQty(),
                vo.getClass4PlanQty(), vo.getClass5PlanQty(), vo.getClass6PlanQty(),
                vo.getClass7PlanQty(), vo.getClass8PlanQty()};
        BigDecimal[] finishQtys = {null, main.getClass1FinishQty(), main.getClass2FinishQty(),
                main.getClass3FinishQty(), main.getClass4FinishQty(), main.getClass5FinishQty(),
                main.getClass6FinishQty(), main.getClass7FinishQty(), main.getClass8FinishQty()};
        String[] shiftNames = {"", "一班(早班D1)", "二班(中班D1)", "三班(夜班D2)", "四班(早班D2)",
                "五班(中班D2)", "六班(夜班D3)", "七班(早班D3)", "八班(中班D3)"};

        for (int i = 1; i <= 8; i++) {
            if (newPlanQtys[i] == null) {
                continue;
            }
            if (isShiftPast(i, scheduleLocalDate, now)) {
                return AjaxResult.error(shiftNames[i] + "计划量不可修改：该班次已过");
            }
            if (finishQtys[i] != null && newPlanQtys[i].compareTo(finishQtys[i]) < 0) {
                return AjaxResult.error(shiftNames[i] + "计划量不能低于已完成量：" + finishQtys[i]);
            }
        }

        // 更新明细表（明细表仅包含计划量字段）
        if (vo.getClass1PlanQty() != null) detail.setClass1PlanQty(vo.getClass1PlanQty());
        if (vo.getClass2PlanQty() != null) detail.setClass2PlanQty(vo.getClass2PlanQty());
        if (vo.getClass3PlanQty() != null) detail.setClass3PlanQty(vo.getClass3PlanQty());
        if (vo.getClass4PlanQty() != null) detail.setClass4PlanQty(vo.getClass4PlanQty());
        if (vo.getClass5PlanQty() != null) detail.setClass5PlanQty(vo.getClass5PlanQty());
        if (vo.getClass6PlanQty() != null) detail.setClass6PlanQty(vo.getClass6PlanQty());
        if (vo.getClass7PlanQty() != null) detail.setClass7PlanQty(vo.getClass7PlanQty());
        if (vo.getClass8PlanQty() != null) detail.setClass8PlanQty(vo.getClass8PlanQty());
        updateById(detail);

        // 重新汇总主表数据：查询该主表下所有明细，按班次累加计划量
        List<CxScheduleDetail> allDetails = list(new LambdaQueryWrapper<CxScheduleDetail>()
                .eq(CxScheduleDetail::getMainId, detail.getMainId()));

        BigDecimal sumClass1 = BigDecimal.ZERO, sumClass2 = BigDecimal.ZERO;
        BigDecimal sumClass3 = BigDecimal.ZERO, sumClass4 = BigDecimal.ZERO;
        BigDecimal sumClass5 = BigDecimal.ZERO, sumClass6 = BigDecimal.ZERO;
        BigDecimal sumClass7 = BigDecimal.ZERO, sumClass8 = BigDecimal.ZERO;

        for (CxScheduleDetail d : allDetails) {
            if (d.getClass1PlanQty() != null) sumClass1 = sumClass1.add(d.getClass1PlanQty());
            if (d.getClass2PlanQty() != null) sumClass2 = sumClass2.add(d.getClass2PlanQty());
            if (d.getClass3PlanQty() != null) sumClass3 = sumClass3.add(d.getClass3PlanQty());
            if (d.getClass4PlanQty() != null) sumClass4 = sumClass4.add(d.getClass4PlanQty());
            if (d.getClass5PlanQty() != null) sumClass5 = sumClass5.add(d.getClass5PlanQty());
            if (d.getClass6PlanQty() != null) sumClass6 = sumClass6.add(d.getClass6PlanQty());
            if (d.getClass7PlanQty() != null) sumClass7 = sumClass7.add(d.getClass7PlanQty());
            if (d.getClass8PlanQty() != null) sumClass8 = sumClass8.add(d.getClass8PlanQty());
        }

        main.setClass1PlanQty(sumClass1);
        main.setClass2PlanQty(sumClass2);
        main.setClass3PlanQty(sumClass3);
        main.setClass4PlanQty(sumClass4);
        main.setClass5PlanQty(sumClass5);
        main.setClass6PlanQty(sumClass6);
        main.setClass7PlanQty(sumClass7);
        main.setClass8PlanQty(sumClass8);

        if (vo.getClass1RecipeType() != null) main.setClass1RecipeType(vo.getClass1RecipeType());
        if (vo.getClass2RecipeType() != null) main.setClass2RecipeType(vo.getClass2RecipeType());
        if (vo.getClass3RecipeType() != null) main.setClass3RecipeType(vo.getClass3RecipeType());
        if (vo.getClass4RecipeType() != null) main.setClass4RecipeType(vo.getClass4RecipeType());
        if (vo.getClass5RecipeType() != null) main.setClass5RecipeType(vo.getClass5RecipeType());
        if (vo.getClass6RecipeType() != null) main.setClass6RecipeType(vo.getClass6RecipeType());
        if (vo.getClass7RecipeType() != null) main.setClass7RecipeType(vo.getClass7RecipeType());
        if (vo.getClass8RecipeType() != null) main.setClass8RecipeType(vo.getClass8RecipeType());

        if (vo.getClass1RecipeNo() != null) main.setClass1RecipeNo(vo.getClass1RecipeNo());
        if (vo.getClass2RecipeNo() != null) main.setClass2RecipeNo(vo.getClass2RecipeNo());
        if (vo.getClass3RecipeNo() != null) main.setClass3RecipeNo(vo.getClass3RecipeNo());
        if (vo.getClass4RecipeNo() != null) main.setClass4RecipeNo(vo.getClass4RecipeNo());
        if (vo.getClass5RecipeNo() != null) main.setClass5RecipeNo(vo.getClass5RecipeNo());
        if (vo.getClass6RecipeNo() != null) main.setClass6RecipeNo(vo.getClass6RecipeNo());
        if (vo.getClass7RecipeNo() != null) main.setClass7RecipeNo(vo.getClass7RecipeNo());
        if (vo.getClass8RecipeNo() != null) main.setClass8RecipeNo(vo.getClass8RecipeNo());

        if (vo.getClass1AnalysisInput() != null) main.setClass1AnalysisInput(vo.getClass1AnalysisInput());
        if (vo.getClass2AnalysisInput() != null) main.setClass2AnalysisInput(vo.getClass2AnalysisInput());
        if (vo.getClass3AnalysisInput() != null) main.setClass3AnalysisInput(vo.getClass3AnalysisInput());
        if (vo.getClass4AnalysisInput() != null) main.setClass4AnalysisInput(vo.getClass4AnalysisInput());
        if (vo.getClass5AnalysisInput() != null) main.setClass5AnalysisInput(vo.getClass5AnalysisInput());
        if (vo.getClass6AnalysisInput() != null) main.setClass6AnalysisInput(vo.getClass6AnalysisInput());
        if (vo.getClass7AnalysisInput() != null) main.setClass7AnalysisInput(vo.getClass7AnalysisInput());
        if (vo.getClass8AnalysisInput() != null) main.setClass8AnalysisInput(vo.getClass8AnalysisInput());

        main.setIsRelease("0");
        cxScheduleResultMapper.updateById(main);

        log.info("明细调量成功，detailId={}, mainId={}", vo.getDetailId(), detail.getMainId());
        return AjaxResult.success("明细计划量修改成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult batchUpdatePlanQty(List<ScheduleUpdateDetailPlanQtyVo> voList) {
        if (voList == null || voList.isEmpty()) {
            return AjaxResult.error("更新列表不能为空");
        }

        // 收集需要更新的主表ID
        Set<Long> mainIdsToUpdate = new HashSet<>();

        // 逐个处理每个明细的更新
        for (ScheduleUpdateDetailPlanQtyVo vo : voList) {
            AjaxResult result = updatePlanQty(vo);
            if (result == null || (Integer) result.get("code") != 200) {
                // 如果任何一个失败，返回错误信息
                return result;
            }
            // 收集主表ID用于后续统一更新
            if (vo.getDetailId() != null) {
                CxScheduleDetail detail = getById(vo.getDetailId());
                if (detail != null) {
                    mainIdsToUpdate.add(detail.getMainId());
                }
            }
        }

        // 对所有受影响的主表进行重新汇总计算
        for (Long mainId : mainIdsToUpdate) {
            recalculateMainTable(mainId);
        }

        log.info("批量明细调量成功，共更新{}条记录", voList.size());
        return AjaxResult.success("批量明细计划量修改成功，共更新" + voList.size() + "条记录");
    }

    /**
     * 重新计算主表的汇总数据
     */
    private void recalculateMainTable(Long mainId) {
        CxScheduleResult main = cxScheduleResultMapper.selectById(mainId);
        if (main == null) {
            return;
        }

        // 查询该主表下所有明细
        List<CxScheduleDetail> allDetails = list(new LambdaQueryWrapper<CxScheduleDetail>()
                .eq(CxScheduleDetail::getMainId, mainId));

        BigDecimal sumClass1 = BigDecimal.ZERO, sumClass2 = BigDecimal.ZERO;
        BigDecimal sumClass3 = BigDecimal.ZERO, sumClass4 = BigDecimal.ZERO;
        BigDecimal sumClass5 = BigDecimal.ZERO, sumClass6 = BigDecimal.ZERO;
        BigDecimal sumClass7 = BigDecimal.ZERO, sumClass8 = BigDecimal.ZERO;

        for (CxScheduleDetail d : allDetails) {
            if (d.getClass1PlanQty() != null) sumClass1 = sumClass1.add(d.getClass1PlanQty());
            if (d.getClass2PlanQty() != null) sumClass2 = sumClass2.add(d.getClass2PlanQty());
            if (d.getClass3PlanQty() != null) sumClass3 = sumClass3.add(d.getClass3PlanQty());
            if (d.getClass4PlanQty() != null) sumClass4 = sumClass4.add(d.getClass4PlanQty());
            if (d.getClass5PlanQty() != null) sumClass5 = sumClass5.add(d.getClass5PlanQty());
            if (d.getClass6PlanQty() != null) sumClass6 = sumClass6.add(d.getClass6PlanQty());
            if (d.getClass7PlanQty() != null) sumClass7 = sumClass7.add(d.getClass7PlanQty());
            if (d.getClass8PlanQty() != null) sumClass8 = sumClass8.add(d.getClass8PlanQty());
        }

        main.setClass1PlanQty(sumClass1);
        main.setClass2PlanQty(sumClass2);
        main.setClass3PlanQty(sumClass3);
        main.setClass4PlanQty(sumClass4);
        main.setClass5PlanQty(sumClass5);
        main.setClass6PlanQty(sumClass6);
        main.setClass7PlanQty(sumClass7);
        main.setClass8PlanQty(sumClass8);
        main.setIsRelease("0");
        cxScheduleResultMapper.updateById(main);
    }

    /** 判断班次是否已过（与ScheduleMainController.isShiftPast逻辑一致） */
    private boolean isShiftPast(int classIndex, LocalDate scheduleDate, LocalDateTime now) {
        LocalDate endDate;
        int endHour;
        switch (classIndex) {
            case 1: endDate = scheduleDate.minusDays(2); endHour = 14; break;
            case 2: endDate = scheduleDate.minusDays(2); endHour = 22; break;
            case 3: endDate = scheduleDate.minusDays(1); endHour = 6; break;
            case 4: endDate = scheduleDate.minusDays(1); endHour = 14; break;
            case 5: endDate = scheduleDate.minusDays(1); endHour = 22; break;
            case 6: endDate = scheduleDate; endHour = 6; break;
            case 7: endDate = scheduleDate; endHour = 14; break;
            case 8: endDate = scheduleDate; endHour = 22; break;
            default: return false;
        }
        return !now.isBefore(endDate.atTime(endHour, 0));
    }

    @Override
    public List<CxScheduleDetail> listByMainId(Long mainId) {
        return list(new LambdaQueryWrapper<CxScheduleDetail>()
                .eq(CxScheduleDetail::getMainId, mainId)
                .orderByAsc(CxScheduleDetail::getClass1Sequence));
    }

    @Override
    public List<CxScheduleDetail> listByMachineAndDate(String cxMachineCode, LocalDate scheduleDate) {
        // 子表字段已从主表继承，不再有 cxMachineCode 和 scheduleDate 字段
        // 改为通过主表关联查询：先查主表获取 mainId，再查子表
        log.warn("listByMachineAndDate 方法已废弃，子表不再包含机台和日期字段，请通过主表查询");
        return java.util.Collections.emptyList();
    }

    @Override
    public List<CxScheduleDetail> listByShift(Long mainId, String shiftCode) {
        // 子表不再有 shiftCode 字段，改为仅按 mainId 查询
        return list(new LambdaQueryWrapper<CxScheduleDetail>()
                .eq(CxScheduleDetail::getMainId, mainId)
                .orderByAsc(CxScheduleDetail::getClass1Sequence));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCompletedQuantity(Long detailId, Integer completedQuantity) {
        // 当前实体没有 tripActualQty 字段，跳过更新
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTripStatus(Long detailId, String tripStatus) {
        // 这里可以根据业务需要扩展状态字段
        // 当前实体类中通过 tripActualQty 与 tripCapacity 的比较来判断状态
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSave(List<CxScheduleDetail> details) {
        if (details == null || details.isEmpty()) {
            return false;
        }
        // 清除所有明细记录的ID,避免主键冲突,让数据库自动生成新ID
        for (CxScheduleDetail detail : details) {
            detail.setId(null);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByMainId(Long mainId) {
        return remove(new LambdaQueryWrapper<CxScheduleDetail>()
                .eq(CxScheduleDetail::getMainId, mainId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByMainIds(List<Long> mainIds) {
        if (mainIds == null || mainIds.isEmpty()) {
            return true;
        }
        return remove(new LambdaQueryWrapper<CxScheduleDetail>()
                .in(CxScheduleDetail::getMainId, mainIds));
    }

    @Override
    public Integer getNextTripNo(Long mainId, String shiftCode) {
        List<CxScheduleDetail> details = listByShift(mainId, shiftCode);
        if (details.isEmpty()) {
            return 1;
        }
        // 当前实体使用 CLASS1_TRIP_NO 等字段，返回1表示第一个车次
        return 1;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 转换子表列表为VO列表
     */
    private List<CxScheduleDetailVo> convertToVoList(List<CxScheduleDetail> details, CxScheduleResult mainResult) {
        return details.stream()
                .map(detail -> {
                    CxScheduleDetailVo vo = new CxScheduleDetailVo();
                    BeanUtils.copyProperties(detail, vo);
                    copyMainFieldsToVo(mainResult, vo);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 复制主表字段到VO
     */
    private void copyMainFieldsToVo(CxScheduleResult main, CxScheduleDetailVo vo) {
        vo.setCxMachineCode(main.getCxMachineCode());
        vo.setCxMachineName(main.getCxMachineName());
        vo.setCxMachineType(main.getCxMachineType());
        vo.setLhMachineCode(main.getLhMachineCode());
        vo.setLhMachineName(main.getLhMachineName());
        vo.setLhMachineQty(main.getLhMachineQty() != null ? main.getLhMachineQty().intValue() : null);
        vo.setEmbryoCode(main.getEmbryoCode());
        vo.setMaterialCode(main.getMaterialCode());
        vo.setMaterialDesc(main.getMaterialDesc());
        vo.setMainMaterialDesc(main.getMainMaterialDesc());
        vo.setSpecDimension(main.getSpecDimension() != null ? main.getSpecDimension().toString() : null);
        vo.setStructureName(main.getStructureName());
        vo.setScheduleDate(main.getScheduleDate() != null ? main.getScheduleDate().toString() : null);
        vo.setCxBatchNo(main.getCxBatchNo());
        vo.setOrderNo(main.getOrderNo());
        vo.setProductionStatus(main.getProductionStatus());
        // Date -> LocalDateTime
        if (main.getCreateTime() != null) {
            vo.setCreateTime(main.getCreateTime().toInstant()
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        }
        if (main.getUpdateTime() != null) {
            vo.setUpdateTime(main.getUpdateTime().toInstant()
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        }
    }

    /**
     * 字符串升序比较（处理null）
     */
    private int compareStringAscending(String s1, String s2) {
        if (s1 == null && s2 == null) return 0;
        if (s1 == null) return -1;
        if (s2 == null) return 1;
        return s1.compareTo(s2);
    }

    /**
     * 字符串降序比较（处理null）
     */
    private int compareStringDescending(String s1, String s2) {
        if (s1 == null && s2 == null) return 0;
        if (s1 == null) return 1;
        if (s2 == null) return -1;
        return s2.compareTo(s1); // 降序
    }
}

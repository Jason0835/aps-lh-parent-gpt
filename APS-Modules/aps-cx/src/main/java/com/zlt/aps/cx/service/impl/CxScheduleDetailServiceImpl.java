package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.mapper.CxScheduleDetailMapper;
import com.zlt.aps.cx.mapper.CxScheduleResultMapper;
import com.zlt.aps.cx.mapper.CxShiftConfigMapper;
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
import java.time.LocalTime;
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

    @javax.annotation.Resource
    private CxShiftConfigMapper cxShiftConfigMapper;

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
        // 排程日期查询
        mainQuery.eq(query.getScheduleDate() != null, "SCHEDULE_DATE", query.getScheduleDate());
        // 机台代码模糊查询
        mainQuery.like(query.getCxMachineCode() != null && !query.getCxMachineCode().isEmpty(), "CX_MACHINE_CODE", query.getCxMachineCode());
        // 物料代码模糊查询
        mainQuery.like(query.getMaterialCode() != null && !query.getMaterialCode().isEmpty(), "MATERIAL_CODE", query.getMaterialCode());
        // 物料描述模糊查询
        mainQuery.like(query.getMaterialDesc() != null && !query.getMaterialDesc().isEmpty(), "MATERIAL_DESC", query.getMaterialDesc());
        // 主要物料描述模糊查询
        mainQuery.like(query.getMainMaterialDesc() != null && !query.getMainMaterialDesc().isEmpty(), "MAIN_MATERIAL_DESC", query.getMainMaterialDesc());
        // 订单号精确查询
        mainQuery.eq(query.getOrderNo() != null && !query.getOrderNo().isEmpty(), "ORDER_NO", query.getOrderNo());
        // 生产状态精确查询
        mainQuery.eq(query.getProductionStatus() != null && !query.getProductionStatus().isEmpty(), "PRODUCTION_STATUS", query.getProductionStatus());
        // 发布状态精确查询
        mainQuery.eq(query.getIsRelease() != null && !query.getIsRelease().isEmpty(), "IS_RELEASE", query.getIsRelease());
        // 成型机台名称精确查询
        mainQuery.eq(query.getCxMachineName() != null && !query.getCxMachineName().isEmpty(), "CX_MACHINE_NAME", query.getCxMachineName());
        // 胎胚编码精确查询
        mainQuery.eq(query.getEmbryoCode() != null && !query.getEmbryoCode().isEmpty(), "EMBRYO_CODE", query.getEmbryoCode());
        // 结构名称精确查询
        mainQuery.eq(query.getStructureName() != null && !query.getStructureName().isEmpty(), "STRUCTURE_NAME", query.getStructureName());
        mainQuery.orderByAsc("CX_MACHINE_CODE");

        List<CxScheduleResult> mainResults = cxScheduleResultMapper.selectList(mainQuery);
        if (mainResults.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> mainIds = mainResults.stream()
                .map(CxScheduleResult::getId)
                .collect(Collectors.toList());

        List<CxScheduleDetail> details = list(new LambdaQueryWrapper<CxScheduleDetail>()
                .in(CxScheduleDetail::getMainId, mainIds));

        Map<Long, CxScheduleResult> mainMap = mainResults.stream()
                .collect(Collectors.toMap(CxScheduleResult::getId, r -> r));

        // 转换为VO并按成型机 + 胎胚 + 物料 + 车次排序
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

        // 按成型机 + 胎胚 + 物料 + 车次排序
        voList.sort((a, b) -> {
            // 1. 成型机代码升序
            int machineCompare = compareStringAscending(a.getCxMachineCode(), b.getCxMachineCode());
            if (machineCompare != 0) {
                return machineCompare;
            }
            // 2. 胎胚编码升序
            int embryoCompare = compareStringAscending(a.getEmbryoCode(), b.getEmbryoCode());
            if (embryoCompare != 0) {
                return embryoCompare;
            }
            // 3. 物料编码升序
            int materialCompare = compareStringAscending(a.getMaterialCode(), b.getMaterialCode());
            if (materialCompare != 0) {
                return materialCompare;
            }
            // 4. 车次号升序（取第一个有车次号的班次）
            String tripNoA = getFirstTripNo(a);
            String tripNoB = getFirstTripNo(b);
            return compareStringAscending(tripNoA, tripNoB);
        });

        return voList;
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

        // 加载班次配置，用于判断班次是否已过
        List<CxShiftConfig> shiftConfigs = cxShiftConfigMapper.selectList(
                new LambdaQueryWrapper<CxShiftConfig>()
                        .eq(CxShiftConfig::getIsActive, 1)
                        .orderByAsc(CxShiftConfig::getScheduleDay)
                        .orderByAsc(CxShiftConfig::getDayShiftOrder));
        Map<String, CxShiftConfig> shiftConfigMap = new HashMap<>();
        for (CxShiftConfig config : shiftConfigs) {
            if (config.getClassField() != null) {
                shiftConfigMap.put(config.getClassField(), config);
            }
        }

        // 校验1：历史班次不可修改（基于 CxShiftConfig 配置表的实际班次时间判断）
        BigDecimal[] newPlanQtys = {null, vo.getClass1PlanQty(), vo.getClass2PlanQty(), vo.getClass3PlanQty(),
                vo.getClass4PlanQty(), vo.getClass5PlanQty(), vo.getClass6PlanQty(),
                vo.getClass7PlanQty(), vo.getClass8PlanQty()};
        // 明细表原始值，用于判断用户是否实际修改
        BigDecimal[] origPlans = {null, detail.getClass1PlanQty(), detail.getClass2PlanQty(), detail.getClass3PlanQty(),
                detail.getClass4PlanQty(), detail.getClass5PlanQty(), detail.getClass6PlanQty(),
                detail.getClass7PlanQty(), detail.getClass8PlanQty()};
        BigDecimal[] finishQtys = {null, main.getClass1FinishQty(), main.getClass2FinishQty(),
                main.getClass3FinishQty(), main.getClass4FinishQty(), main.getClass5FinishQty(),
                main.getClass6FinishQty(), main.getClass7FinishQty(), main.getClass8FinishQty()};
        String[] shiftNames = {"", "一班(早班D1)", "二班(中班D1)", "三班(夜班D2)", "四班(早班D2)",
                "五班(中班D2)", "六班(夜班D3)", "七班(早班D3)", "八班(中班D3)"};

        for (int i = 1; i <= 8; i++) {
            // 前端未传值 或 值未变化（前端置灰未修改）→ 跳过校验
            // 用 compareTo 代替 equals，避免 BigDecimal 精度/scale 差异导致误判
            if (newPlanQtys[i] == null
                    || (origPlans[i] != null && newPlanQtys[i].compareTo(origPlans[i]) == 0)) {
                continue;
            }
            if (isShiftPast(i, scheduleLocalDate, now, shiftConfigMap)) {
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

    /**
     * 判断班次是否已过（基于 CxShiftConfig 配置表的实际班次时间判断）
     * @param classIndex 班次序号 1~8
     * @param scheduleDate 排程日期（T+2日）
     * @param now 当前时间
     * @param configMap 班次配置映射（key=CLASS1~CLASS8）
     */
    private boolean isShiftPast(int classIndex, LocalDate scheduleDate, LocalDateTime now,
                                Map<String, CxShiftConfig> configMap) {
        CxShiftConfig config = configMap.get("CLASS" + classIndex);
        if (config == null) {
            return false;
        }
        int dayOffset;
        if (config.getScheduleDay() == 1) {
            dayOffset = -2;
        } else if (config.getScheduleDay() == 2) {
            dayOffset = -1;
        } else {
            dayOffset = 0;
        }
        LocalTime endLocalTime = config.getShiftEndTime();
        LocalDate endDate;
        if (config.getIsCrossDay() != null && config.getIsCrossDay() == 1) {
            endDate = scheduleDate.plusDays(dayOffset);
        } else {
            endDate = scheduleDate.plusDays(dayOffset);
        }
        LocalDateTime shiftEnd = endDate.atTime(endLocalTime);
        return !now.isBefore(shiftEnd);
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
        return saveBatch(details);
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
        vo.setClass1RecipeType(main.getClass1RecipeType());
        vo.setClass2RecipeType(main.getClass2RecipeType());
        vo.setClass3RecipeType(main.getClass3RecipeType());
        vo.setClass4RecipeType(main.getClass4RecipeType());
        vo.setClass5RecipeType(main.getClass5RecipeType());
        vo.setClass6RecipeType(main.getClass6RecipeType());
        vo.setClass7RecipeType(main.getClass7RecipeType());
        vo.setClass8RecipeType(main.getClass8RecipeType());

        vo.setClass1RecipeNo(main.getClass1RecipeNo());
        vo.setClass2RecipeNo(main.getClass2RecipeNo());
        vo.setClass3RecipeNo(main.getClass3RecipeNo());
        vo.setClass4RecipeNo(main.getClass4RecipeNo());
        vo.setClass5RecipeNo(main.getClass5RecipeNo());
        vo.setClass6RecipeNo(main.getClass6RecipeNo());
        vo.setClass7RecipeNo(main.getClass7RecipeNo());
        vo.setClass8RecipeNo(main.getClass8RecipeNo());

        vo.setClass1AnalysisInput(main.getClass1AnalysisInput());
        vo.setClass2AnalysisInput(main.getClass2AnalysisInput());
        vo.setClass3AnalysisInput(main.getClass3AnalysisInput());
        vo.setClass4AnalysisInput(main.getClass4AnalysisInput());
        vo.setClass5AnalysisInput(main.getClass5AnalysisInput());
        vo.setClass6AnalysisInput(main.getClass6AnalysisInput());
        vo.setClass7AnalysisInput(main.getClass7AnalysisInput());
        vo.setClass8AnalysisInput(main.getClass8AnalysisInput());

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

    /**
     * 获取第一个有车次号的班次车次号
     * 按班次顺序查找，返回第一个非空的车次号
     */
    private String getFirstTripNo(CxScheduleDetailVo vo) {
        if (vo.getClass1TripNo() != null && !vo.getClass1TripNo().isEmpty()) {
            return vo.getClass1TripNo();
        }
        if (vo.getClass2TripNo() != null && !vo.getClass2TripNo().isEmpty()) {
            return vo.getClass2TripNo();
        }
        if (vo.getClass3TripNo() != null && !vo.getClass3TripNo().isEmpty()) {
            return vo.getClass3TripNo();
        }
        if (vo.getClass4TripNo() != null && !vo.getClass4TripNo().isEmpty()) {
            return vo.getClass4TripNo();
        }
        if (vo.getClass5TripNo() != null && !vo.getClass5TripNo().isEmpty()) {
            return vo.getClass5TripNo();
        }
        if (vo.getClass6TripNo() != null && !vo.getClass6TripNo().isEmpty()) {
            return vo.getClass6TripNo();
        }
        if (vo.getClass7TripNo() != null && !vo.getClass7TripNo().isEmpty()) {
            return vo.getClass7TripNo();
        }
        if (vo.getClass8TripNo() != null && !vo.getClass8TripNo().isEmpty()) {
            return vo.getClass8TripNo();
        }
        return null;
    }
}

package com.zlt.aps.monthplan.adjust.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.common.core.constant.BusiConstant;
import com.zlt.aps.monthplan.api.annotation.WeekAdjustType;
import com.zlt.aps.monthplan.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.monthplan.api.domain.vo.MpAdjustStructureInVo;
import com.zlt.aps.monthplan.api.enums.WeekAdjustTypeEnum;
import com.zlt.common.utils.PubUtil;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 结构内调整策略
 * @author wengpc
 */
@Slf4j
@Service
@WeekAdjustType(adjustType = WeekAdjustTypeEnum.STRUCTURE_IN)
public class MpAdjustStructureInStrategy extends AbstractBaseWeekAdjustService {

    @Override
    public void doGenerateAdjust(MpRollAdjustContextDTO contextDTO) throws BusinessException {
        // 1、构建结构内调整记录
        List<MpAdjustStructureInVo> adjustStructureInList = buildAdjustStructureInList(contextDTO);
        contextDTO.setMpAdjustStructureInList(adjustStructureInList);
        if (PubUtil.isEmpty(adjustStructureInList)) {
            return;
        }
        // 2、设置净需求
        setCurrentNetQty(contextDTO);
        // 3、设置计划剩余排产量、计划已排产量
        setMonthUnScheduledQty(contextDTO);
        // 4、筛选：净需求 - 计划剩余排产量 > 0的数据
        filterAdjustList(contextDTO.getMpAdjustStructureInList());
        // 筛选后数据为空，抛出异常
        Assert.isFalse(PubUtil.isEmpty(contextDTO.getMpAdjustStructureInList()), () -> {
            String msg = StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.notFindAdjustList"), contextDTO.getYearMonth());
            return new BusinessException(msg);
        });
        // 5、设置其他字段
        setOtherField(contextDTO);
    }

    @Override
    public void doAutoAdjust(MpRollAdjustContextDTO contextDTO) {

    }

    @Override
    public void doConfirmAdjust(MpRollAdjustContextDTO contextDTO) {

    }

    /**
     * 构建结构内调整记录
     * @param contextDTO
     * @return
     */
    private List<MpAdjustStructureInVo> buildAdjustStructureInList(MpRollAdjustContextDTO contextDTO) {
        // 销售订单池列表
        List<SalesOrderPool> salesOrderPoolList = contextDTO.getSalesOrderPoolList();
        // 月度生产计划列表
        List<FactoryMonthPlanFinalAdjustVo> monthPlanProdList = contextDTO.getFactoryMonthPlanProdFinalList();
        // 结果集初始化
        List<MpAdjustStructureInVo> resultList = new ArrayList<>();
        // 任一列表为空则直接返回空结果
        if (PubUtil.isEmpty(salesOrderPoolList) || PubUtil.isEmpty(monthPlanProdList)) {
            return resultList;
        }
        // 获取版本号
        String version = generateVersion(BusiConstant.WeekRollAdjust.VERSION_PREFIX);
        // 按物料编码分组，合并同分组下的成型机编码（逗号分隔）
        List<FactoryMonthPlanFinalAdjustVo> mergeMonthPlanProdList = mergeMonthPlanProdList(monthPlanProdList);
        // 生产计划列表按照物料编码进行分组
        Map<String, List<FactoryMonthPlanFinalAdjustVo>> monthPlanMap = mergeMonthPlanProdList.stream()
                .collect(Collectors.groupingBy(FactoryMonthPlanFinalAdjustVo::getMaterialCode));
        // 遍历销售订单列表，匹配生产计划
        for (SalesOrderPool salesOrder : salesOrderPoolList) {
            String materialCode = salesOrder.getOriMaterialCode();
            // 物料编码为空则跳过
            if (StringUtils.isEmpty(materialCode)) {
                continue;
            }
            // 根据物料编码获取对应的生产计划列表
            List<FactoryMonthPlanFinalAdjustVo> matchMonthPlanProdList = monthPlanMap.get(materialCode);
            if (PubUtil.isEmpty(matchMonthPlanProdList)) {
                // 匹配不到时跳过
                continue;
            }
            // 组装结果集
            for (FactoryMonthPlanFinalAdjustVo monthPlan : matchMonthPlanProdList) {
                MpAdjustStructureInVo adjustStructureIn = new MpAdjustStructureInVo();
                adjustStructureIn.setMaterialCode(materialCode);
                adjustStructureIn.setScheduledMachines(monthPlan.getCxMachineCode());
                // todo 暂时写死，后续获取
                adjustStructureIn.setHasSpecialMaterial("0");
                adjustStructureIn.setYear(contextDTO.getMpYear());
                adjustStructureIn.setMonth(contextDTO.getMpMonth());
                adjustStructureIn.setVersion(version);
                adjustStructureIn.setStructureName(monthPlan.getStructureName());
                adjustStructureIn.setMaterialDesc(monthPlan.getMaterialDesc());
                // todo 暂时写死，后续获取
                adjustStructureIn.setPreviousNetQty(0);
                // 添加到结果集
                resultList.add(adjustStructureIn);
            }
        }
        return resultList;
    }


    /**
     * 按物料编码分组，合并同分组下的成型机编码（逗号分隔）
     * @param originalList
     * @return 合并后结果集
     */
    private List<FactoryMonthPlanFinalAdjustVo> mergeMonthPlanProdList(List<FactoryMonthPlanFinalAdjustVo> originalList) {
        // 结果集初始化
        List<FactoryMonthPlanFinalAdjustVo> mergedList = new ArrayList<>();
        // 原始列表为空直接返回空结果
        if (PubUtil.isEmpty(originalList)) {
            return mergedList;
        }
        // 按物料编码分组
        Map<String, List<FactoryMonthPlanFinalAdjustVo>> monthPlanGroupMap = originalList.stream()
                .filter(vo -> StringUtils.isNotBlank(vo.getMaterialCode()))
                .collect(Collectors.groupingBy(FactoryMonthPlanFinalAdjustVo::getMaterialCode));
        // 遍历分组，合并成型机编码
        monthPlanGroupMap.forEach((materialCode, list) -> {
            // 收集并合并成型机编码（多个逗号分隔）
            String mergedCxMachineCode = list.stream()
                    .map(FactoryMonthPlanFinalAdjustVo::getCxMachineCode)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .collect(Collectors.joining(","));
            // 构建合并后的月度生产计划
            FactoryMonthPlanFinalAdjustVo mergedVo = new FactoryMonthPlanFinalAdjustVo();
            FactoryMonthPlanFinalAdjustVo firstVo = list.get(0);
            BeanUtil.copyProperties(firstVo,mergedVo,false);
            mergedVo.setMaterialCode(materialCode);
            mergedVo.setCxMachineCode(mergedCxMachineCode);
            // 添加到结果集
            mergedList.add(mergedVo);
        });
        return mergedList;
    }

    /**
     * 设置净需求
     * 净需求 = 销售订单池.当前订单量 - 实时库存 - 月底计划余量
     * @param contextDTO
     */
    private void setCurrentNetQty(MpRollAdjustContextDTO contextDTO) {
        // 月底计划余量列表
        List<MdmMonthSurplus> surplusList = contextDTO.getMdmMonthSurplusesList();
        // 实时成品库存列表
        List<MdmProductStock> stockList = contextDTO.getMdmProductStockList();
        // 结构内调整记录
        List<MpAdjustStructureInVo> adjustList = contextDTO.getMpAdjustStructureInList();
        // 将列表转为Map
        Map<String, MdmMonthSurplus> surplusMap = convertToSurplusMap(surplusList);
        Map<String, MdmProductStock> stockMap = convertToStockMap(stockList);

        // 遍历计算
        for (MpAdjustStructureInVo adjust : adjustList) {
            if (StringUtils.isEmpty(adjust.getMaterialCode())) {
                continue;
            }
            String materialCode = adjust.getMaterialCode();
            Integer ordQty = Convert.toInt(adjust.getOrdQty(),0);
            Integer planSurplusQty = MapUtil.getInt(surplusMap,materialCode,0);
            Integer stockQty = MapUtil.getInt(stockMap,materialCode,0);
            // 计算赋值 净需求 = 销售订单池.当前订单量 - 实时库存 - 月底计划余量
            Integer currentNetQty = ordQty - planSurplusQty - stockQty;
            adjust.setCurrentNetQty(currentNetQty);
        }

    }

    /**
     * 将MdmMonthSurplus转Map
     */
    private Map<String, MdmMonthSurplus> convertToSurplusMap(List<MdmMonthSurplus> surplusList) {
        if (PubUtil.isEmpty(surplusList)) {
            return Collections.emptyMap();
        }
        return surplusList.stream()
                .filter(surplus -> StringUtils.isNotEmpty(surplus.getMaterialCode()))
                .collect(Collectors.toMap(
                        MdmMonthSurplus::getMaterialCode,
                        surplus -> surplus,
                        (existingVal, newVal) -> newVal
                ));
    }


    /**
     * 将MdmProductStock转Map
     */
    private Map<String, MdmProductStock> convertToStockMap(List<MdmProductStock> stockList) {
        if (stockList == null || stockList.isEmpty()) {
            return Collections.emptyMap();
        }
        return stockList.stream()
                .filter(stock -> stock != null && stock.getMaterialCode() != null)
                .collect(Collectors.toMap(
                        MdmProductStock::getMaterialCode,
                        stock -> stock,
                        (existingVal, newVal) -> newVal
                ));
    }


    /**
     * 设置计划剩余排产量
     * 计划剩余排产量 =【 1日 至 （调整日+锁定3天）】.计划量 - 已生产量，出现负数，默认等于0
     * @param contextDTO
     */
    private void setMonthUnScheduledQty(MpRollAdjustContextDTO contextDTO) {
        // 月度硫化监控列表
        List<MpMonthPlanMonitor> monitorList = contextDTO.getMpMonthPlanMonitorList();
        // 月度生产计划列表
        List<FactoryMonthPlanFinalAdjustVo> planList = contextDTO.getFactoryMonthPlanProdFinalList();
        // 结构内调整记录
        List<MpAdjustStructureInVo> adjustList = contextDTO.getMpAdjustStructureInList();
        // 转分组Map
        Map<String, List<FactoryMonthPlanFinalAdjustVo>> planGroupMap = convertToPlanGroupMap(planList);
        Map<String, List<MpMonthPlanMonitor>> monitorGroupMap = convertToMonitorGroupMap(monitorList);
        // 获取当前日期 + 锁定3天的日期，计算目标天数（如：5号+3天=7号，包含当天）
        LocalDate currentDate = LocalDate.now();
        LocalDate targetDate = currentDate.plus(BusiConstant.WeekRollAdjust.LOCK_DAYS, ChronoUnit.DAYS);
        int targetDay = targetDate.getDayOfMonth();
        // 目标天数不超过当月最大天数
        int maxDayOfMonth = currentDate.lengthOfMonth();
        targetDay = Math.min(targetDay, maxDayOfMonth);
        // 遍历目标列表，计算赋值
        for (MpAdjustStructureInVo adjust : adjustList) {
            if (StringUtils.isEmpty(adjust.getMaterialCode())) {
                adjust.setMonthUnScheduledQty(0);
                continue;
            }
            String materialCode = adjust.getMaterialCode();
            // 计算：day1~targetDay的累计值
            Integer totalScheduledQty = calculateQty(planGroupMap, materialCode, targetDay);
            // 获取已生产量（空值按0处理）
            Integer productionQty = MapUtil.getInt(monitorGroupMap,materialCode,0);
            // 计划已排产量
            adjust.setMonthScheduledQty(totalScheduledQty);
            // 计划剩余排产量 = 累计已排产量 - 已生产量
            Integer monthUnScheduledQty = totalScheduledQty - productionQty;
            // 计划剩余排产量为负数时，默认为0
            if (monthUnScheduledQty < 0) {
                monthUnScheduledQty = 0;
            }
            adjust.setMonthUnScheduledQty(monthUnScheduledQty);
        }

    }

    /**
     * 计算day1~targetDay的累计已排产量
     */
    private Integer calculateQty(Map<String, List<FactoryMonthPlanFinalAdjustVo>> planGroupMap, String materialCode, int targetDay) {
        // 从分组Map中获取当前物料的计划列表（空则返回0）
        List<FactoryMonthPlanFinalAdjustVo> planList = Optional.ofNullable(planGroupMap.get(materialCode))
                .filter(list -> PubUtil.isNotEmpty(list))
                .orElse(Collections.emptyList());
        if (PubUtil.isEmpty(planList)) {
            return 0;
        }
        // 取第一个计划对象
        FactoryMonthPlanFinalAdjustVo plan = planList.get(0);
        int total = 0;
        // 遍历day1~targetDay字段，累加值
        for (int day = 1; day <= targetDay; day++) {
            try {
                // 拼接字段名
                String fieldName = "day" + day;
                // 获取字段值，空值按0处理
                Integer dayValue = (Integer) plan.getFieldValueByFieldName(fieldName);
                total += Convert.toInt(dayValue);
            } catch (Exception e) {
                // 异常时跳过
                continue;
            }
        }
        return total;
    }


    /**
     * 转FactoryMonthPlanFinalAdjustVo分组Map
     */
    private Map<String, List<FactoryMonthPlanFinalAdjustVo>> convertToPlanGroupMap(List<FactoryMonthPlanFinalAdjustVo> planList) {
        if (PubUtil.isEmpty(planList)) {
            return Collections.emptyMap();
        }
        return planList.stream()
                .filter(plan -> plan != null && plan.getMaterialCode() != null)
                .collect(Collectors.groupingBy(FactoryMonthPlanFinalAdjustVo::getMaterialCode));
    }

    /**
     * 转MpMonthPlanMonitor分组Map
     */
    private Map<String, List<MpMonthPlanMonitor>> convertToMonitorGroupMap(List<MpMonthPlanMonitor> monitorList) {
        if (PubUtil.isEmpty(monitorList)) {
            return Collections.emptyMap();
        }
        return monitorList.stream()
                .filter(monitor -> monitor != null && monitor.getMaterialCode() != null)
                .collect(Collectors.groupingBy(MpMonthPlanMonitor::getMaterialCode));
    }


    /**
     * 设置其他值
     * @param contextDTO
     */
    private void setOtherField(MpRollAdjustContextDTO contextDTO) {
        // 结构内调整记录
        List<MpAdjustStructureInVo> adjustList = contextDTO.getMpAdjustStructureInList();
        // 循环设置
        adjustList.stream().forEach(vo -> {
            // 计算: 调整量 = 净需求 - 计划剩余排产量
            Integer pendingQty = vo.getCurrentNetQty() - vo.getMonthUnScheduledQty();
            vo.setPendingQty(Convert.toInt(pendingQty,0));
        });
    }


    /**
     * 筛选：净需求 - 计划剩余排产量 > 0的数据
     * @param adjustList
     */
    private void filterAdjustList(List<MpAdjustStructureInVo> adjustList) {
        if (PubUtil.isEmpty(adjustList)) {
            return;
        }
        adjustList.removeIf(adjust -> {
            Integer currentNetQty = Convert.toInt(adjust.getCurrentNetQty(),0);
            Integer monthUnScheduledQty = Convert.toInt(adjust.getMonthUnScheduledQty(),0);
            return (currentNetQty - monthUnScheduledQty) <= 0;
        });
    }
}

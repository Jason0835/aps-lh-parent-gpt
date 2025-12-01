package com.zlt.aps.maindata.utils;

import com.tlt.aps.constant.FactoryConstant;
import com.zlt.aps.maindata.domain.vo.DaySizeCapacityVo;
import com.zlt.aps.monthplan.api.domain.entity.SizeCapacityConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 寸口产能配置工具类
 *
 * @author ZLT
 * @date 20260605
 */
public class SizeCapacityUtils {

    /**
     * 根据配置，构建树形结构
     *
     * @param sizeCapacityConfigurationList
     * @return
     */
    public static List<DaySizeCapacityVo> buildTree(List<SizeCapacityConfiguration> sizeCapacityConfigurationList) {
        if (CollectionUtils.isEmpty(sizeCapacityConfigurationList)) {
            return Collections.emptyList();
        }
        List<SizeCapacityConfiguration> hasNextConfigurationList = sizeCapacityConfigurationList.stream().filter(sizeCapacity -> null != sizeCapacity.getNextProSize()).collect(Collectors.toList());
        Map<String, SizeCapacityConfiguration> nextSizeMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(hasNextConfigurationList)) {
            nextSizeMap = hasNextConfigurationList.stream().collect(Collectors.toMap(SizeCapacityConfiguration::getNextGroupKey, Function.identity()));
        }
        Map<String, SizeCapacityConfiguration> finalNextSizeMap = nextSizeMap;
        //构建上下级信息列表
        List<DaySizeCapacityVo> treeNodeList = new ArrayList<>();
        sizeCapacityConfigurationList.stream().forEach(sizeCapacityConfiguration -> {
            Integer remainingDays = sizeCapacityConfiguration.getRemainingDays();
            if (null == remainingDays) {
                remainingDays = BigDecimal.ZERO.intValue();
            }
            DaySizeCapacityVo daySizeCapacity = buildDaySizeCapacity(sizeCapacityConfiguration);
            String nextGroupKey = sizeCapacityConfiguration.getOneselfKey();
            //20250908 ZLT 去除二级限制(不判断nextProSize)
            if (finalNextSizeMap.containsKey(nextGroupKey) && remainingDays > BigDecimal.ZERO.intValue()) {
                SizeCapacityConfiguration parent = finalNextSizeMap.get(nextGroupKey);
                daySizeCapacity.setParentSizeCapacityTreeKey(parent.getOneselfKey());
            }
            treeNodeList.add(daySizeCapacity);
        });
        //获取非顶级节点
        Map<String, List<DaySizeCapacityVo>> nodeMap = treeNodeList.stream().filter(node -> StringUtils.isNotBlank(node.getParentSizeCapacityTreeKey())).collect(Collectors.groupingBy(DaySizeCapacityVo::getParentSizeCapacityTreeKey));
        //构建树形结构
        treeNodeList.stream().forEach(node -> {
            String sizeCapacityTreeKey = node.getSizeCapacityTreeKey();
            SizeCapacityConfiguration data = node.getData();
            if (null == data.getNextProSize()) {
                return;
            }
            List<DaySizeCapacityVo> children = nodeMap.get(sizeCapacityTreeKey);
            if (CollectionUtils.isEmpty(children)) {
                return;
            }
            node.setNextSize(children.get(0));
        });
        List<DaySizeCapacityVo> tree = treeNodeList.stream().filter(node -> StringUtils.isBlank(node.getParentSizeCapacityTreeKey())).collect(Collectors.toList());
        return tree;
    }

    /**
     * 根据寸口产能分配配置，构建产能控制信息
     * 细化到天的寸口+成型法的日产能量
     *
     * @param daySizeCapacity    当前寸口+成型法的配置信息
     * @param daySizeCapacityMap 寸口+成型法细化到天的产能控制集合，需要将配置转换后加入到集合中
     * @param monthDays          月份最大天数-日产能计算使用
     */
    public static void buildCapacityControlInfo(Set<Integer> stopDays, DaySizeCapacityVo daySizeCapacity, Integer startDay, Map<Integer, Map<String, Long>> daySizeCapacityMap, Integer monthDays, Map<Integer, Map<String, Integer>> dayMaxMouldQtyMap) {
        SizeCapacityConfiguration sizeCapacityConfiguration = daySizeCapacity.getData();
        //日产能细化控制
        buildCapacityControlInfo(stopDays, sizeCapacityConfiguration, startDay, daySizeCapacityMap, monthDays, dayMaxMouldQtyMap);
        DaySizeCapacityVo nextSizeCapacity = daySizeCapacity.getNextSize();
        //下一个寸口
        if (null != nextSizeCapacity) {
            Integer newStartDay = startDay + sizeCapacityConfiguration.getRemainingDays();
            buildCapacityControlInfo(stopDays, nextSizeCapacity, newStartDay, daySizeCapacityMap, monthDays, dayMaxMouldQtyMap);
        }
    }

    /**
     * 机台数为整数时，表示整月日控产能一样
     *
     * @param stopDays                  停工日
     * @param sizeCapacityKey           寸口|*|工装类型|*|成型法|*|胎体布层级
     * @param monthMaxDays              月份最大天数
     * @param daySizeCapacityMap        日产能控制集合
     * @param sizeCapacityConfiguration 产能配置信息
     * @param dayMaxMouldQtyMap         日模具数控制集合
     */
    public static void buildWholeMonth(Set<Integer> stopDays, String sizeCapacityKey, Integer monthMaxDays, Map<Integer, Map<String, Long>> daySizeCapacityMap, SizeCapacityConfiguration sizeCapacityConfiguration, Map<Integer, Map<String, Integer>> dayMaxMouldQtyMap) {
        Integer wholeMonthNumber = sizeCapacityConfiguration.getWholeMachineNumber();
        Integer dayCapacity = sizeCapacityConfiguration.getDayCapacity();
        Integer dayMouldQty = sizeCapacityConfiguration.getMaxMouldQty();
        if (null == dayMouldQty) {
            dayMouldQty = BigDecimal.ZERO.intValue();
        }
        for (int day = FactoryConstant.MONTH_START_DAY; day <= monthMaxDays; day++) {
            //停工日排除
            if (stopDays.contains(day)) {
                continue;
            }
            //天产能控制集合
            Map<String, Long> daySizeCapacity = daySizeCapacityMap.get(day);

            if (null == daySizeCapacity) {
                daySizeCapacity = new HashMap<>();
            }

            //天总产能 = 天产能 * 机器数
            Long dayCapacityQty = BigDecimal.valueOf(wholeMonthNumber).multiply(BigDecimal.valueOf(dayCapacity)).setScale(BigDecimal.ZERO.intValue(), RoundingMode.UP).longValue();
            //20250714 ZLT 因配置到成型产能类型和天产能，故而会有不同产能值
            Long assignedDayCapacityQty = daySizeCapacity.get(sizeCapacityKey);
            if (null == assignedDayCapacityQty) {
                assignedDayCapacityQty = BigDecimal.ZERO.longValue();
            }
            daySizeCapacity.put(sizeCapacityKey, assignedDayCapacityQty + dayCapacityQty);
            daySizeCapacityMap.put(day, daySizeCapacity);
            //20251010 ZLT 产能对等--转化成模具数控制
            Map<String, Integer> dayMaxMouldQty = dayMaxMouldQtyMap.get(day);
            if (null == dayMaxMouldQty) {
                dayMaxMouldQty = new HashMap<>();
            }
            Integer sumDayMouldQty = wholeMonthNumber * dayMouldQty;
            Integer assignedDayMaxMouldQty = dayMaxMouldQty.get(sizeCapacityKey);
            if (null == assignedDayMaxMouldQty) {
                assignedDayMaxMouldQty = BigDecimal.ZERO.intValue();
            }
            dayMaxMouldQty.put(sizeCapacityKey, assignedDayMaxMouldQty + sumDayMouldQty);
            dayMaxMouldQtyMap.put(day, dayMaxMouldQty);
        }
    }

    /**
     * 部分整月排产，某台部分天有排产量
     *
     * @param stopDays                  停工日
     * @param sizeCapacityKey           寸口|*|工装类型|*|成型法|*|胎体布层级
     * @param monthMaxDays              月份最大天数
     * @param daySizeCapacityMap        日产能控制集合
     * @param sizeCapacityConfiguration 产能控制信息
     * @param startDay                  起始天数部分
     * @param dayMaxMouldQtyMap         日模具数控制集合
     */
    public static void buildPartWholeMonthPartDaysMonth(Set<Integer> stopDays, String sizeCapacityKey, Integer monthMaxDays, Map<Integer, Map<String, Long>> daySizeCapacityMap, SizeCapacityConfiguration sizeCapacityConfiguration, Integer startDay, Map<Integer, Map<String, Integer>> dayMaxMouldQtyMap) {
        //整台数
        Integer wholeMonthNumber = sizeCapacityConfiguration.getWholeMachineNumber();
        Integer dayCapacity = sizeCapacityConfiguration.getDayCapacity();
        //分配天数
        Integer allocationDays = sizeCapacityConfiguration.getRemainingDays();
        if (null == allocationDays) {
            allocationDays = BigDecimal.ZERO.intValue();
        }
        //计算分配天数在整个成型产能中的起始、结束天数
        Integer realStartDay = getRealStartDay(startDay, stopDays, monthMaxDays);
        Integer endDay = calculateDays(realStartDay, stopDays, monthMaxDays, allocationDays);
        Integer dayMouldQty = sizeCapacityConfiguration.getMaxMouldQty();
        if (null == dayMouldQty) {
            dayMouldQty = BigDecimal.ZERO.intValue();
        }
        for (int day = FactoryConstant.MONTH_START_DAY; day <= monthMaxDays; day++) {
            //停工日排除
            if (stopDays.contains(day)) {
                continue;
            }
            Map<String, Long> daySizeCapacity = daySizeCapacityMap.get(day);
            if (null == daySizeCapacity) {
                daySizeCapacity = new HashMap<>();
            }
            BigDecimal dayCapacityQty = BigDecimal.valueOf(wholeMonthNumber).multiply(BigDecimal.valueOf(dayCapacity));
            //部分天增加产能
            if (day >= realStartDay && day <= endDay) {
                dayCapacityQty = dayCapacityQty.add(BigDecimal.valueOf(dayCapacity));
            }
            dayCapacityQty = dayCapacityQty.setScale(BigDecimal.ZERO.intValue(), RoundingMode.UP);
            //20250714 ZLT 因配置到成型产能类型和天产能，故而会有不同产能值
            Long assignedDayCapacityQty = daySizeCapacity.get(sizeCapacityKey);
            if (null == assignedDayCapacityQty) {
                assignedDayCapacityQty = BigDecimal.ZERO.longValue();
            }
            daySizeCapacity.put(sizeCapacityKey, assignedDayCapacityQty + dayCapacityQty.longValue());
            daySizeCapacityMap.put(day, daySizeCapacity);

            //20251010 ZLT 产能对等--转化成模具数控制
            Map<String, Integer> dayMaxMouldQty = dayMaxMouldQtyMap.get(day);
            if (null == dayMaxMouldQty) {
                dayMaxMouldQty = new HashMap<>();
            }
            Integer sumDayMouldQty = wholeMonthNumber * dayMouldQty;
            //部分天增加产能--增加模具数
            if (day >= realStartDay && day <= endDay) {
                sumDayMouldQty = sumDayMouldQty + dayMouldQty;
            }
            Integer assignedDayMaxMouldQty = dayMaxMouldQty.get(sizeCapacityKey);
            if (null == assignedDayMaxMouldQty) {
                assignedDayMaxMouldQty = BigDecimal.ZERO.intValue();
            }
            dayMaxMouldQty.put(sizeCapacityKey, assignedDayMaxMouldQty + sumDayMouldQty);
            dayMaxMouldQtyMap.put(day, dayMaxMouldQty);
        }
    }

    /**
     * 只有部分天有排产量
     *
     * @param stopDays                  停工日
     * @param sizeCapacityKey           寸口|*|工装类型|*|成型法|*|胎体布层级
     * @param monthMaxDays              月份最大天数
     * @param daySizeCapacityMap        日产能控制集合
     * @param sizeCapacityConfiguration 日产能值
     * @param startDay                  前面已经分配的天数
     * @param dayMaxMouldQtyMap         日模具数控制集合
     */
    public static void buildPartDaysMonth(Set<Integer> stopDays, String sizeCapacityKey, Integer monthMaxDays, Map<Integer, Map<String, Long>> daySizeCapacityMap, SizeCapacityConfiguration sizeCapacityConfiguration, Integer startDay, Map<Integer, Map<String, Integer>> dayMaxMouldQtyMap) {
        //分配天数
        Integer allocationDays = sizeCapacityConfiguration.getRemainingDays();
        if (null == allocationDays) {
            allocationDays = BigDecimal.ZERO.intValue();
        }
        //天产能
        Integer dayCapacity = sizeCapacityConfiguration.getDayCapacity();
        //计算分配天数在整个成型产能中的起始、结束天数
        Integer realStartDay = getRealStartDay(startDay, stopDays, monthMaxDays);
        Integer endDay = calculateDays(realStartDay, stopDays, monthMaxDays, allocationDays);
        Integer dayMouldQty = sizeCapacityConfiguration.getMaxMouldQty();
        if (null == dayMouldQty) {
            dayMouldQty = BigDecimal.ZERO.intValue();
        }
        //下半月起始天数
        for (int day = realStartDay; day <= endDay; day++) {
            //停工日排除
            if (stopDays.contains(day)) {
                continue;
            }
            Map<String, Long> daySizeCapacity = daySizeCapacityMap.get(day);
            if (null == daySizeCapacity) {
                daySizeCapacity = new HashMap<>();
            }
            //20250714 ZLT 因配置到成型产能类型和天产能，故而会有不同产能值
            Long assignedDayCapacityQty = daySizeCapacity.get(sizeCapacityKey);
            if (null == assignedDayCapacityQty) {
                assignedDayCapacityQty = BigDecimal.ZERO.longValue();
            }
            daySizeCapacity.put(sizeCapacityKey, assignedDayCapacityQty + Long.valueOf(dayCapacity));
            daySizeCapacityMap.put(day, daySizeCapacity);

            //20251010 ZLT 产能对等--转化成模具数控制
            Map<String, Integer> dayMaxMouldQty = dayMaxMouldQtyMap.get(day);
            if (null == dayMaxMouldQty) {
                dayMaxMouldQty = new HashMap<>();
            }
            Integer assignedDayMaxMouldQty = dayMaxMouldQty.get(sizeCapacityKey);
            if (null == assignedDayMaxMouldQty) {
                assignedDayMaxMouldQty = BigDecimal.ZERO.intValue();
            }
            dayMaxMouldQty.put(sizeCapacityKey, assignedDayMaxMouldQty + dayMouldQty);
            dayMaxMouldQtyMap.put(day, dayMaxMouldQty);
        }
    }

    /**
     * 根据寸口产能分配配置，构建产能控制信息
     * 包含寸口+成型法的月产能总量
     * 及细化到天的寸口+成型法的日产能量
     *
     * @param stopDays                  停工日
     * @param sizeCapacityConfiguration 当前寸口+成型法的配置信息
     * @param startDay                  起始小数部分，用以计算开始天数
     * @param daySizeCapacityMap        寸口+成型法细化到天的产能控制集合，需要将配置转换后加入到集合中
     * @param monthDays                 月份最大天数-日产能计算使用
     * @param dayMaxMouldQtyMap         寸口+成型法细化到天的产能控制集合，需要将配置转换后加入到集合中
     */
    private static void buildCapacityControlInfo(Set<Integer> stopDays, SizeCapacityConfiguration sizeCapacityConfiguration, Integer startDay, Map<Integer, Map<String, Long>> daySizeCapacityMap, Integer monthDays, Map<Integer, Map<String, Integer>> dayMaxMouldQtyMap) {
        //寸口|*|工装类型|*|成型法|*|胎体布层级
        String sizeCapacityKey = sizeCapacityConfiguration.getGroupKey();
        Integer remainingDays = sizeCapacityConfiguration.getRemainingDays();
        if (null == remainingDays) {
            remainingDays = BigDecimal.ZERO.intValue();
        }
        Integer wholeMonthNumber = sizeCapacityConfiguration.getWholeMachineNumber();
        if (remainingDays == BigDecimal.ZERO.intValue() && wholeMonthNumber != BigDecimal.ZERO.intValue()) {
            //能整月排产
            buildWholeMonth(stopDays, sizeCapacityKey, monthDays, daySizeCapacityMap, sizeCapacityConfiguration, dayMaxMouldQtyMap);
            return;
        }
        //部分整月，部分中途换寸口
        if (wholeMonthNumber > BigDecimal.ZERO.intValue() && remainingDays > BigDecimal.ZERO.intValue()) {
            buildPartWholeMonthPartDaysMonth(stopDays, sizeCapacityKey, monthDays, daySizeCapacityMap, sizeCapacityConfiguration, startDay, dayMaxMouldQtyMap);
            return;
        }
        //全部部分天排产
        buildPartDaysMonth(stopDays, sizeCapacityKey, monthDays, daySizeCapacityMap, sizeCapacityConfiguration, startDay, dayMaxMouldQtyMap);
    }

    /**
     * 构建树节点信息
     *
     * @param sizeCapacityConfiguration
     * @return
     */
    private static DaySizeCapacityVo buildDaySizeCapacity(SizeCapacityConfiguration sizeCapacityConfiguration) {
        DaySizeCapacityVo daySizeCapacity = new DaySizeCapacityVo();
        if (StringUtils.isNotBlank(sizeCapacityConfiguration.getOneselfKey())) {
            daySizeCapacity.setSizeCapacityTreeKey(sizeCapacityConfiguration.getOneselfKey());
        } else {
            daySizeCapacity.setSizeCapacityTreeKey(sizeCapacityConfiguration.getTreeGroupKey());
        }
        daySizeCapacity.setProSize(sizeCapacityConfiguration.getProSize());
        daySizeCapacity.setMouldMethod(sizeCapacityConfiguration.getMouldMethod());
        daySizeCapacity.setTireFabricNumber(sizeCapacityConfiguration.getCarcassClothType());
        daySizeCapacity.setData(sizeCapacityConfiguration);
        daySizeCapacity.setIntPart(sizeCapacityConfiguration.getWholeMachineNumber());
        daySizeCapacity.setDecimalDays(sizeCapacityConfiguration.getRemainingDays());
        return daySizeCapacity;
    }

    /**
     * 获取真正的起始天
     * 需要考虑停工日期
     *
     * @param startDay     理论开始天数
     * @param stopDays     停工日期集合
     * @param monthMaxDays 月份最大天数
     * @return
     */
    private static Integer getRealStartDay(Integer startDay, Set<Integer> stopDays, Integer monthMaxDays) {
        Integer realStartDay;
        if (null == startDay || startDay == BigDecimal.ZERO.intValue()) {
            realStartDay = FactoryConstant.MONTH_START_DAY;
        } else {
            realStartDay = calculateDays(FactoryConstant.MONTH_START_DAY, stopDays, monthMaxDays, startDay);
            realStartDay = realStartDay + BigDecimal.ONE.intValue();
        }
        return realStartDay;
    }

    /**
     * 根据起始天，计算其分配allocationDays之后所处天数，需要考虑中间的停工日
     *
     * @param startDay       起始天数
     * @param stopDays       停工日信息
     * @param monthMaxDays   月份最大天数
     * @param allocationDays 需排产天数
     * @return
     */
    private static Integer calculateDays(Integer startDay, Set<Integer> stopDays, Integer monthMaxDays, Integer allocationDays) {
        Integer productionDay = BigDecimal.ZERO.intValue();
        for (int day = startDay; day <= monthMaxDays; day++) {
            //停工日排除
            if (stopDays.contains(day)) {
                continue;
            }
            if (productionDay + BigDecimal.ONE.intValue() == allocationDays) {
                return day;
            }
            productionDay = productionDay + BigDecimal.ONE.intValue();
        }
        return null;
    }

    private SizeCapacityUtils() {
    }
}

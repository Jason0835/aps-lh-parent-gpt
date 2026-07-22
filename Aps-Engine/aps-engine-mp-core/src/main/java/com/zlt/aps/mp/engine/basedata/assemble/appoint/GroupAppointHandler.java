package com.zlt.aps.mp.engine.basedata.assemble.appoint;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.domain.vo.GroupAppointProductionInfoVo;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.engine.service.MonthProductionDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分组信息对象指定生产业务处理器
 * 支持业务场景：
 * 1、某个分组(结构)指定其最大生产天数(只在一台成型机上)
 * 2、某个分组(结构)指定其在某个成型机上固定生产周期(即上机时间，最大生产天数)
 *
 * @author ZLT
 * @date 20260713
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupAppointHandler {

    private final MonthProductionDataService monthProductionDataService;

    /**
     * 加载特殊的指定信息，用以满足
     * 特殊场景业务
     *
     * @param context 排产上下文
     * @return
     */
    public void loadAppointInfo(Context context) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //获取配置信息
        List<GroupAppointProductionInfoVo> allConfigurationList = monthProductionDataService.getMonthAppointProductionInfo(productionContext.getFactoryCode(), productionContext.getYear(), productionContext.getMonth());
        if (CollectionUtils.isEmpty(allConfigurationList)) {
            productionContext.getBaseDataContainer().setAppointMap(Collections.emptyMap());
            return;
        }
        Map<String, GroupAppointProductionInfoVo> appointMap = Maps.newHashMap();
        allConfigurationList.forEach(singleConfiguration -> {
            String groupName = singleConfiguration.getGroupName();
            if (StringUtils.isBlank(groupName)) {
                return;
            }
            Integer maxAllocationDays = singleConfiguration.getMaxAllocationDay();
            if (null == maxAllocationDays || maxAllocationDays <= BigDecimal.ZERO.intValue()) {
                return;
            }
            appointMap.put(groupName, singleConfiguration);
        });
        productionContext.getBaseDataContainer().setAppointMap(appointMap);
        return;
    }

    /**
     * 在产机台：续作分组(结构)分配后，进行分配信息调整
     * 场景：为指定了机台分配时间段让路
     * =0 表示没有调整
     * >0 表示延后了(在该场景下不会出现)
     * <0 表示提前了(在当前场景下，正常都是<0)
     *
     * @param context        排产上下文
     * @param allocationInfo 分配信息
     */
    public Integer adjustCxMachineAllocationInfo(Context context, CxMachineAllocationPlanHelper allocationInfo) {
        if (null == allocationInfo) {
            return BigDecimal.ZERO.intValue();
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        String cxMachineCode = allocationInfo.getCxMachineCode();
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineInfoByCode(cxMachineCode);
        if (null == cxMachineInfo) {
            return BigDecimal.ZERO.intValue();
        }
        //理论下机时间
        Integer theoryOffDay = allocationInfo.getEndDay();
        //是否需要调整下机时间
        if (!hasAdvanceOffByContinueCxMachine(productionContext, cxMachineInfo, theoryOffDay)) {
            return BigDecimal.ZERO.intValue();
        }
        //需要调整下机日期
        Integer newOffDay = getContinueCxMachineEndDayByAppoint(productionContext, cxMachineInfo);
        if(null == newOffDay){
            return null;
        }
        return allocationInfo.updateNewEndDay(productionContext, newOffDay);
    }

    /**
     * 判断续作机台是否需要提前下机
     * 1、如果续作成型机台没有配置特定指定排产业务，则无需提前下机
     * 2、如果有配置特定指定排产业务，则可能需要提前下机
     * 获取特定指定排产业务中最早上机的配置(monthStartDay最小的)
     * 2.1、如果续作结构理论的下机日期theoryOffDay >= 最早指定切换结构日，
     * 则需要提前下机
     * 2.2、否则，无需提前下机
     *
     * @param context       排产上下文
     * @param cxMachineInfo 成型机台信息
     * @param theoryOffDay  理论下机日
     * @return
     */
    public boolean hasAdvanceOffByContinueCxMachine(Context context, CxMachineBaseInfoVo cxMachineInfo, Integer theoryOffDay) {
        if (null == theoryOffDay || theoryOffDay < ProductionConstant.MONTH_START_DAY) {
            return false;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        List<GroupAppointProductionInfoVo> configurationList = getAppointConfigurationByCxMachine(productionContext, cxMachineInfo);
        if (CollectionUtils.isEmpty(configurationList)) {
            return false;
        }
        Comparator sort = Comparator.comparing(GroupAppointProductionInfoVo::getMonthStartDay);
        configurationList.sort(sort);
        GroupAppointProductionInfoVo earliest = configurationList.get(BigDecimal.ZERO.intValue());
        return theoryOffDay >= earliest.getMonthStartDay();
    }

    /**
     * 判断成型机台是否有特殊指定排产业务
     *
     * @param context       排产上下文
     * @param cxMachineInfo 成型机台信息
     * @return
     */
    public Integer getContinueCxMachineEndDayByAppoint(Context context, CxMachineBaseInfoVo cxMachineInfo) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        List<GroupAppointProductionInfoVo> configurationList = getAppointConfigurationByCxMachine(productionContext, cxMachineInfo);
        if (CollectionUtils.isEmpty(configurationList)) {
            return null;
        }
        //按起始日从小到大排序
        Comparator sort = Comparator.comparing(GroupAppointProductionInfoVo::getMonthStartDay);
        configurationList.sort(sort);
        GroupAppointProductionInfoVo earliest = configurationList.get(BigDecimal.ZERO.intValue());
        Integer earliestOffDay = earliest.getMonthStartDay();
        Integer monthMaxDay = productionContext.getMonthDays();
        return getPreviousDay(cxMachineInfo, earliestOffDay, monthMaxDay);
    }

    /**
     * 获取针对cxMachineInfo的指定排产配置信息
     *
     * @param context       排产上下文
     * @param cxMachineInfo 成型机台信息对象
     * @return
     */
    private List<GroupAppointProductionInfoVo> getAppointConfigurationByCxMachine(Context context, CxMachineBaseInfoVo cxMachineInfo) {
        if (null == cxMachineInfo || StringUtils.isBlank(cxMachineInfo.getCxMachineCode())) {
            return Collections.emptyList();
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, GroupAppointProductionInfoVo> appointMap = productionContext.getBaseDataContainer().getAppointMap();
        if (CollectionUtils.isEmpty(appointMap)) {
            return Collections.emptyList();
        }
        List<GroupAppointProductionInfoVo> configurationList = Lists.newLinkedList(appointMap.values());
        List<GroupAppointProductionInfoVo> hasConfigurationList = configurationList.stream().filter(configuration -> cxMachineInfo.getCxMachineCode().equals(configuration.getCxMachineCode())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasConfigurationList)) {
            return Collections.emptyList();
        }
        return hasConfigurationList;
    }

    /**
     * 获取cxMachineInfo其在earliestOffDay的前一个排产日
     * 需要剔除其停产日
     *
     * @param cxMachineInfo  成型机信息对象
     * @param earliestOffDay 下机日
     * @param monthEndDay    月末最大日
     * @return
     */
    private Integer getPreviousDay(CxMachineBaseInfoVo cxMachineInfo, Integer earliestOffDay, Integer monthEndDay) {
        if (null == cxMachineInfo || null == earliestOffDay || null == monthEndDay) {
            return null;
        }
        if (ProductionConstant.MONTH_START_DAY.equals(earliestOffDay)) {
            return null;
        }
        if (earliestOffDay > monthEndDay) {
            return monthEndDay;
        }
        Integer startDay = earliestOffDay;
        for (; startDay >= ProductionConstant.MONTH_START_DAY; ) {
            startDay = startDay - BigDecimal.ONE.intValue();
            //非停产
            if (!cxMachineInfo.getStopDayInfo().contains(startDay)) {
                break;
            }
        }
        if (startDay < ProductionConstant.MONTH_START_DAY) {
            return null;
        }
        return startDay;
    }

}

package com.zlt.aps.mp.engine.basedata.assemble.calendar;

import com.google.common.collect.Maps;
import com.zlt.aps.mp.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ContinueGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.ProductionDayInfoVo;
import com.zlt.aps.mp.engine.logrecorder.TbrBeforeProductionGroupLogRecorder;
import com.zlt.aps.mp.engine.service.MonthProductionDataService;
import com.zlt.aps.mp.engine.service.ProductionMdmDataService;
import com.zlt.aps.mp.engine.utils.ProductionCycleUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工作日历相关业务处理器
 *
 * @author ZLT
 * @date 20260626
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkerCalendarHandler {
    /**
     * 主数据数据提供接口
     */
    private final ProductionMdmDataService dataService;
    /**
     * 月度排产计划服务数据提供接口
     */
    private final MonthProductionDataService monthProductionDataService;

    /**
     * 获取当前排产年-月前一个月的续作结构信息
     * 即最后一个排产日排产的结构
     *
     * @param context 排产上下文
     * @return
     */
    public Set<String> getContinueGroupList(Context context) {
        Map<String, Set<String>> continueGroupInfo = getContinueGroupInfo(context, false);
        if (CollectionUtils.isEmpty(continueGroupInfo)) {
            return Collections.emptySet();
        }
        return continueGroupInfo.keySet();
    }

    /**
     * 获取当前排产年-月前一个月最后一日的排产结构信息
     * 即续作结构和在产机台
     *
     * @param context    排产上下文
     * @param isPrintLog 是否打印日志
     * @return
     */
    public Map<String, Set<String>> getContinueGroupInfo(Context context, boolean isPrintLog) {
        //获取前一个月的排产版本信息
        MpFactoryProductionVersion previousVersion = getLastMonthProductionVersion(context);
        if (null == previousVersion) {
            return Collections.emptyMap();
        }
        Integer lastDay = getProductionVersionLastDay(context, previousVersion, isPrintLog);
        if (null == lastDay) {
            return Collections.emptyMap();
        }
        List<ContinueGroupInfo> continueGroupInfoList = monthProductionDataService.getContinueGroupInfo(previousVersion, lastDay);
        if (isPrintLog) {
            log.info(TbrBeforeProductionGroupLogRecorder.addReadContinueGroupDataLog(context, continueGroupInfoList));
        }
        if (CollectionUtils.isEmpty(continueGroupInfoList)) {
            return Collections.emptyMap();
        }
        Map<String, List<ContinueGroupInfo>> continueGroupInfoMap = continueGroupInfoList.stream().collect(Collectors.groupingBy(ContinueGroupInfo::getGroupName));
        Map<String, Set<String>> continueGroupInfo = Maps.newHashMap();
        continueGroupInfoMap.forEach((groupName, continueCxMachineInfoList) -> {
            if (CollectionUtils.isEmpty(continueCxMachineInfoList)) {
                return;
            }
            Set<String> continueCxMachineSet = continueCxMachineInfoList.stream().map(ContinueGroupInfo::getCxMachineCode).collect(Collectors.toSet());
            continueGroupInfo.put(groupName, continueCxMachineSet);
        });
        return continueGroupInfo;
    }

    /**
     * 获取当前年-月前一个月的排产版本信息
     *
     * @param context 排产上下文
     * @return
     */
    private MpFactoryProductionVersion getLastMonthProductionVersion(Context context) {
        String factoryCode = context.getFactoryCode();
        LocalDate previousMonth = context.getPreviousMonth();
        Integer year = previousMonth.getYear();
        Integer month = previousMonth.getMonthValue();
        return monthProductionDataService.getFinalVersion(factoryCode, year, month);
    }

    /**
     * 获取排产版本的最后一个排产日
     *
     * @param context           排产上下文
     * @param productionVersion 排产版本信息
     * @param isPrintLog        是否大于日志
     * @return
     */
    private Integer getProductionVersionLastDay(Context context, MpFactoryProductionVersion productionVersion, boolean isPrintLog) {
        if (null == productionVersion) {
            return null;
        }
        Context productionContext = new Context();
        productionContext.setFactoryCode(productionVersion.getFactoryCode());
        productionContext.setYear(productionVersion.getYear());
        productionContext.setMonth(productionVersion.getMonth());
        productionContext.setProductionStartDate(productionVersion.getProductionStartDate());
        productionContext.setProductionEndDate(productionVersion.getProductionEndDate());
        List<ProductionDayInfoVo> productionCycleDayInfo = dataService.getProductCalendar(productionContext);
        if (isPrintLog) {
            LocalDate previousMonth = context.getPreviousMonth();
            log.info(TbrBeforeProductionGroupLogRecorder.addReaderPreviousMonthProductionCalendarLog(context, productionCycleDayInfo, previousMonth));
        }
        if (CollectionUtils.isEmpty(productionCycleDayInfo)) {
            return null;
        }
        //确认最后排产日
        Integer lastDay = ProductionCycleUtils.getLastProductionDay(productionVersion, productionCycleDayInfo);
        if (lastDay <= BigDecimal.ZERO.intValue()) {
            return null;
        }
        return lastDay;
    }
}

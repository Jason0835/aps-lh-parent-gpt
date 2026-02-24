package com.zlt.aps.factory.logrecorder;

import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.*;
import com.zlt.aps.factory.enums.TbrMouldProductionLogType;
import com.zlt.aps.factory.utils.TbrProductionLogUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 关键数据信息日志记录器
 * TBR-结构名
 * PCR-英寸
 *
 * @author ZLT
 * @date 20260108
 */
@Slf4j
public class KeyInformationLogRecorder {

    /**
     * 记录初始构建后的各结构情形
     *
     * @param context             排产上下文
     * @param allGroupPlanMap     所有结构分组信息
     * @param allContinueGroupMap 所有在机结构分组信息
     */
    public static void recorderInitGroupInfoLog(Context context, Map<String, ProductionPlanGroupInfo> allGroupPlanMap, Map<String, CxContinueInfoHelper> allContinueGroupMap) {
        if (CollectionUtils.isEmpty(allGroupPlanMap)) {
            return;
        }
        allGroupPlanMap.forEach((structureName, groupPlanInfo) -> {
            CxContinueInfoHelper continueInfo = allContinueGroupMap.get(structureName);
            if (null == continueInfo) {
                addContinueGroupInfo(context, groupPlanInfo, "非在机");
                return;
            }
            addContinueGroupInfo(context, groupPlanInfo, "在机");
            addContinueSkuInfo(context, groupPlanInfo, continueInfo);
        });
    }

    /**
     * 记录初始构建后的各结构情形
     *
     * @param context                排产上下文
     * @param allGroupPlanMap        所有结构分组信息
     * @param allContinueGroupMap    所有在机结构分组信息
     * @param continueAllocationList 分配结果
     */
    public static void recorderContinueAllocationGroupInfoLog(Context context, Map<String, ProductionPlanGroupInfo> allGroupPlanMap, Map<String, CxContinueInfoHelper> allContinueGroupMap, List<CxMachineAllocationPlanHelper> continueAllocationList) {
        if (CollectionUtils.isEmpty(allGroupPlanMap)) {
            return;
        }
        Map<String, List<CxMachineAllocationPlanHelper>> groupAllocationMap = getAllocationInfoByGroupName(continueAllocationList);
        allGroupPlanMap.forEach((structureName, groupPlanInfo) -> {
            CxContinueInfoHelper continueInfo = allContinueGroupMap.get(structureName);
            if (null == continueInfo) {
                return;
            }
            List<CxMachineAllocationPlanHelper> allocationInfo = groupAllocationMap.get(structureName);
            addAllocationResultInfo(context, groupPlanInfo, allocationInfo);
        });
    }

    /**
     * 增加在机机构初始化信息日志记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，分组结构排产开始====
     *
     * @param context 排程上下文
     */
    private static void addContinueGroupInfo(Context context, ProductionPlanGroupInfo continueGroupPlanInfo, String text) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，%s结构 %s 汇总信息：总需求量：%s 模具最大排产量：%s 最低硫化机台数：%s 最小日硫化量：%s 需排产天数：%s 估算机台数：%s ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                text, continueGroupPlanInfo.getGroupName(), continueGroupPlanInfo.getAllDemandQty(), continueGroupPlanInfo.getSumPlanQty(),
                continueGroupPlanInfo.getMinLhMachineCount(), continueGroupPlanInfo.getMinLhDayCapacityQty(), continueGroupPlanInfo.getTheoryDays(),
                continueGroupPlanInfo.getNeedCxCapacityMachineCount());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SUMMARY_INFO_SUM, logContent);
    }

    /**
     * 增加在机机构初始化信息日志记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，分组结构排产开始====
     *
     * @param context 排程上下文
     */
    private static void addContinueSkuInfo(Context context, ProductionPlanGroupInfo continueGroupPlanInfo, CxContinueInfoHelper continueInfo) {
        String onLineMachineInfo;
        if (CollectionUtils.isEmpty(continueInfo.getCxMachineCodeSet())) {
            onLineMachineInfo = "没有在产机台";
        } else {
            onLineMachineInfo = String.join(StringConstant.COMMA, continueInfo.getCxMachineCodeSet());
        }
        StringBuilder skuInfo = new StringBuilder();
        Map<String, CxContinueSkuInfoHelper> continueSkuInfo = continueInfo.getContinueSkuMouldNumberMap();
        if (CollectionUtils.isEmpty(continueSkuInfo)) {
            skuInfo.append("没有续作Sku");
        } else {
            String skuFormat = "续作Sku：%s 模具数 %s";
            continueSkuInfo.forEach((materialDesc, detail) -> skuInfo.append(System.lineSeparator()).append(String.format(skuFormat, materialDesc, detail.getMouldNumber())));
        }
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机结构 %s 在产机台 %s 续作Sku信息：%s ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                continueGroupPlanInfo.getGroupName(), onLineMachineInfo, skuInfo);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SUMMARY_INFO_SUM, logContent);
    }

    /**
     * 按分组名进行汇总
     *
     * @param allAllocationInfoList 机台分配明细
     */
    private static Map<String, List<CxMachineAllocationPlanHelper>> getAllocationInfoByGroupName(List<CxMachineAllocationPlanHelper> allAllocationInfoList) {
        if (CollectionUtils.isEmpty(allAllocationInfoList)) {
            return Collections.emptyMap();
        }
        List<CxMachineAllocationPlanHelper> effectiveList = allAllocationInfoList.stream().filter(singleAllocation -> StringUtils.isNotBlank(singleAllocation.getAllocationGroup())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(effectiveList)) {
            return Collections.emptyMap();
        }
        return effectiveList.stream().collect(Collectors.groupingBy(CxMachineAllocationPlanHelper::getAllocationGroup));
    }

    /**
     * 增加在机机构初始化信息日志记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，分组结构排产开始====
     *
     * @param context 排程上下文
     */
    private static void addAllocationResultInfo(Context context, ProductionPlanGroupInfo continueGroupPlanInfo, List<CxMachineAllocationPlanHelper> allocationInfo) {
        String onLineMachineInfo;
        StringBuilder cxMachineAllocationInfo = new StringBuilder();
        if (CollectionUtils.isEmpty(allocationInfo)) {
            onLineMachineInfo = "没有分配到在产机台";
        } else {
            onLineMachineInfo = allocationInfo.stream().map(CxMachineAllocationPlanHelper::getCxMachineCode).collect(Collectors.joining(StringConstant.COMMA));
            String onLineMachineFormat = "在产机台：%s 分配天数 %s 从%s~%s";
            allocationInfo.forEach(singleAllocation -> cxMachineAllocationInfo.append(System.lineSeparator()).append(String.format(onLineMachineFormat, singleAllocation.getCxMachineCode(), singleAllocation.getAllocationDay(), singleAllocation.getStartDay(), singleAllocation.getEndDay())));
        }
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机结构 %s 在产机台 %s 机台分配信息：%s ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                continueGroupPlanInfo.getGroupName(), onLineMachineInfo, cxMachineAllocationInfo);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_GROUP_CONTINUE_CX_MACHINE_SUMMARY_INFO_SUM, logContent);
    }
}

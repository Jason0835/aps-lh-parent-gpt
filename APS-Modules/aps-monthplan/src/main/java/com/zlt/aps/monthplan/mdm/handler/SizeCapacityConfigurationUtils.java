package com.zlt.aps.monthplan.mdm.handler;

import com.tlt.aps.constant.StringConstant;
import com.tlt.aps.enums.FormingMethodTypeEnum;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.maindata.domain.vo.SizeCapacityParamVo;
import com.zlt.aps.monthplan.api.domain.entity.SizeCapacityConfiguration;
import com.zlt.aps.monthplan.api.domain.vo.BaseMoldingMachineInfoVo;
import com.zlt.aps.monthplan.api.domain.vo.MoldingMachineAllocationInfoVo;
import com.zlt.aps.monthplan.mdm.dto.SizeCapacityAllocationResultDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 寸口产能配置转化工具类
 *
 * @author ZLT
 * @date 20250907
 */
public class SizeCapacityConfigurationUtils {
    /**
     * 多段组装模板
     */
    private static final String MULTI_SEGMENT_FORMAT = "%s|*|%s";

    /**
     * 分配结果，转化成配置信息
     *
     * @param requirementAllocationResult 需求分配结果
     * @param param                       寸口分配参数
     * @param allMoldingMachineInfo       成型产能分配结果
     * @return
     */
    public static List<SizeCapacityConfiguration> buildConfigurationResult(List<SizeCapacityAllocationResultDto> requirementAllocationResult, SizeCapacityParamVo param, Map<String, SizeCapacityAllocationResultDto> globalMap, Map<String, BaseMoldingMachineInfoVo> allMoldingMachineInfo) {
        if (CollectionUtils.isEmpty(requirementAllocationResult) || CollectionUtils.isEmpty(allMoldingMachineInfo)) {
            return Collections.emptyList();
        }
        //提取成型产能分配出现两种寸口或是两种胎体布层级的成型产能信息
        List<BaseMoldingMachineInfoVo> multiSegmentList = new ArrayList<>();
        List<BaseMoldingMachineInfoVo> singleSegmentList = new ArrayList<>();
        allMoldingMachineInfo.entrySet().stream().forEach(moldingMachineEntry -> {
            BaseMoldingMachineInfoVo moldingMachineCapacity = moldingMachineEntry.getValue();
            if (moldingMachineCapacity.isAllocationTwo()) {
                multiSegmentList.add(moldingMachineCapacity);
            } else {
                singleSegmentList.add(moldingMachineCapacity);
            }
        });
        //先提取一段配置信息
        Map<String, SizeCapacityConfiguration> singleSegmentConfigurationMap = SizeCapacityConfigurationUtils.buildBySingleSegment(globalMap, param, singleSegmentList);
        //再提取有多段配置的信息
        List<SizeCapacityConfiguration> multiSegmentConfigurationList = SizeCapacityConfigurationUtils.buildMultiSegmentList(globalMap, param, multiSegmentList);
        //收集-配置
        List<SizeCapacityConfiguration> allConfigurationList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(singleSegmentConfigurationMap)) {
            allConfigurationList.addAll(new ArrayList<>(singleSegmentConfigurationMap.values()));
        }
        if (!CollectionUtils.isEmpty(multiSegmentConfigurationList)) {
            allConfigurationList.addAll(multiSegmentConfigurationList);
        }
        return allConfigurationList;
    }

    /**
     * 构建只有单段分配的成型产能信息-转化成配置
     * 一次法：因一次法产能都是多层，故而对一次法的单段条件：只有一个寸口
     * 二次法：二次法产能是区分单层、多层，故而对二次法的单段条件：只有一个寸口且只有一个胎体层级
     * 故而需求处理
     * 一次法：成型法、寸口相同，胎体布层级单层、多层需求需叠加
     * 二次法：成型法、寸口、胎体布层级都相同
     * 同时，需要以成型法、寸口、胎体布层级、成型产能类型维度进行合并
     *
     * @param globalMap         所有需求信息
     * @param param             寸口分配参数
     * @param singleSegmentList 单段分配的成型集合
     * @return
     */
    private static Map<String, SizeCapacityConfiguration> buildBySingleSegment(Map<String, SizeCapacityAllocationResultDto> globalMap, SizeCapacityParamVo param, List<BaseMoldingMachineInfoVo> singleSegmentList) {
        if (CollectionUtils.isEmpty(singleSegmentList)) {
            return Collections.emptyMap();
        }
        Map<String, SizeCapacityConfiguration> configurationMap = new HashMap<>();
        Map<String, Set<String>> configurationCodeMap = new HashMap<>();
        singleSegmentList.stream().forEach(moldingMachineInfo -> {
            String moldingMachineCode = moldingMachineInfo.getMoldingMachineCode();
            SizeCapacityConfiguration configuration = buildConfigurationBySingleSegment(globalMap, param, moldingMachineInfo);
            if (null == configuration) {
                return;
            }
            String mergeKey = configuration.getMergeConfigurationKey();
            SizeCapacityConfiguration existing = configurationMap.get(mergeKey);
            Set<String> codeSet = configurationCodeMap.get(mergeKey);
            if (null == codeSet) {
                codeSet = new HashSet<>();
            }
            codeSet.add(moldingMachineCode);
            if (null == existing) {
                configurationMap.put(mergeKey, configuration);
                configurationCodeMap.put(mergeKey, codeSet);
                return;
            }
            //叠加分配天数
            Integer wholeMachineNumber = existing.getWholeMachineNumber();
            if (null == wholeMachineNumber) {
                wholeMachineNumber = BigDecimal.ZERO.intValue();
            }
            Integer newWholeMachineNumber = configuration.getWholeMachineNumber();
            if (null == newWholeMachineNumber) {
                newWholeMachineNumber = BigDecimal.ZERO.intValue();
            }
            wholeMachineNumber = wholeMachineNumber + newWholeMachineNumber;
            Integer remainingDays = existing.getRemainingDays();
            if (null == remainingDays) {
                remainingDays = BigDecimal.ZERO.intValue();
            }
            Integer newRemainingDays = configuration.getRemainingDays();
            if (null == newRemainingDays) {
                newRemainingDays = BigDecimal.ZERO.intValue();
            }
            remainingDays = remainingDays + newRemainingDays;
            if (remainingDays > param.getMonthMaxDays()) {
                wholeMachineNumber = wholeMachineNumber + BigDecimal.ONE.intValue();
                remainingDays = remainingDays - param.getMonthMaxDays();
            }
            existing.setRemainingDays(remainingDays);
            existing.setWholeMachineNumber(wholeMachineNumber);
        });
        if (!CollectionUtils.isEmpty(configurationMap)) {
            configurationMap.entrySet().forEach(entry -> {
                Set<String> codeSet = configurationCodeMap.get(entry.getKey());
                if (CollectionUtils.isEmpty(codeSet)) {
                    return;
                }
                entry.getValue().setRemark(String.join(StringConstant.COMMA, new ArrayList<>(codeSet)));
            });
        }
        return configurationMap;
    }

    /**
     * 存在多段配置的成型产能配置
     * 将多段配置转换成树形结构数据存储
     * 1、一次法
     * 则表示有两个寸口，同寸口间不同胎体布层级需求都视为多层胎体布层级
     * 2、二次法
     * 则两个寸口或是两种胎体布层级(单层、多层)
     *
     * @param globalMap        所有需求
     * @param param            寸口产能分配参数
     * @param multiSegmentList 多段成型产能配置集合
     * @return
     */
    private static List<SizeCapacityConfiguration> buildMultiSegmentList(Map<String, SizeCapacityAllocationResultDto> globalMap, SizeCapacityParamVo param, List<BaseMoldingMachineInfoVo> multiSegmentList) {
        if (CollectionUtils.isEmpty(multiSegmentList)) {
            return Collections.emptyList();
        }
        List<SizeCapacityConfiguration> allMultiSegmentList = new ArrayList<>();
        //每一个成型产能都会变成多段
        multiSegmentList.stream().forEach(multiSegment -> {
            List<SizeCapacityConfiguration> configurationList = buildConfigurationByMultiSegment(globalMap, param, multiSegment);
            if (CollectionUtils.isEmpty(configurationList)) {
                return;
            }
            allMultiSegmentList.addAll(configurationList);
        });
        return allMultiSegmentList;
    }

    /**
     * 成型产能-单段配置转化成配置
     * 单段：
     * 一次法：同一寸口、不同层级
     * 二次法：同一寸口、同一层级
     *
     * @param globalMap          所有需求信息
     * @param param              寸口产能分配参数
     * @param moldingMachineInfo 成型产能分配
     * @return
     */
    private static SizeCapacityConfiguration buildConfigurationBySingleSegment(Map<String, SizeCapacityAllocationResultDto> globalMap, SizeCapacityParamVo param, BaseMoldingMachineInfoVo moldingMachineInfo) {
        if (null == moldingMachineInfo) {
            return null;
        }
        List<MoldingMachineAllocationInfoVo> proSizeRequirementList = moldingMachineInfo.getProSizeRequirementList();
        if (CollectionUtils.isEmpty(proSizeRequirementList)) {
            return null;
        }
        MoldingMachineAllocationInfoVo firstAllocationInfo = proSizeRequirementList.get(0);
        //基本需求信息 寸口、成型法、胎体布层级(一次法统一为多层)、总需求量、净需求量、备货需求量
        SizeCapacityConfiguration configuration = buildBaseRequirementInfo(firstAllocationInfo, globalMap, param);
        //成型产能机型
        configuration.setMoldingMachineClsType(moldingMachineInfo.getMoldingMachineClsType());
        configuration.setMoldingMachineClsName(moldingMachineInfo.getMoldingMachineClsName());
        //二次法，则只能是一个层级，故而只有一条记录
        if (FormingMethodTypeEnum.TWO_STAGE_TIRE.getMethodValue().equals(moldingMachineInfo.getMouldMethod())) {
            //分配天数
            configuration.setDayCapacity(firstAllocationInfo.getProSizeQuotaQty().intValue());
            Integer allocationDays = firstAllocationInfo.getAllocationDays();
            if (allocationDays < param.getMonthMaxDays()) {
                configuration.setRemainingDays(allocationDays);
                configuration.setWholeMachineNumber(BigDecimal.ZERO.intValue());
            } else {
                configuration.setRemainingDays(BigDecimal.ZERO.intValue());
                configuration.setWholeMachineNumber(BigDecimal.ONE.intValue());
            }
            return configuration;
        }
        //一次法，可能有两种胎体布层级、分配天数叠加
        Integer remainingDays = BigDecimal.ZERO.intValue();
        for (MoldingMachineAllocationInfoVo allocationInfo : proSizeRequirementList) {
            Integer singleRemainingDays = allocationInfo.getAllocationDays();
            if (null == singleRemainingDays) {
                singleRemainingDays = BigDecimal.ZERO.intValue();
            }
            remainingDays = remainingDays + singleRemainingDays;
        }
        configuration.setRemainingDays(remainingDays);
        Integer allocationDays = configuration.getRemainingDays();
        if (allocationDays < param.getMonthMaxDays()) {
            configuration.setRemainingDays(allocationDays);
        } else {
            configuration.setRemainingDays(null);
            configuration.setWholeMachineNumber(BigDecimal.ONE.intValue());
        }
        //一次法 胎体布层级统一为多层
        configuration.setCarcassClothType(ProductionConstant.MULTILAYER_TIRE_FABRIC);
        return configuration;
    }

    /**
     * 成型多段分配转化
     *
     * @param globalMap          所有需求信息
     * @param param              寸口产能分配参数
     * @param moldingMachineInfo 成型分配信息
     * @return
     */
    private static List<SizeCapacityConfiguration> buildConfigurationByMultiSegment(Map<String, SizeCapacityAllocationResultDto> globalMap, SizeCapacityParamVo param, BaseMoldingMachineInfoVo moldingMachineInfo) {
        if (null == moldingMachineInfo) {
            return Collections.emptyList();
        }
        List<MoldingMachineAllocationInfoVo> proSizeRequirementList = moldingMachineInfo.getProSizeRequirementList();
        if (CollectionUtils.isEmpty(proSizeRequirementList)) {
            return Collections.emptyList();
        }
        Integer maxIndex = proSizeRequirementList.size();
        if (maxIndex <= BigDecimal.ONE.intValue()) {
            return Collections.emptyList();
        }
        List<SizeCapacityConfiguration> configurationList = new ArrayList<>();
        for (int index = 0; index < maxIndex; index++) {
            addConfiguration(proSizeRequirementList, configurationList, index, globalMap, moldingMachineInfo, param);
        }
        return configurationList;
    }

    /**
     * 根据当前配置，构建配置，并加入集合
     *
     * @param proSizeRequirementList 分配集合信息
     * @param configurationList      配置集合信息
     * @param currentIndex           当前配置下标
     * @param globalMap              所有需求信息
     * @param moldingMachineInfo     成型产能
     * @param param                  寸口分配参数
     */
    private static void addConfiguration(List<MoldingMachineAllocationInfoVo> proSizeRequirementList, List<SizeCapacityConfiguration> configurationList, int currentIndex, Map<String, SizeCapacityAllocationResultDto> globalMap, BaseMoldingMachineInfoVo moldingMachineInfo, SizeCapacityParamVo param) {
        Integer maxIndex = proSizeRequirementList.size();
        MoldingMachineAllocationInfoVo oneSelfAllocationInfo = proSizeRequirementList.get(currentIndex);
        //第一段
        if (currentIndex == BigDecimal.ZERO.intValue()) {
            MoldingMachineAllocationInfoVo nextAllocationInfo = proSizeRequirementList.get(currentIndex + BigDecimal.ONE.intValue());
            SizeCapacityConfiguration firstConfiguration = SizeCapacityConfigurationUtils.buildFirstConfiguration(oneSelfAllocationInfo, nextAllocationInfo, globalMap, moldingMachineInfo, param);
            firstConfiguration.setRemark(moldingMachineInfo.getMoldingMachineCode());
            if (null == firstConfiguration) {
                return;
            }
            configurationList.add(firstConfiguration);
            return;
        }
        //最后一段
        if (currentIndex == maxIndex - BigDecimal.ONE.intValue()) {
            MoldingMachineAllocationInfoVo previousAllocationInfo = proSizeRequirementList.get(currentIndex - BigDecimal.ONE.intValue());
            SizeCapacityConfiguration finalConfiguration = SizeCapacityConfigurationUtils.buildLastConfiguration(oneSelfAllocationInfo, previousAllocationInfo, globalMap, moldingMachineInfo, param);
            finalConfiguration.setRemark(moldingMachineInfo.getMoldingMachineCode());
            if (null == finalConfiguration) {
                return;
            }
            configurationList.add(finalConfiguration);
            return;
        }
        //中间段
        MoldingMachineAllocationInfoVo nextAllocationInfo = proSizeRequirementList.get(currentIndex + BigDecimal.ONE.intValue());
        MoldingMachineAllocationInfoVo previousAllocationInfo = proSizeRequirementList.get(currentIndex - BigDecimal.ONE.intValue());
        SizeCapacityConfiguration middleConfiguration = buildMiddleConfiguration(previousAllocationInfo, oneSelfAllocationInfo, nextAllocationInfo, globalMap, moldingMachineInfo, param);
        middleConfiguration.setRemark(moldingMachineInfo.getMoldingMachineCode());
        if (null == middleConfiguration) {
            return;
        }
        configurationList.add(middleConfiguration);
    }

    /**
     * 构建第一段配置信息
     *
     * @param firstAllocationInfo 第一段配置
     * @param nextAllocationInfo  下一段配置
     * @param globalMap           所有需求信息
     * @param moldingMachineInfo  成型产能
     * @return
     */
    private static SizeCapacityConfiguration buildFirstConfiguration(MoldingMachineAllocationInfoVo firstAllocationInfo, MoldingMachineAllocationInfoVo nextAllocationInfo, Map<String, SizeCapacityAllocationResultDto> globalMap, BaseMoldingMachineInfoVo moldingMachineInfo, SizeCapacityParamVo param) {
        if (null == firstAllocationInfo || null == nextAllocationInfo || null == moldingMachineInfo) {
            return null;
        }
        String capacityKey = firstAllocationInfo.getSizeCapacityGroupKey();
        String nextCapacityKey = nextAllocationInfo.getSizeCapacityGroupKey();
        SizeCapacityAllocationResultDto requirement = globalMap.get(capacityKey);
        SizeCapacityAllocationResultDto nextRequirement = globalMap.get(nextCapacityKey);
        if (null == requirement || null == nextRequirement) {
            return null;
        }
        String machineCode = moldingMachineInfo.getMoldingMachineCode();
        //基本需求信息 寸口、成型法、胎体布层级(一次法统一为多层)、总需求量、净需求量、备货需求量
        SizeCapacityConfiguration configuration = buildBaseRequirementInfo(firstAllocationInfo, globalMap, param);
        //成型机型
        configuration.setMoldingMachineClsType(moldingMachineInfo.getMoldingMachineClsType());
        configuration.setMoldingMachineClsName(moldingMachineInfo.getMoldingMachineClsName());
        String oneselfKey = String.format(MULTI_SEGMENT_FORMAT, configuration.getGroupKey(), machineCode);
        configuration.setOneselfKey(oneselfKey);
        //下一寸口 一次法 统一为多层
        String nextCapacityGroupKey = nextCapacityKey;
        if (FormingMethodTypeEnum.SINGLE_STAGE_TIRE.equals(nextRequirement.getMouldMethod())) {
            nextCapacityGroupKey = nextAllocationInfo.getTireFabricNumberCapacityGroupKey(ProductionConstant.MULTILAYER_TIRE_FABRIC);
        }
        String nextGroupKey = String.format(MULTI_SEGMENT_FORMAT, nextCapacityGroupKey, machineCode);
        configuration.setNextGroupKey(nextGroupKey);
        configuration.setNextProSize(nextAllocationInfo.getProSize());
        return configuration;
    }

    /**
     * 构建中间段配置
     *
     * @param previousAllocationInfo 前一段配置
     * @param oneSelfAllocationInfo  本身配置
     * @param latterAllocationInfo   后一段配置
     * @param globalMap              所有需求信息
     * @param moldingMachineInfo     成型产能
     * @return
     */
    private static SizeCapacityConfiguration buildMiddleConfiguration(MoldingMachineAllocationInfoVo previousAllocationInfo, MoldingMachineAllocationInfoVo oneSelfAllocationInfo, MoldingMachineAllocationInfoVo latterAllocationInfo, Map<String, SizeCapacityAllocationResultDto> globalMap, BaseMoldingMachineInfoVo moldingMachineInfo, SizeCapacityParamVo param) {
        if (null == previousAllocationInfo || null == oneSelfAllocationInfo || null == latterAllocationInfo || null == moldingMachineInfo) {
            return null;
        }
        String oneCapacityKey = oneSelfAllocationInfo.getSizeCapacityGroupKey();
        String superCapacityKey = previousAllocationInfo.getSizeCapacityGroupKey();
        String nextCapacityKey = latterAllocationInfo.getSizeCapacityGroupKey();
        SizeCapacityAllocationResultDto requirement = globalMap.get(oneCapacityKey);
        SizeCapacityAllocationResultDto superRequirement = globalMap.get(superCapacityKey);
        SizeCapacityAllocationResultDto nextRequirement = globalMap.get(nextCapacityKey);
        if (null == requirement || null == superRequirement || null == nextRequirement) {
            return null;
        }
        String machineCode = moldingMachineInfo.getMoldingMachineCode();
        //基本需求信息 寸口、成型法、胎体布层级(一次法统一为多层)、总需求量、净需求量、备货需求量
        SizeCapacityConfiguration configuration = buildBaseRequirementInfo(oneSelfAllocationInfo, globalMap, param);
        //成型机型
        configuration.setMoldingMachineClsType(moldingMachineInfo.getMoldingMachineClsType());
        configuration.setMoldingMachineClsName(moldingMachineInfo.getMoldingMachineClsName());
        String oneselfKey = String.format(MULTI_SEGMENT_FORMAT, configuration.getGroupKey(), machineCode);
        configuration.setOneselfKey(oneselfKey);
        //上一段寸口 一次法 统一为多层
        String superCapacityGroupKey = superCapacityKey;
        if (FormingMethodTypeEnum.SINGLE_STAGE_TIRE.equals(superRequirement.getMouldMethod())) {
            superCapacityGroupKey = previousAllocationInfo.getTireFabricNumberCapacityGroupKey(ProductionConstant.MULTILAYER_TIRE_FABRIC);
        }
        String superGroupKey = String.format(MULTI_SEGMENT_FORMAT, superCapacityGroupKey, machineCode);
        configuration.setSuperGroupKey(superGroupKey);
        //下一寸口 一次法 统一为多层
        String nextCapacityGroupKey = nextCapacityKey;
        if (FormingMethodTypeEnum.SINGLE_STAGE_TIRE.equals(nextRequirement.getMouldMethod())) {
            nextCapacityGroupKey = latterAllocationInfo.getTireFabricNumberCapacityGroupKey(ProductionConstant.MULTILAYER_TIRE_FABRIC);
        }
        String nextGroupKey = String.format(MULTI_SEGMENT_FORMAT, nextCapacityGroupKey, machineCode);
        configuration.setNextGroupKey(nextGroupKey);
        configuration.setNextProSize(latterAllocationInfo.getProSize());
        return configuration;
    }

    /**
     * 构建最后一段配置
     *
     * @param finalAllocationInfo    最后一段配置
     * @param previousAllocationInfo 前一段配置
     * @param globalMap              所有需求集合
     * @param moldingMachineInfo     成型产能
     * @return
     */
    private static SizeCapacityConfiguration buildLastConfiguration(MoldingMachineAllocationInfoVo finalAllocationInfo, MoldingMachineAllocationInfoVo previousAllocationInfo, Map<String, SizeCapacityAllocationResultDto> globalMap, BaseMoldingMachineInfoVo moldingMachineInfo, SizeCapacityParamVo param) {
        if (null == finalAllocationInfo || null == previousAllocationInfo || null == moldingMachineInfo) {
            return null;
        }
        String capacityKey = finalAllocationInfo.getSizeCapacityGroupKey();
        String superCapacityKey = previousAllocationInfo.getSizeCapacityGroupKey();
        SizeCapacityAllocationResultDto requirement = globalMap.get(capacityKey);
        SizeCapacityAllocationResultDto superRequirement = globalMap.get(superCapacityKey);
        if (null == requirement || null == superRequirement) {
            return null;
        }
        String machineCode = moldingMachineInfo.getMoldingMachineCode();
        //基本需求信息 寸口、成型法、胎体布层级(一次法统一为多层)、总需求量、净需求量、备货需求量
        SizeCapacityConfiguration configuration = buildBaseRequirementInfo(finalAllocationInfo, globalMap, param);
        //成型机型
        configuration.setMoldingMachineClsType(moldingMachineInfo.getMoldingMachineClsType());
        configuration.setMoldingMachineClsName(moldingMachineInfo.getMoldingMachineClsName());
        String oneselfKey = String.format(MULTI_SEGMENT_FORMAT, configuration.getGroupKey(), machineCode);
        configuration.setOneselfKey(oneselfKey);
        //上一段寸口 一次法 统一为多层
        String superCapacityGroupKey = superCapacityKey;
        if (FormingMethodTypeEnum.SINGLE_STAGE_TIRE.equals(superRequirement.getMouldMethod())) {
            superCapacityGroupKey = previousAllocationInfo.getTireFabricNumberCapacityGroupKey(ProductionConstant.MULTILAYER_TIRE_FABRIC);
        }
        String superGroupKey = String.format(MULTI_SEGMENT_FORMAT, superCapacityGroupKey, machineCode);
        configuration.setSuperGroupKey(superGroupKey);
        return configuration;
    }

    /**
     * 根据分配信息，构建基础
     * 寸口、成型法、胎体布层级
     * 总需求、净需求、备货需求
     *
     * @param allocationInfo 分配信息
     * @param globalMap      所有需求信息
     * @param param          寸口参数配置
     * @return
     */
    private static SizeCapacityConfiguration buildBaseRequirementInfo(MoldingMachineAllocationInfoVo allocationInfo, Map<String, SizeCapacityAllocationResultDto> globalMap, SizeCapacityParamVo param) {
        SizeCapacityConfiguration configuration = new SizeCapacityConfiguration();
        String requirementKey = allocationInfo.getSizeCapacityGroupKey();
        setBaseRequirementInfoByOriginKey(configuration, globalMap, requirementKey);
        //分配天数信息
        configuration.setRemainingDays(allocationInfo.getAllocationDays());
        configuration.setDayCapacity(allocationInfo.getProSizeQuotaQty().intValue());
        configuration.setMaxMouldQty(allocationInfo.getDayMaxMouldQty());
        configuration.setWholeMachineNumber(BigDecimal.ZERO.intValue());
        BigDecimal remainingNumber = BigDecimal.valueOf(allocationInfo.getAllocationDays()).divide(BigDecimal.valueOf(param.getMonthMaxDays()), 1, RoundingMode.HALF_UP);
        configuration.setMachineNumber(remainingNumber);
        //二次法
        if (FormingMethodTypeEnum.TWO_STAGE_TIRE.getMethodValue().equals(allocationInfo.getMouldMethod())) {
            return configuration;
        }
        //一次法，多层则需要增加单层，单层需要增加多层；并将配置的胎体布层级设置为多层
        Integer otherTireFabricNumber;
        if (ProductionConstant.MULTILAYER_TIRE_FABRIC.equals(allocationInfo.getTireFabricNumber())) {
            otherTireFabricNumber = BigDecimal.ONE.intValue();
        } else {
            otherTireFabricNumber = ProductionConstant.MULTILAYER_TIRE_FABRIC;
        }
        String otherRequirementKey = allocationInfo.getTireFabricNumberCapacityGroupKey(otherTireFabricNumber);
        addSingleStageTireRequirementInfo(configuration, globalMap, otherRequirementKey);
        //设置胎体布层级为多层
        configuration.setCarcassClothType(ProductionConstant.MULTILAYER_TIRE_FABRIC);
        return configuration;
    }

    /**
     * 根据产能需求key设置基础信息
     *
     * @param configuration  配置信息
     * @param globalMap      所有产能需求组
     * @param requirementKey 产能需求分组Key
     */
    private static void setBaseRequirementInfoByOriginKey(SizeCapacityConfiguration configuration, Map<String, SizeCapacityAllocationResultDto> globalMap, String requirementKey) {
        if (CollectionUtils.isEmpty(globalMap) || StringUtils.isBlank(requirementKey) || null == configuration) {
            return;
        }
        SizeCapacityAllocationResultDto origin = globalMap.get(requirementKey);
        if (null == origin) {
            return;
        }
        //寸口、成型法、胎体布层级
        configuration.setProSize(origin.getProSize());
        configuration.setWorkWearType(origin.getWorkWearType());
        configuration.setMouldMethod(origin.getMouldMethod());
        configuration.setCarcassClothType(origin.getTireFabricNumber());
        //总需求量、净需求量、备货需求量
        configuration.setEffectiveDemandQty(origin.getDemandQty());
        configuration.setEffectiveNetDemandQty(origin.getNetDemandQty());
        configuration.setEffectiveStockUpDemandQty(origin.getStockUpDemandQty());
    }

    /**
     * 增加成型法另外胎体布层级的需求
     * 原来是单层，则增加多层
     * 原来是多层，则增加单层
     *
     * @param configuration       现在的key配置信息
     * @param globalMap           所有产能需求组
     * @param otherRequirementKey 另外的产能需求组key
     */
    private static void addSingleStageTireRequirementInfo(SizeCapacityConfiguration configuration, Map<String, SizeCapacityAllocationResultDto> globalMap, String otherRequirementKey) {
        if (CollectionUtils.isEmpty(globalMap) || StringUtils.isBlank(otherRequirementKey) || null == configuration) {
            return;
        }
        if (otherRequirementKey.equals(configuration.getGroupKey())) {
            return;
        }
        SizeCapacityAllocationResultDto otherRequirement = globalMap.get(otherRequirementKey);
        if (null == otherRequirement) {
            return;
        }
        if (configuration.getProSize().compareTo(otherRequirement.getProSize()) != BigDecimal.ZERO.intValue() || !configuration.getMouldMethod().equals(otherRequirement.getMouldMethod())) {
            return;
        }
        //需求量叠加
        Long effectiveDemandQty = configuration.getEffectiveDemandQty();
        if (null == effectiveDemandQty) {
            effectiveDemandQty = BigDecimal.ZERO.longValue();
        }
        Long effectiveNetDemandQty = configuration.getEffectiveNetDemandQty();
        if (null == effectiveNetDemandQty) {
            effectiveNetDemandQty = BigDecimal.ZERO.longValue();
        }
        Long effectiveStockUpDemandQty = configuration.getEffectiveStockUpDemandQty();
        if (null == effectiveStockUpDemandQty) {
            effectiveStockUpDemandQty = BigDecimal.ZERO.longValue();
        }
        effectiveDemandQty = effectiveDemandQty + otherRequirement.getDemandQty();
        effectiveNetDemandQty = effectiveNetDemandQty + otherRequirement.getNetDemandQty();
        effectiveStockUpDemandQty = effectiveStockUpDemandQty + otherRequirement.getStockUpDemandQty();
        configuration.setEffectiveDemandQty(effectiveDemandQty);
        configuration.setEffectiveNetDemandQty(effectiveNetDemandQty);
        configuration.setEffectiveStockUpDemandQty(effectiveStockUpDemandQty);
    }

    private SizeCapacityConfigurationUtils() {

    }
}
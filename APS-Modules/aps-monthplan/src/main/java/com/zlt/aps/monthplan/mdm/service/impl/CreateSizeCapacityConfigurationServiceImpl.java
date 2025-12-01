package com.zlt.aps.monthplan.mdm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.FormingMethodTypeEnum;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.BeanCopyUtils;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;
import com.zlt.aps.factory.service.IMonthPlanProductionSchedulingService;
import com.zlt.aps.maindata.domain.entity.CxScheduleResultSearchVo;
import com.zlt.aps.maindata.domain.vo.SizeCapacityParamVo;
import com.zlt.aps.maindata.mapper.SizeCapacityConfigurationMapper;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.service.IMdmMoldingMachineService;
import com.zlt.aps.maindata.service.IMdmProductionCalendarService;
import com.zlt.aps.monthplan.api.domain.entity.FactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.SizeCapacityConfiguration;
import com.zlt.aps.monthplan.api.domain.vo.*;
import com.zlt.aps.monthplan.factory.mapper.FactoryProductionVersionMapper;
import com.zlt.aps.monthplan.factory.service.IMonthPlanPreProductionCapacityService;
import com.zlt.aps.monthplan.mdm.dto.SizeCapacityAllocationDto;
import com.zlt.aps.monthplan.mdm.dto.SizeCapacityAllocationResultDto;
import com.zlt.aps.monthplan.mdm.handler.SizeCapacityAllocationService;
import com.zlt.aps.monthplan.mdm.service.ICreateSizeCapacityConfigurationService;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 生成寸口产能配置信息
 *
 * @author ZLT
 * @date 20250617
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateSizeCapacityConfigurationServiceImpl implements ICreateSizeCapacityConfigurationService {

    private final FactoryProductionVersionMapper factoryProductionVersionMapper;

    private final SizeCapacityConfigurationMapper sizeCapacityConfigurationMapper;

    private final BaseDao baseDao;

    private final IFactoryParamService factoryParamService;

    private final IMdmMoldingMachineService moldingMachineService;

    private final IMdmProductionCalendarService productionCalendarService;

    private final IMonthPlanProductionSchedulingService monthPlanProductionSchedulingService;

    private final IMonthPlanPreProductionCapacityService monthPlanPreProductionCapacityService;

    private final SizeCapacityAllocationService sizeCapacityAllocationService;

    @Override
    public AjaxResult buildSizeCapacityConfiguration(BuildSizeCapacityParamVo buildParam) {
        FactoryProductionParamVo factoryProductionParam = BeanCopyUtils.copyBean(buildParam, FactoryProductionParamVo.class);
        AjaxResult checkResult = checkBaseInfo(factoryProductionParam);
        if (AjaxResult.Type.ERROR.value() == (Integer) checkResult.get(AjaxResult.CODE_TAG)) {
            return checkResult;
        }
        Context context = buildContext(factoryProductionParam);
        //得到产能需求--进行模具产能预占，特殊轮胎类型预分配等
        monthPlanProductionSchedulingService.calculateSizeCapacityRequire(context);
        List<MonthPlanManufacturingRequirementVo> requireList = context.getSizeCapacityRequireList();
        if (CollectionUtils.isEmpty(requireList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.noRequireData"));
        }
        //获取可排产的需求
        List<MonthPlanManufacturingRequirementVo> sizeCapacityRequireList = requireList.stream().filter(plan -> YesOrNoEnum.YES.getValue().equals(plan.getIsProduction())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(sizeCapacityRequireList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.noRequireData"));
        }
        //保存(模具)预占产能分配
        monthPlanPreProductionCapacityService.savePreProductionCapacity(requireList);
        String factoryCode = factoryProductionParam.getFactoryCode();
        Integer year = factoryProductionParam.getYear();
        Integer month = factoryProductionParam.getMonth();
        SizeCapacityParamVo param = getSizeCapacityParam(factoryCode, year, month);
        //成型机信息--通过接口获取
        CxScheduleResultSearchVo cxScheduleParam = new CxScheduleResultSearchVo();
        cxScheduleParam.setFactoryCode(factoryCode);
        cxScheduleParam.setScheduleDate(buildParam.getFormingDate());
        List<BaseMoldingMachineInfoVo> moldingMachineInfoList = moldingMachineService.getCurrencyMachineInfo(cxScheduleParam);
        List<SizeCapacityConfiguration> configurationList = sizeCapacityAllocationService.allocationMoldingMachineCapacity(sizeCapacityRequireList, moldingMachineInfoList, param);
        if (CollectionUtils.isEmpty(configurationList)) {
            return AjaxResult.success();
        }
        configurationList.stream().forEach(configuration -> {
            configuration.setYear(year);
            configuration.setMonth(month);
            configuration.setFactoryCode(factoryCode);
            configuration.setMonthPlanVersion(buildParam.getMonthPlanVersion());
            configuration.resetMachineNumber(param.getMonthMaxDays());
        });
        //先删除
        QueryWrapper<SizeCapacityConfiguration> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("FACTORY_CODE", factoryCode);
        deleteWrapper.eq("YEAR", year);
        deleteWrapper.eq("MONTH", month);
        deleteWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        sizeCapacityConfigurationMapper.delete(deleteWrapper);
        baseDao.insertBatch(configurationList);
        return AjaxResult.success();
    }

    /**
     * 按寸口由大到小分配
     *
     * @param buildParam
     * @return
     */
    public AjaxResult buildSizeCapacityConfigurationV2(BuildSizeCapacityParamVo buildParam) {
        FactoryProductionParamVo factoryProductionParam = BeanCopyUtils.copyBean(buildParam, FactoryProductionParamVo.class);
        AjaxResult checkResult = checkBaseInfo(factoryProductionParam);
        if (AjaxResult.Type.ERROR.value() == (Integer) checkResult.get(AjaxResult.CODE_TAG)) {
            return checkResult;
        }
        Context context = buildContext(factoryProductionParam);
        //得到产能需求--进行模具产能预占，特殊轮胎类型预分配等
        monthPlanProductionSchedulingService.calculateSizeCapacityRequire(context);
        List<MonthPlanManufacturingRequirementVo> requireList = context.getSizeCapacityRequireList();
        if (CollectionUtils.isEmpty(requireList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.noRequireData"));
        }
        //获取可排产的需求
        List<MonthPlanManufacturingRequirementVo> sizeCapacityRequireList = requireList.stream().filter(plan -> YesOrNoEnum.YES.getValue().equals(plan.getIsProduction())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(sizeCapacityRequireList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.noRequireData"));
        }
        //保存(模具)预占产能分配
        monthPlanPreProductionCapacityService.savePreProductionCapacity(requireList);
        String factoryCode = factoryProductionParam.getFactoryCode();
        Integer year = factoryProductionParam.getYear();
        Integer month = factoryProductionParam.getMonth();
        SizeCapacityParamVo param = getSizeCapacityParam(factoryCode, year, month);
        //成型机信息--通过接口获取
        CxScheduleResultSearchVo cxScheduleParam = new CxScheduleResultSearchVo();
        cxScheduleParam.setFactoryCode(factoryCode);
        cxScheduleParam.setScheduleDate(buildParam.getFormingDate());
        List<BaseMoldingMachineInfoVo> moldingMachineInfoList = moldingMachineService.getCurrencyMachineInfo(cxScheduleParam);
        moldingMachineInfoList.stream().forEach(moldingMachineInfo -> {
            moldingMachineInfo.setLeftOverCapacityDays(param.getMonthMaxDays());
            moldingMachineInfo.setProSizeList(new ArrayList<>());
            moldingMachineInfo.setCurrentEmbryoCodeNumber(null);
        });
        Map<String, BaseMoldingMachineInfoVo> moldingMachineInfoMap = moldingMachineInfoList.stream().collect(Collectors.toMap(BaseMoldingMachineInfoVo::getMoldingMachineCode, Function.identity()));
        //已分配集合
        Set<String> assignedMachineSet = new HashSet<>();
        List<SizeCapacityAllocationDto> assignedMachineList = new ArrayList<>();
        //第一轮分配：按照需求分配成型产能：按一次法到二次法、寸口由大到小、层级高到底，在产成型优先分配
        List<SizeCapacityAllocationResultDto> allocationResultList = firstAllocation(sizeCapacityRequireList, moldingMachineInfoMap, moldingMachineInfoList, assignedMachineList, assignedMachineSet, param);
        //第二轮分配：对需求还没有分配完，看是否有剩余产能的成型产能，继续分配
        List<SizeCapacityAllocationResultDto> needAllocationList = allocationResultList.stream().filter(allocationResult -> BigDecimal.valueOf(allocationResult.getLeftOverQty()).compareTo(BigDecimal.ZERO) > BigDecimal.ZERO.intValue()).collect(Collectors.toList());
        //获取有剩余产能的成型产能
        List<SizeCapacityAllocationDto> remainingList = getRemainingCapacityList(assignedMachineList, moldingMachineInfoMap, param);
        if (!CollectionUtils.isEmpty(remainingList) && !CollectionUtils.isEmpty(needAllocationList)) {
            secondAllocation(needAllocationList, remainingList, moldingMachineInfoMap, param);
        }
        //转化成配置
        List<SizeCapacityConfiguration> configurationList = buildConfiguration(allocationResultList, param.getMonthMaxDays(), moldingMachineInfoMap);
        if (CollectionUtils.isEmpty(configurationList)) {
            return AjaxResult.success();
        }
        configurationList.stream().forEach(configuration -> {
            configuration.setYear(year);
            configuration.setMonth(month);
            configuration.setFactoryCode(factoryCode);
            configuration.setMonthPlanVersion(buildParam.getMonthPlanVersion());
        });
        //先删除
        QueryWrapper<SizeCapacityConfiguration> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("FACTORY_CODE", factoryCode);
        deleteWrapper.eq("YEAR", year);
        deleteWrapper.eq("MONTH", month);
        deleteWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        sizeCapacityConfigurationMapper.delete(deleteWrapper);
        baseDao.insertBatch(configurationList);
        return AjaxResult.success();
    }

    /**
     * 旧版分配
     *
     * @param factoryProductionParam
     * @return
     */
    public AjaxResult buildSizeCapacityConfigurationV1(FactoryProductionParamVo factoryProductionParam) {
        AjaxResult checkResult = checkBaseInfo(factoryProductionParam);
        if (AjaxResult.Type.ERROR.value() == (Integer) checkResult.get(AjaxResult.CODE_TAG)) {
            return checkResult;
        }
        Context context = buildContext(factoryProductionParam);
        //得到需求
        monthPlanProductionSchedulingService.calculateSizeCapacityRequire(context);
        List<MonthPlanManufacturingRequirementVo> requireList = context.getSizeCapacityRequireList();
        if (CollectionUtils.isEmpty(requireList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.noRequireData"));
        }
        //获取可排产的需求
        List<MonthPlanManufacturingRequirementVo> sizeCapacityRequireList = requireList.stream().filter(plan -> YesOrNoEnum.YES.getValue().equals(plan.getIsProduction())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(sizeCapacityRequireList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.noRequireData"));
        }
        //按寸口+成型法+胎体布层级汇总需求
        String factoryCode = factoryProductionParam.getFactoryCode();
        Integer year = factoryProductionParam.getYear();
        Integer month = factoryProductionParam.getMonth();
        Map<String, List<MonthPlanManufacturingRequirementVo>> requirementGroupMap = sizeCapacityRequireList.stream().collect(Collectors.groupingBy(MonthPlanManufacturingRequirementVo::getSizeCapacityGroupKey));
        List<MdmMoldingMachineProNumVo> sizeMoldingList = moldingMachineService.getMoldingMachineProNum(factoryCode);
        Map<Integer, List<MdmMoldingMachineProNumVo>> methodMap = sizeMoldingList.stream().collect(Collectors.groupingBy(MdmMoldingMachineProNumVo::getMouldMethod));
        Map<String, MdmMoldingMachineProNumVo> sizeMethodMap = sizeMoldingList.stream().collect(Collectors.toMap(MdmMoldingMachineProNumVo::getSizeCapacityGroupKey, Function.identity()));
        Integer monthMaxDays = productionCalendarService.getMonthMaxDays(factoryCode, year, month);
        List<SizeCapacityConfigurationVo> configurationList = new ArrayList<>();
        requirementGroupMap.entrySet().stream().forEach(sizeCapacityGroupEntry -> {
            String sizeCapacityGroupKey = sizeCapacityGroupEntry.getKey();
            List<MonthPlanManufacturingRequirementVo> requirementList = sizeCapacityGroupEntry.getValue();
            if (CollectionUtils.isEmpty(requirementList)) {
                return;
            }
            MdmMoldingMachineProNumVo moldingMachineConfiguration = sizeMethodMap.get(sizeCapacityGroupKey);
            Long sumRequirementQty = requirementList.stream().mapToLong(MonthPlanManufacturingRequirementVo::getProductionQty).sum();
            MonthPlanManufacturingRequirementVo first = requirementList.get(0);
            SizeCapacityConfigurationVo sizeCapacityConfiguration = buildSizeCapacityConfiguration(first);
            sizeCapacityConfiguration.setDemandQty(sumRequirementQty);
            if (null == moldingMachineConfiguration) {
                sizeCapacityConfiguration.setMaxMachineNumber(BigDecimal.ZERO.intValue());
                sizeCapacityConfiguration.setDayCapacity(BigDecimal.ZERO.intValue());
                sizeCapacityConfiguration.setMachineNumber(BigDecimal.ZERO);
            } else {
                setMachineNumber(sumRequirementQty, moldingMachineConfiguration, monthMaxDays, sizeCapacityConfiguration);
            }
            configurationList.add(sizeCapacityConfiguration);
        });
        if (CollectionUtils.isEmpty(configurationList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.noRequireData"));
        }
        //总量不能超过总数，否则等比例分摊
        Map<String, List<SizeCapacityConfigurationVo>> methodGroupMap = configurationList.stream().collect(Collectors.groupingBy(SizeCapacityConfigurationVo::getMouldMethod));
        methodGroupMap.entrySet().stream().forEach(methodSizeGroupEntry -> {
            String mouldMethod = methodSizeGroupEntry.getKey();
            List<SizeCapacityConfigurationVo> mouldMethodConfigurationList = methodSizeGroupEntry.getValue();
            if (CollectionUtils.isEmpty(mouldMethodConfigurationList)) {
                return;
            }
            FormingMethodTypeEnum mouldMethodType = FormingMethodTypeEnum.getInstance(mouldMethod);
            Integer maxSum = getFormingMethodNumber(methodMap, mouldMethodType);
            BigDecimal configurationNumber = getSumMachineNumber(mouldMethodConfigurationList);
            if (configurationNumber.compareTo(BigDecimal.valueOf(maxSum)) > BigDecimal.ZERO.intValue()) {
                //处理比例分摊
                proportionalSharing(mouldMethodConfigurationList, maxSum, configurationNumber);
            }
            //进舍位处理
            placementProcessing(mouldMethodConfigurationList);
            //处理下一寸口
            processingNextProSize(mouldMethodConfigurationList);
        });
        //先删除
        QueryWrapper<SizeCapacityConfiguration> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("FACTORY_CODE", factoryCode);
        deleteWrapper.eq("YEAR", year);
        deleteWrapper.eq("MONTH", month);
        deleteWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        sizeCapacityConfigurationMapper.delete(deleteWrapper);
        baseDao.insertBatch(configurationList);
        return AjaxResult.success();
    }

    /**
     * 信息校验-参数，及需求计划信息
     *
     * @param factoryProductionParam
     * @return
     */
    private AjaxResult checkBaseInfo(FactoryProductionParamVo factoryProductionParam) {
        String factoryCode = factoryProductionParam.getFactoryCode();
        Integer year = factoryProductionParam.getYear();
        Integer month = factoryProductionParam.getMonth();
        String monthPlanVersion = factoryProductionParam.getMonthPlanVersion();
        if (StringUtils.isBlank(factoryCode) || null == year || null == month || StringUtils.isBlank(monthPlanVersion)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.requireVersionNoEmpty"));
        }
        QueryWrapper<FactoryProductionVersion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        queryWrapper.eq("YEAR", year);
        queryWrapper.eq("MONTH", month);
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        queryWrapper.eq("MONTH_PLAN_VERSION", monthPlanVersion);
        List<FactoryProductionVersion> requireVersionList = factoryProductionVersionMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(requireVersionList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.noExistVersion"));
        }
        return AjaxResult.success();
    }

    /**
     * 构建半钢排产上下文对象
     *
     * @param factoryProductionParam
     * @return
     */
    private Context buildContext(FactoryProductionParamVo factoryProductionParam) {
        Context context = new Context();
        context.setFactoryCode(factoryProductionParam.getFactoryCode());
        context.setYear(factoryProductionParam.getYear());
        context.setMonth(factoryProductionParam.getMonth());
        context.setMonthPlanVersion(factoryProductionParam.getMonthPlanVersion());
        context.setProductionVersion(factoryProductionParam.getProductionVersion());
        context.setPrefixVersion(factoryProductionParam.getPrefixVersion());
        context.setProductType(ProductTypeEnum.SEMI_STEEL);
        return context;
    }

    /**
     * 获取寸口产能分配参数配置信息
     *
     * @param factoryCode 分厂编码
     * @param year        年份
     * @param month       月份
     * @return
     */
    private SizeCapacityParamVo getSizeCapacityParam(String factoryCode, Integer year, Integer month) {
        SizeCapacityParamVo param = factoryParamService.getSizeCapacityAllocationParam(factoryCode);
        Integer monthMaxDays = productionCalendarService.getMonthMaxDays(factoryCode, year, month);
        param.setMonthMaxDays(monthMaxDays);
        return param;
    }

    /**
     * 第一轮分配成型产能
     * 分配顺序：寸口由大到小，成型法由一次法到二次法，胎体层级由高到低依次分配
     * 一个成型产能只分配一次
     *
     * @param sizeCapacityRequireList 排产需求集合
     * @param moldingMachineInfoMap   成型产能信息Map集合
     * @param moldingMachineInfoList  成型产能信息集合
     * @param assignedMachineList     已分配成型产能集合
     * @param assignedMachineSet      已分配成型产能编码
     * @param param                   产能分配参数
     * @return
     */
    private List<SizeCapacityAllocationResultDto> firstAllocation(List<MonthPlanManufacturingRequirementVo> sizeCapacityRequireList, Map<String, BaseMoldingMachineInfoVo> moldingMachineInfoMap, List<BaseMoldingMachineInfoVo> moldingMachineInfoList, List<SizeCapacityAllocationDto> assignedMachineList, Set<String> assignedMachineSet, SizeCapacityParamVo param) {
        List<SizeCapacityAllocationResultDto> allocationResultList = new ArrayList<>();
        //按成型法分组
        Map<String, List<MonthPlanManufacturingRequirementVo>> mouldMethodGroupMap = sizeCapacityRequireList.stream().collect(Collectors.groupingBy(MonthPlanManufacturingRequirementVo::getMouldMethod));
        //提取成型法
        List<String> mouldMethodList = new ArrayList<>(mouldMethodGroupMap.keySet());
        mouldMethodList.sort(Comparator.comparing(String::valueOf));
        mouldMethodList.stream().forEach(mouldMethod -> {
            List<MonthPlanManufacturingRequirementVo> mouldMethodRequirementList = mouldMethodGroupMap.get(mouldMethod);
            if (CollectionUtils.isEmpty(mouldMethodRequirementList)) {
                return;
            }
            //一次法，不需要区分胎体层级，因成型产能都能兼容
            singleStageTireAllocation(mouldMethod, mouldMethodRequirementList, allocationResultList, moldingMachineInfoMap, moldingMachineInfoList, assignedMachineList, assignedMachineSet, param);
            //二次法，需要区分胎体层级，因成型产能分单层和多层
            twoStageTireAllocation(mouldMethod, mouldMethodRequirementList, allocationResultList, moldingMachineInfoMap, moldingMachineInfoList, assignedMachineList, assignedMachineSet, param);
        });
        return allocationResultList;
    }

    /**
     * 先从所有成型产能中没有分配的产能则表示可参与第二轮分配
     * 已经在第一轮分配中还有剩余的成型产能也可以参与第二轮分配
     *
     * @param assignedMachineList   已经分配的成型产能
     * @param moldingMachineInfoMap 所有成型产能
     * @param param                 寸口分配参数对象
     * @return
     */
    private List<SizeCapacityAllocationDto> getRemainingCapacityList(List<SizeCapacityAllocationDto> assignedMachineList, Map<String, BaseMoldingMachineInfoVo> moldingMachineInfoMap, SizeCapacityParamVo param) {
        List<BaseMoldingMachineInfoVo> remainingCapacityMachineList = new ArrayList<>();
        moldingMachineInfoMap.entrySet().forEach(moldingMachineInfoEntry -> {
            BaseMoldingMachineInfoVo moldingMachineInfo = moldingMachineInfoEntry.getValue();
            if (moldingMachineInfo.getLeftOverCapacityDays() > BigDecimal.ZERO.intValue()) {
                remainingCapacityMachineList.add(moldingMachineInfo);
            }
        });
        if (CollectionUtils.isEmpty(remainingCapacityMachineList)) {
            return Collections.emptyList();
        }
        Map<String, List<SizeCapacityAllocationDto>> assignedMachineGroupMap = assignedMachineList.stream().collect(Collectors.groupingBy(SizeCapacityAllocationDto::getMoldingMachineCode));
        List<SizeCapacityAllocationDto> remainingCapacityList = new ArrayList<>();
        remainingCapacityMachineList.stream().forEach(remainingCapacityMachine -> {
            String moldingMachineCode = remainingCapacityMachine.getMoldingMachineCode();
            List<SizeCapacityAllocationDto> assignedList = assignedMachineGroupMap.get(moldingMachineCode);
            if (CollectionUtils.isEmpty(assignedList)) {
                //没有分配过
                remainingCapacityList.add(buildRemainingAllocation(remainingCapacityMachine));
                return;
            }
            remainingCapacityList.add(assignedList.get(0));
        });
        return remainingCapacityList;
    }

    /**
     * 还有需求，还有剩余产能的第二次分配
     *
     * @param needAllocationList    待分配的需求
     * @param remainingList         剩余产能的机台
     * @param moldingMachineInfoMap 成型机信息集合
     * @param param                 产能分配控制参数
     * @return
     */
    private void secondAllocation(List<SizeCapacityAllocationResultDto> needAllocationList, List<SizeCapacityAllocationDto> remainingList, Map<String, BaseMoldingMachineInfoVo> moldingMachineInfoMap, SizeCapacityParamVo param) {
        Map<String, SizeCapacityAllocationResultDto> needAllocationMap = needAllocationList.stream().collect(Collectors.toMap(SizeCapacityAllocationResultDto::getSizeCapacityGroupKey, Function.identity()));
        //提取寸口
        List<BigDecimal> proSizeList = new ArrayList<>(needAllocationList.stream().map(SizeCapacityAllocationResultDto::getProSize).collect(Collectors.toSet()));
        //提取成型法
        List<String> mouldMethodList = new ArrayList<>(needAllocationList.stream().map(SizeCapacityAllocationResultDto::getMouldMethod).collect(Collectors.toSet()));
        //提取层级
        List<Integer> tireFabricNumberList = new ArrayList<>(needAllocationList.stream().map(SizeCapacityAllocationResultDto::getTireFabricNumber).collect(Collectors.toSet()));
        //按寸口由大到小、一次法到二次法、层级高到底
        proSizeList.sort(Comparator.comparing(BigDecimal::intValue, Comparator.reverseOrder()));
        mouldMethodList.sort(Comparator.comparing(String::valueOf, Comparator.reverseOrder()));
        tireFabricNumberList.sort(Comparator.comparing(Integer::intValue, Comparator.reverseOrder()));
        //已分配集合--只切换一次寸口
        Set<String> assignedMachineSet = new HashSet<>();
        String groupKey = "%s|*|%s|*|%s";
        mouldMethodList.stream().forEach(mouldMethod -> {
            tireFabricNumberList.stream().forEach(tireFabricNumber -> {
                proSizeList.stream().forEach(proSize -> {
                    String sizeCapacityGroupKey = String.format(groupKey, proSize, mouldMethod, tireFabricNumber);
                    SizeCapacityAllocationResultDto needAllocation = needAllocationMap.get(sizeCapacityGroupKey);
                    if (null == needAllocation) {
                        return;
                    }
                    Integer allocationTireFabricNumber = tireFabricNumber;
                    if (FormingMethodTypeEnum.SINGLE_STAGE_TIRE.getMethodValue().equals(mouldMethod)) {
                        allocationTireFabricNumber = FactoryConstant.MULTILAYER_TIRE_FABRIC;
                    }
                    List<SizeCapacityAllocationDto> enableAllocationList = getEnableRemainingList(proSize, mouldMethod, allocationTireFabricNumber, remainingList, assignedMachineSet, moldingMachineInfoMap);
                    if (CollectionUtils.isEmpty(enableAllocationList)) {
                        return;
                    }
                    allocationMoldingMachine(assignedMachineSet, needAllocation, enableAllocationList, moldingMachineInfoMap, param);
                });
            });
        });
    }

    /**
     * 构建寸口产能分配配置信息
     *
     * @param allocationResultList  分配结果集合
     * @param monthMaxDays          最大生产天数
     * @param moldingMachineInfoMap 成型产能信息
     * @return
     */
    private List<SizeCapacityConfiguration> buildConfiguration(List<SizeCapacityAllocationResultDto> allocationResultList, Integer monthMaxDays, Map<String, BaseMoldingMachineInfoVo> moldingMachineInfoMap) {
        if (CollectionUtils.isEmpty(allocationResultList)) {
            return Collections.emptyList();
        }
        List<SizeCapacityConfiguration> configurationList = new ArrayList<>();
        allocationResultList.stream().forEach(allocationResult -> {
            List<SizeCapacityConfiguration> configurationResult = buildConfiguration(allocationResult, monthMaxDays, moldingMachineInfoMap);
            if (CollectionUtils.isEmpty(configurationResult)) {
                return;
            }
            configurationList.addAll(configurationResult);
        });
        return configurationList;
    }

    /**
     * 一次法分配成型产能
     * 因一次法成型产能都是多层胎体布可直接兼容单层
     * 故而无需区分单层、多层胎体布层级
     *
     * @param mouldMethod                成型法
     * @param mouldMethodRequirementList 一次法分配需求列表
     * @param allocationResultList       存储分配结果
     * @param moldingMachineInfoMap      成型产能Map集合
     * @param moldingMachineInfoList     成型产能列表
     * @param assignedMachineList        已分配成型产能列表
     * @param assignedMachineSet         已分配成型产能集合
     * @param param                      寸口产能分配参数
     */
    private void singleStageTireAllocation(String mouldMethod, List<MonthPlanManufacturingRequirementVo> mouldMethodRequirementList, List<SizeCapacityAllocationResultDto> allocationResultList, Map<String, BaseMoldingMachineInfoVo> moldingMachineInfoMap, List<BaseMoldingMachineInfoVo> moldingMachineInfoList, List<SizeCapacityAllocationDto> assignedMachineList, Set<String> assignedMachineSet, SizeCapacityParamVo param) {
        if (!FormingMethodTypeEnum.SINGLE_STAGE_TIRE.getMethodValue().equals(mouldMethod)) {
            return;
        }
        Map<BigDecimal, List<MonthPlanManufacturingRequirementVo>> proSizeRequirementGroup = mouldMethodRequirementList.stream().collect(Collectors.groupingBy(MonthPlanManufacturingRequirementVo::getProSize));
        List<BigDecimal> proSizeList = new ArrayList<>(proSizeRequirementGroup.keySet());
        //寸口由大到小
        proSizeList.sort(Comparator.comparing(BigDecimal::intValue, Comparator.reverseOrder()));
        proSizeList.stream().forEach(proSize -> {
            List<MonthPlanManufacturingRequirementVo> requirementList = proSizeRequirementGroup.get(proSize);
            //待分配的成型产能
            List<BaseMoldingMachineInfoVo> enableAllocationList = getEnableAllocationMoldingMachine(proSize, mouldMethod, FactoryConstant.MULTILAYER_TIRE_FABRIC, moldingMachineInfoList, assignedMachineSet);
            SizeCapacityAllocationResultDto allocationResult = allocationMoldingMachine(proSize, mouldMethod, FactoryConstant.MULTILAYER_TIRE_FABRIC, assignedMachineList, enableAllocationList, requirementList, param);
            allocationResultList.add(allocationResult);
            List<SizeCapacityAllocationDto> allocationList = allocationResult.getAllocationList();
            if (CollectionUtils.isEmpty(allocationList)) {
                return;
            }
            assignedMachineList.addAll(allocationList);
            Set<String> allocationMachineSet = allocationList.stream().map(SizeCapacityAllocationDto::getMoldingMachineCode).collect(Collectors.toSet());
            //分配完成，则加入已分配
            allocationMachineSet.stream().forEach(machineCode -> {
                BaseMoldingMachineInfoVo moldingMachineInfo = moldingMachineInfoMap.get(machineCode);
                if (moldingMachineInfo.getLeftOverCapacityDays() <= BigDecimal.ZERO.intValue()) {
                    assignedMachineSet.add(machineCode);
                }
            });
        });
    }

    /**
     * 二次法分配成型产能
     * 因二次法成型产能有单层、多层胎体布区分，多层可兼容单层
     * 故而需要区分单层、多层胎体布层级
     * 先多层，再单层
     *
     * @param mouldMethod                成型法
     * @param mouldMethodRequirementList 二次法分配需求列表
     * @param allocationResultList       存储分配结果
     * @param moldingMachineInfoMap      成型产能Map集合
     * @param moldingMachineInfoList     成型产能列表
     * @param assignedMachineList        已分配成型产能列表
     * @param assignedMachineSet         已分配成型产能集合
     * @param param                      寸口产能分配参数
     */
    private void twoStageTireAllocation(String mouldMethod, List<MonthPlanManufacturingRequirementVo> mouldMethodRequirementList, List<SizeCapacityAllocationResultDto> allocationResultList, Map<String, BaseMoldingMachineInfoVo> moldingMachineInfoMap, List<BaseMoldingMachineInfoVo> moldingMachineInfoList, List<SizeCapacityAllocationDto> assignedMachineList, Set<String> assignedMachineSet, SizeCapacityParamVo param) {
        if (!FormingMethodTypeEnum.TWO_STAGE_TIRE.getMethodValue().equals(mouldMethod)) {
            return;
        }
        //按寸口+成型法+胎体布层级汇总需求
        Map<String, List<MonthPlanManufacturingRequirementVo>> requirementGroupMap = mouldMethodRequirementList.stream().collect(Collectors.groupingBy(MonthPlanManufacturingRequirementVo::getSizeCapacityGroupKey));
        //提取寸口
        List<BigDecimal> proSizeList = new ArrayList<>(mouldMethodRequirementList.stream().map(MonthPlanManufacturingRequirementVo::getProSize).collect(Collectors.toSet()));
        //提取层级
        List<Integer> tireFabricNumberList = new ArrayList<>(mouldMethodRequirementList.stream().map(MonthPlanManufacturingRequirementVo::getTireFabricNumber).collect(Collectors.toSet()));
        //按寸口由大到小、胎体布层级高到底 按照需求分配成型产能
        proSizeList.sort(Comparator.comparing(BigDecimal::intValue, Comparator.reverseOrder()));
        tireFabricNumberList.sort(Comparator.comparing(Integer::intValue, Comparator.reverseOrder()));
        String groupKey = "%s|*|%s|*|%s";
        tireFabricNumberList.stream().forEach(tireFabricNumber -> {
            proSizeList.stream().forEach(proSize -> {
                String sizeCapacityGroupKey = String.format(groupKey, proSize, mouldMethod, tireFabricNumber);
                //寸口+成型法+胎体布层级需求
                List<MonthPlanManufacturingRequirementVo> requirementList = requirementGroupMap.get(sizeCapacityGroupKey);
                if (CollectionUtils.isEmpty(requirementList)) {
                    return;
                }
                //待分配的成型产能
                List<BaseMoldingMachineInfoVo> enableAllocationList = getEnableAllocationMoldingMachine(proSize, mouldMethod, tireFabricNumber, moldingMachineInfoList, assignedMachineSet);
                SizeCapacityAllocationResultDto allocationResult = allocationMoldingMachine(proSize, mouldMethod, tireFabricNumber, assignedMachineList, enableAllocationList, requirementList, param);
                allocationResultList.add(allocationResult);
                List<SizeCapacityAllocationDto> allocationList = allocationResult.getAllocationList();
                if (CollectionUtils.isEmpty(allocationList)) {
                    return;
                }
                assignedMachineList.addAll(allocationList);
                Set<String> allocationMachineSet = allocationList.stream().map(SizeCapacityAllocationDto::getMoldingMachineCode).collect(Collectors.toSet());
                //分配完成，则加入已分配
                allocationMachineSet.stream().forEach(machineCode -> {
                    BaseMoldingMachineInfoVo moldingMachineInfo = moldingMachineInfoMap.get(machineCode);
                    if (moldingMachineInfo.getLeftOverCapacityDays() <= BigDecimal.ZERO.intValue()) {
                        assignedMachineSet.add(machineCode);
                    }
                });
            });
        });
    }

    /**
     * 获取可分配的成型产能列表--第一轮分配
     * 1、已分配的产能不再分配
     * 2、先按成型法匹配过滤
     * 3、再按胎体层级，多层可生产单层
     * 4、在按寸口匹配
     * 4.1、优先使用在产寸口匹配，在产寸口匹配不到
     * 4.2、在按寸口班产匹配
     *
     * @param proSize                寸口
     * @param mouldMethod            成形法
     * @param tireFabricNumber       胎体布层级
     * @param moldingMachineInfoList 所有成型机列表
     * @param assignedMachineSet     已分配的成型机
     * @return
     */
    private List<BaseMoldingMachineInfoVo> getEnableAllocationMoldingMachine(BigDecimal proSize, String mouldMethod, Integer tireFabricNumber, List<BaseMoldingMachineInfoVo> moldingMachineInfoList, Set<String> assignedMachineSet) {
        if (CollectionUtils.isEmpty(moldingMachineInfoList)) {
            return Collections.emptyList();
        }
        List<BaseMoldingMachineInfoVo> noAllocationList = new ArrayList<>();
        moldingMachineInfoList.stream().forEach(moldingMachine -> {
            if (assignedMachineSet.contains(moldingMachine.getMoldingMachineCode())) {
                return;
            }
            noAllocationList.add(moldingMachine);
        });
        if (CollectionUtils.isEmpty(noAllocationList)) {
            return Collections.emptyList();
        }
        List<BaseMoldingMachineInfoVo> mouldMethodList = noAllocationList.stream().filter(moldingMachine -> mouldMethod.equals(moldingMachine.getMouldMethod())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(mouldMethodList)) {
            return Collections.emptyList();
        }
        //多层级，则只能是多层级的
        List<BaseMoldingMachineInfoVo> tireFabricList;
        if (tireFabricNumber > BigDecimal.ONE.intValue()) {
            tireFabricList = mouldMethodList.stream().filter(moldingMachine -> tireFabricNumber.equals(moldingMachine.getCarcassClothType())).collect(Collectors.toList());
        } else {
            tireFabricList = mouldMethodList;
        }
        if (CollectionUtils.isEmpty(tireFabricList)) {
            return Collections.emptyList();
        }
        //寸口匹配
        List<BaseMoldingMachineInfoVo> enableAllocationList = new ArrayList<>();
        tireFabricList.stream().forEach(tireFabricMachine -> {
            if (!tireFabricMachine.getProSizeQuotaQtyMap().containsKey(proSize)) {
                return;
            }
            tireFabricMachine.setProSizeQuotaQty(tireFabricMachine.getProSizeQuotaQtyMap().get(proSize));
            enableAllocationList.add(tireFabricMachine);
        });
        return enableAllocationList;
    }

    /**
     * 第一轮分配：
     * 根据需求，分配对应成型产能
     * 产能分配优先使用在产产能
     *
     * @param proSize                寸口
     * @param mouldMethod            成型法
     * @param tireFabricNumber       胎体层级
     * @param moldingMachineInfoList 可分配机台列表
     * @param requirementList        需求信息
     * @param param                  参数信息
     */
    private SizeCapacityAllocationResultDto allocationMoldingMachine(BigDecimal proSize, String mouldMethod, Integer tireFabricNumber, List<SizeCapacityAllocationDto> assignedMachineList, List<BaseMoldingMachineInfoVo> moldingMachineInfoList, List<MonthPlanManufacturingRequirementVo> requirementList, SizeCapacityParamVo param) {
        //总需求量
        Long sumRequirementQty = requirementList.stream().mapToLong(MonthPlanManufacturingRequirementVo::getProductionQty).sum();
        List<MonthPlanManufacturingRequirementVo> netRequirementList = requirementList.stream().filter(requirement -> YesOrNoEnum.NO.getValue().equals(requirement.getIsStockUp())).collect(Collectors.toList());
        Long netDemandQty = BigDecimal.ZERO.longValue();
        if (!CollectionUtils.isEmpty(netRequirementList)) {
            netDemandQty = netRequirementList.stream().mapToLong(MonthPlanManufacturingRequirementVo::getProductionQty).sum();
        }
        List<MonthPlanManufacturingRequirementVo> stockUpRequirementList = requirementList.stream().filter(requirement -> YesOrNoEnum.YES.getValue().equals(requirement.getIsStockUp())).collect(Collectors.toList());
        Long stockUpDemandQty = BigDecimal.ZERO.longValue();
        if (!CollectionUtils.isEmpty(stockUpRequirementList)) {
            stockUpDemandQty = stockUpRequirementList.stream().mapToLong(MonthPlanManufacturingRequirementVo::getProductionQty).sum();
        }
        SizeCapacityAllocationDto base = new SizeCapacityAllocationDto(proSize, mouldMethod, tireFabricNumber, sumRequirementQty, netDemandQty, stockUpDemandQty);
        SizeCapacityAllocationResultDto result = BeanCopyUtils.copyBean(base, SizeCapacityAllocationResultDto.class);
        List<SizeCapacityAllocationDto> allocationList = new ArrayList<>();
        if (CollectionUtils.isEmpty(moldingMachineInfoList)) {
            result.setAllocationList(allocationList);
            result.setLeftOverQty(sumRequirementQty);
            return result;
        }
        //在产寸口优先
        List<BaseMoldingMachineInfoVo> currentProSizeList = moldingMachineInfoList.stream().filter(machineInfo -> proSize.equals(machineInfo.getCurrentProSize())).collect(Collectors.toList());
        sumRequirementQty = allocationMoldingMachine(currentProSizeList, sumRequirementQty, assignedMachineList, allocationList, base, param);
        if (sumRequirementQty <= BigDecimal.ZERO.longValue()) {
            result.setAllocationList(allocationList);
            result.setLeftOverQty(sumRequirementQty);
            return result;
        }
        //非在产寸口
        List<BaseMoldingMachineInfoVo> noCurrentProSizeList = moldingMachineInfoList.stream().filter(machineInfo -> !proSize.equals(machineInfo.getCurrentProSize())).collect(Collectors.toList());
        sumRequirementQty = allocationMoldingMachine(noCurrentProSizeList, sumRequirementQty, assignedMachineList, allocationList, base, param);
        result.setAllocationList(allocationList);
        result.setLeftOverQty(sumRequirementQty);
        return result;
    }

    /**
     * 第二轮分配：
     * 挑选可用机台
     *
     * @param proSize
     * @param mouldMethod
     * @param tireFabricNumber
     * @param remainingList         有剩余产能的机台
     * @param assignedMachineSet    已经二次分配的机台集合
     * @param moldingMachineInfoMap 成型机台信息
     * @return
     */
    private List<SizeCapacityAllocationDto> getEnableRemainingList(BigDecimal proSize, String mouldMethod, Integer tireFabricNumber, List<SizeCapacityAllocationDto> remainingList, Set<String> assignedMachineSet, Map<String, BaseMoldingMachineInfoVo> moldingMachineInfoMap) {
        if (CollectionUtils.isEmpty(remainingList)) {
            return Collections.emptyList();
        }
        List<SizeCapacityAllocationDto> noAllocationList = new ArrayList<>();
        remainingList.stream().forEach(moldingMachine -> {
            if (assignedMachineSet.contains(moldingMachine.getMoldingMachineCode())) {
                return;
            }
            noAllocationList.add(moldingMachine);
        });
        if (CollectionUtils.isEmpty(noAllocationList)) {
            return Collections.emptyList();
        }
        List<SizeCapacityAllocationDto> mouldMethodList = noAllocationList.stream().filter(moldingMachine -> mouldMethod.equals(moldingMachine.getMouldMethod())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(mouldMethodList)) {
            return Collections.emptyList();
        }
        //多层级，则只能是多层级的
        List<SizeCapacityAllocationDto> tireFabricList;
        if (tireFabricNumber > BigDecimal.ONE.intValue()) {
            tireFabricList = mouldMethodList.stream().filter(moldingMachine -> tireFabricNumber.equals(moldingMachine.getTireFabricNumber())).collect(Collectors.toList());
        } else {
            tireFabricList = mouldMethodList;
        }
        if (CollectionUtils.isEmpty(tireFabricList)) {
            return Collections.emptyList();
        }
        //寸口匹配
        List<SizeCapacityAllocationDto> enableList = new ArrayList<>();
        tireFabricList.stream().forEach(tireFabricMachine -> {
            BaseMoldingMachineInfoVo baseMoldingMachineInfo = moldingMachineInfoMap.get(tireFabricMachine.getMoldingMachineCode());
            //有班产
            if (baseMoldingMachineInfo.getProSizeQuotaQtyMap().containsKey(proSize)) {
                enableList.add(tireFabricMachine);
                return;
            }
        });
        return enableList;
    }

    /**
     * 第二轮分配：
     * 根据需求，分配对应成型产能
     * 剩余产能只分配一次
     *
     * @param assignedMachineSet    已分配产能集合
     * @param needAllocation        待分配的需求
     * @param enableAllocationList  可分配机台列表
     * @param moldingMachineInfoMap 全机台信息
     * @param param                 参数信息
     */
    private void allocationMoldingMachine(Set<String> assignedMachineSet, SizeCapacityAllocationResultDto needAllocation, List<SizeCapacityAllocationDto> enableAllocationList, Map<String, BaseMoldingMachineInfoVo> moldingMachineInfoMap, SizeCapacityParamVo param) {
        //还需分配量
        Long sumRequirementQty = needAllocation.getLeftOverQty();
        List<SizeCapacityAllocationDto> secondAllocationList = new ArrayList<>();
        Integer monthMaxDays = param.getMonthMaxDays();
        enableAllocationList.stream().forEach(enableAllocationMachine -> {
            String machineCode = enableAllocationMachine.getMoldingMachineCode();
            BaseMoldingMachineInfoVo machineInfo = moldingMachineInfoMap.get(machineCode);
            Long dayMaxCapacityQty = machineInfo.getProSizeQuotaQtyMap().get(needAllocation.getProSize());
            Long realDayCapacityQty = calculateDayCapacity(machineInfo.getMouldMethod(), machineInfo.getCurrentEmbryoCodeNumber(), dayMaxCapacityQty, param);
            enableAllocationMachine.setNextProSizeDayCapacity(realDayCapacityQty);
            Integer leftOverDays = monthMaxDays - enableAllocationMachine.getAllocationDay();
            enableAllocationMachine.setLeftOverDays(leftOverDays);
            Long leftOverCapacityQty = realDayCapacityQty * leftOverDays;
            enableAllocationMachine.setLeftOverCapacityQty(leftOverCapacityQty);
        });
        enableAllocationList.sort(Comparator.comparing(SizeCapacityAllocationDto::getLeftOverCapacityQty, Comparator.reverseOrder()));
        for (SizeCapacityAllocationDto moldingMachine : enableAllocationList) {
            if (sumRequirementQty <= BigDecimal.ZERO.longValue()) {
                break;
            }
            if (null != moldingMachine.getProSize()) {
                //补充下一寸口
                moldingMachine.setNextProSize(needAllocation.getProSize());
                moldingMachine.setNextGroupKey(buildNextGroupKey(needAllocation, moldingMachine));
                moldingMachine.setOneselfKey(buildOneselfKey(moldingMachine));
            } else {
                //新分配空产能
                moldingMachine.setProSize(needAllocation.getProSize());
            }
            assignedMachineSet.add(moldingMachine.getMoldingMachineCode());
            //剩余整个产能分配
            Long dayCapacityQty = moldingMachine.getNextProSizeDayCapacity();
            Long realAllocationQty = dayCapacityQty * moldingMachine.getLeftOverDays();
            SizeCapacityAllocationDto secondAllocation = buildSecondAllocation(moldingMachine, needAllocation, moldingMachine.getLeftOverDays());
            if (null != moldingMachine.getProSize()) {
                secondAllocation.setSuperGroupKey(buildSuperGroupKey(moldingMachine));
                secondAllocation.setOneselfKey(buildOneselfKey(secondAllocation));
            }
            secondAllocationList.add(secondAllocation);
            sumRequirementQty = sumRequirementQty - realAllocationQty;
        }
        if (!CollectionUtils.isEmpty(secondAllocationList)) {
            List<SizeCapacityAllocationDto> allocationList = needAllocation.getAllocationList();
            if (null == allocationList) {
                allocationList = new ArrayList<>();
                needAllocation.setAllocationList(allocationList);
            }
            allocationList.addAll(secondAllocationList);
        }
    }

    /**
     * 根据可用的分配产能进行产能分配
     *
     * @param enableMoldingMachineList 可分配的成型产能集合
     * @param sumRequirementQty        需要分配的需求量
     * @param assignedMachineList      已分配信息集合
     * @param allocationList           存储成型产能分配结果集合
     * @param base                     需求分配信息对象
     * @param param                    寸口产能分配参数对象
     * @return
     */
    private Long allocationMoldingMachine(List<BaseMoldingMachineInfoVo> enableMoldingMachineList, Long sumRequirementQty, List<SizeCapacityAllocationDto> assignedMachineList, List<SizeCapacityAllocationDto> allocationList, SizeCapacityAllocationDto base, SizeCapacityParamVo param) {
        if (sumRequirementQty <= BigDecimal.ZERO.longValue() || CollectionUtils.isEmpty(enableMoldingMachineList)) {
            return sumRequirementQty;
        }
        enableMoldingMachineList.sort(Comparator.comparing(BaseMoldingMachineInfoVo::getLeftOverCapacityDays).thenComparing(BaseMoldingMachineInfoVo::getProSizeQuotaQty, Comparator.reverseOrder()));
        for (BaseMoldingMachineInfoVo moldingMachine : enableMoldingMachineList) {
            if (sumRequirementQty <= BigDecimal.ZERO.longValue()) {
                break;
            }
            Long dayCapacityQty = calculateDayCapacity(moldingMachine, param);
            Integer days = BigDecimal.valueOf(sumRequirementQty).divide(BigDecimal.valueOf(dayCapacityQty), 0, RoundingMode.UP).intValue();
            SizeCapacityAllocationDto before = null;
            if (CollectionUtils.isEmpty(moldingMachine.getProSizeList())) {
                //首次分配
                if (days >= param.getMonthMaxDays()) {
                    days = param.getMonthMaxDays();
                }
                moldingMachine.getProSizeList().add(base.getProSize());
            } else {
                //一个成型产能不可能分配超过2次，故而只需取第一个即可
                List<SizeCapacityAllocationDto> beforeList = assignedMachineList.stream().filter(beforeAssignedMachine -> moldingMachine.getMoldingMachineCode().equals(beforeAssignedMachine.getMoldingMachineCode())).collect(Collectors.toList());
                before = beforeList.get(0);
                if (null != before) {
                    before.setNextProSize(base.getProSize());
                    before.setNextGroupKey(buildNextGroupKey(base, moldingMachine));
                    before.setOneselfKey(buildOneselfKey(before));
                }
                //二次分配，只换一次寸口，故而剩余时间直接给
                days = moldingMachine.getLeftOverCapacityDays();
                moldingMachine.getProSizeList().add(base.getProSize());
            }
            moldingMachine.setLeftOverCapacityDays(moldingMachine.getLeftOverCapacityDays() - days);
            SizeCapacityAllocationDto assignedMachine = buildAllocation(base, dayCapacityQty, moldingMachine, days);
            if (!CollectionUtils.isEmpty(moldingMachine.getProSizeList()) && null != before) {
                assignedMachine.setSuperGroupKey(buildSuperGroupKey(before, moldingMachine));
                assignedMachine.setOneselfKey(buildOneselfKey(assignedMachine));
            }
            allocationList.add(assignedMachine);
            Long realAllocationQty = dayCapacityQty * days;
            sumRequirementQty = sumRequirementQty - realAllocationQty;
        }
        return sumRequirementQty;
    }

    /**
     * 计算成型的天产能信息
     *
     * @param moldingMachine 成型信息
     * @param param          参数
     * @return
     */
    private Long calculateDayCapacity(BaseMoldingMachineInfoVo moldingMachine, SizeCapacityParamVo param) {
        //得到天产能
        Long dayMaxCapacityQty = moldingMachine.getProSizeQuotaQty();
        Integer currentEmbryoCodeNumber = moldingMachine.getCurrentEmbryoCodeNumber();
        return calculateDayCapacity(moldingMachine.getMouldMethod(), currentEmbryoCodeNumber, dayMaxCapacityQty, param);
    }

    /**
     * 计算成型的天产能信息
     *
     * @param mouldMethod             成型法
     * @param currentEmbryoCodeNumber 当前规格数
     * @param dayMaxCapacityQty       寸口的天产能
     * @param param                   参数
     * @return
     */
    private Long calculateDayCapacity(String mouldMethod, Integer currentEmbryoCodeNumber, Long dayMaxCapacityQty, SizeCapacityParamVo param) {
        //减去切换扣减
        Integer changeCount = param.getDefaultCount();
        if (null != currentEmbryoCodeNumber) {
            changeCount = currentEmbryoCodeNumber;
        }
        if (null != param.getAdditionalCount()) {
            changeCount = changeCount + param.getAdditionalCount();
        }
        Long needSubtractQty;
        if (FormingMethodTypeEnum.SINGLE_STAGE_TIRE.getMethodValue().equals(mouldMethod)) {
            needSubtractQty = Long.valueOf(changeCount * param.getOneMouldMethodSubtractQty());
        } else {
            needSubtractQty = Long.valueOf(changeCount * param.getTwoMouldMethodSubtractQty());
        }
        return dayMaxCapacityQty - needSubtractQty;
    }

    /**
     * 根据产能分配结果，构建产能分配配置信息
     *
     * @param allocationResult 分配结果对象
     * @param monthMaxDays     月最大生产天数
     * @return
     */
    private List<SizeCapacityConfiguration> buildConfiguration(SizeCapacityAllocationResultDto allocationResult, Integer monthMaxDays, Map<String, BaseMoldingMachineInfoVo> moldingMachineInfoMap) {
        List<SizeCapacityAllocationDto> allocationList = allocationResult.getAllocationList();
        if (CollectionUtils.isEmpty(allocationList)) {
            return Collections.emptyList();
        }
        List<SizeCapacityConfiguration> configurationList = new ArrayList<>();
        List<SizeCapacityAllocationDto> aloneList = allocationList.stream().filter(configuration -> configuration.isAlone()).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(aloneList)) {
            aloneList.stream().forEach(aloneConfiguration -> {
                SizeCapacityConfiguration configuration = buildConfiguration(allocationResult, aloneConfiguration);
                if (StringUtils.isNotBlank(aloneConfiguration.getNextGroupKey())) {
                    configuration.setNextGroupKey(aloneConfiguration.getNextGroupKey());
                }
                if (null != aloneConfiguration.getNextProSize()) {
                    configuration.setNextProSize(aloneConfiguration.getNextProSize());
                }
                if (StringUtils.isNotBlank(aloneConfiguration.getSuperGroupKey())) {
                    configuration.setSuperGroupKey(aloneConfiguration.getSuperGroupKey());
                }
                if (StringUtils.isNotBlank(aloneConfiguration.getOneselfKey())) {
                    configuration.setOneselfKey(aloneConfiguration.getOneselfKey());
                }
                Integer remainingDays = aloneConfiguration.getAllocationDay();
                configuration.setRemainingDays(remainingDays);
                BigDecimal remainingNumber = BigDecimal.valueOf(aloneConfiguration.getAllocationDay()).divide(BigDecimal.valueOf(monthMaxDays), 1, RoundingMode.HALF_UP);
                configuration.setWholeMachineNumber(BigDecimal.ZERO.intValue());
                configuration.setMachineNumber(remainingNumber);
                configurationList.add(configuration);
            });
        }
        List<SizeCapacityAllocationDto> mergeList = allocationList.stream().filter(configuration -> !configuration.isAlone()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(mergeList)) {
            return configurationList;
        }
        Map<String, List<SizeCapacityAllocationDto>> typeGroup = mergeList.stream().collect(Collectors.groupingBy(SizeCapacityAllocationDto::getGroupKey));
        typeGroup.entrySet().forEach(typeEntry -> {
            List<SizeCapacityAllocationDto> typeList = typeEntry.getValue();
            SizeCapacityAllocationDto first = typeList.get(0);
            SizeCapacityConfiguration configuration = buildConfiguration(allocationResult, first);
            Integer wholeMachineNumber = BigDecimal.ZERO.intValue();
            BigDecimal remainingNumber = BigDecimal.ZERO;
            for (SizeCapacityAllocationDto singleAllocation : typeList) {
                Integer remainingDays = singleAllocation.getAllocationDay();
                if (monthMaxDays.equals(remainingDays)) {
                    wholeMachineNumber = wholeMachineNumber + BigDecimal.ONE.intValue();
                } else {
                    configuration.setRemainingDays(remainingDays);
                    remainingNumber = BigDecimal.valueOf(singleAllocation.getAllocationDay()).divide(BigDecimal.valueOf(monthMaxDays), 1, RoundingMode.HALF_UP);
                }
            }
            configuration.setWholeMachineNumber(wholeMachineNumber);
            configuration.setMachineNumber(remainingNumber.add(BigDecimal.valueOf(wholeMachineNumber)));
            configurationList.add(configuration);
        });
        return configurationList;
    }

    /**
     * 设置配置
     *
     * @param singleAllocation      当前配置
     * @param moldingMachineInfo    成型产能信息
     * @param typeConfigurationList 所有配置
     * @param allocationResult      配置信息
     */
    private SizeCapacityConfiguration getConfiguration(SizeCapacityAllocationDto singleAllocation, BaseMoldingMachineInfoVo moldingMachineInfo, List<SizeCapacityConfiguration> typeConfigurationList, SizeCapacityAllocationResultDto allocationResult) {
        //表示起始，需要另外开启一个
        if (moldingMachineInfo.isNext() && null != singleAllocation.getNextProSize()) {
            SizeCapacityConfiguration configuration = buildConfiguration(allocationResult, singleAllocation);
            typeConfigurationList.add(configuration);
            return configuration;
        }
        //取最后一个
        if (CollectionUtils.isEmpty(typeConfigurationList)) {
            SizeCapacityConfiguration configuration = buildConfiguration(allocationResult, singleAllocation);
            typeConfigurationList.add(configuration);
            return configuration;
        }
        return typeConfigurationList.get(typeConfigurationList.size() - BigDecimal.ONE.intValue());
    }

    /**
     * 构建成型机分配信息
     *
     * @param base           基础信息
     * @param dayCapacityQty 天产能
     * @param moldingMachine 成型机信息
     * @param allocationDays 分配天数
     * @return
     */
    private SizeCapacityAllocationDto buildAllocation(SizeCapacityAllocationDto base, Long dayCapacityQty, BaseMoldingMachineInfoVo moldingMachine, Integer allocationDays) {
        SizeCapacityAllocationDto allocation = BeanCopyUtils.copyBean(base, SizeCapacityAllocationDto.class);
        allocation.setMoldingMachineCode(moldingMachine.getMoldingMachineCode());
        allocation.setMoldingMachineClsType(moldingMachine.getMoldingMachineClsType());
        allocation.setMoldingMachineClsName(moldingMachine.getMoldingMachineClsName());
        allocation.setAllocationDay(allocationDays);
        allocation.setDayCapacity(dayCapacityQty);
        return allocation;
    }

    /**
     * 构建下一级分组key
     * 寸口|*|成型法|*|胎体层级|*|成型产能"
     *
     * @param base           基础信息
     * @param moldingMachine 分配的成型产能
     * @return
     */
    private String buildNextGroupKey(SizeCapacityAllocationDto base, BaseMoldingMachineInfoVo moldingMachine) {
        String groupKey = "%s|*|%s|*|%s|*|%s";
        return String.format(groupKey, base.getProSize(), base.getMouldMethod(), base.getTireFabricNumber(), moldingMachine.getMoldingMachineCode());
    }

    /**
     * 二次分配，构建下一级分组key
     * 寸口|*|成型法|*|胎体层级|*|成型产能"
     *
     * @param needAllocation       下一个配置
     * @param currentConfiguration 当前成型产能
     * @return
     */
    private String buildNextGroupKey(SizeCapacityAllocationResultDto needAllocation, SizeCapacityAllocationDto currentConfiguration) {
        String groupKey = "%s|*|%s|*|%s|*|%s";
        return String.format(groupKey, needAllocation.getProSize(), needAllocation.getMouldMethod(), needAllocation.getTireFabricNumber(), currentConfiguration.getMoldingMachineCode());
    }

    /**
     * 构建父级分组key
     * 寸口|*|成型法|*|胎体层级|*|成型产能"
     *
     * @param superConfiguration 父级配置
     * @param moldingMachine     分配的成型产能
     * @return
     */
    private String buildSuperGroupKey(SizeCapacityAllocationDto superConfiguration, BaseMoldingMachineInfoVo moldingMachine) {
        String groupKey = "%s|*|%s|*|%s|*|%s";
        return String.format(groupKey, superConfiguration.getProSize(), superConfiguration.getMouldMethod(), superConfiguration.getTireFabricNumber(), moldingMachine.getMoldingMachineCode());
    }

    /**
     * 构建父级分组key
     * 寸口|*|成型法|*|胎体层级|*|成型产能"
     *
     * @param superConfiguration 父级配置
     * @return
     */
    private String buildSuperGroupKey(SizeCapacityAllocationDto superConfiguration) {
        String groupKey = "%s|*|%s|*|%s|*|%s";
        return String.format(groupKey, superConfiguration.getProSize(), superConfiguration.getMouldMethod(), superConfiguration.getTireFabricNumber(), superConfiguration.getMoldingMachineCode());
    }

    /**
     * 构建自身 key
     *
     * @param oneselfConfiguration 本身配置
     * @return
     */
    private String buildOneselfKey(SizeCapacityAllocationDto oneselfConfiguration) {
        String groupKey = "%s|*|%s|*|%s|*|%s";
        return String.format(groupKey, oneselfConfiguration.getProSize(), oneselfConfiguration.getMouldMethod(), oneselfConfiguration.getTireFabricNumber(), oneselfConfiguration.getMoldingMachineCode());
    }

    /**
     * 根据成型产能，构建空的分配对象
     *
     * @param moldingMachine
     * @return
     */
    private SizeCapacityAllocationDto buildRemainingAllocation(BaseMoldingMachineInfoVo moldingMachine) {
        SizeCapacityAllocationDto allocation = new SizeCapacityAllocationDto();
        allocation.setMoldingMachineCode(moldingMachine.getMoldingMachineCode());
        allocation.setMoldingMachineClsType(moldingMachine.getMoldingMachineClsType());
        allocation.setMoldingMachineClsName(moldingMachine.getMoldingMachineClsName());
        allocation.setMouldMethod(moldingMachine.getMouldMethod());
        allocation.setTireFabricNumber(moldingMachine.getCarcassClothType());
        allocation.setAllocationDay(BigDecimal.ZERO.intValue());
        return allocation;
    }

    /**
     * 构建成型机分配信息
     *
     * @param moldingMachine 前一个寸口分配信息
     * @param needAllocation 后一寸口分配需求
     * @return
     */
    private SizeCapacityAllocationDto buildSecondAllocation(SizeCapacityAllocationDto moldingMachine, SizeCapacityAllocationResultDto needAllocation, Integer allocationDays) {
        SizeCapacityAllocationDto secondAllocation = new SizeCapacityAllocationDto();
        secondAllocation.setProSize(needAllocation.getProSize());
        secondAllocation.setDemandQty(needAllocation.getDemandQty());
        secondAllocation.setNetDemandQty(needAllocation.getNetDemandQty());
        secondAllocation.setStockUpDemandQty(needAllocation.getStockUpDemandQty());
        secondAllocation.setMoldingMachineCode(moldingMachine.getMoldingMachineCode());
        secondAllocation.setMoldingMachineClsType(moldingMachine.getMoldingMachineClsType());
        secondAllocation.setMoldingMachineClsName(moldingMachine.getMoldingMachineClsName());
        secondAllocation.setMouldMethod(needAllocation.getMouldMethod());
        secondAllocation.setTireFabricNumber(needAllocation.getTireFabricNumber());
        secondAllocation.setAllocationDay(allocationDays);
        secondAllocation.setDayCapacity(moldingMachine.getNextProSizeDayCapacity());
        return secondAllocation;
    }

    /**
     * @param allocationResult
     * @param first
     * @return
     */
    private SizeCapacityConfiguration buildConfiguration(SizeCapacityAllocationResultDto allocationResult, SizeCapacityAllocationDto first) {
        SizeCapacityConfiguration configuration = new SizeCapacityConfiguration();
        configuration.setProSize(allocationResult.getProSize());
        configuration.setCarcassClothType(allocationResult.getTireFabricNumber());
        configuration.setMouldMethod(allocationResult.getMouldMethod());
        //需求信息
        configuration.setEffectiveDemandQty(allocationResult.getDemandQty());
        configuration.setEffectiveNetDemandQty(allocationResult.getNetDemandQty());
        configuration.setEffectiveStockUpDemandQty(allocationResult.getStockUpDemandQty());
        //机型
        configuration.setMoldingMachineClsName(first.getMoldingMachineClsName());
        configuration.setMoldingMachineClsType(first.getMoldingMachineClsType());
        //天产能
        configuration.setDayCapacity(first.getDayCapacity().intValue());
        return configuration;
    }

    /**
     * 根据需求总量，成型机均产能和月份最大生产天数计算所需的机台数
     *
     * @param sumRequirementQty           需求总量
     * @param moldingMachineConfiguration 成型机信息
     * @param monthMaxDays                月份最大可生产天数
     * @param sizeCapacityConfiguration   寸口产能配置
     * @return
     */
    private void setMachineNumber(Long sumRequirementQty, MdmMoldingMachineProNumVo moldingMachineConfiguration, Integer monthMaxDays, SizeCapacityConfigurationVo sizeCapacityConfiguration) {
        sizeCapacityConfiguration.setMaxMachineNumber(moldingMachineConfiguration.getProNum());
        BigDecimal dayCapacityQty = moldingMachineConfiguration.getAverageQuota();
        if (null == dayCapacityQty) {
            sizeCapacityConfiguration.setDayCapacity(BigDecimal.ZERO.intValue());
            sizeCapacityConfiguration.setMachineNumber(BigDecimal.ZERO);
            return;
        }
        Long capacityQty = dayCapacityQty.setScale(0, RoundingMode.DOWN).longValue();
        Long monthCapacityQty = capacityQty * monthMaxDays;
        if (BigDecimal.ZERO.longValue() == monthCapacityQty) {
            sizeCapacityConfiguration.setDayCapacity(BigDecimal.ZERO.intValue());
            sizeCapacityConfiguration.setMachineNumber(BigDecimal.ZERO);
            return;
        }
        BigDecimal machineNumber = BigDecimal.valueOf(sumRequirementQty).divide(BigDecimal.valueOf(monthCapacityQty), 4, RoundingMode.HALF_UP);
        BigDecimal maxNumber = BigDecimal.valueOf(moldingMachineConfiguration.getProNum());
        if (machineNumber.compareTo(maxNumber) > BigDecimal.ZERO.intValue()) {
            machineNumber = maxNumber;
        }
        sizeCapacityConfiguration.setDayCapacity(capacityQty.intValue());
        sizeCapacityConfiguration.setMachineNumber(machineNumber);
    }

    /**
     * 获取成型法对应的总机台数
     *
     * @param methodMap
     * @param methodType
     * @return
     */
    private Integer getFormingMethodNumber(Map<Integer, List<MdmMoldingMachineProNumVo>> methodMap, FormingMethodTypeEnum methodType) {
        if (CollectionUtils.isEmpty(methodMap) || null == methodType) {
            return BigDecimal.ZERO.intValue();
        }
        List<MdmMoldingMachineProNumVo> machineList = methodMap.get(methodType.getFormingMethod());
        if (CollectionUtils.isEmpty(machineList)) {
            return BigDecimal.ZERO.intValue();
        }
        MdmMoldingMachineProNumVo first = machineList.get(0);
        return first.getMachineSumNum();
    }

    /**
     * 寸口产能配置
     *
     * @param requirement
     * @return
     */
    private SizeCapacityConfigurationVo buildSizeCapacityConfiguration(MonthPlanManufacturingRequirementVo requirement) {
        SizeCapacityConfigurationVo configuration = new SizeCapacityConfigurationVo();
        configuration.setProSize(requirement.getProSize());
        configuration.setMouldMethod(requirement.getMouldMethod());
        configuration.setFactoryCode(requirement.getFactoryCode());
        configuration.setYear(requirement.getYear());
        configuration.setMonth(requirement.getMonth());
        configuration.setMonthPlanVersion(requirement.getMonthPlanVersion());
        return configuration;
    }

    /**
     * 等比例分摊机台数
     *
     * @param mouldMethodConfigurationList 成型法的寸口产能配置集合
     * @param maxSum                       成型法最大机台数
     * @param configurationNumber          配置的总的机台数
     */
    private void proportionalSharing(List<SizeCapacityConfigurationVo> mouldMethodConfigurationList, Integer maxSum, BigDecimal configurationNumber) {
        //分摊比例
        BigDecimal proportion = BigDecimal.valueOf(maxSum).divide(configurationNumber, 4, RoundingMode.HALF_UP);
        BigDecimal sumProportion = BigDecimal.ZERO;
        int index = 1;
        int size = mouldMethodConfigurationList.size();
        for (SizeCapacityConfigurationVo configuration : mouldMethodConfigurationList) {
            if (index == size) {
                configuration.setMachineNumber(BigDecimal.valueOf(maxSum).subtract(sumProportion));
                break;
            }
            BigDecimal proSizeConfigurationNumber = configuration.getMachineNumber();
            BigDecimal proportionValue = proSizeConfigurationNumber.multiply(proportion).setScale(2, RoundingMode.HALF_UP);
            sumProportion = sumProportion.add(proportionValue);
            configuration.setMachineNumber(proportionValue);
            index = index + 1;
        }
    }

    /**
     * 进舍位处理，保留一位小数。如果<0.5则处理为0.5，
     * 如果小数为小于0.5，则处理为零。大于0.5则进一位
     *
     * @param mouldMethodConfigurationList
     */
    private void placementProcessing(List<SizeCapacityConfigurationVo> mouldMethodConfigurationList) {
        if (CollectionUtils.isEmpty(mouldMethodConfigurationList)) {
            return;
        }
        BigDecimal halfUp = BigDecimal.ONE.divide((BigDecimal.ONE.add(BigDecimal.ONE)), 1, RoundingMode.HALF_UP);
        BigDecimal sumNumber = BigDecimal.ZERO;
        int skip = BigDecimal.ZERO.intValue();
        //按寸口由大到小排序
        mouldMethodConfigurationList.sort(Comparator.comparing(SizeCapacityConfigurationVo::getProSize, Comparator.reverseOrder()));
        for (SizeCapacityConfigurationVo configuration : mouldMethodConfigurationList) {
            //处理小数
            BigDecimal processingPointNumber = processingPointNumber(configuration);
            if (sumNumber.compareTo(BigDecimal.ZERO) == BigDecimal.ZERO.intValue()) {
                sumNumber = sumNumber.add(processingPointNumber);
                continue;
            }
            BigDecimal machine = sumNumber.stripTrailingZeros();
            //总的为整数
            if (machine.scale() == BigDecimal.ZERO.intValue()) {
                skip = BigDecimal.ZERO.intValue();
                sumNumber = sumNumber.add(processingPointNumber);
                continue;
            }
            //总的为小数，看本次分配结果是整数还是小数
            BigDecimal processing = processingPointNumber.stripTrailingZeros();
            //表示整数，且没有跳过寸口
            if (processing.scale() == BigDecimal.ZERO.intValue() && skip == BigDecimal.ZERO.intValue()) {
                skip = BigDecimal.ONE.intValue();
                sumNumber = sumNumber.add(processingPointNumber);
                continue;
            }
            //表示整数，且已经跳过一个寸口，则需要减0.5
            if (processing.scale() == BigDecimal.ZERO.intValue() && skip == BigDecimal.ZERO.intValue()) {
                processingPointNumber = processingPointNumber.subtract(halfUp);
                configuration.setMachineNumber(processingPointNumber);
                sumNumber = sumNumber.add(processingPointNumber);
                skip = BigDecimal.ZERO.intValue();
                continue;
            }
            //表示小数
            sumNumber = sumNumber.add(processingPointNumber);
            skip = BigDecimal.ZERO.intValue();
        }
    }

    /**
     * 处理下一个寸口
     *
     * @param mouldMethodConfigurationList
     */
    private void processingNextProSize(List<SizeCapacityConfigurationVo> mouldMethodConfigurationList) {
        if (CollectionUtils.isEmpty(mouldMethodConfigurationList)) {
            return;
        }
        //按寸口由大到小排序
        mouldMethodConfigurationList.sort(Comparator.comparing(SizeCapacityConfigurationVo::getProSize, Comparator.reverseOrder()));
        int size = mouldMethodConfigurationList.size();
        Set<BigDecimal> nextProSizeSet = new HashSet<>();
        for (int index = 0; index <= size - 1; index++) {
            SizeCapacityConfigurationVo configuration = mouldMethodConfigurationList.get(index);
            BigDecimal proSize = configuration.getProSize();
            if (nextProSizeSet.contains(proSize)) {
                continue;
            }
            BigDecimal machineNumber = configuration.getMachineNumber();
            BigDecimal machine = machineNumber.stripTrailingZeros();
            //整数
            if (machine.scale() == BigDecimal.ZERO.intValue()) {
                continue;
            }
            for (int nextIndex = index + 1; nextIndex <= size - 1; nextIndex++) {
                SizeCapacityConfigurationVo nextConfiguration = mouldMethodConfigurationList.get(nextIndex);
                BigDecimal nextProSize = nextConfiguration.getProSize();
                if (nextProSizeSet.contains(nextProSize)) {
                    continue;
                }
                BigDecimal nextMachineNumber = nextConfiguration.getMachineNumber();
                BigDecimal nextMachine = nextMachineNumber.stripTrailingZeros();
                //整数
                if (nextMachine.scale() == BigDecimal.ZERO.intValue()) {
                    continue;
                }
                configuration.setNextProSize(nextProSize);
                nextProSizeSet.add(nextProSize);
                break;
            }
        }
    }

    /**
     * 汇总某个成型法的机台数
     *
     * @param mouldMethodConfigurationList
     * @return
     */
    private BigDecimal getSumMachineNumber(List<SizeCapacityConfigurationVo> mouldMethodConfigurationList) {
        if (CollectionUtils.isEmpty(mouldMethodConfigurationList)) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (SizeCapacityConfigurationVo configuration : mouldMethodConfigurationList) {
            sum = sum.add(configuration.getMachineNumber());
        }
        return sum;
    }

    /**
     * 判断小数位距离 0,0.5,1谁的位置近，取谁
     *
     * @param configuration
     */
    private BigDecimal processingPointNumber(SizeCapacityConfigurationVo configuration) {
        BigDecimal machineNumber = configuration.getMachineNumber();
        BigDecimal machine = machineNumber.stripTrailingZeros();
        //表示整数
        if (machine.scale() == BigDecimal.ZERO.intValue()) {
            return machineNumber;
        }
        //小于0.5，则直接为0.5
        BigDecimal halfUp = BigDecimal.ONE.divide((BigDecimal.ONE.add(BigDecimal.ONE)), 1, RoundingMode.HALF_UP);
        if (machineNumber.compareTo(halfUp) < BigDecimal.ZERO.intValue()) {
            configuration.setMachineNumber(halfUp);
            return halfUp;
        }
        //取得小数部分
        BigDecimal decimalPart = machineNumber.remainder(BigDecimal.ONE);
        Integer intPart = machineNumber.toBigInteger().intValue();
        BigDecimal intPartBigDecimal = BigDecimal.valueOf(intPart);
        //与零的差值
        BigDecimal diffZero = decimalPart.subtract(BigDecimal.ZERO);
        //与0.5的差值
        BigDecimal diffHalfUp;
        if (decimalPart.compareTo(halfUp) > BigDecimal.ZERO.intValue()) {
            diffHalfUp = decimalPart.subtract(halfUp);
        } else {
            diffHalfUp = halfUp.subtract(decimalPart);
        }
        //与1的差值
        BigDecimal oneDiff = BigDecimal.ONE.subtract(decimalPart);
        //接近于零
        if (diffZero.compareTo(diffHalfUp) <= BigDecimal.ZERO.intValue() && diffZero.compareTo(oneDiff) <= BigDecimal.ZERO.intValue()) {
            configuration.setMachineNumber(intPartBigDecimal);
            return intPartBigDecimal;
        }
        //接近于0.5
        if (diffHalfUp.compareTo(diffZero) <= BigDecimal.ZERO.intValue() && diffHalfUp.compareTo(oneDiff) <= BigDecimal.ZERO.intValue()) {
            BigDecimal realMachineNumber = BigDecimal.valueOf(intPart).add(halfUp);
            configuration.setMachineNumber(realMachineNumber);
            return realMachineNumber;
        }
        //接近于1
        BigDecimal realMachineNumber = BigDecimal.valueOf(intPart).add(BigDecimal.ONE);
        configuration.setMachineNumber(realMachineNumber);
        return realMachineNumber;
    }
}
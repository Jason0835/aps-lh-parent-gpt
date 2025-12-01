package com.zlt.aps.factory.utils;

import com.alibaba.fastjson.JSON;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.dto.MouldTableInfoDto;
import com.zlt.aps.factory.domain.dto.ProductionGroupInfoDto;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.enums.ProductionOrientEnum;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.monthplan.api.domain.entity.ProductionGroupResultHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 排产分组工具类
 * <p>
 * 针对排产分组的业务处理
 *
 * @author ZLT
 * @date 20250715
 */
@Slf4j
public class ProductionGroupUtils {

    /**
     * 根据分组值及模台数，构建新空的分组排产对象
     * 初始化排产分组对象
     *
     * @param productionContext    排产上下文
     * @param productionGroupValue 分组值
     * @param mouldNumber          模台数
     * @return
     */
    public static ProductionGroupInfoDto createEmptyProductionGroup(ProductionContext productionContext, String productionGroupValue, Integer mouldNumber) {
        ProductionGroupInfoDto groupInfo = new ProductionGroupInfoDto();
        groupInfo.setProductionGroupValue(productionGroupValue);
        groupInfo.setMouldNumber(mouldNumber);
        groupInfo.setFinishedDay(new HashSet<>());
        groupInfo.setEmptyGroup(true);
        groupInfo.setProductionFinish(false);
        groupInfo.setContinueGroup(false);
        groupInfo.setNeedProductionDateSet(productionContext.getWholeMonthWorkDaySet());
        List<MouldTableInfoDto> mouldTableInfoList = new ArrayList<>();
        for (int index = 0; index < mouldNumber; index++) {
            MouldTableInfoDto mouldTable = createEmptyMouldTable(productionContext, productionGroupValue);
            mouldTableInfoList.add(mouldTable);
        }
        groupInfo.setMouldTableInfoList(mouldTableInfoList);
        return groupInfo;
    }

    /**
     * 根据续作信息，构建续作的排产分组信息
     * 当时分组信息，及本身模台数
     * 和拼模排产情况
     *
     * @param continueProductionGroup 续作排产信息
     * @return
     */
    public static Map<String, ContinueProductionGroupVo> buildContinueProductionGroupInfo(Map<String, List<MouldProductionProductVo>> continueProductionGroup) {
        if (CollectionUtils.isEmpty(continueProductionGroup)) {
            return Collections.emptyMap();
        }
        Map<String, ContinueProductionGroupVo> continueProductionGroupMap = new HashMap<>();
        continueProductionGroup.entrySet().stream().forEach(continueProductionGroupEntry -> {
            String continueProductionGroupValue = continueProductionGroupEntry.getKey();
            ContinueProductionGroupVo continueGroup = new ContinueProductionGroupVo();
            continueGroup.setContinueProductionGroupValue(continueProductionGroupValue);
            List<MouldProductionProductVo> productionInfoList = continueProductionGroupEntry.getValue();
            continueGroup.setContinueProductInfoList(productionInfoList);
            MouldProductionProductVo productionMouldInfo = productionInfoList.get(0);
            continueGroup.setMouldNumber(productionMouldInfo.getMouldNumber());
            if (productionMouldInfo.getMouldQty() < productionMouldInfo.getMouldNumber()) {
                continueGroup.setAssemble(true);
            } else {
                continueGroup.setAssemble(false);
            }
            continueProductionGroupMap.put(continueProductionGroupValue, continueGroup);
        });
        return continueProductionGroupMap;
    }

    /**
     * 根据续作模具信息，得到排产分组信息
     *
     * @param continuePlan      排产计划
     * @param mouldInfo         续作模具
     * @param productionContext 排产上下文
     * @return
     */
    public static ProductionGroupInfoDto buildProductionGroupInfoByContinueMould(MonthPlanManufacturingRequirementVo continuePlan, MouldInfoVO mouldInfo, ProductionContext productionContext) {
        String continueProductionGroupValue = mouldInfo.getContinueProductionGroupValue();
        if (StringUtils.isBlank(continueProductionGroupValue)) {
            return null;
        }
        ContinueProductionGroupVo continueProductionGroup = productionContext.getContinueProductionGroupMap().get(continueProductionGroupValue);
        if (null == continueProductionGroup) {
            return null;
        }
        String productionGroupValue = continueProductionGroup.getProductionGroupValue();
        Map<String, ProductionGroupInfoDto> productionGroupMap = productionContext.getProductionGroupInfoMap();
        //已经构建过排产分组
        if (StringUtils.isNotBlank(productionGroupValue)) {
            ProductionGroupInfoDto buildGroup = productionGroupMap.get(productionGroupValue);
            //记录日志
            String buildContinueProductionGroupFormat = "续作计划排产续作模具找到已构建的续作排产分组：%s 排产方向：%s 是否拼模排产：%s";
            String buildContinueProductionGroupContext = String.format(buildContinueProductionGroupFormat, buildGroup.getProductionGroupValue(), buildGroup.getProductionOrient().getDesc(), buildGroup.isAssemble());
            ProductionLogUtils.addBuildContinueProductionGroupLog(productionContext, continuePlan, buildContinueProductionGroupContext);
            return buildGroup;
        }
        Integer mouldNumber = continueProductionGroup.getMouldNumber();
        List<ProductionGroupInfoDto> productionGroupInfoList = new ArrayList<>(productionGroupMap.values());
        List<ProductionGroupInfoDto> selectedList = productionGroupInfoList.stream().filter(productionGroupInfo -> productionGroupInfo.getEmptyGroup() && mouldNumber.equals(productionGroupInfo.getMouldNumber())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(selectedList)) {
            return null;
        }
        selectedList.sort(Comparator.comparing(ProductionGroupInfoDto::getProductionGroupValue));
        ProductionGroupInfoDto selected = selectedList.get(0);
        selected.setContinueGroup(true);
        selected.setAssemble(continueProductionGroup.isAssemble());
        //续作开始都是正向
        selected.setProductionOrient(ProductionOrientEnum.FORWARD);
        //设置新的排产分组
        continueProductionGroup.setProductionGroupValue(selected.getProductionGroupValue());
        String buildContinueProductionGroupFormat = "续作计划排产续作模具构建排产分组：%s 排产方向：%s 是否拼模排产：%s";
        String buildContinueProductionGroupContext = String.format(buildContinueProductionGroupFormat, selected.getProductionGroupValue(), selected.getProductionOrient().getDesc(), selected.isAssemble());
        ProductionLogUtils.addBuildContinueProductionGroupLog(productionContext, continuePlan, buildContinueProductionGroupContext);
        return selected;
    }

    /**
     * 续作排产--获取选择的模台
     * 1、如果是单模台，则直接选中
     * 2、如果是双模台
     * 2.1、如果都已经有排产信息，
     * 2.1.1、根据最后排产信息，如果排产模具编号匹配，则命中
     * 2.1.2、没有命中，则排产日少的命中
     * 2.2、如果没有排产信息，则表示开始排产分组
     * 2.2.1、都没有排产，则直接命中第一个模台
     * 2.2.2、一个有排产，另外一个没有排产，则命中没有排产的
     *
     * @param productionGroupInfo 排产分组
     * @param mouldCode           模具编码
     * @return
     */
    public static MouldTableInfoDto getSelectedMouldTableInfoByContinue(ProductionGroupInfoDto productionGroupInfo, String mouldCode) {
        if (null == productionGroupInfo || StringUtils.isBlank(mouldCode)) {
            return null;
        }
        //单模台
        List<MouldTableInfoDto> mouldTableInfoList = productionGroupInfo.getMouldTableInfoList();
        if (BigDecimal.ONE.intValue() == productionGroupInfo.getMouldNumber()) {
            return mouldTableInfoList.get(0);
        }
        //双模台
        for (MouldTableInfoDto mouldTableInfo : mouldTableInfoList) {
            MouldDayProductionVo lastProductionInfo = mouldTableInfo.getLastProductionInfo();
            if (null == lastProductionInfo) {
                continue;
            }
            if (mouldCode.equals(lastProductionInfo.getMouldCode())) {
                return mouldTableInfo;
            }
        }
        //按排产日早的选中
        Set<Integer> firstProductionDateSet = mouldTableInfoList.get(0).getProductionDateSet();
        Set<Integer> secondProductionDateSet = mouldTableInfoList.get(1).getProductionDateSet();
        if (CollectionUtils.isEmpty(firstProductionDateSet) && CollectionUtils.isEmpty(secondProductionDateSet)) {
            return mouldTableInfoList.get(0);
        }
        if (CollectionUtils.isEmpty(firstProductionDateSet) && !CollectionUtils.isEmpty(secondProductionDateSet)) {
            return mouldTableInfoList.get(0);
        }
        if (!CollectionUtils.isEmpty(firstProductionDateSet) && CollectionUtils.isEmpty(secondProductionDateSet)) {
            return mouldTableInfoList.get(1);
        }
        Integer firstDays = firstProductionDateSet.size();
        Integer secondDays = firstProductionDateSet.size();
        if (firstDays <= secondDays) {
            return mouldTableInfoList.get(0);
        }
        return mouldTableInfoList.get(1);
    }

    /**
     * 往模台中增加模具日排产信息
     *
     * @param productionContext      排产上下文
     * @param selectedTable          模台
     * @param productionDate         排产日
     * @param mouldDayProductionInfo 模具日排产信息
     */
    public static void addMouldTableProductionInfo(ProductionContext productionContext, MouldTableInfoDto selectedTable, Integer productionDate, MouldDayProductionVo mouldDayProductionInfo) {
        if (null == selectedTable || null == productionDate || null == mouldDayProductionInfo) {
            return;
        }
        //将模具日排产信息加入模台数据中
        selectedTable.setLastProductionInfo(mouldDayProductionInfo);
        selectedTable.getProductionList().add(mouldDayProductionInfo);
        selectedTable.getProductionDateSet().add(productionDate);
        //根据排产分组编号，获取排产分组
        String productionGroupValue = selectedTable.getProductionGroupValue();
        if (StringUtils.isBlank(productionGroupValue)) {
            return;
        }
        ProductionGroupInfoDto productionGroupInfo = productionContext.getProductionGroupInfoMap().get(productionGroupValue);
        if (null == productionGroupInfo) {
            return;
        }
        markFinishByProductionGroup(productionContext, productionGroupInfo);
    }

    /**
     * 批量标记排产分组是否已经排产完毕
     * 对已经排产完毕的分组，状态标记为完成
     * 根据排产模台状况，标记
     *
     * @param productionContext
     */
    public static void markFinishProductionGroup(ProductionContext productionContext) {
        //对已经排产完毕的分组打上排产完毕标记
        Map<String, ProductionGroupInfoDto> productionGroupInfoMap = productionContext.getProductionGroupInfoMap();
        if (CollectionUtils.isEmpty(productionGroupInfoMap)) {
            return;
        }
        productionGroupInfoMap.entrySet().stream().forEach(productionGroupEntry -> {
            ProductionGroupInfoDto productionGroupInfo = productionGroupEntry.getValue();
            markFinishByProductionGroup(productionContext, productionGroupInfo);
        });
    }

    /**
     * 对单个排产分组，标记其是否排产完毕
     * 根据排产分组中的模台排产天数信息，
     * 标记排产分组是否已经排产完毕
     *
     * @param productionContext   排产上下文
     * @param productionGroupInfo 排产分组
     */
    public static void markFinishByProductionGroup(ProductionContext productionContext, ProductionGroupInfoDto productionGroupInfo) {
        if (null == productionGroupInfo) {
            return;
        }
        List<MouldTableInfoDto> mouldTableInfoList = productionGroupInfo.getMouldTableInfoList();
        if (CollectionUtils.isEmpty(mouldTableInfoList)) {
            productionGroupInfo.setProductionFinish(false);
            productionGroupInfo.setEmptyGroup(true);
            return;
        }
        if (productionGroupInfo.isProductionFinish()) {
            return;
        }
        boolean isFinish = true;
        boolean isEmpty = true;
        for (MouldTableInfoDto mouldTableInfo : mouldTableInfoList) {
            Integer productionDays = mouldTableInfo.getProductionDateSet().size();
            if (productionDays > BigDecimal.ZERO.intValue()) {
                isEmpty = false;
            }
            if (!productionContext.getMonthWorkDays().equals(productionDays)) {
                isFinish = false;
                break;
            }
        }
        productionGroupInfo.setEmptyGroup(isEmpty);
        productionGroupInfo.setProductionFinish(isFinish);
    }

    /**
     * 根据排产分组信息，方向查找符合条件的计划
     *
     * @param productionContext   排产上下文
     * @param productionGroupInfo 排产分组信息
     * @param currentPlanList     当前排产计划
     * @return
     */
    public static List<MonthPlanManufacturingRequirementVo> findRequirementPlan(ProductionContext productionContext, ProductionGroupInfoDto productionGroupInfo, List<MonthPlanManufacturingRequirementVo> currentPlanList) {


        return null;
    }

    /**
     * 构建分组排产结果辅助类
     *
     * @param productionContext 排产上下文
     * @return
     */
    public static List<ProductionGroupResultHelper> buildProductionGroupResult(ProductionContext productionContext) {
        Map<String, ProductionGroupInfoDto> productionGroupInfoMap = productionContext.getProductionGroupInfoMap();
        if (CollectionUtils.isEmpty(productionGroupInfoMap)) {
            return Collections.emptyList();
        }
        List<ProductionGroupResultHelper> resultList = new ArrayList<>();
        productionGroupInfoMap.entrySet().stream().forEach(productionGroupEntry -> {
            List<ProductionGroupResultHelper> groupResultList = buildHelper(productionContext, productionGroupEntry.getValue());
            if (CollectionUtils.isEmpty(groupResultList)) {
                return;
            }
            resultList.addAll(groupResultList);
        });
        return resultList;
    }

    /**
     * 单模排产，得到选中的排产分组信息
     *
     * @param productionContext 排产上下文
     * @param mouldInfo         模具信息
     * @param productionPlan    排产计划
     * @return
     */
    public static SingleMouldSelectedMouldTableHelper getStartInfoBySingle(ProductionContext productionContext, MouldInfoVO mouldInfo, MonthPlanManufacturingRequirementVo productionPlan) {
        List<ProductionGroupInfoDto> linkProductionGroupList = findLinkProductionGroup(productionContext, mouldInfo, productionPlan);
        if (CollectionUtils.isEmpty(linkProductionGroupList)) {
            //找不到排产分组，表示不能再排了
            return new SingleMouldSelectedMouldTableHelper(null, null, null);
        }
        ProductionOrientEnum productionOrient = mouldInfo.getProductionOrient();
        ProductionGroupInfoDto selectedProductionGroup = selectedProductionGroup(productionContext, linkProductionGroupList, productionPlan, true, productionOrient);
        if (null == selectedProductionGroup) {
            return new SingleMouldSelectedMouldTableHelper(null, null, null);
        }
        List<MouldTableInfoDto> mouldTableInfoList = selectedProductionGroup.getMouldTableInfoList();
        ProductionOrientEnum groupProductionOrient = selectedProductionGroup.getProductionOrient();
        //单模台
        if (ProductionConstant.SINGLE_MOULD_QTY.equals(selectedProductionGroup.getMouldNumber())) {
            MouldTableInfoDto selectedMouldTable = mouldTableInfoList.get(BigDecimal.ZERO.intValue());
            Integer productionGroupStartDate = getStartDateByMouldTable(productionContext, groupProductionOrient, selectedMouldTable, productionOrient);
            Integer productionGroupEndDate = getEndDateByMouldTable(productionContext, groupProductionOrient, selectedMouldTable, productionOrient);
            String singleMouldGroupFormat = "单模规格排产单模台衔接分组：%s 排产方向：%s 起始日期: [%s-%s]";
            String singleMouldGroupContext = String.format(singleMouldGroupFormat, selectedProductionGroup.getProductionGroupValue(), productionOrient.getDesc(), productionGroupStartDate, productionGroupEndDate);
            ProductionLogUtils.addFindLinkProductionGroupLog(productionContext, productionPlan, singleMouldGroupContext);
            return new SingleMouldSelectedMouldTableHelper(selectedMouldTable, productionGroupStartDate, productionGroupEndDate);
        }
        //双模台同规格，则直接取同规格模台
        String productCode = productionPlan.getProductCode();
        MouldTableInfoDto firstMouldTable = mouldTableInfoList.get(BigDecimal.ZERO.intValue());
        MouldTableInfoDto secondMouldTable = mouldTableInfoList.get(BigDecimal.ONE.intValue());
        if (selectedProductionGroup.isSameProductCode(productCode)) {
            MouldDayProductionVo lastProductionInfo = firstMouldTable.getLastProductionInfo();
            if (null != lastProductionInfo && lastProductionInfo.getProductCode().equals(productCode)) {
                Integer firstStartDate = getStartDateByMouldTable(productionContext, groupProductionOrient, firstMouldTable, productionOrient);
                Integer firstEndDate = getEndDateByMouldTable(productionContext, groupProductionOrient, firstMouldTable, productionOrient);
                return new SingleMouldSelectedMouldTableHelper(firstMouldTable, firstStartDate, firstEndDate);
            }
            Integer secondStartDate = getStartDateByMouldTable(productionContext, groupProductionOrient, secondMouldTable, productionOrient);
            Integer secondEndDate = getEndDateByMouldTable(productionContext, groupProductionOrient, secondMouldTable, productionOrient);
            return new SingleMouldSelectedMouldTableHelper(secondMouldTable, secondStartDate, secondEndDate);
        }
        //双模台--取产能最大的
        Integer firstStartDate = getStartDateByMouldTable(productionContext, groupProductionOrient, firstMouldTable, productionOrient);
        Integer firstEndDate = getEndDateByMouldTable(productionContext, groupProductionOrient, firstMouldTable, productionOrient);
        Integer secondStartDate = getStartDateByMouldTable(productionContext, groupProductionOrient, secondMouldTable, productionOrient);
        Integer secondEndDate = getEndDateByMouldTable(productionContext, groupProductionOrient, secondMouldTable, productionOrient);
        Integer firstDays = Math.abs(firstStartDate - firstEndDate);
        Integer secondDays = Math.abs(secondStartDate - secondEndDate);
        String singleMouldGroupFormat = "单模规格排产双模台衔接分组：%s 排产方向：%s 起始日期: %s: [%s-%s]";
        if (firstDays > secondDays) {
            String singleMouldGroupContext = String.format(singleMouldGroupFormat, selectedProductionGroup.getProductionGroupValue(), productionOrient.getDesc(), 1, firstStartDate, firstEndDate);
            ProductionLogUtils.addFindLinkProductionGroupLog(productionContext, productionPlan, singleMouldGroupContext);
            return new SingleMouldSelectedMouldTableHelper(firstMouldTable, firstStartDate, firstEndDate);
        }
        String singleMouldGroupContext = String.format(singleMouldGroupFormat, selectedProductionGroup.getProductionGroupValue(), productionOrient.getDesc(), 2, secondStartDate, secondEndDate);
        ProductionLogUtils.addFindLinkProductionGroupLog(productionContext, productionPlan, singleMouldGroupContext);
        return new SingleMouldSelectedMouldTableHelper(secondMouldTable, secondStartDate, secondEndDate);
    }

    /**
     * 拼模排产，找到选中的排产分组信息
     *
     * @param productionContext    排产上下文
     * @param beforeProductionPlan 拼模前规格信息
     * @param afterProductionPlan  拼模后规格信息
     * @param days                 拼模需要排产的最大天数
     * @return
     */
    public static SelectedProductionGroupHelper getStartInfoByAssembling(ProductionContext productionContext, AssemblingMouldProductionGroupHelper beforeProductionPlan, AssemblingMouldProductionGroupHelper afterProductionPlan, Integer days) {
        //得到所有排产分组
        List<ProductionGroupInfoDto> allProductionGroupInfoList = new ArrayList<>(productionContext.getProductionGroupInfoMap().values());
        if (CollectionUtils.isEmpty(allProductionGroupInfoList)) {
            return null;
        }
        //获取还未排产完的排产分组
        List<ProductionGroupInfoDto> noFinishList = allProductionGroupInfoList.stream().filter(productionGroupInfo -> !productionGroupInfo.isProductionFinish()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(noFinishList)) {
            return null;
        }
        //获取双模台排产分组
        List<ProductionGroupInfoDto> doubleMouldTableList = noFinishList.stream().filter(productionGroupInfo -> ProductionConstant.DOUBLE_MOULD_QTY.equals(productionGroupInfo.getMouldNumber())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(doubleMouldTableList)) {
            return null;
        }
        //获取可支持拼模排产天数的排产分组
        List<ProductionGroupInfoDto> fulfillmentList = doubleMouldTableList.stream().filter(productionGroupInfo -> productionGroupInfo.isContinueProductionDayCapacity(days, true)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(fulfillmentList)) {
            return null;
        }
        //再获取共生胎同模具
        List<ProductionGroupInfoDto> findGroupList = selectedAssemblingGroup(fulfillmentList, beforeProductionPlan, afterProductionPlan);
        if (CollectionUtils.isEmpty(findGroupList)) {
            return null;
        }
        findGroupList.sort(Comparator.comparing(ProductionGroupInfoDto::getAssemblingContinueDays));
        ProductionGroupInfoDto selected = findGroupList.get(0);
        ProductionOrientEnum productionOrient = selected.getProductionOrient();
        if (null == productionOrient) {
            productionOrient = ProductionOrientEnum.FORWARD;
        }
        List<MouldTableInfoDto> mouldTableInfoList = selected.getMouldTableInfoList();
        MouldTableInfoDto firstMouldTable = mouldTableInfoList.get(BigDecimal.ZERO.intValue());
        MouldTableInfoDto secondMouldTable = mouldTableInfoList.get(BigDecimal.ONE.intValue());
        Integer firstStartDay = firstMouldTable.getStartDay(productionOrient);
        Integer firstEndDay = firstMouldTable.getEndDay(productionOrient);
        Integer maxDay = Math.max(firstStartDay, firstEndDay);

        Integer secondStartDay = secondMouldTable.getStartDay(productionOrient);
        Integer secondEndDay = secondMouldTable.getEndDay(productionOrient);
        Integer minDay = Math.min(secondStartDay, secondEndDay);
        //没有交叉集
        if (maxDay <= minDay) {
            return null;
        }
        //值有交叉集合
        if (ProductionOrientEnum.FORWARD == productionOrient) {
            Integer realStartDay = Math.max(firstStartDay, secondStartDay);
            Integer realEndDay = Math.min(firstEndDay, secondEndDay);
            return new SelectedProductionGroupHelper(selected, realStartDay, realEndDay, null);
        }
        //反向
        Integer realStartDay = Math.min(firstStartDay, secondStartDay);
        Integer realEndDay = Math.max(firstEndDay, secondEndDay);
        return new SelectedProductionGroupHelper(selected, realStartDay, realEndDay, null);
    }

    /**
     * 双模排产，找到选中的排产分组信息
     *
     * @param productionContext 排产上下文
     * @param mouldInfo         模具信息
     * @param productionPlan    排产计划
     * @return
     */
    public static SelectedProductionGroupHelper getStartInfoByDouble(ProductionContext productionContext, MouldInfoVO mouldInfo, MonthPlanManufacturingRequirementVo productionPlan) {
        List<ProductionGroupInfoDto> linkProductionGroupList = findLinkProductionGroup(productionContext, mouldInfo, productionPlan);
        if (CollectionUtils.isEmpty(linkProductionGroupList)) {
            //找不到排产分组，表示不能再排了
            return null;
        }
        ProductionOrientEnum productionOrient = mouldInfo.getProductionOrient();
        ProductionGroupInfoDto selectedProductionGroup = selectedProductionGroup(productionContext, linkProductionGroupList, productionPlan, false, productionOrient);
        if (null == selectedProductionGroup) {
            return null;
        }
        List<MouldTableInfoDto> mouldTableInfoList = selectedProductionGroup.getMouldTableInfoList();
        ProductionOrientEnum groupProductionOrient = selectedProductionGroup.getProductionOrient();
        MouldTableInfoDto firstMouldTable = mouldTableInfoList.get(0);
        MouldTableInfoDto secondMouldTable = mouldTableInfoList.get(1);
        Integer firstStartDate = getStartDateByMouldTable(productionContext, groupProductionOrient, firstMouldTable, productionOrient);
        Integer firstEndDate = getEndDateByMouldTable(productionContext, groupProductionOrient, firstMouldTable, productionOrient);
        Integer secondStartDate = getStartDateByMouldTable(productionContext, groupProductionOrient, secondMouldTable, productionOrient);
        Integer secondEndDate = getEndDateByMouldTable(productionContext, groupProductionOrient, secondMouldTable, productionOrient);
        String doubleMouldGroupFormat = "双模排产衔接分组：%s 排产方向：%s 起始日期: 1: [%s-%s]、2: [%s-%s]";
        String doubleMouldGroupContext = String.format(doubleMouldGroupFormat, selectedProductionGroup.getProductionGroupValue(), productionOrient.getDesc(), firstStartDate, firstEndDate, secondStartDate, secondEndDate);
        ProductionLogUtils.addFindLinkProductionGroupLog(productionContext, productionPlan, doubleMouldGroupContext);
        if (firstStartDate.equals(secondStartDate) && firstEndDate.equals(secondEndDate)) {
            return new SelectedProductionGroupHelper(selectedProductionGroup, firstStartDate, firstEndDate, null);
        }
        Integer firstDiff = Math.abs(firstStartDate - firstEndDate);
        Integer secondDiff = Math.abs(secondStartDate - secondEndDate);
        String beforeProductCode;
        boolean isFirst = (firstDiff > secondDiff) ? true : false;
        if (isFirst) {
            beforeProductCode = secondMouldTable.getLastProductionInfo().getProductCode();
        } else {
            beforeProductCode = firstMouldTable.getLastProductionInfo().getProductCode();
        }
        boolean isAssemblingMould = ProductionPlanAssemblingMouldUtils.isAssemblingMould(productionContext, beforeProductCode, productionPlan.getProductCode());
        //不能拼
        if (!isAssemblingMould) {
            if (isFirst) {
                return new SelectedProductionGroupHelper(selectedProductionGroup, secondStartDate, secondEndDate, null);
            }
            return new SelectedProductionGroupHelper(selectedProductionGroup, firstStartDate, firstEndDate, null);
        }
        //可拼
        DoubleMouldTableHelper doubleMouldTableHelper = new DoubleMouldTableHelper(firstStartDate, firstEndDate, secondStartDate, secondEndDate);
        return new SelectedProductionGroupHelper(selectedProductionGroup, null, null, doubleMouldTableHelper);
    }

    /**
     * 根据选中模台，更新对应排产分组的排产方向
     *
     * @param productionContext 排产上下文
     * @param selectedGroup     选中的排产模台
     * @param productionOrient  排产方向
     */
    public static void updateProductionOrient(ProductionContext productionContext, SingleMouldSelectedMouldTableHelper selectedGroup, ProductionOrientEnum productionOrient) {
        if (null == selectedGroup) {
            return;
        }
        MouldTableInfoDto selectedMouldTableInfo = selectedGroup.getSelectedMouldTableInfo();
        if (null == selectedMouldTableInfo) {
            return;
        }
        ProductionGroupInfoDto productionGroupInfo = productionContext.getProductionGroupInfoMap().get(selectedMouldTableInfo.getProductionGroupValue());
        if (null == productionGroupInfo) {
            return;
        }
        productionGroupInfo.setProductionOrient(productionOrient);
    }

    /**
     * 创建空的排产模台信息对象
     *
     * @param productionContext    排产上下文
     * @param productionGroupValue 排产分组
     * @return
     */
    private static MouldTableInfoDto createEmptyMouldTable(ProductionContext productionContext, String productionGroupValue) {
        MouldTableInfoDto mouldTable = new MouldTableInfoDto();
        mouldTable.setProductionGroupValue(productionGroupValue);
        mouldTable.setProductionList(new ArrayList<>());
        mouldTable.setProductionDateSet(new HashSet<>());
        Set<Integer> wholeMonthWorkDaySet = productionContext.getWholeMonthWorkDaySet();
        mouldTable.setNeedProductionDateSet(wholeMonthWorkDaySet);
        //设置其周期日--最早排产日和最晚排产日
        List<Integer> needProductionDateList = new ArrayList<>(wholeMonthWorkDaySet);
        needProductionDateList.sort(Comparator.comparing(Integer::intValue));
        Integer minStartDay = needProductionDateList.get(0);
        Integer maxEndDay = needProductionDateList.get(needProductionDateList.size() - 1);
        MouldTableCycleProductionVo cycle = new MouldTableCycleProductionVo(minStartDay, maxEndDay);
        mouldTable.setCycleDate(cycle);
        return mouldTable;
    }

    /**
     * 根据排产计划及模具信息，获取可衔接的排产分组信息
     * 优化获取同规格排产分组，再次共生胎、同模具排产分组
     * 再次共生胎、不同模具排产分组；最次同模具排产分组
     * 最后，则为同寸口或是空排产分组
     *
     * @param productionContext 排产上下文
     * @param mouldInfo         模具信息
     * @param productionPlan    排产计划
     * @return
     */
    private static List<ProductionGroupInfoDto> findLinkProductionGroup(ProductionContext productionContext, MouldInfoVO mouldInfo, MonthPlanManufacturingRequirementVo productionPlan) {
        //得到所有排产分组
        List<ProductionGroupInfoDto> allProductionGroupInfoList = new ArrayList<>(productionContext.getProductionGroupInfoMap().values());
        if (CollectionUtils.isEmpty(allProductionGroupInfoList)) {
            return Collections.emptyList();
        }
        //获取还未排产完的排产分组
        List<ProductionGroupInfoDto> noFinishList = allProductionGroupInfoList.stream().filter(productionGroupInfo -> !productionGroupInfo.isProductionFinish()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(noFinishList)) {
            return Collections.emptyList();
        }
        String productCode = productionPlan.getProductCode();
        String embryoCode = productionPlan.getEmbryoCode();
        String mouldNo = mouldInfo.getMouldNo();
        BigDecimal proSize = productionPlan.getProSize();
        //优先获取同规格 再获取共生胎同模具 再获取共生胎不同模具 再获取同模具
        List<ProductionGroupInfoDto> findGroupList = getLeftOverQtyBySame(noFinishList, productCode, embryoCode, mouldNo);
        if (!CollectionUtils.isEmpty(findGroupList)) {
            return findGroupList;
        }
        //再同寸口+空分组
        findGroupList = noFinishList.stream().filter(noFinishGroup -> noFinishGroup.isSameProSize(proSize)).collect(Collectors.toList());
        if (null == findGroupList) {
            findGroupList = new ArrayList<>();
        }
        //加入空排产分组
        List<ProductionGroupInfoDto> emptyProductionGroupList = noFinishList.stream().filter(noFinishProductionGroup -> noFinishProductionGroup.getEmptyGroup()).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(emptyProductionGroupList)) {
            findGroupList.addAll(emptyProductionGroupList);
        }
        return findGroupList;
    }

    /**
     * 获取拼模排产分组，按共生胎、同模具；共生胎、不同模具；同模具方式匹配
     *
     * @param fulfillmentList
     * @param beforeProductionPlan
     * @param afterProductionPlan
     * @return
     */
    private static List<ProductionGroupInfoDto> selectedAssemblingGroup(List<ProductionGroupInfoDto> fulfillmentList, AssemblingMouldProductionGroupHelper beforeProductionPlan, AssemblingMouldProductionGroupHelper afterProductionPlan) {
        if (CollectionUtils.isEmpty(fulfillmentList)) {
            return Collections.emptyList();
        }
        List<ProductionGroupInfoDto> findGroupList = fulfillmentList.stream().filter(findGroup -> findGroup.isSameEmbryoCodeAndMouldNo(beforeProductionPlan.getEmbryoCode(), beforeProductionPlan.getMouldNo()) && findGroup.isSameEmbryoCodeAndMouldNo(afterProductionPlan.getEmbryoCode(), afterProductionPlan.getMouldNo())).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(findGroupList)) {
            return findGroupList;
        }
        findGroupList = fulfillmentList.stream().filter(findGroup -> findGroup.isSameEmbryoCodeAndMouldNo(beforeProductionPlan.getEmbryoCode(), beforeProductionPlan.getMouldNo()) || findGroup.isSameEmbryoCodeAndMouldNo(afterProductionPlan.getEmbryoCode(), afterProductionPlan.getMouldNo())).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(findGroupList)) {
            return findGroupList;
        }
        findGroupList = fulfillmentList.stream().filter(findGroup -> findGroup.isSameEmbryoCodeAndNoMouldNo(beforeProductionPlan.getEmbryoCode(), beforeProductionPlan.getMouldNo()) || findGroup.isSameEmbryoCodeAndNoMouldNo(afterProductionPlan.getEmbryoCode(), afterProductionPlan.getMouldNo())).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(findGroupList)) {
            return findGroupList;
        }
        findGroupList = fulfillmentList.stream().filter(findGroup -> findGroup.isSameMouldNo(beforeProductionPlan.getMouldNo()) || findGroup.isSameMouldNo(afterProductionPlan.getMouldNo())).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(findGroupList)) {
            return findGroupList;
        }
        return fulfillmentList;
    }

    /**
     * 从可衔接列表中，获取选中的链接排产分组
     *
     * @param productionContext       排产上下文
     * @param linkProductionGroupList 可衔接的排产列表
     * @param productionPlan          排产计划
     * @param isSingle                是否单模排产
     * @param productionOrient        当前排产方向
     * @return
     */
    private static ProductionGroupInfoDto selectedProductionGroup(ProductionContext productionContext, List<ProductionGroupInfoDto> linkProductionGroupList, MonthPlanManufacturingRequirementVo productionPlan, boolean isSingle, ProductionOrientEnum productionOrient) {
        if (CollectionUtils.isEmpty(linkProductionGroupList) || null == productionPlan) {
            return null;
        }
        //设置可衔接的排产分组的剩余产能
        linkProductionGroupList.stream().forEach(findProductionGroup -> setProductionGroupLeftOverQtyByProductCode(productionContext, productionPlan, isSingle, findProductionGroup, productionOrient));
        //统计规格还需总排产量
        List<MonthPlanManufacturingRequirementVo> needProductionList = new ArrayList<>(productionContext.getMonthPlanInitMap().values()).stream().filter(plan -> productionPlan.getProductCode().equals(plan.getProductCode())).collect(Collectors.toList());
        Long needProductionQty = needProductionList.stream().collect(Collectors.summingLong(MonthPlanManufacturingRequirementVo::getProductionQty));
        //优先挑选产能满足的排产分组
        List<ProductionGroupInfoDto> fulfillmentList = linkProductionGroupList.stream().filter(selectProductionGroup -> selectProductionGroup.isFulfillment(needProductionQty)).collect(Collectors.toList());
        //没有剩余产能能直接满足的，则取剩余产能最大的分组
        if (CollectionUtils.isEmpty(fulfillmentList)) {
            //取剩余产能最大的
            linkProductionGroupList.sort(Comparator.comparing(ProductionGroupInfoDto::getLeftOverQty, Comparator.reverseOrder()));
            return linkProductionGroupList.get(0);
        }
        //单模，有剩余产能能覆盖的，单模优先选单模台，再次拼模双模台，最次不拼模双模台
        if (isSingle) {
            //单模台
            List<ProductionGroupInfoDto> singleMouldGroupList = fulfillmentList.stream().filter(fulfillmentGroup -> ProductionConstant.SINGLE_MOULD_QTY.equals(fulfillmentGroup.getMouldNumber())).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(singleMouldGroupList)) {
                singleMouldGroupList.sort(Comparator.comparing(ProductionGroupInfoDto::getLeftOverQty));
                return singleMouldGroupList.get(0);
            }
            //拼模-双模台
            List<ProductionGroupInfoDto> assemblingMouldGroupList = fulfillmentList.stream().filter(fulfillmentGroup -> fulfillmentGroup.isAssemble()).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(assemblingMouldGroupList)) {
                assemblingMouldGroupList.sort(Comparator.comparing(ProductionGroupInfoDto::getLeftOverQty));
                return assemblingMouldGroupList.get(0);
            }
            fulfillmentList.sort(Comparator.comparing(ProductionGroupInfoDto::getLeftOverQty));
            return fulfillmentList.get(0);
        }
        //双模,优先双模台不拼模，再次双模台拼模
        List<ProductionGroupInfoDto> doubleMouldNoAssemblingGroupList = fulfillmentList.stream().filter(fulfillmentGroup -> ProductionConstant.DOUBLE_MOULD_QTY.equals(fulfillmentGroup.getMouldNumber()) && !fulfillmentGroup.isAssemble()).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(doubleMouldNoAssemblingGroupList)) {
            doubleMouldNoAssemblingGroupList.sort(Comparator.comparing(ProductionGroupInfoDto::getLeftOverQty));
            return doubleMouldNoAssemblingGroupList.get(0);
        }
        List<ProductionGroupInfoDto> doubleMouldAssemblingGroupList = fulfillmentList.stream().filter(fulfillmentGroup -> ProductionConstant.DOUBLE_MOULD_QTY.equals(fulfillmentGroup.getMouldNumber()) && fulfillmentGroup.isAssemble()).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(doubleMouldAssemblingGroupList)) {
            doubleMouldAssemblingGroupList.sort(Comparator.comparing(ProductionGroupInfoDto::getLeftOverQty));
            return doubleMouldAssemblingGroupList.get(0);
        }
        return null;
    }

    /**
     * 获取具有同规格、共生胎、同模具的有剩余产能的排产分组集合
     * 查找顺序：同规格 > 共生胎、同模具 > 共生胎、不同模具 > 同模具
     *
     * @param noFinishList 所有有空闲的排产分组
     * @param productCode  SAP代码
     * @param embryoCode   生胎代码
     * @param mouldNo      模具
     * @return
     */
    private static List<ProductionGroupInfoDto> getLeftOverQtyBySame(List<ProductionGroupInfoDto> noFinishList, String productCode, String embryoCode, String mouldNo) {
        if (CollectionUtils.isEmpty(noFinishList) || StringUtils.isBlank(productCode) || StringUtils.isBlank(embryoCode) || StringUtils.isBlank(mouldNo)) {
            return Collections.emptyList();
        }
        //优先获取同规格的
        List<ProductionGroupInfoDto> findGroupList = noFinishList.stream().filter(noFinishGroup -> noFinishGroup.isSameProductCode(productCode)).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(findGroupList)) {
            return findGroupList;
        }
        //再获取共生胎同模具
        findGroupList = noFinishList.stream().filter(noFinishGroup -> noFinishGroup.isSameEmbryoCodeAndMouldNo(embryoCode, mouldNo)).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(findGroupList)) {
            return findGroupList;
        }
        //再获取共生胎不同模具
        findGroupList = noFinishList.stream().filter(noFinishGroup -> noFinishGroup.isSameEmbryoCodeAndNoMouldNo(embryoCode, mouldNo)).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(findGroupList)) {
            return findGroupList;
        }
        //再获取同模具
        findGroupList = noFinishList.stream().filter(noFinishGroup -> noFinishGroup.isSameMouldNo(mouldNo)).collect(Collectors.toList());
        return findGroupList;
    }

    /**
     * 根据排产计划信息，设置排产分组剩余产能值
     * 根据排产计划得到排产计划规格的单条硫化时间，并计算出单模单天产能
     * 1、如果是单模台分组，则为单模台产能：根据排产方向取得分组当前时间~结束时间段之间的合计产能
     * 2、如果是双模台分组，需要看排产方式
     * 2.1、排产方式为单模，需要判断排产时间长的模台能否与当前计划规格拼模
     * 2.1.1、如果可拼，则取剩余产能最大的模台产能为剩余产能
     * 2.1.2、如果不能拼，则取剩余产能最小的模台产能为剩余产能
     * 2.2、排产方式为双模，同样需要判断排产时间长的模台能否与当前计划规格拼模
     * 2.2.1、如果可拼，则剩余产能 = 双模台剩余产能之和
     * 2.2.2、如果不能拼，则剩余产能 = 2 * 最小剩余产能的模台产能
     *
     * @param productionContext   排产上下文
     * @param productionPlan      排产计划信息
     * @param isSingleProduction  是否单模排产：包含两种场景 1 单模规格 2 使用一模排产(如拼模)
     * @param productionGroupInfo 排产分组
     * @param productionOrient    排产方向
     */
    private static void setProductionGroupLeftOverQtyByProductCode(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, boolean isSingleProduction, ProductionGroupInfoDto productionGroupInfo, ProductionOrientEnum productionOrient) {
        if (null == productionGroupInfo) {
            return;
        }
        List<MouldTableInfoDto> mouldTableInfoList = productionGroupInfo.getMouldTableInfoList();
        if (CollectionUtils.isEmpty(mouldTableInfoList)) {
            return;
        }
        //计划单条硫化时间(含间隔时间) = 硫化时间 + 间隔时间
        BigDecimal singleCuringTime = ProductUtils.getSingleCuringTime(productionPlan, productionContext);
        //单模单天产能
        Long singleMouldCapacity = MouldUtils.getSingleMouldCapacity(productionContext, singleCuringTime);
        //空分组时，暂定排产方向为正向
        ProductionOrientEnum originProductionOrient = productionGroupInfo.getProductionOrient();
        if (null == originProductionOrient) {
            originProductionOrient = ProductionOrientEnum.FORWARD;
        }
        if (null == productionOrient) {
            productionOrient = originProductionOrient;
        }
        MouldTableInfoDto firstMouldTableInfo = mouldTableInfoList.get(BigDecimal.ZERO.intValue());
        //单模台
        if (productionGroupInfo.getMouldNumber() == BigDecimal.ONE.intValue()) {
            Long leftOverQty = getLeftOverQtyByMouldTable(productionContext, originProductionOrient, firstMouldTableInfo, productionOrient, singleMouldCapacity);
            productionGroupInfo.setLeftOverQty(leftOverQty);
            return;
        }
        //双模台
        MouldTableInfoDto secondMouldTableInfo = mouldTableInfoList.get(BigDecimal.ONE.intValue());
        Integer firstStartDay = getStartDateByMouldTable(productionContext, originProductionOrient, firstMouldTableInfo, productionOrient);
        Integer firstEndDay = getEndDateByMouldTable(productionContext, originProductionOrient, firstMouldTableInfo, productionOrient);
        Integer secondStartDay = getStartDateByMouldTable(productionContext, originProductionOrient, secondMouldTableInfo, productionOrient);
        Integer secondEndDay = getEndDateByMouldTable(productionContext, originProductionOrient, secondMouldTableInfo, productionOrient);
        Long firstLeftOverQty = getLeftOverQtyByMouldTable(productionContext, originProductionOrient, firstMouldTableInfo, productionOrient, singleMouldCapacity);
        Long secondLeftOverQty = getLeftOverQtyByMouldTable(productionContext, originProductionOrient, secondMouldTableInfo, productionOrient, singleMouldCapacity);
        //时间一致
        if (firstStartDay.equals(secondStartDay) && firstEndDay.equals(secondEndDay)) {
            if (isSingleProduction) {
                productionGroupInfo.setLeftOverQty(Math.max(firstLeftOverQty, secondLeftOverQty));
                return;
            }
            productionGroupInfo.setLeftOverQty(firstLeftOverQty + secondLeftOverQty);
            return;
        }
        //时间不一致
        String beforeProductCode;
        if (firstLeftOverQty > secondLeftOverQty) {
            beforeProductCode = secondMouldTableInfo.getLastProductionInfo().getProductCode();
        } else {
            beforeProductCode = firstMouldTableInfo.getLastProductionInfo().getProductCode();
        }
        boolean isAssemblingMould = ProductionPlanAssemblingMouldUtils.isAssemblingMould(productionContext, beforeProductCode, productionPlan.getProductCode());
        //单模排产 排产时间长的模台不能与当前计划规格拼，则取产能小的模台产能
        if (isSingleProduction && !isAssemblingMould) {
            productionGroupInfo.setLeftOverQty(Math.min(firstLeftOverQty, secondLeftOverQty));
            return;
        }
        //单模排产 排产时间长的模台能与当前计划规格拼，则取产能大的模台产能
        if (isSingleProduction && isAssemblingMould) {
            //能拼，则取产能最大的
            productionGroupInfo.setLeftOverQty(Math.max(firstLeftOverQty, secondLeftOverQty));
            return;
        }
        //双模排产-排产时间长的模台不能与当前计划规格拼，则取产能小的模台产能*2
        if (!isAssemblingMould) {
            productionGroupInfo.setLeftOverQty(2 * Math.min(firstLeftOverQty, secondLeftOverQty));
            return;
        }
        Long sumLeftOverQty = BigDecimal.ZERO.longValue();
        for (MouldTableInfoDto mouldTableInfo : mouldTableInfoList) {
            Integer startDay = getStartDateByMouldTable(productionContext, originProductionOrient, mouldTableInfo, productionOrient);
            Integer endDay = getEndDateByMouldTable(productionContext, originProductionOrient, mouldTableInfo, productionOrient);
            if (startDay.equals(endDay)) {
                continue;
            }
            sumLeftOverQty = sumLeftOverQty + singleMouldCapacity * Math.abs(endDay - startDay);
        }
        productionGroupInfo.setLeftOverQty(sumLeftOverQty);
    }

    /**
     * 获取模台的剩余产能
     *
     * @param productionContext     排产上下文
     * @param productionGroupOrient 分组的排产方向
     * @param mouldTableInfo        模台
     * @param productionOrient      排产方向
     * @param singleMouldCapacity   单模单天最大产能
     * @return
     */
    private static Long getLeftOverQtyByMouldTable(ProductionContext productionContext, ProductionOrientEnum productionGroupOrient, MouldTableInfoDto mouldTableInfo, ProductionOrientEnum productionOrient, Long singleMouldCapacity) {
        Integer startDay = getStartDateByMouldTable(productionContext, productionGroupOrient, mouldTableInfo, productionOrient);
        Integer endDay = getEndDateByMouldTable(productionContext, productionGroupOrient, mouldTableInfo, productionOrient);
        if (startDay.equals(endDay)) {
            return BigDecimal.ZERO.longValue();
        }
        return singleMouldCapacity * (Math.abs(endDay - startDay) + BigDecimal.ONE.intValue());
    }

    /**
     * 根据模台排产信息及排产方向，获取起始排产日
     * 1、如果是空分组，
     * 1.1、实际排产方向为正向，则取本月第一天
     * 1.2、实际排产方向为反向，则取本月最后一天
     * 2、如果分组已经排产
     * 2.1、如果分组排产方向与实际的排产方向相同，则取当前排产日
     * 2.2、如果分组排产方向为正向，而实际排产方向为反向，则取第一个大于当前排产日的日期，如果取不到，则为本月最后一天
     * 2.3、如果分组排产方向为反向，而实际排产方向为正向，则取第一个小于当前排产日的日期，如果取不到，则为本月第一天
     *
     * @param productionContext     排产上下文
     * @param groupProductionOrient 分组原有的排产方向
     * @param mouldTableInfo        排产模台信息
     * @param productionOrient      排产方向
     * @return
     */
    private static Integer getStartDateByMouldTable(ProductionContext productionContext, ProductionOrientEnum groupProductionOrient, MouldTableInfoDto mouldTableInfo, ProductionOrientEnum productionOrient) {
        MouldDayProductionVo lastProductionInfo = mouldTableInfo.getLastProductionInfo();
        //空分组，正向取第一天
        if (null == lastProductionInfo && ProductionOrientEnum.FORWARD == productionOrient) {
            return ProductionConstant.MONTH_START_DAY;
        }
        //空分组，反向取最后一天
        if (null == lastProductionInfo && ProductionOrientEnum.REVERSE == productionOrient) {
            return productionContext.getMonthDays();
        }
        //不为空，则表示已经排产过
        Integer currentProductionDate = lastProductionInfo.getProductionDate();
        //方向相同，取当前排产日
        if (productionOrient == groupProductionOrient) {
            return currentProductionDate;
        }
        //方向不同
        List<Integer> productionDateList = new ArrayList<>(mouldTableInfo.getProductionDateSet());
        //原来是正向，现在要反向，则第一个大于已排产日的日期就是其开始日
        if (ProductionOrientEnum.FORWARD == groupProductionOrient) {
            //从小到大排序
            productionDateList.sort(Comparator.comparing(Integer::intValue));
            Integer startDate = null;
            for (Integer day : productionDateList) {
                if (day > currentProductionDate) {
                    startDate = day;
                    break;
                }
            }
            if (null == startDate) {
                return productionContext.getMonthDays();
            }
            return startDate;
        }
        //原来是反向，现在要正向，从大到小排序则取第一个小于当前排产日的日期就是其开始日
        productionDateList.sort(Comparator.comparing(Integer::intValue, Comparator.reverseOrder()));
        Integer startDate = null;
        for (Integer day : productionDateList) {
            if (day < currentProductionDate) {
                startDate = day;
                break;
            }
        }
        if (null == startDate) {
            return ProductionConstant.MONTH_START_DAY;
        }
        return startDate;
    }

    /**
     * 根据模台排产信息及排产方向，获取结束排产日
     * 1、如果是空分组，
     * 1.1、实际排产方向为正向，则取本月最后一天
     * 1.2、实际排产方向为反向，则取本月最后一天
     * 2、如果分组已经排产
     * 2.1、如果分组排产方向与实际的排产方向不相同，则取当前排产日
     * 2.2、如果分组排产与实际排产方向相同，且方向为正向，则取第一个大于当前排产日的日期，如果取不到，则为本月最后一天
     * 2.3、如果分组排产与实际排产方向相同，且方向为反向，则取第一个小于当前排产日的日期，如果取不到，则为本月第一天
     *
     * @param productionContext
     * @param groupProductionOrient
     * @param mouldTableInfo
     * @param productionOrient
     * @return
     */
    private static Integer getEndDateByMouldTable(ProductionContext productionContext, ProductionOrientEnum groupProductionOrient, MouldTableInfoDto mouldTableInfo, ProductionOrientEnum productionOrient) {
        MouldDayProductionVo lastProductionInfo = mouldTableInfo.getLastProductionInfo();
        //空分组，正向取最后一天
        if (null == lastProductionInfo && ProductionOrientEnum.FORWARD == productionOrient) {
            return productionContext.getMonthDays();
        }
        //空分组，反向取第一天
        if (null == lastProductionInfo && ProductionOrientEnum.REVERSE == productionOrient) {
            return ProductionConstant.MONTH_START_DAY;
        }
        //不为空，则表示已经排产过
        Integer currentProductionDate = lastProductionInfo.getProductionDate();
        //方向不同，取当前排产日
        if (productionOrient != groupProductionOrient) {
            return currentProductionDate;
        }
        //方向相同
        List<Integer> productionDateList = new ArrayList<>(mouldTableInfo.getProductionDateSet());
        //正向，则第一个大于已排产日的日期就是其结束日
        Integer endDate = null;
        if (ProductionOrientEnum.FORWARD == groupProductionOrient) {
            //从小到大排序
            productionDateList.sort(Comparator.comparing(Integer::intValue));
            for (Integer day : productionDateList) {
                if (day > currentProductionDate) {
                    endDate = day;
                    break;
                }
            }
            if (null == endDate) {
                return productionContext.getMonthDays();
            }
            return endDate;
        }
        //反向，从大到小排序则取第一个小于当前排产日的日期就是其结束日
        productionDateList.sort(Comparator.comparing(Integer::intValue, Comparator.reverseOrder()));
        for (Integer day : productionDateList) {
            if (day < currentProductionDate) {
                endDate = day;
                break;
            }
        }
        if (null == endDate) {
            return ProductionConstant.MONTH_START_DAY;
        }
        return endDate;
    }

    /**
     * 创建分组排产结果信息对象
     *
     * @param productionContext   排产上下文
     * @param productionGroupInfo 排产分组信息
     * @return
     */
    private static List<ProductionGroupResultHelper> buildHelper(ProductionContext productionContext, ProductionGroupInfoDto productionGroupInfo) {
        List<MouldTableInfoDto> mouldTableInfoList = productionGroupInfo.getMouldTableInfoList();
        if (CollectionUtils.isEmpty(mouldTableInfoList)) {
            return Collections.emptyList();
        }
        List<ProductionGroupResultHelper> resultHelperList = new ArrayList<>();
        Integer number = BigDecimal.ONE.intValue();
        String mouldTableNoFormat = "%s:%d";
        for (MouldTableInfoDto mouldTableInfo : mouldTableInfoList) {
            String mouldTableNo = String.format(mouldTableNoFormat, productionGroupInfo.getProductionGroupValue(), number);
            ProductionGroupResultHelper tableHelper = buildBaseInfo(productionContext, productionGroupInfo);
            tableHelper.setGroupMouldTableNo(mouldTableNo);
            setProductionDateInfo(tableHelper, mouldTableInfo.getProductionList());
            resultHelperList.add(tableHelper);
            number = number + BigDecimal.ONE.intValue();
        }
        return resultHelperList;
    }

    /**
     * 构建排产分组信息，构建排产结果基础信息对象
     *
     * @param productionContext   排产上下文
     * @param productionGroupInfo 排产分组信息对象
     * @return
     */
    private static ProductionGroupResultHelper buildBaseInfo(ProductionContext productionContext, ProductionGroupInfoDto productionGroupInfo) {
        ProductionGroupResultHelper helper = new ProductionGroupResultHelper();
        //排产基础信息
        helper.setFactoryCode(productionContext.getFactoryCode());
        helper.setYear(productionContext.getYear());
        helper.setMonth(productionContext.getMonth());
        helper.setMonthPlanVersion(productionContext.getMonthPlanVersion());
        helper.setProductionVersion(productionContext.getProductionVersion());
        //分组信息
        helper.setProductionGroupNo(productionGroupInfo.getProductionGroupValue());
        helper.setMouldNumber(productionGroupInfo.getMouldNumber());
        return helper;
    }

    /**
     * 设置日排产JSON信息
     *
     * @param tableHelper    分组排产结果
     * @param productionList 分组日排产信息
     */
    private static void setProductionDateInfo(ProductionGroupResultHelper tableHelper, List<MouldDayProductionVo> productionList) {
        if (CollectionUtils.isEmpty(productionList)) {
            return;
        }
        String fieldNameFormat = "day%d";
        String fieldName;
        for (Integer day = ProductionConstant.MONTH_START_DAY; day <= ProductionConstant.MONTH_MAX_DAY; day++) {
            fieldName = String.format(fieldNameFormat, day);
            Integer productionDate = day;
            List<MouldDayProductionVo> productionInfoList = productionList.stream().filter(mouldDayProduction -> productionDate.equals(mouldDayProduction.getProductionDate())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(productionInfoList)) {
                continue;
            }
            String resultJson = JSON.toJSONString(productionInfoList);
            tableHelper.setFieldValueByFieldName(fieldName, resultJson);
        }
    }

    private ProductionGroupUtils() {

    }
}

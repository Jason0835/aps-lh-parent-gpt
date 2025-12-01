package com.zlt.aps.factory.scheduling.moulding.group;

import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.MouldTableInfoDto;
import com.zlt.aps.factory.domain.dto.ProductionGroupInfoDto;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.enums.ProductionOrientEnum;
import com.zlt.aps.factory.scheduling.AbstractProductionBusinessService;
import com.zlt.aps.factory.scheduling.ContinueMouldProductionHelper;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import com.zlt.aps.factory.utils.*;
import com.zlt.aps.monthplan.api.enums.MouldProductionLogType;
import com.zlt.aps.monthplan.api.enums.ProductionTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 续作规格使用续作模具排产
 *
 * @author
 */
@Slf4j
@Service(value = "continueProductMould")
public class ContinueProductProductionService extends AbstractProductionBusinessService {

    public ContinueProductProductionService(ProductionSchedulingDataService dataService) {
        super(dataService);
    }

    @Override
    public void run(Context context, Object userObj) {
        ProductionContext productionContext = (ProductionContext) context;
        //排产流程日志记录 "=====分厂%s, 计划年月：%d-%d, 计划版本：%s，生产计划续作规格使用续作模具排产开始===="
        ProductionLogUtils.addStartContinuePlanContinueMouldProductionLog(productionContext);
        //获取续作计划
        List<MonthPlanManufacturingRequirementVo> continueProductionList = ProductionPlanUtils.getContinuePlan(productionContext);
        if (CollectionUtils.isEmpty(continueProductionList)) {
            //排产流程日志记录 ===没有续作排产计划
            ProductionLogUtils.addNoContinuePlanProductionLog(productionContext);
            return;
        }
        //按规格分组执行
        LinkedHashMap<String, List<MonthPlanManufacturingRequirementVo>> productGroupMap = getProductGroupList(continueProductionList);
        productGroupMap.entrySet().forEach(entry -> {
            String productCode = entry.getKey();
            List<MonthPlanManufacturingRequirementVo> continueProductPlanGroup = entry.getValue();
            if (CollectionUtils.isEmpty(continueProductPlanGroup)) {
                return;
            }
            //保存排产的续作模具
            List<MouldInfoVO> enableContinueMouldInfoList = new ArrayList<>();
            continueProductPlanGroup.stream().forEach(continuePlan -> {
                if (!ProductionPlanUtils.isProductionPlan(continuePlan)) {
                    return;
                }
                //排产流程日志记录
                ProductionLogUtils.addBeforeContinuePlanProductionLog(productionContext, MouldProductionLogType.CONTINUE_MOULD_GENERAL_LOG, continuePlan);
                //续作计划使用续作模具排产
                continuePlanProductionByContinueMould(productGroupMap, productionContext, continuePlan, enableContinueMouldInfoList);
                //排产流程日志记录
                ProductionLogUtils.addAfterContinuePlanProductionLog(productionContext, MouldProductionLogType.CONTINUE_MOULD_GENERAL_LOG, continuePlan);
            });
            //将续作模具排到月底，即对productCode进行超产排产
            productionContinueMouldLastPlanFullMonth(enableContinueMouldInfoList, productionContext, productCode);
            //清除排产信息-排产方向
            clearProductionInfo(enableContinueMouldInfoList);
        });
        //标记已经排产完毕的分组
        ProductionGroupUtils.markFinishProductionGroup(productionContext);
    }

    /**
     * 按规格分组，且保持顺序
     *
     * @param continueProductionList 所有续作规格计划
     * @return
     */
    private LinkedHashMap<String, List<MonthPlanManufacturingRequirementVo>> getProductGroupList(List<MonthPlanManufacturingRequirementVo> continueProductionList) {
        LinkedHashMap<String, List<MonthPlanManufacturingRequirementVo>> productGroupMap = new LinkedHashMap<>();
        continueProductionList.stream().forEach(continueProductionPlan -> {
            String productCode = continueProductionPlan.getProductCode();
            List<MonthPlanManufacturingRequirementVo> groupList = productGroupMap.get(productCode);
            if (null == groupList) {
                groupList = new ArrayList<>();
                productGroupMap.put(productCode, groupList);
            }
            groupList.add(continueProductionPlan);
        });
        return productGroupMap;
    }

    /**
     * 续作计划使用续作模具排产
     *
     * @param productGroupMap             续作规格分组集合
     * @param productionContext           排产上下文
     * @param continuePlan                续作计划
     * @param enableContinueMouldInfoList 可排产的续作模具集合
     */
    private void continuePlanProductionByContinueMould(LinkedHashMap<String, List<MonthPlanManufacturingRequirementVo>> productGroupMap, ProductionContext productionContext, MonthPlanManufacturingRequirementVo continuePlan, List<MouldInfoVO> enableContinueMouldInfoList) {
        String productCode = continuePlan.getProductCode();
        if (StringUtils.isBlank(productCode)) {
            return;
        }
        //获取续作模具
        List<MouldProductionProductVo> continueMouldList = productionContext.getContinueProductMap().get(productCode);
        if (CollectionUtils.isEmpty(continueMouldList)) {
            //排产流程日志记录 ===续作计划没有续作模具
            ProductionLogUtils.addNoContinueMouldProductionContinueProductPlan(productionContext, MouldProductionLogType.CONTINUE_MOULD_GENERAL_LOG, continuePlan);
            return;
        }
        //根据模具号，获取模具信息
        List<MouldInfoVO> enableMouldInfoList = new ArrayList<>();
        continueMouldList.stream().forEach(continueMouldInfo -> {
            String mouldCode = continueMouldInfo.getMouldCode();
            MouldInfoVO mouldInfo = productionContext.getMouldInfoMap().get(mouldCode);
            if (null == mouldInfo) {
                return;
            }
            enableMouldInfoList.add(mouldInfo);
        });
        if (CollectionUtils.isEmpty(enableMouldInfoList)) {
            //排产流程日志记录 ===续作计划的续作模具没有可用模具
            ProductionLogUtils.addNoEnableContinueMouldProductionContinueProductPlan(productionContext, MouldProductionLogType.CONTINUE_MOULD_GENERAL_LOG, continuePlan);
            return;
        }
        int mouldSize = enableMouldInfoList.size();
        //20250726 单模排产
        if (mouldSize == BigDecimal.ONE.intValue()) {
            MouldInfoVO mouldInfo = enableMouldInfoList.get(0);
            handlerSingleMould(productGroupMap, mouldInfo, productionContext, continuePlan, enableContinueMouldInfoList);
            return;
        }
        //20250927 ZLT 根据汇总排产量，测算需使用的续作模具数
        List<MouldInfoVO> realContinueMouldList = getRealContinueMouldList(enableMouldInfoList, productionContext, continuePlan);
        //20250513 ZLT 放入续作条件的满月排产模具集合中
        enableContinueMouldInfoList.addAll(realContinueMouldList);
        multiMouldProduction(realContinueMouldList, productionContext, continuePlan);
    }

    /**
     * 对续作模具，满月排产
     *
     * @param enableContinueMouldInfoList 续作模具集合
     * @param productionContext           排产上下文
     * @param productCode                 排产规格
     */
    private void productionContinueMouldLastPlanFullMonth(List<MouldInfoVO> enableContinueMouldInfoList, ProductionContext productionContext, String productCode) {
        //是否对续作模具继续排产，需同时满足开启续作模具满月排产及月均销量达到值 SYS038 SYS042和需求量排产到 月份天数 - SYS041
        if (!productionContext.isFullMonthProduction(productCode)) {
            return;
        }
        if (CollectionUtils.isEmpty(enableContinueMouldInfoList)) {
            return;
        }
        //取得最后一个排产日
        Integer firstLastDay = getLastProductionDay(enableContinueMouldInfoList.get(0));
        if (firstLastDay < ProductionConstant.MONTH_START_DAY) {
            return;
        }
        //没有配置或者需要排产天数达不到，则不进行超产排产如：周期天数为31 配置为5，则需求量需排产到第26天
        Integer canProductionDay = productionContext.getProductionParam().getFullMonthProductionDay();
        if (null == canProductionDay || canProductionDay <= BigDecimal.ZERO.intValue()) {
            return;
        }
        Integer conditionDay = productionContext.getMonthDays() - canProductionDay;
        if (firstLastDay < conditionDay) {
            return;
        }
        //排产流程日志记录
        ProductionLogUtils.addStartFullMonthProductionLog(productionContext, MouldProductionLogType.CONTINUE_MOULD_GENERAL_LOG, productCode);
        enableContinueMouldInfoList.stream().forEach(continueMouldInfo -> {
            Map<Integer, List<MouldDayProductionVo>> dayProductionMap = continueMouldInfo.getDayProductionMap();
            if (CollectionUtils.isEmpty(dayProductionMap)) {
                return;
            }
            List<Integer> productionDayList = new ArrayList<>(dayProductionMap.keySet());
            Collections.sort(productionDayList);
            Integer lastDay = productionDayList.get(productionDayList.size() - 1);
            List<MouldDayProductionVo> productionList = dayProductionMap.get(lastDay);
            if (CollectionUtils.isEmpty(productionList)) {
                return;
            }
            MouldDayProductionVo mouldDayProduction = productionList.get(productionList.size() - 1);
            fullMonthProduction(productionContext, continueMouldInfo, mouldDayProduction, lastDay);
        });
    }

    /**
     * 根据排产的模具信息，清除模具的排产信息
     * 目前清除的是排产方向信息
     *
     * @param usedMouldList 排产使用的模具
     */
    private void clearProductionInfo(List<MouldInfoVO> usedMouldList) {
        if (CollectionUtils.isEmpty(usedMouldList)) {
            return;
        }
        //清除排产方向
        usedMouldList.stream().forEach(continueMouldInfo -> continueMouldInfo.setProductionOrient(null));
    }

    /**
     * 处理是单模续作情形：
     * 1、如果是拼模，需要判断另外一个规格是否有计划量
     * 1.1、如果有，则各自走单模
     * 1.2、如果没有，则需要判断自己是否有多模，
     * 1.2.1、如果有，则走双模，且都需要算换模时间
     * 1.2.2、如果没有，则预先扣除换模时间
     *
     * @param mouldInfo                   模具
     * @param productionContext           排产上下文
     * @param continuePlan                续作计划
     * @param enableContinueMouldInfoList 续作排产模具集合
     */
    private void handlerSingleMould(LinkedHashMap<String, List<MonthPlanManufacturingRequirementVo>> productGroupMap, MouldInfoVO mouldInfo, ProductionContext productionContext, MonthPlanManufacturingRequirementVo continuePlan, List<MouldInfoVO> enableContinueMouldInfoList) {
        ProductionGroupInfoDto productionGroupInfo = ProductionGroupUtils.buildProductionGroupInfoByContinueMould(continuePlan, mouldInfo, productionContext);
        if (null == productionGroupInfo) {
            return;
        }
        //如果不是拼模
        if (!productionGroupInfo.isAssemble()) {
            addMouldInfo(enableContinueMouldInfoList, mouldInfo);
            singleMouldProduction(mouldInfo, productionContext, continuePlan);
            return;
        }
        //如果是拼模，则需看另外一个规格是否有排产量
        String productCode = continuePlan.getProductCode();
        String continueProductionGroupValue = mouldInfo.getContinueProductionGroupValue();
        ContinueProductionGroupVo continueProductionGroup = productionContext.getContinueProductionGroupMap().get(continueProductionGroupValue);
        List<MouldProductionProductVo> anotherProductionProductList = continueProductionGroup.getContinueProductInfoList().stream().filter(mouldProductionProduct -> !productCode.equals(mouldProductionProduct.getProductCode())).collect(Collectors.toList());
        Set<String> productCodeSet = anotherProductionProductList.stream().map(MouldProductionProductVo::getProductCode).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(productCodeSet)) {
            mouldInfo.setIsContinue(YesOrNoEnum.NO.getValue());
            addMouldInfo(enableContinueMouldInfoList, mouldInfo);
            singleMouldProduction(mouldInfo, productionContext, continuePlan);
            return;
        }
        String anotherOne = new ArrayList<>(productCodeSet).get(0);
        String anotherMouldCode = anotherProductionProductList.get(0).getMouldCode();
        List<MonthPlanManufacturingRequirementVo> anotherPlanList = productGroupMap.get(anotherOne);
        Long productionQty = BigDecimal.ZERO.longValue();
        if (!CollectionUtils.isEmpty(anotherPlanList)) {
            productionQty = anotherPlanList.get(0).getSummaryProductionQty();
        }
        //另一个拼模续作规格有计划量
        if (productionQty > BigDecimal.ZERO.longValue()) {
            addMouldInfo(enableContinueMouldInfoList, mouldInfo);
            singleMouldProduction(mouldInfo, productionContext, continuePlan);
            return;
        }
        //是否已经分组排产，有则现有分组排
        boolean isProduction = continuePreviousPlan(productionGroupInfo, productionContext, continuePlan, mouldInfo, enableContinueMouldInfoList);
        if (isProduction) {
            return;
        }
        //还没有排产
        List<MouldInfoVO> enableMouldList = ProductionPlanUtils.getPlanMaxEnableMouldInfo(productCode, productionContext);
        if (enableMouldList.size() <= BigDecimal.ONE.intValue()) {
            mouldInfo.setIsContinue(YesOrNoEnum.NO.getValue());
            addMouldInfo(enableContinueMouldInfoList, mouldInfo);
            singleMouldProduction(mouldInfo, productionContext, continuePlan);
            return;
        }
        List<MouldInfoVO> anotherMouldList = enableMouldList.stream().filter(findMouldInfo -> !mouldInfo.getMouldCode().equals(findMouldInfo.getMouldCode())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(anotherMouldList)) {
            mouldInfo.setIsContinue(YesOrNoEnum.NO.getValue());
            addMouldInfo(enableContinueMouldInfoList, mouldInfo);
            singleMouldProduction(mouldInfo, productionContext, continuePlan);
            return;
        }
        //拼模的模具，则可用
        Map<String, MouldInfoVO> anotherMouldMap = anotherMouldList.stream().collect(Collectors.toMap(MouldInfoVO::getMouldCode, Function.identity()));
        MouldInfoVO anotherMouldInfo = anotherMouldMap.get(anotherMouldCode);
        if (null != anotherMouldInfo) {
            List<MouldInfoVO> productionMouldList = new ArrayList<>();
            productionMouldList.add(mouldInfo);
            productionMouldList.add(anotherMouldInfo);
            addMouldInfo(enableContinueMouldInfoList, mouldInfo);
            addMouldInfo(enableContinueMouldInfoList, anotherMouldInfo);
            doubleMouldProduction(productionGroupInfo, productionMouldList, productionContext, continuePlan);
            return;
        }
        //不是拼模的，则需要剔除续作模具
        List<MouldInfoVO> noContinueMouldList = anotherMouldList.stream().filter(findMouldInfo -> !YesOrNoEnum.YES.getValue().equals(findMouldInfo.getIsContinue())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(noContinueMouldList)) {
            mouldInfo.setIsContinue(YesOrNoEnum.NO.getValue());
            addMouldInfo(enableContinueMouldInfoList, mouldInfo);
            singleMouldProduction(mouldInfo, productionContext, continuePlan);
            return;
        }
        MouldInfoVO otherMouldInfo = noContinueMouldList.get(0);
        List<MouldInfoVO> productionMouldList = new ArrayList<>();
        productionMouldList.add(mouldInfo);
        productionMouldList.add(otherMouldInfo);
        addMouldInfo(enableContinueMouldInfoList, mouldInfo);
        addMouldInfo(enableContinueMouldInfoList, otherMouldInfo);
        doubleMouldProduction(productionGroupInfo, productionMouldList, productionContext, continuePlan);
    }

    /**
     * 多模具排产
     *
     * @param continueMouldList 可用的续作模具
     * @param productionContext 排产上下文
     * @param continuePlan      续作计划
     */
    private void multiMouldProduction(List<MouldInfoVO> continueMouldList, ProductionContext productionContext, MonthPlanManufacturingRequirementVo continuePlan) {
        continueMouldList.stream().forEach(mouldInfo -> {
            mouldInfo.setProductionOrient(ProductionOrientEnum.FORWARD);
            if (null == mouldInfo.getBeginDay()) {
                mouldInfo.setBeginDay(ProductionConstant.MONTH_START_DAY);
            }
        });
        //排序--同一分组的一起
        continueMouldList.sort(Comparator.comparing(MouldInfoVO::getContinueProductionGroupValue).thenComparing(MouldInfoVO::getMouldCode));
        List<Integer> startDays = new ArrayList<>(continueMouldList.stream().map(MouldInfoVO::getBeginDay).collect(Collectors.toSet()));
        startDays.sort(Comparator.comparing(Integer::intValue));
        Integer startProductionDate = startDays.get(0);
        Map<String, MouldInfoVO> continueMouldMap = continueMouldList.stream().collect(Collectors.toMap(MouldInfoVO::getMouldCode, Function.identity()));
        Integer endDay = productionContext.getMonthDays();
        Long needProductionQty = continuePlan.getProductionQty();
        Long monthPlanId = continuePlan.getMonthPlanId();
        Integer mouldSize = continueMouldList.size();
        int groupCount = mouldSize / 2;
        int remainder = mouldSize % 2;
        MouldInfoVO lastMouldInfo = null;
        if (remainder != 0) {
            lastMouldInfo = continueMouldList.get(mouldSize - 1);
        }
        Map<String, ProductionInfoVo> finalProductionInfoMap = new HashMap<>();
        //逐日续作模具排产
        for (; startProductionDate <= endDay; startProductionDate++) {
            if (ProductionProcessUtils.isProductionEnd(remainder, needProductionQty)) {
                break;
            }
            needProductionQty = productionPlanByProductionDate(continueMouldList, productionContext, lastMouldInfo, finalProductionInfoMap, groupCount, needProductionQty, startProductionDate, continuePlan);
            if (needProductionQty <= BigDecimal.ZERO.longValue()) {
                break;
            }
        }
        //更新模具当前排产信息
        updateMouldCurrentProductionInfo(finalProductionInfoMap, continueMouldMap, continuePlan.getProductCode(), productionContext);
        if (needProductionQty <= 0) {
            //标记不可排产了
            continuePlan.setIsProduction(YesOrNoEnum.NO.getValue());
            continuePlan.setNoProductionQty(BigDecimal.ZERO.longValue());
            productionContext.addProductionFinishPlan(monthPlanId);
            return;
        }
        //双模排产不排单
        if (ProductionProcessUtils.isDoubleMouldNoProductionSingle(remainder, needProductionQty)) {
            String noProductionReason = JsonUtils.getLanguageJsonObject("alg.data.noProductionReason.doubleNoSingle").toString();
            continuePlan.setNoProductionReason(noProductionReason);
            //标记不可排产了
            continuePlan.setIsProduction(YesOrNoEnum.NO.getValue());
            continuePlan.setNoProductionQty(needProductionQty);
            productionContext.addProductionFinishPlan(monthPlanId);
            return;
        }
        continuePlan.setProductionQty(needProductionQty);
    }

    /**
     * 根据续作规格的总需求量，测算续作模具是否需要减量
     *
     * @param continueMouldList 现有的续作模具
     * @param productionContext 排产上下文
     * @param continuePlan      续作规格计划(包含总排产量)
     * @return
     */
    private List<MouldInfoVO> getRealContinueMouldList(List<MouldInfoVO> continueMouldList, ProductionContext productionContext, MonthPlanManufacturingRequirementVo continuePlan) {
        if (CollectionUtils.isEmpty(continueMouldList)) {
            return continueMouldList;
        }
        int lastMonthContinueNumber = continueMouldList.size();
        //超过两模才进行测算
        int thresholdNumber = BigDecimal.ONE.intValue() + BigDecimal.ONE.intValue();
        if (lastMonthContinueNumber <= thresholdNumber) {
            return continueMouldList;
        }
        //获取单模月产能
        BigDecimal singleCuringTime = ProductUtils.getSingleCuringTime(continuePlan, productionContext);
        Long monthCapacity = MouldUtils.getSingleMonthMouldCapacity(productionContext, singleCuringTime);
        if (null == monthCapacity && monthCapacity <= BigDecimal.ZERO.longValue()) {
            return continueMouldList;
        }
        Long summaryQty = continuePlan.getSummaryProductionQty();
        int enableSize = BigDecimal.valueOf(summaryQty).divide(BigDecimal.valueOf(monthCapacity), 0, RoundingMode.UP).intValue();
        if (enableSize >= lastMonthContinueNumber) {
            return continueMouldList;
        }
        int remainder = enableSize % ProductionConstant.DOUBLE_MOULD_QTY;
        if (remainder != BigDecimal.ZERO.intValue()) {
            enableSize = enableSize + BigDecimal.ONE.intValue();
        }
        if (enableSize >= lastMonthContinueNumber) {
            return continueMouldList;
        }
        List<MouldInfoVO> realEnableList = new ArrayList<>();
        for (int index = 0; index < enableSize; index++) {
            realEnableList.add(continueMouldList.get(index));
        }
        return realEnableList;
    }

    /**
     * 单模在productionDate 排产
     *
     * @param mouldInfo
     * @param productionContext
     * @param continuePlan
     */
    private void singleMouldProduction(MouldInfoVO mouldInfo, ProductionContext productionContext, MonthPlanManufacturingRequirementVo continuePlan) {
        ProductionGroupInfoDto productionGroupInfo = ProductionGroupUtils.buildProductionGroupInfoByContinueMould(continuePlan, mouldInfo, productionContext);
        Long needProductionQty = continuePlan.getProductionQty();
        //续作模具，排产方向都定为正向排产
        mouldInfo.setProductionOrient(ProductionOrientEnum.FORWARD);
        if (null == mouldInfo.getBeginDay()) {
            mouldInfo.setBeginDay(ProductionConstant.MONTH_START_DAY);
        }
        Integer startProductionDate = mouldInfo.getBeginDay();
        Integer endDay = productionContext.getMonthDays();
        BigDecimal singleCuringTime = ProductUtils.getSingleCuringTime(continuePlan, productionContext);
        Long monthPlanId = continuePlan.getMonthPlanId();
        ProductionInfoVo finalProductionInfo = null;
        for (; startProductionDate <= endDay; startProductionDate++) {
            if (needProductionQty <= BigDecimal.ZERO.longValue()) {
                break;
            }
            ContinueMouldProductionHelper helper = new ContinueMouldProductionHelper(startProductionDate, continuePlan, singleCuringTime, needProductionQty);
            ProductionInfoVo productionInfo = MouldUtils.productionContinueMould(mouldInfo, helper, productionContext);
            if (null == productionInfo) {
                continue;
            }
            Long productionQty = productionInfo.getProductionQty();
            //排产流程日志记录
            ProductionLogUtils.addContinueMouldProductionLog(productionContext, MouldProductionLogType.CONTINUE_MOULD_GENERAL_LOG, continuePlan, mouldInfo.getMouldCode(), startProductionDate, productionQty);
            //剩余还需排产量
            Long leftOverNeedProductionQty = needProductionQty - productionQty;
            finalProductionInfo = productionInfo;
            //更新模具排产信息--模具排产列表等 20250716 ZLT 更新排产分组信息
            MouldTableInfoDto mouldTable = ProductionGroupUtils.getSelectedMouldTableInfoByContinue(productionGroupInfo, mouldInfo.getMouldCode());
            updateMouldDayProductionInfo(mouldTable, mouldInfo, continuePlan, productionInfo, productionContext, true);
            productionGroupInfo.setEmptyGroup(false);
            //20251011 ZLT 存储排产模具数据
            List<MouldInfoVO> doubleMouldList = new ArrayList<>();
            doubleMouldList.add(mouldInfo);
            handlerProductionMouldQty(productionContext, helper, leftOverNeedProductionQty, doubleMouldList);
            //更新需排产量
            needProductionQty = leftOverNeedProductionQty;
        }
        //排产流程日志记录 ===续作计划:%s 使用续作模具:%s 排产，排产前需排产量 %d 排产后还需排产量:%d
        ProductionLogUtils.addResultContinuePlanProductionByContinueMouldLog(productionContext, continuePlan, mouldInfo.getMouldCode(), needProductionQty);
        //更新模具当前排产信息
        if (null != finalProductionInfo) {
            MouldUtils.setMouldCurrentProductionInfo(mouldInfo, finalProductionInfo, continuePlan.getProductCode(), productionContext);
        }
        if (needProductionQty <= BigDecimal.ZERO.longValue()) {
            //标记不可排产了
            continuePlan.setIsProduction(YesOrNoEnum.NO.getValue());
            continuePlan.setNoProductionQty(BigDecimal.ZERO.longValue());
            productionContext.addProductionFinishPlan(monthPlanId);
            return;
        }
        continuePlan.setProductionQty(needProductionQty);
    }

    /**
     * 是否对计划连续SAP的上一个续作计划分组排产
     * true 延续 false表示不延续
     *
     * @param productionGroupInfo         分组排产信息
     * @param productionContext           排产上下文
     * @param continuePlan                下一个续作计划
     * @param mouldInfo                   续作模具
     * @param enableContinueMouldInfoList 存储已排产模具集合
     */
    private boolean continuePreviousPlan(ProductionGroupInfoDto productionGroupInfo, ProductionContext productionContext, MonthPlanManufacturingRequirementVo continuePlan, MouldInfoVO mouldInfo, List<MouldInfoVO> enableContinueMouldInfoList) {
        List<MouldTableInfoDto> mouldTableInfoList = productionGroupInfo.getMouldTableInfoList();
        Set<String> mouldCodeSet = new HashSet<>();
        mouldTableInfoList.stream().forEach(mouldTableInfo -> {
            if (null == mouldTableInfo.getLastProductionInfo()) {
                return;
            }
            mouldCodeSet.add(mouldTableInfo.getLastProductionInfo().getMouldCode());
        });
        if (CollectionUtils.isEmpty(mouldCodeSet)) {
            return false;
        }
        if (mouldCodeSet.size() > BigDecimal.ONE.intValue()) {
            List<MouldInfoVO> productionMouldList = new ArrayList<>();
            mouldCodeSet.stream().forEach(mouldCode -> {
                productionMouldList.add(productionContext.getMouldInfoMap().get(mouldCode));
            });
            doubleMouldProduction(productionGroupInfo, productionMouldList, productionContext, continuePlan);
            return true;
        }
        mouldInfo.setIsContinue(YesOrNoEnum.NO.getValue());
        addMouldInfo(enableContinueMouldInfoList, mouldInfo);
        singleMouldProduction(mouldInfo, productionContext, continuePlan);
        return true;
    }

    /**
     * 拼模单模，当另外一个规格没有计划量时，根据SAP模具关系，转化为双模进行排产
     * 已经确定了排产分组，和排产模具，因之前是拼模，则相当于另外一副模需要换模，
     * 故而需要同时换规格的产能消耗
     *
     * @param productionGroupInfo 排产分组
     * @param doubleMouldInfoList 双模
     * @param productionContext   排产上下文
     * @param continuePlan        续作计划
     */
    private void doubleMouldProduction(ProductionGroupInfoDto productionGroupInfo, List<MouldInfoVO> doubleMouldInfoList, ProductionContext productionContext, MonthPlanManufacturingRequirementVo continuePlan) {
        Long needProductionQty = continuePlan.getProductionQty();
        MouldInfoVO first = doubleMouldInfoList.get(0);
        MouldInfoVO second = doubleMouldInfoList.get(1);
        String firstMouldCode = first.getMouldCode();
        String secondMouldCode = second.getMouldCode();
        String mouldInfo = String.format("[%s、%s]", firstMouldCode, secondMouldCode);
        Map<String, MouldInfoVO> continueMouldMap = doubleMouldInfoList.stream().collect(Collectors.toMap(MouldInfoVO::getMouldCode, Function.identity()));
        //排产方向都定为正向排产
        doubleMouldInfoList.stream().forEach(productionMouldInfo -> {
            productionMouldInfo.setIsContinue(YesOrNoEnum.NO.getValue());
            productionMouldInfo.setProductionOrient(ProductionOrientEnum.FORWARD);
            if (null == productionMouldInfo.getBeginDay()) {
                productionMouldInfo.setBeginDay(ProductionConstant.MONTH_START_DAY);
            }
        });
        Map<String, ProductionInfoVo> finalProductionInfoMap = new HashMap<>();
        //取得最早日期
        List<Integer> startDays = new ArrayList<>(doubleMouldInfoList.stream().map(MouldInfoVO::getBeginDay).collect(Collectors.toSet()));
        startDays.sort(Comparator.comparing(Integer::intValue));
        Integer startProductionDate = startDays.get(0);
        Integer endDay = productionContext.getMonthDays();
        BigDecimal singleCuringTime = ProductUtils.getSingleCuringTime(continuePlan, productionContext);
        Long monthPlanId = continuePlan.getMonthPlanId();
        for (; startProductionDate <= endDay; startProductionDate++) {
            if (needProductionQty <= BigDecimal.ONE.longValue()) {
                break;
            }
            ContinueMouldProductionHelper helper = new ContinueMouldProductionHelper(startProductionDate, continuePlan, singleCuringTime, needProductionQty);
            Map<String, ProductionInfoVo> productionResult = MouldUtils.productionContinueDoubleMould(first, second, helper, productionContext);
            if (null == productionResult) {
                continue;
            }
            ProductionInfoVo firstProductionInfo = productionResult.get(firstMouldCode);
            ProductionInfoVo secondProductionInfo = productionResult.get(secondMouldCode);
            //双模单日总排产量
            Long productionQty = firstProductionInfo.getProductionQty() + secondProductionInfo.getProductionQty();
            //排产流程日志
            ProductionLogUtils.addContinueMouldProductionLog(productionContext, MouldProductionLogType.CONTINUE_MOULD_GENERAL_LOG, continuePlan, mouldInfo, startProductionDate, productionQty);
            //剩余还需排产量
            Long leftOverNeedProductionQty = needProductionQty - productionQty;
            finalProductionInfoMap.put(firstMouldCode, firstProductionInfo);
            finalProductionInfoMap.put(secondMouldCode, secondProductionInfo);
            //更新模具日排产信息 20250716 ZLT 更新排产分组信息
            MouldTableInfoDto firstMouldTable = ProductionGroupUtils.getSelectedMouldTableInfoByContinue(productionGroupInfo, first.getMouldCode());
            updateMouldDayProductionInfo(firstMouldTable, first, continuePlan, firstProductionInfo, productionContext, true);
            productionGroupInfo.setEmptyGroup(false);
            MouldTableInfoDto secondMouldTable = ProductionGroupUtils.getSelectedMouldTableInfoByContinue(productionGroupInfo, second.getMouldCode());
            updateMouldDayProductionInfo(secondMouldTable, second, continuePlan, secondProductionInfo, productionContext, true);
            productionGroupInfo.setEmptyGroup(false);
            //20251011 ZLT 存储排产模具数据
            handlerProductionMouldQty(productionContext, helper, leftOverNeedProductionQty, doubleMouldInfoList);
            //更新需排产量
            needProductionQty = leftOverNeedProductionQty;
        }
        //排产流程日志记录 ===续作计划:%s 使用续作模具:%s 排产，排产前需排产量 %d 排产后还需排产量:%d
        ProductionLogUtils.addResultContinuePlanProductionByContinueMouldLog(productionContext, continuePlan, mouldInfo, needProductionQty);
        //更新模具当前排产信息
        updateMouldCurrentProductionInfo(finalProductionInfoMap, continueMouldMap, continuePlan.getProductCode(), productionContext);
        if (needProductionQty <= BigDecimal.ZERO.longValue()) {
            //标记不可排产了
            continuePlan.setIsProduction(YesOrNoEnum.NO.getValue());
            continuePlan.setNoProductionQty(BigDecimal.ZERO.longValue());
            productionContext.addProductionFinishPlan(monthPlanId);
            return;
        }
        if (needProductionQty == BigDecimal.ONE.longValue()) {
            //双模排产不排单
            String noProductionReason = JsonUtils.getLanguageJsonObject("alg.data.noProductionReason.doubleNoSingle").toString();
            continuePlan.setNoProductionReason(noProductionReason);
            //标记不可排产了
            continuePlan.setIsProduction(YesOrNoEnum.NO.getValue());
            continuePlan.setNoProductionQty(needProductionQty);
            productionContext.addProductionFinishPlan(monthPlanId);
            return;
        }
        continuePlan.setProductionQty(needProductionQty);
    }

    /**
     * 续作计划在startProductionDate日使用续作模具排产
     *
     * @param continueMouldList      续作模具列表
     * @param productionContext      排产上下文
     * @param lastMouldInfo          单数模具--最后一副模具
     * @param finalProductionInfoMap 最后排产信息
     * @param groupCount             双模排产组数
     * @param needProductionQty      还需排产量
     * @param startProductionDate    排产日
     * @param continuePlan           排产的续作计划
     * @return
     */
    private Long productionPlanByProductionDate(List<MouldInfoVO> continueMouldList, ProductionContext productionContext, MouldInfoVO lastMouldInfo, Map<String, ProductionInfoVo> finalProductionInfoMap, int groupCount, Long needProductionQty, Integer startProductionDate, MonthPlanManufacturingRequirementVo continuePlan) {
        BigDecimal singleCuringTime = ProductUtils.getSingleCuringTime(continuePlan, productionContext);
        //先两副两副满排
        for (int index = 0; index < groupCount; index++) {
            if (needProductionQty <= BigDecimal.ONE.longValue()) {
                break;
            }
            ContinueMouldProductionHelper helper = new ContinueMouldProductionHelper(startProductionDate, continuePlan, singleCuringTime, needProductionQty);
            //剩余还需排产量
            Long leftOverNeedProductionQty = doubleProductionContinueMould(index, helper, continueMouldList, finalProductionInfoMap, productionContext);
            if (null == leftOverNeedProductionQty) {
                continue;
            }
            //更新需排产量
            needProductionQty = leftOverNeedProductionQty;
        }
        //没有排产量，则最后一副无需排产
        if (needProductionQty <= BigDecimal.ZERO.longValue()) {
            return needProductionQty;
        }
        if (null == lastMouldInfo) {
            return needProductionQty;
        }
        //获取排产分组
        ProductionGroupInfoDto productionGroupInfo = ProductionGroupUtils.buildProductionGroupInfoByContinueMould(continuePlan, lastMouldInfo, productionContext);
        //构建续作模具排产信息
        ContinueMouldProductionHelper helper = new ContinueMouldProductionHelper(startProductionDate, continuePlan, singleCuringTime, needProductionQty);
        //得到排产量
        ProductionInfoVo productionInfo = MouldUtils.productionContinueMould(lastMouldInfo, helper, productionContext);
        if (null == productionInfo) {
            return needProductionQty;
        }
        //真实排产量
        Long productionQty = productionInfo.getProductionQty();
        //排产流程日志
        ProductionLogUtils.addContinueMouldProductionLog(productionContext, MouldProductionLogType.CONTINUE_MOULD_GENERAL_LOG, continuePlan, lastMouldInfo.getMouldCode(), startProductionDate, productionQty);
        //剩余还需排产量
        Long leftOverNeedProductionQty = needProductionQty - productionQty;
        finalProductionInfoMap.put(lastMouldInfo.getMouldCode(), productionInfo);
        //更新模具排产信息--模具排产列表等 20250716 ZLT 更新排产分组信息
        MouldTableInfoDto mouldTable = ProductionGroupUtils.getSelectedMouldTableInfoByContinue(productionGroupInfo, lastMouldInfo.getMouldCode());
        updateMouldDayProductionInfo(mouldTable, lastMouldInfo, continuePlan, productionInfo, productionContext, true);
        productionGroupInfo.setEmptyGroup(false);
        //20251011 ZLT 存储排产模具数据
        List<MouldInfoVO> doubleMouldList = new ArrayList<>();
        doubleMouldList.add(lastMouldInfo);
        handlerProductionMouldQty(productionContext, helper, leftOverNeedProductionQty, doubleMouldList);
        //更新需排产量
        needProductionQty = leftOverNeedProductionQty;
        return needProductionQty;
    }

    /**
     * 双模排产量
     *
     * @param index                  分组模具
     * @param helper                 排产计划信息
     * @param allContinueMouldList   所有续作模具
     * @param finalProductionInfoMap 所有模具最后排产信息
     * @param productionContext      排产上下文
     * @return
     */
    private Long doubleProductionContinueMould(int index, ContinueMouldProductionHelper helper, List<MouldInfoVO> allContinueMouldList, Map<String, ProductionInfoVo> finalProductionInfoMap, ProductionContext productionContext) {
        Integer startProductionDate = helper.getStartProductionDate();
        MonthPlanManufacturingRequirementVo continuePlan = helper.getContinuePlan();
        Long needProductionQty = helper.getNeedProductionQty();
        int startIndex = index * 2;
        int endIndex = (index + 1) * 2;
        List<MouldInfoVO> doubleMouldList = allContinueMouldList.subList(startIndex, endIndex);
        MouldInfoVO first = doubleMouldList.get(0);
        MouldInfoVO second = doubleMouldList.get(1);
        ProductionGroupInfoDto firstProductionGroup = ProductionGroupUtils.buildProductionGroupInfoByContinueMould(continuePlan, first, productionContext);
        ProductionGroupInfoDto secondProductionGroup = ProductionGroupUtils.buildProductionGroupInfoByContinueMould(continuePlan, second, productionContext);
        String firstMouldCode = first.getMouldCode();
        String secondMouldCode = second.getMouldCode();
        Map<String, ProductionInfoVo> productionResult = MouldUtils.productionContinueDoubleMould(first, second, helper, productionContext);
        if (null == productionResult) {
            return null;
        }
        String mouldInfo = String.format("[%s、%s]", firstMouldCode, secondMouldCode);
        ProductionInfoVo firstProductionInfo = productionResult.get(firstMouldCode);
        ProductionInfoVo secondProductionInfo = productionResult.get(secondMouldCode);
        //双模单日总排产量
        Long productionQty = firstProductionInfo.getProductionQty() + secondProductionInfo.getProductionQty();
        //排产流程日志
        ProductionLogUtils.addContinueMouldProductionLog(productionContext, MouldProductionLogType.CONTINUE_MOULD_GENERAL_LOG, continuePlan, mouldInfo, startProductionDate, productionQty);
        //剩余还需排产量
        Long leftOverNeedProductionQty = needProductionQty - productionQty;
        finalProductionInfoMap.put(firstMouldCode, firstProductionInfo);
        finalProductionInfoMap.put(secondMouldCode, secondProductionInfo);
        //更新模具日排产信息 20250716 ZLT 更新排产分组信息
        MouldTableInfoDto firstMouldTable = ProductionGroupUtils.getSelectedMouldTableInfoByContinue(firstProductionGroup, first.getMouldCode());
        updateMouldDayProductionInfo(firstMouldTable, first, continuePlan, firstProductionInfo, productionContext, true);
        firstProductionGroup.setEmptyGroup(false);
        //20250716 ZLT 更新排产分组信息
        MouldTableInfoDto secondMouldTable = ProductionGroupUtils.getSelectedMouldTableInfoByContinue(secondProductionGroup, second.getMouldCode());
        updateMouldDayProductionInfo(secondMouldTable, second, continuePlan, secondProductionInfo, productionContext, true);
        secondProductionGroup.setEmptyGroup(false);
        //20251011 ZLT 存储排产模具数据
        handlerProductionMouldQty(productionContext, helper, leftOverNeedProductionQty, doubleMouldList);
        return leftOverNeedProductionQty;
    }

    /**
     * 处理排产模具数量
     *
     * @param productionContext         排产上下文
     * @param helper                    续作排产辅助类
     * @param leftOverNeedProductionQty 剩余排产量
     * @param mouldList                 排产模具信息
     */
    private void handlerProductionMouldQty(ProductionContext productionContext, ContinueMouldProductionHelper helper, Long leftOverNeedProductionQty, List<MouldInfoVO> mouldList) {
        MonthPlanManufacturingRequirementVo productionPlan = helper.getContinuePlan();
        Integer startProductionDate = helper.getStartProductionDate();
        handlerProductionMouldQty(productionContext, ProductionOrientEnum.FORWARD, productionPlan, startProductionDate, leftOverNeedProductionQty, mouldList, true);
//        List<MonthPlanManufacturingRequirementVo> otherNoProductionPlanList = ProductionPlanUtils.getSameProductCodeNoProductionPlanList(productionContext, productionPlan);
//        int productionMouldQty = mouldList.size();
//        boolean isDouble = (productionMouldQty == 2);
//        //判断productCode 是否到了收尾
//        if (ProductionPlanUtils.isEndByProductCode(isDouble, leftOverNeedProductionQty, otherNoProductionPlanList)) {
//            return;
//        }
//        //增加天的排产模具数
//        productionContext.addDayProductionMouldQty(ProductionOrientEnum.FORWARD, startProductionDate, productionPlan, true, productionMouldQty);
    }

    /**
     * 续作满月排产
     *
     * @param productionContext
     * @param mouldInfo
     * @param mouldDayProduction
     * @param lastDay
     */
    private void fullMonthProduction(ProductionContext productionContext, MouldInfoVO mouldInfo, MouldDayProductionVo mouldDayProduction, Integer lastDay) {
        Long monthPlanId = mouldDayProduction.getMonthPlanId();
        MonthPlanManufacturingRequirementVo lastPlan = productionContext.getMonthPlanInitMap().get(monthPlanId);
        Integer maxProductionDate = productionContext.getMonthDays();
        //包含间隔时间
        BigDecimal singleCuringTime = ProductUtils.getSingleCuringTime(lastPlan, productionContext);
        //最后一天是否需要提量
        BigDecimal lastDayLeftOverTime = mouldInfo.getProductionDayList().get(lastDay);
        //向下取整
        Long addQty = lastDayLeftOverTime.divide(singleCuringTime, 0, RoundingMode.DOWN).longValue();
        mouldDayProduction.setProductionQty(mouldDayProduction.getProductionQty() + addQty);
        if (maxProductionDate.equals(lastDay)) {
            return;
        }
        ProductionGroupInfoDto productionGroup = ProductionGroupUtils.buildProductionGroupInfoByContinueMould(lastPlan, mouldInfo, productionContext);
        String sizeCapacityKey = lastPlan.getSizeCapacityGroupKey();
        ProductionLogUtils.addMouldFullMonthProductionLog(productionContext, MouldProductionLogType.CONTINUE_MOULD_GENERAL_LOG, lastPlan, mouldInfo.getMouldCode());
        Integer startProductionDate = lastDay + 1;
        for (; startProductionDate <= maxProductionDate; startProductionDate++) {
            if (mouldInfo.getNoProductionDayList().containsKey(startProductionDate)) {
                continue;
            }
            BigDecimal leftOverSecond = mouldInfo.getProductionDayList().get(startProductionDate);
            Long dayQty = leftOverSecond.divide(singleCuringTime, 0, RoundingMode.DOWN).longValue();
            MouldDayProductionVo dayProduction = buildNewProduction(mouldDayProduction, startProductionDate, dayQty);
            //20250421 增加每日排产规格数
            productionContext.addDayProductNumber(mouldInfo.getProductionOrient(), startProductionDate, lastPlan.getProductCode(), lastPlan, true);
            //20250414 增加每日最大排产量控制 增加每日排产量汇总
            productionContext.addDayProductionQty(startProductionDate, sizeCapacityKey, dayQty);
            List<MouldDayProductionVo> dayProductionList = mouldInfo.getDayProductionMap().get(startProductionDate);
            if (null == dayProductionList) {
                dayProductionList = new ArrayList<>();
            }
            dayProductionList.add(dayProduction);
            mouldInfo.getDayProductionMap().put(startProductionDate, dayProductionList);
            //增加使用硫化时间、剩余硫化时间
            BigDecimal usedCuringTime = singleCuringTime.multiply(BigDecimal.valueOf(dayQty));
            mouldInfo.setUsedSeconds(mouldInfo.getUsedSeconds().add(usedCuringTime));
            mouldInfo.setLeftOverSeconds(mouldInfo.getTotalSeconds().subtract(mouldInfo.getUsedSeconds()));
            //更新模具的排产日剩余硫化时间
            BigDecimal dayLeftOverSeconds = leftOverSecond.subtract(usedCuringTime);
            mouldInfo.getProductionDayList().put(startProductionDate, dayLeftOverSeconds);
            //剩余硫化时间小于单条硫化时间，则表示当前排产日已经排产完毕(同规格都不行，换规格更不行)
            mouldInfo.getProductionFinishDayList().add(startProductionDate);
            //20250716 ZLT 更新排产分组信息
            MouldTableInfoDto selectedTable = ProductionGroupUtils.getSelectedMouldTableInfoByContinue(productionGroup, mouldInfo.getMouldCode());
            if (null != productionGroup && null != selectedTable) {
                selectedTable.setLastProductionInfo(dayProduction);
                selectedTable.getProductionList().add(dayProduction);
                selectedTable.getProductionDateSet().add(startProductionDate);
            }
            //20251011 ZLT 每日排产模具数
            productionContext.addDayProductionMouldQty(ProductionOrientEnum.FORWARD, startProductionDate, lastPlan, true, BigDecimal.ONE.intValue());
        }
    }

    /**
     * 加入到排产模具列表中
     *
     * @param enableContinueMouldInfoList 排产模具列表
     * @param mouldInfo                   模具信息
     */
    private void addMouldInfo(List<MouldInfoVO> enableContinueMouldInfoList, MouldInfoVO mouldInfo) {
        if (null == enableContinueMouldInfoList) {
            return;
        }
        if (CollectionUtils.isEmpty(enableContinueMouldInfoList)) {
            enableContinueMouldInfoList.add(mouldInfo);
            return;
        }
        Set<String> mouldCodeSet = enableContinueMouldInfoList.stream().map(MouldInfoVO::getMouldCode).collect(Collectors.toSet());
        if (mouldCodeSet.contains(mouldInfo.getMouldCode())) {
            return;
        }
        enableContinueMouldInfoList.add(mouldInfo);
    }

    /**
     * 批量更新模具当前信息
     *
     * @param finalProductionInfoMap 需要更新的数据集合
     * @param continueMouldMap       需要更新数据模具集合
     * @param productCode            SAP代码
     * @param productionContext      排产上下文
     */
    private void updateMouldCurrentProductionInfo(Map<String, ProductionInfoVo> finalProductionInfoMap, Map<String, MouldInfoVO> continueMouldMap, String productCode, ProductionContext productionContext) {
        if (CollectionUtils.isEmpty(finalProductionInfoMap)) {
            return;
        }
        //更新模具当前排产信息
        finalProductionInfoMap.entrySet().forEach(entry -> {
            String mouldCode = entry.getKey();
            ProductionInfoVo finalProductionInfo = entry.getValue();
            if (null == finalProductionInfo) {
                return;
            }
            MouldUtils.setMouldCurrentProductionInfo(continueMouldMap.get(mouldCode), finalProductionInfo, productCode, productionContext);
        });
    }

    /**
     * 获取排产模具的最后一个排产日
     *
     * @param continueMouldInfo 续作模具信息
     * @return
     */
    private Integer getLastProductionDay(MouldInfoVO continueMouldInfo) {
        Map<Integer, List<MouldDayProductionVo>> dayProductionMap = continueMouldInfo.getDayProductionMap();
        if (CollectionUtils.isEmpty(dayProductionMap)) {
            return BigDecimal.ZERO.intValue();
        }
        List<Integer> productionDayList = new ArrayList<>(dayProductionMap.keySet());
        Collections.sort(productionDayList);
        return productionDayList.get(productionDayList.size() - 1);
    }

    /**
     * 构建日排产信息
     *
     * @param planLast       排产计划
     * @param productionDate 排产日
     * @param productionQty  排产量
     * @return
     */
    private MouldDayProductionVo buildNewProduction(MouldDayProductionVo planLast, Integer productionDate, Long productionQty) {
        MouldDayProductionVo dayProduction = new MouldDayProductionVo();
        BeanUtils.copyProperties(planLast, dayProduction);
        dayProduction.setProductionDate(productionDate);
        dayProduction.setProductionQty(productionQty);
        dayProduction.setProductionType(ProductionTypeEnum.GENERAL_DAY.getType());
        return dayProduction;
    }
}

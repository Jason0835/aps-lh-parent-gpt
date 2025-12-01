package com.zlt.aps.factory.utils;

import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.enums.ProductionOrientEnum;
import com.zlt.aps.factory.scheduling.ContinueMouldProductionHelper;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.monthplan.api.domain.vo.NoProductionDayMouldVo;
import com.zlt.aps.monthplan.api.enums.ProductionTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模具相关业务方法类
 *
 * @author ZLT
 * @date 20250312
 */
@Slf4j
public class MouldUtils {

    /**
     * 构建模具列表的排序比较器
     * 按续作优先 > 已排优先 > 物料关联数 > 模具 > 分组值 > 剩余硫化时间多 > 模具编号
     * 共用模具越多的越放置在后面使用，防止前面使用完后，后续规格没有模具可用
     *
     * @return
     */
    public static Comparator buildMouldSortComparator() {
        Comparator comparator = Comparator.comparing(MouldInfoVO::getIsContinue, Comparator.reverseOrder()).thenComparing(MouldInfoVO::getIsProduction, Comparator.reverseOrder()).thenComparing(MouldInfoVO::getAssocaiationCount).thenComparing(MouldInfoVO::getMouldNo).thenComparing(MouldInfoVO::getGroupValue, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(MouldInfoVO::getLeftOverSeconds, Comparator.reverseOrder()).thenComparing(MouldInfoVO::getMouldCode);
        return comparator;
    }

    /**
     * 续作需求计划抢占续作模具产能
     *
     * @param continueSaleRequirementPlanList 续作需求计划
     * @param productionContext               排产上下文
     */
    public static void continueSaleRequirePreemptCapacity(List<MonthPlanManufacturingRequirementVo> continueSaleRequirementPlanList, ProductionContext productionContext) {
        if (CollectionUtils.isEmpty(continueSaleRequirementPlanList)) {
            return;
        }
        continueSaleRequirementPlanList.sort(Comparator.comparing(MonthPlanManufacturingRequirementVo::getProductionSequence));
        Map<String, MouldInfoVO> allMouldMap = productionContext.getMouldInfoMap();
        continueSaleRequirementPlanList.stream().forEach(continueSaleRequirementPlan -> {
            String productCode = continueSaleRequirementPlan.getProductCode();
            List<MouldProductionProductVo> continueMouldList = productionContext.getContinueProductMap().get(productCode);
            if (CollectionUtils.isEmpty(continueMouldList)) {
                continueSaleRequirementPlan.setContinueMouldPreemptQty(BigDecimal.ZERO.longValue());
                return;
            }
            Set<String> mouldCodeSet = continueMouldList.stream().map(MouldProductionProductVo::getMouldCode).collect(Collectors.toSet());
            if (CollectionUtils.isEmpty(mouldCodeSet)) {
                continueSaleRequirementPlan.setContinueMouldPreemptQty(BigDecimal.ZERO.longValue());
                return;
            }
            BigDecimal singleCuringTime = ProductUtils.getSingleCuringTime(continueSaleRequirementPlan, productionContext);
            Map<String, MouldInfoVO> enableContinueMouldMap = MouldBaseUtils.getLeftOverMouldInfo(allMouldMap, mouldCodeSet, singleCuringTime);
            if (CollectionUtils.isEmpty(enableContinueMouldMap)) {
                continueSaleRequirementPlan.setContinueMouldPreemptQty(BigDecimal.ZERO.longValue());
                return;
            }
            int mouldSize = enableContinueMouldMap.size();
            continueSaleRequirementPlan.getPreemptMouldCodeSet().addAll(enableContinueMouldMap.keySet());
            Long needProductionQty = continueSaleRequirementPlan.getProductionQty();
            Long singleQty = MouldBaseUtils.preemptSingleMouldQty(enableContinueMouldMap, needProductionQty, singleCuringTime);
            BigDecimal realSinglePreemptTime = singleCuringTime.multiply(BigDecimal.valueOf(singleQty));
            continueSaleRequirementPlan.setContinueMouldPreemptQty(singleQty * mouldSize);
            //模具更新预占产能
            enableContinueMouldMap.entrySet().stream().forEach(entry -> {
                MouldInfoVO mouldInfo = entry.getValue();
                BigDecimal preemptLeftOverSeconds = mouldInfo.getPreemptLeftOverSeconds();
                mouldInfo.setPreemptLeftOverSeconds(preemptLeftOverSeconds.subtract(realSinglePreemptTime));
            });
        });
    }

    /**
     * 获取最优的模具列表,优先选择模具能满足的两副模具
     * todo 纯粹看剩余硫化时间不准确，每天剩余不足一条的硫化时间不能使用
     *
     * @param productionPlan   当前计划
     * @param enableMouldList  可用的模具列表
     * @param singleCuringTime 当前计划规格的可硫化时间
     * @return
     */
    public static List<MouldInfoVO> getOptimalMouldList(MonthPlanManufacturingRequirementVo productionPlan, List<MouldInfoVO> enableMouldList, BigDecimal singleCuringTime) {
        BigDecimal needLhTime = BigDecimalUtils.multiply(BigDecimal.valueOf(productionPlan.getProductionQty()), singleCuringTime);
        List<MouldInfoVO> productionMouldList = null;
        //有排产规格的模具优先选
        List<MouldInfoVO> productionProductCodeMouldInfoList = getProductionProductCodeMould(enableMouldList, productionPlan);
        if (!CollectionUtils.isEmpty(productionProductCodeMouldInfoList)) {
            productionMouldList = getSelectedMould(needLhTime, productionProductCodeMouldInfoList);
        }
        if (!CollectionUtils.isEmpty(productionMouldList)) {
            return productionMouldList;
        }
        return getSelectedMould(needLhTime, enableMouldList);
    }

    /**
     * 挑选有排产规格的模具
     *
     * @param maxEnableMouldList 排产最大模具列表
     * @param productionPlan     排产计划
     * @return
     */
    public static List<MouldInfoVO> getProductionProductCodeMould(List<MouldInfoVO> maxEnableMouldList, MonthPlanManufacturingRequirementVo productionPlan) {
        List<MouldInfoVO> productionProductCodeMouldInfoList = new ArrayList<>();
        maxEnableMouldList.stream().forEach(enableMould -> {
            if (mouldIsProductionProductCodeByPlan(enableMould, productionPlan)) {
                productionProductCodeMouldInfoList.add(enableMould);
            }
        });
        return productionProductCodeMouldInfoList;
    }

    /**
     * 续作模具在productionDate 可排产量
     *
     * @param mouldInfo         模具
     * @param helper            排产计划信息
     * @param productionContext 排产上下文
     * @return
     */
    public static ProductionInfoVo productionContinueMould(MouldInfoVO mouldInfo, ContinueMouldProductionHelper helper, ProductionContext productionContext) {
        Integer productionDate = helper.getStartProductionDate();
        MonthPlanManufacturingRequirementVo continuePlan = helper.getContinuePlan();
        Long needProductionQty = helper.getNeedProductionQty();
        BigDecimal singleCuringTime = helper.getSingleCuringTime();
        String productCode = continuePlan.getProductCode();
        //20250626 续作忽略规格数限制及产能限制--代码移除
        String sizeCapacityKey = continuePlan.getSizeCapacityGroupKey();
        //得到单日排产信息 20250903 续作排产标识-true
        DayProductionPlanInfoVo dayProductionPlanInfo = new DayProductionPlanInfoVo(continuePlan.getMonthPlanId(), productCode, sizeCapacityKey, productionDate, needProductionQty, singleCuringTime, true);
        ProductionInfoVo productionInfo = calculateProductionQty(mouldInfo, dayProductionPlanInfo, productionContext, false);
        return productionInfo;
    }

    /**
     * 续作模具在productionDate 可排产量
     *
     * @param first             第一副模具
     * @param second            第二副模具
     * @param helper            排产计划信息
     * @param productionContext 排产上下文
     * @return
     */
    public static Map<String, ProductionInfoVo> productionContinueDoubleMould(MouldInfoVO first, MouldInfoVO second, ContinueMouldProductionHelper helper, ProductionContext productionContext) {
        Integer productionDate = helper.getStartProductionDate();
        MonthPlanManufacturingRequirementVo continuePlan = helper.getContinuePlan();
        Long needProductionQty = helper.getNeedProductionQty();
        BigDecimal singleCuringTime = helper.getSingleCuringTime();
        String productCode = continuePlan.getProductCode();
        String sizeCapacityKey = continuePlan.getSizeCapacityGroupKey();
        List<MouldInfoVO> productionMouldList = new ArrayList<>();
        productionMouldList.add(first);
        productionMouldList.add(second);
        //双模排产量 20250903 续作排产标识-true
        DayProductionPlanInfoVo dayProductionPlanInfo = new DayProductionPlanInfoVo(continuePlan.getMonthPlanId(), productCode, sizeCapacityKey, productionDate, needProductionQty, singleCuringTime, true);
        Map<String, ProductionInfoVo> productionInfoMap = calculateProductionQty(productionMouldList, dayProductionPlanInfo, productionContext, false);
        return productionInfoMap;
    }

    /**
     * 检测模具在productionDate是否是不排产日
     *
     * @param productionDate 排产日
     * @param mouldInfo      模具信息
     * @return
     */
    public static boolean checkIsNoPlaningDay(int productionDate, MouldInfoVO mouldInfo) {
        Map<Integer, NoProductionDayMouldVo> noProductionDayList = mouldInfo.getNoProductionDayList();
        if (CollectionUtils.isEmpty(noProductionDayList)) {
            return false;
        }
        NoProductionDayMouldVo noProductionDayMould = noProductionDayList.get(productionDate);
        if (noProductionDayMould != null) {
            return true;
        }
        return false;
    }

    /**
     * 获取双模的最早间断日，理论只有一次间断，故而只判断一次
     *
     * @param productionContext 排产上下文
     * @param first             第一个模
     * @param second            第二个模
     * @return
     */
    public static Integer getIntervalDay(ProductionContext productionContext, MouldInfoVO first, MouldInfoVO second) {
        //月份最大天数
        int endDay = productionContext.getMonthDays();
        int intervalStartDay = endDay;
        //得到最早的间隔起始日--从1号开始
        for (Integer startDay = ProductionConstant.MONTH_START_DAY; startDay <= endDay; startDay++) {
            boolean firstIsIntervalDay = MouldUtils.isIntervalDay(startDay, first);
            boolean secondIsIntervalDay = MouldUtils.isIntervalDay(startDay, second);
            if (!firstIsIntervalDay && !secondIsIntervalDay) {
                continue;
            }
            if (intervalStartDay > startDay) {
                intervalStartDay = startDay;
            }
        }
        return intervalStartDay;
    }

    /**
     * 判断模具在productionDate是否已经间断了
     * 在productionDate没有排产，且本身不是不可排产日
     * 则就认为已经间断了
     *
     * @param productionDate 排产日
     * @param mouldInfo      模具
     * @return
     */
    public static boolean isIntervalDay(int productionDate, MouldInfoVO mouldInfo) {
        Map<Integer, List<MouldDayProductionVo>> mouldDayProductionMap = mouldInfo.getDayProductionMap();
        if (!CollectionUtils.isEmpty(mouldDayProductionMap)) {
            return false;
        }
        List<MouldDayProductionVo> mouldDayProductionList = mouldDayProductionMap.get(productionDate);
        if (!CollectionUtils.isEmpty(mouldDayProductionList)) {
            return false;
        }
        return !MouldUtils.checkIsNoPlaningDay(productionDate, mouldInfo);
    }

    /**
     * 20250411
     * 获取排产日的剩余可排产量--日最大可排产量限制
     *
     * @param productionDate       排产日
     * @param sizeCapacityGroupKey 寸口|*|成型法
     * @param productionContext    排产上下文
     * @return
     */
    public static Long getProductionDateLeftOverProductionQty(Integer productionDate, String sizeCapacityGroupKey, ProductionContext productionContext) {
        if (null == productionDate) {
            return Long.valueOf(Integer.MAX_VALUE);
        }
        if (productionDate < ProductionConstant.MONTH_START_DAY || productionDate > productionContext.getMonthDays()) {
            return BigDecimal.ZERO.longValue();
        }
        return productionContext.getDayLeftOverQty(productionDate, sizeCapacityGroupKey);
    }

    /**
     * 是否可连续排产两天
     * 需要剩余排产量>0 且下一天还有可剩余排产量
     *
     * @param isDoubleProduction           是否双模排产，双模排产允许剩余量为1
     * @param previousDateIsProduction     前一天是否排产
     * @param leftOverProductionQty        还剩余的排产量
     * @param nextDayLeftOverProductionQty 下一天还剩余可排产量
     * @return true 表示可连续 false表示不可
     */
    public static boolean isContinueProduction(boolean isDoubleProduction, boolean previousDateIsProduction, Long leftOverProductionQty, Long nextDayLeftOverProductionQty) {
        //前一天有排，则排，没有排则需要判断后一天
        if (previousDateIsProduction) {
            return true;
        }
        //双模，当天可排产完，则直接可连续排产
        if (isDoubleProduction && leftOverProductionQty <= 1) {
            return true;
        }
        //单模，单天可排产完，则直接可连续排产
        if (!isDoubleProduction && leftOverProductionQty <= 0) {
            return true;
        }
        //为空表示今天没有排，或是今天是停工日
        if (null == nextDayLeftOverProductionQty) {
            return true;
        }
        //双模下一天还有可排产量
        if (isDoubleProduction && nextDayLeftOverProductionQty > 1) {
            return true;
        }
        return nextDayLeftOverProductionQty > 0;
    }

    /**
     * 计算模具在productionDate排产的数量-双模
     * 出来的是双模的排产量
     * 预排无法判断洗模日，需预排处进行实现
     *
     * @param mouldInfoList     模具信息
     * @param dayProductionPlan 日排产信息
     * @param productionContext 排产上下文
     * @param isPreFlag         是否预排
     * @return
     */
    public static Map<String, ProductionInfoVo> calculateProductionQty(List<MouldInfoVO> mouldInfoList, DayProductionPlanInfoVo dayProductionPlan, ProductionContext productionContext, boolean isPreFlag) {
        Map<String, ProductionInfoVo> mouldProductionMap = new HashMap<>();
        MouldInfoVO first = mouldInfoList.get(0);
        MouldInfoVO second = mouldInfoList.get(1);
        Integer productionDate = dayProductionPlan.getProductionDate();
        /**
         * 判断能否双模排产--两模都是正常排产日则为双模排产，
         * 其中一模为停车日、维修日以及洗模日则表示排产日不为双模排产
         * 两模的排产模式-都是正常日排产
         *
         */
        boolean isDoubleProduction = false;
        ProductionInfoVo firstProductionResult = calculateProductionQty(first, dayProductionPlan, productionContext, isPreFlag);
        ProductionInfoVo secondProductionResult = calculateProductionQty(second, dayProductionPlan, productionContext, isPreFlag);
        if (firstProductionResult.getProductionType().isNormalProduction() && secondProductionResult.getProductionType().isNormalProduction()) {
            isDoubleProduction = true;
        }
        //不能正常双模排
        if (!isDoubleProduction) {
            mouldProductionMap.put(first.getMouldCode(), firstProductionResult);
            mouldProductionMap.put(second.getMouldCode(), secondProductionResult);
            return mouldProductionMap;
        }
        //双模排产，各模数量不一定一致，因为有些模被提前排产了
        Long firstQty = firstProductionResult.getProductionQty();
        Long secondQty = secondProductionResult.getProductionQty();
        BigDecimal singleCuringTime = dayProductionPlan.getSingleCuringTime();
//        MouldDayProductionLeftOverVo firstLeftOverInfo = getProductDateLeftOverInfo(first, dayProductionPlan.getProductCode(), productionDate, productionContext, isPreFlag);
//        MouldDayProductionLeftOverVo secondLeftOverInfo = getProductDateLeftOverInfo(first, dayProductionPlan.getProductCode(), productionDate, productionContext, isPreFlag);
        //取得偶数
        Long doubleNeedProductionQty = dayProductionPlan.getNeedProductionQty() / 2 * 2;
        Long sumProductionQty = firstQty + secondQty;
        Long doubleProductionQty = sumProductionQty / 2 * 2;
        //实际双模在排产日能排的量
        Long realProductionQty = Math.min(doubleProductionQty, doubleNeedProductionQty);
        //20250414 增加单天最大排产量控制，得到排产日剩余可排产量
        String sizeCapacityKey = dayProductionPlan.getSizeCapacityKey();
        Long dayLeftOverQty = productionContext.getDayLeftOverQty(productionDate, sizeCapacityKey);
        if (null != dayLeftOverQty && isPreFlag) {
            dayLeftOverQty = productionContext.getDayPreLeftOverQty(productionDate, sizeCapacityKey);
        }
        //20250620 判断是否已经连续排产--日剩余量不足以排产，则看前一天是否排产，如果已经排了，则直接排否则不能排
        MonthPlanManufacturingRequirementVo productionPlan = productionContext.getMonthPlanInitMap().get(dayProductionPlan.getMonthPlanId());
        DayProductionCapacityParityVo dayCapacityParity = getDayLimitCapacityInfo(productionContext, productionPlan, productionDate, realProductionQty, singleCuringTime, firstProductionResult, secondProductionResult);
        realProductionQty = getProductionQty(dayLeftOverQty, dayCapacityParity, productionContext, dayProductionPlan, isPreFlag, first, second);
        boolean isSKipProduction = false;
        if (ProductionConstant.SKIP_PRODUCTION.equals(realProductionQty)) {
            isSKipProduction = true;
            realProductionQty = BigDecimal.ZERO.longValue();
        }
//        realProductionQty = getProductionQty(dayLeftOverQty, realProductionQty, productionDate, productionContext, dayProductionPlan, isPreFlag, first, second);
        //取得偶数
        Long realSumProductionQty = realProductionQty / 2 * 2;
        Long singleRealProductionQty = realSumProductionQty / 2;
        if (firstQty > singleRealProductionQty && secondQty > singleRealProductionQty) {
            firstQty = singleRealProductionQty;
            secondQty = singleRealProductionQty;
        } else {
            Long singleLeftOverQty = sumProductionQty - realSumProductionQty;
            //数量大的取剩余的量
            if (firstQty < secondQty) {
                secondQty = secondQty - singleLeftOverQty;
            } else {
                firstQty = firstQty - singleLeftOverQty;
            }
        }
        //20250411 洗模不在是一整天，故而要加入洗模时间 硫化时间 = 单条硫化时间 * 硫化量 + 换规格时间 + 洗模时间
        setProductionResult(mouldProductionMap, isSKipProduction, singleCuringTime, productionDate, first, firstQty, firstProductionResult, second, secondQty, secondProductionResult);
        return mouldProductionMap;
    }

    /**
     * 重新设置排产结果信息
     *
     * @param mouldProductionMap     排产结果
     * @param isSKipProduction       是否跳过排产
     * @param singleCuringTime       单条硫化时间(包含间隔时间)
     * @param productionDate         排产日
     * @param first                  第一副模具
     * @param firstQty               第一副排产量
     * @param firstProductionResult  第一副初步排产信息
     * @param second                 第二副模具
     * @param secondQty              第二副排产量
     * @param secondProductionResult 第二副初步排产信息
     */
    private static void setProductionResult(Map<String, ProductionInfoVo> mouldProductionMap, boolean isSKipProduction, BigDecimal singleCuringTime, Integer productionDate, MouldInfoVO first, Long firstQty, ProductionInfoVo firstProductionResult, MouldInfoVO second, Long secondQty, ProductionInfoVo secondProductionResult) {
        BigDecimal firstChangeSubSecond;
        BigDecimal firstCleanMouldSubSecond;
        BigDecimal firstNextDaySubtractTime;
        BigDecimal secondChangeSubSecond;
        BigDecimal secondCleanMouldSubSecond;
        BigDecimal secondNextDaySubtractTime;
        if (isSKipProduction) {
            firstChangeSubSecond = BigDecimal.ZERO;
            firstCleanMouldSubSecond = BigDecimal.ZERO;
            firstNextDaySubtractTime = BigDecimal.ZERO;
            secondChangeSubSecond = BigDecimal.ZERO;
            secondCleanMouldSubSecond = BigDecimal.ZERO;
            secondNextDaySubtractTime = BigDecimal.ZERO;
        } else {
            firstChangeSubSecond = firstProductionResult.getChangeSubSecond();
            firstCleanMouldSubSecond = firstProductionResult.getCleanMouldSubSecond();
            firstNextDaySubtractTime = firstProductionResult.getNextDaySubtractTime();
            secondChangeSubSecond = secondProductionResult.getChangeSubSecond();
            secondCleanMouldSubSecond = secondProductionResult.getCleanMouldSubSecond();
            secondNextDaySubtractTime = secondProductionResult.getNextDaySubtractTime();
        }
        BigDecimal firstUsedCuringTime = singleCuringTime.multiply(BigDecimal.valueOf(firstQty)).add(firstChangeSubSecond).add(firstCleanMouldSubSecond);
        BigDecimal secondUsedCuringTime = singleCuringTime.multiply(BigDecimal.valueOf(secondQty)).add(secondChangeSubSecond).add(secondCleanMouldSubSecond);
        mouldProductionMap.put(first.getMouldCode(), new ProductionInfoVo(productionDate, firstQty, firstProductionResult.getProductionType(), firstUsedCuringTime, firstChangeSubSecond, firstCleanMouldSubSecond, firstNextDaySubtractTime, singleCuringTime));
        mouldProductionMap.put(second.getMouldCode(), new ProductionInfoVo(productionDate, secondQty, secondProductionResult.getProductionType(), secondUsedCuringTime, secondChangeSubSecond, secondCleanMouldSubSecond, secondNextDaySubtractTime, singleCuringTime));
    }

    /**
     * 根据在productionDate排产量、剩余排产量，得到在productionDate的排产量
     *
     * @param dayProSizeLeftOverQty 剩余排产量
     * @param realProductionQty     最大可排产量
     * @param productionDate        排产日
     * @param productionContext     排产上下文
     * @param dayProductionPlan     排产计划
     * @param isPreFlag             是否预排-有交期使用
     * @param mouldInfoList         模具信息
     * @return
     */
    private static Long getProductionQty(Long dayProSizeLeftOverQty, Long realProductionQty, Integer productionDate, ProductionContext productionContext, DayProductionPlanInfoVo dayProductionPlan, boolean isPreFlag, MouldInfoVO... mouldInfoList) {
        MonthPlanManufacturingRequirementVo currentPlan = productionContext.getMonthPlanInitMap().get(dayProductionPlan.getMonthPlanId());
        //20250626 续作计划不判断产能限制
        if (YesOrNoEnum.YES.getValue().equals(currentPlan.getIsContinue())) {
            return realProductionQty;
        }
        //20250624 拼模排产后一个规格排产，则直接排
        if (productionContext.isAssemblingMouldNextProductCode()) {
            return realProductionQty;
        }
        //单日最大剩余产能
        Long dayMaxLeftOverQty = productionContext.getDayLeftOverQty(productionDate);
        Long minLimitQty = Math.min(dayProSizeLeftOverQty, dayMaxLeftOverQty);
        if (minLimitQty >= realProductionQty) {
            return realProductionQty;
        }
        //当日有排，则表示有同规格前一条计划已经排产，补充当日剩余量即可
        boolean currentDateIsProduction = MouldBaseUtils.isProduction(productionDate, productionContext, dayProductionPlan.getProductCode(), isPreFlag, currentPlan.getEmbryoCode(), mouldInfoList);
        if (currentDateIsProduction) {
            return realProductionQty;
        }
        MouldInfoVO first = mouldInfoList[0];
        String mouldCodeInfo;
        if (mouldInfoList.length > BigDecimal.ONE.intValue()) {
            mouldCodeInfo = String.format("[%s]、[%s]", first.getMouldCode(), mouldInfoList[1].getMouldCode());
        } else {
            mouldCodeInfo = first.getMouldCode();
        }
        //需要看前一日是否排产
        Integer previousProductionDate = MouldUtils.getPreviousProductionDate(productionContext, productionDate, first.getProductionOrient());
        boolean previousDateIsProduction = MouldBaseUtils.isProduction(previousProductionDate, productionContext, dayProductionPlan.getProductCode(), isPreFlag, currentPlan.getEmbryoCode(), mouldInfoList);
        if (previousDateIsProduction) {
            return realProductionQty;
        }
        //前一日没有排产，又没有寸口+成形法剩余量或是单日没有剩余产能，则不能排产
        if (dayMaxLeftOverQty <= BigDecimal.ZERO.longValue() || dayProSizeLeftOverQty <= BigDecimal.ZERO.longValue()) {
            ProductionLogUtils.addNoDayLeftOverQtyLog(productionContext, currentPlan, productionDate, mouldCodeInfo, realProductionQty);
            return BigDecimal.ZERO.longValue();
        }
        return realProductionQty;
    }

    /**
     * 计算模具在productionDate排产的数量-单模产能
     * 1、需要判断productionDate是否在不可排产日列表(包含停工日、维修日)中
     * 1.1、不在不可排产日，反向排产需要进一步判断，正向则直接下一步
     * 1.1.1、反向排产：需判断是否在已排完日列表中(因交期导致反向排产会不连续)
     * 2、然后判断模具的日剩余硫化时间，是否可排一条
     * 最后计算排产量 = 向下取整【日剩余硫化时间/(单条硫化时间)】
     * 单条硫化时间 = 物料硫化时间 + 单条间隔硫化时间，单位秒
     * 最后可排产量 = Min(需排产量, 排产量)
     *
     * @param mouldInfo         模具信息
     * @param dayProductionPlan 日排产计划信息
     * @param productionContext 排产上下文
     * @param isPreFlag         是否预排
     * @return
     */
    public static ProductionInfoVo calculateProductionQty(MouldInfoVO mouldInfo, DayProductionPlanInfoVo dayProductionPlan, ProductionContext productionContext, boolean isPreFlag) {
        Integer productionDate = dayProductionPlan.getProductionDate();
        BigDecimal singleCuringTime = dayProductionPlan.getSingleCuringTime();
        //不可排产日
        if (mouldInfo.getNoProductionDayList().containsKey(productionDate)) {
            ProductionTypeEnum productionType = ProductionTypeEnum.getInstance(mouldInfo.getNoProductionDayList().get(productionDate).getNoProductionType().getType());
            return new ProductionInfoVo(productionDate, BigDecimal.ZERO.longValue(), productionType, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, singleCuringTime);
        }
        //20251014 ZLT 不管正反向都需要判断是否已排完毕日
        if (mouldInfo.getProductionFinishDayList().contains(productionDate)) {
            return new ProductionInfoVo(productionDate, BigDecimal.ZERO.longValue(), ProductionTypeEnum.FINISH_DAY, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, singleCuringTime);
        }
        //剩余硫化时间
        MouldDayProductionLeftOverVo leftOverInfo = getProductDateLeftOverInfo(mouldInfo, dayProductionPlan.getProductCode(), productionDate, productionContext, isPreFlag);
        BigDecimal leftOverSecond = leftOverInfo.getLeftOverSecond();
        //20250325 预排时，需要判断是否跨天扣减换规格产能，是否需要扣减洗模产能
        if (isPreFlag) {
            leftOverSecond = handlerPreLeftOverTime(leftOverSecond, mouldInfo, productionContext);
        }
        if (BigDecimalUtils.safeCompare(leftOverSecond, singleCuringTime) < BigDecimal.ZERO.intValue()) {
            BigDecimal usedCuringTime = leftOverInfo.getChangeSubSecond().add(leftOverInfo.getCleanMouldSubSecond());
            return new ProductionInfoVo(productionDate, BigDecimal.ZERO.longValue(), ProductionTypeEnum.GENERAL_DAY, usedCuringTime, leftOverInfo.getChangeSubSecond(), leftOverInfo.getCleanMouldSubSecond(), leftOverSecond, singleCuringTime);
        }
        ProductionTypeEnum productionType = ProductionTypeEnum.GENERAL_DAY;
        if (leftOverInfo.isCleanMould()) {
            productionType = ProductionTypeEnum.MOULD_CLEANING_DAY;
        }
        String sizeCapacityKey = dayProductionPlan.getSizeCapacityKey();
        //向下取整
        Long maxProductionQty = leftOverSecond.divide(singleCuringTime, 0, RoundingMode.DOWN).longValue();
        Long realProductionQty = Math.min(maxProductionQty, dayProductionPlan.getNeedProductionQty());
        //20250414 增加单天最大排产量控制，得到排产日剩余可排产量
        Long dayLeftOverQty = productionContext.getDayLeftOverQty(productionDate, sizeCapacityKey);
        if (null != dayLeftOverQty && isPreFlag) {
            dayLeftOverQty = productionContext.getDayPreLeftOverQty(productionDate, sizeCapacityKey);
        }
        //20250620 判断是否已经连续排产--日剩余量不足以排产，则看前一天是否排产，如果已经排了，则直接排否则不能排
        Long preemptionQty = leftOverInfo.getRealPreemptionQty(realProductionQty, singleCuringTime);
        DayProductionCapacityParityVo dayCapacityParity = new DayProductionCapacityParityVo(productionDate, realProductionQty, preemptionQty);
        Long confirmProductionQty = getProductionQty(dayLeftOverQty, dayCapacityParity, productionContext, dayProductionPlan, isPreFlag, mouldInfo);
        //20250904 ZLT 达不到排产条件，则不会上模。故而不会有使用硫化时间和跨天换模消耗时间
        if (ProductionConstant.SKIP_PRODUCTION.equals(confirmProductionQty)) {
            return new ProductionInfoVo(productionDate, BigDecimal.ZERO.longValue(), productionType, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, singleCuringTime);
        }
        realProductionQty = confirmProductionQty;
        //硫化时间 = 单条硫化时间 * 硫化量 + 换规格时间 + 洗模扣减时间
        BigDecimal changeSubSecond = leftOverInfo.getChangeSubSecond();
        BigDecimal cleanMouldSubSecond = leftOverInfo.getCleanMouldSubSecond();
        BigDecimal usedCuringTime = singleCuringTime.multiply(BigDecimal.valueOf(realProductionQty)).add(changeSubSecond).add(cleanMouldSubSecond);
        return new ProductionInfoVo(productionDate, realProductionQty, productionType, usedCuringTime, changeSubSecond, cleanMouldSubSecond, leftOverSecond, singleCuringTime);
    }

    /**
     * 根据单条硫化时间，得到单模天产能
     *
     * @param productionContext 排产上下文
     * @param singleCuringTime  单条硫化时间(包含间隔时间)
     */
    public static Long getSingleMouldCapacity(ProductionContext productionContext, BigDecimal singleCuringTime) {
        if (null == singleCuringTime || singleCuringTime.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.longValue();
        }
        BigDecimal dayWorkHours = ProductionProcessUtils.getDayWorkHours(productionContext);
        return dayWorkHours.divide(singleCuringTime, 0, RoundingMode.DOWN).longValue();
    }

    /**
     * 根据单条硫化时间，得到单模的月产能
     * 单模月产能 = 单模天产能 * 月可生产天数
     *
     * @param productionContext 排产上下文
     * @param singleCuringTime  单条硫化时间(包含间隔时间)
     * @return
     */
    public static Long getSingleMonthMouldCapacity(ProductionContext productionContext, BigDecimal singleCuringTime) {
        Long dayCapacity = getSingleMouldCapacity(productionContext, singleCuringTime);
        if (null != dayCapacity && dayCapacity <= BigDecimal.ZERO.longValue()) {
            return BigDecimal.ZERO.longValue();
        }
        return dayCapacity * productionContext.getMonthWorkDays();
    }

    /**
     * 设置模具的排产方向和分组值
     * 以及模具初始的开始排产日和截止日
     * 如果是交期分组，则deadLine = 交期日
     * 否则 deadLine = 月份最大日
     * <p>
     * 正向排产则开始日 = 1 截止日 = deadLine
     * 方向排产则开始日 = deadLine 截止日 = 1
     *
     * @param productionMouldList 当前排产模具列表
     * @param maxMouldInfoList    最大可用模具数
     * @param deadLine            截止日
     * @param hasDeliveryGroup    是否为交期分组
     */
    public static void setGroupValueAndProductionOrient(List<MouldInfoVO> productionMouldList, List<MouldInfoVO> maxMouldInfoList, Integer deadLine, boolean hasDeliveryGroup) {
        if (CollectionUtils.isEmpty(productionMouldList) || productionMouldList.size() != ProductionConstant.DOUBLE_MOULD_PRODUCTION) {
            return;
        }
        MouldInfoVO first = productionMouldList.get(0);
        MouldInfoVO second = productionMouldList.get(1);
        Integer groupValue = first.getGroupValue();
        if (null != groupValue) {
            //20250325 共用模具提前使用了一副时
            initSecondGroupInfoByFirst(first, second);
            if (!hasDeliveryGroup) {
                return;
            }
            //有交期排产，正向则修改截止日，反向则修改起始日
            if (ProductionOrientEnum.FORWARD == first.getProductionOrient()) {
                first.setEndDay(deadLine);
                second.setEndDay(deadLine);
            } else {
                first.setBeginDay(deadLine);
                second.setBeginDay(deadLine);
            }
            return;
        }
        //分组值为空，则表示新挑选上来的模具，则此时需要赋值分组值和排产方向
        Integer nextGroupValue = MouldBaseUtils.getNextGroupValue(maxMouldInfoList);
        ProductionOrientEnum productionOrient = MouldBaseUtils.getProductionOrient(maxMouldInfoList);
        Integer beginDate = BigDecimal.ONE.intValue();
        Integer endDate = deadLine;
        if (ProductionOrientEnum.REVERSE == productionOrient) {
            beginDate = deadLine;
            endDate = BigDecimal.ONE.intValue();
        }
        first.setGroupValue(nextGroupValue);
        first.setProductionOrient(productionOrient);
        first.setBeginDay(beginDate);
        first.setEndDay(endDate);
        second.setGroupValue(nextGroupValue);
        second.setProductionOrient(productionOrient);
        second.setBeginDay(beginDate);
        second.setEndDay(endDate);
    }

    /**
     * 设置最后一副模具的信息
     * 如果没有设置分组值，则表示全新排产模具
     * 设定排产方向、模具排产的起始日，截止日，分组值
     *
     * @param lastMouldInfo    最后一副模具
     * @param maxMouldInfoList 最大排产模具列表
     * @param deadLine         截止日
     * @param hasDeliveryGroup 是否为有交期分组 true表示交期分组 false表示无交期分组
     */
    public static void setLastMouldInfo(MouldInfoVO lastMouldInfo, List<MouldInfoVO> maxMouldInfoList, Integer deadLine, boolean hasDeliveryGroup) {
        //已有分组，则表示模具已排产过
        if (null != lastMouldInfo.getGroupValue()) {
            if (!hasDeliveryGroup) {
                return;
            }
            //有交期排产分组，正向则修改截止日，反向则修改起始日
            if (ProductionOrientEnum.FORWARD == lastMouldInfo.getProductionOrient()) {
                lastMouldInfo.setEndDay(deadLine);
            } else {
                lastMouldInfo.setBeginDay(deadLine);
            }
            return;
        }
        //分组值为空，则表示新使用模具，需要赋值分组值及排产方向
        Integer nextGroupValue = MouldBaseUtils.getNextGroupValue(maxMouldInfoList);
        ProductionOrientEnum productionOrient = MouldBaseUtils.getProductionOrient(maxMouldInfoList);
        Integer beginDate = BigDecimal.ONE.intValue();
        Integer endDate = deadLine;
        if (ProductionOrientEnum.REVERSE == productionOrient) {
            beginDate = deadLine;
            endDate = BigDecimal.ONE.intValue();
        }
        lastMouldInfo.setGroupValue(nextGroupValue);
        lastMouldInfo.setProductionOrient(productionOrient);
        lastMouldInfo.setBeginDay(beginDate);
        lastMouldInfo.setEndDay(endDate);
    }

    /**
     * 获取最大可用模具列表信息
     * 通过排产上下文中存在的物料关联模具关系Map及模具信息列表，
     * 获取其最大的可用模具列表信息
     *
     * @param productionContext 排产上下文
     * @param productCode       物料编码
     * @return
     */
    public static List<MouldInfoVO> getMaxEnableMouldList(ProductionContext productionContext, String productCode) {
        List<MouldInfoVO> mouldInfoList = new ArrayList<>();
        Set<String> mouldAndSpecSet = productionContext.getProductRelationMouldMap().get(productCode);
        mouldAndSpecSet.stream().forEach(mouldAndSpec -> {
            String mouldCode = mouldAndSpec.split(ProductionConstant.PRODUCT_SPLIT)[0];
            if (StringUtils.isBlank(mouldCode)) {
                return;
            }
            MouldInfoVO mouldInfo = productionContext.getMouldInfoMap().get(mouldCode);
            if (null == mouldInfo) {
                return;
            }
            mouldInfoList.add(mouldInfo);
        });
        return mouldInfoList;
    }

    /**
     * 根据模具列表，获取共用的物料集合
     * 通过排产上下文中存储的模具关联的物料Map，
     * 从中取出各模具配置的物料编码集合
     *
     * @param enableMouldList   可用模具列表
     * @param productionContext 排产上下文
     * @return
     */
    public static Set<String> getMouldRelationProductInfo(List<MouldInfoVO> enableMouldList, ProductionContext productionContext) {
        if (CollectionUtils.isEmpty(enableMouldList)) {
            return Collections.emptySet();
        }
        Set<String> relationProductSet = new HashSet<>();
        enableMouldList.stream().forEach(enableMould -> {
            String mouldCode = enableMould.getMouldCode();
            Set<String> mouldRelationProductSet = productionContext.getMouldRelationProductMap().get(mouldCode);
            if (CollectionUtils.isEmpty(mouldRelationProductSet)) {
                return;
            }
            relationProductSet.addAll(mouldRelationProductSet);
        });
        return relationProductSet;
    }

    /**
     * 设置模具的当前排产信息
     * 主要设置当前排产日，排产规格
     * 以及连续排产天数
     *
     * @param mouldInfo             模具信息对象
     * @param productionInfo        排产结果信息
     * @param productionProductCode 排产物料编码
     * @param productionContext     排产上下文
     */
    public static void setMouldCurrentProductionInfo(MouldInfoVO mouldInfo, ProductionInfoVo productionInfo, String productionProductCode, ProductionContext productionContext) {
        Integer startDate = mouldInfo.getBeginDay();
        Integer newStartDate = productionInfo.getProductionDate();
        Integer continueDays = mouldInfo.getContinuousDays();
        if (null == continueDays) {
            continueDays = BigDecimal.ZERO.intValue();
        }
        //取差值
        int addContinueDays = Math.abs(newStartDate - startDate);
        if (productionProductCode.equals(mouldInfo.getCurrentProductCode())) {
            //不是同一天
            if (!startDate.equals(newStartDate)) {
                continueDays = continueDays + addContinueDays;
            }
        } else {
            continueDays = addContinueDays + 1;
        }
        mouldInfo.setBeginDay(newStartDate);
        mouldInfo.setCurrentProductCode(productionProductCode);
        mouldInfo.setContinuousDays(continueDays);
        //模具的截止日--因有交期计划故而需要重设
        if (ProductionOrientEnum.FORWARD == mouldInfo.getProductionOrient()) {
            mouldInfo.setEndDay(productionContext.getMonthDays());
        } else {
            mouldInfo.setBeginDay(productionContext.getMonthDays());
        }
    }

    /**
     * 判断排产日是否可排产
     * 正向： 排产日 <= 截止日
     * 反向：排产日 >= 截止日
     *
     * @param productionDate   排产日
     * @param endDate          截止日
     * @param productionOrient 排产方向
     * @return
     */
    public static boolean isDateProduction(Integer productionDate, Integer endDate, ProductionOrientEnum productionOrient) {
        if (ProductionOrientEnum.FORWARD == productionOrient) {
            return productionDate <= endDate;
        }
        return productionDate >= endDate;
    }

    /**
     * 根据排产方向 获取前一个排产日，如果是停工日，则继续往前查找
     * 正向：排产日 -1
     * 反向： 排产日 + 1
     *
     * @param productionContext 排产上下文
     * @param productionDate    当前排产日
     * @param productionOrient  排产方向
     * @return
     */
    public static Integer getPreviousProductionDate(ProductionContext productionContext, Integer productionDate, ProductionOrientEnum productionOrient) {
        Integer previousDate;
        if (ProductionOrientEnum.FORWARD == productionOrient) {
            previousDate = productionDate - 1;
        } else {
            previousDate = productionDate + 1;
        }
        //如果是停工日，则继续往前找
        if (!productionContext.getFactoryStopDays().contains(previousDate)) {
            return previousDate;
        }
        return getPreviousProductionDate(productionContext, previousDate, productionOrient);
    }

    /**
     * 根据排产方向 获取下一个可排产日，如果下一日为停工日，则继续查找
     * 正向：排产日 + 1
     * 反向： 排产日 - 1
     *
     * @param productionContext 排产上下文
     * @param productionDate    当前排产日
     * @param productionOrient  排产方向
     * @return
     */
    public static Integer getNextProductionDate(ProductionContext productionContext, Integer productionDate, ProductionOrientEnum productionOrient) {
        Integer nextDate;
        if (ProductionOrientEnum.FORWARD == productionOrient) {
            nextDate = productionDate + 1;
        } else {
            nextDate = productionDate - 1;
        }
        //如果是停工日，则继续往后
        if (!productionContext.getFactoryStopDays().contains(nextDate)) {
            return nextDate;
        }
        return getNextProductionDate(productionContext, nextDate, productionOrient);
    }

    /**
     * 根据排产方向，获取下一个排产日，
     * 不考虑下一个排产日是否为停工日
     * 正向：排产日 + 1
     * 反向： 排产日 - 1
     *
     * @param productionDate   当前排产日
     * @param productionOrient 排产方向
     * @return
     */
    public static Integer getNextDate(Integer productionDate, ProductionOrientEnum productionOrient) {
        Integer nextDate;
        if (ProductionOrientEnum.FORWARD == productionOrient) {
            nextDate = productionDate + 1;
        } else {
            nextDate = productionDate - 1;
        }
        return nextDate;
    }

    /**
     * 获取一组模具开始排产日
     * 正方向，则取两个中最小的开始排产日
     * 反方向，则取两个中最大的开始排产日
     *
     * @param productionMouldList
     * @return
     */
    public static Integer getStartProductionDate(List<MouldInfoVO> productionMouldList) {
        if (CollectionUtils.isEmpty(productionMouldList)) {
            return null;
        }
        Integer firstBeginDay = productionMouldList.get(0).getBeginDay();
        if (productionMouldList.size() == 1) {
            return firstBeginDay;
        }
        Integer secondBeginDay = productionMouldList.get(1).getBeginDay();
        ProductionOrientEnum productionOrient = productionMouldList.get(0).getProductionOrient();
        if (ProductionOrientEnum.FORWARD == productionOrient) {
            return Math.min(firstBeginDay, secondBeginDay);
        }
        return Math.max(firstBeginDay, secondBeginDay);
    }

    /**
     * 根据选中的两副模具，从两副中挑选开始时间较早的模具
     *
     * @param first            第一副模具
     * @param second           第二副模具
     * @param productionOrient 排产方向
     * @return
     */
    public static MouldInfoVO getEarlierStartDayMouldInfo(MouldInfoVO first, MouldInfoVO second, ProductionOrientEnum productionOrient) {
        if (null == first || null == second) {
            return null;
        }
        Integer firstBeginDay = first.getBeginDay();
        Integer secondBeginDay = second.getBeginDay();
        if (ProductionOrientEnum.FORWARD == productionOrient) {
            if (firstBeginDay < secondBeginDay) {
                return first;
            }
            return second;
        }
        if (firstBeginDay < secondBeginDay) {
            return second;
        }
        return first;
    }

    /**
     * 获取选择两副模具，从两副模具中挑选结束时间较晚的模具
     *
     * @param first            第一副模具
     * @param second           第二副模具
     * @param productionOrient 排产方向
     * @return
     */
    public static MouldInfoVO getLaterEndDayMouldInfo(MouldInfoVO first, MouldInfoVO second, ProductionOrientEnum productionOrient) {
        if (null == first || null == second) {
            return null;
        }
        Integer firstEndDay = first.getEndDay();
        Integer secondEndDay = second.getEndDay();
        if (ProductionOrientEnum.FORWARD == productionOrient) {
            if (firstEndDay < secondEndDay) {
                return second;
            }
            return first;
        }
        if (firstEndDay < secondEndDay) {
            return first;
        }
        return second;
    }

    /**
     * 获取一组模具截止排产日
     * 正方向，则取两个中最大的截止排产日
     * 反方向，则取两个中最小的截止排产日
     *
     * @param productionMouldList
     * @return
     */
    public static Integer getEndProductionDate(List<MouldInfoVO> productionMouldList) {
        if (CollectionUtils.isEmpty(productionMouldList)) {
            return null;
        }
        Integer firstEndDay = productionMouldList.get(0).getEndDay();
        if (productionMouldList.size() == 1) {
            return firstEndDay;
        }
        Integer secondEndDay = productionMouldList.get(1).getEndDay();
        ProductionOrientEnum productionOrient = productionMouldList.get(0).getProductionOrient();
        if (ProductionOrientEnum.FORWARD == productionOrient) {
            return Math.max(firstEndDay, secondEndDay);
        }
        return Math.min(firstEndDay, secondEndDay);
    }

    /**
     * 根据排程上下文，获取连续排产需洗模的参数设定
     * 如设置为15，则表示连续排产15天，第16天需要洗模
     * 没有配置或是配置成负数，则表示不用洗模，返回null
     *
     * @param productionContext 排产上下文
     * @return
     */
    public static Integer getContinueProductionCleaningDay(ProductionContext productionContext) {
        Integer continueCleanDay = (Integer) productionContext.getFactoryParams().get(FactoryConstant.SYS_PARAM_CONNECTION_SCHEDULING_DAYS);
        if (null == continueCleanDay || continueCleanDay <= 0) {
            return null;
        }
        //洗模日参数+1则为需要洗模的日期
        continueCleanDay = continueCleanDay + 1;
        return continueCleanDay;
    }

    /**
     * 根据第一模具初始化
     * 第二副模具的排产方向等信息
     * 主要场景为：共用模具时，前一规格只使用了一副模具，后一规格双模排产时出现
     *
     * @param first  第一副模具
     * @param second 第二副模具--新模具
     */
    private static void initSecondGroupInfoByFirst(MouldInfoVO first, MouldInfoVO second) {
        Integer secondGroup = second.getGroupValue();
        if (null != secondGroup) {
            return;
        }
        Integer groupValue = first.getGroupValue();
        ProductionOrientEnum productionOrient = first.getProductionOrient();
        second.setGroupValue(groupValue);
        second.setProductionOrient(productionOrient);
        //20250524 ZLT 起始时间与第一副模具一样
        second.setBeginDay(first.getBeginDay());
        second.setEndDay(first.getEndDay());
    }

    /**
     * 从模具列表中挑选合适的模具
     *
     * @param needLhTime 需要的硫化时间
     * @param mouldList  模具列表
     * @return
     */
    private static List<MouldInfoVO> getSelectedMould(BigDecimal needLhTime, List<MouldInfoVO> mouldList) {
        Comparator comparator = Comparator.comparing(MouldInfoVO::getGroupValue, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(MouldInfoVO::getLeftOverSeconds).thenComparing(MouldInfoVO::getMouldCode);
        mouldList.sort(comparator);
        int mouldSize = mouldList.size();
        int groupCount = mouldSize / 2;
        List<MouldInfoVO> productionMouldList;
        //两副两副
        for (int index = 0; index < groupCount; index++) {
            int startIndex = index * 2;
            int endIndex = (index + 1) * 2;
            productionMouldList = mouldList.subList(startIndex, endIndex);

            BigDecimal leftOverSeconds = BigDecimal.ZERO;
            for (MouldInfoVO mouldInfoVO : productionMouldList) {
                leftOverSeconds = BigDecimalUtils.add(leftOverSeconds, mouldInfoVO.getLeftOverSeconds());
            }
            if (BigDecimalUtils.safeCompare(leftOverSeconds, needLhTime) > 0) {
                return productionMouldList;
            }
        }
        return null;
    }

    /**
     * 判断模具是否排产了计划的规格
     *
     * @param mouldInfo      模具信息
     * @param productionPlan 排产计划
     * @return
     */
    private static boolean mouldIsProductionProductCodeByPlan(MouldInfoVO mouldInfo, MonthPlanManufacturingRequirementVo productionPlan) {
        if (null == mouldInfo || null == productionPlan || StringUtils.isBlank(productionPlan.getProductCode())) {
            return false;
        }
        Map<Integer, List<MouldDayProductionVo>> dayProductionMap = mouldInfo.getDayProductionMap();
        if (CollectionUtils.isEmpty(dayProductionMap)) {
            return false;
        }
        for (Map.Entry<Integer, List<MouldDayProductionVo>> dayProductionEntry : dayProductionMap.entrySet()) {
            List<MouldDayProductionVo> dayProductionList = dayProductionEntry.getValue();
            if (CollectionUtils.isEmpty(dayProductionList)) {
                continue;
            }
            for (MouldDayProductionVo dayProduction : dayProductionList) {
                if (productionPlan.getProductCode().equals(dayProduction.getProductCode())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断是否为洗模日
     *
     * @param mouldInfo         模具信息
     * @param productCode       当前排产物料编码
     * @param productionDate    当前排产日
     * @param productionContext 排产上下文
     */
    private static boolean isCleaningDay(MouldInfoVO mouldInfo, String productCode, Integer productionDate, ProductionContext productionContext) {
        Integer continueCleanDay = getContinueProductionCleaningDay(productionContext);
        if (null == continueCleanDay) {
            return false;
        }
        //已排产日
        Map<Integer, List<MouldDayProductionVo>> dayProductionMap = mouldInfo.getDayProductionMap();
        if (CollectionUtils.isEmpty(dayProductionMap)) {
            return false;
        }
        Set<Integer> productionDaySet = dayProductionMap.keySet();
        if ((productionDaySet.size() + 1) < continueCleanDay) {
            return false;
        }
        return isCleaningDayCalculate(mouldInfo, productCode, productionDate, continueCleanDay);
    }

    /**
     * 根据排产的物料规格productCode，获取模具在productionDate的剩余硫化时间
     * 如果在productionDate有换规格，则排产日剩余时间 = 排产日剩余时间 - 换规格扣减时间 - 洗模扣减时间
     * 换规格扣减时间 = SYS016 配置参数值
     * 洗模扣减时间 = SYS026 配置参数值
     *
     * @param mouldInfo         需要排产的模具
     * @param productCode       排产的规格
     * @param productionDate    排产日
     * @param productionContext 排产上下文
     * @param isPreFlag         是否预排标记 true表示预排，false表示正常排产
     */
    private static MouldDayProductionLeftOverVo getProductDateLeftOverInfo(MouldInfoVO mouldInfo, String productCode, Integer productionDate, ProductionContext productionContext, boolean isPreFlag) {
        BigDecimal leftOverSecond = mouldInfo.getProductionDayList().get(productionDate);
        boolean isChangeProduct = MouldBaseUtils.isChangeProductCode(mouldInfo, productCode, productionDate, productionContext);
        BigDecimal changeProductSubTime;
        if (!isChangeProduct) {
            changeProductSubTime = BigDecimal.ZERO;
        } else {
            changeProductSubTime = MouldBaseUtils.getChangeProductConsumeTime(productionContext);
        }
        leftOverSecond = leftOverSecond.subtract(changeProductSubTime);
        BigDecimal cleanMouldSubTime = BigDecimal.ZERO;
        boolean isCleanMould = false;
        //正式排产-判断洗模日--20250411(预排不判断洗模日-在预排时就先判断) 洗模日不再是全天洗模，改为计算剩余产能-故而需要重新设置洗模日的剩余时间
        if (!isPreFlag && isCleaningDay(mouldInfo, productCode, productionDate, productionContext)) {
            cleanMouldSubTime = MouldBaseUtils.getCleaningMouldConsumeTime(productionContext);
            isCleanMould = true;
            leftOverSecond = leftOverSecond.subtract(cleanMouldSubTime);
        }
        return new MouldDayProductionLeftOverVo(productionDate, leftOverSecond, changeProductSubTime, isChangeProduct, isCleanMould, cleanMouldSubTime);
    }

    /**
     * 预排时，额外处理的剩余硫化时间
     * 原因：因为是预排，故而对换规格和洗模的产能扣减不是马上进行，故而需要临时进行计算
     * 20250411 因洗模不再是一整天，故而采用扣减产能方式
     *
     * @param leftOverSecond    剩余硫化时间(单位秒)
     * @param mouldInfo         模具信息
     * @param productionContext 排产信息
     * @return
     */
    private static BigDecimal handlerPreLeftOverTime(BigDecimal leftOverSecond, MouldInfoVO mouldInfo, ProductionContext productionContext) {
        //在预排处理时告知当日要洗模，则需要扣减洗模的产能
        if (mouldInfo.getIsClearMould()) {
            leftOverSecond = leftOverSecond.subtract(MouldBaseUtils.getCleaningMouldConsumeTime(productionContext));
        }
        //在预排处理时，告知换规格出现需要跨天扣减产能，则需要跨天换规格扣减产能
        BigDecimal nextDaySubtractTime = mouldInfo.getNextDaySubtractTime();
        if (null != nextDaySubtractTime && nextDaySubtractTime.compareTo(BigDecimal.ZERO) < 0) {
            leftOverSecond = leftOverSecond.add(nextDaySubtractTime);
        }
        return leftOverSecond;
    }

    /**
     * 20250411 因洗模日不在是一整天洗模，故而导致洗模日还可排产。因此洗模日也当做正常排产日
     * 判断两模是否为双模排产模式
     * 两模都是正常排产日或是洗模日排产则都为双模排产
     *
     * @param firstProductionResult  第一模排产结果
     * @param secondProductionResult 第二模排产结果
     * @return
     */
    private static boolean isDoubleProduction(ProductionInfoVo firstProductionResult, ProductionInfoVo secondProductionResult) {
        ProductionTypeEnum firstProductionType = firstProductionResult.getProductionType();
        ProductionTypeEnum secondProductionType = secondProductionResult.getProductionType();
        return firstProductionType.isNormalProduction() && secondProductionType.isNormalProduction();
    }

    /**
     * 洗模日的判断，根据排产方向不同，判断方向不同
     * 连续排产：排产日最后一个规格与当前排产规格是否相同，相同则天数+1，不同则天数结束
     * 根据连续天数与cleanDay比较，==则表示要洗模
     * 正向排产，则按当前排产日往月初方向，判断规格连续排产日
     * 反向排产，则按当前排产日往模具起始日方向，判断规格连续排产日
     * 循环截止日 正向为1 ，反向为起始日
     *
     * @param mouldInfo      模具信息
     * @param productCode    排产规格
     * @param productionDate 当前排产日
     * @param cleanDay       连续天数洗模
     * @return
     */
    private static boolean isCleaningDayCalculate(MouldInfoVO mouldInfo, String productCode, Integer productionDate, Integer cleanDay) {
        Integer continueDays = 1;
        Integer index;
        ProductionOrientEnum productionOrient = mouldInfo.getProductionOrient();
        Map<Integer, List<MouldDayProductionVo>> dayProductionMap = mouldInfo.getDayProductionMap();
        Integer finishDay;
        if (ProductionOrientEnum.FORWARD == productionOrient) {
            //正向排产 = 1
            finishDay = BigDecimal.ONE.intValue();
        } else {
            //反向排产 = 起始日
            finishDay = mouldInfo.getBeginDay();
        }
        if (ProductionOrientEnum.FORWARD == productionOrient) {
            index = productionDate - 1;
        } else {
            index = productionDate + 1;
        }
        //判断当日是否已洗模，已洗模则不用再洗
        if (mouldInfo.getCleanDayList().containsKey(productionDate)) {
            return false;
        }
        //计算排产日之前(或是之后)最后一个排产规格与当前规格相等的连续天数
        for (; matchCleaningDayFinish(index, productionOrient, finishDay); ) {
            if (!dayProductionMap.containsKey(index)) {
                break;
            }
            List<MouldDayProductionVo> dayProductionList = dayProductionMap.get(index);
            if (CollectionUtils.isEmpty(dayProductionList)) {
                break;
            }
            String last = dayProductionList.get(dayProductionList.size() - 1).getProductCode();
            if (!productCode.equals(last)) {
                break;
            }
            continueDays = continueDays + 1;
            if (continueDays >= cleanDay) {
                break;
            }
            //20250411 是洗模日，则直接断掉
            if (mouldInfo.getCleanDayList().containsKey(index)) {
                break;
            }
            if (ProductionOrientEnum.FORWARD == productionOrient) {
                index--;
            } else {
                index++;
            }
        }
        return continueDays >= cleanDay;
    }

    /**
     * 洗模日，循环退出匹配计算
     * 正向排产，则排产日大于结束日，表示继续判断
     * 反向排产，则排产日 小于等于结束日，则表示继续判断
     * 正向排产：结束日 = 1
     * 反向排产：结束日 = 模具的起始排产日
     *
     * @param currentDate      当前日
     * @param productionOrient 排产方向
     * @param finishDay        结束日
     * @return
     */
    private static boolean matchCleaningDayFinish(int currentDate, ProductionOrientEnum productionOrient, Integer finishDay) {
        if (ProductionOrientEnum.FORWARD == productionOrient) {
            return currentDate >= finishDay;
        }
        return currentDate <= finishDay;
    }

    /**
     * 得到日排产消耗产能信息
     *
     * @param productionContext    排产上下文
     * @param productionPlan       排产计划
     * @param productionDate       排产日
     * @param doubleProductionQty  实际排产量
     * @param singleCuringTime     单条硫化时间(包含间隔增加时间)
     * @param firstProductionInfo  第一副模具排产信息
     * @param secondProductionInfo 第二副模具排产信息
     * @return
     */
    private static DayProductionCapacityParityVo getDayLimitCapacityInfo(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, Integer productionDate, Long doubleProductionQty, BigDecimal singleCuringTime, ProductionInfoVo firstProductionInfo, ProductionInfoVo secondProductionInfo) {
        Long singleQty = doubleProductionQty / 2;
        Long firstPreemptionQty = firstProductionInfo.getRealPreemptionQty(productionContext, productionPlan, singleQty, singleCuringTime);
        Long secondPreemptionQty = secondProductionInfo.getRealPreemptionQty(productionContext, productionPlan, singleQty, singleCuringTime);
        Long sumPreemptionQty = firstPreemptionQty + secondPreemptionQty;
        return new DayProductionCapacityParityVo(productionDate, doubleProductionQty, sumPreemptionQty);
    }

    /**
     * 得到日排产消耗产能信息
     *
     * @param productionDate    排产日
     * @param realProductionQty 实际排产量
     * @param singleCuringTime  单条硫化时间(包含间隔增加时间)
     * @param leftOverInfo      模具排产信息
     * @return
     */
    private static DayProductionCapacityParityVo getDayLimitCapacityInfo(Integer productionDate, Long realProductionQty, BigDecimal singleCuringTime, MouldDayProductionLeftOverVo leftOverInfo) {
        Long preemptionQty = leftOverInfo.getRealPreemptionQty(realProductionQty, singleCuringTime);
        return new DayProductionCapacityParityVo(productionDate, realProductionQty, preemptionQty);
    }

    /**
     * 根据在productionDate排产量、剩余排产量，得到在productionDate的排产量
     *
     * @param dayProSizeLeftOverQty 剩余排产量
     * @param dayCapacityParity     日排产产能消耗对象信息
     * @param productionContext     排产上下文
     * @param dayProductionPlan     排产计划
     * @param isPreFlag             是否预排-有交期使用
     * @param mouldInfoList         模具信息
     * @return
     */
    private static Long getProductionQty(Long dayProSizeLeftOverQty, DayProductionCapacityParityVo dayCapacityParity, ProductionContext productionContext, DayProductionPlanInfoVo dayProductionPlan, boolean isPreFlag, MouldInfoVO... mouldInfoList) {
        //实际排产量
        Long realProductionQty = dayCapacityParity.getRealProductionQty();
        //排产日
        Integer productionDate = dayCapacityParity.getProductionDate();
        //实际占用产能量
        Long realPreemptionQty = dayCapacityParity.getRealPreemptionQty();
        MonthPlanManufacturingRequirementVo currentPlan = productionContext.getMonthPlanInitMap().get(dayProductionPlan.getMonthPlanId());
        //20250626 续作排产不判断产能限制
        if (dayProductionPlan.isContinueProduction()) {
            ProductionLogUtils.addProductionQtyContinuePreemptionQty(productionContext, currentPlan, realProductionQty, realPreemptionQty);
            return realProductionQty;
        }
        //20250624 拼模排产后一个规格排产，则直接排
        if (productionContext.isAssemblingMouldNextProductCode()) {
            ProductionLogUtils.addProductionQtyAssemblingPreemptionQty(productionContext, currentPlan, realProductionQty, realPreemptionQty);
            return realProductionQty;
        }
        //单日最大剩余产能
        Long dayMaxLeftOverQty = productionContext.getDayLeftOverQty(productionDate);
        Long minLimitQty = Math.min(dayProSizeLeftOverQty, dayMaxLeftOverQty);
        ProductionLogUtils.addProductionQtyDiffPreemptionQty(productionContext, currentPlan, realProductionQty, realPreemptionQty, minLimitQty);
        if (minLimitQty >= realPreemptionQty) {
            return realProductionQty;
        }
        //20250903 标记反向排产是否已经超成型产能，如果是反向排产超了，不管有没有量都标记
        boolean isReverse = false;
        if (mouldInfoList.length > BigDecimal.ONE.intValue() && ProductionOrientEnum.REVERSE == mouldInfoList[BigDecimal.ZERO.intValue()].getProductionOrient()) {
            isReverse = true;
        }
        if (isReverse) {
            ProductionLogUtils.addReverseProductionExceedCapacity(productionContext, currentPlan, realPreemptionQty, minLimitQty);
            productionContext.getExceedCapacityProductMap().put(dayProductionPlan.getProductCode(), true);
        }
        /*
         * 单日产能预占量超出单日剩余产能时，需要判断规格排产日是否有排产
         * 如果排产日已经排产，则直接补充当日剩余量即可。
         * 如果没有，则需看前日是否有排产：
         * 1、如果前日有排产，则继续强排，保持续作
         * 2、如果前日没有排产，则不能在排产日上机(新增)
         */
        boolean currentDateIsProduction = MouldBaseUtils.isProduction(productionDate, productionContext, dayProductionPlan.getProductCode(), isPreFlag, currentPlan.getEmbryoCode(), mouldInfoList);
        if (currentDateIsProduction) {
            ProductionLogUtils.addExceedCapacityByCurrentProductionQty(productionContext, currentPlan, productionDate, realProductionQty);
            //已经超出当日产能但当日有排过，则表示有同规格前一条计划已经排产，补充当日剩余量即可
            return realProductionQty;
        }
        MouldInfoVO first = mouldInfoList[0];
        String mouldCodeInfo;
        if (mouldInfoList.length > BigDecimal.ONE.intValue()) {
            mouldCodeInfo = String.format("[%s]、[%s]", first.getMouldCode(), mouldInfoList[1].getMouldCode());
        } else {
            mouldCodeInfo = first.getMouldCode();
        }
        //需要看前一日是否排产
        Integer previousProductionDate = MouldUtils.getPreviousProductionDate(productionContext, productionDate, first.getProductionOrient());
        boolean previousDateIsProduction = MouldBaseUtils.isProduction(previousProductionDate, productionContext, dayProductionPlan.getProductCode(), isPreFlag, currentPlan.getEmbryoCode(), mouldInfoList);
        if (previousDateIsProduction) {
            ProductionLogUtils.addExceedCapacityByBeforeProductionQty(productionContext, currentPlan, previousProductionDate, realProductionQty);
            return realProductionQty;
        }
        //前一日没有排产，又没有寸口+成形法剩余量或是单日没有剩余产能，则不能排产
        ProductionLogUtils.addNoDayLeftOverQtyLog(productionContext, currentPlan, productionDate, mouldCodeInfo, realProductionQty);
        return ProductionConstant.SKIP_PRODUCTION;
//        if (dayMaxLeftOverQty <= BigDecimal.ZERO.longValue() || dayProSizeLeftOverQty <= BigDecimal.ZERO.longValue()) {
//
//        }
//        return realProductionQty;
    }

    private MouldUtils() {

    }
}

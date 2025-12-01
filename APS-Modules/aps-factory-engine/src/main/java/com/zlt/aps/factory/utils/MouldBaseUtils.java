package com.zlt.aps.factory.utils;

import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.enums.MouldAirTypeEnum;
import com.zlt.aps.factory.enums.ProductionOrientEnum;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.monthplan.api.domain.vo.NoProductionDayMouldVo;
import com.zlt.aps.monthplan.api.enums.MouldNoProductionType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模具基础业务-工具类
 *
 * @author ZLT
 * @date 20250830
 */
@Slf4j
public class MouldBaseUtils {

    /**
     * 根据模具返厂配置，构建模具信息
     * 模具不可排产列表，可排产日列表及总的硫化时间(分)
     *
     * @param maintenanceMould         已维修设置
     * @param maintenanceConfiguration 当前维修设置
     * @param zoneId                   时区
     * @param context                  排产上下文
     * @return
     */
    public static MouldInfoVO buildMouldInfo(MouldInfoVO maintenanceMould, MouldMaintenanceConfigurationVo maintenanceConfiguration, ZoneId zoneId, ProductionContext context) {
        MouldInfoVO mouldInfo;
        if (null == maintenanceMould) {
            mouldInfo = new MouldInfoVO();
            mouldInfo.setMouldCode(maintenanceConfiguration.getMouldCode());
            mouldInfo.setFactoryCode(maintenanceConfiguration.getFactoryCode());
            mouldInfo.setMouldNo(maintenanceConfiguration.getMouldNo());
            mouldInfo = buildBaseMouldInfo(mouldInfo);
        } else {
            mouldInfo = maintenanceMould;
        }
        //每天工作时限
        BigDecimal dayCuringTime = ProductionProcessUtils.getDayWorkHours(context);
        //月份最大天数
        Integer maxDays = context.getMonthDays();
        LocalDate maintenanceBeginDate = maintenanceConfiguration.getBeginDate().toInstant().atZone(zoneId).toLocalDate();
        LocalDate maintenanceEndDate = maintenanceConfiguration.getEndDay().toInstant().atZone(zoneId).toLocalDate();
        //20250519 ZLT 排产月份区分自然月与非自然月
        Map<String, Integer> daysMap = DateUtils.calculateDaysByMonth(context, maintenanceBeginDate, maintenanceEndDate);
        Integer beginDay = daysMap.get(DateUtils.START_DAY);
        Integer endDay = daysMap.get(DateUtils.END_DAY);
        //不可排产日列表
        Map<Integer, NoProductionDayMouldVo> noProductionDayMap = mouldInfo.getNoProductionDayList();
        if (null == noProductionDayMap) {
            noProductionDayMap = new HashMap<>(maxDays);
        }
        //停工日
        Set<Integer> stopDays = context.getFactoryStopDays();
        //先停工
        if (!CollectionUtils.isEmpty(stopDays)) {
            for (Integer stopDay : stopDays) {
                if (noProductionDayMap.containsKey(stopDay)) {
                    continue;
                }
                NoProductionDayMouldVo noProductionDay = new NoProductionDayMouldVo();
                noProductionDay.setDay(stopDay);
                noProductionDay.setNoProductionType(MouldNoProductionType.STOP_DAY);
                noProductionDayMap.put(stopDay, noProductionDay);
            }
        }
        //后维修
        for (int maintenanceDay = beginDay; maintenanceDay <= endDay; maintenanceDay++) {
            if (noProductionDayMap.containsKey(maintenanceDay)) {
                continue;
            }
            NoProductionDayMouldVo noProductionDay = new NoProductionDayMouldVo();
            noProductionDay.setDay(maintenanceDay);
            noProductionDay.setNoProductionType(MouldNoProductionType.MAINTENANCE_DAY);
            noProductionDayMap.put(maintenanceDay, noProductionDay);
        }
        mouldInfo.setNoProductionDayList(noProductionDayMap);
        //可排产日列表
        Map<Integer, BigDecimal> productionDayMap = new HashMap<>(maxDays);
        BigDecimal totalCuringTime = BigDecimal.ZERO;
        for (int productionDay = BigDecimal.ONE.intValue(); productionDay <= maxDays; productionDay++) {
            if (noProductionDayMap.containsKey(productionDay)) {
                continue;
            }
            productionDayMap.put(productionDay, dayCuringTime);
            totalCuringTime = totalCuringTime.add(dayCuringTime);
        }
        mouldInfo.setTotalSeconds(totalCuringTime);
        mouldInfo.setLeftOverSeconds(totalCuringTime);
        mouldInfo.setPreemptLeftOverSeconds(totalCuringTime);
        mouldInfo.setProductionDayList(productionDayMap);
        return mouldInfo;
    }

    /**
     * 构建模具对象最基本信息
     * 模具编码、类型、已排产完毕日
     * 已硫化时间、连续排产日数、日排产列表
     *
     * @param baseInfo
     * @return
     */
    public static MouldInfoVO buildBaseMouldInfo(MouldInfoVO baseInfo) {
        MouldInfoVO mouldInfo = new MouldInfoVO();
        mouldInfo.setMouldCode(baseInfo.getMouldCode());
        mouldInfo.setMouldType(baseInfo.getMouldType());
        mouldInfo.setFactoryCode(baseInfo.getFactoryCode());
        mouldInfo.setMouldNo(baseInfo.getMouldNo());
        mouldInfo.setProductionFinishDayList(new HashSet<>());
        mouldInfo.setUsedSeconds(BigDecimal.ZERO);
        mouldInfo.setContinuousDays(BigDecimal.ZERO.intValue());
        mouldInfo.setDayProductionMap(new HashMap<>());
        mouldInfo.setCleanDayList(new HashMap<>());
        //模具类型转换
        setMouldAirType(mouldInfo);
        return mouldInfo;
    }

    /**
     * 设置模具关联的物料编码
     *
     * @param mouldRelationProductMap 模具关联的物料编码集合
     * @param mouldCode               模具号
     * @param productCode             物料编码
     */
    public static void setMouldRelationProduct(Map<String, Set<String>> mouldRelationProductMap, String mouldCode, String productCode) {
        Set<String> productSet = mouldRelationProductMap.get(mouldCode);
        if (null == productSet) {
            productSet = new HashSet<>();
        }
        productSet.add(productCode);
        mouldRelationProductMap.put(mouldCode, productSet);
    }

    /**
     * 根据物料最大可用模具列表、确认模具排产方向
     * 正向数量 = 反向数量，则正向
     * 否则为反向
     *
     * @param maxMouldList 物料对应的最大模具列表
     * @return
     */
    public static ProductionOrientEnum getProductionOrient(List<MouldInfoVO> maxMouldList) {
        List<MouldInfoVO> forwardList = maxMouldList.stream().filter(mouldInfo -> ProductionOrientEnum.FORWARD == mouldInfo.getProductionOrient()).collect(Collectors.toList());
        List<MouldInfoVO> reverseList = maxMouldList.stream().filter(mouldInfo -> ProductionOrientEnum.REVERSE == mouldInfo.getProductionOrient()).collect(Collectors.toList());
        int forwardSize = BigDecimal.ZERO.intValue();
        if (!CollectionUtils.isEmpty(forwardList)) {
            forwardSize = forwardList.size();
        }
        int reverseSize = BigDecimal.ZERO.intValue();
        if (!CollectionUtils.isEmpty(reverseList)) {
            reverseSize = reverseList.size();
        }
        if (forwardSize == reverseSize) {
            return ProductionOrientEnum.FORWARD;
        }
        return ProductionOrientEnum.REVERSE;
    }

    /**
     * 获取下一分组值，取出当前最大的值
     * 根据模具列表，获取其当前最大的groupValue
     * 如果模具列表都没有groupValue，则下一分组值 = 1
     * 否则下一分组值 = max(groupValue) + 1
     *
     * @param maxMouldList
     * @return
     */
    public static int getNextGroupValue(List<MouldInfoVO> maxMouldList) {
        if (CollectionUtils.isEmpty(maxMouldList)) {
            return BigDecimal.ONE.intValue();
        }
        Comparator comparator = Comparator.comparing(MouldInfoVO::getGroupValue, Comparator.nullsFirst(Comparator.naturalOrder()));
        Optional<MouldInfoVO> max = maxMouldList.stream().max(comparator);
        if (max.isPresent()) {
            Integer groupValue = max.get().getGroupValue();
            if (null == groupValue) {
                groupValue = BigDecimal.ZERO.intValue();
            }
            return groupValue + 1;
        }
        return BigDecimal.ONE.intValue();
    }

    /**
     * 判断在productionDate是否已排productCode，不通过模具，查看所有模具排产信息
     *
     * @param productionDate    排产日
     * @param productionContext 排产上下文
     * @param productCode       SAP代码
     * @param isPreFlag         是否预排
     * @param isContinue        是否续作
     * @return
     */
    public static boolean isProductionByProductCode(Integer productionDate, ProductionContext productionContext, String productCode, boolean isPreFlag, Integer isContinue) {
        if (null == productionDate) {
            return false;
        }
        if (productionDate < ProductionConstant.MONTH_START_DAY) {
            return false;
        }
        if (productionDate > productionContext.getMonthDays()) {
            return false;
        }
        Map<String, MouldInfoVO> mouldInfoMap = productionContext.getMouldInfoMap();
        if (CollectionUtils.isEmpty(mouldInfoMap)) {
            return false;
        }
        for (Map.Entry<String, MouldInfoVO> mouldInfoEntry : mouldInfoMap.entrySet()) {
            MouldInfoVO mouldInfo = mouldInfoEntry.getValue();
            //20250906 ZLT 续作模具续作规格跳过
            if (YesOrNoEnum.YES.getValue().equals(isContinue) && productCode.equals(mouldInfo.getContinueProductCode())) {
                continue;
            }
            if (isPreFlag) {
                Long preProductionQty = getPreProductionQtyByMould(productionContext, mouldInfoEntry.getKey(), productionDate);
                if (preProductionQty > BigDecimal.ZERO.longValue()) {
                    return true;
                }
            }
            List<MouldDayProductionVo> mouldDayProductionList = mouldInfo.getDayProductionMap().get(productionDate);
            if (CollectionUtils.isEmpty(mouldDayProductionList)) {
                continue;
            }
            //20250524 ZLT 提取productCode排产信息
            List<MouldDayProductionVo> productionProductList = mouldDayProductionList.stream().filter(dayProductionInfo -> dayProductionInfo.getProductCode().equals(productCode)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(productionProductList)) {
                continue;
            }
            for (MouldDayProductionVo mouldDayProduction : productionProductList) {
                Long productionQty = mouldDayProduction.getProductionQty();
                if (null != productionQty && productionQty > BigDecimal.ZERO.longValue()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断在productionDate是否已排productCode，通过模具
     *
     * @param productionDate    排产日
     * @param productionContext 排产上下文
     * @param productCode       SAP代码
     * @param isPreFlag         是否预排
     * @return
     */
    public static boolean isProductionByProductCodeAndMould(Integer productionDate, ProductionContext productionContext, String productCode, boolean isPreFlag, MouldInfoVO... mouldInfoList) {
        if (null == productionDate) {
            return false;
        }
        if (productionDate < ProductionConstant.MONTH_START_DAY) {
            return false;
        }
        if (productionDate > productionContext.getMonthDays()) {
            return false;
        }
        if (null == mouldInfoList || mouldInfoList.length <= BigDecimal.ZERO.intValue()) {
            return false;
        }
        for (MouldInfoVO mouldInfo : mouldInfoList) {
            if (isPreFlag) {
                Long preProductionQty = getPreProductionQtyByMould(productionContext, mouldInfo.getMouldCode(), productionDate);
                if (preProductionQty > BigDecimal.ZERO.longValue()) {
                    return true;
                }
            }
            List<MouldDayProductionVo> mouldDayProductionList = mouldInfo.getDayProductionMap().get(productionDate);
            if (CollectionUtils.isEmpty(mouldDayProductionList)) {
                continue;
            }
            //20250524 ZLT 提取productCode排产信息
            List<MouldDayProductionVo> productionProductList = mouldDayProductionList.stream().filter(dayProductionInfo -> dayProductionInfo.getProductCode().equals(productCode)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(productionProductList)) {
                continue;
            }
            for (MouldDayProductionVo mouldDayProduction : productionProductList) {
                Long productionQty = mouldDayProduction.getProductionQty();
                if (null != productionQty && productionQty > BigDecimal.ZERO.longValue()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断在productionDate是否已排productCode，不通过模具，查看所有模具排产信息
     *
     * @param productionDate    排产日
     * @param productionContext 排产上下文
     * @param productCode       SAP代码
     * @param isPreFlag         是否预排
     * @param embryoCode        生胎代码
     * @return
     */
    public static boolean isProduction(Integer productionDate, ProductionContext productionContext, String productCode, boolean isPreFlag, String embryoCode) {
        if (null == productionDate) {
            return false;
        }
        if (productionDate < ProductionConstant.MONTH_START_DAY) {
            return false;
        }
        if (productionDate > productionContext.getMonthDays()) {
            return false;
        }
        Map<String, MouldInfoVO> mouldInfoMap = productionContext.getMouldInfoMap();
        if (CollectionUtils.isEmpty(mouldInfoMap)) {
            return false;
        }
        for (Map.Entry<String, MouldInfoVO> mouldInfoEntry : mouldInfoMap.entrySet()) {
            if (isPreFlag) {
                Long preProductionQty = getPreProductionQtyByMould(productionContext, mouldInfoEntry.getKey(), productionDate);
                if (preProductionQty > BigDecimal.ZERO.longValue()) {
                    return true;
                }
            }
            List<MouldDayProductionVo> mouldDayProductionList = mouldInfoEntry.getValue().getDayProductionMap().get(productionDate);
            if (CollectionUtils.isEmpty(mouldDayProductionList)) {
                continue;
            }
            //20250524 ZLT 提取productCode排产信息
            List<MouldDayProductionVo> productionProductList = mouldDayProductionList.stream().filter(dayProductionInfo -> dayProductionInfo.getProductCode().equals(productCode)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(productionProductList)) {
                //20250624 ZLT 获取生胎排产信息
                productionProductList = mouldDayProductionList.stream().filter(embryoProductionInfo -> embryoCode.equals(embryoProductionInfo.getEmbryoCode())).collect(Collectors.toList());
            }
            if (CollectionUtils.isEmpty(productionProductList)) {
                continue;
            }
            for (MouldDayProductionVo mouldDayProduction : productionProductList) {
                Long productionQty = mouldDayProduction.getProductionQty();
                if (null != productionQty && productionQty > BigDecimal.ZERO.longValue()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断在productionDate是否已排productCode
     *
     * @param productionDate    排产日
     * @param productionContext 排产上下文
     * @param productCode       物料编码
     * @param isPreFlag         是否预排
     * @param embryoCode        生胎代码
     * @param mouldInfoList     排产模具
     * @return
     */
    public static boolean isProduction(Integer productionDate, ProductionContext productionContext, String productCode, boolean isPreFlag, String embryoCode, MouldInfoVO... mouldInfoList) {
        if (null == productionDate) {
            return false;
        }
        if (productionDate < ProductionConstant.MONTH_START_DAY) {
            return false;
        }
        if (productionDate > productionContext.getMonthDays()) {
            return false;
        }
        if (null == mouldInfoList || mouldInfoList.length == 0) {
            return false;
        }
        Long qty = BigDecimal.ZERO.longValue();
        for (MouldInfoVO mouldInfo : mouldInfoList) {
            //加入预排，因每次预排之后清空，故而不会出现换规格的预排
            if (isPreFlag) {
                Long preProductionQty = getPreProductionQtyByMould(productionContext, mouldInfo.getMouldCode(), productionDate);
                qty = qty + preProductionQty;
            }
            List<MouldDayProductionVo> mouldDayProductionList = mouldInfo.getDayProductionMap().get(productionDate);
            if (CollectionUtils.isEmpty(mouldDayProductionList)) {
                continue;
            }
            //20250524 ZLT 提取productCode排产信息
            List<MouldDayProductionVo> productionProductList = mouldDayProductionList.stream().filter(dayProductionInfo -> dayProductionInfo.getProductCode().equals(productCode)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(productionProductList)) {
                //20250624 ZLT 获取生胎排产信息
                productionProductList = mouldDayProductionList.stream().filter(embryoProductionInfo -> embryoCode.equals(embryoProductionInfo.getEmbryoCode())).collect(Collectors.toList());
            }
            if (CollectionUtils.isEmpty(productionProductList)) {
                continue;
            }
            for (MouldDayProductionVo mouldDayProduction : productionProductList) {
                qty = qty + mouldDayProduction.getProductionQty();
            }
        }
        return qty > 0;
    }

    /**
     * 根据模具信息构建模具日排产信息对象
     *
     * @param productionPlan          排产计划信息
     * @param mouldInfo               模具信息
     * @param productConstructionInfo 施工信息
     * @param productionInfo          排产信息
     */
    public static MouldDayProductionVo buildMouldDayProductionInfo(MonthPlanManufacturingRequirementVo productionPlan, MouldInfoVO mouldInfo, ProductConstructionInfoVo productConstructionInfo, ProductionInfoVo productionInfo) {
        MouldDayProductionVo dayProduction = new MouldDayProductionVo();
        //排产信息
        dayProduction.setProductionDate(productionInfo.getProductionDate());
        dayProduction.setProductionType(productionInfo.getProductionType().getType());
        dayProduction.setProductionQty(productionInfo.getProductionQty());
        dayProduction.setUsedCuringTime(productionInfo.getUsedCuringTime());
        //计划信息
        dayProduction.setMonthPlanId(productionPlan.getMonthPlanId());
        dayProduction.setProSize(productionPlan.getProSize());
        dayProduction.setProductCode(productionPlan.getProductCode());
        dayProduction.setSpecCode(productionPlan.getSpecCode());
        dayProduction.setEmbryoCode(productionPlan.getEmbryoCode());
        dayProduction.setMouldMethod(productionPlan.getMouldMethod());
        //模具信息
        dayProduction.setMouldNo(mouldInfo.getMouldNo());
        dayProduction.setMouldCode(mouldInfo.getMouldCode());
        return dayProduction;
    }

    /**
     * 共用模具计划占用产能
     *
     * @param shareMouldRequirePlanList 共用模具计划
     * @param productionContext         排产上下文
     */
    public static void generalPreemptCapacity(List<MonthPlanManufacturingRequirementVo> shareMouldRequirePlanList, ProductionContext productionContext) {
        if (CollectionUtils.isEmpty(shareMouldRequirePlanList)) {
            return;
        }
        //再次预占
        shareMouldRequirePlanList.sort(Comparator.comparing(MonthPlanManufacturingRequirementVo::getProductionSequence));
        Map<String, MouldInfoVO> allMouldMap = productionContext.getMouldInfoMap();
        //对模具预占产能并重新设置计划的需排产量
        shareMouldRequirePlanList.stream().forEach(shareMouldPlan -> {
            //获取计划对应的模具
            String productCode = shareMouldPlan.getProductCode();
            String specCode = shareMouldPlan.getSpecCode();
            Set<String> mouldCodeSet = productionContext.getProductRelationSpecCodeMouldMap().get(productCode).get(specCode);
            if (CollectionUtils.isEmpty(mouldCodeSet)) {
                return;
            }
            BigDecimal singleCuringTime = ProductUtils.getSingleCuringTime(shareMouldPlan, productionContext);
            Map<String, MouldInfoVO> enableMouldMap = getLeftOverMouldInfo(allMouldMap, mouldCodeSet, singleCuringTime);
            //续作计划已经占用量
            Long continueMouldPreemptQty = shareMouldPlan.getContinueMouldPreemptQty();
            if (null == continueMouldPreemptQty) {
                continueMouldPreemptQty = BigDecimal.ZERO.longValue();
            }
            if (CollectionUtils.isEmpty(enableMouldMap)) {
                setMouldPreemptQtyReason(shareMouldPlan, continueMouldPreemptQty);
                return;
            }
            int mouldSize = enableMouldMap.size();
            shareMouldPlan.getPreemptMouldCodeSet().addAll(enableMouldMap.keySet());
            Long needProductionQty = shareMouldPlan.getProductionQty() - continueMouldPreemptQty;
            Long singleQty = preemptSingleMouldQty(enableMouldMap, needProductionQty, singleCuringTime);
            Long realPreemptQty = singleQty * mouldSize;
            BigDecimal realSinglePreemptTime = singleCuringTime.multiply(BigDecimal.valueOf(singleQty));
            if (needProductionQty > realPreemptQty) {
                realPreemptQty = realPreemptQty + continueMouldPreemptQty;
                setMouldPreemptQtyReason(shareMouldPlan, realPreemptQty);
            }
            //模具更新预占产能
            enableMouldMap.entrySet().stream().forEach(entry -> {
                MouldInfoVO mouldInfo = entry.getValue();
                BigDecimal preemptLeftOverSeconds = mouldInfo.getPreemptLeftOverSeconds();
                mouldInfo.setPreemptLeftOverSeconds(preemptLeftOverSeconds.subtract(realSinglePreemptTime));
            });
        });
    }

    /**
     * 获取还有剩余硫化时间的模具信息
     *
     * @param mouldInfoMap     所有模具集合
     * @param mouldCodeSet     模具编号集合
     * @param singleCuringTime 单条硫化时间(包含间隔)
     * @return
     */
    public static Map<String, MouldInfoVO> getLeftOverMouldInfo(Map<String, MouldInfoVO> mouldInfoMap, Set<String> mouldCodeSet, BigDecimal singleCuringTime) {
        if (CollectionUtils.isEmpty(mouldCodeSet) || CollectionUtils.isEmpty(mouldInfoMap) || null == singleCuringTime) {
            return Collections.emptyMap();
        }
        Map<String, MouldInfoVO> enableMouldInfo = new HashMap<>();
        for (String mouldCode : mouldCodeSet) {
            MouldInfoVO mouldInfo = mouldInfoMap.get(mouldCode);
            if (null == mouldInfo) {
                continue;
            }
            BigDecimal preemptLeftOverTime = mouldInfo.getPreemptLeftOverSeconds();
            if (preemptLeftOverTime.compareTo(singleCuringTime) <= 0) {
                continue;
            }
            enableMouldInfo.put(mouldCode, mouldInfo);
        }
        return enableMouldInfo;
    }

    /**
     * 计算抢占单副模具产能量
     *
     * @param enableMouldMap    抢占的模具集合
     * @param needProductionQty 需要抢占的计划量
     * @param singleCuringTime  单条硫化时间(包含增加间隔时间)
     * @return
     */
    public static Long preemptSingleMouldQty(Map<String, MouldInfoVO> enableMouldMap, Long needProductionQty, BigDecimal singleCuringTime) {
        int mouldSize = enableMouldMap.size();
        BigDecimal sumCuringTime = singleCuringTime.multiply(BigDecimal.valueOf(needProductionQty));
        BigDecimal totalLeftOverTime = getTotalPreemptLeftOverTime(enableMouldMap);
        BigDecimal preemptTime = sumCuringTime;
        if (totalLeftOverTime.compareTo(preemptTime) < 0) {
            preemptTime = totalLeftOverTime;
        }
        BigDecimal singlePreemptTime = preemptTime.divide(BigDecimal.valueOf(mouldSize), 0, RoundingMode.DOWN);
        Long singleQty = singlePreemptTime.divide(singleCuringTime, 0, RoundingMode.DOWN).longValue();
        if ((needProductionQty % mouldSize) != BigDecimal.ZERO.intValue()) {
            singleQty = singleQty + BigDecimal.ONE.longValue();
        }
        return singleQty;
    }

    /**
     * 获取换规格消耗的产能时间，单位到秒
     * 消耗时长 * (工作时限 / 24) * 3600
     *
     * @param productionContext
     * @return
     */
    public static BigDecimal getChangeProductConsumeTime(ProductionContext productionContext) {
        BigDecimal subTime = (BigDecimal) productionContext.getFactoryParams().get(FactoryConstant.SYS_CHANGE_PRODUCT_SUB_HOURS);
        if (null == subTime) {
            subTime = BigDecimal.ZERO;
        }
        return getSubtractConsumeTime(productionContext, subTime);
    }

    /**
     * 获取洗模需要扣减的产能
     * 消耗时长 * (工作时限 / 24) * 3600
     *
     * @param productionContext 排产上下文
     * @return
     */
    public static BigDecimal getCleaningMouldConsumeTime(ProductionContext productionContext) {
        BigDecimal subTime = (BigDecimal) productionContext.getFactoryParams().get(FactoryConstant.SYS_PARAM_CLEANING_DAY_LEFT_OVER_HOURS);
        if (null == subTime) {
            return ProductionProcessUtils.getDayWorkHours(productionContext);
        }
        return getSubtractConsumeTime(productionContext, subTime);
    }

    /**
     * 判断在productionDate中productCode是否为换规格
     * 20250325 增加月初第一天的换规格判断，
     * 1.1、如果是续作模具，则第一个规格与续作规格一致，则不换规格，否则需要换规格
     * 1.2、如果不是续作模具，则第一个规格就是换规格
     *
     * @param mouldInfo         模具信息，包含了日排产信息
     * @param productCode       排产规格
     * @param productionDate    排产日
     * @param productionContext 排产上下文
     * @return true 表示换规格 false表示不换规格
     */
    public static boolean isChangeProductCode(MouldInfoVO mouldInfo, String productCode, Integer productionDate, ProductionContext productionContext) {
        if (StringUtils.isBlank(productCode) || null == productionDate) {
            return false;
        }
        if (null == mouldInfo) {
            return false;
        }
        List<MouldDayProductionVo> dayProductionList = mouldInfo.getDayProductionMap().get(productionDate);
        if (ProductionConstant.MONTH_START_DAY.equals(productionDate)) {
            //月初第一天: 续作模具空排产
            if (CollectionUtils.isEmpty(dayProductionList) && YesOrNoEnum.YES.getValue().equals(mouldInfo.getIsContinue())) {
                return !productCode.equals(mouldInfo.getContinueProductCode());
            }
            //月初第一天:非续作模具空排产,20250521 则认为换规格
            if (CollectionUtils.isEmpty(dayProductionList) && !YesOrNoEnum.YES.getValue().equals(mouldInfo.getIsContinue())) {
                return true;
            }
            //月初第一天: 模具有排产，判断最后一个规格是否一致
            String lastProductionCode = dayProductionList.get(dayProductionList.size() - 1).getProductCode();
            return !productCode.equals(lastProductionCode);
        }
        //不是月初第一天 20250521 正向排产，如果从没有排产，则认为换规格
        if (ProductionOrientEnum.FORWARD == mouldInfo.getProductionOrient() && CollectionUtils.isEmpty(mouldInfo.getDayProductionMap())) {
            return true;
        }
        //不是月初第一天 正向排产空列表 需判断前一天是否排产过
        if (ProductionOrientEnum.FORWARD == mouldInfo.getProductionOrient() && CollectionUtils.isEmpty(dayProductionList)) {
            Integer beforeDate = productionDate - BigDecimal.ONE.intValue();
            List<MouldDayProductionVo> beforeDayProductionList = mouldInfo.getDayProductionMap().get(beforeDate);
            if (CollectionUtils.isEmpty(beforeDayProductionList)) {
                return true;
            }
            String lastProductionCode = beforeDayProductionList.get(beforeDayProductionList.size() - 1).getProductCode();
            return !productCode.equals(lastProductionCode);
        }
        //不是月初第一天 反向排产空列表--需判断前一天是否已经排产完
        if (ProductionOrientEnum.REVERSE == mouldInfo.getProductionOrient() && CollectionUtils.isEmpty(dayProductionList)) {
            Integer beforeDate = productionDate + BigDecimal.ONE.intValue();
            if (beforeDate > productionContext.getMonthDays()) {
                return false;
            }
            List<MouldDayProductionVo> beforeDayProductionList = mouldInfo.getDayProductionMap().get(beforeDate);
            if (CollectionUtils.isEmpty(beforeDayProductionList)) {
                return false;
            }
            String lastProductionCode = beforeDayProductionList.get(beforeDayProductionList.size() - 1).getProductCode();
            return !productCode.equals(lastProductionCode);
        }
        String lastProductionCode = dayProductionList.get(dayProductionList.size() - 1).getProductCode();
        return !productCode.equals(lastProductionCode);
    }

    /**
     * 根据模具的汽套类型，设置模具汽套类型枚举实例对象
     *
     * @param mouldInfo
     */
    private static void setMouldAirType(MouldInfoVO mouldInfo) {
        String mouldAirType = mouldInfo.getMouldAirType();
        if (StringUtils.isBlank(mouldAirType)) {
            mouldInfo.setMouldAirType(MouldAirTypeEnum.NORMAL.getValue());
            return;
        }
        //非弹簧汽套模具->普通模具
        MouldAirTypeEnum mouldAirTypeEnum = MouldAirTypeEnum.getEnumByValue(mouldInfo.getMouldAirType());
        if (MouldAirTypeEnum.NO_AIR == mouldAirTypeEnum) {
            mouldInfo.setMouldAirType(MouldAirTypeEnum.NORMAL.getValue());
            return;
        }
        mouldInfo.setMouldAirType(mouldAirTypeEnum.getValue());
    }

    /**
     * 根据参数，获取参数扣减消耗的时长，根据最大工作时限，等比例替换
     *
     * @param productionContext 排产上下文
     * @param subTime           扣减产能消耗时长
     * @return
     */
    private static BigDecimal getSubtractConsumeTime(ProductionContext productionContext, BigDecimal subTime) {
        if (null == subTime) {
            return BigDecimal.ZERO;
        }
        BigDecimal hourTime = BigDecimal.valueOf(ProductionConstant.HOUR_SECOND);
        //转换成秒
        subTime = subTime.multiply(hourTime);
        //一天最大小时数-24
        BigDecimal dayMaxHours = BigDecimal.valueOf(ProductionConstant.MAX_DAY_HOURS);
        //最大工作时限
        BigDecimal workHours = (BigDecimal) productionContext.getFactoryParams().get(FactoryConstant.SYS_PARAM_DAY_WORK_HOURS);
        if (null == workHours) {
            workHours = dayMaxHours;
        }
        //扣减不超过1天最大工作时限
        BigDecimal dayMaxWorkTime = dayMaxHours.multiply(hourTime);
        if (subTime.compareTo(dayMaxWorkTime) > 0) {
            subTime = dayMaxWorkTime;
        }
        BigDecimal dayRate = workHours.divide(dayMaxHours, 4, RoundingMode.HALF_UP);
        return subTime.multiply(dayRate).setScale(0, RoundingMode.UP);
    }

    /**
     * 获取对应模具的总的预占剩余硫化时间
     *
     * @param mouldInfoMap 所有模具信息
     * @return
     */
    private static BigDecimal getTotalPreemptLeftOverTime(Map<String, MouldInfoVO> mouldInfoMap) {
        if (CollectionUtils.isEmpty(mouldInfoMap)) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalPreemptLeftOverTime = BigDecimal.ZERO;
        for (Map.Entry<String, MouldInfoVO> entry : mouldInfoMap.entrySet()) {
            MouldInfoVO mouldInfo = entry.getValue();
            if (null == mouldInfo) {
                continue;
            }
            BigDecimal preemptLeftOverTime = mouldInfo.getPreemptLeftOverSeconds();
            if (null == preemptLeftOverTime) {
                preemptLeftOverTime = BigDecimal.ZERO;
            }
            totalPreemptLeftOverTime = totalPreemptLeftOverTime.add(preemptLeftOverTime);
        }
        return totalPreemptLeftOverTime;
    }

    /**
     * 设置扣除模具预占产能不足原因
     *
     * @param shareMouldPlan 共用模具计划
     * @param realPreemptQty 剩余预计可排产量
     */
    private static void setMouldPreemptQtyReason(MonthPlanManufacturingRequirementVo shareMouldPlan, Long realPreemptQty) {
        shareMouldPlan.setProductionQty(realPreemptQty);
        Long noProductionQty = shareMouldPlan.getFactProdReqQty() - shareMouldPlan.getProductionQty();
        if (noProductionQty > BigDecimal.ONE.longValue()) {
            //扣除超出模具产能数:%1$s.
            String noProductionReason = NoProductionReasonUtils.getOverModCaps(noProductionQty);
            shareMouldPlan.addNoProductionReasonAndQty(noProductionReason, noProductionQty);
        }
    }

    /**
     * 加入预排处理
     *
     * @param productionContext
     * @param mouldCode
     */
    private static Long getPreProductionQtyByMould(ProductionContext productionContext, String mouldCode, Integer preProductionDate) {
        if (StringUtils.isBlank(mouldCode) || null == preProductionDate) {
            return BigDecimal.ZERO.longValue();
        }
        Map<String, Map<Integer, Long>> mouldPreProductionDateQtyMap = productionContext.getMouldPreProductionDateQtyMap();
        if (CollectionUtils.isEmpty(mouldPreProductionDateQtyMap)) {
            return BigDecimal.ZERO.longValue();
        }
        Map<Integer, Long> mouldPreProductionQtyMap = mouldPreProductionDateQtyMap.get(mouldCode);
        if (CollectionUtils.isEmpty(mouldPreProductionQtyMap)) {
            return BigDecimal.ZERO.longValue();
        }
        Long preProductionQty = mouldPreProductionQtyMap.get(preProductionDate);
        if (null == preProductionQty) {
            return BigDecimal.ZERO.longValue();
        }
        return preProductionQty;
    }

    private MouldBaseUtils() {

    }
}

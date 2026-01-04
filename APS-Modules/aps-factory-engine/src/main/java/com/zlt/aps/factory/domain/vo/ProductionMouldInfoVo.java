package com.zlt.aps.factory.domain.vo;

import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.CxLhProductionHelper;
import com.zlt.aps.factory.domain.dto.CxMouldDayProductionHelper;
import com.zlt.aps.factory.domain.dto.GroupPlanDayProductionInfoHelper;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.enums.MouldRelationTypeEnum;
import com.zlt.aps.factory.utils.DateUtils;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 排产模具信息对象
 * 根据SKU与模具关系转化，以模具编号为唯一关系，记录模具信息
 * 包含其共用的物料
 *
 * @author ZLT
 * @date 20251218
 */
@Data
public class ProductionMouldInfoVo implements Serializable {

    /**
     * 型腔模号-唯一性
     */
    private String mouldCode;
    /**
     * 关系类型 01 sku与模具关系 02 新模具到货计划
     */
    private MouldRelationTypeEnum relationType;
    /**
     * 关联的物料集合
     */
    private Set<String> associationMaterialSet;
    /**
     * 可排产日集合信息
     */
    private Set<Integer> productionDaySet;
    /**
     * 排产完毕日集合信息
     */
    private Set<Integer> finishDaySet;
    /**
     * 日排产信息
     */
    private Map<Integer, List<CxMouldDayProductionHelper>> dayProductionInfo;

    /**
     * 创建空的排产模具信息
     * 只包含型腔模号及relationType类型
     *
     * @param mouldCode    型腔模号
     * @param relationType 关系类型
     * @return
     */
    public static ProductionMouldInfoVo createEmptyProductionMouldInfo(String mouldCode, MouldRelationTypeEnum relationType) {
        if (StringUtils.isBlank(mouldCode)) {
            return null;
        }
        ProductionMouldInfoVo productionMouldInfo = new ProductionMouldInfoVo();
        productionMouldInfo.setMouldCode(mouldCode);
        if (null == relationType) {
            productionMouldInfo.setRelationType(MouldRelationTypeEnum.SKU_RELATION_CONFIGURATION);
        } else {
            productionMouldInfo.setRelationType(relationType);
        }
        //可排产日信息
        productionMouldInfo.setProductionDaySet(new HashSet<>(64));
        //关联SKU
        productionMouldInfo.setAssociationMaterialSet(new HashSet<>(32));
        //排产完毕日
        productionMouldInfo.setFinishDaySet(new HashSet<>(64));
        return productionMouldInfo;
    }

    /**
     * 模具增加排产信息
     *
     * @param day                  排产日
     * @param productionPlanInfo   分组计划信息对象
     * @param cxLhProductionHelper 硫化分组
     * @param isFinishDay          天是否排产完毕(包含正常排产完成，因换模或是换活字块导致的完成)
     * @param realDayProductionQty 双模实际排产量
     * @param dayLhQty             双模日硫化量(产能)
     * @param cxMachineCode        成型机台
     * @param continueSkuPlanList  排产计划集合
     */
    @Deprecated
    public void addProductionInfo(Integer day, ProductionPlanGroupInfo productionPlanInfo, CxLhProductionHelper cxLhProductionHelper, boolean isFinishDay, Integer realDayProductionQty, Integer dayLhQty, String cxMachineCode, List<MonthPlanProductionRequirePlanVo> continueSkuPlanList) {
        //加入已经排产完毕
        if (isFinishDay) {
            finishDaySet.add(day);
        }
        if (CollectionUtils.isEmpty(continueSkuPlanList)) {
            return;
        }
        List<MonthPlanProductionRequirePlanVo> hasProductionList = continueSkuPlanList.stream().filter(groupPlan -> groupPlan.hasProduction()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasProductionList)) {
            return;
        }
        //todo 怎么分配
        Map<Long, MonthPlanProductionRequirePlanVo> needDeductionMap = hasProductionList.stream().collect(Collectors.toMap(MonthPlanProductionRequirePlanVo::getMonthPlanId, Function.identity()));
        Map<Long, Integer> realDeductionMap = new HashMap<>();
        //先高优先级，再其他净需求
        List<MonthPlanProductionRequirePlanVo> heightPlanList = hasProductionList.stream().filter(groupPlan -> groupPlan.getHeightProductionQty() > BigDecimal.ZERO.intValue()).collect(Collectors.toList());
        realDayProductionQty = deductionHeightProductionQty(heightPlanList, realDeductionMap, realDayProductionQty);
        if (realDayProductionQty <= BigDecimal.ZERO.intValue()) {
            //增加模具排产信息
            realDeductionMap.forEach((monthPlanId, productionQty) -> {
                MonthPlanProductionRequirePlanVo groupPlan = needDeductionMap.get(monthPlanId);
                CxMouldDayProductionHelper mouldProductionHelper = CxMouldDayProductionHelper.createCxMouldDayProductionInfo(groupPlan, cxMachineCode, day, productionQty, cxLhProductionHelper);
                mouldProductionHelper.setMouldCode(mouldCode);
                addDayProductionInfo(day, mouldProductionHelper);
                //日排产信息
                GroupPlanDayProductionInfoHelper helper = GroupPlanDayProductionInfoHelper.buildDayProductionInfo(groupPlan, cxLhProductionHelper, productionQty, BigDecimal.ZERO.intValue(), null);
                productionPlanInfo.addDayProductionInfo(helper);
            });
            return;
        }
        //再其它净需求
        List<MonthPlanProductionRequirePlanVo> noHeightPlanList = hasProductionList.stream().filter(groupPlan -> groupPlan.getProductionQty() > BigDecimal.ZERO.intValue()).collect(Collectors.toList());
        deductionNoHeightQty(noHeightPlanList, realDeductionMap, realDayProductionQty);
        //增加模具排产信息
        realDeductionMap.forEach((monthPlanId, productionQty) -> {
            MonthPlanProductionRequirePlanVo groupPlan = needDeductionMap.get(monthPlanId);
            CxMouldDayProductionHelper mouldProductionHelper = CxMouldDayProductionHelper.createCxMouldDayProductionInfo(groupPlan, cxMachineCode, day, productionQty, cxLhProductionHelper);
            mouldProductionHelper.setMouldCode(mouldCode);
            addDayProductionInfo(day, mouldProductionHelper);
        });
        //增加日排产信息
        realDeductionMap.forEach((monthPlanId, productionQty) -> {
            MonthPlanProductionRequirePlanVo groupPlan = needDeductionMap.get(monthPlanId);
            GroupPlanDayProductionInfoHelper helper = GroupPlanDayProductionInfoHelper.buildDayProductionInfo(groupPlan, cxLhProductionHelper, productionQty, BigDecimal.ZERO.intValue(), null);
            productionPlanInfo.addDayProductionInfo(helper);
        });
    }

    /**
     * 模具增加排产信息
     *
     * @param day                排产日
     * @param productionPlanInfo 排产计划
     * @param isFinishDay        天是否排产完毕(包含正常排产完成，因换模或是换活字块导致的完成)
     * @param productionQty      双模实际排产量
     * @param cxMachineCodeInfo  成型机台
     */
    public void addProductionInfo(Integer day, MonthPlanProductionRequirePlanVo productionPlanInfo, boolean isFinishDay, Integer productionQty, Set<String> cxMachineCodeInfo) {
        //加入已经排产完毕
        if (isFinishDay) {
            finishDaySet.add(day);
        }
        CxMouldDayProductionHelper mouldProductionHelper = CxMouldDayProductionHelper.createCxMouldDayProductionInfo(productionPlanInfo, cxMachineCodeInfo, day, productionQty);
        mouldProductionHelper.setMouldCode(mouldCode);
        addDayProductionInfo(day, mouldProductionHelper);
    }

    /**
     * 扣减高优先级待排产量
     *
     * @param heightPlanList
     * @param realDeductionMap
     * @param realDayProductionQty
     * @return
     */
    private Integer deductionHeightProductionQty(List<MonthPlanProductionRequirePlanVo> heightPlanList, Map<Long, Integer> realDeductionMap, Integer realDayProductionQty) {
        if (CollectionUtils.isEmpty(heightPlanList)) {
            return realDayProductionQty;
        }
        //高优先级量降序排序
        heightPlanList.sort(Comparator.comparing(MonthPlanProductionRequirePlanVo::getHeightProductionQty, Comparator.reverseOrder()));
        for (MonthPlanProductionRequirePlanVo productionPlan : heightPlanList) {
            if (realDayProductionQty <= BigDecimal.ZERO.intValue()) {
                break;
            }
            Long monthPlanId = productionPlan.getMonthPlanId();
            Integer heightProductionQty = productionPlan.getHeightProductionQty();
            if (heightProductionQty <= BigDecimal.ZERO.intValue()) {
                continue;
            }
            Integer realDeductionQty = Math.min(heightProductionQty, realDayProductionQty);
            if (realDeductionQty > BigDecimal.ZERO.longValue()) {
                //扣减计划需求量，并汇总计划总扣减量
                deductionHeightProductionQty(productionPlan, realDeductionQty);
                Integer sumDeductionQty = realDeductionMap.get(monthPlanId);
                if (null == sumDeductionQty) {
                    sumDeductionQty = BigDecimal.ZERO.intValue();
                }
                sumDeductionQty = sumDeductionQty + realDeductionQty;
                realDeductionMap.put(monthPlanId, sumDeductionQty);
            }
            realDayProductionQty = realDayProductionQty - realDeductionQty;
        }
        return realDayProductionQty;
    }

    /**
     * 增加模具排产信息
     *
     * @param day                   排产天
     * @param mouldProductionHelper 排产信息对象
     */
    private void addDayProductionInfo(Integer day, CxMouldDayProductionHelper mouldProductionHelper) {
        if (null == day || null == mouldProductionHelper) {
            return;
        }
        if (null == dayProductionInfo) {
            dayProductionInfo = new HashMap<>();
        }
        List<CxMouldDayProductionHelper> dayProductionList = dayProductionInfo.get(day);
        if (null == dayProductionList) {
            dayProductionList = new ArrayList<>();
            dayProductionInfo.put(day, dayProductionList);
        }
        dayProductionList.add(mouldProductionHelper);
    }

    /**
     * 扣减非高优先级净需求
     *
     * @param noHeightPlanList     非高优先级需求计划
     * @param realDeductionMap     计划总扣减量
     * @param realDayProductionQty 需扣减量
     * @return
     */
    private Integer deductionNoHeightQty(List<MonthPlanProductionRequirePlanVo> noHeightPlanList, Map<Long, Integer> realDeductionMap, Integer realDayProductionQty) {
        if (realDayProductionQty <= BigDecimal.ZERO.longValue()) {
            return realDayProductionQty;
        }
        if (CollectionUtils.isEmpty(noHeightPlanList)) {
            return realDayProductionQty;
        }
        //非高优先级量降序排序
        noHeightPlanList.sort(Comparator.comparing(MonthPlanProductionRequirePlanVo::getProductionQty, Comparator.reverseOrder()));
        for (MonthPlanProductionRequirePlanVo productionPlan : noHeightPlanList) {
            if (realDayProductionQty <= BigDecimal.ZERO.intValue()) {
                break;
            }
            Long monthPlanId = productionPlan.getMonthPlanId();
            Integer noHeightProductionQty = productionPlan.getProductionQty();
            if (noHeightProductionQty <= BigDecimal.ZERO.intValue()) {
                continue;
            }
            Integer realDeductionQty = Math.min(noHeightProductionQty, realDayProductionQty);
            if (realDeductionQty > BigDecimal.ZERO.intValue()) {
                //扣减计划需求量，并汇总计划总扣减量
                deductionNoHeightProductionQty(productionPlan, realDeductionQty);
                Integer sumDeductionQty = realDeductionMap.get(monthPlanId);
                if (null == sumDeductionQty) {
                    sumDeductionQty = BigDecimal.ZERO.intValue();
                }
                sumDeductionQty = sumDeductionQty + realDeductionQty;
                realDeductionMap.put(monthPlanId, sumDeductionQty);
            }
            realDayProductionQty = realDayProductionQty - realDeductionQty;
        }
        return realDayProductionQty;
    }

    /**
     * 扣减高优先级量
     * 扣减高优先级需要同时扣减总需排产量
     *
     * @param productionPlan   计划
     * @param dayProductionQty 真实日排产量
     */
    private void deductionHeightProductionQty(MonthPlanProductionRequirePlanVo productionPlan, Integer dayProductionQty) {
        Integer heightProductionQty = productionPlan.getHeightProductionQty();
        Integer productionQty = productionPlan.getProductionQty();
        heightProductionQty = heightProductionQty - dayProductionQty;
        productionQty = productionQty - dayProductionQty;
        productionPlan.setHeightProductionQty(heightProductionQty);
        productionPlan.setProductionQty(productionQty);
        if (productionQty <= BigDecimal.ZERO.intValue()) {
            productionPlan.setIsProduction(YesOrNoEnum.NO.getCode());
        }
    }

    /**
     * 扣减非高优先级净需求量
     * 只扣减总需求量的值
     *
     * @param productionPlan   计划
     * @param dayProductionQty 真实日排产量
     */
    private void deductionNoHeightProductionQty(MonthPlanProductionRequirePlanVo productionPlan, Integer dayProductionQty) {
        Integer productionQty = productionPlan.getProductionQty();
        productionQty = productionQty - dayProductionQty;
        productionPlan.setProductionQty(productionQty);
        if (productionQty <= BigDecimal.ZERO.intValue()) {
            productionPlan.setIsProduction(YesOrNoEnum.NO.getCode());
        }
    }

    /**
     * 设置模具的可排产日信息
     * 首次可用日期 = 上机时间 + 1
     *
     * @param context      排产上下文
     * @param boardingDate 上机时间
     */
    public void setProductionDayInfo(Context context, Date boardingDate) {
        //模具关系
        if (relationType == MouldRelationTypeEnum.SKU_RELATION_CONFIGURATION) {
            productionDaySet.addAll(context.getProductionDay());
            return;
        }
        if (null == boardingDate) {
            return;
        }
        //可用时间 = 上机日期 + 1
        Integer startDay = DateUtils.getIntervalDays(context.getProductionStartDate(), boardingDate) + BigDecimal.ONE.intValue();
        Integer monthDays = context.getMonthDays();
        Set<Integer> monthStopDaySet = context.getStopDays();
        for (int day = startDay; day <= monthDays; day++) {
            if (null != monthStopDaySet && monthStopDaySet.contains(day)) {
                continue;
            }
            productionDaySet.add(day);
        }
    }

    /**
     * 模具在startDay~endDay范围内是否可一直排产
     * 即在startDay~endDay可排且没有已经排产完毕的天数
     * true表示可排产，false表示不可排产
     *
     * @param startDay 开始排产日
     * @param endDay   结束排产日
     * @return
     */
    public boolean isProduction(Integer startDay, Integer endDay) {
        if (startDay > endDay) {
            return false;
        }
        for (int day = startDay; day <= endDay; day++) {
            if (finishDaySet.contains(day)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 模具共用性
     * 数字越小，共用性越低
     *
     * @return
     */
    public Integer getCommonalityValue() {
        if (CollectionUtils.isEmpty(associationMaterialSet)) {
            return BigDecimal.ZERO.intValue();
        }
        return associationMaterialSet.size();
    }
}

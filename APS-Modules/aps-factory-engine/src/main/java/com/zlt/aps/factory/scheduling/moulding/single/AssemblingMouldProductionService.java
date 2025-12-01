package com.zlt.aps.factory.scheduling.moulding.single;

import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;
import com.zlt.aps.factory.domain.vo.MouldDayProductionVo;
import com.zlt.aps.factory.domain.vo.MouldInfoVO;
import com.zlt.aps.factory.enums.AssemblingMouldBusinessTypeEnum;
import com.zlt.aps.factory.enums.ProductionOrientEnum;
import com.zlt.aps.factory.scheduling.AbstractProductionBusinessService;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.factory.scheduling.ProductionParamConfiguration;
import com.zlt.aps.factory.scheduling.moulding.SinglePlanProductionContext;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import com.zlt.aps.factory.utils.*;
import com.zlt.aps.monthplan.api.domain.entity.MouldProductionLog;
import com.zlt.aps.monthplan.api.enums.MouldProductionLogType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 拼模排产
 * 1、根据当前排产的规格，查找是否有可拼模的规格
 * 1.1、合模压力相差值100 参数
 * 1.2、硫化时间相差30s 参数
 * 1.3、模具类型一致
 * 1.4、模具行腔一样
 * 1.5、两个规格的需求总量值相差400条 参数
 * 1.6、如果不是单模规格，则两个规格的各自需求总量要小于1000 参数
 * 2、可拼规格后，排产量 = 两个规格的总量取均值，如果均值小于其中的净需求，则取净需求
 * 3、多个可拼规格的选择顺序：
 * 1、优先共生胎，其次同寸口，再次相邻两个寸口
 * 2、差量最小的优先
 * 3、优先级一致优先(指都有优先级的跟有优先级的先拼)
 * 4、拼模排产，最终都是采用单模排产，有可能出现起始时间一致，结束时间不一致
 *
 * @author ZLT
 * @date 20250220
 */
@Slf4j
@Service(value = "assemblingMouldProductionService")
public class AssemblingMouldProductionService extends AbstractProductionBusinessService {

    public AssemblingMouldProductionService(ProductionSchedulingDataService dataService) {
        super(dataService);
    }

    @Override
    public void run(Context context, Object userObj) {
        SinglePlanProductionContext singlePlanProductionContext = (SinglePlanProductionContext) context;
        ProductionContext productionContext = singlePlanProductionContext.getGroupContext().getProductionContext();
        MonthPlanManufacturingRequirementVo productionPlan = singlePlanProductionContext.getProductionPlan();
        String productCode = productionPlan.getProductCode();
        if (productionContext.getNoAssemblingMouldProductSet().contains(productCode)) {
            return;
        }
        //获取前规格所有计划
        List<MonthPlanManufacturingRequirementVo> beforeList = getProductionProductCodePlanList(productionContext, productCode);
        if (CollectionUtils.isEmpty(beforeList)) {
            return;
        }
        List<MouldInfoVO> enableMouldList = singlePlanProductionContext.getEnableMouldList();
        //前规格使用的拼模排产模具
        MouldInfoVO beforeMould = getBeforeProductMould(productionContext, enableMouldList, beforeList);
        if (null == beforeMould) {
            productionContext.getNoAssemblingMouldProductSet().add(productCode);
            return;
        }
        //获取符合条件的可拼模规格计划--只取一个规格
        List<MonthPlanManufacturingRequirementVo> assemblingMouldPlanList = findAssemblingMouldPlanList(productionContext, productionPlan, enableMouldList, beforeMould);
        if (CollectionUtils.isEmpty(assemblingMouldPlanList)) {
            productionContext.getNoAssemblingMouldProductSet().add(productCode);
            clearAssemblingMouldInfoByAfter(beforeList);
            ProductionLogUtils.addNoAssemblingMouldLog(productionContext, productionPlan);
            return;
        }
        //拼模规格使用的排产模具
        List<MouldInfoVO> assemblingMouldList = assemblingMouldPlanList.get(0).getEnableAssemblingMouldList();
        if (CollectionUtils.isEmpty(assemblingMouldList)) {
            productionContext.getNoAssemblingMouldProductSet().add(productCode);
            clearAssemblingMouldInfoByAfter(beforeList);
            ProductionLogUtils.addNoAssemblingMouldByMouldLog(productionContext, productionPlan);
            return;
        }
        assemblingMouldList.sort(Comparator.comparing(MouldInfoVO::getAssocaiationCount).thenComparing(MouldInfoVO::getLeftOverSeconds, Comparator.reverseOrder()));
        MouldInfoVO assemblingMouldInfo = assemblingMouldList.get(0);
        if (null == assemblingMouldInfo) {
            productionContext.getNoAssemblingMouldProductSet().add(productCode);
            clearAssemblingMouldInfoByAfter(beforeList);
            clearAssemblingMouldInfoByAfter(assemblingMouldPlanList);
            ProductionLogUtils.addNoAssemblingMouldByMouldLog(productionContext, productionPlan);
            return;
        }
        //重新设置排产量,均值与最大净需求，取最大
        MonthPlanManufacturingRequirementVo assemblingMouldPlan = assemblingMouldPlanList.get(0);
        Long averageProductionQty = ((productionPlan.getSummaryProductionQty() + assemblingMouldPlan.getSummaryProductionQty()) / 2 * 2) / 2;
        Long maxNetDemandQty = Math.max(productionPlan.getSummaryNetDemandQty(), assemblingMouldPlan.getSummaryNetDemandQty());
        Long finalProductionQty = Math.max(maxNetDemandQty, averageProductionQty);
        redistributionProductionQty(beforeList, finalProductionQty);
        redistributionProductionQty(assemblingMouldPlanList, finalProductionQty);
        //设置拼模排产
        productionContext.setAssemblingMouldProduction(true);
        productionContext.setAssemblingMouldNextProductCode(false);
        productionContext.setAssemblingMouldStartDay(null);
        //进行排摸排产：先对前规格单模排产 再对拼模规格单模排产
        String remark = String.format("[%s,%s]进行拼模排产", productionPlan.getProductCode(), assemblingMouldPlan.getProductCode());
        assemblingMouldProduction(beforeList, beforeMould, productionContext, remark, assemblingMouldPlanList, assemblingMouldInfo);
        //拼模结束后
        productionContext.setAssemblingMouldProduction(false);
        productionContext.setAssemblingMouldNextProductCode(false);
        productionContext.setAssemblingMouldStartDay(null);
        clearAssemblingMouldInfoByAfter(beforeList);
        clearAssemblingMouldInfoByAfter(assemblingMouldPlanList);
    }

    /**
     * 获取未排计划中productCode的所有计划
     *
     * @param productionContext 排产上下文
     * @param productCode       SAP代码
     * @return
     */
    private List<MonthPlanManufacturingRequirementVo> getProductionProductCodePlanList(ProductionContext productionContext, String productCode) {
        List<MonthPlanManufacturingRequirementVo> allProductionPlanList = productionContext.getMonthPlanInitMap().values().stream().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(allProductionPlanList)) {
            return Collections.emptyList();
        }
        return getNoProductionPlanByProductCode(allProductionPlanList, productCode);
    }

    /**
     * 获取前规格的排产模具信息
     * 拼模排产是单模，故而先要有模具能满足其量
     *
     * @param productionContext 排产上下文
     * @param enableMouldList   可排产模具
     * @param beforeList        排产计划
     * @return
     */
    private MouldInfoVO getBeforeProductMould(ProductionContext productionContext, List<MouldInfoVO> enableMouldList, List<MonthPlanManufacturingRequirementVo> beforeList) {
        if (CollectionUtils.isEmpty(enableMouldList)) {
            return null;
        }
        //单模
        if (enableMouldList.size() == BigDecimal.ONE.intValue()) {
            MouldInfoVO singleMould = enableMouldList.get(0);
            if (isProductionCapacityByMould(productionContext, singleMould, beforeList)) {
                return singleMould;
            }
            return null;
        }
        //多模
        List<MouldInfoVO> capacityList = new ArrayList<>();
        for (MouldInfoVO mouldInfo : enableMouldList) {
            if (isProductionCapacityByMould(productionContext, mouldInfo, beforeList)) {
                capacityList.add(mouldInfo);
            }
        }
        if (CollectionUtils.isEmpty(capacityList)) {
            return null;
        }
        //取关联数少 剩余产能多的-即优先使用不共用的模具
        capacityList.sort(Comparator.comparing(MouldInfoVO::getAssocaiationCount).thenComparing(MouldInfoVO::getLeftOverSeconds, Comparator.reverseOrder()));
        return capacityList.get(0);
    }

    /**
     * 根据当前需排产的规格，查找可拼模的规格计划
     *
     * @param productionContext 排产上下文
     * @param productionPlan    当前需排产计划
     * @param mouldList         前规格模具列表信息
     * @return
     */
    private List<MonthPlanManufacturingRequirementVo> findAssemblingMouldPlanList(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, List<MouldInfoVO> mouldList, MouldInfoVO beforeMould) {
        if (CollectionUtils.isEmpty(mouldList)) {
            return Collections.emptyList();
        }
        //如果是单模规格
        if (mouldList.size() == BigDecimal.ONE.intValue()) {
            return findAssemblingSingleMould(productionContext, productionPlan, beforeMould);
        }
        //多模，量小的规格拼模排产
        return findAssemblingBySmallAmount(productionContext, productionPlan, beforeMould);
    }

    /**
     * 清空计划的拼模信息
     *
     * @param assemblingMouldPlanList 拼模计划
     */
    private void clearAssemblingMouldInfoByAfter(List<MonthPlanManufacturingRequirementVo> assemblingMouldPlanList) {
        if (CollectionUtils.isEmpty(assemblingMouldPlanList)) {
            return;
        }
        MonthPlanManufacturingRequirementVo plan = assemblingMouldPlanList.get(0);
        List<MouldInfoVO> enableAssemblingMouldList = plan.getEnableAssemblingMouldList();
        if (!CollectionUtils.isEmpty(enableAssemblingMouldList)) {
            enableAssemblingMouldList.stream().forEach(assemblingMould -> assemblingMould.setAssemblingMouldStartDay(null));
        }
        assemblingMouldPlanList.stream().forEach(assemblingMouldPlan -> assemblingMouldPlan.setEnableAssemblingMouldList(null));
    }

    /**
     * 对需排产计划，重新分配可排产量
     *
     * @param needProductionPlanList 需排产计划
     * @param finalProductionQty     可排产量
     */
    private void redistributionProductionQty(List<MonthPlanManufacturingRequirementVo> needProductionPlanList, Long finalProductionQty) {
        if (CollectionUtils.isEmpty(needProductionPlanList) || null == finalProductionQty) {
            return;
        }
        needProductionPlanList.sort(Comparator.comparing(MonthPlanManufacturingRequirementVo::getProductionSequence));
        Long sumProductionQty = finalProductionQty;
        int size = needProductionPlanList.size();
        int index = BigDecimal.ZERO.intValue();
        for (MonthPlanManufacturingRequirementVo needProductionPlan : needProductionPlanList) {
            index = index + 1;
            needProductionPlan.saveBeforeAssemblingMouldPlanInfo();
            Long productionQty = needProductionPlan.getProductionQty();
            if (sumProductionQty <= BigDecimal.ZERO.longValue()) {
                needProductionPlan.setProductionQty(BigDecimal.ZERO.longValue());
                String noProductionReason = NoProductionReasonUtils.getAssemblingMouldCapacity(productionQty);
                needProductionPlan.addNoProductionReasonAndQty(noProductionReason, productionQty);
                continue;
            }
            if (productionQty > sumProductionQty) {
                Long subtractQty = productionQty - sumProductionQty;
                needProductionPlan.setProductionQty(sumProductionQty);
                String noProductionReason = NoProductionReasonUtils.getAssemblingMouldCapacity(subtractQty);
                needProductionPlan.addNoProductionReasonAndQty(noProductionReason, sumProductionQty);
                sumProductionQty = BigDecimal.ZERO.longValue();
                continue;
            }
            if (index == size) {
                needProductionPlan.setProductionQty(sumProductionQty);
            }
            sumProductionQty = sumProductionQty - productionQty;
        }
    }

    /**
     * 执行拼模排产
     *
     * @param beforeList              前规格排产集合
     * @param beforeMould             排产模具
     * @param productionContext       排产上下文
     * @param remark                  拼模备注
     * @param assemblingMouldPlanList 后规格排产集合
     * @param assemblingMouldInfo     后规格排产模具
     */
    private void assemblingMouldProduction(List<MonthPlanManufacturingRequirementVo> beforeList, MouldInfoVO beforeMould, ProductionContext productionContext, String remark, List<MonthPlanManufacturingRequirementVo> assemblingMouldPlanList, MouldInfoVO assemblingMouldInfo) {
        //先对前规格单模排产
        beforeList.forEach(beforeProductionPlan -> {
            boolean isAddRemark = beforeProductionPlan.getProductionQty() > BigDecimal.ZERO.intValue();
            singleMouldGeneralProductionPlan(productionContext, beforeProductionPlan, beforeMould);
            if (isAddRemark) {
                beforeProductionPlan.addAssemblingRemark(remark);
            }
            //加入已排完计划集合
            productionContext.addProductionFinishPlan(beforeProductionPlan.getMonthPlanId());
        });
        //如果前一个规格没有排产成功，则后一规格不能使用拼模排
        if (!productionContext.isAssemblingMouldProduction() || null == productionContext.getAssemblingMouldStartDay()) {
            beforeList.forEach(beforeProductionPlan -> beforeProductionPlan.resetBeforeAssemblingMouldPlanInfo());
            assemblingMouldPlanList.forEach(afterProductionPlan -> afterProductionPlan.resetBeforeAssemblingMouldPlanInfo());
            return;
        }
        //再对拼模规格单模排产
        productionContext.setAssemblingMouldNextProductCode(true);
        setAssemblingMouldMouldInfo(assemblingMouldInfo, productionContext);
        assemblingMouldPlanList.forEach(afterProductionPlan -> {
            boolean isAddRemark = afterProductionPlan.getProductionQty() > BigDecimal.ZERO.intValue();
            singleMouldGeneralProductionPlan(productionContext, afterProductionPlan, assemblingMouldInfo);
            if (isAddRemark) {
                afterProductionPlan.addAssemblingRemark(remark);
            }
            //加入已排完计划集合
            productionContext.addProductionFinishPlan(afterProductionPlan.getMonthPlanId());
        });
    }

    /**
     * 获取规格排产的所有规格计划
     *
     * @param allProductionPlanList
     * @param productCode
     * @return
     */
    private List<MonthPlanManufacturingRequirementVo> getNoProductionPlanByProductCode(List<MonthPlanManufacturingRequirementVo> allProductionPlanList, String productCode) {
        List<MonthPlanManufacturingRequirementVo> noProductionList = allProductionPlanList.stream().filter(needProductionPlan -> {
            if (YesOrNoEnum.NO.getValue().equals(needProductionPlan.getIsProduction())) {
                return false;
            }
            if (null == needProductionPlan.getProductionQty() || needProductionPlan.getProductionQty() <= BigDecimal.ZERO.longValue()) {
                return false;
            }
            if (!needProductionPlan.getProductCode().equals(productCode)) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());
        return noProductionList;
    }

    /**
     * 是否有满足其量的模具
     *
     * @param productionContext  排产上下文
     * @param productionMould    模具
     * @param productionPlanList 排产计划
     * @return
     */
    private boolean isProductionCapacityByMould(ProductionContext productionContext, MouldInfoVO productionMould, List<MonthPlanManufacturingRequirementVo> productionPlanList) {
        if (CollectionUtils.isEmpty(productionPlanList)) {
            return false;
        }
        //获取排产方向
        ProductionOrientEnum productionOrient = productionMould.getProductionOrient();
        if (null == productionOrient) {
            productionMould.setProductionOrient(ProductionOrientEnum.FORWARD);
        }
        MonthPlanManufacturingRequirementVo productionPlan = productionPlanList.get(0);
        BigDecimal singleCuringTime = ProductUtils.getSingleCuringTime(productionPlan, productionContext);
        BigDecimal totalCuringTime = singleCuringTime.multiply(BigDecimal.valueOf(productionPlan.getSummaryProductionQty()));
        BigDecimal totalLeftOverCuringTime = BigDecimal.ZERO;
        Map<Integer, BigDecimal> productionDayLeftOverMap = productionMould.getProductionDayList();
        List<Integer> productionDayList = new ArrayList<>(productionDayLeftOverMap.keySet());
        Collections.sort(productionDayList);
        Map<Integer, List<MouldDayProductionVo>> dayProductionMap = productionMould.getDayProductionMap();
        Integer minDay;
        if (CollectionUtils.isEmpty(dayProductionMap)) {
            minDay = productionDayList.get(0);
        } else {
            List<Integer> plannedProductionDayList = new ArrayList<>(dayProductionMap.keySet());
            Collections.sort(plannedProductionDayList);
            minDay = plannedProductionDayList.get(plannedProductionDayList.size() - 1);
        }
        Integer startDay = null;
        for (Integer day : productionDayList) {
            if (day < minDay) {
                continue;
            }
            BigDecimal dayLeftOverCuringTime = productionDayLeftOverMap.get(day);
            if (dayLeftOverCuringTime.compareTo(singleCuringTime) < BigDecimal.ZERO.intValue() && null == startDay) {
                continue;
            }
            //需要重新计算
            if (dayLeftOverCuringTime.compareTo(singleCuringTime) < BigDecimal.ZERO.intValue() && null != startDay) {
                if (totalCuringTime.compareTo(totalLeftOverCuringTime) <= BigDecimal.ZERO.intValue()) {
                    break;
                }
                totalLeftOverCuringTime = BigDecimal.ZERO;
                startDay = null;
                continue;
            }
            if (null != startDay) {
                totalLeftOverCuringTime = totalLeftOverCuringTime.add(dayLeftOverCuringTime);
                continue;
            }
            //起始天是否换规格,是需要扣除换规格时间
            if (MouldBaseUtils.isChangeProductCode(productionMould, productionPlan.getProductCode(), day, productionContext)) {
                dayLeftOverCuringTime = dayLeftOverCuringTime.subtract(MouldBaseUtils.getChangeProductConsumeTime(productionContext));
            }
            if (dayLeftOverCuringTime.compareTo(singleCuringTime) < BigDecimal.ZERO.intValue()) {
                continue;
            }
            startDay = day;
            totalCuringTime = totalLeftOverCuringTime.add(dayLeftOverCuringTime);
        }
        //还原排产方向
        productionMould.setProductionOrient(productionOrient);
        if (totalCuringTime.compareTo(totalLeftOverCuringTime) <= BigDecimal.ZERO.intValue()) {
            productionMould.setAssemblingMouldStartDay(startDay);
            return true;
        }
        return false;
    }

    /**
     * 根据单模规格，查找对应单模规格-拼模计划规格
     *
     * @param productionContext 排产上下文
     * @param productionPlan    当前排产计划
     * @param mouldInfo         当前排产模具
     * @return
     */
    private List<MonthPlanManufacturingRequirementVo> findAssemblingSingleMould(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, MouldInfoVO mouldInfo) {
        ProductionLogUtils.addFindAssemblingMouldPlanLog(productionContext, productionPlan, "单模规格");
        List<MonthPlanManufacturingRequirementVo> preliminaryAssemblingMouldList = getAssemblingMouldNoProductionListByParam(productionContext, productionPlan, null);
        if (CollectionUtils.isEmpty(preliminaryAssemblingMouldList)) {
            return Collections.emptyList();
        }
        List<MonthPlanManufacturingRequirementVo> finalAssemblingMouldList = getSameMouldTypeAndSingleMouldPlan(productionContext, preliminaryAssemblingMouldList, mouldInfo, productionPlan);
        if (CollectionUtils.isEmpty(finalAssemblingMouldList)) {
            return Collections.emptyList();
        }
        return finalAssemblingMouldList;
    }

    /**
     * 查找多模量小规格能够拼模排产的规格计划
     *
     * @param productionContext 排产上下文
     * @param productionPlan    当前排产计划
     * @param mouldInfo         前规格排产模具
     * @return
     */
    private List<MonthPlanManufacturingRequirementVo> findAssemblingBySmallAmount(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, MouldInfoVO mouldInfo) {
        ProductionLogUtils.addFindAssemblingMouldPlanLog(productionContext, productionPlan, "量小多模规格");
        ProductionParamConfiguration productionParam = productionContext.getProductionParam();
        Integer maxProductionQty = productionParam.getAssemblingMouldProductionQty();
        if (null == maxProductionQty) {
            maxProductionQty = BigDecimal.ZERO.intValue();
        }
        if (productionPlan.getSummaryProductionQty() > maxProductionQty) {
            return Collections.emptyList();
        }
        List<MonthPlanManufacturingRequirementVo> preliminaryAssemblingMouldList = getAssemblingMouldNoProductionListByParam(productionContext, productionPlan, maxProductionQty);
        if (CollectionUtils.isEmpty(preliminaryAssemblingMouldList)) {
            return Collections.emptyList();
        }
        List<MonthPlanManufacturingRequirementVo> finalAssemblingMouldList = getSameMouldTypePlan(productionContext, preliminaryAssemblingMouldList, mouldInfo, productionPlan);
        if (CollectionUtils.isEmpty(finalAssemblingMouldList)) {
            return Collections.emptyList();
        }
        return finalAssemblingMouldList;
    }

    /**
     * 单模排产
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     * @param singleMouldInfo   模具
     */
    private void singleMouldGeneralProductionPlan(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, MouldInfoVO singleMouldInfo) {
        //排产流程日志记录
        MouldProductionLog singleMouldLog = ProductionLogUtils.buildProductionLog(productionContext, productionPlan, MouldProductionLogType.ASSEMBLING_MOULD_LOG, "计划只用1副模具拼模");
        saveProductionLog(productionContext, singleMouldLog);
        if (null == singleMouldInfo.getProductionOrient()) {
            singleMouldInfo.setGroupValue(1);
            singleMouldInfo.setBeginDay(BigDecimal.ONE.intValue());
            singleMouldInfo.setEndDay(productionContext.getMonthDays());
        }
        singleMouldInfo.setProductionOrient(ProductionOrientEnum.FORWARD);
        singleMouldProduction(null, singleMouldInfo, productionContext, productionPlan, productionPlan.getProductionQty());
        //增加计划排产结束日志记录--[%d]计划单模具排产完成。。。
        ProductionLogUtils.addPlanProductionFinishLog(productionContext, false, productionPlan, MouldProductionLogType.ASSEMBLING_MOULD_LOG, "拼模");
    }

    /**
     * 设置拼模排产 模具信息
     *
     * @param assemblingMouldInfo 拼模模具信息
     * @param productionContext   排产上下文
     */
    private void setAssemblingMouldMouldInfo(MouldInfoVO assemblingMouldInfo, ProductionContext productionContext) {
        Integer groupValue = assemblingMouldInfo.getGroupValue();
        if (null == groupValue) {
            assemblingMouldInfo.setGroupValue(Integer.MAX_VALUE - BigDecimal.ONE.intValue());
        }
        assemblingMouldInfo.setProductionOrient(ProductionOrientEnum.FORWARD);
        if (null == productionContext.getAssemblingMouldStartDay()) {
            assemblingMouldInfo.setBeginDay(BigDecimal.ONE.intValue());
        } else {
            assemblingMouldInfo.setBeginDay(productionContext.getAssemblingMouldStartDay());
        }
        assemblingMouldInfo.setEndDay(productionContext.getMonthDays());
    }

    /**
     * 获取符合基本拼模条件的未排产计划
     * 基本条件：
     * 1、合模压力差值在范围内
     * 2、硫化时间差值在范围内
     * 3、需求量差值在范围内
     * 4、模具行腔要一致
     * 5、单模规格则没有最大排产量现在，多模量小规格则有最大排产量要求
     *
     * @param productionContext
     * @param productionPlan
     * @param maxProductionQty  最大排产量限制，可为空
     * @return
     */
    private List<MonthPlanManufacturingRequirementVo> getAssemblingMouldNoProductionListByParam(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, Integer maxProductionQty) {
        //获取合模压力、硫化时间、规格数量相差值符合条件的规格计划
        ProductionParamConfiguration productionParam = productionContext.getProductionParam();
        //合模压力差值
        Integer mouldClampingPressureDiff = productionParam.getMouldClampingPressureDiff();
        if (null == mouldClampingPressureDiff) {
            mouldClampingPressureDiff = BigDecimal.ZERO.intValue();
        }
        //硫化时间差值
        Integer curingTimeDiff = productionParam.getCuringTimeDiff();
        if (null == curingTimeDiff) {
            curingTimeDiff = BigDecimal.ZERO.intValue();
        }
        //需排产量差值
        Integer planQtyDiff = productionParam.getPlanQtyDiff();
        if (null == planQtyDiff) {
            planQtyDiff = BigDecimal.ZERO.intValue();
        }
        List<MonthPlanManufacturingRequirementVo> allProductionPlanList = productionContext.getMonthPlanInitMap().values().stream().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(allProductionPlanList)) {
            return Collections.emptyList();
        }
        List<MonthPlanManufacturingRequirementVo> preliminaryAssemblingMouldList = getAssemblingMouldNoProductionList(allProductionPlanList, productionPlan, mouldClampingPressureDiff, curingTimeDiff, planQtyDiff, maxProductionQty);
        if (CollectionUtils.isEmpty(preliminaryAssemblingMouldList)) {
            return Collections.emptyList();
        }
        return preliminaryAssemblingMouldList;
    }

    /**
     * 获取同模具类型的单模且模具产能满足的拼模计划集合
     *
     * @param productionContext     排产上下文
     * @param mustConditionPlanList 基本符合拼模的排产计划
     * @param mouldInfo             前规格模具
     * @return
     */
    private List<MonthPlanManufacturingRequirementVo> getSameMouldTypeBySingleMouldProductionPlan(ProductionContext productionContext, List<MonthPlanManufacturingRequirementVo> mustConditionPlanList, MouldInfoVO mouldInfo) {
        if (CollectionUtils.isEmpty(mustConditionPlanList)) {
            return Collections.emptyList();
        }
        String mouldType = mouldInfo.getMouldType();
        if (StringUtils.isBlank(mouldType)) {
            return Collections.emptyList();
        }
        String mouldCode = mouldInfo.getMouldCode();
        List<MonthPlanManufacturingRequirementVo> assemblingMouldPlanList = new ArrayList<>();
        Map<String, List<MonthPlanManufacturingRequirementVo>> productCodeGroupMap = mustConditionPlanList.stream().collect(Collectors.groupingBy(MonthPlanManufacturingRequirementVo::getProductCode));
        productCodeGroupMap.entrySet().stream().forEach(productCodeGroupEntry -> {
            String productCode = productCodeGroupEntry.getKey();
            //获取可用模具
            List<MouldInfoVO> enableMouldList = ProductionPlanUtils.getPlanMaxEnableMouldInfo(productCode, productionContext);
            if (CollectionUtils.isEmpty(enableMouldList)) {
                return;
            }
            //不是单模跳过
            if (enableMouldList.size() > BigDecimal.ONE.intValue()) {
                return;
            }
            MouldInfoVO assemblingMould = enableMouldList.get(0);
            //模具类型不一致或是共用模具
            if (!mouldType.equals(assemblingMould.getMouldType()) || assemblingMould.getMouldCode().equals(mouldCode)) {
                return;
            }
            //没有产能的话
            List<MonthPlanManufacturingRequirementVo> assemblingMouldList = productCodeGroupEntry.getValue();
            if (!isProductionCapacityByMould(productionContext, assemblingMould, assemblingMouldList)) {
                return;
            }
            //拼模的起始时间靠后
            if (assemblingMould.getAssemblingMouldStartDay() > mouldInfo.getAssemblingMouldStartDay()) {
                return;
            }
            assemblingMouldList.stream().forEach(assemblingMouldPlan -> assemblingMouldPlan.setEnableAssemblingMouldList(enableMouldList));
            assemblingMouldPlanList.addAll(assemblingMouldList);
        });
        return assemblingMouldPlanList;
    }

    /**
     * 获取同模具类型-多模量小规格且模具产能满足的拼模计划集合
     *
     * @param productionContext     排产上下文
     * @param mustConditionPlanList 满足基本条件的拼模计划集合
     * @param mouldInfo             前规格模具信息
     * @return
     */
    private List<MonthPlanManufacturingRequirementVo> getSameMouldTypeByMultiMouldSmallQtyProductionPlan(ProductionContext productionContext, List<MonthPlanManufacturingRequirementVo> mustConditionPlanList, MouldInfoVO mouldInfo) {
        if (CollectionUtils.isEmpty(mustConditionPlanList)) {
            return Collections.emptyList();
        }
        String mouldType = mouldInfo.getMouldType();
        if (StringUtils.isBlank(mouldType)) {
            return Collections.emptyList();
        }
        List<MonthPlanManufacturingRequirementVo> assemblingMouldPlanList = new ArrayList<>();
        Map<String, List<MonthPlanManufacturingRequirementVo>> productCodeGroupMap = mustConditionPlanList.stream().collect(Collectors.groupingBy(MonthPlanManufacturingRequirementVo::getProductCode));
        productCodeGroupMap.entrySet().stream().forEach(productCodeGroupEntry -> {
            String productCode = productCodeGroupEntry.getKey();
            List<MouldInfoVO> enableMouldList = ProductionPlanUtils.getPlanMaxEnableMouldInfo(productCode, productionContext);
            if (CollectionUtils.isEmpty(enableMouldList)) {
                return;
            }
            MouldInfoVO assemblingMould = enableMouldList.get(0);
            //模具类型一致
            if (!mouldType.equals(assemblingMould.getMouldType())) {
                return;
            }
            List<MonthPlanManufacturingRequirementVo> assemblingMouldPlanGroup = productCodeGroupEntry.getValue();
            //满足产能的模具
            List<MouldInfoVO> assemblingMouldList = getProductionCapacityByMould(productionContext, enableMouldList, assemblingMouldPlanGroup, mouldInfo);
            if (CollectionUtils.isEmpty(assemblingMouldList)) {
                return;
            }
            assemblingMouldPlanGroup.stream().forEach(assemblingMouldPlan -> assemblingMouldPlan.setEnableAssemblingMouldList(assemblingMouldList));
            assemblingMouldPlanList.addAll(assemblingMouldPlanGroup);
        });
        return assemblingMouldPlanList;
    }

    /**
     * 获取初步符合拼模条件的未排产计划
     * 1、合模压力 相差值
     * 2、硫化时间 相差值
     * 3、排产量 相差值
     * 4、最大排产量限制
     * 5、模具行腔要一致
     *
     * @param allProductionPlanList     所有计划
     * @param productionPlan            当前排产计划
     * @param mouldClampingPressureDiff 合模压力差值
     * @param curingTimeDiff            硫化时间差值
     * @param planQtyDiff               排产量差值
     * @param maxProductionQty          最大排产量
     * @return
     */
    private List<MonthPlanManufacturingRequirementVo> getAssemblingMouldNoProductionList(List<MonthPlanManufacturingRequirementVo> allProductionPlanList, MonthPlanManufacturingRequirementVo productionPlan, Integer mouldClampingPressureDiff, Integer curingTimeDiff, Integer planQtyDiff, Integer maxProductionQty) {
        if (CollectionUtils.isEmpty(allProductionPlanList) || null == productionPlan) {
            return Collections.emptyList();
        }
        String moldCavity = null == productionPlan.getMoldCavity() ? "" : productionPlan.getMoldCavity();
        List<MonthPlanManufacturingRequirementVo> noProductionList = allProductionPlanList.stream().filter(needProductionPlan -> {
            if (YesOrNoEnum.NO.getValue().equals(needProductionPlan.getIsProduction())) {
                return false;
            }
            if (null == needProductionPlan.getProductionQty() || needProductionPlan.getProductionQty() <= BigDecimal.ZERO.longValue()) {
                return false;
            }
            if (!moldCavity.equals(needProductionPlan.getMoldCavity())) {
                return false;
            }
            //有最大排产量限制时，表示小量规格拼模排产
            if (null != maxProductionQty && needProductionPlan.getSummaryProductionQty() > maxProductionQty) {
                return false;
            }
            if (needProductionPlan.getProductCode().equals(productionPlan.getProductCode())) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(noProductionList)) {
            return Collections.emptyList();
        }
        //获取符合合模压力，硫化时间、需求量差值的计划
        List<MonthPlanManufacturingRequirementVo> mustConditionPlanList = noProductionList.stream().filter(assemblingMouldPlan -> isMustConditionPlan(productionPlan, assemblingMouldPlan, mouldClampingPressureDiff, curingTimeDiff, planQtyDiff)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(mustConditionPlanList)) {
            return Collections.emptyList();
        }
        return mustConditionPlanList;
    }

    /**
     * 提取是单模，且不是共用模具的其它单模计划
     * 模具类型，模具行腔要一致
     *
     * @param productionContext     排产上下文
     * @param mustConditionPlanList 初步符合条件的拼模计划
     * @param mouldInfo             当前排产模具
     * @param productionPlan        当前排产计划
     * @return
     */
    private List<MonthPlanManufacturingRequirementVo> getSameMouldTypeAndSingleMouldPlan(ProductionContext productionContext, List<MonthPlanManufacturingRequirementVo> mustConditionPlanList, MouldInfoVO mouldInfo, MonthPlanManufacturingRequirementVo productionPlan) {
        if (CollectionUtils.isEmpty(mustConditionPlanList)) {
            return Collections.emptyList();
        }
        String mouldType = mouldInfo.getMouldType();
        if (StringUtils.isBlank(mouldType)) {
            return Collections.emptyList();
        }
        List<MonthPlanManufacturingRequirementVo> assemblingMouldPlanList = getSameMouldTypeBySingleMouldProductionPlan(productionContext, mustConditionPlanList, mouldInfo);
        return getAssemblingMouldProductionPlanList(productionContext, assemblingMouldPlanList, productionPlan, AssemblingMouldBusinessTypeEnum.SINGLE_MOULD_PRODUCT);
    }

    /**
     * 提取同模具类型的可拼模排产的集合-多模拼
     * 需排除共用模具
     * 1、共生胎优先
     * 2、同寸口优先
     * 3、差值最小优先
     * 4、优先等级优先
     *
     * @param productionContext     排产上下文
     * @param mustConditionPlanList 初步符合条件的拼模计划
     * @param mouldInfo             当前排产计划的模具
     * @param productionPlan        当前排产计划
     * @return
     */
    private List<MonthPlanManufacturingRequirementVo> getSameMouldTypePlan(ProductionContext productionContext, List<MonthPlanManufacturingRequirementVo> mustConditionPlanList, MouldInfoVO mouldInfo, MonthPlanManufacturingRequirementVo productionPlan) {
        if (CollectionUtils.isEmpty(mustConditionPlanList)) {
            return Collections.emptyList();
        }
        String mouldType = mouldInfo.getMouldType();
        if (StringUtils.isBlank(mouldType)) {
            return Collections.emptyList();
        }
        List<MonthPlanManufacturingRequirementVo> assemblingMouldPlanList = getSameMouldTypeByMultiMouldSmallQtyProductionPlan(productionContext, mustConditionPlanList, mouldInfo);
        return getAssemblingMouldProductionPlanList(productionContext, assemblingMouldPlanList, productionPlan, AssemblingMouldBusinessTypeEnum.MULTI_MOULD_SMALL_QTY);
    }

    /**
     * 对符合条件的拼模排产计划，
     * 按优先级查找一个productCode进行拼模排产
     * 1、共生胎优先
     * 2、再次同寸口
     * 3、最次相邻两寸口间
     * 在优先级集合下，按差值最小优先，其次优先级高优先
     *
     * @param assemblingMouldPlanList 符合条件的拼模规格计划集合
     * @param productionPlan          前规格计划信息
     * @return
     */
    private List<MonthPlanManufacturingRequirementVo> getAssemblingMouldProductionPlanList(ProductionContext productionContext, List<MonthPlanManufacturingRequirementVo> assemblingMouldPlanList, MonthPlanManufacturingRequirementVo productionPlan, AssemblingMouldBusinessTypeEnum assemblingMouldBusinessType) {
        if (CollectionUtils.isEmpty(assemblingMouldPlanList)) {
            return Collections.emptyList();
        }
        Comparator sortComparator = buildAssemblingMouldComparator();
        //共生胎优先
        String embryoCode = productionPlan.getEmbryoCode();
        List<MonthPlanManufacturingRequirementVo> sameEmbryoCodeList = assemblingMouldPlanList.stream().filter(assemblingMouldPlan -> embryoCode.equals(assemblingMouldPlan.getEmbryoCode())).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(sameEmbryoCodeList)) {
            return getAssemblingMouldProductPlan(sameEmbryoCodeList, sortComparator);
        }
        //没有共生胎拼模规格，则需要剔除本身能找到共生胎拼模排产的计划
        List<MonthPlanManufacturingRequirementVo> rejectOneselfSameEmbryoCodeList = getSuitableAssemblingMouldProductionPlanList(productionContext, assemblingMouldPlanList, productionPlan, sortComparator, assemblingMouldBusinessType);
        if (CollectionUtils.isEmpty(rejectOneselfSameEmbryoCodeList)) {
            return Collections.emptyList();
        }
        //同寸口优先
        BigDecimal proSize = productionPlan.getProSize();
        List<MonthPlanManufacturingRequirementVo> sameProSizeList = rejectOneselfSameEmbryoCodeList.stream().filter(assemblingMouldPlan -> proSize.equals(assemblingMouldPlan.getProSize())).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(sameProSizeList)) {
            return getAssemblingMouldProductPlan(sameProSizeList, sortComparator);
        }
        return getAssemblingMouldProductPlan(rejectOneselfSameEmbryoCodeList, sortComparator);
    }

    /**
     * 在可与productionPlan不共生胎拼模排产的规格集合中，获取比较合适的拼模排产计划
     * 如果找到的规格本身能找到其它共生胎的拼模计划，则该规格就不合适与productionPlan进行拼模排产
     *
     * @param productionContext                 排产上下文
     * @param assemblingMouldProductionPlanList 不共生胎的拼模排产计划集合
     * @param productionPlan                    需要找拼模排产的前置规格计划
     * @param sortComparator                    排产顺序
     * @param assemblingMouldType               拼模类型
     * @return
     */
    private List<MonthPlanManufacturingRequirementVo> getSuitableAssemblingMouldProductionPlanList(ProductionContext productionContext, List<MonthPlanManufacturingRequirementVo> assemblingMouldProductionPlanList, MonthPlanManufacturingRequirementVo productionPlan, Comparator sortComparator, AssemblingMouldBusinessTypeEnum assemblingMouldType) {
        if (CollectionUtils.isEmpty(assemblingMouldProductionPlanList)) {
            return Collections.emptyList();
        }
        //得到拼模规格顺序
        List<String> assemblingMouldProductSortList = getAssemblingMouldProductSortList(assemblingMouldProductionPlanList, sortComparator);
        //按SAP代码分组
        Map<String, List<MonthPlanManufacturingRequirementVo>> assemblingMouldProductGroupMap = assemblingMouldProductionPlanList.stream().collect(Collectors.groupingBy(MonthPlanManufacturingRequirementVo::getProductCode));
        //存储拼模模具信息
        Map<String, List<MouldInfoVO>> assemblingMouldProductCodeMouldInfoGroupMap = new HashMap<>();
        assemblingMouldProductGroupMap.entrySet().stream().forEach(groupEntry -> assemblingMouldProductCodeMouldInfoGroupMap.put(groupEntry.getKey(), groupEntry.getValue().get(0).getEnableAssemblingMouldList()));
        //需要剔除的SAP代码组
        Set<String> needRejectProductCodeSet = new HashSet<>();
        Map<String, Set<String>> sameEmbryoCodeAssemblingMouldMap = new HashMap<>();
        //查找有自己共生胎的拼模规格
        assemblingMouldProductSortList.stream().forEach(productCode -> findSameEmbryoCodeAssemblingMouldPlan(productionContext, productionPlan, productCode, assemblingMouldProductGroupMap, needRejectProductCodeSet, sameEmbryoCodeAssemblingMouldMap, sortComparator, assemblingMouldType));
        //没有
        if (CollectionUtils.isEmpty(needRejectProductCodeSet)) {
            return assemblingMouldProductionPlanList;
        }
        List<MonthPlanManufacturingRequirementVo> rejectResultList = assemblingMouldProductionPlanList.stream().filter(assemblingMouldPlan -> !needRejectProductCodeSet.contains(assemblingMouldPlan.getProductCode())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(rejectResultList)) {
            return Collections.emptyList();
        }
        //还原拼模模具信息
        rejectResultList.stream().forEach(assemblingMouldPlan -> assemblingMouldPlan.setEnableAssemblingMouldList(assemblingMouldProductCodeMouldInfoGroupMap.get(assemblingMouldPlan.getProductCode())));
        return rejectResultList;
    }

    /**
     * 获取productCode是否有共生胎可拼模的其它规格计划
     * 如果productCode找到了可共生胎的其他拼模计划集合，
     * 则表示该productCode不适合当前规格拼模
     *
     * @param productionContext                排产上下文
     * @param productionPlan                   当前拼模计划
     * @param productCode                      拼模规格
     * @param assemblingMouldProductGroupMap   拼模规格计划集合
     * @param needRejectProductCodeSet         需要剔除的拼模规格集合
     * @param sameEmbryoCodeAssemblingMouldMap 共生胎拼模规格集合
     * @param sortComparator                   挑选拼模规格顺序
     * @param assemblingMouldType              拼模业务类型
     */
    private void findSameEmbryoCodeAssemblingMouldPlan(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, String productCode, Map<String, List<MonthPlanManufacturingRequirementVo>> assemblingMouldProductGroupMap, Set<String> needRejectProductCodeSet, Map<String, Set<String>> sameEmbryoCodeAssemblingMouldMap, Comparator sortComparator, AssemblingMouldBusinessTypeEnum assemblingMouldType) {
        //已经找到共生胎拼模分组
        if (needRejectProductCodeSet.contains(productCode)) {
            return;
        }
        //没有，则需要查找是否有共生胎拼模规格，本身与productionPlan计划不共生胎
        List<MonthPlanManufacturingRequirementVo> needProductionPlanList = assemblingMouldProductGroupMap.get(productCode);
        if (CollectionUtils.isEmpty(needProductionPlanList)) {
            return;
        }
        //需要查找是否有共生胎的拼模计划
        MonthPlanManufacturingRequirementVo findAssemblingMouldProductionPlan = needProductionPlanList.get(0);
        String embryoCode = findAssemblingMouldProductionPlan.getEmbryoCode();
        List<MouldInfoVO> enableMouldList = ProductionPlanUtils.getPlanMaxEnableMouldInfo(productCode, productionContext);
        //使用的拼模排产模具
        MouldInfoVO beforeMould = getBeforeProductMould(productionContext, enableMouldList, needProductionPlanList);
        if (null == beforeMould) {
            ProductionLogUtils.addNoRejectAssemblingMouldPlanLog(productionContext, productionPlan, findAssemblingMouldProductionPlan);
            return;
        }
        Set<String> sameEmbryoCodeAssemblingMouldProductCodeSet = sameEmbryoCodeAssemblingMouldMap.get(embryoCode);
        List<MonthPlanManufacturingRequirementVo> sameEmbryoCodeAssemblingList;
        if (AssemblingMouldBusinessTypeEnum.SINGLE_MOULD_PRODUCT == assemblingMouldType) {
            sameEmbryoCodeAssemblingList = getSameEmbryoCodeAssemblingSingleMould(productionContext, findAssemblingMouldProductionPlan, beforeMould, sortComparator, sameEmbryoCodeAssemblingMouldProductCodeSet);
        } else {
            sameEmbryoCodeAssemblingList = getSameEmbryoCodeAssemblingBySmallAmount(productionContext, findAssemblingMouldProductionPlan, beforeMould, sortComparator, sameEmbryoCodeAssemblingMouldProductCodeSet);
        }
        if (CollectionUtils.isEmpty(sameEmbryoCodeAssemblingList)) {
            ProductionLogUtils.addNoRejectAssemblingMouldPlanLog(productionContext, productionPlan, findAssemblingMouldProductionPlan);
            return;
        }
        //找到了，则加入需要剔除的集合中
        MonthPlanManufacturingRequirementVo sameEmbryoCodeAssemblingMouldPlan = sameEmbryoCodeAssemblingList.get(0);
        ProductionLogUtils.addRejectAssemblingMouldPlanLog(productionContext, productionPlan, findAssemblingMouldProductionPlan, sameEmbryoCodeAssemblingMouldPlan);
        String sameEmbryoCodeProductCode = sameEmbryoCodeAssemblingMouldPlan.getProductCode();
        needRejectProductCodeSet.add(productCode);
        needRejectProductCodeSet.add(sameEmbryoCodeProductCode);
        if (null == sameEmbryoCodeAssemblingMouldProductCodeSet) {
            sameEmbryoCodeAssemblingMouldProductCodeSet = new HashSet<>();
            sameEmbryoCodeAssemblingMouldMap.put(embryoCode, sameEmbryoCodeAssemblingMouldProductCodeSet);
        }
        sameEmbryoCodeAssemblingMouldProductCodeSet.add(productCode);
        sameEmbryoCodeAssemblingMouldProductCodeSet.add(sameEmbryoCodeProductCode);
    }

    /**
     * 获取单模规格的拼模计划规格
     *
     * @param productionContext 排产上下文
     * @param productionPlan    当前排产计划
     * @param mouldInfo         当前排产模具
     * @param sortComparator    排产顺序
     * @param sameEmbryoCodeSet 共生胎拼模规格集合
     * @return
     */
    private List<MonthPlanManufacturingRequirementVo> getSameEmbryoCodeAssemblingSingleMould(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, MouldInfoVO mouldInfo, Comparator sortComparator, Set<String> sameEmbryoCodeSet) {
        ProductionLogUtils.addFindAssemblingMouldPlanLog(productionContext, productionPlan, "单模共生胎规格");
        List<MonthPlanManufacturingRequirementVo> preliminaryAssemblingMouldList = getAssemblingMouldNoProductionListByParam(productionContext, productionPlan, null);
        if (CollectionUtils.isEmpty(preliminaryAssemblingMouldList)) {
            return Collections.emptyList();
        }
        String mouldType = mouldInfo.getMouldType();
        if (StringUtils.isBlank(mouldType)) {
            return Collections.emptyList();
        }
        List<MonthPlanManufacturingRequirementVo> assemblingMouldPlanList = getSameMouldTypeBySingleMouldProductionPlan(productionContext, preliminaryAssemblingMouldList, mouldInfo);
        if (CollectionUtils.isEmpty(assemblingMouldPlanList)) {
            return Collections.emptyList();
        }
        if (null == sameEmbryoCodeSet) {
            sameEmbryoCodeSet = new HashSet<>();
        }
        Set<String> finalSameEmbryoCodeSet = sameEmbryoCodeSet;
        //只查找共生胎
        String embryoCode = productionPlan.getEmbryoCode();
        List<MonthPlanManufacturingRequirementVo> sameEmbryoCodeList = assemblingMouldPlanList.stream().filter(assemblingMouldPlan -> embryoCode.equals(assemblingMouldPlan.getEmbryoCode()) && !finalSameEmbryoCodeSet.contains(assemblingMouldPlan.getProductCode())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(sameEmbryoCodeList)) {
            return Collections.emptyList();
        }
        return getAssemblingMouldProductPlan(sameEmbryoCodeList, sortComparator);
    }

    /**
     * 获取规格需排产量小的可拼模排产规格计划
     *
     * @param productionContext 排产上下文
     * @param productionPlan    当前排产计划
     * @param mouldInfo         前规格排产模具
     * @return
     */
    private List<MonthPlanManufacturingRequirementVo> getSameEmbryoCodeAssemblingBySmallAmount(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, MouldInfoVO mouldInfo, Comparator sortComparator, Set<String> sameEmbryoCodeSet) {
        ProductionLogUtils.addFindAssemblingMouldPlanLog(productionContext, productionPlan, "量小多模共生胎规格");
        ProductionParamConfiguration productionParam = productionContext.getProductionParam();
        Integer maxProductionQty = productionParam.getAssemblingMouldProductionQty();
        if (null == maxProductionQty) {
            maxProductionQty = BigDecimal.ZERO.intValue();
        }
        if (productionPlan.getSummaryProductionQty() > maxProductionQty) {
            return Collections.emptyList();
        }
        List<MonthPlanManufacturingRequirementVo> preliminaryAssemblingMouldList = getAssemblingMouldNoProductionListByParam(productionContext, productionPlan, maxProductionQty);
        if (CollectionUtils.isEmpty(preliminaryAssemblingMouldList)) {
            return Collections.emptyList();
        }
        List<MonthPlanManufacturingRequirementVo> assemblingMouldPlanList = getSameMouldTypeByMultiMouldSmallQtyProductionPlan(productionContext, preliminaryAssemblingMouldList, mouldInfo);
        if (CollectionUtils.isEmpty(assemblingMouldPlanList)) {
            return Collections.emptyList();
        }
        if (null == sameEmbryoCodeSet) {
            sameEmbryoCodeSet = new HashSet<>();
        }
        Set<String> finalSameEmbryoCodeSet = sameEmbryoCodeSet;
        //只查找共生胎
        String embryoCode = productionPlan.getEmbryoCode();
        List<MonthPlanManufacturingRequirementVo> sameEmbryoCodeList = assemblingMouldPlanList.stream().filter(assemblingMouldPlan -> embryoCode.equals(assemblingMouldPlan.getEmbryoCode()) && !finalSameEmbryoCodeSet.contains(assemblingMouldPlan.getProductCode())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(sameEmbryoCodeList)) {
            return Collections.emptyList();
        }
        return getAssemblingMouldProductPlan(sameEmbryoCodeList, sortComparator);
    }

    /**
     * 判断计划是否符合拼模必备条件
     * 1、硫化时间-差值在curingTimeDiff范围
     * 2、合模压力-差值在mouldClampingPressureDiff范围
     * 3、汇总需排产量-差值在planQtyDiff范围
     * 4、相邻两个寸口
     *
     * @param productionPlan            当前排产计划
     * @param assemblingMouldPlan       需要拼模的计划
     * @param mouldClampingPressureDiff 合模压力差值
     * @param curingTimeDiff            硫化时间差值
     * @param planQtyDiff               计划量差值
     * @return
     */
    private boolean isMustConditionPlan(MonthPlanManufacturingRequirementVo productionPlan, MonthPlanManufacturingRequirementVo assemblingMouldPlan, Integer mouldClampingPressureDiff, Integer curingTimeDiff, Integer planQtyDiff) {
        if (null == productionPlan || null == assemblingMouldPlan) {
            return false;
        }
        BigDecimal curingTime = productionPlan.getCuringTime();
        BigDecimal assemblingMouldCuringTime = assemblingMouldPlan.getCuringTime();
        if (null == curingTime || null == assemblingMouldCuringTime) {
            return false;
        }
        //硫化时间-差值范围
        Integer diffValue = curingTime.subtract(assemblingMouldCuringTime).setScale(0, RoundingMode.HALF_UP).intValue();
        if (Math.abs(diffValue) > curingTimeDiff) {
            return false;
        }
        BigDecimal mouldClampingPressure = productionPlan.getMouldClampingPressure();
        BigDecimal assemblingMouldMouldClampingPressure = assemblingMouldPlan.getMouldClampingPressure();
        if (null == assemblingMouldMouldClampingPressure || null == mouldClampingPressure) {
            return false;
        }
        //合模压力-差值范围
        Integer mouldClampingPressureDiffValue = mouldClampingPressure.subtract(assemblingMouldMouldClampingPressure).setScale(0, RoundingMode.HALF_UP).intValue();
        if (Math.abs(mouldClampingPressureDiffValue) > mouldClampingPressureDiff) {
            return false;
        }
        //排产量-差值范围
        Long sumProductionQty = productionPlan.getSummaryProductionQty();
        Long assemblingMouldSumProductionQty = assemblingMouldPlan.getSummaryProductionQty();
        Long sumProductionQtyDiffValue = sumProductionQty - assemblingMouldSumProductionQty;
        if (Math.abs(sumProductionQtyDiffValue) > planQtyDiff) {
            return false;
        }
        BigDecimal proSize = productionPlan.getProSize();
        BigDecimal assemblingMouldProSize = assemblingMouldPlan.getProSize();
        if (null == proSize || null == assemblingMouldProSize) {
            return false;
        }
        //相邻两个寸口
        Integer proSizeDiffValue = proSize.subtract(assemblingMouldProSize).setScale(0, RoundingMode.HALF_UP).intValue();
        if (Math.abs(proSizeDiffValue) > BigDecimal.ONE.intValue()) {
            return false;
        }
        assemblingMouldPlan.setDiffValue(Math.abs(sumProductionQtyDiffValue));
        return true;
    }

    /**
     * 是否有模具能满足其排产量
     *
     * @param productionContext       排产上下文
     * @param assemblingMouldList     模具列表
     * @param assemblingMouldPlanList 拼模排产计划列表
     * @param mouldInfo               前规格拼模模具信息
     * @return
     */
    private List<MouldInfoVO> getProductionCapacityByMould(ProductionContext productionContext, List<MouldInfoVO> assemblingMouldList, List<MonthPlanManufacturingRequirementVo> assemblingMouldPlanList, MouldInfoVO mouldInfo) {
        List<MouldInfoVO> enableAssemblingMouldList = new ArrayList<>();
        for (MouldInfoVO assemblingMould : assemblingMouldList) {
            //20250724 ZLT 共用模具时，需要剔除前规格使用的模具
            if (assemblingMould.getMouldCode().equals(mouldInfo.getMouldCode())) {
                continue;
            }
            boolean isSelected = isProductionCapacityByMould(productionContext, assemblingMould, assemblingMouldPlanList);
            //20250626 产能满足，其起始时间也满足
            if (isSelected && assemblingMould.getAssemblingMouldStartDay() <= mouldInfo.getAssemblingMouldStartDay()) {
                enableAssemblingMouldList.add(assemblingMould);
            }
        }
        return enableAssemblingMouldList;
    }

    /**
     * 获取符合条件的拼模排产计划集合的SAP顺序，
     *
     * @param conditionPlanList 符合拼模排产计划集合
     * @param sortComparator    排序
     * @return
     */
    private List<String> getAssemblingMouldProductSortList(List<MonthPlanManufacturingRequirementVo> conditionPlanList, Comparator sortComparator) {
        List<String> sortList = new ArrayList<>();
        Set<String> isAddSet = new HashSet<>();
        conditionPlanList.sort(sortComparator);
        conditionPlanList.stream().forEach(assemblingMouldPlan -> {
            String productCode = assemblingMouldPlan.getProductCode();
            if (isAddSet.contains(productCode)) {
                return;
            }
            sortList.add(productCode);
            isAddSet.add(productCode);
        });
        return sortList;
    }

    /**
     * 根据条件拼模计划，挑选按差值最小，优先级等第一个规格的排产计划进行拼模排产
     *
     * @param conditionPlanList 符合拼模排产的计划集合
     * @param sortComparator    排序
     * @return
     */
    private List<MonthPlanManufacturingRequirementVo> getAssemblingMouldProductPlan(List<MonthPlanManufacturingRequirementVo> conditionPlanList, Comparator sortComparator) {
        conditionPlanList.sort(sortComparator);
        String productCode = conditionPlanList.get(0).getProductCode();
        return conditionPlanList.stream().filter(sameProductCodePlan -> productCode.equals(sameProductCodePlan.getProductCode())).collect(Collectors.toList());
    }

    /**
     * 构建拼模排产规格的顺序，差值最小 ，优先级
     *
     * @return
     */
    private Comparator buildAssemblingMouldComparator() {
        Comparator comparator = Comparator.comparing(MonthPlanManufacturingRequirementVo::getDiffValue).thenComparing(MonthPlanManufacturingRequirementVo::getProductionSequence);
        return comparator;
    }

}

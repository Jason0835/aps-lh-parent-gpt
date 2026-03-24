package com.zlt.aps.mp.engine.domain.vo;

import com.zlt.aps.enums.*;
import com.zlt.aps.mp.api.domain.entity.DpDemandPlan;
import com.zlt.aps.mp.api.domain.entity.ProductionMonthPlanInit;
import com.zlt.aps.mp.engine.basedata.assemble.construction.ConstructionSelector;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxContinueSkuInfoHelper;
import com.zlt.aps.mp.engine.utils.NoProductionReasonUtils;
import com.zlt.common.utils.PubUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工厂月度排产需求计划对象
 *
 * @author ZLT
 * @date 20251209
 */
@Slf4j
@Data
public class MonthPlanProductionRequirePlanVo extends ProductionMonthPlanInit {

    /**
     * 配置的模具数量(SKU与模具关系中配置的数量)
     */
    private Integer configurationMouldQty;

    /**
     * 有模具基础信息的模具数量(模具台账或是新模具到货计划)
     */
    private Integer baseMouldQty;

    /**
     * 不可生产标志
     */
    private String cantProduce;
    /**
     * 排产标记 默认可排产
     */
    private String productionFlag;
    /**
     * 是否没有配置关系
     */
    private boolean noConfigurationConstruction;

    /**
     * 是否没有配置硫化日产能
     */
    private boolean noConfigurationLh;
    /**
     * 计划类型
     */
    private String planType;
    /**
     * 成型产能需求量
     * 首轮 = 净需求
     * 第二轮 = 模拟模具已排产量 + 换模损耗量
     */
    private Integer cxCapacityRequireQty;
    /**
     * 高优先级还需排产量，只有排产高优级量时才扣减
     */
    private Integer heightProductionQty;
    /**
     * 总的还需排产量，每次排产完高优先级需要同步扣减
     * 排产非高优先级时，也同步扣减
     */
    private Integer productionQty;
    /**
     * 高优先级需求量
     * 数据备份-模拟排产后需要用它还原heightProductionQty
     */
    private Integer originHeightProductionQty;
    /**
     * 总的排产净需求量
     * 数据备份-模拟排产后需要用它还原productionQty
     */
    private Integer originProductionQty;
    /**
     * 是否含有特殊材料 1 含有 0 不含有
     */
    private String isSpecialMaterials;
    /**
     * 库销比-动态计算，每次排产后变化
     */
    private Double inventorySalesRatio;
    /**
     * 是否按总需求量排产 1 是 0 否
     */
    private Integer isProductionBySum;
    /**
     * 本轮次是否参与排产 1 是 0 否
     */
    private Integer isThisRound;
    /**
     * 已排产量
     */
    private Integer producedQty;

    /**
     * 初始的排产数据设置
     * 标记初始的排产标记
     * 产能预算的产能需求量
     * 高优级待排产量
     * 总需求排量量
     */
    public void initProductionDataInfo() {
        productionFlag = getIsProduction();
        heightProductionQty = getHeightLossQty();
        cxCapacityRequireQty = this.getCxCapacityRequireQty();
        productionQty = cxCapacityRequireQty;
        originHeightProductionQty = heightProductionQty;
        originProductionQty = productionQty;
    }

    /**
     * 重置排产数据设置
     * 标记初始的排产标记
     * 产能预算的产能需求量
     * 高优级待排产量
     * 总需求排量量
     * 初始的库销比
     */
    public void resetProductionDataInfo() {
        productionFlag = getIsProduction();
        heightProductionQty = originHeightProductionQty;
        productionQty = originProductionQty;
        calculateInventorySalesRatio(BigDecimal.ZERO.intValue());
    }

    /**
     * 结构提前收尾，返回计划排产量
     *
     * @param addProductionQty 收尾需撤回的排产量
     */
    public void withdrawProductionQty(Integer addProductionQty) {
        Integer currentNeedProductionQty = this.productionQty;
        //净需求-先撤回
        this.productionQty = currentNeedProductionQty + addProductionQty;
        if (this.originHeightProductionQty <= BigDecimal.ZERO.intValue()) {
            return;
        }
        Integer originHeightQty = this.originHeightProductionQty;
        //如果高优先级还有量，则表示排产的是高优先级量
        Integer currentHeightNeedQty = this.heightProductionQty;
        if (currentHeightNeedQty > BigDecimal.ZERO.intValue()) {
            currentHeightNeedQty = currentHeightNeedQty + addProductionQty;
            if (currentHeightNeedQty >= originHeightQty) {
                currentHeightNeedQty = originHeightQty;
            }
            this.heightProductionQty = currentHeightNeedQty;
            return;
        }
        //非高优先级排产量
        Integer noHeightProductionQty = originProductionQty - currentNeedProductionQty - originHeightQty;
        //得到高优先级还需排产量
        Integer addHeightQty = addProductionQty - noHeightProductionQty;
        if (addHeightQty < BigDecimal.ZERO.intValue()) {
            addHeightQty = BigDecimal.ZERO.intValue();
        }
        this.heightProductionQty = addHeightQty;
    }

    /**
     * 计划是否可进行最小批量排产
     * 如果计划不排产则不排
     * 如果净需求计划为零，则不排
     *
     * @return
     */
    public boolean isProductionMinProductionQty() {
        if (!YesOrNoEnum.YES.getCode().equals(getIsProduction())) {
            return false;
        }
        Integer sum = Optional.ofNullable(getNetQty()).orElse(BigDecimal.ZERO.intValue());
        return sum > BigDecimal.ZERO.intValue();
    }

    /**
     * 获取计划可排产量 = 排产净需求 + 常规储备 + 可能排产(暂缓)
     *
     * @return
     */
    public Integer getPlanNeedProductionQty() {
        Integer sum = Optional.ofNullable(getNetQty()).orElse(BigDecimal.ZERO.intValue());
        sum = sum + Optional.ofNullable(getConventionReserveQty()).orElse(BigDecimal.ZERO.intValue());
        if (isNormalTypePlan()) {
            return sum;
        }
        sum = sum + Optional.of(getPostponeQty()).orElse(BigDecimal.ZERO.intValue());
        return sum;
    }

    /**
     * 是否为正常类型的计划
     *
     * @return
     */
    public boolean isNormalTypePlan() {
        return ProductionPlanType.NORMAL.getPlanType().equals(planType);
    }

    /**
     * 是否需要排产暂缓的类型计划
     * 实单模拟及产量预测类型需要进行暂缓订单排产
     *
     * @return
     */
    public boolean isNeedPostponeTypePlan() {
        return ProductionPlanType.PREDICTION.getPlanType().equals(planType) || ProductionPlanType.SIMULATE.getPlanType().equals(planType);
    }

    /**
     * 虚拟的还需排产量值
     * 如果有高优先级值，则为高优先级，否则为净需求值
     *
     * @return
     */
    public Integer getVirtualProductionQty() {
        if (getHeightProductionQty() > BigDecimal.ZERO.longValue()) {
            return getHeightProductionQty();
        }
        return getProductionQty();
    }

    /**
     * 是否有供应链优先排产量
     *
     * @return
     */
    public boolean hasPrioritizeQty() {
        if (!YesOrNoEnum.YES.getCode().equals(getIsPrioritize())) {
            return false;
        }
        return hasProduction();
    }

    /**
     * 是否小于minQty
     * 如果有高优先级排产量则使用高优先级排产量比较，否则使用排产量比较
     *
     * @param minQty
     * @return
     */
    public boolean isLess(Integer minQty) {
        if (getHeightProductionQty() > BigDecimal.ZERO.intValue()) {
            return getHeightProductionQty() < minQty;
        }
        return getProductionQty() < minQty;
    }

    /**
     * 计算库销比
     * (当前库存+排产量)/月均销量的比例
     *
     * @param productionQty
     */
    public void calculateInventorySalesRatio(Integer productionQty) {
        //月均销量没有或是为零，则表示库销比越低，最高
        Integer averageSaleQty = getAverageSaleQty();
        if (null == averageSaleQty || averageSaleQty <= BigDecimal.ZERO.intValue()) {
            inventorySalesRatio = BigDecimal.valueOf(Integer.MIN_VALUE).doubleValue();
            return;
        }
        if (null == productionQty || productionQty < BigDecimal.ZERO.intValue()) {
            productionQty = BigDecimal.ZERO.intValue();
        }
        Integer sumStockQty = getStockQty() + productionQty;
        inventorySalesRatio = BigDecimal.valueOf(sumStockQty).divide(BigDecimal.valueOf(averageSaleQty), 1, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 构建初始的排产需求计划
     *
     * @param context           排产上下文
     * @param productionVersion 排产版本
     * @param require           需求计划信息
     * @return
     */
    public static MonthPlanProductionRequirePlanVo buildInitProductionPlan(Context context, String productionVersion, DpDemandPlan require) {
        MonthPlanProductionRequirePlanVo plan = new MonthPlanProductionRequirePlanVo();
        BeanUtils.copyProperties(require, plan);
        plan.setId(null);
        plan.setProductionVersion(productionVersion);
        plan.setMonthPlanId(require.getId());
        //默认可生产
        plan.setCantProduce(YesOrNoEnum.NO.getCode());
        plan.setIsPrioritize(require.getScmPriority());
        //排产为空，则默认可排产
        if (StringUtils.isBlank(require.getIsProduction())) {
            plan.setIsProduction(YesOrNoEnum.YES.getCode());
        } else {
            plan.setIsProduction(require.getIsProduction());
        }
        if (plan.isNeedPostponeTypePlan()) {
            //20260204 实单模拟及产能预测需要排产暂缓订单
            Integer sum = Optional.ofNullable(plan.getNetQty()).orElse(BigDecimal.ZERO.intValue());
            sum = sum + Optional.ofNullable(plan.getPostponeQty()).orElse(BigDecimal.ZERO.intValue());
            plan.setNetQty(sum);
        }
        return plan;
    }

    /**
     * 设置物料基础信息属性
     * 结构名
     * 不可生产标志等
     *
     * @param productBaseInfo
     */
    public void setProductBaseInfo(ProductBaseInfoVo productBaseInfo) {
        if (null == productBaseInfo) {
            return;
        }
        setStructureName(productBaseInfo.getStructureName());
        setSpecifications(productBaseInfo.getSpecifications());
        setProSize(productBaseInfo.getProSize());
        setMainPattern(productBaseInfo.getMainPattern());
        setCantProduce(productBaseInfo.getCantProduce());
        setProductCategory(productBaseInfo.getProductCategory());
    }

    /**
     * 根据配置的施工关系
     * 设置施工信息
     *
     * @param constructionConfigurationList 施工配置信息
     */
    public void setConstructionInfo(List<MonthPlanProductConstructionInfoVo> constructionConfigurationList) {
        if (CollectionUtils.isEmpty(constructionConfigurationList)) {
            noConfigurationConstruction = true;
            return;
        }
        //施工配置
        MonthPlanProductConstructionInfoVo constructionInfo = setConstructionStage(constructionConfigurationList, getProductTypeCode());
        setEmbryoCode(constructionInfo.getEmbryoCode());
        setProductStatus(constructionInfo.getProductStatus());
        setMainMaterialDesc(constructionInfo.getMainMaterialDesc());
        setMouldMethod(constructionInfo.getMouldMethod());
        setSpecCode(constructionInfo.getSpecCode());
        if (StringUtils.isBlank(constructionInfo.getIsZeroRack())) {
            setIsZeroRack(YesOrNoEnum.NO.getCode());
        } else {
            setIsZeroRack(constructionInfo.getIsZeroRack());
        }
        setEmbryoNo(constructionInfo.getEmbryoNo());
        setLhNo(constructionInfo.getLhNo());
        setTextNo(constructionInfo.getTextNo());
    }

    /**
     * 根据日硫化配置，设置硫化信息
     *
     * @param lhCapacity 日硫化配置
     */
    public void setVulcanizationInfo(MonthPlanProductLhCapacityVo lhCapacity) {
        if (null == lhCapacity) {
            noConfigurationLh = true;
            return;
        }
        //单模产能
        Integer singleLhMachineQty = lhCapacity.getDayVulcanizationQty();
        if (null != singleLhMachineQty) {
            setDayVulcanizationQty(singleLhMachineQty / ProductionConstant.DOUBLE_MOULD_PRODUCTION);
        }
        BigDecimal curingTime = Optional.ofNullable(lhCapacity.getVulcanizationTime()).orElse(BigDecimal.ZERO);
        setCuringTime(curingTime);
    }

    /**
     * 设置模具相关信息
     *
     * @param mouldInfoList
     */
    public void setMouldInfo(List<MonthPlanProductMouldInfoVo> mouldInfoList) {
        if (CollectionUtils.isEmpty(mouldInfoList)) {
            configurationMouldQty = BigDecimal.ZERO.intValue();
            return;
        }
        configurationMouldQty = mouldInfoList.size();
        //有基础数据的模具数量
        List<MonthPlanProductMouldInfoVo> baseMouldInfoList = mouldInfoList.stream().filter(mouldInfo -> StringUtils.isNotBlank(mouldInfo.getBaseMouldCode())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(baseMouldInfoList)) {
            baseMouldQty = BigDecimal.ZERO.intValue();
        } else {
            baseMouldQty = baseMouldInfoList.size();
        }
        //可用状态的模具数量
        List<MonthPlanProductMouldInfoVo> enableMouldList = mouldInfoList.stream().filter(mouldInfo -> YesOrNoEnum.YES.getCode().equals(mouldInfo.getMouldStatus())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(enableMouldList)) {
            setMouldQty(BigDecimal.ZERO.intValue());
        } else {
            setMouldQty(enableMouldList.size());
        }
    }

    /**
     * 匹配的sku是否还有需排产的计划
     *
     * @param selectedMaterialDesc 物料描述
     * @return
     */
    public boolean hasSelectedProduction(String selectedMaterialDesc) {
        if (StringUtils.isBlank(selectedMaterialDesc)) {
            return false;
        }
        if (!selectedMaterialDesc.equals(getMaterialDesc())) {
            return false;
        }
        return hasProduction();
    }

    /**
     * 匹配的sku在本轮次是否还有需排产的计划
     *
     * @param selectedMaterialDesc 物料描述
     * @return
     */
    public boolean hasThisRoundSelectedProduction(String selectedMaterialDesc) {
        if (StringUtils.isBlank(selectedMaterialDesc)) {
            return false;
        }
        if (!selectedMaterialDesc.equals(getMaterialDesc())) {
            return false;
        }
        if (YesOrNoEnum.YES.getValue().equals(isThisRound)) {
            return hasProduction();
        }
        return false;
    }

    /**
     * 如果不排产，则标记不排
     * 不排产场景：
     * 1、本身不排产
     * 2、没有排产量
     * 否则判断本轮次排产标记
     * 会出现还有排产。但本轮次不再需要参与后续的排产，否则死循环
     * <p>
     * true 表示本轮次还需排产 false表示本轮次不再参与排产
     *
     * @return
     */
    public boolean hasProductionThisRound() {
        if (hasProduction()) {
            return YesOrNoEnum.YES.getValue().equals(isThisRound);
        }
        return false;
    }

    /**
     * 是否还需排产
     * 排产标记 = 1 且还有可排产量
     * true表示还需排产 false表示无需排产
     *
     * @return
     */
    public boolean hasProduction() {
        //标记不排产
        if (YesOrNoEnum.NO.getCode().equals(getProductionFlag())) {
            return false;
        }
        //总的还需排产量为零
        if (null == productionQty) {
            return false;
        }
        if (productionQty <= BigDecimal.ZERO.longValue()) {
            return false;
        }
        return true;
    }

    /**
     * 判断计划是否为续作Sku排产计划
     * 同规格同花纹或是同生胎
     *
     * @param continueProductInfo
     * @return
     */
    public boolean hasContinueProduction(CxContinueSkuInfoHelper continueProductInfo) {
        boolean isSameSpecificationsAndPattern = isSameSpecificationsAndPattern(continueProductInfo);
        if (!isSameSpecificationsAndPattern) {
            return false;
        }
        return isSameEmbryoCode(continueProductInfo);
    }

    /**
     * 获取天单硫化机台产能
     * 单硫化机台天产能 = 双模产能 = 单模天产能 * 2
     *
     * @return
     */
    public Integer getMaxDaySingleLhMachineQty() {
        return getDayVulcanizationQty() * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
    }

    /**
     * 是否是续作Sku-同规格同花纹
     * 前提是先达到共用模具
     *
     * @param continueProductInfo 续作Sku信息
     * @return
     */
    public boolean isSameSpecificationsAndPattern(CxContinueSkuInfoHelper continueProductInfo) {
        if (null == continueProductInfo) {
            return false;
        }
        //规格
        String specifications = continueProductInfo.getSpecifications();
        //花纹
        String pattern = continueProductInfo.getPattern();
        if (StringUtils.isBlank(specifications) || StringUtils.isBlank(pattern)) {
            return false;
        }
        //同规格同花纹
        if (specifications.equals(getSpecifications()) && pattern.equals(getPattern())) {
            return true;
        }
        return false;
    }

    /**
     * 是否是续作Sku-共生胎
     * 前提是先达到共用模具
     *
     * @param continueProductInfo 续作Sku信息
     * @return
     */
    public boolean isSameEmbryoCode(CxContinueSkuInfoHelper continueProductInfo) {
        if (null == continueProductInfo) {
            return false;
        }
        //同生胎
        String embryoCode = continueProductInfo.getEmbryoCode();
        if (StringUtils.isBlank(embryoCode)) {
            return false;
        }
        return embryoCode.equals(getEmbryoCode());
    }

    /**
     * 检测基本的排产条件
     * 并标记不排产原因及不排产标记
     * 初始化阶段使用
     */
    public void checkProductionConditionByBase() {
        //检测是否符合不排产，且不用往下继续检测的业务场景
        String isProduction = checkNoContinueCondition();
        if (YesOrNoEnum.NO.getCode().equals(isProduction)) {
            return;
        }
        //检查物料基础业务
        isProduction = checkMaterialCondition(isProduction);
        //检查施工业务
        isProduction = checkConstructionCondition(isProduction);
        //检查模具业务
        isProduction = checkMouldInfoCondition(isProduction);
        //检测日硫化量业务
        isProduction = checkLhCapacityCondition(isProduction);
        setIsProduction(isProduction);
    }

    /**
     * 设置不排产，并增加不排产原因
     *
     * @param addNoProductionReason 不排产原因
     */
    public void setNoProductionAndAddReason(String addNoProductionReason) {
        if (StringUtils.isBlank(addNoProductionReason)) {
            return;
        }
        setIsProduction(YesOrNoEnum.NO.getCode());
        setProductionFlag(YesOrNoEnum.NO.getCode());
        addNoProductionReason(addNoProductionReason);
    }

    /**
     * 单独增加不排产原因
     *
     * @param addNoProductionReason
     */
    public void singleAddNoProductionReason(String addNoProductionReason) {
        addNoProductionReason(addNoProductionReason);
    }

    /**
     * 获取模具分配比例控制key
     * 结构+主花纹
     *
     * @return
     */
    public String getMouldAllocationControlDimensionKey() {
        String keyFormat = "%s|*|%s";
        return String.format(keyFormat, getStructureName(), getMainPattern());
    }

    /**
     * 根据补量的值，判断是否可进行月底补量
     *
     * @return
     */
    public boolean hasBoostQty(Set<String> boostScheduleTypeSet) {
        if (CollectionUtils.isEmpty(boostScheduleTypeSet)) {
            return false;
        }
        if (!boostScheduleTypeSet.contains(getProductionType())) {
            return false;
        }
        return true;
    }

    /**
     * 检测不排继续往下匹配的不排产原因
     * 1、计划本身不排产
     * 2、没有物料编码
     * 3、计划没有排产需求
     * 4、工厂不排产
     * 5、停产
     * 初始化阶段使用
     *
     * @return
     */
    private String checkNoContinueCondition() {
        //不排产
        if (YesOrNoEnum.NO.getCode().equals(getIsProduction())) {
            String planNoProductionReason = NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.PLAN_NO_PRODUCTION);
            addNoProductionReason(planNoProductionReason);
            return YesOrNoEnum.NO.getCode();
        }
        //没有物料编码
        if (StringUtils.isBlank(getMaterialCode()) || StringUtils.isBlank(getMaterialDesc())) {
            String noHasMaterialCode = NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.NO_HAS_PRODUCT_CODE);
            addNoProductionReason(noHasMaterialCode);
            setIsProduction(YesOrNoEnum.NO.getCode());
            return YesOrNoEnum.NO.getCode();
        }
        //无排产量
        if (getPlanNeedProductionQty() <= BigDecimal.ZERO.intValue()) {
            String noProductionQtyReason = NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.NO_PRODUCTION_QTY);
            addNoProductionReason(noProductionQtyReason);
            setIsProduction(YesOrNoEnum.NO.getCode());
            return YesOrNoEnum.NO.getCode();
        }
        //工厂不排产
        if (YesOrNoEnum.YES.getCode().equals(getIsFactoryProduction())) {
            String factoryNoProductionReason = NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.FACTORY_NO_PRODUCTION);
            addNoProductionReason(factoryNoProductionReason);
            setIsProduction(YesOrNoEnum.NO.getCode());
            return YesOrNoEnum.NO.getCode();
        }
        //停产
        if (YesOrNoEnum.YES.getCode().equals(getCantProduce())) {
            String noProduceReason = NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.PRODUCT_STATUS_STOP);
            addNoProductionReason(noProduceReason);
            setIsProduction(YesOrNoEnum.NO.getCode());
            return YesOrNoEnum.NO.getCode();
        }
        return YesOrNoEnum.YES.getCode();
    }

    /**
     * 检查物料基础业务
     *
     * @param isProduction
     * @return
     */
    private String checkMaterialCondition(String isProduction) {
        //没有英寸
        if (StringUtils.isBlank(getProSize())) {
            isProduction = YesOrNoEnum.NO.getCode();
            String noProSizeReason = NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.NO_PRO_SIZE);
            addNoProductionReason(noProSizeReason);
        }
        //非全钢
        if (!ProductTypeEnum.WHOLE_STEEL.getValue().equalsIgnoreCase(getProductTypeCode())) {
            return isProduction;
        }
        //全钢 - 结构
        if (StringUtils.isBlank(getStructureName())) {
            isProduction = YesOrNoEnum.NO.getCode();
            String noStructureNameReason = NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.NO_STRUCTURE_NAME);
            addNoProductionReason(noStructureNameReason);
        }
        //全钢 - 主花纹
        if (StringUtils.isBlank(getMainPattern())) {
            isProduction = YesOrNoEnum.NO.getCode();
            String noMainPatternReason = NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.NO_MAIN_PATTERN);
            addNoProductionReason(noMainPatternReason);
        }
        return isProduction;
    }

    /**
     * 根据产品品类，设置施工阶段及施工配置信息
     *
     * @param constructionConfigurationList 施工配置信息
     * @param productTypeCode               产品品类
     * @return
     */
    private MonthPlanProductConstructionInfoVo setConstructionStage(List<MonthPlanProductConstructionInfoVo> constructionConfigurationList, String productTypeCode) {
        boolean isPCR = ProductTypeEnum.SEMI_STEEL.getValue().equalsIgnoreCase(getProductTypeCode());
        MonthPlanProductConstructionInfoVo constructionInfo = ConstructionSelector.selectOneConstruction(constructionConfigurationList, getProductTypeCode());
        String constructionCode = constructionInfo.getConstructionCode();
        if (isPCR) {
            ConstructionStageEnum stage = ConstructionStageEnum.matchByConstructionCode(constructionCode);
            if (null != stage) {
                setConstructionStage(stage.getStage());
            }
            return constructionInfo;
        }
        //全钢TBR -排产类型-正式
        if (StringUtils.isBlank(constructionCode)) {
            setConstructionStage(ConstructionStageEnum.NO_CONSTRUCTION.getStage());
        } else {
            setConstructionStage(ConstructionStageEnum.FORMAL_PRODUCTION.getStage());
        }
        return constructionInfo;
    }

    /**
     * 检查施工相关业务
     *
     * @param isProduction 当前状态
     * @return
     */
    private String checkConstructionCondition(String isProduction) {
        //没有配置施工
        if (noConfigurationConstruction) {
            String noConfigurationConstructionReason = NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.NO_CONSTRUCTION_RELATION);
            addNoProductionReason(noConfigurationConstructionReason);
            return YesOrNoEnum.NO.getCode();
        }
        List<MonthPlanNoProductionReasonEnum> noProductionReasonList = new ArrayList<>();
        //没有胎胚号
        if (StringUtils.isBlank(getEmbryoCode())) {
            isProduction = YesOrNoEnum.NO.getCode();
            noProductionReasonList.add(MonthPlanNoProductionReasonEnum.NO_EMBRYO_CODE);
        }
        //没有制造示方书号
        if (StringUtils.isBlank(getEmbryoNo())) {
            noProductionReasonList.add(MonthPlanNoProductionReasonEnum.NO_EMBRYO_NO);
        }
        //没有文字示方书号
        if (StringUtils.isBlank(getTextNo())) {
            noProductionReasonList.add(MonthPlanNoProductionReasonEnum.NO_TEXT_NO);
        }
        //没有硫化示方书号
        if (StringUtils.isBlank(getLhNo())) {
            noProductionReasonList.add(MonthPlanNoProductionReasonEnum.NO_LH_NO);
        }
        if (!CollectionUtils.isEmpty(noProductionReasonList)) {
            String noConstructionAllInfoReason = NoProductionReasonUtils.getConstructionConfigurationAllInfo(noProductionReasonList);
            addNoProductionReason(noConstructionAllInfoReason);
        }
        //非半钢业务--不继续检测
        if (!ProductTypeEnum.SEMI_STEEL.getValue().equals(getProductTypeCode())) {
            return isProduction;
        }
        //半钢业务，检测成型法、硫化时间？
        if (StringUtils.isBlank(getMouldMethod())) {
            String noMouldMethodReason = NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.NO_MOULD_METHOD);
            addNoProductionReason(noMouldMethodReason);
            isProduction = YesOrNoEnum.NO.getCode();
        }
        if (null == getCuringTime()) {
            String noCuringTimeReason = NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.NO_CURING_TIME);
            addNoProductionReason(noCuringTimeReason);
            isProduction = YesOrNoEnum.NO.getCode();
        }
        //todo 施工阶段
        return isProduction;
    }

    /**
     * 设置模具相关业务
     *
     * @param isProduction 当前状态
     * @return
     */
    private String checkMouldInfoCondition(String isProduction) {
        //没有配置模具
        if (configurationMouldQty <= BigDecimal.ZERO.intValue()) {
            String noConfigurationMouldReason = NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.NO_MOULD_RELATION);
            addNoProductionReason(noConfigurationMouldReason);
            return YesOrNoEnum.NO.getCode();
        }
        //没有模具台账
        if (baseMouldQty <= BigDecimal.ZERO.intValue()) {
            String noBaseMouldReason = NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.NO_MOULD_INFO);
            addNoProductionReason(noBaseMouldReason);
            return YesOrNoEnum.NO.getCode();
        }
        //模具状态不对
        if (baseMouldQty > BigDecimal.ZERO.intValue() && getMouldQty() <= BigDecimal.ZERO.intValue()) {
            String statusMouldReason = NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.MOULD_STATUS_DISABLE);
            addNoProductionReason(statusMouldReason);
            return YesOrNoEnum.NO.getCode();
        }
        return isProduction;
    }

    /**
     * 检测日硫化相关配置
     *
     * @param isProduction
     * @return
     */
    private String checkLhCapacityCondition(String isProduction) {
        //没有配置
        if (noConfigurationLh) {
            String noConfigurationLhReason = NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.NO_DAY_LH_CAPACITY_RELATION);
            addNoProductionReason(noConfigurationLhReason);
            return YesOrNoEnum.NO.getCode();
        }
        if (!ProductTypeEnum.WHOLE_STEEL.getValue().equals(getProductTypeCode())) {
            return isProduction;
        }
        //全钢业务-没有日硫化量
        if (null == getDayVulcanizationQty()) {
            String noDayVulcanizationQtyReason = NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.NO_DAY_LH_CAPACITY);
            addNoProductionReason(noDayVulcanizationQtyReason);
            return YesOrNoEnum.NO.getCode();
        }
        return isProduction;
    }

    /**
     * 增加不排原因
     *
     * @param addNoProductionReason
     */
    private void addNoProductionReason(String addNoProductionReason) {
        if (StringUtils.isBlank(addNoProductionReason)) {
            return;
        }
        String noProductionReason = getNoProductionReason();
        if (PubUtil.isEmpty(noProductionReason)) {
            setNoProductionReason(addNoProductionReason);
        } else {
            setNoProductionReason(String.format("%s,%s", noProductionReason, addNoProductionReason));
        }
    }

    /**
     * 产能需求量 = 高优先级(含损耗量) + 非高优先级(含损耗量)
     *
     * @return
     */
    public int getCxCapacityRequireQty() {
        cxCapacityRequireQty = getFactProdReqQty();
        return cxCapacityRequireQty;
    }

    /**
     * 分组：结构+主花纹
     *
     * @return 分组
     */
    public String getGroupKey() {
        String keyFormat = "%s|*|%s";
        return String.format(keyFormat, getStructureName(), getMainPattern());
    }
}

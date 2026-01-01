package com.zlt.aps.factory.domain.vo;

import com.tlt.aps.enums.*;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.CxContinueSkuInfoHelper;
import com.zlt.aps.factory.utils.NoProductionReasonUtils;
import com.zlt.aps.monthplan.api.domain.entity.ProductionMonthPlanInit;
import com.zlt.aps.monthplan.api.domain.entity.SaleMonthPlanRequire;
import com.zlt.common.utils.PubUtil;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 工厂月度排产需求计划对象
 *
 * @author ZLT
 * @date 20251209
 */
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
     * 产品状态？
     */
    private String productStatus;
    /**
     * 不可生产标志
     */
    private String cantProduce;

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
    private Long cxCapacityRequireQty;
    /**
     * 高优先级还需排产量
     */
    private Long heightProductionQty;
    /**
     * 总的还需排产量
     */
    private Long productionQty;
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
     * 获取计划可排产量 = 排产净需求 + 常规储备 + 可能排产(暂缓)
     *
     * @return
     */
    public Long getPlanNeedProductionQty() {
        Long sum = BigDecimal.ZERO.longValue();
        if (null != getNetQty()) {
            sum = sum + getNetQty();
        }
        if (null != getConventionReserveQty()) {
            sum = sum + getConventionReserveQty();
        }
        if (ProductionPlanType.NORMAL.getPlanType().equals(planType)) {
            return sum;
        }
        if (null != getPostponeQty()) {
            sum = sum + getFactProdReqQty();
        }
        return sum;
    }

    /**
     * 虚拟的还需排产量值
     * 如果有高优先级值，则为高优先级，否则为净需求值
     *
     * @return
     */
    public Long getVirtualProductionQty() {
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
    public boolean isLess(Long minQty) {
        if (getHeightProductionQty() > BigDecimal.ZERO.longValue()) {
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
    public void calculateInventorySalesRatio(Long productionQty) {
        //月均销量没有或是为零，则表示库销比越低，最高
        Long averageSaleQty = getAverageSaleQty();
        if (null == averageSaleQty || averageSaleQty <= BigDecimal.ZERO.longValue()) {
            inventorySalesRatio = BigDecimal.valueOf(Integer.MIN_VALUE).doubleValue();
        }
        if (null == productionQty || productionQty < BigDecimal.ZERO.longValue()) {
            productionQty = BigDecimal.ZERO.longValue();
        }
        Long sumStockQty = getStockQty() + productionQty;
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
    public static MonthPlanProductionRequirePlanVo buildInitProductionPlan(Context context, String productionVersion, SaleMonthPlanRequire require) {
        MonthPlanProductionRequirePlanVo plan = new MonthPlanProductionRequirePlanVo();
        BeanUtils.copyProperties(require, plan);
        plan.setId(null);
        plan.setProductionVersion(productionVersion);
        plan.setMonthPlanId(require.getId());
        //默认可生产
        plan.setCantProduce(YesOrNoEnum.NO.getCode());
        //排产为空，则默认可排产
        if (StringUtils.isBlank("")) {
            plan.setIsProduction(YesOrNoEnum.YES.getCode());
        }
        return plan;
    }

    /**
     * 设置物料基础信息属性
     * 不可生产标志等
     *
     * @param productBaseInfo
     */
    public void setProductBaseInfo(ProductBaseInfoVo productBaseInfo) {
        if (null == productBaseInfo) {
            return;
        }
        setCantProduce(productBaseInfo.getCantProduce());
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
        setMouldMethod(constructionInfo.getMouldMethod());
        setSpecCode(constructionInfo.getSpecCode());
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
        setDayVulcanizationQty(lhCapacity.getDayVulcanizationQty());
        setCuringTime(lhCapacity.getVulcanizationTime());
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
     * 如果不排产，则不排
     * 否则判断本轮次排产标记
     *
     * @return
     */
    public boolean hasProductionThisRound() {
        if (hasProduction()) {
            return YesOrNoEnum.YES.getValue().equals(isThisRound);
        }
        return YesOrNoEnum.YES.getValue().equals(isThisRound);
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
        if (YesOrNoEnum.NO.getCode().equals(getIsProduction())) {
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
    public Long getMaxDaySingleLhMachineQty() {
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
     * 检测不排继续往下匹配的不排产原因
     * 1、计划本身不排产
     * 2、没有物料编码
     * 3、计划没有排产需求
     * 4、工厂不排产
     * 5、停产
     *
     * @return
     */
    private String checkNoContinueCondition() {
        if (YesOrNoEnum.NO.getCode().equals(getIsProduction())) {
            String planNoProductionReason = NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.PLAN_NO_PRODUCTION);
            addNoProductionReason(planNoProductionReason);
            return YesOrNoEnum.NO.getCode();
        }
        //没有物料编码
        if (StringUtils.isBlank(getMaterialCode())) {
            String noHasMaterialCode = NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.NO_HAS_PRODUCT_CODE);
            addNoProductionReason(noHasMaterialCode);
            setIsProduction(YesOrNoEnum.NO.getCode());
            return YesOrNoEnum.NO.getCode();
        }
        //无排产量
        if (getPlanNeedProductionQty() <= BigDecimal.ZERO.longValue()) {
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
        if (isPCR) {
            //优先一次法 按成型法排序 1-1次法 2-2次法
            constructionConfigurationList.sort(Comparator.comparing(MonthPlanProductConstructionInfoVo::getMouldMethod));
        }
        MonthPlanProductConstructionInfoVo constructionInfo = constructionConfigurationList.get(0);
        String constructionCode = constructionInfo.getConstructionCode();
        if (isPCR) {
            setConstructionStage(ConstructionStageEnum.matchByConstructionCode(constructionCode).getStage());
            return constructionInfo;
        }
        //全钢TBR -施工阶段-正式
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
        //没有胎胚号
        if (StringUtils.isBlank(getEmbryoCode())) {
            String emptyEmbryoReason = NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.NO_EMBRYO_CODE);
            addNoProductionReason(emptyEmbryoReason);
            isProduction = YesOrNoEnum.NO.getCode();
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
}

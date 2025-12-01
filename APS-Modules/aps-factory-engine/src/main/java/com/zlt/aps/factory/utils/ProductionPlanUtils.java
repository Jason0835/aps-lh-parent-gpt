package com.zlt.aps.factory.utils;

import com.alibaba.fastjson.JSON;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.Constant;
import com.tlt.aps.constant.StringConstant;
import com.tlt.aps.enums.*;
import com.tlt.aps.exception.BusinessException;
import com.tlt.aps.utils.BeanCopyUtils;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.dto.ProductProductionHelper;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.enums.PlanProductionSortEnum;
import com.zlt.aps.factory.enums.ProductionOrientEnum;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.factory.scheduling.moulding.GroupPlanProductionContext;
import com.zlt.aps.factory.scheduling.moulding.SinglePlanProductionContext;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoProductionRecord;
import com.zlt.aps.monthplan.api.domain.entity.PlanOrderSortConfiguration;
import com.zlt.aps.monthplan.api.domain.entity.SaleMonthPlanRequire;
import com.zlt.aps.monthplan.api.domain.vo.ProductSpecInfoVo;
import com.zlt.common.utils.PubUtil;
import com.zlt.common.utils.SafeCompute;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 排产计划相关业务
 * 针对计划的一些公用方法实现
 *
 * @author ZLT
 * @date 20250315
 */
@Slf4j
public class ProductionPlanUtils {
    /**
     * 备胎标记 T
     */
    private static final String RF_FLAG = "T";
    /**
     * 分组的起始倍数 10万
     */
    private static final Long GROUP_MULTIPLE = 100000L;

    /**
     * 根据制造需求计划信息，构建排产初始化信息
     * 主要信息--物料寸口，施工阶段、实际生产需求量(拆A)
     * 利润优先值、是否备胎等基础信息，
     * 依赖前面的物料信息、拆A、利润优先值等设置
     *
     * @param productionContext
     * @param monthPlanRequire
     * @return
     */
    public static MonthPlanManufacturingRequirementVo buildMonthPlanInit(ProductionContext productionContext, SaleMonthPlanRequire monthPlanRequire) {
        MonthPlanManufacturingRequirementVo monthPlanInit = new MonthPlanManufacturingRequirementVo();
        BeanUtils.copyProperties(monthPlanRequire, monthPlanInit);
        String productCode = monthPlanInit.getProductCode();
        String locationType = monthPlanInit.getLocationType();
        monthPlanInit.setId(null);
        monthPlanInit.setMonthPlanVersion(productionContext.getMonthPlanVersion());
        monthPlanInit.setProductionVersion(productionContext.getProductionVersion());
        monthPlanInit.setMonthPlanId(monthPlanRequire.getId());
        monthPlanInit.setPreemptMouldCodeSet(new HashSet<>());
        //硫化时间、寸口信息
        ProductBaseInfoVo baseInfo = productionContext.getProductInfoMap().get(productCode);
        if (null != baseInfo) {
            //寸口
            monthPlanInit.setProSize(baseInfo.getProSize());
            //轮胎类型
            monthPlanInit.setTireType(baseInfo.getTireType());
            //设置硫化时间--冬夏季切换
            setCuringTimeAndSpecCodeInfo(monthPlanInit, productionContext);
        }
        //生产需求计划 = 制造需求计划
        monthPlanInit.setProdReqPlan(monthPlanRequire.getPlanQty());
        if (null == monthPlanInit.getProdReqPlan()) {
            monthPlanInit.setProdReqPlan(BigDecimal.ZERO.longValue());
        }
        /**
         * 实际生产需求
         * 1、初始 = 生产需求计划
         * 2、根据OEE率配置，匹配 = 向上取整[初始 / (1- 损耗率/100)]
         */
        monthPlanInit.setFactProdReqQty(monthPlanInit.getProdReqPlan());
        //拆A 处理
        BigDecimal damageConfiguration = productionContext.getProductDamageMap().get(productCode);
        if (null != damageConfiguration) {
            Long factoryProdReqQty = monthPlanInit.getFactProdReqQty();
            // 损耗率= 1- (损耗率/100)--保留四位小数
            BigDecimal damageRate = BigDecimal.ONE.subtract(damageConfiguration.divide(BigDecimal.valueOf(100L), 4, RoundingMode.HALF_UP));
            monthPlanInit.setFactProdReqQty(new BigDecimal(factoryProdReqQty).divide(damageRate, 0, RoundingMode.UP).longValue());
        }
        //可排产量 = 实际生产需求量
        monthPlanInit.setProductionQty(monthPlanInit.getFactProdReqQty());
        monthPlanInit.setNoProductionQty(BigDecimal.ZERO.longValue());
        //利润优先值
        Integer profitGrade = productionContext.getProductLocationProfitGradeMap().get(String.format(ProductionConstant.PRODUCT_PROFIT_KEY_FORMAT, productCode, locationType));
        if (null == profitGrade) {
            monthPlanInit.setProfitGrade(ProductionConstant.DEFAULT_PROFIT);
        } else {
            monthPlanInit.setProfitGrade(profitGrade);
        }
//            monthPlanInitVO.setTradeMode(monthPlanEntity.getTradeMode());
        //施工阶段
        ConstructionStageEnum stage = productionContext.getConstructionStageMap().get(productCode);
        monthPlanInit.setConstructionStageType(stage);
        if (null != stage) {
            monthPlanInit.setConstructionStage(stage.getStage());
        }
        //是否备胎
        if (monthPlanInit.getProductCode().substring(0, 1).equalsIgnoreCase(RF_FLAG)) {
            monthPlanInit.setRf(Constant.TRUE);
        } else {
            monthPlanInit.setRf(Constant.FALSE);
        }
        return monthPlanInit;
    }

    /**
     * 初始化检查，检查不通过则设置不排产及不排产原因
     * 默认可排产
     * 包含没有找到可用模具，没有配置硫化时间，没有寸口信息
     * 分厂不进行排产
     *
     * @param monthPlanInitList
     */
    public static void initCheck(List<MonthPlanManufacturingRequirementVo> monthPlanInitList) {
        if (CollectionUtils.isEmpty(monthPlanInitList)) {
            return;
        }
        for (MonthPlanManufacturingRequirementVo monthPlanInit : monthPlanInitList) {
            //20250606 已经标记不排产的跳过，因寸口产能、轮胎类型产能控制提前标记不排产
            if (YesOrNoEnum.NO.getValue().equals(monthPlanInit.getIsProduction())) {
                continue;
            }
            //本身没有需求量的跳过
            Long factoryProductionReqQty = monthPlanInit.getFactProdReqQty();
            if (null == factoryProductionReqQty || factoryProductionReqQty <= BigDecimal.ZERO.longValue()) {
                continue;
            }
            //20250911 ZLT 施工阶段-没有配置施工
            if (null == monthPlanInit.getConstructionStageType() || ConstructionStageEnum.NO_CONSTRUCTION == monthPlanInit.getConstructionStageType()) {
                setNoProductionReason(monthPlanInit, NoProductionReasonUtils.getNoConfigurationConstructionError(), monthPlanInit.getFactProdReqQty());
                continue;
            }
            //20250911 ZLT 施工阶段-不是正式施工
            if (ConstructionStageEnum.TRIAL_PRODUCTION == monthPlanInit.getConstructionStageType() || ConstructionStageEnum.MEASUREMENT == monthPlanInit.getConstructionStageType()) {
                setNoProductionReason(monthPlanInit, NoProductionReasonUtils.getNoFormalProductionConstructionError(), monthPlanInit.getFactProdReqQty());
                continue;
            }

            //硫化时间
            if (null == monthPlanInit.getCuringTime() || SafeCompute.compareToZero(monthPlanInit.getCuringTime()) == 0) {
                setNoProductionReason(monthPlanInit, NoProductionReasonUtils.getCuringTimeError(), monthPlanInit.getFactProdReqQty());
                continue;
            }
            //寸口
            if (null == monthPlanInit.getProSize()) {
                setNoProductionReason(monthPlanInit, NoProductionReasonUtils.getProSizeError(), monthPlanInit.getFactProdReqQty());
                continue;
            }
            //生胎代码、规格代号
            if (StringUtils.isBlank(monthPlanInit.getEmbryoCode()) || StringUtils.isBlank(monthPlanInit.getSpecCode()) || StringUtils.isBlank(monthPlanInit.getMouldMethod())) {
                setNoProductionReason(monthPlanInit, NoProductionReasonUtils.getEmbryoCodeError(), monthPlanInit.getFactProdReqQty());
                continue;
            }
            //分厂是否排产
            if (PubUtil.isTrue(monthPlanInit.getIsFactoryProduction())) {
                //存入json语言包 2024.12.2 分厂的未排原因需要根据语言进行切换
                setNoProductionReason(monthPlanInit, NoProductionReasonUtils.getFactoryNoProductionError(), monthPlanInit.getFactProdReqQty());
                continue;
            }
            //可排产量为零
            if (monthPlanInit.getProductionQty() <= BigDecimal.ZERO.longValue()) {
                setNoProductionReason(monthPlanInit, NoProductionReasonUtils.getNoProductionQty(), monthPlanInit.getProductionQty());
                continue;
            }
            //有无模具
            if (null == monthPlanInit.getMouldQty() || monthPlanInit.getMouldQty().intValue() == BigDecimal.ZERO.intValue()) {
                setNoProductionReason(monthPlanInit, NoProductionReasonUtils.getNoConfigurationMouldError(), monthPlanInit.getFactProdReqQty());
                continue;
            }
            monthPlanInit.setIsProduction(Constant.TRUE);
        }
    }

    /**
     * 对不满足最小批量的计划，设置排产量为零
     *
     * @param monthPlanInitList 排产计划
     * @param productionContext 排产上下文
     * @param isSetReason       是否需要设置原因
     */
    public static void rejectFallShortOfMinQty(List<MonthPlanManufacturingRequirementVo> monthPlanInitList, ProductionContext productionContext, boolean isSetReason) {
        if (CollectionUtils.isEmpty(monthPlanInitList)) {
            return;
        }
        Map<String, List<MonthPlanManufacturingRequirementVo>> productGroupMap = monthPlanInitList.stream().collect(Collectors.groupingBy(MonthPlanManufacturingRequirementVo::getProductCode));
        productGroupMap.entrySet().stream().forEach(productGroupEntry -> {
            String productCode = productGroupEntry.getKey();
            List<MonthPlanManufacturingRequirementVo> productGroupPlanList = productGroupEntry.getValue();
            Long minQty = productionContext.getMinimumLotSizeMap().get(productCode);
            if (null == minQty) {
                String noProductionReason = NoProductionReasonUtils.getNoConfigurationMinQtyError();
                productGroupPlanList.stream().forEach(noProductionPlan -> {
                    if (isSetReason) {
                        noProductionPlan.addNoProductionReasonAndQty(noProductionReason, noProductionPlan.getProductionQty());
                    }
                    noProductionPlan.setProductionQty(BigDecimal.ZERO.longValue());
                });
                return;
            }
            Long sumProductionQty = productGroupPlanList.stream().mapToLong(MonthPlanManufacturingRequirementVo::getProductionQty).sum();
            if (sumProductionQty <= BigDecimal.ZERO.longValue()) {
                return;
            }
            if (sumProductionQty < minQty) {
                String noProductionReason = NoProductionReasonUtils.getNoFallShortOfMinQtyError(sumProductionQty, minQty);
                productGroupPlanList.stream().forEach(noProductionPlan -> {
                    if (isSetReason) {
                        noProductionPlan.addNoProductionReasonAndQty(noProductionReason, noProductionPlan.getProductionQty());
                    }
                    noProductionPlan.setProductionQty(BigDecimal.ZERO.longValue());
                });
            }
        });
    }

    /**
     * 根据排产上下文，构建物料的排产硫化规格代号、胚胎代码、成型法
     * 1、如果是续作规格，则延续其硫化规格代号
     * 2、如果不是续作，则看配置的模具关系中规格代号
     * 2.1、如果只有规格代号，则取该规格代号对应的SAP与施工中的施工代号、胚胎代码、成型法
     * 2.2、如果有多个，则再根据SAP与施工关系，取成型法为1次法的施工代号、胚胎代码、成形法
     *
     * @param productCode       物料编码
     * @param productionContext 排产上下文
     * @param planQty           需要计划量
     * @return
     */
    public static ProductProductionHelper getProductProductionInfo(String productCode, ProductionContext productionContext, Long planQty) {
        Map<String, ProductConstructionInfoVo> productConstructionInfoMap = productionContext.getConstructionConfigurationMap().get(productCode);
        if (CollectionUtils.isEmpty(productConstructionInfoMap)) {
            return ProductProductionHelper.buildEmpty();
        }
        //施工配置信息
        List<ProductConstructionInfoVo> constructionInfoList = productConstructionInfoMap.values().stream().collect(Collectors.toList());
        List<ProductSpecInfoVo> productSpecInfoList = BeanCopyUtils.copyBeanList(constructionInfoList, ProductSpecInfoVo.class);
        String specCodeInfo = JSON.toJSONString(productSpecInfoList);
        //如果计划量为零，则忽略模具匹配--因计划量为零会不找模具关系，当整个规格都没有计划量时，则不会查找模具，导致提示信息不准
        if (planQty <= BigDecimal.ZERO.longValue()) {
            ProductSpecInfoVo specInfo = productSpecInfoList.get(0);
            for (ProductSpecInfoVo productSpecInfo : productSpecInfoList) {
                if (FormingMethodTypeEnum.SINGLE_STAGE_TIRE.getMethodValue().equals(productSpecInfo.getMouldMethod())) {
                    specInfo = productSpecInfo;
                    break;
                }
            }
            return new ProductProductionHelper(productCode, specInfo, specCodeInfo, constructionInfoList.get(0));
        }
        Map<String, List<MouldProductionProductVo>> continueProductMap = productionContext.getContinueProductMap();
        //续作规格
        if (continueProductMap.containsKey(productCode)) {
            List<MouldProductionProductVo> continueInfo = continueProductMap.get(productCode);
            String specCode = continueInfo.get(0).getSpecCode();
            ProductConstructionInfoVo productConstructionInfo = productConstructionInfoMap.get(specCode);
            if (null == productConstructionInfo) {
                return ProductProductionHelper.buildEmpty();
            }
            ProductSpecInfoVo specInfo = BeanCopyUtils.copyBean(productConstructionInfo, ProductSpecInfoVo.class);
            return new ProductProductionHelper(productCode, specInfo, specCodeInfo, productConstructionInfo);
        }
        //非续作--或者模具配置 模具号|*|规格代号
        Set<String> mouldAndSpecCodeSet = productionContext.getProductRelationMouldMap().get(productCode);
        if (CollectionUtils.isEmpty(mouldAndSpecCodeSet)) {
            return ProductProductionHelper.buildEmpty();
        }
        Set<String> specCodeSet = new HashSet<>();
        mouldAndSpecCodeSet.stream().forEach(mouldSpec -> {
            String[] mouldSpecRelationInfo = mouldSpec.split(ProductionConstant.PRODUCT_SPLIT);
            specCodeSet.add(mouldSpecRelationInfo[1]);
        });
        List<String> specCodeList = new ArrayList<>(specCodeSet);
        //模具关系中只有一个规格代号
        if (specCodeList.size() == BigDecimal.ONE.intValue()) {
            String specCode = specCodeList.get(0);
            ProductConstructionInfoVo productConstructionInfo = productConstructionInfoMap.get(specCode);
            //施工关系中没有
            if (null == productConstructionInfo) {
                return ProductProductionHelper.buildEmpty();
            }
            ProductSpecInfoVo specInfo = BeanCopyUtils.copyBean(productConstructionInfo, ProductSpecInfoVo.class);
            return new ProductProductionHelper(productCode, specInfo, specCodeInfo, productConstructionInfo);
        }
        //施工关系中只有一个关系
        if (constructionInfoList.size() == BigDecimal.ONE.intValue()) {
            ProductConstructionInfoVo productConstructionInfo = constructionInfoList.get(0);
            //模具关系中没有
            if (!specCodeSet.contains(productConstructionInfo.getSpecCode())) {
                return ProductProductionHelper.buildEmpty();
            }
            ProductSpecInfoVo specInfo = BeanCopyUtils.copyBean(productConstructionInfo, ProductSpecInfoVo.class);
            return new ProductProductionHelper(productCode, specInfo, specCodeInfo, productConstructionInfo);
        }
        //有两个施工关系，取一次法
        for (String specCode : specCodeList) {
            ProductConstructionInfoVo productConstructionInfo = productConstructionInfoMap.get(specCode);
            if (null != productConstructionInfo && FormingMethodTypeEnum.SINGLE_STAGE_TIRE.getMethodValue().equals(productConstructionInfo.getMouldMethod())) {
                ProductSpecInfoVo specInfo = BeanCopyUtils.copyBean(productConstructionInfo, ProductSpecInfoVo.class);
                return new ProductProductionHelper(productCode, specInfo, specCodeInfo, productConstructionInfo);
            }
        }
        return ProductProductionHelper.buildEmpty();
    }

    /**
     * 处理一次法的胎体布层级，一次法统一将胎体层级修改为多层，
     * 因一次法成型产能都是多层的
     *
     * @param monthPlanList
     */
    public static void handlerOneMethodTireFabricNumber(List<MonthPlanManufacturingRequirementVo> monthPlanList) {
        if (CollectionUtils.isEmpty(monthPlanList)) {
            return;
        }
        monthPlanList.stream().forEach(requirementPlan -> {
            //不是一次法
            if (!FormingMethodTypeEnum.SINGLE_STAGE_TIRE.getMethodValue().equals(requirementPlan.getMouldMethod())) {
                return;
            }
            requirementPlan.setTireFabricNumber(ProductionConstant.MULTILAYER_TIRE_FABRIC);
        });
    }

    /**
     * 判断排产计划productionPlan在productionDate是否为新增规格
     *
     * @param productionContext 排产上下文
     * @param isPreFlag         是否预排
     * @param productionOrient  排产方向
     * @param productionDate    排产日
     * @param productionPlan    排产计划
     * @return
     */
    public static boolean isAddProductByDay(ProductionContext productionContext, boolean isPreFlag, ProductionOrientEnum productionOrient, Integer productionDate, MonthPlanManufacturingRequirementVo productionPlan) {
        //反向排产，都不是新增规格--忽略
        if (ProductionOrientEnum.REVERSE == productionOrient) {
            return false;
        }
        //停产日，不是新增规格
        if (productionContext.getFactoryStopDays().contains(productionDate)) {
            return false;
        }
        //是否标记续作
        Integer isContinue = productionPlan.getIsContinue();
        String productCode = productionPlan.getProductCode();
        //20250906 ZLT 因续作不走，故而可直接判断第一天
        boolean isProductionFirstDay = productionContext.isProductionFirstDay(productionDate);
        if (isProductionFirstDay) {
            return true;
        }
//        //如果是第一天且是续作，则不算新增规格
//        if (YesOrNoEnum.YES.getValue().equals(isContinue) && isProductionFirstDay) {
//            return false;
//        }
//        //如果是第一天，且不是续作则是新增规格
//        if (!YesOrNoEnum.YES.getValue().equals(isContinue) && isProductionFirstDay) {
//            return true;
//        }
        //不是第一天，则需要看前一天是否排产，如果排产了则不是新增规格，否则是新增规格
        Integer previousProductionDate = MouldUtils.getPreviousProductionDate(productionContext, productionDate, productionOrient);
        boolean isProduction = MouldBaseUtils.isProductionByProductCode(previousProductionDate, productionContext, productCode, isPreFlag, isContinue);
        if (isProduction) {
            return false;
        }
        return true;
    }

    /**
     * 生成不排产计划记录数据
     * 根据排程计划中isProduction标记和可排产数量<=0
     *
     * @param monthPlanInitList 排产计划信息集合
     */
    public static List<MonthPlanNoProductionRecord> createNoProductionRecordData(List<MonthPlanManufacturingRequirementVo> monthPlanInitList) {
        List<MonthPlanNoProductionRecord> factoryNoProductionPlanList = new ArrayList<>();
        monthPlanInitList.stream().filter(monthPlanInit -> hasNoProduction(monthPlanInit)).forEach(monthPlanInit -> {
            MonthPlanNoProductionRecord noProductionRecord = new MonthPlanNoProductionRecord();
            BeanUtils.copyProperties(monthPlanInit, noProductionRecord);
            noProductionRecord.setId(null);
            noProductionRecord.setQty(monthPlanInit.getNoProductionQty());
            noProductionRecord.setNoProductionReason(monthPlanInit.getNoProductionReason());
            factoryNoProductionPlanList.add(noProductionRecord);
        });
        return factoryNoProductionPlanList;
    }

    /**
     * 对计划按排产顺序配置进行分组
     * 按排产顺序第一顺序进行分组，分组后，每组按排产顺序第二顺序进行排序
     *
     * @param sortConfigurationList 分厂排产顺序配置
     * @param factoryCode           分厂编码
     * @param monthPlanInitList     排产计划
     * @return
     */
    public static List<ProductionPlanGroupVo> getGroupPlan(List<PlanOrderSortConfiguration> sortConfigurationList, String factoryCode, List<MonthPlanManufacturingRequirementVo> monthPlanInitList) {
        String noConfigurationError = I18nUtil.getMessage("alg.data.alter.message.factoryProductionSortConfigurationEmpty");
        if (CollectionUtils.isEmpty(sortConfigurationList)) {
            throw new BusinessException(String.format(noConfigurationError, factoryCode));
        }
        Map<SortHierarchyEnum, List<PlanOrderSortConfiguration>> hierarchyMap = getGroupProductionConfiguration(sortConfigurationList);
        if (!hierarchyMap.containsKey(SortHierarchyEnum.FIRST_HIERARCHY) || !hierarchyMap.containsKey(SortHierarchyEnum.SECOND_HIERARCHY) || !hierarchyMap.containsKey(SortHierarchyEnum.THIRD_HIERARCHY)) {
            throw new BusinessException(String.format(noConfigurationError, factoryCode));
        }
        //设置库位、渠道、品牌的虚拟值
        List<PlanOrderSortConfiguration> locationSortList = hierarchyMap.get(SortHierarchyEnum.THIRD_HIERARCHY);
        monthPlanInitList.stream().forEach(productionPlan -> {
            setSortValue(productionPlan, locationSortList);
        });
        //分组配置
        List<ProductionPlanGroupVo> groupDataList = new ArrayList<>();
        String noSupportedOption = I18nUtil.getMessage("alg.data.alter.message.noSupportedOption");
        List<PlanOrderSortConfiguration> groupSortList = hierarchyMap.get(SortHierarchyEnum.FIRST_HIERARCHY);
        //排产顺序配置
        List<PlanOrderSortConfiguration> productionSortList = hierarchyMap.get(SortHierarchyEnum.SECOND_HIERARCHY);
        Comparator comparatorConfiguration = getComparator(productionSortList);
        List<PlanOrderSortConfiguration> sortList = groupSortList.stream().sorted(Comparator.comparing(PlanOrderSortConfiguration::getPriority)).collect(Collectors.toList());
        List<ProductionFirstSortOptionsEnum> matchList = new ArrayList<>();
        sortList.stream().forEach(groupConfiguration -> {
            String groupCode = groupConfiguration.getOptionCode();
            ProductionFirstSortOptionsEnum sortOption = ProductionFirstSortOptionsEnum.getInstance(groupCode);
            if (null == sortOption) {
                throw new BusinessException(String.format(noSupportedOption, groupCode));
            }
            matchList.add(sortOption);
            ProductionPlanGroupVo groupData = new ProductionPlanGroupVo();
            groupData.setGroup(sortOption);
            List<MonthPlanManufacturingRequirementVo> groupProductionPlanList = monthPlanInitList.stream().filter(productionPlan -> isMatch(productionPlan, sortOption, matchList)).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(groupProductionPlanList)) {
                //20251011 ZLT 备货计划-排产量小的优化级高的处理
                handlerGroupProductionValue(productionSortList, groupProductionPlanList, comparatorConfiguration, groupConfiguration);
//                groupProductionPlanList.stream().forEach(groupProductionPlan -> groupProductionPlan.setGroupType(sortOption));
//                List<MonthPlanManufacturingRequirementVo> afterSortList = (List<MonthPlanManufacturingRequirementVo>) groupProductionPlanList.stream().sorted(comparatorConfiguration).collect(Collectors.toList());
//                setProductionSeq(groupConfiguration.getPriority(), afterSortList);
                groupData.setGroupPlanList(groupProductionPlanList);
            }
            groupDataList.add(groupData);
        });
        return groupDataList;
    }

    /**
     * 获取全计划同规格其它计划集合
     *
     * @param groupPlanProductionContext 分组排产上下文
     * @param monthPlanId                排产计划
     * @param productCode                排产规格
     * @param improveLevel               计划量需小于该值
     * @return
     */
    public static List<MonthPlanManufacturingRequirementVo> getSameProductProductionLimitList(GroupPlanProductionContext groupPlanProductionContext, Long monthPlanId, String productCode, Integer improveLevel) {
        ProductionContext productionContext = groupPlanProductionContext.getProductionContext();
        List<MonthPlanManufacturingRequirementVo> allProductionPlanList = productionContext.getMonthPlanInitMap().values().stream().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(allProductionPlanList)) {
            return Collections.emptyList();
        }
        List<MonthPlanManufacturingRequirementVo> sameProductList = new ArrayList<>();
        allProductionPlanList.stream().forEach(productionPlan -> {
            if (monthPlanId.equals(productionPlan.getMonthPlanId())) {
                return;
            }
            //已排
            if (productionContext.isProductionFinishPlan(productionPlan.getMonthPlanId())) {
                return;
            }
            if (YesOrNoEnum.NO.getValue().equals(productionPlan.getIsProduction())) {
                return;
            }
            Long productionQty = productionPlan.getProductionQty();
            if (PubUtil.safeCompare(BigDecimal.valueOf(productionQty), BigDecimal.ZERO) <= 0) {
                return;
            }
            if (productionQty > improveLevel) {
                return;
            }
            if (!productCode.equals(productionPlan.getProductCode())) {
                return;
            }
            sameProductList.add(productionPlan);
        });
        return sameProductList;
    }

    /**
     * 获取与当前排产计划同规格其它还未排产的计划
     *
     * @param productionContext     排产上下文
     * @param currentProductionPlan 当前排产计划
     * @return
     */
    public static List<MonthPlanManufacturingRequirementVo> getSameProductCodeNoProductionPlanList(ProductionContext productionContext, MonthPlanManufacturingRequirementVo currentProductionPlan) {
        if (null == currentProductionPlan) {
            return Collections.emptyList();
        }
        List<MonthPlanManufacturingRequirementVo> allProductionPlanList = productionContext.getMonthPlanInitMap().values().stream().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(allProductionPlanList)) {
            return Collections.emptyList();
        }
        String productCode = currentProductionPlan.getProductCode();
        List<MonthPlanManufacturingRequirementVo> otherNoProductionPlan = new ArrayList<>();
        allProductionPlanList.stream().forEach(productionPlan -> {
            if (YesOrNoEnum.NO.getValue().equals(productionPlan.getIsProduction())) {
                return;
            }
            if (productionPlan.getProductionQty() <= BigDecimal.ZERO.longValue()) {
                return;
            }
            if (!productCode.equals(productionPlan.getProductCode())) {
                return;
            }
            if (productionPlan.getMonthPlanId().equals(currentProductionPlan.getMonthPlanId())) {
                return;
            }
            otherNoProductionPlan.add(productionPlan);
        });
        if (CollectionUtils.isEmpty(otherNoProductionPlan)) {
            return Collections.emptyList();
        }
        return otherNoProductionPlan;
    }

    /**
     * 判断productCode排产是否收尾
     *
     * @param isDouble                  是否双模排产，用于判断当前计划是否收尾
     * @param leftOverNeedProductionQty 当前计划剩余需排产量
     * @param otherNoProductionPlanList 当前计划同规格的其它还未排的计划
     * @return
     */
    public static boolean isEndByProductCode(boolean isDouble, Long leftOverNeedProductionQty, List<MonthPlanManufacturingRequirementVo> otherNoProductionPlanList) {
        //还有未排计划，则不算规格收尾
        if (!CollectionUtils.isEmpty(otherNoProductionPlanList)) {
            return false;
        }
        //双模排产-小于等于1
        if (isDouble && leftOverNeedProductionQty <= BigDecimal.ONE.longValue()) {
            return true;
        }
        //单模排产-小于等于0
        if (!isDouble && leftOverNeedProductionQty <= BigDecimal.ZERO.longValue()) {
            return true;
        }
        return false;
    }

    /**
     * 获取全计划共用生胎的其它计划集合
     * 若 A 101 B 102 A 501 C 502 结果为 A 101 A 501 B 102 C 502
     *
     * @param groupPlanProductionContext 分组排产上下文
     * @param monthPlanId                排产计划
     * @param embryoCode                 胚胎代码
     * @return
     */
    public static List<MonthPlanManufacturingRequirementVo> getSameEmbryoCodeProductionPlanList(GroupPlanProductionContext groupPlanProductionContext, Long monthPlanId, String embryoCode) {
        if (StringUtils.isBlank(embryoCode)) {
            return Collections.emptyList();
        }
        ProductionContext productionContext = groupPlanProductionContext.getProductionContext();
        List<MonthPlanManufacturingRequirementVo> allProductionPlanList = productionContext.getMonthPlanInitMap().values().stream().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(allProductionPlanList)) {
            return Collections.emptyList();
        }
        //共用生胎计划集合
        List<MonthPlanManufacturingRequirementVo> sameEmbryoCodeList = new ArrayList<>();
        allProductionPlanList.stream().forEach(productionPlan -> addSameConstructionPlan(groupPlanProductionContext, monthPlanId, embryoCode, productionPlan, sameEmbryoCodeList));
        //按排产顺序，并同第一顺序的同规格计划紧接其后。若 A 101 B 102 A 501 C 502 结果为 A 101 A 501 B 102 C 502
        return getListBySortAndProductCode(sameEmbryoCodeList, null);
    }

    /**
     * 对集合计划，按排产顺序升序排产，同一规格中只要有一个优先级高，则其同规格的其它计划接着其后
     *
     * @param planList       计划集合
     * @param sortComparator 排序
     * @return
     */
    public static List<MonthPlanManufacturingRequirementVo> getListBySortAndProductCode(List<MonthPlanManufacturingRequirementVo> planList, Comparator sortComparator) {
        if (CollectionUtils.isEmpty(planList)) {
            return Collections.emptyList();
        }
        if (null == sortComparator) {
            sortComparator = Comparator.comparing(MonthPlanManufacturingRequirementVo::getProductionSequence);
        }
        Comparator realComparator = sortComparator;
        //按SAP代码分组
        Map<String, List<MonthPlanManufacturingRequirementVo>> groupByProductMap = planList.stream().collect(Collectors.groupingBy(MonthPlanManufacturingRequirementVo::getProductCode));
        //按排产顺序，优先级高的排前面
        planList.sort(Comparator.comparing(MonthPlanManufacturingRequirementVo::getProductionSequence));
        List<MonthPlanManufacturingRequirementVo> resultList = new ArrayList<>();
        Set<String> isAdd = new HashSet<>();
        planList.stream().forEach(continuePlan -> {
            String productCode = continuePlan.getProductCode();
            if (isAdd.contains(productCode)) {
                return;
            }
            List<MonthPlanManufacturingRequirementVo> sameProductCodeList = groupByProductMap.get(productCode);
            if (CollectionUtils.isEmpty(sameProductCodeList)) {
                return;
            }
            isAdd.add(productCode);
            sameProductCodeList.sort(realComparator);
            sameProductCodeList.stream().forEach(sameProductCodePlan -> resultList.add(sameProductCodePlan));
        });
        return resultList;
    }

    /**
     * 从分组计划中获取模具关联的其它计划信息
     *
     * @param groupPlanProductionContext 分组排产上下文
     * @param monthPlanId                上一计划ID
     * @param relationProductCodeSet     模具关联的共用物料编码
     * @return
     */
    public static List<MonthPlanManufacturingRequirementVo> getRelationPlanByGroup(GroupPlanProductionContext groupPlanProductionContext, Long monthPlanId, Set<String> relationProductCodeSet, boolean isSameProductCode) {
        List<MonthPlanManufacturingRequirementVo> groupPlanList = groupPlanProductionContext.getProductionPlanGroup().getGroupPlanList();
        if (CollectionUtils.isEmpty(groupPlanList)) {
            return Collections.emptyList();
        }
        if (CollectionUtils.isEmpty(relationProductCodeSet)) {
            return Collections.emptyList();
        }
        //共用模具计划
        List<MonthPlanManufacturingRequirementVo> relationPlanList = new ArrayList<>();
        groupPlanList.stream().forEach(productionPlan -> addSameMouldPlan(groupPlanProductionContext, monthPlanId, relationProductCodeSet, productionPlan, relationPlanList));
        //按排产顺序，并同第一顺序的同规格计划紧接其后。若 A 101 B 102 A 501 C 502 结果为 A 101 A 501 B 102 C 502
        if (isSameProductCode) {
            return getListBySortAndProductCode(relationPlanList, null);
        }
        if (CollectionUtils.isEmpty(relationPlanList)) {
            return Collections.emptyList();
        }
        return relationPlanList;
    }

    /**
     * 获取续作排产计划
     * 按排产顺序排序后，对同规格计划合在一起
     *
     * @param productionContext
     * @return
     */
    public static List<MonthPlanManufacturingRequirementVo> getContinuePlan(ProductionContext productionContext) {
        Map<Long, MonthPlanManufacturingRequirementVo> monthPlanInitMap = productionContext.getMonthPlanInitMap();
        if (CollectionUtils.isEmpty(monthPlanInitMap)) {
            return Collections.emptyList();
        }
        List<MonthPlanManufacturingRequirementVo> continuePlanList = new ArrayList<>();
        monthPlanInitMap.entrySet().stream().forEach(entry -> {
            MonthPlanManufacturingRequirementVo productionPlan = entry.getValue();
            if (YesOrNoEnum.YES.getValue().equals(productionPlan.getIsContinue())) {
                continuePlanList.add(productionPlan);
            }
        });
        if (CollectionUtils.isEmpty(continuePlanList)) {
            return Collections.emptyList();
        }
        Map<String, List<MonthPlanManufacturingRequirementVo>> groupByProductMap = continuePlanList.stream().collect(Collectors.groupingBy(MonthPlanManufacturingRequirementVo::getProductCode));
        continuePlanList.sort(Comparator.comparing(MonthPlanManufacturingRequirementVo::getProductionSequence));
        List<MonthPlanManufacturingRequirementVo> sameProductList = new ArrayList<>();
        Set<String> isAdd = new HashSet<>();
        continuePlanList.stream().forEach(continuePlan -> {
            String productCode = continuePlan.getProductCode();
            if (isAdd.contains(productCode)) {
                return;
            }
            List<MonthPlanManufacturingRequirementVo> sameProductCodeList = groupByProductMap.get(productCode);
            if (CollectionUtils.isEmpty(sameProductCodeList)) {
                return;
            }
            isAdd.add(productCode);
            sameProductCodeList.sort(Comparator.comparing(MonthPlanManufacturingRequirementVo::getProductionSequence));
            sameProductCodeList.stream().forEach(sameProductCodePlan -> sameProductList.add(sameProductCodePlan));
        });
        return sameProductList;
    }

    /**
     * 从分组计划中获取同规格的其他排产计划列表
     *
     * @param groupPlanProductionContext 分组排产上下文
     * @param monthPlanId                上一排产计划ID
     * @param productCode                上一排产计划的物料编码
     * @return
     */
    public static List<MonthPlanManufacturingRequirementVo> getSameProductProductionList(GroupPlanProductionContext groupPlanProductionContext, Long monthPlanId, String productCode) {
        if (StringUtils.isBlank(productCode)) {
            return Collections.emptyList();
        }
        List<MonthPlanManufacturingRequirementVo> groupPlanList = groupPlanProductionContext.getProductionPlanGroup().getGroupPlanList();
        if (CollectionUtils.isEmpty(groupPlanList)) {
            return Collections.emptyList();
        }
        ProductionContext productionContext = groupPlanProductionContext.getProductionContext();
        List<MonthPlanManufacturingRequirementVo> sameProductList = new ArrayList<>();
        groupPlanList.stream().forEach(productionPlan -> {
            if (monthPlanId.equals(productionPlan.getMonthPlanId())) {
                return;
            }
            //已排
            if (productionContext.isProductionFinishPlan(productionPlan.getMonthPlanId())) {
                return;
            }
            if (YesOrNoEnum.NO.getValue().equals(productionPlan.getIsProduction())) {
                return;
            }
            if (PubUtil.safeCompare(BigDecimal.valueOf(productionPlan.getProductionQty()), BigDecimal.ZERO) <= 0) {
                return;
            }
            if (productCode.equals(productionPlan.getProductCode())) {
                sameProductList.add(productionPlan);
            }
        });
        return sameProductList;
    }

    /**
     * 从分组计划中获取同寸口的其他排产计划列表
     *
     * @param groupPlanProductionContext 分组排产上下文
     * @param monthPlanId                上一排产计划ID
     * @param proSize                    上一排产计划的寸口
     * @return
     */
    public static List<MonthPlanManufacturingRequirementVo> getSameProSizeProductionList(GroupPlanProductionContext groupPlanProductionContext, Long monthPlanId, BigDecimal proSize) {
        if (null == proSize) {
            return Collections.emptyList();
        }
        List<MonthPlanManufacturingRequirementVo> groupPlanList = groupPlanProductionContext.getProductionPlanGroup().getGroupPlanList();
        if (CollectionUtils.isEmpty(groupPlanList)) {
            return Collections.emptyList();
        }
        ProductionContext productionContext = groupPlanProductionContext.getProductionContext();
        List<MonthPlanManufacturingRequirementVo> sameProSizeList = new ArrayList<>();
        groupPlanList.stream().forEach(productionPlan -> {
            if (monthPlanId.equals(productionPlan.getMonthPlanId())) {
                return;
            }
            //已排
            if (productionContext.isProductionFinishPlan(productionPlan.getMonthPlanId())) {
                return;
            }
            if (YesOrNoEnum.NO.getValue().equals(productionPlan.getIsProduction())) {
                return;
            }
            if (PubUtil.safeCompare(BigDecimal.valueOf(productionPlan.getProductionQty()), BigDecimal.ZERO) <= 0) {
                return;
            }
            if (proSize.equals(productionPlan.getProSize())) {
                sameProSizeList.add(productionPlan);
            }
        });
        return sameProSizeList;
    }

    /**
     * 从分组计划中获取同寸口的其他排产计划列表
     *
     * @param groupPlanProductionContext 分组排产上下文
     * @param monthPlanId                上一排产计划ID
     * @param proSize                    上一排产计划的寸口
     * @return
     */
    public static List<MonthPlanManufacturingRequirementVo> getCrossSameProSizeProductionList(GroupPlanProductionContext groupPlanProductionContext, Long monthPlanId, BigDecimal proSize, Integer improveLevel) {
        if (null == proSize) {
            return Collections.emptyList();
        }
        ProductionContext productionContext = groupPlanProductionContext.getProductionContext();
        //20250626 先去除有交期分组，不支持跨组
//        ProductionFirstSortOptionsEnum groupType = groupPlanProductionContext.getProductionPlanGroup().getGroup();
//        if (ProductionFirstSortOptionsEnum.DELIVERY_DATE == groupType) {
//            return Collections.emptyList();
//        }
        List<MonthPlanManufacturingRequirementVo> allProductionPlanList = productionContext.getMonthPlanInitMap().values().stream().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(allProductionPlanList)) {
            return Collections.emptyList();
        }
        //20250519 ZLT 因分自然月和非自然月排产，故而不能直接取排产月最大天数
        List<MonthPlanManufacturingRequirementVo> sameProSizeList = new ArrayList<>();
        allProductionPlanList.stream().forEach(productionPlan -> {
            if (monthPlanId.equals(productionPlan.getMonthPlanId())) {
                return;
            }
            //已排
            if (productionContext.isProductionFinishPlan(productionPlan.getMonthPlanId())) {
                return;
            }
            if (YesOrNoEnum.NO.getValue().equals(productionPlan.getIsProduction())) {
                return;
            }
            Long productionQty = productionPlan.getProductionQty();
            if (PubUtil.safeCompare(BigDecimal.valueOf(productionQty), BigDecimal.ZERO) <= 0) {
                return;
            }
            if (productionQty > improveLevel) {
                return;
            }
            if (proSize.equals(productionPlan.getProSize())) {
                sameProSizeList.add(productionPlan);
            }
        });
        return sameProSizeList;
    }

    /**
     * 构建单计划排产上下文
     *
     * @param manufacturingRequirement 即将要排产计划
     * @param productionContext        排产上下文
     * @return
     */
    public static SinglePlanProductionContext buildSinglePlanProductionContext(MonthPlanManufacturingRequirementVo manufacturingRequirement, ProductionContext productionContext) {
        //同一个模具号可能有多个规格代号
        String productCode = manufacturingRequirement.getProductCode();
        Set<String> enableMouldSet = productionContext.getProductRelationMouldMap().get(productCode);
        List<MouldInfoVO> enableMouldList = getPlanMaxEnableMouldInfo(productCode, productionContext);
        //单计划排产上下文
        SinglePlanProductionContext singleContext = new SinglePlanProductionContext();
        singleContext.setProductionPlan(manufacturingRequirement);
        singleContext.setEnableMouldList(enableMouldList);
        singleContext.setEnableMouldSet(enableMouldSet);
        return singleContext;
    }

    /**
     * 根据排产计划SAP编码，获取其最大可用的模具信息
     *
     * @param productCode       SAP规格
     * @param productionContext 排产上下文
     */
    public static List<MouldInfoVO> getPlanMaxEnableMouldInfo(String productCode, ProductionContext productionContext) {
        //物料配置的模具信息：模具编码|*|规格代码
        Set<String> enableMouldSet = productionContext.getProductRelationMouldMap().get(productCode);
        if (CollectionUtils.isEmpty(enableMouldSet)) {
            return Collections.emptyList();
        }
        Set<String> isAddMouldCodeSet = new HashSet<>();
        List<MouldInfoVO> enableMouldList = new ArrayList<>();
        //模具信息
        Map<String, MouldInfoVO> allEnableMap = productionContext.getMouldInfoMap();
        enableMouldSet.stream().forEach(mouldAndSpec -> {
            String mouldCode = mouldAndSpec.split(ProductionConstant.PRODUCT_SPLIT)[0];
            MouldInfoVO mouldInfo = allEnableMap.get(mouldCode);
            if (null == mouldInfo) {
                return;
            }
            //20250328 因同一物料同一模具可有多个规格代号，不能重复加入可用模具
            if (isAddMouldCodeSet.contains(mouldCode)) {
                return;
            }
            isAddMouldCodeSet.add(mouldCode);
            enableMouldList.add(mouldInfo);
        });
        if (CollectionUtils.isEmpty(enableMouldList)) {
            return Collections.emptyList();
        }
        return enableMouldList;
    }

    /**
     * 是否可排产计划
     * true可排产，false 不可排产
     *
     * @param productionPlan
     * @return
     */
    public static boolean isProductionPlan(MonthPlanManufacturingRequirementVo productionPlan) {
        String productCode = productionPlan.getProductCode();
        if (StringUtils.isBlank(productCode)) {
            return false;
        }
        if (YesOrNoEnum.NO.getValue().equals(productionPlan.getIsProduction())) {
            return false;
        }
        Long needProductionQty = productionPlan.getProductionQty();
        if (null == needProductionQty || needProductionQty <= BigDecimal.ZERO.longValue()) {
            return false;
        }
        return true;
    }

    /**
     * 得到productCode初始的汇总需排产量
     *
     * @param productionContext 排产上下文
     * @param productCode       SAP代码
     * @return
     */
    public static Long getInitSumNeedProductionQty(ProductionContext productionContext, String productCode) {
        //统计规格还需总排产量
        List<MonthPlanManufacturingRequirementVo> needProductionList = new ArrayList<>(productionContext.getMonthPlanInitMap().values()).stream().filter(plan -> productCode.equals(plan.getProductCode())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(needProductionList)) {
            return BigDecimal.ZERO.longValue();
        }
        Long needProductionQty = needProductionList.stream().collect(Collectors.summingLong(MonthPlanManufacturingRequirementVo::getProductionQty));
        if (null == needProductionQty) {
            return BigDecimal.ZERO.longValue();
        }
        return needProductionQty;
    }

    /**
     * 根据排产规格信息及排产量，计划单模连续排产的天数，向上取整
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产规格信息
     * @param productionQty     需排产量
     * @return
     */
    public static Integer calculateSingleMouldContinueProductionDays(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, Long productionQty) {
        if (null == productionContext || null == productionPlan) {
            return BigDecimal.ZERO.intValue();
        }
        if (null == productionQty || productionQty <= BigDecimal.ZERO.longValue()) {
            return BigDecimal.ZERO.intValue();
        }
        //计划单条硫化时间(含间隔时间) = 硫化时间 + 间隔时间
        BigDecimal singleCuringTime = ProductUtils.getSingleCuringTime(productionPlan, productionContext);
        if (null == singleCuringTime) {
            return BigDecimal.ZERO.intValue();
        }
        //单模单天产能
        Long singleMouldCapacity = MouldUtils.getSingleMouldCapacity(productionContext, singleCuringTime);
        return BigDecimal.valueOf(productionQty).divide(BigDecimal.valueOf(singleMouldCapacity), 0, RoundingMode.UP).intValue();
    }

    /**
     * 判断是否因S型排产超出产能-导致后续有模具也不排产
     * true 表示不可继续排产
     * false 表示可继续排产
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划信息
     * @param groupIndex        分组信息值
     * @return
     */
    public static boolean isExceedCapacity(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, Integer groupIndex) {
        String productCode = productionPlan.getProductCode();
        return productionContext.getExceedCapacityProductMap().containsKey(productCode);
    }

    /**
     * 设置排产计划的硫化时间
     *
     * @param monthPlanInit     排产计划信息
     * @param productionContext 排产上下文
     */
    private static void setCuringTimeAndSpecCodeInfo(MonthPlanManufacturingRequirementVo monthPlanInit, ProductionContext productionContext) {
        String productCode = monthPlanInit.getProductCode();
        Map<String, ProductConstructionInfoVo> productConstructionMap = productionContext.getConstructionConfigurationMap().get(productCode);
        if (CollectionUtils.isEmpty(productConstructionMap)) {
            return;
        }
        List<ProductConstructionInfoVo> constructionInfoList = productConstructionMap.values().stream().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(constructionInfoList)) {
            return;
        }
        //硫化规格信息
        List<ProductSpecInfoVo> productSpecInfoList = BeanCopyUtils.copyBeanList(constructionInfoList, ProductSpecInfoVo.class);
        monthPlanInit.setSpecCodeInfo(JSON.toJSONString(productSpecInfoList));
        //设置硫化时间
        ProductConstructionInfoVo constructionInfo = constructionInfoList.get(0);
        setCuringTime(productionContext, monthPlanInit, constructionInfo);
    }

    /**
     * 设置 物料的硫化时间，根据冬夏季切换
     *
     * @param productionContext 排产上下文
     * @param monthPlanInit     排产计划
     * @param constructionInfo  施工配置信息
     */
    public static void setCuringTime(ProductionContext productionContext, MonthPlanManufacturingRequirementVo monthPlanInit, ProductConstructionInfoVo constructionInfo) {
        if (null == constructionInfo || null == monthPlanInit) {
            return;
        }
        //冬夏季切换
        if (productionContext.isSummerMonth()) {
            Integer summerCuringTime = constructionInfo.getSummerCuringTime();
            if (null != summerCuringTime) {
                monthPlanInit.setCuringTime(BigDecimal.valueOf(summerCuringTime));
            }
            return;
        }
        Integer winterCuringTime = constructionInfo.getWinterCuringTime();
        if (null != winterCuringTime) {
            monthPlanInit.setCuringTime(BigDecimal.valueOf(winterCuringTime));
        }
    }

    /**
     * 设置不排产原因及标记
     *
     * @param monthPlanInitVO
     * @param reason
     */
    private static void setNoProductionReason(MonthPlanManufacturingRequirementVo monthPlanInitVO, String reason, Long noProductionQty) {
        //标记不排产
        monthPlanInitVO.setIsProduction(Constant.FALSE);
        monthPlanInitVO.setProductionQty(BigDecimal.ZERO.longValue());
        //20250524 ZLT 设置不排产原因及不排产数量
        monthPlanInitVO.addNoProductionReasonAndQty(reason, noProductionQty);
    }

    /**
     * 按层级维度，构建排产顺序分组信息
     *
     * @return
     */
    private static Map<SortHierarchyEnum, List<PlanOrderSortConfiguration>> getGroupProductionConfiguration(List<PlanOrderSortConfiguration> sortConfigurationList) {
        Map<SortHierarchyEnum, List<PlanOrderSortConfiguration>> hierarchyMap = new HashMap<>();
        if (CollectionUtils.isEmpty(sortConfigurationList)) {
            return hierarchyMap;
        }
        sortConfigurationList.stream().forEach(sortConfiguration -> {
            Integer hierarchy = sortConfiguration.getHierarchy();
            SortHierarchyEnum sortHierarchy = SortHierarchyEnum.getInstance(hierarchy);
            if (null == sortHierarchy) {
                return;
            }
            List<PlanOrderSortConfiguration> hierarchyConfigurationList = hierarchyMap.get(sortHierarchy);
            if (null == hierarchyConfigurationList) {
                hierarchyConfigurationList = new ArrayList<>();
            }
            hierarchyConfigurationList.add(sortConfiguration);
            hierarchyMap.put(sortHierarchy, hierarchyConfigurationList);
        });
        return hierarchyMap;
    }

    /**
     * 根据库位类别顺序配置，设置其排序值
     *
     * @param thirdSortConfiguration 第三排产顺序
     * @param productionPlan         排产计划
     * @return
     */
    private static void setSortValue(MonthPlanManufacturingRequirementVo productionPlan, List<PlanOrderSortConfiguration> thirdSortConfiguration) {
        if (CollectionUtils.isEmpty(thirdSortConfiguration)) {
            productionPlan.setLocationSortValue(Integer.MAX_VALUE);
            return;
        }
        for (PlanOrderSortConfiguration sortConfiguration : thirdSortConfiguration) {
            String optionCode = sortConfiguration.getOptionCode();
            String[] options = optionCode.split(StringConstant.SPLIT_SEMICOLON);
            if (check(productionPlan, options[0], options[1], options[2])) {
                productionPlan.setLocationSortValue(sortConfiguration.getPriority());
                break;
            }
        }
        //没有匹配到，如果是备货设置成最大值，即最低
        if (null == productionPlan.getLocationSortValue() && YesOrNoEnum.YES.getValue().equals(productionPlan.getIsStockUp())) {
            productionPlan.setLocationSortValue(Integer.MAX_VALUE);
        }
        //没有匹配到，如果不是备货设置成最大值-1，即第二低
        if (null == productionPlan.getLocationSortValue() && !YesOrNoEnum.YES.getValue().equals(productionPlan.getIsStockUp())) {
            productionPlan.setLocationSortValue(Integer.MAX_VALUE - BigDecimal.ONE.intValue());
        }
    }

    /**
     * 根据排产顺序配置，构建排序比较器对象
     * 优先值越小则排序优先级越高，则表示越优先进行对冲
     *
     * @param sortConfigurations
     * @return
     */
    private static Comparator getComparator(List<PlanOrderSortConfiguration> sortConfigurations) {
        //对配置按优先值升序排序，优先值越小的配置项排序优先级越高
        sortConfigurations.sort(Comparator.comparing(PlanOrderSortConfiguration::getPriority));
        PlanOrderSortConfiguration firstSort = sortConfigurations.get(0);
        PlanProductionSortEnum firstOptionComparator = PlanProductionSortEnum.getInstance(ProductionSecondSortOptionsEnum.getInstance(firstSort.getOptionCode()));
        Comparator first = firstOptionComparator.getComparator();
        for (int index = 1; index < sortConfigurations.size(); index++) {
            PlanOrderSortConfiguration sortConfiguration = sortConfigurations.get(index);
            ProductionSecondSortOptionsEnum optionEnum = ProductionSecondSortOptionsEnum.getInstance(sortConfiguration.getOptionCode());
            Comparator optionComparator = PlanProductionSortEnum.getInstance(optionEnum).getComparator();
            first = first.thenComparing(optionComparator);
        }
        return first;
    }

    /**
     * 根据排产顺序配置，构建备货排序比较器对象
     * 优先值越小则排序优先级越高，则表示越优先进行对冲
     * 备货量越大的优先级越低
     *
     * @param sortConfigurations
     * @return
     */
    private static Comparator getStockUpComparator(List<PlanOrderSortConfiguration> sortConfigurations) {
        //对配置按优先值升序排序，优先值越小的配置项排序优先级越高
        sortConfigurations.sort(Comparator.comparing(PlanOrderSortConfiguration::getPriority));
        PlanOrderSortConfiguration firstSort = sortConfigurations.get(0);
        PlanProductionSortEnum firstOptionComparator = PlanProductionSortEnum.getInstance(ProductionSecondSortOptionsEnum.getInstance(firstSort.getOptionCode()));
        Comparator first = firstOptionComparator.getComparator();
        for (int index = 1; index < sortConfigurations.size(); index++) {
            PlanOrderSortConfiguration sortConfiguration = sortConfigurations.get(index);
            ProductionSecondSortOptionsEnum optionEnum = ProductionSecondSortOptionsEnum.getInstance(sortConfiguration.getOptionCode());
            Comparator optionComparator = PlanProductionSortEnum.getInstance(optionEnum).getComparator();
            //20251011 ZLT 备货计划则计划量越大优先级越低
            if (ProductionSecondSortOptionsEnum.NEED_PRODUCTION_QTY == optionEnum) {
                optionComparator = Comparator.comparing(MonthPlanManufacturingRequirementVo::getSummaryStockUpDemandQty, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(MonthPlanManufacturingRequirementVo::getProductionQty, Comparator.nullsLast(Comparator.naturalOrder()));
            }
            first = first.thenComparing(optionComparator);
        }
        return first;
    }

    /**
     * 对分组计划进行排序，按排序器进行排序，并设置排产计划的排产顺序值
     *
     * @param productionSortList      第二排产顺序配置信息
     * @param groupProductionPlanList 分组的排产计划
     * @param comparatorConfiguration 排序器
     * @param groupConfiguration      分组配置
     */
    private static void handlerGroupProductionValue(List<PlanOrderSortConfiguration> productionSortList, List<MonthPlanManufacturingRequirementVo> groupProductionPlanList, Comparator comparatorConfiguration, PlanOrderSortConfiguration groupConfiguration) {
        String groupCode = groupConfiguration.getOptionCode();
        ProductionFirstSortOptionsEnum sortOption = ProductionFirstSortOptionsEnum.getInstance(groupCode);
        Long startSeq = BigDecimal.ONE.longValue();
        //非其它计划
        if (ProductionFirstSortOptionsEnum.OTHER_PLAN != sortOption) {
            setGroupProductionValue(startSeq, groupProductionPlanList, comparatorConfiguration, groupConfiguration);
            return;
        }
        //其它计划-非备货计划
        List<MonthPlanManufacturingRequirementVo> noStockUpPlanList = groupProductionPlanList.stream().filter(otherPlan -> YesOrNoEnum.NO.getValue().equals(otherPlan.getIsStockUp())).collect(Collectors.toList());
        Long stockUpStartSeq = startSeq;
        if (!CollectionUtils.isEmpty(noStockUpPlanList)) {
            setGroupProductionValue(startSeq, noStockUpPlanList, comparatorConfiguration, groupConfiguration);
            stockUpStartSeq = stockUpStartSeq + noStockUpPlanList.size();
        }
        //备货计划--总备货量小的优先级越高、排产量小的优先级高
        List<MonthPlanManufacturingRequirementVo> stockUpPlanList = groupProductionPlanList.stream().filter(otherPlan -> YesOrNoEnum.YES.getValue().equals(otherPlan.getIsStockUp())).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(stockUpPlanList)) {
            Comparator stockUpComparator = getStockUpComparator(productionSortList);
            setGroupProductionValue(stockUpStartSeq, stockUpPlanList, stockUpComparator, groupConfiguration);
        }
    }

    /**
     * 对分组计划设置排产顺序值
     *
     * @param startSeq                分组排序组内起始值
     * @param groupProductionPlanList 分组计划集合
     * @param comparatorConfiguration 排序器
     * @param groupConfiguration      分组配置
     */
    private static void setGroupProductionValue(Long startSeq, List<MonthPlanManufacturingRequirementVo> groupProductionPlanList, Comparator comparatorConfiguration, PlanOrderSortConfiguration groupConfiguration) {
        String groupCode = groupConfiguration.getOptionCode();
        ProductionFirstSortOptionsEnum sortOption = ProductionFirstSortOptionsEnum.getInstance(groupCode);
        groupProductionPlanList.stream().forEach(groupProductionPlan -> groupProductionPlan.setGroupType(sortOption));
        List<MonthPlanManufacturingRequirementVo> afterSortList = (List<MonthPlanManufacturingRequirementVo>) groupProductionPlanList.stream().sorted(comparatorConfiguration).collect(Collectors.toList());
        setProductionSeq(groupConfiguration.getPriority(), startSeq, afterSortList);
    }

    /**
     * 提取不排产计划条件
     * isProduction = 0 或是 可排产量为0
     *
     * @param monthPlanInit
     * @return
     */
    private static boolean hasNoProduction(MonthPlanManufacturingRequirementVo monthPlanInit) {
        if (null == monthPlanInit) {
            return false;
        }
        if (YesOrNoEnum.NO.getValue().equals(monthPlanInit.getIsProduction())) {
            return true;
        }
        Long productionQty = monthPlanInit.getProductionQty();
        if (null == productionQty) {
            productionQty = BigDecimal.ZERO.longValue();
        }
        return productionQty <= BigDecimal.ZERO.longValue();
    }

    /**
     * 排产计划是否匹配当前分组配置
     *
     * @param productionPlan 计划
     * @param sortOption     当前分组
     * @param sortOptionList 当前分组列
     * @return
     */
    private static boolean isMatch(MonthPlanManufacturingRequirementVo productionPlan, ProductionFirstSortOptionsEnum sortOption, List<ProductionFirstSortOptionsEnum> sortOptionList) {
        if (CollectionUtils.isEmpty(sortOptionList)) {
            return false;
        }
        //无交期分组--则getHasDeliveryDate不能为1
        if (ProductionFirstSortOptionsEnum.DELIVERY_DATE != sortOption && PubUtil.isTrue(productionPlan.getHasDeliveryDate())) {
            return false;
        }
        int index = 1;
        int optionSize = sortOptionList.size();
        for (ProductionFirstSortOptionsEnum matchOption : sortOptionList) {
            Integer value = getValueByOptions(productionPlan, matchOption);
            if (index == optionSize) {
                return PubUtil.isTrue(value);
            }
            if (PubUtil.isTrue(value)) {
                return false;
            }
            index = index + 1;
        }
        return false;
    }

    /**
     * 根据分组值及分组数据，设置其排产顺序值
     *
     * @param groupSeq      分组排序值
     * @param startSeq      组内起始排序值
     * @param afterSortList 已排好序的组内计划
     */
    private static void setProductionSeq(Integer groupSeq, Long startSeq, List<MonthPlanManufacturingRequirementVo> afterSortList) {
        Long planStartSeq = groupSeq * GROUP_MULTIPLE;
        for (MonthPlanManufacturingRequirementVo manufacturingPlan : afterSortList) {
            manufacturingPlan.setProductionSequence(planStartSeq + startSeq);
            startSeq = startSeq + 1;
        }
    }

    /**
     * 校验是否匹配
     * 库位类别严格匹配
     * 渠道*表示全匹配，
     * 品牌*表示全匹配。
     *
     * @param productionPlan 销售提报订单
     * @param locationType   库位类型
     * @param channelCode    渠道编码
     * @param brandCode      品牌编码
     * @return
     */
    private static boolean check(MonthPlanManufacturingRequirementVo productionPlan, String locationType, String channelCode, String brandCode) {
        if (!locationType.equals(productionPlan.getLocationType())) {
            return false;
        }
        if (StringConstant.ALL_MATCH.equals(channelCode) && StringConstant.ALL_MATCH.equals(brandCode)) {
            return true;
        }
        if (StringConstant.ALL_MATCH.equals(channelCode) && !StringConstant.ALL_MATCH.equals(brandCode)) {
            return brandCode.equals(productionPlan.getBrand());
        }
        if (!StringConstant.ALL_MATCH.equals(channelCode) && StringConstant.ALL_MATCH.equals(brandCode)) {
            return channelCode.equals(productionPlan.getChannel());
        }
        return channelCode.equals(productionPlan.getChannel()) && brandCode.equals(productionPlan.getBrand());
    }

    /**
     * 加入到共用生胎的计划计划中
     *
     * @param groupPlanProductionContext 分组排产上下文
     * @param monthPlanId                共用生胎的计划ID
     * @param embryoCode                 生胎代码
     * @param productionPlan             下一个计划
     * @param sameEmbryoCodeList         需要加入的共用生胎集合
     */
    private static void addSameConstructionPlan(GroupPlanProductionContext groupPlanProductionContext, Long monthPlanId, String embryoCode, MonthPlanManufacturingRequirementVo productionPlan, List<MonthPlanManufacturingRequirementVo> sameEmbryoCodeList) {
        ProductionContext productionContext = groupPlanProductionContext.getProductionContext();
        if (monthPlanId.equals(productionPlan.getMonthPlanId())) {
            return;
        }
        //已排
        if (productionContext.isProductionFinishPlan(productionPlan.getMonthPlanId())) {
            return;
        }
        if (YesOrNoEnum.NO.getValue().equals(productionPlan.getIsProduction())) {
            return;
        }
        Long productionQty = productionPlan.getProductionQty();
        if (PubUtil.safeCompare(BigDecimal.valueOf(productionQty), BigDecimal.ZERO) <= 0) {
            return;
        }
        if (!embryoCode.equals(productionPlan.getEmbryoCode())) {
            return;
        }
        sameEmbryoCodeList.add(productionPlan);
    }

    /**
     * 加入共用模具计划集合
     *
     * @param groupPlanProductionContext 分组排产上下文
     * @param monthPlanId                计划ID
     * @param relationProductCodeSet     共用模具的SAP代码集合
     * @param productionPlan             上一排产计划
     * @param relationPlanList           共用模具计划集合
     */
    private static void addSameMouldPlan(GroupPlanProductionContext groupPlanProductionContext, Long monthPlanId, Set<String> relationProductCodeSet, MonthPlanManufacturingRequirementVo productionPlan, List<MonthPlanManufacturingRequirementVo> relationPlanList) {
        ProductionContext productionContext = groupPlanProductionContext.getProductionContext();
        if (monthPlanId.equals(productionPlan.getMonthPlanId())) {
            return;
        }
        //已排
        if (productionContext.isProductionFinishPlan(productionPlan.getMonthPlanId())) {
            return;
        }
        if (YesOrNoEnum.NO.getValue().equals(productionPlan.getIsProduction())) {
            return;
        }
        String productCode = productionPlan.getProductCode();
        if (!relationProductCodeSet.contains(productCode)) {
            return;
        }
        relationPlanList.add(productionPlan);
    }

    /**
     * 根据第一排产分组顺序，获取对应值
     *
     * @param productionPlan
     * @param option
     * @return
     */
    private static Integer getValueByOptions(MonthPlanManufacturingRequirementVo productionPlan, ProductionFirstSortOptionsEnum option) {
        if (ProductionFirstSortOptionsEnum.DELIVERY_DATE == option) {
            return productionPlan.getHasDeliveryDate();
        }
        if (ProductionFirstSortOptionsEnum.EMERGENCY == option) {
            return productionPlan.getIsEmergency();
        }
        if (ProductionFirstSortOptionsEnum.IMPORTANT_CUSTOM == option) {
            return productionPlan.getIsImportantCustom();
        }
        if (ProductionFirstSortOptionsEnum.ENSURE_PLAN == option) {
            return productionPlan.getIsEnsurePlan();
        }
        if (ProductionFirstSortOptionsEnum.ESTIMATE_EXCEED_SHORT == option) {
            return productionPlan.getIsDebitPlan();
        }
        return YesOrNoEnum.YES.getValue();
    }

    private ProductionPlanUtils() {

    }
}
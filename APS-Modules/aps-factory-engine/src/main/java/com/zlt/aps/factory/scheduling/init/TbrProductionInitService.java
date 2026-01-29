package com.zlt.aps.factory.scheduling.init;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.enums.DayVulcanizationModeEnum;
import com.zlt.aps.factory.logrecorder.TbrProductionInitLogRecorder;
import com.zlt.aps.factory.scheduling.AbstractProductionBusinessService;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import com.zlt.aps.factory.utils.MouldRelationDeduplicator;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工厂TBR业务轮胎初始化业务
 * 主要完成排产前的必要要素的检查
 * 1、计划本身是否不符合排产
 * 2、物料本身是否已停产不进行生产
 * 3、物料是否配置了施工工艺信息
 * 4、物料是否配置了模具关系
 * 5、物料是否配置了日硫化产能信息
 *
 * @author
 */
@Slf4j
@Service(value = "tbrProductionInitService")
public class TbrProductionInitService extends AbstractProductionBusinessService {

    public TbrProductionInitService(ProductionSchedulingDataService dataService) {
        super(dataService);
    }

    /**
     * 排产需求计划初始化业务
     * 1、按条件从t_mp_product_require_plan表中获取数据 factoryCode + year + month + monthPlanVersion + isDeleted
     * 2、根据需求计划信息，获取必要的配置关系信息
     * 2.1、SKU与施工关系：t_mdm_sku_construction_ref ：specCode + embryoCode + constructionCode
     * 2.2、SKU与模具关系：t_mdm_sku_mould_rel：materialDesc + mouldCode + factoryCode
     * 2.3、模具基础信息：t_mdm_mould_info：mouldCode + factoryCode
     * 2.4、模具到货计划：t_mdm_mould_delivery_plan：materialCode + factoryCode + mouldCode
     * 2.5、SKU与结构关系：t_mdm_sku_structure_ref：materialCode + factoryCode + structureName
     * 2.6、SKU日硫化产能：t_mdm_sku_lh_capacity：materialCode + factoryCode + mesCapacity/standardCapacity/apsCapacity
     *
     * @param context 排产上下文
     * @param userObj 用户数据
     */
    @Override
    public void run(Context context, Object userObj) {
        if (null == context.getInsertNewProductionVersion()) {
            context.setInsertNewProductionVersion(Boolean.FALSE);
        }
        //创建排产上下文
        TbrProductionContext productionContext = (TbrProductionContext) buildProductionContext(context);
        //保存或是创建排产版本表记录
        saveProductionVersionRecord(productionContext);
        //开始初始化日志
        log.info(TbrProductionInitLogRecorder.addStartInitLog(productionContext));
        //获取需求计划
        List<MonthPlanProductionRequirePlanVo> requirePlanList = getMonthPlanRequirePlan(productionContext);
        if (CollectionUtils.isEmpty(requirePlanList)) {
            throw new BusinessException(I18nUtil.getMessage("alg.data.initCheck.initEmpty"));
        }
        String planType = requirePlanList.get(BigDecimal.ZERO.intValue()).getPlanType();
        context.setPlanType(planType);
        productionContext.setPlanType(planType);
        //获取初始化业务参数设定
        ProductionInitParamConfiguration paramConfiguration = createParamConfiguration(productionContext);
        //SKU-损耗处理
        handlerLoss(productionContext, requirePlanList, paramConfiguration.getOpenLevelRatio());
        //物料基础信息
        Map<String, ProductBaseInfoVo> productBaseInfoMap = getMaterialInfo(productionContext);
        //施工关系
        Map<String, List<MonthPlanProductConstructionInfoVo>> constructionInfoMap = getProductionConstructionInfo(productionContext);
        //模具关系
        Map<String, List<MonthPlanProductMouldInfoVo>> mouldInfoMap = getProductionMouldInfo(productionContext);
        //SKU-日硫化产能
        Map<String, MonthPlanProductLhCapacityVo> lhCapacityMap = getProductLhCapacityInfo(productionContext, paramConfiguration.getDayVulcanizationQtyConfiguration());
        //赋值施工信息，模具，日硫化产能
        requirePlanList.forEach(requirePlan -> {
            String materialCode = requirePlan.getMaterialCode();
            String materialDesc = requirePlan.getMaterialDesc();
            //物料基础信息
            ProductBaseInfoVo productBaseInfo = productBaseInfoMap.get(materialDesc);
            if (null == productBaseInfo) {
                log.info(TbrProductionInitLogRecorder.addSingleMaterialInfoEmptyLog(productionContext, materialDesc));
            }
            requirePlan.setProductBaseInfo(productBaseInfo);
            //施工配置
            List<MonthPlanProductConstructionInfoVo> constructionInfoList = constructionInfoMap.get(materialCode);
            if (CollectionUtils.isEmpty(constructionInfoList)) {
                log.info(TbrProductionInitLogRecorder.addSingleConstructionInfoEmptyLog(productionContext, materialDesc));
            }
            requirePlan.setConstructionInfo(constructionInfoList);
            //模具信息
            List<MonthPlanProductMouldInfoVo> mouldInfoList = mouldInfoMap.get(materialDesc);
            if (CollectionUtils.isEmpty(mouldInfoList)) {
                log.info(TbrProductionInitLogRecorder.addSingleMouldRelationEmptyLog(productionContext, materialDesc));
            }
            requirePlan.setMouldInfo(mouldInfoList);
            //硫化信息 硫化时间，硫化量
            MonthPlanProductLhCapacityVo lhCapacity = lhCapacityMap.get(materialDesc);
            if (null == lhCapacity) {
                log.info(TbrProductionInitLogRecorder.addSingleDayLhCapacityInfoEmptyLog(productionContext, materialDesc));
            }
            requirePlan.setVulcanizationInfo(lhCapacity);
            //不排产检测
            requirePlan.checkProductionConditionByBase();
        });
        //模具预占参数
        if (FactoryConstant.YES_VALUE.equalsIgnoreCase(paramConfiguration.getOpenPreemptionMouldCapacity())) {
            //TODO 模具产能预占计算
        }
        log.info(TbrProductionInitLogRecorder.addInitEndLog(productionContext));
        //保存初始化结果
        saveInitInfo(productionContext, requirePlanList);
        log.info(TbrProductionInitLogRecorder.addSaveInitDataLog(productionContext));
    }


    /**
     * 在版本表中，保存工厂排产记录
     * 如果排产版本表已经存在，则更新其排产周期和自然月标记
     *
     * @param context
     */
    private void saveProductionVersionRecord(Context context) {
        //工厂排产版本更新或是插入记录
        MpFactoryProductionVersion factoryProductionVersion = getDataService().getFactoryMonthPlanVersion(context);
        if (null != factoryProductionVersion) {
            setProductionVersionCycleInfo(factoryProductionVersion, context);
            context.setPlanType(factoryProductionVersion.getPlanType());
            getDataService().updateFactoryProductionVersion(factoryProductionVersion);
            return;
        }
        //不存在，则表示新插入记录，此时需要获取计划类型等
        MpFactoryProductionVersion firstVersion = getDataService().getFirstFactoryMonthPlanVersion(context);
        if (null == firstVersion) {
            String errorFormat = I18nUtil.getMessage("alg.data.before.production.planListIsNull");
            String errorInfo = String.format(errorFormat, context.getYear(), context.getMonth(), context.getMonthPlanVersion());
            throw new BusinessException(errorInfo);
        }
        //设置计划类型
        context.setPlanType(firstVersion.getPlanType());
        factoryProductionVersion = new MpFactoryProductionVersion();
        factoryProductionVersion.setFactoryCode(context.getFactoryCode());
        factoryProductionVersion.setYear(context.getYear());
        factoryProductionVersion.setMonth(context.getMonth());
        factoryProductionVersion.setMonthPlanVersion(context.getMonthPlanVersion());
        factoryProductionVersion.setProductTypeCode(context.getProductType().getValue());
        factoryProductionVersion.setPlanType(firstVersion.getPlanType());
        factoryProductionVersion.setIsSelectedDemand(firstVersion.getIsSelectedDemand());
        //设置月份排产模式自然月或非自然月及开始、结束排产日期
        setProductionVersionCycleInfo(factoryProductionVersion, context);
        getDataService().addFactoryProductionVersion(factoryProductionVersion);
    }

    /**
     * 根据工厂编码 + 年月 + 需求计划版本，获取对应的月需要排产的需求计划
     *
     * @param productionContext
     * @return
     */
    private List<MonthPlanProductionRequirePlanVo> getMonthPlanRequirePlan(TbrProductionContext productionContext) {
        //得到制造需求计划
        List<DpDemandPlan> monthPlanRequireList = getDataService().getFactoryMonthPlan(productionContext);
        if (CollectionUtils.isEmpty(monthPlanRequireList)) {
            String planListIsNull = I18nUtil.getMessage("alg.data.alter.message.planListIsNull");
            throw new BusinessException(String.format(planListIsNull, productionContext.getYear(), productionContext.getMonth(), productionContext.getMonthPlanVersion()));
        }
        List<MonthPlanProductionRequirePlanVo> productionPlanList = new ArrayList<>();
        monthPlanRequireList.forEach(require -> {
            MonthPlanProductionRequirePlanVo productionPlan = MonthPlanProductionRequirePlanVo.buildInitProductionPlan(productionContext, productionContext.getProductionVersion(), require);
            productionPlanList.add(productionPlan);
        });
        return productionPlanList;
    }

    /**
     * 获取初始化业务的参数设定
     *
     * @param productionContext
     * @return
     */
    private ProductionInitParamConfiguration createParamConfiguration(TbrProductionContext productionContext) {
        ProductionInitParamConfiguration configuration = new ProductionInitParamConfiguration();
        List<String> paramCodeList = new ArrayList<>(16);
        paramCodeList.add(MonthPlanEnums.OPEN_PREEMPTION_MOULD.getCode());
        paramCodeList.add(MonthPlanEnums.OPEN_LEVEL_RATIO.getCode());
        paramCodeList.add(MonthPlanEnums.DAY_VULCANIZATION_MODE.getCode());
        Map<String, Object> paramConfigurationMap = getDataService().getFactoryParamByCondition(productionContext, paramCodeList);
        if (CollectionUtils.isEmpty(paramConfigurationMap)) {
            log.info(TbrProductionInitLogRecorder.addInitParamEmptyLog(productionContext));
            return configuration;
        }
        configuration.setOpenPreemptionMouldCapacity((String) paramConfigurationMap.get(MonthPlanEnums.OPEN_PREEMPTION_MOULD.getCode()));
        configuration.setOpenLevelRatio((String) paramConfigurationMap.get(MonthPlanEnums.OPEN_LEVEL_RATIO.getCode()));
        //日硫化量获取
        String dayVulcanizationParam = (String) paramConfigurationMap.get(MonthPlanEnums.DAY_VULCANIZATION_MODE.getCode());
        if (StringUtils.isBlank(dayVulcanizationParam)) {
            configuration.setDayVulcanizationQtyConfiguration(DayVulcanizationModeEnum.STANDARD_CAPACITY);
        } else {
            configuration.setDayVulcanizationQtyConfiguration(DayVulcanizationModeEnum.getInstance(dayVulcanizationParam));
        }
        return configuration;
    }

    /**
     * 获取物料基础信息
     * key = materialDesc: value = MdmMaterialInfo
     * 对物料描述去重(数据问题，应该源头控制)
     *
     * @param productionContext
     * @return
     */
    private Map<String, ProductBaseInfoVo> getMaterialInfo(TbrProductionContext productionContext) {
        List<ProductBaseInfoVo> productBaseInfoList = getDataService().getProductionMaterialInfo(productionContext);
        if (CollectionUtils.isEmpty(productBaseInfoList)) {
            log.info(TbrProductionInitLogRecorder.addMaterialInfoEmptyLog(productionContext));
            return Collections.emptyMap();
        }
        return productBaseInfoList.stream().collect(Collectors.toMap(ProductBaseInfoVo::getMaterialDesc, Function.identity(), (before, after) -> before));
    }

    /**
     * 获取需要排产的SKU的施工配置信息
     * key = materialCode: value = List<MonthPlanProductConstructionInfoVo>
     *
     * @param productionContext
     * @return
     */
    private Map<String, List<MonthPlanProductConstructionInfoVo>> getProductionConstructionInfo(TbrProductionContext productionContext) {
        List<MonthPlanProductConstructionInfoVo> constructionInfoList = getDataService().getProductionConstructionInfo(productionContext);
        if (CollectionUtils.isEmpty(constructionInfoList)) {
            log.info(TbrProductionInitLogRecorder.addConstructionInfoEmptyLog(productionContext));
            return Collections.emptyMap();
        }
        return constructionInfoList.stream().collect(Collectors.groupingBy(MonthPlanProductConstructionInfoVo::getMaterialCode));
    }

    /**
     * 获取需要排产的SKU的模具配置信息
     * key = materialDesc: value = List<MonthPlanProductMouldInfoVo>
     *
     * @param productionContext
     * @return
     */
    private Map<String, List<MonthPlanProductMouldInfoVo>> getProductionMouldInfo(TbrProductionContext productionContext) {
        //已有模具的配置关系
        List<MonthPlanProductMouldInfoVo> productMouldInfoList = getDataService().getProductionMouldInfo(productionContext);
        //新模具到货计划关系
        List<MonthPlanProductMouldInfoVo> mouldDeliveryList = getDataService().getProductionMouldDeliveryInfo(productionContext);
        List<MonthPlanProductMouldInfoVo> allMouldRelationInfoList = MouldRelationDeduplicator.deduplicateAndMerge(productMouldInfoList, mouldDeliveryList);
        if (CollectionUtils.isEmpty(allMouldRelationInfoList)) {
            return Collections.emptyMap();
        }
        return allMouldRelationInfoList.stream().collect(Collectors.groupingBy(MonthPlanProductMouldInfoVo::getMaterialDesc));
    }

    /**
     * 获取SKU的日硫化产能信息
     * key = materialDesc: value = MonthPlanProductLhCapacityVo
     *
     * @param productionContext 排产上下文
     * @param mode              日硫化量模式
     * @return
     */
    private Map<String, MonthPlanProductLhCapacityVo> getProductLhCapacityInfo(TbrProductionContext productionContext, DayVulcanizationModeEnum mode) {
        List<MonthPlanProductLhCapacityVo> lhCapacityList = getDataService().getProductLhCapacityInfo(productionContext);
        if (CollectionUtils.isEmpty(lhCapacityList)) {
            log.info(TbrProductionInitLogRecorder.addDayLhCapacityInfoEmptyLog(productionContext));
            return Collections.emptyMap();
        }
        //计算日硫化产能
        lhCapacityList.forEach(lhCapacity -> lhCapacity.calculateDayVulcanizationQty(mode));
        return lhCapacityList.stream().collect(Collectors.toMap(MonthPlanProductLhCapacityVo::getMaterialDesc, Function.identity(), (before, after) -> after));
    }

    /**
     * 损耗处理，看是否开启采用损耗率
     *
     * @param requirePlanList 需求计划
     */
    private void handlerLoss(Context context, List<MonthPlanProductionRequirePlanVo> requirePlanList, String openLevelRatio) {
        if (CollectionUtils.isEmpty(requirePlanList)) {
            return;
        }
        //采用损耗率方式计算损耗
        if (FactoryConstant.YES_VALUE.equalsIgnoreCase(openLevelRatio)) {
            //TODO 损耗率方式计算
            return;
        }
        //对物料描述为空的进行过滤
        List<MonthPlanProductionRequirePlanVo> effectiveList = requirePlanList.stream().filter(singlePlan -> StringUtils.isNotBlank(singlePlan.getMaterialDesc())).collect(Collectors.toList());
        //按SKU汇总需求，双数 +2 单数 +3。放置在第一条记录
        Map<String, List<MonthPlanProductionRequirePlanVo>> productGroupMap = effectiveList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc));
        productGroupMap.forEach((materialDesc, planList) -> {
            //统计需求量
            if (CollectionUtils.isEmpty(planList)) {
                return;
            }
            Integer addLossQty = getAddLossQtyUnRatio(planList);
            log.info(TbrProductionInitLogRecorder.addInitLossQtyLog(context, materialDesc, addLossQty));
            //排序，高优先级值高的在前，排产净需求值高的在前
            planList.sort(Comparator.comparing(MonthPlanProductionRequirePlanVo::getHeightQty, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(MonthPlanProductionRequirePlanVo::getNetQty, Comparator.nullsLast(Comparator.reverseOrder())));
            for (MonthPlanProductionRequirePlanVo singlePlan : planList) {
                addLossQty = addLossQtyUnRatio(singlePlan, addLossQty);
                if (addLossQty == BigDecimal.ZERO.intValue()) {
                    break;
                }
            }
        });
        requirePlanList.forEach(singlePlan -> {
            if (null == singlePlan.getHeightLossQty()) {
                singlePlan.setHeightLossQty(singlePlan.getHeightQty());
            }
            if (null == singlePlan.getFactProdReqQty()) {
                singlePlan.setFactProdReqQty(singlePlan.getNetQty());
            }
        });
    }

    /**
     * 计算非损耗率需要增加的损耗量
     * 偶数+2，奇数+3
     *
     * @param requireList
     * @return
     */
    private Integer getAddLossQtyUnRatio(List<MonthPlanProductionRequirePlanVo> requireList) {
        List<MonthPlanProductionRequirePlanVo> hasProductionList = requireList.stream().filter(singlePlan -> null != singlePlan.getNetQty()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasProductionList)) {
            return BigDecimal.ZERO.intValue();
        }
        //汇总排产净需求量
        Integer sumQty = hasProductionList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getNetQty).sum();
        if (sumQty <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        //偶数+2
        if (sumQty % ProductionConstant.EVEN_NUMBER == BigDecimal.ZERO.intValue()) {
            return ProductionConstant.ADD_LOSS_QTY_EVEN_NUMBER;
        }
        //奇数+3
        return ProductionConstant.ADD_LOSS_QTY_ODD_NUMBER;
    }

    /**
     * 对计划增加损耗值
     *
     * @param plan       计划
     * @param addLossQty 增加的损耗量
     */
    private Integer addLossQtyUnRatio(MonthPlanProductionRequirePlanVo plan, Integer addLossQty) {
        if (null == addLossQty || addLossQty == BigDecimal.ZERO.intValue()) {
            plan.setHeightLossQty(plan.getHeightQty());
            plan.setFactProdReqQty(plan.getNetQty());
            return BigDecimal.ZERO.intValue();
        }
        //小于，则表示前面的有多
        if (addLossQty < BigDecimal.ZERO.intValue()) {
            boolean isHandler = handlerAddMoreBefore(plan, addLossQty);
            if (isHandler) {
                return BigDecimal.ZERO.intValue();
            }
            return addLossQty;
        }
        if (addLossQty == BigDecimal.ONE.intValue()) {
            return addLossQtyNoFirst(plan, addLossQty);
        }
        return addLossQtyFirst(plan, addLossQty);
    }

    /**
     * 对首条进行处理损耗值处理
     *
     * @param plan       计划
     * @param addLossQty 需要处理的损耗值
     * @return
     */
    private Integer addLossQtyFirst(MonthPlanProductionRequirePlanVo plan, Integer addLossQty) {
        if (null == plan || null == addLossQty || addLossQty <= BigDecimal.ZERO.intValue()) {
            return addLossQty;
        }
        //首次处理
        if (addLossQty == BigDecimal.ONE.intValue()) {
            return addLossQty;
        }
        Integer heightQty = plan.getHeightQty();
        if (null == heightQty) {
            heightQty = BigDecimal.ZERO.intValue();
        }
        Integer netQty = plan.getNetQty();
        if (null == netQty) {
            netQty = BigDecimal.ZERO.intValue();
        }
        Integer addHeightLossQty = BigDecimal.ZERO.intValue();
        //有高优先级需求，则加在高优先级上
        if (heightQty > BigDecimal.ZERO.intValue()) {
            if (heightQty % ProductionConstant.EVEN_NUMBER == BigDecimal.ZERO.intValue()) {
                addHeightLossQty = ProductionConstant.ADD_LOSS_QTY_EVEN_NUMBER;
            } else {
                addHeightLossQty = ProductionConstant.ADD_LOSS_QTY_ODD_NUMBER;
            }
        }
        //高优先级损耗值
        plan.setHeightLossQty(heightQty + addHeightLossQty);
        //除高优先级的损耗值
        Integer otherLossQty = addLossQty - addHeightLossQty;
        if (otherLossQty < BigDecimal.ZERO.intValue()) {
            plan.setFactProdReqQty(netQty);
            return otherLossQty;
        }
        plan.setFactProdReqQty(netQty + otherLossQty);
        return BigDecimal.ZERO.intValue();
    }

    /**
     * 对首条进行处理损耗值处理
     *
     * @param plan       计划
     * @param addLossQty 需要处理的损耗值
     * @return
     */
    private Integer addLossQtyNoFirst(MonthPlanProductionRequirePlanVo plan, Integer addLossQty) {
        if (null == plan || null == addLossQty || addLossQty <= BigDecimal.ZERO.intValue()) {
            return addLossQty;
        }
        //不是首次处理
        if (addLossQty != BigDecimal.ONE.intValue()) {
            return addLossQty;
        }
        Integer heightQty = plan.getHeightQty();
        if (null == heightQty) {
            heightQty = BigDecimal.ZERO.intValue();
        }
        //高优先级为偶数，则不处理 损耗值 = 高优级的量，否则+1
        boolean isHandler = false;
        if (heightQty % ProductionConstant.EVEN_NUMBER == BigDecimal.ZERO.intValue()) {
            plan.setHeightLossQty(heightQty);
        } else {
            isHandler = true;
            plan.setHeightLossQty(heightQty + addLossQty);
        }
        if (isHandler) {
            plan.setFactProdReqQty(plan.getNetQty());
            return BigDecimal.ZERO.intValue();
        }
        Integer netQty = plan.getNetQty();
        if (null == netQty) {
            netQty = BigDecimal.ZERO.intValue();
        }
        //净需求为偶数，则不处理加 损耗值 = 净需求量，否则+1
        if (netQty % ProductionConstant.EVEN_NUMBER == BigDecimal.ZERO.intValue()) {
            plan.setFactProdReqQty(netQty);
        } else {
            isHandler = true;
            plan.setFactProdReqQty(netQty + addLossQty);
        }
        if (isHandler) {
            return BigDecimal.ZERO.intValue();
        }
        return addLossQty;
    }

    /**
     * 处理因前一条记录多加损耗，则后面的需要反向减值
     *
     * @param plan       当前计划是否需要反向减值
     * @param addLossQty 需要反向减掉的值，只有是-1
     * @return true 表示已处理 false表示没有处理
     */
    private boolean handlerAddMoreBefore(MonthPlanProductionRequirePlanVo plan, Integer addLossQty) {
        if (null == plan || null == addLossQty || addLossQty != -BigDecimal.ONE.intValue()) {
            return false;
        }
        boolean isHandler = false;
        Integer heightQty = plan.getHeightQty();
        if (null == heightQty) {
            heightQty = BigDecimal.ZERO.intValue();
        }
        //高优先级为偶数，则不处理减 损耗值 = 高优级的量，否则-1
        if (heightQty % ProductionConstant.EVEN_NUMBER == BigDecimal.ZERO.intValue()) {
            plan.setHeightLossQty(heightQty);
        } else {
            isHandler = true;
            plan.setHeightLossQty(heightQty + addLossQty);
        }
        if (isHandler) {
            plan.setFactProdReqQty(plan.getNetQty());
            return false;
        }
        Integer netQty = plan.getNetQty();
        if (null == netQty) {
            netQty = BigDecimal.ZERO.intValue();
        }
        //净需求为偶数，则不处理减 损耗值 = 净需求量，否则-1
        if (netQty % ProductionConstant.EVEN_NUMBER == BigDecimal.ZERO.intValue()) {
            plan.setFactProdReqQty(netQty);
        } else {
            isHandler = true;
            plan.setFactProdReqQty(netQty + addLossQty);
        }
        return isHandler;
    }

    /**
     * 保存初始化结果-包含提前不排产原因
     *
     * @param productionContext
     * @param requirePlanList
     */
    private void saveInitInfo(TbrProductionContext productionContext, List<MonthPlanProductionRequirePlanVo> requirePlanList) {
        //先删除旧的初始化数据
        deleteOldData(productionContext);
        //再保存新的初始化数据
        getDataService().saveMonthPlanInit(requirePlanList);
    }

    /**
     * 设置生产版本号，如果已经有生产版本号，则不进行设置
     * 否则根据当前时间戳及版本号前缀设置
     * 已有生产版本号，则根据生产版本号删除旧有数据
     *
     * @param productionContext
     */
    private void deleteOldData(TbrProductionContext productionContext) {
        String productionVersion = productionContext.getProductionVersion();
        if (StringUtils.isBlank(productionVersion)) {
            throw new BusinessException(I18nUtil.getMessage("alg.data.alter.message.productionVersionNoEmpty"));
        }
        //删除版本已有数据
        getDataService().deletedInitData(productionContext);
        getDataService().deletedMouldProductionData(productionContext);
    }

    /**
     * 设置分厂排产周期相关信息
     * 标记是自然月排产还是非自然月排产
     * 排产周期的起始日期
     *
     * @param factoryProductionVersion 分厂排产信息对象
     * @param context                  排产上下文
     */
    private void setProductionVersionCycleInfo(MpFactoryProductionVersion factoryProductionVersion, Context context) {
        String productionVersion = context.getProductionVersion();
        factoryProductionVersion.setProductionInitVersion(productionVersion);
        factoryProductionVersion.setProductionStVersion(productionVersion);
        factoryProductionVersion.setProductionVersion(productionVersion);
        factoryProductionVersion.setProductionStartDate(context.getProductionStartDate());
        factoryProductionVersion.setProductionEndDate(context.getProductionEndDate());
        if (context.isNaturalMonth()) {
            factoryProductionVersion.setIsNaturalMonth(YesOrNoEnum.YES.getCode());
        } else {
            factoryProductionVersion.setIsNaturalMonth(YesOrNoEnum.NO.getCode());
        }
    }

}

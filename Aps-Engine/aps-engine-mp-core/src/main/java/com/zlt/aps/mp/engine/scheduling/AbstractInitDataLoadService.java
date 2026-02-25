package com.zlt.aps.mp.engine.scheduling;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.mp.api.domain.entity.DpDemandPlan;
import com.zlt.aps.mp.engine.domain.vo.*;
import com.zlt.aps.mp.engine.enums.DayVulcanizationModeEnum;
import com.zlt.aps.mp.engine.logrecorder.TbrProductionInitLogRecorder;
import com.zlt.aps.mp.engine.scheduling.init.ProductionInitParamConfiguration;
import com.zlt.aps.mp.engine.service.DpRequireDataService;
import com.zlt.aps.mp.engine.service.MonthProductionDataService;
import com.zlt.aps.mp.engine.service.ProductionMdmDataService;
import com.zlt.aps.mp.engine.utils.MouldRelationDeduplicator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 抽象业务
 * 主要实现初始化时的公共业务
 *
 * @author ZLT
 * @date 20260204
 */
@Slf4j
public abstract class AbstractInitDataLoadService extends AbstractBaseProductionService {

    /**
     * 需求计划服务数据提供接口
     */
    private final DpRequireDataService dpRequireDataService;

    public AbstractInitDataLoadService(ProductionMdmDataService dataService,
                                       DpRequireDataService dpRequireDataService,
                                       MonthProductionDataService monthProductionDataService) {
        super(dataService, monthProductionDataService);
        this.dpRequireDataService = dpRequireDataService;
    }

    public DpRequireDataService getDpRequireDataService() {
        return dpRequireDataService;
    }

    /**
     * 根据工厂编码 + 年月 + 需求计划版本，获取对应的月需要排产的需求计划
     *
     * @param productionContext
     * @return
     */
    protected List<MonthPlanProductionRequirePlanVo> getMonthPlanRequirePlan(TbrProductionContext productionContext) {
        //得到制造需求计划
        List<DpDemandPlan> monthPlanRequireList = dpRequireDataService.getFactoryMonthPlan(productionContext);
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
    protected ProductionInitParamConfiguration createInitParamConfiguration(TbrProductionContext productionContext) {
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
    protected Map<String, ProductBaseInfoVo> getMaterialInfo(TbrProductionContext productionContext) {
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
    protected Map<String, List<MonthPlanProductConstructionInfoVo>> getProductionConstructionInfo(TbrProductionContext productionContext) {
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
    protected Map<String, List<MonthPlanProductMouldInfoVo>> getProductionMouldInfo(TbrProductionContext productionContext) {
        //已有模具的配置关系
        List<MonthPlanProductMouldInfoVo> productMouldInfoList = getDataService().getProductionMouldInfo(productionContext);
        //新模具到货计划关系
        List<MonthPlanProductMouldInfoVo> mouldDeliveryList = getDataService().getProductionMouldDeliveryInfo(productionContext);
        List<MonthPlanProductMouldInfoVo> allMouldRelationInfoList = MouldRelationDeduplicator.deduplicateAndMerge(productMouldInfoList, mouldDeliveryList,productionContext);
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
    protected Map<String, MonthPlanProductLhCapacityVo> getProductLhCapacityInfo(TbrProductionContext productionContext, DayVulcanizationModeEnum mode) {
        List<MonthPlanProductLhCapacityVo> lhCapacityList = getDataService().getProductLhCapacityInfo(productionContext);
        if (CollectionUtils.isEmpty(lhCapacityList)) {
            log.info(TbrProductionInitLogRecorder.addDayLhCapacityInfoEmptyLog(productionContext));
            return Collections.emptyMap();
        }
        //计算日硫化产能
        lhCapacityList.forEach(lhCapacity -> lhCapacity.calculateDayVulcanizationQty(mode));
        return lhCapacityList.stream().collect(Collectors.toMap(MonthPlanProductLhCapacityVo::getMaterialDesc, Function.identity(), (before, after) -> after));
    }
}

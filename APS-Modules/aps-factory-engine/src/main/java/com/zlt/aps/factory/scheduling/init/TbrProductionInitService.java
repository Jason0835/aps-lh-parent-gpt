package com.zlt.aps.factory.scheduling.init;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.enums.DayVulcanizationModeEnum;
import com.zlt.aps.factory.scheduling.AbstractProductionBusinessService;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import com.zlt.aps.factory.utils.TbrProductionLogUtils;
import com.zlt.aps.monthplan.api.domain.entity.SaleMonthPlanRequire;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
@Service(value = "generalInitService")
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
     * 2.3、模具基础信息：t_mdm_model_info：mouldCode + factoryCode
     * 2.4、模具到货计划：t_mdm_mould_delivery_plan：materialCode + factoryCode + mouldCode
     * 2.5、SKU与结构关系：t_mdm_sku_structure_ref：materialCode + factoryCode + structureName
     * 2.6、SKU日硫化产能：t_mdm_sku_lh_capacity：materialCode + factoryCode + mesCapacity/standardCapacity/apsCapacity
     *
     * @param context 排产上下文
     * @param userObj 用户数据
     */
    @Override
    public void run(Context context, Object userObj) {
        //创建排产上下文
        TbrProductionContext productionContext = (TbrProductionContext) buildProductionContext(context);
        //开始初始化日志
        String startInitLog = TbrProductionLogUtils.addStartInitLog(productionContext);
        log.info(startInitLog);
        //获取需求计划
        List<MonthPlanProductionRequirePlanVo> requirePlanList = getMonthPlanRequirePlan(productionContext);
        //物料基础信息
        Map<String, ProductBaseInfoVo> productBaseInfoMap = getMaterialInfo(productionContext);
        //施工关系
        Map<String, List<MonthPlanProductConstructionInfoVo>> constructionInfoMap = getProductionConstructionInfo(productionContext);
        //模具关系
        Map<String, List<MonthPlanProductMouldInfoVo>> mouldInfoMap = getProductionMouldInfo(productionContext);
        //模具预占参数
        String openPreemptionMouldCapacity = getDataService().getOpenPreemptionMouldCapacity(productionContext);
        if (FactoryConstant.YES_VALUE.equalsIgnoreCase(openPreemptionMouldCapacity)) {
            //TODO 模具产能预占计算
        }
        //SKU-日硫化产能
        Map<String, MonthPlanProductLhCapacityVo> lhCapacityMap = getProductLhCapacityInfo(productionContext);
        //赋值施工信息，模具，日硫化产能
        requirePlanList.forEach(requirePlan -> {
            String materialCode = requirePlan.getMaterialCode();
            String materialDesc = requirePlan.getMaterialDesc();
            //物料基础信息
            ProductBaseInfoVo productBaseInfo = productBaseInfoMap.get(materialDesc);
            requirePlan.setProductBaseInfo(productBaseInfo);
            //施工配置
            List<MonthPlanProductConstructionInfoVo> constructionInfoList = constructionInfoMap.get(materialCode);
            requirePlan.setConstructionInfo(constructionInfoList);
            //模具信息
            List<MonthPlanProductMouldInfoVo> mouldInfoList = mouldInfoMap.get(materialDesc);
            requirePlan.setMouldInfo(mouldInfoList);
            //硫化信息 硫化时间，硫化量
            MonthPlanProductLhCapacityVo lhCapacity = lhCapacityMap.get(materialDesc);
            requirePlan.setVulcanizationInfo(lhCapacity);
            //不排产检测
            requirePlan.checkProductionConditionByBase();
        });
        String checkEndLog = TbrProductionLogUtils.addInitEndLog(productionContext);
        log.info(checkEndLog);
        //保存初始化结果
        saveInitInfo(productionContext, requirePlanList);
        String saveInitLog = TbrProductionLogUtils.addSaveInitDataLog(productionContext);
        log.info(saveInitLog);
    }

    /**
     * 根据工厂编码 + 年月 + 需求计划版本，获取对应的月需要排产的需求计划
     *
     * @param productionContext
     * @return
     */
    private List<MonthPlanProductionRequirePlanVo> getMonthPlanRequirePlan(TbrProductionContext productionContext) {
        //得到制造需求计划
        List<SaleMonthPlanRequire> monthPlanRequireList = getDataService().getFactoryMonthPlan(productionContext);
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
     * 获取物料基础信息
     * key = materialDesc: value = MdmMaterialInfo
     *
     * @param productionContext
     * @return
     */
    private Map<String, ProductBaseInfoVo> getMaterialInfo(TbrProductionContext productionContext) {
        List<ProductBaseInfoVo> productBaseInfoList = getDataService().getProductionMaterialInfo(productionContext);
        if (CollectionUtils.isEmpty(productBaseInfoList)) {
            return Collections.emptyMap();
        }
        return productBaseInfoList.stream().collect(Collectors.toMap(ProductBaseInfoVo::getMaterialDesc, Function.identity()));
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
        List<MonthPlanProductMouldInfoVo> allMouldRelationInfoList = new ArrayList<>();
        //已有模具的配置关系
        List<MonthPlanProductMouldInfoVo> productMouldInfoList = getDataService().getProductionMouldInfo(productionContext);
        if (!CollectionUtils.isEmpty(productMouldInfoList)) {
            allMouldRelationInfoList.addAll(productMouldInfoList);
        }
        //新模具到货计划关系
        List<MonthPlanProductMouldInfoVo> mouldDeliveryList = getDataService().getProductionMouldDeliveryInfo(productionContext);
        if (!CollectionUtils.isEmpty(mouldDeliveryList)) {
            allMouldRelationInfoList.addAll(mouldDeliveryList);
        }
        if (CollectionUtils.isEmpty(allMouldRelationInfoList)) {
            return Collections.emptyMap();
        }
        return allMouldRelationInfoList.stream().collect(Collectors.groupingBy(MonthPlanProductMouldInfoVo::getMaterialDesc));
    }

    /**
     * 获取SKU的日硫化产能信息
     * key = materialDesc: value = MonthPlanProductLhCapacityVo
     *
     * @param productionContext
     * @return
     */
    private Map<String, MonthPlanProductLhCapacityVo> getProductLhCapacityInfo(TbrProductionContext productionContext) {
        List<MonthPlanProductLhCapacityVo> lhCapacityList = getDataService().getProductLhCapacityInfo(productionContext);
        if (CollectionUtils.isEmpty(lhCapacityList)) {
            return Collections.emptyMap();
        }
        //计算日硫化产能
        DayVulcanizationModeEnum mode = getDataService().getDayVulcanizationQtyConfiguration(productionContext);
        lhCapacityList.forEach(lhCapacity -> lhCapacity.calculateDayVulcanizationQty(mode));
        return lhCapacityList.stream().collect(Collectors.toMap(MonthPlanProductLhCapacityVo::getMaterialDesc, Function.identity()));
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

}

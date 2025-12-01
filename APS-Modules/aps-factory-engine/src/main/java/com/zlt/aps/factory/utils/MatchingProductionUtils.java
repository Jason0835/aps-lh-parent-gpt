package com.zlt.aps.factory.utils;

import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.ConstructionStageEnum;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.dto.ProductProductionHelper;
import com.zlt.aps.factory.domain.vo.BaseConstructionVersionInfoVo;
import com.zlt.aps.factory.domain.vo.MatchingProductionConfigurationVo;
import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;
import com.zlt.aps.factory.domain.vo.ProductConstructionInfoVo;
import com.zlt.aps.factory.scheduling.ProductionContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 搭配排产业务工具类
 *
 * @author ZLT
 * @date 20250830
 */
@Slf4j
public class MatchingProductionUtils {

    /**
     * 对需搭配的SKU信息，采用已经需要排产的计划信息
     *
     * @param enableMatchingProduct 搭配排产需求物料信息
     * @param planRequirement       已经排产的计划信息对象
     */
    public static void setProductionNecessaryInfo(MatchingProductionConfigurationVo enableMatchingProduct, MonthPlanManufacturingRequirementVo planRequirement) {
        if (null == enableMatchingProduct || null == planRequirement) {
            return;
        }
        //施工信息
        enableMatchingProduct.setConstructionCode(planRequirement.getConstructionCode());
        enableMatchingProduct.setConstructionStageType(planRequirement.getConstructionStageType());
        enableMatchingProduct.setConstructionStage(planRequirement.getConstructionStage());
        //生胎代码、规格代号信息
        enableMatchingProduct.setEmbryoCode(planRequirement.getEmbryoCode());
        enableMatchingProduct.setSpecCode(planRequirement.getSpecCode());
        enableMatchingProduct.setSpecCodeInfo(planRequirement.getSpecCodeInfo());
        //硫化时间
        enableMatchingProduct.setCuringTime(planRequirement.getCuringTime());
        //成型法、胎体布层级
        enableMatchingProduct.setMouldMethod(planRequirement.getMouldMethod());
        enableMatchingProduct.setTireFabricNumber(planRequirement.getTireFabricNumber());
        //合模压力
        enableMatchingProduct.setMouldClampingPressure(planRequirement.getMouldClampingPressure());
        enableMatchingProduct.setMoldCavity(planRequirement.getMoldCavity());
    }

    /**
     * 针对新增搭配的SKU信息(即SKU没有在计划中出现)
     *
     * @param productionContext     排产上下文
     * @param enableMatchingProduct 搭配排产需求物料信息
     * @param helper                排产辅助信息
     */
    public static void setProductionNecessaryInfo(ProductionContext productionContext, MatchingProductionConfigurationVo enableMatchingProduct, ProductProductionHelper helper) {
        //硫化时间
        enableMatchingProduct.setCuringTime(getCuringTime(productionContext, enableMatchingProduct.getProductCode()));
        //施工信息
        String constructionCode = helper.getConstructionCode();
        enableMatchingProduct.setConstructionCode(constructionCode);
        ConstructionStageEnum stage = ConstructionStageEnum.matchByConstructionCode(constructionCode);
        enableMatchingProduct.setConstructionStageType(stage);
        if (null != stage) {
            enableMatchingProduct.setConstructionStage(stage.getStage());
        } else {
            enableMatchingProduct.setConstructionStage(null);
        }
        //生胎代码、规格代号信息
        String embryoCode = helper.getEmbryoCode();
        enableMatchingProduct.setSpecCode(helper.getSpecCode());
        enableMatchingProduct.setEmbryoCode(embryoCode);
        enableMatchingProduct.setSpecCodeInfo(helper.getSpecCodeInfo());
        //成型法、胎体布层级数
        enableMatchingProduct.setMouldMethod(helper.getMouldMethod());
        if (StringUtils.isNotBlank(embryoCode)) {
            BaseConstructionVersionInfoVo constructionInfo = productionContext.getBaseConstructionInfoMap().get(embryoCode);
            if (null != constructionInfo && constructionInfo.getLayerLevelNumber() > BigDecimal.ONE.intValue()) {
                enableMatchingProduct.setTireFabricNumber(BigDecimal.ONE.intValue() + BigDecimal.ONE.intValue());
            } else {
                enableMatchingProduct.setTireFabricNumber(BigDecimal.ONE.intValue());
            }
        } else {
            enableMatchingProduct.setTireFabricNumber(BigDecimal.ONE.intValue());
        }
        //合模压力
        enableMatchingProduct.setMouldClampingPressure(helper.getMouldClampingPressure());
        enableMatchingProduct.setMoldCavity(helper.getMoldCavity());
    }

    /**
     * 获取单条硫化时间 = 单条硫化时间 + 间隔硫化时间
     *
     * @param enableMatchingProduct 搭配排产SKU信息
     * @param productionContext     排产上下文
     * @return
     */
    public static BigDecimal getSingleCuringTime(MatchingProductionConfigurationVo enableMatchingProduct, ProductionContext productionContext) {
        BigDecimal singleCuringTime = enableMatchingProduct.getCuringTime();
        //到分
        Integer addCuringTime = (Integer) productionContext.getFactoryParams().get(FactoryConstant.SYS_PARAM_INTERVAL_TIME_OF_EMBRYO_EXCHANGE);
        if (null != addCuringTime) {
            singleCuringTime = singleCuringTime.add(BigDecimal.valueOf(addCuringTime).multiply(BigDecimal.valueOf(ProductionConstant.MINUTE_SECOND)));
        }
        return singleCuringTime;
    }

    /**
     * 根据物料编码，获取对应的硫化时间
     *
     * @param productionContext 排产上下文
     * @param productCode       物料SAP代码
     * @return
     */
    private static BigDecimal getCuringTime(ProductionContext productionContext, String productCode) {
        if (StringUtils.isBlank(productCode)) {
            return null;
        }
        Map<String, ProductConstructionInfoVo> productConstructionMap = productionContext.getConstructionConfigurationMap().get(productCode);
        if (CollectionUtils.isEmpty(productConstructionMap)) {
            return null;
        }
        List<ProductConstructionInfoVo> constructionInfoList = productConstructionMap.values().stream().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(constructionInfoList)) {
            return null;
        }
        //设置硫化时间:不同施工，硫化时间要一致
        ProductConstructionInfoVo constructionInfo = constructionInfoList.get(0);
        if (null == constructionInfo) {
            return null;
        }
        Integer curingTime;
        //冬夏季切换
        if (productionContext.isSummerMonth()) {
            curingTime = constructionInfo.getSummerCuringTime();
        } else {
            curingTime = constructionInfo.getWinterCuringTime();
        }
        if (null != curingTime) {
            return BigDecimal.valueOf(curingTime);
        }
        return null;
    }

    private MatchingProductionUtils() {

    }
}

package com.zlt.aps.factory.utils;

import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.LocationTypeEnum;
import com.tlt.aps.enums.ProductCommonTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.monthplan.api.domain.entity.MdmInterestRate;
import com.zlt.aps.monthplan.api.domain.vo.MaterialInfoGrossRateJsonVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * 物料相关业务方法类
 * 纯计算类
 *
 * @author ZLT
 * @date 20250312
 */
@Slf4j
public class ProductUtils {

    /**
     * 获取单条硫化时间 = 单条硫化时间 + 间隔硫化时间
     *
     * @param productionPlan    计划
     * @param productionContext 排产上下文
     * @return
     */
    public static BigDecimal getSingleCuringTime(MonthPlanManufacturingRequirementVo productionPlan, ProductionContext productionContext) {
        BigDecimal singleCuringTime = productionPlan.getCuringTime();
        //到分
        Integer addCuringTime = (Integer) productionContext.getFactoryParams().get(FactoryConstant.SYS_PARAM_INTERVAL_TIME_OF_EMBRYO_EXCHANGE);
        if (null != addCuringTime) {
            singleCuringTime = singleCuringTime.add(BigDecimal.valueOf(addCuringTime).multiply(BigDecimal.valueOf(ProductionConstant.MINUTE_SECOND)));
        }
        return singleCuringTime;
    }


    /**
     * 续作的销售需求计划--模具预占产能计算使用
     *
     * @param shareMouldRequirePlanList 共用模具计划
     * @return
     */
    public static List<MonthPlanManufacturingRequirementVo> getContinueSaleRequirementPlan(List<MonthPlanManufacturingRequirementVo> shareMouldRequirePlanList) {
        if (CollectionUtils.isEmpty(shareMouldRequirePlanList)) {
            return Collections.emptyList();
        }
        List<MonthPlanManufacturingRequirementVo> continueSaleRequirementPlanList = new ArrayList<>();
        shareMouldRequirePlanList.stream().forEach(continueSaleRequirementPlan -> {
            //非备货的续作计划
            if (YesOrNoEnum.YES.getValue().equals(continueSaleRequirementPlan.getIsContinue()) && YesOrNoEnum.NO.getValue().equals(continueSaleRequirementPlan.getIsStockUp())) {
                continueSaleRequirementPlanList.add(continueSaleRequirementPlan);
            }
        });
        return continueSaleRequirementPlanList;
    }

    /**
     * 设置物料关联的模具
     *
     * @param productRelationMouldMap         物料关联的模具信息 key 物料编码 值 模具号|*|规格代码
     * @param productRelationSpecCodeMouldMap 物料关联的硫化代号模具 key 物料编码 值 = 硫化代号，模具号
     * @param productCode                     物料编码
     * @param mouldCode                       模具号
     * @param specCode                        硫化代号
     */
    public static void setProductRelationMould(Map<String, Set<String>> productRelationMouldMap,
                                               Map<String, Map<String, Set<String>>> productRelationSpecCodeMouldMap,
                                               String productCode,
                                               String mouldCode,
                                               String specCode) {
        if (StringUtils.isBlank(productCode) || StringUtils.isBlank(specCode) || StringUtils.isBlank(mouldCode)) {
            return;
        }
        //物料编码配置的模具 key 物料编码 值=模具号|*|规格代码
        String mouldAndSpec = String.format(ProductionConstant.PRODUCT_PROFIT_KEY_FORMAT, mouldCode, specCode);
        Set<String> mouldAndSpecSet = productRelationMouldMap.get(productCode);
        if (null == mouldAndSpecSet) {
            mouldAndSpecSet = new HashSet<>();
        }
        mouldAndSpecSet.add(mouldAndSpec);
        productRelationMouldMap.put(productCode, mouldAndSpecSet);
        //物料编码配置的模具 key 物料编码 值=规格代号，模具号
        Map<String, Set<String>> specCodeMouldMap = productRelationSpecCodeMouldMap.get(productCode);
        if (null == specCodeMouldMap) {
            specCodeMouldMap = new HashMap<>();
            productRelationSpecCodeMouldMap.put(productCode, specCodeMouldMap);
        }
        //规格代号(硫化)
        Set<String> mouldConfigurationSet = specCodeMouldMap.get(specCode);
        if (null == mouldConfigurationSet) {
            mouldConfigurationSet = new HashSet<>();
            specCodeMouldMap.put(specCode, mouldConfigurationSet);
        }
        mouldConfigurationSet.add(mouldCode);
    }

    /**
     * 根据物料的库位类别毛利率配置及毛利-利润优先值匹配规则，设置物料对应的库位类别的利润优先值设置
     *
     * @param productLocationProfitGradeMap 存储已物料编码|*|库位编码组装的利润优先值集合
     * @param rateList                      物料的库位类别毛利配置
     * @param productCode                   物料编码
     * @param commonType                    物料的公用规格类型
     * @param interestRateList              毛利-利润优先值匹配规则
     */
    public static void setProductLocationProfit(Map<String, Integer> productLocationProfitGradeMap, List<MaterialInfoGrossRateJsonVo> rateList, String productCode, ProductCommonTypeEnum commonType, List<MdmInterestRate> interestRateList) {
        String profitKeyFormat = ProductionConstant.PRODUCT_PROFIT_KEY_FORMAT;
        int defaultProfit = ProductionConstant.DEFAULT_PROFIT;
        //没有配置，默认为0，则转化利润优先等级 = 100-0
        if (CollectionUtils.isEmpty(rateList)) {
            if (ProductCommonTypeEnum.COMMON_TYPE == commonType) {
                productLocationProfitGradeMap.put(String.format(profitKeyFormat, productCode, LocationTypeEnum.DOMESTIC_LOCATION.getValue()), defaultProfit);
                productLocationProfitGradeMap.put(String.format(profitKeyFormat, productCode, LocationTypeEnum.FOREIGN_LOCATION.getValue()), defaultProfit);
                productLocationProfitGradeMap.put(String.format(profitKeyFormat, productCode, LocationTypeEnum.OE_LOCATION.getValue()), defaultProfit);
            } else {
                productLocationProfitGradeMap.put(String.format(profitKeyFormat, productCode, commonType.getLocationType().getValue()), defaultProfit);
            }
            return;
        }
        //有配置，且不是共用规格
        if (ProductCommonTypeEnum.COMMON_TYPE != commonType) {
            MaterialInfoGrossRateJsonVo rate = rateList.get(0);
            ProductCommonTypeEnum configurationType = ProductCommonTypeEnum.getInstance(rate.getCommonType());
            Integer profit = getProfit(interestRateList, rate.getGrossRate(), defaultProfit);
            productLocationProfitGradeMap.put(String.format(profitKeyFormat, productCode, configurationType.getLocationType().getValue()), profit);
            return;
        }
        //有配置，是共用规格
        Set<LocationTypeEnum> locationTypeSet = new HashSet<>();
        rateList.stream().forEach(rate -> {
            ProductCommonTypeEnum configurationType = ProductCommonTypeEnum.getInstance(rate.getCommonType());
            if (configurationType == ProductCommonTypeEnum.COMMON_TYPE) {
                return;
            }
            locationTypeSet.add(configurationType.getLocationType());
            Integer profit = getProfit(interestRateList, rate.getGrossRate(), defaultProfit);
            productLocationProfitGradeMap.put(String.format(profitKeyFormat, productCode, configurationType.getLocationType().getValue()), profit);
        });
        //补充库位没有配置为默认值
        Arrays.stream(LocationTypeEnum.values()).forEach(locationType -> {
            if (!locationTypeSet.contains(locationType)) {
                productLocationProfitGradeMap.put(String.format(profitKeyFormat, productCode, locationType.getValue()), defaultProfit);
            }
        });
    }


    /**
     * 根据毛利率及利润优先等级配置，得到利润优先值
     * 如果毛利率没有匹配到利润优先值，则利润优先值 = defaultprofit
     * 如果毛利率匹配到的利润优先值<=0，则利润优先值 = defaultProfit - 利润优先值
     * 否则利润优先值 = 利润优先值
     *
     * @param interestRateList 利润优先值匹配
     * @param grossRate        毛利率
     * @param defaultProfit    默认利润优先值
     * @return
     */
    private static Integer getProfit(List<MdmInterestRate> interestRateList, BigDecimal grossRate, int defaultProfit) {
        Integer profit = BigDecimal.ZERO.intValue();
        if (null != grossRate) {
            profit = matchProfit(interestRateList, grossRate);
        }
        if (null == profit) {
            profit = defaultProfit;
        }
        if (profit <= 0) {
            profit = defaultProfit - profit;
        }
        return profit;
    }

    /**
     * 根据毛利匹配利润优先级值
     * min <= grossRate < max
     *
     * @param interestRateList
     * @param grossRate
     * @return
     */
    private static Integer matchProfit(List<MdmInterestRate> interestRateList, BigDecimal grossRate) {
        for (MdmInterestRate interestRate : interestRateList) {
            if (grossRate.compareTo(interestRate.getValueMin()) >= 0 && grossRate.compareTo(interestRate.getValueMax()) < 0) {
                return interestRate.getPriority();
            }
        }
        return null;
    }

    private ProductUtils() {

    }
}

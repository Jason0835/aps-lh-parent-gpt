package com.zlt.aps.factory.utils;

import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;
import com.zlt.aps.factory.domain.vo.MouldInfoVO;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.factory.scheduling.ProductionParamConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 排产计划拼模业务工具类
 * 主要用以判断是否可进行拼模的判断
 * 支撑排产分组衔接业务
 *
 * @author ZLT
 * @date 20250807
 */
@Slf4j
public class ProductionPlanAssemblingMouldUtils {
    /**
     * 判断 beforeProductCode与assemblingMouldProductCode能否进行拼模
     * 满足最基本的拼模条件
     * 即合模压力差值
     * 硫化时间差值
     * 模具类型一致
     * 模具型腔一致
     * true 满足拼模基本条件 false 不满足
     *
     * @param productionContext          排产上下文
     * @param beforeProductCode          前规格
     * @param assemblingMouldProductCode 拼模规格
     * @return
     */
    public static boolean isAssemblingMould(ProductionContext productionContext, String beforeProductCode, String assemblingMouldProductCode) {
        if (null == productionContext || StringUtils.isBlank(beforeProductCode) || StringUtils.isBlank(assemblingMouldProductCode)) {
            return false;
        }
        List<MonthPlanManufacturingRequirementVo> allPlanList = new ArrayList<>(productionContext.getMonthPlanInitMap().values());
        MonthPlanManufacturingRequirementVo before = allPlanList.stream().filter(plan -> plan.getProductCode().equals(beforeProductCode)).findFirst().orElse(null);
        MonthPlanManufacturingRequirementVo assemblingMouldPlan = allPlanList.stream().filter(plan -> plan.getProductCode().equals(assemblingMouldProductCode)).findFirst().orElse(null);
        if (null == before || null == assemblingMouldPlan) {
            return false;
        }
        String beforeMoldCavity = before.getMoldCavity();
        String assemblingMouldMoldCavity = assemblingMouldPlan.getMoldCavity();
        if (StringUtils.isBlank(beforeMoldCavity) || StringUtils.isBlank(assemblingMouldMoldCavity)) {
            return false;
        }
        //模具型腔不一致
        if (!beforeMoldCavity.equals(assemblingMouldMoldCavity)) {
            return false;
        }
        BigDecimal curingTime = before.getCuringTime();
        BigDecimal assemblingMouldCuringTime = assemblingMouldPlan.getCuringTime();
        if (null == curingTime || null == assemblingMouldCuringTime) {
            return false;
        }
        BigDecimal mouldClampingPressure = before.getMouldClampingPressure();
        BigDecimal assemblingMouldMouldClampingPressure = assemblingMouldPlan.getMouldClampingPressure();
        if (null == assemblingMouldMouldClampingPressure || null == mouldClampingPressure) {
            return false;
        }
        //获取合模压力、硫化时间差值信息
        ProductionParamConfiguration productionParam = productionContext.getProductionParam();
        //硫化时间差值
        Integer curingTimeDiff = productionParam.getCuringTimeDiff();
        if (null == curingTimeDiff) {
            curingTimeDiff = BigDecimal.ZERO.intValue();
        }
        //硫化时间-差值范围
        Integer diffValue = curingTime.subtract(assemblingMouldCuringTime).setScale(0, RoundingMode.HALF_UP).intValue();
        if (Math.abs(diffValue) > curingTimeDiff) {
            return false;
        }
        //合模压力差值
        Integer mouldClampingPressureDiff = productionParam.getMouldClampingPressureDiff();
        if (null == mouldClampingPressureDiff) {
            mouldClampingPressureDiff = BigDecimal.ZERO.intValue();
        }
        //合模压力-差值范围
        Integer mouldClampingPressureDiffValue = mouldClampingPressure.subtract(assemblingMouldMouldClampingPressure).setScale(0, RoundingMode.HALF_UP).intValue();
        if (Math.abs(mouldClampingPressureDiffValue) > mouldClampingPressureDiff) {
            return false;
        }
        //模具类型一致
        List<MouldInfoVO> beforeMouldList = ProductionPlanUtils.getPlanMaxEnableMouldInfo(beforeProductCode, productionContext);
        List<MouldInfoVO> assemblingMouldList = ProductionPlanUtils.getPlanMaxEnableMouldInfo(assemblingMouldProductCode, productionContext);
        if (CollectionUtils.isEmpty(beforeMouldList) || CollectionUtils.isEmpty(assemblingMouldList)) {
            return false;
        }
        String mouldType = beforeMouldList.get(0).getMouldType();
        String assemblingMouldType = assemblingMouldList.get(0).getMouldType();
        if (StringUtils.isBlank(mouldType) || StringUtils.isBlank(assemblingMouldType)) {
            return false;
        }
        return mouldType.equals(assemblingMouldType);
    }

    private ProductionPlanAssemblingMouldUtils() {

    }
}

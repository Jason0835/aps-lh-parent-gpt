package com.zlt.aps.cx.service.engine;

import com.zlt.aps.common.engine.utils.MonthPlanSurplusCalculator;
import com.zlt.aps.mp.api.domain.entity.MdmMonthSurplus;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 产品状态维度硫化余量读取测试。
 *
 * @author APS Team
 */
public class ProductionCalculatorProductStatusTest {

    /**
     * 验证同一物料不同产品状态分别读取各自硫化余量。
     */
    @Test
    public void shouldReadSurplusByMaterialAndProductStatus() {
        ProductionCalculator calculator = new ProductionCalculator();
        Map<String, MdmMonthSurplus> monthSurplusMap = new HashMap<>();
        monthSurplusMap.put(
                MonthPlanSurplusCalculator.buildMaterialStatusKey("MAT001", "S"),
                this.buildSurplus("MAT001", "S", 120));
        monthSurplusMap.put(
                MonthPlanSurplusCalculator.buildMaterialStatusKey("MAT001", "T"),
                this.buildSurplus("MAT001", "T", 0));

        Assert.assertEquals(120, calculator.getVulcanizingSurplus("MAT001", "S", monthSurplusMap));
        Assert.assertFalse(calculator.isVulcanizeSurplusExhausted("MAT001", "S", monthSurplusMap));
        Assert.assertTrue(calculator.isVulcanizeSurplusExhausted("MAT001", "T", monthSurplusMap));
    }

    /**
     * 构建测试余量。
     *
     * @param materialCode  物料编码
     * @param productStatus 产品状态
     * @param surplusQty    余量
     * @return 测试余量
     */
    private MdmMonthSurplus buildSurplus(String materialCode, String productStatus, int surplusQty) {
        MdmMonthSurplus surplus = new MdmMonthSurplus();
        surplus.setMaterialCode(materialCode);
        surplus.setProductStatus(productStatus);
        surplus.setPlanSurplusQty(BigDecimal.valueOf(surplusQty));
        return surplus;
    }
}

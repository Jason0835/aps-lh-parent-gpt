package com.zlt.aps.monthplan.factory.handler;

import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanProdDetailFinal;
import com.zlt.aps.monthplan.api.domain.entity.MouldingProductionResultHelper;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 调整对象对象辅助类
 *
 * @author ZLT
 * @date 20250320
 */
@Data
public class AdjustInfoHelperVo {
    /**
     * 当前调整日
     */
    private Integer adjustDate;
    /**
     * 当前调整数量
     */
    private Long adjustQty;
    /**
     * 当前调整的规格
     */
    private String productCode;
    /**
     * 模具号
     */
    private String mouldNo;
    /**
     * 模具数量
     */
    private Integer mouldQty;
    /**
     * 月度最大可用模具数量
     */
    private Set<String> maxMouldSet;
    /**
     * 单条硫化时间(包含间隔时间)
     */
    private BigDecimal curingTime;
    /**
     * 单天单模最大硫化时间
     */
    private BigDecimal dayMaxCuringTime;
    /**
     * 换规格损耗时间
     */
    private BigDecimal changeProductConsumeTime;

    /**
     * 需要保持的调整计划
     */
    private Map<String, FactoryMonthPlanProdFinal> saveAdjustPlanMap;
    /**
     * 更新模具排产结果信息
     */
    private List<MouldingProductionResultHelper> saveMouldProductionHelperList;
    /**
     * 更新排产明细信息
     */
    private List<MonthPlanProdDetailFinal> saveProductionDetailFinalList;

    public AdjustInfoHelperVo(String productCode, Integer adjustDate, Long adjustQty, String mouldNo, Integer mouldQty, Set<String> maxMouldSet) {
        this.productCode = productCode;
        this.adjustDate = adjustDate;
        this.adjustQty = adjustQty;
        this.mouldNo = mouldNo;
        this.mouldQty = mouldQty;
        this.maxMouldSet = maxMouldSet;
    }

    /**
     * 单模单天最大硫化量
     *
     * @return
     */
    public Integer getMaxCuringQty() {
        if (null == dayMaxCuringTime || null == curingTime) {
            return BigDecimal.ZERO.intValue();
        }
        return dayMaxCuringTime.divide(curingTime, 0, RoundingMode.DOWN).intValue();
    }
}

package com.zlt.aps.lh.handler;

import com.zlt.aps.common.engine.domain.LhDayPlanAdjustVo;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Sku硫化余量计算
 * 加载的数据
 *
 * @author ZLT
 * @date 20261103
 */
@Data
public class LhSkuSurplusDataInfo implements Serializable {
    /**
     * 计划起始日：最新库存抓取日
     */
    private Date planStartDate;
    /**
     * 所有月计划数据
     */
    private List<FactoryMonthPlanProductionFinalResult> allMonthPlanList;
    /**
     * 所有日硫化调整量数据
     */
    private List<LhDayPlanAdjustVo> allLhDayPlanAdjustList;
    /**
     * 所有完成量数据(包含夜班完成量)
     */
    private Map<String, Integer> dayFinishSumInfo;
    /**
     * 夜班完成量
     */
    private Map<String, BigDecimal> nightFinishInfo;
    /**
     * 所有年-月的上月超欠产数据
     */
    private Map<YearMonth, Integer> monthOverdueQtyMap;

    /**
     * 获取完成量
     *
     * @param plan
     * @return
     */
    public Integer getFinishQty(FactoryMonthPlanProductionFinalResult plan) {
        if (null == plan) {
            return BigDecimal.ZERO.intValue();
        }
        if (CollectionUtils.isEmpty(dayFinishSumInfo)) {
            return BigDecimal.ZERO.intValue();
        }
        String key = plan.getFactoryMaterialStatusKey();
        Integer finishQty = dayFinishSumInfo.get(key);
        if (null == finishQty || finishQty <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        return finishQty;
    }

    /**
     * 获取夜班完成量
     *
     * @param plan
     * @return
     */
    public Integer getNightFinishQty(FactoryMonthPlanProductionFinalResult plan) {
        if (null == plan) {
            return BigDecimal.ZERO.intValue();
        }
        if (CollectionUtils.isEmpty(nightFinishInfo)) {
            return BigDecimal.ZERO.intValue();
        }
        String key = plan.getFactoryMaterialStatusKey();
        BigDecimal nightFinishQty = nightFinishInfo.get(key);
        if (null == nightFinishQty || nightFinishQty.compareTo(BigDecimal.ZERO) <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        return nightFinishQty.intValue();
    }
}

package com.zlt.aps.common.engine.domain;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

/**
 * 年-月的硫化日计划调整需求-汇总。
 *
 * @author ZLT
 * @date 20260817
 */
@Data
public class YearMonthLhDayAdjustVo implements Serializable {

    /**
     * 产品状态
     */
    private String productStatus;

    /**
     * NC物料编码
     */
    private String materialCode;

    /**
     * 物料描述
     */
    private String materialDesc;
    /**
     * 硫化对应年-月的日计划调整量汇总
     */
    private Map<YearMonth, Integer> yearMonthDayLhAdjustQtyMap;

    /**
     * 获取物料+计划类型Key
     *
     * @return
     */
    public String getMaterialStatusKey() {
        String keyFormat = "%s|*|%s";
        String trimmedProductStatus = StringUtils.trimToEmpty(productStatus);
        return String.format(keyFormat, materialCode, trimmedProductStatus);
    }

    /**
     * 获取物料对应的年月日计划调整量
     *
     * @param yearMonth
     * @return
     */
    public Integer getYearMonthDayLhAdjustQty(YearMonth yearMonth) {
        if (null == yearMonth) {
            BigDecimal.ZERO.intValue();
        }
        if (CollectionUtils.isEmpty(yearMonthDayLhAdjustQtyMap)) {
            return BigDecimal.ZERO.intValue();
        }
        Integer monthAdjustQty = yearMonthDayLhAdjustQtyMap.get(yearMonth);
        if (null == monthAdjustQty) {
            return BigDecimal.ZERO.intValue();
        }
        return monthAdjustQty;
    }
}

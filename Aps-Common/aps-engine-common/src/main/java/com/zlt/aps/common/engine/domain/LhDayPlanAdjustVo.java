package com.zlt.aps.common.engine.domain;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * 硫化日计划调整需求。
 *
 * @author ZLT
 * @date 20260817
 */
@Data
public class LhDayPlanAdjustVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号
     */
    private String factoryCode;

    /**
     * 年份
     */
    private Integer year;

    /**
     * 月份
     */
    private Integer month;

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
     * MES物料编码
     */
    private String mesMaterialCode;

    /**
     * 调整序号，仅允许1、2、3
     */
    private Integer adjustCount;

    /**
     * 调整量，可正可负
     */
    private BigDecimal planQty;

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
     * 年月
     *
     * @return
     */
    public YearMonth getYearMonth() {
        return YearMonth.of(year, month);
    }

    /**
     * 计划调整量
     *
     * @return
     */
    public Integer getPlanQtyValue() {
        if (null == planQty) {
            return BigDecimal.ZERO.intValue();
        }
        return planQty.intValue();
    }
}

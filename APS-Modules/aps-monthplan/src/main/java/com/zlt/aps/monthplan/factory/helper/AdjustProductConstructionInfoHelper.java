package com.zlt.aps.monthplan.factory.helper;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 调增通知单，SAP调增辅助类
 *
 * @author ZLT
 * @date 20250607
 */
@Data
public class AdjustProductConstructionInfoHelper implements Serializable {

    /**
     * 单条硫化时间 --加入间隔时间
     */
    private BigDecimal curingTime;
    /**
     * 单天单模最大硫化时间 --单位到秒
     */
    private BigDecimal dayMaxCuringTime;
    /**
     * 施工代号
     */
    private String constructionCode;

    /**
     * 硫化规格信息信息
     */
    private String specCodeInfo;

    /**
     * 获取单幅模具天的最大产能
     *
     * @return
     */
    public Long getMaxSingleMouldQty() {
        if (null == dayMaxCuringTime || null == curingTime) {
            return BigDecimal.ZERO.longValue();
        }
        return dayMaxCuringTime.divide(curingTime, BigDecimal.ZERO.intValue(), RoundingMode.DOWN).longValue();
    }
}

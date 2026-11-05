package com.zlt.aps.common.engine.domain;

import cn.hutool.core.date.DateUtil;
import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Sku硫化余量计算
 * 计划起始日信息对象
 *
 * @author ZLT
 * @date 20260903
 */
@Getter
public class LhMonthStartDayResult implements Serializable {

    private Date planStartDate;

    private Date nextMonthDate;

    private boolean addLastMonthOverdueQty;
    /**
     * 空起始日
     */
    public static final LhMonthStartDayResult EMPTY = new LhMonthStartDayResult(null, null, false);

    /**
     * 构造函数
     *
     * @param planStartDate          计划起始日
     * @param nextMonthDate          下个月定稿的库存抓取日
     * @param addLastMonthOverdueQty 是否包含上个月超欠产
     */
    public LhMonthStartDayResult(Date planStartDate, Date nextMonthDate, boolean addLastMonthOverdueQty) {
        this.planStartDate = planStartDate;
        this.nextMonthDate = nextMonthDate;
        this.addLastMonthOverdueQty = addLastMonthOverdueQty;
    }

    /**
     * 重新置空起始日
     */
    public void resetEmptyPlanStartDate() {
        this.planStartDate = null;
    }

    /**
     * 获取起始天数
     *
     * @return
     */
    public Integer getStartDay() {
        if (null == planStartDate) {
            return BigDecimal.ONE.intValue();
        }
        return DateUtil.dayOfMonth(planStartDate);
    }
}

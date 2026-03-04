package com.zlt.aps.mp.engine.domain.vo;

import com.zlt.aps.enums.YesOrNoEnum;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 工作日历信息
 *
 * @author ZLT
 * 20251212
 */
@Data
public class ProductionDayInfoVo implements Serializable {

    /**
     * 排产日期
     */
    private Date productionDate;

    /**
     * 停产标识 0 停 1 开
     */
    private String dayFlag;
    /**
     * 产能比例 1~100
     */
    private Integer rate;

    /**
     * 判断是否停产日
     * 停产标识 = 0
     * 或是产能比例<=0
     *
     * @return
     */
    public boolean isStopDay() {
        if (YesOrNoEnum.NO.getCode().equals(getDayFlag())) {
            return true;
        }
        return rate <= BigDecimal.ZERO.intValue();
    }
}

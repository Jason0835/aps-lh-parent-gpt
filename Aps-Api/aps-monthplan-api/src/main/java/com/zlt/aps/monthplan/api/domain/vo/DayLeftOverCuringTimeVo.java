package com.zlt.aps.monthplan.api.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 日剩余硫化时间对象
 *
 * @author ZLT
 * @date 20250324
 */
@Data
public class DayLeftOverCuringTimeVo {
    /**
     * 日在月中所处天数
     * 1~31
     */
    private Integer day;
    /**
     * 剩余硫化时间-单位秒
     */
    private BigDecimal leftOverCuringTime;
}

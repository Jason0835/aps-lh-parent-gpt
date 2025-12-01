package com.zlt.aps.monthplan.api.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 成型消耗量Vo
 *
 * @author Chen
 * @since 2025/8/4
 */
@Data
public class CxConsumeVo implements Serializable {

    /**
     * 排程日期
     */
    private String scheduleDate;

    /**
     * 胎面成型消耗量
     */
    private Double treadConsumeQty;

    /**
     * 胎侧、内衬、纤维直裁、纤维压延成型消耗量
     */
    private Double sideWallConsumeQty;

    /**
     * 钢丝斜裁成型消耗量
     */
    private Double fitConsumeQty;

    /**
     * 胎圈成型消耗量
     */
    private Double cxConsumeQty;

    /**
     * 钢丝压延成型消耗量
     */
    private Double gdyyCxConsumeQty;

}

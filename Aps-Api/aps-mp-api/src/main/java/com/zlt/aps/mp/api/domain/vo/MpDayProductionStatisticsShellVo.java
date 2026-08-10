package com.zlt.aps.mp.api.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 日排产统计明细-模壳标准
 *
 * @author ZLT
 * @date 20260210
 */
@Data
public class MpDayProductionStatisticsShellVo implements Serializable {

    /**
     * 模壳标准
     */
    private String mouldShell;

    /**
     * 换活块机台数
     */
    private Integer blockMachines = 0;
}

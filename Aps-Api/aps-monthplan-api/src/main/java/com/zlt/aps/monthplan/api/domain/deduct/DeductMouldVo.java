package com.zlt.aps.monthplan.api.domain.deduct;

import lombok.Data;

import java.time.LocalDate;
/**
 * 降模排产Vo
 * @author Sandy
 * @date 2025/12/24
 */
@Data
public class DeductMouldVo {

    /**
     * SKU编码
     */
    private String materialCode;

    /**
     * 总需求量
     */
    private Integer totalQty;

    /**
     * 剩余未排产量
     */
    private Integer remainingQty;

    /**
     * 分配的机台数量
     */
    private Integer machinesAssigned;

    /**
     * 结构收尾日
     */
    private LocalDate deadline;

    /**
     * 单机台日产量
     */
    private Integer dailyOutputPerMachine;
}

package com.zlt.aps.monthplan.api.domain.deduct;

import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

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
     * 开始排产日
     */
    private Integer startDate;

    /**
     * 结构收尾日
     */
    private Integer deadline;

    /**
     * 单机台日产量
     */
    private Integer dailyOutputPerMachine;

    /**
     * 参数：分配的机台数，默认3
     */
    private Integer paramAssignedMachines = 3;

    /**
     * 参数：临近收尾天数7天，默认7
     */
    private Integer paramNearDeadline7 = 7;

    /**
     * 参数：临近收尾天数7天，降低的台数，默认3台
     */
    private Integer paramReduceMachines3 = 3;

    /**
     * 参数：临近收尾天数5天，默认5
     */
    private Integer paramNearDeadline5 = 5;

    /**
     * 参数：临近收尾天数5天，降低的台数，默认2台
     */
    private Integer paramReduceMachines2 = 2;

    /**
     * 参数：临近收尾天数2天，默认2
     */
    private Integer paramNearDeadline2 = 2;

    /**
     * 参数：临近收尾天数2天，降低的台数，默认1台
     */
    private Integer paramReduceMachines1 = 1;

    /**
     * 参数：开产日比例
     */
    private double paramStartDayRatio = 0.5;

    /**
     * 停工日集合
     */
    private Set<Integer> shutDownDaySet;

    /**
     * 开产日集合
     */
    private Set<Integer> productionStartDaySet;
}

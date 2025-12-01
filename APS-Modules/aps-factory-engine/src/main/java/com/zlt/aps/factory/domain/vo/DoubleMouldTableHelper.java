package com.zlt.aps.factory.domain.vo;

import lombok.Getter;

import java.io.Serializable;
/**
 * 排产分组选中双模台各自起始-结束日对象
 *
 * @author ZLT
 * @date 20250718
 */
@Getter
public class DoubleMouldTableHelper implements Serializable {
    /**
     * 第一个模台的起始日
     */
    private Integer firstStartDate;
    /**
     * 第一个模台的结束日
     */
    private Integer firstEndDate;
    /**
     * 第二个模台的起始日
     */
    private Integer secondStartDate;
    /**
     * 第二个模台的结束日
     */
    private Integer secondEndDate;

    /**
     * 构建排产分组-起始日期不一致的双模台起始日期对象
     *
     * @param firstStartDate 第一个模台的起始日
     * @param firstEndDate 第一个模台的结束日
     * @param secondStartDate 第二个模台的起始日
     * @param secondEndDate 第二个模台的结束日
     */
    public DoubleMouldTableHelper(Integer firstStartDate, Integer firstEndDate, Integer secondStartDate, Integer secondEndDate) {
        this.firstStartDate = firstStartDate;
        this.firstEndDate = firstEndDate;
        this.secondStartDate = secondStartDate;
        this.secondEndDate = secondEndDate;
    }
}

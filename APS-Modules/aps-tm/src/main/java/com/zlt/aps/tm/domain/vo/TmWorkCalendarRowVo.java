package com.zlt.aps.tm.domain.vo;

import lombok.Data;

/**
 * 胎面自动排程工作日历查询行对象。
 *
 * <p>用于承接工厂工序生产日历结果，供停产需求重分配判断使用。</p>
 */
@Data
public class TmWorkCalendarRowVo {

    /** 工作日标识，1表示工作日，0表示停产日 */
    private String dayFlag;

    /** 一班开班标识，1表示开班，0表示停班 */
    private String oneShiftFlag;

    /** 二班开班标识，1表示开班，0表示停班 */
    private String twoShiftFlag;

    /** 三班开班标识，1表示开班，0表示停班 */
    private String threeShiftFlag;
}

package com.zlt.aps.mp.engine.domain.vo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * 月周期排产对象
 *
 * @author ZLT
 * @date 20260403
 */
@Data
@Slf4j
public class MonthCycleGroupInfoVo implements Serializable {
    /**
     * 年份
     */
    private Integer year;
    /**
     * 月份
     */
    private Integer month;
    /**
     * 分组名称
     * TBR 结构
     */
    private String groupName;

}

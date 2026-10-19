package com.zlt.aps.lh.api.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 换胶囊造成的机台不可生产时间窗口。
 *
 * <p>该对象只承载一次排程内的运行态时间占用，不落库、不参与精度保养、维修或清洗的
 * 业务回填。机台产能、开产时间和完工时间统一将该窗口与既有不可生产区间合并计算。</p>
 *
 * @author APS
 */
@Data
public class CapsuleReplacementTimeWindowDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 归一化后的物理机台编号，单控 L/R 共用同一窗口 */
    private String physicalMachineCode;
    /** 触发换胶囊的物料编码，用于过程日志和问题追溯 */
    private String materialCode;
    /** 触发换胶囊的班次序号 */
    private Integer shiftIndex;
    /** 换胶囊开始时间 */
    private Date replacementStartTime;
    /** 换胶囊结束并可恢复生产的时间 */
    private Date replacementEndTime;
}

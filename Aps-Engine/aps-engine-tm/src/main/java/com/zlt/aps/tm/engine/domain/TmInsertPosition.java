package com.zlt.aps.tm.engine.domain;

import lombok.Data;

/**
 * 胎面人工插单位置对象。
 *
 * <p>用于描述目标机台、班次顺序和锚点任务。骨架阶段不实现“第二个在产规格”限制，
 * 只保留参数结构，待业务口径确认后补充校验。</p>
 */
@Data
public class TmInsertPosition {

    /** 目标机台编码 */
    private String machineCode;

    /** 目标班次顺序 */
    private Integer shiftOrder;

    /** 锚点任务标识 */
    private String anchorTaskId;
}

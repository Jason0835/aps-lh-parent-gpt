package com.zlt.aps.tc.engine.domain;

import lombok.Data;

/**
 * 胎侧机台前置任务快照。
 *
 * <p>用于在当前排程日一班开始前记录同机台上一排程日的链尾任务，也用于机台分配时
 * 将上一班或当前班已排任务转换为统一的连续性评分输入。</p>
 */
@Data
public class TcTaskPredecessor {

    /** 机台编码 */
    private String machineCode;

    /** 胎侧编码 */
    private String sidewallCode;

    /** 主胶料编码 */
    private String glueCode;

    /** 基部胶或整条胶料组合编码 */
    private String baseGlueCode;

    /** 口型板编码 */
    private String mouthPlateCode;

    /** 来源班次序号 */
    private Integer shiftOrder;

    /** 来源班次内顺序 */
    private Integer sequence;

    /** 来源任务业务键 */
    private String businessKey;
}

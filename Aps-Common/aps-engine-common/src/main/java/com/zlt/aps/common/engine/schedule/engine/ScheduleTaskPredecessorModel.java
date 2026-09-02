package com.zlt.aps.common.engine.schedule.engine;
import lombok.Data;
/** TM/TC 任务链前置任务公共运行态模型。 */
@Data
public class ScheduleTaskPredecessorModel {
    /** 工艺物料编码，TM 对应胎面编码，TC 对应胎侧编码。 */
    protected String processCode;

    /** 机台编码。 */
    protected String machineCode;

    /** 胶料编码。 */
    protected String glueCode;

    /** 基础胶编码。 */
    protected String baseGlueCode;

    /** 口型板编码。 */
    protected String mouthPlateCode;

    /** 班次顺序。 */
    protected Integer shiftOrder;

    /** 任务顺序。 */
    protected Integer sequence;

    /** 业务唯一键。 */
    protected String businessKey;
}

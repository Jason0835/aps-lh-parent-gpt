package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

/**
 * TM/TC 自动排程内部异常运行态模型。
 *
 * <p>产品字段在引擎内部统一为 processCode，接口返回时由领域收集器映射为 treadCode 或
 * sidewallCode，避免公共模型反向依赖产品 API。</p>
 */
@Data
public class ScheduleIssueModel {

    /** 异常级别。 */
    private String level;
    /** 排程阶段编码。 */
    private String stageCode;
    /** 排程阶段名称。 */
    private String stageName;
    /** 异常类别编码。 */
    private String category;
    /** 来源工单号。 */
    private String sourceOrderNo;
    /** 胎胚编码。 */
    private String embryoCode;
    /** 统一产品工序编码。 */
    private String processCode;
    /** 示方书编号。 */
    private String recipeNo;
    /** 班次顺序。 */
    private Integer shiftOrder;
    /** 发生异常的字段。 */
    private String fieldName;
    /** 异常说明。 */
    private String message;
}

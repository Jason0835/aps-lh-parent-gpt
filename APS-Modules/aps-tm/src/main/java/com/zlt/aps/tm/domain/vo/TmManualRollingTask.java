package com.zlt.aps.tm.domain.vo;

import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎面人工插单滚动任务。
 *
 * <p>用于把 T_TM_SCHEDULE_RESULT 横向班次字段拆成任务级数据，便于按同机台、
 * 同排程日期、插单班次到第 6 班的局部窗口执行顺序重排和产能滚动。</p>
 */
@Data
public class TmManualRollingTask {

    /** 原排程结果ID，新插单或顺延任务为空 */
    private Long resultId;

    /** 原排程结果对象，新插单或顺延任务为空 */
    private TmScheduleResult sourceResult;

    /** 用于新建插单或顺延结果的模板数据 */
    private TmScheduleResult templateResult;

    /** 当前滚动处理班次 */
    private Integer shiftOrder;

    /** 当前班次内顺序 */
    private Integer sequence;

    /** 需要参与滚动分配的计划量 */
    private BigDecimal planQty;

    /** 当前班次已完成量，滚动减量不能低于该值 */
    private BigDecimal finishQty;

    /** 机台编码 */
    private String machineCode;

    /** 胎面编码 */
    private String treadCode;

    /** 主胶料编码 */
    private String glueCode;

    /** 口型板编码 */
    private String mouthPlateCode;

    /** 数据来源 */
    private String dataSource;

    /** 是否人工插单任务 */
    private boolean insertTask;

    /** 是否跨班顺延新任务 */
    private boolean carryoverTask;
}

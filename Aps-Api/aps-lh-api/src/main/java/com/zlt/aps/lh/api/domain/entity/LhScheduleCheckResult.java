package com.zlt.aps.lh.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 硫化排程结果校验记录实体类（T_LH_SCHEDULE_CHECK_RESULT）。
 *
 * <p>一条记录代表某工厂某业务排程日下、某个检查项发现的某一处异常。
 * 校验执行时按「工厂 + 业务排程日」物理删除旧记录后全量重跑落库，保证幂等。</p>
 *
 * @author APS
 * @date 2026/10/27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "T_LH_SCHEDULE_CHECK_RESULT")
@ApiModel(value = "硫化排程校验结果", description = "硫化排程结果合理性校验记录")
public class LhScheduleCheckResult extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 检查级别：强控（需用户确认后才允许继续发布/导出等操作） */
    public static final String CHECK_LEVEL_HARD = "1";

    /** 检查级别：提示（仅展示，不阻断） */
    public static final String CHECK_LEVEL_TIP = "2";

    /** 触发类型：排程完成自动触发 */
    public static final String TRIGGER_TYPE_SCHEDULE = "1";

    /** 触发类型：数据导入后触发 */
    public static final String TRIGGER_TYPE_IMPORT = "2";

    /** 触发类型：用户手动重新校验 */
    public static final String TRIGGER_TYPE_MANUAL = "3";

    /** 确认状态：未确认 */
    public static final String ACKNOWLEDGED_NO = "0";

    /** 确认状态：已了解 */
    public static final String ACKNOWLEDGED_YES = "1";

    /**
     * 工厂编号
     */
    @ApiModelProperty(value = "工厂编号")
    private String factoryCode;

    /**
     * 排程日期（与排程结果行落库的 SCHEDULE_DATE 一致，即页面排程目标日）
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期")
    private Date scheduleDate;

    /**
     * 检查项编码（IDLE_SHIFT/ENDING_SHIFT_GAP/ENDING_STOCK/MOULD_SHELL/CAPSULE_CHUCK）
     */
    @ApiModelProperty(value = "检查项编码")
    private String checkCode;

    /**
     * 检查项名称的多语言 key（读取报告时按当前语言解析）
     */
    @ApiModelProperty(value = "检查项名称多语言key")
    private String checkName;

    /**
     * 检查级别（1-强控 2-提示）
     */
    @ApiModelProperty(value = "检查级别")
    private String checkLevel;

    /**
     * 维度键（机台编码 / 胎胚编码 / 模壳标准 / 卡盘规格等）
     */
    @ApiModelProperty(value = "维度键")
    private String dimensionKey;

    /**
     * 异常描述（写入时已按当前语言国际化）
     */
    @ApiModelProperty(value = "异常描述")
    private String message;

    /**
     * 异常明细JSON
     */
    @ApiModelProperty(value = "异常明细JSON")
    private String detailJson;

    /**
     * 触发类型（1-排程 2-导入 3-手动）
     */
    @ApiModelProperty(value = "触发类型")
    private String triggerType;

    /**
     * 触发时的排程批次号（导入数据可能为空，仅作追溯字段，不参与校验键）
     */
    @ApiModelProperty(value = "排程批次号")
    private String batchNo;

    /**
     * 确认状态（0-未确认 1-已了解）
     */
    @ApiModelProperty(value = "确认状态")
    private String acknowledged;

    /**
     * 确认人
     */
    @ApiModelProperty(value = "确认人")
    private String acknowledgeBy;

    /**
     * 确认时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "确认时间")
    private Date acknowledgeTime;

    /**
     * 校验时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "校验时间")
    private Date checkTime;
}

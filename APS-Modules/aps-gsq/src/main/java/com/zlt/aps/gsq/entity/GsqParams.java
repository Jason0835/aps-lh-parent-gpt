package com.zlt.aps.gsq.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * <p>
 * 钢丝圈参数信息
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-06-02
 */
@Data
@TableName("T_GSQ_PARAMS")
@ApiModel(value = "GsqParams对象", description = "钢丝圈参数信息")
//@KeySequence(value = "SEQ_PUBLIC",dbType = DbType.ORACLE)
public class GsqParams extends ApsBaseEntity {

    private static final long serialVersionUID = 1110056585174675868L;

    // ==================== 滚动更新相关参数代码常量 ====================

    /** 参数代码：自动触发提前分钟数（班次开始前N分钟触发滚动更新） */
    public static final String PARAM_CODE_ROLLING_LEAD_MINUTES = "GSQ_ROLLING_AUTO_TRIGGER_LEAD_MINUTES";

    /** 参数代码：3班库存阈值（用于库存积压判断的班次数） */
    public static final String PARAM_CODE_STOCK_THRESHOLD_CLASSES = "GSQ_ROLLING_STOCK_THRESHOLD_CLASSES";

    /** 参数代码：自动滚动开关（1-启用，0-关闭） */
    public static final String PARAM_CODE_AUTO_ROLLING_ENABLED = "GSQ_AUTO_ROLLING_ENABLED";

    /** 参数代码：自动滚动提前窗口（分钟），早于班次开始多少分钟开始进入触发窗口 */
    public static final String PARAM_CODE_ROLLING_EARLY_MINUTES = "GSQ_ROLLING_EARLY_MINUTES";

    /** 参数代码：自动滚动延后窗口（分钟），晚于班次开始多少分钟结束触发窗口 */
    public static final String PARAM_CODE_ROLLING_LATE_MINUTES = "GSQ_ROLLING_LATE_MINUTES";

    /** 参数代码：自动滚动输入稳定时间（分钟），库存快照需保持稳定N分钟才允许触发 */
    public static final String PARAM_CODE_ROLLING_STABLE_MINUTES = "GSQ_ROLLING_STABLE_MINUTES";

    /** 默认值：自动触发提前分钟数 */
    public static final int DEFAULT_LEAD_MINUTES = 30;

    /** 默认值：3班库存阈值班次数 */
    public static final int DEFAULT_THRESHOLD_CLASSES = 3;

    /** 默认值：自动滚动开关（关闭） */
    public static final String DEFAULT_AUTO_ROLLING_ENABLED = "0";

    /** 默认值：自动滚动提前窗口（30分钟） */
    public static final String DEFAULT_ROLLING_EARLY_MINUTES = "30";

    /** 默认值：自动滚动延后窗口（15分钟） */
    public static final String DEFAULT_ROLLING_LATE_MINUTES = "15";

    /** 默认值：自动滚动输入稳定时间（5分钟） */
    public static final String DEFAULT_ROLLING_STABLE_MINUTES = "5";

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "分厂编号")
    @TableField("FACTORY_CODE")
    private String factoryCode;

    @ApiModelProperty(value = "参数code")
    @TableField("PARAM_CODE")
    private String paramCode;

    @ApiModelProperty(value = "参数名称")
    @TableField("PARAM_NAME")
    private String paramName;

    @ApiModelProperty(value = "参数值")
    @TableField("PARAM_VALUE")
    private String paramValue;

    @ApiModelProperty(value = "参数默认值")
    @TableField("DEFAULT_VALUE")
    private String defaultValue;

    @ApiModelProperty(value = "启用状态：1-启用，0-停用")
    @TableField("ENABLE_STATUS")
    private String enableStatus;

    @ApiModelProperty(value = "参数值对应的正则表达式")
    @TableField("REGULAR_EXPRESSION")
    private String regularExpression;

    @ApiModelProperty(value = "参数值根据正则表达式校验是失败后的错误提示")
    @TableField("ERROR_TIPS")
    private String errorTips;
}

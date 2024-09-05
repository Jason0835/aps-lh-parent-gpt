package com.zlt.aps.cx.engine.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;

/**
 * 成型自动排程记录对象 t_cx_auto_schedule_record
 * 
 * @author Joran.zhang
 * @date 2021-07-14
 */
@Data
@ApiModel(value = "成型自动排程记录对象", description = "成型自动排程记录对象 ")
public class CxEngineAutoScheduleRecord extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 生产排程记录主计划版本号,年+月+日+01，02 */
    @ApiModelProperty(value = "生产排程记录主计划版本号,年+月+日+01，02")
    private String monthPlanApsVersion;

    /** 成型自动排程批次号信息，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号 */
    @ApiModelProperty(value = "成型自动排程批次号信息，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号")
    private String cxBatchNo;

    /** 状态：0-成功；1-失败 */
    @ApiModelProperty(value = "状态：0-成功；1-失败")
    private String status;

    /** 排程时间 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "排程时间")
    private Date scheduleDate;

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("monthPlanApsVersion", getMonthPlanApsVersion())
            .append("cxBatchNo", getCxBatchNo())
            .append("status", getStatus())
            .append("scheduleDate", getScheduleDate())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("delFlag", getDelFlag())
            .append("remark", getRemark())
            .toString();
    }

}

package com.zlt.aps.common.core.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;

/**
 * 排程结果发布记录对象 t_schedule_publish_record
 * 
 * @author chen
 * @date 2021-08-04
 */
@ApiModel(value = "排程结果发布记录对象", description = "排程结果发布记录对象 ")
@Data
public class SchedulePublishRecord extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "id")
    private Long id;

    /** 工序数据维护在数据字典：0-成型；1硫化等 */
    @Excel(name = "ui.data.column.record.procedureCode")
    @ApiModelProperty(value = "工序数据维护在数据字典：0-成型；1硫化等")
    private String procedureCode;

    /** 发布状态：0-未发布；1-已发布 */
    @Excel(name = "ui.data.column.record.publishStatus")
    @ApiModelProperty(value = "发布状态：0-未发布；1-已发布")
    private String publishStatus;

    /** 排程时间 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.data.column.record.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程时间")
    private Date scheduleDate;

    /** 删除标识（0未删除；1已删除） */
    @ApiModelProperty(value = "删除标识")
    private String delFlag;

    @ApiModelProperty(value = "单次发布对应的数据版本信息")
    private String dataVersion;

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("procedureCode", getProcedureCode())
            .append("publishStatus", getPublishStatus())
            .append("scheduleDate", getScheduleDate())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("delFlag", getDelFlag())
            .append("dataVersion", getDataVersion())
            .append("remark", getRemark())
            .toString();
    }
}

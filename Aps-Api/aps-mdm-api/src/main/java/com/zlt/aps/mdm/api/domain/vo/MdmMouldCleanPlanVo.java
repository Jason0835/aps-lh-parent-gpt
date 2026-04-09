package com.zlt.aps.mdm.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * APS模具清洗计划查询VO
 *
 * @author APS Team
 */
@Data
@ApiModel(value = "APS模具清洗计划查询VO", description = "APS模具清洗计划查询条件")
public class MdmMouldCleanPlanVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "厂别")
    private String factoryCode;

    @ApiModelProperty(value = "硫化机台")
    private String lhCode;

    @ApiModelProperty(value = "清洗类型：01-干冰清洗，02-喷砂清洗")
    private String cleanType;

    @ApiModelProperty(value = "数据来源：0-手工录入，1-系统生成")
    private String dataSource;

    @ApiModelProperty(value = "清洗日期-开始")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date cleanTimeBegin;

    @ApiModelProperty(value = "清洗日期-结束")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date cleanTimeEnd;
}

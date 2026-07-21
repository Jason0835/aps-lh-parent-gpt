package com.zlt.aps.tc.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 胎侧排程看板查询条件。
 */
@Data
@ApiModel(value = "胎侧排程看板查询条件")
public class TcScheduleBoardQueryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编码。 */
    @ApiModelProperty(value = "工厂编码", required = true)
    private String factoryCode;

    /** 查询开始日期。 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "查询开始日期", required = true)
    private Date startDate;

    /** 查询结束日期。 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "查询结束日期", required = true)
    private Date endDate;

    /** 机台编码。 */
    @ApiModelProperty(value = "机台编码")
    private String machineCode;

    /** 胎侧编码。 */
    @ApiModelProperty(value = "胎侧编码")
    private String sidewallCode;

    /** 主胶料编码。 */
    @ApiModelProperty(value = "主胶料编码")
    private String glueCode;

    /** 口型板编码。 */
    @ApiModelProperty(value = "口型板编码")
    private String mouthPlateCode;

    /** 发布状态。 */
    @ApiModelProperty(value = "发布状态")
    private String releaseStatus;

    /** 分配状态：ASSIGNED-已排，UNPLANNED-未排。 */
    @ApiModelProperty(value = "分配状态")
    private String assignStatus;

    /** 当前页码。 */
    @ApiModelProperty(value = "当前页码", example = "1")
    private Integer pageNum = 1;

    /** 每页条数。 */
    @ApiModelProperty(value = "每页条数", example = "20")
    private Integer pageSize = 20;
}

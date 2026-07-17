package com.zlt.aps.tc.api.domain.vo;

import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 胎侧排程看板分页结果。
 */
@Data
@ApiModel(value = "胎侧排程看板分页结果")
public class TcScheduleBoardPageVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 排程结果行。 */
    @ApiModelProperty(value = "排程结果行")
    private List<TcScheduleResult> rows = new ArrayList<>();

    /** 总记录数。 */
    @ApiModelProperty(value = "总记录数")
    private Long total = 0L;

    /** 当前页码。 */
    @ApiModelProperty(value = "当前页码")
    private Integer pageNum = 1;

    /** 每页条数。 */
    @ApiModelProperty(value = "每页条数")
    private Integer pageSize = 20;
}

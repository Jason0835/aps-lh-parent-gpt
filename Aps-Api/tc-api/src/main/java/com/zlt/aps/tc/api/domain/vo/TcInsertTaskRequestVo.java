package com.zlt.aps.tc.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 胎侧人工插单请求。
 */
@Data
@ApiModel(value = "胎侧人工插单请求")
public class TcInsertTaskRequestVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编码。 */
    @ApiModelProperty(value = "工厂编码", required = true)
    private String factoryCode;

    /** 排程日期。 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "排程日期", required = true)
    private Date scheduleDate;

    /** 目标机台编码。 */
    @ApiModelProperty(value = "目标机台编码", required = true)
    private String machineCode;

    /** 胎侧编码。 */
    @ApiModelProperty(value = "胎侧编码", required = true)
    private String sidewallCode;

    /** 多班次计划量与顺序。 */
    @ApiModelProperty(value = "多班次计划量与顺序", required = true)
    private List<TcManualShiftItemVo> shiftList = new ArrayList<>();

    /** 插单备注，同时作为人工操作日志原因。 */
    @ApiModelProperty(value = "插单备注")
    private String remark;
}

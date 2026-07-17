package com.zlt.aps.tc.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 胎侧人工插单和转机台选项集合。
 */
@Data
@ApiModel(value = "胎侧人工操作选项集合")
public class TcManualOptionsVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 有效胎侧施工选项。 */
    @ApiModelProperty(value = "有效胎侧施工选项")
    private List<TcManualConstructionOptionVo> constructionList = new ArrayList<>();

    /** 有效机台选项。 */
    @ApiModelProperty(value = "有效机台选项")
    private List<TcManualMachineOptionVo> machineList = new ArrayList<>();

    /** 六班配置。 */
    @ApiModelProperty(value = "六班配置")
    private List<TcManualShiftOptionVo> shiftList = new ArrayList<>();
}

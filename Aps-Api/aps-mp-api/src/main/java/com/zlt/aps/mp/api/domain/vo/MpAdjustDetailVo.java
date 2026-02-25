package com.zlt.aps.mp.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureIn;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


/**
 * 调整-调整明细实体VO类
 * @author wengpc
 */
@Data
public class MpAdjustDetailVo extends MpAdjustStructureIn {

    @ApiModelProperty(value = "计划剩余排产量")
    @TableField(exist = false)
    private Integer monthUnScheduledQty;

    @ApiModelProperty(value = "订单量")
    @TableField(exist = false)
    private Integer ordQty;

    @ApiModelProperty(value = "是否试制量试")
    @TableField(exist = false)
    private String isTrial;

    @ApiModelProperty(value = "胎胚号")
    @TableField(exist = false)
    private String embryoCode;

    @ApiModelProperty(value = "调整明细来源：01-销售订单池 02-试制量试 03-月度生产计划")
    @TableField(exist = false)
    private String adjustItemSource;

}
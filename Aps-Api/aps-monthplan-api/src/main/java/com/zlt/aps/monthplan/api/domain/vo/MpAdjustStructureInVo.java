package com.zlt.aps.monthplan.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustStructureIn;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


/**
 * 调整-结构内调整记录实体VO类
 * @author wengpc
 */
@Data
public class MpAdjustStructureInVo extends MpAdjustStructureIn {

    @ApiModelProperty(value = "计划剩余排产量", name = "monthUnScheduledQty")
    @TableField(exist = false)
    private Integer monthUnScheduledQty;

    @ApiModelProperty(value = "订单量", name = "ordQty")
    @TableField(exist = false)
    private Integer ordQty;

}
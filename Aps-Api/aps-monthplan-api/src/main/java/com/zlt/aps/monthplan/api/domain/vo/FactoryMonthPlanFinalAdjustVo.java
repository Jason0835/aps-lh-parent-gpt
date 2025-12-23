package com.zlt.aps.monthplan.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 月计划定稿表For调整
 * @author Sandy
 * @date 2025-12-22
 */
@Data
public class FactoryMonthPlanFinalAdjustVo extends FactoryMonthPlanProdFinal {

    /**
     * 锁定量
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "锁定量", name = "lockQty")
    private Integer lockQty;
}

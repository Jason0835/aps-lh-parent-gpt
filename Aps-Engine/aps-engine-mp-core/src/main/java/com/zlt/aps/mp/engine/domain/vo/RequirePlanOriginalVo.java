package com.zlt.aps.mp.engine.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * 需求计划原始数据对象
 *
 * @author ZLT
 * @date 20251209
 */
@Slf4j
@Data
public class RequirePlanOriginalVo implements Serializable {

    /**
     * 高优先级
     */
    private Integer heightQty;
    /**
     * 中优先级
     */
    private Integer midQty;
    /**
     * 暂缓订单
     */
    private Integer postponeQty;
    /**
     * 周期排产储备
     */
    private Integer cycleReserveQty;
    /**
     * 常规储备
     */
    private Integer conventionReserveQty;
    /**
     * 高优先级
     */
    private Integer heightProductionQty;
    /**
     * 排产净需求(含损耗，排除高优先级损耗)
     */
    private Integer productionQty;
}

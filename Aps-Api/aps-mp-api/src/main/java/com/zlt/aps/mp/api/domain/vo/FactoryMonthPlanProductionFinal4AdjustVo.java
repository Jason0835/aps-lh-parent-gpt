package com.zlt.aps.mp.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryMonthPlanProductionFinalResult.java
 * 描    述：工厂月生产计划-最终排产计划定稿对象 t_mp_month_plan_prod_final
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "T_MP_MONTH_PLAN_PROD_FINAL")
@ApiModel(value = "工厂月生产计划-最终排产计划定稿对象", description = "工厂月生产计划-最终排产计划定稿对象")
public class FactoryMonthPlanProductionFinal4AdjustVo extends FactoryMonthPlanProductionFinalResult {

}

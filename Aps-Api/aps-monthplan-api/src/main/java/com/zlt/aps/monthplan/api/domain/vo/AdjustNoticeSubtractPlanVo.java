package com.zlt.aps.monthplan.api.domain.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 计划调整--调整建议信息对象
 *
 * @author ZLT
 * @date 20250528
 */
@Data
@ApiModel(value = "调整通知单调整建议信息对象", description = "调整通知单调整建议信息对象")
public class AdjustNoticeSubtractPlanVo implements Serializable {
    /**
     * 推荐的调整计划列表
     */
    private List<FactoryMonthFinalPlanHelperVo> subtractPlanList;

    /**
     * 模具剩余总产能--调增时有值
     */
    private Long leftOverQty;
}

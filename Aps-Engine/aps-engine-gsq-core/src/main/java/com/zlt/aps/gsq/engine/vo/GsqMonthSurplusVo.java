package com.zlt.aps.gsq.engine.vo;

import lombok.Data;

/**
 * 钢丝圈月度汇总VO
 *
 * <p>从 t_mdm_month_surplus 查询 PLAN_SURPLUS_QTY × 2（胎胚余量转钢丝圈余量，与胎圈口径一致）；</p>
 * <p>关联施工信息表 T_MDM_CONSTRUCTION_INFO，通过 MATERIAL_CODE 取 BEAD_CODE 作为钢丝圈代码（steelRingCode）。</p>
 */
@Data
public class GsqMonthSurplusVo {

    /**
     * 钢丝圈代码（BEAD_CODE）
     */
    private String steelRingCode;

    /**
     * 月度计划完成量
     */
    private Double monthFinishQty;

    /**
     * 月度剩余量（从 t_mdm_month_surplus 的 PLAN_SURPLUS_QTY × 2 取值，胎胚余量转为钢丝圈余量）
     */
    private Double monthRemainQty;
}

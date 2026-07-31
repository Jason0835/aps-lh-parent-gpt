package com.zlt.aps.tq.engine.vo;

import lombok.Data;

/**
 * 胎圈月度汇总VO
 *
 * <p>monthRemainQty（月计划剩余量）从 t_mdm_month_surplus 查询 PLAN_SURPLUS_QTY × 2（胎胚余量转胎圈余量）；</p>
 * <p>关联施工信息表 T_MDM_CONSTRUCTION_INFO，通过 MATERIAL_CODE 取 TIRE_RING_CODE 作为胎圈编码；</p>
 * <p>monthFinishQty（月计划完成量）不再从 t_mp_month_plan_prod_final 取，
 * 改为 MES 回报后通过 t_tq_sche_finish_qty 回填，自动排程阶段默认为 0。</p>
 */
@Data
public class TqMonthSurplusVo {

    /**
     * 胎圈代码
     */
    private String beadCode;

    /**
     * 月度计划完成量（MES回报后由t_tq_sche_finish_qty回填，自动排程阶段默认为0）
     */
    private Double monthFinishQty = 0D;

    /**
     * 月度剩余量（从t_mdm_month_surplus的PLAN_SURPLUS_QTY × 2取值，胎胚余量转为胎圈余量）
     */
    private Double monthRemainQty;
}

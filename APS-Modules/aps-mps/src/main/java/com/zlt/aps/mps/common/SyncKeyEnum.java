package com.zlt.aps.mps.common;

/**
 * @author Gim
 */
public enum SyncKeyEnum {
    UNKNOWN("UNKNOWN"),
    EMBRYO_STOCK_SYNC("EMBRYO_STOCK_SYNC"),// 胎胚库存
    EMBRYO_MONTH_SYNC("EMBRYO_MONTH_SYNC"),// 胎胚月结库存
    FINISHED_STOCK_SYNC("FINISHED_STOCK_SYNC"),// 成品库存
    TREAD_STOCK("TREAD_STOCK"),// 胎面库存
    SIDEWALL_STOCK("SIDEWALL_STOCK"),// 胎侧库存
    LINING_STOCK("LINING_STOCK"),// 内衬库存
    BEAD_STOCK("BEAD_STOCK"),// 胎圈库存
    STEEL_WIRE_STOCK("STEEL_WIRE_STOCK"),// 钢丝圈库存
    ADJUDI15_STOCK("ADJUDI15_STOCK"),// CD15库存
    ADJUDI15_LINESIDE_STOCK("ADJUDI15_LINESIDE_STOCK"),// CD15线边库库存
    ADJUDI90_STOCK("ADJUDI90_STOCK"),// CD90库存
    ADJUDI90_LINESIDE_STOCK("ADJUDI90_LINESIDE_STOCK"),// CD90线边库库存
    GDYY_STOCK("GDYY_STOCK"),// 钢带压延库存
    XWYY_STOCK("XWYY_STOCK"),// 纤维压延库存
    BOM_INFO_SYNC("BOM_INFO_SYNC"),// BOM信息同步
    FINISH_SCHE_COMPLETE("FINISH_SCHE_COMPLETE"),// 成型排程完成量回报
    HALF_PART_SAP("HALF_PART_SAP"),// 半部件代号与SAP物料品号对应关系
    VULCANIZE_SCHE_COMPLETE("VULCANIZE_SCHE_COMPLETE"),// 硫化排程完成量回报
    FORMING8_12_COMPLETE("FORMING8_12_COMPLETE"),// 成型8-12点的完成量
    TREAD_COMPLETE_QUANTITY("TREAD_COMPLETE_QUANTITY"),// 胎面完成量回报
    SIDEWALL_COMPLETE_QUANTITY("SIDEWALL_COMPLETE_QUANTITY"),// 胎侧完成量回报
    LINING_COMPLETE_QUANTITY("LINING_COMPLETE_QUANTITY"),// 内衬完成量回报
    ADJUDI15_COMPLETE_QUANTITY("ADJUDI15_COMPLETE_QUANTITY"),// 15度裁断完成量回报
    ADJUDI90_COMPLETE_QUANTITY("ADJUDI90_COMPLETE_QUANTITY"),// 90度裁断完成量回报
    XWYY_ADJUDI_QUANTITY("XWYY_ADJUDI_QUANTITY"),// 纤维压延度裁断完成量回报
    BEAD_COMPLETE_QUANTITY("BEAD_COMPLETE_QUANTITY"),// 胎圈完成量回报
    STEEL_WIRE_COMPLETE_QUANTITY("STEEL_WIRE_COMPLETE_QUANTITY"),// 钢丝圈完成量回报
    EMBRYO_BAD_QUANTITY("EMBRYO_BAD_QUANTITY"),// 胚胎不良数
    MPS_TO_APS_FAC("MPS_TO_APS_FAC"),// 下发分厂计划版本
    MPS_CONSTRUCTION_INFO("MPS_CONSTRUCTION_INFO"),// 施工信息表(MPS)
    PLM_CONSTRUCTION_INFO("PLM_CONSTRUCTION_INFO"),// PLM参数信息同步
    CX_DAY_COMPLETE("CX_DAY_COMPLETE"),// 成型日完成量
    LH_DAY_COMPLETE("LH_DAY_COMPLETE"),// 硫化日完成量
    TM_DAY_COMPLETE("TM_DAY_COMPLETE"),// 胎面日完成量
    TC_DAY_COMPLETE("TC_DAY_COMPLETE"),// 胎侧日完成量
    TQ_DAY_COMPLETE("TQ_DAY_COMPLETE"),// 胎圈日完成量
    NC_DAY_COMPLETE("NC_DAY_COMPLETE"),// 内衬日完成量
    GSQ_DAY_COMPLETE("GSQ_DAY_COMPLETE"),// 钢丝圈日完成量
    CD90_DAY_COMPLETE("CD90_DAY_COMPLETE"),// 90度裁断日完成量
    CD15_DAY_COMPLETE("CD15_DAY_COMPLETE"),// 15度裁断日完成量
    GDYY_DAY_COMPLETE("GDYY_DAY_COMPLETE"),// 钢带压延日完成量
    XWYY_DAY_COMPLETE("XWYY_DAY_COMPLETE"),// 纤维压延日完成量
    CX_PRODUCTION_SPEC("CX_PRODUCTION_SPEC"),// 成型机台当前生产规格接口
    CX_MID_NIGHT_FINISH("CX_MID_NIGHT_FINISH"),// 成型中夜班完成量接口
    LH_IN_PRODUCTION_SPEC("LH_IN_PRODUCTION_SPEC"),// 硫化机台当前生产规格接口
    LH_MOLD_ADJUST_PLAN("LH_MOLD_ADJUST_PLAN"),// 硫化工序模具变更计划
    CLASS_FINISH_QTY("CLASS_FINISH_QTY")// 各工序班次完成量同步接口
    ;

    private String description;

    // 获取枚举描述
    public String getDescription() {
        return description;
    }

    SyncKeyEnum(String description) {
        this.description = description;
    }

    public static SyncKeyEnum valueOf(Integer value) {
        if (null == value || value < 0 || value >= values().length) {
            return UNKNOWN;
        }
        return values()[value];
    }
}

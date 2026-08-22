package com.zlt.aps.tm.service.loader;

/**
 * 成型需求共用任务基础属性装配输入。
 *
 * <p>仅在自动数据加载服务内部传递 BOM 与 RECIPE 共用属性，不属于接口、数据库或跨模块契约。</p>
 */
final class TmFormingTaskBaseInput {

    final String orderNo;
    final String sourceOrderNos;
    final String materialCode;
    final String materialDesc;
    final String embryoCode;
    final String mainMaterialDesc;
    final String cxMachineCode;
    final String lhMachineCode;
    final String businessKeySuffix;
    final String treadCode;
    final String rubberCategory;
    final String mouthPlateCode;
    final boolean smallGlueFlag;

    /**
     * 创建成型任务基础属性装配输入。
     *
     * @param orderNo 任务工单号
     * @param sourceOrderNos 来源工单号
     * @param materialCode 物料编码
     * @param materialDesc 物料描述
     * @param embryoCode 胎胚编码
     * @param mainMaterialDesc 主物料描述
     * @param cxMachineCode 成型机台编码
     * @param lhMachineCode 硫化机台编码
     * @param businessKeySuffix 来源任务业务键后缀
     * @param treadCode 胎面编码
     * @param rubberCategory 胶料类别
     * @param mouthPlateCode 口型板编码
     * @param smallGlueFlag 主胶料是否命中小胶种规则
     */
    TmFormingTaskBaseInput(String orderNo, String sourceOrderNos, String materialCode, String materialDesc,
                           String embryoCode, String mainMaterialDesc, String cxMachineCode, String lhMachineCode,
                           String businessKeySuffix, String treadCode, String rubberCategory, String mouthPlateCode,
                           boolean smallGlueFlag) {
        this.orderNo = orderNo;
        this.sourceOrderNos = sourceOrderNos;
        this.materialCode = materialCode;
        this.materialDesc = materialDesc;
        this.embryoCode = embryoCode;
        this.mainMaterialDesc = mainMaterialDesc;
        this.cxMachineCode = cxMachineCode;
        this.lhMachineCode = lhMachineCode;
        this.businessKeySuffix = businessKeySuffix;
        this.treadCode = treadCode;
        this.rubberCategory = rubberCategory;
        this.mouthPlateCode = mouthPlateCode;
        this.smallGlueFlag = smallGlueFlag;
    }
}

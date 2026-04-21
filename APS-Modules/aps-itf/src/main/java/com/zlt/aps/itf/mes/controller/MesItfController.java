package com.zlt.aps.itf.mes.controller;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.autoLogin.loginUtils.annotation.AutoLoginLog;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.cx.api.domain.entity.CxMachineOnlineInfo;
import com.zlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.itf.mes.service.ICxScheduleResultIssueService;
import com.zlt.aps.itf.mes.service.ILhScheduleResultIssueService;
import com.zlt.aps.itf.mes.service.IMonthPlanIssueService;
import com.zlt.aps.itf.mes.service.MesBomItfService;
import com.zlt.aps.itf.mes.service.MesItfService;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.itf.vo.MesBrandDict;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmMoldAlterPlan;
import com.zlt.aps.mp.api.domain.entity.*;
import com.zlt.aps.mp.api.domain.entity.LhScheduleResultIssue;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MesItfController.java
 * 描    述：MES接口 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-16
 */
@Slf4j
@Api(tags = "MES接口")
@RestController
@RequestMapping("/mesItf")
public class MesItfController {

    @Autowired
    private MesItfService mesItfService;

    @Autowired
    private MesBomItfService mesBomItfService;

    @Autowired
    private ICxScheduleResultIssueService cxScheduleResultIssueService;

    @Autowired
    private ILhScheduleResultIssueService lhScheduleResultIssueService;

    /**
     * 同步SKU与模具关系
     *
     * @param syncDataLogs SKU与模具关系
     * @return 结果
     */
    @ApiOperation("同步SKU与模具关系")
    @PostMapping("/syncProductModRelation")
    public AjaxResult syncProductModRelation(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        return mesItfService.syncProductModRelation(syncDataLogs);
    }

    /**
     * 同步模具台账
     *
     * @param syncDataLogs 模具台账
     * @return 结果
     */
    @ApiOperation("同步模具台账")
    @PostMapping("/syncModelInfo")
    public AjaxResult syncModelInfo(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        return mesItfService.syncModelInfo(syncDataLogs);
    }

    /**
     * 同步成品库存
     *
     * @param mdmProductStock 参数
     * @return 结果
     */
    @ApiOperation("同步成品库存")
    @PostMapping("/syncProductStock")
    public AjaxResult syncProductStock(@RequestBody MdmProductStock mdmProductStock) throws ParseException {
        String factoryCode = mdmProductStock.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            mdmProductStock.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        /*Date stockDate = mdmProductStock.getStockDate();
        if (Objects.isNull(stockDate)) {
            mdmProductStock.setStockDate(DateUtils.parseDate(DateUtils.getDate(), DateUtils.YYYY_MM_DD));
        }*/
        String productTypeCode = mdmProductStock.getProductTypeCode();
        if (StringUtils.isBlank(productTypeCode)) {
            mdmProductStock.setProductTypeCode(ProductTypeEnum.WHOLE_STEEL.getValue());
        }
        return mesItfService.syncProductStock(mdmProductStock);
    }

    /**
     * 生成超期SKU
     * @param mdmProductStock 参数
     * @return 结果
     */
    @ApiOperation("生成超期SKU")
    @PostMapping("/genOverDueSkuByStock")
    public AjaxResult genOverDueSkuByStock(@RequestBody MdmProductStock mdmProductStock) throws ParseException {
        String factoryCode = mdmProductStock.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            mdmProductStock.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        Date stockDate = mdmProductStock.getStockDate();
        if (Objects.isNull(stockDate)) {
            mdmProductStock.setStockDate(DateUtils.parseDate(DateUtils.getDate(), DateUtils.YYYY_MM_DD));
        }
        String productTypeCode = mdmProductStock.getProductTypeCode();
        if (StringUtils.isBlank(productTypeCode)) {
            mdmProductStock.setProductTypeCode(ProductTypeEnum.WHOLE_STEEL.getValue());
        }
        return mesItfService.genOverDueSkuByStock(mdmProductStock);
    }

    /**
     * 获取实时成品库存
     *
     * @param mdmProductStock 参数
     * @return 结果
     */
    @ApiOperation("获取实时成品库存")
    @PostMapping("/getProductStock")
    public List<MdmProductStock> getProductStock(@RequestBody MdmProductStock mdmProductStock) throws ParseException {
        String factoryCode = mdmProductStock.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            mdmProductStock.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        /*Date stockDate = mdmProductStock.getStockDate();
        if (Objects.isNull(stockDate)) {
            mdmProductStock.setStockDate(DateUtils.parseDate(DateUtils.getDate(), DateUtils.YYYY_MM_DD));
        }*/
        String productTypeCode = mdmProductStock.getProductTypeCode();
        if (StringUtils.isBlank(productTypeCode)) {
            mdmProductStock.setProductTypeCode(ProductTypeEnum.WHOLE_STEEL.getValue());
        }
        return mesItfService.getProductStock(mdmProductStock);
    }

    /**
     * 同步不合格库存
     *
     * @param mdmUnqualifiedStock 参数
     * @return 结果
     */
    @ApiOperation("同步不合格库存")
    @PostMapping("/syncUnqualifiedStock")
    public AjaxResult syncUnqualifiedStock(@RequestBody MdmUnqualifiedStock mdmUnqualifiedStock) throws ParseException {
        String factoryCode = mdmUnqualifiedStock.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            mdmUnqualifiedStock.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        /*Date stockDate = mdmUnqualifiedStock.getStockDate();
        if (Objects.isNull(stockDate)) {
            mdmUnqualifiedStock.setStockDate(DateUtils.parseDate(DateUtils.getDate(), DateUtils.YYYY_MM_DD));
        }*/
        return mesItfService.syncUnqualifiedStock(mdmUnqualifiedStock);
    }

    /**
     * 获取不合格库存
     *
     * @param mdmUnqualifiedStock 参数
     * @return 结果
     */
    @ApiOperation("获取实时不合格库存")
    @PostMapping("/getUnqualifiedStock")
    public List<MdmUnqualifiedStock> getUnqualifiedStock(@RequestBody MdmUnqualifiedStock mdmUnqualifiedStock) throws ParseException {
        String factoryCode = mdmUnqualifiedStock.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            mdmUnqualifiedStock.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        /*Date stockDate = mdmUnqualifiedStock.getStockDate();
        if (Objects.isNull(stockDate)) {
            mdmUnqualifiedStock.setStockDate(DateUtils.parseDate(DateUtils.getDate(), DateUtils.YYYY_MM_DD));
        }*/
        return mesItfService.getUnqualifiedStock(mdmUnqualifiedStock);
    }

    /**
     * 同步特殊材料库存
     *
     * @param rawSpecialMaterialStock 参数
     * @return 结果
     */
    @ApiOperation("同步特殊材料库存")
    @PostMapping("/syncRawSpecialMaterialStock")
    public AjaxResult syncRawSpecialMaterialStock(@RequestBody RawSpecialMaterialStock rawSpecialMaterialStock) throws ParseException {
        String factoryCode = rawSpecialMaterialStock.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            rawSpecialMaterialStock.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        /*Date stockDate = rawSpecialMaterialStock.getStockDate();
        if (Objects.isNull(stockDate)) {
            rawSpecialMaterialStock.setStockDate(DateUtils.parseDate(DateUtils.getDate(), DateUtils.YYYY_MM_DD));
        }*/
        return mesItfService.syncRawSpecialMaterialStock(rawSpecialMaterialStock);
    }

    /**
     * 查询特殊材料库存
     *
     * @param rawSpecialMaterialStock 参数
     * @return 结果
     */
    @ApiOperation("获取实时特殊材料库存")
    @PostMapping("/getRawSpecialMaterialStock")
    public List<RawSpecialMaterialStock> getRawSpecialMaterialStock(@RequestBody RawSpecialMaterialStock rawSpecialMaterialStock) throws ParseException {
        String factoryCode = rawSpecialMaterialStock.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            rawSpecialMaterialStock.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        /*Date stockDate = rawSpecialMaterialStock.getStockDate();
        if (Objects.isNull(stockDate)) {
            rawSpecialMaterialStock.setStockDate(DateUtils.parseDate(DateUtils.getDate(), DateUtils.YYYY_MM_DD));
        }*/
        return mesItfService.getRawSpecialMaterialStock(rawSpecialMaterialStock);
    }

    /**
     * 同步原材料出库
     *
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步原材料出库")
    @PostMapping("/syncRawMaterialOutboundRecord")
    public AjaxResult syncRawMaterialOutboundRecord(@RequestBody AuxReqSyncDataLogs syncDataLogs) throws ParseException {
        return mesItfService.syncRawMaterialOutboundRecord(syncDataLogs);
    }

    /**
     * 同步成品物料信息
     *
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步成品物料信息")
    @PostMapping("/syncMaterial")
    public AjaxResult syncMaterial(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        return mesItfService.syncMaterial(syncDataLogs);
    }

    /**
     * 同步模壳台账信息
     *
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步模壳台账信息")
    @PostMapping("/syncMoldShell")
    public AjaxResult syncMoldShell(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        return mesItfService.syncMoldShell(syncDataLogs);
    }

    @Autowired
    private IMonthPlanIssueService iMonthPlanIssueService;

    /**
     * 下发月计划
     *
     * @param finalResultList 参数
     * @return 结果
     */
    @ApiOperation("下发月计划")
    @PostMapping("/issueMonthPlan")
    public AjaxResult issueMonthPlan(@RequestBody List<FactoryMonthPlanProductionFinalResult> finalResultList) {
        return iMonthPlanIssueService.issueMonthPlan(finalResultList);
    }

    /**
     * 查询MES品牌字典
     *
     * @return 结果
     */
    @ApiOperation("查询MES品牌字典")
    @PostMapping("/selectMesBrandDict")
    public List<MesBrandDict> selectMesBrandDict() {
        return mesItfService.selectMesBrandDict();
    }

    /**
     * 同步产月度计划及硫化施工信息同步接口（SKU与施工关系表）
     *
     * @return 结果
     */
    @ApiOperation("同步产月度计划及硫化施工信息同步接口（SKU与施工关系表）")
    @PostMapping("/syncLhConstructionInfo")
    public AjaxResult syncLhConstructionInfo(String factoryCode, String dataVersion) {
        AuxReqSyncDataLogs syncDataLogs = new AuxReqSyncDataLogs();
        syncDataLogs.setFactoryCode(factoryCode);
        syncDataLogs.setDataVersion(dataVersion);
        return mesBomItfService.syncLhConstructionInfo(syncDataLogs);
    }

    /**
     * 半部件BOM接口
     *
     * @return 结果
     */
    @ApiOperation("半部件BOM接口")
    @PostMapping("/syncConstructionInfo")
    public AjaxResult syncConstructionInfo(String factoryCode, String dataVersion) {
        AuxReqSyncDataLogs syncDataLogs = new AuxReqSyncDataLogs();
        syncDataLogs.setFactoryCode(factoryCode);
        syncDataLogs.setDataVersion(dataVersion);
        return mesBomItfService.syncConstructionInfo(syncDataLogs);
    }

    /**
     * 同步BOM
     *
     * @return 结果
     */
    @ApiOperation("同步BOM")
    @PostMapping("/syncBomInfo")
    public AjaxResult syncBomInfo(String factoryCode, String dataVersion) {
        AuxReqSyncDataLogs syncDataLogs = new AuxReqSyncDataLogs();
        syncDataLogs.setFactoryCode(factoryCode);
        syncDataLogs.setDataVersion(dataVersion);
        return mesBomItfService.syncBomInfo(syncDataLogs);
    }

    /**
     * 同步成型在机数据
     * @param cxMachineOnlineInfo 参数
     * @return 结果
     */
    @ApiOperation("同步成型在机数据")
    @PostMapping("/syncMachineOnlineInfo")
    @AutoLoginLog
    public AjaxResult syncMachineOnlineInfo(@RequestBody CxMachineOnlineInfo cxMachineOnlineInfo) {
        String factoryCode = cxMachineOnlineInfo.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            cxMachineOnlineInfo.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mesItfService.syncMachineOnlineInfo(cxMachineOnlineInfo);
    }

    /**
     * 同步硫化在机数据
     * @param lhMachineOnlineInfo 参数
     * @return 结果
     */
    @ApiOperation("同步硫化在机数据")
    @PostMapping("/syncLhMachineOnlineInfo")
    @AutoLoginLog
    public AjaxResult syncLhMachineOnlineInfo(@RequestBody LhMachineOnlineInfo lhMachineOnlineInfo) {
        String factoryCode = lhMachineOnlineInfo.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            lhMachineOnlineInfo.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mesItfService.syncLhMachineOnlineInfo(lhMachineOnlineInfo);
    }

    /**
     * 同步设备保养计划
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步设备保养计划")
    @PostMapping("/syncDevMaintenancePlan")
    @AutoLoginLog
    public AjaxResult syncDevMaintenancePlan(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        String factoryCode = syncDataLogs.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mesItfService.syncDevMaintenancePlan(syncDataLogs);
    }

    /**
     * 同步胶囊已使用次数
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步胶囊已使用次数")
    @PostMapping("/syncLhRepairCapsule")
    @AutoLoginLog
    public AjaxResult syncLhRepairCapsule(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        String factoryCode = syncDataLogs.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mesItfService.syncLhRepairCapsule(syncDataLogs);
    }

    /**
     * 同步模具清洗预警计划
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步模具清洗预警")
    @PostMapping("/syncMouldCleanWarn")
    @AutoLoginLog
    public AjaxResult syncMouldCleanWarn(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        String factoryCode = syncDataLogs.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mesItfService.syncMouldCleanWarn(syncDataLogs);
    }

    /**
     * 同步结构整车胎面配置
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步结构整车胎面配置")
    @PostMapping("/syncStructureTreadConfig")
    @AutoLoginLog
    public AjaxResult syncStructureTreadConfig(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        String factoryCode = syncDataLogs.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mesItfService.syncStructureTreadConfig(syncDataLogs);
    }

    /**
     * 同步生胎库存
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步生胎库存")
    @PostMapping("/syncMesCxStock")
    @AutoLoginLog
    public AjaxResult syncMesCxStock(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        String factoryCode = syncDataLogs.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mesItfService.syncMesCxStock(syncDataLogs);
    }

    /**
     * 同步胎面库存
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步胎面库存")
    @PostMapping("/syncTreadStock")
    @AutoLoginLog
    public AjaxResult syncTreadStock(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        String factoryCode = syncDataLogs.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mesItfService.syncTreadStock(syncDataLogs);
    }



    /**
     * 成型排程结果下发到MES
     * 业务规则：
     * 1. 更新当天的2班（早中班，即二班和三班）- 清空一班数据
     * 2. 更新明天的3班（早中晚班，即一班、二班和三班）
     * 3. 下发后天的3班（早中晚班，即一班、二班和三班）
     *
     * @param cxScheduleResultIssueList 成型排程结果列表
     * @return 结果
     */
    @ApiOperation("成型排程结果下发到MES")
    @PostMapping("/issueCxScheduleResult")
    @AutoLoginLog
    public AjaxResult issueCxScheduleResult(@RequestBody List<CxScheduleResultIssue> cxScheduleResultIssueList) {
        // 从上下文中获取厂别和分公司编码
        String factoryCode = FactoryConstant.DEFAULT_FACTORY_CODE;
        String companyCode = FactoryConstant.DEFAULT_COMPANY_CODE;
        return cxScheduleResultIssueService.issueCxScheduleResult(cxScheduleResultIssueList, factoryCode, companyCode);
    }

    /**
     * 同步成型排程完成量
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步成型排程完成量")
    @PostMapping("/syncCxClassShiftFinishQty")
    @AutoLoginLog
    public AjaxResult syncCxClassShiftFinishQty(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        String factoryCode = syncDataLogs.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mesItfService.syncCxClassShiftFinishQty(syncDataLogs);
    }

    /**
     * 同步硫化排程完成量
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步硫化排程完成量")
    @PostMapping("/syncLhClassShiftFinishQty")
    @AutoLoginLog
    public AjaxResult syncLhClassShiftFinishQty(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        String factoryCode = syncDataLogs.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mesItfService.syncLhClassShiftFinishQty(syncDataLogs);
    }

    /**
     * 同步成型排程日完成量
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步成型排程日完成量")
    @PostMapping("/syncCxScheDayFinishQty")
    @AutoLoginLog
    public AjaxResult syncCxScheDayFinishQty(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        String factoryCode = syncDataLogs.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mesItfService.syncCxScheDayFinishQty(syncDataLogs);
    }

    /**
     * 同步硫化排程日完成量
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步硫化排程日完成量")
    @PostMapping("/syncLhScheDayFinishQty")
    @AutoLoginLog
    public AjaxResult syncLhScheDayFinishQty(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        String factoryCode = syncDataLogs.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mesItfService.syncLhScheDayFinishQty(syncDataLogs);
    }

    /**
     * 硫化排程结果下发到MES
     * 业务规则：
     * 1. 更新当天的2班（早中班）- 清空一班数据
     * 2. 更新明天的3班（早中晚班）
     * 3. 下发后天的3班（早中晚班）
     *
     * @param lhScheduleResultIssueList 硫化排程结果列表
     * @return 结果
     */
    @ApiOperation("硫化排程结果下发到MES")
    @PostMapping("/issueLhScheduleResult")
    @AutoLoginLog
    public AjaxResult issueLhScheduleResult(@RequestBody List<LhScheduleResultIssue> lhScheduleResultIssueList) {
        // 从上下文中获取厂别和分公司编码
        String factoryCode = FactoryConstant.DEFAULT_FACTORY_CODE;
        String companyCode = FactoryConstant.DEFAULT_COMPANY_CODE;
        return lhScheduleResultIssueService.issueLhScheduleResult(lhScheduleResultIssueList, factoryCode, companyCode);
    }

    /**
     * 模具交替计划下发到MES
     * @param moldAlterPlanList 模具交替计划列表
     * @return 结果
     */
    @ApiOperation("模具交替计划下发到MES")
    @PostMapping("/issueMoldAlterPlan")
    @AutoLoginLog
    public AjaxResult issueMoldAlterPlan(@RequestBody List<MdmMoldAlterPlan> moldAlterPlanList) {
        return mesItfService.issueMoldAlterPlan(moldAlterPlanList);
    }

    /**
     * 同步模具交替计划完成回报
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @ApiOperation("同步模具交替计划完成回报")
    @PostMapping("/syncMoldAlterPlanFinish")
    @AutoLoginLog
    public AjaxResult syncMoldAlterPlanFinish(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        return mesItfService.syncMoldAlterPlanFinish(syncDataLogs);
    }

    /**
     * 同步出库未扫描订单
     *
     * @param outbountOrdersNotScan 参数
     * @return 结果
     */
    @ApiOperation("同步出库未扫描订单")
    @PostMapping("/syncOutbountOrdersNotScan")
    @AutoLoginLog
    public AjaxResult syncOutbountOrdersNotScan(@RequestBody MdmOutbountOrdersNotScan outbountOrdersNotScan) {
        String factoryCode = outbountOrdersNotScan.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            outbountOrdersNotScan.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mesItfService.syncOutbountOrdersNotScan(outbountOrdersNotScan);
    }

    /**
     * 查询出库未扫描订单
     *
     * @param outbountOrdersNotScan 参数
     * @return 结果
     */
    @ApiOperation("查询出库未扫描订单")
    @PostMapping("/getOutbountOrdersNotScan")
    public List<MdmOutbountOrdersNotScan> getOutbountOrdersNotScan(@RequestBody MdmOutbountOrdersNotScan outbountOrdersNotScan) {
        String factoryCode = outbountOrdersNotScan.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            outbountOrdersNotScan.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mesItfService.getOutbountOrdersNotScan(outbountOrdersNotScan);
    }
}

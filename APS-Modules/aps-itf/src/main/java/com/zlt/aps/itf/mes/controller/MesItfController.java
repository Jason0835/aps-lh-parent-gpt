package com.zlt.aps.itf.mes.controller;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.autoLogin.loginUtils.annotation.AutoLoginLog;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.cx.api.domain.entity.CxMachineOnlineInfo;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.itf.mes.service.*;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.itf.vo.MesBrandDict;
import com.zlt.aps.itf.vo.MesShiftStockSyncRequest;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmMoldAlterPlan;
import com.zlt.aps.mp.api.domain.entity.*;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResultIssue;
import com.zlt.aps.tc.api.domain.vo.TcReleaseFeedbackVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @Autowired
    private com.zlt.aps.itf.mes.service.ITqScheduleResultIssueService tqScheduleResultIssueService;

    @Autowired
    private com.zlt.aps.itf.mes.service.ICd90ScheduleResultIssueService cd90ScheduleResultIssueService;

    @Autowired
    private ICd15ScheduleResultIssueService cd15ScheduleResultIssueService;

    @Autowired
    private ITcMesBridgeService tcMesBridgeService;

    @Autowired
    private ITcScheduleResultIssueService tcScheduleResultIssueService;

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
     * 按指定版本号同步硫化在机数据（临时任务）
     * @param dataVersion 指定版本号
     * @return 结果
     */
    @ApiOperation("按指定版本号同步硫化在机数据（临时任务）")
    @PostMapping("/syncLhMachineOnlineInfoByVersion")
    @AutoLoginLog
    public AjaxResult syncLhMachineOnlineInfoByVersion(@RequestParam("dataVersion") String dataVersion) {
        return mesItfService.syncLhMachineOnlineInfoByVersion(dataVersion);
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

    @ApiOperation("仅同步设备保养计划数据（不触发生成精度计划）")
    @PostMapping("/syncDevMaintenancePlanOnly")
    @AutoLoginLog
    public AjaxResult syncDevMaintenancePlanOnly(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        String factoryCode = syncDataLogs.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mesItfService.syncDevMaintenancePlanOnly(syncDataLogs);
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
     * 同步直裁库存（从 MES 中间表 T_MES_CD90_STOCK 同步到 t_cd90_stock）
     * @param syncDataLogs 参数（可传 factoryCode；queryParams.shiftCode 可覆盖自动推断班次）
     * @return 结果
     */
    @ApiOperation("同步直裁库存")
    @PostMapping("/syncMesCd90Stock")
    @AutoLoginLog
    public AjaxResult syncMesCd90Stock(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        String factoryCode = syncDataLogs.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mesItfService.syncMesCd90Stock(syncDataLogs);
    }

    /**
     * 实时查询MES生胎库存（不写入APS本地表，仅供成型排程实时调用）
     * @param syncDataLogs 参数（可传factoryCode过滤分厂）
     * @return 生胎库存列表
     */
    @ApiOperation("实时查询MES生胎库存")
    @PostMapping("/getCxStock")
    @AutoLoginLog
    public List<CxStock> getCxStock(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        String factoryCode = syncDataLogs.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mesItfService.getCxStock(syncDataLogs);
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
     * 同步胎面自动滚动班次库存。
     *
     * @param request 工厂、物理库存日和班序
     * @return 同步结果
     */
    @ApiOperation("同步胎面自动滚动班次库存")
    @PostMapping("/syncTreadShiftStock")
    @AutoLoginLog
    public AjaxResult syncTreadShiftStock(@RequestBody MesShiftStockSyncRequest request) {
        return this.mesItfService.syncTreadShiftStock(request);
    }

    /**
     * 同步胎圈库存
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步胎圈库存")
    @PostMapping("/syncMesTqStock")
    @AutoLoginLog
    public AjaxResult syncMesTqStock(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        String factoryCode = syncDataLogs.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mesItfService.syncMesTqStock(syncDataLogs);
    }

    /**
     * 同步胎圈排程完成量
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步胎圈排程完成量")
    @PostMapping("/syncTqClassShiftFinishQty")
    @AutoLoginLog
    public AjaxResult syncTqClassShiftFinishQty(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        String factoryCode = syncDataLogs.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mesItfService.syncTqClassShiftFinishQty(syncDataLogs);
    }

    /**
     * 同步胎圈排程日完成量
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步胎圈排程日完成量")
    @PostMapping("/syncTqScheDayFinishQty")
    @AutoLoginLog
    public AjaxResult syncTqScheDayFinishQty(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        String factoryCode = syncDataLogs.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mesItfService.syncTqScheDayFinishQty(syncDataLogs);
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
        // 分公司编码与分厂编码保持一致
        String factoryCode = FactoryConstant.DEFAULT_FACTORY_CODE;
        String companyCode = factoryCode;
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
     * 按上一天最新版本号同步硫化排程完成量（临时任务）
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("按上一天最新版本号同步硫化排程完成量（临时任务）")
    @PostMapping("/syncLhClassShiftFinishQtyByYesterday")
    @AutoLoginLog
    public AjaxResult syncLhClassShiftFinishQtyByYesterday(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        String factoryCode = syncDataLogs.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mesItfService.syncLhClassShiftFinishQtyByYesterday(syncDataLogs);
    }

    /**
     * 按指定版本号同步硫化排程完成量（临时任务）
     * @param dataVersion 指定版本号
     * @return 结果
     */
    @ApiOperation("按指定版本号同步硫化排程完成量（临时任务）")
    @PostMapping("/syncLhClassShiftFinishQtyByVersion")
    @AutoLoginLog
    public AjaxResult syncLhClassShiftFinishQtyByVersion(@RequestParam("dataVersion") String dataVersion) {
        return mesItfService.syncLhClassShiftFinishQtyByVersion(dataVersion);
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
     * 按最新版本号同步硫化排程日完成量（临时任务，不限日期）
     * @return 结果
     */
    @ApiOperation("按最新版本号同步硫化排程日完成量（临时任务，不限日期）")
    @PostMapping("/syncLhScheDayFinishQtyByLatestVersion")
    @AutoLoginLog
    public AjaxResult syncLhScheDayFinishQtyByLatestVersion(@RequestParam("dataVersion") String dataVersion) {
        return mesItfService.syncLhScheDayFinishQtyByLatestVersion(dataVersion);
    }

    /**
     * 硫化排程结果下发到MES
     * 业务规则：
     * 每条硫化排程结果自带8班数据，覆盖排程日期前2天到排程日期当天：
     * 1. 更新T-2日的3班（夜早中班）
     * 2. 更新T-1日的3班（夜早中班）
     * 3. 下发T日的2班（早中班，夜班尚未排产不下发）
     *
     * @param lhScheduleResultIssueList 硫化排程结果列表
     * @return 结果
     */
    @ApiOperation("硫化排程结果下发到MES")
    @PostMapping("/issueLhScheduleResult")
    @AutoLoginLog
    public AjaxResult issueLhScheduleResult(@RequestBody List<LhScheduleResultIssue> lhScheduleResultIssueList) {
        String factoryCode = FactoryConstant.DEFAULT_FACTORY_CODE;
        String companyCode = factoryCode;
        return lhScheduleResultIssueService.issueLhScheduleResult(lhScheduleResultIssueList, factoryCode, companyCode);
    }

    /**
     * 胎圈排程结果下发到MES
     * 业务规则：
     * 1. D日（今天）：更新中班数据（胎圈1班→MES中班），夜班早班已过不下发
     * 2. D+1日（明天）：更新夜早中3班数据（钢丝圈2/3/4班→MES夜/早/中班）
     * 3. D+2日（后天）：先删后插夜早2班数据（钢丝圈5/6班→MES夜/早班），中班尚未排产不下发
     * TQ_CLASS1~6_PLAN 全量传递到每条记录
     *
     * @param gsqScheduleResultIssueList 钢丝圈排程结果列表（已按3天拆分）
     * @return 下发结果（data 字段携带 mesStatus：IS_RELEASE/FAILURE_RELEASE/TIMEOUT_FAILURE）
     */
    @ApiOperation("钢丝圈排程结果下发到MES")
    @PostMapping("/issueGsqScheduleResult")
    @AutoLoginLog
    public AjaxResult issueGsqScheduleResult(@RequestBody List<GsqScheduleResultIssue> gsqScheduleResultIssueList) {
        String factoryCode = FactoryConstant.DEFAULT_FACTORY_CODE;
        String companyCode = factoryCode;
        return gsqScheduleResultIssueService.issueGsqScheduleResult(gsqScheduleResultIssueList, factoryCode, companyCode);
    }

    /**
     * 同步胎面排程完成量
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步胎面排程完成量")
    @PostMapping("/syncTmClassShiftFinishQty")
    @AutoLoginLog
    public AjaxResult syncTmClassShiftFinishQty(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        String factoryCode = syncDataLogs.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mesItfService.syncTmClassShiftFinishQty(syncDataLogs);
    }

    /**
     * 同步胎面排程日完成量
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步胎面排程日完成量")
    @PostMapping("/syncTmScheDayFinishQty")
    @AutoLoginLog
    public AjaxResult syncTmScheDayFinishQty(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        String factoryCode = syncDataLogs.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mesItfService.syncTmScheDayFinishQty(syncDataLogs);
    }

    /**
     * 胎面排程结果下发到MES
     * 业务规则（与胎圈一致）：
     * 1. D日（今天）：更新中班数据，夜班早班已过不下发
     * 2. D+1日（明天）：更新夜早中3班数据
     * 3. D+2日（后天）：先删后插夜早2班数据，中班尚未排产不下发
     *
     * @param tmScheduleResultIssueList 胎面排程结果列表（已按3天拆分）
     * @return 结果
     */
    @ApiOperation("胎面排程结果下发到MES")
    @PostMapping("/issueTmScheduleResult")
    @AutoLoginLog
    public AjaxResult issueTmScheduleResult(@RequestBody List<com.zlt.aps.tm.api.domain.entity.TmScheduleResultIssue> tmScheduleResultIssueList) {
        String factoryCode = FactoryConstant.DEFAULT_FACTORY_CODE;
        String companyCode = factoryCode;
        return mesItfService.issueTmScheduleResult(tmScheduleResultIssueList);
    }

    /**
     * 同步胎侧库存。
     *
     * @param syncDataLogs 同步请求
     * @return 同步结果
     */
    @ApiOperation("同步胎侧库存")
    @PostMapping("/syncSidewallStock")
    @AutoLoginLog
    public AjaxResult syncSidewallStock(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        return this.tcMesBridgeService.syncStock(syncDataLogs);
    }

    /**
     * 同步胎侧自动滚动班次库存。
     *
     * @param request 工厂、物理库存日和班序
     * @return 同步结果
     */
    @ApiOperation("同步胎侧自动滚动班次库存")
    @PostMapping("/syncSidewallShiftStock")
    @AutoLoginLog
    public AjaxResult syncSidewallShiftStock(@RequestBody MesShiftStockSyncRequest request) {
        return this.tcMesBridgeService.syncShiftStock(request);
    }

    /**
     * 同步胎侧三班完成量并回写六班排程结果。
     *
     * @param syncDataLogs 同步请求
     * @return 同步结果
     */
    @ApiOperation("同步胎侧排程完成量")
    @PostMapping("/syncTcClassShiftFinishQty")
    @AutoLoginLog
    public AjaxResult syncTcClassShiftFinishQty(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        return this.tcMesBridgeService.syncShiftFinishQty(syncDataLogs);
    }

    /**
     * 同步胎侧日完成量。
     *
     * @param syncDataLogs 同步请求
     * @return 同步结果
     */
    @ApiOperation("同步胎侧排程日完成量")
    @PostMapping("/syncTcScheDayFinishQty")
    @AutoLoginLog
    public AjaxResult syncTcScheDayFinishQty(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        return this.tcMesBridgeService.syncDayFinishQty(syncDataLogs);
    }

    /**
     * 下发胎侧排程结果到MES。
     *
     * @param issueList 胎侧排程结果
     * @return 下发结果
     */
    @ApiOperation("胎侧排程结果下发到MES")
    @PostMapping("/issueTcScheduleResult")
    @AutoLoginLog
    public AjaxResult issueTcScheduleResult(@RequestBody List<TcScheduleResultIssue> issueList) {
        return this.tcScheduleResultIssueService.issue(issueList);
    }

    /**
     * 查询胎侧排程发布处理状态。
     *
     * @param dataVersion 发布数据版本
     * @return MES发布反馈
     */
    @ApiOperation("查询胎侧排程发布处理状态")
    @PostMapping("/queryTcScheduleIssueStatus")
    @AutoLoginLog
    public TcReleaseFeedbackVo queryTcScheduleIssueStatus(@RequestParam("dataVersion") String dataVersion) {
        return this.tcScheduleResultIssueService.queryStatus(dataVersion);
    }

    /**
     * 直裁排程结果下发到MES
     * 业务规则：
     * 1. 班次配置由 t_cd90_shift_config 启用项决定，默认 CLASS1~CLASS6
     * 2. 每条 issue 携带 classField / scheduleDay / dayShiftOrder / shiftName，由 MES 侧按班次槽位映射
     * 3. APS 侧按 SCHEDULE_DAY 推导排班日期，day1=T-1，day2=T，day3=T+1
     *
     * @param cd90ScheduleResultIssueList 直裁排程结果列表（已按班次展开）
     * @return 结果
     */
    @ApiOperation("直裁排程结果下发到MES")
    @PostMapping("/issueCd90ScheduleResult")
    @AutoLoginLog
    public AjaxResult issueCd90ScheduleResult(@RequestBody List<com.zlt.aps.mp.api.domain.entity.Cd90ScheduleResultIssue> cd90ScheduleResultIssueList) {
        String factoryCode = FactoryConstant.DEFAULT_FACTORY_CODE;
        String companyCode = factoryCode;
        return cd90ScheduleResultIssueService.issueCd90ScheduleResult(cd90ScheduleResultIssueList, factoryCode, companyCode);
    }

    /**
     * 斜裁排程结果下发到 MES。
     *
     * @param issueList 按班次展开的斜裁结果
     * @return 下发结果
     */
    @ApiOperation("斜裁排程结果下发到MES")
    @PostMapping("/issueCd15ScheduleResult")
    @AutoLoginLog
    public AjaxResult issueCd15ScheduleResult(
            @RequestBody List<com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResultIssue> issueList) {
        String factoryCode = issueList != null && !issueList.isEmpty()
                && StringUtils.isNotBlank(issueList.get(0).getFactoryCode())
                ? issueList.get(0).getFactoryCode()
                : FactoryConstant.DEFAULT_FACTORY_CODE;
        String companyCode = factoryCode;
        return cd15ScheduleResultIssueService.issueCd15ScheduleResult(
                issueList, factoryCode, companyCode);
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

    @ApiOperation("同步MES硫化精度计划实际执行日期回填数据")
    @PostMapping("/syncLhPrecisionPlanActual")
    public AjaxResult syncLhPrecisionPlanActual(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        String factoryCode = syncDataLogs.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mesItfService.syncLhPrecisionPlanActual(syncDataLogs);
    }

    @ApiOperation("下发硫化精度计划到MES")
    @PostMapping("/issueLhPrecisionPlan")
    public AjaxResult issueLhPrecisionPlan(@RequestParam("factoryCode") String factoryCode) {
        if (StringUtils.isBlank(factoryCode)) {
            factoryCode = FactoryConstant.DEFAULT_FACTORY_CODE;
        }
        return mesItfService.issueLhPrecisionPlan(factoryCode);
    }

    @ApiOperation("同步MES数据并生成硫化精度计划（综合接口）")
    @PostMapping("/syncAndGenerateLhPrecisionPlan")
    public AjaxResult syncAndGenerateLhPrecisionPlan(@RequestParam("year") Integer year) {
        if (year == null) {
            year = java.time.LocalDate.now().getYear();
        }
        return mesItfService.syncAndGenerateLhPrecisionPlan(year);
    }

    @ApiOperation("同步MES数据并生成硫化精度计划（按版本号前缀过滤，综合接口）")
    @PostMapping("/syncAndGenerateLhPrecisionPlanByVersionPrefix")
    public AjaxResult syncAndGenerateLhPrecisionPlanByVersionPrefix(@RequestParam("versionPrefix") String versionPrefix,
                                                                     @RequestParam("year") Integer year) {
        if (year == null) {
            year = java.time.LocalDate.now().getYear();
        }
        return mesItfService.syncAndGenerateLhPrecisionPlanByVersionPrefix(versionPrefix, year);
    }

    @ApiOperation("同步MES数据并生成硫化精度计划（按版本号前缀过滤，不限最大版本号，综合接口）")
    @PostMapping("/syncAndGenerateLhPrecisionPlanByVersionPrefixAllVersions")
    public AjaxResult syncAndGenerateLhPrecisionPlanByVersionPrefixAllVersions(@RequestParam("versionPrefix") String versionPrefix,
                                                                                @RequestParam("year") Integer year) {
        if (year == null) {
            year = java.time.LocalDate.now().getYear();
        }
        return mesItfService.syncAndGenerateLhPrecisionPlanByVersionPrefixAllVersions(versionPrefix, year);
    }

    @ApiOperation("临时任务-按计划时间年份同步回填实际日期并生成下一年度精度计划")
    @PostMapping("/syncAndFillActualDateByOperYear")
    public AjaxResult syncAndFillActualDateByOperYear(@RequestParam("versionPrefix") String versionPrefix,
                                                       @RequestParam("operYear") Integer operYear) {
        return mesItfService.syncAndFillActualDateByOperYear(versionPrefix, operYear);
    }

    /**
     * 清理并重新同步所有MES历史数据（含今天）
     * 执行步骤：
     * 1. 逻辑删除APS库中今天及今天之前的所有数据（8张表）
     * 2. 从MES库重新抓取每天（含今天）最新版本数据
     * 3. 将MES数据插入到APS库
     * 涉及表：成型在机、硫化在机、胶囊已使用次数、生胎库存、成型排程完成量、成型排程日完成量、硫化排程完成量、硫化排程日完成量
     *
     * @return 执行结果
     */
    @ApiOperation("清理并重新同步所有MES历史数据（含今天）")
    @PostMapping("/cleanAndResyncAllHistory")
    @AutoLoginLog
    public AjaxResult cleanAndResyncAllHistory() {
        return mesItfService.cleanAndResyncAllHistory();
    }

    /**
     * 临时任务：按版本迭代同步模具清洗预警数据并生成清洗计划
     * 执行步骤：
     * 1. 清空APS现有的模具清洗预警和清洗计划表全部数据
     * 2. 从MES获取全部模具清洗预警版本号（升序排列）
     * 3. 从最小版本号开始，先插入APS作为初始数据
     * 4. 逐个版本迭代，对后续版本进行更新和新增
     * 5. 迭代到最新版本后，基于全部预警数据（不限制版本号）生成模具清洗计划
     * 6. 删除的预警也同步生成计划（标记为已删除的计划）
     *
     * @param syncDataLogs 同步参数
     * @return 执行结果
     */
    @ApiOperation("临时任务-按版本迭代同步模具清洗预警并生成清洗计划")
    @PostMapping("/syncAllVersionsMouldCleanWarnAndGenPlan")
    @AutoLoginLog
    public AjaxResult syncAllVersionsMouldCleanWarnAndGenPlan(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        String factoryCode = syncDataLogs.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mesItfService.syncAllVersionsMouldCleanWarnAndGenPlan(syncDataLogs);
    }

    @ApiOperation("硫化日完成量回填芯片库存")
    @PostMapping("/syncDayFinishQtyToChipStock")
    @AutoLoginLog
    public AjaxResult syncDayFinishQtyToChipStock() {
        return mesItfService.syncDayFinishQtyToChipStock();
    }

    /**
     * 同步设备计划停机
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步设备计划停机")
    @PostMapping("/syncDevPlanClose")
    @AutoLoginLog
    public AjaxResult syncDevPlanClose(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        String factoryCode = syncDataLogs.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mesItfService.syncDevPlanClose(syncDataLogs);
    }
}

package com.zlt.aps.itf.mes.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.itf.mes.service.MesItfService;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;
import com.zlt.aps.monthplan.api.domain.entity.MdmUnqualifiedStock;
import com.zlt.aps.monthplan.api.domain.entity.RawMaterialOutboundRecord;
import com.zlt.aps.monthplan.api.domain.entity.RawSpecialMaterialStock;
import com.zlt.sync.domain.AuxReqSyncDataLogs;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.util.List;

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
        return mesItfService.syncProductStock(mdmProductStock);
    }

    /**
     * 获取实时成品库存
     *
     * @param mdmProductStock 参数
     * @return 结果
     */
    @ApiOperation("获取实时成品库存")
    @PostMapping("/getProductStock")
    public List<MdmProductStock> getProductStock(@RequestBody MdmProductStock mdmProductStock) {
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
        return mesItfService.syncUnqualifiedStock(mdmUnqualifiedStock);
    }

    /**
     * 获取不合格库存
     *
     * @param mdmUnqualifiedStock 参数
     * @return 结果
     */
    @ApiOperation("获取不合格库存")
    @PostMapping("/getUnqualifiedStock")
    public List<MdmUnqualifiedStock> getUnqualifiedStock(@RequestBody MdmUnqualifiedStock mdmUnqualifiedStock) {
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
        return mesItfService.syncRawSpecialMaterialStock(rawSpecialMaterialStock);
    }

    /**
     * 查询特殊材料库存
     *
     * @param rawSpecialMaterialStock 参数
     * @return 结果
     */
    @ApiOperation("同步特殊材料库存")
    @PostMapping("/getRawSpecialMaterialStock")
    public List<RawSpecialMaterialStock> getRawSpecialMaterialStock(@RequestBody RawSpecialMaterialStock rawSpecialMaterialStock) {
        return mesItfService.getRawSpecialMaterialStock(rawSpecialMaterialStock);
    }

    /**
     * 同步原材料出库
     *
     * @param materialOutboundRecord 参数
     * @return 结果
     */
    @ApiOperation("同步原材料出库")
    @PostMapping("/syncRawMaterialOutboundRecord")
    public AjaxResult syncRawMaterialOutboundRecord(@RequestBody RawMaterialOutboundRecord materialOutboundRecord) throws ParseException {
        return mesItfService.syncRawMaterialOutboundRecord(materialOutboundRecord);
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
}

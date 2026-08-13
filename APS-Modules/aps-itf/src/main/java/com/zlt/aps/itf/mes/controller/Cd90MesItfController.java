package com.zlt.aps.itf.mes.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.autoLogin.loginUtils.annotation.AutoLoginLog;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.itf.mes.service.ICd90MesItfService;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.itf.vo.MesShiftStockSyncRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 直裁MES接口控制器。
 */
@Api(tags = "直裁MES接口")
@RestController
@RequestMapping("/mesItf")
@RequiredArgsConstructor
public class Cd90MesItfController {

    private final ICd90MesItfService cd90MesItfService;

    /**
     * 同步直裁库存。
     *
     * @param syncDataLogs 同步参数
     * @return 同步结果
     */
    @ApiOperation("同步直裁库存")
    @PostMapping("/syncMesCd90Stock")
    @AutoLoginLog
    public AjaxResult syncStock(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        if (StringUtils.isBlank(syncDataLogs.getFactoryCode())) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return cd90MesItfService.syncStock(syncDataLogs);
    }

    /**
     * 同步直裁库排状态。
     *
     * @param syncDataLogs 同步参数
     * @return 同步结果
     */
    @ApiOperation("同步直裁库排状态")
    @PostMapping("/syncCd90StorageLaneLimit")
    @AutoLoginLog
    public AjaxResult syncStorageLaneLimit(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        if (StringUtils.isBlank(syncDataLogs.getFactoryCode())) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return cd90MesItfService.syncStorageLaneLimit(syncDataLogs);
    }

    /**
     * 同步直裁自动滚动目标班次库存。
     *
     * @param request 目标库存日期、班次和开始时间
     * @return 同步结果
     */
    @ApiOperation("同步直裁自动滚动班次库存")
    @PostMapping("/syncCd90ShiftStock")
    @AutoLoginLog
    public AjaxResult syncShiftStock(@RequestBody MesShiftStockSyncRequest request) {
        return this.cd90MesItfService.syncShiftStock(request);
    }

    /**
     * 同步直裁每日三班完成量。
     *
     * @param syncDataLogs 同步参数
     * @return 同步结果
     */
    @ApiOperation("同步直裁每日三班完成量")
    @PostMapping("/syncCd90ClassShiftFinishQty")
    @AutoLoginLog
    public AjaxResult syncClassShiftFinishQty(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        if (StringUtils.isBlank(syncDataLogs.getFactoryCode())) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return cd90MesItfService.syncClassShiftFinishQty(syncDataLogs);
    }
}

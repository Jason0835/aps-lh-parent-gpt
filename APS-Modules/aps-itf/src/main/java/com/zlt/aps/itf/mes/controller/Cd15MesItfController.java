package com.zlt.aps.itf.mes.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.autoLogin.loginUtils.annotation.AutoLoginLog;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.itf.mes.service.ICd15MesItfService;
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
 * 斜裁MES接口控制器。
 */
@Api(tags = "斜裁MES接口")
@RestController
@RequestMapping("/mesItf")
@RequiredArgsConstructor
public class Cd15MesItfController {

    private final ICd15MesItfService cd15MesItfService;

    /** 同步斜裁库存。 */
    @ApiOperation("同步斜裁库存")
    @PostMapping("/syncMesCd15Stock")
    @AutoLoginLog
    public AjaxResult syncStock(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        if (StringUtils.isBlank(syncDataLogs.getFactoryCode())) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return this.cd15MesItfService.syncStock(syncDataLogs);
    }

    /** 同步斜裁库排状态。 */
    @ApiOperation("同步斜裁库排状态")
    @PostMapping("/syncCd15StorageLaneLimit")
    @AutoLoginLog
    public AjaxResult syncStorageLaneLimit(@RequestBody AuxReqSyncDataLogs syncDataLogs) {
        if (StringUtils.isBlank(syncDataLogs.getFactoryCode())) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return this.cd15MesItfService.syncStorageLaneLimit(syncDataLogs);
    }

    /** 同步斜裁自动滚动目标班次库存。 */
    @ApiOperation("同步斜裁自动滚动班次库存")
    @PostMapping("/syncCd15ShiftStock")
    @AutoLoginLog
    public AjaxResult syncShiftStock(@RequestBody MesShiftStockSyncRequest request) {
        return this.cd15MesItfService.syncShiftStock(request);
    }
}

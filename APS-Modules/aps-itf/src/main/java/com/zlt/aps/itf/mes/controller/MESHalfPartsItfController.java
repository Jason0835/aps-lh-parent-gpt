package com.zlt.aps.itf.mes.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.itf.mes.service.IMesItfDjService;
import com.zlt.aps.itf.mes.service.IMesItfNcService;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.itf.vo.MesDjScheduleResult;
import com.zlt.aps.itf.vo.MesNcScheduleResult;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * Copyright (c) 2022, All rights reserved。 文件名称：MesItfController.java 描 述：MES接口
 * 控制层类：....
 *
 * @author zlt
 * @version 1.0
 *          <p>
 *          修改记录： 修改时间：... 修 改 人：zlt 修改内容：...
 * @date 2026-07-23
 */
@Api(tags = "MES半部件接口")
@RestController
@RequestMapping("/mesHalfPartsItf")
public class MESHalfPartsItfController {

    @Autowired
    private IMesItfDjService mesItfDjService;

    @Autowired
    private IMesItfNcService mesItfNcService;

    /**
     * 同步垫胶库存
     *
     * @param syncDataLogs
     * @return 结果
     */
    @ApiOperation("同步垫胶库存")
    @PostMapping("/syncDjStock")
    public AjaxResult syncDjStock() {
        AuxReqSyncDataLogs syncDataLogs = new AuxReqSyncDataLogs();
        return mesItfDjService.syncStock(syncDataLogs);
    }

    /**
     * 同步内衬库存
     *
     * @param syncDataLogs
     * @return 结果
     */
    @ApiOperation("同步内衬库存")
    @PostMapping("/syncNcStock")
    public AjaxResult syncNcStock() {
        AuxReqSyncDataLogs syncDataLogs = new AuxReqSyncDataLogs();
        return mesItfNcService.syncStock(syncDataLogs);
    }

    @ApiOperation("下发内衬排程")
    @PostMapping("/issueNcScheduleResult")
    public AjaxResult issueNcScheduleResult(@RequestBody MesNcScheduleResult mesNcScheduleResult) {
        Long[] ids = mesNcScheduleResult.getIds();
        String factoryCode = mesNcScheduleResult.getFactoryCode();
        String companyCode = mesNcScheduleResult.getCompanyCode();
        return mesItfNcService.issueNcScheduleResult(ids, factoryCode, companyCode);
    }

    @ApiOperation("下发垫胶排程")
    @PostMapping("/issueDjScheduleResult")
    public AjaxResult issueDjScheduleResult(@RequestBody MesDjScheduleResult mesDjScheduleResult) {
        Long[] ids = mesDjScheduleResult.getIds();
        String factoryCode = mesDjScheduleResult.getFactoryCode();
        String companyCode = mesDjScheduleResult.getCompanyCode();
        return mesItfDjService.issueDjScheduleResult(ids, factoryCode, companyCode);
    }
}

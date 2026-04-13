package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.entity.CxDayFinishQty;
import com.zlt.aps.cx.api.domain.entity.CxMachineOnlineInfo;
import com.zlt.aps.cx.api.domain.entity.CxMesStock;
import com.zlt.aps.cx.api.domain.entity.CxScheFinishQty;
import com.zlt.aps.cx.api.domain.entity.CxStructureTreadConfig;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(contextId = "ICxMesSyncRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:/cx}")
public interface ICxMesSyncRemoteService {

    @ApiOperation("批量删除成型在机信息")
    @PostMapping("/mesSync/deleteMachineOnlineInfo")
    AjaxResult deleteMachineOnlineInfo(@RequestParam("factoryCode") String factoryCode);

    @ApiOperation("批量保存成型在机信息")
    @PostMapping("/mesSync/saveMachineOnlineInfoBatch")
    AjaxResult saveMachineOnlineInfoBatch(@RequestBody List<CxMachineOnlineInfo> list);

    @ApiOperation("批量保存结构整车胎面配置")
    @PostMapping("/mesSync/saveStructureTreadConfigBatch")
    AjaxResult saveStructureTreadConfigBatch(@RequestBody List<CxStructureTreadConfig> list);

    @ApiOperation("查询结构整车胎面配置已存在数据")
    @PostMapping("/mesSync/selectStructureTreadConfigExists")
    List<CxStructureTreadConfig> selectStructureTreadConfigExists(@RequestBody List<CxStructureTreadConfig> list);

    @ApiOperation("批量删除生胎库存")
    @PostMapping("/mesSync/deleteMesStock")
    AjaxResult deleteMesStock(@RequestParam("factoryCode") String factoryCode);

    @ApiOperation("批量保存生胎库存")
    @PostMapping("/mesSync/saveMesStockBatch")
    AjaxResult saveMesStockBatch(@RequestBody List<CxMesStock> list);

    @ApiOperation("批量保存成型排程完成量")
    @PostMapping("/mesSync/saveScheFinishQtyBatch")
    AjaxResult saveScheFinishQtyBatch(@RequestBody List<CxScheFinishQty> list);

    @ApiOperation("查询成型排程完成量已存在数据")
    @PostMapping("/mesSync/selectScheFinishQtyExists")
    List<CxScheFinishQty> selectScheFinishQtyExists(@RequestBody List<CxScheFinishQty> list);

    @ApiOperation("批量保存成型排程日完成量")
    @PostMapping("/mesSync/saveDayFinishQtyBatch")
    AjaxResult saveDayFinishQtyBatch(@RequestBody List<CxDayFinishQty> list);

    @ApiOperation("查询成型排程日完成量已存在数据")
    @PostMapping("/mesSync/selectDayFinishQtyExists")
    List<CxDayFinishQty> selectDayFinishQtyExists(@RequestBody List<CxDayFinishQty> list);
}

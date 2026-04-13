package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.lh.api.domain.entity.LhMoldAlterPlanFinish;
import com.zlt.aps.lh.api.domain.entity.LhMouldCleanWarn;
import com.zlt.aps.lh.api.domain.entity.LhRepairCapsule;
import com.zlt.aps.lh.api.domain.entity.LhScheFinishQty;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(contextId = "ILhMesSyncRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhMesSyncRemoteService {

    @ApiOperation("批量删除硫化在机信息")
    @PostMapping("/mesSync/deleteMachineOnlineInfo")
    AjaxResult deleteMachineOnlineInfo(@RequestParam("factoryCode") String factoryCode);

    @ApiOperation("批量保存硫化在机信息")
    @PostMapping("/mesSync/saveMachineOnlineInfoBatch")
    AjaxResult saveMachineOnlineInfoBatch(@RequestBody List<LhMachineOnlineInfo> list);

    @ApiOperation("批量删除胶囊已使用次数")
    @PostMapping("/mesSync/deleteRepairCapsule")
    AjaxResult deleteRepairCapsule(@RequestParam("factoryCode") String factoryCode);

    @ApiOperation("批量保存胶囊已使用次数")
    @PostMapping("/mesSync/saveRepairCapsuleBatch")
    
    AjaxResult saveRepairCapsuleBatch(@RequestBody List<LhRepairCapsule> list);

    @ApiOperation("批量保存模具清洗预警")
    @PostMapping("/mesSync/saveMouldCleanWarnBatch")
    AjaxResult saveMouldCleanWarnBatch(@RequestBody List<LhMouldCleanWarn> list);

    @ApiOperation("查询模具清洗预警已存在数据")
    @PostMapping("/mesSync/selectMouldCleanWarnExists")
    List<LhMouldCleanWarn> selectMouldCleanWarnExists(@RequestBody List<LhMouldCleanWarn> list);

    @ApiOperation("批量保存硫化排程完成量")
    @PostMapping("/mesSync/saveScheFinishQtyBatch")
    AjaxResult saveScheFinishQtyBatch(@RequestBody List<LhScheFinishQty> list);

    @ApiOperation("查询硫化排程完成量已存在数据")
    @PostMapping("/mesSync/selectScheFinishQtyExists")
    List<LhScheFinishQty> selectScheFinishQtyExists(@RequestBody List<LhScheFinishQty> list);

    @ApiOperation("批量保存硫化排程日完成量")
    @PostMapping("/mesSync/saveDayFinishQtyBatch")
    AjaxResult saveDayFinishQtyBatch(@RequestBody List<LhDayFinishQty> list);

    @ApiOperation("查询硫化排程日完成量已存在数据")
    @PostMapping("/mesSync/selectDayFinishQtyExists")
    List<LhDayFinishQty> selectDayFinishQtyExists(@RequestBody List<LhDayFinishQty> list);

    @ApiOperation("批量保存模具交替计划完成回报")
    @PostMapping("/mesSync/saveMoldAlterPlanFinishBatch")
    AjaxResult saveMoldAlterPlanFinishBatch(@RequestBody List<LhMoldAlterPlanFinish> list);

    @ApiOperation("查询模具交替计划完成回报已存在数据")
    @PostMapping("/mesSync/selectMoldAlterPlanFinishExists")
    List<LhMoldAlterPlanFinish> selectMoldAlterPlanFinishExists(@RequestBody List<LhMoldAlterPlanFinish> list);
}

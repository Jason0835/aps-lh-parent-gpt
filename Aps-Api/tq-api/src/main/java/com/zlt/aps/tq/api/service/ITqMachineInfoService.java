package com.zlt.aps.tq.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "iTqMachineInfoService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tq:tq}")
public interface ITqMachineInfoService {

    @PostMapping("/tqMachineInfo/list")
    TableDataInfo list(@RequestBody TqMachineInfo machineInfo);

    @PostMapping("/tqMachineInfo/save")
    AjaxResult save(@Validated @RequestBody TqMachineInfo machineInfo);

    @PostMapping("/tqMachineInfo/delete/{ids}")
    AjaxResult removeByIds(@PathVariable("ids") List<Long> ids);

    @GetMapping("/tqMachineInfo/{id}")
    TqMachineInfo getInfo(@PathVariable("id") Long id);

    @PostMapping("/tqMachineInfo/checkUnique")
    String checkUnique(@Validated @RequestBody TqMachineInfo machineInfo);

    @PostMapping("/tqMachineInfo/exportData/{fileName}")
    byte[] exportData(@RequestBody TqMachineInfo machineInfo, @PathVariable("fileName") String fileName);

    @PostMapping("/tqMachineInfo/listMachineInfo")
    List<TqMachineInfo> listMachineInfo(@RequestBody TqMachineInfo machineInfo);

    @PostMapping("/tqMachineInfo/listEnabledMachines")
    List<TqMachineInfo> listEnabledMachines();

    @PostMapping("/tqMachineInfo/importData")
    @ApiOperation("导入胎圈机台信息")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 导出胎圈机台列表
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/tqMachineInfo/exportList")
    List<TqMachineInfo> exportList(@RequestBody TqMachineInfo machineInfo);
}

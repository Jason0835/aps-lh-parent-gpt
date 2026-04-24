package com.zlt.aps.tq.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.entity.TqSpecifyMachine;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "iTqSpecifyMachineService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.tq:tq}")
public interface ITqSpecifyMachineService {

    @PostMapping("/tqSpecifyMachine/list")
    @ApiOperation("查询定点机台列表")
    TableDataInfo list(@RequestBody TqSpecifyMachine entity);

    @GetMapping("/tqSpecifyMachine/{id}")
    @ApiOperation("获取定点机台信息")
    TqSpecifyMachine getInfo(@PathVariable("id") Long id);

    @PostMapping("/tqSpecifyMachine/save")
    @ApiOperation("保存定点机台信息（id为空则新增，id不为空则修改）")
    AjaxResult save(@RequestBody TqSpecifyMachine entity);

    @PostMapping("/tqSpecifyMachine/delete/{ids}")
    @ApiOperation("批量删除定点机台信息(逻辑删)")
    AjaxResult removeByIds(@PathVariable("ids") List<Long> ids);

    @PostMapping("/tqSpecifyMachine/deleteAll")
    @ApiOperation("删除全部定点机台信息(逻辑删)")
    AjaxResult deleteAll();

    @PostMapping("/tqSpecifyMachine/exportData/{fileName}")
    @ApiOperation("导出定点机台信息")
    byte[] exportData(@RequestBody TqSpecifyMachine entity, @PathVariable("fileName") String fileName);

    @PostMapping("/tqSpecifyMachine/exportList")
    @ApiOperation("导出定点机台列表")
    List<TqSpecifyMachine> exportList(@RequestBody TqSpecifyMachine entity);

    @PostMapping("/tqSpecifyMachine/importData")
    @ApiOperation("导入胎圈定点机台信息")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}

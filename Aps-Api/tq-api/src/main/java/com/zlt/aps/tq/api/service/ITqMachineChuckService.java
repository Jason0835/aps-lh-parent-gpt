package com.zlt.aps.tq.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.entity.TqMachineChuck;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITqMachineChuckService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tq:tq}")
public interface ITqMachineChuckService {

    @PostMapping("/tqMachineChuck/list")
    @ApiOperation("查询胎圈机台寸口对应列表")
    TableDataInfo list(@RequestBody TqMachineChuck entity);

    @GetMapping(value = "/tqMachineChuck/{id}")
    @ApiOperation("获取胎圈机台寸口对应详细信息")
    TqMachineChuck getInfo(@PathVariable("id") Long id);

    @PostMapping("/tqMachineChuck/save")
    @ApiOperation("保存胎圈机台寸口对应（id为空则新增，id不为空则修改）")
    AjaxResult save(@RequestBody TqMachineChuck entity);

    @PostMapping("/tqMachineChuck/checkUnique")
    @ApiOperation("校验机台编码+寸口编码组合唯一性")
    String checkUnique(@RequestBody TqMachineChuck entity);

    @PostMapping("/tqMachineChuck/delete/{ids}")
    @ApiOperation("删除胎圈机台寸口对应")
    AjaxResult removeByIds(@PathVariable("ids") List<Long> ids);

    @PostMapping("/tqMachineChuck/exportData/{fileName}")
    @ApiOperation("导出胎圈机台寸口对应")
    byte[] exportData(@RequestBody TqMachineChuck entity, @PathVariable("fileName") String fileName);

    @PostMapping("/tqMachineChuck/exportList")
    @ApiOperation("导出胎圈机台寸口对应列表")
    List<TqMachineChuck> exportList(@RequestBody TqMachineChuck entity);

    @PostMapping("/tqMachineChuck/importData")
    @ApiOperation("导入胎圈机台寸口对应")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    @PostMapping("/tqMachineChuck/deleteAll")
    @ApiOperation("删除全部(逻辑删)")
    AjaxResult deleteAll();
}

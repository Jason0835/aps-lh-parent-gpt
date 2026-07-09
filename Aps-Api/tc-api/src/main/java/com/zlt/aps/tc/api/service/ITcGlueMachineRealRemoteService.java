package com.zlt.aps.tc.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.entity.TcGlueMachineReal;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITcGlueMachineRealRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:/tc}")
public interface ITcGlueMachineRealRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tcGlueMachineReal/list")
    TableDataInfo list(@RequestBody TcGlueMachineReal queryVO);

    @ApiOperation("保存")
    @PostMapping("/tcGlueMachineReal/save")
    AjaxResult save(TcGlueMachineReal tcGlueMachineReal);

    @ApiOperation("删除")
    @DeleteMapping("/tcGlueMachineReal/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tcGlueMachineReal/{id}")
    TcGlueMachineReal getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tcGlueMachineReal/checkUnique")
    String checkUnique(@RequestBody TcGlueMachineReal tcGlueMachineRealVO);

    @ApiOperation("导出列表")
    @PostMapping("/tcGlueMachineReal/exportData/{fileName}")
    byte[] exportData(@RequestBody TcGlueMachineReal queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tcGlueMachineReal/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
package com.zlt.aps.tm.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.entity.TmGlueMachineReal;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITmGlueMachineRealRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tm:/tm}")
public interface ITmGlueMachineRealRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tmGlueMachineReal/list")
    TableDataInfo list(@RequestBody TmGlueMachineReal queryVO);

    @ApiOperation("保存")
    @PostMapping("/tmGlueMachineReal/save")
    AjaxResult save(TmGlueMachineReal tmGlueMachineReal);

    @ApiOperation("删除")
    @DeleteMapping("/tmGlueMachineReal/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tmGlueMachineReal/{id}")
    TmGlueMachineReal getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tmGlueMachineReal/checkUnique")
    String checkUnique(@RequestBody TmGlueMachineReal tmGlueMachineRealVO);

    @ApiOperation("导出列表")
    @PostMapping("/tmGlueMachineReal/exportData/{fileName}")
    byte[] exportData(@RequestBody TmGlueMachineReal queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tmGlueMachineReal/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}

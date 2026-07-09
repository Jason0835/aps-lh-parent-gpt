package com.zlt.aps.tc.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.entity.TcMachineSpeed;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITcMachineSpeedRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:/tc}")
public interface ITcMachineSpeedRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tcMachineSpeed/list")
    TableDataInfo list(@RequestBody TcMachineSpeed queryVO);

    @ApiOperation("保存")
    @PostMapping("/tcMachineSpeed/save")
    AjaxResult save(TcMachineSpeed tcMachineSpeed);

    @ApiOperation("删除")
    @DeleteMapping("/tcMachineSpeed/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tcMachineSpeed/{id}")
    TcMachineSpeed getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tcMachineSpeed/checkUnique")
    String checkUnique(@RequestBody TcMachineSpeed tcMachineSpeedVO);

    @ApiOperation("导出列表")
    @PostMapping("/tcMachineSpeed/exportData/{fileName}")
    byte[] exportData(@RequestBody TcMachineSpeed queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tcMachineSpeed/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
package com.zlt.aps.tc.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.entity.TcDjSharedMachine;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITcDjSharedMachineRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:/tc}")
public interface ITcDjSharedMachineRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tcDjSharedMachine/list")
    TableDataInfo list(@RequestBody TcDjSharedMachine queryVO);

    @ApiOperation("保存")
    @PostMapping("/tcDjSharedMachine/save")
    AjaxResult save(TcDjSharedMachine tcDjSharedMachine);

    @ApiOperation("删除")
    @DeleteMapping("/tcDjSharedMachine/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tcDjSharedMachine/{id}")
    TcDjSharedMachine getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tcDjSharedMachine/checkUnique")
    String checkUnique(@RequestBody TcDjSharedMachine tcDjSharedMachineVO);

    @ApiOperation("导出列表")
    @PostMapping("/tcDjSharedMachine/exportData/{fileName}")
    byte[] exportData(@RequestBody TcDjSharedMachine queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tcDjSharedMachine/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
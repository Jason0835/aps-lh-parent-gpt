package com.zlt.aps.tm.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.entity.TmMachineSpeed;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITmMachineSpeedRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tm:/tm}")
public interface ITmMachineSpeedRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tmMachineSpeed/list")
    TableDataInfo list(@RequestBody TmMachineSpeed queryVO);

    @ApiOperation("保存")
    @PostMapping("/tmMachineSpeed/save")
    AjaxResult save(TmMachineSpeed tmMachineSpeed);

    @ApiOperation("删除")
    @DeleteMapping("/tmMachineSpeed/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tmMachineSpeed/{id}")
    TmMachineSpeed getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tmMachineSpeed/checkUnique")
    String checkUnique(@RequestBody TmMachineSpeed tmMachineSpeedVO);

    @ApiOperation("导出列表")
    @PostMapping("/tmMachineSpeed/exportData/{fileName}")
    byte[] exportData(@RequestBody TmMachineSpeed queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tmMachineSpeed/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}

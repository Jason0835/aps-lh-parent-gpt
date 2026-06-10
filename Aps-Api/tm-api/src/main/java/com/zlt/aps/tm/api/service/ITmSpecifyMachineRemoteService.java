package com.zlt.aps.tm.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.entity.TmSpecifyMachine;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITmSpecifyMachineRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tm:/tm}")
public interface ITmSpecifyMachineRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tmSpecifyMachine/list")
    TableDataInfo list(@RequestBody TmSpecifyMachine queryVO);

    @ApiOperation("保存")
    @PostMapping("/tmSpecifyMachine/save")
    AjaxResult save(TmSpecifyMachine tmSpecifyMachine);

    @ApiOperation("删除")
    @DeleteMapping("/tmSpecifyMachine/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tmSpecifyMachine/{id}")
    TmSpecifyMachine getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tmSpecifyMachine/checkUnique")
    String checkUnique(@RequestBody TmSpecifyMachine tmSpecifyMachineVO);

    @ApiOperation("导出列表")
    @PostMapping("/tmSpecifyMachine/exportData/{fileName}")
    byte[] exportData(@RequestBody TmSpecifyMachine queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tmSpecifyMachine/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}

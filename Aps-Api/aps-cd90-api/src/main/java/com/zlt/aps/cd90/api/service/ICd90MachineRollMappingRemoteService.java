package com.zlt.aps.cd90.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineRollMapping;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ICd90MachineRollMappingRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:/cd90}")
public interface ICd90MachineRollMappingRemoteService {
    @ApiOperation("查询列表")
    @PostMapping("/cd90MachineRollMapping/list")
    TableDataInfo list(@RequestBody Cd90MachineRollMapping queryVO);

    @ApiOperation("获取详情")
    @GetMapping("/cd90MachineRollMapping/getInfo/{id}")
    Cd90MachineRollMapping getInfo(@PathVariable("id") Long id);

    @ApiOperation("新增")
    @PostMapping("/cd90MachineRollMapping/add")
    AjaxResult add(@RequestBody Cd90MachineRollMapping entity);

    @ApiOperation("编辑")
    @PostMapping("/cd90MachineRollMapping/edit")
    AjaxResult edit(@RequestBody Cd90MachineRollMapping entity);

    @ApiOperation("删除")
    @PostMapping("/cd90MachineRollMapping/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("校验唯一性")
    @PostMapping("/cd90MachineRollMapping/checkUnique")
    String checkUnique(@RequestBody Cd90MachineRollMapping entity);

    @ApiOperation("导出")
    @PostMapping("/cd90MachineRollMapping/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd90MachineRollMapping queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入")
    @PostMapping("/cd90MachineRollMapping/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
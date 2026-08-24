package com.zlt.aps.tq.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.entity.TqMachineSpecSpeed;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITqMachineSpecSpeedService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tq:tq}")
public interface ITqMachineSpecSpeedService {

    @PostMapping("/tqMachineSpecSpeed/list")
    @ApiOperation("查询胎圈机台生产速度列表")
    TableDataInfo list(@RequestBody TqMachineSpecSpeed entity);

    @GetMapping("/tqMachineSpecSpeed/{id}")
    @ApiOperation("获取胎圈机台生产速度详细信息")
    TqMachineSpecSpeed getInfo(@PathVariable("id") Long id);

    @PostMapping("/tqMachineSpecSpeed/save")
    @ApiOperation("保存胎圈机台生产速度信息（id为空则新增，id不为空则修改）")
    AjaxResult save(@RequestBody TqMachineSpecSpeed entity);

    @PostMapping("/tqMachineSpecSpeed/delete/{ids}")
    @ApiOperation("批量删除胎圈机台生产速度信息(逻辑删)")
    AjaxResult removeByIds(@PathVariable("ids") List<Long> ids);

    @PostMapping("/tqMachineSpecSpeed/exportData/{fileName}")
    @ApiOperation("导出胎圈机台生产速度信息")
    byte[] exportData(@RequestBody TqMachineSpecSpeed entity, @PathVariable("fileName") String fileName);

    @PostMapping("/tqMachineSpecSpeed/exportList")
    @ApiOperation("导出胎圈机台生产速度列表")
    List<TqMachineSpecSpeed> exportList(@RequestBody TqMachineSpecSpeed entity);

    @PostMapping("/tqMachineSpecSpeed/importData")
    @ApiOperation("导入胎圈机台生产速度信息")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    @PostMapping("/tqMachineSpecSpeed/checkUnique")
    @ApiOperation("校验机台编码+胎圈规格组合唯一性")
    String checkUnique(@RequestBody TqMachineSpecSpeed entity);
}

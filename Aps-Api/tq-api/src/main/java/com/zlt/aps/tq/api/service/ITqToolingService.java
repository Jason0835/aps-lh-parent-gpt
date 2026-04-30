package com.zlt.aps.tq.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.entity.TqTooling;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITqToolingService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tq:tq}")
public interface ITqToolingService {

    @PostMapping("/tqTooling/list")
    @ApiOperation("查询胎圈工装管理列表")
    TableDataInfo list(@RequestBody TqTooling entity);

    @GetMapping(value = "/tqTooling/{id}")
    @ApiOperation("获取胎圈工装管理详细信息")
    TqTooling getInfo(@PathVariable("id") Long id);

    @PostMapping("/tqTooling/save")
    @ApiOperation("保存胎圈工装管理（id为空则新增，id不为空则修改）")
    AjaxResult save(@RequestBody TqTooling entity);

    @PostMapping("/tqTooling/delete/{ids}")
    @ApiOperation("删除胎圈工装管理")
    AjaxResult removeByIds(@PathVariable("ids") List<Long> ids);

    @PostMapping("/tqTooling/exportData/{fileName}")
    @ApiOperation("导出胎圈工装管理")
    byte[] exportData(@RequestBody TqTooling entity, @PathVariable("fileName") String fileName);

    @PostMapping("/tqTooling/exportList")
    @ApiOperation("导出胎圈工装管理列表")
    List<TqTooling> exportList(@RequestBody TqTooling entity);

    @PostMapping("/tqTooling/importData")
    @ApiOperation("导入胎圈工装管理")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    @PostMapping("/tqTooling/deleteAll")
    @ApiOperation("删除全部(逻辑删)")
    AjaxResult deleteAll();

    @PostMapping("/tqTooling/listAllTooling")
    @ApiOperation("查询所有未删除的工装列表")
    AjaxResult listAllTooling();
}

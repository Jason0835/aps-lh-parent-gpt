package com.zlt.aps.tq.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.entity.TqToolingCartCapacity;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITqToolingCartCapacityService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tq:tq}")
public interface ITqToolingCartCapacityService {

    @PostMapping("/tqToolingCartCapacity/list")
    @ApiOperation("查询胎圈工装车容量管理列表")
    TableDataInfo list(@RequestBody TqToolingCartCapacity entity);

    @GetMapping(value = "/tqToolingCartCapacity/{id}")
    @ApiOperation("获取胎圈工装车容量管理详细信息")
    TqToolingCartCapacity getInfo(@PathVariable("id") Long id);

    @PostMapping("/tqToolingCartCapacity/save")
    @ApiOperation("保存胎圈工装车容量管理（id为空则新增，id不为空则修改）")
    AjaxResult save(@RequestBody TqToolingCartCapacity entity);

    @PostMapping("/tqToolingCartCapacity/delete/{ids}")
    @ApiOperation("删除胎圈工装车容量管理")
    AjaxResult removeByIds(@PathVariable("ids") List<Long> ids);

    @PostMapping("/tqToolingCartCapacity/exportData/{fileName}")
    @ApiOperation("导出胎圈工装车容量管理")
    byte[] exportData(@RequestBody TqToolingCartCapacity entity, @PathVariable("fileName") String fileName);

    @PostMapping("/tqToolingCartCapacity/exportList")
    @ApiOperation("导出胎圈工装车容量管理列表")
    List<TqToolingCartCapacity> exportList(@RequestBody TqToolingCartCapacity entity);

    @PostMapping("/tqToolingCartCapacity/importData")
    @ApiOperation("导入胎圈工装车容量管理")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    @PostMapping("/tqToolingCartCapacity/deleteAll")
    @ApiOperation("删除全部(逻辑删)")
    AjaxResult deleteAll();
}

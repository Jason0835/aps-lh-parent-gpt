package com.zlt.aps.tq.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.entity.TqMouthPlate;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITqMouthPlateService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.tq:tq}")
public interface ITqMouthPlateService {

    @PostMapping("/tqMouthPlate/list")
    @ApiOperation("查询胎圈口型板信息维护列表")
    TableDataInfo list(@RequestBody TqMouthPlate entity);

    @GetMapping(value = "/tqMouthPlate/{id}")
    @ApiOperation("获取胎圈口型板信息详细信息")
    TqMouthPlate getInfo(@PathVariable("id") Long id);

    @PostMapping("/tqMouthPlate/save")
    @ApiOperation("保存胎圈口型板信息（id为空则新增，id不为空则修改）")
    AjaxResult save(@RequestBody TqMouthPlate entity);

    @PostMapping("/tqMouthPlate/checkUnique")
    @ApiOperation("校验胎圈口型板信息唯一性")
    String checkUnique(@RequestBody TqMouthPlate entity);

    @PostMapping("/tqMouthPlate/delete/{ids}")
    @ApiOperation("删除胎圈口型板信息")
    AjaxResult removeByIds(@PathVariable("ids") List<Long> ids);

    @PostMapping("/tqMouthPlate/exportData/{fileName}")
    @ApiOperation("导出胎圈口型板信息")
    byte[] exportData(@RequestBody TqMouthPlate entity, @PathVariable("fileName") String fileName);

    @PostMapping("/tqMouthPlate/exportList")
    @ApiOperation("导出胎圈口型板列表")
    List<TqMouthPlate> exportList(@RequestBody TqMouthPlate entity);

    @PostMapping("/tqMouthPlate/importData")
    @ApiOperation("导入胎圈口型板信息")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    @PostMapping("/tqMouthPlate/deleteAll")
    @ApiOperation("删除全部(逻辑删)")
    AjaxResult deleteAll();
}

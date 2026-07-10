package com.zlt.aps.tc.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.entity.TcMouthPlate;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITcMouthPlateRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:/tc}")
public interface ITcMouthPlateRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tcMouthPlate/list")
    TableDataInfo list(@RequestBody TcMouthPlate queryVO);

    @ApiOperation("保存")
    @PostMapping("/tcMouthPlate/save")
    AjaxResult save(TcMouthPlate tcMouthPlate);

    @ApiOperation("删除")
    @DeleteMapping("/tcMouthPlate/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tcMouthPlate/{id}")
    TcMouthPlate getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tcMouthPlate/checkUnique")
    String checkUnique(@RequestBody TcMouthPlate tcMouthPlateVO);

    @ApiOperation("导出列表")
    @PostMapping("/tcMouthPlate/exportData/{fileName}")
    byte[] exportData(@RequestBody TcMouthPlate queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tcMouthPlate/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
package com.zlt.aps.tc.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.entity.TcGlueOrder;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITcGlueOrderRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:/tc}")
public interface ITcGlueOrderRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tcGlueOrder/list")
    TableDataInfo list(@RequestBody TcGlueOrder queryVO);

    @ApiOperation("保存")
    @PostMapping("/tcGlueOrder/save")
    AjaxResult save(TcGlueOrder tcGlueOrder);

    @ApiOperation("删除")
    @DeleteMapping("/tcGlueOrder/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tcGlueOrder/{id}")
    TcGlueOrder getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tcGlueOrder/checkUnique")
    String checkUnique(@RequestBody TcGlueOrder tcGlueOrderVO);

    @ApiOperation("导出列表")
    @PostMapping("/tcGlueOrder/exportData/{fileName}")
    byte[] exportData(@RequestBody TcGlueOrder queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tcGlueOrder/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
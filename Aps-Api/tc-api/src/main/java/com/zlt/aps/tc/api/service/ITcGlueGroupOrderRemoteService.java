package com.zlt.aps.tc.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.entity.TcGlueGroupOrder;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITcGlueGroupOrderRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:/tc}")
public interface ITcGlueGroupOrderRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tcGlueGroupOrder/list")
    TableDataInfo list(@RequestBody TcGlueGroupOrder queryVO);

    @ApiOperation("保存")
    @PostMapping("/tcGlueGroupOrder/save")
    AjaxResult save(TcGlueGroupOrder tcGlueGroupOrder);

    @ApiOperation("删除")
    @DeleteMapping("/tcGlueGroupOrder/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tcGlueGroupOrder/{id}")
    TcGlueGroupOrder getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tcGlueGroupOrder/checkUnique")
    String checkUnique(@RequestBody TcGlueGroupOrder tcGlueGroupOrderVO);

    @ApiOperation("导出列表")
    @PostMapping("/tcGlueGroupOrder/exportData/{fileName}")
    byte[] exportData(@RequestBody TcGlueGroupOrder queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tcGlueGroupOrder/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
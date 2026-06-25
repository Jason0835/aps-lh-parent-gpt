package com.zlt.aps.cd90.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90DepthConfig;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ICd90DepthConfigRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:/cd90}")
public interface ICd90DepthConfigRemoteService {
    @ApiOperation("查询列表")
    @PostMapping("/cd90DepthConfig/list")
    TableDataInfo list(@RequestBody Cd90DepthConfig queryVO);

    @ApiOperation("获取详情")
    @GetMapping("/cd90DepthConfig/getInfo/{id}")
    Cd90DepthConfig getInfo(@PathVariable("id") Long id);

    @ApiOperation("新增")
    @PostMapping("/cd90DepthConfig/add")
    AjaxResult add(@RequestBody Cd90DepthConfig entity);

    @ApiOperation("编辑")
    @PostMapping("/cd90DepthConfig/edit")
    AjaxResult edit(@RequestBody Cd90DepthConfig entity);

    @ApiOperation("删除")
    @PostMapping("/cd90DepthConfig/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("校验唯一性")
    @PostMapping("/cd90DepthConfig/checkUnique")
    String checkUnique(@RequestBody Cd90DepthConfig entity);

    @ApiOperation("校验范围交叉")
    @PostMapping("/cd90DepthConfig/checkRangeCross")
    String checkRangeCross(@RequestBody Cd90DepthConfig entity);

    @ApiOperation("导出")
    @PostMapping("/cd90DepthConfig/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd90DepthConfig queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入")
    @PostMapping("/cd90DepthConfig/importData")
    AjaxResult importData(@RequestBody ImportContext context, @RequestParam("updateSupport") boolean updateSupport);
}
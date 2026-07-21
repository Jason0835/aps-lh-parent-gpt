package com.zlt.aps.cd15.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15DepthConfig;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 斜裁备库班数与供成型机数配置 Feign 接口。
 */
@FeignClient(contextId = "ICd15DepthConfigRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd15:/cd15}")
public interface ICd15DepthConfigRemoteService {

    @ApiOperation("查询斜裁备库班数列表")
    @PostMapping("/cd15DepthConfig/list")
    TableDataInfo list(@RequestBody Cd15DepthConfig queryVO);

    @ApiOperation("获取斜裁备库班数详情")
    @GetMapping("/cd15DepthConfig/getInfo/{id}")
    Cd15DepthConfig getInfo(@PathVariable("id") Long id);

    @ApiOperation("新增斜裁备库班数")
    @PostMapping("/cd15DepthConfig/add")
    AjaxResult add(@RequestBody Cd15DepthConfig entity);

    @ApiOperation("编辑斜裁备库班数")
    @PostMapping("/cd15DepthConfig/edit")
    AjaxResult edit(@RequestBody Cd15DepthConfig entity);

    @ApiOperation("删除斜裁备库班数")
    @PostMapping("/cd15DepthConfig/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("校验斜裁备库班数唯一性")
    @PostMapping("/cd15DepthConfig/checkUnique")
    String checkUnique(@RequestBody Cd15DepthConfig entity);

    @ApiOperation("校验斜裁备库班数范围交叉")
    @PostMapping("/cd15DepthConfig/checkRangeCross")
    String checkRangeCross(@RequestBody Cd15DepthConfig entity);

    @ApiOperation("导出斜裁备库班数")
    @PostMapping("/cd15DepthConfig/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd15DepthConfig queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入斜裁备库班数")
    @PostMapping("/cd15DepthConfig/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}

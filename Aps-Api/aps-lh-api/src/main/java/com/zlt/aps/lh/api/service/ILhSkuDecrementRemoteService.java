package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhSkuDecrement;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SKU减量清单远程服务接口
 */
@FeignClient(contextId = "ILhSkuDecrementRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhSkuDecrementRemoteService {

    @ApiOperation("查询SKU减量清单列表")
    @PostMapping("/lhSkuDecrement/list")
    TableDataInfo list(@RequestBody LhSkuDecrement queryVO);

    @ApiOperation("获取SKU减量清单详情")
    @GetMapping("/lhSkuDecrement/{id}")
    LhSkuDecrement getInfo(@PathVariable("id") Long id);

    @ApiOperation("确认SKU减量")
    @PostMapping("/lhSkuDecrement/confirm")
    AjaxResult confirm(@RequestBody LhSkuDecrement entity);

    @ApiOperation("删除SKU减量清单")
    @DeleteMapping("/lhSkuDecrement/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("导出SKU减量清单")
    @PostMapping("/lhSkuDecrement/exportData/{fileName}")
    byte[] exportData(@RequestBody LhSkuDecrement entity, @PathVariable("fileName") String fileName);
}

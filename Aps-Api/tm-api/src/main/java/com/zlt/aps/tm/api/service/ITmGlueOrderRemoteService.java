package com.zlt.aps.tm.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.entity.TmGlueOrder;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITmGlueOrderRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tm:/tm}")
public interface ITmGlueOrderRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tmGlueOrder/list")
    TableDataInfo list(@RequestBody TmGlueOrder queryVO);

    @ApiOperation("保存")
    @PostMapping("/tmGlueOrder/save")
    AjaxResult save(TmGlueOrder tmGlueOrder);

    @ApiOperation("删除")
    @DeleteMapping("/tmGlueOrder/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tmGlueOrder/{id}")
    TmGlueOrder getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tmGlueOrder/checkUnique")
    String checkUnique(@RequestBody TmGlueOrder tmGlueOrderVO);

    @ApiOperation("导出列表")
    @PostMapping("/tmGlueOrder/exportData/{fileName}")
    byte[] exportData(@RequestBody TmGlueOrder queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tmGlueOrder/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}

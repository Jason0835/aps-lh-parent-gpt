package com.zlt.aps.tm.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.entity.TmGlueGroupOrder;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITmGlueGroupOrderRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tm:/tm}")
public interface ITmGlueGroupOrderRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tmGlueGroupOrder/list")
    TableDataInfo list(@RequestBody TmGlueGroupOrder queryVO);

    @ApiOperation("保存")
    @PostMapping("/tmGlueGroupOrder/save")
    AjaxResult save(TmGlueGroupOrder tmGlueGroupOrder);

    @ApiOperation("删除")
    @DeleteMapping("/tmGlueGroupOrder/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tmGlueGroupOrder/{id}")
    TmGlueGroupOrder getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tmGlueGroupOrder/checkUnique")
    String checkUnique(@RequestBody TmGlueGroupOrder tmGlueGroupOrderVO);

    @ApiOperation("导出列表")
    @PostMapping("/tmGlueGroupOrder/exportData/{fileName}")
    byte[] exportData(@RequestBody TmGlueGroupOrder queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tmGlueGroupOrder/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}

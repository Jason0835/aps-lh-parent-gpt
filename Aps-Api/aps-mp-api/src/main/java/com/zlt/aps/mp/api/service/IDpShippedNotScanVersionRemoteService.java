package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.DpShippedNotScanVersion;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "IDpShippedNotScanVersionRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IDpShippedNotScanVersionRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/dpShippedNotScanVersion/list")
    TableDataInfo list(@RequestBody DpShippedNotScanVersion queryVO);

    @ApiOperation("保存")
    @PostMapping("/dpShippedNotScanVersion/save")
    AjaxResult save(@RequestBody DpShippedNotScanVersion entity);

    @ApiOperation("删除")
    @DeleteMapping("/dpShippedNotScanVersion/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/dpShippedNotScanVersion/{id}")
    DpShippedNotScanVersion getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/dpShippedNotScanVersion/checkUnique")
    String checkUnique(@RequestBody DpShippedNotScanVersion entity);

    @ApiOperation("导出列表")
    @PostMapping("/dpShippedNotScanVersion/exportData/{fileName}")
    byte[] exportData(@RequestBody DpShippedNotScanVersion queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/dpShippedNotScanVersion/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    @ApiOperation("查询需求计划版本号")
    @PostMapping("/dpShippedNotScanVersion/findMonthPlanVersion")
    AjaxResult findMonthPlanVersion(@RequestBody DpShippedNotScanVersion queryCondition);

    @ApiOperation("生成已出库未扫描版本")
    @PostMapping("/dpShippedNotScanVersion/generate")
    AjaxResult generate(@RequestBody DpShippedNotScanVersion queryCondition);
}

package com.zlt.aps.gdyy.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gdyy.api.domain.entity.GdyyShiftConfig;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢带压延班次配置 Feign 接口。
 */
@FeignClient(contextId = "IGdyyShiftConfigRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gdyy:gdyy}")
public interface IGdyyShiftConfigRemoteService {

    @ApiOperation("查询钢带压延班次配置列表")
    @PostMapping("/gdyyShiftConfig/list")
    TableDataInfo list(@RequestBody GdyyShiftConfig queryVO);

    @ApiOperation("获取钢带压延班次配置详情")
    @GetMapping("/gdyyShiftConfig/getInfo/{id}")
    GdyyShiftConfig getInfo(@PathVariable("id") Long id);

    @ApiOperation("新增钢带压延班次配置")
    @PostMapping("/gdyyShiftConfig/add")
    AjaxResult add(@RequestBody GdyyShiftConfig shiftConfig);

    @ApiOperation("编辑钢带压延班次配置")
    @PostMapping("/gdyyShiftConfig/edit")
    AjaxResult edit(@RequestBody GdyyShiftConfig shiftConfig);

    @ApiOperation("删除钢带压延班次配置")
    @PostMapping("/gdyyShiftConfig/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("校验钢带压延班次配置唯一性")
    @PostMapping("/gdyyShiftConfig/checkUnique")
    String checkUnique(@RequestBody GdyyShiftConfig shiftConfig);

    @ApiOperation("导出钢带压延班次配置")
    @PostMapping("/gdyyShiftConfig/exportData/{fileName}")
    byte[] exportData(@RequestBody GdyyShiftConfig queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入钢带压延班次配置")
    @PostMapping("/gdyyShiftConfig/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    @ApiOperation("修改钢带压延班次启用状态")
    @PostMapping("/gdyyShiftConfig/changeStatus")
    AjaxResult changeStatus(@RequestBody GdyyShiftConfig shiftConfig);
}

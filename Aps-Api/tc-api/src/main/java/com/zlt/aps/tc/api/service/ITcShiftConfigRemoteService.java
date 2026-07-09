package com.zlt.aps.tc.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.entity.TcShiftConfig;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITcShiftConfigRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:/tc}")
public interface ITcShiftConfigRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tcShiftConfig/list")
    TableDataInfo list(@RequestBody TcShiftConfig queryVO);

    @ApiOperation("保存")
    @PostMapping("/tcShiftConfig/save")
    AjaxResult save(TcShiftConfig tcShiftConfig);

    @ApiOperation("删除")
    @DeleteMapping("/tcShiftConfig/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tcShiftConfig/{id}")
    TcShiftConfig getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tcShiftConfig/checkUnique")
    String checkUnique(@RequestBody TcShiftConfig tcShiftConfigVO);

    @ApiOperation("导出列表")
    @PostMapping("/tcShiftConfig/exportData/{fileName}")
    byte[] exportData(@RequestBody TcShiftConfig queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tcShiftConfig/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
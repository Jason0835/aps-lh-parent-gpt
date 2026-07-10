package com.zlt.aps.tc.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.entity.TcLossSetting;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITcLossSettingRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:/tc}")
public interface ITcLossSettingRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tcLossSetting/list")
    TableDataInfo list(@RequestBody TcLossSetting queryVO);

    @ApiOperation("保存")
    @PostMapping("/tcLossSetting/save")
    AjaxResult save(TcLossSetting tcLossSetting);

    @ApiOperation("删除")
    @DeleteMapping("/tcLossSetting/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tcLossSetting/{id}")
    TcLossSetting getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tcLossSetting/checkUnique")
    String checkUnique(@RequestBody TcLossSetting tcLossSettingVO);

    @ApiOperation("导出列表")
    @PostMapping("/tcLossSetting/exportData/{fileName}")
    byte[] exportData(@RequestBody TcLossSetting queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tcLossSetting/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
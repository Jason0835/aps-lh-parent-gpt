package com.zlt.aps.tm.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.entity.TmShiftConfig;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITmShiftConfigRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tm:/tm}")
public interface ITmShiftConfigRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tmShiftConfig/list")
    TableDataInfo list(@RequestBody TmShiftConfig queryVO);

    @ApiOperation("保存")
    @PostMapping("/tmShiftConfig/save")
    AjaxResult save(TmShiftConfig tmShiftConfig);

    @ApiOperation("删除")
    @DeleteMapping("/tmShiftConfig/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tmShiftConfig/{id}")
    TmShiftConfig getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tmShiftConfig/checkUnique")
    String checkUnique(@RequestBody TmShiftConfig tmShiftConfigVO);

    @ApiOperation("导出列表")
    @PostMapping("/tmShiftConfig/exportData/{fileName}")
    byte[] exportData(@RequestBody TmShiftConfig queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tmShiftConfig/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}

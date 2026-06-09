package com.zlt.aps.tm.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.entity.TmMouthPlate;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITmMouthPlateRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tm:/tm}")
public interface ITmMouthPlateRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tmMouthPlate/list")
    TableDataInfo list(@RequestBody TmMouthPlate queryVO);

    @ApiOperation("保存")
    @PostMapping("/tmMouthPlate/save")
    AjaxResult save(TmMouthPlate tmMouthPlate);

    @ApiOperation("删除")
    @DeleteMapping("/tmMouthPlate/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tmMouthPlate/{id}")
    TmMouthPlate getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tmMouthPlate/checkUnique")
    String checkUnique(@RequestBody TmMouthPlate tmMouthPlateVO);

    @ApiOperation("导出列表")
    @PostMapping("/tmMouthPlate/exportData/{fileName}")
    byte[] exportData(@RequestBody TmMouthPlate queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tmMouthPlate/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}

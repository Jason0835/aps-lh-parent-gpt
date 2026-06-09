package com.zlt.aps.tm.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.entity.TmCurlRoll;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITmCurlRollRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tm:/tm}")
public interface ITmCurlRollRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tmCurlRoll/list")
    TableDataInfo list(@RequestBody TmCurlRoll queryVO);

    @ApiOperation("保存")
    @PostMapping("/tmCurlRoll/save")
    AjaxResult save(TmCurlRoll tmCurlRoll);

    @ApiOperation("删除")
    @DeleteMapping("/tmCurlRoll/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tmCurlRoll/{id}")
    TmCurlRoll getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tmCurlRoll/checkUnique")
    String checkUnique(@RequestBody TmCurlRoll tmCurlRollVO);

    @ApiOperation("导出列表")
    @PostMapping("/tmCurlRoll/exportData/{fileName}")
    byte[] exportData(@RequestBody TmCurlRoll queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tmCurlRoll/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}

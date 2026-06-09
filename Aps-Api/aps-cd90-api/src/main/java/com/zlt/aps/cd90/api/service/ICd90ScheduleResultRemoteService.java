package com.zlt.aps.cd90.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ICd90ScheduleResultRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:/cd90}")
public interface ICd90ScheduleResultRemoteService {
    @ApiOperation("查询列表")
    @PostMapping("/cd90ScheduleResult/list")
    TableDataInfo list(@RequestBody Cd90ScheduleResult queryVO);

    @ApiOperation("获取详情")
    @GetMapping("/cd90ScheduleResult/getInfo/{id}")
    Cd90ScheduleResult getInfo(@PathVariable("id") Long id);

    @ApiOperation("删除")
    @PostMapping("/cd90ScheduleResult/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("导出")
    @PostMapping("/cd90ScheduleResult/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd90ScheduleResult queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入")
    @PostMapping("/cd90ScheduleResult/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
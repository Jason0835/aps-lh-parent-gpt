package com.zlt.aps.cd90.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResultLog;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ICd90ScheduleResultLogRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:/cd90}")
public interface ICd90ScheduleResultLogRemoteService {
    @ApiOperation("查询列表")
    @PostMapping("/cd90ScheduleResultLog/list")
    TableDataInfo list(@RequestBody Cd90ScheduleResultLog queryVO);

    @ApiOperation("获取详情")
    @GetMapping("/cd90ScheduleResultLog/getInfo/{id}")
    Cd90ScheduleResultLog getInfo(@PathVariable("id") Long id);

    @ApiOperation("删除")
    @PostMapping("/cd90ScheduleResultLog/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("导出")
    @PostMapping("/cd90ScheduleResultLog/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd90ScheduleResultLog queryVO, @PathVariable("fileName") String fileName);
}
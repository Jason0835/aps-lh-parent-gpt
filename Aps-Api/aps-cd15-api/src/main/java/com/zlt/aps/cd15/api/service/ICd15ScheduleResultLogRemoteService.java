package com.zlt.aps.cd15.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResultLog;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/** CD15斜裁排程结果日志远程服务。 */
@FeignClient(contextId = "ICd15ScheduleResultLogRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd15:/cd15}")
public interface ICd15ScheduleResultLogRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/cd15ScheduleResultLog/list")
    TableDataInfo list(@RequestBody Cd15ScheduleResultLog queryVO);

    @ApiOperation("获取详情")
    @GetMapping("/cd15ScheduleResultLog/getInfo/{id}")
    Cd15ScheduleResultLog getInfo(@PathVariable("id") Long id);

    @ApiOperation("删除")
    @PostMapping("/cd15ScheduleResultLog/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("导出")
    @PostMapping("/cd15ScheduleResultLog/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd15ScheduleResultLog queryVO, @PathVariable("fileName") String fileName);
}
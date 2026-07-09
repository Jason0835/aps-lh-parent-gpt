package com.zlt.aps.tc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.entity.TcDispatcherLog;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 胎侧调度员排程操作日志 远程服务接口
 */
@FeignClient(contextId = "ITcDispatcherLogRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:/tc}")
public interface ITcDispatcherLogRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tcDispatcherLog/list")
    TableDataInfo list(@RequestBody TcDispatcherLog queryVO);

    @ApiOperation("导出数据")
    @PostMapping("/tcDispatcherLog/exportData/{fileName}")
    byte[] exportData(@RequestBody TcDispatcherLog queryVO, @PathVariable("fileName") String fileName);
}
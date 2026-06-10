package com.zlt.aps.tm.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.entity.TmDispatcherLog;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 胎面调度员排程操作日志 远程服务接口
 */
@FeignClient(contextId = "ITmDispatcherLogRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tm:/tm}")
public interface ITmDispatcherLogRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tmDispatcherLog/list")
    TableDataInfo list(@RequestBody TmDispatcherLog queryVO);

    @ApiOperation("导出数据")
    @PostMapping("/tmDispatcherLog/exportData/{fileName}")
    byte[] exportData(@RequestBody TmDispatcherLog queryVO, @PathVariable("fileName") String fileName);
}

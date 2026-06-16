package com.zlt.aps.dj.api.service;


import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.dj.api.domain.entity.DjDispatcherLog;

import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


/**
 * 垫胶调度员排程操作日志Service接口
 * @author Gim
 * @date 2022-02-25
 */
@FeignClient(contextId = "IDjDispatcherLogRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.dj:dj}")
public interface IDjDispatcherLogRemoteService {

    /**
     * 查询调度员排程操作日志列表
     */
    @ApiOperation("查询调度员排程操作日志列表")
    @PostMapping("/dj/dispatcherLog/list")
    TableDataInfo list(@RequestBody DjDispatcherLog ncDispatcherLog);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/dj/dispatcherLog/{id}")
    DjDispatcherLog getInfo(@PathVariable("id") Long id);

    /**
     * 导出调度员排程操作日志列表
     */
    @ApiOperation("导出调度员排程操作日志列表")
    @PostMapping("/dj/dispatcherLog/export")
    public byte[] export(@RequestBody DjDispatcherLog dispatcherLog);
}

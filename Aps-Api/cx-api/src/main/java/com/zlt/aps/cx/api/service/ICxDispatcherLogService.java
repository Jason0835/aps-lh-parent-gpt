package com.zlt.aps.cx.api.service;


import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.entity.CxDispatcherLog;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


/**
 * 成型调度员排程操作日志Service接口
 * @author Gim
 * @date 2022-02-25
 */
@FeignClient(contextId = "ICxDispatcherLogService", value =ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.cx:cx}")
public interface ICxDispatcherLogService {

    /**
     * 查询调度员排程操作日志列表
     */
    @ApiOperation("查询调度员排程操作日志列表")
    @PostMapping("/dispatcherLog/list")
    TableDataInfo list(@RequestBody CxDispatcherLog cxDispatcherLog);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/dispatcherLog/{id}")
    CxDispatcherLog getInfo(@PathVariable("id") Long id);

    /**
     * 导出调度员排程操作日志列表
     */
    @ApiOperation("导出调度员排程操作日志列表")
    @PostMapping("/dispatcherLog/export")
    public byte[] export(@RequestBody CxDispatcherLog dispatcherLog);
}

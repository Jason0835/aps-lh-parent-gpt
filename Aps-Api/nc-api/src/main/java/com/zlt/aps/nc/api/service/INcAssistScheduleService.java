package com.zlt.aps.nc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.nc.api.domain.entity.NcAssistSchedule;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


/**
 * 内衬胶外协排程结果Service接口
 *
 * @author chen
 * @date 2022-02-15
 */
@FeignClient(contextId = "INcAssistScheduleService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.nc:nc}")
public interface INcAssistScheduleService {

    /**
     * 查询内衬胶外协排程结果列表
     */
    @ApiOperation("查询内衬胶外协排程结果列表")
    @PostMapping("/nc/assistSchedule/list")
    TableDataInfo list(@RequestBody NcAssistSchedule ncAssistSchedule);

    /**
     * 导出列表
     */
    @PostMapping("/nc/assistSchedule/export")
    byte[] export(@RequestBody NcAssistSchedule ncAssistSchedule);
}

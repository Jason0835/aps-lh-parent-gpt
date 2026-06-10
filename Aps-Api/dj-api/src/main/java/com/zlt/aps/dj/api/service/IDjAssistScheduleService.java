package com.zlt.aps.dj.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.dj.api.domain.entity.DjAssistSchedule;

import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


/**
 * 垫胶胶外协排程结果Service接口
 *
 * @author chen
 * @date 2022-02-15
 */
@FeignClient(contextId = "INcAssistScheduleService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.dj:nc}")
public interface IDjAssistScheduleService {

    /**
     * 查询垫胶胶外协排程结果列表
     */
    @ApiOperation("查询垫胶胶外协排程结果列表")
    @PostMapping("/dj/assistSchedule/list")
    TableDataInfo list(@RequestBody DjAssistSchedule ncAssistSchedule);

    /**
     * 导出列表
     */
    @PostMapping("/dj/assistSchedule/export")
    byte[] export(@RequestBody DjAssistSchedule ncAssistSchedule);
}

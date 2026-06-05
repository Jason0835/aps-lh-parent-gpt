package com.zlt.aps.cd90.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleAssist;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


/**
 * 90度裁断外协排程结果Service接口
 * @author chen
 * @date 2022-02-16
 */
@FeignClient(contextId = "ICd90ScheduleAssistService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:cd90}")
public interface ICd90ScheduleAssistService {

    /**
     * 查询90度裁断外协排程结果列表
     */
    @ApiOperation("查询90度裁断外协排程结果列表")
    @PostMapping("/cd90/assistSchedule/list")
    TableDataInfo list(@RequestBody Cd90ScheduleAssist cd90ScheduleAssist);

    /**
     * 导出列表
     */
    @PostMapping("/cd90/assistSchedule/export")
    byte[] export(@RequestBody Cd90ScheduleAssist scheduleAssist);
}

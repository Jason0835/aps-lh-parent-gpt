package com.zlt.aps.cd15.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleAssist;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


/**
 * 15度裁断外协排程结果Service接口
 * @author chen
 * @date 2022-02-16
 */
@FeignClient(contextId = "ICd15ScheduleAssistService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd15:cd15}")
public interface ICd15ScheduleAssistService {

    /**
     * 查询15度裁断外协排程结果列表
     */
    @ApiOperation("查询15度裁断外协排程结果列表")
    @PostMapping("/cd15/assistSchedule/list")
    TableDataInfo list(@RequestBody Cd15ScheduleAssist cd15ScheduleAssist);

    /**
     * 导出列表
     */
    @PostMapping("/cd15/assistSchedule/export")
    byte[] export(@RequestBody Cd15ScheduleAssist scheduleAssist);
}

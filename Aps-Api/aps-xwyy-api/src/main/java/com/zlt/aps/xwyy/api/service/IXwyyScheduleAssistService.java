package com.zlt.aps.xwyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.xwyy.api.domain.entity.XwyyScheduleAssist;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


/**
 * 纤维压延外协排程结果Service接口
 * @author chen
 * @date 2022-02-16
 */
@FeignClient(contextId = "IXwyyScheduleAssistService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.xwyy:xwyy}")
public interface IXwyyScheduleAssistService {

    /**
     * 查询纤维压延外协排程结果列表
     */
    @ApiOperation("查询纤维压延外协排程结果列表")
    @PostMapping("/xwyy/assistSchedule/list")
    TableDataInfo list(@RequestBody XwyyScheduleAssist xwyyScheduleAssist);

    /**
     * 导出纤维压延外协排程结果信息
     */
    @PostMapping("/xwyy/assistSchedule/export")
    @ApiOperation("导出纤维压延外协排程结果信息")
    public byte[] export(@RequestBody XwyyScheduleAssist scheduleAssist);
}

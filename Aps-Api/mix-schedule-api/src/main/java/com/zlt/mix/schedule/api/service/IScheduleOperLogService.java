package com.zlt.mix.schedule.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.schedule.api.domain.dto.ScheduleOperLogDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.annotations.ApiOperation;

import com.zlt.mix.schedule.api.domain.entity.ScheduleOperLog;

/**
 * 排程操作日志Service接口
 * @author chen
 * @date 2022-07-13
 */
@FeignClient(contextId = "IScheduleOperLogService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IScheduleOperLogService {

    /**
     * 查询排程操作日志列表
     */
    @PostMapping("/scheduleOperLog/list")
    TableDataInfo listScheduleOperLog(@RequestBody ScheduleOperLog scheduleOperLog);

    /**
    * 根据ID获取详细信息
    */
    @GetMapping(value = "/scheduleOperLog/{id}")
    ScheduleOperLog getScheduleOperLogInfo(@PathVariable("id") Long id);

    /**
     * 导出排程操作日志列表
     */
    @PostMapping("/scheduleOperLog/exportData")
    public byte[] exportData(@RequestBody ScheduleOperLogDto dto);
}

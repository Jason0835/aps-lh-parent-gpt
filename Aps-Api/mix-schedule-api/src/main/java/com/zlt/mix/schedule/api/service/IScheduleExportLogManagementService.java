package com.zlt.mix.schedule.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.schedule.api.domain.entity.ScheduleExportLogManagement;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 导出日志管理对外暴露接口
 */
@FeignClient(contextId = "IScheduleExportLogManagementService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.schedule:mixSchedule}")
public interface IScheduleExportLogManagementService
{
    String prefix = "/schedule/exportLogManagement";

    /**
     * 查询排程设置参数信息列表
     */
    @PostMapping(value = prefix + "/list")
    public TableDataInfo list(@RequestBody ScheduleExportLogManagement dto);

    /**
     * 获取排程设置参数信息详细信息
     */
    @GetMapping(value = prefix + "/{id}")
    public ScheduleExportLogManagement getExportLogManagement(@PathVariable("id") Long id);

    /**
     * 导出接口
     * @param dto 查询条件
     */
    @GetMapping(value = prefix + "/exportData")
    List<ScheduleExportLogManagement> exportData(@SpringQueryMap ScheduleExportLogManagement dto);
}

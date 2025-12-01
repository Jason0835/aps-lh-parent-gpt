package com.zlt.aps.tm.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.dto.MachineDto;
import com.zlt.aps.tm.api.domain.dto.MaintenanceLogDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 胎面胶料顺序对外暴露接口
 */
@FeignClient(contextId = "IMaintenanceLogService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.tm:tm}")
public interface IMaintenanceLogService {

    /**
     * 根据查询条件查询运维操作日志
     */
    @PostMapping("/tm/maintenanceLog/listMaintenanceLog")
    TableDataInfo listMaintenanceLog(@RequestBody MaintenanceLogDto dto);

    /**
     * 根据id查询运维操作日志的明细信息
     */
    @GetMapping("/tm/maintenanceLog/getDetailInfo/{id}")
    MaintenanceLogDto getDetailInfo(@PathVariable("id") Long id);

    /**
     * 排程发布重置
     */
    @PostMapping("/tm/maintenanceLog/resetScheduleRelease")
    AjaxResult resetScheduleRelease(@RequestBody MaintenanceLogDto dto);

    /**
     * 排程删除
     */
    @PostMapping("/tm/maintenanceLog/deleteSchedule")
    AjaxResult deleteSchedule(@RequestBody MaintenanceLogDto dto);

    /**
     * 根据工序类型，查询指定工序的机台列表
     */
    @GetMapping("/tm/maintenanceLog/listMachineByProcedure/{procedureCode}")
    List<MachineDto> listMachineByProcedure(@PathVariable("procedureCode") String procedureCode);
}

package com.zlt.aps.tm.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.dto.MachineDto;
import com.zlt.aps.tm.api.domain.dto.MaintenanceLogDto;
import com.zlt.aps.tm.entity.MaintenanceLog;
import com.zlt.aps.tm.service.MaintenanceLogService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 运维操作日志表 前端控制器
 * </p>
 *
 * @author zhangbinglin
 * @since 2022-02-08
 */
@RestController
@RequestMapping("/maintenanceLog")
public class MaintenanceLogController extends BaseController {

    @Resource
    private MaintenanceLogService maintenanceLogService;

    @ApiOperation("根据查询条件查询运维操作日志")
    @GetMapping("/listMaintenanceLog")
    public TableDataInfo listMaintenanceLog(MaintenanceLogDto dto) {
        startPage();
        dto.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<MaintenanceLog> list = maintenanceLogService.listMaintenanceLog(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询运维操作日志的明细信息")
    @GetMapping("/getDetailInfo/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public MaintenanceLogDto getDetailInfo(@PathVariable("id") Long id) {
        return maintenanceLogService.getDetailInfo(id);
    }

    @ApiOperation("排程发布重置")
    @PostMapping("/resetScheduleRelease")
    public AjaxResult resetScheduleRelease(@RequestBody MaintenanceLogDto dto) {
        this.maintenanceLogService.resetScheduleRelease(dto);
        return AjaxResult.success();
    }

    @ApiOperation("排程删除")
    @PostMapping("/deleteSchedule")
    public AjaxResult deleteSchedule(@RequestBody MaintenanceLogDto dto) {
        maintenanceLogService.deleteSchedule(dto);
        return AjaxResult.success();
    }

    @ApiOperation("根据工序类型，查询指定工序的机台列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "procedureCode", dataType = "string", value = "工序code：0-硫化、1-成型、2-胎面、3-胎侧、4-内衬、5-胎圈、6-钢丝圈、7-15度裁断、8-90裁断、9-钢带压延、10-纤维压延（对应数据字典：PROCEDURE_CODE", paramType = "query")
    })
    @GetMapping("/listMachineByProcedure/{procedureCode}")
    public List<MachineDto> listMachineByProcedure(@PathVariable("procedureCode") String procedureCode) {
        List<MachineDto> list = maintenanceLogService.listMachineByProcedure(procedureCode);
        return list;
    }
}

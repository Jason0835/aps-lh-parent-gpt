package com.zlt.aps.gsq.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.gsq.api.domain.entity.GsqAssistSchedule;
import com.zlt.aps.gsq.service.GsqAssistScheduleService;
import com.zlt.aps.gsq.service.GsqMachineInfoService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 钢丝圈外协排程结果Controller
 *
 * @author chen
 * @date 2022-02-15
 */
@RestController
@RequestMapping("/assistSchedule")
public class GsqAssistScheduleController extends BaseController {

    @Value("${excelModelPath}")
    public String excelModelPath;
    @Autowired
    private GsqAssistScheduleService gsqAssistScheduleService;
    @Autowired
    private GsqMachineInfoService gsqMachineInfoService;

    /**
     * 查询钢丝圈外协排程结果列表
     */
    @ApiOperation("查询钢丝圈外协排程结果列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody GsqAssistSchedule gsqAssistSchedule) {
        gsqAssistSchedule.setOrderStr(orderStr());
        List<GsqAssistSchedule> list = gsqAssistScheduleService.selectGsqAssistScheduleList(gsqAssistSchedule);
        return getDataTable(list);
    }

    /**
     * 导出钢丝圈外协排程结果列表
     */
    @Log(title = "ui.data.column.gsq.assistSchedule.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出钢丝圈外协排程结果列表")
    @PostMapping("/export")
    public byte[] export(@RequestBody GsqAssistSchedule assistSchedule) {
        assistSchedule.setOrderStr(orderStr());
        List<GsqAssistSchedule> list = gsqAssistScheduleService.selectGsqAssistScheduleList(assistSchedule);
        return gsqAssistScheduleService.export(list);
    }
}

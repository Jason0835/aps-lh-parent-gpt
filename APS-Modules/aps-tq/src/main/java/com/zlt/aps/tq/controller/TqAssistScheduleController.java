package com.zlt.aps.tq.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.tq.api.domain.entity.TqAssistSchedule;
import com.zlt.aps.tq.service.TqAssistScheduleService;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 胎圈外协排程结果Controller
 *
 * @author chen
 * @date 2022-02-16
 */
@RestController
@RequestMapping("/assistSchedule")
public class TqAssistScheduleController extends BaseController {
    @Autowired
    private TqAssistScheduleService tqAssistScheduleService;

    /**
     * 查询胎圈外协排程结果列表
     */
    @ApiOperation("查询胎圈外协排程结果列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody TqAssistSchedule tqAssistSchedule) {
        tqAssistSchedule.setOrderStr(orderStr());
        List<TqAssistSchedule> list = tqAssistScheduleService.selectTqAssistScheduleList(tqAssistSchedule);
        return getDataTable(list);
    }

    /**
     * 导出胎圈外协排程结果列表
     */
    @Log(title = "ui.data.column.tq.assistSchedule.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出胎圈外协排程结果列表")
    @PostMapping("/export")
    public byte[] export(@RequestBody TqAssistSchedule assistSchedule) {
        assistSchedule.setYear(DateFormatUtils.format(assistSchedule.getScheduleDate(), "yyyy"));
        assistSchedule.setMonth(DateFormatUtils.format(assistSchedule.getScheduleDate(), "MM"));
        assistSchedule.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<TqAssistSchedule> list = tqAssistScheduleService.selectTqAssistScheduleList(assistSchedule);
        return tqAssistScheduleService.export(list);
    }
}

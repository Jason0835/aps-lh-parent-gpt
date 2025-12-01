package com.zlt.aps.xwyy.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.xwyy.api.domain.entity.XwyyScheduleAssist;
import com.zlt.aps.xwyy.service.XwyyScheduleAssistService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 纤维压延外协排程结果Controller
 *
 * @author chen
 * @date 2022-02-16
 */
@RestController
@RequestMapping("/xwyy/assistSchedule")
public class XwyyScheduleAssistController extends BaseController {
    @Autowired
    private XwyyScheduleAssistService xwyyScheduleAssistService;

    /**
     * 查询纤维压延外协排程结果列表
     */
    @ApiOperation("查询纤维压延外协排程结果列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody XwyyScheduleAssist xwyyScheduleAssist) {
        xwyyScheduleAssist.setOrderStr(orderStr());
        List<XwyyScheduleAssist> list = xwyyScheduleAssistService.selectXwyyScheduleAssistList(xwyyScheduleAssist);
        return getDataTable(list);
    }

    /**
     * 导出纤维压延外协排程结果列表
     */
    @Log(title = "ui.data.column.xwyy.assistSchedule.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出纤维压延外协排程结果列表")
    @PostMapping("/export")
    public byte[] export(@RequestBody XwyyScheduleAssist scheduleAssist) {
        scheduleAssist.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<XwyyScheduleAssist> list = xwyyScheduleAssistService.selectXwyyScheduleAssistList(scheduleAssist);
        return xwyyScheduleAssistService.export(list);
    }
}

package com.zlt.aps.xwyy.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.xwyy.api.domain.entity.XwyyDispatcherLog;
import com.zlt.aps.xwyy.service.XwyyDispatcherLogService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 纤维压延调度员排程操作日志Controller
 *
 * @author Gim
 * @date 2022-02-25
 */
@RestController
@RequestMapping("/dispatcherLog")
public class XwyyDispatcherLogController extends BaseController
{
    @Autowired
    private XwyyDispatcherLogService dispatcherLogService;

    /**
     * 查询纤维压延调度员排程操作日志列表
     */
    @ApiOperation("查询纤维压延调度员排程操作日志列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody XwyyDispatcherLog dispatcherLog)
    {
        startPage();
        dispatcherLog.setOrderStr(orderStr());
        List<XwyyDispatcherLog> list = dispatcherLogService.selectXwyyDispatcherLogList(dispatcherLog);
        return getDataTable(list);
    }

    /**
     * 获取纤维压延调度员排程操作日志详细信息
     */
    @ApiOperation("获取纤维压延调度员排程操作日志详细信息")
    @GetMapping(value = "/{id}")
    public XwyyDispatcherLog getInfo(@PathVariable("id") Long id){
        return dispatcherLogService.selectXwyyDispatcherLogById(id);
    }

    /**
     * 导出调度员排程操作日志列表
     */
    @ApiOperation("导出调度员排程操作日志列表")
    @PostMapping("/export")
    public byte[] export(@RequestBody XwyyDispatcherLog dispatcherLog) {
        startPage();
        dispatcherLog.setOrderStr(orderStr());
        return dispatcherLogService.export(dispatcherLog);
    }
}

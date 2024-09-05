package com.zlt.aps.gdyy.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gdyy.api.domain.entity.GdyyDispatcherLog;
import com.zlt.aps.gdyy.service.GdyyDispatcherLogService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢带压延调度员排程操作日志Controller
 *
 * @author Gim
 * @date 2022-02-25
 */
@RestController
@RequestMapping("/dispatcherLog")
public class GdyyDispatcherLogController extends BaseController
{
    @Autowired
    private GdyyDispatcherLogService dispatcherLogService;

    /**
     * 查询钢带压延调度员排程操作日志列表
     */
    @ApiOperation("查询钢带压延调度员排程操作日志列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody GdyyDispatcherLog dispatcherLog)
    {
        startPage();
        dispatcherLog.setOrderStr(orderStr());
        List<GdyyDispatcherLog> list = dispatcherLogService.selectGdyyDispatcherLogList(dispatcherLog);
        return getDataTable(list);
    }

    /**
     * 获取钢带压延调度员排程操作日志详细信息
     */
    @ApiOperation("获取钢带压延调度员排程操作日志详细信息")
    @GetMapping(value = "/{id}")
    public GdyyDispatcherLog getInfo(@PathVariable("id") Long id){
        return dispatcherLogService.selectGdyyDispatcherLogById(id);
    }

    /**
     * 导出调度员排程操作日志列表
     */
    @ApiOperation("导出调度员排程操作日志列表")
    @PostMapping("/export")
    public byte[] export(@RequestBody GdyyDispatcherLog dispatcherLog) {
        startPage();
        dispatcherLog.setOrderStr(orderStr());
        return dispatcherLogService.export(dispatcherLog);
    }
}

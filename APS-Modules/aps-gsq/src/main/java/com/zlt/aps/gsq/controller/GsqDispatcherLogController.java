package com.zlt.aps.gsq.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqDispatcherLog;
import com.zlt.aps.gsq.service.GsqDispatcherLogService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢丝圈调度员排程操作日志Controller
 *
 * @author Gim
 * @date 2022-02-25
 */
@RestController
@RequestMapping("/dispatcherLog")
public class GsqDispatcherLogController extends BaseController
{
    @Autowired
    private GsqDispatcherLogService dispatcherLogService;

    /**
     * 查询钢丝圈调度员排程操作日志列表
     */
    @ApiOperation("查询钢丝圈调度员排程操作日志列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody GsqDispatcherLog dispatcherLog)
    {
        startPage();
        dispatcherLog.setOrderStr(orderStr());
        List<GsqDispatcherLog> list = dispatcherLogService.selectGsqDispatcherLogList(dispatcherLog);
        return getDataTable(list);
    }

    /**
     * 获取钢丝圈调度员排程操作日志详细信息
     */
    @ApiOperation("获取钢丝圈调度员排程操作日志详细信息")
    @GetMapping(value = "/{id}")
    public GsqDispatcherLog getInfo(@PathVariable("id") Long id){
        return dispatcherLogService.selectGsqDispatcherLogById(id);
    }

    /**
     * 导出调度员排程操作日志列表
     */
    @ApiOperation("导出调度员排程操作日志列表")
    @PostMapping("/export")
    public byte[] export(@RequestBody GsqDispatcherLog dispatcherLog) {
        startPage();
        dispatcherLog.setOrderStr(orderStr());
        return dispatcherLogService.export(dispatcherLog);
    }
}

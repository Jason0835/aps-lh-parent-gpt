package com.zlt.aps.tq.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.entity.TqDispatcherLog;
import com.zlt.aps.tq.service.TqDispatcherLogService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎圈调度员排程操作日志Controller
 *
 * @author Gim
 * @date 2022-02-25
 */
@RestController
@RequestMapping("/dispatcherLog")
public class TqDispatcherLogController extends BaseController
{
    @Autowired
    private TqDispatcherLogService dispatcherLogService;

    /**
     * 查询胎圈调度员排程操作日志列表
     */
    @ApiOperation("查询胎圈调度员排程操作日志列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody TqDispatcherLog dispatcherLog)
    {
        startPage();
        dispatcherLog.setOrderStr(orderStr());
        List<TqDispatcherLog> list = dispatcherLogService.selectTqDispatcherLogList(dispatcherLog);
        return getDataTable(list);
    }

    /**
     * 获取胎圈调度员排程操作日志详细信息
     */
    @ApiOperation("获取胎圈调度员排程操作日志详细信息")
    @GetMapping(value = "/{id}")
    public TqDispatcherLog getInfo(@PathVariable("id") Long id){
        return dispatcherLogService.selectTqDispatcherLogById(id);
    }

    /**
     * 导出调度员排程操作日志列表
     */
    @ApiOperation("导出调度员排程操作日志列表")
    @PostMapping("/export")
    public byte[] export(@RequestBody TqDispatcherLog dispatcherLog) {
        startPage();
        dispatcherLog.setOrderStr(orderStr());
        return dispatcherLogService.export(dispatcherLog);
    }
}

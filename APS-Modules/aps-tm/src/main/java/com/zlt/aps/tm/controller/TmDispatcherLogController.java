package com.zlt.aps.tm.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.entity.TmDispatcherLog;
import com.zlt.aps.tm.service.TmDispatcherLogService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎面调度员排程操作日志Controller
 *
 * @author Gim
 * @date 2022-02-25
 */
@RestController
@RequestMapping("/tm/dispatcherLog")
public class TmDispatcherLogController extends BaseController
{
    @Autowired
    private TmDispatcherLogService dispatcherLogService;

    /**
     * 查询胎面调度员排程操作日志列表
     */
    @ApiOperation("查询胎面调度员排程操作日志列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody TmDispatcherLog dispatcherLog)
    {
        startPage();
        dispatcherLog.setOrderStr(orderStr());
        List<TmDispatcherLog> list = dispatcherLogService.selectTmDispatcherLogList(dispatcherLog);
        return getDataTable(list);
    }

    /**
     * 获取胎面调度员排程操作日志详细信息
     */
    @ApiOperation("获取胎面调度员排程操作日志详细信息")
    @GetMapping(value = "/{id}")
    public TmDispatcherLog getInfo(@PathVariable("id") Long id){
        return dispatcherLogService.selectTmDispatcherLogById(id);
    }

    /**
     * 导出调度员排程操作日志列表
     */
    @ApiOperation("导出调度员排程操作日志列表")
    @PostMapping("/export")
    public byte[] export(@RequestBody TmDispatcherLog dispatcherLog) {
        startPage();
        dispatcherLog.setOrderStr(orderStr());
        return dispatcherLogService.export(dispatcherLog);
    }
}

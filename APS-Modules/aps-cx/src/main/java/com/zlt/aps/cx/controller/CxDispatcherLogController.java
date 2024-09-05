package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.entity.CxDispatcherLog;
import com.zlt.aps.cx.service.CxDispatcherLogService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型调度员排程操作日志Controller
 *
 * @author Gim
 * @date 2022-02-25
 */
@RestController
@RequestMapping("/dispatcherLog")
public class CxDispatcherLogController extends BaseController
{
    @Autowired
    private CxDispatcherLogService dispatcherLogService;

    /**
     * 查询成型调度员排程操作日志列表
     */
    @ApiOperation("查询成型调度员排程操作日志列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody CxDispatcherLog dispatcherLog)
    {
        startPage();
        dispatcherLog.setOrderStr(orderStr());
        List<CxDispatcherLog> list = dispatcherLogService.selectCxDispatcherLogList(dispatcherLog);
        return getDataTable(list);
    }

    /**
     * 获取成型调度员排程操作日志详细信息
     */
    @ApiOperation("获取成型调度员排程操作日志详细信息")
    @GetMapping(value = "/{id}")
    public CxDispatcherLog getInfo(@PathVariable("id") Long id){
        return dispatcherLogService.selectCxDispatcherLogById(id);
    }

    /**
     * 导出调度员排程操作日志列表
     */
    @ApiOperation("导出调度员排程操作日志列表")
    @PostMapping("/export")
    public byte[] export(@RequestBody CxDispatcherLog dispatcherLog) {
        startPage();
        dispatcherLog.setOrderStr(orderStr());
        return dispatcherLogService.export(dispatcherLog);
    }
}

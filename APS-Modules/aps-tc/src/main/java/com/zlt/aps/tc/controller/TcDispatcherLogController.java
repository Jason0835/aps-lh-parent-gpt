package com.zlt.aps.tc.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.entity.TcDispatcherLog;
import com.zlt.aps.tc.service.TcDispatcherLogService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎侧调度员排程操作日志Controller
 *
 * @author Gim
 * @date 2022-02-25
 */
@RestController
@RequestMapping("/tc/dispatcherLog")
public class TcDispatcherLogController extends BaseController
{
    @Autowired
    private TcDispatcherLogService dispatcherLogService;

    /**
     * 查询胎侧调度员排程操作日志列表
     */
    @ApiOperation("查询胎侧调度员排程操作日志列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody TcDispatcherLog dispatcherLog)
    {
        startPage();
        dispatcherLog.setOrderStr(orderStr());
        List<TcDispatcherLog> list = dispatcherLogService.selectTcDispatcherLogList(dispatcherLog);
        return getDataTable(list);
    }

    /**
     * 获取胎侧调度员排程操作日志详细信息
     */
    @ApiOperation("获取胎侧调度员排程操作日志详细信息")
    @GetMapping(value = "/{id}")
    public TcDispatcherLog getInfo(@PathVariable("id") Long id){
        return dispatcherLogService.selectTcDispatcherLogById(id);
    }

    /**
     * 导出调度员排程操作日志列表
     */
    @ApiOperation("导出调度员排程操作日志列表")
    @PostMapping("/export")
    public byte[] export(@RequestBody TcDispatcherLog dispatcherLog) {
        startPage();
        dispatcherLog.setOrderStr(orderStr());
        return dispatcherLogService.export(dispatcherLog);
    }
}

package com.zlt.aps.nc.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.nc.api.domain.entity.NcDispatcherLog;
import com.zlt.aps.nc.service.NcDispatcherLogService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 内衬调度员排程操作日志Controller
 *
 * @author Gim
 * @date 2022-02-25
 */
@RestController
@RequestMapping("/nc/dispatcherLog")
public class NcDispatcherLogController extends BaseController
{
    @Autowired
    private NcDispatcherLogService dispatcherLogService;

    /**
     * 查询内衬调度员排程操作日志列表
     */
    @ApiOperation("查询内衬调度员排程操作日志列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody NcDispatcherLog dispatcherLog)
    {
        startPage();
        dispatcherLog.setOrderStr(orderStr());
        List<NcDispatcherLog> list = dispatcherLogService.selectNcDispatcherLogList(dispatcherLog);
        return getDataTable(list);
    }

    /**
     * 获取内衬调度员排程操作日志详细信息
     */
    @ApiOperation("获取内衬调度员排程操作日志详细信息")
    @GetMapping(value = "/{id}")
    public NcDispatcherLog getInfo(@PathVariable("id") Long id){
        return dispatcherLogService.selectNcDispatcherLogById(id);
    }

    /**
     * 导出调度员排程操作日志列表
     */
    @ApiOperation("导出调度员排程操作日志列表")
    @PostMapping("/export")
    public byte[] export(@RequestBody NcDispatcherLog dispatcherLog) {
        startPage();
        dispatcherLog.setOrderStr(orderStr());
        return dispatcherLogService.export(dispatcherLog);
    }
}

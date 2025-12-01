package com.zlt.aps.cd90.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90DispatcherLog;
import com.zlt.aps.cd90.service.Cd90DispatcherLogService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 90度裁断调度员排程操作日志Controller
 *
 * @author Gim
 * @date 2022-02-25
 */
@RestController
@RequestMapping("/cd90/dispatcherLog")
public class Cd90DispatcherLogController extends BaseController
{
    @Autowired
    private Cd90DispatcherLogService dispatcherLogService;

    /**
     * 查询90度裁断调度员排程操作日志列表
     */
    @ApiOperation("查询90度裁断调度员排程操作日志列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody Cd90DispatcherLog dispatcherLog)
    {
        startPage();
        dispatcherLog.setOrderStr(orderStr());
        List<Cd90DispatcherLog> list = dispatcherLogService.selectCd90DispatcherLogList(dispatcherLog);
        return getDataTable(list);
    }

    /**
     * 获取90度裁断调度员排程操作日志详细信息
     */
    @ApiOperation("获取90度裁断调度员排程操作日志详细信息")
    @GetMapping(value = "/{id}")
    public Cd90DispatcherLog getInfo(@PathVariable("id") Long id){
        return dispatcherLogService.selectCd90DispatcherLogById(id);
    }

    /**
     * 导出调度员排程操作日志列表
     */
    @ApiOperation("导出调度员排程操作日志列表")
    @PostMapping("/export")
    public byte[] export(@RequestBody Cd90DispatcherLog dispatcherLog) {
        startPage();
        dispatcherLog.setOrderStr(orderStr());
        return dispatcherLogService.export(dispatcherLog);
    }
}

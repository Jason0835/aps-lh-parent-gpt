package com.zlt.aps.cd15.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15DispatcherLog;
import com.zlt.aps.cd15.service.Cd15DispatcherLogService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 15度裁断调度员排程操作日志Controller
 *
 * @author Gim
 * @date 2022-02-25
 */
@RestController
@RequestMapping("/dispatcherLog")
public class Cd15DispatcherLogController extends BaseController
{
    @Autowired
    private Cd15DispatcherLogService dispatcherLogService;

    /**
     * 查询15度裁断调度员排程操作日志列表
     */
    @ApiOperation("查询15度裁断调度员排程操作日志列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody Cd15DispatcherLog dispatcherLog)
    {
        startPage();
        dispatcherLog.setOrderStr(orderStr());
        List<Cd15DispatcherLog> list = dispatcherLogService.selectCd15DispatcherLogList(dispatcherLog);
        return getDataTable(list);
    }

    /**
     * 获取15度裁断调度员排程操作日志详细信息
     */
    @ApiOperation("获取15度裁断调度员排程操作日志详细信息")
    @GetMapping(value = "/{id}")
    public Cd15DispatcherLog getInfo(@PathVariable("id") Long id){
        return dispatcherLogService.selectCd15DispatcherLogById(id);
    }

    /**
     * 导出调度员排程操作日志列表
     */
    @ApiOperation("导出调度员排程操作日志列表")
    @PostMapping("/export")
    public byte[] export(@RequestBody Cd15DispatcherLog dispatcherLog) {
        startPage();
        dispatcherLog.setOrderStr(orderStr());
        return dispatcherLogService.export(dispatcherLog);
    }
}

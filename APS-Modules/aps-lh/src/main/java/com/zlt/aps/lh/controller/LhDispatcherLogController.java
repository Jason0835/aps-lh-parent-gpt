package com.zlt.aps.lh.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhDispatcherLog;
import com.zlt.aps.lh.service.LhDispatcherLogService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 硫化调度员排程操作日志Controller
 *
 * @author Gim
 * @date 2022-02-25
 */
@RestController
@RequestMapping("/dispatcherLog")
public class LhDispatcherLogController extends BaseController
{
    @Autowired
    private LhDispatcherLogService dispatcherLogService;

    /**
     * 查询硫化调度员排程操作日志列表
     */
    @ApiOperation("查询硫化调度员排程操作日志列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody LhDispatcherLog dispatcherLog)
    {
        startPage();
        dispatcherLog.setOrderStr(orderStr());
        List<LhDispatcherLog> list = dispatcherLogService.selectLhDispatcherLogList(dispatcherLog);
        return getDataTable(list);
    }

    /**
     * 获取硫化调度员排程操作日志详细信息
     */
    @ApiOperation("获取硫化调度员排程操作日志详细信息")
    @GetMapping(value = "/{id}")
    public LhDispatcherLog getInfo(@PathVariable("id") Long id){
        return dispatcherLogService.selectLhDispatcherLogById(id);
    }

    /**
     * 导出调度员排程操作日志列表
     */
    @ApiOperation("导出调度员排程操作日志列表")
    @PostMapping("/export")
    public byte[] export(@RequestBody LhDispatcherLog dispatcherLog) {
        startPage();
        dispatcherLog.setOrderStr(orderStr());
        return dispatcherLogService.export(dispatcherLog);
    }
}

package com.zlt.aps.dj.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.dj.api.domain.entity.DjDispatcherLog;
import com.zlt.aps.dj.service.DjDispatcherLogService;

import io.swagger.annotations.ApiOperation;

/**
 * 垫胶调度员排程操作日志Controller
 *
 * @author Gim
 * @date 2022-02-25
 */
@RestController
@RequestMapping("/dj/dispatcherLog")
public class DjDispatcherLogController extends BaseController
{
    @Autowired
    private DjDispatcherLogService dispatcherLogService;

    /**
     * 查询垫胶调度员排程操作日志列表
     */
    @ApiOperation("查询垫胶调度员排程操作日志列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody DjDispatcherLog dispatcherLog)
    {
        startPage();
        dispatcherLog.setOrderStr(orderStr());
        List<DjDispatcherLog> list = dispatcherLogService.selectNcDispatcherLogList(dispatcherLog);
        return getDataTable(list);
    }

    /**
     * 获取垫胶调度员排程操作日志详细信息
     */
    @ApiOperation("获取垫胶调度员排程操作日志详细信息")
    @GetMapping(value = "/{id}")
    public DjDispatcherLog getInfo(@PathVariable("id") Long id){
        return dispatcherLogService.selectNcDispatcherLogById(id);
    }

    /**
     * 导出调度员排程操作日志列表
     */
    @ApiOperation("导出调度员排程操作日志列表")
    @PostMapping("/export")
    public byte[] export(@RequestBody DjDispatcherLog dispatcherLog) {
        startPage();
        dispatcherLog.setOrderStr(orderStr());
        return dispatcherLogService.export(dispatcherLog);
    }
}

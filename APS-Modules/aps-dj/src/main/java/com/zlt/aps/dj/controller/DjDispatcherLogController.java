package com.zlt.aps.dj.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.dj.api.domain.entity.DjDispatcherLog;
import com.zlt.aps.dj.service.DjDispatcherLogService;
import com.zlt.bill.common.controller.AbstractBillBizController;
import com.zlt.bill.common.service.IBillService;

import io.swagger.annotations.ApiOperation;

/**
 * 垫胶调度员排程操作日志Controller
 *
 * @author zlt
 * @date 2026-02-25
 */
@RestController
@RequestMapping("/dj/dispatcherLog")
public class DjDispatcherLogController extends AbstractBillBizController<DjDispatcherLog> {
    @Autowired
    private DjDispatcherLogService dispatcherLogService;

    /**
     * 查询垫胶调度员排程操作日志列表
     */
    @ApiOperation("查询垫胶调度员排程操作日志列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody DjDispatcherLog dispatcherLog) {
        return super.list(dispatcherLog);
    }

    /**
     * 获取垫胶调度员排程操作日志详细信息
     */
    @ApiOperation("获取垫胶调度员排程操作日志详细信息")
    @GetMapping(value = "/{id}")
    public DjDispatcherLog getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /**
     * 导出调度员排程操作日志列表
     * @throws IOException 
     */
    @ApiOperation("导出调度员排程操作日志列表")
    @PostMapping("/export")
    public byte[] export(@RequestBody DjDispatcherLog dispatcherLog, HttpServletResponse response) throws IOException {
        return super.exportData(dispatcherLog, I18nUtil.getMessage("ui.data.column.dj.dispatcherlog.modelName"), response);
    }

    @Override
    protected IBillService<DjDispatcherLog> getBillService() {
        return dispatcherLogService;
    }

    @Override
    protected String orderStr() {
        return "AFTER_MACHINE_CODE";
    }

    @Override
    protected String getTypeCode() {
        return "";
    }
}

package com.zlt.aps.nc.controller;

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
import com.zlt.aps.nc.api.domain.entity.NcDispatcherLog;
import com.zlt.aps.nc.service.NcDispatcherLogService;
import com.zlt.bill.common.controller.AbstractBillBizController;
import com.zlt.bill.common.service.IBillService;

import io.swagger.annotations.ApiOperation;

/**
 * 内衬调度员排程操作日志Controller
 *
 * @author zlt
 * @date 2026-02-25
 */
@RestController
@RequestMapping("/nc/dispatcherLog")
public class NcDispatcherLogController extends AbstractBillBizController<NcDispatcherLog> {
    @Autowired
    private NcDispatcherLogService dispatcherLogService;

    /**
     * 查询内衬调度员排程操作日志列表
     */
    @ApiOperation("查询内衬调度员排程操作日志列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody NcDispatcherLog dispatcherLog) {
        return super.list(dispatcherLog);
    }

    /**
     * 获取内衬调度员排程操作日志详细信息
     */
    @ApiOperation("获取内衬调度员排程操作日志详细信息")
    @GetMapping(value = "/{id}")
    public NcDispatcherLog getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /**
     * 导出调度员排程操作日志列表
     * @throws IOException 
     */
    @ApiOperation("导出调度员排程操作日志列表")
    @PostMapping("/export")
    public byte[] export(@RequestBody NcDispatcherLog dispatcherLog, HttpServletResponse response) throws IOException {
        return super.exportData(dispatcherLog, I18nUtil.getMessage("ui.data.column.nc.dispatcherlog.modelName"), response);
    }

    @Override
    protected IBillService<NcDispatcherLog> getBillService() {
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

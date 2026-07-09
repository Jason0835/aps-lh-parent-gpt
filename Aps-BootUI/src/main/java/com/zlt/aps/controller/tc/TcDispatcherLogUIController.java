package com.zlt.aps.controller.tc;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.tc.api.domain.entity.TcDispatcherLog;
import com.zlt.aps.tc.api.service.ITcDispatcherLogRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * 胎侧调度员排程操作日志 页面控制层
 */
@Slf4j
@Api(tags = "胎侧调度员排程操作日志")
@Controller
@RequestMapping("/tc/tcDispatcherLog")
public class TcDispatcherLogUIController extends BaseUIController<TcDispatcherLog> {

    private final String prefix = "aps/tc/tcDispatcherLog";

    @Autowired
    private ITcDispatcherLogRemoteService iTcDispatcherLogService;

    @RequiresPermissions("tc:tcDispatcherLog:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/tcDispatcherLog";
    }

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TcDispatcherLog query) {
        return iTcDispatcherLogService.list(query);
    }

    @ApiOperation("导出数据")
    @GetMapping("/export")
    @RequiresPermissions("tc:tcDispatcherLog:export")
    public void export(HttpServletResponse response, TcDispatcherLog entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.tc.dispatcherLog.modelName");
        byte[] excelBytes = iTcDispatcherLogService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }
}
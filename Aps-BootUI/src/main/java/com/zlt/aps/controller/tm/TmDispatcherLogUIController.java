package com.zlt.aps.controller.tm;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.tm.api.domain.entity.TmDispatcherLog;
import com.zlt.aps.tm.api.service.ITmDispatcherLogRemoteService;
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
 * 胎面调度员排程操作日志 页面控制层
 */
@Slf4j
@Api(tags = "胎面调度员排程操作日志")
@Controller
@RequestMapping("/tm/tmDispatcherLog")
public class TmDispatcherLogUIController extends BaseUIController<TmDispatcherLog> {

    private final String prefix = "aps/tm/tmDispatcherLog";

    @Autowired
    private ITmDispatcherLogRemoteService iTmDispatcherLogService;

    @RequiresPermissions("tm:tmDispatcherLog:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/tmDispatcherLog";
    }

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TmDispatcherLog query) {
        return iTmDispatcherLogService.list(query);
    }

    @ApiOperation("导出数据")
    @GetMapping("/export")
    @RequiresPermissions("tm:tmDispatcherLog:export")
    public void export(HttpServletResponse response, TmDispatcherLog entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.tm.dispatcherLog.modelName");
        byte[] excelBytes = iTmDispatcherLogService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }
}

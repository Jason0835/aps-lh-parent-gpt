package com.zlt.aps.controller.cd90;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResultLog;
import com.zlt.aps.cd90.api.service.ICd90ScheduleResultLogRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

@Api(tags = "直裁排程结果日志")
@Controller
@RequestMapping("/cd90/cd90ScheduleResultLog")
public class Cd90ScheduleResultLogUIController extends BaseUIController<Cd90ScheduleResultLog> {

    @Resource private ICd90ScheduleResultLogRemoteService remote;

    @ApiOperation("查询列表") @RequiresPermissions("cd90:scheduleResultLog:list") @PostMapping("/list") @ResponseBody
    public TableDataInfo list(Cd90ScheduleResultLog q) { return remote.list(q); }
    @ApiOperation("获取详情") @GetMapping("/getInfo/{id}") @ResponseBody
    public Cd90ScheduleResultLog getInfo(@PathVariable("id") Long id) { return remote.getInfo(id); }
    @ApiOperation("删除") @RequiresPermissions("cd90:scheduleResultLog:remove") @PostMapping("/remove") @ResponseBody
    public AjaxResult remove(String ids) { return remote.removeByIds(Arrays.asList(Convert.toLongArray(ids))); }
    @Override public String getExportTemplateFileName() { return getFunctionName(); }
    @Override public String getProcedureCode() { return "CD90"; }
    @Override public String getFunctionName() { return I18nUtil.getMessage("ui.data.column.cd90ScheduleResultLog.modelName"); }
    @ApiOperation("导出") @RequiresPermissions("cd90:scheduleResultLog:export") @GetMapping("/export") @ResponseBody @Override
    public void export(HttpServletResponse response, Cd90ScheduleResultLog entity) throws IOException {
        byte[] excelBytes = remote.exportData(entity, getExportTemplateFileName());
        IOUtils.copy(new ByteArrayInputStream(excelBytes), response.getOutputStream()); response.flushBuffer();
    }
}
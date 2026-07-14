package com.zlt.aps.controller.cd15;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResultLog;
import com.zlt.aps.cd15.api.service.ICd15ScheduleResultLogRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/** CD15斜裁排程结果日志UIController。 */
@Api(tags = "CD15斜裁排程结果日志")
@Controller
@RequestMapping("/cd15/cd15ScheduleResultLog")
public class Cd15ScheduleResultLogUIController extends BaseUIController<Cd15ScheduleResultLog> {

    @Resource
    private ICd15ScheduleResultLogRemoteService remote;

    @ApiOperation("查询列表")
    @RequiresPermissions("cd15:scheduleResultLog:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd15ScheduleResultLog query) {
        return remote.list(query);
    }

    @ApiOperation("获取详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public Cd15ScheduleResultLog getInfo(@PathVariable("id") Long id) {
        return remote.getInfo(id);
    }

    @ApiOperation("删除")
    @RequiresPermissions("cd15:scheduleResultLog:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return remote.removeByIds(Arrays.asList(Convert.toLongArray(ids)));
    }

    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "CD15";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.cd15ScheduleResultLog.modelName");
    }

    @ApiOperation("导出")
    @RequiresPermissions("cd15:scheduleResultLog:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, Cd15ScheduleResultLog entity) throws IOException {
        byte[] excelBytes = remote.exportData(entity, this.getExportTemplateFileName());
        IOUtils.copy(new ByteArrayInputStream(excelBytes), response.getOutputStream());
        response.flushBuffer();
    }
}
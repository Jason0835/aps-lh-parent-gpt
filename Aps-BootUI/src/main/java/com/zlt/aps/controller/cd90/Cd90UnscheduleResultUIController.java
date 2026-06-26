package com.zlt.aps.controller.cd90;

import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd90.api.domain.entity.Cd90UnscheduleResult;
import com.zlt.aps.cd90.api.service.ICd90UnscheduleResultRemoteService;
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

/** 直裁未排结果UIController。 */
@Api(tags = "直裁未排结果")
@Controller
@RequestMapping("/cd90/cd90UnscheduleResult")
public class Cd90UnscheduleResultUIController extends BaseUIController<Cd90UnscheduleResult> {

    @Resource
    private ICd90UnscheduleResultRemoteService remote;

    @ApiOperation("查询列表")
    @RequiresPermissions("cd90:scheduleResult:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd90UnscheduleResult q) {
        return remote.list(q);
    }

    @ApiOperation("获取详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public Cd90UnscheduleResult getInfo(@PathVariable("id") Long id) {
        return remote.getInfo(id);
    }

    @Override
    public String getExportTemplateFileName() {
        return getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "CD90";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.cd90UnscheduleResult.modelName");
    }

    @ApiOperation("导出")
    @RequiresPermissions("cd90:scheduleResult:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, Cd90UnscheduleResult entity) throws IOException {
        byte[] excelBytes = remote.exportData(entity, getExportTemplateFileName());
        IOUtils.copy(new ByteArrayInputStream(excelBytes), response.getOutputStream());
        response.flushBuffer();
    }
}

package com.zlt.aps.controller.cx;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxDispatcherLog;
import com.zlt.aps.cxlh.cx.api.service.ICxDispatcherLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
 * 成型调度员排程操作日志Controller
 * @author Gim
 * @date 2022-02-25
 */
@Api(tags = "成型调度员排程操作日志")
@Controller
@RequestMapping("/cx/dispatcherLog")
public class CxDispatcherLogUIController extends BaseController {

    @Autowired
    private ICxDispatcherLogService dispatcherLogService;

    /**
     * 根据条件查询成型调度员排程操作日志列表
     */
    @ApiOperation("根据条件查询成型调度员排程操作日志列表")
    @RequiresPermissions("cx:dispatcherLog:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxDispatcherLog entity) {
        return dispatcherLogService.list(entity);
    }

    public String getExportTemplateFileName(){
        return this.getFunctionName();
    }

    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.cx.dispatcherlog.modelName");
    }

    @ApiOperation("数据导出")
    @RequiresPermissions("cx:dispatcherLog:export")
    @GetMapping({"/export"})
    @ResponseBody
    public void export(HttpServletResponse response, CxDispatcherLog entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = dispatcherLogService.exportData(entity,fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }
}

package com.zlt.aps.controller.cx;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cx.api.domain.entity.CxDayFinishQty;
import com.zlt.aps.cx.api.service.ICxDayFinishQtyRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * 成型排程日完成量 UI控制层
 *
 * @author APS Team
 * @since 2026/05/12
 */
@Slf4j
@Api(tags = "成型排程日完成量管理")
@Controller
@RequestMapping("/cx/cxDayFinishQty")
public class CxDayFinishQtyUIController extends BaseUIController<CxDayFinishQty> {

    @Autowired
    private ICxDayFinishQtyRemoteService cxDayFinishQtyRemoteService;

    private final String prefix = "aps/cx/cxDayFinishQty";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:cxDayFinishQty:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/cxDayFinishQty";
    }

    /**
     * 查询成型排程日完成量列表
     */
    @ApiOperation("查询列表")
    @RequiresPermissions("cx:cxDayFinishQty:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxDayFinishQty cxDayFinishQty) {
        return cxDayFinishQtyRemoteService.list(cxDayFinishQty);
    }

    /**
     * 根据ID获取详情
     */
    @ApiOperation("获取详情")
    @RequiresPermissions("cx:cxDayFinishQty:view")
    @GetMapping("/{id}")
    @ResponseBody
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(cxDayFinishQtyRemoteService.getInfo(id));
    }

    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "0";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.cxDayFinishQty.modelName");
    }

    /**
     * 导出数据
     */
    @ApiOperation("导出数据")
    @GetMapping({"/export"})
    @RequiresPermissions("cx:cxDayFinishQty:export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, CxDayFinishQty entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = cxDayFinishQtyRemoteService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }
}

package com.zlt.aps.controller.lh;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.lh.api.domain.entity.LhSkuDecrement;
import com.zlt.aps.lh.api.service.ILhSkuDecrementRemoteService;
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
import java.util.Arrays;

/**
 * SKU减量清单UI控制器
 */
@Slf4j
@Api(tags = "SKU减量清单")
@Controller
@RequestMapping("/lh/lhSkuDecrement")
public class LhSkuDecrementUIController extends BaseUIController<LhSkuDecrement> {

    @Autowired
    private ILhSkuDecrementRemoteService iLhSkuDecrementRemoteService;

    @ApiOperation("查询列表")
    @RequiresPermissions("lh:skuDecrement:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(LhSkuDecrement query) {
        return iLhSkuDecrementRemoteService.list(query);
    }

    @ApiOperation("获取详情")
    @RequiresPermissions("lh:skuDecrement:query")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(iLhSkuDecrementRemoteService.getInfo(id));
    }

    @ApiOperation("确认减量")
    @RequiresPermissions("lh:skuDecrement:confirm")
    @PostMapping("/confirm")
    @ResponseBody
    public AjaxResult confirm(@RequestBody LhSkuDecrement entity) {
        return iLhSkuDecrementRemoteService.confirm(entity);
    }

    @ApiOperation("删除")
    @RequiresPermissions("lh:skuDecrement:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iLhSkuDecrementRemoteService.removeByIds(Arrays.asList(arr));
    }

    @ApiOperation("导出")
    @RequiresPermissions("lh:skuDecrement:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, LhSkuDecrement entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.lhSkuDecrement.modelName");
        byte[] excelBytes = iLhSkuDecrementRemoteService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @Override
    public String getProcedureCode() {
        return "0";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.lhSkuDecrement.modelName");
    }

    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }
}
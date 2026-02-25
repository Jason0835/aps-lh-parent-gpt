package com.zlt.aps.controller.monthplan;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.mp.api.domain.entity.EstimateExceedShort;
import com.zlt.aps.mp.api.service.IEstimateExceedShortRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：EstimateExceedShortUIController.java
 * 描    述：预计超欠产 UI控制层类：....
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-18
 */
@Slf4j
@Api(tags = "预计超欠产")
@Controller
@RequestMapping("/lean/estimateExceedShort")
public class EstimateExceedShortUIController extends BaseUIController<EstimateExceedShort> {

    private final IEstimateExceedShortRemoteService iEstimateExceedShortService;

    public EstimateExceedShortUIController(IEstimateExceedShortRemoteService iEstimateExceedShortService) {
        this.iEstimateExceedShortService = iEstimateExceedShortService;
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:estimateExceedShort:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(EstimateExceedShort estimateExceedShort) {
        return iEstimateExceedShortService.list(estimateExceedShort);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions(value = {"monthplan:estimateExceedShort:edit", "monthplan:estimateExceedShort:add"}, logical = Logical.OR)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult save(EstimateExceedShort estimateExceedShort) {
        if (UserConstants.NOT_UNIQUE.equals(iEstimateExceedShortService.checkUnique(estimateExceedShort))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.estimateExceedShort.checkUnique"));
        }
        return iEstimateExceedShortService.save(estimateExceedShort);
    }

    /**
     * 删除预计超欠产
     */
    @ApiOperation("删除,id不为空）")
    @RequiresPermissions("monthplan:estimateExceedShort:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iEstimateExceedShortService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验预计超欠产唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(EstimateExceedShort estimateExceedShort) {
        return iEstimateExceedShortService.checkUnique(estimateExceedShort);
    }

    @ApiOperation("修改预计超欠数")
    @PostMapping("/updateExceedShortQty")
    @ResponseBody
    public AjaxResult updateExceedShortQty(@RequestParam("id") Long id, @RequestParam("exceedShortQty") int exceedShortQty) {
        EstimateExceedShort estimateExceedShort = new EstimateExceedShort();
        estimateExceedShort.setId(id);
        estimateExceedShort.setExceedShortQty(exceedShortQty);
        return iEstimateExceedShortService.updateExceedShortQty(estimateExceedShort);
    }

    @Override
    public String getExportTemplateFileName() {
        return I18nUtil.getMessage("ui.data.column.estimateExceedShort.modelName");
    }

    @Override
    public String getExportTemplateFileNameForce() {
        return I18nUtil.getMessage("ui.data.column.estimateExceedShort.modelName", Locale.SIMPLIFIED_CHINESE);
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<EstimateExceedShort> util = new ExcelUtil<>(EstimateExceedShort.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getProcedureCode() {
        return "0";
    }

    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.estimateExceedShort.modelName");
    }

    @ResponseBody
    @Override
    public List<EstimateExceedShort> exportDataByFeign(EstimateExceedShort entity) {
        return iEstimateExceedShortService.getList(entity);
    }

    // @Override
    // public AjaxResult importDataByFeign(List list, boolean updateSupport, Long importLogId) {
    //     return iEstimateExceedShortService.importData(list, true, importLogId);
    // }

    @RequiresPermissions("monthplan:estimateExceedShort:export")
    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, EstimateExceedShort entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iEstimateExceedShortService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @RequiresPermissions("monthplan:estimateExceedShort:import")
    @PostMapping({"/importData"})
    @ResponseBody
    @ApiOperation("数据导入")
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        return iEstimateExceedShortService.importData(context, updateSupport);
    }
}

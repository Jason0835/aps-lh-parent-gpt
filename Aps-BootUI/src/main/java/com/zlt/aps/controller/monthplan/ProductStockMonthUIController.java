package com.zlt.aps.controller.monthplan;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.entity.ProductStockMonth;
import com.zlt.aps.monthplan.api.service.IProductStockMonthRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ProductStockMonthUIController.java
 * 描    述：物料月库存信息 UI控制层类：....
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-03-12
 */
@Slf4j
@Api(tags = "物料月库存信息")
@Controller
@RequestMapping("/monthplan/monthStock")
public class ProductStockMonthUIController extends BaseUIController<ProductStockMonth> {

    @Autowired
    private IProductStockMonthRemoteService iProductStockMonthService;

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:monthStock:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(ProductStockMonth productStockMonth) {
        return iProductStockMonthService.list(productStockMonth);
    }

    // /**
    //  * 修改或新增
    //  */
    // @ApiOperation("修改或新增")
    // @RequiresPermissions("monthplan:monthStock:edit")
    // @PostMapping("/save")
    // @ResponseBody
    // public AjaxResult save(ProductStockMonth productStockMonth) {
    //     if (UserConstants.NOT_UNIQUE.equals(iProductStockMonthService.checkUnique(productStockMonth))) {
    //         return AjaxResult.error(I18nUtil.getMessage("ui.data.column.productStockMonth.checkUnique"));
    //     }
    //
    //     return iProductStockMonthService.save(productStockMonth);
    // }

    // /**
    //  * 删除物料月库存信息
    //  */
    // @ApiOperation("删除,id不为空")
    // @RequiresPermissions("monthplan:monthStock:remove")
    // @PostMapping("/remove")
    // @ResponseBody
    // public AjaxResult remove(String ids) {
    //     Long[] arr = Convert.toLongArray(ids);
    //     return iProductStockMonthService.removeByIds(Arrays.asList(arr));
    // }

    /**
     * 校验物料月库存信息唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(ProductStockMonth productStockMonth) {
        return iProductStockMonthService.checkUnique(productStockMonth);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     *
     * @return
     */
    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
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
        return I18nUtil.getMessage("ui.data.column.monthStock.modelName");
    }

    /**
     * 抓取
     */
    @ApiOperation("抓取")
    @RequiresPermissions("monthplan:monthStock:craw")
    @PostMapping("/craw")
    @ResponseBody
    public AjaxResult craw(ProductStockMonth productStockMonth) {
        // todo 补充抓取的相关接口
        return AjaxResult.error("抓取还未实现");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<ProductStockMonth> util = new ExcelUtil<>(ProductStockMonth.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @RequiresPermissions("monthplan:monthStock:export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, ProductStockMonth entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iProductStockMonthService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @PostMapping({"/importData"})
    @RequiresPermissions("monthplan:monthStock:import")
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
        AjaxResult ajaxResult = iProductStockMonthService.importData(context, false);
        return ajaxResult;
    }
}

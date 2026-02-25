package com.zlt.aps.controller.monthplan;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.mp.api.domain.entity.ProductionMonthPlanInit;
import com.zlt.aps.mp.api.service.IProductionMonthPlanInitRemoteService;
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
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ProductionMonthPlanInitUIController.java
 * 描    述：分厂月生产计划排产过程-计划初始化 UI控制层类：....
 *@author zlt
 *@date 2025-03-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Api(tags = "分厂月生产计划排产过程-计划初始化")
@Controller
@RequestMapping("/monthplan/productionMonthPlanInit")
public class ProductionMonthPlanInitUIController extends BaseUIController<ProductionMonthPlanInit> {

    @Autowired
    private IProductionMonthPlanInitRemoteService iProductionMonthPlanInitService;

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:productionMonthPlanInit:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(ProductionMonthPlanInit productionMonthPlanInit) {
        return iProductionMonthPlanInitService.list(productionMonthPlanInit);
    }

    // /**
    //  * 修改或新增
    //  */
    // @ApiOperation("修改或新增")
    // @RequiresPermissions("monthplan:productionMonthPlanInit:edit")
    // @PostMapping("/save")
    // @ResponseBody
    // public AjaxResult save(ProductionMonthPlanInit productionMonthPlanInit) {
    //     if (UserConstants.NOT_UNIQUE.equals(iProductionMonthPlanInitService.checkUnique(productionMonthPlanInit))) {
    //         return AjaxResult.error(I18nUtil.getMessage("ui.data.column.productionMonthPlanInit.checkUnique"));
    //     }
    //
    //     return iProductionMonthPlanInitService.save(productionMonthPlanInit);
    // }
    //
    // /**
    //  * 删除分厂月生产计划排产过程-计划初始化
    //  */
    // @ApiOperation("删除,id不为空")
    // @RequiresPermissions("monthplan:productionMonthPlanInit:remove")
    // @PostMapping("/remove")
    // @ResponseBody
    // public AjaxResult remove(String ids) {
    //     Long[] arr = Convert.toLongArray(ids);
    //     return iProductionMonthPlanInitService.removeByIds(Arrays.asList(arr));
    // }
    //
    // /**
    //  * 校验分厂月生产计划排产过程-计划初始化唯一性
    //  */
    // @ApiOperation("校验唯一性")
    // @PostMapping("/checkUnique")
    // @ResponseBody
    // public String checkUnique(ProductionMonthPlanInit productionMonthPlanInit) {
    //     return iProductionMonthPlanInitService.checkUnique(productionMonthPlanInit);
    // }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     * @return
     */
    @Override
    public String getExportTemplateFileName(){
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
        return I18nUtil.getMessage("ui.data.column.productionMonthPlanInit.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<ProductionMonthPlanInit> util = new ExcelUtil<>(ProductionMonthPlanInit.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, ProductionMonthPlanInit entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iProductionMonthPlanInitService.exportData(entity,fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    // @PostMapping({"/importData"})
    // @ResponseBody
    // @ApiOperation("数据导入")
    // @Override
    // public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
    //     byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
    //
    //     ImportContext context = new ImportContext();
    //     context.setImportFilePath(this.importFilePath);
    //     context.setFunctionName(this.getFunctionName());
    //     context.setProcedureCode(this.getProcedureCode());
    //     context.setOriFileName(file.getOriginalFilename());
    //     context.setFileBytes(data);
    //     AjaxResult ajaxResult = iProductionMonthPlanInitService.importData(context,false);
    //     return ajaxResult;
    // }
}

package com.zlt.aps.controller.monthplan;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.entity.SaleMonthPlanRequire;
import com.zlt.aps.monthplan.api.service.ISaleMonthPlanRequireRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：SaleMonthPlanRequireUIController.java
 * 描    述：月度生产需求计划 UI控制层类
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-14
 */
@Slf4j
@Controller
@RequestMapping("/demand/requireProductionPlan")
@Api(tags = "月度制造需求计划服务")
@RequiredArgsConstructor
public class SaleMonthPlanRequireUIController extends BaseUIController<SaleMonthPlanRequire> {

    private final ISaleMonthPlanRequireRemoteService iSaleMonthPlanRequireService;

    /**
     * 根据条件查询销售生产需求排产计划数据
     */
    @ResponseBody
    @PostMapping("/list")
    @RequiresPermissions("monthplan:require:list")
    @ApiOperation("根据条件查询主表数据")
    public TableDataInfo list(SaleMonthPlanRequire saleMonthPlanRequire) {
        return iSaleMonthPlanRequireService.list(saleMonthPlanRequire);
    }

    /**
     * 删除月度生产需求计划
     */
    @ResponseBody
    @PostMapping("/remove")
    @ApiOperation("删除,id不为空）")
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iSaleMonthPlanRequireService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 查询对应年月+分厂的需求计划版本
     */
    @ResponseBody
    @PostMapping("/versionList")
    @ApiOperation("查询对应年月+分厂的需求计划版本")
    public AjaxResult versionList(SaleMonthPlanRequire saleMonthPlanRequire) {
        return iSaleMonthPlanRequireService.versionList(saleMonthPlanRequire);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     *
     * @return
     */
    @Override
    public String getExportTemplateFileName() {
        return I18nUtil.getMessage("ui.data.column.require.modelName");
    }


    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getProcedureCode() {
        return "月度制造需求计划";
    }

    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.require.modelName");
    }

    @Override
    @ResponseBody
    @RequiresPermissions("monthplan:require:export")
    @GetMapping({"/export"})
    @ApiOperation("导出销售月度生产需求排产计划")
    public void export(HttpServletResponse response, SaleMonthPlanRequire entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iSaleMonthPlanRequireService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @Override
    @ResponseBody
    @PostMapping({"/importData"})
    @ApiOperation("销售月度生产需求排产计划导入")
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iSaleMonthPlanRequireService.importData(context, false);
        return ajaxResult;
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<SaleMonthPlanRequire> util = new ExcelUtil<>(SaleMonthPlanRequire.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 根据条件查询统计数据
     *
     * @param queryVO 查询条件
     * @return 结果
     */
    @ApiOperation("根据条件查询统计数据")
    @PostMapping("/getSummaryVo")
    @ResponseBody
    public AjaxResult getSummaryVo(SaleMonthPlanRequire queryVO) {
        return iSaleMonthPlanRequireService.getSummaryVo(queryVO);
    }
}

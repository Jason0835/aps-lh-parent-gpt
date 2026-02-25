package com.zlt.aps.controller.monthplan;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.mp.api.domain.entity.MonthPlanSaleOrder;
import com.zlt.aps.mp.api.domain.itf.InSaleOrderDto;
import com.zlt.aps.mp.api.service.IMonthPlanSaleOrderRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
 * 文件名称：MonthPlanSaleOrderUIController.java
 * 描    述：月度销售计划订单 UI控制层类
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-14
 */
@Slf4j
@Controller
@RequestMapping("/demand/monthSaleOrderPlan")
@Api(tags = "月度销售计划订单-原始订单前端操作服务")
public class MonthPlanSaleOrderUIController extends BaseUIController<MonthPlanSaleOrder> {

    private final IMonthPlanSaleOrderRemoteService iMonthPlanSaleOrderService;

    public MonthPlanSaleOrderUIController(IMonthPlanSaleOrderRemoteService iMonthPlanSaleOrderService) {
        this.iMonthPlanSaleOrderService = iMonthPlanSaleOrderService;
    }

    /**
     * 根据条件查询主表数据
     */
    @ResponseBody
    @PostMapping("/list")
    @ApiOperation("根据条件查询原始销售订单数据")
    public TableDataInfo list(MonthPlanSaleOrder monthPlanSaleOrder) {
        return iMonthPlanSaleOrderService.list(monthPlanSaleOrder);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("monthplan:monthSaleOrderPlan:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(MonthPlanSaleOrder monthPlanSaleOrder) {
        AjaxResult ajaxResult;
        if (UserConstants.NOT_UNIQUE.equals(iMonthPlanSaleOrderService.checkMonthPlanSaleOrderUnique(monthPlanSaleOrder))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanSaleOrder.checkUnique"));
        }
        if (monthPlanSaleOrder.getId() != null) {
            ajaxResult = iMonthPlanSaleOrderService.edit(monthPlanSaleOrder);
        } else {
            ajaxResult = iMonthPlanSaleOrderService.add(monthPlanSaleOrder);
        }
        return ajaxResult;
    }

    /**
     * 删除月度销售计划订单
     */
    @RequiresPermissions("monthplan:monthSaleOrderPlan:remove")
    @ResponseBody
    @PostMapping("/remove")
    @ApiOperation("删除月度销售计划订单,id不为空）")
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMonthPlanSaleOrderService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     *
     * @return
     */
    @Override
    public String getExportTemplateFileName() {
        return "月度销售计划订单";
    }


    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getProcedureCode() {
        return "月度销售计划订单";
    }

    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getFunctionName() {
        return "月度销售计划订单";
    }

    @Override
    @ResponseBody
    @GetMapping({"/export"})
    @ApiOperation("月度销售计划订单导出")
    public void export(HttpServletResponse response, MonthPlanSaleOrder entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iMonthPlanSaleOrderService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<MonthPlanSaleOrder> util = new ExcelUtil<>(MonthPlanSaleOrder.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @Override
    @ResponseBody
    @PostMapping({"/importData"})
    @ApiOperation("导入月度销售计划订单")
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iMonthPlanSaleOrderService.importData(context, updateSupport);
        return ajaxResult;
    }

    /**
     * 内销销售订单同步
     *
     * @param inSaleOrderDto 年月
     * @return 结果
     */
    @RequiresPermissions("monthplan:syncInSaleOrder:sync")
    @ApiOperation("内销销售订单同步")
    @PostMapping("/syncInSaleOrder")
    @ResponseBody
    public AjaxResult syncInSaleOrder(InSaleOrderDto inSaleOrderDto) {
        return iMonthPlanSaleOrderService.syncInSaleOrder(inSaleOrderDto);
    }

    /**
     * 外销销售订单同步
     *
     * @param inSaleOrderDto 年月
     * @return 结果
     */
    @RequiresPermissions("monthplan:syncOutSaleOrder:sync")
    @ApiOperation("外销销售订单同步")
    @PostMapping("/syncOutSaleOrder")
    @ResponseBody
    public AjaxResult syncOutSaleOrder(InSaleOrderDto inSaleOrderDto) {
        return iMonthPlanSaleOrderService.syncOutSaleOrder(inSaleOrderDto);
    }
}

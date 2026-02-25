package com.zlt.aps.controller.monthplan;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.mp.api.domain.entity.OrderPlanAllocation;
import com.zlt.aps.mp.api.service.IOrderPlanAllocationRemoteService;
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
 * 文件名称：OrderPlanAllocationUIController.java
 * 描    述：月度销售计划订单分配结果 UI控制层类：....
 *@author ZLT
 *@date 2025-03-10
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：ZLT
 *     修改内容：...
 */
@Slf4j
@Api(tags = "月度销售计划订单分配结果")
@Controller
@RequestMapping("/monthplan/SaleOrderAllocation")
public class OrderPlanAllocationUIController extends BaseUIController<OrderPlanAllocation> {

    @Autowired
    private IOrderPlanAllocationRemoteService iOrderPlanAllocationService;

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:SaleOrderAllocation:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(OrderPlanAllocation orderPlanAllocation) {
        return iOrderPlanAllocationService.list(orderPlanAllocation);
    }

    /**
     * 根据查询条件查询统计数据
     */
    @ApiOperation("根据查询条件查询统计数据")
    @PostMapping("/getSummaryVo")
    @ResponseBody
    public AjaxResult getSummaryVo(OrderPlanAllocation orderPlanAllocation) {
        return iOrderPlanAllocationService.getSummaryVo(orderPlanAllocation);
    }

    /**
     * 查询对应年月+分厂的需求计划版本
     */
    @ResponseBody
    @PostMapping("/versionList")
    @ApiOperation("查询对应年月+分厂的需求计划版本")
    public AjaxResult versionList(OrderPlanAllocation query) {
        return iOrderPlanAllocationService.versionList(query);
    }

    // /**
    //  * 修改或新增
    //  */
    // @ApiOperation("修改或新增")
    // @RequiresPermissions("monthplan:SaleOrderAllocation:edit")
    // @PostMapping("/save")
    // @ResponseBody
    // public AjaxResult save(OrderPlanAllocation orderPlanAllocation) {
    //     AjaxResult ajaxResult = null;
    //     if (UserConstants.NOT_UNIQUE.equals(iOrderPlanAllocationService.checkUnique(orderPlanAllocation))) {
    //         return ajaxResult.error(I18nUtil.getMessage("ui.data.column.orderPlanAllocation.checkUnique"));
    //     }
    //
    //     return iOrderPlanAllocationService.save(orderPlanAllocation);
    // }
    //
    // /**
    //  * 删除月度销售计划订单分配结果
    //  */
    // @ApiOperation("删除,id不为空）")
    // @RequiresPermissions("monthplan:SaleOrderAllocation:remove")
    // @PostMapping("/remove")
    // @ResponseBody
    // public AjaxResult remove(String ids) {
    //     Long[] arr = Convert.toLongArray(ids);
    //     return iOrderPlanAllocationService.removeByIds(Arrays.asList(arr));
    // }
    //
    // /**
    //  * 校验月度销售计划订单分配结果唯一性
    //  */
    // @ApiOperation("校验唯一性")
    // @PostMapping("/checkUnique")
    // @ResponseBody
    // public String checkUnique(OrderPlanAllocation orderPlanAllocation) {
    //     return iOrderPlanAllocationService.checkUnique(orderPlanAllocation);
    // }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     * @return
     */
    @Override
    public String getExportTemplateFileName(){
        return I18nUtil.getMessage("ui.data.column.SaleOrderAllocation.modelName");
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
        return I18nUtil.getMessage("ui.data.column.SaleOrderAllocation.modelName");
    }

    @RequiresPermissions("monthplan:SaleOrderAllocation:export")
    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, OrderPlanAllocation entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iOrderPlanAllocationService.exportData(entity,fileName);
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
    //     AjaxResult ajaxResult = iOrderPlanAllocationService.importData(context,false);
    //     return ajaxResult;
    // }
}

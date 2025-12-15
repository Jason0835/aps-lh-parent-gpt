package com.zlt.aps.controller.monthplan;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.ruoyi.common4ui.exception.base.BaseException;
import com.zlt.aps.monthplan.api.domain.entity.SupplyOrderPool;
import com.zlt.aps.monthplan.api.service.ISupplyOrderPoolRemoteService;
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
 * 文件名称：SupplyOrderPoolUIController.java
 * 描    述：供应链订单池 UI控制层类：....
 *@author zlt
 *@date 2025-12-06
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Api(tags = "供应链订单池")
@Controller
@RequestMapping("/monthplan/supplyOrderPool")
public class SupplyOrderPoolUIController extends BaseUIController<SupplyOrderPool> {

    @Autowired
    private ISupplyOrderPoolRemoteService iSupplyOrderPoolService;

  /**
     * 根据条件查询供应链订单池列表
     */
    @ApiOperation("根据条件查询供应链订单池列表")
    @RequiresPermissions("monthplan:pool:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(SupplyOrderPool entity) {
        return iSupplyOrderPoolService.list(entity);
    }

    /**
     * 修改或新增供应链订单池
     */
    @ApiOperation("修改或新增供应链订单池")
    @RequiresPermissions("monthplan:pool:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(SupplyOrderPool supplyOrderPool) {
        AjaxResult ajaxResult;
        if (UserConstants.NOT_UNIQUE.equals(iSupplyOrderPoolService.checkSupplyOrderPoolUnique(supplyOrderPool))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.supplyOrderPool.checkUnique"));
        }
        if (supplyOrderPool.getId() != null){
            ajaxResult = iSupplyOrderPoolService.edit(supplyOrderPool);
        } else{
            ajaxResult = iSupplyOrderPoolService.add(supplyOrderPool);
        }
        return ajaxResult;
    }

    /**
     * 删除供应链订单池
     */
    @ApiOperation("删除供应链订单池（id不为空）")
    @RequiresPermissions("monthplan:pool:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iSupplyOrderPoolService.remove(arr);
    }

    /**
     * 校验供应链订单池唯一性
     */
    @ApiOperation("校验供应链订单池唯一性")
    @PostMapping("/checkSupplyOrderPoolUnique")
    @ResponseBody
    public String checkSupplyOrderPoolUnique(SupplyOrderPool supplyOrderPool) {
        return iSupplyOrderPoolService.checkSupplyOrderPoolUnique(supplyOrderPool);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     * @return
     */
    @Override
    public String getExportTemplateFileName(){
        throw new BaseException("没有定义导出模板的文件名");
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
        return I18nUtil.getMessage("ui.no.export.sheetName");
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, SupplyOrderPool entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iSupplyOrderPoolService.exportData(entity,fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

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
        AjaxResult ajaxResult = iSupplyOrderPoolService.importData(context,false);
        return ajaxResult;
    }

  /**
   * 生成周期排产储备
   */
  @ApiOperation("生成周期排产储备")
  @PostMapping("/createCycleStockUp")
  @ResponseBody
  public AjaxResult createCycleStockUp(SupplyOrderPool supplyOrderPool) {
    return iSupplyOrderPoolService.createCycleStockUp(supplyOrderPool);
  }

  /**
   * 生成常规储备
   */
  @ApiOperation("生成常规储备")
  @PostMapping("/createPrecedentStockUp")
  @ResponseBody
  public AjaxResult createPrecedentStockUp(SupplyOrderPool supplyOrderPool) {
    return iSupplyOrderPoolService.createPrecedentStockUp(supplyOrderPool);
  }

}

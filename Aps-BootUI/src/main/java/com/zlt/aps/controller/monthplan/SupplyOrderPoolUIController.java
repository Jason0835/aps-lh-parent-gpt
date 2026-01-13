package com.zlt.aps.controller.monthplan;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
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
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：SupplyOrderPoolUIController.java
 * 描    述：供应链订单池 UI控制层类：....
 *@author yelq
 *@date 2025-12-22
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Api(tags = "供应链订单池")
@Controller
@RequestMapping("/monthplan/supplyOrderPool")
public class SupplyOrderPoolUIController extends BaseUIController<SupplyOrderPool> {

    @Autowired
    private ISupplyOrderPoolRemoteService iSupplyOrderPoolService;

    private final String prefix = "system/monthplan/supplyOrderPool";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("monthplan:supplyOrderPool:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/supplyOrderPool";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("supplyOrderPool", new SupplyOrderPool());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("supplyOrderPool", iSupplyOrderPoolService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:supplyOrderPool:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(SupplyOrderPool supplyOrderPool) {
        return iSupplyOrderPoolService.list(supplyOrderPool);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("monthplan:supplyOrderPool:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(SupplyOrderPool supplyOrderPool) {
        if (UserConstants.NOT_UNIQUE.equals(iSupplyOrderPoolService.checkUnique(supplyOrderPool))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.supplyOrderPool.checkUnique"));
        }

        return iSupplyOrderPoolService.save(supplyOrderPool);
    }

    /**
     * 新增周期排产储备时候，输入储备数量的时候，需要加一个提示用户无订单库存有多少，月底计划余量有多少
     * @param supplyOrderPool 入参
     * @return AjaxResult
     */
    @ApiOperation("新增周期排产储备时候，输入储备数量的时候，需要加一个提示用户无订单库存有多少，月底计划余量有多少")
    @PostMapping("/queryStockUpByMaterialCode")
    @ResponseBody
    public AjaxResult queryStockUpByMaterialCode(@RequestBody SupplyOrderPool supplyOrderPool){
        return AjaxResult.success(iSupplyOrderPoolService.queryStockUpByMaterialCode(supplyOrderPool));
    }


    /**
     * 删除供应链订单池
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("monthplan:supplyOrderPool:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iSupplyOrderPoolService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验供应链订单池唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(SupplyOrderPool supplyOrderPool) {
        return iSupplyOrderPoolService.checkUnique(supplyOrderPool);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     * @return
     */
    @Override
    public String getExportTemplateFileName(){
        return I18nUtil.getMessage("ui.data.column.supplyOrderPool.modelName");
    }


    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getProcedureCode() {
        return I18nUtil.getMessage("ui.data.column.supplyOrderPool.modelName");
    }

    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.supplyOrderPool.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<SupplyOrderPool> util = new ExcelUtil<>(SupplyOrderPool.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
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
    @RequiresPermissions("monthplan:supplyOrderPool:createCycleStockUp")
    @ResponseBody
    public AjaxResult createCycleStockUp(SupplyOrderPool supplyOrderPool) {
        return iSupplyOrderPoolService.createCycleStockUp(supplyOrderPool);
    }

    /**
     * 生成常规储备
     */
    @ApiOperation("生成常规储备")
    @RequiresPermissions("monthplan:supplyOrderPool:createPrecedentStockUp")
    @PostMapping("/createPrecedentStockUp")
    @ResponseBody
    public AjaxResult createPrecedentStockUp(SupplyOrderPool supplyOrderPool) {
        return iSupplyOrderPoolService.createPrecedentStockUp(supplyOrderPool);
    }

    /**
     * 输入物料编码，带出对应信息
     */
    @ApiOperation("输入物料编码，带出对应信息")
    @PostMapping("/queryRelationByMaterialCode")
    @ResponseBody
    public AjaxResult queryRelationByMaterialCode(SupplyOrderPool supplyOrderPool)
    {
        return AjaxResult.success(iSupplyOrderPoolService.queryRelationByMaterialCode(supplyOrderPool));
    }

    /**
     * 超期校验
     */
    @ApiOperation("超期校验")
    @PostMapping("/checkOverdue")
    @ResponseBody
    public AjaxResult checkOverdue(SupplyOrderPool supplyOrderPool)
    {
        return iSupplyOrderPoolService.checkOverdue(supplyOrderPool);
    }
}

package com.zlt.aps.controller.monthplan;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.mp.api.domain.dto.SalesOrderPoolDto;
import com.zlt.aps.mp.api.domain.entity.SalesOrderPool;
import com.zlt.aps.mp.api.service.ISalesOrderPoolRemoteService;
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
 * 文件名称：SalesOrderPoolUIController.java
 * 描    述：销售订单池 UI控制层类：....
 *@author zlt
 *@date 2025-12-04
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Api(tags = "销售订单池")
@Controller
@RequestMapping("/monthplan/SalesOrderPool")
public class SalesOrderPoolUIController extends BaseUIController<SalesOrderPool> {

    @Autowired
    private ISalesOrderPoolRemoteService iSalesOrderPoolService;

    private final String prefix = "monthplan/SalesOrderPool";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("monthplan:SalesOrderPool:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/SalesOrderPool";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("salesOrderPool", new SalesOrderPool());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("salesOrderPool", iSalesOrderPoolService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:SalesOrderPool:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(SalesOrderPool salesOrderPool) {
        return iSalesOrderPoolService.list(salesOrderPool);
    }

    /**
     * 修改，仅能更新供应链优先级
     */
    @ApiOperation("修改")
    @RequiresPermissions("monthplan:SalesOrderPool:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(SalesOrderPoolDto salesOrderPool) {
    	if (salesOrderPool.getId() == null) { // 新增直接返回
    		return AjaxResult.success();
    	}
    	// 重新赋值对象，仅能更新供应链优先级
    	SalesOrderPool updateInfo = new SalesOrderPool();
    	updateInfo.setId(salesOrderPool.getId());
    	updateInfo.setScmPriority(salesOrderPool.getScmPriority());
        return iSalesOrderPoolService.save(updateInfo);
    }



    /**
     * 批量修改同PO号的销售优先级
     */
    @ApiOperation("批量修改同PO号的销售优先级")
    @RequiresPermissions("monthplan:SalesOrderPool:edit")
    @PostMapping("/editBySalCodePo")
    @ResponseBody
    public AjaxResult editBySalCodePo(SalesOrderPool salesOrderPool) {
    	if (StringUtils.isEmpty(salesOrderPool.getSalCodePo())) {
            return AjaxResult.error("请输入PO号！");
    	}
    	return iSalesOrderPoolService.editBySalCodePo(salesOrderPool);
    }

	/**
	 * 锁定订单池
	 * @return
	 */
    @ApiOperation("锁定订单池")
    @RequiresPermissions( "monthplan:SalesOrderPool:lock")
    @PostMapping("/lockSalesOrderPool")
    @ResponseBody
    public AjaxResult lockSalesOrderPool(SalesOrderPool salesOrderPool){
    	if (salesOrderPool.getYear() == null || salesOrderPool.getMonth() == null) {
            return AjaxResult.error("请输入正确的年月！");
    	}
        return iSalesOrderPoolService.lockSalesOrderPool(salesOrderPool);
    }

	/**
	 * 解锁订单池
	 * @return 结果
	 */
    @ApiOperation("解锁订单池")
    @RequiresPermissions("monthplan:SalesOrderPool:unlock")
    @PostMapping("/unlockSalesOrderPool")
    @ResponseBody
    public AjaxResult unlockSalesOrderPool(SalesOrderPool salesOrderPool){
    	if (salesOrderPool.getYear() == null || salesOrderPool.getMonth() == null) {
            return AjaxResult.error("请输入正确的年月！");
    	}
        return iSalesOrderPoolService.unlockSalesOrderPool(salesOrderPool);
    }

    /**
     * 删除销售订单池
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("monthplan:SalesOrderPool:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iSalesOrderPoolService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 检查SCM数据
     */
    @ApiOperation("检查SCM数据")
    @RequiresPermissions("monthplan:SalesOrderPool:getSCMData")
    @PostMapping("/checkSCMData")
    @ResponseBody
    public AjaxResult checkSCMData(SalesOrderPool salesOrderPool) {
        return iSalesOrderPoolService.checkSCMData(salesOrderPool);
    }

    /**
     * 抓取SCM数据
     */
    @ApiOperation("抓取SCM数据")
    @RequiresPermissions("monthplan:SalesOrderPool:getSCMData")
    @PostMapping("/getSCMData")
    @ResponseBody
    public AjaxResult getSCMData(SalesOrderPool salesOrderPool) {
        return iSalesOrderPoolService.getSCMData(salesOrderPool);
    }

    /**
     * 校验销售订单池唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(SalesOrderPool salesOrderPool) {
        return iSalesOrderPoolService.checkUnique(salesOrderPool);
    }

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
        return I18nUtil.getMessage("ui.data.column.SalesOrderPool.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<SalesOrderPool> util = new ExcelUtil<>(SalesOrderPool.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, SalesOrderPool entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iSalesOrderPoolService.exportData(entity,fileName);
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
        AjaxResult ajaxResult = iSalesOrderPoolService.importData(context,false);
        return ajaxResult;
    }
}

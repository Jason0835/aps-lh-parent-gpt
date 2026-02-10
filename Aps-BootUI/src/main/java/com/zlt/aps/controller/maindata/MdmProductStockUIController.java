package com.zlt.aps.controller.maindata;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.monthplan.api.domain.entity.IMdmProductStockRemoteService;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;
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
import java.text.ParseException;
import java.util.Arrays;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmProductStockUIController.java
 * 描    述：成品库存 UI控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-22
 */
@Slf4j
@Api(tags = "成品库存")
@Controller
@RequestMapping("/monthplan/mdmProductStock")
public class MdmProductStockUIController extends BaseUIController<MdmProductStock> {

    private final String prefix = "aps/monthplan/mdmProductStock";
    @Autowired
    private IMdmProductStockRemoteService iMdmProductStockService;

    @Autowired
    private IMesItfService iMesItfService;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("monthplan:mdmProductStock:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/mdmProductStock";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("mdmProductStock", new MdmProductStock());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("mdmProductStock", iMdmProductStockService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:mdmFinishStock:list4Mes")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MdmProductStock mdmProductStock) {
        return iMdmProductStockService.list(mdmProductStock);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("monthplan:mdmProductStock:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(MdmProductStock mdmProductStock) {
        if (UserConstants.NOT_UNIQUE.equals(iMdmProductStockService.checkUnique(mdmProductStock))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mdmProductStock.checkUnique"));
        }

        return iMdmProductStockService.save(mdmProductStock);
    }

    /**
     * 删除成品库存
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("monthplan:mdmProductStock:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMdmProductStockService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验成品库存唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(MdmProductStock mdmProductStock) {
        return iMdmProductStockService.checkUnique(mdmProductStock);
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
        return I18nUtil.getMessage("ui.data.column.productStock.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<MdmProductStock> util = new ExcelUtil<>(MdmProductStock.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @RequiresPermissions("monthplan:mdmFinishStock:export4Mes")
    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, MdmProductStock entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iMdmProductStockService.exportData(entity, fileName);
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
        AjaxResult ajaxResult = iMdmProductStockService.importData(context, updateSupport);
        return ajaxResult;
    }

    /**
     * 生成超期SKU
     * @param mdmProductStock 参数
     * @return 结果
     */
    @RequiresPermissions("monthplan:mdmFinishStock:genOverDueSkuByStock")
    @ApiOperation("生成超期SKU")
    @PostMapping("/genOverDueSkuByStock")
    @ResponseBody
    public AjaxResult genOverDueSkuByStock(MdmProductStock mdmProductStock) throws ParseException {
        return iMesItfService.genOverDueSkuByStock(mdmProductStock);
    }
}

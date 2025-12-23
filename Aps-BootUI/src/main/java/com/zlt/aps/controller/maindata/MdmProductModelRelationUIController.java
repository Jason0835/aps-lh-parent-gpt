package com.zlt.aps.controller.maindata;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.dto.ProductMouldConfigurationParam;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuMouldRel;
import com.zlt.aps.monthplan.api.domain.vo.ProductMouldInfoVo;
import com.zlt.aps.monthplan.api.service.IMdmProductModelRelationRemoteService;
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
 * 文件名称：MdmProductModelRelationUIController.java
 * 描    述：SKU与模具关系 UI控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-24
 */
@Slf4j
@Api(tags = "SKU与模具关系")
@Controller
@RequestMapping("/maindata/relation")
public class MdmProductModelRelationUIController extends BaseUIController<MdmSkuMouldRel> {

    @Autowired
    private IMdmProductModelRelationRemoteService iMdmProductModelRelationService;

    private final String prefix = "aps/maindata/relation";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("maindata:relation:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/relation";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("MdmSkuMouldRel", new MdmSkuMouldRel());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("MdmSkuMouldRel", iMdmProductModelRelationService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("maindata:relation:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MdmSkuMouldRel MdmSkuMouldRel) {
        return iMdmProductModelRelationService.list(MdmSkuMouldRel);
    }

    /**
     * 根据物料获取分厂年月的匹配模具
     *
     * @param queryParam
     * @return
     */
    @ResponseBody
    @ApiOperation("物料匹配的模具")
    @PostMapping("/matchMouldConfiguration")
    public ProductMouldInfoVo getProductMouldConfiguration(@RequestBody ProductMouldConfigurationParam queryParam) {
        if (null == queryParam) {
            return null;
        }
        return iMdmProductModelRelationService.getProductMouldConfiguration(queryParam);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("maindata:relation:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(MdmSkuMouldRel MdmSkuMouldRel) {
        if (UserConstants.NOT_UNIQUE.equals(iMdmProductModelRelationService.checkUnique(MdmSkuMouldRel))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.productmodelrelation.notUnique"));
        }
        return iMdmProductModelRelationService.save(MdmSkuMouldRel);
    }

    /**
     * 删除SKU与模具关系
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("maindata:relation:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMdmProductModelRelationService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验SKU与模具关系唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(MdmSkuMouldRel MdmSkuMouldRel) {
        return iMdmProductModelRelationService.checkUnique(MdmSkuMouldRel);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     *
     * @return
     */
    @Override
    public String getExportTemplateFileName() {
        return I18nUtil.getMessage("ui.data.column.relation.modelName");
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
        return this.getExportTemplateFileName();
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<MdmSkuMouldRel> util = new ExcelUtil<>(MdmSkuMouldRel.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, MdmSkuMouldRel entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iMdmProductModelRelationService.exportData(entity, fileName);
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
        AjaxResult ajaxResult = iMdmProductModelRelationService.importData(context, true);
        return ajaxResult;
    }

    /**
     * 抓取MES数据
     */
    @RequiresPermissions("maindata:relation:mesCapture")
    @ApiOperation("抓取MES数据")
    @PostMapping("/mesCapture")
    @ResponseBody
    public AjaxResult mesCapture() {
        return iMdmProductModelRelationService.mesCapture();
    }
}

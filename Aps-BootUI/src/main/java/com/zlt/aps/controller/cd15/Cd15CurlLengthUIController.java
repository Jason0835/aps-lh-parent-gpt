package com.zlt.aps.controller.cd15;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd15.api.domain.entity.Cd15CurlLength;
import com.zlt.aps.cd15.api.service.ICd15CurlLengthRemoteService;
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
 * 文件名称：Cd15CurlLengthUIController.java
 * 描    述：钢丝斜裁卷曲长度 UI控制层类：....
 *@author zlt
 *@date 2025-03-11
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Api(tags = "钢丝斜裁卷曲长度")
@Controller
@RequestMapping("/cd15/cd15CurlLength")
public class Cd15CurlLengthUIController extends BaseUIController<Cd15CurlLength> {

    @Autowired
    private ICd15CurlLengthRemoteService iCd15CurlLengthService;

    private final String prefix = "aps/cd15/cd15CurlLength";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cd15:cd15CurlLength:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/cd15CurlLength";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("cd15CurlLength", new Cd15CurlLength());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cd15CurlLength", iCd15CurlLengthService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("cd15:cd15CurlLength:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd15CurlLength cd15CurlLength) {
        return iCd15CurlLengthService.list(cd15CurlLength);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("cd15:cd15CurlLength:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(Cd15CurlLength cd15CurlLength) {
        if (UserConstants.NOT_UNIQUE.equals(iCd15CurlLengthService.checkUnique(cd15CurlLength))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15CurlLength.checkUnique"));
        }

        return iCd15CurlLengthService.save(cd15CurlLength);
    }

    /**
     * 删除钢丝斜裁卷曲长度
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("cd15:cd15CurlLength:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCd15CurlLengthService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验钢丝斜裁卷曲长度唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(Cd15CurlLength cd15CurlLength) {
        return iCd15CurlLengthService.checkUnique(cd15CurlLength);
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
        return I18nUtil.getMessage("ui.data.column.cd15CurlLength.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<Cd15CurlLength> util = new ExcelUtil<>(Cd15CurlLength.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, Cd15CurlLength entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iCd15CurlLengthService.exportData(entity,fileName);
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
        AjaxResult ajaxResult = iCd15CurlLengthService.importData(context,false);
        return ajaxResult;
    }

    /**
     * 根据编号查询卷曲长度
     *
     * @param curlLength 查询条件
     * @return 结果
     */
    @ApiOperation("根据编号查询卷曲长度")
    @PostMapping("/selectCurlLengthByCode")
    @ResponseBody
    public AjaxResult selectCurlLengthByCode(Cd15CurlLength curlLength) {
        return iCd15CurlLengthService.selectCurlLengthByCode(curlLength);
    }
}

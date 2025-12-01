package com.zlt.aps.controller.maindata;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.entity.MdmConstructionInfo;
import com.zlt.aps.monthplan.api.domain.vo.MdmConstructionInfoVo;
import com.zlt.aps.monthplan.api.service.IMdmConstructionInfoRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.BeanUtils;
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
 * 文件名称：MdmConstructionInfoUIController.java
 * 描    述：投产胎胚施工信息 UI控制层类：....
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
@Api(tags = "投产胎胚施工信息")
@Controller
@RequestMapping("/monthplan/mdmConstructionInfo")
public class MdmConstructionInfoUIController extends BaseUIController<MdmConstructionInfoVo> {

    @Autowired
    private IMdmConstructionInfoRemoteService iMdmConstructionInfoService;

    private final String prefix = "aps/monthplan/mdmConstructionInfo";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("monthplan:mdmConstructionInfo:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/mdmConstructionInfo";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("mdmConstructionInfo", new MdmConstructionInfo());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("mdmConstructionInfo", iMdmConstructionInfoService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:mdmConstructionInfo:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MdmConstructionInfo mdmConstructionInfo) {
        return iMdmConstructionInfoService.list(mdmConstructionInfo);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("monthplan:mdmConstructionInfo:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(MdmConstructionInfo mdmConstructionInfo) {
        AjaxResult ajaxResult = null;
        if (UserConstants.NOT_UNIQUE.equals(iMdmConstructionInfoService.checkUnique(mdmConstructionInfo))) {
            return ajaxResult.error(I18nUtil.getMessage("ui.data.column.mdmConstructionInfo.checkUnique"));
        }

        return iMdmConstructionInfoService.save(mdmConstructionInfo);
    }

    /**
     * 删除投产胎胚施工信息
     */
    @ApiOperation("删除,id不为空）")
    @RequiresPermissions("monthplan:mdmConstructionInfo:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMdmConstructionInfoService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验投产胎胚施工信息唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(MdmConstructionInfo mdmConstructionInfo) {
        return iMdmConstructionInfoService.checkUnique(mdmConstructionInfo);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     *
     * @return
     */
    @Override
    public String getExportTemplateFileName() {
        return I18nUtil.getMessage("ui.data.column.productConstruction.modelName");
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
        return I18nUtil.getMessage("ui.data.column.productConstruction.modelName");
    }

    @GetMapping({"/importTemplate"})
    @ApiOperation("下载导入模板")
    @ResponseBody
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        return super.importTemplate(response);
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, MdmConstructionInfoVo entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        MdmConstructionInfo mdmConstructionInfo = new MdmConstructionInfo();
        BeanUtils.copyProperties(entity, mdmConstructionInfo);
        byte[] excelBytes = iMdmConstructionInfoService.exportData(mdmConstructionInfo, fileName);
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
        AjaxResult ajaxResult = iMdmConstructionInfoService.importData(context, false);
        return ajaxResult;
    }
}

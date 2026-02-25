package com.zlt.aps.controller.maindata;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.mp.api.domain.entity.MdmProductConstruction;
import com.zlt.aps.mp.api.service.IMdmProductConstructionRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmProductConstructionUIController.java
 * 描    述：SAP与施工对照 UI控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-25
 */
@Slf4j
@Api(tags = "SAP与施工对照")
@Controller
@RequestMapping("/maindata/mdmProductConstruction")
public class MdmProductConstructionUIController extends BaseUIController<MdmProductConstruction> {

    @Autowired
    private IMdmProductConstructionRemoteService iMdmProductConstructionService;

    private final String prefix = "aps/maindata/mdmProductConstruction";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("maindata:mdmProductConstruction:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/mdmProductConstruction";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("mdmProductConstruction", new MdmProductConstruction());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("mdmProductConstruction", iMdmProductConstructionService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("maindata:mdmProductConstruction:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MdmProductConstruction mdmProductConstruction) {
        return iMdmProductConstructionService.list(mdmProductConstruction);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("maindata:mdmProductConstruction:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(@Validated MdmProductConstruction mdmProductConstruction) {
        if (UserConstants.NOT_UNIQUE.equals(iMdmProductConstructionService.checkUnique(mdmProductConstruction))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.mdmProductConstruction.notUnique"));
        }
        if (mdmProductConstruction.getCuringTime() == null
//                || mdmProductConstruction.getHydraulicPressureCuringTime() == null
        ) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.mdmProductConstruction.curingTime"));
        }

        if (mdmProductConstruction.getCuringTime2() == null
//                || mdmProductConstruction.getHydraulicPressureCuringTime2() == null
        ) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.mdmProductConstruction.curingTime2"));
        }

        //todo 机械液压硫化时间同步
        mdmProductConstruction.setHydraulicPressureCuringTime(mdmProductConstruction.getCuringTime());
        mdmProductConstruction.setHydraulicPressureCuringTime2(mdmProductConstruction.getCuringTime2());
        return iMdmProductConstructionService.save(mdmProductConstruction);
    }

    /**
     * 删除SAP与施工对照
     */
    @ApiOperation("删除,id不为空）")
    @RequiresPermissions("maindata:mdmProductConstruction:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMdmProductConstructionService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<MdmProductConstruction> util = new ExcelUtil<>(MdmProductConstruction.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 校验SAP与施工对照唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(MdmProductConstruction mdmProductConstruction) {
        return iMdmProductConstructionService.checkUnique(mdmProductConstruction);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     *
     * @return
     */
    @Override
    public String getExportTemplateFileName() {
        return I18nUtil.getMessage("ui.data.column.mdmProductConstruction.modelName");
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

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, MdmProductConstruction entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iMdmProductConstructionService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @RequiresPermissions("maindata:mdmProductConstruction:import")
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
        AjaxResult ajaxResult = iMdmProductConstructionService.importData(context, updateSupport);
        return ajaxResult;
    }

    @RequiresPermissions("maindata:mdmProductConstruction:importOfflineData")
    @PostMapping({"/importOfflineData"})
    @ResponseBody
    @ApiOperation("客户格式数据导入")
    public AjaxResult importOfflineData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iMdmProductConstructionService.importOfflineData(context, true);
        return ajaxResult;
    }
}

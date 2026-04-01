package com.zlt.aps.controller.lh;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.lh.api.domain.entity.LhMachineInfo;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.service.ILhMouldChangePlanRemoteService;
import com.zlt.aps.lh.api.service.ILhMachineInfoRemoteService;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.service.IMdmMaterialInfoRemoteService;
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
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhMouldChangePlanUIController.java
 * 描    述：模具交替计划 UI控制层类
 *@author APS Team
 *@date 2026-04-01
 *@version 1.0
 *
 * 修改记录：
 *     修改时间：...
 *     修 改 人：...
 *     修改内容：...
 */
@Slf4j
@Api(tags = "模具交替计划")
@Controller
@RequestMapping("/lh/lhMouldChangePlan")
public class LhMouldChangePlanUIController extends BaseUIController<LhMouldChangePlan> {

    @Autowired
    private ILhMouldChangePlanRemoteService iLhMouldChangePlanService;

    @Autowired
    private ILhMachineInfoRemoteService iLhMachineInfoService;

    @Autowired
    private IMdmMaterialInfoRemoteService iMdmMaterialInfoService;

    private final String prefix = "aps/lh/lhMouldChangePlan";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("lh:lhMouldChangePlan:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/lhMouldChangePlan";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("lhMouldChangePlan", new LhMouldChangePlan());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("lhMouldChangePlan", iLhMouldChangePlanService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("lh:lhMouldChangePlan:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(LhMouldChangePlan lhMouldChangePlan) {
        return iLhMouldChangePlanService.list(lhMouldChangePlan);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("lh:lhMouldChangePlan:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(LhMouldChangePlan lhMouldChangePlan) {
        if (UserConstants.NOT_UNIQUE.equals(iLhMouldChangePlanService.checkUnique(lhMouldChangePlan))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.lhMouldChangePlan.checkUnique"));
        }

        return iLhMouldChangePlanService.save(lhMouldChangePlan);
    }

    /**
     * 删除模具交替计划
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("lh:lhMouldChangePlan:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iLhMouldChangePlanService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验模具交替计划唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(LhMouldChangePlan lhMouldChangePlan) {
        return iLhMouldChangePlanService.checkUnique(lhMouldChangePlan);
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
        return I18nUtil.getMessage("ui.data.column.lhMouldChangePlan.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<LhMouldChangePlan> util = new ExcelUtil<>(LhMouldChangePlan.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, LhMouldChangePlan entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iLhMouldChangePlanService.exportData(entity,fileName);
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
        AjaxResult ajaxResult = iLhMouldChangePlanService.importData(context,false);
        return ajaxResult;
    }

    @ApiOperation("获取机台下拉列表 - 支持搜索筛选")
    @PostMapping("/getMachineList")
    @ResponseBody
    public AjaxResult getMachineList(LhMachineInfo query) {
        TableDataInfo tableDataInfo = iLhMachineInfoService.list(query);
        return AjaxResult.success(tableDataInfo.getRows());
    }

    @ApiOperation("获取物料下拉列表 - 支持搜索筛选")
    @PostMapping("/getMaterialList")
    @ResponseBody
    public AjaxResult getMaterialList(MdmMaterialInfo query) {
        TableDataInfo tableDataInfo = iMdmMaterialInfoService.list(query);
        return AjaxResult.success(tableDataInfo.getRows());
    }
}

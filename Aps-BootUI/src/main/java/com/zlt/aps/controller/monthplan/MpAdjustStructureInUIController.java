package com.zlt.aps.controller.monthplan;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureIn;
import com.zlt.aps.mp.api.service.IMpAdjustStructureInRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;

import lombok.extern.slf4j.Slf4j;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import org.apache.commons.io.IOUtils;

import java.util.Arrays;
import java.io.IOException;
import java.io.ByteArrayInputStream;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpAdjustStructureInUIController.java
 * 描    述：调整-结构内调整记录 UI控制层类：....
 *@author zlt
 *@date 2025-12-19
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Api(tags = "调整-结构内调整记录")
@Controller
@RequestMapping("/monthplan/mpAdjustStructureIn")
public class MpAdjustStructureInUIController extends BaseUIController<MpAdjustStructureIn> {

    @Autowired
    private IMpAdjustStructureInRemoteService iMpAdjustStructureInService;

    private final String prefix = "aps/monthplan/mpAdjustStructureIn";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("monthplan:mpAdjustStructureIn:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/mpAdjustStructureIn";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("mpAdjustStructureIn", new MpAdjustStructureIn());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("mpAdjustStructureIn", iMpAdjustStructureInService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:mpAdjustStructureIn:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MpAdjustStructureIn mpAdjustStructureIn) {
        return iMpAdjustStructureInService.list(mpAdjustStructureIn);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
//    @RequiresPermissions("monthplan:mpAdjustStructureIn:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(MpAdjustStructureIn mpAdjustStructureIn) {
        if (UserConstants.NOT_UNIQUE.equals(iMpAdjustStructureInService.checkUnique(mpAdjustStructureIn))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mpAdjustStructureIn.checkUnique"));
        }
        // 确认调整量
        Integer confirmAdjustQty = mpAdjustStructureIn.getConfirmAdjustQty();
        // 判断确认调整量是否为奇数，若是则返回错误提示
        if (confirmAdjustQty != null && !NumberUtil.isEven(confirmAdjustQty)) {
            String errorMsg = StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpAdjustStructure.isOddNumber"), mpAdjustStructureIn.getMaterialCode());
            return AjaxResult.error(errorMsg);
        }
        return iMpAdjustStructureInService.save(mpAdjustStructureIn);
    }

    /**
     * 删除调整-结构内调整记录
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("monthplan:mpAdjustStructureIn:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMpAdjustStructureInService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验调整-结构内调整记录唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(MpAdjustStructureIn mpAdjustStructureIn) {
        return iMpAdjustStructureInService.checkUnique(mpAdjustStructureIn);
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
        return I18nUtil.getMessage("ui.data.column.mpAdjustStructureIn.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<MpAdjustStructureIn> util = new ExcelUtil<>(MpAdjustStructureIn.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

//    @RequiresPermissions("monthplan:mpAdjustStructureIn:export")
    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, MpAdjustStructureIn entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iMpAdjustStructureInService.exportData(entity,fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

//    @RequiresPermissions("monthplan:mpAdjustStructureIn:import")
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
        AjaxResult ajaxResult = iMpAdjustStructureInService.importData(context,false);
        return ajaxResult;
    }

    /**
     * 查询版本列表
     */
    @ApiOperation("查询版本列表")
    @PostMapping("/getVersionList")
    @ResponseBody
    public TableDataInfo getVersionList(MpAdjustStructureIn queryVO) {
        return iMpAdjustStructureInService.getVersionList(queryVO);
    }


}

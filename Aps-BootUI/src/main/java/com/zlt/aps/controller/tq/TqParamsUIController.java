package com.zlt.aps.controller.tq;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.tq.api.domain.entity.TqParams;
import com.zlt.aps.tq.api.service.ITqParamsService;
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
 * 文件名称：TqParamsUIController.java
 * 描    述：胎圈参数设置 前端控制器（对齐胎面 TmParamsUIController）
 *
 * @author zlt
 * @version 1.0
 * @date 2025-12-12
 */
@Slf4j
@Api(tags = "胎圈参数设置")
@Controller
@RequestMapping("/tq/params")
public class TqParamsUIController extends BaseUIController<TqParams> {

    private final String prefix = "aps/tq/params";

    @Autowired
    private ITqParamsService iTqParamsService;

    @RequiresPermissions("tq:params:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/params";
    }

    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("tqParams", new TqParams());
        return prefix + "/add";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("tqParams", iTqParamsService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TqParams query) {
        return iTqParamsService.list(query);
    }

    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @ResponseBody
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(iTqParamsService.getInfo(id));
    }

    @ApiOperation("保存")
    @PostMapping("/save")
    @RequiresPermissions("tq:params:edit")
    @ResponseBody
    public AjaxResult save(TqParams tqParams) {
        return iTqParamsService.save(tqParams);
    }

    @ApiOperation("删除")
    @PostMapping("/remove")
    @RequiresPermissions("tq:params:remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTqParamsService.removeByIds(Arrays.asList(arr));
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(@RequestBody TqParams query) {
        return iTqParamsService.checkUnique(query);
    }

    @ApiOperation("导出数据")
    @GetMapping("/export")
    @RequiresPermissions("tq:params:export")
    public void export(HttpServletResponse response, TqParams entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.tq.params.modelName");
        byte[] excelBytes = iTqParamsService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(@RequestParam("file") MultipartFile file, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportContext context = new ImportContext();
        context.setFunctionName(I18nUtil.getMessage("ui.data.column.tq.params.modelName"));
        context.setProcedureCode(I18nUtil.getMessage("ui.data.column.tq.params.modelName"));
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iTqParamsService.importData(context, updateSupport);
        return ajaxResult;
    }

    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.tq.params.modelName");
        ExcelUtil<TqParams> util = new ExcelUtil<>(TqParams.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }
}
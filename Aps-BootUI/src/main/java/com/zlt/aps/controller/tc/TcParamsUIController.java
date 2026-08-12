package com.zlt.aps.controller.tc;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.common.utils.ExportSortParamUtil;
import com.zlt.aps.tc.api.domain.entity.TcParams;
import com.zlt.aps.tc.api.service.ITcParamsRemoteService;
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
 * 文件名称：TcParamsUIController.java
 * 描    述：胎侧参数设置 页面控制层
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2026-07-07
 */
@Slf4j
@Api(tags = "胎侧参数设置")
@Controller
@RequestMapping("/tc/tcParams")
public class TcParamsUIController extends BaseUIController<TcParams> {

    private final String prefix = "aps/tc/tcParams";

    @Autowired
    private ITcParamsRemoteService iTcParamsService;

    @RequiresPermissions("tc:tcParams:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/tcParams";
    }

    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("tcParams", new TcParams());
        return prefix + "/add";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("tcParams", iTcParamsService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TcParams query) {
        ExportSortParamUtil.applySortParams(query, this.getRequest());
        return iTcParamsService.list(query);
    }

    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @ResponseBody
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(iTcParamsService.getInfo(id));
    }

    @ApiOperation("保存")
    @PostMapping("/save")
    @RequiresPermissions("tc:tcParams:edit")
    @ResponseBody
    public AjaxResult save(TcParams tcParams) {
        return iTcParamsService.save(tcParams);
    }

    @ApiOperation("删除")
    @PostMapping("/remove")
    @RequiresPermissions("tc:tcParams:remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTcParamsService.removeByIds(Arrays.asList(arr));
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(@RequestBody TcParams query) {
        return iTcParamsService.checkUnique(query);
    }

    @ApiOperation("导出数据")
    @GetMapping("/export")
    @RequiresPermissions("tc:tcParams:export")
    public void export(HttpServletResponse response, TcParams entity) throws IOException {
        ExportSortParamUtil.applySortParams(entity, this.getRequest());
        String fileName = I18nUtil.getMessage("ui.data.column.tc.Params.modelName");
        byte[] excelBytes = iTcParamsService.exportData(entity, fileName);
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
        context.setFunctionName(I18nUtil.getMessage("ui.data.column.tc.Params.modelName"));
        context.setProcedureCode(I18nUtil.getMessage("ui.data.column.tc.Params.modelName"));
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iTcParamsService.importData(context, updateSupport);
        return ajaxResult;
    }

    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.tc.Params.modelName");
        ExcelUtil<TcParams> util = new ExcelUtil<>(TcParams.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }
}

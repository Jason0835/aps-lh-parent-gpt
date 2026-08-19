package com.zlt.aps.controller.gsq;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.gsq.api.domain.entity.GsqParams;
import com.zlt.aps.gsq.api.service.IGsqParamsService;
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
 * 钢丝圈参数设置 前端控制器（对齐胎圈 TqParamsUIController）
 *
 * @author zlt
 * @version 1.0
 * @date 2025-12-12
 */
@Slf4j
@Api(tags = "钢丝圈参数设置")
@Controller
@RequestMapping("/gsq/params")
public class GsqParamsUIController extends BaseUIController<GsqParams> {

    private final String prefix = "aps/gsq/params";

    @Autowired
    private IGsqParamsService iGsqParamsService;

    @RequiresPermissions("gsq:params:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/params";
    }

    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("gsqParams", new GsqParams());
        return prefix + "/add";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("gsqParams", iGsqParamsService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(GsqParams query) {
        return iGsqParamsService.list(query);
    }

    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @ResponseBody
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(iGsqParamsService.getInfo(id));
    }

    @ApiOperation("保存")
    @PostMapping("/save")
    @RequiresPermissions("gsq:params:edit")
    @ResponseBody
    public AjaxResult save(GsqParams gsqParams) {
        return iGsqParamsService.save(gsqParams);
    }

    @ApiOperation("删除")
    @PostMapping("/remove")
    @RequiresPermissions("gsq:params:remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iGsqParamsService.removeByIds(Arrays.asList(arr));
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(GsqParams query) {
        return iGsqParamsService.checkUnique(query);
    }

    @ApiOperation("导出数据")
    @GetMapping("/export")
    @RequiresPermissions("gsq:params:export")
    public void export(HttpServletResponse response, GsqParams entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.gsq.params.modelName");
        byte[] excelBytes = iGsqParamsService.exportData(entity, fileName);
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
        context.setFunctionName(I18nUtil.getMessage("ui.data.column.gsq.params.modelName"));
        context.setProcedureCode(I18nUtil.getMessage("ui.data.column.gsq.params.modelName"));
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iGsqParamsService.importData(context, updateSupport);
        return ajaxResult;
    }

    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.gsq.params.modelName");
        ExcelUtil<GsqParams> util = new ExcelUtil<>(GsqParams.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }
}
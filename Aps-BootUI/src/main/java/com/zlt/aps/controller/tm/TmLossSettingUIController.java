package com.zlt.aps.controller.tm;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.common.utils.ExportSortParamUtil;
import com.zlt.aps.tm.api.domain.entity.TmLossSetting;
import com.zlt.aps.tm.api.service.ITmLossSettingRemoteService;
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

@Slf4j
@Api(tags = "胎面损耗率设定")
@Controller
@RequestMapping("/tm/tmLossSetting")
public class TmLossSettingUIController extends BaseUIController<TmLossSetting> {

    private final String prefix = "aps/tm/lossSetting";

    @Autowired
    private ITmLossSettingRemoteService iTmLossSettingService;

    @RequiresPermissions("tm:tmLossSetting:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/lossSetting";
    }

    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("tmLossSetting", new TmLossSetting());
        return prefix + "/add";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("tmLossSetting", iTmLossSettingService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TmLossSetting query) {
        ExportSortParamUtil.applySortParams(query, this.getRequest());
        return iTmLossSettingService.list(query);
    }

    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @ResponseBody
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(iTmLossSettingService.getInfo(id));
    }

    @ApiOperation("保存")
    @PostMapping("/save")
    @RequiresPermissions("tm:tmLossSetting:edit")
    @ResponseBody
    public AjaxResult save(TmLossSetting tmLossSetting) {
        return iTmLossSettingService.save(tmLossSetting);
    }

    @ApiOperation("删除")
    @PostMapping("/remove")
    @RequiresPermissions("tm:tmLossSetting:remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTmLossSettingService.removeByIds(Arrays.asList(arr));
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(@RequestBody TmLossSetting query) {
        return iTmLossSettingService.checkUnique(query);
    }

    @ApiOperation("导出数据")
    @GetMapping("/export")
    @RequiresPermissions("tm:tmLossSetting:export")
    public void export(HttpServletResponse response, TmLossSetting entity) throws IOException {
        ExportSortParamUtil.applySortParams(entity, this.getRequest());
        String fileName = I18nUtil.getMessage("ui.data.column.tm.lossSetting.modelName");
        byte[] excelBytes = iTmLossSettingService.exportData(entity, fileName);
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
        context.setFunctionName(I18nUtil.getMessage("ui.data.column.tm.lossSetting.modelName"));
        context.setProcedureCode(I18nUtil.getMessage("ui.data.column.tm.lossSetting.modelName"));
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iTmLossSettingService.importData(context, updateSupport);
        return ajaxResult;
    }

    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.tm.lossSetting.modelName");
        ExcelUtil<TmLossSetting> util = new ExcelUtil<>(TmLossSetting.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }
}

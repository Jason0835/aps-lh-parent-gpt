package com.zlt.aps.controller.tc;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.common.utils.ExportSortParamUtil;
import com.zlt.aps.tc.api.domain.entity.TcLossSetting;
import com.zlt.aps.tc.api.service.ITcLossSettingRemoteService;
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
@Api(tags = "胎侧损耗率设定")
@Controller
@RequestMapping("/tc/tcLossSetting")
public class TcLossSettingUIController extends BaseUIController<TcLossSetting> {

    private final String prefix = "aps/tc/lossSetting";

    @Autowired
    private ITcLossSettingRemoteService iTcLossSettingService;

    @RequiresPermissions("tc:tcLossSetting:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/lossSetting";
    }

    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("tcLossSetting", new TcLossSetting());
        return prefix + "/add";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("tcLossSetting", iTcLossSettingService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TcLossSetting query) {
        ExportSortParamUtil.applySortParams(query, this.getRequest());
        return iTcLossSettingService.list(query);
    }

    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @ResponseBody
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(iTcLossSettingService.getInfo(id));
    }

    @ApiOperation("保存")
    @PostMapping("/save")
    @RequiresPermissions("tc:tcLossSetting:edit")
    @ResponseBody
    public AjaxResult save(TcLossSetting tcLossSetting) {
        return iTcLossSettingService.save(tcLossSetting);
    }

    @ApiOperation("删除")
    @PostMapping("/remove")
    @RequiresPermissions("tc:tcLossSetting:remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTcLossSettingService.removeByIds(Arrays.asList(arr));
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(@RequestBody TcLossSetting query) {
        return iTcLossSettingService.checkUnique(query);
    }

    @ApiOperation("导出数据")
    @GetMapping("/export")
    @RequiresPermissions("tc:tcLossSetting:export")
    public void export(HttpServletResponse response, TcLossSetting entity) throws IOException {
        ExportSortParamUtil.applySortParams(entity, this.getRequest());
        String fileName = I18nUtil.getMessage("ui.data.column.tc.lossSetting.modelName");
        byte[] excelBytes = iTcLossSettingService.exportData(entity, fileName);
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
        context.setFunctionName(I18nUtil.getMessage("ui.data.column.tc.lossSetting.modelName"));
        context.setProcedureCode(I18nUtil.getMessage("ui.data.column.tc.lossSetting.modelName"));
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iTcLossSettingService.importData(context, updateSupport);
        return ajaxResult;
    }

    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.tc.lossSetting.modelName");
        ExcelUtil<TcLossSetting> util = new ExcelUtil<>(TcLossSetting.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }
}

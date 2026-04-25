package com.zlt.aps.controller.tq;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.tq.api.domain.entity.TqLossSetting;
import com.zlt.aps.tq.api.domain.vo.TqLossSettingExportVO;
import com.zlt.aps.tq.api.service.ITqLossSettingService;
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

@Slf4j
@Controller
@RequestMapping("/tq/loss")
@Api(tags = {"胎圈损耗率设定接口"})
public class TqLossSettingUIController extends BaseUIController<TqLossSetting> {

    private final String prefix = "tq/loss";

    @Autowired
    private ITqLossSettingService iTqLossSettingService;

    @RequiresPermissions("tq:loss:view")
    @GetMapping()
    @ApiOperation("跳转到胎圈损耗率设定首页")
    public String toIndex() {
        return prefix + "/loss";
    }

    @RequiresPermissions("tq:loss:list")
    @PostMapping("/list")
    @ResponseBody
    @ApiOperation("查询胎圈损耗率设定列表")
    public TableDataInfo list(TqLossSetting entity) {
        return iTqLossSettingService.list(entity);
    }

    @GetMapping(value = "/edit/{id}")
    @ApiOperation("获取胎圈损耗率设定详细信息,跳转到编辑页面")
    public String getInfo(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("LossSetting", iTqLossSettingService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("跳转到胎圈损耗率设定新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("LossSetting", new TqLossSetting());
        return prefix + "/edit";
    }

    @RequiresPermissions("tq:loss:edit")
    @PostMapping("/save")
    @ResponseBody
    @ApiOperation("保存胎圈损耗率设定（id为空则新增，id不为空则修改）")
    public AjaxResult save(TqLossSetting entity) {
        return iTqLossSettingService.save(entity);
    }

    @RequiresPermissions("tq:loss:remove")
    @PostMapping("/remove")
    @ResponseBody
    @ApiOperation("删除胎圈损耗率设定")
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTqLossSettingService.removeByIds(Arrays.asList(arr));
    }

    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "TQ";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.tq.loss.modelName");
    }

    @RequiresPermissions("tq:loss:export")
    @ApiOperation("导出胎圈损耗率设定")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, TqLossSetting entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iTqLossSettingService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @ApiOperation("下载模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<TqLossSettingExportVO> util = new ExcelUtil<>(TqLossSettingExportVO.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @RequiresPermissions("tq:loss:import")
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @ResponseBody
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        return iTqLossSettingService.importData(context, updateSupport);
    }
}

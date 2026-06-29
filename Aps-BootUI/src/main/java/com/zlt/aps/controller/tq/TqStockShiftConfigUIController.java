package com.zlt.aps.controller.tq;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.tq.api.domain.entity.TqStockShiftConfig;
import com.zlt.aps.tq.api.service.ITqStockShiftConfigService;
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
 * 胎圈备库班数配置 UIController
 *
 * @author zlt
 * @date 2026-06-25
 */
@Slf4j
@Controller
@RequestMapping("/tq/stockShiftConfig")
@Api(tags = {"胎圈备库班数配置接口"})
public class TqStockShiftConfigUIController extends BaseUIController<TqStockShiftConfig> {

    private final String prefix = "tq/stockShiftConfig";

    @Autowired
    private ITqStockShiftConfigService iTqStockShiftConfigService;

    @RequiresPermissions("tq:stockShiftConfig:view")
    @GetMapping()
    @ApiOperation("跳转到胎圈备库班数配置首页")
    public String toIndex() {
        return prefix + "/index";
    }

    @RequiresPermissions("tq:stockShiftConfig:list")
    @PostMapping("/list")
    @ResponseBody
    @ApiOperation("查询胎圈备库班数配置列表")
    public TableDataInfo list(TqStockShiftConfig entity) {
        return iTqStockShiftConfigService.list(entity);
    }

    @GetMapping(value = "/edit/{id}")
    @ApiOperation("获取胎圈备库班数配置详细信息,跳转到编辑页面")
    public String getInfo(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("StockShiftConfig", iTqStockShiftConfigService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("跳转到胎圈备库班数配置新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("StockShiftConfig", new TqStockShiftConfig());
        return prefix + "/edit";
    }

    @RequiresPermissions("tq:stockShiftConfig:edit")
    @PostMapping("/save")
    @ResponseBody
    @ApiOperation("保存胎圈备库班数配置（id为空则新增，id不为空则修改）")
    public AjaxResult save(TqStockShiftConfig entity) {
        // 校验业务唯一约束（同一工厂下同机台范围同机台数只能有一条）
        if (UserConstants.NOT_UNIQUE.equals(iTqStockShiftConfigService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.error.message.stockShiftConfig.unique"));
        }
        // 校验范围交叉（新增/修改的规则不能与现有规则有交集）
        if (UserConstants.NOT_UNIQUE.equals(iTqStockShiftConfigService.checkRangeCross(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.tq.stockShiftConfig.rangeCross"));
        }
        return iTqStockShiftConfigService.save(entity);
    }

    @RequiresPermissions("tq:stockShiftConfig:remove")
    @PostMapping("/remove")
    @ResponseBody
    @ApiOperation("删除胎圈备库班数配置")
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTqStockShiftConfigService.removeByIds(Arrays.asList(arr));
    }

    @RequiresPermissions("tq:stockShiftConfig:checkUnique")
    @PostMapping("/checkUnique")
    @ResponseBody
    @ApiOperation("校验胎圈备库班数配置唯一性")
    public String checkUnique(TqStockShiftConfig entity) {
        return iTqStockShiftConfigService.checkUnique(entity);
    }

    @PostMapping("/checkRangeCross")
    @ResponseBody
    @ApiOperation("校验配置规则交叉（确保新增/修改的规则不与现有规则有范围交叉）")
    public String checkRangeCross(TqStockShiftConfig entity) {
        return iTqStockShiftConfigService.checkRangeCross(entity);
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
        return I18nUtil.getMessage("ui.data.column.tq.stockShiftConfig.modelName");
    }

    @RequiresPermissions("tq:stockShiftConfig:export")
    @ApiOperation("导出胎圈备库班数配置")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, TqStockShiftConfig entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iTqStockShiftConfigService.exportData(entity, fileName);
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
        ExcelUtil<TqStockShiftConfig> util = new ExcelUtil<>(TqStockShiftConfig.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @RequiresPermissions("tq:stockShiftConfig:import")
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
        return iTqStockShiftConfigService.importData(context, updateSupport);
    }
}

package com.zlt.aps.controller.tm;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.common.utils.ExportSortParamUtil;
import com.zlt.aps.tm.api.domain.entity.TmDepthConfig;
import com.zlt.aps.tm.api.service.ITmDepthConfigRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
 * 备库班数配置Controller
 *
 * @author zlt
 */
@Api(tags = "备库班数配置")
@Controller
@RequestMapping("/tm/depthConfig")
public class TmDepthConfigUIController extends BaseUIController<TmDepthConfig> {

    @Autowired
    private ITmDepthConfigRemoteService iTmDepthConfigService;

    private final String prefix = "aps/tm/depthConfig";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("tm:depthConfig:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/tmDepthConfig";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("tmDepthConfig", new TmDepthConfig());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("tmDepthConfig", iTmDepthConfigService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("tm:depthConfig:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TmDepthConfig tmDepthConfig) {
        ExportSortParamUtil.applySortParams(tmDepthConfig, this.getRequest());
        return iTmDepthConfigService.list(tmDepthConfig);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("tm:depthConfig:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(TmDepthConfig tmDepthConfig) {
        // 统一校验区间字段、连续性和完整性
        if (UserConstants.NOT_UNIQUE.equals(iTmDepthConfigService.checkRangeCross(tmDepthConfig))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.tm.depthConfig.rangeCross"));
        }
        return iTmDepthConfigService.save(tmDepthConfig);
    }

    /**
     * 删除
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("tm:depthConfig:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTmDepthConfigService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 兼容旧调用路径，执行连续区间校验。
     */
    @ApiOperation("兼容旧调用路径校验连续区间")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(TmDepthConfig tmDepthConfig) {
        return iTmDepthConfigService.checkRangeCross(tmDepthConfig);
    }

    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "TM0816";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.tm.depthConfig.column.modalName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<TmDepthConfig> util = new ExcelUtil<>(TmDepthConfig.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({ "/export" })
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, TmDepthConfig entity) throws IOException {
        ExportSortParamUtil.applySortParams(entity, this.getRequest());
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iTmDepthConfigService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @PostMapping({ "/importData" })
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
        AjaxResult ajaxResult = iTmDepthConfigService.importData(context, updateSupport);
        return ajaxResult;
    }
}

package com.zlt.aps.controller.dj;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.dj.api.domain.entity.DjDepthConfig;
import com.zlt.aps.dj.api.service.IDjDepthConfigRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 垫胶备库班数与供成型机数配置Controller
 *
 * @author zlt
 */
@Api(tags = "垫胶备库班数与供成型机数配置")
@Controller
@RequestMapping("/dj/depthConfig")
public class DjDepthConfigUIController extends BaseUIController<DjDepthConfig> {

    @Autowired
    private IDjDepthConfigRemoteService iDjDepthConfigService;

    private final String prefix = "aps/dj/depthConfig";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("dj:depthConfig:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/djDepthConfig";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("djDepthConfig", new DjDepthConfig());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("djDepthConfig", iDjDepthConfigService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("dj:depthConfig:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(DjDepthConfig djDepthConfig) {
        return iDjDepthConfigService.list(djDepthConfig);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("dj:depthConfig:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(DjDepthConfig djDepthConfig) {
        if (UserConstants.NOT_UNIQUE.equals(iDjDepthConfigService.checkUnique(djDepthConfig))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.djMachine.embryoCodeNotUnique"));
        }
        // 校验范围交叉
        if (UserConstants.NOT_UNIQUE.equals(iDjDepthConfigService.checkRangeCross(djDepthConfig))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.dj.depthConfig.rangeCross"));
        }
        return iDjDepthConfigService.save(djDepthConfig);
    }

    /**
     * 删除
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("dj:depthConfig:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iDjDepthConfigService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(DjDepthConfig djDepthConfig) {
        return iDjDepthConfigService.checkUnique(djDepthConfig);
    }

    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "0";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.dj.depthConfig.column.modalName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<DjDepthConfig> util = new ExcelUtil<>(DjDepthConfig.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({ "/export" })
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, DjDepthConfig entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iDjDepthConfigService.exportData(entity, fileName);
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
        AjaxResult ajaxResult = iDjDepthConfigService.importData(context, updateSupport);
        return ajaxResult;
    }
}

package com.zlt.aps.controller.tc;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.tc.api.domain.entity.TcDepthConfig;
import com.zlt.aps.tc.api.service.ITcDepthConfigRemoteService;
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
 * 胎侧备库班数配置Controller
 *
 * @author zlt
 */
@Api(tags = "胎侧备库班数配置")
@Controller
@RequestMapping("/tc/depthConfig")
public class TcDepthConfigUIController extends BaseUIController<TcDepthConfig> {

    private final String prefix = "aps/tc/depthConfig";
    @Autowired
    private ITcDepthConfigRemoteService iTcDepthConfigService;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("tc:depthConfig:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/tcDepthConfig";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("tcDepthConfig", new TcDepthConfig());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("tcDepthConfig", iTcDepthConfigService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("tc:depthConfig:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TcDepthConfig tcDepthConfig) {
        return iTcDepthConfigService.list(tcDepthConfig);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("tc:depthConfig:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(TcDepthConfig tcDepthConfig) {
        if (UserConstants.NOT_UNIQUE.equals(iTcDepthConfigService.checkUnique(tcDepthConfig))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.tc.depthConfig.notUnique"));
        }
        // 校验范围交叉
        if (UserConstants.NOT_UNIQUE.equals(iTcDepthConfigService.checkRangeCross(tcDepthConfig))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.tc.depthConfig.rangeCross"));
        }
        return iTcDepthConfigService.save(tcDepthConfig);
    }

    /**
     * 删除
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("tc:depthConfig:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTcDepthConfigService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(TcDepthConfig tcDepthConfig) {
        return iTcDepthConfigService.checkUnique(tcDepthConfig);
    }

    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "TC0916";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.tc.depthConfig.column.modalName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<TcDepthConfig> util = new ExcelUtil<>(TcDepthConfig.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({ "/export" })
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, TcDepthConfig entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iTcDepthConfigService.exportData(entity, fileName);
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
        AjaxResult ajaxResult = iTcDepthConfigService.importData(context, updateSupport);
        return ajaxResult;
    }
}
package com.zlt.aps.controller.nc;

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
import com.zlt.aps.nc.api.domain.entity.NcLossSetting;
import com.zlt.aps.nc.api.service.INcLossSettingRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 内衬损耗率设定Controller
 *
 * @author zlt
 * @date 2026-06-10
 */
@Api(tags = "内衬损耗率设定")
@Controller
@RequestMapping("/nc/lossSetting")
public class NcLossSettingUIController extends BaseUIController<NcLossSetting> {

    @Autowired
    private INcLossSettingRemoteService iNcLossSettingService;

    private final String prefix = "aps/nc/lossSetting";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("nc:lossSetting:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/ncMachine";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("ncMachine", new NcLossSetting());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("ncMachine", iNcLossSettingService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("nc:lossSetting:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(NcLossSetting ncMachine) {
        return iNcLossSettingService.list(ncMachine);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("nc:lossSetting:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(NcLossSetting ncMachine) {
        if (UserConstants.NOT_UNIQUE.equals(iNcLossSettingService.checkUnique(ncMachine))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.ncMachine.embryoCodeNotUnique"));
        }

        return iNcLossSettingService.save(ncMachine);
    }

    /**
     * 删除
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("nc:lossSetting:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iNcLossSettingService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(NcLossSetting ncMachine) {
        return iNcLossSettingService.checkUnique(ncMachine);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 
     * @return
     */
    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }

    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getProcedureCode() {
        return "0";
    }

    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.nc.lossSetting.column.modalName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<NcLossSetting> util = new ExcelUtil<>(NcLossSetting.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({ "/export" })
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, NcLossSetting entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iNcLossSettingService.exportData(entity, fileName);
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
        AjaxResult ajaxResult = iNcLossSettingService.importData(context, updateSupport);
        return ajaxResult;
    }
}

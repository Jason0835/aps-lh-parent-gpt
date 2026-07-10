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
import com.zlt.aps.nc.api.domain.entity.NcSpecifyMachine;
import com.zlt.aps.nc.api.service.INcSpecifyMachineRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 内衬机台信息Controller
 *
 * @author zlt
 */
@Controller
@RequestMapping("/nc/specifyMachine")
@Api(tags = { "内衬机台信息维护接口" })
public class NcSpecifyMachineUIController extends BaseUIController<NcSpecifyMachine> {

    @Autowired
    private INcSpecifyMachineRemoteService iNcSpecifyMachineService;

    private final String prefix = "aps/nc/specifyMachine";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("nc:machine:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/ncSpecifyMachine";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("ncSpecifyMachine", new NcSpecifyMachine());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("ncSpecifyMachine", iNcSpecifyMachineService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("nc:ncSpecifyMachine:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(NcSpecifyMachine ncMachine) {
        return iNcSpecifyMachineService.list(ncMachine);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("nc:ncSpecifyMachine:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(NcSpecifyMachine ncMachine) {
        if (UserConstants.NOT_UNIQUE.equals(iNcSpecifyMachineService.checkUnique(ncMachine))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.ncMachine.embryoCodeNotUnique"));
        }

        return iNcSpecifyMachineService.save(ncMachine);
    }

    /**
     * 删除
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("nc:ncSpecifyMachine:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iNcSpecifyMachineService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(NcSpecifyMachine ncMachine) {
        return iNcSpecifyMachineService.checkUnique(ncMachine);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * @return
     */
    @Override
    public String getExportTemplateFileName(){
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
        return I18nUtil.getMessage("ui.data.column.ncSpecifyMachine.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<NcSpecifyMachine> util = new ExcelUtil<>(NcSpecifyMachine.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, NcSpecifyMachine entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iNcSpecifyMachineService.exportData(entity,fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @PostMapping({"/importData"})
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
        AjaxResult ajaxResult = iNcSpecifyMachineService.importData(context, updateSupport);
        return ajaxResult;
    }
}

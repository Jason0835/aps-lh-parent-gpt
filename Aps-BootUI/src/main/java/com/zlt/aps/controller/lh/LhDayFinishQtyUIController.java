package com.zlt.aps.controller.lh;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import com.zlt.aps.lh.api.service.ILhDayFinishQtyRemoteService;
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
 * 硫化排程日完成量 UI控制层类
 *
 * @author APS Team
 * @date 2026-04-10
 */
@Slf4j
@Api(tags = "硫化排程日完成量")
@Controller
@RequestMapping("/lh/lhDayFinishQty")
public class LhDayFinishQtyUIController extends BaseUIController<LhDayFinishQty> {

    @Autowired
    private ILhDayFinishQtyRemoteService iLhDayFinishQtyRemoteService;

    private final String prefix = "aps/lh/lhDayFinishQty";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("lh:lhDayFinishQty:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/lhDayFinishQty";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("lhDayFinishQty", new LhDayFinishQty());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("lhDayFinishQty", iLhDayFinishQtyRemoteService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("lh:lhDayFinishQty:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(LhDayFinishQty lhDayFinishQty) {
        return iLhDayFinishQtyRemoteService.list(lhDayFinishQty);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("lh:lhDayFinishQty:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(LhDayFinishQty lhDayFinishQty) {
        if (UserConstants.NOT_UNIQUE.equals(iLhDayFinishQtyRemoteService.checkUnique(lhDayFinishQty))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhDayFinishQty.notUnique"));
        }
        return iLhDayFinishQtyRemoteService.save(lhDayFinishQty);
    }

    /**
     * 删除硫化排程日完成量
     */
    @ApiOperation("删除")
    @RequiresPermissions("lh:lhDayFinishQty:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iLhDayFinishQtyRemoteService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验硫化排程日完成量唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(LhDayFinishQty lhDayFinishQty) {
        return iLhDayFinishQtyRemoteService.checkUnique(lhDayFinishQty);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
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
        return I18nUtil.getMessage("ui.data.column.lhDayFinishQty.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<LhDayFinishQty> util = new ExcelUtil<>(LhDayFinishQty.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, LhDayFinishQty entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iLhDayFinishQtyRemoteService.exportData(entity,fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @PostMapping({"/importData"})
    @ResponseBody
    @ApiOperation("数据导入")
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(null);
        context.setFunctionName(I18nUtil.getMessage("ui.data.column.lhDayFinishQty.modelName"));
        context.setProcedureCode(I18nUtil.getMessage("ui.data.column.lhDayFinishQty.modelName"));
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iLhDayFinishQtyRemoteService.importData(context,updateSupport);
        return ajaxResult;
    }
}

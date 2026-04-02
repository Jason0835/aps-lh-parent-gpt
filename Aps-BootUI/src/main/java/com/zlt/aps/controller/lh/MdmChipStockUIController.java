package com.zlt.aps.controller.lh;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.lh.api.service.ILhMachineInfoRemoteService;
import com.zlt.aps.mdm.api.domain.entity.MdmChipStock;
import com.zlt.aps.mdm.api.service.IMdmChipStockRemoteService;
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
 * 芯片库存 UI控制层类
 *
 * @author APS Team
 * @date 2026-04-02
 */
@Slf4j
@Api(tags = "芯片库存")
@Controller
@RequestMapping("/lh/mdmChipStock")
public class MdmChipStockUIController extends BaseUIController<MdmChipStock> {

    @Autowired
    private IMdmChipStockRemoteService iMdmChipStockRemoteService;

    @Autowired
    private ILhMachineInfoRemoteService iLhMachineInfoService;

    private final String prefix = "aps/lh/mdmChipStock";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("lh:mdmChipStock:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/mdmChipStock";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("mdmChipStock", new MdmChipStock());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("mdmChipStock", iMdmChipStockRemoteService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("lh:mdmChipStock:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MdmChipStock mdmChipStock) {
        return iMdmChipStockRemoteService.list(mdmChipStock);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("lh:mdmChipStock:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(MdmChipStock mdmChipStock) {
        if (UserConstants.NOT_UNIQUE.equals(iMdmChipStockRemoteService.checkUnique(mdmChipStock))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.mdmChipStock.notUnique"));
        }

        return iMdmChipStockRemoteService.save(mdmChipStock);
    }

    /**
     * 删除芯片库存
     */
    @ApiOperation("删除")
    @RequiresPermissions("lh:mdmChipStock:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMdmChipStockRemoteService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验芯片库存唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(MdmChipStock mdmChipStock) {
        return iMdmChipStockRemoteService.checkUnique(mdmChipStock);
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
        return I18nUtil.getMessage("ui.data.column.mdmChipStock.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<MdmChipStock> util = new ExcelUtil<>(MdmChipStock.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, MdmChipStock entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iMdmChipStockRemoteService.exportData(entity,fileName);
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
        context.setFunctionName(I18nUtil.getMessage("ui.data.column.mdmChipStock.modelName"));
        context.setProcedureCode(I18nUtil.getMessage("ui.data.column.mdmChipStock.modelName"));
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iMdmChipStockRemoteService.importData(context,updateSupport);
        return ajaxResult;
    }

    @ApiOperation("获取机台下拉列表 - 支持搜索筛选")
    @PostMapping("/getMachineList")
    @ResponseBody
    public AjaxResult getMachineList(com.zlt.aps.lh.api.domain.entity.LhMachineInfo query) {
        TableDataInfo tableDataInfo = iLhMachineInfoService.list(query);
        return AjaxResult.success(tableDataInfo.getRows());
    }

    /**
     * 合并保存 - 新增时检测到重复，将库存量和完成量累加到已有数据上
     */
    @ApiOperation("合并保存")
    @RequiresPermissions("lh:mdmChipStock:edit")
    @PostMapping("/mergeSave")
    @ResponseBody
    public AjaxResult mergeSave(MdmChipStock mdmChipStock) {
        return iMdmChipStockRemoteService.mergeSave(mdmChipStock);
    }
}

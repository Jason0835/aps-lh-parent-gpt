package com.zlt.aps.controller.tc;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.common.utils.ExportSortParamUtil;
import com.zlt.aps.tc.api.domain.entity.TcSpecifyMachine;
import com.zlt.aps.tc.api.service.ITcSpecifyMachineRemoteService;
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
@Api(tags = "胎侧定点与禁排机台规则")
@Controller
@RequestMapping("/tc/tcSpecifyMachine")
public class TcSpecifyMachineUIController extends BaseUIController<TcSpecifyMachine> {

    private final String prefix = "aps/tc/tcSpecifyMachine";

    @Autowired
    private ITcSpecifyMachineRemoteService iTcSpecifyMachineService;

    @RequiresPermissions("tc:tcSpecifyMachine:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/tcSpecifyMachine";
    }

    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("tcSpecifyMachine", new TcSpecifyMachine());
        return prefix + "/add";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("tcSpecifyMachine", iTcSpecifyMachineService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TcSpecifyMachine query) {
        ExportSortParamUtil.applySortParams(query, this.getRequest());
        return iTcSpecifyMachineService.list(query);
    }

    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @ResponseBody
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(iTcSpecifyMachineService.getInfo(id));
    }

    @ApiOperation("保存")
    @PostMapping("/save")
    @RequiresPermissions("tc:tcSpecifyMachine:edit")
    @ResponseBody
    public AjaxResult save(TcSpecifyMachine tcSpecifyMachine) {
        return iTcSpecifyMachineService.save(tcSpecifyMachine);
    }

    @ApiOperation("删除")
    @PostMapping("/remove")
    @RequiresPermissions("tc:tcSpecifyMachine:remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTcSpecifyMachineService.removeByIds(Arrays.asList(arr));
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(@RequestBody TcSpecifyMachine query) {
        return iTcSpecifyMachineService.checkUnique(query);
    }

    @ApiOperation("导出数据")
    @GetMapping("/export")
    @RequiresPermissions("tc:tcSpecifyMachine:export")
    public void export(HttpServletResponse response, TcSpecifyMachine entity) throws IOException {
        ExportSortParamUtil.applySortParams(entity, this.getRequest());
        String fileName = I18nUtil.getMessage("ui.data.column.tc.specifyMachine.modelName");
        byte[] excelBytes = iTcSpecifyMachineService.exportData(entity, fileName);
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
        context.setFunctionName(I18nUtil.getMessage("ui.data.column.tc.specifyMachine.modelName"));
        context.setProcedureCode(I18nUtil.getMessage("ui.data.column.tc.specifyMachine.modelName"));
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iTcSpecifyMachineService.importData(context, updateSupport);
        return ajaxResult;
    }

    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.tc.specifyMachine.modelName");
        ExcelUtil<TcSpecifyMachine> util = new ExcelUtil<>(TcSpecifyMachine.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }
}

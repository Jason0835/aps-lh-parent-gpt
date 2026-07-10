package com.zlt.aps.controller.tc;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.tc.api.domain.entity.TcMouthPlate;
import com.zlt.aps.tc.api.service.ITcMouthPlateRemoteService;
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
@Api(tags = "胎侧口型板信息")
@Controller
@RequestMapping("/tc/tcMouthPlate")
public class TcMouthPlateUIController extends BaseUIController<TcMouthPlate> {

    private final String prefix = "aps/tc/tcMouthPlate";

    @Autowired
    private ITcMouthPlateRemoteService iTcMouthPlateService;

    @RequiresPermissions("tc:tcMouthPlate:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/tcMouthPlate";
    }

    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("tcMouthPlate", new TcMouthPlate());
        return prefix + "/add";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("tcMouthPlate", iTcMouthPlateService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TcMouthPlate query) {
        return iTcMouthPlateService.list(query);
    }

    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @ResponseBody
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(iTcMouthPlateService.getInfo(id));
    }

    @ApiOperation("保存")
    @PostMapping("/save")
    @RequiresPermissions("tc:tcMouthPlate:edit")
    @ResponseBody
    public AjaxResult save(TcMouthPlate tcMouthPlate) {
        return iTcMouthPlateService.save(tcMouthPlate);
    }

    @ApiOperation("删除")
    @PostMapping("/remove")
    @RequiresPermissions("tc:tcMouthPlate:remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTcMouthPlateService.removeByIds(Arrays.asList(arr));
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(@RequestBody TcMouthPlate query) {
        return iTcMouthPlateService.checkUnique(query);
    }

    @ApiOperation("导出数据")
    @GetMapping("/export")
    @RequiresPermissions("tc:tcMouthPlate:export")
    public void export(HttpServletResponse response, TcMouthPlate entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.tc.mouthPlate.modelName");
        byte[] excelBytes = iTcMouthPlateService.exportData(entity, fileName);
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
        context.setFunctionName(I18nUtil.getMessage("ui.data.column.tc.mouthPlate.modelName"));
        context.setProcedureCode(I18nUtil.getMessage("ui.data.column.tc.mouthPlate.modelName"));
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iTcMouthPlateService.importData(context, updateSupport);
        return ajaxResult;
    }

    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.tc.mouthPlate.modelName");
        ExcelUtil<TcMouthPlate> util = new ExcelUtil<>(TcMouthPlate.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }
}
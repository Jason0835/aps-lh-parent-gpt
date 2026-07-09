package com.zlt.aps.controller.tc;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.tc.api.domain.entity.TcGlueGroupOrder;
import com.zlt.aps.tc.api.service.ITcGlueGroupOrderRemoteService;
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
@Api(tags = "胎侧胶料组顺序")
@Controller
@RequestMapping("/tc/tcGlueGroupOrder")
public class TcGlueGroupOrderUIController extends BaseUIController<TcGlueGroupOrder> {

    private final String prefix = "aps/tc/glueGroupOrder";

    @Autowired
    private ITcGlueGroupOrderRemoteService iTcGlueGroupOrderService;

    @RequiresPermissions("tc:tcGlueGroupOrder:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/glueGroupOrder";
    }

    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("tcGlueGroupOrder", new TcGlueGroupOrder());
        return prefix + "/add";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("tcGlueGroupOrder", iTcGlueGroupOrderService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TcGlueGroupOrder query) {
        return iTcGlueGroupOrderService.list(query);
    }

    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @ResponseBody
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(iTcGlueGroupOrderService.getInfo(id));
    }

    @ApiOperation("保存")
    @PostMapping("/save")
    @RequiresPermissions("tc:tcGlueGroupOrder:edit")
    @ResponseBody
    public AjaxResult save(TcGlueGroupOrder tcGlueGroupOrder) {
        return iTcGlueGroupOrderService.save(tcGlueGroupOrder);
    }

    @ApiOperation("删除")
    @PostMapping("/remove")
    @RequiresPermissions("tc:tcGlueGroupOrder:remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTcGlueGroupOrderService.removeByIds(Arrays.asList(arr));
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(@RequestBody TcGlueGroupOrder query) {
        return iTcGlueGroupOrderService.checkUnique(query);
    }

    @ApiOperation("导出数据")
    @GetMapping("/export")
    @RequiresPermissions("tc:tcGlueGroupOrder:export")
    public void export(HttpServletResponse response, TcGlueGroupOrder entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.tc.glueGroupOrder.modelName");
        byte[] excelBytes = iTcGlueGroupOrderService.exportData(entity, fileName);
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
        context.setFunctionName(I18nUtil.getMessage("ui.data.column.tc.glueGroupOrder.modelName"));
        context.setProcedureCode(I18nUtil.getMessage("ui.data.column.tc.glueGroupOrder.modelName"));
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iTcGlueGroupOrderService.importData(context, updateSupport);
        return ajaxResult;
    }

    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.tc.glueGroupOrder.modelName");
        ExcelUtil<TcGlueGroupOrder> util = new ExcelUtil<>(TcGlueGroupOrder.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }
}
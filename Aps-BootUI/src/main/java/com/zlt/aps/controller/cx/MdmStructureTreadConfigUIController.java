package com.zlt.aps.controller.cx;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cx.api.service.IMdmStructureTreadConfigRemoteService;
import com.zlt.aps.mp.api.domain.entity.MdmStructureTreadConfig;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Api(tags = "胎面整车配置")
@Controller
@RequestMapping("/cx/mdmStructureTreadConfig")
public class MdmStructureTreadConfigUIController extends BaseUIController<MdmStructureTreadConfig> {

    @Autowired
    private IMdmStructureTreadConfigRemoteService mdmStructureTreadConfigRemoteService;

    @ApiOperation("条件查询列表")
    @RequiresPermissions("cx:mdmStructureTreadConfig:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MdmStructureTreadConfig query) {
        return mdmStructureTreadConfigRemoteService.list(query);
    }

    @ApiOperation("获取详情")
    @RequiresPermissions("cx:mdmStructureTreadConfig:list")
    @GetMapping("/{id}")
    @ResponseBody
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(mdmStructureTreadConfigRemoteService.getInfo(id));
    }

    @ApiOperation("新增")
    @RequiresPermissions("cx:mdmStructureTreadConfig:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody MdmStructureTreadConfig entity) {
        return mdmStructureTreadConfigRemoteService.add(entity);
    }

    @ApiOperation("修改")
    @RequiresPermissions("cx:mdmStructureTreadConfig:edit")
    @PutMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody MdmStructureTreadConfig entity) {
        return mdmStructureTreadConfigRemoteService.edit(entity);
    }

    @ApiOperation("删除")
    @RequiresPermissions("cx:mdmStructureTreadConfig:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        return mdmStructureTreadConfigRemoteService.remove(ids);
    }

    @ApiOperation("导出")
    @RequiresPermissions("cx:mdmStructureTreadConfig:export")
    @PostMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, MdmStructureTreadConfig entity) throws IOException {
        mdmStructureTreadConfigRemoteService.export(response, entity);
    }

    @ApiOperation("导入数据")
    @RequiresPermissions("cx:mdmStructureTreadConfig:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(@RequestPart("file") MultipartFile file,
                                 @RequestParam(defaultValue = "false") boolean updateSupport) throws Exception {
        byte[] data = file.getBytes();
        MdmStructureTreadConfig entity = new MdmStructureTreadConfig();
        ExcelUtil<MdmStructureTreadConfig> util = new ExcelUtil<>(MdmStructureTreadConfig.class);
        return mdmStructureTreadConfigRemoteService.importData(file, updateSupport);
    }

    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.mdmStructureTreadConfig.modelName", "胎面整车配置");
        ExcelUtil<MdmStructureTreadConfig> util = new ExcelUtil<>(MdmStructureTreadConfig.class);
        util.importTemplateExcel(response, fileName);
        return AjaxResult.success();
    }
}

package com.zlt.aps.controller.gsq;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.api.service.IGsqMachineInfoService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 钢丝圈机台信息UIController
 * 继承BaseUIController，通过Feign接口调用后端微服务
 *
 * @author zlt
 * @date 2021-05-28
 */
@Slf4j
@Api(tags = "钢丝圈机台信息")
@Controller
@RequestMapping("/gsq/machine")
public class GsqMachineInfoUIController extends BaseUIController<GsqMachineInfo> {

    @Autowired
    private IGsqMachineInfoService gsqMachineInfoService;

    private final String prefix = "gsq/machine";

    /**
     * 列表跳转至machine页面
     */
    @RequiresPermissions("gsq:machine:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/machine";
    }

    /**
     * 新增跳转至add页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("machineInfo", new GsqMachineInfo());
        return prefix + "/edit";
    }

    /**
     * 修改跳转至edit页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("machineInfo", gsqMachineInfoService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 查询钢丝圈机台信息列表
     */
    @ApiOperation("查询钢丝圈机台信息列表")
    @RequiresPermissions("gsq:machine:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(GsqMachineInfo machineInfo) {
        return gsqMachineInfoService.list(machineInfo);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions({"gsq:machine:edit", "gsq:machine:add"})
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(GsqMachineInfo machineInfo) {
        //校验机台编号唯一性
        if (UserConstants.NOT_UNIQUE.equals(gsqMachineInfoService.checkMachineCodeUnique(machineInfo))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.gsqMachineInfo.notUnique"));
        }
        //id为空则是新增操作，否则是编辑
        if (machineInfo.getId() != null) {
            return gsqMachineInfoService.edit(machineInfo);
        }
        return gsqMachineInfoService.add(machineInfo);
    }

    /**
     * 删除钢丝圈机台信息
     */
    @ApiOperation("删除钢丝圈机台信息")
    @RequiresPermissions("gsq:machine:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return gsqMachineInfoService.remove(arr);
    }

    /**
     * 校验机台编号唯一性
     */
    @ApiOperation("校验机台编号唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(GsqMachineInfo machineInfo) {
        return gsqMachineInfoService.checkMachineCodeUnique(machineInfo);
    }

    /**
     * 获取机台信息列表
     */
    @ApiOperation("获取机台信息列表")
    @PostMapping("/listMachineInfo")
    @ResponseBody
    public AjaxResult listMachineInfo(GsqMachineInfo machineInfo) {
        List<GsqMachineInfo> list = gsqMachineInfoService.listMachineInfo(machineInfo);
        return AjaxResult.success(list);
    }

    /**
     * 查询未删除且启用的机台列表
     * 改为GET请求，与Feign接口和后端微服务Controller保持一致
     * 直接透传Feign返回的AjaxResult，避免二次包装
     */
    @ApiOperation("查询未删除且启用的机台列表")
    @GetMapping("/listEnabledMachines")
    @ResponseBody
    public AjaxResult listEnabledMachines() {
        return gsqMachineInfoService.listEnabledMachines();
    }

    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "GSQ";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.gsq.machine.export.fileName");
    }

    /**
     * 下载导入模板
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<GsqMachineInfo> util = new ExcelUtil<>(GsqMachineInfo.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 导出钢丝圈机台信息
     */
    @ApiOperation("导出钢丝圈机台信息")
    @GetMapping("/export")
    @RequiresPermissions("gsq:machine:export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, GsqMachineInfo entity) throws IOException {
        List<GsqMachineInfo> list = gsqMachineInfoService.exportList(entity);
        ExcelUtil<GsqMachineInfo> util = new ExcelUtil<>(GsqMachineInfo.class);
        String fileName = this.getExportTemplateFileName();
        util.exportExcel(response, list, fileName, fileName);
    }

    /**
     * 导入钢丝圈机台信息
     */
    @PostMapping("/importData")
    @RequiresPermissions("gsq:machine:import")
    @ResponseBody
    @ApiOperation("导入钢丝圈机台信息")
    @Override
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        return gsqMachineInfoService.importData(context, updateSupport);
    }
}

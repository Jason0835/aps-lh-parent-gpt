package com.zlt.mix.controller.setting;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.ExcelUtil;
import com.zlt.mix.common.utils.ExportUtil;
import com.zlt.mix.common.utils.ImportUtil;
import com.zlt.mix.setting.api.domain.entity.AccessoriesMachine;
import com.zlt.mix.setting.api.service.IAccessoriesMachineService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 硫磺辅料与机台对应Controller
 *
 * @author Liam
 * @date 2022-04-18
 */
@Api(tags = "硫磺辅料与机台对应")
@Controller
@RequestMapping("/setting/accessoriesMachine")
public class AccessoriesMachineController extends BaseController {

    @Resource
    private IAccessoriesMachineService iAccessoriesMachineService;
    @Resource
    private IExportLogService iExportLogService;
    @Resource
    private IImportErrorLogService iImportErrorLogService;
    @Resource
    private IImportLogService iImportLogService;

    private final String prefix = "setting/accessoriesMachine";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("setting:accessoriesMachine:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/accessoriesMachine";
    }

    @ApiOperation("根据条件查询硫磺辅料与机台对应列表")
    @RequiresPermissions("setting:accessoriesMachine:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listAccessoriesMachine(AccessoriesMachine entity) {
        return iAccessoriesMachineService.listAccessoriesMachine(entity);
    }

    /**
     * 跳转至新增页面
     */
    @ApiOperation("跳转至新增页面")
    @GetMapping("/add")
    public String toAdd(ModelMap mmap) {
        mmap.put("accessoriesMachine", new AccessoriesMachine());
        mmap.put("editType", "0");
        return prefix + "/edit";
    }

    @ApiOperation("跳转至修改页面")
    @GetMapping("/edit/{id}")
    public String toEdit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("accessoriesMachine", iAccessoriesMachineService.getAccessoriesMachineInfo(id));
        mmap.put("editType", "1");
        return prefix + "/edit";
    }

    @ApiOperation("修改或新增硫磺辅料与机台对应")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveAccessoriesMachine(AccessoriesMachine accessoriesMachine) {
        return iAccessoriesMachineService.saveAccessoriesMachine(accessoriesMachine);
    }

    @ApiOperation("删除硫磺辅料与机台对应（id不为空）")
    @RequiresPermissions("setting:accessoriesMachine:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult removeAccessoriesMachine(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iAccessoriesMachineService.deleteAccessoriesMachine(arr);
    }

    @ApiOperation("校验硫磺辅料与机台对应唯一性")
    @PostMapping("/checkAccessoriesMachineUnique")
    @ResponseBody
    public String checkAccessoriesMachineUnique(AccessoriesMachine accessoriesMachine) {
        return iAccessoriesMachineService.checkAccessoriesMachineUnique(accessoriesMachine);
    }

    /**
     * 导出硫磺辅料与机台对应
     */
    @ApiOperation("导出硫磺辅料与机台对应")
    @RequiresPermissions("setting:accessoriesMachine:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, AccessoriesMachine accessoriesMachine) throws IOException {
        String fileName = I18nUtil.getMessage("setting.accessoriesMachine.modelName");
        List<AccessoriesMachine> list = iAccessoriesMachineService.exportData(accessoriesMachine);
        ExcelUtil<AccessoriesMachine> util = new ExcelUtil<>(AccessoriesMachine.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, accessoriesMachine.toString(), ZltConstant.PROCEDURE_CODE_FL_SETTING);
        iExportLogService.add(exportLog);
    }

    /**
     * 下载导入模板
     *
     * @param response 下载的模板文件
     * @throws IOException 异常
     */
    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("setting.accessoriesMachine.modelName");
        ExcelUtil<AccessoriesMachine> util = new ExcelUtil<>(AccessoriesMachine.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * excel数据导入
     *
     * @param file          要导入的文件
     * @param updateSupport 已存在的记录是否更新
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("setting:accessoriesMachine:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_FL_SETTING,
                I18nUtil.getMessage("setting.accessoriesMachine.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<AccessoriesMachine> util = new ExcelUtil<>(AccessoriesMachine.class);
        List<AccessoriesMachine> list = util.importExcel(in);
        //导入数据
        AjaxResult ajaxResult = iAccessoriesMachineService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

    /**
     * 根据密炼区和胶料名称查询机台信息
     */
    @ApiOperation("根据密炼区和胶料名称查询机台信息")
    @PostMapping("/getAccessoriesMachineList")
    @ResponseBody
    public AjaxResult getAccessoriesMachineList(AccessoriesMachine accessoriesMachine) {
        ArrayList<AccessoriesMachine> machineList = iAccessoriesMachineService.getAccessoriesMachineList(accessoriesMachine);
        return AjaxResult.success(machineList);
    }

    /**
     * 根据密炼区和胶料名称查询机台信息
     */
    @ApiOperation("根据密炼区和胶料名称查询机台信息")
    @PostMapping("/getRecipeMachineList")
    @ResponseBody
    public AjaxResult getRecipeMachineList(AccessoriesMachine accessoriesMachine) {
        ArrayList<AccessoriesMachine> machineList = iAccessoriesMachineService.listRecipeMachine(accessoriesMachine);
        return AjaxResult.success(machineList);
    }
}

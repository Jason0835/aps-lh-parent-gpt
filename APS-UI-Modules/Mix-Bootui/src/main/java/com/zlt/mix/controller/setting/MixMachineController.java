package com.zlt.mix.controller.setting;

import com.zlt.mix.common.core.utils.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.mix.common.core.constant.ZltConstant;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import javax.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import org.apache.poi.ss.usermodel.Workbook;
import com.zlt.mix.common.utils.ExportUtil;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.zlt.mix.common.utils.ImportUtil;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.io.*;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.zlt.file.encryptbyll.FileEncryptUtils;

import com.zlt.mix.setting.api.domain.entity.MixMachine;
import com.zlt.mix.setting.api.service.IMixMachineService;

/**
 * 密炼机台信息Controller
 * @author Gim
 * @date 2022-03-22
 */
@Api(tags = "密炼机台信息")
@Controller
@RequestMapping("/setting/machine")
public class MixMachineController extends BaseController {

    @Resource
    private IMixMachineService iMixMachineService;
    @Resource
    private IExportLogService iExportLogService;
    @Resource
    private IImportErrorLogService iImportErrorLogService;
    @Resource
    private IImportLogService iImportLogService;

    private final String prefix = "setting/machine";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("setting:machine:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/machine";
    }

    @ApiOperation("根据条件查询密炼机台信息列表")
    @RequiresPermissions("setting:machine:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listMixMachine(MixMachine entity) {
        return iMixMachineService.listMixMachine(entity);
    }

    /**
     * 获取机台信息列表
     */
    @ApiOperation("获取机台信息列表")
    @PostMapping("/getMachines")
    @ResponseBody
    public AjaxResult getMachines(MixMachine mixMachine) {
        mixMachine.setDelFlag("0");
        List<MixMachine> pcList = iMixMachineService.getMachines(mixMachine);
        return AjaxResult.success(pcList);
    }

    /**
     * 跳转至新增页面
     */
    @ApiOperation("跳转至新增页面")
    @GetMapping("/add")
    public String toAdd(ModelMap mmap) {
        mmap.put("mixMachine", new MixMachine());
        mmap.put("editType", "0");
        return prefix + "/edit";
    }

    @ApiOperation("跳转至修改页面")
    @GetMapping("/edit/{id}")
    public String toEdit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("mixMachine", iMixMachineService.getMixMachineInfo(id));
        mmap.put("editType", "1");
        return prefix + "/edit";
    }

    @ApiOperation("修改或新增密炼机台信息")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveMixMachine(MixMachine mixMachine) {
        return iMixMachineService.saveMixMachine(mixMachine);
    }

    @ApiOperation("删除密炼机台信息（id不为空）")
    @RequiresPermissions("setting:machine:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult removeMixMachine(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMixMachineService.deleteMixMachine(arr);
    }

    @ApiOperation("校验密炼机台信息唯一性")
    @PostMapping("/checkMixMachineUnique")
    @ResponseBody
    public String checkMixMachineUnique(MixMachine mixMachine) {
        return iMixMachineService.checkMixMachineUnique(mixMachine);
    }

    /**
     * 导出密炼机台信息
     */
    @ApiOperation("导出密炼机台信息")
    @RequiresPermissions("setting:machine:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response,MixMachine mixMachine) throws IOException {
        String fileName = I18nUtil.getMessage("setting.machine.modelName");
        List<MixMachine> list = iMixMachineService.exportData(mixMachine);
        ExcelUtil<MixMachine> util = new ExcelUtil<>(MixMachine. class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, mixMachine.toString(),ZltConstant.PROCEDURE_CODE_SETTING);
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
        String fileName = I18nUtil.getMessage("setting.machine.modelName");
        ExcelUtil<MixMachine> util = new ExcelUtil<>(MixMachine.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * excel数据导入
     *
     * @param file 要导入的文件
     * @param updateSupport 已存在的记录是否更新
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("setting:machine:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_SETTING,
                I18nUtil.getMessage("setting.machine.modelName"),file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<MixMachine> util = new ExcelUtil<>(MixMachine.class);
        List<MixMachine> list = util.importExcel(in);
        //导入数据
        AjaxResult ajaxResult = iMixMachineService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}

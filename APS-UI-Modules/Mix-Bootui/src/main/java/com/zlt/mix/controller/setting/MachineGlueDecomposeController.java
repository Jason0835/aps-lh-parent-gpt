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
import com.zlt.mix.setting.api.domain.dto.MachineGlueDecomposeDto;
import com.zlt.mix.setting.api.domain.entity.MachineGlueDecompose;
import com.zlt.mix.setting.api.service.IMachineGlueDecomposeService;
import com.zlt.mix.template.setting.MachineGlueDecomposeTemp;
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
import java.util.List;

/**
 * 密炼机指定胶料分解Controller
 *
 * @author Liam
 * @date 2022-03-29
 */
@Api(tags = "密炼机指定胶料分解")
@Controller
@RequestMapping("/setting/machineGlueDecompose")
public class MachineGlueDecomposeController extends BaseController {

    @Resource
    private IMachineGlueDecomposeService iMachineGlueDecomposeService;
    @Resource
    private IExportLogService iExportLogService;
    @Resource
    private IImportErrorLogService iImportErrorLogService;
    @Resource
    private IImportLogService iImportLogService;

    private final String prefix = "setting/machineGlueDecompose";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("setting:machineGlueDecompose:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/machineGlueDecompose";
    }

    @ApiOperation("根据条件查询密炼机指定胶料分解列表")
    @RequiresPermissions("setting:machineGlueDecompose:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listMachineGlueDecompose(MachineGlueDecompose entity) {
        return iMachineGlueDecomposeService.listMachineGlueDecompose(entity);
    }

    /**
     * 跳转至新增页面
     */
    @ApiOperation("跳转至新增页面")
    @GetMapping("/add")
    public String toAdd(ModelMap mmap) {
        mmap.put("machineGlueDecompose", new MachineGlueDecompose());
        return prefix + "/edit";
    }

    @ApiOperation("跳转至修改页面")
    @GetMapping("/edit/{id}")
    public String toEdit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("machineGlueDecompose", iMachineGlueDecomposeService.getMachineGlueDecomposeInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改或新增密炼机指定胶料分解")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveMachineGlueDecompose(MachineGlueDecompose machineGlueDecompose) {
        return iMachineGlueDecomposeService.saveMachineGlueDecompose(machineGlueDecompose);
    }

    @ApiOperation("删除密炼机指定胶料分解（id不为空）")
    @RequiresPermissions("setting:machineGlueDecompose:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult removeMachineGlueDecompose(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMachineGlueDecomposeService.deleteMachineGlueDecompose(arr);
    }

    @ApiOperation("校验密炼机指定胶料分解唯一性")
    @PostMapping("/checkMachineGlueDecomposeUnique")
    @ResponseBody
    public String checkMachineGlueDecomposeUnique(MachineGlueDecompose machineGlueDecompose) {
        return iMachineGlueDecomposeService.checkMachineGlueDecomposeUnique(machineGlueDecompose);
    }

    /**
     * 导出密炼机指定胶料分解
     */
    @ApiOperation("导出密炼机指定胶料分解")
    @RequiresPermissions("setting:machineGlueDecompose:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, MachineGlueDecompose machineGlueDecompose) throws IOException {
        String fileName = I18nUtil.getMessage("setting.machineGlueDecompose.modelName");
        List<MachineGlueDecomposeDto> list = iMachineGlueDecomposeService.exportData(machineGlueDecompose);
        ExcelUtil<MachineGlueDecomposeDto> util = new ExcelUtil<>(MachineGlueDecomposeDto.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, machineGlueDecompose.toString(), ZltConstant.PROCEDURE_CODE_SETTING);
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
        String fileName = I18nUtil.getMessage("setting.machineGlueDecompose.modelName");
        ExcelUtil<MachineGlueDecomposeTemp> util = new ExcelUtil<>(MachineGlueDecomposeTemp.class);
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
    @RequiresPermissions("setting:machineGlueDecompose:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_SETTING,
                I18nUtil.getMessage("setting.machineGlueDecompose.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<MachineGlueDecomposeDto> util = new ExcelUtil<>(MachineGlueDecomposeDto.class);
        List<MachineGlueDecomposeDto> list = util.importExcel(in);
        //导入数据
        AjaxResult ajaxResult = iMachineGlueDecomposeService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}

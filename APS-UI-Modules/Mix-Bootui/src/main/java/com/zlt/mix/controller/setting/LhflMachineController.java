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
import com.zlt.mix.setting.api.domain.entity.LhflMachine;
import com.zlt.mix.setting.api.service.ILhflMachineService;
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
 * 小料机台信息Controller
 *
 * @author Liam
 * @date 2022-04-18
 */
@Api(tags = "小料机台信息")
@Controller
@RequestMapping("/setting/lhflMachine")
public class LhflMachineController extends BaseController {

    @Resource
    private ILhflMachineService iLhflMachineService;
    @Resource
    private IExportLogService iExportLogService;
    @Resource
    private IImportErrorLogService iImportErrorLogService;
    @Resource
    private IImportLogService iImportLogService;

    private final String prefix = "setting/lhflMachine";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("setting:lhflMachine:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/lhflMachine";
    }

    @ApiOperation("根据条件查询小料机台信息列表")
    @RequiresPermissions("setting:lhflMachine:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listLhflMachine(LhflMachine entity) {
        return iLhflMachineService.listLhflMachine(entity);
    }

    /**
     * 跳转至新增页面
     */
    @ApiOperation("跳转至新增页面")
    @GetMapping("/add")
    public String toAdd(ModelMap mmap) {
        mmap.put("lhflMachine", new LhflMachine());
        mmap.put("editType", "0");
        return prefix + "/edit";
    }

    @ApiOperation("跳转至修改页面")
    @GetMapping("/edit/{id}")
    public String toEdit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("lhflMachine", iLhflMachineService.getLhflMachineInfo(id));
        mmap.put("editType", "1");
        return prefix + "/edit";
    }

    @ApiOperation("修改或新增小料机台信息")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveLhflMachine(LhflMachine lhflMachine) {
        return iLhflMachineService.saveLhflMachine(lhflMachine);
    }

    @ApiOperation("删除小料机台信息（id不为空）")
    @RequiresPermissions("setting:lhflMachine:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult removeLhflMachine(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iLhflMachineService.deleteLhflMachine(arr);
    }

    @ApiOperation("校验小料机台信息唯一性")
    @PostMapping("/checkLhflMachineUnique")
    @ResponseBody
    public String checkLhflMachineUnique(LhflMachine lhflMachine) {
        return iLhflMachineService.checkLhflMachineUnique(lhflMachine);
    }

    /**
     * 导出小料机台信息
     */
    @ApiOperation("导出小料机台信息")
    @RequiresPermissions("setting:lhflMachine:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, LhflMachine lhflMachine) throws IOException {
        String fileName = I18nUtil.getMessage("setting.lhflMachine.modelName");
        List<LhflMachine> list = iLhflMachineService.exportData(lhflMachine);
        ExcelUtil<LhflMachine> util = new ExcelUtil<>(LhflMachine.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, lhflMachine.toString(), ZltConstant.PROCEDURE_CODE_FL_SETTING);
        iExportLogService.add(exportLog);
    }

    /**
     * 获取小料机台信息
     */
    @ApiOperation("获取小料机台信息")
    @PostMapping("/getMachines")
    @ResponseBody
    public AjaxResult getMachines(LhflMachine lhflMachine) {
        List<LhflMachine> lhflMachines = iLhflMachineService.exportData(lhflMachine);
        return AjaxResult.success(lhflMachines);
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
        String fileName = I18nUtil.getMessage("setting.lhflMachine.modelName");
        ExcelUtil<LhflMachine> util = new ExcelUtil<>(LhflMachine.class);
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
    @RequiresPermissions("setting:lhflMachine:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_FL_SETTING,
                I18nUtil.getMessage("setting.lhflMachine.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<LhflMachine> util = new ExcelUtil<>(LhflMachine.class);
        List<LhflMachine> list = util.importExcel(in);
        //导入数据
        AjaxResult ajaxResult = iLhflMachineService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}

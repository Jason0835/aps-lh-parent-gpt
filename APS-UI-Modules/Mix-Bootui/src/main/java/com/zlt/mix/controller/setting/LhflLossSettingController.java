package com.zlt.mix.controller.setting;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
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
import com.zlt.mix.setting.api.domain.entity.LhflLossSetting;
import com.zlt.mix.setting.api.service.ILhflLossSettingService;
import com.zlt.mix.setting.api.service.IMixMachineService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
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
 * 硫磺辅料耗损率设定Controller
 *
 * @author Joran.zhang
 * @date 2022-05-23
 */
@Api(tags = "硫磺辅料耗损率设定")
@Controller
@RequestMapping("/setting/lhflLossSetting")
public class LhflLossSettingController extends BaseController {

    @Resource
    private ILhflLossSettingService lossSettingService;
    @Resource
    private IExportLogService iExportLogService;
    @Resource
    private IImportErrorLogService iImportErrorLogService;
    @Resource
    private IImportLogService iImportLogService;

    @Resource
    private IMixMachineService mixMachineService;
    @Autowired
    private ISysDictDataCacheService dictDataCacheService;

    private final String prefix = "setting/lhflLossSetting";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("setting:lhflLossSetting:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/lhflLossSetting";
    }

    @ApiOperation("根据条件查询炼胶时间信息列表")
    @RequiresPermissions("setting:lhflLossSetting:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listLossSetting(LhflLossSetting entity) {
        return lossSettingService.listLhflLossSetting(entity);
    }
//
    /**
     * 跳转至新增页面
     */
    @ApiOperation("跳转至新增页面")
    @GetMapping("/add")
    public String toAdd(ModelMap mmap) {
        mmap.put("lhflLossSetting", new LhflLossSetting());
       return prefix + "/edit";
    }

    @ApiOperation("跳转至修改页面")
    @GetMapping("/edit/{id}")
   public String toEdit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("lhflLossSetting", lossSettingService.getLhflLossSettingInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改或新增耗损率设定信息")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveLossSetting(LhflLossSetting lossSetting) {
       return lossSettingService.saveLhflLossSetting(lossSetting);
   }

    @ApiOperation("删除耗损率设定（id不为空）")
    @RequiresPermissions("setting:lhflLossSetting:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult removeLossSetting(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return lossSettingService.deleteLhflLossSetting(arr);
    }

    @ApiOperation("校验硫磺辅料耗损率唯一性")
    @PostMapping("/checkLossSettingUnique")
    @ResponseBody
    public String checkLossSettingUnique(LhflLossSetting lossSetting) {
        return lossSettingService.checkLhflLossSettingUnique(lossSetting);
    }

    /**
     * 导出炼胶时间信息
     */
    @ApiOperation("导出炼胶时间信息")
    @RequiresPermissions("setting:lhflLossSetting:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, LhflLossSetting lossSetting) throws IOException {
        String fileName = I18nUtil.getMessage("setting.lhflLossSetting.modelName");
        List<LhflLossSetting> list = lossSettingService.exportData(lossSetting);
        ExcelUtil<LhflLossSetting> util = new ExcelUtil<>(LhflLossSetting.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, lossSetting.toString(), ZltConstant.PROCEDURE_CODE_SETTING);
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
        String fileName = I18nUtil.getMessage("setting.lhflLossSetting.modelName");
        ExcelUtil<LhflLossSetting> util = new ExcelUtil<>(LhflLossSetting.class);
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
    @RequiresPermissions("setting:lhflLossSetting:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_FL_SETTING,
                I18nUtil.getMessage("setting.lhflLossSetting.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<LhflLossSetting> util = new ExcelUtil<>(LhflLossSetting.class);
        List<LhflLossSetting> list = util.importExcel(in);

        //导入数据
        AjaxResult ajaxResult = lossSettingService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}

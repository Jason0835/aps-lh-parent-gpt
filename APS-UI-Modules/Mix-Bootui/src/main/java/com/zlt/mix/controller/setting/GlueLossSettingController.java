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
import com.zlt.mix.setting.api.domain.entity.GlueLossSetting;
import com.zlt.mix.setting.api.service.IGlueLossSettingService;
import com.zlt.mix.setting.api.service.IMixMachineService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 胶料耗损率设定Controller
 *
 * @author Joran.zhang
 * @date 2022-05-23
 */
@Api(tags = "胶料耗损率设定")
@Controller
@RequestMapping("/setting/glueLossSetting")
public class GlueLossSettingController extends BaseController {

    @Resource
    private IGlueLossSettingService lossSettingService;
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

    private final String prefix = "setting/glueLossSetting";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("setting:glueLossSetting:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/glueLossSetting";
    }

    @ApiOperation("根据条件查询炼胶时间信息列表")
    @RequiresPermissions("setting:glueLossSetting:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listLossSetting(GlueLossSetting entity) {
        return lossSettingService.listGlueLossSetting(entity);
    }
//
    /**
     * 跳转至新增页面
     */
    @ApiOperation("跳转至新增页面")
    @GetMapping("/add")
    public String toAdd(ModelMap mmap) {
        mmap.put("glueLossSetting", new GlueLossSetting());
       return prefix + "/edit";
    }

    @ApiOperation("跳转至修改页面")
    @GetMapping("/edit/{id}")
   public String toEdit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("glueLossSetting", lossSettingService.getGlueLossSettingInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改或新增耗损率设定信息")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveLossSetting(GlueLossSetting lossSetting) {
       return lossSettingService.saveGlueLossSetting(lossSetting);
   }

    @ApiOperation("删除耗损率设定（id不为空）")
    @RequiresPermissions("setting:glueLossSetting:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult removeLossSetting(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return lossSettingService.deleteGlueLossSetting(arr);
    }

    @ApiOperation("校验胶料耗损率息唯一性")
    @PostMapping("/checkLossSettingUnique")
    @ResponseBody
    public String checkLossSettingUnique(GlueLossSetting lossSetting) {
        return lossSettingService.checkGlueLossSettingUnique(lossSetting);
    }

    /**
     * 导出炼胶时间信息
     */
    @ApiOperation("导出炼胶时间信息")
    @RequiresPermissions("setting:glueLossSetting:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, GlueLossSetting lossSetting) throws IOException {
        String fileName = I18nUtil.getMessage("setting.glueLossSetting.modelName");
        List<GlueLossSetting> list = lossSettingService.exportData(lossSetting);
        ExcelUtil<GlueLossSetting> util = new ExcelUtil<>(GlueLossSetting.class);
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
        String fileName = I18nUtil.getMessage("setting.glueLossSetting.modelName");
        ExcelUtil<GlueLossSetting> util = new ExcelUtil<>(GlueLossSetting.class);
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
    @RequiresPermissions("setting:glueLossSetting:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_SETTING,
                I18nUtil.getMessage("setting.glueLossSetting.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<GlueLossSetting> util = new ExcelUtil<>(GlueLossSetting.class);
        List<GlueLossSetting> list = util.importExcel(in);

        //导入数据
        AjaxResult ajaxResult = lossSettingService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}

package com.zlt.mix.controller.setting;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
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
import com.zlt.mix.setting.api.domain.entity.GlueUnclaimed;
import com.zlt.mix.setting.api.domain.entity.GlueUnclaimedImportModel;
import com.zlt.mix.setting.api.service.IGlueUnclaimedService;
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
import java.util.Date;
import java.util.List;

/**
 * 胶料白班待支领Controller
 * @author zlt
 * @date 2022-09-05
 */
@Api(tags = "胶料白班待支领")
@Controller
@RequestMapping("/setting/unclaimed")
public class GlueUnclaimedController extends BaseController {

    @Resource
    private IGlueUnclaimedService iGlueUnclaimedService;
    @Resource
    private IExportLogService iExportLogService;
    @Resource
    private IImportErrorLogService iImportErrorLogService;
    @Resource
    private IImportLogService iImportLogService;

    private final String prefix = "setting/unclaimed";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("setting:unclaimed:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/unclaimed";
    }

    @ApiOperation("根据条件查询胶料白班待支领列表")
    @RequiresPermissions("setting:unclaimed:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listGlueUnclaimed(GlueUnclaimed entity) {
        return iGlueUnclaimedService.listGlueUnclaimed(entity);
    }

    /**
     * 跳转至新增页面
     */
    @ApiOperation("跳转至新增页面")
    @GetMapping("/add")
    public String toAdd(ModelMap mmap) {
        mmap.put("glueUnclaimed", new GlueUnclaimed());
        return prefix + "/edit";
    }

    @ApiOperation("跳转至修改页面")
    @GetMapping("/edit/{id}")
    public String toEdit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("glueUnclaimed", iGlueUnclaimedService.getGlueUnclaimedInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改或新增胶料白班待支领")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveGlueUnclaimed(GlueUnclaimed glueUnclaimed) {
        return iGlueUnclaimedService.saveGlueUnclaimed(glueUnclaimed);
    }

    @ApiOperation("删除胶料白班待支领（id不为空）")
    @RequiresPermissions("setting:unclaimed:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult removeGlueUnclaimed(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iGlueUnclaimedService.deleteGlueUnclaimed(arr);
    }

    @ApiOperation("校验胶料白班待支领唯一性")
    @PostMapping("/checkGlueUnclaimedUnique")
    @ResponseBody
    public String checkGlueUnclaimedUnique(GlueUnclaimed glueUnclaimed) {
        return iGlueUnclaimedService.checkGlueUnclaimedUnique(glueUnclaimed);
    }

    /**
     * 导出胶料白班待支领
     */
    @ApiOperation("导出胶料白班待支领")
    @RequiresPermissions("setting:unclaimed:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response,GlueUnclaimed glueUnclaimed) throws IOException {
        String fileName = I18nUtil.getMessage("setting.unclaimed.modelName");
        List<GlueUnclaimed> list = iGlueUnclaimedService.exportData(glueUnclaimed);
        ExcelUtil<GlueUnclaimed> util = new ExcelUtil<>(GlueUnclaimed. class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, glueUnclaimed.toString(),ZltConstant.PROCEDURE_CODE_SETTING);
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
        String fileName = I18nUtil.getMessage("setting.unclaimed.modelName");
        ExcelUtil<GlueUnclaimedImportModel> util = new ExcelUtil<>(GlueUnclaimedImportModel.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("跳转至导入页面")
    @GetMapping("/importData")
    public String importDate(ModelMap mmp) {
        mmp.put("prefix", prefix);
        mmp.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));
        return prefix + "/importData";
    }

    /**
     * excel数据导入
     *
     * @param file 要导入的文件
     * @param updateSupport 已存在的记录是否更新
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("setting:unclaimed:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport, Date scheduleDate, String mixArea) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_SETTING,
                I18nUtil.getMessage("setting.unclaimed.modelName"),file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<GlueUnclaimedImportModel> util = new ExcelUtil<>(GlueUnclaimedImportModel.class);
        List<GlueUnclaimedImportModel> list = util.importExcel(in);
        List<GlueUnclaimed> newList = new ArrayList<>();
        for(GlueUnclaimedImportModel importModel : list) {
            GlueUnclaimed glueUnclaimed = new GlueUnclaimed();
            BeanUtils.copyProperties(importModel, glueUnclaimed);
            glueUnclaimed.setScheduleDate(scheduleDate);
            glueUnclaimed.setMixArea(mixArea);
            newList.add(glueUnclaimed);
        }
        //导入数据
        AjaxResult ajaxResult = iGlueUnclaimedService.importData(newList, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}

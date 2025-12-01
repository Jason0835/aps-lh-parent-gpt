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
import com.zlt.file.encryptbyll.FileEncryptUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.ExcelUtil;
import com.zlt.mix.common.utils.ExportUtil;
import com.zlt.mix.common.utils.ImportUtil;
import com.zlt.mix.setting.api.domain.dto.GlueFinishDto;
import com.zlt.mix.setting.api.domain.entity.GlueFinish;
import com.zlt.mix.setting.api.service.IGlueFinishService;
import com.zlt.mix.setting.api.service.IMixMachineService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
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
 * 炼胶时间信息Controller
 *
 * @author Gim
 * @date 2022-03-29
 */
@Api(tags = "炼胶时间信息")
@Controller
@RequestMapping("/setting/glueFinish")
public class GlueFinishController extends BaseController {

    @Resource
    private IGlueFinishService iGlueFinishService;
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

    private final String prefix = "setting/glueFinish";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("setting:glueFinish:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/glueFinish";
    }

    @ApiOperation("根据条件查询炼胶时间信息列表")
    @RequiresPermissions("setting:glueFinish:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listGlueFinish(GlueFinish entity) {
        return iGlueFinishService.listGlueFinish(entity);
    }
//
//    /**
//     * 跳转至新增页面
//     */
//    @ApiOperation("跳转至新增页面")
//    @GetMapping("/add")
//    public String toAdd(ModelMap mmap) {
//        mmap.put("glueFinish", new GlueFinish());
//        return prefix + "/edit";
//    }
//
//    @ApiOperation("跳转至修改页面")
//    @GetMapping("/edit/{id}")
//    public String toEdit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
//        mmap.put("glueFinish", iGlueFinishService.getGlueFinishInfo(id));
//        return prefix + "/edit";
//    }
//
//    @ApiOperation("修改或新增炼胶时间信息")
//    @PostMapping("/save")
//    @ResponseBody
//    public AjaxResult saveGlueFinish(GlueFinish glueFinish) {
//        return iGlueFinishService.saveGlueFinish(glueFinish);
//    }
//
//    @ApiOperation("删除炼胶时间信息（id不为空）")
//    @RequiresPermissions("setting:glueFinish:remove")
//    @PostMapping("/remove")
//    @ResponseBody
//    public AjaxResult removeGlueFinish(String ids) {
//        Long[] arr = Convert.toLongArray(ids);
//        return iGlueFinishService.deleteGlueFinish(arr);
//    }

    @ApiOperation("校验炼胶时间信息唯一性")
    @PostMapping("/checkGlueFinishUnique")
    @ResponseBody
    public String checkGlueFinishUnique(GlueFinish glueFinish) {
        return iGlueFinishService.checkGlueFinishUnique(glueFinish);
    }

    /**
     * 导出炼胶时间信息
     */
    @ApiOperation("导出炼胶时间信息")
    @RequiresPermissions("setting:glueFinish:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, GlueFinish glueFinish) throws IOException {
        String fileName = I18nUtil.getMessage("setting.glueFinish.modelName");
        List<GlueFinish> list = iGlueFinishService.exportData(glueFinish);
        ExcelUtil<GlueFinish> util = new ExcelUtil<>(GlueFinish.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, glueFinish.toString(), ZltConstant.PROCEDURE_CODE_SETTING);
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
        String fileName = I18nUtil.getMessage("setting.glueFinish.modelName");
        ExcelUtil<GlueFinishDto> util = new ExcelUtil<>(GlueFinishDto.class);
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
    @RequiresPermissions("setting:glueFinish:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_SETTING,
                I18nUtil.getMessage("setting.glueFinish.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<GlueFinish> util = new ExcelUtil<>(GlueFinish.class);
        List<GlueFinish> list = util.importExcel(in);

        //导入数据
        AjaxResult ajaxResult = iGlueFinishService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}

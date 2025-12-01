package com.zlt.mix.controller.setting;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
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
import com.zlt.mix.setting.api.domain.entity.GlueDecompose;
import com.zlt.mix.setting.api.service.IGlueDecomposeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
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
 * 终炼母炼分解Controller
 *
 * @author Liam
 * @date 2022-03-28
 */
@Api(tags = "终炼母炼分解")
@Controller
@RequestMapping("/setting/decompose")
public class GlueDecomposeController extends BaseController {

    @Resource
    private IGlueDecomposeService iGlueDecomposeService;
    @Resource
    private IExportLogService iExportLogService;
    @Resource
    private IImportErrorLogService iImportErrorLogService;
    @Resource
    private IImportLogService iImportLogService;

    private final String prefix = "setting/decompose";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("setting:decompose:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/decompose";
    }

    @ApiOperation("根据条件查询终炼母炼分解列表")
    @RequiresPermissions("setting:decompose:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listGlueDecompose(GlueDecompose entity) {
        return iGlueDecomposeService.listGlueDecompose(entity);
    }

    /**
     * 跳转至新增页面
     */
    @ApiOperation("跳转至新增页面")
    @GetMapping("/add")
    public String toAdd(ModelMap mmap) {
        mmap.put("glueDecompose", new GlueDecompose());
        return prefix + "/edit";
    }

    @ApiOperation("跳转至修改页面")
    @GetMapping("/edit/{id}")
    public String toEdit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("glueDecompose", iGlueDecomposeService.getGlueDecomposeInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改或新增终炼母炼分解")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveGlueDecompose(GlueDecompose glueDecompose) {
        return iGlueDecomposeService.saveGlueDecompose(glueDecompose);
    }

    @ApiOperation("删除终炼母炼分解（id不为空）")
    @RequiresPermissions("setting:decompose:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult removeGlueDecompose(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iGlueDecomposeService.deleteGlueDecompose(arr);
    }

    @ApiOperation("校验终炼母炼分解唯一性")
    @PostMapping("/checkGlueDecomposeUnique")
    @ResponseBody
    public String checkGlueDecomposeUnique(GlueDecompose glueDecompose) {
        return iGlueDecomposeService.checkGlueDecomposeUnique(glueDecompose);
    }


    @ApiOperation("检验母胶是否填写完整")
    @PostMapping("/checkComplete")
    @ResponseBody
    public AjaxResult checkComplete(GlueDecompose glueDecompose) {
        for (int i = 1; i < glueDecompose.getSegment(); i++) {
            if (StringUtils.isEmpty(ReflectUtils.invokeGetter(glueDecompose, "motherGlue" + i))) {
                return AjaxResult.success("0");
            }
        }
        return AjaxResult.success("1");
    }

    /**
     * 导出终炼母炼分解
     */
    @ApiOperation("导出终炼母炼分解")
    @RequiresPermissions("setting:decompose:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, GlueDecompose glueDecompose) throws IOException {
        String fileName = I18nUtil.getMessage("setting.decompose.modelName");
        List<GlueDecompose> list = iGlueDecomposeService.exportData(glueDecompose);
        ExcelUtil<GlueDecompose> util = new ExcelUtil<>(GlueDecompose.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, glueDecompose.toString(), ZltConstant.PROCEDURE_CODE_SETTING);
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
        String fileName = I18nUtil.getMessage("setting.decompose.modelName");
        ExcelUtil<GlueDecompose> util = new ExcelUtil<>(GlueDecompose.class);
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
    @RequiresPermissions("setting:decompose:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_SETTING,
                I18nUtil.getMessage("setting.decompose.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<GlueDecompose> util = new ExcelUtil<>(GlueDecompose.class);
        List<GlueDecompose> list = util.importExcel(in);
        //导入数据
        AjaxResult ajaxResult = iGlueDecomposeService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}

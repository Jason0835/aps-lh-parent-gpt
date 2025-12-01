package com.zlt.mix.controller.setting;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.ExcelUtil;
import com.zlt.mix.setting.api.domain.entity.RecipeType;
import com.zlt.mix.setting.api.service.IRecipeTypeService;
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


/**
 * 配方类型Controller
 * @author Joran.zhang
 * @date 2022-05-31
 */
@Api(tags = "配方类型")
@Controller
@RequestMapping("/setting/type")
public class RecipeTypeController extends BaseController {

    @Resource
    private IRecipeTypeService iRecipeTypeService;
    @Resource
    private IExportLogService iExportLogService;
    @Resource
    private IImportErrorLogService iImportErrorLogService;
    @Resource
    private IImportLogService iImportLogService;

    private final String prefix = "setting/type";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("setting:type:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/type";
    }

    @ApiOperation("根据条件查询配方类型列表")
    @RequiresPermissions("setting:type:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listRecipeType(RecipeType entity) {
        return iRecipeTypeService.listRecipeType(entity);
    }

    /**
     * 跳转至新增页面
     */
    @ApiOperation("跳转至新增页面")
    @GetMapping("/add")
    @RequiresPermissions("setting:type:add")
    public String toAdd(ModelMap mmap) {
        mmap.put("recipeType", new RecipeType());
        return prefix + "/edit";
    }

    @ApiOperation("跳转至修改页面")
    @GetMapping("/edit/{id}")
    @RequiresPermissions("setting:type:edit")
    public String toEdit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("recipeType", iRecipeTypeService.getRecipeTypeInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改或新增配方类型")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveRecipeType(RecipeType recipeType) {
        return iRecipeTypeService.saveRecipeType(recipeType);
    }

    @ApiOperation("删除配方类型（id不为空）")
    @RequiresPermissions("setting:type:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult removeRecipeType(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iRecipeTypeService.deleteRecipeType(arr);
    }

    @ApiOperation("校验配方类型唯一性")
    @PostMapping("/checkRecipeTypeUnique")
    @ResponseBody
    public String checkRecipeTypeUnique(RecipeType recipeType) {
        return iRecipeTypeService.checkRecipeTypeUnique(recipeType);
    }

    /**
     * 导出配方类型
     */
    @ApiOperation("导出配方类型")
    @RequiresPermissions("setting:type:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response,RecipeType recipeType) throws IOException {
        String fileName = I18nUtil.getMessage("setting.type.modelName");
        List<RecipeType> list = iRecipeTypeService.exportData(recipeType);
        ExcelUtil<RecipeType> util = new ExcelUtil<>(RecipeType. class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, recipeType.toString(),ZltConstant.PROCEDURE_CODE_SETTING);
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
        String fileName = I18nUtil.getMessage("setting.type.modelName");
        ExcelUtil<RecipeType> util = new ExcelUtil<>(RecipeType.class);
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
    @RequiresPermissions("setting:type:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_SETTING,
                I18nUtil.getMessage("setting.type.modelName"),file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<RecipeType> util = new ExcelUtil<>(RecipeType.class);
        List<RecipeType> list = util.importExcel(in);
        //导入数据
        AjaxResult ajaxResult = iRecipeTypeService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}

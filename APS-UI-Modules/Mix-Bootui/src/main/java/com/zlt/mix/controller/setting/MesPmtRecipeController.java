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
import com.zlt.file.encryptbyll.FileEncryptUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.ExcelUtil;
import com.zlt.mix.common.utils.ExportUtil;
import com.zlt.mix.common.utils.ImportUtil;
import com.zlt.mix.setting.api.domain.entity.MesPmtRecipe;
import com.zlt.mix.setting.api.domain.vo.MesPmtRecipeTemplateVo;
import com.zlt.mix.setting.api.service.IMesPmtRecipeService;
import com.zlt.mix.setting.api.service.ISyncDataService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 配方信息Controller
 * @author chen
 * @date 2022-06-01
 */
@Api(tags = "配方信息")
@Controller
@RequestMapping("/setting/MesPmtRecipe")
public class MesPmtRecipeController extends BaseController {

    @Resource
    private IMesPmtRecipeService iMesPmtRecipeService;
    @Resource
    private IExportLogService iExportLogService;
    @Resource
    private ISyncDataService iSyncDataService;
    @Resource
    private IImportErrorLogService iImportErrorLogService;
    @Resource
    private IImportLogService iImportLogService;

    private final String prefix = "setting/MesPmtRecipe";

    @Value("${recipeMachine.timeout:86400000}")
    private Long recipeMachineTimeout;

    @Resource
    private RedisTemplate<String, ArrayList<MesPmtRecipe>> redisTemplate;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("setting:MesPmtRecipe:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/MesPmtRecipe";
    }

    @ApiOperation("根据条件查询配方信息列表")
    @RequiresPermissions("setting:MesPmtRecipe:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listMesPmtRecipe(MesPmtRecipe entity) {
        return iMesPmtRecipeService.listMesPmtRecipe(entity);
    }

    /**
     * 导出配方信息
     */
    @ApiOperation("导出配方信息")
    @RequiresPermissions("setting:MesPmtRecipe:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response,MesPmtRecipe mesPmtRecipe) throws IOException {
        String fileName = I18nUtil.getMessage("setting.MesPmtRecipe.modelName");
        List<MesPmtRecipe> list = iMesPmtRecipeService.exportData(mesPmtRecipe);
        ExcelUtil<MesPmtRecipe> util = new ExcelUtil<>(MesPmtRecipe. class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, mesPmtRecipe.toString(),ZltConstant.PROCEDURE_CODE_SETTING);
        iExportLogService.add(exportLog);
    }

    /**
     * 根据机台名称和胶料名称查询配方信息
     *
     * @param mesPmtRecipe 机台名称和胶料名称
     * @return 配方集合
     */
    @ApiOperation("根据机台名称和胶料名称查询配方信息")
    @PostMapping("/selectMesPmtRecipeByParams")
    @ResponseBody
    public TableDataInfo selectMesPmtRecipeByParams(MesPmtRecipe mesPmtRecipe) {
        return iMesPmtRecipeService.selectMesPmtRecipeByParams(mesPmtRecipe);
    }

    /**
     * 根据密炼区、胶料名称，查询对应配方的机台信息
     *
     * @param mesPmtRecipe 密炼区、胶料名称
     * @return 对应配方的机台信息
     */
    @ApiOperation("根据密炼区、胶料名称，查询对应配方的机台信息")
    @PostMapping("/selectMesPmtRecipeMachine")
    @ResponseBody
    public AjaxResult selectMesPmtRecipeMachine(MesPmtRecipe mesPmtRecipe) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(ZltConstant.CACHE_RECIPE_MACHINE);
        String mixArea = mesPmtRecipe.getMixArea();
        String recipeMaterialName = mesPmtRecipe.getRecipeMaterialName();
        if (entries.size() > 0) {
            List<MesPmtRecipe> list = (List<MesPmtRecipe>) entries.get(mixArea + ":" + recipeMaterialName);
            // 有缓存但未找到匹配机台
            return AjaxResult.success(list == null ? new ArrayList<>() : list);
        }
        ArrayList<MesPmtRecipe> list;
        try {
            list = iMesPmtRecipeService.selectMesPmtRecipeMachine(new MesPmtRecipe());
        } catch (Exception e) {
            //获取返回值类型为List出现异常
            e.printStackTrace();
            return AjaxResult.error(I18nUtil.getMessage("ui.message.filterMachine.errorWhileGetMachine"));
        }
        Map<String, List<MesPmtRecipe>> recipeMixAreaMap = list.stream().collect(Collectors.groupingBy(item ->item.getMixArea() + ":" + item.getRecipeMaterialName()));
        redisTemplate.opsForHash().putAll(ZltConstant.CACHE_RECIPE_MACHINE, recipeMixAreaMap);
        List<MesPmtRecipe> data = recipeMixAreaMap.get(mixArea + ":" + recipeMaterialName);
        return AjaxResult.success(data == null ? new ArrayList<>() : data);
    }

    /**
     * 手动同步mes配方接口
     * @return 结果
     */
    @ApiOperation("手动同步mes配方接口")
    @RequiresPermissions("setting:MesPmtRecipe:syncMesPmtRecipe")
    @PostMapping("/syncMesPmtRecipe")
    @ResponseBody
    public AjaxResult syncMesPmtRecipe() {
    	// 同步配方前先同步一次物料
    	iSyncDataService.syncBasMaterial();
        return iSyncDataService.syncMesPmtRecipe();
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
        String fileName = I18nUtil.getMessage("setting.MesPmtRecipe.modelName");
        ExcelUtil<MesPmtRecipeTemplateVo> util = new ExcelUtil<>(MesPmtRecipeTemplateVo.class);
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
    @RequiresPermissions("setting:MesPmtRecipe:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_SETTING,
                I18nUtil.getMessage("setting.MesPmtRecipe.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<MesPmtRecipeTemplateVo> util = new ExcelUtil<>(MesPmtRecipeTemplateVo.class);
        List<MesPmtRecipeTemplateVo> list = util.importExcel(in);
        //导入数据
        AjaxResult ajaxResult = iMesPmtRecipeService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}

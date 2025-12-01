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
import com.zlt.mix.setting.api.domain.dto.MixingTimeDto;
import com.zlt.mix.setting.api.domain.entity.MixingTime;
import com.zlt.mix.setting.api.service.IMixMachineService;
import com.zlt.mix.setting.api.service.IMixingTimeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.Logical;
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
 * 炼胶时间信息Controller
 *
 * @author Liam
 * @date 2022-03-31
 */
@Api(tags = "炼胶时间信息")
@Controller
@RequestMapping("/setting/mixingTime")
public class MixingTimeController extends BaseController {

    @Resource
    private IMixingTimeService iMixingTimeService;
    @Resource
    private IExportLogService iExportLogService;
    @Resource
    private IImportErrorLogService iImportErrorLogService;
    @Resource
    private IImportLogService iImportLogService;
    @Autowired
    private IMixMachineService iMixMachineService;

    private final String prefix = "setting/mixingTime";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("setting:mixingTime:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/mixingTime";
    }

    @ApiOperation("根据条件查询炼胶时间信息列表")
    @RequiresPermissions("setting:mixingTime:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listMixingTime(MixingTime entity) {
        return iMixingTimeService.listMixingTime(entity);
    }

    /**
     * 跳转至新增页面
     */
    @ApiOperation("跳转至新增页面")
    @GetMapping("/add")
    public String toAdd(ModelMap mmap) {
        mmap.put("mixingTime", new MixingTime());
        return prefix + "/edit";
    }

    @ApiOperation("跳转至修改页面")
    @GetMapping("/edit/{id}")
    public String toEdit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("mixingTime", iMixingTimeService.getMixingTimeInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改或新增炼胶时间信息")
    @RequiresPermissions(value = {"setting:mixingTime:add", "setting:mixingTime:edit"}, logical = Logical.OR)
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveMixingTime(MixingTime mixingTime) {
        return iMixingTimeService.saveMixingTime(mixingTime);
    }

    @ApiOperation("删除炼胶时间信息（id不为空）")
    @RequiresPermissions("setting:mixingTime:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult removeMixingTime(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMixingTimeService.deleteMixingTime(arr);
    }

    @ApiOperation("校验炼胶时间信息唯一性")
    @PostMapping("/checkMixingTimeUnique")
    @ResponseBody
    public String checkMixingTimeUnique(MixingTime mixingTime) {
        return iMixingTimeService.checkMixingTimeUnique(mixingTime);
    }

    /**
     * 导出炼胶时间信息
     */
    @ApiOperation("导出炼胶时间信息")
    @RequiresPermissions("setting:mixingTime:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, MixingTime mixingTime) throws IOException {
        String fileName = I18nUtil.getMessage("setting.mixingTime.modelName");
        List<MixingTimeDto> list = iMixingTimeService.exportData(mixingTime);
        ExcelUtil<MixingTimeDto> util = new ExcelUtil<>(MixingTimeDto.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, mixingTime.toString(), ZltConstant.PROCEDURE_CODE_SETTING);
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
        String fileName = I18nUtil.getMessage("setting.mixingTime.modelName");
        ExcelUtil<MixingTimeDto> util = new ExcelUtil<>(MixingTimeDto.class);
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
    @RequiresPermissions("setting:mixingTime:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_SETTING,
                I18nUtil.getMessage("setting.mixingTime.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<MixingTimeDto> util = new ExcelUtil<>(MixingTimeDto.class);
        List<MixingTimeDto> list = util.importExcel(in);

        //导入数据
        AjaxResult ajaxResult = iMixingTimeService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}

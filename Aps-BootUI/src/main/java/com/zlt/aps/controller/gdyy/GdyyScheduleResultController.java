package com.zlt.aps.controller.gdyy;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common4ui.utils.file.FileUtils;
import com.zlt.aps.common.constant.ApsBootConstant;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.gdyy.api.domain.dto.GdyyScheduleResultDto;
import com.zlt.aps.gdyy.api.domain.dto.GdyyScheduleResultDto2;
import com.zlt.aps.gdyy.api.service.IGdyyScheduleResultService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import com.zlt.framework.utils.AuthorizationUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 钢带压延排程结果Controller
 *
 * @author chen
 * @date 2021-07-05
 */
@Api(tags = "钢带压延排程结果")
@Controller
@RequestMapping("/gdyy/scheduleResult")
public class GdyyScheduleResultController extends BaseController {

    private final String prefix = "gdyy/scheduleResult";
    @Autowired
    private IGdyyScheduleResultService iGdyyScheduleResultService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @Value("${excelTemplateModel}")
    private String excelTemplateModel;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("gdyy:scheduleResult:view")
    @GetMapping()
    public String toIndex(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        return prefix + "/scheduleResult";
    }

    /**
     * 跳转至插单页面
     */
    @ApiOperation("跳转到钢带压延排程结果插单页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        mmap.put("minDate", DateUtils.parseDateToStr("yyyy-MM-dd", new Date()));  //当前日期
        mmap.put("scheduleResult", new GdyyScheduleResultDto());
        return prefix + "/insertOrder";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping(value = "/edit/{id}")
    @ApiOperation("获取钢带压延排程结果信息详细信息,跳转到编辑页面")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("scheduleResult", iGdyyScheduleResultService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 跳转到调计划量页面
     *
     * @return 结果
     */
    @ApiOperation("调计划量")
    @GetMapping("/changeQty/{id}")
    public String changeQty(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("scheduleResult", iGdyyScheduleResultService.getInfo(id));
        return prefix + "/changeQty";
    }

    /**
     * 弹出自动排程日期选择框
     *
     * @return 结果
     */
    @ApiOperation("弹出自动排程日期选择框")
    @GetMapping("/toAutoPlan")
    public String toAutoPlan(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        return prefix + "/autoPlan";
    }

    /**
     * 根据条件查询钢带压延排程结果列表
     */
    @ApiOperation("根据条件查询钢带压延排程结果列表")
    @RequiresPermissions("gdyy:scheduleResult:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(GdyyScheduleResultDto dto) {
        //设置默认排程日期,这里在后端设置会有问题
        if (dto.getScheduleDate() == null) {
            dto.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        dto.setYear(DateFormatUtils.format(dto.getScheduleDate(), "yyyy"));
        dto.setMonth(DateFormatUtils.format(dto.getScheduleDate(), "MM"));
        return iGdyyScheduleResultService.list(dto);
    }

    /**
     * 修改或新增钢带压延排程结果
     */
    @ApiOperation("修改或新增钢带压延排程结果（id为空则新增，id不为空则修改）")
    @RequiresPermissions({"gdyy:scheduleResult:edit", "gdyy:scheduleResult:insertOrder"})
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(GdyyScheduleResultDto dto) {
        if (dto.getId() == null) {
            double class1Plan = dto.getClass1Plan() == null ? 0d : dto.getClass1Plan();
            double class2Plan = dto.getClass2Plan() == null ? 0d : dto.getClass2Plan();
            double class3Plan = dto.getClass3Plan() == null ? 0d : dto.getClass3Plan();
            // 若插单量为0报错
            if ((class1Plan + class2Plan + class3Plan) == 0d) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.qty.zero"));
            }
            dto.setDataSource("1");
        }
        return iGdyyScheduleResultService.edit(dto);
    }

    /**
     * 调量
     */
    @ApiOperation("调量")
    @RequiresPermissions("gdyy:scheduleResult:changePlan")
    @PostMapping("/changeQty")
    @ResponseBody
    public AjaxResult changeQty(GdyyScheduleResultDto dto) {
        return iGdyyScheduleResultService.changeQty(dto);
    }

    /**
     * 删除钢带压延排程结果
     */
    @ApiOperation("删除钢带压延排程结果（id不为空）")
    @RequiresPermissions("gdyy:scheduleResult:remove")
    @PostMapping("/remove")
    @ResponseBody
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(String ids) {
        String newIds = "";
        String scheduleDate = "";
        if (StringUtils.isNotBlank(ids)){
            newIds = ids.substring(0,ids.indexOf("|"));
            scheduleDate = ids.substring(ids.indexOf("|")+1);
        }
//        GdyyScheduleResultDto queryEntity = new GdyyScheduleResultDto();
//        queryEntity.setScheduleDate(DateUtils.parseDate(scheduleDate));
//        if(iGdyyScheduleResultService.isPublish(queryEntity)){
//            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.hasPublishedCanNotDelete"));
//        }
        Long[] arr = Convert.toLongArray(newIds);
        return iGdyyScheduleResultService.remove(arr);
    }

    /**
     * 导出钢带压延排程结果
     */
    @ApiOperation("导出钢带压延排程结果")
    @RequiresPermissions("gdyy:scheduleResult:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, GdyyScheduleResultDto dto) throws IOException {
        //若是没传日期则默认查询当日排程
        if (dto.getScheduleDate() == null) {
            dto.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        dto.setYear(DateFormatUtils.format(dto.getScheduleDate(), "yyyy"));
        dto.setMonth(DateFormatUtils.format(dto.getScheduleDate(), "MM"));
        //获取字节流数据
        byte[] data = iGdyyScheduleResultService.exportData(dto);
        if (data == null) {
            return;
        }
        String fileName = I18nUtil.getMessage("ui.data.column.gdyy.scheduleResult.modelName");
        ExportLog exportLog = ExportUtil.uploadAndExportExcelByByte(response, data, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_GDYY);
        iExportLogService.add(exportLog);
    }

    /**
     * 生成自动排程前校验
     *
     * @param dto 日期
     * @return 响应
     */
    @ApiOperation("校验选择的日期是否已经生成排程记录")
    @PostMapping("/validateAutoPlan")
    @ResponseBody
    public AjaxResult validateAutoPlan(GdyyScheduleResultDto dto) {
        int releasingOrTimeoutByDate = iGdyyScheduleResultService.isReleasingOrTimeoutByDate(dto);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        /*
         当天已经生成过排程记录，给予提示
         */
        Boolean unique = iGdyyScheduleResultService.checkUnique(dto);
        if (unique) {
            // 未生成，直接生成
            return AjaxResult.success("1");
        } else {
            // 排程记录已生成，弹窗提示，确认后重新生成
            return AjaxResult.success("2");
        }
    }

    /**
     * 校验记录唯一性
     *
     * @param dto 日期及钢丝圈代码
     * @return 是否唯一
     */
    @ApiOperation("校验记录唯一性")
    @PostMapping("/checkScheduleResultUnique")
    @ResponseBody
    public String checkUnique(GdyyScheduleResultDto dto) {
        // 根据传入的日期查询是否已经生成排程记录
        Boolean unique = iGdyyScheduleResultService.checkUnique(dto);
        if (!unique) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 自动排程
     */
    @ApiOperation("自动排程")
    @RequiresPermissions("gdyy:scheduleResult:autoPlan")
    @PostMapping("/autoPlan")
    @ResponseBody
    public AjaxResult autoPlan(GdyyScheduleResultDto dto) {
        // 用户点击过确定重新生成排程记录,或已有权限重新生成排程记录
        //TODO 执行自动排程算法
        return iGdyyScheduleResultService.autoPlan(dto);
    }

    /**
     * 发布排程
     */
    @ApiOperation("发布排程")
    @RequiresPermissions("gdyy:scheduleResult:publish")
    @PostMapping("/publish")
    @ResponseBody
    public AjaxResult publish(GdyyScheduleResultDto dto) {
        if (dto.getScheduleDate() == null) {
            dto.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        return iGdyyScheduleResultService.publish(dto);
    }

    /**
     * 插单校验
     */
    @PostMapping("/validateAdd")
    @ResponseBody
    public AjaxResult validateAdd(GdyyScheduleResultDto dto) {
        if (dto.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        int releasingOrTimeoutByDate = iGdyyScheduleResultService.isReleasingOrTimeoutByDate(dto);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        // 根据传入的日期查询是否已经生成排程记录
        Boolean unique = iGdyyScheduleResultService.checkUnique(dto);
        if (unique) {
            return AjaxResult.success("0");
        }
        return AjaxResult.success();
    }

    /**
     * 跳转到公共排程结果导入页面
     *
     * @param mmap 用于存放当前模块前缀路径
     */
    @GetMapping("/toImport")
    public String toImport(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        mmap.put("prefix", prefix);
        return "common/importData";
    }

    /**
     * 跳转到公共排程结果导入页面
     *
     * @param mmap 用于存放当前模块前缀路径
     */
    @GetMapping("/toImport2")
    public String toImport2(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        mmap.put("prefix", prefix);
        return "common/importData2";
    }

    /**
     * 下载模板
     *
     * @param response
     * @throws IOException
     */
    @ApiOperation("下载模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String lang = AuthorizationUtils.getLang();  //国际化编码
        String tempName = (ApsBootConstant.EN_US.equals(lang) ? ApsBootConstant.GDYY_EN_TEMP : ApsBootConstant.GDYY_ZH_TEMP);  //根据国际化获取导入模板名称
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelTemplateModel + "gdyy/" + tempName + ".xlsx");
        if (in == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.common.message.fileNotFound"));
        }
        String fileName = I18nUtil.getMessage("ui.data.column.gdyy.scheduleResult.modelName");
        ExcelUtil.setResponseHeader(response, fileName);
        FileUtils.writeInputStream(in, response.getOutputStream());
        return AjaxResult.success();
    }

    /**
     * 数据导入
     *
     * @param file
     * @param updateSupport
     * @return
     * @throws Exception
     */
    @RequiresPermissions("gdyy:scheduleResult:import")
    @ApiOperation("数据导入")
    @PostMapping("/importScheduleData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, Date scheduleDate) throws Exception {
        if (scheduleDate.before(DateUtils.getNowDate("yyyy-MM-dd"))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.scheduleDateError"));
        }
        //若当天存在发布成功、发布中、超时失败的记录则不予导入
        GdyyScheduleResultDto entity = new GdyyScheduleResultDto();
        entity.setScheduleDate(scheduleDate);
        int releasingOrTimeoutByDate = iGdyyScheduleResultService.isReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        Boolean isPublish = iGdyyScheduleResultService.isPublish(entity);
        if (isPublish) {
            return AjaxResult.error(I18nUtil.getMessage("ui.biz.alter.publishedNotImport"));
        }
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data,
                ApsConstant.PROCEDURE_CODE_GDYY,
                I18nUtil.getMessage("ui.data.column.gdyy.scheduleResult.modelName"),
                file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<GdyyScheduleResultDto> util = new ExcelUtil<>(GdyyScheduleResultDto.class);
        List<GdyyScheduleResultDto> list = util.importExcel(in, 1);

        AjaxResult ajaxResult = iGdyyScheduleResultService.importData(list, importLog.getId(), DateFormatUtils.format(scheduleDate, "yyyy-MM-dd"));
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入失败详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
    /**
     * 数据导入
     *
     * @param file
     * @param updateSupport
     * @return
     * @throws Exception
     */
    @RequiresPermissions("gdyy:scheduleResult:import2")
    @ApiOperation("数据导入")
    @PostMapping("/importScheduleData2")
    @ResponseBody
    public AjaxResult importData2(MultipartFile file, Date scheduleDate) throws Exception {
        if (scheduleDate.before(DateUtils.getNowDate("yyyy-MM-dd"))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.scheduleDateError"));
        }
        //若当天存在发布成功、发布中、超时失败的记录则不予导入
        GdyyScheduleResultDto entity = new GdyyScheduleResultDto();
        entity.setScheduleDate(scheduleDate);
        int releasingOrTimeoutByDate = iGdyyScheduleResultService.isReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        Boolean isPublish = iGdyyScheduleResultService.isPublish(entity);
        if (isPublish) {
            return AjaxResult.error(I18nUtil.getMessage("ui.biz.alter.publishedNotImport"));
        }
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data,
                ApsConstant.PROCEDURE_CODE_GDYY,
                I18nUtil.getMessage("ui.data.column.gdyy.scheduleResult.modelName"),
                file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<GdyyScheduleResultDto2> util = new ExcelUtil<>(GdyyScheduleResultDto2.class);
        List<GdyyScheduleResultDto2> list = util.importExcel(in, 1);
        List<GdyyScheduleResultDto> newList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(list)) {
            newList = list.stream().map(a -> {
                GdyyScheduleResultDto result = new GdyyScheduleResultDto();
                BeanUtils.copyProperties(a, result);
                return result;
            }).collect(Collectors.toList());
        }
        AjaxResult ajaxResult = iGdyyScheduleResultService.importData(newList, importLog.getId(), DateFormatUtils.format(scheduleDate, "yyyy-MM-dd"));
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入失败详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

    /**
     * 跳转到更改发布状态页面
     */
    @GetMapping("/toChangeReleaseStatus")
    public String changeReleaseStatus(ModelMap map, Date scheduleDate) {
        map.put("prefix", prefix);
        map.put("scheduleDate", scheduleDate);
        return "common/changeReleaseStatus";
    }

    /**
     * 更改发布状态
     */
    @ApiOperation("更改发布状态")
    @RequiresRoles("admin")
    @PostMapping("/changeReleaseStatus")
    @ResponseBody
    public AjaxResult changeReleaseStatus(GdyyScheduleResultDto entity) {
        Date scheduleDate = entity.getScheduleDate();
        if (scheduleDate == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        return iGdyyScheduleResultService.changeReleaseStatus(entity);
    }
}

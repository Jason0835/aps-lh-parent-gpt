package com.zlt.aps.controller.gsq;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.constant.GatewayConstants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common4ui.utils.file.FileUtils4UI;
import com.zlt.aps.common.constant.ApsBootConstant;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.gsq.api.domain.dto.GsqScheduleResultDto;
import com.zlt.aps.gsq.api.domain.dto.GsqScheduleResultDto2;
import com.zlt.aps.gsq.api.domain.entity.GsqDayFinishQty;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.api.service.IGsqMachineInfoService;
import com.zlt.aps.gsq.api.service.IGsqScheduleResultService;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 钢丝圈排程结果Controller
 *
 * @author chen
 * @date 2021-06-21
 */
@Api(tags = "钢丝圈排程结果")
@Controller
@RequestMapping("/gsq/scheduleResult")
public class GsqScheduleResultController extends BaseController {

    private final String prefix = "gsq/scheduleResult";
    @Autowired
    private IGsqScheduleResultService iGsqScheduleResultService;
    @Autowired
    private IGsqMachineInfoService iGsqMachineInfoService;
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
    @RequiresPermissions("gsq:scheduleResult:view")
    @ApiOperation("跳转到钢丝圈排程结果首页")
    @GetMapping()
    public String toIndex(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        return prefix + "/scheduleResult";
    }

    /**
     * 跳转至插单页面
     */
    @ApiOperation("跳转到钢丝圈排程结果插单页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        mmap.put("minDate", DateUtils.parseDateToStr("yyyy-MM-dd", new Date()));  //当前日期
        mmap.put("scheduleResult", new GsqScheduleResultDto());
        return prefix + "/insertOrder";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping(value = "/edit/{id}")
    @ApiOperation("获取钢丝圈排程结果信息详细信息,跳转到编辑页面")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("scheduleResult", iGsqScheduleResultService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 跳转到转机台页面
     *
     * @return 结果
     */
    @ApiOperation("转机台")
    @GetMapping("/changeMachine/{id}")
    public String changeMachine(@PathVariable("id") Long id, ModelMap mmap) {
        // 编辑类型为转机台
        mmap.put("editType", "1");
        mmap.put("scheduleResult", iGsqScheduleResultService.getInfo(id));
        return prefix + "/changeQtyOrMachine";
    }


    /**
     * 跳转至转机台
     */
    @GetMapping("/batchChangeMachine/{ids}")
    public String batchChangeMachine(@PathVariable("ids") String ids, ModelMap mmap) {
        String[] split = ids.split(",");
        List<Long> idList = new ArrayList<>();
        for (String s : split) {
            idList.add(Long.valueOf(s));
        }
        GsqScheduleResultDto scheduleResult = new GsqScheduleResultDto();
        scheduleResult.setIds2(idList);
        mmap.put("selectList", iGsqScheduleResultService.getInfos(scheduleResult));
        return prefix + "/changeQtyOrMachine2";
    }

    /**
     * 跳转到调计划量页面
     *
     * @return 结果
     */
    @ApiOperation("调计划量")
    @GetMapping("/changeQty/{id}")
    public String changeQty(@PathVariable("id") Long id, ModelMap mmap) {
        // 编辑类型为调计划量
        mmap.put("editType", "2");
        mmap.put("scheduleResult", iGsqScheduleResultService.getInfo(id));
        return prefix + "/changeQtyOrMachine";
    }

    /**
     * 跳转到选机台
     *
     * @return 结果
     */
    @ApiOperation("跳转到选机台")
    @GetMapping("/chooseMachine/{id}")
    public String chooseMachine(@PathVariable("id") String idAndRowIndex, ModelMap mmap) {
        String[] idAndRowIndexArr = idAndRowIndex.split(",");
        GsqScheduleResultDto dto = iGsqScheduleResultService.getInfo(Long.valueOf(idAndRowIndexArr[0]));
        GsqMachineInfo machineInfo = new GsqMachineInfo();
        machineInfo.setStatus("0");
        List<GsqMachineInfo> machineInfoList = iGsqMachineInfoService.exportList(machineInfo);
        Map<String, GsqMachineInfo> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(item -> item.getId() + "", item -> item));

        if (StringUtils.isNotEmpty(dto.getMachineId())) {
            List<GsqMachineInfo> newMachineInfoList = new ArrayList<>();
            String[] machineIds = dto.getMachineId().split(",");
            for (String item : machineIds) {
                if (machineCodeMap.get(item) != null) {
                    newMachineInfoList.add(machineCodeMap.get(item));
                }
            }
            mmap.put("machineInfoList", newMachineInfoList);
        } else {
            mmap.put("machineInfoList", machineInfoList);
        }
        mmap.put("id", idAndRowIndexArr[0]);
        mmap.put("rowIndex", idAndRowIndexArr[1]);
        mmap.put("publishSuccessCount", idAndRowIndexArr[2]);
        return prefix + "/chooseMachine";
    }

    /**
     * 选机台
     *
     * @return 结果
     */
    @ApiOperation("选机台")
    @PostMapping("/chooseMachine")
    @ResponseBody
    public AjaxResult chooseMachine(GsqScheduleResultDto dto) {
        return iGsqScheduleResultService.chooseMachine(dto);
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
     * 根据条件查询钢丝圈排程结果列表
     */
    @ApiOperation("根据条件查询钢丝圈排程结果列表")
    @RequiresPermissions("gsq:scheduleResult:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(GsqScheduleResultDto dto) {
        //设置默认排程日期,这里在后端设置会有问题
        if (dto.getScheduleDate() == null) {
            dto.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        dto.setYear(DateFormatUtils.format(dto.getScheduleDate(), "yyyy"));
        dto.setMonth(DateFormatUtils.format(dto.getScheduleDate(), "MM"));
        return iGsqScheduleResultService.list(dto);
    }

    /**
     * 修改或新增钢丝圈排程结果
     */
    @ApiOperation("修改或新增钢丝圈排程结果（id为空则新增，id不为空则修改）")
    @RequiresPermissions("gsq:scheduleResult:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(GsqScheduleResultDto dto) {
        if (dto.getId() == null) {
            double class1Plan = dto.getMidPlanQty() == null ? 0d : dto.getMidPlanQty();
            double class2Plan = dto.getNightPlanQty() == null ? 0d : dto.getNightPlanQty();
            double class3Plan = dto.getDayPlanQty() == null ? 0d : dto.getDayPlanQty();
            // 若插单量为0报错
            if ((class1Plan + class2Plan + class3Plan) == 0d) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.qty.zero"));
            }
            dto.setDataSource("1");
            return iGsqScheduleResultService.add(dto);
        }
        return iGsqScheduleResultService.edit(dto);
    }

    /**
     * 转机台
     */
    @ApiOperation("转机台")
    @RequiresPermissions("gsq:scheduleResult:changeMachine")
    @PostMapping("/changeMachine")
    @ResponseBody
    public AjaxResult changeMachine(GsqScheduleResultDto dto) {
        return iGsqScheduleResultService.changeMachine(dto);
    }

    /**
     * 转机台
     */
    @ApiOperation("转机台")
    @PostMapping("/batchChangeMachine/{machineId}")
    @ResponseBody
    public AjaxResult batchChangeMachine(@PathVariable("machineId") String machineId, String selects) {
        List<GsqScheduleResultDto> scheduleResultList = JSON.parseArray(selects, GsqScheduleResultDto.class);
        GsqScheduleResultDto query = new GsqScheduleResultDto();
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        for (GsqScheduleResultDto scheduleResult : scheduleResultList) {
            query.setId(scheduleResult.getId());
            query.setScheduleDate(scheduleResult.getScheduleDate());
            query.setMachineId(machineId);
            query.setSteelRingCode(scheduleResult.getSteelRingCode());
            Boolean unique = iGsqScheduleResultService.checkUnique(query);
            if (!unique) {
                if (sb1.length() > 0) {
                    sb1.append(",").append(query.getSteelRingCode());
                } else {
                    sb1.append(query.getSteelRingCode());
                }
                continue;
            }
            scheduleResult.setMachineId(machineId);
            AjaxResult result = iGsqScheduleResultService.changeMachine(scheduleResult);
            if (result.get(GatewayConstants.MSG_TAG).equals(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"))) {
                if (sb2.length() > 0) {
                    sb2.append(",").append(query.getSteelRingCode());
                } else {
                    sb2.append(query.getSteelRingCode());
                }
            }
        }
        if (sb1.length() > 0) {
            sb1.append(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }
        if (sb2.length() > 0) {
            sb2.append(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById2"));
        }
        sb1.append(sb2);
        if (sb1.length() > 0) {
            return AjaxResult.error(sb1.toString());
        }
        return AjaxResult.success();
    }

    /**
     * 调量
     */
    @ApiOperation("调量")
    @RequiresPermissions("gsq:scheduleResult:changeQty")
    @PostMapping("/changeQty")
    @ResponseBody
    public AjaxResult changeQty(GsqScheduleResultDto dto) {
        return iGsqScheduleResultService.changeQty(dto);
    }

    /**
     * 删除钢丝圈排程结果
     */
    @ApiOperation("删除钢丝圈排程结果（id不为空）")
    @RequiresPermissions("gsq:scheduleResult:remove")
    @PostMapping("/remove")
    @ResponseBody
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(String ids) {
        String newIds = "";
        String scheduleDate = "";
        if (StringUtils.isNotBlank(ids)) {
            newIds = ids.substring(0, ids.indexOf("|"));
            scheduleDate = ids.substring(ids.indexOf("|") + 1);
        }
//        GsqScheduleResultDto queryEntity = new GsqScheduleResultDto();
//        queryEntity.setScheduleDate(DateUtils.parseDate(scheduleDate));
//        if (iGsqScheduleResultService.isPublish(queryEntity)) {
//            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.hasPublishedCanNotDelete"));
//        }
        Long[] arr = Convert.toLongArray(newIds);
        return iGsqScheduleResultService.remove(arr);
    }

    /**
     * 导出钢丝圈排程结果
     */
    @ApiOperation("导出钢丝圈排程结果")
    @RequiresPermissions("gsq:scheduleResult:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, GsqScheduleResultDto dto) throws IOException {
        //若是没传日期则默认查询当日排程
        if (dto.getScheduleDate() == null) {
            dto.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        dto.setYear(DateFormatUtils.format(dto.getScheduleDate(), "yyyy"));
        dto.setMonth(DateFormatUtils.format(dto.getScheduleDate(), "MM"));
        //获取字节流数据
        byte[] data = iGsqScheduleResultService.exportData(dto);
        if (data == null) {
            return;
        }
        String fileName = I18nUtil.getMessage("ui.data.column.gsq.scheduleResult.modelName");
        ExportLog exportLog = ExportUtil.uploadAndExportExcelByByte(response, data, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_GSQ);
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
    public AjaxResult validateAutoPlan(GsqScheduleResultDto dto) {
        /*
         当天已经生成过排程记录，给予提示
         前端根据返回状态码500判断是否给予用户弹窗提示
         1为已生成未发布 2为已发布 （已发布有权限重新生成直接返回success）
         */
        if (dto.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        int releasingOrTimeoutByDate = iGsqScheduleResultService.isReleasingOrTimeoutByDate(dto);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        Boolean unique = iGsqScheduleResultService.checkUnique(dto);
        if (unique) {
            // 未生成，直接生成
            return AjaxResult.success("2");
        } else {
            // 排程记录已生成，弹窗提示，确认后重新生成
            return AjaxResult.success("1");
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
    public String checkUnique(GsqScheduleResultDto dto) {
        // 根据传入的日期查询是否已经生成排程记录
        Boolean unique = iGsqScheduleResultService.checkUnique(dto);
        if (!unique) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 自动排程
     */
    @ApiOperation("自动排程")
    @RequiresPermissions("gsq:scheduleResult:autoPlan")
    @PostMapping("/autoPlan")
    @ResponseBody
    public AjaxResult autoPlan(GsqScheduleResultDto dto) {
        // 用户点击过确定重新生成排程记录,或已有权限重新生成排程记录
        //TODO 执行自动排程算法
        return iGsqScheduleResultService.autoPlan(dto);
    }

    /**
     * 发布排程
     */
    @ApiOperation("发布排程")
    @RequiresPermissions("gsq:scheduleResult:publish")
    @PostMapping("/publish")
    @ResponseBody
    public AjaxResult publish(GsqScheduleResultDto dto) {
        // 默认发布当天排程结果
        if (dto.getScheduleDate() == null) {
            dto.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        //TODO 发布排程
        return iGsqScheduleResultService.publish(dto);
    }

    /**
     * 插单校验
     */
    @PostMapping("/validateAdd")
    @ResponseBody
    public AjaxResult validateAdd(GsqScheduleResultDto dto) {
        int releasingOrTimeoutByDate = iGsqScheduleResultService.isReleasingOrTimeoutByDate(dto);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        Boolean unique = iGsqScheduleResultService.checkUnique(dto);
        if (unique) {
            return AjaxResult.success("0");
        }
        return AjaxResult.success();
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
        String tempName = (ApsBootConstant.EN_US.equals(lang) ? ApsBootConstant.GSQ_EN_TEMP : ApsBootConstant.GSQ_ZH_TEMP);  //根据国际化获取导入模板名称
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelTemplateModel + "gsq/" + tempName + ".xlsx");
        if (in == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.common.message.fileNotFound"));
        }
        String fileName = I18nUtil.getMessage("ui.data.column.gsq.scheduleResult.modelName");
        ExcelUtil.setResponseHeader(response, fileName);
        FileUtils4UI.writeInputStream(in, response.getOutputStream());
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
     * 数据导入
     *
     * @param file
     * @return
     * @throws Exception
     */
    @RequiresPermissions("gsq:scheduleResult:import")
    @ApiOperation("数据导入")
    @PostMapping("/importScheduleData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, Date scheduleDate) throws Exception {
        if (scheduleDate.before(DateUtils.getNowDate("yyyy-MM-dd"))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.scheduleDateError"));
        }
        //若当天存在发布成功的记录则不予导入
        GsqScheduleResultDto entity = new GsqScheduleResultDto();
        entity.setScheduleDate(scheduleDate);
        int releasingOrTimeoutByDate = iGsqScheduleResultService.isReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        Boolean isPublish = iGsqScheduleResultService.isPublish(entity);
        if (isPublish) {
            return AjaxResult.error(I18nUtil.getMessage("ui.biz.alter.publishedNotImport"));
        }
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_GSQ,
                I18nUtil.getMessage("ui.data.column.gsq.scheduleResult.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        ExcelUtil<GsqScheduleResultDto> util = new ExcelUtil<>(GsqScheduleResultDto.class);
        List<GsqScheduleResultDto> list = util.importExcel(in, 1);
        String format = DateFormatUtils.format(scheduleDate, "yyyy-MM-dd");
        AjaxResult ajaxResult = iGsqScheduleResultService.importData(list, importLog.getId(), format);
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
     * @return
     * @throws Exception
     */
    @RequiresPermissions("gsq:scheduleResult:import2")
    @ApiOperation("数据导入")
    @PostMapping("/importScheduleData2")
    @ResponseBody
    public AjaxResult importData2(MultipartFile file, Date scheduleDate) throws Exception {
        if (scheduleDate.before(DateUtils.getNowDate("yyyy-MM-dd"))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.scheduleDateError"));
        }
        //若当天存在发布成功的记录则不予导入
        GsqScheduleResultDto entity = new GsqScheduleResultDto();
        entity.setScheduleDate(scheduleDate);
        int releasingOrTimeoutByDate = iGsqScheduleResultService.isReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        Boolean isPublish = iGsqScheduleResultService.isPublish(entity);
        if (isPublish) {
            return AjaxResult.error(I18nUtil.getMessage("ui.biz.alter.publishedNotImport"));
        }
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_GSQ,
                I18nUtil.getMessage("ui.data.column.gsq.scheduleResult.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        ExcelUtil<GsqScheduleResultDto2> util = new ExcelUtil<>(GsqScheduleResultDto2.class);
        List<GsqScheduleResultDto2> list = util.importExcel(in, 1);
        List<GsqScheduleResultDto> newList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(list)) {
            for (GsqScheduleResultDto2 gsq : list) {
                GsqScheduleResultDto dto = new GsqScheduleResultDto();
                BeanUtils.copyProperties(gsq, dto);
                newList.add(dto);
            }
        }
        String format = DateFormatUtils.format(scheduleDate, "yyyy-MM-dd");
        AjaxResult ajaxResult = iGsqScheduleResultService.importData(newList, importLog.getId(), format);
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
    public AjaxResult changeReleaseStatus(GsqScheduleResultDto entity) {
        Date scheduleDate = entity.getScheduleDate();
        if (scheduleDate == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        return iGsqScheduleResultService.changeReleaseStatus(entity);
    }

    /**
     * 完成量下载模板
     *
     * @param response 下载
     * @throws IOException 异常
     */
    @ApiOperation("完成量下载模板")
    @GetMapping("/importFinishQtyTemplate")
    @ResponseBody
    public AjaxResult importFinishQtyTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.dayFinishQty.modelName");
        ExcelUtil<GsqDayFinishQty> util = new ExcelUtil<>(GsqDayFinishQty.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 完成量数据导入
     *
     * @param file 要导入的文件
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("gsq:finishQty:import")
    @ApiOperation("完成量数据导入")
    @PostMapping("/importFinishQty")
    @ResponseBody
    public AjaxResult importFinishQty(MultipartFile file) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data,
                ApsConstant.PROCEDURE_CODE_GSQ,
                I18nUtil.getMessage("ui.data.column.dayFinishQty.modelName"),
                file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<GsqDayFinishQty> util = new ExcelUtil<>(GsqDayFinishQty.class);
        List<GsqDayFinishQty> list = util.importExcel(in);

        AjaxResult ajaxResult = iGsqScheduleResultService.importFinishQty(list, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入失败详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    @PostMapping("/getSummaryVo")
    @ApiOperation("获取排程日期的排程结果合计")
    @ResponseBody
    public AjaxResult getSummaryVo(GsqScheduleResultDto scheduleResult) {
        return iGsqScheduleResultService.getSummaryVo(scheduleResult);
    }
}

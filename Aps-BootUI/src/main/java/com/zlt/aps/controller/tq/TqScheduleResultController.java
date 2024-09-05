package com.zlt.aps.controller.tq;

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
import com.ruoyi.common4ui.utils.StringUtils;
import com.ruoyi.common4ui.utils.file.FileUtils;
import com.zlt.aps.common.constant.ApsBootConstant;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.tq.api.domain.dto.TqScheduleResultDto;
import com.zlt.aps.tq.api.domain.dto.TqScheduleResultDto2;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.api.service.ITqMachineInfoService;
import com.zlt.aps.tq.api.service.ITqScheduleResultService;
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
 * 胎圈排程结果Controller
 *
 * @author chen
 * @date 2021-06-21
 */
@Api(tags = "胎圈排程结果")
@Controller
@RequestMapping("/tq/scheduleResult")
public class TqScheduleResultController extends BaseController {

    private final String prefix = "tq/scheduleResult";
    @Autowired
    private ITqScheduleResultService iTqScheduleResultService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @Autowired
    private ITqMachineInfoService machineInfoService;

    @Value("${excelTemplateModel}")
    private String excelTemplateModel;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("tq:scheduleResult:view")
    @ApiOperation("跳转到胎圈排程结果首页")
    @GetMapping()
    public String toIndex(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        return prefix + "/scheduleResult";
    }

    /**
     * 跳转至插单页面
     */
    @ApiOperation("跳转到胎圈排程结果插单页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        mmap.put("minDate", DateUtils.parseDateToStr("yyyy-MM-dd", new Date()));  //当前日期
        mmap.put("scheduleResult", new TqScheduleResultDto());
        return prefix + "/insertOrder";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping(value = "/edit/{id}")
    @ApiOperation("获取胎圈排程结果信息详细信息,跳转到编辑页面")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("scheduleResult", iTqScheduleResultService.getInfo(id));
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
        mmap.put("scheduleResult", iTqScheduleResultService.getInfo(id));
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
        TqScheduleResultDto scheduleResult = new TqScheduleResultDto();
        scheduleResult.setIds2(idList);
        mmap.put("selectList", iTqScheduleResultService.getInfos(scheduleResult));
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
        mmap.put("scheduleResult", iTqScheduleResultService.getInfo(id));
        return prefix + "/changeQtyOrMachine";
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
     * 根据条件查询胎圈排程结果列表
     */
    @ApiOperation("根据条件查询胎圈排程结果列表")
    @RequiresPermissions("tq:scheduleResult:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TqScheduleResultDto dto) {
        //设置默认排程日期,这里在后端设置会有问题
        if (dto.getScheduleDate() == null) {
            dto.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        dto.setYear(DateFormatUtils.format(dto.getScheduleDate(), "yyyy"));
        dto.setMonth(DateFormatUtils.format(dto.getScheduleDate(), "MM"));
        return iTqScheduleResultService.list(dto);
    }

    /**
     * 修改或新增胎圈排程结果
     */
    @ApiOperation("修改或新增胎圈排程结果（id为空则新增，id不为空则修改）")
    @RequiresPermissions("tq:scheduleResult:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(TqScheduleResultDto dto) {
        if (dto.getId() == null) {
            double class1Plan = dto.getMidPlanQty() == null ? 0d : dto.getMidPlanQty();
            double class2Plan = dto.getNightPlanQty() == null ? 0d : dto.getNightPlanQty();
            double class3Plan = dto.getDayPlanQty() == null ? 0d : dto.getDayPlanQty();
            double class4Plan = dto.getNextMidPlanQty() == null ? 0d : dto.getNextMidPlanQty();
            // 若插单量为0报错
            if ((class1Plan + class2Plan + class3Plan + class4Plan) == 0d) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.qty.zero"));
            }
            dto.setDataSource("1");
        }
        return iTqScheduleResultService.edit(dto);
    }

    /**
     * 转机台
     */
    @ApiOperation("转机台")
    @RequiresPermissions("tq:scheduleResult:changeMachine")
    @PostMapping("/changeMachine")
    @ResponseBody
    public AjaxResult changeMachine(TqScheduleResultDto dto) {
        return iTqScheduleResultService.changeMachine(dto);
    }

    /**
     * 转机台
     */
    @ApiOperation("转机台")
    @PostMapping("/batchChangeMachine/{machineId}")
    @ResponseBody
    public AjaxResult batchChangeMachine(@PathVariable("machineId") String machineId, String selects) {
        List<TqScheduleResultDto> scheduleResultList = JSON.parseArray(selects, TqScheduleResultDto.class);
        TqScheduleResultDto query = new TqScheduleResultDto();
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        for (TqScheduleResultDto scheduleResult : scheduleResultList) {
            query.setId(scheduleResult.getId());
            query.setScheduleDate(scheduleResult.getScheduleDate());
            query.setMachineId(machineId);
            query.setBeadCode(scheduleResult.getBeadCode());
            Boolean unique = iTqScheduleResultService.checkUnique(query);
            if (!unique) {
                if (sb1.length() > 0) {
                    sb1.append(",").append(query.getBeadCode());
                } else {
                    sb1.append(query.getBeadCode());
                }
                continue;
            }
            scheduleResult.setMachineId(machineId);
            AjaxResult result = iTqScheduleResultService.changeMachine(scheduleResult);
            if (result.get(GatewayConstants.MSG_TAG).equals(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"))) {
                if (sb2.length() > 0) {
                    sb2.append(",").append(query.getBeadCode());
                } else {
                    sb2.append(query.getBeadCode());
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
    @RequiresPermissions("tq:scheduleResult:changeQty")
    @PostMapping("/changeQty")
    @ResponseBody
    public AjaxResult changeQty(TqScheduleResultDto dto) {
        return iTqScheduleResultService.changeQty(dto);
    }

    /**
     * 删除胎圈排程结果
     */
    @ApiOperation("删除胎圈排程结果（id不为空）")
    @RequiresPermissions("tq:scheduleResult:remove")
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
//        TqScheduleResultDto queryEntity = new TqScheduleResultDto();
//        queryEntity.setScheduleDate(DateUtils.parseDate(scheduleDate));
//        if (iTqScheduleResultService.isPublish(queryEntity)) {
//            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.hasPublishedCanNotDelete"));
//        }
        Long[] arr = Convert.toLongArray(newIds);
        return iTqScheduleResultService.remove(arr);
    }

    /**
     * 导出胎圈排程结果
     */
    @ApiOperation("导出胎圈排程结果")
    @RequiresPermissions("tq:scheduleResult:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, TqScheduleResultDto dto) throws IOException {
        //若是没传日期则默认查询当日排程
        if (dto.getScheduleDate() == null) {
            dto.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        dto.setYear(DateFormatUtils.format(dto.getScheduleDate(), "yyyy"));
        dto.setMonth(DateFormatUtils.format(dto.getScheduleDate(), "MM"));
        //获取字节流数据
        byte[] data = iTqScheduleResultService.exportData(dto);
        if (data == null) {
            return;
        }
        String fileName = I18nUtil.getMessage("ui.data.column.tq.scheduleResult.modelName");
        ExportLog exportLog = ExportUtil.uploadAndExportExcelByByte(response, data, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_TQ);
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
    public AjaxResult validateAutoPlan(TqScheduleResultDto dto) {
        /*
         当天已经生成过排程记录，给予提示
         前端根据返回状态码500判断是否给予用户弹窗提示
         1为已生成未发布 2为已发布 （已发布有权限重新生成直接返回success）
         */
        if (dto.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        int releasingOrTimeoutByDate = iTqScheduleResultService.isReleasingOrTimeoutByDate(dto);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        Boolean unique = iTqScheduleResultService.checkUnique(dto);
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
    public String checkUnique(TqScheduleResultDto dto) {
        // 根据传入的日期查询是否已经生成排程记录
        Boolean unique = iTqScheduleResultService.checkUnique(dto);
        if (!unique) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 自动排程
     */
    @ApiOperation("自动排程")
    @RequiresPermissions("tq:scheduleResult:autoPlan")
    @PostMapping("/autoPlan")
    @ResponseBody
    public AjaxResult autoPlan(TqScheduleResultDto dto) {
        // 用户点击过确定重新生成排程记录,或已有权限重新生成排程记录
        //TODO 执行自动排程算法
        return iTqScheduleResultService.autoPlan(dto);
    }

    /**
     * 发布排程
     */
    @ApiOperation("发布排程")
    @RequiresPermissions("tq:scheduleResult:publish")
    @PostMapping("/publish")
    @ResponseBody
    public AjaxResult publish(TqScheduleResultDto dto) {
        if (dto.getScheduleDate() == null) {
            dto.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        return iTqScheduleResultService.publish(dto);
    }

    /**
     * 插单校验
     */
    @ApiOperation("插单校验")
    @PostMapping("/validateAdd")
    @ResponseBody
    public AjaxResult validateAdd(TqScheduleResultDto dto) {
        int releasingOrTimeoutByDate = iTqScheduleResultService.isReleasingOrTimeoutByDate(dto);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        Boolean unique = iTqScheduleResultService.checkUnique(dto);
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
        String tempName = (ApsBootConstant.EN_US.equals(lang) ? ApsBootConstant.TQ_EN_TEMP : ApsBootConstant.TQ_ZH_TEMP);  //根据国际化获取导入模板名称
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelTemplateModel + "tq/" + tempName + ".xlsx");
        if (in == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.common.message.fileNotFound"));
        }
        String fileName = I18nUtil.getMessage("ui.data.column.tq.scheduleResult.modelName");
        ExcelUtil.setResponseHeader(response, fileName);
        FileUtils.writeInputStream(in, response.getOutputStream());
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
     * @param updateSupport
     * @return
     * @throws Exception
     */
    @RequiresPermissions("tq:scheduleResult:import2")
    @ApiOperation("数据导入")
    @PostMapping("/importScheduleData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, Date scheduleDate) throws Exception {
        if (scheduleDate.before(DateUtils.getNowDate("yyyy-MM-dd"))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.scheduleDateError"));
        }
        //若当天存在发布成功的记录则不予导入
        TqScheduleResultDto entity = new TqScheduleResultDto();
        entity.setScheduleDate(scheduleDate);
        int releasingOrTimeoutByDate = iTqScheduleResultService.isReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        Boolean isPublish = iTqScheduleResultService.isPublish(entity);
        if (isPublish) {
            return AjaxResult.error(I18nUtil.getMessage("ui.biz.alter.publishedNotImport"));
        }
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_TQ,
                I18nUtil.getMessage("ui.data.column.tq.scheduleResult.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<TqScheduleResultDto> util = new ExcelUtil<>(TqScheduleResultDto.class);
        List<TqScheduleResultDto> list = util.importExcel(in, 1);

        AjaxResult ajaxResult = iTqScheduleResultService.importData(list, importLog.getId(), DateFormatUtils.format(scheduleDate, "yyyy-MM-dd"));
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
    @RequiresPermissions("tq:scheduleResult:import")
    @ApiOperation("数据导入")
    @PostMapping("/importScheduleData2")
    @ResponseBody
    public AjaxResult importData2(MultipartFile file, Date scheduleDate) throws Exception {
        if (scheduleDate.before(DateUtils.getNowDate("yyyy-MM-dd"))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.scheduleDateError"));
        }
        //若当天存在发布成功的记录则不予导入
        TqScheduleResultDto entity = new TqScheduleResultDto();
        entity.setScheduleDate(scheduleDate);
        int releasingOrTimeoutByDate = iTqScheduleResultService.isReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        Boolean isPublish = iTqScheduleResultService.isPublish(entity);
        if (isPublish) {
            return AjaxResult.error(I18nUtil.getMessage("ui.biz.alter.publishedNotImport"));
        }
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_TQ,
                I18nUtil.getMessage("ui.data.column.tq.scheduleResult.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<TqScheduleResultDto2> util = new ExcelUtil<>(TqScheduleResultDto2.class);
        List<TqScheduleResultDto2> list = util.importExcel(in, 1);
        List<TqScheduleResultDto> newList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(list)) {
            for (TqScheduleResultDto2 tq : list) {
                TqScheduleResultDto dto = new TqScheduleResultDto();
                BeanUtils.copyProperties(tq, dto);
                newList.add(dto);
            }
        }

        AjaxResult ajaxResult = iTqScheduleResultService.importData(newList, importLog.getId(), DateFormatUtils.format(scheduleDate, "yyyy-MM-dd"));
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入失败详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }


    /**
     * 跳转至选机台页面
     */
    @GetMapping("/chooseMachine/{id}")
    public String chooseMachine(@PathVariable("id") String idAndRowIndex, ModelMap mmap) {
        String[] idAndRowIndexArr = idAndRowIndex.split(",");
        TqScheduleResultDto scheduleResult = iTqScheduleResultService.getInfo(Long.valueOf(idAndRowIndexArr[0]));
        TqMachineInfo machineInfo = new TqMachineInfo();
        machineInfo.setStatus("0");
        List<TqMachineInfo> machineInfoList = machineInfoService.exportList(machineInfo);
        Map<String, TqMachineInfo> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(b -> b.getId() + "", a -> a));

        if (StringUtils.isNotEmpty(scheduleResult.getMachineId())) {
            List<TqMachineInfo> newMachineInfoList = new ArrayList<>();
            String[] machineIds = scheduleResult.getMachineId().split(",");
            for (String item : machineIds) {
                if (machineCodeMap.get(item) != null) {
                    newMachineInfoList.add(machineCodeMap.get(item));
                }
            }
            mmap.put("machineInfoList", newMachineInfoList);
        } else {
            mmap.put("machineInfoList", machineInfoList);
        }
        mmap.put("scheduleResult", scheduleResult);
        mmap.put("rowIndex", idAndRowIndexArr[1]);
        return prefix + "/chooseMachine";
    }

    /**
     * 选机台
     */
    @ApiOperation("选机台")
    @PostMapping("/chooseMachine")
    @ResponseBody
    public AjaxResult chooseMachine(TqScheduleResultDto entity) {
        return iTqScheduleResultService.chooseMachine(entity);
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
    public AjaxResult changeReleaseStatus(TqScheduleResultDto entity) {
        Date scheduleDate = entity.getScheduleDate();
        if (scheduleDate == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        return iTqScheduleResultService.changeReleaseStatus(entity);
    }
}

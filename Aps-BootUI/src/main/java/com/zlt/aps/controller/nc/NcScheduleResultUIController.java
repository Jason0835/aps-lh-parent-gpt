package com.zlt.aps.controller.nc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSON;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.constant.GatewayConstants;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.utils.file.FileUtils4UI;
import com.zlt.aps.common.constant.ApsBootConstant;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.nc.api.domain.entity.NcDayFinishQty;
import com.zlt.aps.nc.api.domain.entity.NcDayFinishQty;
import com.zlt.aps.nc.api.domain.entity.NcScheduleResult;
import com.zlt.aps.nc.api.service.INcScheduleResultRemoteService;
import com.zlt.framework.utils.AuthorizationUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 内衬胶排程结果Controller
 *
 * @author zlt
 * @date 2021-06-24
 */
@Api(tags = "内衬胶排程结果")
@Controller
@RequestMapping("/nc/ncScheduleResult")
public class NcScheduleResultUIController extends BaseController<NcScheduleResult> {

    @Autowired
    private INcScheduleResultRemoteService iNcScheduleResultService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @Autowired
    private IImportLogService iImportLogService;

    @Value("${excelTemplateModel}")
    private String excelTemplateModel;


    private String prefix = "nc/ncScheduleResult";

    /**
     * 根据条件查询垫胶排程结果列表
     */
    @ApiOperation("根据条件查询垫胶排程结果列表")
    @RequiresPermissions("nc:ncScheduleResult:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(NcScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            entity.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        entity.setYear(DateFormatUtils.format(entity.getScheduleDate(), "yyyy"));
        entity.setMonth(DateFormatUtils.format(entity.getScheduleDate(), "MM"));
        return iNcScheduleResultService.list(entity);
    }

    /**
     * 修改或新增垫胶排程结果
     */
    @ApiOperation("修改或新增垫胶排程结果")
    @RequiresPermissions("nc:ncScheduleResult:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(NcScheduleResult entity) {

        NcScheduleResult query = new NcScheduleResult();
        query.setId(entity.getId());
        query.setScheduleDate(entity.getScheduleDate());
        query.setMachineCode(entity.getMachineCode());
        query.setLiningCode(entity.getLiningCode());
        Boolean isUnique = iNcScheduleResultService.checkUnique(query);
        if (!isUnique) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }

        AjaxResult ajaxResult = null;
        if (entity.getId() != null) {
            ajaxResult = iNcScheduleResultService.edit(entity);
        } else {
            BigDecimal dayPlanQty = BigDecimalUtils.valueOf(entity.getClass1PlanQty());
            BigDecimal nightPlanQty = BigDecimalUtils.valueOf(entity.getClass2PlanQty());
            // 若插单量为0报错
            if (BigDecimalUtils.add(dayPlanQty, nightPlanQty).compareTo(BigDecimal.ZERO) == 0) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.qty.zero"));
            }
            entity.setDataSource("1");
            ajaxResult = iNcScheduleResultService.add(entity);
        }
        return ajaxResult;
    }

    /**
     * 转机台
     */
    @ApiOperation("转机台")
    @PostMapping("/changeMachine")
    @ResponseBody
    public AjaxResult changeMachine(NcScheduleResult scheduleResult) {

        NcScheduleResult query = new NcScheduleResult();
        query.setId(scheduleResult.getId());
        query.setScheduleDate(scheduleResult.getScheduleDate());
        query.setMachineCode(scheduleResult.getMachineCode());
        query.setLiningCode(scheduleResult.getLiningCode());
        Boolean isUnique = iNcScheduleResultService.checkUnique(query);
        if (!isUnique) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }
        AjaxResult ajaxResult = iNcScheduleResultService.changeMachine(scheduleResult);
        return ajaxResult;
    }

    /**
     * 转机台
     */
    @ApiOperation("转机台")
    @PostMapping("/batchChangeMachine/{machineCode}")
    @ResponseBody
    public AjaxResult batchChangeMachine(@PathVariable("machineCode") String machineCode, String selects) {
        List<NcScheduleResult> scheduleResultList = JSON.parseArray(selects, NcScheduleResult.class);
        NcScheduleResult query = new NcScheduleResult();
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        for (NcScheduleResult scheduleResult : scheduleResultList) {
            query.setId(scheduleResult.getId());
            query.setScheduleDate(scheduleResult.getScheduleDate());
            query.setMachineCode(machineCode);
            query.setLiningCode(scheduleResult.getLiningCode());
            Boolean isUnique = iNcScheduleResultService.checkUnique(query);
            if (!isUnique) {
                if (sb1.length() > 0) {
                    sb1.append(",").append(query.getLiningCode());
                } else {
                    sb1.append(query.getLiningCode());
                }
                continue;
            }
            scheduleResult.setMachineCode(machineCode);
            AjaxResult result = iNcScheduleResultService.changeMachine(scheduleResult);
            if (result.get(GatewayConstants.MSG_TAG).equals(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"))) {
                if (sb2.length() > 0) {
                    sb2.append(",").append(query.getLiningCode());
                } else {
                    sb2.append(query.getLiningCode());
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
     * 调量前置校验（产能校验）
     */
    @ApiOperation("调量前置校验")
    @PostMapping("/changeQtyValidate")
    @ResponseBody
    public AjaxResult changeQtyValidate(NcScheduleResult scheduleResult) {
        return iNcScheduleResultService.changeQtyValidate(scheduleResult);
    }

    /**
     * 调量
     */
    @ApiOperation("调量")
    @PostMapping("/changeQty")
    @ResponseBody
    public AjaxResult changeQty(NcScheduleResult scheduleResult) {
        AjaxResult ajaxResult = iNcScheduleResultService.changeQty(scheduleResult);
        return ajaxResult;
    }

    /**
     * 删除垫胶排程结果
     */
    @ApiOperation("删除垫胶排程结果（id不为空）")
    @RequiresPermissions("nc:ncScheduleResult:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iNcScheduleResultService.remove(arr);
    }

    /**
     * 导出垫胶排程结果
     */
    @ApiOperation("导出垫胶排程结果")
    @RequiresPermissions("nc:ncScheduleResult:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, NcScheduleResult ncScheduleResult) throws Exception {
        //若是没传日期则默认查询当日排程
        if (ncScheduleResult.getScheduleDate() == null) {
            ncScheduleResult.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        ncScheduleResult.setYear(DateFormatUtils.format(ncScheduleResult.getScheduleDate(), "yyyy"));
        ncScheduleResult.setMonth(DateFormatUtils.format(ncScheduleResult.getScheduleDate(), "MM"));
        //获取字节流数据
        byte[] data = iNcScheduleResultService.export(ncScheduleResult);
        if (data == null) {
            return;
        }
        String fileName = I18nUtil.getMessage("ui.nc.ncScheduleResult.export.fileName");
        ExportLog exportLog = ExportUtil.uploadAndExportExcelByByte(response, data, fileName, ncScheduleResult.toString(), ApsConstant.PROCEDURE_CODE_NC);
        iExportLogService.add(exportLog);
    }


    /**
     * 自动排程
     */
    @ApiOperation("自动排程")
    @RequiresPermissions("nc:ncScheduleResult:autoPlan")
    @PostMapping("/autoPlan")
    @ResponseBody
    public AjaxResult autoPlan(NcScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        return iNcScheduleResultService.autoPlan(entity);
    }

    /**
     * 排程发布
     */
    @ApiOperation("排程发布")
    @RequiresPermissions("nc:ncScheduleResult:publish")
    @PostMapping("/publish")
    @ResponseBody
    public AjaxResult publish(NcScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            entity.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        return iNcScheduleResultService.publish(entity);
    }

    /**
     * 自动排程校验
     */
    @PostMapping("/validateAutoPlan")
    @ResponseBody
    public AjaxResult validateAutoPlan(NcScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        int releasingOrTimeoutByDate = iNcScheduleResultService.isReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        Boolean isUnique = iNcScheduleResultService.checkUnique(entity);
        String msg = "";
        if (isUnique) {
            //未生成，直接生成
            msg = "2";
        } else {
            //未投产
            msg = "1";
//            Boolean isPublish = iNcScheduleResultService.isPublish(entity);
//            if (isPublish) {
//                //已投产
//                msg = "3";
//            }
        }
        return AjaxResult.success(msg);
    }

    /**
     * 插单校验（含跨天日期计算）
     * <p>委派后端 {@code insertOrderValidate} 方法根据 scheduleShiftClass 计算实际排产日期后执行校验。</p>
     */
    @PostMapping("/validateAdd")
    @ResponseBody
    public AjaxResult validateAdd(NcScheduleResult entity) {
        return iNcScheduleResultService.validateAdd(entity);
    }

    /**
     * 查询当前排程日期是否已发布
     */
    @PostMapping("/isPublish")
    @ResponseBody
    public AjaxResult isPublish(NcScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        Boolean isPublish = iNcScheduleResultService.isPublish(entity);
        return isPublish ? AjaxResult.error() : AjaxResult.success();
    }

//    /**
//     * 均衡
//     */
//    @ApiOperation("均衡")
//    @RequiresPermissions("nc:ncScheduleResult:baladje")
//    @PostMapping("/baladje")
//    @ResponseBody
//    public AjaxResult baladje(NcScheduleResult entity) {
//        Date scheduleDate = entity.getScheduleDate();
//        if (scheduleDate == null) {
//            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
//        }
//        return iNcScheduleResultService.baladje(entity);
//    }

    /**
     * 同胶料归并生产
     */
    @ApiOperation("同胶料归并生产")
    @RequiresPermissions("nc:ncScheduleResult:mergeProduct")
    @PostMapping("/mergeProduct")
    @ResponseBody
    public AjaxResult mergeProduct(NcScheduleResult entity) {
        Date scheduleDate = entity.getScheduleDate();
        if (scheduleDate == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        return iNcScheduleResultService.mergeProduct(entity);
    }

    /**
     * 跳转至导入页面
     *
     * @param mop
     * @return
     */
    @GetMapping("/importData")
    public String importDate(ModelMap mop) {
        mop.put("prefix", prefix);
        mop.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        return "common/importData";
    }

    /**
     * 跳转至导入页面
     *
     * @param mop
     * @return
     */
    @GetMapping("/importData2")
    public String importDate2(ModelMap mop) {
        mop.put("prefix", prefix);
        mop.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        return "common/importData2";
    }

    /**
     * 下载模板
     *
     * @param response
     * @throws IOException
     */
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.ncScheduleResult.modalName");
        String lang = AuthorizationUtils.getLang();
        String tempName = (ApsBootConstant.EN_US.equals(lang) ? ApsBootConstant.NC_EN_TEMP : ApsBootConstant.NC_ZH_TEMP);
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelTemplateModel + "nc/" + tempName + ".xlsx");
        if (in == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.common.message.fileNotFound"));
        }
        ExcelUtil.setResponseHeader(response, fileName);
        FileUtils4UI.writeInputStream(in, response.getOutputStream());
        return AjaxResult.success();
    }

    /**
     * 数据导入
     *
     * @param file
     * @return
     * @throws Exception
     */
    @PostMapping("/importScheduleData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, Date scheduleDate) throws Exception {
        //日期校验
        if (scheduleDate.before(DateUtils.getNowDate("yyyy-MM-dd"))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.scheduleDateError"));
        }
        //若当天存在发布成功的记录则不予导入
        NcScheduleResult entity = new NcScheduleResult();
        entity.setScheduleDate(scheduleDate);
        int releasingOrTimeoutByDate = iNcScheduleResultService.isReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        Boolean isPublish = iNcScheduleResultService.isPublish(entity);
        if (isPublish) {
            return AjaxResult.error(I18nUtil.getMessage("ui.biz.alter.publishedNotImport"));
        }
        //文件解密
//        byte[] data = this.useFileEdjrypt ? FileEdjryptUtils.DecodeFile(file) : file.getBytes();
        byte[] data = file.getBytes();
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_NC,
                I18nUtil.getMessage("ui.data.column.ncScheduleResult.modalName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //解析文件
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<NcScheduleResult> util = new ExcelUtil<>(NcScheduleResult.class);
        List<NcScheduleResult> list = util.importExcel(in, 1);
        AjaxResult ajaxResult = iNcScheduleResultService.importData(list, importLog.getId(), DateUtils.dateTime(scheduleDate));
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
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
    @PostMapping("/importScheduleData2")
    @ResponseBody
    public AjaxResult importData2(MultipartFile file, Date scheduleDate) throws Exception {
        //日期校验
        if (scheduleDate.before(DateUtils.getNowDate("yyyy-MM-dd"))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.scheduleDateError"));
        }
        //若当天存在发布成功的记录则不予导入
        NcScheduleResult entity = new NcScheduleResult();
        entity.setScheduleDate(scheduleDate);
        int releasingOrTimeoutByDate = iNcScheduleResultService.isReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        Boolean isPublish = iNcScheduleResultService.isPublish(entity);
        if (isPublish) {
            return AjaxResult.error(I18nUtil.getMessage("ui.biz.alter.publishedNotImport"));
        }
        //文件解密
//        byte[] data = this.useFileEdjrypt ? FileEdjryptUtils.DecodeFile(file) : file.getBytes();
        byte[] data = file.getBytes();
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_NC,
                I18nUtil.getMessage("ui.data.column.ncScheduleResult.modalName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //解析文件
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<NcScheduleResult> util = new ExcelUtil<>(NcScheduleResult.class);
        List<NcScheduleResult> list = util.importExcel(in, 1);
        List<NcScheduleResult> newList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(list)) {
            for (NcScheduleResult nc : list) {
                NcScheduleResult result = new NcScheduleResult();
                BeanUtils.copyProperties(nc, result);
                newList.add(result);
            }
        }

        AjaxResult ajaxResult = iNcScheduleResultService.importData(newList, importLog.getId(), DateUtils.dateTime(scheduleDate));
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

//    /**
//     * 跳转至选机台页面
//     */
//    @GetMapping("/chooseMachine/{id}")
//    public String chooseMachine(@PathVariable("id") String idAndRowIndex, ModelMap mmap) {
//        String[] idAndRowIndexArr = idAndRowIndex.split(",");
//        NcScheduleResult scheduleResult = iNcScheduleResultService.getInfo(Long.valueOf(idAndRowIndexArr[0]));
//        DjMachineInfo machineInfo = new DjMachineInfo();
//        machineInfo.setStatus("0");
//        List<DjMachineInfo> machineInfoList = machineInfoService.exportList(machineInfo);
//        Map<String, DjMachineInfo> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(b -> b.getId() + "", a -> a));
//
//        if (StringUtils.isNotEmpty(scheduleResult.getMachineCode())) {
//            List<DjMachineInfo> newMachineInfoList = new ArrayList<>();
//            String[] machineIds = scheduleResult.getMachineCode().split(",");
//            for (String item : machineIds) {
//                if (machineCodeMap.get(item) != null) {
//                    newMachineInfoList.add(machineCodeMap.get(item));
//                }
//            }
//            mmap.put("machineInfoList", newMachineInfoList);
//        } else {
//            mmap.put("machineInfoList", machineInfoList);
//        }
//        mmap.put("scheduleResult", scheduleResult);
//        mmap.put("rowIndex", idAndRowIndexArr[1]);
//        return prefix + "/chooseMachine";
//    }

    /**
     * 选机台
     */
    @ApiOperation("选机台")
    @PostMapping("/chooseMachine")
    @ResponseBody
    public AjaxResult chooseMachine(NcScheduleResult entity) {
        return iNcScheduleResultService.chooseMachine(entity);
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
    public AjaxResult changeReleaseStatus(NcScheduleResult entity) {
        Date scheduleDate = entity.getScheduleDate();
        if (scheduleDate == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        return iNcScheduleResultService.changeReleaseStatus(entity);
    }

    /**
     * 跳转到归并中夜班页面
     */
    @GetMapping("/toCombinationMiddleAndNight")
    public String toCombinationMiddleAndNight(String ids, Date scheduleDate, ModelMap modelMap){
        modelMap.put("prefix", prefix);
        modelMap.put("ids", ids);
        modelMap.put("scheduleDate", scheduleDate);
        return "common/combinationMiddleAndNight";
    }

    /**
     * 归并中夜班计划量，合并到同一个班次
     *
     * @param ids             id
     * @param classifiedShift 合并班次
     */
    @ApiOperation("更改发布状态")
    @RequiresPermissions("nc:ncScheduleResult:combinationMiddleAndNight")
    @PostMapping("/combinationMiddleAndNight")
    @ResponseBody
    public AjaxResult combinationMiddleAndNight(String ids, String classifiedShift) {
        Long[] arr = Convert.toLongArray(ids);
        return iNcScheduleResultService.combinationMiddleAndNight(arr, classifiedShift);
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
        ExcelUtil<NcDayFinishQty> util = new ExcelUtil<>(NcDayFinishQty.class);
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
    @RequiresPermissions("nc:finishQty:import")
    @ApiOperation("完成量数据导入")
    @PostMapping("/importFinishQty")
    @ResponseBody
    public AjaxResult importFinishQty(MultipartFile file) throws Exception {
        //文件解密
//        byte[] data = this.useFileEdjrypt ? FileEdjryptUtils.DecodeFile(file) : file.getBytes();
        byte[] data = file.getBytes();

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data,
                ApsConstant.PROCEDURE_CODE_NC,
                I18nUtil.getMessage("ui.data.column.dayFinishQty.modelName"),
                file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<NcDayFinishQty> util = new ExcelUtil<>(NcDayFinishQty.class);
        List<NcDayFinishQty> list = util.importExcel(in);

        AjaxResult ajaxResult = iNcScheduleResultService.importFinishQty(list, importLog.getId());
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
    public AjaxResult getSummaryVo(NcScheduleResult scheduleResult) {
        return iNcScheduleResultService.getSummaryVo(scheduleResult);
    }

    /**
     * 获取连续6个班次的表头（以参数scheduleDate的上一天中班作为第一个班，格式：x班MM/dd）
     */
    @ApiOperation("获取连续6个班次的表头")
    @GetMapping("/getWorkClass")
    @ResponseBody
    public AjaxResult getWorkClass(String scheduleDate) {
        return iNcScheduleResultService.getWorkClass(scheduleDate);
    }

    /**
     * 获取垫胶下拉列表
     */
    @ApiOperation("获取垫胶下拉列表")
    @GetMapping("/getPaddingDistList")
    @ResponseBody
    public AjaxResult getPaddingDistList() {
        return iNcScheduleResultService.getPaddingDistList();
    }

    /**
     * 获取当前服务器时间对应的班次信息
     */
    @ApiOperation("获取当前班次信息")
    @GetMapping("/getCurrentShift")
    @ResponseBody
    public AjaxResult getCurrentShift() {
        return iNcScheduleResultService.getCurrentShift();
    }
}

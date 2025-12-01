package com.zlt.aps.controller.cd15;

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
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common4ui.utils.file.FileUtils4UI;
import com.zlt.aps.cd15.api.domain.entity.Cd15DayFinishQty;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult2;
import com.zlt.aps.cd15.api.service.ICd15MachineInfoService;
import com.zlt.aps.cd15.api.service.ICd15ScheduleResultService;
import com.zlt.aps.common.constant.ApsBootConstant;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import com.zlt.framework.utils.AuthorizationUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
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
 * 15度裁断排程结果Controller
 *
 * @author zlt
 * @date 2021-07-05
 */
@Api(tags = "15度裁断排程结果")
@Controller
@RequestMapping("/cd15/cd15ScheduleResult")
public class Cd15ScheduleResultController extends BaseController {

    @Autowired
    private ICd15ScheduleResultService iCd15ScheduleResultService;

    @Autowired
    private ICd15MachineInfoService iCd15MachineInfoService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @Autowired
    private IImportLogService iImportLogService;

    @Value("${excelTemplateModel}")
    private String excelTemplateModel;

    private String prefix = "cd15/cd15ScheduleResult";


    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cd15:cd15ScheduleResult:view")
    @GetMapping()
    public String operlog(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        return prefix + "/cd15ScheduleResult";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        mmap.put("minDate", DateUtils.parseDateToStr("yyyy-MM-dd", new Date()));  //当前日期
        mmap.put("cd15ScheduleResult", new Cd15ScheduleResult());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cd15ScheduleResult", iCd15ScheduleResultService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 跳转至转机台
     */
    @GetMapping("/changeMachine/{id}")
    public String changeMachine(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("editType", "1");
        mmap.put("cd15ScheduleResult", iCd15ScheduleResultService.getInfo(id));
        return prefix + "/changePlanOrMachine";
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
        Cd15ScheduleResult cd15ScheduleResult = new Cd15ScheduleResult();
        cd15ScheduleResult.setIds2(idList);
        mmap.put("selectList", iCd15ScheduleResultService.getInfos(cd15ScheduleResult));
        return prefix + "/changePlanOrMachine2";
    }

    /**
     * 跳转至调量页面
     */
    @GetMapping("/changePlan/{id}")
    public String changePlan(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("editType", "2");
        mmap.put("cd15ScheduleResult", iCd15ScheduleResultService.getInfo(id));
        return prefix + "/changePlanOrMachine";
    }

    /**
     * 跳转至自动排程日期页面
     */
    @GetMapping("/toAutoPlan")
    public String toAutoPlan(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        return prefix + "/autoPlan";
    }

    /**
     * 跳转至均衡页面
     */
    @GetMapping("/toBalance")
    public String toBalance(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        return prefix + "/balance";
    }

    /**
     * 跳转到选机台
     *
     * @return 结果
     */
    @GetMapping("/chooseMachine/{id}")
    public String chooseMachine(@PathVariable("id") String idAndRowIndex, ModelMap mmap) {
        String[] idAndRowIndexArr = idAndRowIndex.split(",");
        Cd15ScheduleResult scheduleResult = iCd15ScheduleResultService.getInfo(Long.valueOf(idAndRowIndexArr[0]));
        Cd15MachineInfo machineInfo = new Cd15MachineInfo();
        machineInfo.setStatus("0");
        List<Cd15MachineInfo> machineInfoList = iCd15MachineInfoService.exportList(machineInfo);
        Map<String, Cd15MachineInfo> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(item -> item.getId() + "", item -> item));

        if (StringUtils.isNotEmpty(scheduleResult.getMachineId())) {
            List<Cd15MachineInfo> newMachineInfoList = new ArrayList<>();
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
    public AjaxResult chooseMachine(Cd15ScheduleResult scheduleResult) {
        return iCd15ScheduleResultService.chooseMachine(scheduleResult);
    }

    /**
     * 根据条件查询15度裁断排程结果列表
     */
    @ApiOperation("根据条件查询15度裁断排程结果列表")
    @RequiresPermissions("cd15:cd15ScheduleResult:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd15ScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            entity.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        entity.setYear(DateFormatUtils.format(entity.getScheduleDate(), "yyyy"));
        entity.setMonth(DateFormatUtils.format(entity.getScheduleDate(), "MM"));
        return iCd15ScheduleResultService.list(entity);
    }

    /**
     * 修改或新增15度裁断排程结果
     */
    @ApiOperation("修改或新增15度裁断排程结果")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(Cd15ScheduleResult cd15ScheduleResult) {

        Cd15ScheduleResult query = new Cd15ScheduleResult();
        query.setId(cd15ScheduleResult.getId());
        query.setScheduleDate(cd15ScheduleResult.getScheduleDate());
        query.setMachineId(cd15ScheduleResult.getMachineId());
        query.setSteelStripCode1(cd15ScheduleResult.getSteelStripCode1());
        query.setSteelStripCode2(cd15ScheduleResult.getSteelStripCode2());
        List<Cd15ScheduleResult> list = iCd15ScheduleResultService.checkScheduleResultUnique(query);
        if (CollectionUtils.isNotEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }

        AjaxResult ajaxResult = null;
        if (cd15ScheduleResult.getId() != null) {
            //修改
            ajaxResult = iCd15ScheduleResultService.edit(cd15ScheduleResult);
        } else {
            //插单
            double dayPlanQty = cd15ScheduleResult.getDayPlanQty1() == null ? 0d : cd15ScheduleResult.getDayPlanQty1();
            double nightPlanQty = cd15ScheduleResult.getNightPlanQty1() == null ? 0d : cd15ScheduleResult.getNightPlanQty1();
            // 若插单量为0报错
            if ((dayPlanQty + nightPlanQty) == 0d) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.qty.zero"));
            }
            cd15ScheduleResult.setDataSource("1");
            ajaxResult = iCd15ScheduleResultService.add(cd15ScheduleResult);
        }
        return ajaxResult;
    }

    /**
     * 转机台
     */
    @ApiOperation("转机台")
    @PostMapping("/changeMachine")
    @ResponseBody
    public AjaxResult changeMachine(Cd15ScheduleResult cd15ScheduleResult) {

        Cd15ScheduleResult query = new Cd15ScheduleResult();
        query.setId(cd15ScheduleResult.getId());
        query.setScheduleDate(cd15ScheduleResult.getScheduleDate());
        query.setMachineId(cd15ScheduleResult.getMachineId());
        query.setSteelStripCode1(cd15ScheduleResult.getSteelStripCode1());
        query.setSteelStripCode2(cd15ScheduleResult.getSteelStripCode2());
        List<Cd15ScheduleResult> list = iCd15ScheduleResultService.checkScheduleResultUnique(query);
        if (CollectionUtils.isNotEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }
        AjaxResult ajaxResult = iCd15ScheduleResultService.changeMachine(cd15ScheduleResult);
        return ajaxResult;
    }

    /**
     * 转机台
     */
    @ApiOperation("转机台")
    @PostMapping("/batchChangeMachine/{machineId}")
    @ResponseBody
    public AjaxResult batchChangeMachine(@PathVariable("machineId") String machineId, String selects) {
        List<Cd15ScheduleResult> scheduleResultList = JSON.parseArray(selects, Cd15ScheduleResult.class);
        Cd15ScheduleResult query = new Cd15ScheduleResult();
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        for (Cd15ScheduleResult cd15ScheduleResult : scheduleResultList) {
            query.setId(cd15ScheduleResult.getId());
            query.setScheduleDate(cd15ScheduleResult.getScheduleDate());
            query.setMachineId(machineId);
            query.setSteelStripCode1(cd15ScheduleResult.getSteelStripCode1());
            query.setSteelStripCode2(cd15ScheduleResult.getSteelStripCode2());
            List<Cd15ScheduleResult> list = iCd15ScheduleResultService.checkScheduleResultUnique(query);
            if (CollectionUtils.isNotEmpty(list)) {
                if (sb1.length() > 0) {
                    sb1.append(",").append(query.getSteelStripCode1());
                } else {
                    sb1.append(query.getSteelStripCode1());
                }
                continue;
            }
            cd15ScheduleResult.setMachineId(machineId);
            AjaxResult result = iCd15ScheduleResultService.changeMachine(cd15ScheduleResult);
            if (result.get(GatewayConstants.MSG_TAG).equals(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"))) {
                if (sb2.length() > 0) {
                    sb2.append(",").append(query.getSteelStripCode1());
                } else {
                    sb2.append(query.getSteelStripCode1());
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
    @PostMapping("/changeQty")
    @ResponseBody
    public AjaxResult changeQty(Cd15ScheduleResult cd15ScheduleResult) {
        AjaxResult ajaxResult = iCd15ScheduleResultService.changeQty(cd15ScheduleResult);
        return ajaxResult;
    }

    /**
     * 删除15度裁断排程结果
     */
    @ApiOperation("删除15度裁断排程结果（id不为空）")
    @RequiresPermissions("cd15:cd15ScheduleResult:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        String newIds = "";
        String scheduleDate = "";
        if (StringUtils.isNotBlank(ids)) {
            newIds = ids.substring(0, ids.indexOf("|"));
            scheduleDate = ids.substring(ids.indexOf("|") + 1);
        }
//        Cd15ScheduleResult queryEntity = new Cd15ScheduleResult();
//        queryEntity.setScheduleDate(DateUtils.parseDate(scheduleDate));
//        if (iCd15ScheduleResultService.isPublish(queryEntity)) {
//            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.hasPublishedCanNotDelete"));
//        }
        Long[] arr = Convert.toLongArray(newIds);
        return iCd15ScheduleResultService.remove(arr);
    }

    /**
     * 导出15度裁断排程结果
     */
    @ApiOperation("导出15度裁断排程结果")
    @RequiresPermissions("cd15:cd15ScheduleResult:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, Cd15ScheduleResult cd15ScheduleResult) throws Exception {
        //若是没传日期则默认查询当日排程
        if (cd15ScheduleResult.getScheduleDate() == null) {
            cd15ScheduleResult.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        cd15ScheduleResult.setYear(DateFormatUtils.format(cd15ScheduleResult.getScheduleDate(), "yyyy"));
        cd15ScheduleResult.setMonth(DateFormatUtils.format(cd15ScheduleResult.getScheduleDate(), "MM"));
        //获取字节流数据
        byte[] data = iCd15ScheduleResultService.export(cd15ScheduleResult);
        if (data == null) {
            return;
        }
        String fileName = I18nUtil.getMessage("ui.cd15.cd15ScheduleResult.export.fileName");
        ExportLog exportLog = ExportUtil.uploadAndExportExcelByByte(response, data, fileName, cd15ScheduleResult.toString(), ApsConstant.PROCEDURE_CODE_CD15);
        iExportLogService.add(exportLog);
    }


    /**
     * 自动排程
     */
    @ApiOperation("自动排程")
    @RequiresPermissions("cd15:cd15ScheduleResult:autoPlan")
    @PostMapping("/autoPlan")
    @ResponseBody
    public AjaxResult autoPlan(Cd15ScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        return iCd15ScheduleResultService.autoPlan(entity);
    }

    /**
     * 排程发布
     */
    @ApiOperation("排程发布")
    @RequiresPermissions("cd15:cd15ScheduleResult:publish")
    @PostMapping("/publish")
    @ResponseBody
    public AjaxResult publish(Cd15ScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            entity.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        return iCd15ScheduleResultService.publish(entity);
    }

    /**
     * 自动排程校验
     */
    @PostMapping("/validateAutoPlan")
    @ResponseBody
    public AjaxResult validateAutoPlan(Cd15ScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        int releasingOrTimeoutByDate = iCd15ScheduleResultService.isReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        List<Cd15ScheduleResult> list = iCd15ScheduleResultService.checkScheduleResultUnique(entity);
        String msg = "";
        if (CollectionUtils.isEmpty(list)) {
            //未生成，直接生成
            msg = "2";
        } else {
            //未投产
            msg = "1";
//            Boolean isPublish = iCd15ScheduleResultService.isPublish(entity);
//            if (isPublish) {
//                //已投产
//                msg = "3";
//            }
        }
        return AjaxResult.success(msg);
    }

    /**
     * 插单校验
     */
    @PostMapping("/validateAdd")
    @ResponseBody
    public AjaxResult validateAdd(Cd15ScheduleResult entity) {
        int releasingOrTimeoutByDate = iCd15ScheduleResultService.isReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        List<Cd15ScheduleResult> list = iCd15ScheduleResultService.checkScheduleResultUnique(entity);
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.success("0");
        }
        return AjaxResult.success();
    }

    /**
     * 查询当前排程日期是否已发布
     */
    @PostMapping("/isPublish")
    @ResponseBody
    public AjaxResult isPublish(Cd15ScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        Boolean isPublish = iCd15ScheduleResultService.isPublish(entity);
        return isPublish ? AjaxResult.error() : AjaxResult.success();
    }

    /**
     * 均衡
     */
    @ApiOperation("均衡")
    @RequiresPermissions("cd15:cd15ScheduleResult:balance")
    @PostMapping("/balance")
    @ResponseBody
    public AjaxResult balance(Cd15ScheduleResult entity) {
        Date scheduleDate = entity.getScheduleDate();
        if (scheduleDate == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        return iCd15ScheduleResultService.balance(entity);
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
        String lang = AuthorizationUtils.getLang();  //国际化编码
        String tempName = (ApsBootConstant.EN_US.equals(lang) ? ApsBootConstant.CD15_EN_TEMP : ApsBootConstant.CD15_ZH_TEMP);  //根据国际化获取导入模板名称
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelTemplateModel + "cd15/" + tempName + ".xlsx");
        if (in == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.common.message.fileNotFound"));
        }
        String fileName = I18nUtil.getMessage("ui.cd15.cd15ScheduleResult.export.fileName");
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
        //若当天存在发布成功、发布中、超时失败的记录则不予导入
        Cd15ScheduleResult entity = new Cd15ScheduleResult();
        entity.setScheduleDate(scheduleDate);
        int releasingOrTimeoutByDate = iCd15ScheduleResultService.isReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        Boolean isPublish = iCd15ScheduleResultService.isPublish(entity);
        if (isPublish) {
            return AjaxResult.error(I18nUtil.getMessage("ui.biz.alter.publishedNotImport"));
        }
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CD15, I18nUtil.getMessage("ui.cd15.cd15ScheduleResult.export.fileName"),
                file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<Cd15ScheduleResult> util = new ExcelUtil<>(Cd15ScheduleResult.class);
        List<Cd15ScheduleResult> list = util.importExcel(in, 1);
        AjaxResult ajaxResult = iCd15ScheduleResultService.importData(list, importLog.getId(), DateUtils.dateTime(scheduleDate));
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
        //若当天存在发布成功、发布中、超时失败的记录则不予导入
        Cd15ScheduleResult entity = new Cd15ScheduleResult();
        entity.setScheduleDate(scheduleDate);
        int releasingOrTimeoutByDate = iCd15ScheduleResultService.isReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        Boolean isPublish = iCd15ScheduleResultService.isPublish(entity);
        if (isPublish) {
            return AjaxResult.error(I18nUtil.getMessage("ui.biz.alter.publishedNotImport"));
        }
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CD15, I18nUtil.getMessage("ui.cd15.cd15ScheduleResult.export.fileName"),
                file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<Cd15ScheduleResult2> util = new ExcelUtil<>(Cd15ScheduleResult2.class);
        List<Cd15ScheduleResult2> list = util.importExcel(in, 1);
        List<Cd15ScheduleResult> newList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(list)) {
            newList = list.stream().map(a -> {
                Cd15ScheduleResult result = new Cd15ScheduleResult();
                BeanUtils.copyProperties(a, result);
                return result;
            }).collect(Collectors.toList());
        }
        AjaxResult ajaxResult = iCd15ScheduleResultService.importData(newList, importLog.getId(), DateUtils.dateTime(scheduleDate));
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
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
    public AjaxResult changeReleaseStatus(Cd15ScheduleResult entity) {
        Date scheduleDate = entity.getScheduleDate();
        if (scheduleDate == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        return iCd15ScheduleResultService.changeReleaseStatus(entity);
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
    @RequiresPermissions("cd15:cd15ScheduleResult:combinationMiddleAndNight")
    @PostMapping("/combinationMiddleAndNight")
    @ResponseBody
    public AjaxResult combinationMiddleAndNight(String ids, String classifiedShift) {
        Long[] arr = Convert.toLongArray(ids);
        return iCd15ScheduleResultService.combinationMiddleAndNight(arr, classifiedShift);
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
        ExcelUtil<Cd15DayFinishQty> util = new ExcelUtil<>(Cd15DayFinishQty.class);
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
    @RequiresPermissions("cd15:finishQty:import")
    @ApiOperation("完成量数据导入")
    @PostMapping("/importFinishQty")
    @ResponseBody
    public AjaxResult importFinishQty(MultipartFile file) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data,
                ApsConstant.PROCEDURE_CODE_CD15,
                I18nUtil.getMessage("ui.data.column.dayFinishQty.modelName"),
                file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<Cd15DayFinishQty> util = new ExcelUtil<>(Cd15DayFinishQty.class);
        List<Cd15DayFinishQty> list = util.importExcel(in);

        AjaxResult ajaxResult = iCd15ScheduleResultService.importFinishQty(list, importLog.getId());
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
    public AjaxResult getSummaryVo(Cd15ScheduleResult scheduleResult) {
        return iCd15ScheduleResultService.getSummaryVo(scheduleResult);
    }
}

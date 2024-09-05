package com.zlt.aps.controller.cd90;

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
import com.ruoyi.common4ui.utils.StringUtils;
import com.ruoyi.common4ui.utils.file.FileUtils;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult2;
import com.zlt.aps.cd90.api.service.ICd90MachineInfoService;
import com.zlt.aps.cd90.api.service.ICd90ScheduleResultService;
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
 * 90度裁断排程结果Controller
 *
 * @author zlt
 * @date 2021-07-06
 */
@Api(tags = "90度裁断排程结果")
@Controller
@RequestMapping("/cd90/cd90ScheduleResult")
public class Cd90ScheduleResultController extends BaseController {

    @Autowired
    private ICd90ScheduleResultService iCd90ScheduleResultService;

    @Autowired
    private ICd90MachineInfoService iCd90MachineInfoService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @Autowired
    private IImportLogService iImportLogService;

    @Value("${excelTemplateModel}")
    private String excelTemplateModel;

    private String prefix = "cd90/cd90ScheduleResult";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cd90:cd90ScheduleResult:view")
    @GetMapping()
    public String operlog(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        return prefix + "/cd90ScheduleResult";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        mmap.put("minDate", DateUtils.parseDateToStr("yyyy-MM-dd", new Date()));  //当前日期
        mmap.put("cd90ScheduleResult", new Cd90ScheduleResult());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cd90ScheduleResult", iCd90ScheduleResultService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 跳转至转机台页面
     */
    @GetMapping("/changeMachine/{id}")
    public String changeMachine(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("editType", "1");
        mmap.put("cd90ScheduleResult", iCd90ScheduleResultService.getInfo(id));
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
        Cd90ScheduleResult cd90ScheduleResult = new Cd90ScheduleResult();
        cd90ScheduleResult.setIds2(idList);
        mmap.put("selectList", iCd90ScheduleResultService.getInfos(cd90ScheduleResult));
        return prefix + "/changePlanOrMachine2";
    }

    /**
     * 跳转至调量页面
     */
    @GetMapping("/changePlan/{id}")
    public String changePlan(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("editType", "2");
        mmap.put("cd90ScheduleResult", iCd90ScheduleResultService.getInfo(id));
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
        Cd90ScheduleResult scheduleResult = iCd90ScheduleResultService.getInfo(Long.valueOf(idAndRowIndexArr[0]));
        Cd90MachineInfo machineInfo = new Cd90MachineInfo();
        machineInfo.setStatus("0");
        List<Cd90MachineInfo> machineInfoList = iCd90MachineInfoService.exportList(machineInfo);
        Map<String, Cd90MachineInfo> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(item -> item.getId() + "", item -> item));

        if (StringUtils.isNotEmpty(scheduleResult.getMachineId())) {
            List<Cd90MachineInfo> newMachineInfoList = new ArrayList<>();
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
    public AjaxResult chooseMachine(Cd90ScheduleResult scheduleResult) {
        return iCd90ScheduleResultService.chooseMachine(scheduleResult);
    }

    /**
     * 根据条件查询90度裁断排程结果列表
     */
    @ApiOperation("根据条件查询90度裁断排程结果列表")
    @RequiresPermissions("cd90:cd90ScheduleResult:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd90ScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            entity.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        entity.setYear(DateFormatUtils.format(entity.getScheduleDate(), "yyyy"));
        entity.setMonth(DateFormatUtils.format(entity.getScheduleDate(), "MM"));
        return iCd90ScheduleResultService.list(entity);
    }

    /**
     * 修改或新增90度裁断排程结果
     */
    @ApiOperation("修改或新增90度裁断排程结果")
    @RequiresPermissions("cd90:cd90ScheduleResult:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(Cd90ScheduleResult cd90ScheduleResult) {

        Cd90ScheduleResult query = new Cd90ScheduleResult();
        query.setId(cd90ScheduleResult.getId());
        query.setScheduleDate(cd90ScheduleResult.getScheduleDate());
        query.setMachineId(cd90ScheduleResult.getMachineId());
        query.setClothCode(cd90ScheduleResult.getClothCode());
        List<Cd90ScheduleResult> list = iCd90ScheduleResultService.checkScheduleResultUnique(query);
        if (CollectionUtils.isNotEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }

        AjaxResult ajaxResult = null;
        if (cd90ScheduleResult.getId() != null) {
            ajaxResult = iCd90ScheduleResultService.edit(cd90ScheduleResult);
        } else {
            double dayPlanQty = cd90ScheduleResult.getDayPlanQty() == null ? 0d : cd90ScheduleResult.getDayPlanQty();
            double nightPlanQty = cd90ScheduleResult.getNightPlanQty() == null ? 0d : cd90ScheduleResult.getNightPlanQty();
            // 若插单量为0报错
            if ((dayPlanQty + nightPlanQty) == 0d) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.qty.zero"));
            }
            cd90ScheduleResult.setDataSource("1");
            ajaxResult = iCd90ScheduleResultService.add(cd90ScheduleResult);
        }
        return ajaxResult;
    }

    /**
     * 转机台
     */
    @ApiOperation("转机台")
    @PostMapping("/changeMachine")
    @ResponseBody
    public AjaxResult changeMachine(Cd90ScheduleResult scheduleResult) {

        Cd90ScheduleResult query = new Cd90ScheduleResult();
        query.setId(scheduleResult.getId());
        query.setScheduleDate(scheduleResult.getScheduleDate());
        query.setMachineId(scheduleResult.getMachineId());
        query.setClothCode(scheduleResult.getClothCode());
        List<Cd90ScheduleResult> list = iCd90ScheduleResultService.checkScheduleResultUnique(query);
        if (CollectionUtils.isNotEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }
        AjaxResult ajaxResult = iCd90ScheduleResultService.changeMachine(scheduleResult);
        return ajaxResult;
    }

    /**
     * 转机台
     */
    @ApiOperation("转机台")
    @PostMapping("/batchChangeMachine/{machineId}")
    @ResponseBody
    public AjaxResult batchChangeMachine(@PathVariable("machineId") String machineId, String selects) {
        List<Cd90ScheduleResult> scheduleResultList = JSON.parseArray(selects, Cd90ScheduleResult.class);
        Cd90ScheduleResult query = new Cd90ScheduleResult();
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        for (Cd90ScheduleResult scheduleResult : scheduleResultList) {
            query.setId(scheduleResult.getId());
            query.setScheduleDate(scheduleResult.getScheduleDate());
            query.setMachineId(machineId);
            query.setClothCode(scheduleResult.getClothCode());
            List<Cd90ScheduleResult> list = iCd90ScheduleResultService.checkScheduleResultUnique(query);
            if (CollectionUtils.isNotEmpty(list)) {
                if (sb1.length() > 0) {
                    sb1.append(",").append(query.getClothCode());
                } else {
                    sb1.append(query.getClothCode());
                }
                continue;
            }
            scheduleResult.setMachineId(machineId);
            AjaxResult result = iCd90ScheduleResultService.changeMachine(scheduleResult);
            if (result.get(GatewayConstants.MSG_TAG).equals(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"))) {
                if (sb2.length() > 0) {
                    sb2.append(",").append(query.getClothCode());
                } else {
                    sb2.append(query.getClothCode());
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
    public AjaxResult changeQty(Cd90ScheduleResult scheduleResult) {
        AjaxResult ajaxResult = iCd90ScheduleResultService.changeQty(scheduleResult);
        return ajaxResult;
    }

    /**
     * 删除90度裁断排程结果
     */
    @ApiOperation("删除90度裁断排程结果（id不为空）")
    @RequiresPermissions("cd90:cd90ScheduleResult:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        String newIds = "";
        String scheduleDate = "";
        if (StringUtils.isNotBlank(ids)){
            newIds = ids.substring(0,ids.indexOf("|"));
            scheduleDate = ids.substring(ids.indexOf("|")+1);
        }
//        Cd90ScheduleResult queryEntity = new Cd90ScheduleResult();
//        queryEntity.setScheduleDate(DateUtils.parseDate(scheduleDate));
//        if(iCd90ScheduleResultService.isPublish(queryEntity)){
//            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.hasPublishedCanNotDelete"));
//        }
        Long[] arr = Convert.toLongArray(newIds);
        return iCd90ScheduleResultService.remove(arr);
    }

    /**
     * 导出90度裁断排程结果
     */
    @ApiOperation("导出90度裁断排程结果")
    @RequiresPermissions("cd90:cd90ScheduleResult:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, Cd90ScheduleResult cd90ScheduleResult) throws Exception {
        //若是没传日期则默认查询当日排程
        if (cd90ScheduleResult.getScheduleDate() == null) {
            cd90ScheduleResult.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        cd90ScheduleResult.setYear(DateFormatUtils.format(cd90ScheduleResult.getScheduleDate(), "yyyy"));
        cd90ScheduleResult.setMonth(DateFormatUtils.format(cd90ScheduleResult.getScheduleDate(), "MM"));
        //获取字节流数据
        byte[] data = iCd90ScheduleResultService.export(cd90ScheduleResult);
        if (data == null) {
            return;
        }
        String fileName = I18nUtil.getMessage("ui.cd90.cd90ScheduleResult.export.fileName");
        ExportLog exportLog = ExportUtil.uploadAndExportExcelByByte(response, data, fileName, cd90ScheduleResult.toString(), ApsConstant.PROCEDURE_CODE_CD90);
        iExportLogService.add(exportLog);
    }


    /**
     * 自动排程
     */
    @ApiOperation("自动排程")
    @RequiresPermissions("cd90:cd90ScheduleResult:autoPlan")
    @PostMapping("/autoPlan")
    @ResponseBody
    public AjaxResult autoPlan(Cd90ScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        return iCd90ScheduleResultService.autoPlan(entity);
    }

    /**
     * 排程发布
     */
    @ApiOperation("排程发布")
    @RequiresPermissions("cd90:cd90ScheduleResult:publish")
    @PostMapping("/publish")
    @ResponseBody
    public AjaxResult publish(Cd90ScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            entity.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        return iCd90ScheduleResultService.publish(entity);
    }

    /**
     * 自动排程校验
     */
    @PostMapping("/validateAutoPlan")
    @ResponseBody
    public AjaxResult validateAutoPlan(Cd90ScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        int releasingOrTimeoutByDate = iCd90ScheduleResultService.isReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        List<Cd90ScheduleResult> list = iCd90ScheduleResultService.checkScheduleResultUnique(entity);
        String msg = "";
        if (CollectionUtils.isEmpty(list)) {
            //未生成，直接生成
            msg = "2";
        } else {
            //未投产
            msg = "1";
//            Boolean isPublish = iCd90ScheduleResultService.isPublish(entity);
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
    public AjaxResult validateAdd(Cd90ScheduleResult entity) {
        int releasingOrTimeoutByDate = iCd90ScheduleResultService.isReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        List<Cd90ScheduleResult> list = iCd90ScheduleResultService.checkScheduleResultUnique(entity);
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
    public AjaxResult isPublish(Cd90ScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        Boolean isPublish = iCd90ScheduleResultService.isPublish(entity);
        return isPublish ? AjaxResult.error() : AjaxResult.success();
    }

    /**
     * 均衡
     */
    @ApiOperation("均衡")
    @RequiresPermissions("cd90:cd90ScheduleResult:balance")
    @PostMapping("/balance")
    @ResponseBody
    public AjaxResult balance(Cd90ScheduleResult entity) {
        Date scheduleDate = entity.getScheduleDate();
        if (scheduleDate == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        return iCd90ScheduleResultService.balance(entity);
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
        String tempName = (ApsBootConstant.EN_US.equals(lang) ? ApsBootConstant.CD90_EN_TEMP : ApsBootConstant.CD90_ZH_TEMP);  //根据国际化获取导入模板名称
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelTemplateModel + "cd90/" + tempName + ".xlsx");
        if (in == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.common.message.fileNotFound"));
        }
        String fileName = I18nUtil.getMessage("ui.cd90.cd90ScheduleResult.export.fileName");
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
    @PostMapping("/importScheduleData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, Date scheduleDate) throws Exception {
        //日期校验
        if (scheduleDate.before(DateUtils.getNowDate("yyyy-MM-dd"))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.scheduleDateError"));
        }
        //若当天存在发布成功、发布中、超时失败的记录则不予导入
        Cd90ScheduleResult entity = new Cd90ScheduleResult();
        entity.setScheduleDate(scheduleDate);
        int releasingOrTimeoutByDate = iCd90ScheduleResultService.isReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        Boolean isPublish = iCd90ScheduleResultService.isPublish(entity);
        if (isPublish) {
            return AjaxResult.error(I18nUtil.getMessage("ui.biz.alter.publishedNotImport"));
        }
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CD90, I18nUtil.getMessage("ui.cd90.cd90ScheduleResult.export.fileName"),
                file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<Cd90ScheduleResult> util = new ExcelUtil<>(Cd90ScheduleResult.class);
        List<Cd90ScheduleResult> list = util.importExcel(in, 1);
        AjaxResult ajaxResult = iCd90ScheduleResultService.importData(list, importLog.getId(), DateUtils.dateTime(scheduleDate));
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
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
    @PostMapping("/importScheduleData2")
    @ResponseBody
    public AjaxResult importData2(MultipartFile file, Date scheduleDate) throws Exception {
        //日期校验
        if (scheduleDate.before(DateUtils.getNowDate("yyyy-MM-dd"))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.scheduleDateError"));
        }
        //若当天存在发布成功、发布中、超时失败的记录则不予导入
        Cd90ScheduleResult entity = new Cd90ScheduleResult();
        entity.setScheduleDate(scheduleDate);
        int releasingOrTimeoutByDate = iCd90ScheduleResultService.isReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        Boolean isPublish = iCd90ScheduleResultService.isPublish(entity);
        if (isPublish) {
            return AjaxResult.error(I18nUtil.getMessage("ui.biz.alter.publishedNotImport"));
        }
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CD90, I18nUtil.getMessage("ui.cd90.cd90ScheduleResult.export.fileName"),
                file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<Cd90ScheduleResult2> util = new ExcelUtil<>(Cd90ScheduleResult2.class);
        List<Cd90ScheduleResult2> list = util.importExcel(in, 1);
        List<Cd90ScheduleResult> newList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(list)) {
            newList = list.stream().map(a -> {
                Cd90ScheduleResult result = new Cd90ScheduleResult();
                BeanUtils.copyProperties(a, result);
                return result;
            }).collect(Collectors.toList());
        }
        AjaxResult ajaxResult = iCd90ScheduleResultService.importData(newList, importLog.getId(), DateUtils.dateTime(scheduleDate));
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
    public AjaxResult changeReleaseStatus(Cd90ScheduleResult entity) {
        Date scheduleDate = entity.getScheduleDate();
        if (scheduleDate == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        return iCd90ScheduleResultService.changeReleaseStatus(entity);
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
    @RequiresPermissions("cd90:cd90ScheduleResult:combinationMiddleAndNight")
    @PostMapping("/combinationMiddleAndNight")
    @ResponseBody
    public AjaxResult combinationMiddleAndNight(String ids, String classifiedShift) {
        Long[] arr = Convert.toLongArray(ids);
        return iCd90ScheduleResultService.combinationMiddleAndNight(arr, classifiedShift);
    }
}

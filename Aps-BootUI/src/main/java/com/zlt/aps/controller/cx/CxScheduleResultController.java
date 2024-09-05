package com.zlt.aps.controller.cx;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.SysDictData;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.utils.file.FileUtils;
import com.zlt.aps.common.constant.ApsBootConstant;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.cx.api.domain.dto.CxParamsDto;
import com.zlt.aps.cx.api.domain.dto.LhMachineInfoDto;
import com.zlt.aps.cx.api.domain.entity.*;
import com.zlt.aps.cx.api.service.*;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import com.zlt.framework.utils.AuthorizationUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.xml.crypto.Data;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 成型排程结果Controller
 *
 * @author zlt
 * @date 2021-07-12
 */
@Api(tags = "成型排程结果")
@Controller
@RequestMapping("/cx/cxScheduleResult")
public class CxScheduleResultController extends BaseController {

    @Autowired
    private ICxScheduleResultService iCxScheduleResultService;

    @Autowired
    private ICxPlanProductStatusService iCxPlanProductStatusService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private ICxProductConstructionInfoService iCxProductConstructionInfoService;

    @Autowired
    private ICxMachineInfoService iCxMachineInfoService;

    @Autowired
    private ICxParamsService iCxParamsService;

    @Autowired
    private ISysDictDataCacheService iSysDictDataCacheService;

    @Value("${excelTemplateModel}")
    private String excelTemplateModel;

    private String prefix = "cx/cxScheduleResult";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:cxScheduleResult:view")
    @GetMapping()
    public String operlog(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天

        CxParamsDto dto = new CxParamsDto();
        dto.setParamCode("MINIMUM_LH_MACHINE_COM_RATIO");
        List<CxParamsDto> list = iCxParamsService.exportData(dto);
        String minimumLhMachine = "";
        if (CollectionUtils.isNotEmpty(list)) {
            minimumLhMachine = list.get(0).getParamValue();
        }
        mmap.put("minimumLhMachine", minimumLhMachine);

        dto.setParamCode("MONTH_PLAN_OS");
        List<CxParamsDto> list2 = iCxParamsService.exportData(dto);
        String monthPlanSurplusTip = "";
        if (CollectionUtils.isNotEmpty(list2)) {
            monthPlanSurplusTip = list2.get(0).getParamValue();
        }
        mmap.put("monthPlanSurplusTip", monthPlanSurplusTip);

        return prefix + "/cxScheduleResult";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        mmap.put("minDate", DateUtils.parseDateToStr("yyyy-MM-dd", new Date()));  //当前日期
        mmap.put("cxScheduleResult", new CxScheduleResult());
        List<CxProductConstructionInfo> pcList = new ArrayList<CxProductConstructionInfo>();
        mmap.put("embryoVersions", pcList);
        return prefix + "/add";
    }

    /**
     * 获取胎胚版本列表
     */
    @ApiOperation("获取胎胚版本列表")
    @PostMapping("/getProductEmbryoVersions")
    @ResponseBody
    public AjaxResult getEmbryoVersions(CxProductConstructionInfo cxProductConstructionInfo) {
        List<CxProductConstructionInfo> pcList = iCxProductConstructionInfoService.getList(cxProductConstructionInfo);
        return AjaxResult.success(pcList);
    }

    /**
     * 获取可用成型机台列表
     */
    @ApiOperation("获取可用成型机台列表")
    @PostMapping("/getCxMachines")
    @ResponseBody
    public AjaxResult getCxMachines(CxProductConstructionInfo cxProductConstructionInfo) {
        String embryoCode = cxProductConstructionInfo.getEmbryoCode();
        if (StringUtils.isEmpty(embryoCode)){
            return AjaxResult.success(new ArrayList<>());
        }
        CxMachineInfo machineQry = new CxMachineInfo();
        machineQry.setStatus("0");
        machineQry.setMachineType(embryoCode.startsWith("Y") ? "1":"2");
        List<CxMachineInfo> machineList = iCxMachineInfoService.listOrderByName(machineQry);
        return AjaxResult.success(machineList);
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cxScheduleResult", iCxScheduleResultService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 跳转至甘特图
     */
    @GetMapping("/gantt/{flag}")
    public String gantt(ModelMap mmap,@PathVariable("flag") int flag) {
        if (flag == 1 ){
            mmap.put("scheduleDate",DateUtils.getDate());
            return prefix + "/machineGant";
        }else {
            mmap.put("scheduleDate", DateUtils.getDate());
            return prefix + "/specGant";
        }
    }


    /**
     * 获取甘特图数据
     */
    @PostMapping("/getGantData")
    @ResponseBody
    public AjaxResult getGantData(Gante gante) {
        int flag = gante.getFlag();
        Date scheduleDate = gante.getScheduleDate()==null?new Date():gante.getScheduleDate();
        gante.setScheduleDate(scheduleDate);
        if (flag == 2 ){ //规格
            gante.setStartDay(DateUtils.getFirstDayByDate(scheduleDate));
            gante.setEndDay(DateUtils.getLastDayByDate(scheduleDate));
        }
        List<Gante> cxGanteDataList = iCxScheduleResultService.getCxGanteData(gante);
        return AjaxResult.success(cxGanteDataList);
    }



    /**
     * 转机台页面
     */
    @GetMapping("/changePlanOrMachine/{id}")
    public String changeMachine(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("editType", "1");
        mmap.put("cxScheduleResult", iCxScheduleResultService.getInfo(id));
        return prefix + "/changePlanOrMachine";
    }

    /**
     * 修改施工版本
     */
    @GetMapping("/changeBomDataVersion/{id}")
    public String changeBomDataVersion(@PathVariable("id") Long id, ModelMap mmap) {

        CxScheduleResult csr = iCxScheduleResultService.getInfo(id);
        CxProductConstructionInfo pc = new CxProductConstructionInfo();
        pc.setDelFlag("0");
        pc.setEmbryoCode(csr.getEmbryoCode());
        List<CxProductConstructionInfo> pcList = iCxProductConstructionInfoService.getList(pc);
        if (CollectionUtils.isEmpty(pcList)) {
            pcList = new ArrayList<CxProductConstructionInfo>();
        }
        mmap.put("cxScheduleResult", csr);
        mmap.put("embryoVersions", pcList);
        return prefix + "/changeBomDataVersion";
    }

    /**
     * 修改施工版本
     */
    @ApiOperation("修改施工版本")
    @RequiresPermissions("cx:cxScheduleResult:edit")
    @PostMapping("/changeBomDataVersion")
    @ResponseBody
    public AjaxResult changeBomDataVersion(CxScheduleResult cxScheduleResult) {
        int releasingOrTimeoutByIds = iCxScheduleResultService.isReleasingOrTimeoutByIds(new Long[]{cxScheduleResult.getId()});
        if (releasingOrTimeoutByIds > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        return iCxScheduleResultService.changeBomDataVersion(cxScheduleResult);
    }

    /**
     * 跳转至投产状态页面
     */
    @GetMapping("/productStatus")
    public String productStatus(ModelMap mmap) {
        mmap.put("beginDate", DateUtils.getNowFirstDay());
        mmap.put("endDate", DateUtils.getNowLastDay());
        return "cx/productStatus/productStatus";
    }

    /**
     * 跳转至收尾列表页面
     */
    @GetMapping("/finishedList")
    public String finishedList(ModelMap mmap) {
        mmap.put("startTime", DateUtils.getNowFirstDay());
        mmap.put("endTime", DateUtils.getDate());
        return prefix + "/finishedList";
    }

    /**
     * 收尾列表->投产编辑页面
     */
    @GetMapping("/production/{id}")
    public String production(@PathVariable("id") Long id, ModelMap mmap) {
        CxScheduleResult cxScheduleResult = iCxScheduleResultService.getInfo(id);
        CxPlanProductStatus cxPlanProductStatus = new CxPlanProductStatus();
        cxPlanProductStatus.setScheduleDate(cxScheduleResult.getScheduleDate());
        cxPlanProductStatus.setSapCode(cxScheduleResult.getSapCode());
        cxPlanProductStatus.setEmbryoCode(cxScheduleResult.getEmbryoCode());
        cxPlanProductStatus.setCxMachineCode(cxScheduleResult.getCxMachineCode());
        cxPlanProductStatus.setStorageLocation(cxScheduleResult.getStorageLocation());
        mmap.put("cxPlanProductStatus", cxPlanProductStatus);
        return "cx/productStatus/finishedProduction";
    }

    /**
     * 修改硫化机台数页面
     */
    @GetMapping("/modifyLhMachineQty/{id}")
    public String modifyLhMachineQty(@PathVariable("id") Long id, ModelMap mmap) {
        CxScheduleResult cxScheduleResult = iCxScheduleResultService.getInfo(id);
        mmap.put("cxScheduleResult", cxScheduleResult);
        return prefix + "/modifyLhMachineQty";
    }

    /**
     * 修改成型、硫化状态页面
     */
    @GetMapping("/modifyStatus/{id}")
    public String modifyStatus(@PathVariable("id") Long id, ModelMap mmap) {
        CxScheduleResult cxScheduleResult = iCxScheduleResultService.getInfo(id);
        mmap.put("cxScheduleResult", cxScheduleResult);
        return prefix + "/modifyStatus";
    }

    /**
     * 投产编辑页面校验
     */
    @PostMapping("/hasRecordValidate")
    @ResponseBody
    public AjaxResult hasRecordValidate(CxScheduleResult cxScheduleResult) {
        int releasingOrTimeoutByIds = iCxScheduleResultService.isReleasingOrTimeoutByIds(new Long[]{cxScheduleResult.getId()});
        if (releasingOrTimeoutByIds > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        CxPlanProductStatus cxPlanProductStatus = new CxPlanProductStatus();
        cxPlanProductStatus.setEmbryoCode(cxScheduleResult.getEmbryoCode());
        cxPlanProductStatus.setSapCode(cxScheduleResult.getSapCode());
        cxPlanProductStatus.setMonthPlanApsVersion(cxScheduleResult.getCxBatchNo());
        cxPlanProductStatus.setBomDataVersion(cxScheduleResult.getBomDataVersion());
        CxPlanProductStatus cxPlanProductStatus2 = iCxPlanProductStatusService.getInfo2(cxPlanProductStatus);
        if (cxPlanProductStatus2 == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.hasRecordValidate"));
        }
        return AjaxResult.success();
    }

    /**
     * 跳转至投产编辑页面
     */
    @GetMapping("/modifyQty/{params}")
    public String modifyQty(@PathVariable("params") String params, ModelMap mmap) {
        String[] aa = params.split(",");
        CxPlanProductStatus cxPlanProductStatus = new CxPlanProductStatus();
        cxPlanProductStatus.setSapCode(aa[1]);
        cxPlanProductStatus.setEmbryoCode(aa[0]);
        cxPlanProductStatus.setMonthPlanApsVersion(aa[2]);
        cxPlanProductStatus.setBomDataVersion(aa[3]);
        CxPlanProductStatus cxPlanProductStatus2 = iCxPlanProductStatusService.getInfo2(cxPlanProductStatus);
        //调整来源：0:投产列表，1：成型排程
        cxPlanProductStatus2.setAdjustSource("1");
        mmap.put("editType", "2");
        mmap.put("cxPlanProductStatus", cxPlanProductStatus2);
        return "cx/productStatus/edit";
    }

    /**
     * 如果浮点是个整数，那么去掉浮点后面的0（例如一个浮点为12.0，那么页面展示需要把。0去掉，直接展示12）
     *
     * @param value
     * @return
     */
    public String stripZeros(Object value) {
        if (value == null) {
            return "0";
        }
        return new BigDecimal(String.valueOf(value)).stripTrailingZeros().toPlainString();
    }

    /**
     * 跳转至调量页面
     */
    @GetMapping("/changePlan/{id}")
    public String changePlan(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("editType", "2");
        CxScheduleResult cxScheduleResult = iCxScheduleResultService.getInfoForQty(id);
        String osClass1Plan = I18nUtil.getMessage("ui.data.column.scheduleResult.osClass1Plan");
        String osClass2Plan = I18nUtil.getMessage("ui.data.column.scheduleResult.osClass2Plan");
        String osClass3Plan = I18nUtil.getMessage("ui.data.column.scheduleResult.osClass3Plan");
        String osClass4Plan = I18nUtil.getMessage("ui.data.column.scheduleResult.osClass4Plan");
        if (CollectionUtils.isNotEmpty(cxScheduleResult.getCd15ScheduleList())) {
            List<CxScheduleSub> cxScheduleSubList = cxScheduleResult.getCd15ScheduleList();
            for (CxScheduleSub cxScheduleSub : cxScheduleSubList) {
                String steelStripCode = I18nUtil.getMessage("ui.common.column.gy.steelStripCode") + "：" + cxScheduleSub.getCd15SteelStripCode1();
                String getCd15DayPlanQty1 = stripZeros(cxScheduleSub.getCd15DayPlanQty1());
                String getCd15NightPlanQty1 = stripZeros(cxScheduleSub.getCd15NightPlanQty1());
                String cd15Lable = steelStripCode + "，" + osClass1Plan + "：" + getCd15DayPlanQty1 + "，" + osClass2Plan + "：" + getCd15NightPlanQty1 + "；";
                cxScheduleSub.setOsLable(cd15Lable);
            }
        }
        if (CollectionUtils.isNotEmpty(cxScheduleResult.getCd90ScheduleList())) {
            List<CxScheduleSub> cxScheduleSubList = cxScheduleResult.getCd90ScheduleList();
            for (CxScheduleSub cxScheduleSub : cxScheduleSubList) {
                String clothCode = I18nUtil.getMessage("ui.common.column.lb.clothCode") + "：" + cxScheduleSub.getCd90ClothCode();
                String getCd90DayPlanQty1 = stripZeros(cxScheduleSub.getCd90DayPlanQty());
                String getCd90NightPlanQty1 = stripZeros(cxScheduleSub.getCd90NightPlanQty());
                String cd90Lable1 = clothCode + "，" + osClass1Plan + "：" + getCd90DayPlanQty1 + "，" + osClass2Plan + "：" + getCd90NightPlanQty1 + "；";
                cxScheduleSub.setOsLable(cd90Lable1);
            }
        }
        if (CollectionUtils.isNotEmpty(cxScheduleResult.getGdyyScheduleList())) {
            List<CxScheduleSub> cxScheduleSubList = cxScheduleResult.getGdyyScheduleList();
            for (CxScheduleSub cxScheduleSub : cxScheduleSubList) {
                String bigRollCode = I18nUtil.getMessage("ui.steelRollColor.column.bigRollCode") + "：" + cxScheduleSub.getGdyyBigRollCode();
                String getGdyyClass1Plan = stripZeros(cxScheduleSub.getGdyyClass1Plan());
                String getGdyyClass2Plan = stripZeros(cxScheduleSub.getGdyyClass2Plan());
                String getGdyyClass3Plan = stripZeros(cxScheduleSub.getGdyyClass3Plan());
                String gdyyLable = bigRollCode + "，" + osClass1Plan + "：" + getGdyyClass1Plan + "，" + osClass2Plan + "：" + getGdyyClass2Plan + "，" + osClass3Plan + "：" + getGdyyClass3Plan + "；";
                cxScheduleSub.setOsLable(gdyyLable);
            }
        }
        if (CollectionUtils.isNotEmpty(cxScheduleResult.getGsqScheduleList())) {
            List<CxScheduleSub> cxScheduleSubList = cxScheduleResult.getGsqScheduleList();
            for (CxScheduleSub cxScheduleSub : cxScheduleSubList) {
                String steelRingCode = I18nUtil.getMessage("ui.data.column.gsq.scheduleResult.steelRingCode") + "：" + cxScheduleSub.getGsqSteelRingCode();
                String getGsqMidPlanQty = stripZeros(cxScheduleSub.getGsqMidPlanQty());
                String getGsqNightPlanQty = stripZeros(cxScheduleSub.getGsqNightPlanQty());
                String getGsqDayPlanQty = stripZeros(cxScheduleSub.getGsqDayPlanQty());
                String gsqLable = steelRingCode + "，" + osClass1Plan + "：" + getGsqMidPlanQty + "，" + osClass2Plan + "：" + getGsqNightPlanQty + "，" + osClass3Plan + "：" + getGsqDayPlanQty + "；";
                cxScheduleSub.setOsLable(gsqLable);
            }
        }
        if (CollectionUtils.isNotEmpty(cxScheduleResult.getNcScheduleList())) {
            List<CxScheduleSub> cxScheduleSubList = cxScheduleResult.getNcScheduleList();
            for (CxScheduleSub cxScheduleSub : cxScheduleSubList) {
                String liningCode = I18nUtil.getMessage("ui.data.column.quota.liningCode") + "：" + cxScheduleSub.getNcLiningCode();
                String getNcDayPlanQty = stripZeros(cxScheduleSub.getNcDayPlanQty());
                String getNcNightPlanQty = stripZeros(cxScheduleSub.getNcNightPlanQty());
                String ncLable = liningCode + "，" + osClass1Plan + "：" + getNcDayPlanQty + "，" + osClass2Plan + "：" + getNcNightPlanQty + "；";
                cxScheduleSub.setOsLable(ncLable);
            }
        }
        if (CollectionUtils.isNotEmpty(cxScheduleResult.getTcScheduleList())) {
            List<CxScheduleSub> cxScheduleSubList = cxScheduleResult.getTcScheduleList();
            for (CxScheduleSub cxScheduleSub : cxScheduleSubList) {
                String sidewallCode = I18nUtil.getMessage("ui.data.column.quota.sidewallCode") + "：" + cxScheduleSub.getTcSidewallCode();
                String getTcDayPlanQty = stripZeros(cxScheduleSub.getTcDayPlanQty());
                String getTcNightPlanQty = stripZeros(cxScheduleSub.getTcNightPlanQty());
                String tcLable = sidewallCode + "，" + osClass1Plan + "：" + getTcDayPlanQty + "，" + osClass2Plan + "：" + getTcNightPlanQty + "；";
                cxScheduleSub.setOsLable(tcLable);
            }
        }
        if (CollectionUtils.isNotEmpty(cxScheduleResult.getTmScheduleList())) {
            List<CxScheduleSub> cxScheduleSubList = cxScheduleResult.getTmScheduleList();
            for (CxScheduleSub cxScheduleSub : cxScheduleSubList) {
                String treadCode = I18nUtil.getMessage("ui.tm.specifyMachine.column.treadCode") + "：" + cxScheduleSub.getTmTreadCode();
                String getTmDayPlanQty = stripZeros(cxScheduleSub.getTmDayPlanQty());
                String getTmNightPlanQty = stripZeros(cxScheduleSub.getTmNightPlanQty());
                String tmLable = treadCode + "，" + osClass1Plan + "：" + getTmDayPlanQty + "，" + osClass2Plan + "：" + getTmNightPlanQty + "；";
                cxScheduleSub.setOsLable(tmLable);
            }
        }
        if (CollectionUtils.isNotEmpty(cxScheduleResult.getTqScheduleList())) {
            List<CxScheduleSub> cxScheduleSubList = cxScheduleResult.getTqScheduleList();
            for (CxScheduleSub cxScheduleSub : cxScheduleSubList) {
                String beadCode = I18nUtil.getMessage("ui.tq.specifyMachine.column.beadCode") + "：" + cxScheduleSub.getTqBeadCode();
                String getTqMidPlanQty = stripZeros(cxScheduleSub.getTqMidPlanQty());
                String getTqNightPlanQty = stripZeros(cxScheduleSub.getTqNightPlanQty());
                String getTqDayPlanQty = stripZeros(cxScheduleSub.getTqDayPlanQty());
                String getTqNextMidPlanQty = stripZeros(cxScheduleSub.getTqNextMidPlanQty());
                String tqLable = beadCode + "，" + osClass1Plan + "：" + getTqMidPlanQty + "，" + osClass2Plan + "：" + getTqNightPlanQty + "，" + osClass3Plan + "：" + getTqDayPlanQty + "，" + osClass4Plan + ": " + getTqNextMidPlanQty + "；";
                cxScheduleSub.setOsLable(tqLable);
            }
        }
        if (CollectionUtils.isNotEmpty(cxScheduleResult.getXwyyScheduleList())) {
            List<CxScheduleSub> cxScheduleSubList = cxScheduleResult.getXwyyScheduleList();
            for (CxScheduleSub cxScheduleSub : cxScheduleSubList) {
                String getXwyyBigRollCode = I18nUtil.getMessage("ui.bigRollColor.column.bigRollCode") + "：" + cxScheduleSub.getXwyyBigRollCode();
                String getXwyyDayPlanQty = stripZeros(cxScheduleSub.getXwyyDayPlanQty());
                String getXwyyNightPlanQty = stripZeros(cxScheduleSub.getXwyyNightPlanQty());
                String xwyyLable = getXwyyBigRollCode + "，" + osClass1Plan + "：" + getXwyyDayPlanQty + "，" + osClass2Plan + "：" + getXwyyNightPlanQty + "；";
                cxScheduleSub.setOsLable(xwyyLable);
            }
        }
        mmap.put("cxScheduleResult", cxScheduleResult);
        return prefix + "/changePlanOrMachine";
    }

    /**
     * 成型自动排程
     */
    @GetMapping("/toAutoPlan")
    public String toAutoPlan(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        mmap.put("editType", "1");
        return prefix + "/autoPlan";
    }

    /**
     * 硫化自动排程
     */
    @GetMapping("/toLhAutoPlan")
    public String lhAutoPlan(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        mmap.put("editType", "2");
        return prefix + "/autoPlan";
    }

    /**
     * 生成模具变动单
     */
    @GetMapping("/toModelChange")
    public String modelChange(ModelMap mmap) {
        mmap.put("editType", "3");
        return prefix + "/autoPlan";
    }

    /**
     * 生成模具调整计划
     */
    @GetMapping("/toModelAdjustPlan")
    public String toModelAdjustPlan(ModelMap mmap) {
        mmap.put("editType", "4");
        return prefix + "/autoPlan";
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
     * 根据条件查询成型排程结果列表
     */
    @ApiOperation("根据条件查询成型排程结果列表")
    @RequiresPermissions("cx:cxScheduleResult:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            entity.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        return   iCxScheduleResultService.list(entity);
    }

    /**
     * 修改或新增成型排程结果
     */
    @ApiOperation("修改或新增成型排程结果")
    @RequiresPermissions("cx:cxScheduleResult:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(CxScheduleResult cxScheduleResult) {
        AjaxResult ajaxResult = null;
        if (cxScheduleResult.getId() != null) {
            int releasingOrTimeoutByIds = iCxScheduleResultService.isReleasingOrTimeoutByIds(new Long[]{cxScheduleResult.getId()});
            if (releasingOrTimeoutByIds > 0) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
            }
            if ("tl".equals(cxScheduleResult.getLocal())) {
                //调量，需要更新9个半部件的计划量
                ajaxResult = iCxScheduleResultService.changeQty(cxScheduleResult);
            } else if ("zjt".equals(cxScheduleResult.getLocal())) {
                //转机台
                ajaxResult = iCxScheduleResultService.changeMachine(cxScheduleResult);
            } else if ("xg".equals(cxScheduleResult.getLocal())) {
                //修改
                ajaxResult = iCxScheduleResultService.edit(cxScheduleResult);
            }
        } else {
            int releasingOrTimeoutByDate = iCxScheduleResultService.isReleasingOrTimeoutByDate(cxScheduleResult);
            if (releasingOrTimeoutByDate > 0) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
            }
            //成型插单
            double class1PlanQty = cxScheduleResult.getClass1PlanQty() == null ? 0d : cxScheduleResult.getClass1PlanQty();
            double class2PlanQty = cxScheduleResult.getClass2PlanQty() == null ? 0d : cxScheduleResult.getClass2PlanQty();
            double class3PlanQty = cxScheduleResult.getClass3PlanQty() == null ? 0d : cxScheduleResult.getClass3PlanQty();
            double class4PlanQty = cxScheduleResult.getClass4PlanQty() == null ? 0d : cxScheduleResult.getClass4PlanQty();
            double class5PlanQty = cxScheduleResult.getClass5PlanQty() == null ? 0d : cxScheduleResult.getClass5PlanQty();
            // 若插单量为0报错
            if ((class1PlanQty + class2PlanQty + class3PlanQty + class4PlanQty + class5PlanQty) == 0d) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.qty.zero"));
            }
            cxScheduleResult.setDataSource("1");
            ajaxResult = iCxScheduleResultService.add(cxScheduleResult);
        }
        return ajaxResult;
    }

    /**
     * 修改状态
     */
    @PostMapping("/modifyStatus")
    @ResponseBody
    public AjaxResult modifyStatus(CxScheduleResult cxScheduleResult) {
        int releasingOrTimeoutByIds = iCxScheduleResultService.isReleasingOrTimeoutByIds(new Long[]{cxScheduleResult.getId()});
        if (releasingOrTimeoutByIds > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        AjaxResult ajaxResult = iCxScheduleResultService.modifyStatus(cxScheduleResult);
        return ajaxResult;
    }

    /**
     * 转机台校验
     */
    @PostMapping("/validateChangeMachine")
    @ResponseBody
    public AjaxResult validateChangeMachine(CxScheduleResult entity) {
        int releasingOrTimeoutByIds = iCxScheduleResultService.isReleasingOrTimeoutByIds(new Long[]{entity.getId()});
        if (releasingOrTimeoutByIds > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        return iCxScheduleResultService.validateChangeMachine(entity);
    }

    /**
     * 调量校验
     */
    @PostMapping("/validateChangeQty")
    @ResponseBody
    public AjaxResult validateChangeQty(CxScheduleResult entity) {
        return iCxScheduleResultService.validateChangeQty(entity);
    }

    /**
     * 成型插单功能校验0
     */
    @PostMapping("/validateBeforeAdd")
    @ResponseBody
    public AjaxResult validateBeforeAdd(CxScheduleResult entity) {
        int releasingOrTimeoutByDate = iCxScheduleResultService.isReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        //唯一性校验
        List<CxScheduleResult> list1 = iCxScheduleResultService.checkScheduleResultUnique(entity);
        if (CollectionUtils.isNotEmpty(list1)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cxScheduleResult.uniqueValidate"));
        }

        CxScheduleResult entity2 = new CxScheduleResult();
        entity2.setScheduleDate(entity.getScheduleDate());
        List<CxScheduleResult> list = iCxScheduleResultService.getList(entity2);
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.success("0");
        }
        return AjaxResult.success();
    }

    /**
     * 成型插单算法校验
     */
    @PostMapping("/validateAdd")
    @ResponseBody
    public AjaxResult validateAdd(CxScheduleResult entity) {
        return iCxScheduleResultService.validateAdd(entity);
    }

    /**
     * 计算半部件调量参考值
     */
    @ApiOperation("计算半部件调量参考值")
    @PostMapping("/qtyReference")
    @ResponseBody
    public AjaxResult qtyReference(CxScheduleResult cxScheduleResult) {
        AjaxResult ajaxResult = iCxScheduleResultService.qtyReference(cxScheduleResult);
        return ajaxResult;
    }

    /**
     * 删除成型排程结果
     */
    @ApiOperation("删除成型排程结果（id不为空）")
    @RequiresPermissions("cx:cxScheduleResult:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
//        int releasingOrTimeoutByIds = iCxScheduleResultService.isReleasingOrTimeoutByIds(arr);
//        if (releasingOrTimeoutByIds > 0) {
//            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
//        }
        return iCxScheduleResultService.remove(arr);
    }

    /**
     * 导出成型排程结果
     */
    @ApiOperation("导出成型排程结果")
    @RequiresPermissions("cx:cxScheduleResult:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, CxScheduleResult cxScheduleResult) throws Exception {
        //若是没传日期则默认查询当日排程
        if (cxScheduleResult.getScheduleDate() == null) {
            cxScheduleResult.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        //获取字节流数据
        byte[] data = iCxScheduleResultService.export(cxScheduleResult);
        if (data == null) {
            return;
        }
        String fileName = I18nUtil.getMessage("ui.cx.cxScheduleResult.export.fileName");
        ExportLog exportLog = ExportUtil.uploadAndExportExcelByByte(response, data, fileName, cxScheduleResult.toString(), ApsConstant.PROCEDURE_CODE_CX);
        iExportLogService.add(exportLog);

    }

    /**
     * 导出收尾列表
     */
    @ApiOperation("导出收尾列表")
    @RequiresPermissions("cx:cxScheduleResult:export")
    @GetMapping("/exportFinishedList")
    @ResponseBody
    public void exportFinishedList(HttpServletResponse response, CxScheduleResult cxScheduleResult) throws Exception {
        cxScheduleResult.setProductionStatus("2");
        //获取字节流数据
        byte[] data = iCxScheduleResultService.export(cxScheduleResult);
        if (data == null) {
            return;
        }
        String fileName = I18nUtil.getMessage("ui.cx.cxScheduleResult.export.fileName4FinishedList");
        ExportLog exportLog = ExportUtil.uploadAndExportExcelByByte(response, data, fileName, cxScheduleResult.toString(), ApsConstant.PROCEDURE_CODE_CX);
        iExportLogService.add(exportLog);

    }

    /**
     * 排程发布校验
     */
    @ApiOperation("排程发布校验")
    @PostMapping("/publishValidate")
    @ResponseBody
    public AjaxResult publishValidate(CxScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            entity.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        return iCxScheduleResultService.publishValidate(entity);
    }

    /**
     * 排程发布
     */
    @ApiOperation("排程发布")
    @RequiresPermissions("cx:cxScheduleResult:publish")
    @PostMapping("/publish")
    @ResponseBody
    public AjaxResult publish(CxScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            entity.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        return iCxScheduleResultService.publish(entity);
    }

    /**
     * 成型自动排程
     */
    @ApiOperation("成型自动排程")
    @RequiresPermissions("cx:cxScheduleResult:autoPlan")
    @PostMapping("/autoPlan")
    @ResponseBody
    public AjaxResult autoPlan(CxScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        return iCxScheduleResultService.autoPlan(entity);
    }

    /**
     * 硫化自动排程
     */
    @ApiOperation("硫化自动排程")
    @RequiresPermissions("cx:cxScheduleResult:lhAutoPlan")
    @PostMapping("/lhAutoPlan")
    @ResponseBody
    public AjaxResult lhAutoPlan(CxScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        return iCxScheduleResultService.lhAutoPlan(entity);
    }

    /**
     * 生成模具变动单校验
     */
    @ApiOperation("生成模具变动单校验")
    @PostMapping("/modelChangeValidate")
    @ResponseBody
    public AjaxResult modelChangeValidate(CxScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            entity.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        return iCxScheduleResultService.modelChangeValidate(entity);
    }

    /**
     * 生成模具变动单
     */
    @ApiOperation("生成模具变动单")
    @RequiresPermissions("cx:cxScheduleResult:modelChange")
    @PostMapping("/modelChange")
    @ResponseBody
    public AjaxResult modelChange(CxScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            entity.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        return iCxScheduleResultService.modelChange(entity);
    }

    /**
     * 生成模具调整计划
     */
    @ApiOperation("生成模具调整计划")
    @RequiresPermissions("cx:cxScheduleResult:modelAdjustPlan")
    @PostMapping("/modelAdjustPlan")
    @ResponseBody
    public AjaxResult modelAdjustPlan(CxScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            entity.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        return iCxScheduleResultService.modelAdjustPlan(entity);
    }

    /**
     * 成型自动排程校验
     */
    @PostMapping("/validateAutoPlan")
    @ResponseBody
    public AjaxResult validateAutoPlan(CxScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        int releasingOrTimeoutByDate = iCxScheduleResultService.isReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        // 增补计划校验
        AjaxResult ajaxResult = iCxScheduleResultService.autoScheduleValidateSupplePlanByScheduleDate(entity);
        if (ajaxResult.get(AjaxResult.CODE_TAG).equals(HttpStatus.ERROR)) {
            return ajaxResult;
        }
        //单机自动排程校验
        if (StringUtils.isNotBlank(entity.getCxMachineCode())) {
            List<CxScheduleResult> partList = iCxScheduleResultService.singleMachinAutoPlanValidate(entity);
            if (CollectionUtils.isNotEmpty(partList)) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.singleMachinAutoPlanValidate"));
            }
        }

        List<CxScheduleResult> list = iCxScheduleResultService.getList(entity);
        String msg = "";
        if (CollectionUtils.isEmpty(list)) {
            //未生成，直接生成
            msg = "2";
        } else {
            //未投产
            msg = "1";
            Boolean isPublish = iCxScheduleResultService.isCxPublish(entity);
            if (isPublish) {
                //已投产
                msg = "3";
            }
        }
        return AjaxResult.success(msg);
    }

    /**
     * 硫化自动排程校验(此处用成型实体来接受硫化排程结果)
     */
    @PostMapping("/lhValidateAutoPlan")
    @ResponseBody
    public AjaxResult lhValidateAutoPlan(CxScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        int releasingOrTimeoutByDate = iCxScheduleResultService.lhIsReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        List<CxScheduleResult> list = iCxScheduleResultService.getLhList(entity);
        String msg = "";
        if (CollectionUtils.isEmpty(list)) {
            //未生成，直接生成
            msg = "2";
        } else {
            //未投产
            msg = "1";
            Boolean isPublish = iCxScheduleResultService.isLhPublish(entity);
            if (isPublish) {
                //已投产
                msg = "3";
            }
        }
        return AjaxResult.success(msg);
    }

    /**
     * 手工收尾
     */
    @ApiOperation("手工收尾")
    @RequiresPermissions("cx:cxScheduleResult:manualClose")
    @PostMapping("/manualClose")
    @ResponseBody
    public AjaxResult manualClose(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        int releasingOrTimeoutByIds = iCxScheduleResultService.isReleasingOrTimeoutByIds(arr);
        if (releasingOrTimeoutByIds > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        return iCxScheduleResultService.manualClose(arr);
    }

    /**
     * 获取-使用模数
     */
    @ApiOperation("获取硫化机可使用模数")
    @PostMapping("/getMolds")
    @ResponseBody
    public CxScheduleResult getMolds(CxScheduleResult entity) {
        return iCxScheduleResultService.getMolds(entity);
    }

    /**
     * 校验-使用模数
     */
    @PostMapping("/modifyMoldsValidate")
    @ResponseBody
    public AjaxResult modifyMoldsValidate(CxScheduleResult entity) {
        return iCxScheduleResultService.modifyMoldsValidate(entity);
    }

    /**
     * 修改-使用模数
     */
    @ApiOperation("修改使用模数")
    @RequiresPermissions("cx:cxScheduleResult:modifyLhMachineQty")
    @PostMapping("/modifyMolds")
    @ResponseBody
    public AjaxResult modifyMolds(CxScheduleResult entity) {
        return iCxScheduleResultService.modifyMolds(entity);
    }

    /**
     * 收尾列表
     */
    @ApiOperation("获取收尾列表")
    @PostMapping("/finished/list")
    @ResponseBody
    public TableDataInfo finishedList(CxScheduleResult entity) {
        entity.setProductionStatus("2");
        return iCxScheduleResultService.finishedList(entity);
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
        String lang = AuthorizationUtils.getLang();
        String tempName = (ApsBootConstant.EN_US.equals(lang) ? ApsBootConstant.CX_EN_TEMP : ApsBootConstant.CX_ZH_TEMP);
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelTemplateModel + "cx/" + tempName + ".xlsx");
        if (in == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.common.message.fileNotFound"));
        }
        String fileName = I18nUtil.getMessage("ui.cx.cxScheduleResult.export.fileName");
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
//        //日期校验
//        if (scheduleDate.before(DateUtils.getNowDate("yyyy-MM-dd"))) {
//            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.scheduleDateError"));
//        }
        //若当天存在发布成功、发布中、超时失败的记录则不予导入
        CxScheduleResult entity = new CxScheduleResult();
        entity.setScheduleDate(scheduleDate);
        int releasingOrTimeoutByDate = iCxScheduleResultService.isReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        Boolean isPublish = iCxScheduleResultService.isCxPublish(entity);
        if (isPublish) {
            return AjaxResult.error(I18nUtil.getMessage("ui.biz.alter.publishedNotImport"));
        }
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CX,
                I18nUtil.getMessage("ui.cx.cxScheduleResult.export.fileName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        ExcelUtil<CxScheduleResult> util = new ExcelUtil<>(CxScheduleResult.class);
        List<CxScheduleResult> list = util.importExcel(in, 2);
        AjaxResult ajaxResult = iCxScheduleResultService.importData(list, importLog.getId(), DateUtils.dateTime(scheduleDate));
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
//        //日期校验
//        if (scheduleDate.before(DateUtils.getNowDate("yyyy-MM-dd"))) {
//            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.scheduleDateError"));
//        }
        //若当天存在发布成功、发布中、超时失败的记录则不予导入
        CxScheduleResult entity = new CxScheduleResult();
        entity.setScheduleDate(scheduleDate);
        int releasingOrTimeoutByDate = iCxScheduleResultService.isReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        Boolean isPublish = iCxScheduleResultService.isCxPublish(entity);
        if (isPublish) {
            return AjaxResult.error(I18nUtil.getMessage("ui.biz.alter.publishedNotImport"));
        }
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CX,
                I18nUtil.getMessage("ui.cx.cxScheduleResult.export.fileName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        List<CxScheduleResult> list = parseObject(in);

        AjaxResult ajaxResult = iCxScheduleResultService.importData(list, importLog.getId(), DateUtils.dateTime(scheduleDate));
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

    public List<CxScheduleResult> parseObject(InputStream in) throws Exception {

        List<SysDictData> STORAGE_LOCATION = iSysDictDataCacheService.getType("STORAGE_LOCATION");
        Map<String, String> dictMap = STORAGE_LOCATION.stream().collect(Collectors.toMap(SysDictData::getDictLabel, SysDictData::getDictValue));

        List<CxScheduleResult> list = new ArrayList<>();
        if (in == null) {
            return list;
        }
        Workbook workbook = WorkbookFactory.create(in);
        Sheet sheet = workbook.getSheetAt(0);
        if (sheet == null) {
            throw new IOException(I18nUtil.getMessage("common.error.util.file.sheet.noexist"));
        }
        int rows = sheet.getPhysicalNumberOfRows();
        if (rows > 0) {
            for (int i = 3; i < rows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String val2 = getCellValue(row.getCell(2));    //2       硫化机台
                String val3 = getCellValue(row.getCell(3));    //3       备注
                String val4 = getCellValue(row.getCell(4));    //4       使用模数
                String val6 = getCellValue(row.getCell(6));    //6       可用模具数量

                //String val8 = getCellValue(row.getCell(8));    //8       成型机台
                //20230909 Nick+ 导入2是合并单元格，所以需要判断
                String val8 = getMergedRegionValue(sheet, i, 8);  //8       成型机台

                String val11 = getCellValue(row.getCell(11));  //11      SAP品号
                String val12 = getCellValue(row.getCell(12));  //12      库存地点
                String val14 = getCellValue(row.getCell(14));  //14      胎胚代码
                String val17 = getCellValue(row.getCell(17));  //17      硫化中夜班产量
                String val18 = getCellValue(row.getCell(19));  //19      三班(8点-16点)计划量
                String val25 = getCellValue(row.getCell(26));  //26      废次品数量
                String val26 = getCellValue(row.getCell(27));  //27      最新计划数(初稿)
                String val27 = getCellValue(row.getCell(28));  //28      实际超欠产
                String val28 = getCellValue(row.getCell(29));  //29      预计超欠产
                String val29 = getCellValue(row.getCell(30));  //30      超欠产差额(实际-预计)
                String val31 = getCellValue(row.getCell(32));  //32      一班计划量
                String val33 = getCellValue(row.getCell(34));  //34      一班原因分析
                String val35 = getCellValue(row.getCell(36));  //36      二班计划量
                String val37 = getCellValue(row.getCell(38));  //38      二班原因分析
                String val39 = getCellValue(row.getCell(40));  //40      三班计划量
                String val41 = getCellValue(row.getCell(42));  //42      三班原因分析
                String val43 = getCellValue(row.getCell(44));  //44      次日一班计划量
                String val45 = getCellValue(row.getCell(46));  //46      次日一班原因分析
                String val47 = getCellValue(row.getCell(48));  //48      次日二班计划量
                String val49 = getCellValue(row.getCell(50));  //50      次日二班原因分析

                CxScheduleResult entity = new CxScheduleResult();
                entity.setLhMachineName(getStringValue(val2));                                    //2       硫化机台
                entity.setRemark(getStringValue(val3));                                           //3       备注
                entity.setLhMachineQty(getDoubleValue(val4));                                     //4       使用模数
                entity.setAvailableMoldQty(getIntegerValue(val6));                                //6       可用模具数量
                entity.setCxMachineName(getStringValue(val8));                                    //8       成型机台
                entity.setSapCode(getStringValue(val11));                                         //11      SAP品号
                entity.setStorageLocation(getStringValue(dictMap == null ? null : dictMap.get(val12))); //12      库存地点
                entity.setEmbryoCode(getStringValue(val14));                                      //14      胎胚代码
                entity.setLhMiddleNightFinishQty(getIntegerValue(val17));                         //17      硫化中夜班产量
                entity.setClass3PlannedQty(getIntegerValue(val18));                               //18      三班(8点-16点)计划量
                entity.setRejectQty(getIntegerValue(val25));                                      //25      废次品数量
                entity.setNewestPlanQty(getIntegerValue(val26));                                  //26      最新计划数(初稿)
                entity.setActualOverProduction(getIntegerValue(val27));                           //27      实际超欠产
                entity.setExpectedOverProduction(getIntegerValue(val28));                         //28      预计超欠产
                entity.setDifferenceOverProduction(getIntegerValue(val29));                       //29      超欠产差额(实际-预计)
                entity.setClass1PlanQty(getIntegerValue(val31));                                  //31      一班计划量
                entity.setClass1AnalysisInput(getStringValue(val33));                             //33      一班原因分析
                entity.setClass2PlanQty(getIntegerValue(val35));                                  //35      二班计划量
                entity.setClass2AnalysisInput(getStringValue(val37));                             //37      二班原因分析
                entity.setClass3PlanQty(getIntegerValue(val39));                                  //39      三班计划量
                entity.setClass3AnalysisInput(getStringValue(val41));                             //41      三班原因分析
                entity.setClass4PlanQty(getIntegerValue(val43));                                  //43      次日一班计划量
                entity.setClass4AnalysisInput(getStringValue(val45));                             //45      次日一班原因分析
                entity.setClass5PlanQty(getIntegerValue(val47));                                  //47      次日二班计划量
                entity.setClass5AnalysisInput(getStringValue(val49));                             //49      次日二班原因分析
                list.add(entity);
            }
        }
        return list;
    }


    /**
     * 20230909 Nick 新增 获取合并单元格值
     * 成型排程导入2中："机台名称","班制"是以合并单元格导入的。
     * 如此出现BUG：导入时合并的单元格只取了第一个单元格的值。
     * 解决方案：获取合并单元格的值。
     *
     * @param sheet  导入对象
     * @param row    行号
     * @param column 列号
     * @return
     */
    public String getMergedRegionValue(Sheet sheet, int row, int column) {
        //0.获取合并单元格数
        int sheetMergeCount = sheet.getNumMergedRegions();
        //1.遍历合并数,取值
        for (int i = 0; i < sheetMergeCount; i++) {
            CellRangeAddress ca = sheet.getMergedRegion(i);
            int firstColumn = ca.getFirstColumn();
            int lastColumn = ca.getLastColumn();
            int firstRow = ca.getFirstRow();
            int lastRow = ca.getLastRow();
            if (row >= firstRow && row <= lastRow) {
                if (column >= firstColumn && column <= lastColumn) {
                    Row fRow = sheet.getRow(firstRow);
                    Cell fCell = fRow.getCell(firstColumn);
                    return getCellValue(fCell);
                }
            }
        }
        //2.获取单元格值
        Row rowData = sheet.getRow(row);
        return getCellValue(rowData.getCell(column));
    }


    public Integer getIntegerValue(String val) {
        Integer integerVal = null;
        if (val == null) {
            return integerVal;
        }
        try {
            if (val.endsWith(".0")) {
                val = val.substring(0, val.indexOf(".0"));
            }
            integerVal = Integer.parseInt(val);
        } catch (Exception e) {
            return null;
        }
        return integerVal;
    }

    public Double getDoubleValue(String val) {
        Double db = null;
        if (val == null) {
            return db;
        }
        try {
            db = Double.valueOf(val);
        } catch (Exception e) {
            return null;
        }
        return db;
    }

    public String getStringValue(String val) {
        String stringVal = null;
        if (val == null) {
            return stringVal;
        }
        try {
            stringVal = val + "";
        } catch (Exception e) {
            return null;
        }
        return stringVal;
    }

    public String getCellValue(Cell cell) {
        Object val = null;
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC || cell.getCellType() == CellType.FORMULA) {
            val = cell.getNumericCellValue();
            //当val值为大值时，很可能解析为科学计数法显示
            String valStr = val + "";
            if (valStr.indexOf("E") >= 0) {
                BigDecimal realValue = new BigDecimal(valStr);
                val = realValue.toPlainString();
            }
        } else if (cell.getCellType() == CellType.STRING) {
            val = cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.BOOLEAN) {
            val = cell.getBooleanCellValue();
        }
        if (val == null) {
            return null;
        }
        return val + "";
    }

    /**
     * 在产下发MPS
     */
    @ApiOperation("在产下发MPS")
    @RequiresPermissions("cx:cxScheduleResult:producingIssue")
    @PostMapping("/producingIssue")
    @ResponseBody
    public AjaxResult producingIssue(CxScheduleResult entity) {
        int releasingOrTimeoutByDate = iCxScheduleResultService.isReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        return iCxScheduleResultService.producingIssue(entity);
    }

    /**
     * 硫化机台详情页面
     */
    @GetMapping("/lhMachineListPage/{idAndPid}")
    public String lhMachineList(@PathVariable("idAndPid") String idAndPid, ModelMap mmap) {
        String[] str = idAndPid.split("&");
        Long id = Long.valueOf(str[0]);
        Long pid = Long.valueOf(str[1]);
        String scheduleDate = str[2];
        mmap.put("index", id);
        mmap.put("pid", pid);
        mmap.put("scheduleDate", scheduleDate);
        return prefix + "/lhMachineDetails";
    }

    /**
     * 硫化机台详情列表
     */
    @PostMapping("/lhMachineList")
    @ResponseBody
    public TableDataInfo list(LhMachineInfoDto lhMachineInfoDto) {
        return iCxMachineInfoService.getLhMachineForQty(lhMachineInfoDto);
    }

    /**
     * 更改发布状态
     */
    @ApiOperation("更改发布状态")
    @RequiresRoles("admin")
    @PostMapping("/changeReleaseStatus")
    @ResponseBody
    public AjaxResult changeReleaseStatus(CxScheduleResult entity) {
        Date scheduleDate = entity.getScheduleDate();
        if (scheduleDate == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        return iCxScheduleResultService.changeReleaseStatus(entity);
    }

    /**
     * 验证施工
     */
    @ApiOperation("验证施工信息")
    //@RequiresPermissions("cx:cxScheduleResult:validateConstruction")
    @PostMapping("/validateConstruction")
    @ResponseBody
    public AjaxResult validateConstruction(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxScheduleResultService.validateConstructionByIds(arr);
    }


}

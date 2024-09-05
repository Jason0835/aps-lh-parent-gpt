package com.zlt.aps.cx.controller;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.common.engine.domain.MdmMonthPlanAnalysis;
import com.zlt.aps.common.engine.domain.SyncDataLogs;
import com.zlt.aps.common.engine.domain.TCxMonthPlanSurplus;
import com.zlt.aps.common.engine.planmain.MdmMonthPlanAmountSumService;
import com.zlt.aps.common.engine.result.ValidateResult;
import com.zlt.aps.common.engine.service.*;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.common.engine.utils.DateUtil;
import com.zlt.aps.cx.api.domain.entity.*;
import com.zlt.aps.cx.common.handle.CxSyncDataHandle;
import com.zlt.aps.cx.engine.constants.CxEngineConstants;
import com.zlt.aps.cx.engine.enums.AdjustTypeEnums;
import com.zlt.aps.cx.engine.exception.CxScheduleEngineException;
import com.zlt.aps.cx.engine.service.CxScheduleEngineService;
import com.zlt.aps.cx.service.CxMachineInfoService;
import com.zlt.aps.cx.service.CxScheduleResultService;
import com.zlt.aps.cx.service.CxScheduleStopInfoService;
import com.zlt.aps.cx.service.CxShareMoldInfoService;
import com.zlt.aps.lh.engine.service.LhEngineService;
import com.zlt.sync.povo.SyncParamsVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ApsCommonUtil.getIntOrDefault;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;


/**
 * 成型排程结果Controller
 *
 * @author zlt
 * @date 2021-07-12
 */
@RestController
@RequestMapping("/cxScheduleResult")
public class CxScheduleResultController extends BaseController {
    @Value("${excelModelPath}")
    public String excelModelPath;
    @Autowired
    private CxScheduleResultService cxScheduleResultService;
    @Autowired
    private CxMachineInfoService machineInfoService;
    @Autowired
    private MdmMonthPlanAmountSumService mdmMonthPlanAmountSumService;

    @Resource(name = "cxScheduleEngineService")
    private CxScheduleEngineService cxScheduleEngineService;

    @Resource(name = "lhEngineService")
    private LhEngineService lhEngineService;

    @Resource
    private CxSyncDataHandle syncDataHandle;
    @Autowired
    private FactoryService factoryService;
    @Resource
    private SyncDataLogsService syncDataLogsService;
    @Autowired
    private CxShareMoldInfoService cxShareMoldInfoService;
    @Autowired
    private TCxEmbryoMonthPlanSurplusService cxEmbryoMonthPlanSurplusService;
    @Autowired
    private TCxMonthPlanSurplusService tCxMonthPlanSurplusService;
    @Autowired
    private CxScheduleStopInfoService cxScheduleStopInfoService;

    @Autowired
    private CxEngineChangeLhMachineService cxEngineChangeLhMachineService;

    /**
     * 查询成型排程结果列表
     */
    //@PreAuthorize(hasPermi = "cx:cxScheduleResult:list")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody CxScheduleResult cxScheduleResult) {
        List<CxScheduleResult> list = cxScheduleResultService.selectCxScheduleResultList(cxScheduleResult);

        // 获取使用模数信息，<工单, 使用模数>
//        Map<String, String> cxOrderWithLhMachines = cxEngineChangeLhMachineService.splitCxOrderWithLhMachines(cxScheduleResult.getScheduleDate(), null,"");
        // 获取所有胎胚对应的月度剩余量，用于页面悬浮展示胎胚共用模具信息
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(cxScheduleResult.getScheduleDate());
//        Map<String, BigDecimal> embryoCodeMonthRemainQtyMap = cxEmbryoMonthPlanSurplusService.selectMonthRemainQtyByYearAndMonthGroupByMaterialCode(String.valueOf(calendar.get(Calendar.YEAR)), String.valueOf(calendar.get(Calendar.MONTH)) + 1);
        List<CxShareMoldInfo> shareMoldInfoList = cxShareMoldInfoService.selectShareMoldInfoList();
        Map<String, String> cxShareMoldInfoGroupNameMap = new HashMap<>();
        Map<String, String> cxShareMoldInfoSapCodeMap = new HashMap<>();
        putShareMoldInfoMap(shareMoldInfoList, cxShareMoldInfoGroupNameMap, cxShareMoldInfoSapCodeMap);
        // 从共用模具列表中根据sap品号获取对应的胎胚、规格型号,后面用于拼接sap悬浮显示信息
        Map<String, Set<String>> sapCodeEmbryoCodeMap = shareMoldInfoList.stream()
                .collect(Collectors.groupingBy(CxShareMoldInfo::getSapCode,
                        mapping(CxShareMoldInfo::getEmbryoCode, toSet())));
        // 获取所有外胎计划汇总数据，用于回显共用模具信息
        TCxMonthPlanSurplus entity = new TCxMonthPlanSurplus();
        entity.setYear(DateFormatUtils.format(cxScheduleResult.getScheduleDate(),"yyyy"));
        entity.setMonth(DateFormatUtils.format(cxScheduleResult.getScheduleDate(),"MM"));
        /*Map<String, Integer> cxMonthPlanSurplusMap = tCxMonthPlanSurplusService.getByParams(entity).stream()
                .collect(Collectors.toMap(TCxMonthPlanSurplus::getSapCode, TCxMonthPlanSurplus::getMonthRemainQty));*/
        Map<String, TCxMonthPlanSurplus> cxMonthPlanSurplusDataMap = CollectionUtil.toMap(tCxMonthPlanSurplusService.getByParams(entity), TCxMonthPlanSurplus::getSapCode);
        // 获取成型机台自动停排信息
        CxScheduleStopInfo stopInfo = new CxScheduleStopInfo();
        stopInfo.setScheduleDate(cxScheduleResult.getScheduleDate());
        Map<String, Integer> stopInfoMap = cxScheduleStopInfoService.selectCxScheduleStopInfoList(stopInfo).stream().collect(Collectors.toMap(CxScheduleStopInfo::getCxMachineCode, CxScheduleStopInfo::getClassShift));
        //颜色设置
        Map<String, Long> sapCodeCountMap = list.stream().collect(Collectors.groupingBy(a->a.getSapCode(),Collectors.counting()));
        Map<String, Long> embryoCodeCountMap = list.stream().collect(Collectors.groupingBy(a->a.getEmbryoCode(),Collectors.counting()));
        Map<String,Integer> lhMachineCountMap=new HashMap<>();

        //Map<类型+机台,最小寸口>
        Map<String, Double> typeAndCodeMap =new HashMap<>();

        list.forEach(a->{
            if (StringUtils.isNotBlank(a.getLhMachineCode())){
                String[] machines=a.getLhMachineCode().split(",");
                for (String machine: machines) {
                    if(lhMachineCountMap.containsKey(machine)){
                        lhMachineCountMap.put(machine,lhMachineCountMap.get(machine)+1);
                    }else{
                        lhMachineCountMap.put(machine,1);
                    }
                }
            }
            if (StringUtils.isNotBlank(a.getSapCode()) && sapCodeCountMap.get(a.getSapCode())>1){
                a.setColorForSapCode(sapCodeCountMap.get(a.getSapCode()));
            }
            if (StringUtils.isNotBlank(a.getEmbryoCode()) && embryoCodeCountMap.get(a.getEmbryoCode())>1){
                a.setColorForEmbryoCode(embryoCodeCountMap.get(a.getEmbryoCode()));
            }

            String typeAndCode= a.getCxMachineType()+a.getCxMachineCode();
            Double specDimension=a.getSpecDimension();
            if (!typeAndCodeMap.containsKey(typeAndCode)){
                typeAndCodeMap.put(typeAndCode,specDimension);
            }

        });
        list.forEach(a->{
            if (StringUtils.isNotBlank(a.getLhMachineCode())){
                String[] machines=a.getLhMachineCode().split(",");
                for (String machine: machines) {
                    if (lhMachineCountMap.containsKey(machine) && lhMachineCountMap.get(machine)>1) {
                        a.setColorForLhMachine(lhMachineCountMap.get(machine));
                    }
                }
            }
            String typeAndCode= a.getCxMachineType()+a.getCxMachineCode();
            a.setOrderByStr(a.getCxMachineType()+typeAndCodeMap.get(typeAndCode)+a.getCxMachineName()+a.getId());

            // 页面胎胚代码悬浮显示共用模具信息
            StringBuilder shareMoldInfoStr = new StringBuilder();
            List<String> sapCodes = getEmbryoCodesBySapCode(cxShareMoldInfoGroupNameMap, cxShareMoldInfoSapCodeMap, a.getSapCode());
//            for (String embryoCode : sapCodes) {
//                BigDecimal monthRemainQty = embryoCodeMonthRemainQtyMap.get(embryoCode);
//                if (ObjectUtils.isEmpty(monthRemainQty)) {
//                    monthRemainQty = BigDecimal.valueOf(0);
//                }
//                shareMoldInfoStr.append(embryoCode)
//                        .append(I18nUtil.getMessage("ui.data.column.scheduleResult.monthRemainQty")).append(":")
//                        .append(monthRemainQty).append(";<br>");
//            }
            //Joran 2022-04-27 成型排程结果列表 月度剩余量 和公用模不良数展示调整start
            if(cxMonthPlanSurplusDataMap.containsKey(a.getSapCode())){
                TCxMonthPlanSurplus cxMonthPlanSurplus = cxMonthPlanSurplusDataMap.getOrDefault(a.getSapCode(), null);
                if(cxMonthPlanSurplus!=null){
                    if (CollectionUtils.isNotEmpty(sapCodes) && StringUtil.isNotBlank(a.getSapCode())) {
                        for (String sapCode : sapCodes) {
                            TCxMonthPlanSurplus monthPlanSurplus = cxMonthPlanSurplusDataMap.getOrDefault(sapCode, null);
                            Set<String> embryoCodeSet = sapCodeEmbryoCodeMap.get(sapCode);
                            Integer monthRemainQty = 0;
                            for (String embryoCodeStr : embryoCodeSet) {
                                if (monthPlanSurplus != null) {
                                    monthRemainQty = monthPlanSurplus.getMonthRemainQty();
                                }
                                shareMoldInfoStr.append(sapCode).append(",")
                                        .append(embryoCodeStr).append(",")
                                        .append(I18nUtil.getMessage("ui.data.column.hover.monthRemainQty")).append(monthRemainQty)
                                        .append("<br>");
                            }
                        }
                    }
                    a.setShareMoldInfoStr(shareMoldInfoStr.toString());
                    a.setMonthPlanOs(cxMonthPlanSurplus.getMonthRemainQty());
                }
            }
            //Joran 2022-04-27 成型排程结果列表 月度剩余量 和公用模不良数展示调整end


            // 页面停排成型机颜色提示
            Integer classShift = stopInfoMap.get(a.getCxMachineCode());
            if (ObjectUtils.isNotEmpty(classShift)) {
                a.setScheduleStop(1);
                a.setStopClassShift(classShift);
            }
            if (ObjectUtils.isNotEmpty(a.getRejectQty())) {
                a.setMonthPlanOsHoverStr(I18nUtil.getMessage("ui.data.column.hover.rejectQty") + Objects.toString(a.getRejectQty(), "0"));
            }
//            a.setLhMachineName(cxOrderWithLhMachines.getOrDefault(a.getOrderNo(), ""));
        });

        List<CxScheduleResult> newList = list.stream().sorted(Comparator.comparing(CxScheduleResult::getOrderByStr)).collect(Collectors.toList());

        return getDataTable(newList);
    }

    /**
     * 查询成型排程结果列表
     */
    //@PreAuthorize(hasPermi = "cx:cxScheduleResult:list")
    @PostMapping("/finishedList")
    public TableDataInfo finishedList(@RequestBody CxScheduleResult cxScheduleResult) {
        List<CxScheduleResult> list = cxScheduleResultService.finishedList(cxScheduleResult);

        //颜色设置
        Map<String, Long> sapCodeCountMap = list.stream().collect(Collectors.groupingBy(a->a.getSapCode(),Collectors.counting()));
        Map<String, Long> embryoCodeCountMap = list.stream().collect(Collectors.groupingBy(a->a.getEmbryoCode(),Collectors.counting()));
        Map<String,Integer> lhMachineCountMap=new HashMap<>();

        //Map<类型+机台,最小寸口>
        Map<String, Double> typeAndCodeMap =new HashMap<>();

        list.forEach(a->{
            if (StringUtils.isNotBlank(a.getLhMachineCode())){
                String[] machines=a.getLhMachineCode().split(",");
                for (String machine: machines) {
                    if(lhMachineCountMap.containsKey(machine)){
                        lhMachineCountMap.put(machine,lhMachineCountMap.get(machine)+1);
                    }else{
                        lhMachineCountMap.put(machine,1);
                    }
                }
            }
            if (StringUtils.isNotBlank(a.getSapCode()) && sapCodeCountMap.get(a.getSapCode())>1){
                a.setColorForSapCode(sapCodeCountMap.get(a.getSapCode()));
            }
            if (StringUtils.isNotBlank(a.getEmbryoCode()) && embryoCodeCountMap.get(a.getEmbryoCode())>1){
                a.setColorForEmbryoCode(embryoCodeCountMap.get(a.getEmbryoCode()));
            }

            String typeAndCode= a.getCxMachineType()+a.getCxMachineCode();
            Double specDimension=a.getSpecDimension();
            if (!typeAndCodeMap.containsKey(typeAndCode)){
                typeAndCodeMap.put(typeAndCode,specDimension);
            }

        });
        list.forEach(a->{
            if (StringUtils.isNotBlank(a.getLhMachineCode())){
                String[] machines=a.getLhMachineCode().split(",");
                for (String machine: machines) {
                    if (lhMachineCountMap.containsKey(machine) && lhMachineCountMap.get(machine)>1) {
                        a.setColorForLhMachine(lhMachineCountMap.get(machine));
                    }
                }
            }
            String typeAndCode= a.getCxMachineType()+a.getCxMachineCode();
            a.setOrderByStr(a.getCxMachineType()+typeAndCodeMap.get(typeAndCode)+a.getCxMachineName()+a.getId());
        });

        List<CxScheduleResult> newList = list.stream().sorted(Comparator.comparing(CxScheduleResult::getOrderByStr)).collect(Collectors.toList());

        return getDataTable(newList);
    }


    /**
     * 查询成型排程结果列表
     */
    @PostMapping("/getList")
    public List<CxScheduleResult> getList(@RequestBody CxScheduleResult cxScheduleResult) {
//        startPage("a.cx_machine_code,a.embryo_code,a.sap_code asc");
        List<CxScheduleResult> list = cxScheduleResultService.selectCxScheduleResultList(cxScheduleResult);
        return list;
    }

    /**
     * 硫化自动排程校验
     */
    @PostMapping("/getLhList")
    public List<CxScheduleResult> getLhList(@RequestBody CxScheduleResult cxScheduleResult) {
        List<CxScheduleResult> list = cxScheduleResultService.getLhList(cxScheduleResult);
        return list;
    }

    /**
     * 查询非本id的且包含该硫化机的记录
     */
    @PostMapping("/getListByLhMachineCode")
    public List<CxScheduleResult> getListByLhMachineCode(@RequestBody CxScheduleResult cxScheduleResult) {
        List<CxScheduleResult> list = cxScheduleResultService.getListByLhMachineCode(cxScheduleResult);
        return list;
    }

    /**
     * 获取成型排程结果详细信息
     */
    //@PreAuthorize(hasPermi = "cx:cxScheduleResult:query")
    @GetMapping(value = "/{id}")
    public CxScheduleResult getInfo(@PathVariable("id") Long id) {
        return cxScheduleResultService.selectCxScheduleResultById(id);
    }

    /**
     * 获取成型排程结果详细信息
     */
    @GetMapping(value = "/getInfoForQty/{id}")
    public CxScheduleResult getInfoForQty(@PathVariable("id") Long id) {
        return cxScheduleResultService.selectCxScheduleResultByIdForQty(id);
    }


    /**
     * 成型插单校验
     */
    @PostMapping("/validateAdd")
    public AjaxResult validateAdd(@RequestBody CxScheduleResult cxScheduleResult) {
        //此处调用插单校验接口，需覆盖 AjaxResult
        ValidateResult validateResult = cxScheduleEngineService.insertPreCheck(cxScheduleResult);
        String msg = validateResult.getMsg();
        if (validateResult.isSuccess()) {
            return AjaxResult.success(msg);
        } else {
            return AjaxResult.error(msg);
        }
    }

    /**
     * 转机台校验校验
     */
    @PostMapping("/validateChangeMachine")
    public AjaxResult validateChangeMachine(@RequestBody CxScheduleResult cxScheduleResult) {
        CxScheduleResult localScheduleResult = cxScheduleResultService.selectCxScheduleResultById(cxScheduleResult.getId());
        //此处调用转机台校验校验接口，需覆盖 AjaxResult
        ValidateResult validateResult = cxScheduleEngineService.changeMachinePreCheck(localScheduleResult, cxScheduleResult.getCxMachineCode());
        String msg = validateResult.getMsg();
        if (validateResult.isSuccess()) {
            return AjaxResult.success(msg);
        } else {
            return AjaxResult.error(msg);
        }
    }

    /**
     * 调量校验
     */
    @PostMapping("/validateChangeQty")
    public AjaxResult validateChangeQty(@RequestBody CxScheduleResult entity) {
        ValidateResult validateResult = cxScheduleEngineService.changePlanQtyPreCheck(entity);
        String msg = validateResult.getMsg();
        if (validateResult.isSuccess()) {
            return AjaxResult.success(msg);
        } else {
            return AjaxResult.error(msg);
        }
    }


    /**
     * 唯一性校验
     */
    @PostMapping("/checkScheduleResultUnique")
    public List<CxScheduleResult> checkScheduleResultUnique(@RequestBody CxScheduleResult cxScheduleResult) {
        List<CxScheduleResult> list = cxScheduleResultService.checkScheduleResultUnique(cxScheduleResult);
        return list;
    }

    /**
     * 成型插单
     */
    //@PreAuthorize(hasPermi = "cx:cxScheduleResult:add")
    @Log(title = "ui.cx.cxScheduleResult.export.fileName", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody CxScheduleResult cxScheduleResult) {
        //此处调用插单算法
        try {
            List<CxScheduleResult> scheduleResults = cxScheduleResultService.selectByScheduleDateAndCode(cxScheduleResult);
            cxScheduleEngineService.insertTask(cxScheduleResult);
            cxScheduleResultService.insetDispatcherLogInsertOrder(ApsConstant.DISPATCHER_OPER_INSERT_ORDER, scheduleResults, cxScheduleResult);
            return AjaxResult.success();
        } catch (CxScheduleEngineException e) {
            logger.error("插单异常：" + e.getMessage());
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 转机台
     */
    @Log(title = "ui.cx.cxScheduleResult.export.fileName", businessType = BusinessType.CHANGE_MACHINE)
    @PostMapping("/changeMachine")
    public AjaxResult changeMachine(@RequestBody CxScheduleResult cxScheduleResult) {
        //唯一性校验
        List<CxScheduleResult> list = cxScheduleResultService.checkScheduleResultUnique(cxScheduleResult);
        if (CollectionUtils.isNotEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cxScheduleResult.uniqueValidate"));
        } else {
            CxScheduleResult beforeCxScheduleResult= cxScheduleResultService.selectCxScheduleResultById(cxScheduleResult.getId());
            beforeCxScheduleResult.setIsRelease(beforeCxScheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
            cxScheduleEngineService.changeMachineTask(beforeCxScheduleResult,cxScheduleResult.getCxMachineCode());
            cxScheduleResultService.insetDispatcherLog(ApsConstant.DISPATCHER_OPER_MACHINE, beforeCxScheduleResult, cxScheduleResult);  //如果是调度员操作，则需要增加操作日志
            return AjaxResult.success();
        }
    }

    /**
     * 修改
     */
    @Log(title = "ui.cx.cxScheduleResult.export.fileName", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody CxScheduleResult cxScheduleResult) {
        //唯一性校验
        List<CxScheduleResult> list = cxScheduleResultService.checkScheduleResultUnique(cxScheduleResult);
        if (CollectionUtils.isNotEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cxScheduleResult.uniqueValidate"));
        } else {
            return toAjax(cxScheduleResultService.updateCxScheduleResult(cxScheduleResult));
        }
    }

    /**
     * 修改
     */
    @Log(title = "ui.cx.cxScheduleResult.export.fileName", businessType = BusinessType.UPDATE)
    @PostMapping("/modifyStatus")
    public AjaxResult modifyStatus(@RequestBody CxScheduleResult cxScheduleResult) {
        return toAjax(cxScheduleResultService.modifyStatus(cxScheduleResult));
    }

    /**
     * 调量更新
     */
    @Log(title = "ui.cx.cxScheduleResult.export.fileName", businessType = BusinessType.CHANGE_QTY)
    @PostMapping("/changeQty")
    public AjaxResult changeQty(@RequestBody CxScheduleResult cxScheduleResult) {
        //唯一性校验
        List<CxScheduleResult> list = cxScheduleResultService.checkScheduleResultUnique(cxScheduleResult);
        if (CollectionUtils.isNotEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cxScheduleResult.uniqueValidate"));
        } else {
            //Joran 2021-08-10 调量调用引擎进行各个班次可硫化班数重新就算
            cxScheduleEngineService.calcAvaliableClassShift(cxScheduleResult, AdjustTypeEnums.CHANGE_QTY);
            //计算平均可硫化班次超过库表
            cxScheduleResultService.insetDispatcherLog(ApsConstant.DISPATCHER_OPER_PLAN, null, cxScheduleResult);  //如果是调度员操作，则需要增加操作日志
            //Nick 20231020 调量如果是9999999 可能导致1-5班平均可硫化班次超过数据库精度,这里进行提示。
            if(cxScheduleResult.getClass1AvailableLhShift() > 9999999 || cxScheduleResult.getClass2AvailableLhShift() > 9999999 || cxScheduleResult.getClass3AvailableLhShift() > 9999999 || cxScheduleResult.getClass4AvailableLhShift() > 9999999 || cxScheduleResult.getClass5AvailableLhShift() > 9999999){
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cxScheduleResult.changeplanValidate"));
            }
            return toAjax(cxScheduleResultService.updateCxScheduleResultForQty(cxScheduleResult));
        }
    }



    /**
     * 修改施工版本
     */
    @Log(title = "ui.cx.cxScheduleResult.export.fileName", businessType = BusinessType.UPDATE)
    @PostMapping("/changeBomDataVersion")
    public AjaxResult changeBomDataVersion(@RequestBody CxScheduleResult cxScheduleResult) {
        //唯一性校验
        List<CxScheduleResult> list = cxScheduleResultService.checkScheduleResultUnique(cxScheduleResult);
        if (CollectionUtils.isNotEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cxScheduleResult.uniqueValidate"));
        }
        return toAjax(cxScheduleResultService.changeBomDataVersion(cxScheduleResult));
    }

    /**
     * 计算半部件调量参考值
     */
    @PostMapping("/qtyReference")
    public AjaxResult qtyReference(@RequestBody CxScheduleResult cxScheduleResult) {
        CxScheduleResult osCxScheduleResult = cxScheduleResultService.selectCxScheduleResultById(cxScheduleResult.getId());
        Integer class1PlanQty = osCxScheduleResult.getClass1PlanQty()==null?0:osCxScheduleResult.getClass1PlanQty();
        Integer class2PlanQty = osCxScheduleResult.getClass2PlanQty()==null?0:osCxScheduleResult.getClass2PlanQty();
        Integer class3PlanQty = osCxScheduleResult.getClass3PlanQty()==null?0:osCxScheduleResult.getClass3PlanQty();
        Integer class4PlanQty = osCxScheduleResult.getClass4PlanQty()==null?0:osCxScheduleResult.getClass4PlanQty();
        Integer class5PlanQty = osCxScheduleResult.getClass5PlanQty()==null?0:osCxScheduleResult.getClass5PlanQty();
        Integer osTotal = class1PlanQty + class2PlanQty + class3PlanQty + class4PlanQty + class5PlanQty;
        Integer totalClassPlanQty = cxScheduleResult.getTotalClassPlanQty()==null?0:cxScheduleResult.getTotalClassPlanQty();
        Integer difference = totalClassPlanQty - osTotal;
        MdmMonthPlanAnalysis mdmMonthPlanAnalysis = mdmMonthPlanAmountSumService.getEmbryoConsumption(cxScheduleResult.getEmbryoCode(), Math.abs(difference), cxScheduleResult.getBomDataVersion());
        Map<String, String> map = new HashMap<>();
        if (mdmMonthPlanAnalysis != null) {
            String Symbol = difference < 0 ? "-" : "";
            String reference = I18nUtil.getMessage("ui.data.column.cxScheduleResult.reference");
            map.put("cd15Reference", reference + "：" + (mdmMonthPlanAnalysis.getCd15OneMonthPlanQty().compareTo(new BigDecimal(0)) == 0 ? 0 : Symbol + mdmMonthPlanAnalysis.getCd15OneMonthPlanQty()));

            BigDecimal cd90OneMonthPlanQty= mdmMonthPlanAnalysis.getCd90OneMonthPlanQty()==null?new BigDecimal(0):mdmMonthPlanAnalysis.getCd90OneMonthPlanQty();
            BigDecimal cd90TwoMonthPlanQty= mdmMonthPlanAnalysis.getCd90TwoMonthPlanQty()==null?new BigDecimal(0):mdmMonthPlanAnalysis.getCd90TwoMonthPlanQty();
            BigDecimal cd90ThreeMonthPlanQty= mdmMonthPlanAnalysis.getCd90ThreeMonthPlanQty()==null?new BigDecimal(0):mdmMonthPlanAnalysis.getCd90ThreeMonthPlanQty();
            BigDecimal cd90MonthPlanQty=cd90OneMonthPlanQty.add(cd90TwoMonthPlanQty).add(cd90ThreeMonthPlanQty);
            map.put("cd90Reference", reference + "：" + (cd90MonthPlanQty.compareTo(new BigDecimal(0)) == 0 ? 0 : Symbol + cd90MonthPlanQty));
            map.put("gdyyReference", reference + "：" + (mdmMonthPlanAnalysis.getGdyyMonthPlanQty().compareTo(new BigDecimal(0)) == 0 ? 0 : Symbol + mdmMonthPlanAnalysis.getGdyyMonthPlanQty()));
            map.put("gsqReference", reference + "：" + (mdmMonthPlanAnalysis.getGsqMonthPlanQty().compareTo(new BigDecimal(0)) == 0 ? 0 : Symbol + mdmMonthPlanAnalysis.getGsqMonthPlanQty()));
            map.put("ncReference", reference + "：" + (mdmMonthPlanAnalysis.getNcMonthPlanQty().compareTo(new BigDecimal(0)) == 0 ? 0 : Symbol + mdmMonthPlanAnalysis.getNcMonthPlanQty()));
            map.put("tcReference", reference + "：" + (mdmMonthPlanAnalysis.getTcMonthPlanQty().compareTo(new BigDecimal(0)) == 0 ? 0 : Symbol + mdmMonthPlanAnalysis.getTcMonthPlanQty()));
            map.put("tmReference", reference + "：" + (mdmMonthPlanAnalysis.getTmMonthPlanQty() == 0 ? 0 : Symbol + mdmMonthPlanAnalysis.getTmMonthPlanQty()));
            map.put("tqReference", reference + "：" + (mdmMonthPlanAnalysis.getTqMonthPlanQty().compareTo(new BigDecimal(0)) == 0 ? 0 : Symbol + mdmMonthPlanAnalysis.getTqMonthPlanQty()));
            map.put("xwyyReference", reference + "：" + (mdmMonthPlanAnalysis.getXwyyMonthPlanQty().compareTo(new BigDecimal(0)) == 0 ? 0 : Symbol + mdmMonthPlanAnalysis.getXwyyMonthPlanQty()));
        }
        return AjaxResult.success(map);
    }

    /**
     * 删除成型排程结果
     */
    //@PreAuthorize(hasPermi = "cx:cxScheduleResult:remove")
    @Log(title = "ui.cx.cxScheduleResult.export.fileName", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        if (cxScheduleResultService.isPublishByIds(ids) != ids.length) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isPublishById"));
        }
        List<CxScheduleResult> removeList= new ArrayList<>();
        String validateMsg=cxScheduleResultService.removeResultCheck(ids,removeList);
        if(StringUtils.isNotEmpty(validateMsg)){
            return AjaxResult.error(validateMsg);
        }
        return toAjax(cxScheduleResultService.removeCxSecheduleResultByList(ids,removeList));
    }

    /**
     * 收尾成型排程结果
     */
    @Log(title = "ui.cx.cxScheduleResult.export.fileName", businessType = BusinessType.MANUAL_CLOSE)
    @GetMapping("/manualClose/{ids}")
    public AjaxResult manualClose(@PathVariable Long[] ids) {
        return toAjax(cxScheduleResultService.manualClose(ids));
    }

    /**
     * 导出列表
     */
    //@PreAuthorize(hasPermi = "cx:cxScheduleResult:export")
    @Log(title = "ui.cx.cxScheduleResult.export.fileName", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public byte[] export(@RequestBody CxScheduleResult cxScheduleResult) throws Exception {

        //查询数据
//        startPage("a.cx_machine_code,a.embryo_code,a.sap_code asc");
        Locale lang = ServletUtils.getUserLang();
        cxScheduleResult.setLocal(lang.toString());
        List<CxScheduleResult> list0 = cxScheduleResultService.selectCxScheduleResultListForExport(cxScheduleResult);

        //相同类型、机台、寸口的记录数
        Map<String, Double> typeAndCodeMap =new HashMap<>();
        list0.forEach(a->{
            String typeAndCode= a.getCxMachineType()+a.getCxMachineCode();
            Double specDimension=a.getSpecDimension();
            if (!typeAndCodeMap.containsKey(typeAndCode)){
                typeAndCodeMap.put(typeAndCode,specDimension);
            }
        });
        list0.forEach(a->{
            String typeAndCode= a.getCxMachineType()+a.getCxMachineCode();
            a.setOrderByStr(a.getCxMachineType()+typeAndCodeMap.get(typeAndCode)+a.getCxMachineCode());
        });

        List<CxScheduleResult> newList = list0.stream().sorted(Comparator.comparing(CxScheduleResult::getOrderByStr)).collect(Collectors.toList());


        //按用户语言读取模板
        InputStream in = null;
        if (Locale.SIMPLIFIED_CHINESE.equals(lang) || lang == null) {
            // 中文
            in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "cxScheduleResult.xlsx");
        } else if (Locale.US.equals(lang)) {
            // 英文
            in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "cxScheduleResult_en.xlsx");
        }
        Workbook webBook = ExcelUtils.readExcel(in);

        //填充数据
        if (CollectionUtils.isNotEmpty(newList)) {
            List<CxMachineInfo> tcMachineInfoList = machineInfoService.selectCxMachineInfoList(new CxMachineInfo());
            Map<String, String> cxmap = null;
            if (CollectionUtils.isNotEmpty(tcMachineInfoList)) {
                cxmap = tcMachineInfoList.stream().collect(Collectors.toMap(item -> item.getMachineCode() + "", item -> item.getMachineName()));
            }
            CxMachineInfo machineInfo = new CxMachineInfo();
            machineInfo.setId(5L);
            List<CxMachineInfo> list3 = machineInfoService.selectCxMachineInfoList2(machineInfo);
            Map<String, String> lhmap = null;
            if (CollectionUtils.isNotEmpty(list3)) {
                lhmap = list3.stream().collect(Collectors.toMap(item -> item.getMachineCode() + "", item -> item.getMachineName()));
            }

            Sheet sheet = webBook.getSheetAt(0);
            sheet.shiftRows(0, sheet.getLastRowNum(), 1,true,false);
            Row title = sheet.createRow(0);
            for (int i = 0; i < 51; i++) {
                title.createCell(i);
            }


            //重置表头基本信息
            String dateStr="";
            Locale langZh = ServletUtils.getUserLang();
            // 导出收尾列表
            if (!"2".equals(cxScheduleResult.getProductionStatus())) {
                int month = DateUtil.getMonth(cxScheduleResult.getScheduleDate());
                int day = DateUtil.getDay(cxScheduleResult.getScheduleDate());
                if("zh_CN".equals(langZh.toString())){
                    dateStr=DateUtils.parseDateToStr("MM月dd日",cxScheduleResult.getScheduleDate());
                }else{
                    String monthStr=month+"";
                    String dayStr=day+"";
                    if(monthStr.length()<=1){
                        monthStr="0"+month;
                    }
                    if(dayStr.length()<=1){
                        dayStr="0"+day;
                    }
                    dateStr= DateUtil.getEngMonthDay(monthStr+dayStr) + " ";
                }
            }
            Integer class1PlanQty=0;
            Integer class2PlanQty=0;
            Integer class3PlanQty=0;
            Integer class4PlanQty=0;
            Integer class5PlanQty=0;
            Integer totalPlan=0;
            for (CxScheduleResult csr:newList){
                class1PlanQty += csr.getClass1PlanQty() == null ? 0 : csr.getClass1PlanQty();
                class2PlanQty += csr.getClass2PlanQty() == null ? 0 : csr.getClass2PlanQty();
                class3PlanQty += csr.getClass3PlanQty() == null ? 0 : csr.getClass3PlanQty();
                class4PlanQty += csr.getClass4PlanQty() == null ? 0 : csr.getClass4PlanQty();
                class5PlanQty += csr.getClass5PlanQty() == null ? 0 : csr.getClass5PlanQty();
            }
            totalPlan=class1PlanQty+class2PlanQty+class3PlanQty;
            String baseInfo=I18nUtil.getMessage("ui.data.column.scheduleResult.cx.baseInfo");
            String class1Plan=I18nUtil.getMessage("ui.data.column.scheduleResult.class1Plan");
            String class2Plan=I18nUtil.getMessage("ui.data.column.scheduleResult.class2Plan");
            String class3Plan=I18nUtil.getMessage("ui.data.column.scheduleResult.class3Plan");
            String class4Plan=I18nUtil.getMessage("ui.data.column.scheduleResult.class4Plan");
            String class5Plan=I18nUtil.getMessage("ui.data.column.scheduleResult.class5Plan");
            String totalQty=I18nUtil.getMessage("ui.data.column.scheduleResult.total3Qty");
            String planInfo = '：'+class1Plan+'：'+class1PlanQty+'，'+class2Plan+'：'+class2PlanQty+'，'+class3Plan+'：'+class3PlanQty+'，'+class4Plan+'：'+class4PlanQty+'，'+class5Plan+'：'+class5PlanQty+'，'+totalQty+'：'+totalPlan;
            baseInfo=dateStr+baseInfo+planInfo;
            Cell cell0=sheet.getRow(0).getCell(0);
            CellStyle cellStyle0=cell0.getCellStyle();
            cellStyle0.setVerticalAlignment(VerticalAlignment.CENTER);
            cellStyle0.setAlignment(HorizontalAlignment.CENTER);
            cell0.setCellValue(baseInfo);
            cell0.setCellStyle(cellStyle0);
            sheet.addMergedRegion(new CellRangeAddress(title.getRowNum(), title.getRowNum(), title.getFirstCellNum(), title.getLastCellNum()));


            CellStyle cellStyle = ExcelUtils.createCellStyle(webBook);
            Map<String, Region> regionMap = new HashMap<>();

            this.summaryExport(newList);  //给导出的数据增加汇总行
            for (int i = 0; i < newList.size(); i++) {
                CxScheduleResult scheduleResult = newList.get(i);
                Row row = sheet.createRow(i + 3);
                int rowNum = 0;
                row.createCell(rowNum++).setCellValue(scheduleResult.getTaskType() == null ? "" : scheduleResult.getTaskType());
                row.createCell(rowNum++).setCellValue(scheduleResult.getProductionStatus() == null ? "" : scheduleResult.getProductionStatus());

                String lhMachineCode = "";
                if (StringUtils.isNotEmpty(scheduleResult.getLhMachineCode()) && lhmap != null) {
                    String[] aa = scheduleResult.getLhMachineCode().split(",");
                    for (String a : aa) {
                        lhMachineCode = lhMachineCode + lhmap.get(a) + ",";
                    }
                }
                if (StringUtils.isNotEmpty(lhMachineCode)) {
                    lhMachineCode = lhMachineCode.substring(0, lhMachineCode.length() - 1);
                }
                row.createCell(rowNum++).setCellValue(lhMachineCode);

                //Joran 2022-01-12 备注拼接特殊要求start
                String specialRequirements=scheduleResult.getSpecialRequirements();
                StringBuilder remark=new StringBuilder();
                if(StringUtils.isNotEmpty(scheduleResult.getRemark())){
                    remark.append(scheduleResult.getRemark());
                }
                if (StringUtils.isNotEmpty(specialRequirements)) {
                    if (StringUtils.isNotEmpty(remark)) {
                        remark.append(",").append(specialRequirements);
                    } else {
                        remark.append(specialRequirements);
                    }
                }
                //Joran 2022-01-12 备注拼接特殊要求end

                row.createCell(rowNum++).setCellValue(StringUtils.isEmpty(remark)?"":remark.toString());
                row.createCell(rowNum++).setCellValue(scheduleResult.getLhMachineQty() == null ? 0 : scheduleResult.getLhMachineQty());
                row.createCell(rowNum++).setCellValue(scheduleResult.getMinimumLhMachineComQty() == null ? 0 : scheduleResult.getMinimumLhMachineComQty());
                row.createCell(rowNum++).setCellValue(scheduleResult.getAvailableMoldQty() == null ? 0 : scheduleResult.getAvailableMoldQty());
                row.createCell(rowNum++).setCellValue(scheduleResult.getSpecDimension() == null ? "" : scheduleResult.getSpecDimension() + "");

                String cxMachineCode = "";
                if (StringUtils.isNotEmpty(scheduleResult.getCxMachineCode()) && cxmap != null) {
                    cxMachineCode = cxmap.get(scheduleResult.getCxMachineCode());
                }
                row.createCell(rowNum++).setCellValue(cxMachineCode);

                Region region = regionMap.get(cxMachineCode);
                if (region == null) {
                    regionMap.put(cxMachineCode, new Region(i + 3, i + 3));
                } else {
                    region.setLastRow(region.getLastRow() + 1);
                }

                row.createCell(rowNum++).setCellValue(scheduleResult.getWorkShifts() == null ? "" : scheduleResult.getWorkShifts() + "");
                row.createCell(rowNum++).setCellValue(scheduleResult.getMaximumClassQty() == null ? 0 : scheduleResult.getMaximumClassQty());

                row.createCell(rowNum++).setCellValue(scheduleResult.getSapCode() == null ? "" : scheduleResult.getSapCode());
                row.createCell(rowNum++).setCellValue(scheduleResult.getStorageLocation() == null ? "" : scheduleResult.getStorageLocation());
                row.createCell(rowNum++).setCellValue(scheduleResult.getSpecDesc() == null ? "" : scheduleResult.getSpecDesc());
                row.createCell(rowNum++).setCellValue(scheduleResult.getEmbryoCode() == null ? "" : scheduleResult.getEmbryoCode());
                row.createCell(rowNum++).setCellValue(scheduleResult.getBomDataVersion() == null ? "" : scheduleResult.getBomDataVersion());
                row.createCell(rowNum++).setCellValue(scheduleResult.getTotalStock() == null ? 0 : scheduleResult.getTotalStock());
                row.createCell(rowNum++).setCellValue(scheduleResult.getLhMiddleNightFinishQty() == null ? 0 : scheduleResult.getLhMiddleNightFinishQty());
                row.createCell(rowNum++).setCellValue(scheduleResult.getPlanModifyQty() == null ? 0 : scheduleResult.getPlanModifyQty());
                row.createCell(rowNum++).setCellValue(scheduleResult.getClass3PlannedQty() == null ? 0 : scheduleResult.getClass3PlannedQty());
                row.createCell(rowNum++).setCellValue(scheduleResult.getSingleShiftLhQty() == null ? 0 : scheduleResult.getSingleShiftLhQty());
                row.createCell(rowNum++).setCellValue(scheduleResult.getCxMonthFinishQty() == null ? 0 : scheduleResult.getCxMonthFinishQty());
                row.createCell(rowNum++).setCellValue(scheduleResult.getMonthPlan() == null ? 0 : scheduleResult.getMonthPlan());
                row.createCell(rowNum++).setCellValue(scheduleResult.getPlanModifyQty() == null ? 0 : scheduleResult.getPlanModifyQty());
                row.createCell(rowNum++).setCellValue(scheduleResult.getMonthPlanOs() == null ? 0 : scheduleResult.getMonthPlanOs());
                row.createCell(rowNum++).setCellValue(scheduleResult.getMonthStock() == null ? 0 : scheduleResult.getMonthStock());
                row.createCell(rowNum++).setCellValue(scheduleResult.getRejectQty() == null ? 0 : scheduleResult.getRejectQty());
                row.createCell(rowNum++).setCellValue(scheduleResult.getNewestPlanQty() == null ? 0 : scheduleResult.getNewestPlanQty());
                row.createCell(rowNum++).setCellValue(scheduleResult.getActualOverProduction() == null ? 0 : scheduleResult.getActualOverProduction());
                row.createCell(rowNum++).setCellValue(scheduleResult.getExpectedOverProduction() == null ? 0 : scheduleResult.getExpectedOverProduction());
                row.createCell(rowNum++).setCellValue(scheduleResult.getDifferenceOverProduction() == null ? 0 : scheduleResult.getDifferenceOverProduction());

                row.createCell(rowNum++).setCellValue(scheduleResult.getClass1AvailableLhShift() == null ? 0 : scheduleResult.getClass1AvailableLhShift());
                row.createCell(rowNum++).setCellValue(scheduleResult.getClass1PlanQty() == null ? 0 : scheduleResult.getClass1PlanQty());
                row.createCell(rowNum++).setCellValue(scheduleResult.getClass1FinishQty() == null ? 0 : scheduleResult.getClass1FinishQty());
                String sysAnaly = scheduleResult.getClass1Analysis();
                String handAnaly = scheduleResult.getClass1AnalysisInput();
                String anly = "";
                if (StringUtils.isNotEmpty(sysAnaly)) {
                    anly = anly + sysAnaly;
                }
                if (StringUtils.isNotEmpty(handAnaly)) {
                    if (StringUtils.isNotEmpty(anly)) {
                        anly = anly + "," + handAnaly;
                    } else {
                        anly = handAnaly;
                    }
                }
                row.createCell(rowNum++).setCellValue(anly);

                row.createCell(rowNum++).setCellValue(scheduleResult.getClass2AvailableLhShift() == null ? 0 : scheduleResult.getClass2AvailableLhShift());
                row.createCell(rowNum++).setCellValue(scheduleResult.getClass2PlanQty() == null ? 0 : scheduleResult.getClass2PlanQty());
                row.createCell(rowNum++).setCellValue(scheduleResult.getClass2FinishQty() == null ? 0 : scheduleResult.getClass2FinishQty());
                String sysAnaly2 = scheduleResult.getClass2Analysis();
                String handAnaly2 = scheduleResult.getClass2AnalysisInput();
                String anly2 = "";
                if (StringUtils.isNotEmpty(sysAnaly2)) {
                    anly2 = anly2 + sysAnaly2;
                }
                if (StringUtils.isNotEmpty(handAnaly2)) {
                    if (StringUtils.isNotEmpty(anly2)) {
                        anly2 = anly2 + "," + handAnaly2;
                    } else {
                        anly2 = handAnaly2;
                    }
                }
                row.createCell(rowNum++).setCellValue(anly2);

                row.createCell(rowNum++).setCellValue(scheduleResult.getClass3AvailableLhShift() == null ? 0 : scheduleResult.getClass3AvailableLhShift());
                row.createCell(rowNum++).setCellValue(scheduleResult.getClass3PlanQty() == null ? 0 : scheduleResult.getClass3PlanQty());
                row.createCell(rowNum++).setCellValue(scheduleResult.getClass3FinishQty() == null ? 0 : scheduleResult.getClass3FinishQty());

                String sysAnaly3 = scheduleResult.getClass3Analysis();
                String handAnaly3 = scheduleResult.getClass3AnalysisInput();
                String anly3 = "";
                if (StringUtils.isNotEmpty(sysAnaly3)) {
                    anly3 = anly3 + sysAnaly3;
                }
                if (StringUtils.isNotEmpty(handAnaly3)) {
                    if (StringUtils.isNotEmpty(anly3)) {
                        anly3 = anly3 + "," + handAnaly3;
                    } else {
                        anly3 = handAnaly3;
                    }
                }
                row.createCell(rowNum++).setCellValue(anly3);

                row.createCell(rowNum++).setCellValue(scheduleResult.getClass4AvailableLhShift() == null ? 0 : scheduleResult.getClass4AvailableLhShift());
                row.createCell(rowNum++).setCellValue(scheduleResult.getClass4PlanQty() == null ? 0 : scheduleResult.getClass4PlanQty());
                row.createCell(rowNum++).setCellValue(scheduleResult.getClass4FinishQty() == null ? 0 : scheduleResult.getClass4FinishQty());

                String sysAnaly4 = scheduleResult.getClass4Analysis();
                String handAnaly4 = scheduleResult.getClass4AnalysisInput();
                String anly4 = "";
                if (StringUtils.isNotEmpty(sysAnaly4)) {
                    anly4 = anly4 + sysAnaly4;
                }
                if (StringUtils.isNotEmpty(handAnaly4)) {
                    if (StringUtils.isNotEmpty(anly4)) {
                        anly4 = anly4 + "," + handAnaly4;
                    } else {
                        anly4 = handAnaly4;
                    }
                }
                row.createCell(rowNum++).setCellValue(anly4);

                row.createCell(rowNum++).setCellValue(scheduleResult.getClass5AvailableLhShift() == null ? 0 : scheduleResult.getClass5AvailableLhShift());
                row.createCell(rowNum++).setCellValue(scheduleResult.getClass5PlanQty() == null ? 0 : scheduleResult.getClass5PlanQty());
                row.createCell(rowNum++).setCellValue(scheduleResult.getClass5FinishQty() == null ? 0 : scheduleResult.getClass5FinishQty());

                String sysAnaly5 = scheduleResult.getClass5Analysis();
                String handAnaly5 = scheduleResult.getClass5AnalysisInput();
                String anly5 = "";
                if (StringUtils.isNotEmpty(sysAnaly5)) {
                    anly5 = anly5 + sysAnaly5;
                }
                if (StringUtils.isNotEmpty(handAnaly5)) {
                    if (StringUtils.isNotEmpty(anly5)) {
                        anly5 = anly5 + "," + handAnaly5;
                    } else {
                        anly5 = handAnaly5;
                    }
                }
                row.createCell(rowNum).setCellValue(anly5);

                //设置单元格样式
                int a = row.getPhysicalNumberOfCells();
                for (int j = 0; j < a; j++) {
                    row.getCell(j).setCellStyle(cellStyle);
                }
                CellStyle leftCellStyle = ExcelUtils.getAlignmentLeftCellStyle(webBook);
                row.getCell(2).setCellStyle(leftCellStyle);
                row.getCell(13).setCellStyle(leftCellStyle);
            }

            //合并单元格
            if (!regionMap.isEmpty()) {
                for (Region region : regionMap.values()) {
                    if (region.getLastRow() > region.getFirstRow()) {
                        CellRangeAddress region2 = new CellRangeAddress(region.getFirstRow(), region.getLastRow(), 8, 8);
                        CellRangeAddress region4 = new CellRangeAddress(region.getFirstRow(), region.getLastRow(), 9, 9);
                        sheet.addMergedRegion(region2);
                        sheet.addMergedRegion(region4);
                    }
                }
            }
        }
        //写出字节流
        ByteArrayOutputStream out = null;
        byte[] data = null;
        try {
            out = new ByteArrayOutputStream();
            webBook.write(out);
            data = out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return data;
    }

    /**
     * 给导出的数据增加汇总行
     * @param list
     */
    private void summaryExport(List<CxScheduleResult> list) {
        if(list == null || list.isEmpty()) {
            return;
        }
        CxScheduleResult summary = new  CxScheduleResult();
        summary.setEmbryoCode(I18nUtil.getMessage("ui.data.column.scheduleResult.totalQty"));
        summary.setClass1PlanQty(list.stream().mapToInt(r->getIntOrDefault(r.getClass1PlanQty())).sum());
        summary.setClass1FinishQty(list.stream().mapToInt(r->getIntOrDefault(r.getClass1FinishQty())).sum());
        summary.setClass2PlanQty(list.stream().mapToInt(r->getIntOrDefault(r.getClass2PlanQty())).sum());
        summary.setClass2FinishQty(list.stream().mapToInt(r->getIntOrDefault(r.getClass2FinishQty())).sum());
        summary.setClass3PlanQty(list.stream().mapToInt(r->getIntOrDefault(r.getClass3PlanQty())).sum());
        summary.setClass3FinishQty(list.stream().mapToInt(r->getIntOrDefault(r.getClass3FinishQty())).sum());
        summary.setClass4PlanQty(list.stream().mapToInt(r->getIntOrDefault(r.getClass4PlanQty())).sum());
        summary.setClass4FinishQty(list.stream().mapToInt(r->getIntOrDefault(r.getClass4FinishQty())).sum());
        summary.setClass5PlanQty(list.stream().mapToInt(r->getIntOrDefault(r.getClass5PlanQty())).sum());
        summary.setClass5FinishQty(list.stream().mapToInt(r->getIntOrDefault(r.getClass5FinishQty())).sum());
        list.add(summary);
    }

    /**
     * 合并单元格起始行
     */
    class Region {

        int firstRow;
        int lastRow;
        public Region(int firstRow, int lastRow) {
            this.firstRow = firstRow;
            this.lastRow = lastRow;
        }

        public int getFirstRow() {
            return firstRow;
        }

        public void setFirstRow(int firstRow) {
            this.firstRow = firstRow;
        }

        public int getLastRow() {
            return lastRow;
        }

        public void setLastRow(int lastRow) {
            this.lastRow = lastRow;
        }

    }
    /**
     * 成型自动排程
     */
    @Log(title = "ui.cx.cxScheduleResult.export.fileName", businessType = BusinessType.AUTOPLAN)
    @PostMapping("/autoPlan")
    public AjaxResult autoPlan(@RequestBody CxScheduleResult cxScheduleResult) {
        //执行自动排程算法
        Date scheduleDate = cxScheduleResult.getScheduleDate();
        if(StringUtils.isNotBlank(cxScheduleResult.getCxMachineCode())){
            cxScheduleEngineService.singleMachineAutoSchedule(cxScheduleResult.getCxMachineCode(),scheduleDate);
        }else{
            cxScheduleEngineService.allMachineAutoSchedule(scheduleDate);
        }
        return AjaxResult.success();
    }

    /**
     * 硫化自动排程
     */
    @Log(title = "ui.data.column.lh.scheduleResult.modelName", businessType = BusinessType.AUTOPLAN)
    @PostMapping("/lhAutoPlan")
    public AjaxResult lhAutoPlan(@RequestBody CxScheduleResult cxScheduleResult) {
        //执行硫化自动排程算法
        Date scheduleDate = cxScheduleResult.getScheduleDate();
        int releasingOrTimeoutByDate = cxScheduleResultService.lhIsReleasingOrTimeoutByDate(cxScheduleResult.getScheduleDate());
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        lhEngineService.autoLhSchedule(scheduleDate);
        return AjaxResult.success();
    }

    /**
     * 生成模具变动单校验
     */
    @PostMapping("/modelChangeValidate")
    public AjaxResult modelChangeValidate(@RequestBody CxScheduleResult cxScheduleResult) {
        int releasingOrTimeoutByDate = cxScheduleResultService.isReleasingOrTimeoutByDate(cxScheduleResult.getScheduleDate());
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        return cxScheduleResultService.modelChangeValidate(cxScheduleResult);
    }

    /**
     * 生成模具变动单
     */
    @Log(title = "ui.cx.cxScheduleResult.export.fileName", businessType = BusinessType.auto_mold_change)
    @PostMapping("/modelChange")
    public AjaxResult modelChange(@RequestBody CxScheduleResult cxScheduleResult) {
        //执行模具变动单算法
        Date scheduleDate = cxScheduleResult.getScheduleDate();
        if (scheduleDate == null) {
            return AjaxResult.error();
        }
        String scheduleDateStr = DateUtils.parseDateToStr("yyyy-MM-dd", scheduleDate);
        String msg = lhEngineService.moldChangePlanTask(scheduleDateStr);
        if (StringUtils.isNotEmpty(msg)) {
            return AjaxResult.error(msg);
        }
        return AjaxResult.success();
    }

    /**
     * 生成模具调整计划
     */
    @Log(title = "ui.cx.cxScheduleResult.export.fileName", businessType = BusinessType.auto_mold_change)
    @PostMapping("/modelAdjustPlan")
    public AjaxResult modelAdjustPlan(@RequestBody CxScheduleResult cxScheduleResult) {
        //获取数据版本号
        String dataVersion = syncDataHandle.getDataVersion(ApsConstant.LH_MOLD_ADJUST_PLAN);
        // 厂别、分公司编号
        String factoryCode = factoryService.getFactoryCode();
        String companyCode = factoryService.getCompanyCode();

        Date scheduleDate=cxScheduleResult.getScheduleDate();
        AjaxResult ajaxResult = null;
        try{
            SyncParamsVO syncParamsVO = new SyncParamsVO();
            syncParamsVO.setSyncKey(ApsConstant.LH_MOLD_ADJUST_PLAN);
            syncParamsVO.setDataVersion(dataVersion);
            // 请求参数
            JSONObject params = new JSONObject();
            params.put("startDate", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, scheduleDate) + " 00:00:00");
            params.put("endDate", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, scheduleDate) + " 23:59:59");
            syncParamsVO.setParams(params);
            syncParamsVO.setFactoryCode(factoryCode);
            syncParamsVO.setCompanyCode(companyCode);
            //Joran 2022-03-24 调整为请求调用
            syncDataHandle.syncRequest(syncParamsVO);

            // 取回mes的反馈结果
            SyncDataLogs logs = syncDataLogsService.getSyncDataResult(dataVersion);
            String status = logs.getStatus();
            if (ApsConstant.IS_RELEASE.equals(status)) {
                // 成功
                ajaxResult = AjaxResult.success();
            } else {
                // 失败，需要返回异常信息
                ajaxResult = AjaxResult.error(logs.getMsg());
            }
        }catch (Exception e){
            //异常时进行堆栈内容打印
            e.printStackTrace();
            ajaxResult=AjaxResult.error(I18nUtil.getMessage("ui.data.column.cxScheduleResult.modelChange") + I18nUtil.getMessage("ui.biz.oper.fail"));
        }
        return ajaxResult;
    }

    /**
     * 排程发布校验
     */
    @PostMapping("/publishValidate")
    public AjaxResult publishValidate(@RequestBody CxScheduleResult cxScheduleResult) {
        //Joran 2022-03-16添加发布选中记录施工校验
        Long[] ids =cxScheduleResult.getIds();
        String msg=cxScheduleResultService.validateConstructionByIds(ids);
        if(StringUtils.isNotEmpty(msg)){
            return AjaxResult.error(msg);
        }
        int releasingOrTimeoutByIds = cxScheduleResultService.isReleasingOrTimeoutByIds(cxScheduleResult.getIds());
        if (releasingOrTimeoutByIds > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        //查询排程发布list,过滤出未发布及发布失败的记录
//        startPage("a.cx_machine_code,a.embryo_code,a.sap_code asc");
        List<CxScheduleResult> list = cxScheduleResultService.selectCxScheduleResultList(cxScheduleResult).stream()
                .filter(item -> ApsConstant.NO_RELEASE.equals(item.getIsRelease()) || ApsConstant.FAILURE_RELEASE.equals(item.getIsRelease()) || ApsConstant.WAIT_RELEASING.equals(item.getIsRelease())).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.errorPublish"));
        }

        //Joran 2021-12-04 发布前校验如果存在施工版本为空的不允许发布
        msg=cxScheduleResultService.checkBomDataVersion(list);
        if(StringUtils.isNotEmpty(msg)){
            return AjaxResult.error(msg);
        }
        //校验硫化机台
        List<CxScheduleResult> collect = list.stream().filter(item -> StringUtil.isBlank(item.getLhMachineCode())).collect(Collectors.toList());
        if (collect.size() > 0) {
            return AjaxResult.success("0");
        }
        return AjaxResult.success();
    }

    /**
     * 排程发布
     */
    @Log(title = "ui.cx.cxScheduleResult.export.fileName", businessType = BusinessType.PUBLISH)
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody CxScheduleResult cxScheduleResult) {
        // 发布前需要先获得同步锁，防止在集群环境下出现一个前端命令发送两次mes请求，modify by hak 20220708
        if (syncDataLogsService.checkPublishLocking("cx:publish:lock", cxScheduleResult.getIds())) {
            return AjaxResult.success(); // 如果已经被锁定了，则直接返回
        }

        //获取数据版本号
        String dataVersion = syncDataHandle.getDataVersion(ApsConstant.CX_DEPLOY_SYNC_KEY);
        // 厂别、分公司编号
        String factoryCode = factoryService.getFactoryCode();
        String companyCode = factoryService.getCompanyCode();

        //查询排程发布list
//        cxScheduleResult.setIsRelease(ApsConstant.NO_RELEASE);

        //Joran 2022-03-08 没有施工版本的不允许发布
        cxScheduleResult.setHasVersion(0);
        cxScheduleResult.setToProduct(CxEngineConstants.TO_PRODUCT_YES);

//        startPage("a.cx_machine_code,a.embryo_code,a.sap_code asc");
        List<CxScheduleResult> list = cxScheduleResultService.selectCxScheduleResultList(cxScheduleResult).stream()
                .filter(item -> ApsConstant.NO_RELEASE.equals(item.getIsRelease()) || ApsConstant.FAILURE_RELEASE.equals(item.getIsRelease()) || ApsConstant.WAIT_RELEASING.equals(item.getIsRelease())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.hasNotVersionOrNotToProduct"));
        }

        Date scheduleDate=cxScheduleResult.getScheduleDate();
        AjaxResult ajaxResult=null;
        //排程发布
        long[] arr = list.stream().mapToLong(item -> item.getId()).toArray();
        try{
            ajaxResult=cxScheduleResultService.publish(arr,scheduleDate,dataVersion,factoryCode,companyCode);
            SyncParamsVO syncParamsVO = new SyncParamsVO();
            syncParamsVO.setSyncKey(ApsConstant.CX_DEPLOY_SYNC_KEY);
            syncParamsVO.setDataVersion(dataVersion);
            // 请求参数
            JSONObject params = new JSONObject();
            params.put("scheduleDate", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, scheduleDate));
            params.put("rowCount", arr.length);
            syncParamsVO.setParams(params);
            syncParamsVO.setFactoryCode(factoryCode);
            syncParamsVO.setCompanyCode(companyCode);
            syncDataHandle.syncNotice(syncParamsVO);

            // 取回mes的反馈结果
            SyncDataLogs logs = syncDataLogsService.getSyncDataResult(dataVersion);
            String status = logs.getStatus();
            // 更新状态
            cxScheduleResultService.updateRelaseStatus(dataVersion, arr, status);
            if (ApsConstant.IS_RELEASE.equals(status)) {
                // 成功
                ajaxResult = AjaxResult.success();
            } else {
                // 失败，需要返回异常信息
                ajaxResult = AjaxResult.error(logs.getMsg());
            }
        }catch (Exception e){
            //异常时进行堆栈内容打印
            e.printStackTrace();
            ajaxResult=AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.failedPublish"));
        }
        return ajaxResult;

    }

    /**
     * 查询成型排程日期是否已发布
     *
     * @return 是否已经发布
     */
    @PostMapping("/isCxPublish")
    public Boolean isCxPublish(@RequestBody CxScheduleResult entity) {
        return cxScheduleResultService.isCxPublish(entity.getScheduleDate());
    }

    /**
     * 查询硫化排程日期是否已发布
     *
     * @return 是否已经发布
     */
    @PostMapping("/isLhPublish")
    public Boolean isLhPublish(@RequestBody CxScheduleResult entity) {
        return cxScheduleResultService.isLhPublish(entity.getScheduleDate());
    }

    /**
     * 导入数据
     */
    @Log(title = "ui.cx.cxScheduleResult.export.fileName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<CxScheduleResult> list, @RequestParam("importLogId") Long importLogId,@RequestParam("scheduleDate")String scheduleDate) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        AjaxResult result=null;
        try {
            result=cxScheduleResultService.importData(list, importLogId,scheduleDate);
        } catch (Exception e) {
            e.printStackTrace();
            result=AjaxResult.error(e.getMessage().replaceAll(",", "，"));
        }
        return result;
    }

    /**
     * 获取-使用模数
     */
    @PostMapping("/getMolds")
    public CxScheduleResult getMolds(@RequestBody CxScheduleResult cxScheduleResult){
        return cxScheduleResultService.getMolds(cxScheduleResult);
    }

    /**
     * 校验-使用模数
     */
    @PostMapping("/modifyMoldsValidate")
    public AjaxResult modifyMoldsValidate(@RequestBody CxScheduleResult cxScheduleResult){
        int releasingOrTimeoutByDate = cxScheduleResultService.isReleasingOrTimeoutByIds(new Long[]{cxScheduleResult.getId()});
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        return cxScheduleResultService.modifyMoldsValidate(cxScheduleResult);
    }

    /**
     * 修改-使用模数
     */
    @Log(title = "ui.cx.cxScheduleResult.export.fileName", businessType = BusinessType.UPDATE_MOLDS)
    @PostMapping("/modifyMolds")
    AjaxResult modifyMolds(@RequestBody CxScheduleResult cxScheduleResult){
        return cxScheduleResultService.modifyMolds(cxScheduleResult);
    }

    /**
     * 在产下发MPS
     */
    @Log(title = "ui.cx.cxScheduleResult.export.fileName", businessType = BusinessType.PRODUCING_ISSUE)
    @PostMapping("/producingIssue")
    public AjaxResult producingIssue(@RequestBody CxScheduleResult entity){
        return cxScheduleResultService.producingIssue(entity);
    }

    /**
     * 单机自动排程校验
     */
    @PostMapping("/singleMachinAutoPlanValidate")
    public List<CxScheduleResult> singleMachinAutoPlanValidate(@RequestBody CxScheduleResult entity){
        return cxScheduleResultService.singleMachinAutoPlanValidate(entity);
    }

    /**
     * 增补计划校验
     * @param entity 校验日期
     * @return 结果
     */
    @PostMapping("/autoScheduleValidateSupplePlanByScheduleDate")
    public AjaxResult autoScheduleValidateSupplePlanByScheduleDate(@RequestBody CxScheduleResult entity) {
        ValidateResult validateResult = cxScheduleEngineService.autoScheduleValidateSupplePlanByScheduleDate(entity.getScheduleDate());
        return validateResult.isSuccess() ? AjaxResult.success(validateResult.getMsg()) : AjaxResult.error(validateResult.getMsg());
    }

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录(硫化排程结果)
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @PostMapping("/isReleasingOrTimeoutByDate")
    public int isReleasingOrTimeoutByDate(@RequestBody CxScheduleResult scheduleResult){
        return cxScheduleResultService.isReleasingOrTimeoutByDate(scheduleResult.getScheduleDate());
    }

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录(硫化排程结果)
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @PostMapping("/lhIsReleasingOrTimeoutByDate")
    public int lhIsReleasingOrTimeoutByDate(@RequestBody CxScheduleResult scheduleResult){
        return cxScheduleResultService.lhIsReleasingOrTimeoutByDate(scheduleResult.getScheduleDate());
    }

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param ids id
     * @return 查询到的记录数
     */
    @PostMapping("/isReleasingOrTimeoutByIds/{ids}")
    public int isReleasingOrTimeoutByIds(@PathVariable Long[] ids){
        return cxScheduleResultService.isReleasingOrTimeoutByIds(ids);
    }

    /**
     * 更改发布状态
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @Log(title = "ui.data.column.tmScheduleResult.modalName")
    @PostMapping("/changeReleaseStatus")
    public AjaxResult changeReleaseStatus(@RequestBody CxScheduleResult entity){
        cxScheduleResultService.changeReleaseStatus(entity);
        return AjaxResult.success();
    }

    /**
     * 验证施工信息
     * @return 结果
     */
    @PostMapping("/validateConstructionByIds")
    public AjaxResult validateConstructionByIds(@RequestParam("ids") Long[] ids){
        String msg=cxScheduleResultService.validateConstructionByIds(ids);
        if(StringUtils.isNotEmpty(msg)){
            return AjaxResult.error(msg);
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.data.column.cxScheduleResult.validateSuccess"));
    }

    /**
     * 查询成型排程最新排程日期
     * @return 最新排程日期
     */
    @PostMapping("/selectMaxScheduleDate")
    public String selectMaxScheduleDate() {
        return DateUtils.parseDateToStr("yyyy-MM-dd",cxScheduleResultService.selectMaxScheduleDate());
    }

    /**
     * 查询成型排程硫化机台更换类型集合
     * @param cxChangeLhMachine 参数：工单号
     */
    @PostMapping("/listCxChangeLhMachine")
    public TableDataInfo listCxChangeLhMachine(@RequestBody CxChangeLhMachine cxChangeLhMachine){
        List<CxChangeLhMachine> list = cxEngineChangeLhMachineService.listChangeLhMachineList(cxChangeLhMachine);
        return getDataTable(list);
    }

    /**
     * 根据胎胚代码查询所在组别，再根据所在组别查询所有胎胚代码（除传入胎胚代码）
     * @param cxShareMoldInfoGroupNameMap 公用模具组别map key:组别 value:当前组别下胎胚代码(,分割)
     * @param cxShareMoldInfoEmbryoCodeMap 公用模具胎胚map key:胎胚代码 value:当前胎胚代码下所在组别(,分割)
     * @param embryoCode 要查询的胎胚代码
     * @return 查询到的所有胎胚代码（除本身）
     */
    private List<String> getEmbryoCodesByEmbryoCode(Map<String, String> cxShareMoldInfoGroupNameMap, Map<String, String> cxShareMoldInfoEmbryoCodeMap, String embryoCode){
        List<String> list = new ArrayList<>();
        String groups = cxShareMoldInfoEmbryoCodeMap.getOrDefault(embryoCode,"");
        String[] groupArr = groups.split(",");
        for (String group : groupArr) {
            String embryoCodes = cxShareMoldInfoGroupNameMap.getOrDefault(group, "");
            String[] embryoCodeArr = embryoCodes.split(",");
            for (String embryoCodeStr : embryoCodeArr) {
                if (!embryoCode.equals(embryoCodeStr) && StringUtil.isNotBlank(embryoCodeStr)) {
                    list.add(embryoCodeStr);
                }
            }
        }
        return list;
    }

    /**
     * 根据sap品号查询所属组别对应的所有sap品号信息（排除传入sap品号）
     * @param sapCode sap品号
     * @return 查询到的共用模具信息
     */
    private List<String> getEmbryoCodesBySapCode(Map<String, String> cxShareMoldInfoGroupNameMap, Map<String, String> cxShareMoldInfoSapCodeMap, String sapCode){
        List<String> list = new ArrayList<>();
        String groups = cxShareMoldInfoSapCodeMap.getOrDefault(sapCode,"");
        String[] groupArr = groups.split(",");
        for (String group : groupArr) {
            String sapCodes = cxShareMoldInfoGroupNameMap.getOrDefault(group, "");
            String[] sapCodeArr = sapCodes.split(",");
            for (String sapCodeStr : sapCodeArr) {
                if (!sapCode.equals(sapCodeStr) && StringUtil.isNotBlank(sapCodeStr)) {
                    list.add(sapCodeStr);
                }
            }
        }
        return list;
    }

    /**
     * 根据共用模集合填充对应的共用模信息map
     * @param shareMoldInfoList 共用模集合
     * @param cxShareMoldInfoGroupNameMap 共用模组别map
     * @param cxShareMoldInfoSapCodeMap 共用模sapMap
     */
    private void putShareMoldInfoMap(List<CxShareMoldInfo> shareMoldInfoList, Map<String, String> cxShareMoldInfoGroupNameMap, Map<String, String> cxShareMoldInfoSapCodeMap) {
        for (CxShareMoldInfo cxShareMoldInfo : shareMoldInfoList) {
            if (cxShareMoldInfoGroupNameMap.containsKey(cxShareMoldInfo.getGroupName())) {
                String sapCode = cxShareMoldInfoGroupNameMap.get(cxShareMoldInfo.getGroupName());
                cxShareMoldInfoGroupNameMap.put(cxShareMoldInfo.getGroupName(), sapCode + "," + cxShareMoldInfo.getSapCode());
            }else {
                cxShareMoldInfoGroupNameMap.put(cxShareMoldInfo.getGroupName(), cxShareMoldInfo.getSapCode());
            }
            if (cxShareMoldInfoSapCodeMap.containsKey(cxShareMoldInfo.getSapCode())) {
                String groupNames = cxShareMoldInfoSapCodeMap.get(cxShareMoldInfo.getSapCode());
                cxShareMoldInfoSapCodeMap.put(cxShareMoldInfo.getSapCode(), groupNames + "," + cxShareMoldInfo.getGroupName());
            }else {
                cxShareMoldInfoSapCodeMap.put(cxShareMoldInfo.getSapCode(), cxShareMoldInfo.getGroupName());
            }
        }
    }

    /**
     * 查询成型排程机台甘特图数据
     */
    @PostMapping("/getCxGanteData")
    public List<Gante> getCxGanteData(@RequestBody Gante gante){
        return  cxScheduleResultService.getCxGanteData(gante);
    }


}

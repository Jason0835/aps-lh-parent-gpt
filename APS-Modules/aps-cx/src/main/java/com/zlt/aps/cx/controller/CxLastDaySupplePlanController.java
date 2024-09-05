package com.zlt.aps.cx.controller;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.engine.domain.TCxMonthPlanSurplus;
import com.zlt.aps.common.engine.service.TCxEmbryoMonthPlanSurplusService;
import com.zlt.aps.common.engine.service.TCxMonthPlanSurplusService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.cx.api.domain.dto.CxLastDaySupplePlanDto;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cx.api.domain.entity.CxScheduleStopInfo;
import com.zlt.aps.cx.engine.exception.CxScheduleEngineException;
import com.zlt.aps.cx.engine.service.CxEngineLastDaySupplePlanService;
import com.zlt.aps.cx.engine.service.CxScheduleEngineService;
import com.zlt.aps.cx.service.CxLastDaySupplePlanService;
import com.zlt.aps.cx.service.CxScheduleStopInfoService;
import com.zlt.aps.cx.service.CxShareMoldInfoService;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 成型前日计划增补Controller
 *
 * @author chen
 * @date 2022-02-09
 */
@RestController
@RequestMapping("/lastDaySupplyPlan")
public class CxLastDaySupplePlanController extends BaseController {
    @Autowired
    private CxLastDaySupplePlanService cxLastDaySupplePlanService;

    @Autowired
    private CxScheduleEngineService cxScheduleEngineService;

    @Autowired
    private CxEngineLastDaySupplePlanService cxEngineLastDaySupplePlanService;

    @Autowired
    private CxShareMoldInfoService cxShareMoldInfoService;
    @Autowired
    private TCxEmbryoMonthPlanSurplusService cxEmbryoMonthPlanSurplusService;
    @Autowired
    private TCxMonthPlanSurplusService tCxMonthPlanSurplusService;
    @Autowired
    private CxScheduleStopInfoService cxScheduleStopInfoService;

    /**
     * 查询成型前日计划增补列表
     */
    @ApiOperation("查询成型前日计划增补列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody CxLastDaySupplePlanDto cxLastDaySupplePlan) {
        List<CxLastDaySupplePlanDto> list = cxLastDaySupplePlanService.selectCxLastDaySupplePlanList(cxLastDaySupplePlan);

        // 获取所有胎胚对应的月度剩余量，用于页面悬浮展示胎胚共用模具信息
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTime(cxLastDaySupplePlan.getScheduleDate());
//        Map<String, BigDecimal> embryoCodeMonthRemainQtyMap = cxEmbryoMonthPlanSurplusService.selectMonthRemainQtyByYearAndMonthGroupByMaterialCode(String.valueOf(calendar.get(Calendar.YEAR)), String.valueOf(calendar.get(Calendar.MONTH)) + 1);
//
//        List<CxShareMoldInfo> shareMoldInfoList = cxShareMoldInfoService.selectCxShareMoldInfoList(new CxShareMoldInfo());
//        Map<String, String> cxShareMoldInfoGroupNameMap = new HashMap<>();
//        Map<String, String> cxShareMoldInfoEmbryoCodeMap = new HashMap<>();
//        for (CxShareMoldInfo cxShareMoldInfo : shareMoldInfoList) {
//            if (cxShareMoldInfoGroupNameMap.containsKey(cxShareMoldInfo.getGroupName())) {
//                String embryoCodes = cxShareMoldInfoGroupNameMap.get(cxShareMoldInfo.getGroupName());
//                cxShareMoldInfoGroupNameMap.put(cxShareMoldInfo.getGroupName(), embryoCodes + "," + cxShareMoldInfo.getEmbryoCode());
//            }else {
//                cxShareMoldInfoGroupNameMap.put(cxShareMoldInfo.getGroupName(), cxShareMoldInfo.getEmbryoCode());
//            }
//            if (cxShareMoldInfoEmbryoCodeMap.containsKey(cxShareMoldInfo.getEmbryoCode())) {
//                String groupNames = cxShareMoldInfoEmbryoCodeMap.get(cxShareMoldInfo.getEmbryoCode());
//                cxShareMoldInfoEmbryoCodeMap.put(cxShareMoldInfo.getEmbryoCode(), groupNames + "," + cxShareMoldInfo.getGroupName());
//            }else {
//                cxShareMoldInfoEmbryoCodeMap.put(cxShareMoldInfo.getEmbryoCode(), cxShareMoldInfo.getGroupName());
//            }
//        }
        // 获取所有外胎计划汇总数据，用于回显共用模具信息
        TCxMonthPlanSurplus entity = new TCxMonthPlanSurplus();
        entity.setYear(DateFormatUtils.format(cxLastDaySupplePlan.getScheduleDate(),"yyyy"));
        entity.setMonth(DateFormatUtils.format(cxLastDaySupplePlan.getScheduleDate(),"MM"));
        Map<String, Integer> cxMonthPlanSurplusMap = tCxMonthPlanSurplusService.getByParams(entity).stream()
                .collect(Collectors.toMap(TCxMonthPlanSurplus::getSapCode, TCxMonthPlanSurplus::getMonthRemainQty));
        // 获取成型机台自动停排信息
        CxScheduleStopInfo stopInfo = new CxScheduleStopInfo();
        stopInfo.setScheduleDate(cxLastDaySupplePlan.getScheduleDate());
        Map<String, Integer> stopInfoMap = cxScheduleStopInfoService.selectCxScheduleStopInfoList(stopInfo).stream().collect(Collectors.toMap(CxScheduleStopInfo::getCxMachineCode, CxScheduleStopInfo::getClassShift));

        //颜色设置
        Map<String, Long> sapCodeCountMap = list.stream().collect(Collectors.groupingBy(CxScheduleResult::getSapCode, Collectors.counting()));
        Map<String, Long> embryoCodeCountMap = list.stream().collect(Collectors.groupingBy(CxScheduleResult::getEmbryoCode, Collectors.counting()));
        Map<String, Integer> lhMachineCountMap = new HashMap<>();

        //Map<类型+机台,最小寸口>
        Map<String, Double> typeAndCodeMap = new HashMap<>();

        list.forEach(a -> {
            if (StringUtils.isNotBlank(a.getLhMachineCode())) {
                String[] machines = a.getLhMachineCode().split(",");
                for (String machine : machines) {
                    if (lhMachineCountMap.containsKey(machine)) {
                        lhMachineCountMap.put(machine, lhMachineCountMap.get(machine) + 1);
                    } else {
                        lhMachineCountMap.put(machine, 1);
                    }
                }
            }
            if (StringUtils.isNotBlank(a.getSapCode()) && sapCodeCountMap.get(a.getSapCode()) > 1) {
                a.setColorForSapCode(sapCodeCountMap.get(a.getSapCode()));
            }
            if (StringUtils.isNotBlank(a.getEmbryoCode()) && embryoCodeCountMap.get(a.getEmbryoCode()) > 1) {
                a.setColorForEmbryoCode(embryoCodeCountMap.get(a.getEmbryoCode()));
            }

            String typeAndCode = a.getCxMachineType() + a.getCxMachineCode();
            Double specDimension = a.getSpecDimension();
            if (!typeAndCodeMap.containsKey(typeAndCode)) {
                typeAndCodeMap.put(typeAndCode, specDimension);
            }

        });
        list.forEach(a -> {
            if (StringUtils.isNotBlank(a.getLhMachineCode())) {
                String[] machines = a.getLhMachineCode().split(",");
                for (String machine : machines) {
                    if (lhMachineCountMap.containsKey(machine) && lhMachineCountMap.get(machine) > 1) {
                        a.setColorForLhMachine(lhMachineCountMap.get(machine));
                    }
                }
            }
            String typeAndCode = a.getCxMachineType() + a.getCxMachineCode();
            a.setOrderByStr(a.getCxMachineType() + typeAndCodeMap.get(typeAndCode) + a.getCxMachineName() + a.getId());

            // 页面胎胚代码悬浮显示共用模具信息
            StringBuilder shareMoldInfoStr = new StringBuilder();
//            List<String> embryoCodes = getEmbryoCodesByEmbryoCode(cxShareMoldInfoGroupNameMap, cxShareMoldInfoEmbryoCodeMap, a.getEmbryoCode());
//            for (String embryoCode : embryoCodes) {
//                BigDecimal monthRemainQty = embryoCodeMonthRemainQtyMap.get(embryoCode);
//                if (ObjectUtils.isEmpty(monthRemainQty)) {
//                    monthRemainQty = BigDecimal.valueOf(0);
//                }
//                shareMoldInfoStr.append(embryoCode)
//                        .append(I18nUtil.getMessage("ui.data.column.scheduleResult.monthRemainQty")).append(":")
//                        .append(monthRemainQty).append(";<br>");
//            }
            Integer monthPlanOs = cxMonthPlanSurplusMap.getOrDefault(a.getSapCode(), 0);
            if (StringUtil.isNotBlank(a.getSapCode())) {
                shareMoldInfoStr.append(a.getSapCode()).append(",")
                        .append(I18nUtil.getMessage("ui.data.column.hover.monthRemainQty"))
                        .append(monthPlanOs).append(",")
                        .append(I18nUtil.getMessage("ui.data.column.hover.rejectQty")).append(Objects.toString(a.getRejectQty(), "0"));
            }
            a.setShareMoldInfoStr(shareMoldInfoStr.toString());
            // 页面停排成型机颜色提示
            Integer classShift = stopInfoMap.get(a.getCxMachineCode());
            if (ObjectUtils.isNotEmpty(classShift)) {
                a.setScheduleStop(1);
                a.setStopClassShift(classShift);
            }
        });

        List<CxScheduleResult> newList = list.stream().sorted(Comparator.comparing(CxScheduleResult::getOrderByStr)).collect(Collectors.toList());

        return getDataTable(newList);
    }

    /**
     * 根据id查询成型前日计划增补
     */
    @ApiOperation("根据id查询成型前日计划增补")
    @PostMapping("/changeMachine/{id}")
    public CxLastDaySupplePlanDto getInfo(@PathVariable("id") Long id) {
        return cxLastDaySupplePlanService.getInfo(id);
    }

    /**
     * 修改成型前日计划增补
     */
    @Log(title = "ui.data.column.cx.lastDaySupplyPlan.modalName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改成型前日计划增补")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody CxLastDaySupplePlanDto cxLastDaySupplePlan) {
        // 状态已确认则无法修改
        if (ApsConstant.STATUS_DISABLE.equals(cxLastDaySupplePlan.getStatus())) {
            return AjaxResult.error(I18nUtil.getMessage("cx.lastDaySupplePlan.message.confirmed"));
        }
        return toAjax(cxLastDaySupplePlanService.updateCxLastDaySupplePlan(cxLastDaySupplePlan));
    }

    /**
     * 修改成型前日计划增补机台
     */
    @Log(title = "ui.data.column.cx.lastDaySupplyPlan.modalName", businessType = BusinessType.CHANGE_MACHINE)
    @ApiOperation("修改成型前日计划增补机台")
    @PostMapping("/changeMachine")
    public AjaxResult changeMachine(@RequestBody CxLastDaySupplePlanDto cxLastDaySupplePlan) {
        // 状态已确认则无法修改
        if (ApsConstant.STATUS_DISABLE.equals(cxLastDaySupplePlan.getStatus())) {
            return AjaxResult.error(I18nUtil.getMessage("cx.lastDaySupplePlan.message.confirmed"));
        }
        return toAjax(cxLastDaySupplePlanService.updateCxLastDaySupplePlan(cxLastDaySupplePlan));
    }

    /**
     * 删除成型前日计划增补
     */
    @Log(title = "ui.data.column.cx.lastDaySupplyPlan.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("删除成型前日计划增补")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        String  statusStr = cxLastDaySupplePlanService.selectStatusByIds(ids);
        if (statusStr != null && statusStr.contains(ApsConstant.STATUS_DISABLE)) {
            return AjaxResult.error(I18nUtil.getMessage("cx.lastDaySupplePlan.message.confirmed"));
        }
        return toAjax(cxLastDaySupplePlanService.deleteCxLastDaySupplePlanByIds(ids));
    }

    /**
     * 生成成型前日计划增补
     */
    @Log(title = "ui.data.column.cx.lastDaySupplyPlan.modalName", businessType = BusinessType.GENERATE_SUPPLEMENT_PLAN)
    @ApiOperation("生成成型前日计划增补")
    @PostMapping("/generateSupplyPlan")
    public AjaxResult generateSupplyPlan(@RequestBody CxLastDaySupplePlanDto cxLastDaySupplePlanDto) {
        try {
            cxScheduleEngineService.createSupplePlanTask(cxLastDaySupplePlanDto.getScheduleDate());
            //若成型机台全部收尾，则给予提示 add by pancd+ 20230831
            return checkMachineAllClose(cxLastDaySupplePlanDto.getScheduleDate());
        } catch (CxScheduleEngineException e) {
            e.printStackTrace();
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 生成成型前日计划增补
     */
    @Log(title = "ui.data.column.cx.lastDaySupplyPlan.modalName", businessType = BusinessType.GENERATE_SUPPLEMENT_PLAN)
    @ApiOperation("重新生成成型前日计划增补")
    @PostMapping("/regenerateSupplyPlan")
    public AjaxResult regenerateSupplyPlan(@RequestBody CxLastDaySupplePlanDto cxLastDaySupplePlanDto) {
        try {
            cxScheduleEngineService.reCreateSupplePlanTask(cxLastDaySupplePlanDto.getScheduleDate());
            //若成型机台全部收尾，则给予提示 add by pancd+ 20230831
            return checkMachineAllClose(cxLastDaySupplePlanDto.getScheduleDate());
        } catch (CxScheduleEngineException e) {
            e.printStackTrace();
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 检查所有规格是否全部收尾
     * @param suppleDate 增补日期
     * @return
     */
    private AjaxResult checkMachineAllClose(Date suppleDate) {
        String suppleDateStr= DateUtils.parseDateToStr("yyyyMMdd", suppleDate);
        List<String> closeMachineList = cxEngineLastDaySupplePlanService.selectAllCloseMachine(suppleDateStr);
        if (!CollectionUtil.isEmpty(closeMachineList)) {
            String message = I18nUtil.getMessage("ui.data.column.machine.allClose");
            message=String.format(message,closeMachineList.toString());
            return AjaxResult.error(message);
        }
        return AjaxResult.success();
    }

    /**
     * 确认成型前日计划增补
     */
    @Log(title = "ui.data.column.cx.lastDaySupplyPlan.modalName", businessType = BusinessType.CONFIRM_SUPPLEMENT_PLAN)
    @ApiOperation("确认成型前日计划增补")
    @PostMapping("/confirmSupplyPlan")
    public AjaxResult confirmSupplyPlan(@RequestBody CxLastDaySupplePlanDto cxLastDaySupplePlanDto) {
        try {
            cxScheduleEngineService.autoSuppleScheduleTask(cxLastDaySupplePlanDto.getScheduleDate());
        } catch (CxScheduleEngineException e) {
            e.printStackTrace();
            return AjaxResult.error(e.getMessage());
        }
        return AjaxResult.success();
    }

    /**
     * 新增成型前日增补计划
     *
     * @param cxScheduleResult 前日增补计划
     * @return 结果
     */
    @Log(title = "ui.data.column.cx.lastDaySupplyPlan.modalName", businessType = BusinessType.INSERT)
    @ApiOperation("确新增成型前日计划增补")
    @PostMapping("/insertCxLastDaySupplePlan")
    public AjaxResult insertCxLastDaySupplePlan(@RequestBody CxLastDaySupplePlanDto cxLastDaySupplePlan) {
        return cxLastDaySupplePlanService.insertCxLastDaySupplePlan(cxLastDaySupplePlan);
    }

    /**
     * 校验-使用模数
     */
    @PostMapping("/modifyMoldsValidate")
    public AjaxResult modifyMoldsValidate(@RequestBody CxLastDaySupplePlanDto lastDaySupplePlanDto){
        int releasingOrTimeoutByDate = cxLastDaySupplePlanService.isReleasingOrTimeoutByIds(new Long[]{lastDaySupplePlanDto.getId()});
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        return cxLastDaySupplePlanService.modifyMoldsValidate(lastDaySupplePlanDto);
    }

    /**
     * 修改-使用模数
     */
    @Log(title = "ui.data.column.cx.lastDaySupplyPlan.modalName", businessType = BusinessType.UPDATE_MOLDS)
    @PostMapping("/modifyMolds")
    AjaxResult modifyMolds(@RequestBody CxLastDaySupplePlanDto lastDaySupplePlanDto){
        return cxLastDaySupplePlanService.modifyMolds(lastDaySupplePlanDto);
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
}

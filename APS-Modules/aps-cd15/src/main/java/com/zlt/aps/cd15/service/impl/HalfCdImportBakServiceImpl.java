package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.HalfCdImportBak;
import com.zlt.aps.cd15.api.domain.entity.HalfCdImportBakExportData;
import com.zlt.aps.cd15.api.domain.vo.HalfCdImportBakExportVo;
import com.zlt.aps.cd15.engine.service.Cd15EngineService;
import com.zlt.aps.cd15.enums.CdMachineExportEnums;
import com.zlt.aps.cd15.mapper.HalfCdImportBakEntityMapper;
import com.zlt.aps.cd15.service.IHalfCdImportBakExportDataService;
import com.zlt.aps.cd15.service.IHalfCdImportBakService;
import com.zlt.aps.cd90.engine.service.Cd90EngineService;
import com.zlt.aps.common.engine.enums.OpenMachineClassEnums;
import com.zlt.aps.gsq.engine.service.GsqEngineService;
import com.zlt.aps.nc.engine.service.NcEngineService;
import com.zlt.aps.tq.engine.service.TqEngineService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：HalfCdImportBakServiceImpl.java
 * 描    述：HalfCdImportBakServiceImpl裁断线下计划导入导出业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-05-29
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class HalfCdImportBakServiceImpl extends ServiceImpl<HalfCdImportBakEntityMapper, HalfCdImportBak> implements IHalfCdImportBakService {

    private final List<String> lbMachineNameList = Arrays.asList("1#直裁", "2#直裁", "3#直裁", "4#直裁");
    private final List<String> cdMachineNameList = Arrays.asList("1#机", "2#机", "3#机", "4#机");
    private final List<String> tqMachineNameList = Arrays.asList("1号", "2号", "3号", "4号", "5号", "7号", "8号", "9号", "12号", "13号");
    @Autowired
    private HalfCdImportBakEntityMapper entityMapper;
    @Autowired
    private IHalfCdImportBakExportDataService exportDataService;
    @Autowired
    private NcEngineService ncEngineService;
    @Autowired
    private Cd15EngineService cd15EngineService;
    @Autowired
    private Cd90EngineService cd90EngineService;
    @Autowired
    private TqEngineService tqEngineService;
    @Autowired
    private GsqEngineService gsqEngineService;

    @Override
    public AjaxResult importData(List<HalfCdImportBak> list) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.data.empty"));
        }
        // 删除同一天的数据
        Date scheduleDate = list.get(0).getScheduleDate();
        LambdaUpdateWrapper<HalfCdImportBak> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(HalfCdImportBak::getScheduleDate, scheduleDate);
        entityMapper.delete(wrapper);
        this.saveBatch(list);
        /*entityMapper.deleteTqStock(scheduleDate);
        entityMapper.insertTqStock(scheduleDate);
        entityMapper.deleteGsqStock(scheduleDate);
        entityMapper.insertGsqStock(scheduleDate);
        entityMapper.deleteNcStock(scheduleDate);
        entityMapper.insertNcStock(scheduleDate);
        entityMapper.deleteCd90Stock(scheduleDate);
        entityMapper.insertCd90Stock(scheduleDate);
        entityMapper.deleteCd15Stock(scheduleDate);
        entityMapper.insertCd15Stock(scheduleDate);
        entityMapper.updateTqScheduleResult(scheduleDate);
        entityMapper.insertTqScheduleResult(scheduleDate);
        entityMapper.updateGsqScheduleResult(scheduleDate);
        entityMapper.insertGsqScheduleResult(scheduleDate);
        entityMapper.updateNcScheduleResult(scheduleDate);
        entityMapper.insertNcScheduleResult(scheduleDate);
        entityMapper.updateCd90ScheduleResult(scheduleDate);
        entityMapper.insertCd90ScheduleResult(scheduleDate);
        entityMapper.updateCd15ScheduleResult(scheduleDate);
        entityMapper.insertCd15ScheduleResult(scheduleDate);

        // 更新、新增月计划剩余量
        entityMapper.insertTqMonthPlanSurplus(scheduleDate);
        entityMapper.updateTqMonthPlanSurplus(scheduleDate);
        entityMapper.insertGsqMonthPlanSurplus(scheduleDate);
        entityMapper.updateGsqMonthPlanSurplus(scheduleDate);
        entityMapper.insertNcMonthPlanSurplus(scheduleDate);
        entityMapper.updateNcMonthPlanSurplus(scheduleDate);
        entityMapper.insertCd15MonthPlanSurplus(scheduleDate);
        entityMapper.updateCd15MonthPlanSurplus(scheduleDate);
        entityMapper.insertCd90MonthPlanSurplus(scheduleDate);
        entityMapper.updateCd90MonthPlanSurplus(scheduleDate);*/
        // 调用存储过程
        entityMapper.importCdData(scheduleDate);
        // 查询导入后的数据，生成批次号，工单号，更新对应数据
        ncEngineService.batchUpdateBatchNoAndOrderNo(DateUtils.parseDateToStr("yyyy-MM-dd", scheduleDate));
        cd15EngineService.batchUpdateBatchNoAndOrderNo(scheduleDate);
        cd90EngineService.batchUpdateBatchNoAndOrderNo(scheduleDate);
        tqEngineService.batchUpdateBatchNoAndOrderNo(DateUtils.parseDateToStr("yyyy-MM-dd", scheduleDate));
        gsqEngineService.batchUpdateBatchNoAndOrderNo(DateUtils.parseDateToStr("yyyy-MM-dd", scheduleDate));
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + list.size());
    }

    @Override
    public List<HalfCdImportBak> exportDataToList(List<HalfCdImportBak> list, List<HalfCdImportBak> nextDayList, Date scheduleDate) {
        if (scheduleDate == null) {
            return list;
        }
        // 暂使用2024年数据start
//        Calendar instance = Calendar.getInstance();
//        instance.setTime(scheduleDate);
//        instance.set(Calendar.YEAR, 2024);
//        scheduleDate.setTime(instance.getTimeInMillis());
        // 暂使用2024年数据end
        String scheduleDateStr = DateFormatUtils.format(scheduleDate, "yyyy-MM-dd");
        List<HalfCdImportBakExportVo> exportVos = entityMapper.selectScheduleResult(scheduleDateStr);
        List<HalfCdImportBakExportVo> cxPlanQtyList = entityMapper.selectCxScheduleResult(scheduleDateStr);
        List<HalfCdImportBakExportData> resultList = new ArrayList<>();
        Map<String, HalfCdImportBakExportVo> exportVoMap = new HashMap<>(16);
        Map<String, Integer> cxPlanQtyMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(exportVos)) {
            exportVoMap = exportVos.stream().collect(Collectors.toMap(HalfCdImportBakExportVo::getCode, Function.identity()));
        }
        if (CollectionUtils.isNotEmpty(cxPlanQtyList)) {
            cxPlanQtyMap = cxPlanQtyList.stream().collect(Collectors.toMap(HalfCdImportBakExportVo::getCode, HalfCdImportBakExportVo::getCxPlanQty));
        }
        if (CollectionUtils.isNotEmpty(list)) {
            Set<String> codeExistSet1 = new HashSet<>(16);
            Set<String> codeExistSet2 = new HashSet<>(16);
            Set<String> codeExistSet3 = new HashSet<>(16);
            Set<String> codeExistSet4 = new HashSet<>(16);
            Set<String> codeExistSet5 = new HashSet<>(16);
            Set<String> codeExistSet6 = new HashSet<>(16);
            for (HalfCdImportBak halfCdImportBak : list) {
                HalfCdImportBakExportData halfCdImportBakExportData = new HalfCdImportBakExportData();
                int nightClassIndex = OpenMachineClassEnums.CLASS_TWO.getClassIndex();
                // 胎身帘布
                String lb1 = halfCdImportBak.getLb1();
                Double lb6 = halfCdImportBak.getLb6();
                if (StringUtils.isNotBlank(lb1) && lb6 != null && lb6 > 0) {
                    for (int i = 0; i < lbMachineNameList.size(); i++) {
                        String machineName = lbMachineNameList.get(i);
                        this.setFieldPlanQty(exportVoMap, cxPlanQtyMap, halfCdImportBak, nightClassIndex, lb1 + "-" + machineName + "-1", "lb" + i, "dayPlanQtyRollNum", codeExistSet1);
                    }
                }
                // 2#胎身帘布
                String lbt1 = halfCdImportBak.getLbt1();
                Double lbt6 = halfCdImportBak.getLbt6();
                if (StringUtils.isNotBlank(lbt1) && lbt6 != null && lbt6 > 0) {
                    for (int i = 0; i < lbMachineNameList.size(); i++) {
                        String machineName = lbMachineNameList.get(i);
                        this.setFieldPlanQty(exportVoMap, cxPlanQtyMap, halfCdImportBak, nightClassIndex, lbt1 + "-" + machineName + "-2", "lbt" + i, "dayPlanQtyRollNum", codeExistSet2);
                    }
                }
                // 内衬
                String nc1 = halfCdImportBak.getNc1();
                Double nc6 = halfCdImportBak.getNc6();
                if (StringUtils.isNotBlank(nc1) && nc6 != null && nc6 > 0) {
                    this.setFieldPlanQtyWithOutMachine(exportVoMap, cxPlanQtyMap, halfCdImportBak, nc1, "dayPlanQtyRollNum", "nc13");
                }
                // 1#带束层
                String gd1 = halfCdImportBak.getGd1();
                Double gd6 = halfCdImportBak.getGd6();
                if (StringUtils.isNotBlank(gd1) && gd6 != null && gd6 > 0) {
                    for (int i = 0; i < cdMachineNameList.size(); i++) {
                        String machineName = cdMachineNameList.get(i);
                        this.setFieldPlanQty(exportVoMap, cxPlanQtyMap, halfCdImportBak, nightClassIndex, gd1 + "-" + machineName, "gd" + i, "dayPlanQtyRollNum", codeExistSet3);
                    }
                }
                // 2#带束层
                String gdt1 = halfCdImportBak.getGdt1();
                Double gdt6 = halfCdImportBak.getGdt6();
                if (StringUtils.isNotBlank(gdt1) && gdt6 != null && gdt6 > 0) {
                    for (int i = 0; i < cdMachineNameList.size(); i++) {
                        String machineName = cdMachineNameList.get(i);
                        this.setFieldPlanQty(exportVoMap, cxPlanQtyMap, halfCdImportBak, nightClassIndex, gdt1 + "-" + machineName, "gdt" + i, "dayPlanQtyRollNum", codeExistSet4);
                    }
                }
                // 裸胎圈
                String gsq1 = halfCdImportBak.getGsq1();
                Double gsq4 = halfCdImportBak.getGsq4();
                if (StringUtils.isNotBlank(gsq1) && gsq4 != null && gsq4 > 0) {
                    for (int i = 0; i < cdMachineNameList.size(); i++) {
                        if (i == 1) {
                            // 裸胎圈没有2#机
                            continue;
                        }
                        String machineName = cdMachineNameList.get(i);
                        this.setFieldPlanQty(exportVoMap, cxPlanQtyMap, halfCdImportBak, nightClassIndex, gsq1 + "-" + machineName, "gsq" + i, "dayPlanQty", codeExistSet5);
                    }
                }
                // 胎圈
                /*String tq1 = halfCdImportBak.getTq1();
                Double tq5 = halfCdImportBak.getTq5();
                if (StringUtils.isNotBlank(tq1) && tq5 != null && tq5 > 0) {
                    for (int i = 0; i < tqMachineNameList.size(); i++) {
                        String machineName = tqMachineNameList.get(i);
                        this.setFieldPlanQty(exportVoMap, cxPlanQtyMap, halfCdImportBak, nightClassIndex, tq1 + "-" + machineName, "tq" + i, "dayPlanQty", codeExistSet6);
                    }
                }*/
//                BeanUtils.copyProperties(halfCdImportBak, halfCdImportBakExportData);
//                resultList.add(halfCdImportBakExportData);
            }
        }
        if (CollectionUtils.isNotEmpty(nextDayList)) {
            Set<String> codeExistSet1 = new HashSet<>(16);
            Set<String> codeExistSet2 = new HashSet<>(16);
            Set<String> codeExistSet3 = new HashSet<>(16);
            Set<String> codeExistSet4 = new HashSet<>(16);
            Set<String> codeExistSet5 = new HashSet<>(16);
            Set<String> codeExistSet6 = new HashSet<>(16);
            for (HalfCdImportBak halfCdImportBak : nextDayList) {
                HalfCdImportBakExportData halfCdImportBakExportData = new HalfCdImportBakExportData();
                int dayClassIndex = OpenMachineClassEnums.CLASS_THREE.getClassIndex();
                int nextNightClassIndex = OpenMachineClassEnums.CLASS_TWO.getClassIndex();
                // 胎身帘布
                String lb1 = halfCdImportBak.getLb1();
                Double lb6 = halfCdImportBak.getLb6();
                if (StringUtils.isNotBlank(lb1) && lb6 != null && lb6 > 0) {
                    for (int i = 0; i < lbMachineNameList.size(); i++) {
                        String machineName = lbMachineNameList.get(i);
                        this.setFieldPlanQtyWithTwoClass(exportVoMap, cxPlanQtyMap, halfCdImportBak, lb1 + "-" + machineName + "-1", "lb" + i, "nightPlanQtyRollNum", "nextDayPlanQtyRollNum", dayClassIndex, nextNightClassIndex, codeExistSet1);
                    }
                }
                // 2#胎身帘布
                String lbt1 = halfCdImportBak.getLbt1();
                Double lbt6 = halfCdImportBak.getLbt6();
                if (StringUtils.isNotBlank(lbt1) && lbt6 != null && lbt6 > 0) {
                    for (int i = 0; i < lbMachineNameList.size(); i++) {
                        String machineName = lbMachineNameList.get(i);
                        this.setFieldPlanQtyWithTwoClass(exportVoMap, cxPlanQtyMap, halfCdImportBak, lbt1 + "-" + machineName + "-2", "lbt" + i, "nightPlanQtyRollNum", "nextDayPlanQtyRollNum", dayClassIndex, nextNightClassIndex, codeExistSet2);
                    }
                }
                // 内衬
                String nc1 = halfCdImportBak.getNc1();
                Double nc6 = halfCdImportBak.getNc6();
                if (StringUtils.isNotBlank(nc1) && nc6 != null && nc6 > 0) {
                    this.setFieldPlanQtyWithOutMachineTwoClass(exportVoMap, cxPlanQtyMap, halfCdImportBak, nc1, "nightPlanQtyRollNum", "nc11", "nextDayPlanQtyRollNum", "nc13");
                }
                // 1#带束层
                String gd1 = halfCdImportBak.getGd1();
                Double gd6 = halfCdImportBak.getGd6();
                if (StringUtils.isNotBlank(gd1) && gd6 != null && gd6 > 0) {
                    for (int i = 0; i < cdMachineNameList.size(); i++) {
                        String machineName = cdMachineNameList.get(i);
                        this.setFieldPlanQtyWithTwoClass(exportVoMap, cxPlanQtyMap, halfCdImportBak, gd1 + "-" + machineName, "gd" + i, "nightPlanQtyRollNum", "nextDayPlanQtyRollNum", dayClassIndex, nextNightClassIndex, codeExistSet3);
                    }
                }
                // 2#带束层
                String gdt1 = halfCdImportBak.getGdt1();
                Double gdt6 = halfCdImportBak.getGdt6();
                if (StringUtils.isNotBlank(gdt1) && gdt6 != null && gdt6 > 0) {
                    for (int i = 0; i < cdMachineNameList.size(); i++) {
                        String machineName = cdMachineNameList.get(i);
                        this.setFieldPlanQtyWithTwoClass(exportVoMap, cxPlanQtyMap, halfCdImportBak, gdt1 + "-" + machineName, "gdt" + i, "nightPlanQtyRollNum", "nextDayPlanQtyRollNum", dayClassIndex, nextNightClassIndex, codeExistSet4);
                    }
                }
                // 裸胎圈
                String gsq1 = halfCdImportBak.getGsq1();
                Double gsq4 = halfCdImportBak.getGsq4();
                if (StringUtils.isNotBlank(gsq1) && gsq4 != null && gsq4 > 0) {
                    for (int i = 0; i < cdMachineNameList.size(); i++) {
                        if (i == 1) {
                            // 裸胎圈没有2#机
                            continue;
                        }
                        String machineName = cdMachineNameList.get(i);
                        this.setFieldPlanQtyWithTwoClass(exportVoMap, cxPlanQtyMap, halfCdImportBak, gsq1 + "-" + machineName, "gsq" + i, "nightPlanQty", "nextDayPlanQty", dayClassIndex, nextNightClassIndex, codeExistSet5);
                    }
                }
                // 胎圈
                /*String tq1 = halfCdImportBak.getTq1();
                Double tq5 = halfCdImportBak.getTq5();
                if (StringUtils.isNotBlank(tq1) && tq5 != null && tq5 > 0) {
                    for (int i = 0; i < tqMachineNameList.size(); i++) {
                        String machineName = tqMachineNameList.get(i);
                        this.setFieldPlanQtyWithTwoClass(exportVoMap, cxPlanQtyMap, halfCdImportBak, tq1 + "-" + machineName, "tq" + i, "nightPlanQty", "nextDayPlanQty", dayClassIndex, nextNightClassIndex, codeExistSet6);
                    }
                }*/
//                BeanUtils.copyProperties(halfCdImportBak, halfCdImportBakExportData);
//                resultList.add(halfCdImportBakExportData);
            }
        }
//        exportDataService.saveBatch(resultList);

        return Collections.emptyList();
    }

    private void setFieldPlanQty(Map<String, HalfCdImportBakExportVo> exportVoMap, Map<String, Integer> cxPlanQtyMap, HalfCdImportBak halfCdImportBak, int classIndex, String code, String fieldCode, String fieldName,
                                 Set<String> nightCodeExistSet) {
        if (exportVoMap.containsKey(code)
//                && !nightCodeExistSet.contains(code)
        ) {
            HalfCdImportBakExportVo exportVo = exportVoMap.get(code);
//            String cx3 = halfCdImportBak.getCx3();
//            Integer cxPlanQty = cxPlanQtyMap.get(cx3);
//            if (cxPlanQty != null && cxPlanQty > 0) {
            // 计划
            Double dayPlanQtyRollNum = ReflectUtils.getFieldValue(exportVo, fieldName);
            CdMachineExportEnums machineExportEnums = CdMachineExportEnums.getInstance(fieldCode + "-" + classIndex);
            if (machineExportEnums != null) {
                ReflectUtils.setFieldValue(halfCdImportBak, machineExportEnums.getFieldName(), dayPlanQtyRollNum);
            }
//                nightCodeExistSet.add(code);
//            }
        }
    }

    private void setFieldPlanQtyWithOutMachine(Map<String, HalfCdImportBakExportVo> exportVoMap, Map<String, Integer> cxPlanQtyMap, HalfCdImportBak halfCdImportBak, String code, String getFieldName, String setFieldName) {
        if (exportVoMap.containsKey(code)) {
            HalfCdImportBakExportVo exportVo = exportVoMap.get(code);
            Integer cxPlanQty = cxPlanQtyMap.get(code);
//            if (cxPlanQty != null && cxPlanQty > 0) {
            // 计划
            Double dayPlanQtyRollNum = ReflectUtils.getFieldValue(exportVo, getFieldName);
            ReflectUtils.setFieldValue(halfCdImportBak, setFieldName, dayPlanQtyRollNum);
//            }
        }
    }

    private void setFieldPlanQtyWithTwoClass(Map<String, HalfCdImportBakExportVo> exportVoMap, Map<String, Integer> cxPlanQtyMap, HalfCdImportBak halfCdImportBak, String code, String fieldCode, String fieldName, String fieldName1, int classIndex, int classIndex1,
                                             Set<String> codeExistSet) {
        if (exportVoMap.containsKey(code) && !codeExistSet.contains(code)) {
            HalfCdImportBakExportVo exportVo = exportVoMap.get(code);
//            String cx3 = halfCdImportBak.getCx3();
//            Integer cxPlanQty = cxPlanQtyMap.get(cx3);
//            if (cxPlanQty != null && cxPlanQty > 0) {
            // 计划
            Double dayPlanQtyRollNum = ReflectUtils.getFieldValue(exportVo, fieldName);
            CdMachineExportEnums machineExportEnums = CdMachineExportEnums.getInstance(fieldCode + "-" + classIndex);
            if (machineExportEnums != null) {
                ReflectUtils.setFieldValue(halfCdImportBak, machineExportEnums.getFieldName(), dayPlanQtyRollNum);
            }
            Double fieldValue1 = ReflectUtils.getFieldValue(exportVo, fieldName1);
            CdMachineExportEnums machineExportEnums1 = CdMachineExportEnums.getInstance(fieldCode + "-" + classIndex1);
            if (machineExportEnums1 != null) {
                ReflectUtils.setFieldValue(halfCdImportBak, machineExportEnums1.getFieldName(), fieldValue1);
            }
//                codeExistSet.add(code);
//            }
        }
    }

    private void setFieldPlanQtyWithOutMachineTwoClass(Map<String, HalfCdImportBakExportVo> exportVoMap, Map<String, Integer> cxPlanQtyMap, HalfCdImportBak halfCdImportBak, String code, String getFieldName, String setFieldName, String getFieldName1, String setFieldName1) {
        if (exportVoMap.containsKey(code)) {
            HalfCdImportBakExportVo exportVo = exportVoMap.get(code);
            Integer cxPlanQty = cxPlanQtyMap.get(code);
//            if (cxPlanQty != null && cxPlanQty > 0) {
            // 计划
            Double fieldValue = ReflectUtils.getFieldValue(exportVo, getFieldName);
            ReflectUtils.setFieldValue(halfCdImportBak, setFieldName, fieldValue);
            Double fieldValue1 = ReflectUtils.getFieldValue(exportVo, getFieldName1);
            ReflectUtils.setFieldValue(halfCdImportBak, setFieldName1, fieldValue1);
//            }
        }
    }
}

package com.zlt.aps.tm.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tc.engine.service.TcEngineService;
import com.zlt.aps.tc.mapper.TcScheduleResultMapper;
import com.zlt.aps.tm.api.domain.entity.HalfYcImportBak;
import com.zlt.aps.tm.api.domain.entity.HalfYcImportBakExportData;
import com.zlt.aps.tm.api.domain.vo.HalfYcImportBakExportVo;
import com.zlt.aps.tm.engine.service.TmEngineService;
import com.zlt.aps.tm.mapper.HalfYcImportBakEntityMapper;
import com.zlt.aps.tm.service.IHalfYcImportBakExportDataService;
import com.zlt.aps.tm.service.IHalfYcImportBakService;
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
 * 文件名称：HalfYcImportBakServiceImpl.java
 * 描    述：HalfYcImportBakServiceImpl线下计划导入业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-05-26
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class HalfYcImportBakServiceImpl extends ServiceImpl<HalfYcImportBakEntityMapper, HalfYcImportBak> implements IHalfYcImportBakService {

    @Autowired
    private HalfYcImportBakEntityMapper entityMapper;
    @Autowired
    private IHalfYcImportBakExportDataService exportDataService;
    @Autowired
    private TmEngineService tmEngineService;
    @Autowired
    private TcEngineService tcEngineService;
    @Autowired
    private TcScheduleResultMapper tcScheduleResultMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult importData(List<HalfYcImportBak> list) {
        if (CollectionUtils.isNotEmpty(list)) {
            Date scheduleDate = list.get(0).getScheduleDate();
            LambdaUpdateWrapper<HalfYcImportBak> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(HalfYcImportBak::getScheduleDate, scheduleDate);
            entityMapper.delete(wrapper);
            this.saveBatch(list);
            /*entityMapper.deleteTmStock(scheduleDate);
            entityMapper.insertTmStock(scheduleDate);
            entityMapper.deleteTcStock(scheduleDate);
            entityMapper.insertTcStock(scheduleDate);
            entityMapper.updateTmScheduleResult(scheduleDate);
            entityMapper.insertTmScheduleResult(scheduleDate);
            entityMapper.updateTcScheduleResult(scheduleDate);
            entityMapper.insertTcScheduleResult(scheduleDate);
            entityMapper.insertTmMonthPlanSurplus(scheduleDate);
            entityMapper.updateTmMonthPlanSurplus(scheduleDate);
            entityMapper.insertTcMonthPlanSurplus(scheduleDate);
            entityMapper.updateTcMonthPlanSurplus(scheduleDate);*/
            // 调用存储过程
            entityMapper.importYcData(scheduleDate);
            // 查询导入后的数据，生成批次号，工单号，更新对应数据
            tmEngineService.batchUpdateBatchNoAndOrderNo(DateUtils.parseDateToStr("yyyy-MM-dd", scheduleDate));
            tcEngineService.batchUpdateBatchNoAndOrderNo(DateUtils.parseDateToStr("yyyy-MM-dd", scheduleDate));
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + list.size());
    }

    @Override
    public List<HalfYcImportBak> exportDataToList(List<HalfYcImportBak> list, List<HalfYcImportBak> nextDayList, Date scheduleDate) {
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
        List<HalfYcImportBakExportVo> exportVos = entityMapper.selectScheduleResult(scheduleDateStr);
        List<HalfYcImportBakExportVo> cxPlanQtyList = entityMapper.selectCxScheduleResult(scheduleDateStr);
        List<HalfYcImportBakExportData> resultList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(list)) {
            Map<String, HalfYcImportBakExportVo> exportVoMap = new HashMap<>(16);
            Map<String, Integer> cxPlanQtyMap = new HashMap<>(16);
            if (CollectionUtils.isNotEmpty(exportVos)) {
                exportVoMap = exportVos.stream().collect(Collectors.toMap(HalfYcImportBakExportVo::getCode, Function.identity()));
            }
            if (CollectionUtils.isNotEmpty(cxPlanQtyList)) {
                cxPlanQtyMap = cxPlanQtyList.stream().collect(Collectors.toMap(HalfYcImportBakExportVo::getCode, HalfYcImportBakExportVo::getCxPlanQty));
            }
            for (HalfYcImportBak halfYcImportBak : list) {
                String cx3 = halfYcImportBak.getCx3();
                String suffix = "";
                // 胎面
                if (StringUtils.isNotBlank(cx3) && cx3.contains("VM")) {
                    cx3 = cx3.substring(0, 4);
                    suffix = "(VMI)";
                }
                if (exportVoMap.containsKey(cx3)) {
                    HalfYcImportBakExportVo exportVo = exportVoMap.get(cx3);
                    Integer cxPlanQty = cxPlanQtyMap.get(cx3);
//                    if (cxPlanQty != null && cxPlanQty > 0) {
                        // 夜班计划
                        Double dayPlanQtyRollNum = exportVo.getDayPlanQtyRollNum();
                        String machineName = exportVo.getMachineName();
                        String cx4 = halfYcImportBak.getCx4();
                    Double tm6 = halfYcImportBak.getTm6();
                    boolean isSetCellValue = StringUtils.isBlank(cx4) ||
                            StringUtils.isNotBlank(cx4) && !cx4.contains("试");
                    if (isSetCellValue) {
                        if (tm6 != null && tm6 > 0) {
                            if ("四复合3线".equals(machineName)) {
                                halfYcImportBak.setTm10(exportVo.getDayProduceOrder());
                                halfYcImportBak.setTm11(dayPlanQtyRollNum);
                            } else if ("五复合4线".equals(machineName)) {
                                halfYcImportBak.setTm18(exportVo.getDayProduceOrder());
                                halfYcImportBak.setTm19(dayPlanQtyRollNum);
                            }
                        }
                    }
//                    }
                }
                String tc1 = StringUtils.defaultIfBlank(halfYcImportBak.getTc1(), "");
                if (StringUtils.isNotBlank(tc1)) {
                    tc1 = tc1.substring(0, 6) + suffix;
                }
                if (exportVoMap.containsKey(tc1)) {
                    HalfYcImportBakExportVo exportVo = exportVoMap.get(tc1);
                    String machineName = exportVo.getMachineName();
                    // 夜班计划
                    Double tc6 = halfYcImportBak.getTc6();
                    Integer cxPlanQty = cxPlanQtyMap.get(cx3);
                    if (tc6 != null && tc6 > 0) {
                        Double dayPlanQtyRollNum = exportVo.getDayPlanQtyRollNum();
                        if ("三复合1号线".equals(machineName)) {
                            halfYcImportBak.setTc10(exportVo.getDayProduceOrder());
                            halfYcImportBak.setTc11(dayPlanQtyRollNum);
                        } else if ("三复合2号线".equals(machineName)) {
                            halfYcImportBak.setTc14(exportVo.getDayProduceOrder());
                            halfYcImportBak.setTc15(dayPlanQtyRollNum);
                        }
                    }
                }
                HalfYcImportBakExportData halfYcImportBakExportData = new HalfYcImportBakExportData();
                BeanUtils.copyProperties(halfYcImportBak, halfYcImportBakExportData);
                resultList.add(halfYcImportBakExportData);
            }
            for (HalfYcImportBak halfYcImportBak : nextDayList) {
                String cx3 = halfYcImportBak.getCx3();
                String suffix = "";
                // 胎面
                if (StringUtils.isNotBlank(cx3) && cx3.contains("VM")) {
                    cx3 = cx3.substring(0, 4);
                    suffix = "(VMI)";
                }
                if (exportVoMap.containsKey(cx3)) {
                    HalfYcImportBakExportVo exportVo = exportVoMap.get(cx3);
                    // 夜班计划
                    String machineName = exportVo.getMachineName();
                    Integer cxPlanQty = cxPlanQtyMap.get(cx3);
//                    if (cxPlanQty != null && cxPlanQty > 0) {
                        // 早班计划
                        Double nightPlanQtyRollNum = exportVo.getNightPlanQtyRollNum();
                        String cx4 = halfYcImportBak.getCx4();
                    Double tm6 = halfYcImportBak.getTm6();
                    boolean isSetCellValue = StringUtils.isBlank(cx4) ||
                            StringUtils.isNotBlank(cx4) && !cx4.contains("试");
                    if (isSetCellValue) {
                        if (tm6 != null && tm6 > 0) {
                            if ("四复合3线".equals(machineName)) {
                                halfYcImportBak.setTm8(exportVo.getNightProduceOrder());
                                halfYcImportBak.setTm9(nightPlanQtyRollNum);
                            } else if ("五复合4线".equals(machineName)) {
                                halfYcImportBak.setTm16(exportVo.getNightProduceOrder());
                                halfYcImportBak.setTm17(nightPlanQtyRollNum);
                            }
                        }
                    }
//                    }
                }
                String tc1 = StringUtils.defaultIfBlank(halfYcImportBak.getTc1(), "");
                if (StringUtils.isNotBlank(tc1)) {
                    tc1 = tc1.substring(0, 6) + suffix;
                }
                if (exportVoMap.containsKey(tc1)) {
                    HalfYcImportBakExportVo exportVo = exportVoMap.get(tc1);
                    String machineName = exportVo.getMachineName();
                    // 夜班计划
                    Double tc6 = halfYcImportBak.getTc6();
                    Integer cxPlanQty = cxPlanQtyMap.get(cx3);
                    if (tc6 != null && tc6 > 0) {
                        Double nightPlanQtyRollNum = exportVo.getNightPlanQtyRollNum();
                        if ("三复合1号线".equals(machineName)) {
                            halfYcImportBak.setTc8(exportVo.getNightProduceOrder());
                            halfYcImportBak.setTc9(nightPlanQtyRollNum);
                        } else if ("三复合2号线".equals(machineName)) {
                            halfYcImportBak.setTc12(exportVo.getNightProduceOrder());
                            halfYcImportBak.setTc13(nightPlanQtyRollNum);
                        }
                    }
                }
                HalfYcImportBakExportData halfYcImportBakExportData = new HalfYcImportBakExportData();
                BeanUtils.copyProperties(halfYcImportBak, halfYcImportBakExportData);
                resultList.add(halfYcImportBakExportData);
            }
        }

//        exportDataService.saveBatch(resultList);
        return Collections.emptyList();
    }
}

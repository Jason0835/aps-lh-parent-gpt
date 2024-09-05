package com.zlt.aps.gsq.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.security.aspect.PreAuthorizeAspect;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.SchedulePublishRecord;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.common.engine.utils.DateUtil;
import com.zlt.aps.gsq.api.domain.dto.GsqScheduleResultDto;
import com.zlt.aps.gsq.api.domain.entity.GsqDispatcherLog;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.engine.service.GsqEngineService;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import com.zlt.aps.gsq.entity.GsqScheduleResult;
import com.zlt.aps.gsq.mapper.GsqScheduleResultMapper;
import com.zlt.aps.gsq.service.GsqDispatcherLogService;
import com.zlt.aps.gsq.service.GsqMachineInfoService;
import com.zlt.aps.gsq.service.GsqScheduleResultService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ApsCommonUtil.getDoubleOrDefault;
import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 钢丝圈排程结果Service业务层处理
 *
 * @author chen
 * @date 2021-06-21
 */
@Service
public class GsqScheduleResultServiceImpl extends ServiceImpl<GsqScheduleResultMapper, GsqScheduleResult> implements GsqScheduleResultService {
    @Resource
    private GsqScheduleResultMapper gsqScheduleResultMapper;
    @Resource
    private GsqEngineService gsqEngineService;
    @Value("${excelModelPath}")
    private String excelModelPath;
    @Autowired
    private GsqMachineInfoService gsqMachineInfoService;
    @Resource
    private PreAuthorizeAspect preAuthorizeAspect;
    @Resource
    private GsqDispatcherLogService gsqDispatcherLogService;


    /**
     * 查询钢丝圈排程结果信息维护列表
     *
     * @param scheduleResult 钢丝圈排程结果信息维护
     * @return 钢丝圈排程结果信息维护集合
     */
    @Override
    public List<GsqScheduleResultDto> selectScheduleResultList(GsqScheduleResult scheduleResult) {
        return gsqScheduleResultMapper.selectScheduleResultList(scheduleResult);
    }

    /**
     * 查询钢丝圈排程结果信息维护列表
     *
     * @param id 要查询的id
     * @return 钢丝圈排程结果信息维护集合
     */
    @Override
    public GsqScheduleResultDto selectScheduleResultById(Long id) {
        return gsqScheduleResultMapper.selectScheduleResultById(id);
    }

    /**
     * 保存钢丝圈排程结果信息维护
     *
     * @param scheduleResult 钢丝圈排程结果信息维护
     */
    @Override
    public void editScheduleResult(GsqScheduleResult scheduleResult) {
        // 修改操作 校验字段是否修改，修改则改状态为未发布
        boolean flag;
        GsqScheduleResultDto resultDto = gsqScheduleResultMapper.selectScheduleResultById(scheduleResult.getId());
        flag = compare(resultDto.getMachineId(), scheduleResult.getMachineId());
//        if (!flag) {
//            //机台有变更,需要重新计算计划量（因为不同机台的耗损率不同）
//            GsqScheduleResultVo scheduleResultVo = new GsqScheduleResultVo();
//            BeanUtils.copyProperties(scheduleResult, scheduleResultVo);
//            gsqEngineService.changeGsqMachine(resultDto.getMachineId(), scheduleResultVo);
//            BeanUtils.copyProperties(scheduleResultVo, scheduleResult);
//        }
        flag = flag && compare(resultDto.getDayPlanQty(), scheduleResult.getDayPlanQty());
        flag = flag && compare(resultDto.getNightPlanQty(), scheduleResult.getNightPlanQty());
        flag = flag && compare(resultDto.getMidPlanQty(), scheduleResult.getMidPlanQty());
        flag = flag && compare(resultDto.getDayHandAnalysis(), scheduleResult.getDayHandAnalysis());
        flag = flag && compare(resultDto.getNightHandAnalysis(), scheduleResult.getNightHandAnalysis());
        flag = flag && compare(resultDto.getMidHandAnalysis(), scheduleResult.getMidHandAnalysis());
        flag = flag && compare(resultDto.getDayProduceOrder(), scheduleResult.getDayProduceOrder());
        flag = flag && compare(resultDto.getNightProduceOrder(), scheduleResult.getNightProduceOrder());
        flag = flag && compare(resultDto.getMidProduceOrder(), scheduleResult.getMidProduceOrder());
        flag = flag && compare(resultDto.getRemark(), scheduleResult.getRemark());

        if (!flag) {
            scheduleResult.setIsRelease(scheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
            LambdaUpdateWrapper<GsqScheduleResult> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(GsqScheduleResult::getId, scheduleResult.getId());
            wrapper.set(GsqScheduleResult::getIsRelease, scheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
            update(wrapper);
        }
        scheduleResult.setBaseVale(scheduleResult.getId());
        saveOrUpdate(scheduleResult);
    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule
     */
    public void insetDispatcherLog(String operType, GsqScheduleResult newSchedule) {
        // 20231018 需求确认单各个工序中，调度员操作日志，改成排程操作日志，统计全部人员的操作记录，调度员字段改为“操作人员”字段
        //        if(!preAuthorizeAspect.hasRole(ApsConstant.DISPATCHER_ROLE)) {
        //            return;
        //        }
        GsqScheduleResultDto oldSchedule = this.gsqScheduleResultMapper.selectScheduleResultById(newSchedule.getId());  //操作前的排程数据
        GsqDispatcherLog log = new GsqDispatcherLog();
        //基础信息赋值
        log.setScheduleId(newSchedule.getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate());  //排程日期
        log.setMaterialCode(newSchedule.getSteelRingCode());    //钢丝圈代码
        //操作前的信息赋值
        log.setBeforeMachineId(oldSchedule.getMachineId());
        log.setBeforeMidPlan(oldSchedule.getMidPlanQty());
        log.setBeforeNightPlan(oldSchedule.getNightPlanQty());
        log.setBeforeDayPlan(oldSchedule.getDayPlanQty());
        //操作后的信息赋值
        log.setAfterMachineId(newSchedule.getMachineId());
        log.setAfterMidPlan(newSchedule.getMidPlanQty());
        log.setAfterNightPlan(newSchedule.getNightPlanQty());
        log.setAfterDayPlan(newSchedule.getDayPlanQty());
        /** 调用插入日志方法 **/
        gsqDispatcherLogService.insertGsqDispatcherLog(log);
    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     *
     * @param operType        操作类型：0--转机台、1--调量、2--插单
     */
    @Override
    public void insetDispatcherLogInsertOrder(String operType, List<GsqScheduleResult> scheduleResults, GsqScheduleResult newSchedule) {
        // 20231018 需求确认单各个工序中，调度员操作日志，改成排程操作日志，统计全部人员的操作记录，调度员字段改为“操作人员”字段
        //        if(!preAuthorizeAspect.hasRole(ApsConstant.DISPATCHER_ROLE)) {
        //            return;
        //        }
        List<GsqScheduleResult> scheduleResultList = this.selectByScheduleDateAndCode(newSchedule);
        GsqDispatcherLog log = new GsqDispatcherLog();
        //基础信息赋值
        log.setScheduleId(scheduleResultList.get(0).getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate());  //排程日期
        log.setMaterialCode(newSchedule.getSteelRingCode());    //钢丝圈代码
        // 操作前的信息赋值，取创建时间最大的记录为操作前信息
        if (CollectionUtils.isNotEmpty(scheduleResults)) {
            Optional<GsqScheduleResult> max = scheduleResults.stream().max(Comparator.comparing(GsqScheduleResult::getCreateTime));
            if (max.isPresent()) {
                GsqScheduleResult scheduleResult = max.get();
                log.setBeforeMachineId(scheduleResult.getMachineId());
                log.setBeforeMidPlan(scheduleResult.getMidPlanQty());
                log.setBeforeNightPlan(scheduleResult.getNightPlanQty());
                log.setBeforeDayPlan(scheduleResult.getDayPlanQty());
            }
        }
        //操作后的信息赋值
        log.setAfterMachineId(newSchedule.getMachineId());
        log.setAfterMidPlan(newSchedule.getMidPlanQty());
        log.setAfterNightPlan(newSchedule.getNightPlanQty());
        log.setAfterDayPlan(newSchedule.getDayPlanQty());
        /* 调用插入日志方法 **/
        gsqDispatcherLogService.insertGsqDispatcherLog(log);
    }

    /**
     * 根据排程日期和代码查询排程结果
     * @param scheduleResult 排程日期、代码
     * @return 查询到的数据
     */
    @Override
    public List<GsqScheduleResult> selectByScheduleDateAndCode(GsqScheduleResult scheduleResult) {
        return gsqScheduleResultMapper.selectByScheduleDateAndCode(scheduleResult);
    }

    @Override
    public void addScheduleResult(GsqScheduleResult scheduleResult){
        // 插单操作
        GsqScheduleResultVo scheduleVo = new GsqScheduleResultVo();
        BeanUtils.copyProperties(scheduleResult, scheduleVo);
        gsqEngineService.inertGsqOrder(scheduleVo);
        BeanUtils.copyProperties(scheduleVo, scheduleResult);
    }

    /**
     * 保存钢丝圈排程结果选机台信息
     *
     * @param scheduleResult 钢丝圈排程结果信息
     */
    @Override
    public void chooseMachine(GsqScheduleResult scheduleResult) {
        scheduleResult.setBaseVale(scheduleResult.getId());
        scheduleResult.setIsRelease(scheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
        saveOrUpdate(scheduleResult);
    }

    /**
     * 批量删除钢丝圈排程结果信息维护
     *
     * @param ids 需要删除的钢丝圈排程结果信息维护ID
     */
    @Override
    public void deleteScheduleResultByIds(long[] ids) {
        gsqScheduleResultMapper.deleteByIds(ids);
    }

    /**
     * 导出excel
     *
     * @param list 要导出的数据集合
     * @return 数据数组
     */
    @Override
    public byte[] export(List<GsqScheduleResultDto> list) {
        GsqScheduleResultDto summarySchedule = this.summaryExport(list);  //给导出的数据增加汇总行
        // 按用户语言读取模板
        Locale lang = ServletUtils.getUserLang();
        InputStream inputStream = null;
        if (Locale.SIMPLIFIED_CHINESE.equals(lang) || lang == null) {
            // 中文
            inputStream = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "gsqScheduleResult.xlsx");
        } else if (Locale.US.equals(lang)) {
            // 英文
            inputStream = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "gsqScheduleResult_en.xlsx");
        }
        Workbook webBook = ExcelUtils.readExcel(inputStream);
        CellStyle cellStyle = ExcelUtils.createCellStyle(webBook);
        DataFormat format = webBook.createDataFormat();
        cellStyle.setDataFormat(format.getFormat("[=0]\"\""));
        //填充数据
        if (CollectionUtils.isNotEmpty(list)) {
            // 添加导出生产线
            List<GsqMachineInfo> machineInfos = gsqMachineInfoService.selectMachineInfoList(new GsqMachineInfo());
            Map<String, String> map = null;
            if (CollectionUtils.isNotEmpty(machineInfos)) {
                map = machineInfos.stream().collect(Collectors.toMap(item -> item.getId() + "", GsqMachineInfo::getMachineName));
            }
            DecimalFormat df = new DecimalFormat("0.00%");
            Sheet sheet = webBook.getSheetAt(0);
            webBook.setSheetName(0, I18nUtil.getMessage("ui.data.column.gsq.scheduleResult.modelName"));
            int month = DateUtil.getMonth(list.get(0).getScheduleDate());
            int day = DateUtil.getDay(list.get(0).getScheduleDate());
            Row row1 = sheet.getRow(0);
            BigDecimal midPlan = new BigDecimal(summarySchedule.getMidPlanQty());
            BigDecimal nightPlan = new BigDecimal(summarySchedule.getNightPlanQty());
            BigDecimal dayPlan = new BigDecimal(summarySchedule.getDayPlanQty());
            for (int i = 0; i < list.size(); i++) {
                int cellNum = 0;
                GsqScheduleResultDto scheduleResult = list.get(i);
                Row row = sheet.createRow(i + 2);
//                row.createCell(cellNum++).setCellValue(DateFormatUtils.format(scheduleResult.getScheduleDate(), "yyyy-MM-dd"));
                row.createCell(cellNum++).setCellValue(scheduleResult.getSteelType() == null ? "" : scheduleResult.getSteelType());
                row.createCell(cellNum++).setCellValue(scheduleResult.getSteelRingCode() == null ? "" : scheduleResult.getSteelRingCode());
                row.createCell(cellNum++).setCellValue(scheduleResult.getDimension() == null ? 0 : Double.parseDouble(scheduleResult.getDimension()));
                row.createCell(cellNum++).setCellValue(scheduleResult.getRank() == null ? "" : scheduleResult.getRank());
                StringBuilder produceLine = new StringBuilder();
                if (StringUtils.isNotEmpty(scheduleResult.getMachineId()) && map != null) {
                    String[] aa = scheduleResult.getMachineId().split(",");
                    for (String a : aa) {
                        produceLine.append(map.get(a)).append(",");
                    }
                }
                if (StringUtils.isNotEmpty(produceLine.toString())) {
                    produceLine = new StringBuilder(produceLine.substring(0, produceLine.length() - 1));
                }
                row.createCell(cellNum++).setCellValue(produceLine.toString());
                row.createCell(cellNum++).setCellValue(scheduleResult.getMonthPlanOs() == null ? 0 : Double.parseDouble(scheduleResult.getMonthPlanOs()));
                row.createCell(cellNum++).setCellValue(scheduleResult.getStockQty() == null ? 0 : scheduleResult.getStockQty());
                row.createCell(cellNum++).setCellValue(scheduleResult.getSupplyTime() == null ? 0 : scheduleResult.getSupplyTime());
                row.createCell(cellNum++).setCellValue(scheduleResult.getDailyTotalQty() == null ? 0 : scheduleResult.getDailyTotalQty());
                Double midPlanQty = scheduleResult.getMidPlanQty();
                row.createCell(cellNum++).setCellValue(midPlanQty == null ? 0 : midPlanQty);
                row.createCell(cellNum++).setCellValue(scheduleResult.getMidFinishQty() == null ? 0 : scheduleResult.getMidFinishQty());
                row.createCell(cellNum++).setCellValue(scheduleResult.getMidFinishRate() == null ? "" : df.format(scheduleResult.getMidFinishRate().doubleValue()));
                row.createCell(cellNum++).setCellValue(scheduleResult.getMidProduceOrder() == null ? 0 : scheduleResult.getMidProduceOrder());
                String midSysAnalysis = scheduleResult.getMidSysAnalysis();
                String midHandAnalysis = scheduleResult.getMidHandAnalysis();
                String midAnalysis = "";
                if (StringUtils.isNotEmpty(midSysAnalysis)) {
                    midAnalysis = midAnalysis + midSysAnalysis;
                }
                if (StringUtils.isNotEmpty(midHandAnalysis)) {
                    if (StringUtils.isNotEmpty(midAnalysis)) {
                        midAnalysis = midAnalysis + "," + midHandAnalysis;
                    } else {
                        midAnalysis = midHandAnalysis;
                    }
                }
                row.createCell(cellNum++).setCellValue(midAnalysis);
                row.createCell(cellNum++).setCellValue(scheduleResult.getNightPlanQty() == null ? 0 : scheduleResult.getNightPlanQty());
                row.createCell(cellNum++).setCellValue(scheduleResult.getNightFinishQty() == null ? 0 : scheduleResult.getNightFinishQty());
                row.createCell(cellNum++).setCellValue(scheduleResult.getNightFinishRate() == null ? "" : df.format(scheduleResult.getNightFinishRate().doubleValue()));
                row.createCell(cellNum++).setCellValue(scheduleResult.getNightProduceOrder() == null ? 0 : scheduleResult.getNightProduceOrder());
                String nightSysAnaly = scheduleResult.getNightSysAnalysis();
                String nightHandAnaly = scheduleResult.getNightHandAnalysis();
                String nightAnly = "";
                if (StringUtils.isNotEmpty(nightSysAnaly)) {
                    nightAnly = nightAnly + nightSysAnaly;
                }
                if (StringUtils.isNotEmpty(nightHandAnaly)) {
                    if (StringUtils.isNotEmpty(nightAnly)) {
                        nightAnly = nightAnly + "," + nightHandAnaly;
                    } else {
                        nightAnly = nightHandAnaly;
                    }
                }
                row.createCell(cellNum++).setCellValue(nightAnly);
                row.createCell(cellNum++).setCellValue(scheduleResult.getDayPlanQty() == null ? 0 : scheduleResult.getDayPlanQty());
                row.createCell(cellNum++).setCellValue(scheduleResult.getDayFinishQty() == null ? 0 : scheduleResult.getDayFinishQty());
                row.createCell(cellNum++).setCellValue(scheduleResult.getDayFinishRate() == null ? "" : df.format(scheduleResult.getDayFinishRate()));
                row.createCell(cellNum++).setCellValue(scheduleResult.getDayProduceOrder() == null ? 0 : scheduleResult.getDayProduceOrder());
                String sysAnaly = scheduleResult.getDaySysAnalysis();
                String handAnaly = scheduleResult.getDayHandAnalysis();
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
                row.createCell(cellNum++).setCellValue(anly);
                row.createCell(cellNum++).setCellValue(scheduleResult.getCxClass1Plan() == null ? 0 : scheduleResult.getCxClass1Plan());
                row.createCell(cellNum++).setCellValue(scheduleResult.getCxClass2Plan() == null ? 0 : scheduleResult.getCxClass2Plan());
                row.createCell(cellNum++).setCellValue(scheduleResult.getCxClass3Plan() == null ? 0 : scheduleResult.getCxClass3Plan());
                row.createCell(cellNum++).setCellValue(scheduleResult.getCxClass4Plan() == null ? 0 : scheduleResult.getCxClass4Plan());
                row.createCell(cellNum++).setCellValue(scheduleResult.getCxClass5Plan() == null ? 0 : scheduleResult.getCxClass5Plan());
                row.createCell(cellNum).setCellValue(scheduleResult.getRemark() == null ? "" : scheduleResult.getRemark());
                setCellStyle(row, row.getPhysicalNumberOfCells(), cellStyle);
            }

            //重置表头基本信息
            String dateStr="";
            if("zh_CN".equals(lang.toString())){
                dateStr=DateUtils.parseDateToStr("MM月dd日",list.get(0).getScheduleDate());
            }else{
                String monthStr=month+"";
                String dayStr=day+"";
                if(monthStr.length()<=1){
                    monthStr="0"+month;
                }
                if(dayStr.length()<=1){
                    dayStr="0"+day;
                }
                dateStr=DateUtil.getEngMonthDay(monthStr+dayStr) + " ";
            }
            String baseInfo=I18nUtil.getMessage("ui.data.column.scheduleResult.gsq.baseInfo");
            String class1Plan=I18nUtil.getMessage("ui.data.column.scheduleResult.heji.zhongban");
            String class2Plan=I18nUtil.getMessage("ui.data.column.scheduleResult.heji.yeban");
            String class3Plan=I18nUtil.getMessage("ui.data.column.scheduleResult.heji.baiban");
            String totalQty=I18nUtil.getMessage("ui.data.column.scheduleResult.totalQty");
            String planInfo = '：'+class1Plan+'：'+midPlan.setScale(0,BigDecimal.ROUND_HALF_UP)+'，'+class2Plan+'：'+nightPlan.setScale(0,BigDecimal.ROUND_HALF_UP)+'，'+class3Plan+'：'+dayPlan.setScale(0,BigDecimal.ROUND_HALF_UP)+'，'+totalQty+'：'+(midPlan.add(nightPlan).add(dayPlan)).setScale(0,BigDecimal.ROUND_HALF_UP);
            baseInfo=dateStr+baseInfo+planInfo;
            Cell cell0=sheet.getRow(0).getCell(0);
            CellStyle cellStyle0=cell0.getCellStyle();
            cell0.setCellValue(baseInfo);
            cell0.setCellStyle(cellStyle0);

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
                assert out != null;
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
    private GsqScheduleResultDto summaryExport(List<GsqScheduleResultDto> list) {
        if(list == null || list.isEmpty()) {
            return null;
        }
        GsqScheduleResultDto summary = new  GsqScheduleResultDto();
        summary.setSteelRingCode(I18nUtil.getMessage("ui.data.column.scheduleResult.totalQty"));
        summary.setMidPlanQty(list.stream().mapToDouble(r->getDoubleOrDefault(r.getMidPlanQty())).sum());
        summary.setMidFinishQty(list.stream().mapToDouble(r->getDoubleOrDefault(r.getMidFinishQty())).sum());
        summary.setNightPlanQty(list.stream().mapToDouble(r->getDoubleOrDefault(r.getNightPlanQty())).sum());
        summary.setNightFinishQty(list.stream().mapToDouble(r->getDoubleOrDefault(r.getNightFinishQty())).sum());
        summary.setDayPlanQty(list.stream().mapToDouble(r->getDoubleOrDefault(r.getDayPlanQty())).sum());
        summary.setDayFinishQty(list.stream().mapToDouble(r->getDoubleOrDefault(r.getDayFinishQty())).sum());
        summary.setDailyTotalQty(BigDecimalUtil.add(summary.getMidPlanQty(), summary.getNightPlanQty(), summary.getDayPlanQty()));

        summary.setCxClass1Plan(list.stream().mapToDouble(r->getDoubleOrDefault(r.getCxClass1Plan())).sum());
        summary.setCxClass2Plan(list.stream().mapToDouble(r->getDoubleOrDefault(r.getCxClass2Plan())).sum());
        summary.setCxClass3Plan(list.stream().mapToDouble(r->getDoubleOrDefault(r.getCxClass3Plan())).sum());
        summary.setCxClass4Plan(list.stream().mapToDouble(r->getDoubleOrDefault(r.getCxClass4Plan())).sum());
        summary.setCxClass5Plan(list.stream().mapToDouble(r->getDoubleOrDefault(r.getCxClass5Plan())).sum());
        list.add(summary);
        return summary;
    }

    @Override
    public void publish(GsqScheduleResult scheduleResult, long[] ids, String dataVersion, String factoryCode, String companyCode) {
        //把排程数据发布到中间库
        this.deployScheduleToMid(ids, dataVersion, factoryCode, companyCode);
        //保存发布日志
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setBaseVale(null);
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_GSQ);
        record.setScheduleDate(scheduleResult.getScheduleDate());
        record.setPublishStatus(ApsConstant.RELEASING);
        record.setDataVersion(dataVersion);
        gsqScheduleResultMapper.insertPublishRecord(record);

        if (ids == null || ids.length == 0) {
            //设置更新人和更新时间
            scheduleResult.setBaseVale(0L);
            scheduleResult.setIsRelease(ApsConstant.RELEASING);
            gsqScheduleResultMapper.publishAll(scheduleResult);
        }
        // ids不为空，发布指定记录，需求暂未变更，变更后测试
        gsqScheduleResultMapper.batchUpdate(ids, ApsConstant.RELEASING);
    }
    
	/**
	 * 更新指定相关数据记录的发布状态
	 * 
	 * @param dataVersion 数据版本
	 * @param ids         排程ID列表
	 * @param status      更新的状态
	 */
    @Override
    public void updateRelaseStatus(String dataVersion, long[] ids, String status) {
        gsqScheduleResultMapper.batchUpdate(ids, status);
        gsqScheduleResultMapper.updatePublishRecordVersion(dataVersion, status);
    }

    /**
     * 发布排程数据到中间库,并通知 MES
     * @param ids 发布的排程记录id
     * @param scheduleDate 排产日期
     */
    private void deployScheduleToMid(long[] ids, String dataVersion, String factoryCode, String companyCode) {
        if(ids == null) {
            return;
        }
        //把排程数据同步到接口中间库中
        gsqScheduleResultMapper.deployGsqScheduleToMid(dataVersion, ids, factoryCode, companyCode);
    }

    /**
     * 根据排程日期、物料编号、机台id校验唯一性
     *
     * @param scheduleResult 要校验记录
     * @return 查询到的记录数
     */
    @Override
    public Boolean checkUnique(GsqScheduleResult scheduleResult) {
        return gsqScheduleResultMapper.checkUnique(scheduleResult) == 0;
    }

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param importLogId   导入日志id
     * @param scheduleDate  排程日期
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importData(List<GsqScheduleResultDto> list, Long importLogId, Date scheduleDate) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<GsqScheduleResultDto> importList = new ArrayList<>();

        try {
            //将机台名称转为机台code
            GsqMachineInfo gsqMachineInfo=new GsqMachineInfo();
            gsqMachineInfo.setStatus("0");
            List<GsqMachineInfo> machineInfoList = gsqMachineInfoService.selectMachineInfoList(gsqMachineInfo);
            if (CollectionUtils.isEmpty(machineInfoList)) {
                // 未查询到机台信息
                String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
                addImportErrorLog(importLogId, null, message, importErrorLogs);
                return AjaxResult.error(message, importErrorLogs);
            }

            //根据机台名称去重
            TreeSet<GsqMachineInfo> treeSet = new TreeSet<GsqMachineInfo>(new Comparator<GsqMachineInfo>() {
                @Override
                public int compare(GsqMachineInfo o1, GsqMachineInfo o2) {
                    return o1.getMachineName().compareTo(o2.getMachineName());
                }
            });
            treeSet.addAll(machineInfoList);
            machineInfoList = new ArrayList<>(treeSet);

            Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(GsqMachineInfo::getMachineName, GsqMachineInfo::getId));
            //按业务主键分组
            Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(item -> item.getSteelRingCode() + item.getMachineId(), Collectors.counting()));

            for (int i = 0; i < list.size(); i++) {
                GsqScheduleResultDto scheduleResultDto = list.get(i);
                scheduleResultDto.setDataSource("2");
                scheduleResultDto.setScheduleDate(scheduleDate);

                if (groupMap.get(scheduleResultDto.getSteelRingCode() + scheduleResultDto.getMachineId()) > 1) {
                    failureNum++;
                    String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                    String columnName = I18nUtil.getMessage("ui.data.column.gsq.scheduleResult.steelRingCode");
                    String columnName2 = I18nUtil.getMessage("ui.data.column.scheduleResult.produceLine");
                    message=String.format(message,columnName+"+"+columnName2);
                    addImportErrorLog(importLogId, i + 3,message, importErrorLogs);
                    continue;
                }

                int errorNum = i + 3;
                List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, scheduleResultDto);

                // 机台code 转为机台id
                if(scheduleResultDto.getMachineId()!=null && scheduleResultDto.getMachineId().indexOf(",")>0){
                    String message = I18nUtil.getMessage("ui.data.column.machine.produceLineValidate");
                    message=String.format(message, i + 3, I18nUtil.getMessage("ui.data.column.scheduleResult.produceLine"));
                    addImportErrorLog(importLogId, i + 3,message, validated);
                }
                if (machineCodeMap.get(scheduleResultDto.getMachineId())==null) {
                    addImportErrorLog(importLogId, i + 3,
                            I18nUtil.getMessage("ui.error.message.column.produceLineNotExist"), validated);
                }

                if (CollectionUtils.isNotEmpty(validated)) {
                    failureNum++;
                    importErrorLogs.addAll(validated);
                } else {
                    successNum++;
                    scheduleResultDto.setMachineId(machineCodeMap.get(scheduleResultDto.getMachineId())+"");
                    scheduleResultDto.setBaseVale(null);
                    importList.add(scheduleResultDto);
                }

                // 查看数据
                System.out.println("--------------------------");
                System.out.println(scheduleResultDto);
                System.out.println("--------------------------");
            }
            this.batchSaveGsqSchedule(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, scheduleDate), importList);  //把验证成功的记录进行导入

        } catch (Exception e) {
            e.printStackTrace();
            // 执行sql失败，插入导入失败记录
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 批量更新或新增排程记录信息
     * @param scheduleDate 排程日志，格式：yyyy-MM-dd
     * @param importList 导入数据
     */
    private void batchSaveGsqSchedule(String scheduleDate, List<GsqScheduleResultDto> importList) {
        List<GsqScheduleResultVo> scheduleList = new ArrayList<>();
        for(GsqScheduleResultDto result : importList) {
            GsqScheduleResultVo vo = new GsqScheduleResultVo();
            BeanUtils.copyProperties(result, vo);
            scheduleList.add(vo);
        }
        if(!scheduleList.isEmpty()) {
            this.gsqEngineService.batchSaveGsqSchedule(scheduleDate, scheduleList);
        }
    }

    /**
     * 查询排程日期是否已发布
     *
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    @Override
    public Boolean isPublish(Date scheduleDate) {
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_GSQ);
        record.setScheduleDate(scheduleDate);
        return gsqScheduleResultMapper.isPublish(record) > 0;
    }

    public boolean compare(String str1, String str2) {
        return (StringUtils.isEmpty(str1) ? StringUtils.isEmpty(str2) : str1.equals(str2));
    }

    public boolean compare(Integer str1, Integer str2) {
        return (ObjectUtils.isEmpty(str1) ? ObjectUtils.isEmpty(str2) : str1.equals(str2));
    }

    /**
     * 设置单元格样式
     *
     * @param row       单元行对象
     * @param cellNum   列数
     * @param cellStyle 样式
     */
    private void setCellStyle(Row row, int cellNum, CellStyle cellStyle) {
        for (int i = 0; i < cellNum; i++) {
            row.getCell(i).setCellStyle(cellStyle);
        }
    }

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @Override
    public int isReleasingOrTimeoutByDate(Date scheduleDate) {
        return gsqScheduleResultMapper.isReleasingOrTimeoutByDate(scheduleDate);
    }

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param ids id
     * @return 查询到的记录数
     */
    @Override
    public int isReleasingOrTimeoutByIds(long[] ids) {
        return gsqScheduleResultMapper.isReleasingOrTimeoutByIds(ids);
    }

    /**
     * 更改发布状态
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @Override
    public int changeReleaseStatus(GsqScheduleResult entity) {
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setBaseVale(1L);
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_GSQ);
        record.setScheduleDate(entity.getScheduleDate());
        record.setPublishStatus(entity.getIsRelease());
        gsqScheduleResultMapper.updatePublishRecord(record);
        return gsqScheduleResultMapper.changeReleaseStatus(entity);
    }

    @Override
    public int checkGsqCodeExist(GsqScheduleResult scheduleResult) {
        return gsqScheduleResultMapper.checkGsqCodeExist(scheduleResult);
    }

    @Override
    public int isPublishByIds(long[] ids) {
        return gsqScheduleResultMapper.isPublishByIds(ids);
    }

    @Override
    public List<GsqScheduleResultDto> selectByIds(List<Long> ids2) {
        return gsqScheduleResultMapper.selectByIds(ids2);
    }

    public boolean compare(Double d1, Double d2) {
        d1 = ObjectUtils.isEmpty(d1) ? 0D : d1;
        d2 = ObjectUtils.isEmpty(d2) ? 0D : d2;
        return d1.equals(d2);
    }
}

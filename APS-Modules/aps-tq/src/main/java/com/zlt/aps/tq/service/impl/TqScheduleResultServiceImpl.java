package com.zlt.aps.tq.service.impl;

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
import com.zlt.aps.tq.api.domain.dto.TqScheduleResultDto;
import com.zlt.aps.tq.api.domain.entity.TqDispatcherLog;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.engine.service.TqEngineService;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import com.zlt.aps.tq.entity.TqScheduleResult;
import com.zlt.aps.tq.mapper.TqScheduleResultMapper;
import com.zlt.aps.tq.service.TqDispatcherLogService;
import com.zlt.aps.tq.service.TqMachineInfoService;
import com.zlt.aps.tq.service.TqScheduleResultService;
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
 * 胎圈排程结果Service业务层处理
 *
 * @author chen
 * @date 2021-06-24
 */
@Service
public class TqScheduleResultServiceImpl extends ServiceImpl<TqScheduleResultMapper, TqScheduleResult> implements TqScheduleResultService {
    @Resource
    private TqScheduleResultMapper scheduleResultMapper;
    @Value("${excelModelPath}")
    private String excelModelPath;
    @Autowired
    private TqMachineInfoService machineInfoService;
    @Resource
    private TqEngineService tqEngineService;
    @Resource
    private PreAuthorizeAspect preAuthorizeAspect;
    @Resource
    private TqDispatcherLogService tqDispatcherLogService;


    /**
     * 查询胎圈排程结果信息维护列表
     *
     * @param scheduleResult 胎圈排程结果信息维护
     * @return 胎圈排程结果信息维护集合
     */
    @Override
    public List<TqScheduleResultDto> selectScheduleResultList(TqScheduleResult scheduleResult) {
        return scheduleResultMapper.selectScheduleResultList(scheduleResult);
    }

    /**
     * 查询胎圈排程结果信息维护列表
     *
     * @param id 要查询的id
     * @return 胎圈排程结果信息维护集合
     */
    @Override
    public TqScheduleResultDto selectScheduleResultById(Long id) {
        return scheduleResultMapper.selectScheduleResultById(id);
    }

    /**
     * 保存胎圈排程结果信息维护
     *
     * @param scheduleResult 胎圈排程结果信息维护
     */
    @Override
    public void saveScheduleResult(TqScheduleResult scheduleResult) {
        // 校验字段是否修改，修改则改状态为未发布
        if (scheduleResult.getId() != null) {
            boolean flag;
            TqScheduleResultDto resultDto = scheduleResultMapper.selectScheduleResultById(scheduleResult.getId());
            flag = compareFields(scheduleResult, resultDto);
            if (!flag) {
                scheduleResult.setIsRelease(scheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
            }
            scheduleResult.setBaseVale(scheduleResult.getId());
            saveOrUpdate(scheduleResult);
        } else {
            // 插单操作
            TqScheduleResultVo scheduleVo = new TqScheduleResultVo();
            BeanUtils.copyProperties(scheduleResult, scheduleVo);
            List<TqScheduleResult> scheduleResults = this.selectByScheduleDateAndCode(scheduleResult);
            tqEngineService.inertTqOrder(scheduleVo);
            this.insetDispatcherLogInsertOrder(ApsConstant.DISPATCHER_OPER_INSERT_ORDER, scheduleResults, scheduleResult);
            BeanUtils.copyProperties(scheduleVo, scheduleResult);
        }

    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule
     */
    public void insetDispatcherLog(String operType, TqScheduleResult newSchedule) {
        // 20231018 需求确认单各个工序中，调度员操作日志，改成排程操作日志，统计全部人员的操作记录，调度员字段改为“操作人员”字段
        //        if(!preAuthorizeAspect.hasRole(ApsConstant.DISPATCHER_ROLE)) {
        //            return;
        //        }
        TqScheduleResultDto oldSchedule = this.scheduleResultMapper.selectScheduleResultById(newSchedule.getId());  //操作前的排程数据
        TqDispatcherLog log = new TqDispatcherLog();
        //基础信息赋值
        log.setScheduleId(newSchedule.getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate());  //排程日期
        log.setMaterialCode(newSchedule.getBeadCode());    //胎圈代码
        //操作前的信息赋值
        log.setBeforeMachineId(oldSchedule.getMachineId());
        log.setBeforeMidPlan(oldSchedule.getMidPlanQty());
        log.setBeforeNightPlan(oldSchedule.getNightPlanQty());
        log.setBeforeDayPlan(oldSchedule.getDayPlanQty());
        log.setBeforeNextMidPlan(oldSchedule.getNextMidPlanQty());
        //操作后的信息赋值
        log.setAfterMachineId(newSchedule.getMachineId());
        log.setAfterMidPlan(newSchedule.getMidPlanQty());
        log.setAfterNightPlan(newSchedule.getNightPlanQty());
        log.setAfterDayPlan(newSchedule.getDayPlanQty());
        log.setAfterNextMidPlan(newSchedule.getNextMidPlanQty());
        /** 调用插入日志方法 **/
        tqDispatcherLogService.insertTqDispatcherLog(log);
    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     *
     * @param operType        操作类型：0--转机台、1--调量、2--插单
     */
    @Override
    public void insetDispatcherLogInsertOrder(String operType, List<TqScheduleResult> scheduleResults, TqScheduleResult newSchedule) {
        // 20231018 需求确认单各个工序中，调度员操作日志，改成排程操作日志，统计全部人员的操作记录，调度员字段改为“操作人员”字段
        //        if(!preAuthorizeAspect.hasRole(ApsConstant.DISPATCHER_ROLE)) {
        //            return;
        //        }
        List<TqScheduleResult> scheduleResultList = this.selectByScheduleDateAndCode(newSchedule);
        TqDispatcherLog log = new TqDispatcherLog();
        //基础信息赋值
        log.setScheduleId(scheduleResultList.get(0).getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate());  //排程日期
        log.setMaterialCode(newSchedule.getBeadCode());    //胎圈代码
        // 操作前的信息赋值，取创建时间最大的记录为操作前信息
        if (CollectionUtils.isNotEmpty(scheduleResults)) {
            Optional<TqScheduleResult> max = scheduleResults.stream().max(Comparator.comparing(TqScheduleResult::getCreateTime));
            if (max.isPresent()) {
                TqScheduleResult scheduleResult = max.get();
                log.setBeforeMachineId(scheduleResult.getMachineId());
                log.setBeforeMidPlan(scheduleResult.getMidPlanQty());
                log.setBeforeNightPlan(scheduleResult.getNightPlanQty());
                log.setBeforeDayPlan(scheduleResult.getDayPlanQty());
                log.setBeforeDayPlan(scheduleResult.getNextMidPlanQty());
            }
        }
        //操作后的信息赋值
        log.setAfterMachineId(newSchedule.getMachineId());
        log.setAfterMidPlan(newSchedule.getMidPlanQty());
        log.setAfterNightPlan(newSchedule.getNightPlanQty());
        log.setAfterDayPlan(newSchedule.getDayPlanQty());
        log.setAfterNextMidPlan(newSchedule.getNextMidPlanQty());
        /* 调用插入日志方法 **/
        tqDispatcherLogService.insertTqDispatcherLog(log);
    }

    /**
     * 根据排程日期和代码查询排程结果
     * @param scheduleResult 排程日期、代码
     * @return 查询到的数据
     */
    @Override
    public List<TqScheduleResult> selectByScheduleDateAndCode(TqScheduleResult scheduleResult) {
        return scheduleResultMapper.selectByScheduleDateAndCode(scheduleResult);
    }

    /**
     * 批量删除胎圈排程结果信息维护
     *
     * @param ids 需要删除的胎圈排程结果信息维护ID
     */
    @Override
    public void deleteScheduleResultByIds(long[] ids) {
        scheduleResultMapper.deleteByIds(ids);
    }

    @Override
    public byte[] export(List<TqScheduleResultDto> list) {
        TqScheduleResultDto summarySchedule = this.summaryExport(list);  //给导出的数据增加汇总行
        // 按用户语言读取模板
        Locale lang = ServletUtils.getUserLang();
        InputStream inputStream = null;
        if (Locale.SIMPLIFIED_CHINESE.equals(lang) || lang == null) {
            // 中文
            inputStream = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "tqScheduleResult.xlsx");
        } else if (Locale.US.equals(lang)) {
            // 英文
            inputStream = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "tqScheduleResult_en.xlsx");
        }
        Workbook webBook = ExcelUtils.readExcel(inputStream);
        CellStyle cellStyle = ExcelUtils.createCellStyle(webBook);
        DataFormat format = webBook.createDataFormat();
        cellStyle.setDataFormat(format.getFormat("[=0]\"\""));  //导出的单元格如果值为0，则显示空白
        //填充数据
        if (CollectionUtils.isNotEmpty(list)) {
            // 添加导出生产线
            List<TqMachineInfo> tmMachineInfoList = machineInfoService.selectMachineInfoList(new TqMachineInfo());
            Map<String, String> map = null;
            if (CollectionUtils.isNotEmpty(tmMachineInfoList)) {
                map = tmMachineInfoList.stream().collect(Collectors.toMap(item -> item.getId() + "", TqMachineInfo::getMachineName));
            }
            DecimalFormat df = new DecimalFormat("0.00%");
            Sheet sheet = webBook.getSheetAt(0);
            int month = DateUtil.getMonth(list.get(0).getScheduleDate());
            int day = DateUtil.getDay(list.get(0).getScheduleDate());
            Row row1 = sheet.getRow(0);
            BigDecimal midPlan = new BigDecimal(summarySchedule.getMidPlanQty());
            BigDecimal nightPlan = new BigDecimal(summarySchedule.getNightPlanQty());
            BigDecimal dayPlan = new BigDecimal(summarySchedule.getDayPlanQty());
            BigDecimal nextMidPlan = new BigDecimal(summarySchedule.getNextMidPlanQty());
            for (int i = 0; i < list.size(); i++) {
                int cellNum = 0;
                TqScheduleResultDto scheduleResult = list.get(i);
                Row row = sheet.createRow(i + 2);
//                row.createCell(cellNum++).setCellValue(DateFormatUtils.format(scheduleResult.getScheduleDate(), "yyyy-MM-dd"));
                row.createCell(cellNum++).setCellValue(scheduleResult.getBeadCode());
                row.createCell(cellNum++).setCellValue(scheduleResult.getSteelRingCode());
                row.createCell(cellNum++).setCellValue(scheduleResult.getTriangleGlueCode());
                row.createCell(cellNum++).setCellValue(scheduleResult.getGlueCode());
                row.createCell(cellNum++).setCellValue(scheduleResult.getMouthPlateCode());
                row.createCell(cellNum++).setCellValue(scheduleResult.getSpecSize());
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
                String monthPlanOs = scheduleResult.getMonthPlanOs();
                row.createCell(cellNum++).setCellValue(monthPlanOs == null ? 0 : Integer.parseInt(monthPlanOs));
                row.createCell(cellNum++).setCellValue(scheduleResult.getStockQty() == null ? 0 : scheduleResult.getStockQty());
                Double supplyTime = scheduleResult.getSupplyTime() == null ? 0 : scheduleResult.getSupplyTime() ;
                row.createCell(cellNum++).setCellValue(supplyTime);
                row.createCell(cellNum++).setCellValue(scheduleResult.getDailyTotalQty() == null ? 0 : scheduleResult.getDailyTotalQty());
                row.createCell(cellNum++).setCellValue(scheduleResult.getMidPlanQty() == null ? 0 : scheduleResult.getMidPlanQty());
                row.createCell(cellNum++).setCellValue(scheduleResult.getMidFinishQty() == null ? 0 : scheduleResult.getMidFinishQty());
                String midFinishRate = scheduleResult.getMidFinishRate() == null ? "" : df.format(scheduleResult.getMidFinishRate());
                row.createCell(cellNum++).setCellValue(midFinishRate);
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
                String nightFinishRate = scheduleResult.getNightFinishRate() == null ? "" : df.format(scheduleResult.getNightFinishRate());
                row.createCell(cellNum++).setCellValue(nightFinishRate);
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
                String dayFinishRate = scheduleResult.getDayFinishRate() == null ? "" : df.format(scheduleResult.getDayFinishRate().doubleValue());
                row.createCell(cellNum++).setCellValue(dayFinishRate);
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
                row.createCell(cellNum++).setCellValue(scheduleResult.getNextMidPlanQty() == null ? 0 : scheduleResult.getNextMidPlanQty());
                row.createCell(cellNum++).setCellValue(scheduleResult.getNextMidFinishQty() == null ? 0 : scheduleResult.getNextMidFinishQty());
                String nextMidFinishRate = scheduleResult.getNextMidFinishRate() == null ? "" : df.format(scheduleResult.getNextMidFinishRate());
                row.createCell(cellNum++).setCellValue(nextMidFinishRate);
                row.createCell(cellNum++).setCellValue(scheduleResult.getNextMidProduceOrder() == null ? 0 : scheduleResult.getNextMidProduceOrder());
                String nextMidSysAnalysis = scheduleResult.getNextMidSysAnalysis();
                String nextMidHandAnalysis = scheduleResult.getNextMidHandAnalysis();
                String nextMidAnalysis = "";
                if (StringUtils.isNotEmpty(nextMidSysAnalysis)) {
                    nextMidAnalysis = nextMidAnalysis + nextMidSysAnalysis;
                }
                if (StringUtils.isNotEmpty(nextMidHandAnalysis)) {
                    if (StringUtils.isNotEmpty(nextMidAnalysis)) {
                        nextMidAnalysis = nextMidAnalysis + "," + nextMidHandAnalysis;
                    } else {
                        nextMidAnalysis = nextMidHandAnalysis;
                    }
                }
                row.createCell(cellNum++).setCellValue(nextMidAnalysis);
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
            String baseInfo=I18nUtil.getMessage("ui.data.column.scheduleResult.tq.baseInfo");
            String class1Plan=I18nUtil.getMessage("ui.data.column.scheduleResult.heji.zhongban");
            String class2Plan=I18nUtil.getMessage("ui.data.column.scheduleResult.heji.yeban");
            String class3Plan=I18nUtil.getMessage("ui.data.column.scheduleResult.heji.baiban");
            String class4Plan=I18nUtil.getMessage("ui.data.column.scheduleResult.heji.cirizhongban");
            String totalQty=I18nUtil.getMessage("ui.data.column.scheduleResult.totalQty");
            String planInfo = '：'+class1Plan+'：'+midPlan.setScale(0,BigDecimal.ROUND_HALF_UP)+'，'+class2Plan+'：'+nightPlan.setScale(0,BigDecimal.ROUND_HALF_UP)+'，'+class3Plan+'：'+dayPlan.setScale(0,BigDecimal.ROUND_HALF_UP)+'，'+class4Plan+'：'+nextMidPlan.setScale(0,BigDecimal.ROUND_HALF_UP)+'，'+totalQty+'：'+(midPlan.add(nightPlan).add(dayPlan).add(nextMidPlan)).setScale(0,BigDecimal.ROUND_HALF_UP);
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
    private TqScheduleResultDto summaryExport(List<TqScheduleResultDto> list) {
        if(list == null || list.isEmpty()) {
            return null;
        }
        TqScheduleResultDto summary = new  TqScheduleResultDto();
        summary.setBeadCode(I18nUtil.getMessage("ui.data.column.scheduleResult.totalQty"));
        summary.setMidPlanQty(list.stream().mapToDouble(r->getDoubleOrDefault(r.getMidPlanQty())).sum());
        summary.setMidFinishQty(list.stream().mapToDouble(r->getDoubleOrDefault(r.getMidFinishQty())).sum());
        summary.setNightPlanQty(list.stream().mapToDouble(r->getDoubleOrDefault(r.getNightPlanQty())).sum());
        summary.setNightFinishQty(list.stream().mapToDouble(r->getDoubleOrDefault(r.getNightFinishQty())).sum());
        summary.setDayPlanQty(list.stream().mapToDouble(r->getDoubleOrDefault(r.getDayPlanQty())).sum());
        summary.setDayFinishQty(list.stream().mapToDouble(r->getDoubleOrDefault(r.getDayFinishQty())).sum());
        summary.setNextMidPlanQty(list.stream().mapToDouble(r->getDoubleOrDefault(r.getNextMidPlanQty())).sum());
        summary.setNextMidFinishQty(list.stream().mapToDouble(r->getDoubleOrDefault(r.getNextMidFinishQty())).sum());
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
    public void publish(TqScheduleResult scheduleResult, long[] ids, String dataVersion, String factoryCode, String companyCode) {
        //把排程数据发布到中间库
        this.deployScheduleToMid(ids, dataVersion, factoryCode, companyCode);
        //保存发布日志
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setBaseVale(null);
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_TQ);
        record.setScheduleDate(scheduleResult.getScheduleDate());
        record.setPublishStatus(ApsConstant.RELEASING);
        record.setDataVersion(dataVersion);
        scheduleResultMapper.insertPublishRecord(record);
        if (ids == null || ids.length == 0) {
            //设置更新人和更新时间
            scheduleResult.setBaseVale(0L);
            scheduleResult.setIsRelease("1");
            scheduleResultMapper.publishAll(scheduleResult);
        }
        // ids不为空，发布指定记录，需求暂未变更，变更后测试
        scheduleResultMapper.batchUpdate(ids, ApsConstant.RELEASING);
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
        scheduleResultMapper.batchUpdate(ids, status);
        scheduleResultMapper.updatePublishRecordVersion(dataVersion, status);
    }

    /**
     * 发布排程数据到中间库,并通知 MES
     *
     * @param ids 发布的排程记录id
     */
    private void deployScheduleToMid(long[] ids, String dataVersion, String factoryCode, String companyCode) {
        if (ids == null) {
            return;
        }
        //把排程数据同步到接口中间库中
        scheduleResultMapper.deployTqScheduleToMid(dataVersion, ids, factoryCode, companyCode);
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
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_TQ);
        record.setScheduleDate(scheduleDate);
        return scheduleResultMapper.isPublish(record) > 0;
    }

    /**
     * 根据排程日期、物料编号、机台id校验唯一性
     *
     * @param scheduleResult 要校验记录
     * @return 查询到的记录数
     */
    @Override
    public Boolean checkUnique(TqScheduleResult scheduleResult) {
        return scheduleResultMapper.checkUnique(scheduleResult) == 0;
    }

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importData(List<TqScheduleResultDto> list, Long importLogId, Date scheduleDate) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TqScheduleResultDto> importList = new ArrayList<>();

        try {
            //将机台名称转为机台code
            TqMachineInfo tqMachineInfo= new TqMachineInfo();
            tqMachineInfo.setStatus("0");
            List<TqMachineInfo> machineInfoList = machineInfoService.selectMachineInfoList(tqMachineInfo);
            if (CollectionUtils.isEmpty(machineInfoList)) {
                // 未查询到机台信息
                String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
                addImportErrorLog(importLogId, null, message, importErrorLogs);
                return AjaxResult.error(message, importErrorLogs);
            }

            //根据机台名称去重
            TreeSet<TqMachineInfo> treeSet = new TreeSet<TqMachineInfo>(new Comparator<TqMachineInfo>() {
                @Override
                public int compare(TqMachineInfo o1, TqMachineInfo o2) {
                    return o1.getMachineName().compareTo(o2.getMachineName());
                }
            });
            treeSet.addAll(machineInfoList);
            machineInfoList = new ArrayList<>(treeSet);


            Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(TqMachineInfo::getMachineName, TqMachineInfo::getId));
            //按业务主键分组
            Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(item -> item.getBeadCode() + item.getMachineId(), Collectors.counting()));

            for (int i = 0; i < list.size(); i++) {
                TqScheduleResultDto scheduleResultDto = list.get(i);
                scheduleResultDto.setDataSource("2");
                scheduleResultDto.setScheduleDate(scheduleDate);

                if (groupMap.get(scheduleResultDto.getBeadCode() + scheduleResultDto.getMachineId()) > 1) {
                    failureNum++;
                    String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                    String columnName = I18nUtil.getMessage("ui.data.column.quota.beadCode");
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
                    addImportErrorLog(importLogId, i + 3, I18nUtil.getMessage("ui.error.message.column.produceLineNotExist"), validated);
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
            }
            this.batchSaveTqSchedule(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, scheduleDate), importList);  //把验证成功的记录进行导入

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
     *
     * @param scheduleDate 排程日志，格式：yyyy-MM-dd
     * @param importList   导入数据
     */
    private void batchSaveTqSchedule(String scheduleDate, List<TqScheduleResultDto> importList) {
        List<TqScheduleResultVo> scheduleList = new ArrayList<>();
        for (TqScheduleResultDto result : importList) {
            TqScheduleResultVo vo = new TqScheduleResultVo();
            BeanUtils.copyProperties(result, vo);
            scheduleList.add(vo);
        }
        if (!scheduleList.isEmpty()) {
            this.tqEngineService.batchSaveTqSchedule(scheduleDate, scheduleList);
        }
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
     * 比较指定字段是否有被修改
     *
     * @param scheduleResult 前端传入对象
     * @param resultDto      查询到的数据
     * @return 是否被修改
     */
    private boolean compareFields(TqScheduleResult scheduleResult, TqScheduleResultDto resultDto) {
        boolean flag;
        flag = compare(resultDto.getMachineId(), scheduleResult.getMachineId());
        flag = flag && compare(resultDto.getDayPlanQty(), scheduleResult.getDayPlanQty());
        flag = flag && compare(resultDto.getNightPlanQty(), scheduleResult.getNightPlanQty());
        flag = flag && compare(resultDto.getMidPlanQty(), scheduleResult.getMidPlanQty());
        flag = flag && compare(resultDto.getNextMidPlanQty(), scheduleResult.getNextMidPlanQty());
        flag = flag && compare(resultDto.getDayHandAnalysis(), scheduleResult.getDayHandAnalysis());
        flag = flag && compare(resultDto.getNightHandAnalysis(), scheduleResult.getNightHandAnalysis());
        flag = flag && compare(resultDto.getMidHandAnalysis(), scheduleResult.getMidHandAnalysis());
        flag = flag && compare(resultDto.getNextMidHandAnalysis(), scheduleResult.getNextMidHandAnalysis());
        flag = flag && compare(resultDto.getDayProduceOrder(), scheduleResult.getDayProduceOrder());
        flag = flag && compare(resultDto.getNightProduceOrder(), scheduleResult.getNightProduceOrder());
        flag = flag && compare(resultDto.getMidProduceOrder(), scheduleResult.getMidProduceOrder());
        flag = flag && compare(resultDto.getNextMidProduceOrder(), scheduleResult.getNextMidProduceOrder());
        flag = flag && compare(resultDto.getRemark(), scheduleResult.getRemark());
        return flag;
    }

    public boolean compare(String str1, String str2) {
        return (StringUtils.isEmpty(str1) ? StringUtils.isEmpty(str2) : str1.equals(str2));
    }

    public boolean compare(Integer str1, Integer str2) {
        return (ObjectUtils.isEmpty(str1) ? ObjectUtils.isEmpty(str2) : str1.equals(str2));
    }

    /**
     * 选机台
     */
    public AjaxResult chooseMachine(TqScheduleResultDto dto) {
        TqScheduleResult scheduleResult = new TqScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        if (scheduleResultMapper.checkUnique(scheduleResult) > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }

        scheduleResult = new TqScheduleResult();
        this.tqEngineService.confirmTqMachine(dto); //最终确认机台
        BeanUtils.copyProperties(dto, scheduleResult);

        scheduleResult.setIsRelease(scheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
        scheduleResult.setBaseVale(scheduleResult.getId());
        saveOrUpdate(scheduleResult);
        return AjaxResult.success();
    }


    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @Override
    public int isReleasingOrTimeoutByDate(Date scheduleDate) {
        return scheduleResultMapper.isReleasingOrTimeoutByDate(scheduleDate);
    }

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param ids id
     * @return 查询到的记录数
     */
    @Override
    public int isReleasingOrTimeoutByIds(long[] ids) {
        return scheduleResultMapper.isReleasingOrTimeoutByIds(ids);
    }

    /**
     * 更改发布状态
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @Override
    public int changeReleaseStatus(TqScheduleResult entity) {
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setBaseVale(1L);
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_TQ);
        record.setScheduleDate(entity.getScheduleDate());
        record.setPublishStatus(entity.getIsRelease());
        scheduleResultMapper.updatePublishRecord(record);
        return scheduleResultMapper.changeReleaseStatus(entity);
    }

    @Override
    public int checkTqCodeExist(TqScheduleResultDto dto) {
        return scheduleResultMapper.checkTqCodeExist(dto);
    }

    @Override
    public int isPublishByIds(long[] ids) {
        return scheduleResultMapper.isPublishByIds(ids);
    }

    @Override
    public List<TqScheduleResultDto> selectByIds(List<Long> ids2) {
        return scheduleResultMapper.selectByIds(ids2);
    }

    public boolean compare(Double d1, Double d2) {
        d1 = ObjectUtils.isEmpty(d1) ? 0D : d1;
        d2 = ObjectUtils.isEmpty(d2) ? 0D : d2;
        return d1.equals(d2);
    }
}

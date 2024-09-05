package com.zlt.aps.lh.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.security.aspect.PreAuthorizeAspect;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.SchedulePublishRecord;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.common.engine.common.CxEngineQuotaCommonService;
import com.zlt.aps.common.engine.domain.EngineProductConstructionInfo;
import com.zlt.aps.common.engine.domain.LhEngineTireConstructionInfo;
import com.zlt.aps.common.engine.service.LhEngineTireConstructionInfoService;
import com.zlt.aps.common.engine.utils.DateUtil;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.lh.api.domain.dto.LhScheduleResultDto;
import com.zlt.aps.lh.api.domain.entity.Gante;
import com.zlt.aps.lh.api.domain.entity.LhDispatcherLog;
import com.zlt.aps.lh.api.domain.entity.LhMachineInfo;
import com.zlt.aps.lh.engine.mapper.CommonCxEngineMapper;
import com.zlt.aps.lh.engine.service.LhEngineService;
import com.zlt.aps.lh.engine.task.LhScheduleTaskCheck;
import com.zlt.aps.lh.entity.LhScheduleResult;
import com.zlt.aps.lh.mapper.LhScheduleResultMapper;
import com.zlt.aps.lh.service.LhDispatcherLogService;
import com.zlt.aps.lh.service.LhMachineInfoService;
import com.zlt.aps.lh.service.LhScheduleResultService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ApsCommonUtil.getIntOrDefault;
import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;


/**
 * 硫化排程结果Service业务层处理
 *
 * @author chen
 * @date 2021-07-19
 */
@Service
public class LhScheduleResultServiceImpl extends ServiceImpl<LhScheduleResultMapper, LhScheduleResult> implements LhScheduleResultService {
    @Autowired
    private LhScheduleResultMapper lhScheduleResultMapper;
    @Value("${excelModelPath}")
    private String excelModelPath;
    @Autowired
    private LhMachineInfoService lhMachineInfoService;


    @Autowired
    private LhEngineService lhEngineService;

    @Autowired
    private LhScheduleTaskCheck lhScheduleTaskCheck;


    @Resource
    private PreAuthorizeAspect preAuthorizeAspect;

    @Resource
    private LhDispatcherLogService lhDispatcherLogService;

    @Autowired
    private LhEngineTireConstructionInfoService lhEngineTireConstructionInfoService;
    @Autowired
    private CxEngineQuotaCommonService cxEngineQuotaCommonService;

    @Autowired
    private CommonCxEngineMapper commonCxEngineMapper;


    /**
     * 查询硫化排程结果
     *
     * @param id 硫化排程结果ID
     * @return 硫化排程结果
     */
    @Override
    public LhScheduleResultDto selectLhScheduleResultById(Long id) {
        return lhScheduleResultMapper.selectLhScheduleResultById(id);
    }

    /**
     * 查询硫化排程结果列表
     *
     * @param lhScheduleResult 硫化排程结果
     * @return 硫化排程结果
     */
    @Override
    public List<LhScheduleResultDto> selectLhScheduleResultList(LhScheduleResult lhScheduleResult) {
        List<LhScheduleResultDto> resultDtoList = lhScheduleResultMapper.selectLhScheduleResultList(lhScheduleResult);
        // Steve 相同SAP相同机台时，添加变色标识 start 2022年6月30日
        Map<String, Long> sapEmbryoCodeCountMap = resultDtoList.stream().collect(Collectors.groupingBy(item -> item.getSapCode() + item.getLhMachineCode(), Collectors.counting()));
        for (LhScheduleResultDto resultDto : resultDtoList) {
            Long count = sapEmbryoCodeCountMap.getOrDefault(resultDto.getSapCode() + resultDto.getLhMachineCode(), 0L);
            if (count > 1) {
                resultDto.setMultipleEmbryosOfSameSapFlag(count.toString());
            }
        }
        // Steve 相同SAP相同机台时，添加变色标识 end
        return resultDtoList;
    }

    /**
     * 新增硫化排程结果
     *
     * @param lhScheduleResult 硫化排程结果
     * @return 结果
     */
    @Override
    public int insertLhScheduleResult(LhScheduleResult lhScheduleResult) {
        // 插单操作
        List<LhScheduleResultDto> list = lhScheduleResultMapper.selectLhScheduleResultList(lhScheduleResult);
        if (CollectionUtils.isNotEmpty(list)) {
            // 已生成排程记录，直接获取批次号及成型批次号
            lhScheduleResult.setBatchNo(list.get(0).getBatchNo());
        }
        lhScheduleResult.setBaseVale(null);
        lhScheduleResult.setProductionStatus(ApsConstant.NO_PRODUNTION);
        lhScheduleResult.setIsRelease(ApsConstant.NO_RELEASE);
        return lhScheduleResultMapper.insertLhScheduleResult(lhScheduleResult);
    }

    /**
     * 修改硫化排程结果
     *
     * @param lhScheduleResult 硫化排程结果
     * @return 结果
     */
    @Override
    public int updateLhScheduleResult(LhScheduleResult lhScheduleResult) {
        // 修改操作 校验字段是否修改，修改则改状态为未发布
        boolean flag;
        LhScheduleResultDto resultDto = lhScheduleResultMapper.selectLhScheduleResultById(lhScheduleResult.getId());
        flag = compare(resultDto.getLhMachineCode(), lhScheduleResult.getLhMachineCode());
        flag = flag && compare(resultDto.getEmbryoCode(), lhScheduleResult.getEmbryoCode());
        flag = flag && compare(resultDto.getClass1PlanQty(), lhScheduleResult.getClass1PlanQty());
        flag = flag && compare(resultDto.getClass1AnalysisInput(), lhScheduleResult.getClass1AnalysisInput());
        flag = flag && compare(resultDto.getClass2PlanQty(), lhScheduleResult.getClass2PlanQty());
        flag = flag && compare(resultDto.getClass2AnalysisInput(), lhScheduleResult.getClass2AnalysisInput());
        flag = flag && compare(resultDto.getClass3PlanQty(), lhScheduleResult.getClass3PlanQty());
        flag = flag && compare(resultDto.getClass3AnalysisInput(), lhScheduleResult.getClass3AnalysisInput());
        flag = flag && compare(resultDto.getRemark(), lhScheduleResult.getRemark());

        if (!flag) {
            lhScheduleResult.setIsRelease(lhScheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
            LambdaUpdateWrapper<LhScheduleResult> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(LhScheduleResult::getId, lhScheduleResult.getId());
            wrapper.set(LhScheduleResult::getIsRelease, lhScheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
            update(wrapper);
        }
        lhScheduleResult.setBaseVale(lhScheduleResult.getId());
        return lhScheduleResultMapper.updateLhScheduleResult(lhScheduleResult);
    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule
     */
    public void insetDispatcherLog(String operType, LhScheduleResultDto oldSchedule, LhScheduleResultDto newSchedule) {
        // 20231018 需求确认单各个工序中，调度员操作日志，改成排程操作日志，统计全部人员的操作记录，调度员字段改为“操作人员”字段
        //        if(!preAuthorizeAspect.hasRole(ApsConstant.DISPATCHER_ROLE)) {
        //            return;
        //        }
        if(oldSchedule == null) {
            oldSchedule = this.lhScheduleResultMapper.selectLhScheduleResultById(newSchedule.getId());  //操作前的排程数据
        }
        LhDispatcherLog log = new LhDispatcherLog();
        //基础信息赋值
        log.setScheduleId(newSchedule.getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate());  //排程日期
        log.setSapCode(newSchedule.getSapCode());   //sap品号
        //操作前的信息赋值
        log.setBeforeMachineCode(oldSchedule.getLhMachineCode());
        log.setBeforeClass1Plan(oldSchedule.getClass1PlanQty());
        log.setBeforeClass2Plan(oldSchedule.getClass2PlanQty());
        log.setBeforeClass3Plan(oldSchedule.getClass3PlanQty());
        //操作后的信息赋值
        log.setAfterMachineCode(newSchedule.getLhMachineCode());
        log.setAfterClass1Plan(newSchedule.getClass1PlanQty());
        log.setAfterClass2Plan(newSchedule.getClass2PlanQty());
        log.setAfterClass3Plan(newSchedule.getClass3PlanQty());
        /** 调用插入日志方法 **/
        lhDispatcherLogService.insertLhDispatcherLog(log);
    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     *
     * @param operType        操作类型：0--转机台、1--调量、2--插单
     */
    @Override
    public void insetDispatcherLogInsertOrder(String operType, List<LhScheduleResultDto> scheduleResults, LhScheduleResultDto newSchedule) {
        // 20231018 需求确认单各个工序中，调度员操作日志，改成排程操作日志，统计全部人员的操作记录，调度员字段改为“操作人员”字段
        //        if(!preAuthorizeAspect.hasRole(ApsConstant.DISPATCHER_ROLE)) {
        //            return;
        //        }
        List<LhScheduleResultDto> scheduleResultList = this.selectByScheduleDateAndCode(newSchedule);
        LhDispatcherLog log = new LhDispatcherLog();
        //基础信息赋值
        log.setScheduleId(scheduleResultList.get(0).getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate());  //排程日期
        log.setSapCode(newSchedule.getSapCode());   //sap品号
        // 操作前的信息赋值，取创建时间最大的记录为操作前信息
        if (CollectionUtils.isNotEmpty(scheduleResults)) {
            Optional<LhScheduleResultDto> max = scheduleResults.stream().max(Comparator.comparing(LhScheduleResultDto::getCreateTime));
            if (max.isPresent()) {
                LhScheduleResultDto scheduleResult = max.get();
                log.setBeforeMachineCode(scheduleResult.getLhMachineCode());
                log.setBeforeClass1Plan(scheduleResult.getClass1PlanQty());
                log.setBeforeClass2Plan(scheduleResult.getClass2PlanQty());
                log.setBeforeClass3Plan(scheduleResult.getClass3PlanQty());
            }
        }
        //操作后的信息赋值
        log.setAfterMachineCode(newSchedule.getLhMachineCode());
        log.setAfterClass1Plan(newSchedule.getClass1PlanQty());
        log.setAfterClass2Plan(newSchedule.getClass2PlanQty());
        log.setAfterClass3Plan(newSchedule.getClass3PlanQty());
        /* 调用插入日志方法 **/
        lhDispatcherLogService.insertLhDispatcherLog(log);
    }

    /**
     * 根据排程日期、SAP查询记录
     * @return 查询到的记录
     */
    @Override
    public List<LhScheduleResultDto> selectByScheduleDateAndCode(LhScheduleResultDto scheduleResult) {
        return lhScheduleResultMapper.selectByScheduleDateAndCode(scheduleResult);
    }

    /**
     * 批量删除硫化排程结果
     *
     * @param ids 需要删除的硫化排程结果ID
     */
    @Override
    public void deleteLhScheduleResultByIds(long[] ids) {
        lhScheduleResultMapper.deleteLhScheduleResultByIds(ids);
    }

    /**
     * 删除硫化排程结果信息
     *
     * @param id 硫化排程结果ID
     * @return 结果
     */
    @Override
    public int deleteLhScheduleResultById(Long id) {
        return lhScheduleResultMapper.deleteLhScheduleResultById(id);
    }

    /**
     * 校验记录唯一性
     */
    @Override
    public String checkLhScheduleResultUnique(LhScheduleResult lhScheduleResult) {
        if (lhScheduleResult == null) {
            return UserConstants.NOT_UNIQUE;
        }
        int unique = lhScheduleResultMapper.checkUnique(lhScheduleResult);
        if (unique != 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导出excel表格
     *
     * @param list 要导出的数据集合
     * @return 字节数组
     */
    @Override
    public byte[] export(List<LhScheduleResultDto> list) {
//        this.summaryExport(list);  //给导出的数据增加汇总行
        // 按用户语言读取模板
        Locale lang = ServletUtils.getUserLang();
        InputStream in = null;
        if (Locale.SIMPLIFIED_CHINESE.equals(lang) || lang == null) {
            // 中文
            in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "lhScheduleResult.xlsx");
        } else if (Locale.US.equals(lang)) {
            // 英文
            in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "lhScheduleResult_en.xlsx");
        }
        Workbook webBook = ExcelUtils.readExcel(in);
        CellStyle cellStyle = ExcelUtils.createCellStyle(webBook);
        //填充数据
        if (CollectionUtils.isNotEmpty(list)) {
            List<LhMachineInfo> machineInfos = lhMachineInfoService.selectMachineInfoList(new LhMachineInfo());
            HashMap<String, String> map = new HashMap<>();
            for (LhMachineInfo machineInfo : machineInfos) {
                map.put(machineInfo.getMachineCode(), machineInfo.getMachineName());
            }
            Sheet sheet = webBook.getSheetAt(0);

            //重置表头基本信息
            String dateStr="";
            Locale langZh = ServletUtils.getUserLang();
            int month = com.zlt.aps.common.engine.utils.DateUtil.getMonth(list.get(0).getScheduleDate());
            int day = com.zlt.aps.common.engine.utils.DateUtil.getDay(list.get(0).getScheduleDate());
            if("zh_CN".equals(langZh.toString())){
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
                dateStr= DateUtil.getEngMonthDay(monthStr+dayStr) + " ";
            }
            Integer class1PlanQty=0;
            Integer class2PlanQty=0;
            Integer class3PlanQty=0;
            Integer totalPlan=0;
            for (LhScheduleResultDto csr:list){
                class1PlanQty=class1PlanQty+csr.getClass1PlanQty();
                class2PlanQty=class2PlanQty+csr.getClass2PlanQty();
                class3PlanQty=class3PlanQty+csr.getClass3PlanQty();
            }
            totalPlan=class1PlanQty+class2PlanQty+class3PlanQty;
            String baseInfo=I18nUtil.getMessage("ui.data.column.scheduleResult.lh.baseInfo");
            String class1Plan=I18nUtil.getMessage("ui.data.column.scheduleResult.class1Plan");
            String class2Plan=I18nUtil.getMessage("ui.data.column.scheduleResult.class2Plan");
            String class3Plan=I18nUtil.getMessage("ui.data.column.scheduleResult.class3Plan");
            String totalQty=I18nUtil.getMessage("ui.data.column.scheduleResult.totalQty");
            String planInfo = '：'+class1Plan+'：'+class1PlanQty+'，'+class2Plan+'：'+class2PlanQty+'，'+class3Plan+'：'+class3PlanQty+'，'+totalQty+'：'+totalPlan;
            baseInfo=dateStr+baseInfo+planInfo;
            Cell cell0=sheet.getRow(0).getCell(0);
            CellStyle cellStyle0=cell0.getCellStyle();
            cell0.setCellValue(baseInfo);
            cell0.setCellStyle(cellStyle0);

            webBook.setSheetName(0, I18nUtil.getMessage("ui.data.column.lh.scheduleResult.modelName"));
            for (int i = 0; i < list.size(); i++) {
                int cellNum = 0;
                LhScheduleResultDto scheduleResult = list.get(i);
                Row row = sheet.createRow(i + 2);
                // 导出机台名称
                StringBuilder machineName = new StringBuilder();
                if (StringUtils.isNotBlank(scheduleResult.getLhMachineCode())) {
                    String machineCodeStr = scheduleResult.getLhMachineCode();
                    String[] machineCodes = machineCodeStr.split(",");
                    for (String machineCode : machineCodes) {
                        machineName.append(map.get(machineCode) == null ? "" : map.get(machineCode)).append(",");
                    }
                    if (StringUtils.isNotEmpty(machineName)) {
                        machineName = new StringBuilder(machineName.substring(0, machineName.length() - 1));
                    }
                }
                row.createCell(cellNum++).setCellValue(row.getRowNum() - 1);
                row.createCell(cellNum++).setCellValue(DateFormatUtils.format(scheduleResult.getScheduleDate(), "yyyy-MM-dd"));
                row.createCell(cellNum++).setCellValue(machineName.toString());
                row.createCell(cellNum++).setCellValue(scheduleResult.getLeftRightMold() == null ? "" : scheduleResult.getLeftRightMold());
                row.createCell(cellNum++).setCellValue(scheduleResult.getSapCode() == null ? "" : scheduleResult.getSapCode());
                row.createCell(cellNum++).setCellValue(scheduleResult.getSpecDesc() == null ? "" : scheduleResult.getSpecDesc());
                row.createCell(cellNum++).setCellValue(scheduleResult.getLhTime() == null ? 0 : scheduleResult.getLhTime());
                row.createCell(cellNum++).setCellValue(scheduleResult.getDailyPlanQty() == null ? 0 : scheduleResult.getDailyPlanQty());
                row.createCell(cellNum++).setCellValue(scheduleResult.getClass1PlanQty() == null ? 0 : scheduleResult.getClass1PlanQty());
                row.createCell(cellNum++).setCellValue(scheduleResult.getClass1FinishQty() == null ? 0 : scheduleResult.getClass1FinishQty());
                String class1SysAnalysis = scheduleResult.getClass1Analysis();
                String class1HandAnalysis = scheduleResult.getClass1AnalysisInput();
                String class1Analysis = "";
                if (StringUtils.isNotEmpty(class1SysAnalysis)) {
                    class1Analysis = class1Analysis + class1SysAnalysis;
                }
                if (StringUtils.isNotEmpty(class1HandAnalysis)) {
                    if (StringUtils.isNotEmpty(class1Analysis)) {
                        class1Analysis = class1Analysis + "," + class1HandAnalysis;
                    } else {
                        class1Analysis = class1HandAnalysis;
                    }
                }
                row.createCell(cellNum++).setCellValue(class1Analysis);
                row.createCell(cellNum++).setCellValue(scheduleResult.getClass2PlanQty() == null ? 0 : scheduleResult.getClass2PlanQty());
                row.createCell(cellNum++).setCellValue(scheduleResult.getClass2FinishQty() == null ? 0 : scheduleResult.getClass2FinishQty());
                String class2SysAnalysis = scheduleResult.getClass2Analysis();
                String class2HandAnalysis = scheduleResult.getClass2AnalysisInput();
                String class2Analysis = "";
                if (StringUtils.isNotEmpty(class2SysAnalysis)) {
                    class2Analysis = class2Analysis + class2SysAnalysis;
                }
                if (StringUtils.isNotEmpty(class2HandAnalysis)) {
                    if (StringUtils.isNotEmpty(class2Analysis)) {
                        class2Analysis = class2Analysis + "," + class2HandAnalysis;
                    } else {
                        class2Analysis = class2HandAnalysis;
                    }
                }
                row.createCell(cellNum++).setCellValue(class2Analysis);
                row.createCell(cellNum++).setCellValue(scheduleResult.getClass3PlanQty() == null ? 0 : scheduleResult.getClass3PlanQty());
                row.createCell(cellNum++).setCellValue(scheduleResult.getClass3FinishQty() == null ? 0 : scheduleResult.getClass3FinishQty());
                String class3SysAnalysis = scheduleResult.getClass3Analysis();
                String class3HandAnalysis = scheduleResult.getClass3AnalysisInput();
                String class3Analysis = "";
                if (StringUtils.isNotEmpty(class3SysAnalysis)) {
                    class3Analysis = class3Analysis + class3SysAnalysis;
                }
                if (StringUtils.isNotEmpty(class3HandAnalysis)) {
                    if (StringUtils.isNotEmpty(class3Analysis)) {
                        class3Analysis = class3Analysis + "," + class3HandAnalysis;
                    } else {
                        class3Analysis = class3HandAnalysis;
                    }
                }
                row.createCell(cellNum).setCellValue(class3Analysis);
                setCellStyle(row, row.getPhysicalNumberOfCells(), cellStyle);
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
    private void summaryExport(List<LhScheduleResultDto> list) {
        if(list == null || list.isEmpty()) {
            return;
        }
        LhScheduleResultDto summary = new  LhScheduleResultDto();
        summary.setSapCode(I18nUtil.getMessage("ui.data.column.scheduleResult.totalQty"));
        summary.setClass1PlanQty(list.stream().mapToInt(r->getIntOrDefault(r.getClass1PlanQty())).sum());
        summary.setClass1FinishQty(list.stream().mapToInt(r->getIntOrDefault(r.getClass1FinishQty())).sum());
        summary.setClass2PlanQty(list.stream().mapToInt(r->getIntOrDefault(r.getClass2PlanQty())).sum());
        summary.setClass2FinishQty(list.stream().mapToInt(r->getIntOrDefault(r.getClass2FinishQty())).sum());
        summary.setClass3PlanQty(list.stream().mapToInt(r->getIntOrDefault(r.getClass3PlanQty())).sum());
        summary.setClass3FinishQty(list.stream().mapToInt(r->getIntOrDefault(r.getClass3FinishQty())).sum());
        summary.setDailyPlanQty(summary.getClass1PlanQty() + summary.getClass2PlanQty() + summary.getClass3PlanQty());
        list.add(summary);
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
     * 导入数据，并保存记录
     *
     * @param list         要导入数据
     * @param importLogId  导入日志id
     * @param scheduleDate
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importData(List<LhScheduleResultDto> list, Long importLogId, Date scheduleDate) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<LhScheduleResultDto> importList = new ArrayList<>();

        try {
            //将机台名称转为机台code
            LhMachineInfo lhMachineInfo= new LhMachineInfo();
            lhMachineInfo.setStatus("0");
            List<LhMachineInfo> machineInfoList = lhMachineInfoService.selectMachineInfoList(lhMachineInfo);
            if (CollectionUtils.isEmpty(machineInfoList)) {
                // 未查询到机台信息
                String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
                addImportErrorLog(importLogId, null, message, importErrorLogs);
                return AjaxResult.error(message, importErrorLogs);
            }
            Map<String, String> machineCodeMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(machineInfoList)) {

                //根据机台名称去重
                TreeSet<LhMachineInfo> treeSet = new TreeSet<LhMachineInfo>(new Comparator<LhMachineInfo>() {
                    @Override
                    public int compare(LhMachineInfo o1, LhMachineInfo o2) {
                        return o1.getMachineName().compareTo(o2.getMachineName());
                    }
                });
                treeSet.addAll(machineInfoList);
                machineInfoList =new ArrayList<>(treeSet);

                machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(LhMachineInfo::getMachineName, LhMachineInfo::getMachineCode));
            }

            //按业务主键分组
            Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(item -> item.getSapCode() + item.getLhMachineName() + item.getLeftRightMold(), Collectors.counting()));
            //获取胎胚施工信息
            Map<String, EngineProductConstructionInfo> engineConstructionInfoMap=cxEngineQuotaCommonService.loadEngineConstructionMapFromRedis();
            //硫化外胎施工信息start
            LhEngineTireConstructionInfo condition=new LhEngineTireConstructionInfo();
            List<LhEngineTireConstructionInfo> constructionInfoList=lhEngineTireConstructionInfoService.selectLhTireConstructionInfoList(condition);
            Map<String,List<LhEngineTireConstructionInfo>> sapTireConstructionListMap=new HashMap<>();
            if(StringUtils.isNotEmpty(constructionInfoList)){
                sapTireConstructionListMap=constructionInfoList.stream().collect(Collectors.groupingBy(lhEngineScheduleResult -> lhEngineScheduleResult.getSapCode()));
            }
            //硫化外胎施工信息end

            //初始化成型前一天的库存信息start
            String lastDateStr= DateUtils.parseDateToStr("yyyy-MM-dd",DateUtils.addDays(scheduleDate,-1));
            CxStock stockCondition=new CxStock();
            stockCondition.setStockDateStr(lastDateStr);
            List<CxStock> stockList =this.commonCxEngineMapper.selectMergeCxStockList(stockCondition);
            Map<String, Integer> embryoCodeStockMap =stockList.stream().collect(Collectors.toMap(CxStock::getEmbryoCode,CxStock::getStockRealNum));
            //初始化成型前一天的库存信息end
            for (int i = 0; i < list.size(); i++) {
                LhScheduleResultDto scheduleResultDto = list.get(i);
                scheduleResultDto.setDataSource("2");
                scheduleResultDto.setScheduleDate(scheduleDate);

                /*if (groupMap.get(scheduleResultDto.getSapCode() + scheduleResultDto.getLhMachineName()  + scheduleResultDto.getLeftRightMold() ) > 1) {
                    failureNum++;
                    String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                    String columnName = I18nUtil.getMessage("ui.data.column.scheduleResult.sapCode");
                    String columnName2 = I18nUtil.getMessage("ui.data.column.scheduleResult.lhMachineName");
                    message=String.format(message,columnName+"+"+columnName2);
                    addImportErrorLog(importLogId, i + 4,message, importErrorLogs);
                    continue;
                }*/

                int errorNum = i + 4;
                List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, scheduleResultDto);

                if (machineCodeMap.get(scheduleResultDto.getLhMachineName())==null) {
                    addImportErrorLog(importLogId, i + 4, I18nUtil.getMessage("ui.error.message.column.machineNotExist"), validated);
                }
                scheduleResultDto.setLhMachineCode(machineCodeMap.get(scheduleResultDto.getLhMachineName()));

                //Joran 2021-09-13 添加验证sap信息和规格设置信息start
                StringBuilder errorDetail=new StringBuilder();
                lhScheduleTaskCheck.validateSapCodeByConstruction(scheduleResultDto,sapTireConstructionListMap,engineConstructionInfoMap,errorDetail);
                if(StringUtils.isNotEmpty(errorDetail)){
                    addImportErrorLog(importLogId, errorNum, errorDetail.toString(), validated);
                }
                //Joran 2021-09-13 添加验证sap信息和规格设置信息end

                // Steve 2022-06-14 添加验证获取胎胚代码 start
                if(StringUtils.isNotEmpty(sapTireConstructionListMap)&&sapTireConstructionListMap.containsKey(scheduleResultDto.getSapCode())){
                    List<LhEngineTireConstructionInfo> constructionInfos = sapTireConstructionListMap.get(scheduleResultDto.getSapCode());
                    constructionInfos = constructionInfos.stream().collect(Collectors.collectingAndThen
                            (Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing
                                    (LhEngineTireConstructionInfo::getEmbryoCode))), ArrayList::new));
                    if (constructionInfos.size() == 1) {
                        scheduleResultDto.setEmbryoCode(constructionInfos.get(0).getEmbryoCode());
                    }else if(constructionInfos.size()>1){
                        setEmbryoCodeByConstruction(scheduleResultDto,constructionInfos,embryoCodeStockMap);
                    }
                }

                // Steve 2022-06-14 添加验证获取胎胚代码 end

                if (CollectionUtils.isNotEmpty(validated)) {
                    failureNum++;
                    importErrorLogs.addAll(validated);
                } else {
                    successNum++;
                    scheduleResultDto.setBaseVale(null);
                    importList.add(scheduleResultDto);
                }
                //Joran 2022-04-18 控制台打印进行调整控制start
                if(log.isDebugEnabled()){
                    log.debug("--------------------------");
                    log.debug(scheduleResultDto.toString());
                    log.debug("--------------------------");
                }
                //Joran 2022-04-18 控制台打印进行调整控制end

            }

            // TODO 调用导入功能，传入 importList
            if(CollectionUtils.isNotEmpty(importList)){
                lhEngineService.lhScheduleResultImport(importList,sapTireConstructionListMap,scheduleDate);
            }
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
     * 结合施工信息+胎胚库存进行胎胚代码设定
     * @param scheduleResultDto
     * @param constructionInfos
     * @param embryoCodeStockMap
     */
    private void setEmbryoCodeByConstruction(LhScheduleResultDto scheduleResultDto, List<LhEngineTireConstructionInfo> constructionInfos, Map<String, Integer> embryoCodeStockMap) {
        if(StringUtils.isEmpty(embryoCodeStockMap)){
            return;
        }
        Set<String> embryoCodeSet=new HashSet<>();
        for(LhEngineTireConstructionInfo lhEngineTireConstructionInfo:constructionInfos){
            String embryoCode= lhEngineTireConstructionInfo.getEmbryoCode();
            if(embryoCodeStockMap.containsKey(embryoCode)&&embryoCodeStockMap.get(embryoCode)>0){
                embryoCodeSet.add(embryoCode);
            }
        }

        if(embryoCodeSet!=null &&!embryoCodeSet.isEmpty()&&embryoCodeSet.size()==1){
            scheduleResultDto.setEmbryoCode(embryoCodeSet.iterator().next());
        }

    }

    public boolean compare(String str1, String str2) {
        return (StringUtils.isEmpty(str1) ? StringUtils.isEmpty(str2) : str1.equals(str2));
    }

    /**
     * 排程发布
     */
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult publish(long[] ids, Date scheduleDate,String dataVersion,String factoryCode,String companyCode) {

        //数据同步
        lhScheduleResultMapper.deployScheduleToMes(dataVersion, ids, factoryCode, companyCode);

        //保存发布记录，更新发布状态
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setBaseVale(null);
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_LH);
        record.setScheduleDate(scheduleDate);
        record.setPublishStatus(ApsConstant.RELEASING);
        //Joran 2022-03-09记录发布对应的数据版本号
        record.setDataVersion(dataVersion);
        lhScheduleResultMapper.insertPublishRecord(record);
        lhScheduleResultMapper.batchUpdate(ids, ApsConstant.RELEASING);
        return AjaxResult.success(I18nUtil.getMessage("ui.data.column.scheduleResult.successPublish"));
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
        lhScheduleResultMapper.batchUpdate(ids, status);
        lhScheduleResultMapper.updatePublishRecordVersion(dataVersion, status);
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
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_LH);
        record.setScheduleDate(scheduleDate);
        return lhScheduleResultMapper.isPublish(record) > 0;
    }

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @Override
    public int isReleasingOrTimeoutByDate(Date scheduleDate) {
        return lhScheduleResultMapper.isReleasingOrTimeoutByDate(scheduleDate);
    }

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param ids id
     * @return 查询到的记录数
     */
    @Override
    public int isReleasingOrTimeoutByIds(long[] ids) {
        return lhScheduleResultMapper.isReleasingOrTimeoutByIds(ids);
    }

    /**
     * 更改发布状态
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @Override
    public int changeReleaseStatus(LhScheduleResult entity) {
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setBaseVale(1L);
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_LH);
        record.setScheduleDate(entity.getScheduleDate());
        record.setPublishStatus(entity.getIsRelease());
        lhScheduleResultMapper.updatePublishRecord(record);
        return lhScheduleResultMapper.changeReleaseStatus(entity);
    }

    @Override
    public int isPublishByIds(long[] ids) {
        return lhScheduleResultMapper.isPublishByIds(ids);
    }

    public boolean compare(Integer i1, Integer i2) {
        i1 = ObjectUtils.isEmpty(i1) ? 0 : i1;
        i2 = ObjectUtils.isEmpty(i2) ? 0 : i2;
        return i1.equals(i2);
    }

    /**
     * 查询成型排程机台甘特图数据
     */
    public List<Gante> getLhGanteData(Gante gante) {
        //机台甘特图
        List<Gante> newGanteList=new ArrayList<>();
        if (gante.getFlag() == 1) {
            List<Gante> ganteList = lhScheduleResultMapper.getLhGanteData(gante);
            if (CollectionUtils.isNotEmpty(ganteList)) {
                for (Gante item : ganteList) {

                    //构造开始日、结束日、开始时刻、结束时刻、起点位置、时差;
                    String scheduleDay = DateUtils.getDay(item.getScheduleDate())+"";
                    String startDay = DateUtils.getDay(item.getStartDate())+"";
                    String endDay = DateUtils.getDay(item.getEndDate())+"";
                    int startHours = DateUtils.getHour(item.getStartDate());
                    int endHours = DateUtils.getHour(item.getEndDate());
                    int dayInterval = DateUtils.getDayInterval(item.getEndDate(), item.getStartDate());
                    int dayInterval2 = DateUtils.getDayInterval(item.getScheduleDate(), item.getStartDate());

                    //计算以下三个值，用户画甘特图
                    //算起点位置：后端给72小时制的起始时刻
                    //算长条宽度：小时差*25：(endHour-startHour+1)*25，后端给时差;
                    //算margin-left宽度：固定值*天数，不用后端给

                    if (dayInterval2>0){ //起始日期在排程日期前
                        item.setHourStart(startHours);
                    }else if (dayInterval2==0){ //起始日期就是排程日期
                        item.setHourStart(startHours+24);
                    }else{ //起始日期在排程日期后
                        item.setHourStart(startHours+48);
                    }

                    //跨天存在前一天数据
                    if (!startDay.equals(endDay) && scheduleDay.equals(endDay)){
                        item.setHourInterval(24-startHours+endHours);
                        //跨多天
                        if (dayInterval>1){
                            item.setHourInterval(24-startHours+24*(dayInterval-1)+endHours);
                        }
                    }else if (!startDay.equals(endDay) && !scheduleDay.equals(endDay)) {
                        item.setHourInterval(24-startHours+endHours);
                        //跨多天
                        if (dayInterval>1){
                            item.setHourInterval(24-startHours+24*(dayInterval-1)+endHours);
                        }
                    }else{
                        item.setHourInterval(endHours-startHours);
                    }

                    item.setStartDay(startDay + "");
                    item.setEndDay(endDay + "");
                    item.setStartHour(startHours + "");
                    item.setEndHour(endHours + "");
                    newGanteList.add(item);
                }
            }
            return newGanteList;
        } else if (gante.getFlag() == 2) {
            //规格甘特图
            List<Gante> ganteList = lhScheduleResultMapper.getLhSpecGanteData(gante);
            //构造开始日、结束日
            if (CollectionUtils.isNotEmpty(ganteList)) {
                for (Gante item : ganteList) {
                    //判断是否跨月
                    int scheduleMonth = DateUtils.getMonth(item.getScheduleDate());
                    int startMonth = DateUtils.getMonth(item.getStartDate());
                    int endMonth = DateUtils.getMonth(item.getEndDate());
                    if (startMonth != scheduleMonth && endMonth != scheduleMonth) {
                        continue;
                    }
                    //构造开始日、结束日
                    String startDay = DateUtils.getDay(item.getStartDate())+"";
                    String endDay = DateUtils.getDay(item.getEndDate())+"";
                    item.setStartDay(startDay);
                    item.setEndDay(endDay);

                    //判断是否跨月
                    if (startMonth != endMonth && endMonth == scheduleMonth) { //月初跨月
                        item.setStartDay("1");
                    }
                    if (startMonth != endMonth && startMonth == scheduleMonth) {  //月末跨月
                        item.setEndDay(DateUtils.getLastDay(item.getStartDate()).substring(8));
                    }
                    newGanteList.add(item);
                }
            }
            return newGanteList;
        }
        return new ArrayList<>();
    }


}

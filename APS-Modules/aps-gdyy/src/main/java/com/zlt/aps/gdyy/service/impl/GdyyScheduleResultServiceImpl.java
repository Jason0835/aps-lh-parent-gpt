package com.zlt.aps.gdyy.service.impl;

import static com.zlt.aps.common.core.utils.ApsCommonUtil.getDoubleOrDefault;
import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.zlt.aps.common.core.enums.HalfComponentFinishTableEnum;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.domain.CxTCd15BigRoll;
import com.zlt.aps.common.engine.domain.CxTCd15Params;
import com.zlt.aps.common.engine.domain.ScheduleSummaryVo;
import com.zlt.aps.common.engine.service.CxTCd15BigRollService;
import com.zlt.aps.common.engine.service.CxTCd15ParamsService;
import com.zlt.aps.common.engine.service.FactoryService;
import com.zlt.aps.common.engine.service.impl.BaseFinishQtyImportService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.common.engine.utils.DateUtil;
import com.zlt.aps.gdyy.api.domain.dto.GdyyScheduleResultDto;
import com.zlt.aps.gdyy.api.domain.entity.GdyyDayFinishQty;
import com.zlt.aps.gdyy.api.domain.entity.GdyyDispatcherLog;
import com.zlt.aps.gdyy.api.domain.entity.GdyyMachineInfo;
import com.zlt.aps.gdyy.engine.service.GdyyEngineService;
import com.zlt.aps.gdyy.engine.vo.GdyyBigRollVo;
import com.zlt.aps.gdyy.entity.GdyyParams;
import com.zlt.aps.gdyy.entity.GdyyScheduleResult;
import com.zlt.aps.gdyy.mapper.GdyyMachineInfoMapper;
import com.zlt.aps.gdyy.mapper.GdyyParamsMapper;
import com.zlt.aps.gdyy.mapper.GdyyScheduleResultMapper;
import com.zlt.aps.gdyy.service.GdyyDispatcherLogService;
import com.zlt.aps.gdyy.service.GdyyScheduleResultService;


/**
 * 钢带压延排程结果Service业务层处理
 *
 * @author chen
 * @date 2021-07-05
 */
@Service
public class GdyyScheduleResultServiceImpl extends ServiceImpl<GdyyScheduleResultMapper, GdyyScheduleResult> implements GdyyScheduleResultService {
    @Autowired
    private GdyyScheduleResultMapper gdyyScheduleResultMapper;
    @Value("${excelModelPath}")
    private String excelModelPath;
    @Autowired
    private GdyyEngineService gdyyEngineService;
    @Autowired
    private CxTCd15BigRollService cd15BigRollService;
    @Autowired
    private CxTCd15ParamsService cd15ParamsService;
    @Resource
    private PreAuthorizeAspect preAuthorizeAspect;
    @Resource
    private GdyyDispatcherLogService gdyyDispatcherLogService;

    @Autowired
    private GdyyMachineInfoMapper gdyyMachineInfoMapper;

    @Autowired
    private GdyyParamsMapper gdyyParamsMapper;

    @Autowired
    private FactoryService factoryService;

    /**
     * 大卷标准长度默认值：600
     */
    private final static Double DEFAULT_STANDARD_SIZE = new Double("600");

    @Autowired
    private BaseFinishQtyImportService baseFinishQtyImportService;

    /**
     * 查询钢带压延排程结果信息维护列表
     *
     * @param scheduleResult 钢带压延排程结果信息维护
     * @return 钢带压延排程结果信息维护集合
     */
    @Override
    public List<GdyyScheduleResultDto> selectScheduleResultList(GdyyScheduleResult scheduleResult) {
        List<GdyyScheduleResultDto> list = gdyyScheduleResultMapper.selectScheduleResultList(scheduleResult);
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        // 大卷信息
        List<String> bigRollCodeList = CollectionUtil.propertiesToList(list, GdyyScheduleResultDto::getBigRollCode);
        List<CxTCd15BigRoll> cd15BigRollList = cd15BigRollService.getByBeltSpecList(bigRollCodeList);
        Map<String, CxTCd15BigRoll> cd15BigRollMap = CollectionUtil.toMap(cd15BigRollList, CxTCd15BigRoll::getBigRollCode);

        List<GdyyMachineInfo> machineInfoList = gdyyMachineInfoMapper.selectMachineInfoList(new GdyyMachineInfo());
        Map<Long, GdyyMachineInfo> machineInfoMap = machineInfoList.stream().collect(Collectors.toMap(GdyyMachineInfo::getId, Function.identity(), (s1, s2) -> s1));

        // 大卷默认信息
        CxTCd15Params cd15Params = cd15ParamsService.getByParamCode(EngineConstants.STANDARD_SIZE);
        // 分别计算每条钢带压延无库存个数
        for (GdyyScheduleResultDto dto : list) {
            CxTCd15BigRoll cd15BigRoll = cd15BigRollMap.get(dto.getBigRollCode());
            BigDecimal class1NoStockNum = BigDecimal.ZERO;
            BigDecimal class2NoStockNum = BigDecimal.ZERO;
            BigDecimal class3NoStockNum = BigDecimal.ZERO;
            dto.setActClothLength(BigDecimal.ZERO);
            if (cd15BigRoll != null && cd15BigRoll.getActClothLength() != null && !cd15BigRoll.getActClothLength().equals(BigDecimal.ZERO)) {
                class1NoStockNum = BigDecimal.valueOf(ObjectUtils.defaultIfNull(dto.getClass1PlanNoStock(), 0d)).divide(cd15BigRoll.getActClothLength(), 1, BigDecimal.ROUND_UP);
                class2NoStockNum = BigDecimal.valueOf(ObjectUtils.defaultIfNull(dto.getClass2PlanNoStock(), 0d)).divide(cd15BigRoll.getActClothLength(), 1, BigDecimal.ROUND_UP);
                class3NoStockNum = BigDecimal.valueOf(ObjectUtils.defaultIfNull(dto.getClass3PlanNoStock(), 0d)).divide(cd15BigRoll.getActClothLength(), 1, BigDecimal.ROUND_UP);
                dto.setActClothLength(cd15BigRoll.getActClothLength());
            } else if (cd15Params != null && StringUtils.isNotBlank(cd15Params.getParamValue()) && !Double.valueOf(cd15Params.getParamValue()).equals(0d)) {
                class1NoStockNum = BigDecimal.valueOf(ObjectUtils.defaultIfNull(dto.getClass1PlanNoStock(), 0d)).divide(BigDecimal.valueOf(Double.parseDouble(cd15Params.getParamValue())), 1, BigDecimal.ROUND_UP);
                class2NoStockNum = BigDecimal.valueOf(ObjectUtils.defaultIfNull(dto.getClass2PlanNoStock(), 0d)).divide(BigDecimal.valueOf(Double.parseDouble(cd15Params.getParamValue())), 1, BigDecimal.ROUND_UP);
                class3NoStockNum = BigDecimal.valueOf(ObjectUtils.defaultIfNull(dto.getClass3PlanNoStock(), 0d)).divide(BigDecimal.valueOf(Double.parseDouble(cd15Params.getParamValue())), 1, BigDecimal.ROUND_UP);
                dto.setActClothLength(BigDecimal.valueOf(Double.parseDouble(cd15Params.getParamValue())));
            }
            dto.setClass1PlanNoStockNum(class1NoStockNum.doubleValue());
            dto.setClass2PlanNoStockNum(class2NoStockNum.doubleValue());
            dto.setClass3PlanNoStockNum(class3NoStockNum.doubleValue());

            String machineIdStr = dto.getMachineCode();
            if (StringUtils.isNotBlank(machineIdStr)) {
                List<String> machineNameList = new ArrayList<>();
                String[] machineIdArr = machineIdStr.split(",");
                for (String machineId : machineIdArr) {
                    Long key = null;
                    try {
                        key = Long.valueOf(machineId);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        continue;
                    }
                    if (machineInfoMap.containsKey(key)) {
                        GdyyMachineInfo machineInfo = machineInfoMap.get(key);
                        machineNameList.add(machineInfo.getMachineName());
                    }
                }
                dto.setMachineName(String.join(",", machineNameList));
            }
        }
        return list;
    }

    /**
     * 查询钢带压延排程结果信息维护列表
     *
     * @param id 要查询的id
     * @return 钢带压延排程结果信息维护集合
     */
    @Override
    public GdyyScheduleResultDto selectScheduleResultById(Long id) {
        return gdyyScheduleResultMapper.selectScheduleResultById(id);
    }

    /**
     * 保存钢带压延排程结果信息维护
     *
     * @param scheduleResult 钢带压延排程结果信息维护
     */
    @Override
    public void saveScheduleResult(GdyyScheduleResult scheduleResult) {
        // 校验字段是否修改，修改则改状态为未发布
        if (scheduleResult.getId() != null) {
            GdyyScheduleResultDto resultDto = gdyyScheduleResultMapper.selectScheduleResultById(scheduleResult.getId());
            boolean flag = compareFields(scheduleResult, resultDto);
            if (!flag) {
                scheduleResult.setIsRelease(scheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
            }
            scheduleResult.setBaseVale(scheduleResult.getId());
            // 根据计划量计算对应个数
            this.calculatePlanNum(scheduleResult);
            saveOrUpdate(scheduleResult);
        } else {
            // 插单操作，直接调用引擎插单接口
            scheduleResult.setBaseVale(null);
            GdyyScheduleResultDto scheduleResultDto = new GdyyScheduleResultDto();
            BeanUtils.copyProperties(scheduleResult, scheduleResultDto);
            List<GdyyScheduleResult> scheduleResults = this.selectByScheduleDateAndCode(scheduleResult);
            gdyyEngineService.insertGdyyOrder(scheduleResultDto);
            this.insetDispatcherLogInsertOrder(ApsConstant.DISPATCHER_OPER_INSERT_ORDER, scheduleResults, scheduleResult);
        }
    }

    /**
     * 根据计划量计算对应个数
     * @param scheduleResult 计划量
     */
    private void calculatePlanNum(GdyyScheduleResult scheduleResult) {
        // 标准大卷长度默认值
        LambdaQueryWrapper<GdyyParams> paramsWrapper = new LambdaQueryWrapper<>();
        List<GdyyParams> gdyyParamsList = gdyyParamsMapper.selectList(paramsWrapper);
        Map<String, String> paramsMap = gdyyParamsList.stream().collect(Collectors.toMap(GdyyParams::getParamCode, GdyyParams::getParamValue, (v1, v2) -> v2));
        Double standardSize = getDoubleOrDefault(paramsMap.get(EngineConstants.STANDARD_SIZE), DEFAULT_STANDARD_SIZE);
        String bigRollCode = scheduleResult.getBigRollCode();
        // 获取大卷信息
        Map<String, BigDecimal> bigRollMap = gdyyScheduleResultMapper.listCd15BigRoll().stream()
                .collect(Collectors.toMap(GdyyBigRollVo::getBigRollCode, GdyyBigRollVo::getClothLength));
        // 大卷标准长度，没有则获取默认长度
        BigDecimal newStandardSize = bigRollMap.getOrDefault(bigRollCode, BigDecimal.valueOf(standardSize));
        BigDecimal class1Plan = BigDecimalUtil.getValue(scheduleResult.getClass1Plan());
        BigDecimal class2Plan = BigDecimalUtil.getValue(scheduleResult.getClass2Plan());
        BigDecimal class3Plan = BigDecimalUtil.getValue(scheduleResult.getClass3Plan());
        // 计算大卷数 = 计划量 / 标准长度
        BigDecimal class1PlanNum = class1Plan.divide(newStandardSize, 1, RoundingMode.UP);
        BigDecimal class2PlanNum = class2Plan.divide(newStandardSize, 1, RoundingMode.UP);
        BigDecimal class3PlanNum = class3Plan.divide(newStandardSize, 1, RoundingMode.UP);
        scheduleResult.setClass1PlanNum(class1PlanNum.doubleValue());
        scheduleResult.setClass2PlanNum(class2PlanNum.doubleValue());
        scheduleResult.setClass3PlanNum(class3PlanNum.doubleValue());
    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule
     */
    public void insetDispatcherLog(String operType, GdyyScheduleResult newSchedule) {
        // 20231018 需求确认单各个工序中，调度员操作日志，改成排程操作日志，统计全部人员的操作记录，调度员字段改为“操作人员”字段
        //        if(!preAuthorizeAspect.hasRole(ApsConstant.DISPATCHER_ROLE)) {
        //            return;
        //        }
        GdyyScheduleResultDto oldSchedule = this.gdyyScheduleResultMapper.selectScheduleResultById(newSchedule.getId());  //操作前的排程数据
        GdyyDispatcherLog log = new GdyyDispatcherLog();
        log.setBaseVale(null);
        //基础信息赋值
        log.setScheduleId(newSchedule.getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate());  //排程日期
        log.setMaterialCode(newSchedule.getBigRollCode());    //钢压大卷编号
        //操作前的信息赋值
        log.setBeforeMachineId(oldSchedule.getMachineCode());
        log.setBeforeMidPlan(oldSchedule.getClass1Plan());
        log.setBeforeNightPlan(oldSchedule.getClass2Plan());
        log.setBeforeDayPlan(oldSchedule.getClass3Plan());
        //操作后的信息赋值
        log.setAfterMachineId(newSchedule.getMachineCode());
        log.setAfterMidPlan(newSchedule.getClass1Plan());
        log.setAfterNightPlan(newSchedule.getClass2Plan());
        log.setAfterDayPlan(newSchedule.getClass3Plan());
        /** 调用插入日志方法 **/
        gdyyDispatcherLogService.insertGdyyDispatcherLog(log);
    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     *
     * @param operType        操作类型：0--转机台、1--调量、2--插单
     */
    @Override
    public void insetDispatcherLogInsertOrder(String operType, List<GdyyScheduleResult> scheduleResults, GdyyScheduleResult newSchedule) {
        // 20231018 需求确认单各个工序中，调度员操作日志，改成排程操作日志，统计全部人员的操作记录，调度员字段改为“操作人员”字段
        //        if(!preAuthorizeAspect.hasRole(ApsConstant.DISPATCHER_ROLE)) {
        //            return;
        //        }
        List<GdyyScheduleResult> scheduleResultList = this.selectByScheduleDateAndCode(newSchedule);
        GdyyDispatcherLog log = new GdyyDispatcherLog();
        //基础信息赋值
        log.setScheduleId(scheduleResultList.get(0).getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate());  //排程日期
        log.setMaterialCode(newSchedule.getBigRollCode());
        // 操作前的信息赋值，取创建时间最大的记录为操作前信息
        if (CollectionUtils.isNotEmpty(scheduleResults)) {
            Optional<GdyyScheduleResult> max = scheduleResults.stream().max(Comparator.comparing(GdyyScheduleResult::getCreateTime));
            if (max.isPresent()) {
                GdyyScheduleResult scheduleResult = max.get();
                log.setBeforeMachineId(scheduleResult.getMachineCode());
                log.setBeforeMidPlan(scheduleResult.getClass1Plan());
                log.setBeforeNightPlan(scheduleResult.getClass2Plan());
                log.setBeforeDayPlan(scheduleResult.getClass3Plan());
            }
        }
        //操作后的信息赋值
        log.setAfterMachineId(newSchedule.getMachineCode());
        log.setAfterMidPlan(newSchedule.getClass1Plan());
        log.setAfterNightPlan(newSchedule.getClass2Plan());
        log.setAfterDayPlan(newSchedule.getClass3Plan());
        // 调用插入日志方法
        gdyyDispatcherLogService.insertGdyyDispatcherLog(log);
    }

    /**
     * 根据排程日期和代码查询排程结果
     *
     * @param scheduleResult 排程日期、代码
     * @return 查询到的数据
     */
    @Override
    public List<GdyyScheduleResult> selectByScheduleDateAndCode(GdyyScheduleResult scheduleResult) {
        return gdyyScheduleResultMapper.selectByScheduleDateAndCode(scheduleResult);
    }

    /**
     * 给mes发送排程下发通知
     *
     * @param scheduleDate 排产日
     * @param dataVersion  数据版本
     * @param rowCount     同步记录数据
     */
    @Override
    public void publishNoticeMes(Date scheduleDate, String dataVersion, int rowCount) {
    	// 调整为itf接口
//        // 厂别、分公司编号
//        String factoryCode = factoryService.getFactoryCode();
//        String companyCode = factoryService.getCompanyCode();
//        //数据同步到中间库后，往mq中发送消息通知MES去取数据
//        SyncParamsVO syncParamsVO = new SyncParamsVO();
//        syncParamsVO.setSyncKey(ApsConstant.GDYY_DEPLOY_SYNC_KEY);
//        syncParamsVO.setDataVersion(dataVersion);
//        // 请求参数
//        JSONObject params = new JSONObject();
//        params.put("scheduleDate", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, scheduleDate));
//        params.put("rowCount", rowCount);
//        syncParamsVO.setParams(params);
//        syncParamsVO.setFactoryCode(factoryCode);
//        syncParamsVO.setCompanyCode(companyCode);
//        gdyySyncDataHandle.syncNotice(syncParamsVO);  //往消息队列发送消息
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
        gdyyScheduleResultMapper.batchUpdate(Arrays.stream(ids)
                .boxed().collect(Collectors.toList()), status);
        gdyyScheduleResultMapper.updatePublishRecordVersion(dataVersion, status);
    }

    /**
     * 批量删除钢带压延排程结果信息维护
     *
     * @param ids 需要删除的钢带压延排程结果信息维护ID
     */
    @Override
    public void deleteScheduleResultByIds(long[] ids) {
        gdyyScheduleResultMapper.deleteByIds(Arrays.stream(ids)
                .boxed()
                .collect(Collectors.toList()));
    }

    /**
     * 导出excel表格
     *
     * @param list 要导出的数据集合
     * @return 字节数组
     */
    @Override
    public byte[] export(List<GdyyScheduleResultDto> list) {
        GdyyScheduleResultDto summarySchedule = this.summaryExport(list);  //给导出的数据增加汇总行
        // 按用户语言读取模板
        Locale lang = ServletUtils.getUserLang();
        InputStream inputStream = null;
        if (Locale.SIMPLIFIED_CHINESE.equals(lang) || lang == null) {
            // 中文
            inputStream = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "gdyyScheduleResult.xlsx");
        } else if (Locale.US.equals(lang)) {
            // 英文
            inputStream = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "gdyyScheduleResult_en.xlsx");
        }
        Workbook webBook = ExcelUtils.readExcel(inputStream);
        CellStyle cellStyle = ExcelUtils.createCellStyle(webBook);
        DataFormat format = webBook.createDataFormat();
        cellStyle.setDataFormat(format.getFormat("[=0]\"\""));  //导出的单元格如果值为0，则显示空白

        //填充数据
        if (CollectionUtils.isNotEmpty(list)) {
            int month = DateUtil.getMonth(list.get(0).getScheduleDate());
            int day = DateUtil.getDay(list.get(0).getScheduleDate());
            Sheet sheet = webBook.getSheetAt(0);
            Row row1 = sheet.getRow(0);
            // TODO 国际化
            row1.getCell(0).setCellValue(month +"月" + day + "日钢带压延生产排程计划");

            BigDecimal midPlan = new BigDecimal(summarySchedule.getClass1Plan());
            BigDecimal nightPlan = new BigDecimal(summarySchedule.getClass2Plan());
            BigDecimal dayPlan = new BigDecimal(summarySchedule.getClass3Plan());
            for (int i = 0; i < list.size(); i++) {
                int cellNum = 0;
                GdyyScheduleResultDto scheduleResult = list.get(i);
                Row row = sheet.createRow(i + 2);
//                row.createCell(cellNum++).setCellValue(DateFormatUtils.format(scheduleResult.getScheduleDate(), "yyyy-MM-dd"));
                row.createCell(cellNum++).setCellValue(scheduleResult.getBigRollCode());
                row.createCell(cellNum++).setCellValue(scheduleResult.getMachineName());
                row.createCell(cellNum++).setCellValue(scheduleResult.getDayUsed() == null ? 0 : scheduleResult.getDayUsed());
//                row.createCell(cellNum++).setCellValue(scheduleResult.getMonthPlan() == null ? "" : scheduleResult.getMonthPlan());
                row.createCell(cellNum++).setCellValue(scheduleResult.getMonthPlanOs() == null ? 0 : Double.parseDouble(scheduleResult.getMonthPlanOs()));
                row.createCell(cellNum++).setCellValue(scheduleResult.getStockQty() == null ? 0 : scheduleResult.getStockQty());
                row.createCell(cellNum++).setCellValue(scheduleResult.getNotes() == null ? "" : scheduleResult.getNotes());
                row.createCell(cellNum++).setCellValue(scheduleResult.getDailyTotalQty() == null ? 0 : scheduleResult.getDailyTotalQty());
                row.createCell(cellNum++).setCellValue(scheduleResult.getDailyTotalQtyNum() == null ? 0 : scheduleResult.getDailyTotalQtyNum());
                row.createCell(cellNum++).setCellValue(scheduleResult.getClass1Plan() == null ? 0 : scheduleResult.getClass1Plan());
                row.createCell(cellNum++).setCellValue(scheduleResult.getClass1PlanNum() == null ? 0 : scheduleResult.getClass1PlanNum());
                row.createCell(cellNum++).setCellValue(scheduleResult.getClass1PlanNoStock() == null ? 0 : scheduleResult.getClass1PlanNoStock());
                row.createCell(cellNum++).setCellValue(scheduleResult.getClass1PlanNoStockNum() == null ? 0 : scheduleResult.getClass1PlanNoStockNum());
                row.createCell(cellNum++).setCellValue(scheduleResult.getClass1Finish() == null ? 0 : scheduleResult.getClass1Finish());
                row.createCell(cellNum++).setCellValue(scheduleResult.getClass1Remark() == null ? "" : scheduleResult.getClass1Remark());
                row.createCell(cellNum++).setCellValue(scheduleResult.getClass2Plan() == null ? 0 : scheduleResult.getClass2Plan());
                row.createCell(cellNum++).setCellValue(scheduleResult.getClass2PlanNum() == null ? 0 : scheduleResult.getClass2PlanNum());
                row.createCell(cellNum++).setCellValue(scheduleResult.getClass2PlanNoStock() == null ? 0 : scheduleResult.getClass2PlanNoStock());
                row.createCell(cellNum++).setCellValue(scheduleResult.getClass2PlanNoStockNum() == null ? 0 : scheduleResult.getClass2PlanNoStockNum());
                row.createCell(cellNum++).setCellValue(scheduleResult.getClass2Finish() == null ? 0 : scheduleResult.getClass2Finish());
                row.createCell(cellNum++).setCellValue(scheduleResult.getClass2Remark() == null ? "" : scheduleResult.getClass2Remark());
                /*row.createCell(cellNum++).setCellValue(scheduleResult.getClass3Plan() == null ? 0 : scheduleResult.getClass3Plan());
                row.createCell(cellNum++).setCellValue(scheduleResult.getClass3PlanNum() == null ? 0 : scheduleResult.getClass3PlanNum());
                row.createCell(cellNum++).setCellValue(scheduleResult.getClass3PlanNoStock() == null ? 0 : scheduleResult.getClass3PlanNoStock());
                row.createCell(cellNum++).setCellValue(scheduleResult.getClass3PlanNoStockNum() == null ? 0 : scheduleResult.getClass3PlanNoStockNum());
                row.createCell(cellNum++).setCellValue(scheduleResult.getClass3Finish() == null ? 0 : scheduleResult.getClass3Finish());
                row.createCell(cellNum++).setCellValue(scheduleResult.getClass3Remark() == null ? "" : scheduleResult.getClass3Remark());*/
                row.createCell(cellNum).setCellValue(scheduleResult.getRemark() == null ? "" : scheduleResult.getRemark());
                setCellStyle(row, row.getPhysicalNumberOfCells(), cellStyle);
            }

            //重置表头基本信息
            String dateStr="";
            if("zh_CN".equals(lang.toString())){
                dateStr= DateUtils.parseDateToStr("MM月dd日",list.get(0).getScheduleDate());
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
            String baseInfo=I18nUtil.getMessage("ui.data.column.scheduleResult.gdyy.baseInfo");
            String class1Plan=I18nUtil.getMessage("ui.data.column.scheduleResult.heji.zhongban");
            String class2Plan=I18nUtil.getMessage("ui.data.column.scheduleResult.heji.yeban");
//            String class3Plan=I18nUtil.getMessage("ui.data.column.scheduleResult.heji.baiban");
            String totalQty=I18nUtil.getMessage("ui.data.column.scheduleResult.totalQty");
            String planInfo = '：'+class1Plan+'：'+midPlan.setScale(0,BigDecimal.ROUND_HALF_UP)+'，'+class2Plan+'：'+nightPlan.setScale(0,BigDecimal.ROUND_HALF_UP)+'，'
//                    +class3Plan+'：'+dayPlan.setScale(0,BigDecimal.ROUND_HALF_UP)+'，'
                    +totalQty+'：'+(midPlan.add(nightPlan).add(dayPlan)).setScale(0,BigDecimal.ROUND_HALF_UP);
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
    private GdyyScheduleResultDto summaryExport(List<GdyyScheduleResultDto> list) {
        if(list == null || list.isEmpty()) {
            return null;
        }
        GdyyScheduleResultDto summary = new  GdyyScheduleResultDto();
        summary.setBigRollCode(I18nUtil.getMessage("ui.data.column.scheduleResult.totalQty"));
        summary.setClass1Plan(list.stream().mapToDouble(r->getDoubleOrDefault(r.getClass1Plan())).sum());
        summary.setClass1PlanNum(list.stream().mapToDouble(r->getDoubleOrDefault(r.getClass1PlanNum())).sum());
        summary.setClass1PlanNoStock(list.stream().mapToDouble(r->getDoubleOrDefault(r.getClass1PlanNoStock())).sum());
        summary.setClass1PlanNoStockNum(list.stream().mapToDouble(r->getDoubleOrDefault(r.getClass1PlanNoStockNum())).sum());
        summary.setClass1Finish(list.stream().mapToDouble(r->getDoubleOrDefault(r.getClass1Finish())).sum());

        summary.setClass2Plan(list.stream().mapToDouble(r->getDoubleOrDefault(r.getClass2Plan())).sum());
        summary.setClass2PlanNum(list.stream().mapToDouble(r->getDoubleOrDefault(r.getClass2PlanNum())).sum());
        summary.setClass2PlanNoStock(list.stream().mapToDouble(r->getDoubleOrDefault(r.getClass2PlanNoStock())).sum());
        summary.setClass2PlanNoStockNum(list.stream().mapToDouble(r->getDoubleOrDefault(r.getClass2PlanNoStockNum())).sum());
        summary.setClass2Finish(list.stream().mapToDouble(r->getDoubleOrDefault(r.getClass2Finish())).sum());

        summary.setClass3Plan(list.stream().mapToDouble(r->getDoubleOrDefault(r.getClass3Plan())).sum());
        summary.setClass3PlanNum(list.stream().mapToDouble(r->getDoubleOrDefault(r.getClass3PlanNum())).sum());
        summary.setClass3PlanNoStock(list.stream().mapToDouble(r->getDoubleOrDefault(r.getClass3PlanNoStock())).sum());
        summary.setClass3PlanNoStockNum(list.stream().mapToDouble(r->getDoubleOrDefault(r.getClass3PlanNoStockNum())).sum());
        summary.setClass3Finish(list.stream().mapToDouble(r->getDoubleOrDefault(r.getClass3Finish())).sum());

        summary.setDailyTotalQty(list.stream().mapToDouble(r->getDoubleOrDefault(r.getDailyTotalQty())).sum());
        summary.setDailyTotalQtyNum(list.stream().mapToDouble(r->getDoubleOrDefault(r.getDailyTotalQtyNum())).sum());

        list.add(summary);
        return summary;
    }

    /**
     * 发布排程结果
     *
     * @param scheduleResult 排程日期
     * @param ids            要发布的排程结果id
     */
    @Override
    public void publish(GdyyScheduleResult scheduleResult, long[] ids, String dataVersion) {
        //保存发布日志
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setBaseVale(null);
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_GDYY);
        record.setScheduleDate(scheduleResult.getScheduleDate());
        record.setPublishStatus(ApsConstant.IS_RELEASE);
        gdyyScheduleResultMapper.insertPublishRecord(record);
        this.deployGdyyScheduleToMid(scheduleResult.getScheduleDate(), ids, dataVersion);

        if (ids == null || ids.length == 0) {
            //设置更新人和更新时间
            scheduleResult.setBaseVale(0L);
            scheduleResult.setIsRelease("1");
            gdyyScheduleResultMapper.publishAll(scheduleResult);
        } else {
            // ids不为空，发布指定记录，需求暂未变更，变更后测试
            gdyyScheduleResultMapper.batchUpdate(Arrays.stream(ids)
                    .boxed().collect(Collectors.toList()), ApsConstant.IS_RELEASE);
        }
    }


    /**
     * 把排程数据发布到中间库
     *
     * @param scheduleDate 排程日期
     * @param ids          排程id
     */
    private void deployGdyyScheduleToMid(Date scheduleDate, long[] ids, String dataVersion) {
        // 厂别、分公司编号
        String factoryCode = factoryService.getFactoryCode();
        String companyCode = factoryService.getCompanyCode();
        // 把排程数据同步到接口中间库中
        gdyyScheduleResultMapper.deployGdyyScheduleToMid(dataVersion, scheduleDate, ids, factoryCode, companyCode,
                DateUtils.getNowDate());
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
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_GDYY);
        record.setScheduleDate(scheduleDate);
        return gdyyScheduleResultMapper.isPublish(record) > 0;
    }

    /**
     * 根据排程日期、物料编号、机台id校验唯一性
     *
     * @param scheduleResult 要校验记录
     * @return 查询到的记录数
     */
    @Override
    public Boolean checkUnique(GdyyScheduleResult scheduleResult) {
        return gdyyScheduleResultMapper.checkUnique(scheduleResult) == 0;
    }

    /**
     * 导入数据，并保存记录
     *
     * @param list         要导入数据
     * @param importLogId  导入日志id
     * @param scheduleDate 排程日期
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importData(List<GdyyScheduleResultDto> list, Long importLogId, Date scheduleDate) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<GdyyScheduleResultDto> importList = new ArrayList<>();

        try {
            //将机台名称转为机台code
            GdyyMachineInfo gdyyMachineInfo = new GdyyMachineInfo();
            gdyyMachineInfo.setStatus("0");
            List<GdyyMachineInfo> machineInfoList = gdyyMachineInfoMapper.selectMachineInfoList(gdyyMachineInfo);
            if (CollectionUtils.isEmpty(machineInfoList)) {
                // 未查询到机台信息
                String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
                addImportErrorLog(importLogId, null, message, importErrorLogs);
                return AjaxResult.error(message, importErrorLogs);
            }
            //根据机台名称去重
            TreeSet<GdyyMachineInfo> treeSet = new TreeSet<>(Comparator.comparing(GdyyMachineInfo::getMachineName));
            treeSet.addAll(machineInfoList);
            machineInfoList = new ArrayList<>(treeSet);

            Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(GdyyMachineInfo::getMachineName, GdyyMachineInfo::getId));

            //按业务主键分组
            Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(item -> String.join("|", item.getBigRollCode(), item.getMachineCode()), Collectors.counting()));
            for (int i = 0; i < list.size(); i++) {
                GdyyScheduleResultDto scheduleResultDto = list.get(i);
                scheduleResultDto.setDataSource("2");
                scheduleResultDto.setScheduleDate(scheduleDate);
                int errorNum = i + 3;
                List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, scheduleResultDto);

                if (groupMap.get(String.join("|", scheduleResultDto.getBigRollCode(), scheduleResultDto.getMachineCode())) > 1) {
                    // 代表重复的记录
                    String message = I18nUtil.getMessage("ui.data.column.scheduleResult.conflictRecord");
                    addImportErrorLog(importLogId, errorNum, message, validated);
                }

                // 机台code 转为机台id
                if (scheduleResultDto.getMachineCode() != null && scheduleResultDto.getMachineCode().indexOf(",") > 0) {
                    String message = I18nUtil.getMessage("ui.data.column.machine.produceLineValidate");
                    message = String.format(message, i + 3, I18nUtil.getMessage("ui.data.column.scheduleResult.produceLine"));
                    addImportErrorLog(importLogId, i + 3, message, validated);
                }
                if (machineCodeMap.get(scheduleResultDto.getMachineCode()) == null) {
                    addImportErrorLog(importLogId, i + 3, I18nUtil.getMessage("ui.error.message.column.produceLineNotExist"), validated);
                }

                if (CollectionUtils.isNotEmpty(validated)) {
                    failureNum++;
                    importErrorLogs.addAll(validated);
                } else {
                    successNum++;
                    scheduleResultDto.setMachineCode(machineCodeMap.get(scheduleResultDto.getMachineCode()) + "");
                    scheduleResultDto.setBaseVale(null);
                    scheduleResultDto.setDataSource(EngineConstants.SCHEDULE_DATA_SOURCE_IMPORT);
                    importList.add(scheduleResultDto);
                }

                System.out.println("--------------------------");
                System.out.println(scheduleResultDto);
                System.out.println("--------------------------");
            }

			// 调用引擎导入,传入 importList
			if (!importList.isEmpty()) {
				// 如果引擎导入失败，会将失败日志返回
				List<ImportErrorLog> engineImportErrorLogs = gdyyEngineService.batchSaveGdyySchedule(scheduleDate,
						importList);
				// 如果有记录导入失败，则需要合并失败日志
				if (!engineImportErrorLogs.isEmpty()) {
					engineImportErrorLogs.stream().forEach(v -> v.setImportLogId(importLogId));
					importErrorLogs.addAll(engineImportErrorLogs);
					successNum -= engineImportErrorLogs.size();
					failureNum += engineImportErrorLogs.size();
				}
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
     * 比较两字符串是否相同
     *
     * @param str1 字符串1
     * @param str2 字符串2
     * @return 是否相同
     */
    private boolean compare(String str1, String str2) {
        return (StringUtils.isEmpty(str1) ? StringUtils.isEmpty(str2) : str1.equals(str2));
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
     * 比较两对象属性值是否相同（除排程日期，钢带大卷编号）
     *
     * @param scheduleResult 前端传入对象
     * @param resultDto      查询结果对象
     * @return 是否相同
     */
    private boolean compareFields(GdyyScheduleResult scheduleResult, GdyyScheduleResultDto resultDto) {
        boolean result = compare(resultDto.getClass1Plan(),scheduleResult.getClass1Plan());
        result = result && compare(resultDto.getClass2Plan(),scheduleResult.getClass2Plan());
        result = result && compare(resultDto.getClass3Plan(),scheduleResult.getClass3Plan());
        if (!result) {
            return false;
        }
        result = scheduleResult.getDayUsed().equals(resultDto.getDayUsed());
        result = result && compare(resultDto.getStockQty(), scheduleResult.getStockQty());
        result = result && compare(resultDto.getClass1Finish(), scheduleResult.getClass1Finish());
        result = result && compare(resultDto.getClass2Finish(), scheduleResult.getClass2Finish());
        result = result && compare(resultDto.getClass3Finish(), scheduleResult.getClass3Finish());
        result = result && compare(resultDto.getClass1Remark(), scheduleResult.getClass1Remark());
        result = result && compare(resultDto.getClass2Remark(), scheduleResult.getClass2Remark());
        result = result && compare(resultDto.getClass3Remark(), scheduleResult.getClass3Remark());
        result = result && compare(resultDto.getRemark(), scheduleResult.getRemark());
        return result;
    }


    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @Override
    public int isReleasingOrTimeoutByDate(Date scheduleDate) {
        return gdyyScheduleResultMapper.isReleasingOrTimeoutByDate(scheduleDate);
    }

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param ids id
     * @return 查询到的记录数
     */
    @Override
    public int isReleasingOrTimeoutByIds(long[] ids) {
        return gdyyScheduleResultMapper.isReleasingOrTimeoutByIds(ids);
    }

    /**
     * 更改发布状态
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @Override
    public int changeReleaseStatus(GdyyScheduleResult entity) {
        return gdyyScheduleResultMapper.changeReleaseStatus(entity);
    }

    @Override
    public int checkGdyyCodeExist(GdyyScheduleResult scheduleResult) {
        return gdyyScheduleResultMapper.checkGdyyCodeExist(scheduleResult);
    }

    @Override
    public int isPublishByIds(long[] ids) {
        return gdyyScheduleResultMapper.isPublishByIds(ids);
    }

    public boolean compare(Double d1, Double d2) {
        d1 = ObjectUtils.isEmpty(d1) ? 0D : d1;
        d2 = ObjectUtils.isEmpty(d2) ? 0D : d2;
        return d1.equals(d2);
    }

    /**
     * 导入数据，并保存记录
     *
     * @param list        要导入数据
     * @param importLogId 导入日志id
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importFinishQty(List<GdyyDayFinishQty> list, Long importLogId) {
        return baseFinishQtyImportService.importFinishQty(list, importLogId, HalfComponentFinishTableEnum.GDYY);
    }

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    @Override
    public AjaxResult getSummaryVo(GdyyScheduleResultDto scheduleResult) {
        ScheduleSummaryVo summaryVo = gdyyScheduleResultMapper.getSummaryVo(scheduleResult);
        if (summaryVo == null) {
            summaryVo = new ScheduleSummaryVo();
            summaryVo.setScheduleDate(scheduleResult.getScheduleDate());
        }
        ScheduleSummaryVo lastDayPlanQtySummaryVo = gdyyScheduleResultMapper.getLastDayPlanQty(scheduleResult);
        Double lastDayPlanQty = null;
        if (lastDayPlanQtySummaryVo != null) {
            lastDayPlanQty = lastDayPlanQtySummaryVo.getNightPlanQty();
            summaryVo.setLastDayPlanQty(lastDayPlanQty);
        }
        ScheduleSummaryVo cxConsumeSummaryVo = null;
        Double cxConsumeQty = null;
        if (StringUtils.isBlank(scheduleResult.getIsRelease()) && StringUtils.isBlank(scheduleResult.getMachineCode())) {
            cxConsumeSummaryVo = gdyyScheduleResultMapper.getCxConsume(scheduleResult);
        }
        if (cxConsumeSummaryVo != null) {
            cxConsumeQty = cxConsumeSummaryVo.getCxConsumeQty();
            summaryVo.setCxConsumeQty(cxConsumeQty);
        }
        // 理论交班库存计算,理论交班库存 = 库存 + 昨日早班 + 夜班 - 成型消耗量
        Double stockQty = ObjectUtils.defaultIfNull(summaryVo.getStockQty(), 0D);
        Double nightPlanQty = ObjectUtils.defaultIfNull(summaryVo.getNightPlanQty(), 0D);
        if (lastDayPlanQty != null && cxConsumeQty != null) {
            summaryVo.setTheoreticClassStockQty(stockQty + lastDayPlanQty + nightPlanQty - cxConsumeQty);
        }
        return AjaxResult.success(summaryVo);
    }
}

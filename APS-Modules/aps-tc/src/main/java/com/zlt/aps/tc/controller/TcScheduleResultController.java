package com.zlt.aps.tc.controller;

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
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.common.engine.domain.SyncDataLogs;
import com.zlt.aps.common.engine.service.FactoryService;
import com.zlt.aps.common.engine.service.SyncDataLogsService;
import com.zlt.aps.common.engine.utils.DateUtil;
import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.common.handle.TcSyncDataHandle;
import com.zlt.aps.tc.engine.service.TcEngineService;
import com.zlt.aps.tc.service.TcMachineInfoService;
import com.zlt.aps.tc.service.TcScheduleResultService;
import com.zlt.sync.povo.SyncParamsVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ApsCommonUtil.getDoubleOrDefault;

/**
 * 胎侧排程结果Controller
 *
 * @author zlt
 * @date 2021-06-21
 */
@RestController
@RequestMapping("/tcScheduleResult")
public class TcScheduleResultController extends BaseController {

    @Value("${excelModelPath}")
    public String excelModelPath;
    @Autowired
    private TcScheduleResultService tcScheduleResultService;
    @Autowired
    private TcMachineInfoService tcMachineInfoService;
    @Resource
    private TcEngineService tcEngineService;
    @Resource
    private TcSyncDataHandle syncDataHandle;
    @Autowired
    private FactoryService factoryService;
	@Resource
	private SyncDataLogsService syncDataLogsService;

    /**
     * 查询胎侧排程结果列表
     */
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody TcScheduleResult tcScheduleResult) {
//        startPage("a.GLUE_SEQ,a.GLUE_CODE asc");
        tcScheduleResult.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<TcScheduleResult> list = tcScheduleResultService.selectTcScheduleResultList(tcScheduleResult);
        return getDataTable(list);
    }

    /**
     * 获取胎侧排程结果详细信息
     */
    @GetMapping(value = "/{id}")
    public TcScheduleResult getInfo(@PathVariable("id") Long id) {
        return tcScheduleResultService.selectTcScheduleResultById(id);
    }

    @PostMapping(value = "/getInfos")
    public List<TcScheduleResult> getInfos(@RequestBody TcScheduleResult scheduleResult) {
        return tcScheduleResultService.selectByIds(scheduleResult.getIds2());
    }

    /**
     * 新增胎侧排程结果
     */
    @Log(title = "ui.data.column.tcScheduleResult.modalName", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TcScheduleResult tcScheduleResult) {
        int exist = tcScheduleResultService.checkTcCodeExist(tcScheduleResult);
        if (exist == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.specNotExist"));
        }
        List<TcScheduleResult> scheduleResults = tcScheduleResultService.selectByScheduleDateAndCode(tcScheduleResult);
        int rows = tcScheduleResultService.insertTcScheduleResult(tcScheduleResult);
        tcScheduleResultService.insetDispatcherLogInsertOrder(ApsConstant.DISPATCHER_OPER_INSERT_ORDER, scheduleResults, tcScheduleResult);
        return toAjax(rows);
    }

    /**
     * 修改胎侧排程结果
     */
    @Log(title = "ui.data.column.tcScheduleResult.modalName", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody TcScheduleResult tcScheduleResult) {
        if (tcScheduleResult.getId() != null) {
            int releasingOrTimeoutByIds = tcScheduleResultService.isReleasingOrTimeoutByIds(new Long[]{tcScheduleResult.getId()});
            if (releasingOrTimeoutByIds > 0) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
            }
        }
        return toAjax(tcScheduleResultService.updateTcScheduleResult(tcScheduleResult));
    }

    /**
     * 调量
     */
    @Log(title = "ui.data.column.tcScheduleResult.modalName", businessType = BusinessType.CHANGE_QTY)
    @PostMapping("/changeQty")
    public AjaxResult changeQty(@RequestBody TcScheduleResult tcScheduleResult) {
        int releasingOrTimeoutByIds = tcScheduleResultService.isReleasingOrTimeoutByIds(new Long[]{tcScheduleResult.getId()});
        if (releasingOrTimeoutByIds > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        tcScheduleResult.setDayPlanQty(tcScheduleResult.getDayPlanQty() == null ? 0D : tcScheduleResult.getDayPlanQty());
        tcScheduleResult.setNightPlanQty(tcScheduleResult.getNightPlanQty() == null ? 0D : tcScheduleResult.getNightPlanQty());
        tcScheduleResultService.insetDispatcherLog(ApsConstant.DISPATCHER_OPER_PLAN, tcScheduleResult);  //如果是调度员操作，则需要增加操作日志
        return toAjax(tcScheduleResultService.updateTcScheduleResult(tcScheduleResult));
    }

    /**
     * 转机台
     */
    @Log(title = "ui.data.column.tcScheduleResult.modalName", businessType = BusinessType.CHANGE_MACHINE)
    @PostMapping("/changeMachine")
    public AjaxResult changeMachine(@RequestBody TcScheduleResult tcScheduleResult) {
        int releasingOrTimeoutByIds = tcScheduleResultService.isReleasingOrTimeoutByIds(new Long[]{tcScheduleResult.getId()});
        if (releasingOrTimeoutByIds > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        tcScheduleResult.setBaseVale(tcScheduleResult.getId());
        tcScheduleResultService.insetDispatcherLog(ApsConstant.DISPATCHER_OPER_MACHINE, tcScheduleResult);  //如果是调度员操作，则需要增加操作日志
        return toAjax(tcScheduleResultService.updateTcScheduleResult(tcScheduleResult));
    }

    /**
     * 删除胎侧排程结果
     */
    @Log(title = "ui.data.column.tcScheduleResult.modalName", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
//        int releasingOrTimeoutByIds = tcScheduleResultService.isReleasingOrTimeoutByIds(ids);
//        if (releasingOrTimeoutByIds > 0) {
//            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
//        }
        if (tcScheduleResultService.isPublishByIds(ids) != ids.length) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isPublishById"));
        }
        return toAjax(tcScheduleResultService.deleteTcScheduleResultByIds(ids));
    }

    /**
     * 查询胎侧排程结果列表
     */
    @PostMapping("/getList")
    public List<TcScheduleResult> getList(@RequestBody TcScheduleResult tcScheduleResult) {
//        startPage("a.GLUE_SEQ,a.GLUE_CODE asc");
        List<TcScheduleResult> list = tcScheduleResultService.selectTcScheduleResultList(tcScheduleResult);
        return list;
    }

    /**
     * 导出列表
     */
    @Log(title = "ui.data.column.tcScheduleResult.modalName", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public byte[] export(@RequestBody TcScheduleResult tcScheduleResult) throws Exception {

        //查询数据
//        startPage("a.GLUE_SEQ,a.GLUE_CODE asc");
        tcScheduleResult.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<TcScheduleResult> list = tcScheduleResultService.selectTcScheduleResultList(tcScheduleResult);
        TcScheduleResult summarySchedule = this.summaryExport(list);  //给导出的数据增加汇总行

        //按用户语言读取模板
        Locale lang = ServletUtils.getUserLang();
        InputStream in = null;
        if (Locale.SIMPLIFIED_CHINESE.equals(lang) || lang == null) {
            // 中文
            in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "tcScheduleResult.xlsx");
        } else if (Locale.US.equals(lang)) {
            // 英文
            in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "tcScheduleResult_en.xlsx");
        }
        Workbook webBook = ExcelUtils.readExcel(in);

        //填充数据
        if (CollectionUtils.isNotEmpty(list)) {
            List<TcMachineInfo> tcMachineInfoList = tcMachineInfoService.selectMachineInfoList(new TcMachineInfo());
            Map<String, String> map = null;
            if (CollectionUtils.isNotEmpty(tcMachineInfoList)) {
                map = tcMachineInfoList.stream().collect(Collectors.toMap(item -> item.getId() + "", item -> item.getMachineName()));
            }
            DecimalFormat df = new DecimalFormat("0.00%");
            Sheet sheet = webBook.getSheetAt(0);
            CellStyle cellStyle = ExcelUtils.createCellStyle(webBook);
            DataFormat format = webBook.createDataFormat();
            cellStyle.setDataFormat(format.getFormat("[=0]\"\""));  //导出的单元格如果值为0，则显示空白
            int month = DateUtil.getMonth(list.get(0).getScheduleDate());
            int day = DateUtil.getDay(list.get(0).getScheduleDate());
            Row row1 = sheet.getRow(0);
            BigDecimal midPlan = new BigDecimal(summarySchedule.getDayPlanQty());
            BigDecimal nightPlan = new BigDecimal(summarySchedule.getNightPlanQty());
            for (int i = 0; i < list.size(); i++) {
                int n = 0;
                TcScheduleResult scheduleResult = list.get(i);
                Row row = sheet.createRow(i + 2);
//                row.createCell(n++).setCellValue(DateUtils.parseDateToStr("yyyy-MM-dd",scheduleResult.getScheduleDate()));
                row.createCell(n++).setCellValue(scheduleResult.getSidewallCode() == null ? "" : scheduleResult.getSidewallCode());
                row.createCell(n++).setCellValue(scheduleResult.getWholeGlueCode() == null ? "" : scheduleResult.getWholeGlueCode());
                row.createCell(n++).setCellValue(scheduleResult.getGlueSeq() == null ? 0 : Double.parseDouble(scheduleResult.getGlueSeq()));
                row.createCell(n++).setCellValue(scheduleResult.getMouthPlateCode() == null ? "" : scheduleResult.getMouthPlateCode());
//                row.createCell(n++).setCellValue(scheduleResult.getUnitConsume() == null ? 0 : scheduleResult.getUnitConsume());


                String produceLine = "";
                if (StringUtils.isNotEmpty(scheduleResult.getMachineId()) && map != null) {
                    String[] aa = scheduleResult.getMachineId().split(",");
                    for (String a : aa) {
                        if(StringUtils.isNotBlank(map.get(a))){
                            produceLine = produceLine + map.get(a) + ",";
                        }
                    }
                }
                if (StringUtils.isNotEmpty(produceLine)) {
                    produceLine = produceLine.substring(0, produceLine.length() - 1);
                }
                row.createCell(n++).setCellValue(produceLine);
                row.createCell(n++).setCellValue(scheduleResult.getMonthPlanOs() == null ? 0 : scheduleResult.getMonthPlanOs());
                row.createCell(n++).setCellValue(scheduleResult.getStockQty() == null ? 0 : scheduleResult.getStockQty());
                row.createCell(n++).setCellValue(scheduleResult.getSupplyTime() == null ? 0 : scheduleResult.getSupplyTime());
                row.createCell(n++).setCellValue(scheduleResult.getDailyTotalQty() == null ? 0 : scheduleResult.getDailyTotalQty());
                row.createCell(n++).setCellValue(scheduleResult.getDayPlanQty() == null ? 0 : scheduleResult.getDayPlanQty());
                row.createCell(n++).setCellValue(scheduleResult.getDayProduceOrder() == null ? 0 : scheduleResult.getDayProduceOrder());
                row.createCell(n++).setCellValue(scheduleResult.getDayFinishQty() == null ? 0 : scheduleResult.getDayFinishQty());
                row.createCell(n++).setCellValue(scheduleResult.getDayFinishRate() == null ? "" : df.format(scheduleResult.getDayFinishRate()));

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
                row.createCell(n++).setCellValue(anly);
                row.createCell(n++).setCellValue(scheduleResult.getNightPlanQty() == null ? 0 : scheduleResult.getNightPlanQty());
                row.createCell(n++).setCellValue(scheduleResult.getNightProduceOrder() == null ? 0 : scheduleResult.getNightProduceOrder());
                row.createCell(n++).setCellValue(scheduleResult.getNightFinishQty() == null ? 0 : scheduleResult.getNightFinishQty());
                row.createCell(n++).setCellValue(scheduleResult.getNightFinishRate() == null ? "" : df.format(scheduleResult.getNightFinishRate()));


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
                row.createCell(n++).setCellValue(nightAnly);
                row.createCell(n++).setCellValue(scheduleResult.getPrePlanQty() == null ? 0 : scheduleResult.getPrePlanQty());
                row.createCell(n++).setCellValue(scheduleResult.getCxClass1Plan() == null ? 0 : scheduleResult.getCxClass1Plan());
                row.createCell(n++).setCellValue(scheduleResult.getCxClass2Plan() == null ? 0 : scheduleResult.getCxClass2Plan());
                row.createCell(n++).setCellValue(scheduleResult.getCxClass3Plan() == null ? 0 : scheduleResult.getCxClass3Plan());
                row.createCell(n++).setCellValue(scheduleResult.getCxClass4Plan() == null ? 0 : scheduleResult.getCxClass4Plan());
                row.createCell(n++).setCellValue(scheduleResult.getCxClass5Plan() == null ? 0 : scheduleResult.getCxClass5Plan());
                row.createCell(n).setCellValue(scheduleResult.getRemark() == null ? "" : scheduleResult.getRemark());
                int a = row.getPhysicalNumberOfCells();
                for (int j = 0; j < a; j++) {
                    row.getCell(j).setCellStyle(cellStyle);
                }
            }
            //重置表头基本信息
            String dateStr="";
            if("zh_CN".equals(lang.toString())){
                dateStr=DateUtils.parseDateToStr("MM月dd日",tcScheduleResult.getScheduleDate());
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
            String baseInfo=I18nUtil.getMessage("ui.data.column.scheduleResult.tc.baseInfo");
            String class1Plan=I18nUtil.getMessage("ui.data.column.scheduleResult.heji.zhongban");
            String class2Plan=I18nUtil.getMessage("ui.data.column.scheduleResult.heji.yeban");
            String totalQty=I18nUtil.getMessage("ui.data.column.scheduleResult.totalQty");
            String planInfo = '：'+class1Plan+'：'+midPlan.setScale(0,BigDecimal.ROUND_HALF_UP)+'，'+class2Plan+'：'+nightPlan.setScale(0,BigDecimal.ROUND_HALF_UP)+'，'+totalQty+'：'+(midPlan.add(nightPlan)).setScale(0,BigDecimal.ROUND_HALF_UP);
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
    private TcScheduleResult summaryExport(List<TcScheduleResult> list) {
        if(list == null || list.isEmpty()) {
            return null;
        }
        TcScheduleResult summary = new  TcScheduleResult();
        summary.setSidewallCode(I18nUtil.getMessage("ui.data.column.scheduleResult.totalQty"));
        summary.setDayPlanQty(list.stream().mapToDouble(r->getDoubleOrDefault(r.getDayPlanQty())).sum());
        summary.setDayFinishQty(list.stream().mapToDouble(r->getDoubleOrDefault(r.getDayFinishQty())).sum());
        summary.setNightPlanQty(list.stream().mapToDouble(r->getDoubleOrDefault(r.getNightPlanQty())).sum());
        summary.setNightFinishQty(list.stream().mapToDouble(r->getDoubleOrDefault(r.getNightFinishQty())).sum());
        summary.setDailyTotalQty(BigDecimalUtil.add(summary.getDayPlanQty(), summary.getNightPlanQty()));

        summary.setPrePlanQty(list.stream().mapToDouble(r->getDoubleOrDefault(r.getPrePlanQty())).sum());
        summary.setCxClass1Plan(list.stream().mapToDouble(r->getDoubleOrDefault(r.getCxClass1Plan())).sum());
        summary.setCxClass2Plan(list.stream().mapToDouble(r->getDoubleOrDefault(r.getCxClass2Plan())).sum());
        summary.setCxClass3Plan(list.stream().mapToDouble(r->getDoubleOrDefault(r.getCxClass3Plan())).sum());
        summary.setCxClass4Plan(list.stream().mapToDouble(r->getDoubleOrDefault(r.getCxClass4Plan())).sum());
        summary.setCxClass5Plan(list.stream().mapToDouble(r->getDoubleOrDefault(r.getCxClass5Plan())).sum());
        list.add(summary);
        return summary;
    }

    /**
     * 自动排程
     */
    @Log(title = "ui.data.column.tcScheduleResult.modalName", businessType = BusinessType.AUTOPLAN)
    @PostMapping("/autoPlan")
    public AjaxResult autoPlan(@RequestBody TcScheduleResult tcScheduleResult) {
        //执行自动排程算法
        Date scheduleDate = tcScheduleResult.getScheduleDate();
        tcEngineService.autoTcSchedule(DateUtils.parseDateToStr("yyyy-MM-dd", scheduleDate));
        return AjaxResult.success();
    }

    /**
     * 排程发布
     */
    @Log(title = "ui.data.column.tcScheduleResult.modalName", businessType = BusinessType.PUBLISH)
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody TcScheduleResult tcScheduleResult) {
    	// 发布前需要先获得同步锁，防止在集群环境下出现一个前端命令发送两次mes请求，modify by hak 20220708
    	if (syncDataLogsService.checkPublishLocking("tc:publish:lock", tcScheduleResult.getIds())) {
    		return AjaxResult.success(); // 如果已经被锁定了，则直接返回
    	}
        int releasingOrTimeoutByIds = tcScheduleResultService.isReleasingOrTimeoutByIds(tcScheduleResult.getIds());
        if (releasingOrTimeoutByIds > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        //查询排程发布list
        tcScheduleResult.setYear(DateFormatUtils.format(tcScheduleResult.getScheduleDate(), "yyyy"));
        tcScheduleResult.setMonth(DateFormatUtils.format(tcScheduleResult.getScheduleDate(), "MM"));
        // 过滤未发布及发布失败的数据
        List<TcScheduleResult> list = tcScheduleResultService.selectTcScheduleResultList(tcScheduleResult).stream()
                .filter(item -> ApsConstant.NO_RELEASE.equals(item.getIsRelease()) || ApsConstant.FAILURE_RELEASE.equals(item.getIsRelease()) || ApsConstant.WAIT_RELEASING.equals(item.getIsRelease())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.errorPublish"));
        }
        //校验是否单机台
        List<TcScheduleResult> collect = list.stream().filter(item -> StringUtil.isEmpty(item.getMachineId()) || item.getMachineId().contains(",")).collect(Collectors.toList());
        if (collect.size() > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.hasMultipleIds"));
        }

        //排程发布
        long[] arr = list.stream().mapToLong(TcScheduleResult::getId).toArray();
        //获取数据版本号
        String dataVersion = syncDataHandle.getDataVersion(ApsConstant.TC_DEPLOY_SYNC_KEY);
        // 厂别、分公司编号
        String factoryCode = factoryService.getFactoryCode();
        String companyCode = factoryService.getCompanyCode();
        AjaxResult ajaxResult = null;
        try {
            ajaxResult = tcScheduleResultService.publish(arr, tcScheduleResult.getScheduleDate(), dataVersion, factoryCode, companyCode);
            SyncParamsVO syncParamsVO = new SyncParamsVO();
            syncParamsVO.setSyncKey(ApsConstant.TC_DEPLOY_SYNC_KEY);
            syncParamsVO.setDataVersion(dataVersion);
            // 请求参数
            JSONObject params = new JSONObject();
            params.put("scheduleDate", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, tcScheduleResult.getScheduleDate()));
			params.put("rowCount", arr.length);
            syncParamsVO.setParams(params);
            syncParamsVO.setFactoryCode(factoryCode);
            syncParamsVO.setCompanyCode(companyCode);
            syncDataHandle.syncNotice(syncParamsVO);

			// 取回mes的反馈结果
			SyncDataLogs logs = syncDataLogsService.getSyncDataResult(dataVersion);
			String status = logs.getStatus();
			// 更新状态
			tcScheduleResultService.updateRelaseStatus(dataVersion, arr, status);
			if (ApsConstant.IS_RELEASE.equals(status)) {
				// 成功
				ajaxResult = AjaxResult.success();
			} else {
				// 失败，需要返回异常信息
				ajaxResult = AjaxResult.error(logs.getMsg());
			}
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.failedPublish"));
        }
        return ajaxResult;
    }

    /**
     * 查询排程日期是否已发布
     *
     * @return 是否已经发布
     */
    @PostMapping("/isPublish")
    public Boolean isPublish(@RequestBody TcScheduleResult entity) {
        return tcScheduleResultService.isPublish(entity.getScheduleDate());
    }

    /**
     * 唯一性校验
     */
    @PostMapping("/checkUnique")
    public List<TcScheduleResult> checkUnique(@RequestBody TcScheduleResult entity) {
        List<TcScheduleResult> list = tcScheduleResultService.checkUnique(entity);
        return list;
    }

    @Log(title = "ui.data.column.tcScheduleResult.modalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<TcScheduleResult> list, @RequestParam("importLogId") Long importLogId, @RequestParam("scheduleDate") String scheduleDate) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return tcScheduleResultService.importData(list, importLogId, scheduleDate);
    }

    /**
     * 选机台
     */
    @Log(title = "ui.data.column.tcScheduleResult.modalName", businessType = BusinessType.CHOOSE_MACHINE)
    @PostMapping("/chooseMachine")
    public AjaxResult chooseMachine(@RequestBody TcScheduleResult schesduleResult) {
        TcScheduleResult scheduleResult0 = tcScheduleResultService.selectTcScheduleResultById(schesduleResult.getId());
        if (compare(schesduleResult.getMachineId(), scheduleResult0.getMachineId())) {
            return AjaxResult.success();
        }
        scheduleResult0.setMachineId(schesduleResult.getMachineId());
        return tcScheduleResultService.chooseMachine(scheduleResult0);
    }

    /**
     * 对比
     */
    public boolean compare(String str1, String str2) {
        return (StringUtils.isEmpty(str1) ? StringUtils.isEmpty(str2) : str1.equals(str2));
    }

    /**
     * 均衡
     * @param entity
     * @return
     */
    @Log(title = "ui.data.column.tcScheduleResult.modalName", businessType = BusinessType.BALANCE)
    @PostMapping("/balance")
    public AjaxResult balance(@RequestBody TcScheduleResult entity){
        Date scheduleDate = entity.getScheduleDate();
        if (scheduleDate == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        int releasingOrTimeoutByDate = tcScheduleResultService.isReleasingOrTimeoutByDate(scheduleDate);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        this.tcEngineService.handEquilibriumAndProduceOrder(DateUtils.dateTime(scheduleDate));
        return AjaxResult.success(scheduleDate);
    }

    /**
     * 同胶料归并生产
     * @param tcScheduleResult
     * @return
     */
    @Log(title = "ui.data.column.tcScheduleResult.modalName", businessType = BusinessType.MERGE_PRODUCT)
    @PostMapping("/mergeProduct")
    public AjaxResult mergeProduct(@RequestBody TcScheduleResult tcScheduleResult){
        Date scheduleDate = tcScheduleResult.getScheduleDate();
        if (scheduleDate == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        int releasingOrTimeoutByDate = tcScheduleResultService.isReleasingOrTimeoutByDate(scheduleDate);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        this.tcEngineService.handGlueMerge(DateUtils.dateTime(scheduleDate));
        return AjaxResult.success(scheduleDate);
    }

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @PostMapping("/isReleasingOrTimeoutByDate")
    public int isReleasingOrTimeoutByDate(@RequestBody TcScheduleResult scheduleResult){
        return tcScheduleResultService.isReleasingOrTimeoutByDate(scheduleResult.getScheduleDate());
    }

    /**
     * 更改发布状态
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @Log(title = "ui.data.column.tcScheduleResult.modalName")
    @PostMapping("/changeReleaseStatus")
    public AjaxResult changeReleaseStatus(@RequestBody TcScheduleResult entity){
        tcScheduleResultService.changeReleaseStatus(entity);
        return AjaxResult.success();
    }

    /**
     * 归并中夜班计划量，合并到同一个班次
     *
     * @param ids             id
     * @param classifiedShift 合并班次
     */
    @Log(title = "ui.data.column.tcScheduleResult.modalName", businessType = BusinessType.CONSOLIDATION)
    @PostMapping("/combinationMiddleAndNight/{ids}")
    public AjaxResult combinationMiddleAndNight(@PathVariable("ids")Long[] ids, @RequestParam("classifiedShift") String classifiedShift) {
        int releasingOrTimeoutByDate = tcScheduleResultService.isReleasingOrTimeoutByIds(ids);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        tcScheduleResultService.combinationMiddleAndNight(ids, classifiedShift);
        return AjaxResult.success();
    }
}

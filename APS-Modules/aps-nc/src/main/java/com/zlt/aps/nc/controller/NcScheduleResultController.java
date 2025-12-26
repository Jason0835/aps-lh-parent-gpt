package com.zlt.aps.nc.controller;

import static com.zlt.aps.common.core.utils.ApsCommonUtil.getDoubleOrDefault;

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

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.enums.BillTypeCodeEnums;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.common.engine.service.FactoryService;
import com.zlt.aps.common.engine.utils.DateUtil;
import com.zlt.aps.itf.vo.SyncDataLogs;
import com.zlt.aps.nc.api.domain.entity.NcDayFinishQty;
import com.zlt.aps.nc.api.domain.entity.NcMachineInfo;
import com.zlt.aps.nc.api.domain.entity.NcScheduleResult;
import com.zlt.aps.nc.engine.service.NcEngineService;
import com.zlt.aps.nc.service.NcMachineInfoService;
import com.zlt.aps.nc.service.NcScheduleResultService;
import com.zlt.bill.common.controller.AbstractBillBizController;
import com.zlt.bill.common.service.IBillService;
import com.zlt.common.utils.StringUtil;
import com.zlt.sync.api.service.ISyncDataLogsApiService;

import io.swagger.annotations.ApiOperation;

/**
 * 内衬胶排程结果Controller
 *
 * @author zlt
 * @date 2021-06-24
 */
@RestController
@RequestMapping("/ncScheduleResult")
public class NcScheduleResultController extends AbstractBillBizController<NcScheduleResult> {

    @Value("${excelModelPath}")
    public String excelModelPath;
    @Autowired
    private NcScheduleResultService ncScheduleResultService;
    @Autowired
    private NcMachineInfoService ncMachineInfoService;
    @Resource
    private NcEngineService ncEngineService;
    @Autowired
    private FactoryService factoryService;
	@Resource
	private ISyncDataLogsApiService syncDataLogsService;

    /**
     * 查询内衬排程结果列表
     */
    @Override
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody NcScheduleResult ncScheduleResult) {
//        startPage("a.GLUE_SEQ,a.GLUE_CODE asc");
        ncScheduleResult.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<NcScheduleResult> list = ncScheduleResultService.selectNcScheduleResultList(ncScheduleResult);
        return getDataTable(list);
    }

    /**
     * 获取内衬排程结果详细信息
     */
    @Override
    @GetMapping(value = "/{id}")
    public NcScheduleResult getInfo(@PathVariable("id") Long id) {
        return ncScheduleResultService.selectNcScheduleResultById(id);
    }

    @PostMapping(value = "/getInfos")
    public List<NcScheduleResult> getInfos(@RequestBody NcScheduleResult scheduleResult) {
        return ncScheduleResultService.selectByIds(scheduleResult.getIds2());
    }

    /**
     * 新增内衬排程结果
     */
    @Log(title = "ui.data.column.ncScheduleResult.modalName", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody NcScheduleResult ncScheduleResult) {
        int exist = ncScheduleResultService.checkNcCodeExist(ncScheduleResult);
        if (exist == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.specNotExist"));
        }
        List<NcScheduleResult> scheduleResults = ncScheduleResultService.selectByScheduleDateAndCode(ncScheduleResult);
        int rows = ncScheduleResultService.insertNcScheduleResult(ncScheduleResult);
        ncScheduleResultService.insetDispatcherLogInsertOrder(ApsConstant.DISPATCHER_OPER_INSERT_ORDER, scheduleResults, ncScheduleResult);
        return toAjax(rows);
    }

    /**
     * 修改内衬排程结果
     */
    @Log(title = "ui.data.column.ncScheduleResult.modalName", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody NcScheduleResult ncScheduleResult) {
        if (ncScheduleResult.getId() != null) {
            int releasingOrTimeoutByIds = ncScheduleResultService.isReleasingOrTimeoutByIds(new Long[]{ncScheduleResult.getId()});
            if (releasingOrTimeoutByIds > 0) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
            }
        }
        return toAjax(ncScheduleResultService.updateNcScheduleResult(ncScheduleResult));
    }

    /**
     * 调量
     */
    @Log(title = "ui.data.column.ncScheduleResult.modalName", businessType = BusinessType.CHANGE_QTY)
    @PostMapping("/changeQty")
    public AjaxResult changeQty(@RequestBody NcScheduleResult scheduleResult) {
        int releasingOrTimeoutByDate = ncScheduleResultService.isReleasingOrTimeoutByIds(new Long[]{scheduleResult.getId()});
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        scheduleResult.setBaseVale(scheduleResult.getId());
        scheduleResult.setDayPlanQty(scheduleResult.getDayPlanQty() == null ? 0D : scheduleResult.getDayPlanQty());
        scheduleResult.setNightPlanQty(scheduleResult.getNightPlanQty() == null ? 0D : scheduleResult.getNightPlanQty());
        ncScheduleResultService.insetDispatcherLog(ApsConstant.DISPATCHER_OPER_PLAN, scheduleResult);  //如果是调度员操作，则需要增加操作日志
        return toAjax(ncScheduleResultService.updateNcScheduleResult(scheduleResult));
    }

    /**
     * 转机台
     */
    @Log(title = "ui.data.column.ncScheduleResult.modalName", businessType = BusinessType.CHANGE_MACHINE)
    @PostMapping("/changeMachine")
    public AjaxResult changeMachine(@RequestBody NcScheduleResult scheduleResult) {
        int releasingOrTimeoutByDate = ncScheduleResultService.isReleasingOrTimeoutByIds(new Long[]{scheduleResult.getId()});
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        scheduleResult.setBaseVale(scheduleResult.getId());
        ncScheduleResultService.insetDispatcherLog(ApsConstant.DISPATCHER_OPER_MACHINE, scheduleResult);  //如果是调度员操作，则需要增加操作日志
        return toAjax(ncScheduleResultService.updateNcScheduleResult(scheduleResult));
    }

    /**
     * 删除内衬排程结果
     */
    @Log(title = "ui.data.column.ncScheduleResult.modalName", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
//        int releasingOrTimeoutByIds = ncScheduleResultService.isReleasingOrTimeoutByIds(ids);
//        if (releasingOrTimeoutByIds > 0) {
//            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
//        }
        if (ncScheduleResultService.isPublishByIds(ids) != ids.length) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isPublishById"));
        }
        return toAjax(ncScheduleResultService.deleteNcScheduleResultByIds(ids));
    }

    /**
     * 查询内衬排程结果列表
     */
    @PostMapping("/getList")
    public List<NcScheduleResult> getList(@RequestBody NcScheduleResult ncScheduleResult) {
//        startPage("a.GLUE_SEQ,a.GLUE_CODE asc");
        List<NcScheduleResult> list = ncScheduleResultService.selectNcScheduleResultList(ncScheduleResult);
        return list;
    }

    /**
     * 导出列表
     */
    @Log(title = "ui.data.column.ncScheduleResult.modalName", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public byte[] export(@RequestBody NcScheduleResult ncScheduleResult) throws Exception {

        //查询数据
//        startPage("a.GLUE_SEQ,a.GLUE_CODE asc");
        ncScheduleResult.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<NcScheduleResult> list = ncScheduleResultService.selectNcScheduleResultList(ncScheduleResult);
        NcScheduleResult summarySchedule = this.summaryExport(list);  //给导出的数据增加汇总行

        //按用户语言读取模板
        Locale lang = ServletUtils.getUserLang();
        InputStream in = null;
        if (Locale.SIMPLIFIED_CHINESE.equals(lang) || lang == null) {
            // 中文
            in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "ncScheduleResult.xlsx");
        } else if (Locale.US.equals(lang)) {
            // 英文
            in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "ncScheduleResult_en.xlsx");
        }
        Workbook webBook = ExcelUtils.readExcel(in);

        //填充数据
        if (CollectionUtils.isNotEmpty(list)) {
            List<NcMachineInfo> tcMachineInfoList = ncMachineInfoService.selectMachineInfoList(new NcMachineInfo());
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
                NcScheduleResult scheduleResult = list.get(i);
                Row row = sheet.createRow(i + 2);
//                row.createCell(n++).setCellValue(DateUtils.parseDateToStr("yyyy-MM-dd",scheduleResult.getScheduleDate()));
                row.createCell(n++).setCellValue(scheduleResult.getLiningCode() == null ? "" : scheduleResult.getLiningCode());
                row.createCell(n++).setCellValue(scheduleResult.getWholeGlueCode() == null ? "" : scheduleResult.getWholeGlueCode());
                row.createCell(n++).setCellValue(scheduleResult.getGlueSeq() == null ? 0 : Double.parseDouble(scheduleResult.getGlueSeq()));
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
                dateStr=DateUtils.parseDateToStr("MM月dd日",ncScheduleResult.getScheduleDate());
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
            String baseInfo=I18nUtil.getMessage("ui.data.column.scheduleResult.nc.baseInfo");
            String class1Plan=I18nUtil.getMessage("ui.data.column.scheduleResult.heji.zhongban");
            String class2Plan=I18nUtil.getMessage("ui.data.column.scheduleResult.heji.yeban");
            String totalQty=I18nUtil.getMessage("ui.data.column.scheduleResult.totalQty");
            String planInfo = '：'+class1Plan+'：'+midPlan.setScale(2,BigDecimal.ROUND_HALF_UP)+'，'+class2Plan+'：'+nightPlan.setScale(2,BigDecimal.ROUND_HALF_UP)+'，'+totalQty+'：'+(midPlan.add(nightPlan)).setScale(2,BigDecimal.ROUND_HALF_UP);
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
    private NcScheduleResult summaryExport(List<NcScheduleResult> list) {
        if(list == null || list.isEmpty()) {
            return null;
        }
        NcScheduleResult summary = new  NcScheduleResult();
        summary.setLiningCode(I18nUtil.getMessage("ui.data.column.scheduleResult.totalQty"));
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
    @Log(title = "ui.data.column.ncScheduleResult.modalName", businessType = BusinessType.AUTOPLAN)
    @PostMapping("/autoPlan")
    public AjaxResult autoPlan(@RequestBody NcScheduleResult ncScheduleResult) {
        //执行自动排程算法
        Date scheduleDate = ncScheduleResult.getScheduleDate();
        ncEngineService.autoNcSchedule(DateUtils.parseDateToStr("yyyy-MM-dd", scheduleDate));
        return AjaxResult.success();
    }

    /**
     * 排程发布
     */
    @Log(title = "ui.data.column.ncScheduleResult.modalName", businessType = BusinessType.PUBLISH)
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody NcScheduleResult ncScheduleResult) {
    	// 发布前需要先获得同步锁，防止在集群环境下出现一个前端命令发送两次mes请求，modify by hak 20220708
    	if (syncDataLogsService.checkPublishLocking("nc:publish:lock", ncScheduleResult.getIds())) {
    		return AjaxResult.success(); // 如果已经被锁定了，则直接返回
    	}
        int releasingOrTimeoutByIds = ncScheduleResultService.isReleasingOrTimeoutByIds(ncScheduleResult.getIds());
        if (releasingOrTimeoutByIds > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        //查询排程发布list
        ncScheduleResult.setYear(DateFormatUtils.format(ncScheduleResult.getScheduleDate(), "yyyy"));
        ncScheduleResult.setMonth(DateFormatUtils.format(ncScheduleResult.getScheduleDate(), "MM"));
        // 过滤未发布及发布失败的数据
        List<NcScheduleResult> list = ncScheduleResultService.selectNcScheduleResultList(ncScheduleResult).stream()
                .filter(item -> ApsConstant.NO_RELEASE.equals(item.getIsRelease()) || ApsConstant.FAILURE_RELEASE.equals(item.getIsRelease()) || ApsConstant.WAIT_RELEASING.equals(item.getIsRelease())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.errorPublish"));
        }
        //校验是否单机台
        List<NcScheduleResult> collect = list.stream().filter(item -> StringUtil.isEmpty(item.getMachineId()) || item.getMachineId().contains(",")).collect(Collectors.toList());
        if (collect.size() > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.hasMultipleIds"));
        }
        //更新发布状态
        long[] arr = list.stream().mapToLong(NcScheduleResult::getId).toArray();
        Date scheduleDate = list.get(0).getScheduleDate();

        String dataVersion = syncDataLogsService.getDataVersion(ApsConstant.NC_DEPLOY_SYNC_KEY);  //下发接口版本号
        // 厂别、分公司编号
        String factoryCode = factoryService.getFactoryCode();
        String companyCode = factoryService.getCompanyCode();
        AjaxResult ajaxResult = null;
        try {
            ncScheduleResultService.batchUpdate(arr, scheduleDate, dataVersion, factoryCode, companyCode);
            // 调整为itf接口
//            //数据同步到中间库后，往mq中发送消息通知MES去取数据
//            SyncParamsVO syncParamsVO = new SyncParamsVO();
//            syncParamsVO.setSyncKey(ApsConstant.NC_DEPLOY_SYNC_KEY);
//            syncParamsVO.setDataVersion(dataVersion);
//            // 请求参数
//            JSONObject params = new JSONObject();
//            params.put("scheduleDate", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, scheduleDate));
//			params.put("rowCount", arr.length);
//            syncParamsVO.setParams(params);
//            syncParamsVO.setFactoryCode(factoryCode);
//            syncParamsVO.setCompanyCode(companyCode);
//            ncSyncDataHandle.syncNotice(syncParamsVO);  //往消息队列发送消息

			// 取回mes的反馈结果
			SyncDataLogs logs = syncDataLogsService.getSyncDataResult(dataVersion);
			String status = logs.getStatus();
			// 更新状态
			ncScheduleResultService.updateRelaseStatus(dataVersion, arr, status);
			if (ApsConstant.IS_RELEASE.equals(status)) {
				// 成功
				ajaxResult = AjaxResult.success(I18nUtil.getMessage("ui.data.column.scheduleResult.successPublish"));
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
    public Boolean isPublish(@RequestBody NcScheduleResult entity) {
        return ncScheduleResultService.isPublish(entity.getScheduleDate());
    }

    /**
     * 唯一性校验
     */
    @PostMapping("/checkUnique")
    public List<NcScheduleResult> checkUnique(@RequestBody NcScheduleResult entity) {
        List<NcScheduleResult> list = ncScheduleResultService.checkUnique(entity);
        return list;
    }

    /**
     * 导入数据
     */
    @Log(title = "ui.data.column.ncScheduleResult.modalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<NcScheduleResult> list, @RequestParam("importLogId") Long importLogId, @RequestParam("scheduleDate") String scheduleDate) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return ncScheduleResultService.importData(list, importLogId, scheduleDate);
    }

    /**
     * 选机台
     */
    @Log(title = "ui.data.column.ncScheduleResult.modalName", businessType = BusinessType.CHOOSE_MACHINE)
    @PostMapping("/chooseMachine")
    public AjaxResult chooseMachine(@RequestBody NcScheduleResult schesduleResult) {
        NcScheduleResult scheduleResult0 = ncScheduleResultService.selectNcScheduleResultById(schesduleResult.getId());
        if (compare(schesduleResult.getMachineId(), scheduleResult0.getMachineId())) {
            return AjaxResult.success();
        }
        scheduleResult0.setMachineId(schesduleResult.getMachineId());
        return ncScheduleResultService.chooseMachine(scheduleResult0);
    }

    /**
     * 对比
     */
    public boolean compare(String str1, String str2) {
        return (StringUtils.isEmpty(str1) ? StringUtils.isEmpty(str2) : str1.equals(str2));
    }

    /**
     * 均衡
     *
     * @param entity
     * @return
     */
    @Log(title = "ui.data.column.ncScheduleResult.modalName", businessType = BusinessType.BALANCE)
    @PostMapping("/balance")
    public AjaxResult balance(@RequestBody NcScheduleResult entity) {
        Date scheduleDate = entity.getScheduleDate();
        if (scheduleDate == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        int releasingOrTimeoutByDate = ncScheduleResultService.isReleasingOrTimeoutByDate(scheduleDate);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        this.ncEngineService.handEquilibriumAndProduceOrder(DateUtils.dateTime(scheduleDate));
        return AjaxResult.success(scheduleDate);
    }

    /**
     * 同胶料归并生产
     * @param ncScheduleResult
     * @return
     */
    @Log(title = "ui.data.column.ncScheduleResult.modalName", businessType = BusinessType.MERGE_PRODUCT)
    @PostMapping("/mergeProduct")
    public AjaxResult mergeProduct(@RequestBody NcScheduleResult ncScheduleResult){
        Date scheduleDate = ncScheduleResult.getScheduleDate();
        if (scheduleDate == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        int releasingOrTimeoutByDate = ncScheduleResultService.isReleasingOrTimeoutByDate(scheduleDate);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        this.ncEngineService.handGlueMerge(DateUtils.dateTime(scheduleDate));
        return AjaxResult.success(scheduleDate);
    }


    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param scheduleResult 排程日期
     * @return 查询到的记录数
     */
    @PostMapping("/isReleasingOrTimeoutByDate")
    public int isReleasingOrTimeoutByDate(@RequestBody NcScheduleResult scheduleResult) {
        return ncScheduleResultService.isReleasingOrTimeoutByDate(scheduleResult.getScheduleDate());
    }

    /**
     * 更改发布状态
     *
     * @param entity 排程日期
     * @return 结果
     */
    @Log(title = "ui.data.column.ncScheduleResult.modalName")
    @PostMapping("/changeReleaseStatus")
    public AjaxResult changeReleaseStatus(@RequestBody NcScheduleResult entity) {
        ncScheduleResultService.changeReleaseStatus(entity);
        return AjaxResult.success();
    }

    /**
     * 归并中夜班计划量，合并到同一个班次
     *
     * @param ids             id
     * @param classifiedShift 合并班次
     */
    @Log(title = "ui.data.column.ncScheduleResult.modalName", businessType = BusinessType.CONSOLIDATION)
    @PostMapping("/combinationMiddleAndNight/{ids}")
    public AjaxResult combinationMiddleAndNight(@PathVariable("ids") Long[] ids, @RequestParam("classifiedShift") String classifiedShift) {
        int releasingOrTimeoutByDate = ncScheduleResultService.isReleasingOrTimeoutByIds(ids);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        ncScheduleResultService.combinationMiddleAndNight(ids, classifiedShift);
        return AjaxResult.success();
    }

    @Override
    protected IBillService getBillService() {
        return ncScheduleResultService;
    }

    @Override
    protected String getTypeCode() {
        return BillTypeCodeEnums.NC_SCHEDULE_RESULT.getBillTypeCode();
    }

    /**
     * 导入完成量
     * @param list 完成量集合
     * @param importLogId 导入记录id
     * @return 结果
     */
    @PostMapping("/importFinishQty")
    @ApiOperation("导入完成量")
    public AjaxResult importFinishQty(@RequestBody List<NcDayFinishQty> list, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.isEmpty()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return ncScheduleResultService.importFinishQty(list, importLogId);
    }

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    @PostMapping("/getSummaryVo")
    @ApiOperation("获取排程日期的排程结果合计")
    public AjaxResult getSummaryVo(@RequestBody NcScheduleResult scheduleResult) {
        return ncScheduleResultService.getSummaryVo(scheduleResult);
    }
}

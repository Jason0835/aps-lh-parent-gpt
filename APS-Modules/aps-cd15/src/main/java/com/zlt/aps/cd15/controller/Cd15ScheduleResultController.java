package com.zlt.aps.cd15.controller;

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
import org.apache.commons.lang3.ObjectUtils;
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

import com.alibaba.csp.sentinel.util.StringUtil;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd15.api.domain.entity.Cd15DayFinishQty;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.engine.service.Cd15EngineService;
import com.zlt.aps.cd15.service.Cd15MachineInfoService;
import com.zlt.aps.cd15.service.Cd15ScheduleResultService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.common.engine.utils.DateUtil;
import com.zlt.aps.itf.vo.SyncDataLogs;
import com.zlt.sync.api.service.ISyncDataLogsApiService;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/**
 * 15度裁断排程结果Controller
 *
 * @author zlt
 * @date 2021-07-05
 */
@RestController
@RequestMapping("/cd15ScheduleResult")
@Slf4j
public class Cd15ScheduleResultController extends BaseController {
    @Value("${excelModelPath}")
    public String excelModelPath;
    @Autowired
    private Cd15ScheduleResultService cd15ScheduleResultService;
    @Autowired
    private Cd15MachineInfoService machineInfoService;
    @Autowired
    private Cd15EngineService cd15EngineService;
	@Resource
	private ISyncDataLogsApiService syncDataLogsService;
    /**
     * 查询15度裁断排程结果列表
     */
    //@PreAuthorize(hasPermi = "cd15:cd15ScheduleResult:list")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody Cd15ScheduleResult cd15ScheduleResult) {
//        startPage("a.big_roll_code,a.CUTTING_ANGLE asc");
        cd15ScheduleResult.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<Cd15ScheduleResult> list = cd15ScheduleResultService.selectCd15ScheduleResultList(cd15ScheduleResult);
        return getDataTable(list);
    }

    /**
     * 获取15度裁断排程结果详细信息
     */
    //@PreAuthorize(hasPermi = "cd15:cd15ScheduleResult:query")
    @GetMapping(value = "/{id}")
    public Cd15ScheduleResult getInfo(@PathVariable("id") Long id) {
        return cd15ScheduleResultService.selectCd15ScheduleResultById(id);
    }

    /**
     * 获取15度裁断排程结果详细信息
     */
    //@PreAuthorize(hasPermi = "cd15:cd15ScheduleResult:query")
    @PostMapping(value = "/getInfos")
    public List<Cd15ScheduleResult> getInfos(@RequestBody Cd15ScheduleResult cd15ScheduleResult) {
        return cd15ScheduleResultService.selectCd15ScheduleResultByIds(cd15ScheduleResult.getIds2());
    }

    /**
     * 新增15度裁断排程结果
     */
    @Log(title = "ui.data.column.cd15ScheduleResult.modalName", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Cd15ScheduleResult cd15ScheduleResult) {
        int exist = cd15ScheduleResultService.checkCd15CodeExist(cd15ScheduleResult);
        if (exist == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.specNotExist"));
        }
        List<Cd15ScheduleResult> scheduleResults = cd15ScheduleResultService.selectByScheduleDateAndBigRollCode(cd15ScheduleResult);
        int rows = cd15ScheduleResultService.insertCd15ScheduleResult(cd15ScheduleResult);
        cd15ScheduleResultService.insetDispatcherLogInsertOrder(ApsConstant.DISPATCHER_OPER_INSERT_ORDER, scheduleResults, cd15ScheduleResult);
        return toAjax(rows);
    }

    /**
     * 修改
     */
    @Log(title = "ui.data.column.cd15ScheduleResult.modalName", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd15ScheduleResult cd15ScheduleResult) {
    	if (cd15ScheduleResult.getId() != null) {
            int releasingOrTimeoutByDate = cd15ScheduleResultService.isReleasingOrTimeoutByIds(new Long[]{cd15ScheduleResult.getId()});
            if (releasingOrTimeoutByDate > 0) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
            }
    	}
        return toAjax(cd15ScheduleResultService.updateCd15ScheduleResult(cd15ScheduleResult));
    }

    /**
     * 调量
     */
    @Log(title = "ui.data.column.cd15ScheduleResult.modalName", businessType = BusinessType.CHANGE_QTY)
    @PostMapping("/changeQty")
    public AjaxResult changeQty(@RequestBody Cd15ScheduleResult scheduleResult) {
        int releasingOrTimeoutByDate = cd15ScheduleResultService.isReleasingOrTimeoutByIds(new Long[]{scheduleResult.getId()});
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        scheduleResult.setBaseVale(scheduleResult.getId());
        scheduleResult.setDayPlanQty1(scheduleResult.getDayPlanQty1() == null ? 0D : scheduleResult.getDayPlanQty1());
        scheduleResult.setNightPlanQty1(scheduleResult.getNightPlanQty1() == null ? 0D : scheduleResult.getNightPlanQty1());
        cd15ScheduleResultService.insetDispatcherLog(ApsConstant.DISPATCHER_OPER_PLAN, scheduleResult);  //如果是调度员操作，则需要增加操作日志
        return toAjax(cd15ScheduleResultService.updateCd15ScheduleResult(scheduleResult));
    }

    /**
     * 转机台
     */
    @Log(title = "ui.data.column.cd15ScheduleResult.modalName", businessType = BusinessType.CHANGE_MACHINE)
    @PostMapping("/changeMachine")
    public AjaxResult changeMachine(@RequestBody Cd15ScheduleResult scheduleResult) {
        int releasingOrTimeoutByDate = cd15ScheduleResultService.isReleasingOrTimeoutByIds(new Long[]{scheduleResult.getId()});
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        scheduleResult.setBaseVale(scheduleResult.getId());
        cd15ScheduleResultService.insetDispatcherLog(ApsConstant.DISPATCHER_OPER_MACHINE, scheduleResult);  //如果是调度员操作，则需要增加操作日志
        return toAjax(cd15ScheduleResultService.updateCd15ScheduleResult(scheduleResult));
    }

    /**
     * 选机台
     * @param scheduleResult 更改后机台信息
     * @return 结果
     */
    @PostMapping("/chooseMachine")
    public AjaxResult chooseMachine(@RequestBody Cd15ScheduleResult scheduleResult){
        int releasingOrTimeoutByDate = cd15ScheduleResultService.isReleasingOrTimeoutByIds(new Long[]{scheduleResult.getId()});
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        // 校验机台字段是否修改，未修改则返回成功
        Cd15ScheduleResult result = cd15ScheduleResultService.selectCd15ScheduleResultById(scheduleResult.getId());
        if (ObjectUtils.compare(result.getMachineId(), scheduleResult.getMachineId()) == 0) {
            return AjaxResult.success();
        }
        result.setMachineId(scheduleResult.getMachineId());
        if (CollectionUtils.isNotEmpty(cd15ScheduleResultService.checkScheduleResultUnique(result))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }
        cd15ScheduleResultService.chooseMachine(result);
        return AjaxResult.success();
    }

    /**
     * 删除15度裁断排程结果
     */
    @Log(title = "ui.data.column.cd15ScheduleResult.modalName", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
//        int releasingOrTimeoutByDate = cd15ScheduleResultService.isReleasingOrTimeoutByIds(ids);
//        if (releasingOrTimeoutByDate > 0) {
//            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
//        }
        if (cd15ScheduleResultService.isPublishByIds(ids) != ids.length) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isPublishById"));
        }
        return toAjax(cd15ScheduleResultService.deleteCd15ScheduleResultByIds(ids));
    }

    /**
     * 15度裁断排程结果列表
     */
    @PostMapping("/checkScheduleResultUnique")
    public List<Cd15ScheduleResult> checkScheduleResultUnique(@RequestBody Cd15ScheduleResult cd15ScheduleResult) {
        return cd15ScheduleResultService.checkScheduleResultUnique(cd15ScheduleResult);
    }

    /**
     * 唯一性判断
     */
    @PostMapping("/getList")
    public List<Cd15ScheduleResult> getList(@RequestBody Cd15ScheduleResult cd15ScheduleResult) {
//        startPage("a.big_roll_code,a.CUTTING_ANGLE asc");
        return cd15ScheduleResultService.selectCd15ScheduleResultList(cd15ScheduleResult);
    }

    /**
     * 导出列表
     */
    @Log(title = "ui.data.column.cd15ScheduleResult.modalName", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public byte[] export(@RequestBody Cd15ScheduleResult cd15ScheduleResult) throws Exception {

        //查询数据
//        startPage("a.big_roll_code,a.CUTTING_ANGLE asc");
        cd15ScheduleResult.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<Cd15ScheduleResult> list = cd15ScheduleResultService.selectCd15ScheduleResultList(cd15ScheduleResult);
        Cd15ScheduleResult summarySchedule = this.summaryExport(list);  //给导出的数据增加汇总行

        //按用户语言读取模板
        Locale lang = ServletUtils.getUserLang();
        InputStream in = null;
        if (Locale.SIMPLIFIED_CHINESE.equals(lang) || lang == null) {
            // 中文
            in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "cd15ScheduleResult.xlsx");
        } else if (Locale.US.equals(lang)) {
            // 英文
            in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "cd15ScheduleResult_en.xlsx");
        }
        Workbook webBook = ExcelUtils.readExcel(in);

        //填充数据
        if (CollectionUtils.isNotEmpty(list)) {
            List<Cd15MachineInfo> tcMachineInfoList = machineInfoService.selectMachineInfoList(new Cd15MachineInfo());
            Map<String, String> map = null;
            if (CollectionUtils.isNotEmpty(tcMachineInfoList)) {
                map = tcMachineInfoList.stream().collect(Collectors.toMap(item -> item.getId() + "", item -> item.getMachineName()));
            }
            DecimalFormat df = new DecimalFormat("0.00%");
            Sheet sheet = webBook.getSheetAt(0);
            CellStyle cellStyle = ExcelUtils.createCellStyle(webBook);
            DataFormat format = webBook.createDataFormat();
            cellStyle.setDataFormat(format.getFormat("[=0]\"\""));  //导出的单元格如果值为0，则显示空白
            int month = DateUtil.getMonth(cd15ScheduleResult.getScheduleDate());
            int day = DateUtil.getDay(cd15ScheduleResult.getScheduleDate());

            BigDecimal midPlan = new BigDecimal(summarySchedule.getDayPlanQty1());
            BigDecimal nightPlan = new BigDecimal(summarySchedule.getNightPlanQty1());
            for (int i = 0; i < list.size(); i++) {
                int n = 0;
                Cd15ScheduleResult scheduleResult = list.get(i);
                Row row = sheet.createRow(i + 2);
                row.createCell(n++).setCellValue(scheduleResult.getBigRollCode() == null ? "" : scheduleResult.getBigRollCode());
                row.createCell(n++).setCellValue(scheduleResult.getCuttingAngle() == null ? 0 : scheduleResult.getCuttingAngle());  //裁断角度

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
                row.createCell(n++).setCellValue(scheduleResult.getSteelStripCode1() == null ? "" : scheduleResult.getSteelStripCode1());
                row.createCell(n++).setCellValue(scheduleResult.getCraft1() == null ? "" : scheduleResult.getCraft1()); //工艺1
                row.createCell(n++).setCellValue(scheduleResult.getEdgeGlue() == null ? "" : scheduleResult.getEdgeGlue()); //半钢边胶
                row.createCell(n++).setCellValue(scheduleResult.getSteelStripCode2() == null ? "" : scheduleResult.getSteelStripCode2());
                row.createCell(n++).setCellValue(scheduleResult.getCraft2() == null ? "" : scheduleResult.getCraft2()); //工艺2
                row.createCell(n++).setCellValue(scheduleResult.getStock1Qty1() == null ? 0 : scheduleResult.getStock1Qty1());
                row.createCell(n++).setCellValue(scheduleResult.getStock1Qty2() == null ? 0 : scheduleResult.getStock1Qty2());
                row.createCell(n++).setCellValue(scheduleResult.getMonthPlanOs() == null ? 0 : scheduleResult.getMonthPlanOs());
                row.createCell(n++).setCellValue(scheduleResult.getSupplyTime1() == null ? 0 : scheduleResult.getSupplyTime1());
                row.createCell(n++).setCellValue(scheduleResult.getDailyTotalQty() == null ? 0 : scheduleResult.getDailyTotalQty());
                row.createCell(n++).setCellValue(scheduleResult.getTotalFinishQty() == null ? 0 : scheduleResult.getTotalFinishQty());
                row.createCell(n++).setCellValue(scheduleResult.getDailyFinishRate() == null ? "" : df.format(scheduleResult.getDailyFinishRate()));
                row.createCell(n++).setCellValue(scheduleResult.getDayPlanQty1() == null ? 0 : scheduleResult.getDayPlanQty1());
                row.createCell(n++).setCellValue(scheduleResult.getDayFinishQty1() == null ? 0 : scheduleResult.getDayFinishQty1());
                row.createCell(n++).setCellValue(scheduleResult.getDayProduceOrder1() == null ? 0 : scheduleResult.getDayProduceOrder1());
                row.createCell(n++).setCellValue(scheduleResult.getDayFinishRate1() == null ? "" : df.format(scheduleResult.getDayFinishRate1()));

                String sysAnaly = scheduleResult.getDaySysAnalysis1();
                String handAnaly = scheduleResult.getDayHandAnalysis1();
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
                row.createCell(n++).setCellValue(scheduleResult.getNightPlanQty1() == null ? 0 : scheduleResult.getNightPlanQty1());
                row.createCell(n++).setCellValue(scheduleResult.getNightFinishQty1() == null ? 0 : scheduleResult.getNightFinishQty1());
                row.createCell(n++).setCellValue(scheduleResult.getNightProduceOrder1() == null ? 0 : scheduleResult.getNightProduceOrder1());
                row.createCell(n++).setCellValue(scheduleResult.getNightFinishRate1() == null ? "" : df.format(scheduleResult.getNightFinishRate1()));

                String nightSysAnaly = scheduleResult.getNightSysAnalysis1();
                String nightHandAnaly = scheduleResult.getNightHandAnalysis1();
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
                dateStr=DateUtils.parseDateToStr("MM月dd日",cd15ScheduleResult.getScheduleDate());
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
            String baseInfo=I18nUtil.getMessage("ui.data.column.scheduleResult.cd15.baseInfo");
            String class1Plan=I18nUtil.getMessage("ui.data.column.scheduleResult.heji.zhongban");
            String class2Plan=I18nUtil.getMessage("ui.data.column.scheduleResult.heji.yeban");
            String totalQty=I18nUtil.getMessage("ui.data.column.scheduleResult.totalQty");
            String planInfo = '：'+class1Plan+'：'+midPlan.setScale(0, BigDecimal.ROUND_HALF_UP)+'，'+class2Plan+'：'+nightPlan.setScale(0,BigDecimal.ROUND_HALF_UP)+'，'+totalQty+'：'+(midPlan.add(nightPlan)).setScale(0,BigDecimal.ROUND_HALF_UP);
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
    private Cd15ScheduleResult summaryExport(List<Cd15ScheduleResult> list) {
        if(list == null || list.isEmpty()) {
            return null;
        }
        Cd15ScheduleResult summary = new  Cd15ScheduleResult();
        summary.setBigRollCode(I18nUtil.getMessage("ui.data.column.scheduleResult.totalQty"));
        summary.setDayPlanQty1(list.stream().mapToDouble(r->getDoubleOrDefault(r.getDayPlanQty1())).sum());
        summary.setDayFinishQty1(list.stream().mapToDouble(r->getDoubleOrDefault(r.getDayFinishQty1())).sum());
        summary.setNightPlanQty1(list.stream().mapToDouble(r->getDoubleOrDefault(r.getNightPlanQty1())).sum());
        summary.setNightFinishQty1(list.stream().mapToDouble(r->getDoubleOrDefault(r.getNightFinishQty1())).sum());
        summary.setDailyTotalQty(BigDecimalUtil.add(summary.getDayPlanQty1(), summary.getNightPlanQty1()));
        summary.setTotalFinishQty(BigDecimalUtil.add(summary.getDayFinishQty1(), summary.getNightFinishQty1()));

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
    @Log(title = "ui.data.column.cd15ScheduleResult.modalName", businessType = BusinessType.AUTOPLAN)
    @PostMapping("/autoPlan")
    public AjaxResult autoPlan(@RequestBody Cd15ScheduleResult cd15ScheduleResult) {
        //执行自动排程算法
        Date scheduleDate = cd15ScheduleResult.getScheduleDate();
        cd15EngineService.autoCd15Schedule(scheduleDate);

        return AjaxResult.success();
    }

    /**
     * 排程发布
     */
    @Log(title = "ui.data.column.cd15ScheduleResult.modalName", businessType = BusinessType.PUBLISH)
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody Cd15ScheduleResult cd15ScheduleResult) {
    	// 发布前需要先获得同步锁，防止在集群环境下出现一个前端命令发送两次mes请求，modify by hak 20220708
    	if (syncDataLogsService.checkPublishLocking("cd15:publish:lock", cd15ScheduleResult.getIds())) {
    		return AjaxResult.success(); // 如果已经被锁定了，则直接返回
    	}
    	
        int releasingOrTimeoutByDate = cd15ScheduleResultService.isReleasingOrTimeoutByIds(cd15ScheduleResult.getIds());
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        //查询排程发布list
        cd15ScheduleResult.setYear(DateFormatUtils.format(cd15ScheduleResult.getScheduleDate(), "yyyy"));
        cd15ScheduleResult.setMonth(DateFormatUtils.format(cd15ScheduleResult.getScheduleDate(), "MM"));
        // 过滤未发布及发布失败的数据
        List<Cd15ScheduleResult> list = cd15ScheduleResultService.selectCd15ScheduleResultList(cd15ScheduleResult)
                .stream().filter(item -> ApsConstant.NO_RELEASE.equals(item.getIsRelease()) || ApsConstant.FAILURE_RELEASE.equals(item.getIsRelease()) || ApsConstant.WAIT_RELEASING.equals(item.getIsRelease())).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.errorPublish"));
        }
        //校验是否单机台
        List<Cd15ScheduleResult> collect = list.stream().filter(item -> StringUtil.isEmpty(item.getMachineId()) || item.getMachineId().contains(",")).collect(Collectors.toList());
        if (collect.size() > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.hasMultipleIds"));
        }
        //执行排程发布对象-->list 不是collect

        //更新发布状态
        long[] arr = list.stream().mapToLong(item -> item.getId()).toArray();
        Date scheduleDate = list.get(0).getScheduleDate();
        AjaxResult ajaxResult = null;
        // 获取下发接口版本号
        String dataVersion = syncDataLogsService.getDataVersion(ApsConstant.CD15_DEPLOY_SYNC_KEY);
        try {
			cd15ScheduleResultService.batchUpdate(arr,scheduleDate, dataVersion);
	        // 给mes发送排程下发通知
	        cd15ScheduleResultService.publishNoticeMes(scheduleDate, dataVersion, arr.length);

			// 取回mes的反馈结果
			SyncDataLogs logs = syncDataLogsService.getSyncDataResult(dataVersion);
			String status = logs.getStatus();
			// 更新状态
			cd15ScheduleResultService.updateRelaseStatus(dataVersion, arr, status);
			if (ApsConstant.IS_RELEASE.equals(status)) {
				// 成功
				ajaxResult = AjaxResult.success(I18nUtil.getMessage("ui.data.column.scheduleResult.successPublish"));
			} else {
				// 失败，需要返回异常信息
				ajaxResult = AjaxResult.error(logs.getMsg());
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.failedPublish"));
		}
        
        return ajaxResult;
    }

    /**
     * 查询排程日期是否已发布
     * @return 是否已经发布
     */
    @PostMapping("/isPublish")
    public Boolean isPublish(@RequestBody Cd15ScheduleResult entity){
        return cd15ScheduleResultService.isPublish(entity.getScheduleDate());
    }

    /**
     * 导入数据
     */
    @Log(title = "ui.data.column.cd15ScheduleResult.modalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<Cd15ScheduleResult> list, @RequestParam("importLogId") Long importLogId,@RequestParam("scheduleDate")String scheduleDate) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cd15ScheduleResultService.importData(list, importLogId,scheduleDate);
    }

    /**
     * 均衡
     */
    @Log(title = "ui.data.column.cd15ScheduleResult.modalName", businessType = BusinessType.BALANCE)
    @PostMapping("/balance")
    public AjaxResult balance(@RequestBody Cd15ScheduleResult entity){
        Date scheduleDate = entity.getScheduleDate();
        if (scheduleDate == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        int releasingOrTimeoutByDate = cd15ScheduleResultService.isReleasingOrTimeoutByDate(scheduleDate);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        // 调用引擎手工均衡接口
        cd15EngineService.handleEquilibrium(scheduleDate);
        return AjaxResult.success(scheduleDate);
    }

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @PostMapping("/isReleasingOrTimeoutByDate")
    public int isReleasingOrTimeoutByDate(@RequestBody Cd15ScheduleResult scheduleResult){
        return cd15ScheduleResultService.isReleasingOrTimeoutByDate(scheduleResult.getScheduleDate());
    }

    /**
     * 更改发布状态
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @Log(title = "ui.data.column.cd15ScheduleResult.modalName", businessType = BusinessType.BALANCE)
    @PostMapping("/changeReleaseStatus")
    public AjaxResult changeReleaseStatus(@RequestBody Cd15ScheduleResult entity){
        cd15ScheduleResultService.changeReleaseStatus(entity);
        return AjaxResult.success();
    }

    /**
     * 归并中夜班计划量，合并到同一个班次
     *
     * @param ids             id
     * @param classifiedShift 合并班次
     */
    @Log(title = "ui.data.column.cd15ScheduleResult.modalName", businessType = BusinessType.CONSOLIDATION)
    @PostMapping("/combinationMiddleAndNight/{ids}")
    public AjaxResult combinationMiddleAndNight(@PathVariable("ids")Long[] ids, @RequestParam("classifiedShift") String classifiedShift) {
        int releasingOrTimeoutByDate = cd15ScheduleResultService.isReleasingOrTimeoutByIds(ids);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        cd15ScheduleResultService.combinationMiddleAndNight(ids, classifiedShift);
        return AjaxResult.success();
    }

    /**
     * 导入完成量
     * @param list 完成量集合
     * @param importLogId 导入记录id
     * @return 结果
     */
    @PostMapping("/importFinishQty")
    @ApiOperation("导入完成量")
    public AjaxResult importFinishQty(@RequestBody List<Cd15DayFinishQty> list, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.isEmpty()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cd15ScheduleResultService.importFinishQty(list, importLogId);
    }

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    @PostMapping("/getSummaryVo")
    @ApiOperation("获取排程日期的排程结果合计")
    public AjaxResult getSummaryVo(@RequestBody Cd15ScheduleResult scheduleResult) {
        return cd15ScheduleResultService.getSummaryVo(scheduleResult);
    }
}

package com.zlt.aps.xwyy.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.util.StringUtil;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.engine.domain.SyncDataLogs;
import com.zlt.aps.common.engine.service.SyncDataLogsService;
import com.zlt.aps.common.engine.utils.BeanConverUtil;
import com.zlt.aps.xwyy.api.domain.dto.XwyyScheduleResultDto;
import com.zlt.aps.xwyy.api.domain.entity.XwyyDayFinishQty;
import com.zlt.aps.xwyy.api.domain.vo.HalfYyExportDataVo;
import com.zlt.aps.xwyy.common.handle.XwyySyncDataHandle;
import com.zlt.aps.xwyy.engine.service.XwyyEngineService;
import com.zlt.aps.xwyy.entity.XwyyParams;
import com.zlt.aps.xwyy.entity.XwyyScheduleResult;
import com.zlt.aps.xwyy.mapper.XwyyParamsMapper;
import com.zlt.aps.xwyy.service.XwyyScheduleResultService;
import com.zlt.common.utils.ExcelReadUtils;
import com.zlt.common.utils.ImportExcelUtils;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 纤维压延排程结果Controller
 *
 * @author chen
 * @date 2021-07-06
 */
@RestController
@RequestMapping("/xwyy/scheduleResult")
@Slf4j
public class XwyyScheduleResultController extends BaseController {
    @Autowired
    private XwyyScheduleResultService xwyyScheduleResultService;
    @Autowired
    private XwyyEngineService xwyyEngineService;
    @Resource
    private XwyySyncDataHandle xwyySyncDataHandle;
	@Resource
	private SyncDataLogsService syncDataLogsService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private XwyyParamsMapper xwyyParamsMapper;

    /**
     * 查询纤维压延排程结果列表
     */
    @ApiOperation("查询纤维压延排程结果列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody XwyyScheduleResultDto dto) {
//        startPage("a.CREATE_TIME");
        XwyyScheduleResult scheduleResult = new XwyyScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        scheduleResult.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<XwyyScheduleResultDto> list = xwyyScheduleResultService.selectScheduleResultList(scheduleResult);
        return getDataTable(list);
    }

    /**
     * 获取纤维压延排程结果详细信息
     */
    @ApiOperation("获取纤维压延排程结果详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public XwyyScheduleResultDto getInfo(@PathVariable("id") Long id) {
        return xwyyScheduleResultService.selectScheduleResultById(id);
    }

    @PostMapping(value = "/getInfos")
    public List<XwyyScheduleResultDto> getInfos(@RequestBody XwyyScheduleResultDto scheduleResult) {
        return xwyyScheduleResultService.selectByIds(scheduleResult.getIds2());
    }

    /**
     * 修改纤维压延排程结果
     */
    @Log(title = "ui.data.column.xwyy.scheduleResult.modelName", businessType = com.ruoyi.common.log.enums.BusinessType.INSERT_OR_UPDATE)
    @PostMapping("/edit")
    @ApiOperation("修改纤维压延排程结果（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@RequestBody XwyyScheduleResultDto dto) {
        XwyyScheduleResult scheduleResult = new XwyyScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        if (dto.getId() == null) {
            int exist = xwyyScheduleResultService.checkXwyyCodeExist(dto);
            if (exist == 0) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.specNotExist"));
            }
        }

        if (scheduleResult.getId() != null) {
            int releasingOrTimeoutByDate = xwyyScheduleResultService.isReleasingOrTimeoutByIds(new long[]{scheduleResult.getId()});
            if (releasingOrTimeoutByDate > 0) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
            }
        }
        // 唯一性校验
        // 根据传入的日期查询是否已有对应排程记录
        Boolean unique = xwyyScheduleResultService.checkUnique(scheduleResult);
        if (!unique) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }
        xwyyScheduleResultService.saveScheduleResult(scheduleResult);
        return AjaxResult.success();
    }

    /**
     * 转机台
     */
    @Log(title = "ui.data.column.xwyy.scheduleResult.modelName", businessType = BusinessType.CHANGE_MACHINE)
    @PostMapping("/changeMachine")
    @ApiOperation("转机台")
    public AjaxResult changeMachine(@RequestBody XwyyScheduleResultDto dto) {
        XwyyScheduleResult scheduleResult = new XwyyScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        int releasingOrTimeoutByDate = xwyyScheduleResultService.isReleasingOrTimeoutByIds(new long[]{scheduleResult.getId()});
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        // 根据传入的日期查询是否已有对应排程记录
        Boolean unique = xwyyScheduleResultService.checkUnique(scheduleResult);
        if (!unique) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }
        xwyyScheduleResultService.insetDispatcherLog(ApsConstant.DISPATCHER_OPER_MACHINE, scheduleResult);  //如果是调度员操作，则需要增加操作日志
        xwyyScheduleResultService.saveScheduleResult(scheduleResult);
        return AjaxResult.success();
    }

    /**
     * 调量
     */
    @Log(title = "ui.data.column.xwyy.scheduleResult.modelName", businessType = com.ruoyi.common.log.enums.BusinessType.CHANGE_QTY)
    @PostMapping("/changeQty")
    @ApiOperation("调量")
    public AjaxResult changeQty(@RequestBody XwyyScheduleResultDto dto) {
        XwyyScheduleResult scheduleResult = new XwyyScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        int releasingOrTimeoutByDate = xwyyScheduleResultService.isReleasingOrTimeoutByIds(new long[]{scheduleResult.getId()});
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        scheduleResult.setDayPlanQty(scheduleResult.getDayPlanQty() == null ? 0D : scheduleResult.getDayPlanQty());
        scheduleResult.setNightPlanQty(scheduleResult.getNightPlanQty() == null ? 0D : scheduleResult.getNightPlanQty());
        xwyyScheduleResultService.insetDispatcherLog(ApsConstant.DISPATCHER_OPER_PLAN, scheduleResult);  //如果是调度员操作，则需要增加操作日志
        xwyyScheduleResultService.saveScheduleResult(scheduleResult);
        return AjaxResult.success();
    }

    /**
     * 选机台
     * @param dto 更改后机台信息
     * @return 结果
     */
    @PostMapping("/chooseMachine")
    public AjaxResult chooseMachine(@RequestBody XwyyScheduleResultDto dto){
        int releasingOrTimeoutByDate = xwyyScheduleResultService.isReleasingOrTimeoutByIds(new long[]{dto.getId()});
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        // 校验机台字段是否修改，未修改则返回成功
        XwyyScheduleResultDto result = xwyyScheduleResultService.selectScheduleResultById(dto.getId());
        if (ObjectUtils.compare(result.getMachineId(), dto.getMachineId()) == 0) {
            return AjaxResult.success();
        }
        result.setMachineId(dto.getMachineId());
        XwyyScheduleResult gsqScheduleResult = new XwyyScheduleResult();
        BeanUtils.copyProperties(result, gsqScheduleResult);
        if (!xwyyScheduleResultService.checkUnique(gsqScheduleResult)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }
        xwyyScheduleResultService.chooseMachine(gsqScheduleResult);
        return AjaxResult.success();
    }

    /**
     * 删除纤维压延排程结果
     */
    @Log(title = "ui.data.column.xwyy.scheduleResult.modelName", businessType = com.ruoyi.common.log.enums.BusinessType.DELETE)
    @PostMapping("/remove")
    @ApiOperation("删除纤维压延排程结果信息")
    public AjaxResult remove(@RequestBody List<XwyyScheduleResultDto> removeList) {
//        int releasingOrTimeoutByDate = xwyyScheduleResultService.isReleasingOrTimeoutByIds(ids);
//        if (releasingOrTimeoutByDate > 0) {
//            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
//        }
        Long[] ids = removeList.stream().map(XwyyScheduleResultDto::getId).toArray(Long[]::new);
        if (xwyyScheduleResultService.isPublishByIds(ids) != ids.length) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isPublishById"));
        }
        List<XwyyScheduleResult> list = BeanConverUtil.converList(removeList, XwyyScheduleResult.class);
        xwyyScheduleResultService.deleteScheduleResultByIds(ids, list);
        return AjaxResult.success();
    }

    /**
     * 导出纤维压延排程结果列表
     */
    @Log(title = "ui.data.column.xwyy.scheduleResult.modelName", businessType = com.ruoyi.common.log.enums.BusinessType.EXPORT)
    @ApiOperation("导出纤维压延排程结果列表")
    @PostMapping("/export")
    public byte[] export(@RequestBody XwyyScheduleResultDto dto) {
//        startPage("a.CREATE_TIME");
        XwyyScheduleResult scheduleResult = new XwyyScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        scheduleResult.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<XwyyScheduleResultDto> list = xwyyScheduleResultService.selectScheduleResultList(scheduleResult);
        return xwyyScheduleResultService.export(list);
    }

    /**
     * 查询纤维压延排程结果列表
     */
    @ApiOperation("查询纤维压延排程结果列表")
    @PostMapping("/getList")
    public List<XwyyScheduleResultDto> getList(@RequestBody XwyyScheduleResultDto dto) {
        XwyyScheduleResult scheduleResult = new XwyyScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        return xwyyScheduleResultService.selectScheduleResultList(scheduleResult);
    }

    /**
     * 查询纤维压延排程结果列表
     */
    @Log(title = "ui.data.column.xwyy.ScheduleResult.modalName", businessType = com.ruoyi.common.log.enums.BusinessType.PUBLISH)
    @ApiOperation("发布排程")
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody XwyyScheduleResultDto dto) {
    	// 发布前需要先获得同步锁，防止在集群环境下出现一个前端命令发送两次mes请求，modify by hak 20220708
    	if (syncDataLogsService.checkPublishLocking("xwyy:publish:lock", dto.getIds())) {
    		return AjaxResult.success(); // 如果已经被锁定了，则直接返回
    	}

        XwyyScheduleResult scheduleResult = new XwyyScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        int releasingOrTimeoutByDate = xwyyScheduleResultService.isReleasingOrTimeoutByIds(ArrayUtils.toPrimitive(scheduleResult.getIds()));
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        scheduleResult.setYear(DateFormatUtils.format(dto.getScheduleDate(), "yyyy"));
        scheduleResult.setMonth(DateFormatUtils.format(dto.getScheduleDate(), "MM"));

        // 过滤未发布及发布失败的数据
        List<XwyyScheduleResultDto> list = xwyyScheduleResultService.selectScheduleResultList(scheduleResult)
                .stream().filter(item -> ApsConstant.NO_RELEASE.equals(item.getIsRelease()) || ApsConstant.FAILURE_RELEASE.equals(item.getIsRelease()) || ApsConstant.WAIT_RELEASING.equals(item.getIsRelease())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.errorPublish"));
        }
        // 获取机台id为空和多机台的记录
        List<XwyyScheduleResultDto> collect = list.stream().filter(item -> StringUtil.isEmpty(item.getMachineId()) || item.getMachineId().contains(",")).collect(Collectors.toList());
        if (collect.size() > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.hasMultipleIds"));
        }
        long[] arr = list.stream().mapToLong(XwyyScheduleResultDto::getId).toArray();
        // 获取下发接口版本号
        String dataVersion = xwyySyncDataHandle.getDataVersion(ApsConstant.XWYY_DEPLOY_SYNC_KEY);
        AjaxResult ajaxResult = null;
        try {
			// 发布排程记录
			xwyyScheduleResultService.publish(scheduleResult, arr, dataVersion);

            xwyyScheduleResultService.publishNoticeMes(dto.getScheduleDate(), dataVersion, arr.length);

			// 取回mes的反馈结果
            SyncDataLogs logs = syncDataLogsService.getSyncDataResult(dataVersion);
//            SyncDataLogs logs = new SyncDataLogs();
//            logs.setStatus("2");
//            logs.setMsg("测试");
            String status = logs.getStatus();
			// 更新状态
			xwyyScheduleResultService.updateRelaseStatus(dataVersion, arr, status);
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
     * 自动排程
     */
    @Log(title = "ui.data.column.xwyy.scheduleResult.modelName", businessType = com.ruoyi.common.log.enums.BusinessType.AUTOPLAN)
    @ApiOperation("自动排程")
    @PostMapping("/autoPlan")
    public AjaxResult autoPlan(@RequestBody XwyyScheduleResultDto dto) {
        // 调用引擎自动排程接口
    	xwyyEngineService.autoXwyySchedule(dto.getScheduleDate());
        return AjaxResult.success("自动排程成功");
    }

    /**
     * 查询排程日期是否已发布
     *
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    @ApiOperation("查询排程日期是否已发布")
    @PostMapping("/isPublish")
    public Boolean isPublish(@RequestBody XwyyScheduleResultDto dto) {
        return xwyyScheduleResultService.isPublish(dto.getScheduleDate());
    }

    /**
     * 查询排程记录是否唯一
     *
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    @ApiOperation("查询排程记录是否唯一")
    @PostMapping("/checkUnique")
    public Boolean checkUnique(@RequestBody XwyyScheduleResultDto dto) {
        XwyyScheduleResult xwyyScheduleResult = new XwyyScheduleResult();
        BeanUtils.copyProperties(dto, xwyyScheduleResult);
        return xwyyScheduleResultService.checkUnique(xwyyScheduleResult);
    }

    @Log(title = "ui.data.column.xwyy.scheduleResult.modelName", businessType = com.ruoyi.common.log.enums.BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入纤维压延排程结果信息")
    public AjaxResult importData(@RequestBody List<XwyyScheduleResultDto> list, @RequestParam("importLogId") Long importLogId, @RequestParam("scheduleDate") String scheduleDate) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return xwyyScheduleResultService.importData(list, importLogId, DateUtils.parseDate(scheduleDate));
    }


    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @PostMapping("/isReleasingOrTimeoutByDate")
    public int isReleasingOrTimeoutByDate(@RequestBody XwyyScheduleResultDto dto){
        XwyyScheduleResult scheduleResult = new XwyyScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        return xwyyScheduleResultService.isReleasingOrTimeoutByDate(scheduleResult.getScheduleDate());
    }

    /**
     * 更改发布状态
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @Log(title = "ui.data.column.xwyyScheduleResult.modalName", businessType = com.ruoyi.common.log.enums.BusinessType.BALANCE)
    @PostMapping("/changeReleaseStatus")
    public AjaxResult changeReleaseStatus(@RequestBody XwyyScheduleResultDto dto){
        XwyyScheduleResult scheduleResult = new XwyyScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        xwyyScheduleResultService.changeReleaseStatus(scheduleResult);
        return AjaxResult.success();
    }

    /**
     * 根据帘布大卷代号获取帘线大卷标准长度
     * @param bigRollCode 帘布大卷代号
     * @return 帘线大卷标准长度
     */
    @PostMapping("/getActClothLength")
    public AjaxResult getActClothLength(@RequestBody String bigRollCode) {
        return AjaxResult.success(xwyyScheduleResultService.getActClothLength(bigRollCode));
    }

    /**
     * 归并中夜班计划量，合并到同一个班次
     *
     * @param ids             id
     * @param classifiedShift 合并班次
     */
    @Log(title = "ui.data.column.xwyy.scheduleResult.modelName", businessType = com.ruoyi.common.log.enums.BusinessType.CONSOLIDATION)
    @PostMapping("/combinationMiddleAndNight/{ids}")
    public AjaxResult combinationMiddleAndNight(@PathVariable("ids")long[] ids, @RequestParam("classifiedShift") String classifiedShift) {
        int releasingOrTimeoutByDate = xwyyScheduleResultService.isReleasingOrTimeoutByIds(ids);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        xwyyScheduleResultService.combinationMiddleAndNight(ids, classifiedShift);
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
    public AjaxResult importFinishQty(@RequestBody List<XwyyDayFinishQty> list, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.isEmpty()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return xwyyScheduleResultService.importFinishQty(list, importLogId);
    }

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    @PostMapping("/getSummaryVo")
    @ApiOperation("获取排程日期的排程结果合计")
    public AjaxResult getSummaryVo(@RequestBody XwyyScheduleResultDto scheduleResult) {
        return xwyyScheduleResultService.getSummaryVo(scheduleResult);
    }

    /**
     * 将排程数据导出到文件
     *
     * @param importContext 导入上下文
     * @return 结果
     */
    @Log(title = "ui.data.column.HalfYyExportData.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("将排程数据导出到文件")
    @PostMapping("/importExcelToListAndExport")
    public byte[] importExcelToListAndExport(@RequestBody ImportContext importContext, HttpServletResponse response) throws Exception {
        Date beginTime = DateUtils.getNowDate();
        InputStream is = new ByteArrayInputStream(importContext.getFileBytes());
        String fileName = I18nUtil.getMessage("ui.data.column.halfYyExportData.modelName");
        Workbook workbook = this.importExcelToListAndExport("", is, response, fileName);
        byte[] resultBytes = ExcelReadUtils.writeExcel(workbook);
        Date endTime = DateUtils.getNowDate();
        ExportLog exportLog = new ExportLog();
        exportLog.setProcedureCode("0");
        String uri = ServletUtils.getRequest().getRequestURI();
        exportLog.setFunctionCode(uri.split("/")[1]);
        exportLog.setFunctionName(fileName);
        exportLog.setFileName(fileName + ".xlsx");
        exportLog.setBeginTime(beginTime);
        exportLog.setEndTime(endTime);
        exportLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        this.iExportLogService.add(exportLog);
        return resultBytes;
    }

    /**
     * 将排程数据导出到文件
     *
     * @param sheetName 页签名
     * @param is        输入流
     * @param response  结果
     * @param fileName  文件名
     * @return 结果
     * @throws Exception 异常
     */
    public Workbook importExcelToListAndExport(String sheetName, InputStream is, HttpServletResponse response, String fileName) throws Exception {
        // 设置更低的压缩率阈值（例如 0.005），避免Zip bomb detected! 报错
        ZipSecureFile.setMinInflateRatio(0.005);

        Workbook wb = WorkbookFactory.create(is);
        List<HalfYyExportDataVo> list = new LinkedList<>();
        List<HalfYyExportDataVo> nextDayList = new LinkedList<>();
        Sheet sheet = null;
        if (StringUtils.isNotEmpty(sheetName)) {
            sheet = wb.getSheet(sheetName);
        } else {
            sheet = wb.getSheetAt(0);
        }

        if (sheet == null) {
            throw new IOException(I18nUtil.getMessage("common.error.util.file.sheet.noexist"));
        } else {
            Object scheduleDateStr = getCellValue(sheet.getRow(0), 0);
            String dateStr = scheduleDateStr.toString().substring(0, 10);
            Date scheduleDate = DateUtils.parseDate(dateStr.trim(), "yyyy年MM月dd日");
            scheduleDate = DateUtils.addDays(scheduleDate, 1);
            // 重算计划用量公式
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
            Map<String, HalfYyExportDataVo> exportDataVoMap = new HashMap<>(16);
            List<HalfYyExportDataVo> resultList = xwyyScheduleResultService.exportDataToList(scheduleDate);
            if (CollectionUtils.isNotEmpty(resultList)) {
                exportDataVoMap = resultList.stream().collect(Collectors.toMap(HalfYyExportDataVo::getCode, Function.identity()));
            }
            List<String> continueFieldNameList = Arrays.asList("code", "qty", "scrq", "scheduleDate");
            // 将列表数据导出到excel
            int startRow = 3;
            int endRow = 19;
            int headRow = 1;
            int headStartCol = 7;
            int headEndCol = 22;
            // 日期开始时间是scheduleDate的前一天，偏移量比开始列号大1
            int dateCellOffset = 8;
            List<Field> classField = getClassField(HalfYyExportDataVo.class);
            for (int i = headStartCol; i <= headEndCol; i++) {
                Row row = sheet.getRow(headRow);
                Date date = DateUtils.addDays(scheduleDate, i - dateCellOffset);
                Cell cell = row.getCell(i);
                if (cell != null) {
                    cell.setCellValue(DateUtils.parseDateToStr("MM-dd", date));
                }
            }

            for (int i = startRow; i < endRow; i++) {
                Row row = sheet.getRow(i);
                String cellValueCode = getCellValue(row, 0).toString();
                if (exportDataVoMap.containsKey(cellValueCode)) {
                    HalfYyExportDataVo halfYyExportDataVo = exportDataVoMap.get(cellValueCode);
                    for (int j = 0; j < classField.size(); j++) {
                        Field field = classField.get(j);
                        String fieldName = field.getName();
                        if (continueFieldNameList.contains(fieldName)) {
                            continue;
                        }
                        Object fieldValue = ReflectUtils.getFieldValue(halfYyExportDataVo, fieldName);
                        if (Objects.nonNull(fieldValue)) {
                            double cellValue = Double.parseDouble(fieldValue.toString());
                            Cell cell = row.getCell(j);
                            // 原有单元格有值，重新写入值可能未覆盖，这里需要清空原有值
                            cell.setBlank();
                            if (cellValue > 0) {
                                cell.setCellValue(cellValue);
                            }
                        } else {
                            Cell cell = row.getCell(j);
                            if (cell != null) {
                                cell.setBlank();
                            }
                        }
                    }
                }
            }
            evaluator.clearAllCachedResultValues();
            evaluator.evaluateAll();
        }
        return wb;
    }

    public Object getCellValue(Row row, int column) {
        if (row == null) {
            return row;
        } else {
            Object val = "";

            try {
                Cell cell = row.getCell(column);
                if (StringUtils.isNotNull(cell)) {
                    if (cell.getCellType() != CellType.NUMERIC && cell.getCellType() != CellType.FORMULA) {
                        if (cell.getCellType() == CellType.STRING) {
                            val = cell.getStringCellValue();
                        } else if (cell.getCellType() == CellType.BOOLEAN) {
                            val = cell.getBooleanCellValue();
                        } else if (cell.getCellType() == CellType.ERROR) {
                            val = cell.getErrorCellValue();
                        }
                    } else {
                        val = cell.getNumericCellValue();
                        if (DateUtil.isCellDateFormatted(cell)) {
                            val = DateUtils.getJavaDate((Double) val, TimeZone.getDefault());
                        } else if ((Double) val % 1.0 != 0.0) {
                            val = new BigDecimal(val.toString());
                        } else {
                            val = (new DecimalFormat("0")).format(val);
                        }
                    }
                }

                return val;
            } catch (Exception var5) {
                return val;
            }
        }
    }

    public List<Field> getClassField(Class<? super HalfYyExportDataVo> tClass) {
        List<Field> tempFields = new ArrayList<>();

        while (tClass != null) {
            tempFields.addAll(Arrays.asList(tClass.getDeclaredFields()));
            tClass = tClass.getSuperclass();
            if (StringUtils.equals(tClass.getSimpleName(), BaseEntity.class.getSimpleName())) {
                break;
            }
        }

        return tempFields;
    }

    /**
     * 将线下排程模板的昨日计划、昨日库存，导入到系统
     *
     * @param importContext 导入数据
     * @return 结果
     */
    @Log(title = "ui.data.column.HalfYyExportData.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("将线下排程模板的昨日计划、昨日库存，导入到系统")
    @PostMapping("/importExcelToLastDayPlanAndStock")
    public AjaxResult importExcelToLastDayPlanAndStock(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(), importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(), importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        InputStream is = new ByteArrayInputStream(importContext.getFileBytes());
        List<HalfYyExportDataVo> list = this.importExcel("", is, 3, 18);
        AjaxResult ajaxResult = xwyyScheduleResultService.importExcelToLastDayPlanAndStock(list);
        Date endTime = DateUtils.getNowDate();
        importLog.setRowCount(list.size());
        importLog.setBeginTime(beginTime);
        importLog.setEndTime(endTime);
        importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        ImportExcelUtils.updateImportLogAndFormatMsg(importLog, ajaxResult, this.iImportLogService);
        ImportExcelUtils.saveImportErrorLogs(ajaxResult, this.iImportErrorLogService);
        return ajaxResult;
    }

    public List<HalfYyExportDataVo> importExcel(String sheetName, InputStream is, Integer dataStartRowNum, Integer lastRowNum) throws Exception {
        // 设置更低的压缩率阈值（例如 0.005），避免Zip bomb detected! 报错
        ZipSecureFile.setMinInflateRatio(0.005);

        Workbook wb = WorkbookFactory.create(is);
        List<HalfYyExportDataVo> list = new LinkedList<>();
        Sheet sheet = null;
        if (StringUtils.isNotEmpty(sheetName)) {
            sheet = wb.getSheet(sheetName);
        } else {
            sheet = wb.getSheetAt(0);
        }

        if (sheet == null) {
            throw new IOException(I18nUtil.getMessage("common.error.util.file.sheet.noexist"));
        } else {
            int scheduleDateRowNum = 0;

            LambdaQueryWrapper<XwyyParams> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(XwyyParams::getParamCode, "XWYY_IMPORT_DATE_ROW_NUM");
            XwyyParams xwyyParams = xwyyParamsMapper.selectOne(wrapper);
            if (xwyyParams != null) {
                try {
                    scheduleDateRowNum = Integer.parseInt(xwyyParams.getParamValue());
                } catch (NumberFormatException e) {
                    log.error("XWYY_IMPORT_DATE_ROW_NUM参数错误，取默认值0");
                }
            }
            Object scheduleDateStr = getCellValue(sheet.getRow(scheduleDateRowNum), 0);
            String dateStr = scheduleDateStr.toString().substring(0, 11);
            Date scheduleDate = DateUtils.parseDate(dateStr.trim(), "yyyy年MM月dd日");

            int stockColumn = 3;
            int findDateRowNum = scheduleDateRowNum + 1;
            int planColumnNum = 7;
            Row dateRow = sheet.getRow(findDateRowNum);
            while (true) {
                Object cellValue = getCellValue(dateRow, planColumnNum);
                if (cellValue instanceof Date) {
                    if (scheduleDate.compareTo((Date) cellValue) == 0
                            // 避免死循环
                            || planColumnNum > 99999) {
                        break;
                    }
                }
                planColumnNum++;
            }

            for (int i = dataStartRowNum; i < sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                Object cellValue = getCellValue(row, 0);
                if ("合计".equals(cellValue)) {
                    break;
                }
                if (StringUtils.isNotBlank(cellValue.toString())) {
                    HalfYyExportDataVo halfYyExportDataVo = new HalfYyExportDataVo();
                    halfYyExportDataVo.setCode(cellValue.toString());
                    halfYyExportDataVo.setScheduleDate(scheduleDate);
                    Object stockCellValue = getCellValue(row, stockColumn);
                    try {
                        halfYyExportDataVo.setStockQty(Double.parseDouble(stockCellValue.toString()));
                    } catch (NumberFormatException e) {
                        log.error("库存列，数值转换异常：{}", e.getMessage());
                    }
                    Object planCellValue = getCellValue(row, planColumnNum);
                    try {
                        halfYyExportDataVo.setDay0(Double.parseDouble(planCellValue.toString()));
                    } catch (NumberFormatException e) {
                        log.error("计划量列，数值转换异常：{}", e.getMessage());
                        halfYyExportDataVo.setDay0(0D);
                    }
                    list.add(halfYyExportDataVo);
                }
            }
            return list;
        }
    }
}

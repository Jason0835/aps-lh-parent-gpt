package com.zlt.aps.cx.controller;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.FactoryService;
import com.zlt.aps.common.SyncDataLogsService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.constants.CxEngineConstants;
import com.zlt.aps.cx.handle.CxScheduleResultCheckHandle;
import com.zlt.aps.cx.handle.CxSyncDataHandle;
import com.zlt.aps.cx.mapper.entity.CxScheduleResultEntityMapper;
import com.zlt.aps.cx.service.CxScheduleResultService;
import com.zlt.aps.cxlh.cx.api.domain.dto.CxTransferDeskDTO;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxOnlineImport;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cxlh.cx.api.domain.vo.CxGanttVo;
import com.zlt.aps.domain.SyncDataLogs;
import com.zlt.aps.lh.api.domain.bo.ValidateResult;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.ExcelReadUtils;
import com.zlt.common.utils.ImportExcelUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.sync.povo.SyncParamsVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 成型排程服务入口
 *
 * @author LTL-Nick
 */
@Slf4j
@Api("成型排程接口")
@RestController
@RequestMapping("/cxScheduleResult")
public class CxScheduleResultController extends AbstractDocBizController<CxScheduleResult> {

    @Autowired
    private CxScheduleResultService cxScheduleResultService;
    @Autowired
    private CxScheduleResultEntityMapper cxScheduleResultEntityMapper;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private CxScheduleResultCheckHandle cxScheduleResultCheckHandle;
    @Resource
    private SyncDataLogsService syncDataLogsService;
    @Resource
    private CxSyncDataHandle syncDataHandle;
    @Autowired
    private FactoryService factoryService;

    @Autowired
    private IExportLogService iExportLogService;

    /**
     * 查询2025212成型排程列表查询
     */
    @ApiOperation("查询默认子表数据")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody CxScheduleResult entity) {
        return super.list(entity);
    }


    @Override
    protected String[] getQueryFormulas() {
        return new String[]{
                "createByName->getcolvalue(SYS_USER, nick_name, user_name, createBy)",
                "updateByName->getcolvalue(SYS_USER, nick_name, user_name, updateBy)",
        };
    }


    /**
     * 编辑成型排程
     */
    @Log(title = "ui.data.column.outDn.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation(value = "编辑", hidden = true)
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody CxScheduleResult cxScheduleResult) {
        AjaxResult ajaxResult = null;
        if (cxScheduleResult.getId() != null) {
            Long releasingOrTimeoutByIds = cxScheduleResultService.isReleasingOrTimeoutByIds(new Long[]{cxScheduleResult.getId()});
            if (releasingOrTimeoutByIds > 0) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
            }
            if ("tl".equals(cxScheduleResult.getLocal())) {
                //调量，需要更新9个半部件的计划量
                ajaxResult = cxScheduleResultService.changeQty(cxScheduleResult);
            } else if ("zjt".equals(cxScheduleResult.getLocal())) {
                //转机台
                CxTransferDeskDTO dto = new CxTransferDeskDTO();
                dto.setCxMachineCode(cxScheduleResult.getCxMachineCode());
                dto.setFactoryCode(cxScheduleResult.getFactoryCode());
                dto.setId(cxScheduleResult.getId());
                ajaxResult = this.changeMachine(dto);
            } else if ("xg".equals(cxScheduleResult.getLocal())) {
                //修改
                ajaxResult = cxScheduleResultService.edit(cxScheduleResult);
            }
        } else {
            Long releasingOrTimeoutByDate = cxScheduleResultService.isReleasingOrTimeoutByDate(cxScheduleResult.getScheduleDate());
            if (releasingOrTimeoutByDate > 0) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
            }
            //成型插单
            double class1PlanQty = cxScheduleResult.getClass1PlanQty() == null ? 0d : cxScheduleResult.getClass1PlanQty();
            double class2PlanQty = cxScheduleResult.getClass2PlanQty() == null ? 0d : cxScheduleResult.getClass2PlanQty();
            double class3PlanQty = cxScheduleResult.getClass3PlanQty() == null ? 0d : cxScheduleResult.getClass3PlanQty();
            double class4PlanQty = cxScheduleResult.getClass4PlanQty() == null ? 0d : cxScheduleResult.getClass4PlanQty();
            double class5PlanQty = cxScheduleResult.getClass5PlanQty() == null ? 0d : cxScheduleResult.getClass5PlanQty();
            double class6PlanQty = cxScheduleResult.getClass6PlanQty() == null ? 0d : cxScheduleResult.getClass6PlanQty();
            // 若插单量为0报错
            if ((class1PlanQty + class2PlanQty + class3PlanQty + class4PlanQty + class5PlanQty + class6PlanQty) == 0d) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.qty.zero"));
            }
            cxScheduleResult.setDataSource("1");
            ajaxResult = cxScheduleResultService.add(cxScheduleResult);
        }
        return ajaxResult;
    }


    /**
     * 成型插单校验
     */
    @PostMapping("/validateAdd")
    public AjaxResult validateAdd(@RequestBody CxScheduleResult cxScheduleResult) {
        //此处调用插单校验接口，需覆盖 AjaxResult
        //ValidateResult validateResult = cxScheduleResultService.insertPreCheck(cxScheduleResult);
        //String msg = validateResult.getMsg();
//        if (validateResult.isSuccess()) {
//            return AjaxResult.success();
//        } else {
//            return AjaxResult.error(msg);
//        }
        return AjaxResult.success();
    }

    /**
     * 获取BomData
     */
    @ApiOperation("获取BomData")
    @PostMapping("/getBomData")
    public AjaxResult getBomData(@RequestBody CxScheduleResult cxScheduleResult) {
        return AjaxResult.success(cxScheduleResultService.getBomData(cxScheduleResult));
    }


    /**
     * 调量校验
     */
    @PostMapping("/validateChangeQty")
    public AjaxResult validateChangeQty(@RequestBody CxScheduleResult entity) {
        ValidateResult validateResult = cxScheduleResultService.changePlanQtyPreCheck(entity);
        String msg = validateResult.getMsg();
        if (validateResult.isSuccess()) {
            return AjaxResult.success(msg);
        } else {
            return AjaxResult.error(msg);
        }
    }

    /**
     * 唯一性校验
     */
    @PostMapping("/checkScheduleResultUnique")
    public List<CxScheduleResult> checkScheduleResultUnique(@RequestBody CxScheduleResult cxScheduleResult) {
        List<CxScheduleResult> list = cxScheduleResultService.checkScheduleResultUnique(cxScheduleResult);
        return list;
    }


    /**
     * 删除
     */
    @Log(title = "ui.data.column.outDn.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /**
     * 成型排程导入
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.port.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData/{updateSupport}")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导入数据
     */
    @Log(title = "ui.data.column.port.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入线下数据")
    @PostMapping("/importData2")
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("importLogId") Long importLogId, @RequestParam("scheduleDate") String scheduleDate) throws Exception {

        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(), importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(), importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        ExcelUtil<CxScheduleResult> util = new ExcelUtil<>(this.getTClass());
        InputStream is = new ByteArrayInputStream(importContext.getFileBytes());
        List<CxScheduleResult> list = util.importExcel(is);
        AjaxResult ajaxResult = cxScheduleResultService.importData2(list, importLog.getId(), scheduleDate);
        Date endTime = DateUtils.getNowDate();
        importLog.setRowCount(list.size());
        importLog.setBeginTime(beginTime);
        importLog.setEndTime(endTime);
        importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        ImportExcelUtils.updateImportLogAndFormatMsg(importLog, ajaxResult, this.iImportLogService);
        ImportExcelUtils.saveImportErrorLogs(ajaxResult, this.iImportErrorLogService);
        return ajaxResult;
    }

    /**
     * 导入数据
     */
    @Log(title = "ui.data.column.port.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入线下数据")
    @PostMapping("/importData3")
    public AjaxResult importData3(@RequestBody ImportContext importContext, @RequestParam("importLogId") Long importLogId, @RequestParam("scheduleDate") String scheduleDate) throws Exception {

        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(), importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(), importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        ExcelUtil<CxOnlineImport> util = new ExcelUtil<>(CxOnlineImport.class);
        InputStream is = new ByteArrayInputStream(importContext.getFileBytes());
        List<CxOnlineImport> list = util.importExcel(is);
        AjaxResult ajaxResult = cxScheduleResultService.importData3(list, importLog.getId(), scheduleDate);
        Date endTime = DateUtils.getNowDate();
        importLog.setRowCount(list.size());
        importLog.setBeginTime(beginTime);
        importLog.setEndTime(endTime);
        importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        ImportExcelUtils.updateImportLogAndFormatMsg(importLog, ajaxResult, this.iImportLogService);
        ImportExcelUtils.saveImportErrorLogs(ajaxResult, this.iImportErrorLogService);
        return ajaxResult;
    }

    /**
     * 导出列表
     */
    @Log(title = "成型排程导出", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody CxScheduleResult queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }


    @Log(title = "导出数据收汇报表", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据收汇报表")
    @PostMapping("/exportData2/{fileName}")
    public byte[] exportData2(@RequestBody CxScheduleResult obj, @PathVariable("fileName")String fileName, HttpServletResponse response) throws IOException {
        Date beginTime = DateUtils.getNowDate();
        List<CxOnlineImport> list = cxScheduleResultService.genXcScheduleResult(obj);
        if (list == null || list.size() == 0){
            list = new ArrayList<>();
        }

        //2.将导出数据转换成Excel流
        ExcelUtil<CxOnlineImport> util = new ExcelUtil(CxOnlineImport.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        byte[] resultBytes = ExcelReadUtils.writeExcel(workbook);
        Date endTime = DateUtils.getNowDate();

        //3.组装导出日志
        ExportLog exportLog = new ExportLog();
        exportLog.setProcedureCode("0");
        exportLog.setExportParams(obj.toString());
        String uri = ServletUtils.getRequest().getRequestURI();
        exportLog.setFunctionCode(uri.split("/")[1]);
        exportLog.setFunctionName(fileName);
        exportLog.setFileName(fileName + ExcelUtil.XLSX_FILE);
        //exportLog.setFileUrl(pathFileName);
        exportLog.setRowCount(list.size());
        exportLog.setBeginTime(beginTime);
        exportLog.setEndTime(endTime);
        exportLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        iExportLogService.add(exportLog);
        return resultBytes;
    }


    @Override
    public List<CxScheduleResult> listExportData(CxScheduleResult port) {
        startPage("create_time desc");
        QueryWrapper<CxScheduleResult> queryWrapper = new QueryWrapper<>();
        this.builderCondition(queryWrapper, port);
        return cxScheduleResultService.selectListExportData(queryWrapper);
    }

    @ApiOperation(value = "模板下载", notes = "导入模板下载")
    @GetMapping("/downloadTemplate")
    @ResponseBody
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.port.modelName");
        ExcelUtil<CxScheduleResult> util = new ExcelUtil<>(CxScheduleResult.class);
        util.exportExcel(response, null, fileName, fileName);
    }


    /**
     * 转机台校验校验
     */
    @PostMapping("/validateChangeMachine")
    public AjaxResult validateChangeMachine(@RequestBody CxScheduleResult cxScheduleResult) {
        long releasingOrTimeoutByIds = cxScheduleResultService.isReleasingOrTimeoutByIds(new Long[]{cxScheduleResult.getId()});
        if (releasingOrTimeoutByIds > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        CxTransferDeskDTO dto = new CxTransferDeskDTO();
        dto.setCxMachineCode(cxScheduleResult.getCxMachineCode());
        dto.setFactoryCode(cxScheduleResult.getFactoryCode());
        dto.setId(cxScheduleResult.getId());
        ValidateResult validateResult = cxScheduleResultCheckHandle.changeMachinePreCheck(dto);
        if (!validateResult.isSuccess()) {
            return AjaxResult.error(validateResult.getMsg());
        }
        return AjaxResult.success();
    }


    /**
     * 转机台
     *
     * @param dto
     * @return
     */
    @PostMapping("/changeMachine")
    @ApiOperation("转机台")
    public AjaxResult changeMachine(@RequestBody CxTransferDeskDTO dto) {
        ValidateResult validateResult = cxScheduleResultCheckHandle.changeMachinePreCheck(dto);
        if (!validateResult.isSuccess()) {
            return AjaxResult.error(validateResult.getMsg());
        }
        //调用转机台业务
        cxScheduleResultService.changeMachine(dto);
        return AjaxResult.success();
    }

    /**
     * 查询条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<CxScheduleResult> queryWrapper, CxScheduleResult queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleDate")), "SCHEDULE_DATE", queryVO.getFieldValueByFieldName("scheduleDate"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("embryoCode")), "EMBRYO_CODE", queryVO.getFieldValueByFieldName("embryoCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("sapCode")), "SAP_CODE", queryVO.getFieldValueByFieldName("sapCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionStatus")), "PRODUCTION_STATUS", queryVO.getFieldValueByFieldName("productionStatus"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isRelease")), "IS_RELEASE", queryVO.getFieldValueByFieldName("isRelease"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cxMachineCode")), "CX_MACHINE_CODE", queryVO.getFieldValueByFieldName("cxMachineCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lhMachineCode")), "LH_MACHINE_CODE", queryVO.getFieldValueByFieldName("lhMachineCode"));
    }

    @Override
    protected String getOrderBy() {
        return "CX_MACHINE_CODE asc";
    }

    @Override
    protected IDocService getDocService() {
        return cxScheduleResultService;
    }

    @Override
    protected String getTypeCode() {
        return "CX2025212";
    }


    /**
     * 排程发布校验
     */
    @PostMapping("/publishValidate")
    public AjaxResult publishValidate(@RequestBody CxScheduleResult cxScheduleResult) {
        //Joran 2022-03-16添加发布选中记录施工校验
        Long[] ids = cxScheduleResult.getIds();
        String msg = cxScheduleResultService.validateConstructionByIds(ids);
        if (StringUtils.isNotEmpty(msg)) {
            return AjaxResult.error(msg);
        }
        Long releasingOrTimeoutByIds = cxScheduleResultService.isReleasingOrTimeoutByIds(cxScheduleResult.getIds());
        if (releasingOrTimeoutByIds > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        //查询排程发布list,过滤出未发布及发布失败的记录
        QueryWrapper<CxScheduleResult> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, cxScheduleResult);
        List<CxScheduleResult> list = cxScheduleResultEntityMapper.selectList(wrapper).stream()
                .filter(item -> ApsConstant.NO_RELEASE.equals(item.getIsRelease()) || ApsConstant.FAILURE_RELEASE.equals(item.getIsRelease()) || ApsConstant.WAIT_RELEASING.equals(item.getIsRelease())).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.errorPublish"));
        }

        //Joran 2021-12-04 发布前校验如果存在施工版本为空的不允许发布
        msg = cxScheduleResultService.checkBomDataVersion(list);
        if (StringUtils.isNotEmpty(msg)) {
            return AjaxResult.error(msg);
        }
        List<CxScheduleResult> filterPlanList = list.stream().filter(item -> Optional.of(item.getClass2PlanQty()).orElse(0) > 0
                && Optional.of(item.getClass3PlanQty()).orElse(0) > 0).collect(Collectors.toList());
        if (!filterPlanList.isEmpty()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.publish.qtyNull"));
        }
        //校验硫化机台
        List<CxScheduleResult> collect = list.stream().filter(item -> StringUtil.isBlank(item.getLhMachineCode())).collect(Collectors.toList());
        if (collect.size() > 0) {
            return AjaxResult.success("0");
        }
        return AjaxResult.success();
    }


    /**
     * 删除成型排程结果
     */
    @Log(title = "ui.cx.cxScheduleResult.export.fileName", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        if (cxScheduleResultService.isPublishByIds(ids) != ids.length) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isPublishById"));
        }
        List<CxScheduleResult> removeList= new ArrayList<>();
        String validateMsg=cxScheduleResultService.removeResultCheck(ids,removeList);
        if(StringUtils.isNotEmpty(validateMsg)){
            return AjaxResult.error(validateMsg);
        }
        return toAjax(cxScheduleResultService.removeCxSecheduleResultByList(ids,removeList));
    }


    /**
     * 更改发布状态
     * @param entity 排程日期
     * @return 结果
     */
    @Log(title = "ui.data.column.tmScheduleResult.modalName")
    @PostMapping("/changeReleaseStatus")
    public AjaxResult changeReleaseStatus(@RequestBody CxScheduleResult entity){
        cxScheduleResultService.changeReleaseStatus(entity);
        return AjaxResult.success();
    }


    /**
     * 排程发布
     */
    @Log(title = "ui.cx.cxScheduleResult.export.fileName", businessType = BusinessType.PUBLISH)
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody CxScheduleResult cxScheduleResult) {
        // 发布前需要先获得同步锁，防止在集群环境下出现一个前端命令发送两次mes请求，modify by hak 20220708
        if (syncDataLogsService.checkPublishLocking("cx:publish:lock", cxScheduleResult.getIds())) {
            return AjaxResult.success(); // 如果已经被锁定了，则直接返回
        }

        //获取数据版本号
        String dataVersion = syncDataHandle.getDataVersion(ApsConstant.CX_DEPLOY_SYNC_KEY);
        // 厂别、分公司编号
        String factoryCode = factoryService.getFactoryCode();
        String companyCode = factoryService.getCompanyCode();

        //查询排程发布list
        cxScheduleResult.setIsRelease(ApsConstant.NO_RELEASE);

        //Joran 2022-03-08 没有施工版本的不允许发布
        cxScheduleResult.setHasVersion(0);
        cxScheduleResult.setToProduct(CxEngineConstants.TO_PRODUCT_YES);

        QueryWrapper<CxScheduleResult> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, cxScheduleResult);
        wrapper.in(PubUtil.isNotEmpty(cxScheduleResult.getIds()), "id", Arrays.asList(cxScheduleResult.getIds()));
        List<CxScheduleResult> list = cxScheduleResultEntityMapper.selectList(wrapper).stream()
                .filter(item -> ApsConstant.NO_RELEASE.equals(item.getIsRelease()) || ApsConstant.FAILURE_RELEASE.equals(item.getIsRelease()) || ApsConstant.WAIT_RELEASING.equals(item.getIsRelease())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.hasNotVersionOrNotToProduct"));
        }

        Date scheduleDate = cxScheduleResult.getScheduleDate();
        AjaxResult ajaxResult = null;
        //排程发布
        long[] arr = list.stream().mapToLong(item -> item.getId()).toArray();
        try {
            ajaxResult = cxScheduleResultService.publish(arr, scheduleDate, dataVersion, factoryCode, companyCode);
            SyncParamsVO syncParamsVO = new SyncParamsVO();
            syncParamsVO.setSyncKey(ApsConstant.CX_DEPLOY_SYNC_KEY);
            syncParamsVO.setDataVersion(dataVersion);
            // 请求参数
            JSONObject params = new JSONObject();
            params.put("scheduleDate", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, scheduleDate));
            params.put("rowCount", arr.length);
            syncParamsVO.setParams(params);
            syncParamsVO.setFactoryCode(factoryCode);
            syncParamsVO.setCompanyCode(companyCode);
            syncDataHandle.syncNotice(syncParamsVO);

            // 取回mes的反馈结果
            SyncDataLogs logs = syncDataLogsService.getSyncDataResult(dataVersion);
            String status = logs.getStatus();
            // 更新状态
            cxScheduleResultService.updateRelaseStatus(arr, status);
            if (ApsConstant.IS_RELEASE.equals(status)) {
                // 成功
                ajaxResult = AjaxResult.success();
            } else {
                // 失败，需要返回异常信息
                ajaxResult = AjaxResult.error(logs.getMsg());
            }
        } catch (Exception e) {
            //异常时进行堆栈内容打印
            e.printStackTrace();
            ajaxResult = AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.failedPublish"));
        }
        return AjaxResult.success();
    }

    /**
     * 将成型排程解析成月度剩余量，胎胚库存，月度完成量
     */
    @PostMapping("/parseCxScheduleResult")
    public AjaxResult parseCxScheduleResult(@RequestBody CxScheduleResult cxScheduleResult) {
        return cxScheduleResultService.parseCxScheduleResult(cxScheduleResult);
    }


    /**
     * 导出现场计划
     */
    @ApiOperation("导出现场计划")
    @GetMapping({"/export3"})
    @ResponseBody
    public void export3(HttpServletResponse response, CxScheduleResult entity) throws IOException {
        String fileName ="现场计划"+ DateUtils.parseDateToStr("yyyyMMddHHmmss",new Date())+".xlsx";
        byte[] excelBytes = this.exportData2(entity, fileName,response);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 查询成型机台甘特图
     *
     * @param queryVO 查询参数
     * @return 结果
     */
    @ApiOperation("查询成型机台甘特图")
    @PostMapping("/selectMachineGantt")
    public AjaxResult selectMachineGantt(@RequestBody CxGanttVo queryVO) {
        return cxScheduleResultService.selectMachineGantt(queryVO);
    }


    /**
     * 成型调整硫化
     */
    @PostMapping("/updateLhScheduleResult")
    public AjaxResult updateCxScheduleResult(@RequestBody CxScheduleResult cxScheduleResult) {
        return cxScheduleResultService.updateLhScheduleResult(cxScheduleResult);
    }

    /**
     * 校验施工切换版本是否还有剩余旧版本半部件的库存
     * @param embryoCode 施工代号
     * @param oldVersion 旧版本
     * @param newVersion 新版本
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @ApiOperation("校验施工切换版本是否还有剩余旧版本半部件的库存")
    @PostMapping("/checkConsOldVersionStock")
    public AjaxResult checkConsOldVersionStock(String embryoCode, String oldVersion, String newVersion, Date scheduleDate) {
        return cxScheduleResultService.checkConsOldVersionStock(embryoCode, oldVersion, newVersion, scheduleDate);
    }
}

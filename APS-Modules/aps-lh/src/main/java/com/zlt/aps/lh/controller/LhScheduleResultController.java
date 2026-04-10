package com.zlt.aps.lh.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.util.StringUtil;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.lh.api.domain.dto.LhOrderInsertDTO;
import com.zlt.aps.lh.api.domain.dto.LhOrderInsertParamDTO;
import com.zlt.aps.lh.api.domain.dto.LhScheduleRequestDTO;
import com.zlt.aps.lh.api.domain.dto.LhScheduleResponseDTO;
import com.zlt.aps.lh.api.domain.dto.LhScheduleResultUpdateDTO;
import com.zlt.aps.lh.api.domain.dto.LhTransferDeskDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.service.ILhScheduleService;
import com.zlt.aps.mp.api.domain.entity.LhMachineInfo;
import com.zlt.aps.mp.api.domain.entity.LhScheduleResultIssue;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 硫化排程控制器
 *
 * @author APS
 */
@Api(tags = "硫化排程接口")
@Slf4j
@RestController
@RequestMapping("/lhScheduleResult")
public class LhScheduleResultController extends AbstractDocBizController<LhScheduleResult> {

    @Resource
    private ILhScheduleService lhScheduleService;

    /**
     * 执行自动排程
     *
     * @param request 排程请求参数
     * @return 排程响应结果
     */
    @PostMapping("/execute")
    @ApiOperation("执行自动排程")
    public LhScheduleResponseDTO executeSchedule(@RequestBody LhScheduleRequestDTO request) {
        log.info("收到排程请求, 工厂: {}, 日期: {}", request.getFactoryCode(), request.getScheduleDate());
//        return lhScheduleService.executeSchedule(request);
        return LhScheduleResponseDTO.success("100001", "排程完成");
    }

    /**
     * 发布排程结果到MES
     *
     * @param batchNo 批次号
     * @return 发布响应结果
     */
    @PostMapping("/publishSchedule/{batchNo}")
    @ApiOperation("发布排程结果到MES")
    public LhScheduleResponseDTO publishSchedule(@PathVariable("batchNo") String batchNo) {
        log.info("收到发布请求, 批次号: {}", batchNo);
        return lhScheduleService.publishSchedule(batchNo);
    }


    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody LhScheduleResult entity) {
        TableDataInfo tableDataInfo = super.list(entity);
//        List<LhScheduleResult> list = (List<LhScheduleResult>) tableDataInfo.getRows();
//        if (CollectionUtils.isNotEmpty(list)) {
//            List<Long> idList = list.stream().map(BaseEntity::getId).collect(Collectors.toList());
//            LhDispatcherLog queryVO = new LhDispatcherLog();
//            queryVO.setScheduleDate(entity.getScheduleDate());
//            List<LhDispatcherLogVo> lhDispatcherLogVos = lhDispatcherLogServiceImpl.selectIsChangeList(queryVO, idList);
//            Map<Long, LhDispatcherLogVo> logVoMap = new HashMap<>(16);
//            if (CollectionUtils.isNotEmpty(lhDispatcherLogVos)) {
//                logVoMap = lhDispatcherLogVos.stream().collect(Collectors.toMap(LhDispatcherLogVo::getScheduleId, Function.identity(), (s1, s2) -> s1));
//            }
//            for (LhScheduleResult lhScheduleResult : list) {
//                Long id = lhScheduleResult.getId();
//                if (logVoMap.containsKey(id)) {
//                    LhDispatcherLogVo lhDispatcherLogVo = logVoMap.get(id);
//                    BeanUtils.copyBeanProp(lhScheduleResult, lhDispatcherLogVo);
//                }
//                Integer class1PlanQty = ObjectUtils.defaultIfNull(lhScheduleResult.getClass1PlanQty(), 0);
//                Integer class2PlanQty = ObjectUtils.defaultIfNull(lhScheduleResult.getClass2PlanQty(), 0);
//                Integer class3PlanQty = ObjectUtils.defaultIfNull(lhScheduleResult.getClass3PlanQty(), 0);
//                Integer class4PlanQty = ObjectUtils.defaultIfNull(lhScheduleResult.getClass4PlanQty(), 0);
//                Integer class5PlanQty = ObjectUtils.defaultIfNull(lhScheduleResult.getClass5PlanQty(), 0);
//                Integer class6PlanQty = ObjectUtils.defaultIfNull(lhScheduleResult.getClass6PlanQty(), 0);
//                double totalPlanQty = class1PlanQty + class2PlanQty + class3PlanQty + class4PlanQty + class5PlanQty + class6PlanQty;
//                Integer class1FinishQty = ObjectUtils.defaultIfNull(lhScheduleResult.getClass1FinishQty(), 0);
//                Integer class2FinishQty = ObjectUtils.defaultIfNull(lhScheduleResult.getClass2FinishQty(), 0);
//                Integer class3FinishQty = ObjectUtils.defaultIfNull(lhScheduleResult.getClass3FinishQty(), 0);
//                Integer class4FinishQty = ObjectUtils.defaultIfNull(lhScheduleResult.getClass4FinishQty(), 0);
//                Integer class5FinishQty = ObjectUtils.defaultIfNull(lhScheduleResult.getClass5FinishQty(), 0);
//                Integer class6FinishQty = ObjectUtils.defaultIfNull(lhScheduleResult.getClass6FinishQty(), 0);
//                double totalFinishQty = class1FinishQty + class2FinishQty + class3FinishQty + class4FinishQty + class5FinishQty + class6FinishQty;
//            }
//        }
        return tableDataInfo;
    }

    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public LhScheduleResult getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 编辑
     */
    @Log(title = "ui.data.column.outDn.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation(value = "编辑", hidden = true)
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody LhScheduleResult entity) {
        try {
            return super.save(entity);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return AjaxResult.error(I18nUtil.getMessage("ui.data.save.error.msg"));
        }
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
     * 导入数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.port.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData/{updateSupport}")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    @ApiOperation(value = "模板下载" , notes = "导入模板下载")
    @GetMapping("/downloadTemplate")
    @ResponseBody
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.lh.scheduleResult.modelName");
        ExcelUtil<LhScheduleResult> util = new ExcelUtil<>(LhScheduleResult.class);
        util.exportExcel(response, null, fileName, fileName);
    }


    /**
     * 导出列表
     */
    @Log(title = "硫化排程导出", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody LhScheduleResult queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Log(title = "ui.data.column.lhParams.modelName")
    @ApiOperation("插单查询可用机台列表")
    @PostMapping("/getScheduleMachineInfo")
    public List<LhMachineInfo> getScheduleMachineInfo(@RequestBody LhOrderInsertParamDTO insertParamDTO){
//        return lhScheduleResultService.getScheduleMachineInfo(insertParamDTO);
        return Collections.emptyList();
    }

    @Log(title = "ui.data.column.lhParams.modelName")
    @ApiOperation("插单")
    @PostMapping("/insertOrder")
    public AjaxResult insertOrder(@RequestBody LhOrderInsertDTO insertDTO){
//        ValidateResult validateResult = lhScheduleResultCheckHandle.insertLhScheduleResultCheck(insertDTO);
//        if (!validateResult.isSuccess()) {
//            return AjaxResult.error(validateResult.getMsg());
//        }
//        lhScheduleResultService.insertOrder(insertDTO);
        return AjaxResult.success();
    }


    /**
     * 转机台校验
     */
    @Log(title = "ui.data.column.lhParams.modelName")
    @PostMapping("/validateChangeMachine")
    @ApiOperation("硫化排程结果转机台校验")
    public AjaxResult validateChangeMachine(@RequestBody LhTransferDeskDTO dto) {
//        ValidateResult validateResult = lhScheduleResultCheckHandle.changeMachinePreCheck(dto);
//        if (!validateResult.isSuccess()) {
//            return AjaxResult.error(validateResult.getMsg());
//        }

        return AjaxResult.success("校验通过");
    }

    /**
     * 转机台
     * @param dto
     * @return
     */
    @PostMapping("/changeMachine")
    @ApiOperation("转机台")
    public AjaxResult changeMachine(@RequestBody LhTransferDeskDTO dto) {
//        ValidateResult validateResult = lhScheduleResultCheckHandle.changeMachinePreCheck(dto);
//        if (!validateResult.isSuccess()) {
//            return AjaxResult.error(validateResult.getMsg());
//        }
//        //调用转机台业务
//        lhScheduleResultService.changeMachine(dto);
        return AjaxResult.success();
    }

    @Log(title = "ui.data.column.lhParams.modelName")
    @PostMapping("/adjustQuantity")
    @ApiOperation("调量")
    public AjaxResult adjustQuantity(@RequestBody LhScheduleResultUpdateDTO dto) {
//        ValidateResult validateResult = lhScheduleResultCheckHandle.updateLhScheduleResultCheck(dto);
//        if (!validateResult.isSuccess()) {
//            return AjaxResult.error(validateResult.getMsg());
//        }
//        //调用调量业务
//        lhScheduleAdjustService.preAdjustment(dto);
        return AjaxResult.success();
    }

    /**
     * 文字示方调整
     * @param dto
     * @return
     */
    @PostMapping("/adjustTextNo")
    @ApiOperation("文字示方调整")
    public AjaxResult adjustTextNo(@RequestBody LhTransferDeskDTO dto) {
        return AjaxResult.success();
    }


    /**
     * 发布当天未发布的排程结果
     */
    @Log(title = "ui.data.column.lh.scheduleResult.modelName", businessType = BusinessType.PUBLISH)
    @ApiOperation("发布排程")
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody LhScheduleResult dto) {
//        // 发布前需要先获得同步锁，防止在集群环境下出现一个前端命令发送两次mes请求，modify by hak 20220708
//        if (syncDataLogsService.checkPublishLocking("lh:publish:lock", dto.getIds())) {
//            // 如果已经被锁定了，则直接返回
//            return AjaxResult.success();
//        }
//        int releasingOrTimeoutByIds = lhScheduleResultService.isReleasingOrTimeoutByIds(Arrays.stream(dto.getIds()).mapToLong(Long::longValue).toArray());
//        if (releasingOrTimeoutByIds > 0) {
//            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
//        }
////        //获取数据版本号 由于报错先注释，后续由hak调整
////        String dataVersion = syncDataLogsService.getDataVersion(ApsConstant.LH_DEPLOY_SYNC_KEY);
//        String dataVersion = "";
//        // 厂别、分公司编号
//        String factoryCode = factoryService.getFactoryCode();
//        String companyCode = factoryService.getCompanyCode();
//
//        LhScheduleResult scheduleResult = new LhScheduleResult();
//        org.springframework.beans.BeanUtils.copyProperties(dto, scheduleResult);
//        QueryWrapper<LhScheduleResult> wrapper = new QueryWrapper<>();
//        this.builderCondition(wrapper, scheduleResult);
//        wrapper.in(PubUtil.isNotEmpty(scheduleResult.getIds()), "id", Arrays.asList(scheduleResult.getIds()));
//        // 过滤未发布及发布失败的数据
//        List<LhScheduleResult> list = lhScheduleResultEntityMapper.selectList(wrapper).stream()
//                .filter(item -> ApsConstant.NO_RELEASE.equals(item.getIsRelease()) || ApsConstant.FAILURE_RELEASE.equals(item.getIsRelease()) || ApsConstant.WAIT_RELEASING.equals(item.getIsRelease())).collect(Collectors.toList());
//        if (CollectionUtils.isEmpty(list)) {
//            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.errorPublish"));
//        }
//        // 获取机台id为空和多机台的记录
//        List<LhScheduleResult> collect = list.stream().filter(item -> StringUtil.isEmpty(item.getLhMachineCode()) || item.getLhMachineCode().contains(",")).collect(Collectors.toList());
//        if (!collect.isEmpty()) {
//            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.hasMultipleIds"));
//        }
//        //排程发布
//        long[] arr = list.stream().mapToLong(BaseEntity::getId).toArray();
//
//        Date scheduleDate = scheduleResult.getScheduleDate();
//        AjaxResult ajaxResult = null;
//        try {
//            ajaxResult = lhScheduleResultService.publish(arr, scheduleDate, dataVersion, factoryCode, companyCode);
//            // 后续需要替换成新的itf同步接口
////            // 请求参数
////            JSONObject params = new JSONObject();
////            params.put("scheduleDate", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, scheduleDate));
////            params.put("rowCount", arr.length);
////            SyncParamsVO syncParamsVO = new SyncParamsVO();
////            syncParamsVO.setSyncKey(ApsConstant.LH_DEPLOY_SYNC_KEY);
////            syncParamsVO.setDataVersion(dataVersion);
////            syncParamsVO.setParams(params);
////            syncParamsVO.setFactoryCode(factoryCode);
////            syncParamsVO.setCompanyCode(companyCode);
////            syncDataHandle.syncNotice(syncParamsVO);
//
//            // 取回mes的反馈结果
//            SyncDataLogs logs = syncDataLogsService.getSyncDataResult(dataVersion);
//            String status = logs.getStatus();
//            // 更新状态
//            lhScheduleResultService.updateReleaseStatus(dataVersion, arr, status);
//            if (ApsConstant.IS_RELEASE.equals(status)) {
//                // 成功
//                ajaxResult = AjaxResult.success();
//            } else {
//                // 失败，需要返回异常信息
//                ajaxResult = AjaxResult.error(logs.getMsg());
//            }
//        } catch (Exception e) {
//            //异常时进行堆栈内容打印
//            e.printStackTrace();
//            ajaxResult = AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.failedPublish"));
//        }
//        return ajaxResult;
          return AjaxResult.success();
    }



    /**
     * 硫化排程结果下发到MES
     * 背景逻辑：
     * 1. 夜班是凌晨的叫夜班，所以当天的夜班是已经在执行确定了
     * 2. 到早上的时候触发排程，排程逻辑是夜早中
     * 3. 8班对应关系（使用aps-cx-lh-api实体）：
     *    - 1-2班：当天的早、中班（夜班已生产）
     *    - 3-5班：第二天的夜、早、中班
     *    - 6-8班：第三天的夜、早、中班
     * 4. 中间表映射：1班=夜班，2班=早班，3班=中班
     * 5. 当天、隔天数据更新（存在则更新，不存在则插入）
     * 6. 第三天数据下发（插入）
     *
     * @return 下发结果
     */
    @ApiOperation(value = "硫化排程结果下发到MES", notes = "将硫化排程结果下发到MES中间表，8班数据对应3天班次")
    @Log(title = "硫化排程结果下发", businessType = BusinessType.PUBLISH)
    @PostMapping("/issueToMes")
    public AjaxResult issueLhScheduleResultToMes() {

//        // 获取今天、明天、后天的日期
//        LocalDate today = LocalDate.now();
//        LocalDate tomorrow = today.plusDays(1);
//        LocalDate dayAfterTomorrow = today.plusDays(2);
//
//        // 只查询当天的排程结果数据（包含8班数据）- 使用aps-cx-lh-api实体
//        List<com.zlt.aps.cx.entity.schedule.LhScheduleResult> scheduleResultList =
//                lhScheduleResultService.getCxLhScheduleResultList(java.sql.Date.valueOf(today));
//
//        if (scheduleResultList.isEmpty()) {
//            return AjaxResult.error("没有需要下发的硫化排程结果数据");
//        }
//
//        // 转换为3天的下发数据
//        List<LhScheduleResultIssue> day1IssueList = new ArrayList<>();    // 当天（更新）
//        List<LhScheduleResultIssue> day2IssueList = new ArrayList<>();    // 隔天（更新）
//        List<LhScheduleResultIssue> day3IssueList = new ArrayList<>();    // 后天（插入）
//
//        for (com.zlt.aps.cx.entity.schedule.LhScheduleResult source : scheduleResultList) {
//            // 第1天（当天）- 更新2班数据（早中班）
//            LhScheduleResultIssue day1Issue = convertToDay1IssueEntity(source, today);
//            if (day1Issue != null) {
//                day1IssueList.add(day1Issue);
//            }
//
//            // 第2天（隔天）- 更新3班数据（夜早中班）
//            LhScheduleResultIssue day2Issue = convertToDay2IssueEntity(source, tomorrow);
//            if (day2Issue != null) {
//                day2IssueList.add(day2Issue);
//            }
//
//            // 第3天（后天）- 下发3班数据（夜早中班）
//            LhScheduleResultIssue day3Issue = convertToDay3IssueEntity(source, dayAfterTomorrow);
//            if (day3Issue != null) {
//                day3IssueList.add(day3Issue);
//            }
//        }
//
//        if (day1IssueList.isEmpty() && day2IssueList.isEmpty() && day3IssueList.isEmpty()) {
//            return AjaxResult.error("没有需要下发的硫化排程结果数据");
//        }
//
//        // 合并所有数据并调用下发接口
//        List<LhScheduleResultIssue> allIssueList = new ArrayList<>();
//        allIssueList.addAll(day1IssueList);
//        allIssueList.addAll(day2IssueList);
//        allIssueList.addAll(day3IssueList);
//
//        // 通过Feign客户端调用itf模块的下发接口
//        return mesItfService.issueLhScheduleResult(allIssueList);
        return AjaxResult.success();
    }


    /**
     * 转换为第1天（当天）的下发实体
     * 8班数据：1班(早)、2班(中) -> 中间表：1班(夜)=空, 2班(早)=1班, 3班(中)=2班
     * 业务规则：只更新2班数据（早中班），即中间表的2班(早)和3班(中)
     * 使用aps-cx-lh-api实体（有8班数据）
     */
    private LhScheduleResultIssue convertToDay1IssueEntity(com.zlt.aps.cx.entity.schedule.LhScheduleResult source, LocalDate scheduleDate) {
        if (source == null) {
            return null;
        }

        LhScheduleResultIssue target = new LhScheduleResultIssue();

        // 基础字段映射
        target.setId(source.getId());
        target.setLhBatchNo(source.getBatchNo());
        target.setOrderNo(source.getOrderNo());
        target.setScheduleDate(scheduleDate.atStartOfDay());

        // 机台信息
        target.setLhMachineCode(source.getLhMachineCode());
        target.setLhMachineName(source.getLhMachineName());
        target.setLeftRightMold(source.getLeftRightMould());

        // 物料信息（aps-cx-lh-api使用materialCode而不是productCode）
        target.setMaterialCode(source.getMaterialCode());
        target.setMesMaterialCode(null); // MES物料编码需要另外查询
        target.setSpecCode(source.getSpecCode());
        target.setSpecDesc(source.getSpecDesc());
        target.setDailyPlanQty(source.getDailyPlanQty());


        // 中间表2班 = 早班（对应APS 1班）
        target.setClass2PlanQtySeq(2);
        target.setClass2AnalysisInput(null);
        target.setClass2Analysis(source.getClass1Analysis());
        target.setClass2PlanQty(source.getClass1PlanQty());
        target.setClass2ExampleType(null);
        target.setClass2ExampleNo(null);

        // 中间表3班 = 中班（对应APS 2班）
        target.setClass3PlanQtySeq(3);
        target.setClass3AnalysisInput(null);
        target.setClass3Analysis(source.getClass2Analysis());
        target.setClass3PlanQty(source.getClass2PlanQty());
        target.setClass3ExampleType(null);
        target.setClass3ExampleNo(null);

        // 硫化时长
        target.setLhTime(source.getLhTime());

        // 版本和工厂信息（aps-cx-lh-api没有bomVersion字段，使用dataSource或其他字段）
        target.setDataVersion(null);
        target.setCompanyCode(source.getFactoryCode()); // 使用分厂编码作为分公司编码
        target.setFactoryCode(source.getFactoryCode());

        return target;
    }

    /**
     * 转换为第2天（隔天）的下发实体
     * 8班数据：3班(夜)、4班(早)、5班(中) -> 中间表：1班(夜)=3班, 2班(早)=4班, 3班(中)=5班
     * 业务规则：更新3班数据（夜早中班）
     * 使用aps-cx-lh-api实体（有8班数据）
     */
    private LhScheduleResultIssue convertToDay2IssueEntity(com.zlt.aps.cx.entity.schedule.LhScheduleResult source, LocalDate scheduleDate) {
        if (source == null) {
            return null;
        }

        LhScheduleResultIssue target = new LhScheduleResultIssue();

        // 基础字段映射
        target.setId(source.getId());
        target.setLhBatchNo(source.getBatchNo());
        target.setOrderNo(source.getOrderNo());
        target.setScheduleDate(scheduleDate.atStartOfDay());

        // 机台信息
        target.setLhMachineCode(source.getLhMachineCode());
        target.setLhMachineName(source.getLhMachineName());
        target.setLeftRightMold(source.getLeftRightMould());

        // 物料信息（aps-cx-lh-api使用materialCode）
        target.setMaterialCode(source.getMaterialCode());
        target.setMesMaterialCode(null);
        target.setSpecCode(source.getSpecCode());
        target.setSpecDesc(source.getSpecDesc());
        target.setDailyPlanQty(source.getDailyPlanQty());

        // 中间表1班 = 夜班（对应APS 3班）
        target.setClass1PlanQtySeq(1);
        target.setClass1AnalysisInput(null);
        target.setClass1Analysis(source.getClass3Analysis());
        target.setClass1PlanQty(source.getClass3PlanQty());
        target.setClass1ExampleType(null);
        target.setClass1ExampleNo(null);

        // 中间表2班 = 早班（对应APS 4班）
        target.setClass2PlanQtySeq(2);
        target.setClass2AnalysisInput(null);
        target.setClass2Analysis(source.getClass4Analysis());
        target.setClass2PlanQty(source.getClass4PlanQty());
        target.setClass2ExampleType(null);
        target.setClass2ExampleNo(null);

        // 中间表3班 = 中班（对应APS 5班）
        target.setClass3PlanQtySeq(3);
        target.setClass3AnalysisInput(null);
        target.setClass3Analysis(source.getClass5Analysis());
        target.setClass3PlanQty(source.getClass5PlanQty());
        target.setClass3ExampleType(null);
        target.setClass3ExampleNo(null);

        // 硫化时长
        target.setLhTime(source.getLhTime());

        // 版本和工厂信息（aps-cx-lh-api没有bomVersion字段）
        target.setDataVersion(null);
        target.setCompanyCode(source.getFactoryCode());
        target.setFactoryCode(source.getFactoryCode());

        return target;
    }

    /**
     * 转换为第3天（后天）的下发实体
     * 8班数据：6班(夜)、7班(早)、8班(中) -> 中间表：1班(夜)=6班, 2班(早)=7班, 3班(中)=8班
     * 业务规则：下发3班数据（夜早中班）
     * 使用aps-cx-lh-api实体（有8班数据）
     */
    private LhScheduleResultIssue convertToDay3IssueEntity(com.zlt.aps.cx.entity.schedule.LhScheduleResult source, LocalDate scheduleDate) {
        if (source == null) {
            return null;
        }

        LhScheduleResultIssue target = new LhScheduleResultIssue();

        // 基础字段映射
        target.setId(source.getId());
        target.setLhBatchNo(source.getBatchNo());
        target.setOrderNo(source.getOrderNo());
        target.setScheduleDate(scheduleDate.atStartOfDay());

        // 机台信息
        target.setLhMachineCode(source.getLhMachineCode());
        target.setLhMachineName(source.getLhMachineName());
        target.setLeftRightMold(source.getLeftRightMould());

        // 物料信息（aps-cx-lh-api使用materialCode）
        target.setMaterialCode(source.getMaterialCode());
        target.setMesMaterialCode(null);
        target.setSpecCode(source.getSpecCode());
        target.setSpecDesc(source.getSpecDesc());
        target.setDailyPlanQty(source.getDailyPlanQty());

        // 中间表1班 = 夜班（对应APS 6班）
        target.setClass1PlanQtySeq(1);
        target.setClass1AnalysisInput(null);
        target.setClass1Analysis(source.getClass6Analysis());
        target.setClass1PlanQty(source.getClass6PlanQty());
        target.setClass1ExampleType(null);
        target.setClass1ExampleNo(null);

        // 中间表2班 = 早班（对应APS 7班）
        target.setClass2PlanQtySeq(2);
        target.setClass2AnalysisInput(null);
        target.setClass2Analysis(source.getClass7Analysis());
        target.setClass2PlanQty(source.getClass7PlanQty());
        target.setClass2ExampleType(null);
        target.setClass2ExampleNo(null);

        // 中间表3班 = 中班（对应APS 8班）
        target.setClass3PlanQtySeq(3);
        target.setClass3AnalysisInput(null);
        target.setClass3Analysis(source.getClass8Analysis());
        target.setClass3PlanQty(source.getClass8PlanQty());
        target.setClass3ExampleType(null);
        target.setClass3ExampleNo(null);

        // 硫化时长
        target.setLhTime(source.getLhTime());

        // 版本和工厂信息（aps-cx-lh-api没有bomVersion字段）
        target.setDataVersion(null);
        target.setCompanyCode(source.getFactoryCode());
        target.setFactoryCode(source.getFactoryCode());

        return target;
    }



    /**
     * 查询条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<LhScheduleResult> queryWrapper, LhScheduleResult queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleDate")), "SCHEDULE_DATE", queryVO.getFieldValueByFieldName("scheduleDate"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lhMachineCode")), "LH_MACHINE_CODE", queryVO.getFieldValueByFieldName("lhMachineCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specCode")), "SPEC_CODE", queryVO.getFieldValueByFieldName("specCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specDesc")), "SPEC_DESC", queryVO.getFieldValueByFieldName("specDesc"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("embryoCode")), "EMBRYO_CODE", queryVO.getFieldValueByFieldName("embryoCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isRelease")), "IS_RELEASE", queryVO.getFieldValueByFieldName("isRelease"));
    }

    @Override
    protected IDocService getDocService() {
        return lhScheduleService;
    }

    @Override
    protected String getTypeCode() {
        return "LH2025213";
    }







}

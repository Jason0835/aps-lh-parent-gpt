package com.zlt.aps.lh.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.util.StringUtil;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.common.FactoryService;
import com.zlt.aps.common.SyncDataLogsService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.domain.SyncDataLogs;
import com.zlt.aps.lh.api.domain.bo.ValidateResult;
import com.zlt.aps.lh.api.domain.dto.*;
import com.zlt.aps.lh.api.domain.entity.LhDispatcherLog;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhDispatcherLogVo;
import com.zlt.aps.lh.api.domain.vo.LhGanttVo;
import com.zlt.aps.lh.api.domain.vo.LhMachineInfoVo;
import com.zlt.aps.lh.handle.LhScheduleResultCheckHandle;
import com.zlt.aps.lh.handle.LhSyncDataHandle;
import com.zlt.aps.lh.mapper.LhScheduleResultEntityMapper;
import com.zlt.aps.lh.service.ILhDispatcherLogService;
import com.zlt.aps.lh.service.ILhMachineInfoService;
import com.zlt.aps.lh.service.LhScheduleAdjustService;
import com.zlt.aps.lh.service.LhScheduleResultService;
import com.zlt.aps.maindata.service.IMdmProductConstructionService;
import com.zlt.aps.mp.api.domain.entity.LhMachineInfo;
import com.zlt.aps.mp.api.domain.entity.LhMonthPlanSurplusDetail;
import com.zlt.aps.mp.api.domain.entity.MdmProductConstruction;
import com.zlt.aps.mp.api.domain.vo.LhMonthFinishQtyVo;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.exception.QueryExprException;
import com.zlt.common.utils.ImportExcelUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.queryformulas.QueryFormulaUtil;
import com.zlt.sync.povo.SyncParamsVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author xh
 * @version 1.0
 * @Description
 * @date 2025/2/13
 */
@Slf4j
@Api(tags = "硫化排程")
@RestController
@RequestMapping("/lhScheduleResult")
public class LhScheduleResultController extends AbstractDocBizController<LhScheduleResult> {


    @Autowired
    private LhScheduleResultService lhScheduleResultService;
    //    @Autowired
//    private LhScheduleResultService0725 lhScheduleResultService0725;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IMdmProductConstructionService mdmProductConstructionService;
    @Autowired
    RedisService redisService;
    @Autowired
    private LhScheduleResultCheckHandle lhScheduleResultCheckHandle;
    @Autowired
    private LhScheduleAdjustService lhScheduleAdjustService;
    @Autowired
    private ILhDispatcherLogService lhDispatcherLogServiceImpl;

    @Autowired
    private ILhMachineInfoService lhMachineInfoService;
    @Resource
    private SyncDataLogsService syncDataLogsService;
    @Autowired
    private FactoryService factoryService;
    /**
     * 查询2025213硫化排程列表查询
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody LhScheduleResult entity) {
        TableDataInfo tableDataInfo = super.list(entity);
        List<LhScheduleResult> list = (List<LhScheduleResult>) tableDataInfo.getRows();
        if (CollectionUtils.isNotEmpty(list)) {
            List<Long> idList = list.stream().map(BaseEntity::getId).collect(Collectors.toList());
            LhDispatcherLog queryVO = new LhDispatcherLog();
            queryVO.setScheduleDate(entity.getScheduleDate());
            List<LhDispatcherLogVo> lhDispatcherLogVos = lhDispatcherLogServiceImpl.selectIsChangeList(queryVO, idList);
            Map<Long, LhDispatcherLogVo> logVoMap = new HashMap<>(16);
            if (CollectionUtils.isNotEmpty(lhDispatcherLogVos)) {
                logVoMap = lhDispatcherLogVos.stream().collect(Collectors.toMap(LhDispatcherLogVo::getScheduleId, Function.identity(), (s1, s2) -> s1));
            }
            for (LhScheduleResult lhScheduleResult : list) {
                Long id = lhScheduleResult.getId();
                if (logVoMap.containsKey(id)) {
                    LhDispatcherLogVo lhDispatcherLogVo = logVoMap.get(id);
                    BeanUtils.copyBeanProp(lhScheduleResult, lhDispatcherLogVo);
                }
                Integer class1PlanQty = ObjectUtils.defaultIfNull(lhScheduleResult.getClass1PlanQty(), 0);
                Integer class2PlanQty = ObjectUtils.defaultIfNull(lhScheduleResult.getClass2PlanQty(), 0);
                Integer class3PlanQty = ObjectUtils.defaultIfNull(lhScheduleResult.getClass3PlanQty(), 0);
                Integer class4PlanQty = ObjectUtils.defaultIfNull(lhScheduleResult.getClass4PlanQty(), 0);
                Integer class5PlanQty = ObjectUtils.defaultIfNull(lhScheduleResult.getClass5PlanQty(), 0);
                Integer class6PlanQty = ObjectUtils.defaultIfNull(lhScheduleResult.getClass6PlanQty(), 0);
                double totalPlanQty = class1PlanQty + class2PlanQty + class3PlanQty + class4PlanQty + class5PlanQty + class6PlanQty;
                Integer class1FinishQty = ObjectUtils.defaultIfNull(lhScheduleResult.getClass1FinishQty(), 0);
                Integer class2FinishQty = ObjectUtils.defaultIfNull(lhScheduleResult.getClass2FinishQty(), 0);
                Integer class3FinishQty = ObjectUtils.defaultIfNull(lhScheduleResult.getClass3FinishQty(), 0);
                Integer class4FinishQty = ObjectUtils.defaultIfNull(lhScheduleResult.getClass4FinishQty(), 0);
                Integer class5FinishQty = ObjectUtils.defaultIfNull(lhScheduleResult.getClass5FinishQty(), 0);
                Integer class6FinishQty = ObjectUtils.defaultIfNull(lhScheduleResult.getClass6FinishQty(), 0);
                double totalFinishQty = class1FinishQty + class2FinishQty + class3FinishQty + class4FinishQty + class5FinishQty + class6FinishQty;
                if (totalPlanQty != 0) {
                    lhScheduleResult.setFinishRate(totalFinishQty / totalPlanQty);
                }
            }
        }
        return tableDataInfo;
    }

    private Integer ifNull(Integer x) {
        return ObjectUtils.isEmpty(x) ? 0 : x;
    }

    @Override
    protected String[] getQueryFormulas() {
        return new String[]{
                "createByName->getcolvalue(SYS_USER, nick_name, user_name, createBy)",
                "updateByName->getcolvalue(SYS_USER, nick_name, user_name, updateBy)",
        };
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
     * 成型排程导入
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

    /**
     * 导入数据
     */
    @Log(title = "ui.data.column.port.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据2")
    @PostMapping("/importData2")
    public AjaxResult importData2(@RequestBody LhScheduleImportFileDTO lhScheduleImportFileDTO) throws Exception {

        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(lhScheduleImportFileDTO.getImportContext().getFileBytes(), lhScheduleImportFileDTO.getImportContext().getImportFilePath(), lhScheduleImportFileDTO.getImportContext().getProcedureCode(), lhScheduleImportFileDTO.getImportContext().getFunctionName(), lhScheduleImportFileDTO.getImportContext().getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        ExcelUtil<LhScheduleResult> util = new ExcelUtil<>(this.getTClass());
        InputStream is = new ByteArrayInputStream(lhScheduleImportFileDTO.getImportContext().getFileBytes());
        List<LhScheduleResult> list = util.importExcel(is);
        AjaxResult ajaxResult = lhScheduleResultService.importData2(list, importLog.getId(), lhScheduleImportFileDTO.getScheduleDate());
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
    @Log(title = "硫化排程导出", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody LhScheduleResult queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    public List<LhScheduleResult> listExportData(LhScheduleResult port) {
        startPage("MACHINE_ORDER,REAL_SCHEDULE_DATE,EMBRYO_CODE,LEFT_RIGHT_MOLD,SPEC_END_TIME");
        QueryWrapper<LhScheduleResult> queryWrapper = new QueryWrapper<>();
        this.builderCondition(queryWrapper, port);
        if (ApsConstant.TRUE.equals(port.getExportCombineFlag())){
            queryWrapper.last(" AND STR_TO_DATE(SCHEDULE_DATE, '%Y-%m-%d' ) = STR_TO_DATE(REAL_SCHEDULE_DATE, '%Y-%m-%d' )");
            List<LhScheduleResult> list = lhScheduleResultService.selectList(queryWrapper);
            return PubUtil.isEmpty(list) ? new ArrayList<>(): combineLeftRightMold(list);
        }else{
            return lhScheduleResultService.selectList(queryWrapper);
        }
        //return list;
    }

    /**
     * 合并左右模
     * @param list
     * @return
     */
    private List<LhScheduleResult> combineLeftRightMold(List<LhScheduleResult> list){
        if (PubUtil.isEmpty(list)){
            return null;
        }
        String factoryCode = list.get(0).getFactoryCode();
        LhMachineInfo queryLhMachineInfo = new LhMachineInfo();
        queryLhMachineInfo.setFactoryCode(factoryCode);
        //queryLhMachineInfo.setStatus(YesOrNoEnum.YES.getCode());
        List<LhMachineInfo> allMachineList = lhMachineInfoService.selectList(queryLhMachineInfo);
        //allMachineList.sort(Comparator.comparingInt(LhMachineInfo::getMachineOrder));
        List<LhScheduleResult> scheduleResultList = new ArrayList<>();
        List<String> specCodeList = new ArrayList<>();
        LhScheduleResult lhScheduledResult,rScheduledResult,lScheduledResult;
        int planQty;
        //过滤出1班和2班不全空，或所有班次全空（大概率是故障待机）
        Map<String, List<LhScheduleResult>> machineScheduledMap = list.stream().filter(x->(!(x.getClass1PlanQty() == null && x.getClass2PlanQty() == null)) || (
                x.getClass1PlanQty() == null && x.getClass2PlanQty() == null && x.getClass3PlanQty() == null && x.getClass4PlanQty() == null && x.getClass5PlanQty() == null && x.getClass6PlanQty() == null
                )).collect(Collectors.groupingBy(item->item.getLhMachineCode()));
        for (LhMachineInfo machineInfo:allMachineList) {
            List<LhScheduleResult> lhScheduleResults = machineScheduledMap.get(machineInfo.getMachineCode());
            if(PubUtil.isEmpty(lhScheduleResults)){
                //不存在机台排程，加空机台，便于线下操作
                lhScheduledResult = new LhScheduleResult();
                lhScheduledResult.setFactoryCode(factoryCode);
                lhScheduledResult.setLhMachineCode(machineInfo.getMachineCode());
                lhScheduledResult.setMachineOrder(machineInfo.getMachineOrder());
                scheduleResultList.add(lhScheduledResult);
            }else{
                //存在机台排程
                if (lhScheduleResults.size() == 1 || PubUtil.isEmpty(lhScheduleResults.get(0).getLeftRightMold())){
                    lhScheduleResults.get(0).setMachineOrder(machineInfo.getMachineOrder());
                    scheduleResultList.add(lhScheduleResults.get(0));
                    specCodeList.add(lhScheduleResults.get(0).getSpecCode());
                }else{
                    lScheduledResult = lhScheduleResults.get(0);
                    rScheduledResult = lhScheduleResults.get(1);
                    specCodeList.add(lScheduledResult.getSpecCode());
                    specCodeList.add(rScheduledResult.getSpecCode());
                    lhScheduledResult = new LhScheduleResult();
                    BeanUtils.copyProperties(lScheduledResult, lhScheduledResult);
                    String productCode = lScheduledResult.getProductCode();
                    String specDesc = lScheduledResult.getSpecDesc();
                    if (StringUtils.isNotEmpty(lScheduledResult.getProductCode()) && !lScheduledResult.getProductCode().equals(rScheduledResult.getProductCode())){
                        //物料号前后不同
                        productCode = lScheduledResult.getProductCode() + "*"+rScheduledResult.getProductCode();
                        specDesc = lScheduledResult.getSpecDesc() + "*"+rScheduledResult.getSpecDesc();
                    }
                    lhScheduledResult.setProductCode(productCode);
                    lhScheduledResult.setSpecDesc(specDesc);
                    String specCode = lScheduledResult.getSpecCode();
                    if (!lScheduledResult.getSpecCode().equals(rScheduledResult.getSpecCode())){
                        //规格代号前后不同
                        specCode = lScheduledResult.getSpecCode() + "*"+rScheduledResult.getSpecCode();
                    }
                    lhScheduledResult.setSpecCode(specCode);
                    String embryoCode = lScheduledResult.getEmbryoCode();
                    if (!lScheduledResult.getEmbryoCode().equals(rScheduledResult.getEmbryoCode())){
                        //生胎代号前后不同
                        embryoCode = lScheduledResult.getEmbryoCode() + "*"+rScheduledResult.getEmbryoCode();
                    }
                    lhScheduledResult.setEmbryoCode(embryoCode);
                    planQty = (lScheduledResult.getClass1PlanQty() != null ? lScheduledResult.getClass1PlanQty():0)+(rScheduledResult.getClass1PlanQty() != null ? rScheduledResult.getClass1PlanQty():0);
                    lhScheduledResult.setClass1PlanQty(planQty == 0 ? null : planQty);
                    planQty = (lScheduledResult.getClass2PlanQty() != null ? lScheduledResult.getClass2PlanQty():0)+(rScheduledResult.getClass2PlanQty() != null ? rScheduledResult.getClass2PlanQty():0);
                    lhScheduledResult.setClass2PlanQty(planQty == 0 ? null : planQty);
                    planQty = (lScheduledResult.getClass4PlanQty() != null ? lScheduledResult.getClass4PlanQty():0)+(rScheduledResult.getClass4PlanQty() != null ? rScheduledResult.getClass4PlanQty():0);
                    lhScheduledResult.setClass4PlanQty(planQty == 0 ? null : planQty);
                    planQty = (lScheduledResult.getClass5PlanQty() != null ? lScheduledResult.getClass5PlanQty():0)+(rScheduledResult.getClass5PlanQty() != null ? rScheduledResult.getClass5PlanQty():0);
                    lhScheduledResult.setClass5PlanQty(planQty == 0 ? null : planQty);
                    planQty = (lScheduledResult.getDailyPlanQty() != null ? lScheduledResult.getDailyPlanQty():0)+(rScheduledResult.getDailyPlanQty() != null ? rScheduledResult.getDailyPlanQty():0);
                    lhScheduledResult.setDailyPlanQty(planQty == 0 ? null : planQty);

                    planQty = (lScheduledResult.getClass1FinishQty() != null ? lScheduledResult.getClass1FinishQty():0)+(rScheduledResult.getClass1FinishQty() != null ? rScheduledResult.getClass1FinishQty():0);
                    lhScheduledResult.setClass1FinishQty(planQty == 0 ? null : planQty);
                    planQty = (lScheduledResult.getClass2FinishQty() != null ? lScheduledResult.getClass2FinishQty():0)+(rScheduledResult.getClass2FinishQty() != null ? rScheduledResult.getClass2FinishQty():0);
                    lhScheduledResult.setClass2FinishQty(planQty == 0 ? null : planQty);
                    planQty = (lScheduledResult.getClass4FinishQty() != null ? lScheduledResult.getClass4FinishQty():0)+(rScheduledResult.getClass4FinishQty() != null ? rScheduledResult.getClass4FinishQty():0);
                    lhScheduledResult.setClass4FinishQty(planQty == 0 ? null : planQty);
                    planQty = (lScheduledResult.getClass5FinishQty() != null ? lScheduledResult.getClass5FinishQty():0)+(rScheduledResult.getClass5FinishQty() != null ? rScheduledResult.getClass5FinishQty():0);
                    lhScheduledResult.setClass5FinishQty(planQty == 0 ? null : planQty);

                    lhScheduledResult.setMachineOrder(machineInfo.getMachineOrder());
                    scheduleResultList.add(lhScheduledResult);
                }
            }

        }
        //execMachineOrderFormulas(scheduleResultList);
        scheduleResultList.sort(Comparator.comparingInt(LhScheduleResult::getMachineOrder));

        //加载未在顺序机台列表的规格
        scheduleResultList.addAll(getRemainList(list,specCodeList));

        return scheduleResultList;
    }

    private List<LhScheduleResult> getRemainList(List<LhScheduleResult> list,List<String> specCodeList){
        return list.stream().filter(x->specCodeList.indexOf(x.getSpecCode())<0).collect(Collectors.toList());
    }
    /**
     * 执行机台顺序公式
     * @param scheduleResultList
     */
    private void execMachineOrderFormulas(List<LhScheduleResult> scheduleResultList) {
        //执行公式
        try {
            QueryFormulaUtil.execFormula(scheduleResultList, new String[]{
                    "machineOrder -> getcolvaluewithcondition(T_LH_MACHINE_INFO, MACHINE_ORDER, MACHINE_CODE, lhMachineCode, IS_DELETE = 0)",
            });
        } catch (QueryExprException e) {
            throw new ServiceException("执行查询公式时发生错误.");
        }
    }

    @ApiOperation(value = "模板下载" , notes = "导入模板下载")
    @GetMapping("/downloadTemplate")
    @ResponseBody
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.lh.scheduleResult.modelName");
        ExcelUtil<LhScheduleResult> util = new ExcelUtil<>(LhScheduleResult.class);
        util.exportExcel(response, null, fileName, fileName);
    }

    @Log(title = "ui.data.column.lhParams.modelName")
    @ApiOperation("插单查询可用机台列表")
    @PostMapping("/getScheduleMachineInfo")
    public List<LhMachineInfoVo> getScheduleMachineInfo(@RequestBody LhOrderInsertParamDTO insertParamDTO){
        return lhScheduleResultService.getScheduleMachineInfo(insertParamDTO);
    }

    @Log(title = "ui.data.column.lhParams.modelName")
    @ApiOperation("插单")
    @PostMapping("/insertOrder")
    public AjaxResult insertOrder(@RequestBody LhOrderInsertDTO insertDTO){
        ValidateResult validateResult = lhScheduleResultCheckHandle.insertLhScheduleResultCheck(insertDTO);
        if (!validateResult.isSuccess()) {
            return AjaxResult.error(validateResult.getMsg());
        }
        lhScheduleResultService.insertOrder(insertDTO);
        return AjaxResult.success();
    }

    @Log(title = "ui.data.column.lhParams.modelName")
    @ApiOperation("根据规格号查询物料号List")
    @PostMapping("/selectListMdmProductConstruction")
    public List<MdmProductConstruction> selectListMdmProductConstruction(@RequestBody LhSpecCodeParamDTO dto){
        return mdmProductConstructionService.selectListByFactoryCodeAndSpecCode(dto.getFactoryCode(), Collections.singletonList(dto.getSpecCode()));
    }

    @Log(title = "ui.data.column.lhParams.modelName")
    @ApiOperation("自动排程")
    @PostMapping("/autoLhScheduleResult")
    public AjaxResult autoLhScheduleResult(@RequestBody AutoLhScheduleResultDTO autoLhScheduleResultDTO){
        String key = ApsConstant.REDIS_APS_LH_AUTO_SCHEDULE + autoLhScheduleResultDTO.getFactoryCode()+DateUtils.parseDateToStr("yyyyMMdd",autoLhScheduleResultDTO.getScheduleTime());
        if (ApsConstant.TRUE.equals(redisService.getCacheObject(key))) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.column.lhScheduleResult.oneUse.autoSchedule"));
        }
        redisService.setCacheObject(key, ApsConstant.TRUE, ApsConstant.EXPIRE_ONE, TimeUnit.HOURS);
        try{
            lhScheduleResultService.autoLhScheduleResult(autoLhScheduleResultDTO);
        }catch (BusinessException ex){
            return AjaxResult.error(ex.getMessage());
        }finally {
            redisService.setCacheObject(key, ApsConstant.FALSE, ApsConstant.EXPIRE_ONE, TimeUnit.HOURS);
        }
        return AjaxResult.success();
    }


    @Log(title = "ui.data.column.lhParams.modelName")
    @ApiOperation("自动排程测试接口")
    @PostMapping("/autoLhScheduleResultTest")
    public AjaxResult autoLhScheduleResultTest(@RequestBody AutoLhScheduleResultDTO autoLhScheduleResultDTO){
        String key = ApsConstant.REDIS_APS_LH_AUTO_SCHEDULE + autoLhScheduleResultDTO.getFactoryCode()+DateUtils.parseDateToStr("yyyyMMdd",autoLhScheduleResultDTO.getScheduleTime());
        if (ApsConstant.TRUE.equals(redisService.getCacheObject(key))) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.column.lhScheduleResult.oneUse.autoSchedule"));
        }
        redisService.setCacheObject(key, ApsConstant.TRUE, ApsConstant.EXPIRE_ONE, TimeUnit.HOURS);
        try{
//            lhScheduleResultService0725.autoLhScheduleResult0725(autoLhScheduleResultDTO);
        }catch (BusinessException ex){
            return AjaxResult.error(ex.getMessage());
        }finally {
            redisService.setCacheObject(key, ApsConstant.FALSE, ApsConstant.EXPIRE_ONE, TimeUnit.HOURS);
        }
        return AjaxResult.success();
    }


    /**
     * 转机台校验
     */
    @Log(title = "ui.data.column.lhParams.modelName")
    @PostMapping("/validateChangeMachine")
    @ApiOperation("硫化排程结果转机台校验")
    public AjaxResult validateChangeMachine(@RequestBody LhTransferDeskDTO dto) {
        ValidateResult validateResult = lhScheduleResultCheckHandle.changeMachinePreCheck(dto);
        if (!validateResult.isSuccess()) {
            return AjaxResult.error(validateResult.getMsg());
        }

        return AjaxResult.success("校验通过");
    }

    @Log(title = "ui.data.column.lhParams.modelName")
    @PostMapping("/adjustQuantity")
    @ApiOperation("调量")
    public AjaxResult adjustQuantity(@RequestBody LhScheduleResultUpdateDTO dto) {
        ValidateResult validateResult = lhScheduleResultCheckHandle.updateLhScheduleResultCheck(dto);
        if (!validateResult.isSuccess()) {
            return AjaxResult.error(validateResult.getMsg());
        }
        //调用调量业务
        lhScheduleAdjustService.preAdjustment(dto);
        return AjaxResult.success();
    }

    /**
     * 转机台
     * @param dto
     * @return
     */
    @PostMapping("/changeMachine")
    @ApiOperation("转机台")
    public AjaxResult changeMachine(@RequestBody LhTransferDeskDTO dto) {
        ValidateResult validateResult = lhScheduleResultCheckHandle.changeMachinePreCheck(dto);
        if (!validateResult.isSuccess()) {
            return AjaxResult.error(validateResult.getMsg());
        }
        //调用转机台业务
        lhScheduleResultService.changeMachine(dto);
        return AjaxResult.success();
    }

    @PostMapping("/getBatchNo")
    @ApiOperation("根据排程时间获取批次号")
    public String getBatchNo(@RequestBody AutoLhScheduleResultDTO dto) {
        return lhScheduleResultService.selectBatchNoByScheduleDateAndFactoryCode(dto.getScheduleTime(),dto.getFactoryCode());
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
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productCode")), "PRODUCT_CODE", queryVO.getFieldValueByFieldName("productCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specCode")), "SPEC_CODE", queryVO.getFieldValueByFieldName("specCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specDesc")), "SPEC_DESC", queryVO.getFieldValueByFieldName("specDesc"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("embryoCode")), "EMBRYO_CODE", queryVO.getFieldValueByFieldName("embryoCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isRelease")), "IS_RELEASE", queryVO.getFieldValueByFieldName("isRelease"));
    }

    @Override
    protected String getOrderBy() {
        return "MACHINE_ORDER,REAL_SCHEDULE_DATE,EMBRYO_CODE,LEFT_RIGHT_MOLD,SPEC_END_TIME";

    }

    @Override
    protected IDocService getDocService() {
        return lhScheduleResultService;
    }

    @Override
    protected String getTypeCode() {
        return "LH2025213";
    }

   /* @Autowired
    private FactoryService factoryService;

    @Autowired
    private SyncDataLogsService syncDataLogsService;*/

    @Resource
    private LhSyncDataHandle syncDataHandle;

    @Autowired
    private LhScheduleResultEntityMapper lhScheduleResultEntityMapper;

    /**
     * 发布当天未发布的排程结果
     */
    @Log(title = "ui.data.column.lh.scheduleResult.modelName", businessType = BusinessType.PUBLISH)
    @ApiOperation("发布排程")
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody LhScheduleResult dto) {
        // 发布前需要先获得同步锁，防止在集群环境下出现一个前端命令发送两次mes请求，modify by hak 20220708
        if (syncDataLogsService.checkPublishLocking("lh:publish:lock", dto.getIds())) {
            // 如果已经被锁定了，则直接返回
            return AjaxResult.success();
        }
        int releasingOrTimeoutByIds = lhScheduleResultService.isReleasingOrTimeoutByIds(Arrays.stream(dto.getIds()).mapToLong(Long::longValue).toArray());
        if (releasingOrTimeoutByIds > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        //获取数据版本号
        String dataVersion = syncDataHandle.getDataVersion(ApsConstant.LH_DEPLOY_SYNC_KEY);
        // 厂别、分公司编号
        String factoryCode = factoryService.getFactoryCode();
        String companyCode = factoryService.getCompanyCode();

        LhScheduleResult scheduleResult = new LhScheduleResult();
        org.springframework.beans.BeanUtils.copyProperties(dto, scheduleResult);
        QueryWrapper<LhScheduleResult> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, scheduleResult);
        wrapper.in(PubUtil.isNotEmpty(scheduleResult.getIds()), "id", Arrays.asList(scheduleResult.getIds()));
        // 过滤未发布及发布失败的数据
        List<LhScheduleResult> list = lhScheduleResultEntityMapper.selectList(wrapper).stream()
                .filter(item -> ApsConstant.NO_RELEASE.equals(item.getIsRelease()) || ApsConstant.FAILURE_RELEASE.equals(item.getIsRelease()) || ApsConstant.WAIT_RELEASING.equals(item.getIsRelease())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.errorPublish"));
        }
        // 获取机台id为空和多机台的记录
        List<LhScheduleResult> collect = list.stream().filter(item -> StringUtil.isEmpty(item.getLhMachineCode()) || item.getLhMachineCode().contains(",")).collect(Collectors.toList());
        if (!collect.isEmpty()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.hasMultipleIds"));
        }
        //排程发布
        long[] arr = list.stream().mapToLong(BaseEntity::getId).toArray();

        Date scheduleDate = scheduleResult.getScheduleDate();
        AjaxResult ajaxResult = null;
        try {
            ajaxResult = lhScheduleResultService.publish(arr, scheduleDate, dataVersion, factoryCode, companyCode);
            // 请求参数
            JSONObject params = new JSONObject();
            params.put("scheduleDate", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, scheduleDate));
            params.put("rowCount", arr.length);
            SyncParamsVO syncParamsVO = new SyncParamsVO();
            syncParamsVO.setSyncKey(ApsConstant.LH_DEPLOY_SYNC_KEY);
            syncParamsVO.setDataVersion(dataVersion);
            syncParamsVO.setParams(params);
            syncParamsVO.setFactoryCode(factoryCode);
            syncParamsVO.setCompanyCode(companyCode);
            syncDataHandle.syncNotice(syncParamsVO);

            // 取回mes的反馈结果
            SyncDataLogs logs = syncDataLogsService.getSyncDataResult(dataVersion);
            String status = logs.getStatus();
            // 更新状态
            lhScheduleResultService.updateReleaseStatus(dataVersion, arr, status);
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
        return ajaxResult;
    }

    /**
     * 查询硫化机台甘特图
     *
     * @param queryVO 查询参数
     * @return 结果
     */
    @ApiOperation("查询硫化机台甘特图")
    @PostMapping("/selectMachineGantt")
    public AjaxResult selectMachineGantt(@RequestBody LhGanttVo queryVO) {
        return lhScheduleResultService.selectMachineGantt(queryVO);
    }

    /**
     * 查询硫化计划月度完成量情况
     */
    @ApiOperation("查询硫化计划月度完成量情况")
    @PostMapping("/monthFinishQtyList")
    public TableDataInfo monthFinishQtyList(@RequestBody LhMonthPlanSurplusDetail queryVO) {
        try {
//            startPage();
            List<LhMonthFinishQtyVo> list = lhScheduleResultService.monthFinishQtyList(queryVO);
            return getDataTable(list);
        } finally {
//            clearPage();
        }
    }


    @ResponseBody
    @PostMapping("/statisticsDay")
    @ApiOperation("查询硫化计划月度完成量情况统计")
    public AjaxResult statisticsByDay(@RequestBody LhMonthPlanSurplusDetail param) {
        if (null == param) {
            return AjaxResult.success(Collections.emptyList());
        }
        return AjaxResult.success(lhScheduleResultService.getStatisticsDay(param));
    }
}

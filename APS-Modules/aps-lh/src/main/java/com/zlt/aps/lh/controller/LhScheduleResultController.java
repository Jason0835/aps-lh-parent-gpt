package com.zlt.aps.lh.controller;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.util.StringUtil;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.engine.domain.LhEngineTireConstructionInfo;
import com.zlt.aps.common.engine.domain.SyncDataLogs;
import com.zlt.aps.common.engine.result.ValidateResult;
import com.zlt.aps.common.engine.service.FactoryService;
import com.zlt.aps.common.engine.service.LhEngineTireConstructionInfoService;
import com.zlt.aps.common.engine.service.SyncDataLogsService;
import com.zlt.aps.lh.api.domain.entity.Gante;
import com.zlt.aps.lh.api.domain.dto.LhScheduleResultDto;
import com.zlt.aps.lh.common.handle.LhSyncDataHandle;
import com.zlt.aps.lh.engine.exception.LhEngineException;
import com.zlt.aps.lh.engine.service.LhEngineService;
import com.zlt.aps.lh.entity.LhScheduleResult;
import com.zlt.aps.lh.service.LhScheduleResultService;
import com.zlt.sync.povo.SyncParamsVO;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 硫化排程结果Controller
 *
 * @author chen
 * @date 2021-07-19
 */
@RestController
@RequestMapping("/lh/scheduleResult")
public class LhScheduleResultController extends BaseController {
    @Autowired
    private LhScheduleResultService lhScheduleResultService;

    @Autowired
    private LhEngineService lhEngineService;

    @Resource
    private LhSyncDataHandle syncDataHandle;

    @Autowired
    private LhEngineTireConstructionInfoService lhEngineTireConstructionInfoService;

    @Autowired
    private FactoryService factoryService;

    @Autowired
    private SyncDataLogsService syncDataLogsService;

    /**
     * 查询硫化排程结果列表
     */
    @ApiOperation("查询硫化排程结果列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody LhScheduleResultDto dto) {
//        startPage("a.LH_MACHINE_CODE");
        LhScheduleResult lhScheduleResult = new LhScheduleResult();
        BeanUtils.copyProperties(dto, lhScheduleResult);
        lhScheduleResult.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<LhScheduleResultDto> list = lhScheduleResultService.selectLhScheduleResultList(lhScheduleResult);
        return getDataTable(list);
    }

    /**
     * 获取硫化排程结果详细信息
     */
    @ApiOperation("获取硫化排程结果详细信息")
    @GetMapping(value = "/{id}")
    public LhScheduleResultDto getInfo(@PathVariable("id") Long id) {
        return lhScheduleResultService.selectLhScheduleResultById(id);
    }

    /**
     * 新增硫化排程结果
     */
    @Log(title = "ui.data.column.lh.scheduleResult.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增硫化排程结果")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody LhScheduleResultDto dto) {
        try {
            // 根据sap获取胎胚代码 start
            if (StringUtil.isEmpty(dto.getEmbryoCode())) {
                LhEngineTireConstructionInfo condition=new LhEngineTireConstructionInfo();
                List<LhEngineTireConstructionInfo> constructionInfoList=lhEngineTireConstructionInfoService.selectLhTireConstructionInfoList(condition);
                Map<String,List<LhEngineTireConstructionInfo>> sapTireConstructionListMap=new HashMap<>();
                if(StringUtils.isNotEmpty(constructionInfoList)){
                    sapTireConstructionListMap=constructionInfoList.stream().collect(Collectors.groupingBy(LhEngineTireConstructionInfo::getSapCode));
                }
                List<LhEngineTireConstructionInfo> constructionInfos = sapTireConstructionListMap.get(dto.getSapCode());
                if (constructionInfos.size() == 1) {
                    dto.setEmbryoCode(constructionInfos.get(0).getEmbryoCode());
                }
            }
            // 根据sap获取胎胚代码 end
            List<LhScheduleResultDto> scheduleResults = lhScheduleResultService.selectByScheduleDateAndCode(dto);
            lhEngineService.insertLhScheduleOrder(dto);
            lhScheduleResultService.insetDispatcherLogInsertOrder(ApsConstant.DISPATCHER_OPER_INSERT_ORDER, scheduleResults, dto);
            return AjaxResult.success();
        } catch (LhEngineException e) {
            logger.error("插单异常：" + e.getMessage());
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 修改硫化排程结果
     */
    @Log(title = "ui.data.column.lh.scheduleResult.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改硫化排程结果")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody LhScheduleResultDto dto) {
        LhScheduleResult lhScheduleResult = new LhScheduleResult();
        BeanUtils.copyProperties(dto, lhScheduleResult);
        if(lhScheduleResult.getId() != null) {
            int releasingOrTimeoutByIds = lhScheduleResultService.isReleasingOrTimeoutByIds(new long[]{lhScheduleResult.getId()});
            if (releasingOrTimeoutByIds > 0) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
            }
        }
        return toAjax(lhScheduleResultService.updateLhScheduleResult(lhScheduleResult));
    }

    /**
     * 删除硫化排程结果
     */
    @Log(title = "ui.data.column.lh.scheduleResult.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除硫化排程结果")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable long[] ids) {
//        int releasingOrTimeoutByIds = lhScheduleResultService.isReleasingOrTimeoutByIds(ids);
//        if (releasingOrTimeoutByIds > 0) {
//            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
//        }
        if (lhScheduleResultService.isPublishByIds(ids) != ids.length) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isPublishById"));
        }
        lhScheduleResultService.deleteLhScheduleResultByIds(ids);
        return AjaxResult.success();
    }

    /**
     * 获取硫化排程结果列表
     */
    @ApiOperation("获取硫化排程结果列表")
    @PostMapping("/getList")
    public List<LhScheduleResultDto> getList(@RequestBody LhScheduleResultDto dto) {
//        startPage("a.LH_MACHINE_CODE");
        LhScheduleResult lhScheduleResult = new LhScheduleResult();
        BeanUtils.copyProperties(dto, lhScheduleResult);
        return lhScheduleResultService.selectLhScheduleResultList(lhScheduleResult);
    }

    /**
     * 导出硫化排程结果列表
     */
    @Log(title = "ui.data.column.lh.scheduleResult.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出硫化排程结果列表")
    @PostMapping("/export")
    public byte[] export(@RequestBody LhScheduleResultDto dto) throws IOException {
//        startPage("a.LH_MACHINE_CODE");
        LhScheduleResult scheduleResult = new LhScheduleResult();
        BeanUtils.copyProperties(dto, scheduleResult);
        scheduleResult.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        List<LhScheduleResultDto> list = lhScheduleResultService.selectLhScheduleResultList(scheduleResult);
        return lhScheduleResultService.export(list);
    }

    /**
     * 校验硫化排程结果唯一性
     */
    @ApiOperation("校验硫化排程结果唯一性")
    @PostMapping("/checkLhScheduleResultUnique")
    public String checkLhScheduleResultUnique(@RequestBody LhScheduleResultDto dto) {
        LhScheduleResult lhScheduleResult = new LhScheduleResult();
        BeanUtils.copyProperties(dto, lhScheduleResult);
        return lhScheduleResultService.checkLhScheduleResultUnique(lhScheduleResult);
    }

    /**
     * 插单校验
     */
    @PostMapping("/validateAdd")
    @ApiOperation("硫化排程结果插单校验")
    public AjaxResult validateAdd(@RequestBody LhScheduleResultDto dto){
        int releasingOrTimeoutByDate = lhScheduleResultService.isReleasingOrTimeoutByDate(dto.getScheduleDate());
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        ValidateResult validateResult = lhEngineService.inertLhScheduleResultPreCheck(dto);
        String msg = validateResult.getMsg();
        if (validateResult.isSuccess()) {
            return AjaxResult.success(msg,dto);
        } else {
            return AjaxResult.error(msg);
        }
    }

    /**
     * 转机台校验
     */
    @PostMapping("/validateChangeMachine")
    @ApiOperation("硫化排程结果转机台校验")
    public AjaxResult validateChangeMachine(@RequestBody LhScheduleResultDto dto){
        int releasingOrTimeoutByIds = lhScheduleResultService.isReleasingOrTimeoutByIds(new long[]{dto.getId()});
        if (releasingOrTimeoutByIds > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        //1.根据原始单据ID查出转机台前的原始数据
        LhScheduleResultDto historyDto=lhScheduleResultService.selectLhScheduleResultById(dto.getId());
        if(historyDto!=null){
            ValidateResult validateResult = lhEngineService.changeLhMachinePreCheck(historyDto, dto.getLhMachineCode());
            String msg = validateResult.getMsg();
            if (validateResult.isSuccess()) {
                return AjaxResult.success(msg, historyDto);
            } else {
                return AjaxResult.error(msg);
            }
        }else{
            return AjaxResult.error();
        }

    }

    /**
     * 转机台
     * @param dto 更改数据
     * @return 结果
     */
    @Log(title = "ui.data.column.lh.scheduleResult.modelName", businessType = BusinessType.CHANGE_MACHINE)
    @PostMapping("/changeMachine")
    @ApiOperation("硫化排程结果转机台")
    public AjaxResult changeMachine(@RequestBody LhScheduleResultDto dto){
        int releasingOrTimeoutByIds = lhScheduleResultService.isReleasingOrTimeoutByIds(new long[]{dto.getId()});
        if (releasingOrTimeoutByIds > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        LhScheduleResultDto beforeCxScheduleResultDto= lhScheduleResultService.selectLhScheduleResultById(dto.getId());
        beforeCxScheduleResultDto.setIsSuccess(dto.getIsSuccess());
        beforeCxScheduleResultDto.setIsRelease(beforeCxScheduleResultDto.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
        lhEngineService.changeLhMachine(beforeCxScheduleResultDto,dto.getLhMachineCode());
        lhScheduleResultService.insetDispatcherLog(ApsConstant.DISPATCHER_OPER_MACHINE, beforeCxScheduleResultDto, dto);  //如果是调度员操作，则需要增加操作日志
        return AjaxResult.success();
    }

    /**
     * 调量
     * @param dto 更改数据
     * @return 结果
     */
    @Log(title = "ui.data.column.lh.scheduleResult.modelName", businessType = BusinessType.CHANGE_QTY)
    @PostMapping("/changeQty")
    @ApiOperation("硫化排程结果调量")
    public AjaxResult changeQty(@RequestBody LhScheduleResultDto dto){
        int releasingOrTimeoutByIds = lhScheduleResultService.isReleasingOrTimeoutByIds(new long[]{dto.getId()});
        if (releasingOrTimeoutByIds > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        dto.setBaseVale(null);
        lhScheduleResultService.insetDispatcherLog(ApsConstant.DISPATCHER_OPER_PLAN, null, dto);  //如果是调度员操作，则需要增加操作日志
        return this.edit(dto);
    }

    /**
     * 发布当天未发布的排程结果
     */
    @Log(title = "ui.data.column.lh.scheduleResult.modelName", businessType = BusinessType.PUBLISH)
    @ApiOperation("发布排程")
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody LhScheduleResultDto dto) {
    	// 发布前需要先获得同步锁，防止在集群环境下出现一个前端命令发送两次mes请求，modify by hak 20220708
    	if (syncDataLogsService.checkPublishLocking("lh:publish:lock", dto.getIds())) {
    		return AjaxResult.success(); // 如果已经被锁定了，则直接返回
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
        BeanUtils.copyProperties(dto, scheduleResult);
        // 过滤未发布及发布失败的数据
        List<LhScheduleResultDto> list = lhScheduleResultService.selectLhScheduleResultList(scheduleResult).stream()
                .filter(item -> ApsConstant.NO_RELEASE.equals(item.getIsRelease()) || ApsConstant.FAILURE_RELEASE.equals(item.getIsRelease()) || ApsConstant.WAIT_RELEASING.equals(item.getIsRelease())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.errorPublish"));
        }
        // 获取机台id为空和多机台的记录
        List<LhScheduleResultDto> collect = list.stream().filter(item -> StringUtil.isEmpty(item.getLhMachineCode()) || item.getLhMachineCode().contains(",")).collect(Collectors.toList());
        if (collect.size() > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.hasMultipleIds"));
        }
        //排程发布
        long[] arr = list.stream().mapToLong(item -> item.getId()).toArray();

        Date scheduleDate=scheduleResult.getScheduleDate();
        AjaxResult ajaxResult=null;
        try{
            ajaxResult=lhScheduleResultService.publish(arr,scheduleDate,dataVersion,factoryCode,companyCode);
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
			lhScheduleResultService.updateRelaseStatus(dataVersion, arr, status);
			if (ApsConstant.IS_RELEASE.equals(status)) {
				// 成功
				ajaxResult = AjaxResult.success();
			} else {
				// 失败，需要返回异常信息
				ajaxResult = AjaxResult.error(logs.getMsg());
			}
        }catch (Exception e){
            //异常时进行堆栈内容打印
            e.printStackTrace();
            ajaxResult=AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.failedPublish"));
        }
        return ajaxResult;
    }

    @Log(title = "ui.data.column.lh.scheduleResult.modelName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入硫化排程结果信息")
    public AjaxResult importData(@RequestBody List<LhScheduleResultDto> list, @RequestParam("importLogId") Long importLogId, @RequestParam("scheduleDate") String scheduleDate) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        AjaxResult ajaxResult = null;
        try {
            ajaxResult = lhScheduleResultService.importData(list, importLogId, DateUtils.parseDate(scheduleDate));
        } catch (Exception e) {
            e.printStackTrace();
            ajaxResult =  AjaxResult.error(e.getMessage().replaceAll(",", "，"));
        }

        return ajaxResult;
    }

    /**
     * 查询排程日期是否已发布
     */
    @PostMapping("/isPublish")
    public Boolean isPublish(@RequestBody LhScheduleResultDto entity) {
        return lhScheduleResultService.isPublish(entity.getScheduleDate());
    }

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @PostMapping("/isReleasingOrTimeoutByDate")
    public int isReleasingOrTimeoutByDate(@RequestBody LhScheduleResultDto scheduleResult){
        return lhScheduleResultService.isReleasingOrTimeoutByDate(scheduleResult.getScheduleDate());
    }

    /**
     * 更改发布状态
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @Log(title = "ui.data.column.tcScheduleResult.modalName")
    @PostMapping("/changeReleaseStatus")
    public AjaxResult changeReleaseStatus(@RequestBody LhScheduleResultDto entity){
        LhScheduleResult lhScheduleResult = new LhScheduleResult();
        BeanUtils.copyProperties(entity, lhScheduleResult);
        lhScheduleResultService.changeReleaseStatus(lhScheduleResult);
        return AjaxResult.success();
    }

    /**
     * 自动排程
     */
    @PostMapping("/autoPlan")
    public AjaxResult autoPlan(@RequestBody LhScheduleResultDto dto) {
        //执行硫化自动排程算法
        Date scheduleDate = dto.getScheduleDate();
        int releasingOrTimeoutByDate = lhScheduleResultService.isReleasingOrTimeoutByDate(dto.getScheduleDate());
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        lhEngineService.autoLhSchedule(scheduleDate);
        return AjaxResult.success();
    }


    /**
     * 查询排程机台甘特图数据
     */
    @PostMapping("/getLhGanteData")
    public List<Gante> getLhGanteData(@RequestBody Gante gante){
        return  lhScheduleResultService.getLhGanteData(gante);
    }

}

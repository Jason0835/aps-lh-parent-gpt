package com.zlt.aps.lh.controller;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.engine.domain.LhEngineTireConstructionInfo;
import com.zlt.aps.common.engine.domain.SyncDataLogs;
import com.zlt.aps.common.engine.service.FactoryService;
import com.zlt.aps.common.engine.service.LhEngineTireConstructionInfoService;
import com.zlt.aps.common.engine.service.SyncDataLogsService;
import com.zlt.aps.lh.api.domain.dto.LhApsMoldAdjustPlanDto;
import com.zlt.aps.lh.api.domain.entity.LhApsMoldAdjustPlan;
import com.zlt.aps.lh.common.handle.LhSyncDataHandle;
import com.zlt.aps.lh.service.LhApsMoldAdjustPlanService;
import com.zlt.sync.povo.SyncParamsVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 硫化工序模具变动单APSController
 *
 * @author Joran.zhang
 * @date 2022-06-07
 */
@RestController
@RequestMapping("/lhApsMoldAdjustPlan")
public class LhApsMoldAdjustPlanController extends BaseController
{
    @Autowired
    private LhApsMoldAdjustPlanService lhApsMoldAdjustPlanService;

    @Resource
    private LhSyncDataHandle syncDataHandle;


    @Autowired
    private FactoryService factoryService;

    @Autowired
    private SyncDataLogsService syncDataLogsService;

    @Autowired
    private LhEngineTireConstructionInfoService lhEngineTireConstructionInfoService;

    /**
     * 查询硫化工序模具变动单APS列表
     */
    @ApiOperation("查询硫化工序模具变动单APS列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody LhApsMoldAdjustPlan lhApsMoldAdjustPlan)
    {
        startPage("mdp.plan_date desc,lh_machine_name asc, orderStr asc, left_right_mold");
        List<LhApsMoldAdjustPlan> list = lhApsMoldAdjustPlanService.selectLhApsMoldAdjustPlanList(lhApsMoldAdjustPlan);
        return getDataTable(list);
    }

    /**
     * 获取硫化工序模具变动单APS详细信息
     */
    @ApiOperation("获取硫化工序模具变动单APS详细信息")
    @GetMapping(value = "/{id}")
    public LhApsMoldAdjustPlan getInfo(@PathVariable("id") Long id){
        return lhApsMoldAdjustPlanService.selectLhApsMoldAdjustPlanById(id);
    }

    /**
     * 新增硫化工序模具变动单APS
     */
    @Log(title = "ui.data.column.lhApsMoldAdjustPlan.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增硫化工序模具变动单APS")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody LhApsMoldAdjustPlan lhApsMoldAdjustPlan){
        return toAjax(lhApsMoldAdjustPlanService.insertLhApsMoldAdjustPlan(lhApsMoldAdjustPlan));
    }

    /**
     * 修改硫化工序模具变动单APS
     */
    @Log(title = "ui.data.column.lhApsMoldAdjustPlan.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改硫化工序模具变动单APS")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody LhApsMoldAdjustPlan lhApsMoldAdjustPlan){
        return toAjax(lhApsMoldAdjustPlanService.updateLhApsMoldAdjustPlan(lhApsMoldAdjustPlan));
    }

    /**
     * 删除硫化工序模具变动单APS
     */
    @Log(title = "ui.data.column.lhApsMoldAdjustPlan.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除硫化工序模具变动单APS")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        int releasingOrTimeoutByIds = lhApsMoldAdjustPlanService.isReleasingOrTimeoutByIds(ids);
        if (releasingOrTimeoutByIds > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.lhMoldPlan.release.isReleasingOrTimeoutById"));
        }
        if (lhApsMoldAdjustPlanService.isPublishByIds(ids) != ids.length) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.lhMoldPlan.release.isPublishById"));
        }
        return toAjax(lhApsMoldAdjustPlanService.deleteLhApsMoldAdjustPlanByIds(ids));
    }

    /**
     * 导出硫化工序模具变动单APS列表
     */
    @Log(title = "ui.data.column.lhApsMoldAdjustPlan.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出硫化工序模具变动单APS列表")
    @PostMapping("/getList")
    public List<LhApsMoldAdjustPlan> getList(@RequestBody LhApsMoldAdjustPlan lhApsMoldAdjustPlan){
        startPage("create_time desc");
        return  lhApsMoldAdjustPlanService.selectLhApsMoldAdjustPlanList(lhApsMoldAdjustPlan);
    }

    /**
     * 校验硫化工序模具变动单APS唯一性
     */
    @ApiOperation("校验硫化工序模具变动单APS唯一性")
    @PostMapping("/checkLhApsMoldAdjustPlanUnique")
    public String checkLhApsMoldAdjustPlanUnique(@RequestBody LhApsMoldAdjustPlan lhApsMoldAdjustPlan){
        return lhApsMoldAdjustPlanService.checkLhApsMoldAdjustPlanUnique(lhApsMoldAdjustPlan);
    }

    /**
     * 根据集合导入硫化工序模具变动单APS数据
     * @param list 集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId 导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.lhApsMoldAdjustPlan.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入硫化工序模具变动单APS数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<LhApsMoldAdjustPlan> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return lhApsMoldAdjustPlanService.importData(list, updateSupport, importLogId);
    }

    /**
     * 发布当天未发布的排程结果
     */
    @Log(title = "ui.data.column.lhApsMoldAdjustPlan.modelName", businessType = BusinessType.PUBLISH)
    @ApiOperation("模具计划下发MES")
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody LhApsMoldAdjustPlan lhApsMoldAdjustPlan) {
        int releasingOrTimeoutByIds = lhApsMoldAdjustPlanService.isReleasingOrTimeoutByIds(lhApsMoldAdjustPlan.getIds());
        if (releasingOrTimeoutByIds > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.lhMoldPlan.release.isReleasingOrTimeoutById"));
        }
        //获取数据版本号
        String dataVersion = syncDataHandle.getDataVersion(ApsConstant.APS_MOLD_PLAN_2_MES);
        // 厂别、分公司编号
        String factoryCode = factoryService.getFactoryCode();
        String companyCode = factoryService.getCompanyCode();

        LhApsMoldAdjustPlan apsMoldAdjustPlan = new LhApsMoldAdjustPlan();
        BeanUtils.copyProperties(lhApsMoldAdjustPlan, apsMoldAdjustPlan);
        // 过滤未发布及发布失败的数据
        List<LhApsMoldAdjustPlan> list = lhApsMoldAdjustPlanService.selectLhApsMoldAdjustPlanList(apsMoldAdjustPlan).stream()
                .filter(item -> ApsConstant.NO_RELEASE.equals(item.getIsRelease()) || ApsConstant.FAILURE_RELEASE.equals(item.getIsRelease()) || ApsConstant.WAIT_RELEASING.equals(item.getIsRelease())).collect(Collectors.toList());
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.lhMoldPlan.errorPublish"));
        }

        //排程发布
        long[] arr = list.stream().mapToLong(item -> item.getId()).toArray();

        Date planDate=apsMoldAdjustPlan.getPlanDate();
        AjaxResult ajaxResult=null;
        try{
            ajaxResult=lhApsMoldAdjustPlanService.publish(arr,planDate,dataVersion,factoryCode,companyCode);
            // 请求参数
            JSONObject params = new JSONObject();
            params.put("planDate", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, planDate));
            params.put("rowCount", arr.length);
            SyncParamsVO syncParamsVO = new SyncParamsVO();
            syncParamsVO.setSyncKey(ApsConstant.APS_MOLD_PLAN_2_MES);
            syncParamsVO.setDataVersion(dataVersion);
            syncParamsVO.setParams(params);
            syncParamsVO.setFactoryCode(factoryCode);
            syncParamsVO.setCompanyCode(companyCode);
            syncDataHandle.syncNotice(syncParamsVO);

            // 取回mes的反馈结果
            SyncDataLogs logs = syncDataLogsService.getSyncDataResult(dataVersion);
            String status = logs.getStatus();
            // 更新状态
            lhApsMoldAdjustPlanService.updateRelaseStatus(dataVersion, arr, status);
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

    /**
     * 获取前规格信息
     * @param sapCode sap品号
     * @param embryoCode 胎胚代码
     * @return 规格信息
     */
    @ApiOperation("获取前规格信息")
    @PostMapping("/getBeforeSpecDesc")
    public LhApsMoldAdjustPlan getBeforeSpecDesc(@RequestBody LhApsMoldAdjustPlan lhApsMoldAdjustPlan) {
        LhEngineTireConstructionInfo infoByCondition = lhEngineTireConstructionInfoService.getLhConstructionInfoByCondition(lhApsMoldAdjustPlan.getBeforeSapCode(), lhApsMoldAdjustPlan.getBeforeEmbryoCode());
        lhApsMoldAdjustPlan.setBeforeSpecDesc(infoByCondition.getSpecDesc());
        return lhApsMoldAdjustPlan;
    }

    /**
     * 获取后规格信息
     * @param sapCode sap品号
     * @param embryoCode 胎胚代码
     * @return 规格信息
     */
    @ApiOperation("获取后规格信息")
    @PostMapping("/getAfterSpecDesc")
    public LhApsMoldAdjustPlan getAfterSpecDesc(@RequestBody LhApsMoldAdjustPlan lhApsMoldAdjustPlan) {
        LhEngineTireConstructionInfo infoByCondition = lhEngineTireConstructionInfoService.getLhConstructionInfoByCondition(lhApsMoldAdjustPlan.getAfterSapCode(), lhApsMoldAdjustPlan.getAfterEmbryoCode());
        lhApsMoldAdjustPlan.setAfterSpecDesc(infoByCondition.getSpecDesc());
        return lhApsMoldAdjustPlan;
    }

    /**
     * 根据ids更改执行状态
     * @param lhApsMoldAdjustPlan ids、要更改的状态
     * @return 结果
     */
    @ApiOperation("根据ids更改执行状态")
    @PostMapping("/changeExecute")
    public AjaxResult changeExecute(@RequestBody LhApsMoldAdjustPlan lhApsMoldAdjustPlan) {
        return lhApsMoldAdjustPlanService.changeExecute(lhApsMoldAdjustPlan);
    }

    /**
     * 新增硫化工序模具变动单APS主子表
     */
    @ApiOperation("新增硫化工序模具变动单APS主子表")
    @PostMapping("/addSubData")
    public AjaxResult addSubData(@RequestBody LhApsMoldAdjustPlanDto dto) {
        return lhApsMoldAdjustPlanService.addSubData(dto);
    }
}

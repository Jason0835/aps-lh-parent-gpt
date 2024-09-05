package com.zlt.aps.cx.controller;

import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.planmain.MdmMonthPlanAmountSumService;
import com.zlt.aps.common.engine.result.ValidateResult;
import com.zlt.aps.cx.api.domain.entity.CxPlanProductStatus;
import com.zlt.aps.cx.engine.service.CxScheduleEngineService;
import com.zlt.aps.cx.service.CxPlanProductStatusService;
import io.swagger.annotations.ApiOperation;
import org.apache.ibatis.javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型计划投产状态Controller
 *
 * @author zlt
 * @date 2021-07-21
 */
@RestController
@RequestMapping("/productStatus")
public class CxPlanProductStatusController extends BaseController {
    @Autowired
    private CxPlanProductStatusService cxPlanProductStatusService;

    @Autowired
    private MdmMonthPlanAmountSumService mdmMonthPlanAmountSumService;

    @Autowired
    private CxScheduleEngineService cxScheduleEngineService;

    /**
     * 查询成型计划投产状态列表
     */
    //@PreAuthorize(hasPermi = "cx:productStatus:list")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody CxPlanProductStatus cxPlanProductStatus) {
        startPage();
        cxPlanProductStatus.setOrderStr(orderStr());
        List<CxPlanProductStatus> list = cxPlanProductStatusService.selectCxPlanProductStatusList(cxPlanProductStatus);
        return getDataTable(list);
    }

    /**
     * 获取成型计划投产状态详细信息
     */
    @GetMapping(value = "/{id}")
    public CxPlanProductStatus getInfo(@PathVariable("id") Long id) {
        return cxPlanProductStatusService.selectCxPlanProductStatusById(id);
    }

    /**
     * 根据成型批次号获取成型计划投产状态详细信息
     */
    @PostMapping(value = "/getInfo2")
    public CxPlanProductStatus getInfo2(@RequestBody CxPlanProductStatus cxPlanProductStatus) {
        return cxPlanProductStatusService.selectCxPlanProductStatusByCxBatchNo(cxPlanProductStatus);
    }

    /**
     * 新增成型计划投产状态
     */
    //@PreAuthorize(hasPermi = "cx:productStatus:add")
    @Log(title = "ui.data.column.productStatus.modalName", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody CxPlanProductStatus cxPlanProductStatus) {
        return toAjax(cxPlanProductStatusService.insertCxPlanProductStatus(cxPlanProductStatus));
    }

    /**
     * 投产校验
     */
    @PostMapping("/validateProduction")
    public AjaxResult validateProduction(@RequestBody CxPlanProductStatus cxPlanProductStatus) {
        if(StringUtils.isEmpty(cxPlanProductStatus.getIds())){
            //收尾列表转过来的投产（未关联投产记录）
            ValidateResult validateResult = cxScheduleEngineService.reProductTaskPreCheck(cxPlanProductStatus);
            if (!validateResult.isSuccess()) {
                return AjaxResult.error(validateResult.getMsg());
            }
            return AjaxResult.success();
        }else {
            //投产列表转过来的投产（存在投产记录）
            Long[] arr = Convert.toLongArray(cxPlanProductStatus.getIds());
            List<CxPlanProductStatus> list = cxPlanProductStatusService.seleteCxPlanProductStatusByIds(arr);
            list.forEach(aa -> {
                aa.setCxMachineCode(cxPlanProductStatus.getCxMachineCode());
                aa.setScheduleDate(cxPlanProductStatus.getScheduleDate());
                aa.setStorageLocation(cxPlanProductStatus.getStorageLocation());
                aa.setClass1PlanQty(cxPlanProductStatus.getClass1PlanQty());
                aa.setClass2PlanQty(cxPlanProductStatus.getClass2PlanQty());
                aa.setClass3PlanQty(cxPlanProductStatus.getClass3PlanQty());
                aa.setClass4PlanQty(cxPlanProductStatus.getClass4PlanQty());
                aa.setClass5PlanQty(cxPlanProductStatus.getClass5PlanQty());
            });
            StringBuilder errorMsg = new StringBuilder();
            for (CxPlanProductStatus production : list) {
                //此处调用投产验证接口，需返回AjaxResult
                ValidateResult validateResult = cxScheduleEngineService.productPreCheck(production);
                if (!validateResult.isSuccess()) {
                    errorMsg.append(validateResult.getMsg());
                }
            }
            //此处要改返回值
            if (StringUtils.isNotEmpty(errorMsg)) {
                return AjaxResult.error(errorMsg.toString());
            }
            return AjaxResult.success();
        }
    }

    /**
     * 投产
     */
    @Log(title = "ui.data.column.productStatus.modalName", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody CxPlanProductStatus cxPlanProductStatus) {

        if(StringUtils.isEmpty(cxPlanProductStatus.getIds())){
            //收尾列表转过来的投产（未关联投产记录）
            cxScheduleEngineService.closeOutProductTask(cxPlanProductStatus);
            return AjaxResult.success();
        }else {
            //投产列表转过来的投产（存在投产记录）
            Long[] arr = Convert.toLongArray(cxPlanProductStatus.getIds());
            List<CxPlanProductStatus> list = cxPlanProductStatusService.seleteCxPlanProductStatusByIds(arr);
            list.forEach(aa -> {
                aa.setCxMachineCode(cxPlanProductStatus.getCxMachineCode());
                aa.setScheduleDate(cxPlanProductStatus.getScheduleDate());
                aa.setStorageLocation(cxPlanProductStatus.getStorageLocation());
                aa.setClass1PlanQty(cxPlanProductStatus.getClass1PlanQty());
                aa.setClass2PlanQty(cxPlanProductStatus.getClass2PlanQty());
                aa.setClass3PlanQty(cxPlanProductStatus.getClass3PlanQty());
                aa.setClass4PlanQty(cxPlanProductStatus.getClass4PlanQty());
                aa.setClass5PlanQty(cxPlanProductStatus.getClass5PlanQty());
            });
            for (CxPlanProductStatus production : list) {
                cxScheduleEngineService.productTask(production);
            }
            return AjaxResult.success();
        }
    }


    /**
     * 修改月度计划总量
     */
    @Log(title = "ui.data.column.productStatus.modalName", businessType = BusinessType.UPDATE)
    @PostMapping("/modifyQty")
    @Transactional
    public AjaxResult modifyQty(@RequestBody CxPlanProductStatus cxPlanProductStatus) throws NotFoundException {
        //此处调用重算汇总表接口
        AjaxResult ajaxResult=null;
        ajaxResult=mdmMonthPlanAmountSumService.updateCx(cxPlanProductStatus.getMonthPlanApsVersion(), cxPlanProductStatus.getSapCode(), cxPlanProductStatus.getEmbryoCode(), cxPlanProductStatus.getMonthPlanTotalModifyQty().intValue(), cxPlanProductStatus.getAdjustSource(), cxPlanProductStatus.getBomDataVersion());
        int code= (int)ajaxResult.get("code");
        if(HttpStatus.ERROR==code){
            return ajaxResult;
        }
        //更新备注
        cxPlanProductStatusService.updateCxPlanProductStatus(cxPlanProductStatus);
        //此处要改返回值
        return AjaxResult.success();
    }

    /**
     * 标记不投产
     */
    @Log(title = "ui.data.column.productStatus.modalName", businessType = BusinessType.UPDATE)
    @GetMapping("/markUnProduct/{ids}")
    public AjaxResult markUnProduct(@PathVariable("ids") Long[] ids) {
        return toAjax(cxPlanProductStatusService.markUnProduct(ids));
    }

    /**
     * 删除成型计划投产状态
     */
    //@PreAuthorize(hasPermi = "cx:productStatus:remove")
    @Log(title = "ui.data.column.productStatus.modalName", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(cxPlanProductStatusService.deleteCxPlanProductStatusByIds(ids));
    }

    /**
     * 导出成型计划投产状态列表
     */
    //@PreAuthorize(hasPermi = "cx:productStatus:export")
    @Log(title = "ui.data.column.productStatus.modalName", businessType = BusinessType.EXPORT)
    @PostMapping("/getList")
    public List<CxPlanProductStatus> getList(@RequestBody CxPlanProductStatus cxPlanProductStatus) {
        startPage();
        cxPlanProductStatus.setOrderStr(orderStr());
        return cxPlanProductStatusService.selectCxPlanProductStatusList(cxPlanProductStatus);
    }

    /**
     * 校验成型计划投产状态唯一性
     */
    @ApiOperation("校验成型计划投产状态唯一性")
    @PostMapping("/checkCxPlanProductStatusUnique")
    public String checkCxPlanProductStatusUnique(@RequestBody CxPlanProductStatus cxPlanProductStatus) {
        return cxPlanProductStatusService.checkCxPlanProductStatusUnique(cxPlanProductStatus);
    }

    /**
     * 修改投产计划表备注
     */
    @Log(title = "ui.data.column.productStatus.modalName", businessType = BusinessType.UPDATE)
    @PostMapping("/editRemark")
    public AjaxResult editRemark(@RequestBody CxPlanProductStatus cxPlanProductStatus) throws NotFoundException {
        //更新备注
        cxPlanProductStatusService.updateCxPlanProductStatus(cxPlanProductStatus);
        //此处要改返回值
        return AjaxResult.success();
    }

}

package com.zlt.aps.monthplan.factory.controller;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.PageUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoticeOrder;
import com.zlt.aps.monthplan.api.domain.vo.*;
import com.zlt.aps.monthplan.factory.helper.AdjustNoticeUtils;
import com.zlt.aps.monthplan.factory.service.IMonthPlanAdjustNoticeOrderService;
import com.zlt.common.controller.BusiController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanAdjustNoticeOrderController.java
 * 描    述：月计划调整通知单 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-05-21
 */
@Slf4j
@Api(tags = "月计划调整通知单后端服务接口")
@RestController
@RequestMapping("/monthPlanNoticeOrder")
@RequiredArgsConstructor
public class MonthPlanAdjustNoticeOrderController extends BusiController<MonthPlanNoticeOrder> {

    private final IMonthPlanAdjustNoticeOrderService monthPlanAdjustNoticeOrderService;

    /**
     * 查询月计划调整通知单列表
     */
    @ApiOperation("查询月计划调整通知单列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MonthPlanNoticeOrder queryVO) {
        try {
            startPage();
            List<MonthPlanNoticeOrder> list = monthPlanAdjustNoticeOrderService.selectList(queryVO);
            return getDataTable(list);
        } finally {
            PageUtils.clearPage();
        }
    }

    /**
     * 导入调整通知单
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.monthPlanNoticeOrder.modelName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入分厂计划调整通知单")
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.commonImport(importContext, updateSupport);
    }

    @Override
    protected AjaxResult doImportData(List<MonthPlanNoticeOrder> list, boolean updateSupport, long importLogId) {
        return monthPlanAdjustNoticeOrderService.importData(list, updateSupport, importLogId);
    }

    /**
     * 查询列表
     *
     * @param condition
     * @return
     */
    @ApiOperation("查询调整通知单调整明细列表")
    @PostMapping("/getAdjustDetail")
    TableDataInfo getAdjustDetail(@RequestBody MonthPlanNoticeOrder condition) {
        if (null == condition) {
            List<MonthPlanAdjustDetailVo> dataResult = new ArrayList<>();
            return getDataTable(dataResult);
        }
        String noticeNo = condition.getNoticeNo();
        if (StringUtils.isBlank(noticeNo)) {
            List<MonthPlanAdjustDetailVo> dataResult = new ArrayList<>();
            return getDataTable(dataResult);
        }
        List<MonthPlanAdjustDetailVo> dataResult = monthPlanAdjustNoticeOrderService.getNoticeDetail(noticeNo);
        return getDataTable(dataResult);
    }

    /**
     * 根据ID获取调整通知单明细信息
     *
     * @param id
     * @return
     */
    @PostMapping("/getMonthPlanNoticeInfo")
    @ApiOperation("根据ID获取调整通知单明细信息")
    public MonthPlanNoticeOrderVo getMonthPlanNoticeInfo(@RequestBody Long id) {
        if (null == id) {
            return new MonthPlanNoticeOrderVo();
        }
        return monthPlanAdjustNoticeOrderService.getMonthPlanNoticeInfo(id);
    }

    /**
     * 根据分厂，年月及SAP代码获取结余库存
     *
     * @param noticeOrder
     * @return
     */
    @PostMapping("/getStockInfo")
    @ApiOperation("根据查询条件获取结余库存信息")
    public MonthPlanNoticeOrderVo getMonthPlanNoticeStockInfo(@RequestBody MonthPlanNoticeOrder noticeOrder) {
        if (null == noticeOrder) {
            return new MonthPlanNoticeOrderVo();
        }
        return monthPlanAdjustNoticeOrderService.getMonthPlanNoticeStockInfo(noticeOrder);
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.monthPlanNoticeOrder.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    public AjaxResult save(@RequestBody MonthPlanNoticeOrder noticeOrder) {
        if (isEmpty(noticeOrder)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.param.checkError"));
        }
        return monthPlanAdjustNoticeOrderService.save(noticeOrder);
    }

    /**
     * 提交
     */
    @Log(title = "ui.data.column.monthPlanNoticeOrder.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("提交")
    @PostMapping("/submit")
    public AjaxResult submit(@RequestBody Long id) {
        return monthPlanAdjustNoticeOrderService.submit(id);
    }

    /**
     * 作废
     */
    @Log(title = "ui.data.column.monthPlanNoticeOrder.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("作废")
    @PostMapping("/cancel")
    public AjaxResult cancel(@RequestBody Long id) {
        return monthPlanAdjustNoticeOrderService.cancel(id);
    }

    /**
     * 调整通知单进行调整操作，得到调整信息数据
     */
    @ApiOperation("调整通知单进入调整操作接口数据")
    @PostMapping("/getAdjustNoticeAdjustPlan")
    public AjaxResult getAdjustNoticeAdjustPlan(@RequestBody MonthPlanNoticeOrder noticeOrderOperate) {
        if (null == noticeOrderOperate || StringUtils.isBlank(noticeOrderOperate.getNoticeNo())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.noticeOrder.noEmpty"));
        }
        return monthPlanAdjustNoticeOrderService.getAdjustNoticeAdjustPlan(noticeOrderOperate);
    }

    /**
     * 根据调整通知单的调整信息，获取需要调整的计划列表信息
     */
    @ApiOperation("根据调整通知单的调整信息，获取需要调整的计划列表信息")
    @PostMapping("/getOperatePlanList")
    public AjaxResult getOperatePlanList(@RequestBody MonthPlanAdjustNoticeOrderOperateVo noticeOrderOperate) {
        AjaxResult checkResult = checkParam(noticeOrderOperate);
        if (AjaxResult.Type.ERROR.value() == (Integer) checkResult.get(AjaxResult.CODE_TAG)) {
            return checkResult;
        }
        return monthPlanAdjustNoticeOrderService.getOperatePlanList(noticeOrderOperate);
    }

    /**
     * 根据调整通知单信息及调减计划，转换对应调增的数量
     *
     * @param param
     * @return
     */
    @PostMapping("/calculateAddQty")
    @ApiOperation("根据调整通知单信息及调减计划，转换对应调增的数量")
    public AjaxResult calculateAddQty(@RequestBody MonthPlanAdjustNoticeApplyOperateVo param) {
        MonthPlanAdjustNoticeAdjustApplyVo result = new MonthPlanAdjustNoticeAdjustApplyVo();
        result.setAddAdjustQty(BigDecimal.ZERO.longValue());
        MonthPlanNeedAdjustPlanVo applySubtract = param.getApplySubtract();
        if (null == param || null == applySubtract || StringUtils.isBlank(param.getNoticeNo())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.apply.param.noEmpty"));
        }
        if (StringUtils.isBlank(applySubtract.getProductionNo()) || null == applySubtract.getStartAdjustDate() || null == applySubtract.getNeedAdjustNumber()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.apply.subtractPlan.noEmpty"));
        }
        Long addQty = param.getAdjustNumber();
        if(addQty > BigDecimal.ZERO.longValue()) {
            if (StringUtils.isBlank(param.getSpecCode()) || StringUtils.isBlank(param.getMouldNo())) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.apply.addPlan.lackMessage"));
            }
        }
        return monthPlanAdjustNoticeOrderService.calculateAddQty(param);
    }

    /**
     * 对调整通知单执行调整V3
     */
    @ApiOperation("对调整通知单执行调整")
    @PostMapping("/executeAdjust")
    public AjaxResult executeAdjust(@RequestBody MonthPlanAdjustNoticeOrderOperateVo noticeOrderOperate) {
        AjaxResult checkResult = checkParam(noticeOrderOperate);
        if (AjaxResult.Type.ERROR.value() == (Integer) checkResult.get(AjaxResult.CODE_TAG)) {
            return checkResult;
        }
        Long adjustNumber = noticeOrderOperate.getAdjustNumber();
        if (adjustNumber < BigDecimal.ZERO.longValue()) {
            //如果是调减，则调减计划明细不可为空
            List<MonthPlanNeedAdjustPlanVo> confirmSubtractList = noticeOrderOperate.getConfirmSubtractList();
            if (CollectionUtils.isEmpty(confirmSubtractList)) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.adjustSubtract.noEmpty"));
            }
            for (MonthPlanNeedAdjustPlanVo subtractPlan : confirmSubtractList) {
                if (!AdjustNoticeUtils.isEffective(subtractPlan)) {
                    return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.adjustSubtract.noEmpty"));
                }
            }
        }
        return monthPlanAdjustNoticeOrderService.confirmAdjust(noticeOrderOperate);
    }

    /**
     * 调整通知单-确认调整
     * 采用直接对排产计划进行编辑的方式进行调整V4版本
     *
     * @param confirmAdjust
     * @return
     */
    @Deprecated
    @ApiOperation("对调整通知单采用排产计划编辑方式确认调整V4")
    @PostMapping("/confirmAdjust")
    public AjaxResult confirmAdjust(@RequestBody MonthPlanAdjustNoticeOrderConfirmOperateVo confirmAdjust) {
        if (null == confirmAdjust || StringUtils.isBlank(confirmAdjust.getNoticeNo()) || CollectionUtils.isEmpty(confirmAdjust.getAdjustPlanList())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.noticeOrder.param.noEmpty"));
        }
        return monthPlanAdjustNoticeOrderService.confirmAdjustByDetail(confirmAdjust);
    }

    /**
     * 参数校验
     * 调整通知单号不可为空
     * 调增数量不可为空或是零
     * 如果是调增，则模具号和规格代号不可为空
     *
     * @param noticeOrderOperate
     * @return
     */
    private AjaxResult checkParam(MonthPlanAdjustNoticeOrderOperateVo noticeOrderOperate) {
        if (null == noticeOrderOperate || StringUtils.isBlank(noticeOrderOperate.getNoticeNo())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.noticeOrder.noEmpty"));
        }
        Long adjustNumber = noticeOrderOperate.getAdjustNumber();
        if (null == adjustNumber || adjustNumber.equals(BigDecimal.ZERO.longValue())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.checkAdjustNumber"));
        }
        if (null == noticeOrderOperate.getStartDate()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.adjustDate.noEmpty"));
        }
        if (adjustNumber > BigDecimal.ZERO.longValue()) {
            if (StringUtils.isBlank(noticeOrderOperate.getMouldNo()) || StringUtils.isBlank(noticeOrderOperate.getSpecCode())) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.checkSpecCodeInfo"));
            }
        }
        return AjaxResult.success();
    }

    /**
     * 校验参数不可为空
     *
     * @param noticeOrder
     * @return
     */
    private boolean isEmpty(MonthPlanNoticeOrder noticeOrder) {
        if (null == noticeOrder) {
            return true;
        }
        if (StringUtils.isBlank(noticeOrder.getFactoryCode()) || StringUtils.isBlank(noticeOrder.getChannel()) || StringUtils.isBlank(noticeOrder.getLocationType())) {
            return true;
        }
        //调整数量及物料编码
        if (StringUtils.isBlank(noticeOrder.getProductCode()) || null == noticeOrder.getNeedQty() || BigDecimal.ZERO.longValue() == noticeOrder.getNeedQty()) {
            return true;
        }
        if (null == noticeOrder.getYear() || null == noticeOrder.getMonth()) {
            return true;
        }
        return false;
    }
}

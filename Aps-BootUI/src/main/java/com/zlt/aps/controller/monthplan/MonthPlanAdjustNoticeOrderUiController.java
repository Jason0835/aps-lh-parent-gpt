package com.zlt.aps.controller.monthplan;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoticeOrder;
import com.zlt.aps.monthplan.api.domain.vo.*;
import com.zlt.aps.monthplan.api.service.IMonthPlanAdjustNoticeOrderRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 分厂月计划调整通知单业务
 *
 * @author ZLT
 * @date 20250603
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/factory/monthPlanAdjustNotice")
@Api(tags = "分厂月生产计划调整业务(调整通知单)前端接口服务类-->ZLT")
public class MonthPlanAdjustNoticeOrderUiController extends BaseUIController<MonthPlanNoticeOrder> {

    private final IMonthPlanAdjustNoticeOrderRemoteService monthPlanAdjustNoticeOrderRemoteService;

    /**
     * 根据查询条件，查询调整通知单列表信息
     */
    @ResponseBody
    @PostMapping("/list")
    @RequiresPermissions("monthplan:adjustNotice:list")
    @ApiOperation("根据分厂、年份、月份获取计划调整控制信息")
    public TableDataInfo getMonthPlanAdjustControlInfo(MonthPlanNoticeOrder param) {
        return monthPlanAdjustNoticeOrderRemoteService.list(param);
    }

    /**
     * 根据调整通知单，获取调整通知单的调整明细
     */
    @ResponseBody
    @PostMapping("/getAdjustDetail")
    @ApiOperation("根据调整通知单，获取调整通知单的调整明细")
    public TableDataInfo getAdjustDetail(@RequestBody MonthPlanNoticeOrder param) {
        return monthPlanAdjustNoticeOrderRemoteService.getAdjustDetail(param);
    }

    @RequiresPermissions("monthplan:adjustNotice:import")
    @ApiOperation("调整通知单导入")
    @PostMapping({"/importData"})
    @ResponseBody
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = monthPlanAdjustNoticeOrderRemoteService.importData(context, false);
        return ajaxResult;
    }

    /**
     * 根据条件获取调整通知单编辑信息
     */
    @ResponseBody
    @PostMapping("/getInfo")
    @ApiOperation("根据条件获取调整通知单编辑信息")
    public MonthPlanNoticeOrderVo getMonthPlanNoticeInfo(@RequestBody MonthPlanNoticeOrder noticeOrder) {
        if (null == noticeOrder) {
            return new MonthPlanNoticeOrderVo();
        }
        Long id = noticeOrder.getId();
        if (null == id) {
            return new MonthPlanNoticeOrderVo();
        }
        return monthPlanAdjustNoticeOrderRemoteService.getMonthPlanNoticeInfo(id);
    }

    /**
     * 根据条件获取对应SAP的剩余库存信息
     */
    @ResponseBody
    @PostMapping("/getStockInfo")
    @ApiOperation("根据条件获取对应SAP的剩余库存信息")
    public MonthPlanNoticeOrderVo getMonthPlanNoticeStockInfo(@RequestBody MonthPlanNoticeOrder noticeOrder) {
        if (null == noticeOrder) {
            return new MonthPlanNoticeOrderVo();
        }
        if (StringUtils.isBlank(noticeOrder.getFactoryCode()) || null == noticeOrder.getYear() || null == noticeOrder.getMonth()) {
            return new MonthPlanNoticeOrderVo();
        }
        if (StringUtils.isBlank(noticeOrder.getProductCode())) {
            return new MonthPlanNoticeOrderVo();
        }
        return monthPlanAdjustNoticeOrderRemoteService.getMonthPlanNoticeStockInfo(noticeOrder);
    }

    /**
     * 修改保存
     *
     * @param monthPlanNoticeOrder
     * @return
     */
    @ResponseBody
    @PostMapping({"/save"})
    @RequiresPermissions("monthplan:adjustNotice:edit")
    @ApiOperation("调整通知单修改编辑-保存")
    public AjaxResult save(@RequestBody MonthPlanNoticeOrder monthPlanNoticeOrder) {
        return monthPlanAdjustNoticeOrderRemoteService.save(monthPlanNoticeOrder);
    }

    /**
     * 根据调整通知单，对调整通知单提交
     *
     * @param noticeOrder
     * @return
     */
    @ResponseBody
    @PostMapping({"/submit"})
    @RequiresPermissions("monthplan:adjustNotice:submit")
    @ApiOperation("调整通知单提交，增量则需要进行库存对冲-提交")
    public AjaxResult submit(@RequestBody MonthPlanNoticeOrder noticeOrder) {
        if (null == noticeOrder || null == noticeOrder.getId()) {
            //参数不可为空
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.param.noEmpty"));
        }
        return monthPlanAdjustNoticeOrderRemoteService.submit(noticeOrder.getId());
    }

    /**
     * 根据调整通知单，作废调整通知单
     *
     * @param noticeOrder
     * @return
     */
    @ResponseBody
    @PostMapping({"/cancel"})
    @RequiresPermissions("monthplan:adjustNotice:cancel")
    @ApiOperation("作废调整通知单，不进行调整")
    public AjaxResult cancel(@RequestBody MonthPlanNoticeOrder noticeOrder) {
        if (null == noticeOrder || null == noticeOrder.getId()) {
            //参数不可为空
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.param.noEmpty"));
        }
        return monthPlanAdjustNoticeOrderRemoteService.cancel(noticeOrder.getId());
    }

    /**
     * 调整通知单进入调整操作接口数据
     *
     * @param noticeOrderOperate
     * @return
     */
    @ResponseBody
    @ApiOperation("调整通知单进入调整操作接口数据")
    @PostMapping("/getAdjustNoticeAdjustPlan")
    AjaxResult getAdjustNoticeAdjustPlan(@RequestBody MonthPlanNoticeOrder noticeOrderOperate) {
        return monthPlanAdjustNoticeOrderRemoteService.getAdjustNoticeAdjustPlan(noticeOrderOperate);
    }

    /**
     * 根据调整通知单的调整信息，获取需要调整的计划列表信息
     *
     * @param noticeOrderOperate
     * @return
     */
    @ResponseBody
    @PostMapping("/getOperatePlanList")
    @ApiOperation("根据调整通知单的调整信息，获取需要调整的计划列表信息")
    AjaxResult getOperatePlanList(@RequestBody MonthPlanAdjustNoticeOrderOperateVo noticeOrderOperate) {
        return monthPlanAdjustNoticeOrderRemoteService.getOperatePlanList(noticeOrderOperate);
    }

    /**
     * 根据调整通知单信息及调减计划，转换对应调增的数量
     *
     * @param param
     * @return
     */
    @ResponseBody
    @PostMapping("/calculateAddQty")
    @ApiOperation("根据调整通知单信息及调减计划，转换对应调增的数量")
    AjaxResult calculateAddQty(@RequestBody MonthPlanAdjustNoticeApplyOperateVo param) {
        if (null == param || null == param.getApplySubtract() || StringUtils.isBlank(param.getNoticeNo())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.apply.param.noEmpty"));
        }
        return monthPlanAdjustNoticeOrderRemoteService.calculateAddQty(param);
    }

    /**
     * 对调整通知单执行调整
     *
     * @param noticeOrderOperate
     * @return
     */
    @ResponseBody
    @PostMapping("/executeAdjust")
    @ApiOperation("对调整通知单执行调整-V3版本")
    AjaxResult executeAdjust(@RequestBody MonthPlanAdjustNoticeOrderOperateVo noticeOrderOperate) {
        return monthPlanAdjustNoticeOrderRemoteService.executeAdjust(noticeOrderOperate);
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<MonthPlanNoticeOrderExcelTemplateVo> util = new ExcelUtil<>(MonthPlanNoticeOrderExcelTemplateVo.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @Override
    public String getExportTemplateFileName() {
        return getFunctionName();
    }


    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.importTemplateName");
    }

    /**
     * 对调整通知单进行调整操作
     *
     * @param confirmAdjust 调整通知单信息及调整计划信息
     * @return
     */
    @Deprecated
    @ResponseBody
    @ApiOperation("对调整通知单进行调整操作--V4版本作废使用V3版本")
    @RequiresPermissions("monthplan:adjustNotice:confirmAdjust")
    @PostMapping("/confirmAdjust")
    public AjaxResult adjustMonthPlan(@RequestBody MonthPlanAdjustNoticeOrderConfirmOperateVo confirmAdjust) {
        if (null == confirmAdjust || StringUtils.isBlank(confirmAdjust.getNoticeNo()) || CollectionUtils.isEmpty(confirmAdjust.getAdjustPlanList())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.noticeOrder.param.noEmpty"));
        }
        return monthPlanAdjustNoticeOrderRemoteService.confirmAdjust(confirmAdjust);
    }
}

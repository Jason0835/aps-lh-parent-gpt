package com.zlt.aps.controller.tm;

import com.alibaba.fastjson.JSON;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.tm.api.domain.dto.TmRollingRecalcRequestDTO;
import com.zlt.aps.tm.api.domain.dto.TmScheduleResultImportDTO;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.vo.*;
import com.zlt.aps.tm.api.service.ITmScheduleResultRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import com.zlt.framework.utils.AuthorizationUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 胎面排程结果表 页面控制层
 */
@Slf4j
@Api(tags = "胎面排程结果表")
@Controller
@RequestMapping("/tm/tmScheduleResult")
public class TmScheduleResultUIController extends BaseUIController<TmScheduleResult> {

    private final String prefix = "aps/tm/tmScheduleResult";

    @Autowired
    private ITmScheduleResultRemoteService iTmScheduleResultService;

    @RequiresPermissions("tm:tmScheduleResult:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/tmScheduleResult";
    }

    /**
     * 插单页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("tmScheduleResult", new TmScheduleResult());
        return prefix + "/add";
    }

    /**
     * 调量页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("tmScheduleResult", iTmScheduleResultService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TmScheduleResult query) {
        return iTmScheduleResultService.list(query);
    }

    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @ResponseBody
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(iTmScheduleResultService.getInfo(id));
    }

    @ApiOperation("保存")
    @PostMapping("/save")
    @RequiresPermissions("tm:tmScheduleResult:edit")
    @ResponseBody
    public AjaxResult save(TmScheduleResult tmScheduleResult) {
        return iTmScheduleResultService.save(tmScheduleResult);
    }

    @ApiOperation("删除")
    @PostMapping("/remove")
    @RequiresPermissions("tm:tmScheduleResult:remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTmScheduleResultService.removeByIds(Arrays.asList(arr));
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(TmScheduleResult query) {
        return iTmScheduleResultService.checkUnique(query);
    }

    /**
     * 校验胎面自动排程请求。
     *
     * @param request 自动排程请求
     * @return 校验结果
     */
    @ApiOperation("校验胎面自动排程")
    @PostMapping("/validateAutoPlan")
    @RequiresPermissions("tm:tmScheduleResult:autoPlan")
    @ResponseBody
    public AjaxResult validateAutoPlan(TmAutoScheduleRequestVo request) {
        return iTmScheduleResultService.validateAutoPlan(request);
    }

    /**
     * 执行胎面自动排程。
     *
     * @param request 自动排程请求
     * @return 自动排程结果
     */
    @ApiOperation("执行胎面自动排程")
    @PostMapping("/autoPlan")
    @RequiresPermissions("tm:tmScheduleResult:autoPlan")
    @ResponseBody
    public AjaxResult autoPlan(TmAutoScheduleRequestVo request) {
        return iTmScheduleResultService.autoPlan(request);
    }


    /**
     * 查询胎面自动排程任务状态。
     *
     * @param taskId 自动排程任务 ID
     * @return 任务状态和异常明细
     */
    @ApiOperation("查询胎面自动排程任务状态")
    @GetMapping("/autoPlan/task/{taskId}")
    @RequiresPermissions("tm:tmScheduleResult:autoPlan")
    @ResponseBody
    public AjaxResult getAutoPlanTask(@PathVariable("taskId") String taskId) {
        return iTmScheduleResultService.getAutoPlanTask(taskId);
    }

    /**
     * 查询最近胎面自动排程任务。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 最近任务状态
     */
    @ApiOperation("查询最近胎面自动排程任务")
    @GetMapping("/autoPlan/task/latest")
    @RequiresPermissions("tm:tmScheduleResult:autoPlan")
    @ResponseBody
    public AjaxResult getLatestAutoPlanTask(@RequestParam("factoryCode") String factoryCode,
                                            @RequestParam("scheduleDate") String scheduleDate) {
        return iTmScheduleResultService.getLatestAutoPlanTask(factoryCode, scheduleDate);
    }

    /**
     * 查询胎面排程看板。
     *
     * @param query 看板查询条件
     * @return 看板数据
     */
    @ApiOperation("查询胎面排程看板")
    @PostMapping("/board")
    @ResponseBody
    public AjaxResult board(TmScheduleResult query) {
        return iTmScheduleResultService.board(query);
    }

    /**
     * 查询胎面排程结果合计（库存合计与各班次计划量合计）。
     *
     * @param query 查询条件，与列表同口径
     * @return 库存合计与各班次计划量合计
     */
    @ApiOperation("查询胎面排程结果合计")
    @PostMapping("/summary")
    @ResponseBody
    public AjaxResult summary(TmScheduleResult query) {
        return iTmScheduleResultService.summary(query);
    }

    /**
     * 分页查询胎面未排任务。
     *
     * @param queryVO 工厂、排程日期、可选批次和分页条件
     * @return 未排任务分页结果
     */
    @ApiOperation("查询胎面未排任务")
    @PostMapping("/unplanned/list")
    @ResponseBody
    public TmScheduleUnplannedPageVo listUnplanned(@RequestBody TmScheduleUnplannedQueryVo queryVO) {
        return this.iTmScheduleResultService.listUnplanned(queryVO);
    }

    /**
     * 人工插入排程任务。
     *
     * @param requestVo 插单内容
     * @return 插单结果
     */
    @ApiOperation("人工插单")
    @PostMapping("/insertTask")
    @RequiresPermissions("tm:tmScheduleResult:add")
    @ResponseBody
    public AjaxResult insertTask(TmInsertTaskRequestVo requestVo) {
        return iTmScheduleResultService.insertTask(requestVo);
    }

    /**
     * 调整排程计划量。
     *
     * @param scheduleResult 调量内容
     * @return 调量结果
     */
    @ApiOperation("调整计划量")
    @PostMapping("/changeQty")
    @RequiresPermissions("tm:tmScheduleResult:edit")
    @ResponseBody
    public AjaxResult changeQty(TmScheduleResult scheduleResult) {
        return iTmScheduleResultService.changeQty(scheduleResult);
    }

    /**
     * 校验页面发布请求。
     *
     * @param ids 逗号分隔的排程结果 ID
     * @return 校验结果
     */
    @ApiOperation("校验胎面发布")
    @PostMapping("/publishValidate")
    @RequiresPermissions("tm:tmScheduleResult:publish")
    @ResponseBody
    public AjaxResult publishValidate(@RequestParam("ids") String ids) {
        Long[] idArray = Convert.toLongArray(ids);
        return iTmScheduleResultService.publishValidate(Arrays.asList(idArray));
    }

    /**
     * 将排程结果置为待发布。
     *
     * @param ids 逗号分隔的排程结果 ID
     * @return 发布状态变更结果
     */
    @ApiOperation("发布胎面排程")
    @PostMapping("/publish")
    @RequiresPermissions("tm:tmScheduleResult:publish")
    @ResponseBody
    public AjaxResult publish(@RequestParam("ids") String ids) {
        Long[] idArray = Convert.toLongArray(ids);
        return iTmScheduleResultService.publish(Arrays.asList(idArray));
    }

    /**
     * 批量转机台
     */
    @ApiOperation("批量转机台")
    @PostMapping("/batchChangeMachine/{machineCode}")
    @RequiresPermissions("tm:tmScheduleResult:changeMachine")
    @ResponseBody
    public AjaxResult batchChangeMachine(@PathVariable("machineCode") String machineCode, String selects) {
        List<TmScheduleResult> scheduleResultList = JSON.parseArray(selects, TmScheduleResult.class);
        return iTmScheduleResultService.batchChangeMachine(machineCode, scheduleResultList);
    }

    /**
     * 提交胎面人工插单异步任务。
     *
     * @param requestVo 插单内容
     * @return 初始任务
     */
    @ApiOperation("提交胎面人工插单异步任务")
    @PostMapping("/operation/insertTask")
    @RequiresPermissions("tm:tmScheduleResult:add")
    @ResponseBody
    public TmOperationTaskVo submitInsertTask(TmInsertTaskRequestVo requestVo) {
        return this.iTmScheduleResultService.submitInsertTask(requestVo);
    }

    /**
     * 提交胎面调量异步任务。
     *
     * @param scheduleResult 调量内容
     * @return 初始任务
     */
    @ApiOperation("提交胎面调量异步任务")
    @PostMapping("/operation/changeQty")
    @RequiresPermissions("tm:tmScheduleResult:edit")
    @ResponseBody
    public TmOperationTaskVo submitChangeQty(TmScheduleResult scheduleResult) {
        return this.iTmScheduleResultService.submitChangeQty(scheduleResult);
    }

    /**
     * 提交胎面单条转机台异步任务。
     *
     * @param scheduleResult 转机台内容
     * @return 初始任务
     */
    @ApiOperation("提交胎面单条转机台异步任务")
    @PostMapping("/operation/changeMachine")
    @RequiresPermissions("tm:tmScheduleResult:changeMachine")
    @ResponseBody
    public TmOperationTaskVo submitChangeMachine(TmScheduleResult scheduleResult) {
        return this.iTmScheduleResultService.submitChangeMachine(scheduleResult);
    }

    /**
     * 提交胎面批量转机台异步任务。
     *
     * @param machineCode 目标机台
     * @param selects 选中结果JSON
     * @return 初始任务
     */
    @ApiOperation("提交胎面批量转机台异步任务")
    @PostMapping("/operation/batchChangeMachine/{machineCode}")
    @RequiresPermissions("tm:tmScheduleResult:changeMachine")
    @ResponseBody
    public TmOperationTaskVo submitBatchChangeMachine(@PathVariable("machineCode") String machineCode,
                                                      String selects) {
        List<TmScheduleResult> scheduleResultList = JSON.parseArray(selects, TmScheduleResult.class);
        return this.iTmScheduleResultService.submitBatchChangeMachine(machineCode, scheduleResultList);
    }

    /**
     * 提交胎面删除异步任务。
     *
     * @param ids 逗号分隔结果ID
     * @return 初始任务
     */
    @ApiOperation("提交胎面删除异步任务")
    @PostMapping("/operation/remove")
    @RequiresPermissions("tm:tmScheduleResult:remove")
    @ResponseBody
    public TmOperationTaskVo submitRemove(@RequestParam String ids) {
        return this.iTmScheduleResultService.submitRemove(Arrays.asList(Convert.toLongArray(ids)));
    }

    /**
     * 提交胎面发布异步任务。
     *
     * @param ids 逗号分隔结果ID
     * @return 初始任务
     */
    @ApiOperation("提交胎面发布异步任务")
    @PostMapping("/operation/publish")
    @RequiresPermissions("tm:tmScheduleResult:publish")
    @ResponseBody
    public TmOperationTaskVo submitPublish(@RequestParam("ids") String ids) {
        return this.iTmScheduleResultService.submitPublish(Arrays.asList(Convert.toLongArray(ids)));
    }

    /**
     * 查询胎面人工操作任务。
     *
     * @param taskId 任务编号
     * @return 任务状态
     */
    @ApiOperation("查询胎面人工操作任务")
    @GetMapping("/operation/task/{taskId}")
    @ResponseBody
    public TmOperationTaskVo getOperationTask(@PathVariable("taskId") String taskId) {
        return this.iTmScheduleResultService.getOperationTask(taskId);
    }

    /**
     * 查询最近胎面人工操作任务。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 最近任务
     */
    @ApiOperation("查询最近胎面人工操作任务")
    @GetMapping("/operation/task/latest")
    @ResponseBody
    public TmOperationTaskVo getLatestOperationTask(@RequestParam("factoryCode") String factoryCode,
                                                    @RequestParam("scheduleDate") String scheduleDate) {
        return this.iTmScheduleResultService.getLatestOperationTask(factoryCode, scheduleDate);
    }

    /**
     * 手动触发胎面自动滚动重算，不增加页面按钮。
     *
     * @param request 工厂、排程日期和目标班次
     * @return 滚动重算统计
     */
    @ApiOperation("胎面自动滚动重算")
    @PostMapping("/rollingRecalc")
    @RequiresPermissions("tm:tmScheduleResult:autoPlan")
    @ResponseBody
    public AjaxResult rollingRecalc(TmRollingRecalcRequestDTO request) {
        request.setOperator(AuthorizationUtils.getLoginName());
        return iTmScheduleResultService.rollingRecalc(request);
    }

    @ApiOperation("导出数据")
    @GetMapping("/export")
    @RequiresPermissions("tm:tmScheduleResult:export")
    public void export(HttpServletResponse response, TmScheduleResult entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.tm.scheduleResult.modelName");
        byte[] excelBytes = iTmScheduleResultService.exportDataScheduleResult(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @ApiOperation("导入数据")
    @PostMapping("/importDataCust")
    @RequiresPermissions("tm:tmScheduleResult:import")
    @ResponseBody
    public AjaxResult importData(@RequestParam("file") MultipartFile file,
                                 @RequestParam("factoryCode") String factoryCode,
                                 @RequestParam("scheduleDate")
                                 @DateTimeFormat(pattern = "yyyy-MM-dd") Date scheduleDate,
                                 @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(I18nUtil.getMessage("ui.data.column.tm.scheduleResult.modelName"));
        context.setProcedureCode(I18nUtil.getMessage("ui.data.column.tm.scheduleResult.modelName"));
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        TmScheduleResult condition = new TmScheduleResult();
        condition.setFactoryCode(factoryCode);
        condition.setScheduleDate(scheduleDate);
        TmScheduleResultImportDTO importDTO = new TmScheduleResultImportDTO();
        importDTO.setImportContext(context);
        importDTO.setScheduleResult(condition);
        return iTmScheduleResultService.importDataScheduleResult(importDTO, updateSupport);
    }

    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplateCust")
    @RequiresPermissions("tm:tmScheduleResult:import")
    public void importTemplate(HttpServletResponse response, TmScheduleResult entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.tm.scheduleResult.modelName");
        entity.setExportTemplate(Boolean.TRUE);
        byte[] excelBytes = iTmScheduleResultService.exportDataScheduleResult(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 获取胎面排程班次日期列表
     *
     * @param scheduleDate 排程日期
     * @return 班次日期列表
     */
    @ApiOperation("获取胎面排程班次日期列表")
    @PostMapping("/listScheduleShiftDates")
    @ResponseBody
    public AjaxResult listScheduleShiftDates(Date scheduleDate) {
        TmScheduleResult scheduleResult = new TmScheduleResult();
        scheduleResult.setScheduleDate(scheduleDate);
        List<TmScheduleShiftDateVO> list = iTmScheduleResultService.listScheduleShiftDates(scheduleResult);
        return AjaxResult.success(list);
    }

    @RequiresRoles("admin")
    @ApiOperation("更改发布状态")
    @PostMapping("/changeReleaseStatus")
    @ResponseBody
    public AjaxResult changeReleaseStatus(@RequestParam("ids") String ids, @RequestParam("isRelease") String isRelease) {
        return iTmScheduleResultService.changeReleaseStatus(ids, isRelease);
    }
}

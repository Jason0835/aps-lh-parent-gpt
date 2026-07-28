package com.zlt.aps.controller.tc;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.tc.api.domain.dto.TcScheduleResultImportDTO;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResultExplain;
import com.zlt.aps.tc.api.domain.vo.*;
import com.zlt.aps.tc.api.service.ITcScheduleResultRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * 胎侧排程结果表 页面控制层。
 *
 * <p>承接前端 {@code /tc/tcScheduleResult/**} 请求，经 Feign 调用 aps-tc 后端
 * {@code TcScheduleResultController}，返回值原样透传（查询接口返回领域 VO，
 * 写操作返回 {@link AjaxResult}），由前端响应拦截器统一处理。</p>
 */
@Slf4j
@Api(tags = "胎侧排程结果表")
@Controller
@RequestMapping("/tc/tcScheduleResult")
public class TcScheduleResultUIController extends BaseUIController<TcScheduleResult> {

    @Autowired
    private ITcScheduleResultRemoteService iTcScheduleResultService;

    /**
     * 按胎侧专用模板导出单日排程结果。
     *
     * @param response HTTP 响应
     * @param entity 工厂和单日排程条件
     * @throws IOException 响应流写入失败时抛出
     */
    @ApiOperation("导出胎侧排程结果")
    @GetMapping("/export")
    @RequiresPermissions("tc:tcScheduleResult:export")
    public void export(HttpServletResponse response, TcScheduleResult entity) throws IOException {
        String fileName = com.ruoyi.common.i18n.utils.I18nUtil.getMessage(
                "ui.tc.schedule.scheduleResult.modelName");
        byte[] excelBytes = this.iTcScheduleResultService.exportDataScheduleResult(entity, fileName);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(excelBytes)) {
            ExcelUtil.setResponseHeader(response, fileName, ExcelUtil.XLSX_FILE);
            IOUtils.copy(inputStream, response.getOutputStream());
            response.flushBuffer();
        }
    }

    /**
     * 下载指定工厂和日期的胎侧空模板。
     *
     * @param response HTTP 响应
     * @param entity 工厂和排程日期
     * @throws IOException 响应流写入失败时抛出
     */
    @ApiOperation("下载胎侧排程导入模板")
    @GetMapping("/importTemplateCust")
    @RequiresPermissions("tc:tcScheduleResult:import")
    public void importTemplate(HttpServletResponse response, TcScheduleResult entity) throws IOException {
        entity.setExportTemplate(Boolean.TRUE);
        this.export(response, entity);
    }

    /**
     * 按胎侧专用模板导入排程结果。
     *
     * @param file xlsx 文件
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param updateSupport 是否允许覆盖更新
     * @return 导入结果和行级错误
     * @throws Exception 文件解密、上传日志或远程调用失败时抛出
     */
    @ApiOperation("导入胎侧排程结果")
    @PostMapping("/importDataCust")
    @RequiresPermissions("tc:tcScheduleResult:import")
    @ResponseBody
    public AjaxResult importData(@RequestParam("file") MultipartFile file,
                                 @RequestParam("factoryCode") String factoryCode,
                                 @RequestParam("scheduleDate")
                                 @DateTimeFormat(pattern = "yyyy-MM-dd") Date scheduleDate,
                                 @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        byte[] fileBytes = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportContext importContext = new ImportContext();
        importContext.setImportFilePath(this.importFilePath);
        importContext.setFunctionName(com.ruoyi.common.i18n.utils.I18nUtil.getMessage(
                "ui.tc.schedule.scheduleResult.modelName"));
        importContext.setProcedureCode("tcScheduleResult");
        importContext.setOriFileName(file.getOriginalFilename());
        importContext.setFileBytes(fileBytes);
        TcScheduleResult condition = new TcScheduleResult();
        condition.setFactoryCode(factoryCode);
        condition.setScheduleDate(scheduleDate);
        TcScheduleResultImportDTO importDTO = new TcScheduleResultImportDTO();
        importDTO.setImportContext(importContext);
        importDTO.setScheduleResult(condition);
        return this.iTcScheduleResultService.importDataScheduleResult(importDTO, updateSupport);
    }

    /**
     * 查询胎侧排程平铺看板。
     *
     * @param queryVO 看板查询条件
     * @return 已排分页、日期列、批次、汇总和未排数量
     */
    @ApiOperation("查询胎侧排程看板")
    @PostMapping("/board")
    @ResponseBody
    public TcScheduleBoardVo board(@RequestBody TcScheduleBoardQueryVo queryVO) {
        return iTcScheduleResultService.board(queryVO);
    }

    /**
     * 分页查询当前有效批次未排任务。
     *
     * @param queryVO 看板查询条件
     * @return 未排任务分页
     */
    @ApiOperation("查询胎侧未排任务")
    @PostMapping("/unplanned/list")
    @ResponseBody
    public TcScheduleUnplannedPageVo listUnplanned(@RequestBody TcScheduleBoardQueryVo queryVO) {
        return iTcScheduleResultService.listUnplanned(queryVO);
    }

    /**
     * 懒加载已排结果解释。
     *
     * @param resultId 排程结果 ID
     * @return 解释明细
     */
    @ApiOperation("查询胎侧已排结果解释")
    @GetMapping("/explain/result/{resultId}")
    @ResponseBody
    public List<TcScheduleResultExplain> listResultExplain(@PathVariable("resultId") Long resultId) {
        return iTcScheduleResultService.listResultExplain(resultId);
    }

    /**
     * 懒加载未排任务解释。
     *
     * @param unplannedId 未排任务 ID
     * @return 解释明细
     */
    @ApiOperation("查询胎侧未排任务解释")
    @GetMapping("/explain/unplanned/{unplannedId}")
    @ResponseBody
    public List<TcScheduleResultExplain> listUnplannedExplain(@PathVariable("unplannedId") Long unplannedId) {
        return iTcScheduleResultService.listUnplannedExplain(unplannedId);
    }

    /**
     * 查询人工插单和普通转机可用选项。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期(yyyy-MM-dd)
     * @return 施工、机台和六班选项
     */
    @ApiOperation("查询胎侧人工排程选项")
    @GetMapping("/manual/options")
    @ResponseBody
    public TcManualOptionsVo manualOptions(@RequestParam("factoryCode") String factoryCode,
                                           @RequestParam("scheduleDate") String scheduleDate) {
        return iTcScheduleResultService.manualOptions(factoryCode, scheduleDate);
    }

    /**
     * 执行胎侧人工插单。
     *
     * @param requestVO 插单请求
     * @return 新增结果行数
     */
    @ApiOperation("胎侧人工插单")
    @PostMapping("/insertTask")
    @RequiresPermissions("tc:tcScheduleResult:add")
    @ResponseBody
    public AjaxResult insertTask(@RequestBody TcInsertTaskRequestVo requestVO) {
        return iTcScheduleResultService.insertTask(requestVO);
    }

    /**
     * 调整胎侧选中班次计划量。
     *
     * @param requestVO 调量请求
     * @return 受影响行数
     */
    @ApiOperation("调整胎侧班次计划量")
    @PostMapping("/changeQty")
    @RequiresPermissions("tc:tcScheduleResult:edit")
    @ResponseBody
    public AjaxResult changeQty(@RequestBody TcChangeQtyRequestVo requestVO) {
        return iTcScheduleResultService.changeQty(requestVO);
    }

    /**
     * 原子批量执行胎侧普通转机台。
     *
     * @param requestVO 转机台请求
     * @return 受影响行数
     */
    @ApiOperation("胎侧普通转机台")
    @PostMapping("/changeMachine")
    @RequiresPermissions("tc:tcScheduleResult:changeMachine")
    @ResponseBody
    public AjaxResult changeMachine(@RequestBody TcChangeMachineRequestVo requestVO) {
        return iTcScheduleResultService.changeMachine(requestVO);
    }

    /**
     * 按结果 ID 整行删除胎侧六班排程结果。
     *
     * @param resultIdList 排程结果 ID
     * @return 删除行数
     */
    @ApiOperation("整行删除胎侧排程结果")
    @DeleteMapping("/remove")
    @RequiresPermissions("tc:tcScheduleResult:remove")
    @ResponseBody
    public AjaxResult remove(@RequestBody List<Long> resultIdList) {
        return iTcScheduleResultService.remove(resultIdList);
    }

    /**
     * 提交胎侧人工插单异步任务。
     *
     * @param requestVO 插单请求
     * @return 初始任务
     */
    @ApiOperation("提交胎侧人工插单异步任务")
    @PostMapping("/operation/insertTask")
    @RequiresPermissions("tc:tcScheduleResult:add")
    @ResponseBody
    public TcOperationTaskVo submitInsertTask(@RequestBody TcInsertTaskRequestVo requestVO) {
        return this.iTcScheduleResultService.submitInsertTask(requestVO);
    }

    /**
     * 提交胎侧调量异步任务。
     *
     * @param requestVO 调量请求
     * @return 初始任务
     */
    @ApiOperation("提交胎侧调量异步任务")
    @PostMapping("/operation/changeQty")
    @RequiresPermissions("tc:tcScheduleResult:edit")
    @ResponseBody
    public TcOperationTaskVo submitChangeQty(@RequestBody TcChangeQtyRequestVo requestVO) {
        return this.iTcScheduleResultService.submitChangeQty(requestVO);
    }

    /**
     * 提交胎侧单条或批量转机台异步任务。
     *
     * @param requestVO 转机台请求
     * @return 初始任务
     */
    @ApiOperation("提交胎侧转机台异步任务")
    @PostMapping("/operation/changeMachine")
    @RequiresPermissions("tc:tcScheduleResult:changeMachine")
    @ResponseBody
    public TcOperationTaskVo submitChangeMachine(@RequestBody TcChangeMachineRequestVo requestVO) {
        return this.iTcScheduleResultService.submitChangeMachine(requestVO);
    }

    /**
     * 提交胎侧删除异步任务。
     *
     * @param resultIdList 结果ID
     * @return 初始任务
     */
    @ApiOperation("提交胎侧删除异步任务")
    @DeleteMapping("/operation/remove")
    @RequiresPermissions("tc:tcScheduleResult:remove")
    @ResponseBody
    public TcOperationTaskVo submitRemove(@RequestBody List<Long> resultIdList) {
        return this.iTcScheduleResultService.submitRemove(resultIdList);
    }

    /**
     * 查询胎侧人工操作任务。
     *
     * @param taskId 任务编号
     * @return 任务状态
     */
    @ApiOperation("查询胎侧人工操作任务")
    @GetMapping("/operation/task/{taskId}")
    @ResponseBody
    public TcOperationTaskVo getOperationTask(@PathVariable("taskId") String taskId) {
        return this.iTcScheduleResultService.getOperationTask(taskId);
    }

    /**
     * 查询最近胎侧人工操作任务。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 最近任务
     */
    @ApiOperation("查询最近胎侧人工操作任务")
    @GetMapping("/operation/task/latest")
    @ResponseBody
    public TcOperationTaskVo getLatestOperationTask(@RequestParam("factoryCode") String factoryCode,
                                                    @RequestParam("scheduleDate") String scheduleDate) {
        return this.iTcScheduleResultService.getLatestOperationTask(factoryCode, scheduleDate);
    }

    /**
     * 校验自动排程请求及旧结果覆盖条件。
     *
     * @param request 自动排程请求
     * @return 校验响应
     */
    @ApiOperation("校验胎侧自动排程请求")
    @PostMapping("/validateAutoPlan")
    @RequiresPermissions("tc:tcScheduleResult:autoPlan")
    @ResponseBody
    public TcAutoScheduleResponseVo validateAutoPlan(@RequestBody TcAutoScheduleRequestVo request) {
        return iTcScheduleResultService.validateAutoPlan(request);
    }

    /**
     * 提交胎侧自动排程异步任务。
     *
     * @param request 自动排程请求
     * @return 待执行任务响应
     */
    @ApiOperation("提交胎侧自动排程任务")
    @PostMapping("/autoPlan")
    @RequiresPermissions("tc:tcScheduleResult:autoPlan")
    @ResponseBody
    public TcAutoScheduleResponseVo autoPlan(@RequestBody TcAutoScheduleRequestVo request) {
        return iTcScheduleResultService.autoPlan(request);
    }

    /**
     * 查询指定胎侧自动排程任务。
     *
     * @param taskId 对外任务编号
     * @return 任务进度和结果摘要
     */
    @ApiOperation("查询胎侧自动排程任务")
    @GetMapping("/autoPlan/task/{taskId}")
    @RequiresPermissions("tc:tcScheduleResult:autoPlan")
    @ResponseBody
    public TcAutoScheduleResponseVo getAutoPlanTask(@PathVariable("taskId") String taskId) {
        return iTcScheduleResultService.getAutoPlanTask(taskId);
    }

    /**
     * 查询指定工厂和排程日期最近一次任务。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期(yyyy-MM-dd)
     * @return 最近任务进度和结果摘要
     */
    @ApiOperation("查询最近一次胎侧自动排程任务")
    @GetMapping("/autoPlan/task/latest")
    @RequiresPermissions("tc:tcScheduleResult:autoPlan")
    @ResponseBody
    public TcAutoScheduleResponseVo getLatestAutoPlanTask(@RequestParam("factoryCode") String factoryCode,
                                                          @RequestParam("scheduleDate") String scheduleDate) {
        return iTcScheduleResultService.getLatestAutoPlanTask(factoryCode, scheduleDate);
    }

    /**
     * 清理胎侧自动排程 Redis 基础资料缓存。
     *
     * @param factoryCode 工厂编码，为空时清理全部胎侧自动排程缓存
     * @param scheduleDate 排程日期(yyyy-MM-dd)
     * @return 清理数量
     */
    @ApiOperation("清理胎侧自动排程Redis缓存")
    @PostMapping("/clearAutoPlanRedisCache")
    @RequiresPermissions("tc:tcScheduleResult:autoPlan")
    @ResponseBody
    public AjaxResult clearAutoPlanRedisCache(
            @RequestParam(value = "factoryCode", required = false) String factoryCode,
            @RequestParam(value = "scheduleDate", required = false) String scheduleDate) {
        return iTcScheduleResultService.clearAutoPlanRedisCache(factoryCode, scheduleDate);
    }

    /**
     * 校验所选胎侧结果是否允许发布。
     *
     * @param requestVO 发布请求
     * @return 校验结果
     */
    @ApiOperation("校验胎侧排程发布")
    @PostMapping("/release/validate")
    @RequiresPermissions("tc:tcScheduleResult:publish")
    @ResponseBody
    public TcReleaseValidateVo validateRelease(@RequestBody TcReleaseRequestVo requestVO) {
        return iTcScheduleResultService.validateRelease(requestVO);
    }

    /**
     * 创建胎侧排程异步发布任务。
     *
     * @param requestVO 发布请求
     * @return 发布任务
     */
    @ApiOperation("发布胎侧排程结果")
    @PostMapping("/release")
    @RequiresPermissions("tc:tcScheduleResult:publish")
    @ResponseBody
    public TcReleaseTaskVo release(@RequestBody TcReleaseRequestVo requestVO) {
        return iTcScheduleResultService.release(requestVO);
    }

    /**
     * 查询指定发布任务。
     *
     * @param taskId 发布任务ID
     * @return 发布任务
     */
    @ApiOperation("查询胎侧发布任务")
    @GetMapping("/release/task/{taskId}")
    @RequiresPermissions("tc:tcScheduleResult:publish")
    @ResponseBody
    public TcReleaseTaskVo getReleaseTask(@PathVariable("taskId") String taskId) {
        return iTcScheduleResultService.getReleaseTask(taskId);
    }

    /**
     * 查询指定工厂日期最近发布任务。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期(yyyy-MM-dd)
     * @return 最近发布任务
     */
    @ApiOperation("查询最近胎侧发布任务")
    @GetMapping("/release/task/latest")
    @RequiresPermissions("tc:tcScheduleResult:publish")
    @ResponseBody
    public TcReleaseTaskVo getLatestReleaseTask(@RequestParam("factoryCode") String factoryCode,
                                                @RequestParam("scheduleDate") String scheduleDate) {
        return iTcScheduleResultService.getLatestReleaseTask(factoryCode, scheduleDate);
    }
}

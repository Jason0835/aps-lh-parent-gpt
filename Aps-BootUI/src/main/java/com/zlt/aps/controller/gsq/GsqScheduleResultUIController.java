package com.zlt.aps.controller.gsq;

import cn.hutool.core.convert.Convert;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.gsq.api.domain.dto.GsqScheduleResultImportDTO;
import com.zlt.aps.gsq.api.domain.entity.GsqScheduleResult;
import com.zlt.aps.gsq.api.domain.vo.GsqScheduleShiftDateVO;
import com.zlt.aps.gsq.api.service.IGsqScheduleResultService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
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
 * 钢丝圈排程结果UIController
 *
 * <p>6班次制：1班=D日中班，2班=D+1日夜班，3班=D+1日早班，4班=D+1日中班，5班=D+2日夜班，6班=D+2日早班
 * 其中 D+1 = 排程日期（SCHEDULE_DATE）
 *
 * @author APS
 */
@Slf4j
@Controller
@RequestMapping("/gsq/scheduleResult")
@Api(tags = {"钢丝圈排程结果界面接口"})
public class GsqScheduleResultUIController extends BaseUIController<GsqScheduleResult> {

    private final String prefix = "gsq/scheduleResult";

    @Autowired
    private IGsqScheduleResultService iGsqScheduleResultService;

    /**
     * 跳转到钢丝圈排程结果首页
     */
    @RequiresPermissions("gsq:scheduleResult:view")
    @GetMapping()
    @ApiOperation("跳转到钢丝圈排程结果首页")
    public String toIndex() {
        return prefix + "/scheduleResult";
    }

    /**
     * 查询钢丝圈排程结果列表
     */
    @RequiresPermissions("gsq:scheduleResult:list")
    @PostMapping("/list")
    @ResponseBody
    @ApiOperation("查询钢丝圈排程结果列表")
    public TableDataInfo list(GsqScheduleResult entity) {
        return iGsqScheduleResultService.list(entity);
    }

    /**
     * 跳转至编辑页面
     */
    @GetMapping(value = "/edit/{id}")
    @ApiOperation("获取钢丝圈排程结果详细信息,跳转到编辑页面")
    public String getInfo(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("scheduleResult", iGsqScheduleResultService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 新增钢丝圈排程结果
     */
    @RequiresPermissions("gsq:scheduleResult:add")
    @PostMapping("/add")
    @ResponseBody
    @ApiOperation("新增钢丝圈排程结果")
    public AjaxResult add(GsqScheduleResult entity) {
        return iGsqScheduleResultService.save(entity);
    }

    /**
     * 修改钢丝圈排程结果
     */
    @RequiresPermissions("gsq:scheduleResult:edit")
    @PostMapping("/edit")
    @ResponseBody
    @ApiOperation("修改钢丝圈排程结果")
    public AjaxResult edit(GsqScheduleResult entity) {
        return iGsqScheduleResultService.save(entity);
    }

    /**
     * 删除钢丝圈排程结果
     */
    @RequiresPermissions("gsq:scheduleResult:remove")
    @PostMapping("/remove")
    @ResponseBody
    @ApiOperation("删除钢丝圈排程结果")
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iGsqScheduleResultService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 逻辑删除排程记录（已发布成功的计划不允许删除）
     */
    @RequiresPermissions("gsq:scheduleResult:remove")
    @PostMapping("/logicDelete")
    @ResponseBody
    @ApiOperation("逻辑删除排程记录（已发布成功的计划不允许删除）")
    public AjaxResult logicDelete(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iGsqScheduleResultService.logicDelete(Arrays.asList(arr));
    }

    /**
     * 获取钢丝圈排程班次日期列表
     * 钢丝圈排程6个班次覆盖D日中班、D+1日夜早中、D+2日夜早：
     * 班次1：D日中班，班次2~4：D+1日(夜/早/中)，班次5~6：D+2日(夜/早)
     */
    @ApiOperation("获取钢丝圈排程班次日期列表")
    @PostMapping("/listScheduleShiftDates")
    @ResponseBody
    public AjaxResult listScheduleShiftDates(GsqScheduleResult queryVO) {
        List<GsqScheduleShiftDateVO> list = iGsqScheduleResultService.listScheduleShiftDates(queryVO);
        return AjaxResult.success(list);
    }

    /**
     * 自动排程
     */
    @RequiresPermissions("gsq:scheduleResult:autoPlan")
    @PostMapping("/autoPlan")
    @ResponseBody
    @ApiOperation("自动排程")
    public AjaxResult autoPlan(GsqScheduleResult entity) {
        return iGsqScheduleResultService.autoPlan(entity);
    }

    /**
     * 插单
     */
//    @RequiresPermissions("gsq:scheduleResult:insertOrder")
//    @PostMapping("/insertOrder")
//    @ResponseBody
//    @ApiOperation("插单")
//    public AjaxResult insertOrder(GsqScheduleResult entity) {
//        return iGsqScheduleResultService.insertOrder(entity);
//    }
//
//    /**
//     * 转机台
//     */
//    @RequiresPermissions("gsq:scheduleResult:changeMachine")
//    @PostMapping("/changeMachine")
//    @ResponseBody
//    @ApiOperation("转机台")
//    public AjaxResult changeMachine(GsqScheduleResult entity) {
//        return iGsqScheduleResultService.changeMachine(entity);
//    }

    /**
     * 调量
     */
    @RequiresPermissions("gsq:scheduleResult:adjustQty")
    @PostMapping("/changeQty")
    @ResponseBody
    @ApiOperation("调量")
    public AjaxResult changeQty(GsqScheduleResult entity) {
        return iGsqScheduleResultService.changeQty(entity);
    }

    /**
     * 发布排程
     */
    @RequiresPermissions("gsq:scheduleResult:release")
    @PostMapping("/publish")
    @ResponseBody
    @ApiOperation("发布排程")
    public AjaxResult publish(GsqScheduleResult entity) {
        return iGsqScheduleResultService.publish(entity);
    }

    /**
     * 查询排程日期是否已发布
     */
    @PostMapping("/isPublish")
    @ResponseBody
    @ApiOperation("查询排程日期是否已发布")
    public AjaxResult isPublish(GsqScheduleResult entity) {
        return AjaxResult.success(iGsqScheduleResultService.isPublish(entity));
    }

    /**
     * 唯一性校验
     */
    @PostMapping("/checkUnique")
    @ResponseBody
    @ApiOperation("唯一性校验")
    public AjaxResult checkUnique(GsqScheduleResult entity) {
        return AjaxResult.success(iGsqScheduleResultService.checkUnique(entity));
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     *
     * @return 导出模板文件名
     */
    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }

    /**
     * 继承时重写方法，返回业务模块编码。
     *
     * @return 业务模块编码
     */
    @Override
    public String getProcedureCode() {
        return "GSQ";
    }

    /**
     * 继承时重写方法，返回功能名称。
     *
     * @return 功能名称
     */
    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.gsqScheduleResult.modelName");
    }

    /**
     * 数据导出（按专用模板导出钢丝圈排程结果，需传工厂和排程日期）。
     *
     * @param response 响应对象
     * @param entity   查询条件（含工厂、排程日期）
     * @throws IOException 流写入失败时抛出
     */
    @ApiOperation("数据导出")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, GsqScheduleResult entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iGsqScheduleResultService.exportDataScheduleResult(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 按专用模板导入钢丝圈排程结果。
     *
     * @param file          上传的 Excel 模板文件
     * @param factoryCode   工厂编码
     * @param scheduleDate  排程日期
     * @param updateSupport 是否允许覆盖更新
     * @return 导入结果
     * @throws Exception 文件解析或日志处理失败时抛出
     */
    @ApiOperation("按专用模板导入钢丝圈排程结果")
    @PostMapping("/importDataCust")
    @RequiresPermissions("gsq:scheduleResult:import")
    @ResponseBody
    public AjaxResult importData(@RequestParam("file") MultipartFile file,
                                 @RequestParam("factoryCode") String factoryCode,
                                 @RequestParam("scheduleDate")
                                 @DateTimeFormat(pattern = "yyyy-MM-dd") Date scheduleDate,
                                 @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(I18nUtil.getMessage("ui.data.column.gsqScheduleResult.modelName"));
        context.setProcedureCode(I18nUtil.getMessage("ui.data.column.gsqScheduleResult.modelName"));
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        GsqScheduleResult condition = new GsqScheduleResult();
        condition.setFactoryCode(factoryCode);
        condition.setScheduleDate(scheduleDate);
        GsqScheduleResultImportDTO importDTO = new GsqScheduleResultImportDTO();
        importDTO.setImportContext(context);
        importDTO.setScheduleResult(condition);
        return iGsqScheduleResultService.importDataScheduleResult(importDTO, updateSupport);
    }

    /**
     * 下载导入模板（按专用模板生成空模板）。
     *
     * @param response 响应对象
     * @param entity   工厂和排程日期条件
     * @throws IOException 流写入失败时抛出
     */
    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplateCust")
    @RequiresPermissions("gsq:scheduleResult:import")
    public void importTemplate(HttpServletResponse response, GsqScheduleResult entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.gsqScheduleResult.modelName");
        byte[] excelBytes = iGsqScheduleResultService.exportDataScheduleResult(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }
}

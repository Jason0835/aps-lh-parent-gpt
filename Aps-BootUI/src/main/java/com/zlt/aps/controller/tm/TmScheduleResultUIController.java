package com.zlt.aps.controller.tm;

import com.alibaba.fastjson.JSON;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.vo.TmScheduleShiftDateVO;
import com.zlt.aps.tm.api.service.ITmScheduleResultRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
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
    public String checkUnique(@RequestBody TmScheduleResult query) {
        return iTmScheduleResultService.checkUnique(query);
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
        TmScheduleResult query = new TmScheduleResult();
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        for (TmScheduleResult scheduleResult : scheduleResultList) {
            query.setId(scheduleResult.getId());
            query.setScheduleDate(scheduleResult.getScheduleDate());
            query.setMachineCode(machineCode);
            query.setTreadCode(scheduleResult.getTreadCode());
            // 唯一性校验：tm的checkUnique返回String，"0"表示唯一，"1"表示不唯一
            String uniqueResult = iTmScheduleResultService.checkUnique(query);
            if ("1".equals(uniqueResult)) {
                if (sb1.length() > 0) {
                    sb1.append(",").append(query.getTreadCode());
                } else {
                    sb1.append(query.getTreadCode());
                }
                continue;
            }
            scheduleResult.setMachineCode(machineCode);
            AjaxResult result = iTmScheduleResultService.changeMachine(scheduleResult);
            if (result.get(com.ruoyi.common.constant.GatewayConstants.MSG_TAG)
                    .equals(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"))) {
                if (sb2.length() > 0) {
                    sb2.append(",").append(query.getTreadCode());
                } else {
                    sb2.append(query.getTreadCode());
                }
            }
        }
        if (sb1.length() > 0) {
            sb1.append(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }
        if (sb2.length() > 0) {
            sb2.append(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById2"));
        }
        sb1.append(sb2);
        if (sb1.length() > 0) {
            return AjaxResult.error(sb1.toString());
        }
        return AjaxResult.success();
    }

    @ApiOperation("导出数据")
    @GetMapping("/export")
    @RequiresPermissions("tm:tmScheduleResult:export")
    public void export(HttpServletResponse response, TmScheduleResult entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.tm.scheduleResult.modelName");
        byte[] excelBytes = iTmScheduleResultService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(@RequestParam("file") MultipartFile file, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportContext context = new ImportContext();
        context.setFunctionName(I18nUtil.getMessage("ui.data.column.tm.scheduleResult.modelName"));
        context.setProcedureCode(I18nUtil.getMessage("ui.data.column.tm.scheduleResult.modelName"));
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iTmScheduleResultService.importData(context, updateSupport);
        return ajaxResult;
    }

    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.tm.scheduleResult.modelName");
        ExcelUtil<TmScheduleResult> util = new ExcelUtil<>(TmScheduleResult.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
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
        List<TmScheduleShiftDateVO> list = iTmScheduleResultService.listScheduleShiftDates(scheduleDate);
        return AjaxResult.success(list);
    }
}

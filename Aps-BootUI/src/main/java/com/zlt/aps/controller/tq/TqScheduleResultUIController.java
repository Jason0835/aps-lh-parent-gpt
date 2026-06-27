package com.zlt.aps.controller.tq;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.tq.api.domain.entity.TqScheduleResult;
import com.zlt.aps.tq.api.domain.vo.TqScheduleShiftDateVO;
import com.zlt.aps.tq.api.service.ITqScheduleResultService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import cn.hutool.core.convert.Convert;

import java.util.Arrays;
import java.util.List;

/**
 * 胎圈排程结果UIController
 *
 * @author APS
 */
@Slf4j
@Controller
@RequestMapping("/tq/scheduleResult")
@Api(tags = {"胎圈排程结果界面接口"})
public class TqScheduleResultUIController extends BaseUIController<TqScheduleResult> {

    private final String prefix = "tq/scheduleResult";

    @Autowired
    private ITqScheduleResultService iTqScheduleResultService;

    @RequiresPermissions("tq:scheduleResult:view")
    @GetMapping()
    @ApiOperation("跳转到胎圈排程结果首页")
    public String toIndex() {
        return prefix + "/scheduleResult";
    }

    @RequiresPermissions("tq:scheduleResult:list")
    @PostMapping("/list")
    @ResponseBody
    @ApiOperation("查询胎圈排程结果列表")
    public TableDataInfo list(TqScheduleResult entity) {
        return iTqScheduleResultService.list(entity);
    }

    @GetMapping(value = "/edit/{id}")
    @ApiOperation("获取胎圈排程结果详细信息,跳转到编辑页面")
    public String getInfo(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("scheduleResult", iTqScheduleResultService.getInfo(id));
        return prefix + "/edit";
    }

    @RequiresPermissions("tq:scheduleResult:add")
    @PostMapping("/add")
    @ResponseBody
    @ApiOperation("新增胎圈排程结果")
    public AjaxResult add(TqScheduleResult entity) {
        return iTqScheduleResultService.add(entity);
    }

    @RequiresPermissions("tq:scheduleResult:edit")
    @PostMapping("/edit")
    @ResponseBody
    @ApiOperation("修改胎圈排程结果")
    public AjaxResult edit(TqScheduleResult entity) {
        return iTqScheduleResultService.edit(entity);
    }

    @RequiresPermissions("tq:scheduleResult:remove")
    @PostMapping("/remove")
    @ResponseBody
    @ApiOperation("删除胎圈排程结果")
    public AjaxResult remove(String ids) {
        return iTqScheduleResultService.remove(ids);
    }

    @RequiresPermissions("tq:scheduleResult:remove")
    @PostMapping("/logicDelete")
    @ResponseBody
    @ApiOperation("逻辑删除排程记录（已发布成功的计划不允许删除）")
    public AjaxResult logicDelete(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTqScheduleResultService.logicDelete(Arrays.asList(arr));
    }

    @ApiOperation("获取胎圈排程班次日期列表")
    @PostMapping("/listScheduleShiftDates")
    @ResponseBody
    public AjaxResult listScheduleShiftDates(TqScheduleResult queryVO) {
        List<TqScheduleShiftDateVO> list = iTqScheduleResultService.listScheduleShiftDates(queryVO);
        return AjaxResult.success(list);
    }

    /**
     * 自动排程
     */
    @RequiresPermissions("tq:scheduleResult:autoPlan")
    @PostMapping("/autoPlan")
    @ResponseBody
    @ApiOperation("自动排程")
    public AjaxResult autoPlan(TqScheduleResult entity) {
        return iTqScheduleResultService.autoPlan(entity);
    }

    /**
     * 插单
     */
    @RequiresPermissions("tq:scheduleResult:insertOrder")
    @PostMapping("/insertOrder")
    @ResponseBody
    @ApiOperation("插单")
    public AjaxResult insertOrder(TqScheduleResult entity) {
        return iTqScheduleResultService.insertOrder(entity);
    }

    /**
     * 转机台
     */
    @RequiresPermissions("tq:scheduleResult:changeMachine")
    @PostMapping("/changeMachine")
    @ResponseBody
    @ApiOperation("转机台")
    public AjaxResult changeMachine(TqScheduleResult entity) {
        return iTqScheduleResultService.changeMachine(entity);
    }

    /**
     * 调量
     */
    @RequiresPermissions("tq:scheduleResult:adjustQty")
    @PostMapping("/changeQty")
    @ResponseBody
    @ApiOperation("调量")
    public AjaxResult changeQty(TqScheduleResult entity) {
        return iTqScheduleResultService.changeQty(entity);
    }

    /**
     * 发布排程
     */
    @RequiresPermissions("tq:scheduleResult:release")
    @PostMapping("/publish")
    @ResponseBody
    @ApiOperation("发布排程")
    public AjaxResult publish(TqScheduleResult entity) {
        return iTqScheduleResultService.publish(entity);
    }

    /**
     * 查询排程日期是否已发布
     */
    @PostMapping("/isPublish")
    @ResponseBody
    @ApiOperation("查询排程日期是否已发布")
    public AjaxResult isPublish(TqScheduleResult entity) {
        return AjaxResult.success(iTqScheduleResultService.isPublish(entity));
    }

    /**
     * 唯一性校验
     */
    @PostMapping("/checkUnique")
    @ResponseBody
    @ApiOperation("唯一性校验")
    public AjaxResult checkUnique(TqScheduleResult entity) {
        return AjaxResult.success(iTqScheduleResultService.checkUnique(entity));
    }
}

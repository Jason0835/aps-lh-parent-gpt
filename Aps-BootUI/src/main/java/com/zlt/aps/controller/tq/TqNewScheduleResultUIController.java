package com.zlt.aps.controller.tq;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.tq.api.domain.entity.TqNewScheduleResult;
import com.zlt.aps.tq.api.domain.vo.TqScheduleShiftDateVO;
import com.zlt.aps.tq.api.service.ITqNewScheduleResultService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎圈排程结果UIController（新版）
 *
 * @author APS
 */
@Slf4j
@Controller
@RequestMapping("/tq/newScheduleResult")
@Api(tags = {"胎圈排程结果界面接口(新)"})
public class TqNewScheduleResultUIController extends BaseUIController<TqNewScheduleResult> {

    private final String prefix = "tq/newScheduleResult";

    @Autowired
    private ITqNewScheduleResultService iTqNewScheduleResultService;

    @RequiresPermissions("tq:newScheduleResult:view")
    @GetMapping()
    @ApiOperation("跳转到胎圈排程结果首页")
    public String toIndex() {
        return prefix + "/newScheduleResult";
    }

    @RequiresPermissions("tq:newScheduleResult:list")
    @PostMapping("/list")
    @ResponseBody
    @ApiOperation("查询胎圈排程结果列表")
    public TableDataInfo list(TqNewScheduleResult entity) {
        return iTqNewScheduleResultService.list(entity);
    }

    @GetMapping(value = "/edit/{id}")
    @ApiOperation("获取胎圈排程结果详细信息,跳转到编辑页面")
    public String getInfo(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("NewScheduleResult", iTqNewScheduleResultService.getInfo(id));
        return prefix + "/edit";
    }

    @RequiresPermissions("tq:newScheduleResult:add")
    @PostMapping("/add")
    @ResponseBody
    @ApiOperation("新增胎圈排程结果")
    public AjaxResult add(TqNewScheduleResult entity) {
        return iTqNewScheduleResultService.add(entity);
    }

    @RequiresPermissions("tq:newScheduleResult:edit")
    @PostMapping("/edit")
    @ResponseBody
    @ApiOperation("修改胎圈排程结果")
    public AjaxResult edit(TqNewScheduleResult entity) {
        return iTqNewScheduleResultService.edit(entity);
    }

    @RequiresPermissions("tq:newScheduleResult:remove")
    @PostMapping("/remove")
    @ResponseBody
    @ApiOperation("删除胎圈排程结果")
    public AjaxResult remove(String ids) {
        return iTqNewScheduleResultService.remove(ids);
    }

    @ApiOperation("获取胎圈排程班次日期列表")
    @PostMapping("/listScheduleShiftDates")
    @ResponseBody
    public AjaxResult listScheduleShiftDates(TqNewScheduleResult queryVO) {
        List<TqScheduleShiftDateVO> list = iTqNewScheduleResultService.listScheduleShiftDates(queryVO);
        return AjaxResult.success(list);
    }

    /**
     * 自动排程
     */
    @RequiresPermissions("tq:newScheduleResult:autoPlan")
    @PostMapping("/autoPlan")
    @ResponseBody
    @ApiOperation("自动排程")
    public AjaxResult autoPlan(TqNewScheduleResult entity) {
        return iTqNewScheduleResultService.autoPlan(entity);
    }

    /**
     * 插单
     */
    @RequiresPermissions("tq:newScheduleResult:insertOrder")
    @PostMapping("/insertOrder")
    @ResponseBody
    @ApiOperation("插单")
    public AjaxResult insertOrder(TqNewScheduleResult entity) {
        return iTqNewScheduleResultService.insertOrder(entity);
    }

    /**
     * 转机台
     */
    @RequiresPermissions("tq:newScheduleResult:changeMachine")
    @PostMapping("/changeMachine")
    @ResponseBody
    @ApiOperation("转机台")
    public AjaxResult changeMachine(TqNewScheduleResult entity) {
        return iTqNewScheduleResultService.changeMachine(entity);
    }

    /**
     * 调量
     */
    @RequiresPermissions("tq:newScheduleResult:adjustQty")
    @PostMapping("/changeQty")
    @ResponseBody
    @ApiOperation("调量")
    public AjaxResult changeQty(TqNewScheduleResult entity) {
        return iTqNewScheduleResultService.changeQty(entity);
    }

    /**
     * 发布排程
     */
    @RequiresPermissions("tq:newScheduleResult:release")
    @PostMapping("/publish")
    @ResponseBody
    @ApiOperation("发布排程")
    public AjaxResult publish(TqNewScheduleResult entity) {
        return iTqNewScheduleResultService.publish(entity);
    }

    /**
     * 查询排程日期是否已发布
     */
    @PostMapping("/isPublish")
    @ResponseBody
    @ApiOperation("查询排程日期是否已发布")
    public AjaxResult isPublish(TqNewScheduleResult entity) {
        return AjaxResult.success(iTqNewScheduleResultService.isPublish(entity));
    }

    /**
     * 唯一性校验
     */
    @PostMapping("/checkUnique")
    @ResponseBody
    @ApiOperation("唯一性校验")
    public AjaxResult checkUnique(TqNewScheduleResult entity) {
        return AjaxResult.success(iTqNewScheduleResultService.checkUnique(entity));
    }
}

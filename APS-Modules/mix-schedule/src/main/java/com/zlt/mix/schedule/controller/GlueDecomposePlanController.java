package com.zlt.mix.schedule.controller;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.schedule.api.domain.dto.GlueDecomposePlanExportDictDto;
import com.zlt.mix.schedule.api.domain.dto.GlueSpanReceiveDto;
import com.zlt.mix.schedule.api.domain.dto.GlueSpanSendDto;
import com.zlt.mix.schedule.api.domain.entity.GlueDecomposePlan;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanReceive;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanSend;
import com.zlt.mix.schedule.engine.service.decompose.DecomposeEngineService;
import com.zlt.mix.schedule.service.GlueDecomposePlanService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 分解胶料需求量Controller
 *
 * @author chen
 * @date 2022-05-04
 */
@RestController
@RequestMapping("/glueDecomposePlan")
public class GlueDecomposePlanController extends BaseController {
    @Resource
    private GlueDecomposePlanService glueDecomposePlanService;
    @Resource
    private DecomposeEngineService decomposeEngineService;

    /**
     * 查询分解胶料需求量列表
     */
    @ApiOperation("查询分解胶料需求量列表")
    @PostMapping("/list")
    public TableDataInfo listGlueDecomposePlan(@RequestBody GlueDecomposePlan glueDecomposePlan) throws ParseException {
        startPage(false);
        String orderStr = orderStr();
        glueDecomposePlan.setOrderStr(orderStr);
        if (StringUtils.isNotBlank(orderStr) && orderStr.contains("machine_name")) {
            glueDecomposePlan.setOrderStr(orderStr.replace("machine_name", "machine_code"));
        }
        if (glueDecomposePlan.getPlanDate() == null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String format = sdf.format(DateUtils.addDays(new Date(), 1));
            glueDecomposePlan.setPlanDate(sdf.parse(format));
        }
        List<GlueDecomposePlan> list = glueDecomposePlanService.selectGlueDecomposePlanList(glueDecomposePlan);
        return getDataTable(list);
    }

    @ApiOperation("获取分解胶料需求量详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GlueDecomposePlan getGlueDecomposePlanInfo(@PathVariable("id") Long id) {
        return glueDecomposePlanService.getById(id);
    }

    @Log(title = "schedule.glueDecomposePlan.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存分解胶料需求量信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveGlueDecomposePlan(@RequestBody GlueDecomposePlan glueDecomposePlan) {
        List<GlueDecomposePlan> list = glueDecomposePlanService.saveGlueDecomposePlan(glueDecomposePlan);
        return AjaxResult.success(list);
    }

    @Log(title = "schedule.glueDecomposePlan.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除分解胶料需求量")
    @PostMapping("/delete/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteGlueDecomposePlan(@PathVariable Long[] ids) {
        return toAjax(glueDecomposePlanService.deleteGlueDecomposePlanByIds(ids));
    }

    @Log(title = "schedule.glueDecomposePlan.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出分解胶料需求量列表")
    @PostMapping("/exportData")
    public byte[] exportData(@RequestBody GlueDecomposePlanExportDictDto dto) throws ParseException {
        startPage(false);
        dto.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        if (dto.getPlanDate() == null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String format = sdf.format(DateUtils.addDays(new Date(), 1));
            dto.setPlanDate(sdf.parse(format));
        }
        return glueDecomposePlanService.exportData(dto);
    }

    @ApiOperation("校验分解胶料需求量唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkGlueDecomposePlanUnique")
    public String checkGlueDecomposePlanUnique(@RequestBody GlueDecomposePlan glueDecomposePlan) {
        return glueDecomposePlanService.checkGlueDecomposePlanUnique(glueDecomposePlan);
    }

    /**
     * 分解计划
     */
    @Log(title = "schedule.glueDecomposePlan.modelName", newBusinessType = BusinessConstant.DECOMPOSITION)
    @ApiOperation("分解计划")
    @PostMapping("/decompositionPlan")
    public AjaxResult decompositionPlan(@RequestBody GlueDecomposePlan glueDecomposePlan){
        String msg=glueDecomposePlanService.validateMachineData(glueDecomposePlan);
        if(StringUtils.isNotEmpty(msg)){
            return AjaxResult.error(msg);
        }
        //根据终炼胶的汇总计划分解出对应的母炼胶的日计划
        decomposeEngineService.decomposePlan(glueDecomposePlan.getPlanDate(), glueDecomposePlan.getMixArea());
        //分解胶料计划后，在根据胶料跨区设置表，自动胶料发送跨区记录
        // glueDecomposePlanService.autoCreateSpanSend(glueDecomposePlan.getPlanDate(), glueDecomposePlan.getMixArea());
        return AjaxResult.success();
    }

    /**
     * 更新安全库存
     * @param glueDecomposePlan 要更新的数据
     * @return 结果
     */
    @Log(title = "schedule.glueDecomposePlan.modelName", newBusinessType = BusinessConstant.UPDATE)
    @ApiOperation("更新安全库存")
    @PostMapping("/updateSafeStock")
    public AjaxResult updateSafeStock(@RequestBody GlueDecomposePlan glueDecomposePlan) {
        List<GlueDecomposePlan> list = glueDecomposePlanService.updateSafeStock(glueDecomposePlan);
        return AjaxResult.success(list);
    }

    @ApiOperation("检测对应日期和密炼区的数据是否存在")
    @PostMapping("/checkPlanDateAndMixAreaExist")
    public String checkPlanDateAndMixAreaExist(@RequestBody GlueDecomposePlan glueDecomposePlan) {
        return glueDecomposePlanService.checkPlanDateAndMixAreaExist(glueDecomposePlan);
    }

    /**
     * 根据条件查询分解胶料需求量跨区发送列表
     * @param entity 查询条件
     * @return 结果
     */
    @ApiOperation("根据条件查询分解胶料需求量跨区发送列表")
    @PostMapping("/listGlueSpanSend")
    public TableDataInfo listGlueSpanSend(@RequestBody GlueSpanSend entity) {
        startPage(false);
        entity.setOrderStr(orderStr());
        List<GlueSpanSend> list = glueDecomposePlanService.listGlueSpanSend(entity);
        return getDataTable(list);
    }

    /**
     * 发送跨区请求
     * @param dto 跨区请求集合
     * @return 结果
     */
    @ApiOperation("发送跨区请求")
    @PostMapping("/sendGlueSpan")
    public AjaxResult sendGlueSpan(@RequestBody GlueSpanSendDto dto) throws ParseException {
        return glueDecomposePlanService.sendGlueSpan(dto);
    }

    /**
     * 根据条件查询分解胶料需求量跨区接收列表
     * @param entity 查询条件
     * @return 结果
     */
    @ApiOperation("根据条件查询分解胶料需求量跨区接收列表")
    @PostMapping("/listGlueSpanReceive")
    public TableDataInfo listGlueSpanReceive(@RequestBody GlueSpanReceive entity) {
        startPage(false);
        entity.setOrderStr(orderStr());
        List<GlueSpanReceive> list = glueDecomposePlanService.listGlueSpanReceive(entity);
        return getDataTable(list);
    }

    /**
     * 根据id查询跨区接收信息
     * @param entity id
     * @return 查询到的记录
     */
    @ApiOperation("根据id查询跨区接收信息")
    @PostMapping("/getGlueSpanReceiveInfo")
    public GlueSpanReceive getGlueSpanReceiveInfo(@RequestBody GlueSpanReceive entity) {
        return glueDecomposePlanService.getGlueSpanReceiveInfo(entity);
    }

    /**
     * 接收跨区请求
     * @param dto 要接收的跨区请求
     * @return 结果
     */
    @ApiOperation("接收跨区请求")
    @PostMapping("/receiveGlueSpanReceive")
    public AjaxResult receiveGlueSpanReceive(@RequestBody GlueSpanReceiveDto dto) {
        return glueDecomposePlanService.receiveGlueSpanReceive(dto);
    }

    /**
     * 删除跨区发送请求
     * @param ids 要删除的跨区发送请求id
     * @return 结果
     */
    @ApiOperation("删除跨区发送请求")
    @PostMapping("/deleteGlueSpanSend/{ids}")
    public AjaxResult deleteGlueSpanSend(@PathVariable("ids") Long[] ids) {
        return glueDecomposePlanService.deleteGlueSpanSend(ids);
    }

    /**
     * 根据选中的ids查询跨区发送时要携带的字段
     * @param ids 选中的id
     * @return 查询结果
     */
    @ApiOperation("根据选中的ids查询跨区发送时要携带的字段")
    @PostMapping("/selectSpanSendNeedFieldByIds/{ids}")
    public List<GlueDecomposePlan> selectSpanSendNeedFieldByIds(@PathVariable("ids") Long[] ids) {
        return glueDecomposePlanService.selectSpanSendNeedFieldByIds(ids);
    }

    /**
     * 计算跨区请求发送量
     * @param dto 要计算的跨区请求
     * @return 查询结果
     */
    @ApiOperation("根据选中的ids查询跨区发送时要携带的字段")
    @PostMapping("/caculateGlueSpanSendQty")
    public List<GlueSpanReceive> caculateGlueSpanSendQty(@RequestBody GlueSpanReceiveDto dto) {
        return glueDecomposePlanService.caculateGlueSpanSendQty(dto);
    }
}

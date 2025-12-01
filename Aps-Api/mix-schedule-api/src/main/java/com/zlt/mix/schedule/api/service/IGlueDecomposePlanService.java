package com.zlt.mix.schedule.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.schedule.api.domain.dto.GlueDecomposePlanExportDictDto;
import com.zlt.mix.schedule.api.domain.dto.GlueSpanReceiveDto;
import com.zlt.mix.schedule.api.domain.dto.GlueSpanSendDto;
import com.zlt.mix.schedule.api.domain.entity.GlueDecomposePlan;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanReceive;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanSend;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 分解胶料需求量Service接口
 *
 * @author chen
 * @date 2022-05-04
 */
@FeignClient(contextId = "IGlueDecomposePlanService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IGlueDecomposePlanService {

    /**
     * 查询分解胶料需求量列表
     */
    @PostMapping("/glueDecomposePlan/list")
    TableDataInfo listGlueDecomposePlan(@RequestBody GlueDecomposePlan glueDecomposePlan);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/glueDecomposePlan/{id}")
    GlueDecomposePlan getGlueDecomposePlanInfo(@PathVariable("id") Long id);

    /**
     * 保存分解胶料需求量信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/glueDecomposePlan/save")
    AjaxResult saveGlueDecomposePlan(@RequestBody GlueDecomposePlan glueDecomposePlan);

    /**
     * 批量删除分解胶料需求量
     */
    @PostMapping("/glueDecomposePlan/delete/{ids}")
    AjaxResult deleteGlueDecomposePlan(@PathVariable("ids") Long[] ids);

    /**
     * 校验分解胶料需求量唯一性
     */
    @ApiOperation("校验分解胶料需求量唯一性")
    @PostMapping("/glueDecomposePlan/checkGlueDecomposePlanUnique")
    String checkGlueDecomposePlanUnique(@RequestBody GlueDecomposePlan glueDecomposePlan);

    /**
     * 导出分解胶料需求量列表
     */
    @PostMapping("/glueDecomposePlan/exportData")
    byte[] exportData(@RequestBody GlueDecomposePlanExportDictDto dto);

    /**
     * 分解计划
     */
    @PostMapping("/glueDecomposePlan/decompositionPlan")
    public AjaxResult decompositionPlan(@RequestBody GlueDecomposePlan glueDecomposePlan);

    /**
     * 更新安全库存
     *
     * @param glueDecomposePlan 要更新的数据
     * @return 结果
     */
    @PostMapping("/glueDecomposePlan/updateSafeStock")
    public AjaxResult updateSafeStock(@RequestBody GlueDecomposePlan glueDecomposePlan);

    /**
     * 检测对应日期和密炼区的数据是否存在
     */
    @PostMapping("/glueDecomposePlan/checkPlanDateAndMixAreaExist")
    String checkPlanDateAndMixAreaExist(@RequestBody GlueDecomposePlan glueDecomposePlan);

    /**
     * 根据条件查询分解胶料需求量跨区发送列表
     * @param entity 查询条件
     * @return 结果
     */
    @PostMapping("/glueDecomposePlan/listGlueSpanSend")
    TableDataInfo listGlueSpanSend(@RequestBody GlueSpanSend entity);

    /**
     * 发送跨区请求
     * @param dto 跨区请求集合
     * @return 结果
     */
    @PostMapping("/glueDecomposePlan/sendGlueSpan")
    AjaxResult sendGlueSpan(@RequestBody GlueSpanSendDto dto);

    /**
     * 根据条件查询分解胶料需求量跨区接收列表
     * @param entity 查询条件
     * @return 结果
     */
    @PostMapping("/glueDecomposePlan/listGlueSpanReceive")
    TableDataInfo listGlueSpanReceive(@RequestBody GlueSpanReceive entity);

    /**
     * 根据id查询跨区接收信息
     * @param entity id
     * @return 查询到的记录
     */
    @PostMapping("/glueDecomposePlan/getGlueSpanReceiveInfo")
    GlueSpanReceive getGlueSpanReceiveInfo(@RequestBody GlueSpanReceive entity);

    /**
     * 接收跨区请求
     * @param dto 要接收的跨区请求
     * @return 结果
     */
    @PostMapping("/glueDecomposePlan/receiveGlueSpanReceive")
    AjaxResult receiveGlueSpanReceive(@RequestBody GlueSpanReceiveDto dto);

    /**
     * 删除跨区发送请求
     * @param ids 要删除的跨区发送请求id
     * @return 结果
     */
    @PostMapping("/glueDecomposePlan/deleteGlueSpanSend/{ids}")
    AjaxResult deleteGlueSpanSend(@PathVariable("ids") Long[] ids);

    /**
     * 根据选中的ids查询跨区发送时要携带的字段
     * @param ids 选中的id
     * @return 查询结果
     */
    @PostMapping("/glueDecomposePlan/selectSpanSendNeedFieldByIds/{ids}")
    public List<GlueDecomposePlan> selectSpanSendNeedFieldByIds(@PathVariable("ids") Long[] ids);

    /**
     * 计算跨区请求发送量
     * @param dto 要计算的跨区请求
     * @return 结果
     */
    @PostMapping("/glueDecomposePlan/caculateGlueSpanSendQty")
    List<GlueSpanReceive> caculateGlueSpanSendQty(@RequestBody GlueSpanReceiveDto dto);
}

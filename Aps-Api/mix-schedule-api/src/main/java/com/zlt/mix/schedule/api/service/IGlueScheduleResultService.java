package com.zlt.mix.schedule.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.schedule.api.domain.dto.*;
import com.zlt.mix.schedule.api.domain.entity.GlueScheduleResult;
import com.zlt.mix.schedule.api.domain.entity.GlueScheduleSupplement;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanReceive;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanSend;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 终炼/母炼日计划排程Service接口
 *
 * @author chen
 * @date 2022-05-16
 */
@FeignClient(contextId = "IGlueScheduleResultService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IGlueScheduleResultService {

    /**
     * 查询终炼/母炼日计划排程列表
     */
    @PostMapping("/glueScheduleResult/list")
    TableDataInfo listGlueScheduleResult(@RequestBody GlueScheduleResult glueScheduleResult);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/glueScheduleResult/{id}")
    GlueScheduleResult getGlueScheduleResultInfo(@PathVariable("id") Long id);

    /**
     * 保存终炼/母炼日计划排程信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/glueScheduleResult/save")
    AjaxResult saveGlueScheduleResult(@RequestBody GlueScheduleResult glueScheduleResult);

    /**
     * 批量删除终炼/母炼日计划排程
     */
    @PostMapping("/glueScheduleResult/delete/{ids}/{isChangeMasterbatch}")
    AjaxResult deleteGlueScheduleResult(@PathVariable("ids") Long[] ids, @PathVariable("isChangeMasterbatch") Boolean isChangeMasterbatch);

    /**
     * 校验终炼/母炼日计划排程唯一性
     */
    @ApiOperation("校验终炼/母炼日计划排程唯一性")
    @PostMapping("/glueScheduleResult/checkGlueScheduleResultUnique")
    String checkGlueScheduleResultUnique(@RequestBody GlueScheduleResult glueScheduleResult);

    /**
     * 导出终炼/母炼日计划排程列表
     */
    @PostMapping("/glueScheduleResult/exportData")
    byte[] exportData(@RequestBody GlueScheduleResultExportDictDto dto);

    /**
     * 导入终炼/母炼日计划排程数据
     */
    @ApiOperation("导入终炼/母炼日计划排程")
    @PostMapping("/glueScheduleResult/importData")
    public AjaxResult importData(@RequestBody List<GlueScheduleResult> list, @RequestParam("scheduleDate") String scheduleDate, @RequestParam("mixArea") String mixArea, @RequestParam("importLogId") Long importLogId);

    /**
     * 发布终炼母炼日计划
     */
    @ApiOperation("发布终炼母炼日计划")
    @PostMapping("/glueScheduleResult/publish")
    public AjaxResult publish(@RequestBody GlueScheduleResult glueScheduleResult);

    /**
     * 自动排程
     */
    @ApiOperation("自动排程")
    @PostMapping("/glueScheduleResult/autoSchedule")
    public AjaxResult autoSchedule(@RequestBody GlueScheduleResult glueScheduleResult);

    /**
     * 批量转机台
     */
    @ApiOperation("批量转机台")
    @PostMapping("/glueScheduleResult/batchChangeMachine/{machineCode}")
    public AjaxResult batchChangeMachine(@PathVariable("machineCode") String machineCode, @RequestParam("ids") String ids);

    /**
     * 转机台
     */
    @ApiOperation("转机台")
    @PostMapping("/glueScheduleResult/changeMachine")
    public AjaxResult changeMachine(@RequestBody GlueScheduleResult glueScheduleResult);


    /**
     * 检测对应日期和密炼区的数据是否存在
     */
    @PostMapping("/glueScheduleResult/checkScheduleDateAndMixAreaExist")
    String checkScheduleDateAndMixAreaExist(@RequestBody GlueScheduleResult glueScheduleResult);

    /**
     * 重排
     */
    @ApiOperation("重排")
    @PostMapping("/glueScheduleResult/reschedule")
    AjaxResult reschedule(GlueScheduleResult glueScheduleResult);

    /**
     * 更改配方信息
     */
    @ApiOperation("更改配方信息")
    @PostMapping("/glueScheduleResult/changeRecipe")
    public AjaxResult changeRecipe(@RequestBody GlueScheduleResult glueScheduleResult);

    /**
     * 获取统计信息
     */
    @PostMapping("/glueScheduleResult/statistics")
    TableDataInfo statistics(@RequestBody GlueScheduleResult glueScheduleResult);

    /**
     * 根据条件查询终炼母炼日计划跨区发送列表
     * @param entity 查询条件
     * @return 结果
     */
    @PostMapping("/glueScheduleResult/listGlueSpanSend")
    TableDataInfo listGlueSpanSend(@RequestBody GlueSpanSend entity);

    /**
     * 发送跨区请求
     * @param dto 跨区请求集合
     * @return 结果
     */
    @PostMapping("/glueScheduleResult/sendGlueSpan")
    AjaxResult sendGlueSpan(@RequestBody GlueSpanSendDto dto);

    /**
     * 根据条件查询终炼母炼日计划跨区接收列表
     * @param entity 查询条件
     * @return 结果
     */
    @PostMapping("/glueScheduleResult/listGlueSpanReceive")
    TableDataInfo listGlueSpanReceive(@RequestBody GlueSpanReceive entity);

    /**
     * 接收跨区请求
     * @param dto 要接收的跨区请求
     * @return 结果
     */
    @PostMapping("/glueScheduleResult/receiveGlueSpanReceive")
    AjaxResult receiveGlueSpanReceive(@RequestBody GlueSpanReceiveDto dto);

    /**
     * 根据排程日期、密炼区、机台，查询机台的各班次总计划量
     *
     * @param glueScheduleResult 参数
     * @return 结果
     */
    @ApiOperation("根据排程日期、密炼区、机台，查询机台的各班次总计划量")
    @PostMapping("/glueScheduleResult/getSumQtyByMachineCode")
    public GlueSpanReceiveQtyDto getSumQtyByMachineCode(@RequestBody GlueScheduleResult glueScheduleResult);

    /**
     * 删除跨区发送请求
     * @param ids 要删除的跨区发送请求id
     * @return 结果
     */
    @PostMapping("/glueScheduleResult/deleteGlueSpanSend/{ids}")
    AjaxResult deleteGlueSpanSend(@PathVariable("ids") Long[] ids);

    /**
     * 根据选中的ids查询跨区发送时要携带的字段
     * @param ids 选中的id
     * @return 查询结果
     */
    @PostMapping("/glueScheduleResult/selectSpanSendNeedFieldByIds/{ids}")
    public List<GlueScheduleResult> selectSpanSendNeedFieldByIds(@PathVariable("ids") Long[] ids);

    /**
     * 计算终炼/母炼日计划补量列表
     */
    @PostMapping("/glueScheduleResult/caculateSupplement")
    TableDataInfo caculateSupplement(@RequestBody GlueScheduleSupplement glueScheduleResult);

    /**
     * 保存生产补量记录
     * @param supplementList 待保存的生产补量记录
     * @return 结果
     */
    @PostMapping("/glueScheduleResult/saveSupplement")
    AjaxResult saveSupplement(@RequestBody List<GlueScheduleSupplement> glueScheduleSupplementList);

    /**
     * 检查班次是否可编辑
     * @param scheduleDate	排产日期
     * @param classShift	班次编号
     * @return
     */
    @PostMapping("/glueScheduleResult/checkCLassEditable")
    Boolean checkCLassEditable(@RequestBody ScheduleClassEditableDto dto);
    
    /**
     * 获取各班次可编辑状态
     * @param scheduleDate	排产日期
     * @return
     */
    @PostMapping("/glueScheduleResult/getCLassEditableStatus")
    ScheduleClassEditableDto getCLassEditableStatus(@RequestBody ScheduleClassEditableDto dto);

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    @PostMapping("/glueScheduleResult/getSummaryVo")
    @ApiOperation("获取排程日期的排程结果合计")
    public AjaxResult getSummaryVo(@RequestBody GlueScheduleResult scheduleResult);

}

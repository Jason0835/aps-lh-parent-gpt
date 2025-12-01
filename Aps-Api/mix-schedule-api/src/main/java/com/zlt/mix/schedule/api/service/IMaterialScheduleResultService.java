package com.zlt.mix.schedule.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.schedule.api.domain.dto.*;
import com.zlt.mix.schedule.api.domain.entity.*;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 硫化辅料日计划排程Service接口
 *
 * @author chen
 * @date 2022-05-24
 */
@FeignClient(contextId = "IMaterialScheduleResultService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IMaterialScheduleResultService {

    /**
     * 查询硫化辅料日计划排程列表
     */
    @PostMapping("/materialScheduleResult/list")
    TableDataInfo listMaterialScheduleResult(@RequestBody MaterialScheduleResult materialScheduleResult);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/materialScheduleResult/{id}")
    MaterialScheduleResult getMaterialScheduleResultInfo(@PathVariable("id") Long id);

    /**
     * 保存硫化辅料日计划排程信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/materialScheduleResult/save")
    AjaxResult saveMaterialScheduleResult(@RequestBody MaterialScheduleResult materialScheduleResult);

    /**
     * 批量删除硫化辅料日计划排程
     */
    @PostMapping("/materialScheduleResult/delete/{ids}")
    AjaxResult deleteMaterialScheduleResult(@PathVariable("ids") Long[] ids);

    /**
     * 校验硫化辅料日计划排程唯一性
     */
    @ApiOperation("校验硫化辅料日计划排程唯一性")
    @PostMapping("/materialScheduleResult/checkMaterialScheduleResultUnique")
    String checkMaterialScheduleResultUnique(@RequestBody MaterialScheduleResult materialScheduleResult);

    /**
     * 导出硫化辅料日计划排程列表
     */
    @PostMapping("/materialScheduleResult/exportData")
    byte[] exportData(@RequestBody MaterialScheduleResultExportDictDto dto);

    /**
     * 导入硫化辅料日计划排程数据
     */
    @ApiOperation("导入硫化辅料日计划排程")
    @PostMapping("/materialScheduleResult/importData")
    public AjaxResult importData(@RequestBody List<MaterialScheduleResult> list, @RequestParam("scheduleDate") String scheduleDate, @RequestParam("mixArea") String mixArea, @RequestParam("importLogId") Long importLogId);

    /**
     * 发布硫磺辅料日计划
     */
    @ApiOperation("发布硫磺辅料日计划")
    @PostMapping("/materialScheduleResult/publish")
    public AjaxResult publish(@RequestBody MaterialScheduleResult scheduleResult);

    /**
     * 自动排程
     */
    @ApiOperation("自动排程")
    @PostMapping("/materialScheduleResult/autoSchedule")
    public AjaxResult autoSchedule(@RequestBody MaterialScheduleResult scheduleResult);

    /**
     * 批量转机台
     */
    @ApiOperation("批量转机台")
    @PostMapping("/materialScheduleResult/batchChangeMachine/{machineCode}")
    public AjaxResult batchChangeMachine(@PathVariable("machineCode") String machineCode, @RequestParam("ids") String ids);

    /**
     * 转机台
     */
    @ApiOperation("转机台")
    @PostMapping("/materialScheduleResult/changeMachine")
    public AjaxResult changeMachine(@RequestBody MaterialScheduleResult scheduleResult);

    /**
     * 检测对应日期和密炼区的数据是否存在
     */
    @PostMapping("/materialScheduleResult/checkScheduleDateAndMixAreaExist")
    String checkScheduleDateAndMixAreaExist(@RequestBody MaterialScheduleResult scheduleResult);

    /**
     * 更改配方信息
     */
    @ApiOperation("更改配方信息")
    @PostMapping("/materialScheduleResult/changeRecipe")
    public AjaxResult changeRecipe(@RequestBody MaterialScheduleResult materialScheduleResult);


    /**
     * 获取统计信息
     */
    @PostMapping("/materialScheduleResult/statistics")
    TableDataInfo statistics(@RequestBody MaterialScheduleResult materialScheduleResult);
    

    /**
     * 获取超期预警信息
     */
    @PostMapping("/materialScheduleResult/expireWarning")
    TableDataInfo expireWarning(@RequestBody MaterialScheduleResult materialScheduleResult);
    

    /**
     * 根据条件查询硫磺辅料日计划跨区发送列表
     * @param entity 查询条件
     * @return 结果
     */
    @PostMapping("/materialScheduleResult/listMaterialSpanSend")
    TableDataInfo listMaterialSpanSend(@RequestBody MaterialSpanSend entity);

    /**
     * 发送跨区请求
     * @param dto 跨区请求集合
     * @return 结果
     */
    @PostMapping("/materialScheduleResult/sendMaterialSpan")
    AjaxResult sendMaterialSpan(@RequestBody MaterialSpanSendDto dto);

    /**
     * 根据条件查询硫磺辅料日计划跨区接收列表
     * @param entity 查询条件
     * @return 结果
     */
    @PostMapping("/materialScheduleResult/listMaterialSpanReceive")
    TableDataInfo listMaterialSpanReceive(@RequestBody MaterialSpanReceive entity);

    /**
     * 接收跨区请求
     * @param dto 要接收的跨区请求
     * @return 结果
     */
    @PostMapping("/materialScheduleResult/receiveMaterialSpanReceive")
    AjaxResult receiveMaterialSpanReceive(@RequestBody MaterialSpanReceiveDto dto);

    /**
     * 根据排程日期、密炼区、机台，查询机台的各班次总计划量
     *
     * @param scheduleResult 参数
     * @return 结果
     */
    @ApiOperation("根据排程日期、密炼区、机台，查询机台的各班次总计划量")
    @PostMapping("/materialScheduleResult/getSumQtyByMachineCode")
    public MaterialSpanReceiveQtyDto getSumQtyByMachineCode(@RequestBody MaterialScheduleResult scheduleResult);

    /**
     * 删除跨区发送请求
     * @param ids 要删除的跨区发送请求id
     * @return 结果
     */
    @PostMapping("/materialScheduleResult/deleteMaterialSpanSend/{ids}")
    AjaxResult deleteMaterialSpanSend(@PathVariable("ids") Long[] ids);

    /**
     * 根据选中的ids查询跨区发送时要携带的字段
     * @param ids 选中的id
     * @return 查询结果
     */
    @PostMapping("/materialScheduleResult/selectSpanSendNeedFieldByIds/{ids}")
    public List<MaterialScheduleResult> selectSpanSendNeedFieldByIds(@PathVariable("ids") Long[] ids);
    
    /**
     * 检查班次是否可编辑
     * @param scheduleDate	排产日期
     * @param classShift	班次编号
     * @return
     */
    @PostMapping("/materialScheduleResult/checkCLassEditable")
    Boolean checkCLassEditable(@RequestBody ScheduleClassEditableDto dto);
    
    /**
     * 获取各班次可编辑状态
     * @param scheduleDate	排产日期
     * @return
     */
    @PostMapping("/materialScheduleResult/getCLassEditableStatus")
    ScheduleClassEditableDto getCLassEditableStatus(@RequestBody ScheduleClassEditableDto dto);
}

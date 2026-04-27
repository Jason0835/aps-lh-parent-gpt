package com.zlt.aps.lh.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.dto.*;
import com.zlt.aps.lh.api.domain.entity.LhMachineInfo;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhScheduleShiftDateVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 硫化排程结果Service接口
 *
 */
@FeignClient(contextId = "ILhScheduleResultService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:lh}")
public interface ILhScheduleResultRemoteService {


    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/lhScheduleResult/list")
    TableDataInfo list(@RequestBody LhScheduleResult queryVO);

    @ApiOperation("获取详细信息")
    @GetMapping("/lhScheduleResult/{id}")
    LhScheduleResult getInfo(@PathVariable("id") Long id);

    /**
     * 插单查询可用机台列表
     */
    @ApiOperation("插单查询可用机台列表")
    @PostMapping("/lhScheduleResult/getScheduleMachineInfo")
    List<LhMachineInfo> getScheduleMachineInfo(@RequestBody LhOrderInsertParamDTO insertParamDTO);

    /**
     * 插单
     * @param insertDTO
     * @return
     */
    @ApiOperation("插单")
    @PostMapping("/lhScheduleResult/insertOrder")
    AjaxResult insertOrder(@RequestBody LhOrderInsertDTO insertDTO);

    /**
     * 执行自动排程
     */
    @ApiOperation("硫化自动排程")
    @PostMapping("/lhScheduleResult/execute")
    LhScheduleResponseDTO execute(@RequestBody LhScheduleRequestDTO lhScheduleRequestDTO);


    /**
     * 导入
     */
    @ApiOperation("导入")
    @PostMapping("/lhScheduleResult/importData/{updateSupport}")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);


    /**
     * 导出数据
     */
    @ApiOperation("导出列表")
    @PostMapping("/lhScheduleResult/exportData/{fileName}")
    byte[] exportData(@RequestBody LhScheduleResult queryVO, @PathVariable("fileName") String fileName);

    /**
     * 保存
     */
    @PostMapping("/lhScheduleResult/save")
    AjaxResult save(@RequestBody LhScheduleResult lhSpecifyMachine);

    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/lhScheduleResult/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("硫化排程结果转机台校验")
    @PostMapping("/lhScheduleResult/validateChangeMachine")
    AjaxResult validateChangeMachine(@RequestBody LhTransferDeskDTO dto);

    @ApiOperation("转机台")
    @PostMapping("/lhScheduleResult/changeMachine")
    AjaxResult changeMachine(@RequestBody LhTransferDeskDTO dto);

    @ApiOperation("调量")
    @PostMapping("/lhScheduleResult/adjustQuantity")
    AjaxResult adjustQuantity(@RequestBody LhScheduleResultUpdateDTO dto);

    @ApiOperation("文字示方调整")
    @PostMapping("/lhScheduleResult/adjustTextNo")
    AjaxResult adjustTextNo(@RequestBody LhTransferDeskDTO dto);

    /**
     * 生成文字示方换模计划
     *
     * @param dto 生成入参
     * @return 处理结果
     */
    @ApiOperation("生成文字示方换模计划")
    @PostMapping("/lhScheduleResult/generateTextMouldChangePlan")
    AjaxResult generateTextMouldChangePlan(@RequestBody LhGenerateTextMouldPlanDTO dto);

    /**
     * 换模开产增加计划
     *
     * @param scheduleResult 当前硫化排程结果
     * @return 处理结果
     */
    @ApiOperation("换模开产增加计划")
    @PostMapping("/lhScheduleResult/increaseMouldStartPlan")
    AjaxResult increaseMouldStartPlan(@RequestBody LhScheduleResult scheduleResult);

    /**
     * 发布所有排程结果
     *
     * @param dto 查询条件
     * @return 结果
     */
    @PostMapping("/lhScheduleResult/publish")
    public AjaxResult publish(@RequestBody LhScheduleResult dto, @RequestParam("ids") String ids);

    /**
     * 硫化排程结果下发到MES
     *
     * @return 下发结果
     */
    @ApiOperation("硫化排程结果下发到MES")
    @PostMapping("/lhScheduleResult/issueToMes")
    AjaxResult issueToMes();

    /**
     * 根据排程结束日获取窗口内 8 个班次的日期展示列表
     *
     * @param query 含 scheduleDate（yyyy-MM-dd）
     * @return 班次 1～8 与对应 MM/dd
     */
    @ApiOperation("排程日期对象列表")
    @PostMapping("/lhScheduleResult/listScheduleShiftDates")
    List<LhScheduleShiftDateVO> listScheduleShiftDates(@RequestBody LhScheduleShiftDateQueryDTO query);

    @ApiOperation("硫化排程结果调量校验")
    @PostMapping("/lhScheduleResult/validateAdjustQuantity")
    AjaxResult validateAdjustQuantity(@RequestBody LhScheduleResultUpdateDTO dto);
    /**
     * 导出导入模板
     *
     * @param entity
     * @param fileName
     * @return
     */
    @ApiOperation("导出导入模板")
    @PostMapping("/lhScheduleResult/downloadTemplate/{fileName}")
    byte[] downloadTemplate(@RequestBody LhScheduleResult entity,  @PathVariable("fileName") String fileName);

}

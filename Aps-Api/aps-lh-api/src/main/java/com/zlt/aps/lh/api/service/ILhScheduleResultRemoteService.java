package com.zlt.aps.lh.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.dto.*;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.zlt.aps.lh.api.domain.entity.LhMachineInfo;
import com.zlt.aps.lh.api.domain.dto.LhTransferDeskDTO;

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
     * 发布所有排程结果
     *
     * @param dto 查询条件
     * @return 结果
     */
    @PostMapping("/lhScheduleResult/publish")
    public AjaxResult publish(@RequestBody LhScheduleResult dto);

}

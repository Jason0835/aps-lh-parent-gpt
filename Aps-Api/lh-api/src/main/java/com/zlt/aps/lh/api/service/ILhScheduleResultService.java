package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.dto.LhScheduleResultDto;
import com.zlt.aps.lh.api.domain.entity.Gante;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 硫化排程结果Service接口
 *
 * @author chen
 * @date 2021-07-19
 */
@FeignClient(contextId = "ILhScheduleResultService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:lh}")
public interface ILhScheduleResultService {

    /**
     * 查询硫化排程结果列表
     */
    @PostMapping("/lh/scheduleResult/list")
    TableDataInfo list(@RequestBody LhScheduleResultDto dto);

    /**
     * 新增硫化排程结果
     */
    @PostMapping("/lh/scheduleResult/add")
    AjaxResult add(@RequestBody LhScheduleResultDto dto);

    /**
     * 修改硫化排程结果
     */
    @PostMapping("/lh/scheduleResult/edit")
    AjaxResult edit(@RequestBody LhScheduleResultDto dto);

    /**
     * 删除硫化排程结果
     */
    @DeleteMapping("/lh/scheduleResult/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/lh/scheduleResult/{id}")
    LhScheduleResultDto getInfo(@PathVariable("id") Long id);

    /**
     * 校验硫化排程结果唯一性
     */
    @PostMapping("/lh/scheduleResult/checkLhScheduleResultUnique")
    String checkLhScheduleResultUnique(@RequestBody LhScheduleResultDto dto);

    /**
     * 插单校验
     */
    @PostMapping("/lh/scheduleResult/validateAdd")
    AjaxResult validateAdd(@RequestBody LhScheduleResultDto dto);

    /**
     * 转机台校验校验
     */
    @PostMapping("/lh/scheduleResult/validateChangeMachine")
    AjaxResult validateChangeMachine(@RequestBody LhScheduleResultDto dto);

    /**
     * 转机台
     */
    @PostMapping("/lh/scheduleResult/changeMachine")
    @ApiOperation("硫化排程结果转机台")
    AjaxResult changeMachine(@RequestBody LhScheduleResultDto lhScheduleResult);

    /**
     * 调量
     */
    @PostMapping("/lh/scheduleResult/changeQty")
    @ApiOperation("硫化排程结果调量")
    AjaxResult changeQty(@RequestBody LhScheduleResultDto lhScheduleResult);

    /**
     * 导出硫化排程结果列表
     */
    @PostMapping("/lh/scheduleResult/getList")
    List<LhScheduleResultDto> getList(@RequestBody LhScheduleResultDto dto);

    /**
     * 导出列表
     */
    @PostMapping("/lh/scheduleResult/export")
    byte[] export(@RequestBody LhScheduleResultDto dto);

    /**
     * 发布所有排程结果
     *
     * @param dto 查询条件
     */
    @PostMapping("/lh/scheduleResult/publish")
    public AjaxResult publish(@RequestBody LhScheduleResultDto dto);

    /**
     * 自动排程
     */
    @PostMapping("/lh/scheduleResult/autoPlan")
    public AjaxResult autoPlan(@RequestBody LhScheduleResultDto dto);

    @PostMapping("/lh/scheduleResult/importData")
    @ApiOperation("导入硫化排程结果信息")
    public AjaxResult importData(@RequestBody List<LhScheduleResultDto> list, @RequestParam("importLogId") Long importLogId, @RequestParam("scheduleDate")String scheduleDate);

    /**
     * 查询排程日期是否已发布
     */
    @PostMapping("/lh/scheduleResult/isPublish")
    Boolean isPublish(@RequestBody LhScheduleResultDto entity);


    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @PostMapping("/lh/scheduleResult/isReleasingOrTimeoutByDate")
    public int isReleasingOrTimeoutByDate(@RequestBody LhScheduleResultDto scheduleResult);

    /**
     * 更改发布状态
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @PostMapping("/lh/scheduleResult/changeReleaseStatus")
    public AjaxResult changeReleaseStatus(@RequestBody LhScheduleResultDto entity);

    /**
     * 查询排程机台甘特图数据
     */
    @PostMapping("/lh/scheduleResult/getLhGanteData")
    public List<Gante> getLhGanteData(@RequestBody Gante gante);

}

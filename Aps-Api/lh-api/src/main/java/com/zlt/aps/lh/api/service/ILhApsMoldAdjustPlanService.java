package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.dto.LhApsMoldAdjustPlanDto;
import com.zlt.aps.lh.api.domain.entity.LhApsMoldAdjustPlan;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;



/**
 * 硫化工序模具变动单APSService接口
 * @author Joran.zhang
 * @date 2022-06-07
 */
@FeignClient(contextId = "ILhApsMoldAdjustPlanService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:lh}")
public interface ILhApsMoldAdjustPlanService {

    /**
     * 查询硫化工序模具变动单APS列表
     */
    @ApiOperation("查询硫化工序模具变动单APS列表")
    @PostMapping("/lhApsMoldAdjustPlan/list")
    TableDataInfo list(@RequestBody LhApsMoldAdjustPlan lhApsMoldAdjustPlan);

    /**
    * 新增硫化工序模具变动单APS
    */
    @ApiOperation("新增硫化工序模具变动单APS")
    @PostMapping("/lhApsMoldAdjustPlan/add")
    AjaxResult add(@RequestBody LhApsMoldAdjustPlan lhApsMoldAdjustPlan);

    /**
     * 修改硫化工序模具变动单APS
     */
    @ApiOperation("修改硫化工序模具变动单APS")
    @PostMapping("/lhApsMoldAdjustPlan/edit")
    AjaxResult edit(@RequestBody LhApsMoldAdjustPlan lhApsMoldAdjustPlan);

    /**
     * 删除硫化工序模具变动单APS
     */
    @ApiOperation("删除硫化工序模具变动单APS")
    @DeleteMapping("/lhApsMoldAdjustPlan/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/lhApsMoldAdjustPlan/{id}")
    LhApsMoldAdjustPlan getInfo(@PathVariable("id") Long id);

    /**
     * 校验硫化工序模具变动单APS唯一性
     */
    @ApiOperation("校验硫化工序模具变动单APS唯一性")
    @PostMapping("/lhApsMoldAdjustPlan/checkLhApsMoldAdjustPlanUnique")
    String checkLhApsMoldAdjustPlanUnique(@RequestBody LhApsMoldAdjustPlan lhApsMoldAdjustPlan);

    /**
     * 导出硫化工序模具变动单APS列表
     */
    @ApiOperation("导出硫化工序模具变动单APS列表")
    @PostMapping("/lhApsMoldAdjustPlan/getList")
    List<LhApsMoldAdjustPlan> getList(@RequestBody LhApsMoldAdjustPlan lhApsMoldAdjustPlan);

    /**
     * 导入硫化工序模具变动单APS数据
     */
    @ApiOperation("导入硫化工序模具变动单APS")
    @PostMapping("/lhApsMoldAdjustPlan/importData")
    public AjaxResult importData(@RequestBody List<LhApsMoldAdjustPlan> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 发布所有排程结果
     *
     * @param lhApsMoldAdjustPlan 查询条件
     */
    @PostMapping("/lhApsMoldAdjustPlan/publish")
    public AjaxResult publish(@RequestBody LhApsMoldAdjustPlan lhApsMoldAdjustPlan);

    /**
     * 获取前规格信息
     * @param sapCode sap品号
     * @param embryoCode 胎胚代码
     * @return 规格信息
     */
    @ApiOperation("获取前规格信息")
    @PostMapping("/lhApsMoldAdjustPlan/getBeforeSpecDesc")
    public LhApsMoldAdjustPlan getBeforeSpecDesc(@RequestBody LhApsMoldAdjustPlan lhApsMoldAdjustPlan);

    /**
     * 获取后规格信息
     * @param sapCode sap品号
     * @param embryoCode 胎胚代码
     * @return 规格信息
     */
    @ApiOperation("获取后规格信息")
    @PostMapping("/lhApsMoldAdjustPlan/getAfterSpecDesc")
    public LhApsMoldAdjustPlan getAfterSpecDesc(@RequestBody LhApsMoldAdjustPlan lhApsMoldAdjustPlan);

    /**
     * 根据ids更改执行状态
     * @param lhApsMoldAdjustPlan ids、要更改的状态
     * @return 结果
     */
    @ApiOperation("根据ids更改执行状态")
    @PostMapping("/lhApsMoldAdjustPlan/changeExecute")
    public AjaxResult changeExecute(@RequestBody LhApsMoldAdjustPlan lhApsMoldAdjustPlan);

    /**
     * 新增硫化工序模具变动单APS主子表
     */
    @ApiOperation("新增硫化工序模具变动单APS主子表")
    @PostMapping("/lhApsMoldAdjustPlan/addSubData")
    public AjaxResult addSubData(@RequestBody LhApsMoldAdjustPlanDto dto);
}

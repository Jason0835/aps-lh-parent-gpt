package com.zlt.aps.xwyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.xwyy.api.domain.entity.XwyyAssistRequirement;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 纤维压延外厂需求Service接口
 * @author chen
 * @date 2022-03-14
 */
@FeignClient(contextId = "IXwyyAssistRequirementService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.xwyy:xwyy}")
public interface IXwyyAssistRequirementService {

    /**
     * 查询纤维压延外厂需求列表
     */
    @ApiOperation("查询纤维压延外厂需求列表")
    @PostMapping("/assistRequirement/list")
    TableDataInfo list(@RequestBody XwyyAssistRequirement xwyyAssistRequirement);

    /**
    * 新增纤维压延外厂需求
    */
    @ApiOperation("新增纤维压延外厂需求")
    @PostMapping("/assistRequirement/add")
    AjaxResult add(@RequestBody XwyyAssistRequirement xwyyAssistRequirement);

    /**
     * 修改纤维压延外厂需求
     */
    @ApiOperation("修改纤维压延外厂需求")
    @PostMapping("/assistRequirement/edit")
    AjaxResult edit(@RequestBody XwyyAssistRequirement xwyyAssistRequirement);

    /**
     * 删除纤维压延外厂需求
     */
    @ApiOperation("删除纤维压延外厂需求")
    @DeleteMapping("/assistRequirement/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/assistRequirement/{id}")
    XwyyAssistRequirement getInfo(@PathVariable("id") Long id);

    /**
     * 校验纤维压延外厂需求唯一性
     */
    @ApiOperation("校验纤维压延外厂需求唯一性")
    @PostMapping("/assistRequirement/checkXwyyAssistRequirementUnique")
    String checkXwyyAssistRequirementUnique(@RequestBody XwyyAssistRequirement xwyyAssistRequirement);

    /**
     * 导出纤维压延外厂需求列表
     */
    @ApiOperation("导出纤维压延外厂需求列表")
    @PostMapping("/assistRequirement/getList")
    List<XwyyAssistRequirement> getList(@RequestBody XwyyAssistRequirement xwyyAssistRequirement);

    /**
     * 导入纤维压延外厂需求数据
     */
    @ApiOperation("导入纤维压延外厂需求")
    @PostMapping("/assistRequirement/importData")
    public AjaxResult importData(@RequestBody List<XwyyAssistRequirement> list, @RequestParam("importLogId") Long importLogId, @RequestParam("scheduleDate")String scheduleDate);
}

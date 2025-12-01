package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.GlueFinish;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 炼胶时间信息Service接口
 * @author Gim
 * @date 2022-03-29
 */
@FeignClient(contextId = "IGlueFinishService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IGlueFinishService {

    /**
     * 查询炼胶时间信息列表
     */
    @PostMapping("/glueFinish/list")
    TableDataInfo listGlueFinish(@RequestBody GlueFinish glueFinish);

    /**
    * 根据ID获取详细信息
    */
    @GetMapping(value = "/glueFinish/{id}")
    GlueFinish getGlueFinishInfo(@PathVariable("id") Long id);

//    /**
//    * 保存炼胶时间信息信息（id为空则新增，id不为空则修改）
//    */
//    @PostMapping("/glueFinish/save")
//    AjaxResult saveGlueFinish(@RequestBody GlueFinish glueFinish);
//
//    /**
//     * 批量删除炼胶时间信息
//     */
//    @PostMapping("/glueFinish/delete/{ids}")
//    AjaxResult deleteGlueFinish(@PathVariable("ids") Long[] ids);

    /**
     * 校验炼胶时间信息唯一性
     */
    @ApiOperation("校验炼胶时间信息唯一性")
    @PostMapping("/glueFinish/checkGlueFinishUnique")
    String checkGlueFinishUnique(@RequestBody GlueFinish glueFinish);

    /**
     * 导出炼胶时间信息列表
     */
    @PostMapping("/glueFinish/exportData")
    List<GlueFinish> exportData(@RequestBody GlueFinish glueFinish);

    /**
     * 导入炼胶时间信息数据
     */
    @ApiOperation("导入炼胶时间信息")
    @PostMapping("/glueFinish/importData")
    public AjaxResult importData(@RequestBody List<GlueFinish> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}

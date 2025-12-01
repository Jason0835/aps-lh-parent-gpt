package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.GlueCommonDemand;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 密炼机常用大规格设置Service接口
 * @author zlt
 * @date 2023-02-05
 */
@FeignClient(contextId = "IGlueCommonDemandService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IGlueCommonDemandService {

    /**
     * 查询密炼机常用大规格设置列表
     */
    @PostMapping("/glueCommonDemand/list")
    TableDataInfo listGlueCommonDemand(@RequestBody GlueCommonDemand glueCommonDemand);

    /**
    * 根据ID获取详细信息
    */
    @GetMapping(value = "/glueCommonDemand/{id}")
    GlueCommonDemand getGlueCommonDemandInfo(@PathVariable("id") Long id);

    /**
    * 保存密炼机常用大规格设置信息（id为空则新增，id不为空则修改）
    */
    @PostMapping("/glueCommonDemand/save")
    AjaxResult saveGlueCommonDemand(@RequestBody GlueCommonDemand glueCommonDemand);

    /**
     * 批量删除密炼机常用大规格设置
     */
    @PostMapping("/glueCommonDemand/delete/{ids}")
    AjaxResult deleteGlueCommonDemand(@PathVariable("ids") Long[] ids);

    /**
     * 校验密炼机常用大规格设置唯一性
     */
    @ApiOperation("校验密炼机常用大规格设置唯一性")
    @PostMapping("/glueCommonDemand/checkGlueCommonDemandUnique")
    String checkGlueCommonDemandUnique(@RequestBody GlueCommonDemand glueCommonDemand);

    /**
     * 导出密炼机常用大规格设置列表
     */
    @PostMapping("/glueCommonDemand/exportData")
    List<GlueCommonDemand> exportData(@RequestBody GlueCommonDemand glueCommonDemand);

    /**
     * 导入密炼机常用大规格设置数据
     */
    @ApiOperation("导入密炼机常用大规格设置")
    @PostMapping("/glueCommonDemand/importData")
    public AjaxResult importData(@RequestBody List<GlueCommonDemand> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}

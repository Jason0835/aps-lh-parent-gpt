package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.GlueDecompose;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 终炼母炼分解Service接口
 *
 * @author Liam
 * @date 2022-03-28
 */
@FeignClient(contextId = "IGlueDecomposeService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IGlueDecomposeService {

    /**
     * 查询终炼母炼分解列表
     */
    @PostMapping("/decompose/list")
    TableDataInfo listGlueDecompose(@RequestBody GlueDecompose glueDecompose);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/decompose/{id}")
    GlueDecompose getGlueDecomposeInfo(@PathVariable("id") Long id);

    /**
     * 保存终炼母炼分解信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/decompose/save")
    AjaxResult saveGlueDecompose(@RequestBody GlueDecompose glueDecompose);

    /**
     * 批量删除终炼母炼分解
     */
    @PostMapping("/decompose/delete/{ids}")
    AjaxResult deleteGlueDecompose(@PathVariable("ids") Long[] ids);

    /**
     * 校验终炼母炼分解唯一性
     */
    @ApiOperation("校验终炼母炼分解唯一性")
    @PostMapping("/decompose/checkGlueDecomposeUnique")
    String checkGlueDecomposeUnique(@RequestBody GlueDecompose glueDecompose);

    /**
     * 导出终炼母炼分解列表
     */
    @PostMapping("/decompose/exportData")
    List<GlueDecompose> exportData(@RequestBody GlueDecompose glueDecompose);

    /**
     * 导入终炼母炼分解数据
     */
    @ApiOperation("导入终炼母炼分解")
    @PostMapping("/decompose/importData")
    public AjaxResult importData(@RequestBody List<GlueDecompose> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}

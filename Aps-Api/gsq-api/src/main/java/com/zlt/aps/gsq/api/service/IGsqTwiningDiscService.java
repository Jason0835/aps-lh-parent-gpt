package com.zlt.aps.gsq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.dto.GsqTwiningDiscDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢丝圈缠绕盘对外暴露接口
 */
@FeignClient(contextId = "iGsqTwiningDiscService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.gsq:gsq}")
public interface IGsqTwiningDiscService {

    /**
     * 根据条件查询缠绕盘列表
     */
    @PostMapping("/gsq/twiningDisc/listTwiningDisc")
    TableDataInfo listTwiningDisc(@RequestBody GsqTwiningDiscDto dto);

    /**
     * 根据id查询缠绕盘信息
     */
    @GetMapping("/gsq/twiningDisc/getTwiningDisc/{id}")
    GsqTwiningDiscDto getTwiningDisc(@PathVariable("id") Long id);

    /**
     * 保存缠绕盘信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/gsq/twiningDisc/saveTwiningDisc")
    AjaxResult saveTwiningDisc(@RequestBody GsqTwiningDiscDto dto);

    /**
     * 判断缠绕code是否唯一
     */
    @PostMapping("/gsq/twiningDisc/checkSerialNumberUnique")
    String checkSerialNumberUnique(@RequestBody GsqTwiningDiscDto dto);


    /**
     * 批量删除缠绕盘信息(逻辑删)
     * @param ids 多个id逗号分割
     */
    @PostMapping("/gsq/twiningDisc/deleteTwiningDisc/{ids}")
    AjaxResult deleteTwiningDisc(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     * @param dto
     */
    @PostMapping("/gsq/twiningDisc/exportData")
    List<GsqTwiningDiscDto> exportData(@RequestBody GsqTwiningDiscDto dto);

    @PostMapping("/gsq/twiningDisc/importData")
    @ApiOperation("导入钢丝圈缠绕盘信息")
    public AjaxResult importData(@RequestBody List<GsqTwiningDiscDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 删除全部(逻辑删)
     */
    @PostMapping("/gsq/twiningDisc/deleteAll")
    AjaxResult deleteAll();
}

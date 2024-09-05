package com.zlt.aps.gsq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.dto.GsqTwiningDiscDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
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
    @GetMapping("/twiningDisc/listTwiningDisc")
    TableDataInfo listTwiningDisc(@SpringQueryMap GsqTwiningDiscDto dto);

    /**
     * 根据id查询缠绕盘信息
     */
    @GetMapping("/twiningDisc/getTwiningDisc/{id}")
    GsqTwiningDiscDto getTwiningDisc(@PathVariable("id") Long id);

    /**
     * 保存缠绕盘信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/twiningDisc/saveTwiningDisc")
    AjaxResult saveTwiningDisc(@RequestBody GsqTwiningDiscDto dto);

    /**
     * 判断缠绕code是否唯一
     */
    @PostMapping("/twiningDisc/checkSerialNumberUnique")
    String checkSerialNumberUnique(@RequestBody GsqTwiningDiscDto dto);


    /**
     * 批量删除缠绕盘信息(逻辑删)
     * @param ids 多个id逗号分割
     */
    @PostMapping("/twiningDisc/deleteTwiningDisc/{ids}")
    AjaxResult deleteTwiningDisc(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     * @param dto
     */
    @GetMapping("/twiningDisc/exportData")
    List<GsqTwiningDiscDto> exportData(@SpringQueryMap GsqTwiningDiscDto dto);

    @PostMapping("/twiningDisc/importData")
    @ApiOperation("导入钢丝圈缠绕盘信息")
    public AjaxResult importData(@RequestBody List<GsqTwiningDiscDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 删除全部(逻辑删)
     */
    @PostMapping("/twiningDisc/deleteAll")
    AjaxResult deleteAll();
}

package com.zlt.aps.gsq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.dto.GsqSteelTypeColorDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢丝圈颜色提示信息维护对外暴露接口
 */
@FeignClient(contextId = "gsqSteelTypeColorService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.gsq:gsq}")
public interface IGsqSteelTypeColorService {

    /**
     * 根据条件查询帘布大卷颜色提示信息维护列表
     */
    @PostMapping("/gsqSteelTypeColor/listGsqSteelTypeColor")
    TableDataInfo listGsqSteelTypeColor(@RequestBody GsqSteelTypeColorDto dto);

    /**
     * 根据id查询帘布大卷颜色提示信息维护
     */
    @GetMapping("/gsqSteelTypeColor/getGsqSteelTypeColor/{id}")
    GsqSteelTypeColorDto getGsqSteelTypeColor(@PathVariable("id") Long id);

    /**
     * 保存颜色提示信息维护信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/gsqSteelTypeColor/saveGsqSteelTypeColor")
    AjaxResult saveGsqSteelTypeColor(@RequestBody GsqSteelTypeColorDto dto);

    /**
     * 保存颜色提示信息维护信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/gsqSteelTypeColor/checkGsqSteelTypeColor")
    String checkGsqSteelTypeColor(@RequestBody GsqSteelTypeColorDto dto);

    /**
     * 批量删除颜色提示信息维护信息(逻辑删)
     * @param ids 多个id逗号分割
     */
    @PostMapping("/gsqSteelTypeColor/deleteGsqSteelTypeColor/{ids}")
    AjaxResult deleteGsqSteelTypeColor(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     * @param dto
     */
    @GetMapping("/gsqSteelTypeColor/exportData")
    List<GsqSteelTypeColorDto> exportData(@SpringQueryMap GsqSteelTypeColorDto dto);

    @PostMapping("/gsqSteelTypeColor/importData")
    @ApiOperation("导入钢丝圈颜色信息")
    public AjaxResult importData(@RequestBody List<GsqSteelTypeColorDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}

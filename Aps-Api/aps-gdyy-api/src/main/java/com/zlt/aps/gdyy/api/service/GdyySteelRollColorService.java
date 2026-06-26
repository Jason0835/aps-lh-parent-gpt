package com.zlt.aps.gdyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gdyy.api.domain.dto.GdyySteelRollColorDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢带大卷颜色提示信息维护对外暴露接口
 */
@FeignClient(contextId = "gdyySteelRollColorService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.gdyy:gdyy}")
public interface GdyySteelRollColorService {

    /**
     * 根据条件查询帘布大卷颜色提示信息维护列表
     */
    @PostMapping("/gdyySteelRollColor/listGdyySteelRollColor")
    TableDataInfo listGdyySteelRollColor(@RequestBody GdyySteelRollColorDto dto);

    /**
     * 根据id查询帘布大卷颜色提示信息维护
     */
    @GetMapping("/gdyySteelRollColor/getGdyySteelRollColor/{id}")
    GdyySteelRollColorDto getGdyySteelRollColor(@PathVariable("id") Long id);

    /**
     * 保存帘布大卷颜色提示信息维护信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/gdyySteelRollColor/saveGdyySteelRollColor")
    AjaxResult saveGdyySteelRollColor(@RequestBody GdyySteelRollColorDto dto);

    /**
     * 保存帘布大卷颜色提示信息维护信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/gdyySteelRollColor/checkGdyySteelRollColor")
    String checkGdyySteelRollColor(@RequestBody GdyySteelRollColorDto dto);

    /**
     * 批量删除帘布大卷颜色提示信息维护信息(逻辑删)
     * @param ids 多个id逗号分割
     */
    @PostMapping("/gdyySteelRollColor/deleteGdyySteelRollColor/{ids}")
    AjaxResult deleteGdyySteelRollColor(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     * @param dto
     */
    @PostMapping("/gdyySteelRollColor/exportData")
    List<GdyySteelRollColorDto> exportData(@RequestBody GdyySteelRollColorDto dto);

    @PostMapping("/gdyySteelRollColor/importData")
    @ApiOperation("导入钢带压延库存信息")
    public AjaxResult importData(@RequestBody List<GdyySteelRollColorDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}

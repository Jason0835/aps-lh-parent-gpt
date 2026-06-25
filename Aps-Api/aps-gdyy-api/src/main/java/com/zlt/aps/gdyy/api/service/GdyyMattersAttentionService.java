package com.zlt.aps.gdyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gdyy.api.domain.dto.GdyyMattersAttentionDto;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢带大卷注意事项信息维护对外暴露接口
 */
@FeignClient(contextId = "gdyyMattersAttentionService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.gdyy:gdyy}")
public interface GdyyMattersAttentionService {

    /**
     * 根据条件查询帘布大卷注意事项信息维护列表
     */
    @PostMapping("/gdyyMattersAttention/listGdyyMattersAttention")
    TableDataInfo listGdyyMattersAttention(@RequestBody GdyyMattersAttentionDto dto);

    /**
     * 根据id查询帘布大卷注意事项信息维护
     */
    @GetMapping("/gdyyMattersAttention/getGdyyMattersAttention/{id}")
    GdyyMattersAttentionDto getGdyyMattersAttention(@PathVariable("id") Long id);

    /**
     * 保存帘布大卷注意事项信息维护信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/gdyyMattersAttention/saveGdyyMattersAttention")
    AjaxResult saveGdyyMattersAttention(@RequestBody GdyyMattersAttentionDto dto);

    /**
     * 保存帘布大卷注意事项信息维护信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/gdyyMattersAttention/checkGdyyMattersAttention")
    String checkGdyyMattersAttention(@RequestBody GdyyMattersAttentionDto dto);

    /**
     * 批量删除帘布大卷注意事项信息维护信息(逻辑删)
     * @param ids 多个id逗号分割
     */
    @PostMapping("/gdyyMattersAttention/deleteGdyyMattersAttention/{ids}")
    AjaxResult deleteGdyyMattersAttention(@PathVariable("ids") Long[] ids);

    /**
     * 导出帘布大卷注意事项信息
     *
     * @param dto
     * @return
     */
    @PostMapping("/gdyyMattersAttention/exportData")
    List<GdyyMattersAttentionDto> exportData(@RequestBody GdyyMattersAttentionDto dto);


    @PostMapping("/gdyyMattersAttention/importData")
    @ApiOperation("导入钢带压延注意事项信息")
    public AjaxResult importData(@RequestBody List<GdyyMattersAttentionDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}

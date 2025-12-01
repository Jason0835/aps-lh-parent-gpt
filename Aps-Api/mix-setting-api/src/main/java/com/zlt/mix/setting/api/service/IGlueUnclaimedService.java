package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.GlueUnclaimed;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胶料白班待支领Service接口
 * @author zlt
 * @date 2022-09-05
 */
@FeignClient(contextId = "IGlueUnclaimedService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IGlueUnclaimedService {

    /**
     * 查询胶料白班待支领列表
     */
    @PostMapping("/unclaimed/list")
    TableDataInfo listGlueUnclaimed(@RequestBody GlueUnclaimed glueUnclaimed);

    /**
    * 根据ID获取详细信息
    */
    @GetMapping(value = "/unclaimed/{id}")
    GlueUnclaimed getGlueUnclaimedInfo(@PathVariable("id") Long id);

    /**
    * 保存胶料白班待支领信息（id为空则新增，id不为空则修改）
    */
    @PostMapping("/unclaimed/save")
    AjaxResult saveGlueUnclaimed(@RequestBody GlueUnclaimed glueUnclaimed);

    /**
     * 批量删除胶料白班待支领
     */
    @PostMapping("/unclaimed/delete/{ids}")
    AjaxResult deleteGlueUnclaimed(@PathVariable("ids") Long[] ids);

    /**
     * 校验胶料白班待支领唯一性
     */
    @ApiOperation("校验胶料白班待支领唯一性")
    @PostMapping("/unclaimed/checkGlueUnclaimedUnique")
    String checkGlueUnclaimedUnique(@RequestBody GlueUnclaimed glueUnclaimed);

    /**
     * 导出胶料白班待支领列表
     */
    @PostMapping("/unclaimed/exportData")
    List<GlueUnclaimed> exportData(@RequestBody GlueUnclaimed glueUnclaimed);

    /**
     * 导入胶料白班待支领数据
     */
    @ApiOperation("导入胶料白班待支领")
    @PostMapping("/unclaimed/importData")
    public AjaxResult importData(@RequestBody List<GlueUnclaimed> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}

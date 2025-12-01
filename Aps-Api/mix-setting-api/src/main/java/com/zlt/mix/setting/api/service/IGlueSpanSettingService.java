package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.GlueSpanSetting;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 终炼母炼胶料跨区设置Service接口
 *
 * @author chen
 * @date 2022-08-12
 */
@FeignClient(contextId = "IGlueSpanSettingService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IGlueSpanSettingService {

    /**
     * 查询终炼母炼胶料跨区设置列表
     */
    @PostMapping("/glueSpanSetting/list")
    TableDataInfo listGlueSpanSetting(@RequestBody GlueSpanSetting glueSpanSetting);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/glueSpanSetting/{id}")
    GlueSpanSetting getGlueSpanSettingInfo(@PathVariable("id") Long id);

    /**
     * 保存终炼母炼胶料跨区设置信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/glueSpanSetting/save")
    AjaxResult saveGlueSpanSetting(@RequestBody GlueSpanSetting glueSpanSetting);

    /**
     * 批量删除终炼母炼胶料跨区设置
     */
    @PostMapping("/glueSpanSetting/delete/{ids}")
    AjaxResult deleteGlueSpanSetting(@PathVariable("ids") Long[] ids);

    /**
     * 校验终炼母炼胶料跨区设置唯一性
     */
    @ApiOperation("校验终炼母炼胶料跨区设置唯一性")
    @PostMapping("/glueSpanSetting/checkGlueSpanSettingUnique")
    String checkGlueSpanSettingUnique(@RequestBody GlueSpanSetting glueSpanSetting);

    /**
     * 导出终炼母炼胶料跨区设置列表
     */
    @PostMapping("/glueSpanSetting/exportData")
    List<GlueSpanSetting> exportData(@RequestBody GlueSpanSetting glueSpanSetting);

    /**
     * 导入终炼母炼胶料跨区设置数据
     */
    @ApiOperation("导入终炼母炼胶料跨区设置")
    @PostMapping("/glueSpanSetting/importData")
    public AjaxResult importData(@RequestBody List<GlueSpanSetting> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}

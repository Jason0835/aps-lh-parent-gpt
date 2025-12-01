package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.LhflSpanSetting;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 硫磺辅料跨区设置Service接口
 * @author chen
 * @date 2022-08-12
 */
@FeignClient(contextId = "ILhflSpanSettingService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface ILhflSpanSettingService {

    /**
     * 查询硫磺辅料跨区设置列表
     */
    @PostMapping("/lhflSpanSetting/list")
    TableDataInfo listLhflSpanSetting(@RequestBody LhflSpanSetting lhflSpanSetting);

    /**
    * 根据ID获取详细信息
    */
    @GetMapping(value = "/lhflSpanSetting/{id}")
    LhflSpanSetting getLhflSpanSettingInfo(@PathVariable("id") Long id);

    /**
    * 保存硫磺辅料跨区设置信息（id为空则新增，id不为空则修改）
    */
    @PostMapping("/lhflSpanSetting/save")
    AjaxResult saveLhflSpanSetting(@RequestBody LhflSpanSetting lhflSpanSetting);

    /**
     * 批量删除硫磺辅料跨区设置
     */
    @PostMapping("/lhflSpanSetting/delete/{ids}")
    AjaxResult deleteLhflSpanSetting(@PathVariable("ids") Long[] ids);

    /**
     * 校验硫磺辅料跨区设置唯一性
     */
    @ApiOperation("校验硫磺辅料跨区设置唯一性")
    @PostMapping("/lhflSpanSetting/checkLhflSpanSettingUnique")
    String checkLhflSpanSettingUnique(@RequestBody LhflSpanSetting lhflSpanSetting);

    /**
     * 导出硫磺辅料跨区设置列表
     */
    @PostMapping("/lhflSpanSetting/exportData")
    List<LhflSpanSetting> exportData(@RequestBody LhflSpanSetting lhflSpanSetting);

    /**
     * 导入硫磺辅料跨区设置数据
     */
    @ApiOperation("导入硫磺辅料跨区设置")
    @PostMapping("/lhflSpanSetting/importData")
    public AjaxResult importData(@RequestBody List<LhflSpanSetting> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}

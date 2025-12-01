package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.LhflLossSetting;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 硫磺辅料耗损率设定Service接口
 * @author Joran.zhang
 * @date 2022-05-23
 */
@FeignClient(contextId = "ILhflLossSettingService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface ILhflLossSettingService {

    /**
     * 查询硫磺辅料耗损率设定列表
     */
    @PostMapping("/lhflLossSetting/list")
    TableDataInfo listLhflLossSetting(@RequestBody LhflLossSetting lhflLossrateSetting);

    /**
    * 根据ID获取详细信息
    */
    @GetMapping(value = "/lhflLossSetting/{id}")
    LhflLossSetting getLhflLossSettingInfo(@PathVariable("id") Long id);

    /**
    * 保存硫磺辅料耗损率设定信息（id为空则新增，id不为空则修改）
    */
    @PostMapping("/lhflLossSetting/save")
    AjaxResult saveLhflLossSetting(@RequestBody LhflLossSetting lhflLossrateSetting);

    /**
     * 批量删除硫磺辅料耗损率设定
     */
    @PostMapping("/lhflLossSetting/delete/{ids}")
    AjaxResult deleteLhflLossSetting(@PathVariable("ids") Long[] ids);

    /**
     * 校验硫磺辅料耗损率设定唯一性
     */
    @ApiOperation("校验硫磺辅料耗损率设定唯一性")
    @PostMapping("/lhflLossSetting/checkLhflLossrateSettingUnique")
    String checkLhflLossSettingUnique(@RequestBody LhflLossSetting lhflLossrateSetting);

    /**
     * 导出硫磺辅料耗损率设定列表
     */
    @PostMapping("/lhflLossSetting/exportData")
    List<LhflLossSetting> exportData(@RequestBody LhflLossSetting lhflLossrateSetting);

    /**
     * 导入硫磺辅料耗损率设定数据
     */
    @ApiOperation("导入硫磺辅料耗损率设定")
    @PostMapping("/lhflLossSetting/importData")
    public AjaxResult importData(@RequestBody List<LhflLossSetting> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}

package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.RemindSetting;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 提醒设备Service接口
 *
 * @author Gim
 * @date 2022-03-23
 */
@FeignClient(contextId = "IRemindSettingService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IRemindSettingService {

    /**
     * 查询提醒设备列表
     */
    @PostMapping("/remindSetting/list")
    TableDataInfo listRemindSetting(@RequestBody RemindSetting remindSetting);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/remindSetting/{id}")
    RemindSetting getRemindSettingInfo(@PathVariable("id") Long id);

    /**
     * 保存提醒设备信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/remindSetting/save")
    AjaxResult saveRemindSetting(@RequestBody RemindSetting remindSetting);

    /**
     * 批量删除提醒设备
     */
    @PostMapping("/remindSetting/delete/{ids}")
    AjaxResult deleteRemindSetting(@PathVariable("ids") Long[] ids);

    /**
     * 校验提醒设备唯一性
     */
    @ApiOperation("校验提醒设备唯一性")
    @PostMapping("/remindSetting/checkRemindSettingUnique")
    String checkRemindSettingUnique(@RequestBody RemindSetting remindSetting);

    /**
     * 导出提醒设备列表
     */
    @PostMapping("/remindSetting/exportData")
    List<RemindSetting> exportData(@RequestBody RemindSetting remindSetting);

    /**
     * 导入提醒设备数据
     */
    @ApiOperation("导入提醒设备")
    @PostMapping("/remindSetting/importData")
    public AjaxResult importData(@RequestBody List<RemindSetting> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}

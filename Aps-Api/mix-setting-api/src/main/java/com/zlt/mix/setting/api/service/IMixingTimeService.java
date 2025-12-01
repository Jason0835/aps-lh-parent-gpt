package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.dto.MixingTimeDto;
import com.zlt.mix.setting.api.domain.entity.MixingTime;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 炼胶时间信息Service接口
 *
 * @author Liam
 * @date 2022-03-31
 */
@FeignClient(contextId = "IMixingTimeService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IMixingTimeService {

    /**
     * 查询炼胶时间信息列表
     */
    @PostMapping("/mixingTime/list")
    TableDataInfo listMixingTime(@RequestBody MixingTime mixingTime);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/mixingTime/{id}")
    MixingTime getMixingTimeInfo(@PathVariable("id") Long id);

    /**
     * 保存炼胶时间信息信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/mixingTime/save")
    AjaxResult saveMixingTime(@RequestBody MixingTime mixingTime);

    /**
     * 批量删除炼胶时间信息
     */
    @PostMapping("/mixingTime/delete/{ids}")
    AjaxResult deleteMixingTime(@PathVariable("ids") Long[] ids);

    /**
     * 校验炼胶时间信息唯一性
     */
    @ApiOperation("校验炼胶时间信息唯一性")
    @PostMapping("/mixingTime/checkMixingTimeUnique")
    String checkMixingTimeUnique(@RequestBody MixingTime mixingTime);

    /**
     * 导出炼胶时间信息列表
     */
    @PostMapping("/mixingTime/exportData")
    List<MixingTimeDto> exportData(@RequestBody MixingTime mixingTime);

    /**
     * 导入炼胶时间信息数据
     */
    @ApiOperation("导入炼胶时间信息")
    @PostMapping("/mixingTime/importData")
    public AjaxResult importData(@RequestBody List<MixingTimeDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}

package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.LhflScheduleParams;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 排程参数（硫磺辅料排程设置）Service接口
 *
 * @author Liam
 * @date 2022-04-06
 */
@FeignClient(contextId = "ILhflScheduleParamsService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface ILhflScheduleParamsService {

    /**
     * 查询排程参数（硫磺辅料排程设置）列表
     */
    @PostMapping("/lhflScheduleParams/list")
    TableDataInfo listLhflScheduleParams(@RequestBody LhflScheduleParams lhflScheduleParams);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/lhflScheduleParams/{id}")
    LhflScheduleParams getLhflScheduleParamsInfo(@PathVariable("id") Long id);

    /**
     * 保存排程参数（硫磺辅料排程设置）信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/lhflScheduleParams/save")
    AjaxResult saveLhflScheduleParams(@RequestBody LhflScheduleParams lhflScheduleParams);

    /**
     * 复制排程参数（硫磺辅料排程设置）信息
     */
    @PostMapping("/lhflScheduleParams/copy")
    AjaxResult copyLhflScheduleParams(@RequestBody LhflScheduleParams lhflScheduleParams);

    /**
     * 导出排程参数（硫磺辅料排程设置）列表
     */
    @PostMapping("/lhflScheduleParams/exportData")
    List<LhflScheduleParams> exportData(@RequestBody LhflScheduleParams lhflScheduleParams);
}

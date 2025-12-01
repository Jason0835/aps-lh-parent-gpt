package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.SettingScheduleParams;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 密炼参数对外暴露接口
 *
 * @author Liam
 * @date 2022-03-14
 */
@FeignClient(contextId = "ISettingScheduleParamsService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface ISettingScheduleParamsService {

    String prefix = "/setting/scheduleParams";

    /**
     * 查询密炼参数信息列表
     *
     * @param settingScheduleParams 密炼参数信息
     * @return 密炼参数信息表格数据
     */
    @PostMapping(prefix + "/list")
    TableDataInfo list(@RequestBody SettingScheduleParams settingScheduleParams);

    /**
     * 加载指定条件的密炼参数
     *
     * @param settingScheduleParams 密炼参数信息
     * @return 操作消息
     */
    @PostMapping(prefix + "/find" )
    AjaxResult find(@RequestBody SettingScheduleParams settingScheduleParams);

    /**
     * 获取密炼参数信息详细信息
     *
     * @param id 密炼参数信息ID
     * @return 密炼参数信息
     */
    @GetMapping(prefix + "/{id}")
    SettingScheduleParams getInfo(@PathVariable("id") Long id);

    /**
     * 修改密炼参数信息
     *
     * @param settingScheduleParams 密炼参数信息
     * @return 操作消息
     */
    @PostMapping(prefix + "/edit" )
    AjaxResult edit(@RequestBody SettingScheduleParams settingScheduleParams);

    /**
     * 复制密炼参数信息
     *
     * @param settingScheduleParams 密炼参数信息
     * @return 操作消息
     */
    @PostMapping(prefix + "/copy" )
    AjaxResult copy(@RequestBody SettingScheduleParams settingScheduleParams);


    /**
     * 导出密炼参数信息
     *
     * @param settingScheduleParams 密炼参数信息
     * @return 密炼参数信息列表
     */
    @GetMapping(prefix + "/exportData" )
    List<SettingScheduleParams> exportData(@SpringQueryMap SettingScheduleParams settingScheduleParams);
}

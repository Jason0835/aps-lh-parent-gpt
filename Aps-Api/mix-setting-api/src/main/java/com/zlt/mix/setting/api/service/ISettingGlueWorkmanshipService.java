package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.SettingGlueWorkmanship;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分厂胶料工艺对外暴露接口
 *
 * @author Liam
 * @date 2022-03-18
 */
@FeignClient(contextId = "ISettingGlueWorkmanshipService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface ISettingGlueWorkmanshipService {

    String prefix = "setting/glueWorkmanship";


    /**
     * 获取分厂胶料工艺信息表格数据
     *
     * @param entity 分厂胶料工艺信息
     * @return 分厂胶料工艺信息表格数据
     */
    @PostMapping(prefix + "/list")
    TableDataInfo list(@RequestBody SettingGlueWorkmanship entity);

    /**
     * 获取分厂胶料工艺详细信息
     *
     * @param id 分厂胶料工艺ID
     * @return 分厂胶料工艺详细信息
     */
    @GetMapping(prefix + "/edit/{id}")
    SettingGlueWorkmanship getInfo(@PathVariable("id") Long id);

    /**
     * 保存分厂胶料工艺信息（id为空则新增，id不为空则修改）
     *
     * @param entity 分厂胶料工艺信息
     * @return 操作消息
     */
    @PostMapping(prefix + "/save")
    AjaxResult save(@RequestBody SettingGlueWorkmanship entity);

    /**
     * 删除分厂胶料工艺信息
     *
     * @param ids 分厂胶料工艺信息的id数组
     * @return 操作消息
     */
    @PostMapping(prefix + "/remove/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 导出分厂胶料工艺信息
     *
     * @param entity 分厂胶料工艺信息
     * @return 分厂胶料工艺信息列表
     */
    @GetMapping(prefix + "/export")
    List<SettingGlueWorkmanship> export(@SpringQueryMap SettingGlueWorkmanship entity);

    /**
     * 导入分厂胶料工艺信息
     *
     * @param list          分厂胶料工艺信息列表
     * @param updateSupport 是否更新
     * @param importLogId   导入日志的ID
     * @return 操作消息
     */
    @PostMapping(prefix + "/import")
    AjaxResult importData(@RequestBody List<SettingGlueWorkmanship> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}

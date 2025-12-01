package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.SettingFormulaInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 配方信息对外暴露接口
 *
 * @author Liam
 * @date 2022-03-22
 */
@FeignClient(contextId = "ISettingFormulaInfoService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface ISettingFormulaInfoService {

    String prefix = "setting/formulaInfo";

    /**
     * 查询配方信息表格数据
     *
     * @param entity 配方信息
     * @return 配方信息表格数据
     */
    @PostMapping(prefix + "/list")
    TableDataInfo list(@RequestBody SettingFormulaInfo entity);

    /**
     * 查询配方信息详细信息
     *
     * @param id 配方信息的主键ID
     * @return 配方信息详细信息
     */
    @GetMapping(prefix + "/edit/{id}")
    SettingFormulaInfo getInfo(@PathVariable("id") Long id);

    /**
     * 保存配方信息（id为空则新增，id不为空则修改）
     *
     * @param entity 配方信息
     * @return 操作消息
     */
    @PostMapping(prefix + "/save")
    AjaxResult save(@RequestBody SettingFormulaInfo entity);

    /**
     * 批量删除配方信息
     *
     * @param ids 配方信息的ID数组
     * @return 操作消息
     */
    @PostMapping(prefix + "/remove/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 导出配方信息
     *
     * @param entity 配方信息
     * @return 配方信息列表
     */
    @GetMapping(prefix + "/export")
    List<SettingFormulaInfo> export(@SpringQueryMap SettingFormulaInfo entity);

    /**
     * 导入配方信息
     *
     * @param list          配方信息列表
     * @param updateSupport 是否更新
     * @param importLogId   导入日志的id
     * @return 操作消息
     */
    @PostMapping(prefix + "/import")
    AjaxResult importData(@RequestBody List<SettingFormulaInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 判断胶料名称是否已经存在
     *
     * @param entity 配方信息
     * @return 是否存在
     */
    @PostMapping(prefix + "/checkGlueUnique")
    String checkGlueUnique(@RequestBody SettingFormulaInfo entity);
}

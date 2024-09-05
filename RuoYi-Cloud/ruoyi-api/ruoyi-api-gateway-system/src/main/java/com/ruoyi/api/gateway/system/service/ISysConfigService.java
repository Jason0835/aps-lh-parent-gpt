package com.ruoyi.api.gateway.system.service;

import com.ruoyi.api.gateway.system.domain.SysConfig;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 系统管理参数配置对外暴露接口
 */
@FeignClient(contextId = "iSysConfigService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.system:system}")
public interface ISysConfigService {

    /**
     * 获取参数配置列表
     * @param config
     * @return
     */
    @GetMapping("/config/list")
    TableDataInfo list(@SpringQueryMap SysConfig config);


    @GetMapping("/config/totalList")
    List<SysConfig> totalList(@SpringQueryMap SysConfig role);

    /**
     * 导出参数配置
     */
    /*@PostMapping("/export")
    void export(HttpServletResponse response, SysConfig config) throws IOException;*/

    /**
     * 根据参数编号获取详细信息
     * @param configId
     * @return
     */
    @GetMapping(value = "/config/{configId}")
    AjaxResult getInfo(@PathVariable("configId") Long configId);

    /**
     * 根据参数键名查询参数值
     * @param configKey
     * @return
     */
    @GetMapping(value = "/config/configKey/{configKey}")
    AjaxResult getConfigKey(@PathVariable("configKey") String configKey);

    /**
     * 新增参数配置
     * @param config
     * @return
     */
    @PostMapping("/config")
    AjaxResult add(@Validated @RequestBody SysConfig config);

    /**
     * 修改参数配置
     * @param config
     * @return
     */
    @PutMapping("/config")
    AjaxResult edit(@Validated @RequestBody SysConfig config);

    /**
     * 删除参数配置
     * @param configIds
     * @return
     */
    @DeleteMapping("/config/{configIds}")
    AjaxResult remove(@PathVariable("configIds") Long[] configIds);

    /**
     * 清空缓存
     * @return
     */
    @DeleteMapping("/config/clearCache")
    AjaxResult clearCache();

    /**
     * 根据配置文件Key值获取配置
     * @param configKey
     * @return
     */
    @PostMapping("/config/selectConfigByKey")
    String selectConfigByKey(@RequestParam("configKey") String configKey);

    /**
     * 根据配置ID获取参数配置信息
     * @param configId
     * @return
     */
    @PostMapping("/config/selectConfigById")
    SysConfig selectConfigById(@RequestParam("configId") Long configId);

    /**
     * 校验参数键名
     */
    @PostMapping("/config/checkConfigKeyUnique")
    public String checkConfigKeyUnique(@RequestBody SysConfig config);
}

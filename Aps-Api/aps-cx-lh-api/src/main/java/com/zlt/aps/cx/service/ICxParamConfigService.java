package com.zlt.aps.cx.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 排程参数配置对外暴露接口
 *
 * @author APS Team
 */
@FeignClient(contextId = "cxParamConfigService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxParamConfigService {

    /**
     * 获取排程参数配置列表
     *
     * @param cxParamConfig 查询条件
     * @return 分页结果
     */
    @PostMapping("/cxParamConfig/list")
    TableDataInfo list(@RequestBody CxParamConfig cxParamConfig);

    /**
     * 删除排程参数配置
     *
     * @param ids ID数组
     * @return 操作结果
     */
    @DeleteMapping("/cxParamConfig/remove")
    AjaxResult remove(@RequestBody Long[] ids);

    /**
     * 新增排程参数配置
     *
     * @param cxParamConfig 实体对象
     * @return 操作结果
     */
    @PostMapping("/cxParamConfig/add")
    AjaxResult add(@Validated @RequestBody CxParamConfig cxParamConfig);

    /**
     * 根据ID获取详细信息
     *
     * @param id 主键ID
     * @return 实体对象
     */
    @GetMapping(value = "/cxParamConfig/{billId}")
    CxParamConfig selectCxParamConfigById(@PathVariable("billId") Long id);

    /**
     * 修改排程参数配置
     *
     * @param cxParamConfig 实体对象
     * @return 操作结果
     */
    @PutMapping("/cxParamConfig/edit")
    AjaxResult edit(@Validated @RequestBody CxParamConfig cxParamConfig);

    /**
     * 导出排程参数配置列表
     *
     * @param cxParamConfig 查询条件
     * @return 导出数据列表
     */
    @PostMapping("/cxParamConfig/exportList")
    List<CxParamConfig> exportList(@RequestBody CxParamConfig cxParamConfig);

    /**
     * 导入数据
     *
     * @param list 导入数据列表
     * @param updateSupport 是否支持更新
     * @param importLogId 导入日志ID
     * @return 操作结果
     */
    @PostMapping("/cxParamConfig/importData")
    AjaxResult importData(@RequestBody List<CxParamConfig> list, 
                         @RequestParam("updateSupport") boolean updateSupport, 
                         @RequestParam("importLogId") Long importLogId);
}

package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhMoldChangePlan;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模具变动单Service接口
 *
 * @author zlt
 * @date 2021-06-17
 */
@FeignClient(contextId = "ILhMoldChangePlanService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:lh}")
public interface ILhMoldChangePlanService {

    /**
     * 查询模具变动单信息
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/moldChange/list")
    TableDataInfo list(@RequestBody LhMoldChangePlan lhMoldChangePlan);


    /**
     * 新增模具变动单信息
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/moldChange")
    AjaxResult add(@RequestBody LhMoldChangePlan lhMoldChangePlan);

    /**
     * 修改模具变动单信息
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/moldChange/edit")
    AjaxResult edit(@RequestBody LhMoldChangePlan lhMoldChangePlan);

    /**
     * 删除模具变动单信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/moldChange/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/moldChange/{id}")
    LhMoldChangePlan getInfo(@PathVariable("id") Long id);

    /**
     * 校验模具变动单唯一性
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/moldChange/checkMoldChangeUnique")
    String checkMoldChangeUnique(@RequestBody LhMoldChangePlan lhMoldChangePlan);

    /**
     * 导出模具变动单列表
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/moldChange/getList")
    List<LhMoldChangePlan> getList(@RequestBody LhMoldChangePlan lhMoldChangePlan);

    /**
     * 导入数据
     */
    @PostMapping("/moldChange/importData")
    public AjaxResult importData(@RequestBody List<LhMoldChangePlan> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 变动单发布
     */
    @PostMapping("/moldChange/publish")
    AjaxResult publish(@RequestBody LhMoldChangePlan lhMoldChangePlan);
}

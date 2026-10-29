package com.zlt.aps.lh.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.dto.LhDayPlanAdjustRequireImportDTO;
import com.zlt.aps.lh.api.domain.entity.LhDayPlanAdjustRequire;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 硫化日计划调整需求远程服务。
 */
@FeignClient(contextId = "ILhDayPlanAdjustRequireRemoteService",
        value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhDayPlanAdjustRequireRemoteService {

    /**
     * 查询硫化日计划调整需求列表。
     *
     * @param queryVO 查询条件
     * @return 分页列表
     */
    @ApiOperation("查询硫化日计划调整需求列表")
    @PostMapping("/lhDayPlanAdjustRequire/list")
    TableDataInfo list(@RequestBody LhDayPlanAdjustRequire queryVO);

    /**
     * 查询硫化日计划调整需求汇总。
     *
     * @param queryVO 查询条件，与列表一致
     * @return 全部匹配行的调整量汇总
     */
    @ApiOperation("查询硫化日计划调整需求汇总")
    @PostMapping("/lhDayPlanAdjustRequire/summary")
    AjaxResult summary(@RequestBody LhDayPlanAdjustRequire queryVO);

    /**
     * 保存当前行三次调整。
     *
     * @param entity 当前行数据
     * @return 保存结果
     */
    @ApiOperation("保存硫化日计划调整需求")
    @PostMapping("/lhDayPlanAdjustRequire/save")
    AjaxResult save(@RequestBody LhDayPlanAdjustRequire entity);

    /**
     * 导出硫化日计划调整需求。
     *
     * @param queryVO 查询条件，与列表一致但不分页
     * @param fileName 导出文件名
     * @return Excel 文件字节
     */
    @ApiOperation("导出硫化日计划调整需求")
    @PostMapping("/lhDayPlanAdjustRequire/exportData/{fileName}")
    byte[] exportData(@RequestBody LhDayPlanAdjustRequire queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入硫化日计划调整需求。
     *
     * @param importDTO 导入文件上下文及页面工厂、年月
     * @param updateSupport 已存在调整槽位是否允许覆盖
     * @return 导入结果
     */
    @ApiOperation("导入硫化日计划调整需求")
    @PostMapping("/lhDayPlanAdjustRequire/importData")
    AjaxResult importData(@RequestBody LhDayPlanAdjustRequireImportDTO importDTO,
                          @RequestParam("updateSupport") boolean updateSupport);
}

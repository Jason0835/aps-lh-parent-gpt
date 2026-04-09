package com.zlt.aps.cx.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型排程明细对外暴露接口
 *
 * @author APS Team
 */
@FeignClient(contextId = "cxScheduleDetailService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxScheduleDetailService {

    /**
     * 获取成型排程明细列表
     *
     * @param cxScheduleDetail 查询条件
     * @return 分页结果
     */
    @PostMapping("/cxScheduleDetail/list")
    TableDataInfo list(@RequestBody CxScheduleDetail cxScheduleDetail);

    /**
     * 删除成型排程明细
     *
     * @param ids ID数组
     * @return 操作结果
     */
    @DeleteMapping("/cxScheduleDetail/remove")
    AjaxResult remove(@RequestBody Long[] ids);

    /**
     * 新增成型排程明细
     *
     * @param cxScheduleDetail 实体对象
     * @return 操作结果
     */
    @PostMapping("/cxScheduleDetail/add")
    AjaxResult add(@Validated @RequestBody CxScheduleDetail cxScheduleDetail);

    /**
     * 根据ID获取详细信息
     *
     * @param id 主键ID
     * @return 实体对象
     */
    @GetMapping(value = "/cxScheduleDetail/{billId}")
    CxScheduleDetail selectCxScheduleDetailById(@PathVariable("billId") Long id);

    /**
     * 修改成型排程明细
     *
     * @param cxScheduleDetail 实体对象
     * @return 操作结果
     */
    @PutMapping("/cxScheduleDetail/edit")
    AjaxResult edit(@Validated @RequestBody CxScheduleDetail cxScheduleDetail);

    /**
     * 导出成型排程明细列表
     *
     * @param cxScheduleDetail 查询条件
     * @return 导出数据列表
     */
    @PostMapping("/cxScheduleDetail/exportList")
    List<CxScheduleDetail> exportList(@RequestBody CxScheduleDetail cxScheduleDetail);

    /**
     * 导入数据
     *
     * @param list 导入数据列表
     * @param updateSupport 是否支持更新
     * @param importLogId 导入日志ID
     * @return 操作结果
     */
    @PostMapping("/cxScheduleDetail/importData")
    AjaxResult importData(@RequestBody List<CxScheduleDetail> list,
                         @RequestParam("updateSupport") boolean updateSupport,
                         @RequestParam("importLogId") Long importLogId);
}

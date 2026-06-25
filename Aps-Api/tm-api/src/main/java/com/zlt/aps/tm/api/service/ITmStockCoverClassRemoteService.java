package com.zlt.aps.tm.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.entity.TmStockCoverClass;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 备库班数配置对外暴露接口
 *
 * @author zlt
 */
@FeignClient(contextId = "ITmStockCoverClassRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tm:/tm}")
public interface ITmStockCoverClassRemoteService {

    /**
     * 根据条件查询列表
     */
    @PostMapping("/tmStockCoverClass/list")
    TableDataInfo list(@RequestBody TmStockCoverClass stockCoverClass);

    /**
     * 保存（id为空则新增，id不为空则修改）
     */
    @PostMapping("/tmStockCoverClass/save")
    AjaxResult save(@RequestBody TmStockCoverClass stockCoverClass);

    /**
     * 根据id查询
     */
    @GetMapping(value = "/tmStockCoverClass/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @PostMapping("/tmStockCoverClass/checkUnique")
    String checkUnique(@RequestBody TmStockCoverClass stockCoverClass);

    /**
     * 校验配置规则交叉（确保新增/修改的规则不与现有规则有范围交叉）
     */
    @PostMapping("/tmStockCoverClass/checkRangeCross")
    String checkRangeCross(@RequestBody TmStockCoverClass stockCoverClass);

    /**
     * 批量删除(逻辑删)
     */
    @DeleteMapping("/tmStockCoverClass/{ids}")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 导出
     */
    @PostMapping("/tmStockCoverClass/exportData/{fileName}")
    byte[] exportData(@RequestBody TmStockCoverClass queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入
     */
    @PostMapping("/tmStockCoverClass/importData")
    AjaxResult importData(@RequestBody ImportContext importContext,
            @RequestParam("updateSupport") boolean updateSupport);
}

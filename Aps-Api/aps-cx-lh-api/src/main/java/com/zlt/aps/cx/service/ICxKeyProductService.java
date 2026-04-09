package com.zlt.aps.cx.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.entity.config.CxKeyProduct;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 关键产品配置对外暴露接口
 *
 * @author APS Team
 */
@FeignClient(contextId = "cxKeyProductService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxKeyProductService {

    /**
     * 获取关键产品配置列表
     *
     * @param cxKeyProduct 查询条件
     * @return 分页结果
     */
    @PostMapping("/cxKeyProduct/list")
    TableDataInfo list(@RequestBody CxKeyProduct cxKeyProduct);

    /**
     * 删除关键产品配置
     *
     * @param ids ID数组
     * @return 操作结果
     */
    @DeleteMapping("/cxKeyProduct/remove")
    AjaxResult remove(@RequestBody Long[] ids);

    /**
     * 新增关键产品配置
     *
     * @param cxKeyProduct 实体对象
     * @return 操作结果
     */
    @PostMapping("/cxKeyProduct/add")
    AjaxResult add(@Validated @RequestBody CxKeyProduct cxKeyProduct);

    /**
     * 根据ID获取详细信息
     *
     * @param id 主键ID
     * @return 实体对象
     */
    @GetMapping(value = "/cxKeyProduct/{billId}")
    CxKeyProduct selectCxKeyProductById(@PathVariable("billId") Long id);

    /**
     * 修改关键产品配置
     *
     * @param cxKeyProduct 实体对象
     * @return 操作结果
     */
    @PutMapping("/cxKeyProduct/edit")
    AjaxResult edit(@Validated @RequestBody CxKeyProduct cxKeyProduct);

    /**
     * 导出关键产品配置列表
     *
     * @param cxKeyProduct 查询条件
     * @return 导出数据列表
     */
    @PostMapping("/cxKeyProduct/exportList")
    List<CxKeyProduct> exportList(@RequestBody CxKeyProduct cxKeyProduct);

    /**
     * 导入数据
     *
     * @param list 导入数据列表
     * @param updateSupport 是否支持更新
     * @param importLogId 导入日志ID
     * @return 操作结果
     */
    @PostMapping("/cxKeyProduct/importData")
    AjaxResult importData(@RequestBody List<CxKeyProduct> list, 
                         @RequestParam("updateSupport") boolean updateSupport, 
                         @RequestParam("importLogId") Long importLogId);
}

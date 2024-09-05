package com.ruoyi.api.gateway.system.service;

import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 导入记录Service接口
 *
 * @author zlt
 * @date 2021-07-27
 */
@FeignClient(contextId = "IImportLogService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.system:system}")
public interface IImportLogService {

    /**
     * 查询导入记录列表
     */
    @PostMapping("/importLog/list")
    TableDataInfo list(@RequestBody ImportLog importLog);

    /**
     * 新增导入记录
     */
    @PostMapping("/importLog/add")
    ImportLog add(@RequestBody ImportLog importLog);

    /**
     * 修改导入记录
     */
    @PostMapping("/importLog/edit")
    AjaxResult edit(@RequestBody ImportLog importLog);

    /**
     * 删除导入记录
     */
    @DeleteMapping("/importLog/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/importLog/{id}")
    ImportLog getInfo(@PathVariable("id") Long id);

    /**
     * 校验导入记录唯一性
     */
    @PostMapping("/importLog/checkImportLogUnique")
    String checkImportLogUnique(@RequestBody ImportLog importLog);

    /**
     * 导出导入记录列表
     */
    @PostMapping("/importLog/getList")
    List<ImportLog> getList(@RequestBody ImportLog importLog);

}

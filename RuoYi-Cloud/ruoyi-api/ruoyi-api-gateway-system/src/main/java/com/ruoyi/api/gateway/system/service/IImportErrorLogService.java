package com.ruoyi.api.gateway.system.service;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;



/**
 * 导入错误日志记录Service接口
 * @author zlt
 * @date 2021-07-27
 */
@FeignClient(contextId = "IImportErrorLogService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.system:system}")
public interface IImportErrorLogService {

    /**
     * 查询导入错误日志记录列表
     */
    @PostMapping("/importErrorLog/list")
    TableDataInfo list(@RequestBody ImportErrorLog importErrorLog);

    /**
    * 新增导入错误日志记录
    */
    @PostMapping("/importErrorLog/add")
    AjaxResult add(@RequestBody ImportErrorLog importErrorLog);

    /**
     * 修改导入错误日志记录
     */
    @PostMapping("/importErrorLog/edit")
    AjaxResult edit(@RequestBody ImportErrorLog importErrorLog);

    /**
     * 删除导入错误日志记录
     */
    @DeleteMapping("/importErrorLog/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/importErrorLog/{id}")
    ImportErrorLog getInfo(@PathVariable("id") Long id);

    /**
     * 校验导入错误日志记录唯一性
     */
    @PostMapping("/importErrorLog/checkImportErrorLogUnique")
    String checkImportErrorLogUnique(@RequestBody ImportErrorLog importErrorLog);

    /**
     * 导出导入错误日志记录列表
     */
    @PostMapping("/importErrorLog/getList")
    List<ImportErrorLog> getList(@RequestBody ImportErrorLog importErrorLog);

    /**
     * 批量新增导入错误日志记录
     *
     * @param importErrorLogs 导入错误日志记录
     * @return 结果
     */
    @PostMapping("/importErrorLog/insertImportErrorLogList")
    public int insertImportErrorLogList(@RequestBody List<ImportErrorLog> importErrorLogs);
}

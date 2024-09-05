package com.ruoyi.api.gateway.system.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.api.gateway.system.domain.SysDictData;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@FeignClient(contextId = "iSysDictDataService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.system:system}")
public interface ISysDictDataService {

    @GetMapping("/dict/data/list")
    TableDataInfo list(@SpringQueryMap SysDictData dictData);

    /*@PostMapping("/dict/data/export")
    void export(HttpServletResponse response, SysDictData dictData) throws IOException;*/

    @GetMapping(value = "/dict/data/{dictCode}")
    AjaxResult getInfo(@PathVariable("dictCode") Long dictCode);

    @GetMapping(value = "/dict/data/type/{dictType}")
    AjaxResult dictType(@PathVariable("dictType") String dictType);

    @PostMapping("/dict/data")
    AjaxResult add(@Validated @RequestBody SysDictData dict);

    @PutMapping("/dict/data")
    AjaxResult edit(@Validated @RequestBody SysDictData dict);

    @DeleteMapping("/dict/data/{dictCodes}")
    AjaxResult remove(@PathVariable("dictCodes") Long[] dictCodes);

    /**
     * 根据数据字典ID获取数据字典信息
     * @param dictId
     * @return
     */
    @PostMapping(value = "/dict/data/selectDictDataById")
    SysDictData selectDictDataById(@RequestParam("dictId") Long dictId);

}

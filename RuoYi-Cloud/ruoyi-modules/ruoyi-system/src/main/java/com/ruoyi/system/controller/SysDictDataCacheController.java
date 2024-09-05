package com.ruoyi.system.controller;


import com.ruoyi.api.gateway.system.domain.SysDictData;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.system.service.ISysDictTypeService;
import com.ruoyi.system.utils.DictUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dict/cache")
public class SysDictDataCacheController extends BaseController {

    @Autowired
    ISysDictTypeService iSysDictTypeService;

    @PostMapping("/getType")
    public List<SysDictData> getType(@RequestParam("dictType") String dictType) {
        return DictUtils.getDictCache(dictType);
    }

    @PostMapping("/getLabel")
    public String getLabel(@RequestParam("dictType") String dictType, @RequestParam("dictValue") String dictValue) {

        return DictUtils.getLabel(dictType, dictValue);
    }

    @GetMapping("/reloadCache")
    public AjaxResult reloadCache() {
        DictUtils.initCache();
        return AjaxResult.success();
    }
}

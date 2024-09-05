package com.ruoyi.system.controller;

import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.ruoyi.system.service.ImportLogService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 导入记录Controller
 * 
 * @author zlt
 * @date 2021-07-27
 */
@RestController
@RequestMapping("/importLog")
public class ImportLogController extends BaseController
{
    @Autowired
    private ImportLogService importLogService;

    /**
     * 查询导入记录列表
     */
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody ImportLog importLog)
    {
        startPage("create_time desc");
        List<ImportLog> list = importLogService.selectImportLogList(importLog);
        return getDataTable(list);
    }

    /**
     * 获取导入记录详细信息
     */
    @GetMapping(value = "/{id}")
    public ImportLog getInfo(@PathVariable("id") Long id){
        return importLogService.selectImportLogById(id);
    }

    /**
     * 新增导入记录
     */
    @PostMapping("/add")
    public ImportLog add(@RequestBody ImportLog importLog){
        return importLogService.insertImportLog(importLog);
    }

    /**
     * 修改导入记录
     */
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody ImportLog importLog){
        return toAjax(importLogService.updateImportLog(importLog));
    }

    /**
     * 删除导入记录
     */
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(importLogService.deleteImportLogByIds(ids));
    }

    /**
     * 导出导入记录列表
     */
    @PostMapping("/getList")
    public List<ImportLog> getList(@RequestBody ImportLog importLog){
        startPage("create_time desc");
        return  importLogService.selectImportLogList(importLog);
    }

    /**
     * 校验导入记录唯一性
     */
    @ApiOperation("校验导入记录唯一性")
    @PostMapping("/checkImportLogUnique")
    public String checkImportLogUnique(@RequestBody ImportLog importLog){
        return importLogService.checkImportLogUnique(importLog);
    }

}

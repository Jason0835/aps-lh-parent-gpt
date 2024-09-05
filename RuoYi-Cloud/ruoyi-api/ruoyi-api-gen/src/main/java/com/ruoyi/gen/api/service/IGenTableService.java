package com.ruoyi.gen.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.gen.api.domain.GenTable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 代码生成部分对外暴露接口
 *
 *  * @author lbn
 *
 */
@FeignClient(contextId = "iGenTableService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.gen:code/gen}")
public interface IGenTableService {

    /**
     * 查询代码生成列表
     * @param genTable
     * @return
     */
    @PostMapping("/list")
    TableDataInfo genList(@RequestBody GenTable genTable);

    /**
     * 获取代码生成业务
     * @param talbleId
     * @return
     */
    @GetMapping(value = "/{tableId}")
    AjaxResult getInfo(@PathVariable("tableId") Long talbleId);

    /**
     * 查询数据库列表
     * @param genTable
     * @return
     */
    @PostMapping("/db/list")
    TableDataInfo dataList(@RequestBody GenTable genTable);

    /**
     * 查询数据表字段列表
     * @param tableId
     * @return
     */
    @GetMapping(value = "/column/{tableId}")
    TableDataInfo columnList(@PathVariable("tableId") Long tableId);

    /**
     * 导入表结构（保存）
     * @param tables
     * @return
     */
    @PostMapping("/importTable")
    AjaxResult importTableSave(@RequestParam("tables") String tables);

    /**
     * 修改保存代码生成业务
     * @param genTable
     * @return
     */
    @PutMapping("")
    AjaxResult editSave(@RequestBody GenTable genTable);

    /**
     * 删除代码生成
     * @param tableIds
     * @return
     */
    @DeleteMapping("/{tableIds}")
    AjaxResult remove(@PathVariable("tableIds") Long[] tableIds);

    /**
     * 预览代码
     * @param tableId
     * @return
     * @throws IOException
     */
    @GetMapping("/preview/{tableId}")
    AjaxResult preview(@PathVariable("tableId") Long tableId) throws IOException;

    /**
     * 生成代码（下载方式）
     * @param tableName
     * @throws IOException
     */
    @GetMapping("/download/{tableName}")
    byte[] download(@PathVariable("tableName") String tableName) throws IOException;

    /**
     * 生成代码（自定义路径）
     * @param tableName
     * @return
     */
    @GetMapping("/genCode/{tableName}")
    AjaxResult genCode(@PathVariable("tableName") String tableName);

    /**
     * 同步数据库
     * @param tableName
     * @return
     */
    @GetMapping("/synchDb/{tableName}")
    AjaxResult synchDb(@PathVariable("tableName") String tableName);

    /**
     * 批量生成代码
     * @param response
     * @param tables
     * @throws IOException
     */
    @GetMapping("/batchGenCode")
    void batchGenCode(@RequestBody HttpServletResponse response, @RequestParam("tables") String tables) throws IOException;

    /**
     * 获取表单对象
     * @param id
     * @return
     */
    @PostMapping("/selectGenTableById")
    public GenTable selectGenTableById(@RequestParam("id") Long id);
}

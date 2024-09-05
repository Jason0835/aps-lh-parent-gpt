package com.ruoyi.web.controller.gen;

import com.alibaba.fastjson.JSON;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common4ui.core.controller.BaseController;
import com.ruoyi.common4ui.core.domain.CxSelect;
import com.ruoyi.gen.api.domain.GenTable;
import com.ruoyi.gen.api.domain.GenTableColumn;
import com.ruoyi.gen.api.service.IGenTableService;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 代码生成 操作处理
 *
 * @author ruoyi
 */
@Controller
@RequestMapping("/tool/gen")
public class GenController extends BaseController
{
    private String prefix = "tool/gen";

    @Autowired
    private IGenTableService genTableService;

    @RequiresPermissions("tool:gen:view")
    @GetMapping()
    public String gen()
    {
        return prefix + "/gen";
    }

    /**
     * 查询代码生成列表
     */
    @RequiresPermissions("tool:gen:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo genList(GenTable genTable)
    {
        return genTableService.genList(genTable);
    }

    /**
     * 查询数据库列表
     */
    @RequiresPermissions("tool:gen:list")
    @PostMapping("/db/list")
    @ResponseBody
    public TableDataInfo dataList(GenTable genTable)
    {
        return genTableService.dataList(genTable);
    }

    /**
     * 查询数据表字段列表
     */
    @RequiresPermissions("tool:gen:list")
    @PostMapping("/column/list")
    @ResponseBody
    public TableDataInfo columnList(GenTableColumn genTableColumn)
    {
        return genTableService.columnList(genTableColumn.getTableId());
    }

    /**
     * 导入表结构
     */
    @RequiresPermissions("tool:gen:list")
    @GetMapping("/importTable")
    public String importTable()
    {
        return prefix + "/importTable";
    }

    /**
     * 导入表结构（保存）
     */
    @RequiresPermissions("tool:gen:list")
    @PostMapping("/importTable")
    @ResponseBody
    public AjaxResult importTableSave(String tables)
    {
        // 查询表信息
        return genTableService.importTableSave(tables);
    }

    /**
     * 修改代码生成业务
     */
    @GetMapping("/edit/{tableId}")
    public String edit(@PathVariable("tableId") Long tableId, ModelMap mmap)
    {
        GenTable table = genTableService.selectGenTableById(tableId);
//        List<GenTable> genTables = genTableService.selectGenTableAll();
        AjaxResult result = genTableService.getInfo(tableId);
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        String tables = JSON.toJSONString(data.get("tables"));
        List<GenTable> genTables = JSON.parseArray(tables, GenTable.class);
        List<CxSelect> cxSelect = new ArrayList<>();
        for (GenTable genTable : genTables)
        {
            if (!StringUtils.equals(table.getTableName(), genTable.getTableName()))
            {
                CxSelect cxTable = new CxSelect(genTable.getTableName(), genTable.getTableName() + '：' + genTable.getTableComment());
                List<CxSelect> cxColumns = new ArrayList<CxSelect>();
                for (GenTableColumn tableColumn : genTable.getColumns())
                {
                    cxColumns.add(new CxSelect(tableColumn.getColumnName(), tableColumn.getColumnName() + '：' + tableColumn.getColumnComment()));
                }
                cxTable.setS(cxColumns);
                cxSelect.add(cxTable);
            }
        }
        mmap.put("table", table);
        mmap.put("data", JSON.toJSON(cxSelect));
        return prefix + "/edit";
    }

    /**
     * 修改保存代码生成业务
     */
    @RequiresPermissions("tool:gen:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(GenTable genTable)
    {
        return genTableService.editSave(genTable);
    }

    @RequiresPermissions("tool:gen:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        Long[] arr= Convert.toLongArray(ids);
        return genTableService.remove(arr);
    }

    /**
     * 预览代码
     */
    @RequiresPermissions("tool:gen:preview")
    @GetMapping("/preview/{tableId}")
    @ResponseBody
    public AjaxResult preview(@PathVariable("tableId") Long tableId) throws IOException
    {
        return genTableService.preview(tableId);
    }

    /**
     * 生成代码（下载方式）
     */
    @RequiresPermissions("tool:gen:code")
//    @Log(title = "代码生成", businessType = BusinessType.GENCODE)
    @GetMapping("/download/{tableName}")
    public void download(HttpServletResponse response, @PathVariable("tableName") String tableName) throws IOException
    {
        byte[] data = genTableService.download(tableName);
        genCode(response, data);
    }

    /**
     * 生成代码（自定义路径）
     */
    @RequiresPermissions("tool:gen:code")
    @GetMapping("/genCode/{tableName}")
    @ResponseBody
    public AjaxResult genCode(@PathVariable("tableName") String tableName)
    {
        return genTableService.genCode(tableName);
    }

    /**
     * 同步数据库
     */
    @RequiresPermissions("tool:gen:edit")
    @GetMapping("/synchDb/{tableName}")
    @ResponseBody
    public AjaxResult synchDb(@PathVariable("tableName") String tableName)
    {
        return genTableService.synchDb(tableName);
    }

    /**
     * 批量生成代码
     */
    @RequiresPermissions("tool:gen:code")
    @GetMapping("/batchGenCode")
    @ResponseBody
    public void batchGenCode(HttpServletResponse response, String tables) throws IOException
    {
        genTableService.batchGenCode(response,tables);
    }

    /**
     * 生成zip文件
     */
    private void genCode(HttpServletResponse response, byte[] data) throws IOException
    {
        response.reset();
        response.setHeader("Content-Disposition", "attachment; filename=\"temp.zip\"");
        response.addHeader("Content-Length", "" + data.length);
        response.setContentType("application/octet-stream; charset=UTF-8");
        IOUtils.write(data, response.getOutputStream());
    }
}
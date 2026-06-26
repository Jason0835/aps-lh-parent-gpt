package com.zlt.aps.xwyy.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.utils.AppUtils;
import com.zlt.aps.xwyy.api.domain.entity.XwyyShiftConfig;
import com.zlt.aps.xwyy.mapper.XwyyShiftConfigMapper;
import com.zlt.aps.xwyy.service.IXwyyShiftConfigService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Api(tags = "纤维压延班次配置")
@RestController
@RequestMapping("/xwyyShiftConfig")
public class XwyyShiftConfigController extends AbstractDocBizController<XwyyShiftConfig> {
    @Resource
    private IXwyyShiftConfigService service;
    @Resource
    private XwyyShiftConfigMapper mapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody XwyyShiftConfig query) {
        return super.list(query);
    }

    @Log(title = "ui.data.column.xwyyShiftConfig.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody XwyyShiftConfig entity) {
        return super.save(entity);
    }

    @Log(title = "ui.data.column.xwyyShiftConfig.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody XwyyShiftConfig entity) {
        return super.save(entity);
    }

    @Log(title = "ui.data.column.xwyyShiftConfig.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public XwyyShiftConfig getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody XwyyShiftConfig entity) {
        return service.checkUnique(entity);
    }

    @Log(title = "ui.data.column.xwyyShiftConfig.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("启用/禁用")
    @PostMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody XwyyShiftConfig entity) {
        return service.changeStatus(entity);
    }

    @Log(title = "ui.data.column.xwyyShiftConfig.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext c, @RequestParam("updateSupport") boolean u) throws Exception {
        return super.importData(c, u);
    }

    @Log(title = "ui.data.column.xwyyShiftConfig.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody XwyyShiftConfig query, @PathVariable("fileName") String fileName, HttpServletResponse r) throws IOException {
        return super.exportData(query, fileName, r);
    }

    @Override
    protected List<XwyyShiftConfig> listExportData(XwyyShiftConfig output) {
        QueryWrapper<XwyyShiftConfig> w = new QueryWrapper<>();
        builderCondition(w, output);
        List<XwyyShiftConfig> l = mapper.selectList(w);
        AppUtils.formatData(l, getQueryFormulas());
        return l;
    }

    @Override
    protected IDocService getDocService() {
        return service;
    }

    @Override
    protected void builderCondition(QueryWrapper<XwyyShiftConfig> qw, XwyyShiftConfig vo) {
        qw.eq(PubUtil.isNotEmpty(vo.getFactoryCode()), "FACTORY_CODE", vo.getFactoryCode());
        qw.like(PubUtil.isNotEmpty(vo.getShiftCode()), "SHIFT_CODE", vo.getShiftCode());
        qw.like(PubUtil.isNotEmpty(vo.getShiftName()), "SHIFT_NAME", vo.getShiftName());
        qw.eq(vo.getIsActive() != null, "IS_ACTIVE", vo.getIsActive());
    }

    @Override
    protected String getTypeCode() {
        return "XWYY_SHIFT_CONFIG";
    }

    @Override
    protected String getOrderBy() {
        return "FACTORY_CODE asc, SHIFT_ORDER asc";
    }
}

package com.zlt.aps.tc.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.tc.api.domain.entity.TcGlueGroupOrder;
import com.zlt.aps.tc.mapper.TcGlueGroupOrderMapper;
import com.zlt.aps.tc.service.ITcGlueGroupOrderService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Slf4j
@Api(tags = "胎侧胶料组顺序")
@RestController
@RequestMapping("/tcGlueGroupOrder")
public class TcGlueGroupOrderController extends AbstractDocBizController<TcGlueGroupOrder> {

    @Autowired
    private ITcGlueGroupOrderService tcGlueGroupOrderService;

    @Resource
    private TcGlueGroupOrderMapper tcGlueGroupOrderMapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TcGlueGroupOrder queryVO) {
        return super.list(queryVO);
    }

    @Log(title = "ui.data.column.tc.GlueGroupOrder.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TcGlueGroupOrder billVO) {
        if (StringUtil.isBlank(billVO.getFactoryCode())) {
            billVO.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return super.save(billVO);
    }

    @Log(title = "ui.data.column.tc.GlueGroupOrder.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{id}")
    @Override
    public TcGlueGroupOrder getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody TcGlueGroupOrder query) {
        return tcGlueGroupOrderService.checkUnique(query);
    }

    @Log(title = "ui.data.column.tc.GlueGroupOrder.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "ui.data.column.tc.GlueGroupOrder.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TcGlueGroupOrder queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<TcGlueGroupOrder> listExportData(TcGlueGroupOrder obj) {
        QueryWrapper<TcGlueGroupOrder> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return tcGlueGroupOrderMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return tcGlueGroupOrderService;
    }

    @Override
    protected void builderCondition(QueryWrapper<TcGlueGroupOrder> queryWrapper, TcGlueGroupOrder queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("glueGroupCode")), "GLUE_GROUP_CODE", queryVO.getFieldValueByFieldName("glueGroupCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("glueGroupName")), "GLUE_GROUP_NAME", queryVO.getFieldValueByFieldName("glueGroupName"));
    }

    @Override
    protected String getTypeCode() {
        return "TC0908";
    }

    @Override
    protected String getOrderBy() {
        return "order_num asc";
    }
}
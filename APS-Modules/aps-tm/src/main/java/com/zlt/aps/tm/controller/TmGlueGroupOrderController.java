package com.zlt.aps.tm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.tm.api.domain.entity.TmGlueGroupOrder;
import com.zlt.aps.tm.mapper.TmGlueGroupOrderMapper;
import com.zlt.aps.tm.service.ITmGlueGroupOrderService;
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
@Api(tags = "胎面胶料组顺序")
@RestController
@RequestMapping("/tmGlueGroupOrder")
public class TmGlueGroupOrderController extends AbstractDocBizController<TmGlueGroupOrder> {

    @Autowired
    private ITmGlueGroupOrderService tmGlueGroupOrderService;

    @Resource
    private TmGlueGroupOrderMapper tmGlueGroupOrderMapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TmGlueGroupOrder queryVO) {
        return super.list(queryVO);
    }

    @Log(title = "ui.data.column.tm.GlueGroupOrder.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TmGlueGroupOrder billVO) {
        if (StringUtil.isBlank(billVO.getFactoryCode())) {
            billVO.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return super.save(billVO);
    }

    @Log(title = "ui.data.column.tm.GlueGroupOrder.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{id}")
    @Override
    public TmGlueGroupOrder getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody TmGlueGroupOrder query) {
        return tmGlueGroupOrderService.checkUnique(query);
    }

    @Log(title = "ui.data.column.tm.GlueGroupOrder.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData/{updateSupport}")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "ui.data.column.tm.GlueGroupOrder.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TmGlueGroupOrder queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<TmGlueGroupOrder> listExportData(TmGlueGroupOrder obj) {
        QueryWrapper<TmGlueGroupOrder> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return tmGlueGroupOrderMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return tmGlueGroupOrderService;
    }

    @Override
    protected void builderCondition(QueryWrapper<TmGlueGroupOrder> queryWrapper, TmGlueGroupOrder queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("glueGroupCode")), "GLUE_GROUP_CODE", queryVO.getFieldValueByFieldName("glueGroupCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("glueGroupName")), "GLUE_GROUP_NAME", queryVO.getFieldValueByFieldName("glueGroupName"));
    }

    @Override
    protected String getTypeCode() {
        return "TM0808";
    }

    @Override
    protected String getOrderBy() {
        return "order_num asc";
    }
}

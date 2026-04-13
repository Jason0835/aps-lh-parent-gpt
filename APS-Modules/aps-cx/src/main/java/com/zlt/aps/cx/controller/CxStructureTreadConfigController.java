package com.zlt.aps.cx.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.entity.CxStructureTreadConfig;
import com.zlt.aps.cx.mapper.CxStructureTreadConfigMapper;
import com.zlt.aps.cx.service.ICxStructureTreadConfigService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Slf4j
@Api(tags = "胎面整车配置")
@RestController
@RequestMapping("/cxStructureTreadConfig")
public class CxStructureTreadConfigController extends AbstractDocBizController<CxStructureTreadConfig> {

    @Autowired
    private ICxStructureTreadConfigService cxStructureTreadConfigService;

    @Resource
    private CxStructureTreadConfigMapper cxStructureTreadConfigMapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody CxStructureTreadConfig queryVO) {
        return super.list(queryVO);
    }

    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public CxStructureTreadConfig getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    @Log(title = "ui.data.column.mdmStructureTreadConfig.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody CxStructureTreadConfig entity) {
        return super.save(entity);
    }

    @Log(title = "ui.data.column.mdmStructureTreadConfig.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @Log(title = "ui.data.column.mdmStructureTreadConfig.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "胎面整车配置", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody CxStructureTreadConfig queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<CxStructureTreadConfig> listExportData(CxStructureTreadConfig obj) {
        QueryWrapper<CxStructureTreadConfig> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return cxStructureTreadConfigMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return cxStructureTreadConfigService;
    }

    @Override
    protected void builderCondition(QueryWrapper<CxStructureTreadConfig> queryWrapper, CxStructureTreadConfig queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("structureCode")), "STRUCTURE_CODE", queryVO.getFieldValueByFieldName("structureCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("treadCount")), "TREAD_COUNT", queryVO.getFieldValueByFieldName("treadCount"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dataVersion")), "DATA_VERSION", queryVO.getFieldValueByFieldName("dataVersion"));
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }
}

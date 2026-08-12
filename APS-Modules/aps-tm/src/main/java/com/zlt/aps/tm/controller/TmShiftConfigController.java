package com.zlt.aps.tm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.tm.api.domain.entity.TmShiftConfig;
import com.zlt.aps.tm.mapper.TmShiftConfigMapper;
import com.zlt.aps.tm.service.ITmShiftConfigService;
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
@Api(tags = "胎面班制配置")
@RestController
@RequestMapping("/tmShiftConfig")
public class TmShiftConfigController extends AbstractDocBizController<TmShiftConfig> {

    @Autowired
    private ITmShiftConfigService tmShiftConfigService;

    @Resource
    private TmShiftConfigMapper tmShiftConfigMapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TmShiftConfig queryVO) {
        return super.list(queryVO);
    }

    @Log(title = "ui.data.column.tm.ShiftConfig.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TmShiftConfig billVO) {
        if (StringUtil.isBlank(billVO.getFactoryCode())) {
            billVO.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return super.save(billVO);
    }

    @Log(title = "ui.data.column.tm.ShiftConfig.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{id}")
    @Override
    public TmShiftConfig getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody TmShiftConfig query) {
        return tmShiftConfigService.checkUnique(query);
    }

    @Log(title = "ui.data.column.tm.ShiftConfig.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "ui.data.column.tm.ShiftConfig.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TmShiftConfig queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<TmShiftConfig> listExportData(TmShiftConfig obj) {
        QueryWrapper<TmShiftConfig> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        wrapper.last("ORDER BY " + this.getOrderBy(obj));
        return tmShiftConfigMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return tmShiftConfigService;
    }

    @Override
    protected void builderCondition(QueryWrapper<TmShiftConfig> queryWrapper, TmShiftConfig queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("shiftCode")), "SHIFT_CODE", queryVO.getFieldValueByFieldName("shiftCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("shiftName")), "SHIFT_NAME", queryVO.getFieldValueByFieldName("shiftName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("shiftOrder")), "SHIFT_ORDER", queryVO.getFieldValueByFieldName("shiftOrder"));
    }

    @Override
    protected String getTypeCode() {
        return "TM0813";
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }
}

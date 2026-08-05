package com.zlt.aps.tm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.tm.api.domain.entity.TmGlueMachineReal;
import com.zlt.aps.tm.mapper.TmGlueMachineRealMapper;
import com.zlt.aps.tm.service.ITmGlueMachineRealService;
import com.zlt.aps.utils.AppUtils;
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
@Api(tags = "胎面胶料与机台关系")
@RestController
@RequestMapping("/tmGlueMachineReal")
public class TmGlueMachineRealController extends AbstractDocBizController<TmGlueMachineReal> {

    @Autowired
    private ITmGlueMachineRealService tmGlueMachineRealService;

    @Resource
    private TmGlueMachineRealMapper tmGlueMachineRealMapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TmGlueMachineReal queryVO) {
        return super.list(queryVO);
    }

    @Log(title = "ui.data.column.tm.GlueMachineReal.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TmGlueMachineReal billVO) {
        if (StringUtil.isBlank(billVO.getFactoryCode())) {
            billVO.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return super.save(billVO);
    }

    @Log(title = "ui.data.column.tm.GlueMachineReal.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{id}")
    @Override
    public TmGlueMachineReal getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody TmGlueMachineReal query) {
        return tmGlueMachineRealService.checkUnique(query);
    }

    @Log(title = "ui.data.column.tm.GlueMachineReal.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "ui.data.column.tm.GlueMachineReal.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TmGlueMachineReal queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected String[] getQueryFormulas() {
        return new String[]{
                "machineName->getcolvalue(T_TM_MACHINE_INFO, MACHINE_NAME, MACHINE_CODE, machineCode)"
        };
    }

    @Override
    protected List<TmGlueMachineReal> listExportData(TmGlueMachineReal obj) {
        QueryWrapper<TmGlueMachineReal> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<TmGlueMachineReal> list = tmGlueMachineRealMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return tmGlueMachineRealService;
    }

    @Override
    protected void builderCondition(QueryWrapper<TmGlueMachineReal> queryWrapper, TmGlueMachineReal queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("glueCode")), "GLUE_CODE", queryVO.getFieldValueByFieldName("glueCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("machineCode")), "MACHINE_CODE", queryVO.getFieldValueByFieldName("machineCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("enableStatus")), "ENABLE_STATUS", queryVO.getFieldValueByFieldName("enableStatus"));
    }

    @Override
    protected String getTypeCode() {
        return "TM0802";
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }
}

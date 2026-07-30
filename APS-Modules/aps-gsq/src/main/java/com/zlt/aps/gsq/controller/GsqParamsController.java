package com.zlt.aps.gsq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.gsq.entity.GsqParams;
import com.zlt.aps.gsq.mapper.GsqParamsMapper;
import com.zlt.aps.gsq.service.GsqParamsService;
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

/**
 * 钢丝圈排程参数配置 Controller
 *
 * @author zlt
 * @date 2021-05-25
 */
@Slf4j
@Api(tags = "钢丝圈排程参数配置")
@RestController
@RequestMapping("/gsq/params")
public class GsqParamsController extends AbstractDocBizController<GsqParams> {

    @Autowired
    private GsqParamsService gsqParamsService;

    @Resource
    private GsqParamsMapper gsqParamsMapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody GsqParams queryVO) {
        return super.list(queryVO);
    }

    @Log(title = "ui.data.column.gsq.params.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody GsqParams billVO) {
        if (StringUtil.isBlank(billVO.getFactoryCode())) {
            billVO.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return super.save(billVO);
    }

    @Log(title = "ui.data.column.gsq.params.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{id}")
    @Override
    public GsqParams getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody GsqParams query) {
        return gsqParamsService.checkUnique(query);
    }

    @Log(title = "ui.data.column.gsq.params.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "ui.data.column.gsq.params.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody GsqParams queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<GsqParams> listExportData(GsqParams obj) {
        QueryWrapper<GsqParams> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return gsqParamsMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return gsqParamsService;
    }

    @Override
    protected void builderCondition(QueryWrapper<GsqParams> queryWrapper, GsqParams queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("paramCode")), "PARAM_CODE", queryVO.getFieldValueByFieldName("paramCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("paramName")), "PARAM_NAME", queryVO.getFieldValueByFieldName("paramName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("paramGroup")), "PARAM_GROUP", queryVO.getFieldValueByFieldName("paramGroup"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("valueType")), "VALUE_TYPE", queryVO.getFieldValueByFieldName("valueType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("enableStatus")), "ENABLE_STATUS", queryVO.getFieldValueByFieldName("enableStatus"));
    }

    @Override
    protected String getTypeCode() {
        return "GSQ0801";
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }
}

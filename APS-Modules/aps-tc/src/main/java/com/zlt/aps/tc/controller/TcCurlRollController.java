package com.zlt.aps.tc.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.tc.api.domain.entity.TcCurlRoll;
import com.zlt.aps.tc.mapper.TcCurlRollMapper;
import com.zlt.aps.tc.service.ITcCurlRollService;
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
@Api(tags = "胎侧卷曲长度")
@RestController
@RequestMapping("/tcCurlRoll")
public class TcCurlRollController extends AbstractDocBizController<TcCurlRoll> {

    @Autowired
    private ITcCurlRollService tcCurlRollService;

    @Resource
    private TcCurlRollMapper tcCurlRollMapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TcCurlRoll queryVO) {
        return super.list(queryVO);
    }

    @Log(title = "ui.data.column.tc.CurlRoll.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TcCurlRoll billVO) {
        if (StringUtil.isBlank(billVO.getFactoryCode())) {
            billVO.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return super.save(billVO);
    }

    @Log(title = "ui.data.column.tc.CurlRoll.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{id}")
    @Override
    public TcCurlRoll getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody TcCurlRoll query) {
        return tcCurlRollService.checkUnique(query);
    }

    @Log(title = "ui.data.column.tc.CurlRoll.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "ui.data.column.tc.CurlRoll.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TcCurlRoll queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<TcCurlRoll> listExportData(TcCurlRoll obj) {
        QueryWrapper<TcCurlRoll> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        wrapper.last("ORDER BY " + this.getOrderBy(obj));
        return tcCurlRollMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return tcCurlRollService;
    }

    @Override
    protected void builderCondition(QueryWrapper<TcCurlRoll> queryWrapper, TcCurlRoll queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("sidewallCode")), "SIDEWALL_CODE", queryVO.getFieldValueByFieldName("sidewallCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("curlLength")), "CURL_LENGTH", queryVO.getFieldValueByFieldName("curlLength"));
    }

    @Override
    protected String getTypeCode() {
        return "TC0911";
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }
}

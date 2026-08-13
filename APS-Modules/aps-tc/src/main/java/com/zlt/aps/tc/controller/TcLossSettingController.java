package com.zlt.aps.tc.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.tc.api.domain.entity.TcLossSetting;
import com.zlt.aps.tc.mapper.TcLossSettingMapper;
import com.zlt.aps.tc.service.ITcLossSettingService;
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
@Api(tags = "胎侧损耗率设定")
@RestController
@RequestMapping("/tcLossSetting")
public class TcLossSettingController extends AbstractDocBizController<TcLossSetting> {

    @Autowired
    private ITcLossSettingService tcLossSettingService;

    @Resource
    private TcLossSettingMapper tcLossSettingMapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TcLossSetting queryVO) {
        return super.list(queryVO);
    }

    @Log(title = "ui.data.column.tc.LossSetting.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TcLossSetting billVO) {
        if (StringUtil.isBlank(billVO.getFactoryCode())) {
            billVO.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        tcLossSettingService.validateLossRate(billVO);
        return super.save(billVO);
    }

    @Log(title = "ui.data.column.tc.LossSetting.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{id}")
    @Override
    public TcLossSetting getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody TcLossSetting query) {
        return tcLossSettingService.checkUnique(query);
    }

    @Log(title = "ui.data.column.tc.LossSetting.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "ui.data.column.tc.LossSetting.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TcLossSetting queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<TcLossSetting> listExportData(TcLossSetting obj) {
        QueryWrapper<TcLossSetting> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        wrapper.last("ORDER BY " + this.getOrderBy(obj));
        return tcLossSettingMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return tcLossSettingService;
    }

    @Override
    protected void builderCondition(QueryWrapper<TcLossSetting> queryWrapper, TcLossSetting queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("sidewallCode")), "SIDEWALL_CODE", queryVO.getFieldValueByFieldName("sidewallCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("machineCode")), "MACHINE_CODE", queryVO.getFieldValueByFieldName("machineCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("enableStatus")), "ENABLE_STATUS", queryVO.getFieldValueByFieldName("enableStatus"));
    }

    @Override
    protected String getTypeCode() {
        return "TC0910";
    }

    @Override
    protected String getOrderBy() {
        return "update_time desc";
    }
}

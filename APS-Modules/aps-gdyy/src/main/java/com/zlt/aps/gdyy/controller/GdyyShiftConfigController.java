package com.zlt.aps.gdyy.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.gdyy.api.domain.entity.GdyyShiftConfig;
import com.zlt.aps.gdyy.mapper.GdyyShiftConfigMapper;
import com.zlt.aps.gdyy.service.IGdyyShiftConfigService;
import com.zlt.aps.utils.AppUtils;
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

/**
 * 钢带压延班次配置 控制层。
 */
@Api(tags = "钢带压延班次配置")
@RestController
@RequestMapping("/gdyyShiftConfig")
public class GdyyShiftConfigController extends AbstractDocBizController<GdyyShiftConfig> {

    @Resource
    private IGdyyShiftConfigService gdyyShiftConfigService;

    @Resource
    private GdyyShiftConfigMapper gdyyShiftConfigMapper;

    @ApiOperation("查询钢带压延班次配置列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody GdyyShiftConfig queryVO) {
        return super.list(queryVO);
    }

    @Log(title = "ui.data.column.gdyyShiftConfig.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增钢带压延班次配置")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody GdyyShiftConfig shiftConfig) {
        return super.save(shiftConfig);
    }

    @Log(title = "ui.data.column.gdyyShiftConfig.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑钢带压延班次配置")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody GdyyShiftConfig shiftConfig) {
        return super.save(shiftConfig);
    }

    @Log(title = "ui.data.column.gdyyShiftConfig.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除钢带压延班次配置")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取钢带压延班次配置详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public GdyyShiftConfig getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @ApiOperation("校验钢带压延班次配置唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody GdyyShiftConfig shiftConfig) {
        return gdyyShiftConfigService.checkUnique(shiftConfig);
    }

    @Log(title = "ui.data.column.gdyyShiftConfig.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改钢带压延班次启用状态")
    @PostMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody GdyyShiftConfig shiftConfig) {
        LambdaUpdateWrapper<GdyyShiftConfig> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(GdyyShiftConfig::getId, shiftConfig.getId())
               .set(GdyyShiftConfig::getIsActive, shiftConfig.getIsActive());
        gdyyShiftConfigMapper.update(null, wrapper);
        return AjaxResult.success();
    }

    @Log(title = "ui.data.column.gdyyShiftConfig.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入钢带压延班次配置")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "ui.data.column.gdyyShiftConfig.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出钢带压延班次配置")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody GdyyShiftConfig queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<GdyyShiftConfig> listExportData(GdyyShiftConfig obj) {
        QueryWrapper<GdyyShiftConfig> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<GdyyShiftConfig> list = gdyyShiftConfigMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return gdyyShiftConfigService;
    }

    @Override
    protected void builderCondition(QueryWrapper<GdyyShiftConfig> queryWrapper, GdyyShiftConfig queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getShiftCode()), "SHIFT_CODE", queryVO.getShiftCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getShiftName()), "SHIFT_NAME", queryVO.getShiftName());
        queryWrapper.eq(queryVO.getIsActive() != null, "IS_ACTIVE", queryVO.getIsActive());
    }

    @Override
    protected String getTypeCode() {
        return "GDYY_SHIFT_CONFIG";
    }

    @Override
    protected String getOrderBy() {
        return "SHIFT_ORDER asc, UPDATE_TIME desc";
    }
}

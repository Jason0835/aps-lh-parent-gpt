package com.zlt.aps.cd15.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftConfig;
import com.zlt.aps.cd15.mapper.Cd15ShiftConfigMapper;
import com.zlt.aps.cd15.service.ICd15ShiftConfigService;
import com.zlt.aps.common.core.constant.ApsConstant;
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
 * 班次配置控制层。
 */
@Api(tags = "班次配置")
@RestController
@RequestMapping("/cd15ShiftConfig")
public class Cd15ShiftConfigController extends AbstractDocBizController<Cd15ShiftConfig> {

    @Resource
    private ICd15ShiftConfigService cd15ShiftConfigService;

    @Resource
    private Cd15ShiftConfigMapper cd15ShiftConfigMapper;

    /** 查询班次配置列表 */
    @ApiOperation("查询班次配置列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd15ShiftConfig queryVO) {
        return super.list(queryVO);
    }

    /** 新增班次配置 */
    @Log(title = "ui.data.column.cd15ShiftConfig.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增班次配置")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Cd15ShiftConfig shiftConfig) {
        return super.save(shiftConfig);
    }

    /** 编辑班次配置 */
    @Log(title = "ui.data.column.cd15ShiftConfig.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑班次配置")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd15ShiftConfig shiftConfig) {
        return super.save(shiftConfig);
    }

    /** 删除班次配置 */
    @Log(title = "ui.data.column.cd15ShiftConfig.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除班次配置")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /** 获取班次配置详情 */
    @ApiOperation("获取班次配置详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public Cd15ShiftConfig getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /** 校验班次配置唯一性 */
    @ApiOperation("校验班次配置唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody Cd15ShiftConfig shiftConfig) {
        return cd15ShiftConfigService.checkUnique(shiftConfig);
    }

    /** 修改班次启用状态 */
    @Log(title = "ui.data.column.cd15ShiftConfig.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改班次启用状态")
    @PostMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody Cd15ShiftConfig shiftConfig) {
        LambdaUpdateWrapper<Cd15ShiftConfig> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Cd15ShiftConfig::getId, shiftConfig.getId())
               .set(Cd15ShiftConfig::getIsActive, shiftConfig.getIsActive());
        cd15ShiftConfigMapper.update(null, wrapper);
        return AjaxResult.success();
    }

    /** 导入班次配置 */
    @Log(title = "ui.data.column.cd15ShiftConfig.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入班次配置")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /** 导出班次配置 */
    @Log(title = "ui.data.column.cd15ShiftConfig.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出班次配置")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody Cd15ShiftConfig queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<Cd15ShiftConfig> listExportData(Cd15ShiftConfig obj) {
        QueryWrapper<Cd15ShiftConfig> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<Cd15ShiftConfig> list = cd15ShiftConfigMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return cd15ShiftConfigService;
    }

    @Override
    protected void builderCondition(QueryWrapper<Cd15ShiftConfig> queryWrapper, Cd15ShiftConfig queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getShiftCode()), "SHIFT_CODE", queryVO.getShiftCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getShiftName()), "SHIFT_NAME", queryVO.getShiftName());
        queryWrapper.eq(queryVO.getIsActive() != null, "IS_ACTIVE", queryVO.getIsActive());
    }

    @Override
    protected String getTypeCode() {
        return "CD15_SHIFT_CONFIG";
    }

    @Override
    protected String getOrderBy() {
        return "SHIFT_ORDER asc, UPDATE_TIME desc";
    }
}
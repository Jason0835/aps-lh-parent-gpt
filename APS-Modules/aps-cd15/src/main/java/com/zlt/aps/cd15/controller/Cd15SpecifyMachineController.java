package com.zlt.aps.cd15.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd15.api.domain.entity.Cd15SpecifyMachine;
import com.zlt.aps.cd15.mapper.Cd15SpecifyMachineMapper;
import com.zlt.aps.cd15.service.ICd15SpecifyMachineService;
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
import java.util.stream.Collectors;

/**
 * 斜裁定点机台控制层。
 */
@Api(tags = "斜裁定点机台")
@RestController
@RequestMapping("/specifyMachine")
public class Cd15SpecifyMachineController extends AbstractDocBizController<Cd15SpecifyMachine> {

    @Resource
    private ICd15SpecifyMachineService cd15SpecifyMachineService;

    @Resource
    private Cd15SpecifyMachineMapper cd15SpecifyMachineMapper;

    /** 查询斜裁定点机台列表 */
    @ApiOperation("查询斜裁定点机台列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd15SpecifyMachine queryVO) {
        return super.list(queryVO);
    }

    /** 新增斜裁定点机台 */
    @Log(title = "ui.data.column.cd15SpecifyMachine.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增斜裁定点机台")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Cd15SpecifyMachine specifyMachine) {
        return super.save(specifyMachine);
    }

    /** 编辑斜裁定点机台 */
    @Log(title = "ui.data.column.cd15SpecifyMachine.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑斜裁定点机台")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd15SpecifyMachine specifyMachine) {
        return super.save(specifyMachine);
    }

    /** 删除斜裁定点机台 */
    @Log(title = "ui.data.column.cd15SpecifyMachine.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除斜裁定点机台")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /** 清空斜裁定点机台 */
    @Log(title = "ui.data.column.cd15SpecifyMachine.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("清空斜裁定点机台")
    @PostMapping("/removeAll")
    public AjaxResult removeAll(@RequestBody Cd15SpecifyMachine queryVO) {
        QueryWrapper<Cd15SpecifyMachine> wrapper = new QueryWrapper<>();
        builderCondition(wrapper, queryVO);
        List<Long> ids = cd15SpecifyMachineMapper.selectList(wrapper).stream()
                .map(Cd15SpecifyMachine::getId)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return AjaxResult.success();
        }
        return super.removeByIds(ids);
    }

    /** 获取斜裁定点机台详情 */
    @ApiOperation("获取斜裁定点机台详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public Cd15SpecifyMachine getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /** 校验斜裁定点机台唯一性 */
    @ApiOperation("校验斜裁定点机台唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody Cd15SpecifyMachine specifyMachine) {
        return cd15SpecifyMachineService.checkUnique(specifyMachine);
    }

    /** 导入斜裁定点机台 */
    @Log(title = "ui.data.column.cd15SpecifyMachine.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入斜裁定点机台")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /** 导出斜裁定点机台 */
    @Log(title = "ui.data.column.cd15SpecifyMachine.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出斜裁定点机台")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody Cd15SpecifyMachine queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<Cd15SpecifyMachine> listExportData(Cd15SpecifyMachine obj) {
        QueryWrapper<Cd15SpecifyMachine> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<Cd15SpecifyMachine> list = cd15SpecifyMachineMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return cd15SpecifyMachineService;
    }

    @Override
    protected void builderCondition(QueryWrapper<Cd15SpecifyMachine> queryWrapper, Cd15SpecifyMachine queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getSteelStripCode()), "STEEL_STRIP_CODE", queryVO.getSteelStripCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getJobType()), "JOB_TYPE", queryVO.getJobType());
    }

    @Override
    protected String getTypeCode() {
        return "CD15_SPECIFY_MACHINE";
    }

    @Override
    protected String getOrderBy() {
        return "MACHINE_CODE asc, STEEL_STRIP_CODE asc, UPDATE_TIME desc";
    }
}
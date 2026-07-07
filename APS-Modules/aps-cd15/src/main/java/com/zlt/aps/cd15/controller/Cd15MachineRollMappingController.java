package com.zlt.aps.cd15.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineRollMapping;
import com.zlt.aps.cd15.mapper.Cd15MachineRollMappingMapper;
import com.zlt.aps.cd15.service.ICd15MachineRollMappingService;
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
 * 斜裁大卷与机台映射控制层。
 */
@Api(tags = "斜裁大卷与机台映射")
@RestController
@RequestMapping("/cd15MachineRollMapping")
public class Cd15MachineRollMappingController extends AbstractDocBizController<Cd15MachineRollMapping> {

    @Resource
    private ICd15MachineRollMappingService cd15MachineRollMappingService;

    @Resource
    private Cd15MachineRollMappingMapper cd15MachineRollMappingMapper;

    /** 查询斜裁大卷与机台映射列表 */
    @ApiOperation("查询斜裁大卷与机台映射列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd15MachineRollMapping queryVO) {
        return super.list(queryVO);
    }

    /** 新增斜裁大卷与机台映射 */
    @Log(title = "ui.data.column.cd15MachineRollMapping.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增斜裁大卷与机台映射")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Cd15MachineRollMapping entity) {
        return super.save(entity);
    }

    /** 编辑斜裁大卷与机台映射 */
    @Log(title = "ui.data.column.cd15MachineRollMapping.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑斜裁大卷与机台映射")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd15MachineRollMapping entity) {
        return super.save(entity);
    }

    /** 删除斜裁大卷与机台映射 */
    @Log(title = "ui.data.column.cd15MachineRollMapping.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除斜裁大卷与机台映射")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /** 清空斜裁大卷与机台映射 */
    @Log(title = "ui.data.column.cd15MachineRollMapping.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("清空斜裁大卷与机台映射")
    @PostMapping("/removeAll")
    public AjaxResult removeAll(@RequestBody Cd15MachineRollMapping queryVO) {
        QueryWrapper<Cd15MachineRollMapping> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, queryVO);
        List<Long> ids = cd15MachineRollMappingMapper.selectList(wrapper).stream()
                .map(Cd15MachineRollMapping::getId)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return AjaxResult.success();
        }
        return super.removeByIds(ids);
    }

    /** 获取斜裁大卷与机台映射详情 */
    @ApiOperation("获取斜裁大卷与机台映射详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public Cd15MachineRollMapping getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /** 校验斜裁大卷与机台映射唯一性 */
    @ApiOperation("校验斜裁大卷与机台映射唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody Cd15MachineRollMapping entity) {
        return cd15MachineRollMappingService.checkUnique(entity);
    }

    /** 导入斜裁大卷与机台映射 */
    @Log(title = "ui.data.column.cd15MachineRollMapping.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入斜裁大卷与机台映射")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /** 导出斜裁大卷与机台映射 */
    @Log(title = "ui.data.column.cd15MachineRollMapping.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出斜裁大卷与机台映射")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody Cd15MachineRollMapping queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<Cd15MachineRollMapping> listExportData(Cd15MachineRollMapping obj) {
        QueryWrapper<Cd15MachineRollMapping> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<Cd15MachineRollMapping> list = cd15MachineRollMappingMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return cd15MachineRollMappingService;
    }

    @Override
    protected void builderCondition(QueryWrapper<Cd15MachineRollMapping> queryWrapper, Cd15MachineRollMapping queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getBigRollCode()), "BIG_ROLL_CODE", queryVO.getBigRollCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getShiftCode()), "SHIFT_CODE", queryVO.getShiftCode());
    }

    @Override
    protected String getTypeCode() {
        return "CD15_MACHINE_ROLL_MAPPING";
    }

    @Override
    protected String getOrderBy() {
        return "MACHINE_CODE asc, BIG_ROLL_CODE asc, SHIFT_CODE asc, UPDATE_TIME desc";
    }
}

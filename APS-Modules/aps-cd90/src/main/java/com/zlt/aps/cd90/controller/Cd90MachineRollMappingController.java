package com.zlt.aps.cd90.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineRollMapping;
import com.zlt.aps.cd90.mapper.Cd90MachineRollMappingMapper;
import com.zlt.aps.cd90.service.ICd90MachineRollMappingService;
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
 * 直裁大卷与机台映射控制层。
 */
@Api(tags = "直裁大卷与机台映射")
@RestController
@RequestMapping("/cd90MachineRollMapping")
public class Cd90MachineRollMappingController extends AbstractDocBizController<Cd90MachineRollMapping> {

    @Resource
    private ICd90MachineRollMappingService cd90MachineRollMappingService;

    @Resource
    private Cd90MachineRollMappingMapper cd90MachineRollMappingMapper;

    /**
     * 查询直裁大卷与机台映射列表
     */
    @ApiOperation("查询直裁大卷与机台映射列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd90MachineRollMapping queryVO) {
        return super.list(queryVO);
    }

    /**
     * 新增直裁大卷与机台映射
     */
    @Log(title = "ui.data.column.cd90MachineRollMapping.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增直裁大卷与机台映射")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Cd90MachineRollMapping entity) {
        return super.save(entity);
    }

    /**
     * 编辑直裁大卷与机台映射
     */
    @Log(title = "ui.data.column.cd90MachineRollMapping.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑直裁大卷与机台映射")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd90MachineRollMapping entity) {
        return super.save(entity);
    }

    /**
     * 删除直裁大卷与机台映射
     */
    @Log(title = "ui.data.column.cd90MachineRollMapping.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除直裁大卷与机台映射")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /**
     * 清空直裁大卷与机台映射
     */
    @Log(title = "ui.data.column.cd90MachineRollMapping.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("清空直裁大卷与机台映射")
    @PostMapping("/removeAll")
    public AjaxResult removeAll(@RequestBody Cd90MachineRollMapping queryVO) {
        QueryWrapper<Cd90MachineRollMapping> wrapper = new QueryWrapper<>();
        builderCondition(wrapper, queryVO);
        List<Long> ids = cd90MachineRollMappingMapper.selectList(wrapper).stream()
                .map(Cd90MachineRollMapping::getId)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return AjaxResult.success();
        }
        return super.removeByIds(ids);
    }

    /**
     * 获取直裁大卷与机台映射详情
     */
    @ApiOperation("获取直裁大卷与机台映射详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public Cd90MachineRollMapping getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /**
     * 校验直裁大卷与机台映射唯一性
     */
    @ApiOperation("校验直裁大卷与机台映射唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody Cd90MachineRollMapping entity) {
        return cd90MachineRollMappingService.checkUnique(entity);
    }

    /**
     * 导入直裁大卷与机台映射
     */
    @Log(title = "ui.data.column.cd90MachineRollMapping.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入直裁大卷与机台映射")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出直裁大卷与机台映射
     */
    @Log(title = "ui.data.column.cd90MachineRollMapping.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出直裁大卷与机台映射")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody Cd90MachineRollMapping queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<Cd90MachineRollMapping> listExportData(Cd90MachineRollMapping obj) {
        QueryWrapper<Cd90MachineRollMapping> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<Cd90MachineRollMapping> list = cd90MachineRollMappingMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return cd90MachineRollMappingService;
    }

    @Override
    protected void builderCondition(QueryWrapper<Cd90MachineRollMapping> queryWrapper, Cd90MachineRollMapping queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getBigRollCode()), "BIG_ROLL_CODE", queryVO.getBigRollCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getCordFabricCode()), "CORD_FABRIC_CODE", queryVO.getCordFabricCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
    }

    @Override
    protected String getTypeCode() {
        return "CD90_MACHINE_ROLL_MAPPING";
    }

    @Override
    protected String getOrderBy() {
        return "MACHINE_CODE asc, BIG_ROLL_CODE asc, UPDATE_TIME desc";
    }
}

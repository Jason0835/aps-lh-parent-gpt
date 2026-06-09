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

@Api(tags = "直裁大卷与机台映射")
@RestController
@RequestMapping("/cd90MachineRollMapping")
public class Cd90MachineRollMappingController extends AbstractDocBizController<Cd90MachineRollMapping> {

    @Resource
    private ICd90MachineRollMappingService cd90MachineRollMappingService;
    @Resource
    private Cd90MachineRollMappingMapper cd90MachineRollMappingMapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd90MachineRollMapping queryVO) {
        return super.list(queryVO);
    }

    @Log(title = "ui.data.column.machineRollMapping.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Cd90MachineRollMapping entity) {
        return super.save(entity);
    }

    @Log(title = "ui.data.column.machineRollMapping.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd90MachineRollMapping entity) {
        return super.save(entity);
    }

    @Log(title = "ui.data.column.machineRollMapping.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public Cd90MachineRollMapping getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody Cd90MachineRollMapping entity) {
        return cd90MachineRollMappingService.checkUnique(entity);
    }

    @Log(title = "ui.data.column.machineRollMapping.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "ui.data.column.machineRollMapping.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出")
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
    protected void builderCondition(QueryWrapper<Cd90MachineRollMapping> qw, Cd90MachineRollMapping vo) {
        qw.eq(PubUtil.isNotEmpty(vo.getFactoryCode()), "FACTORY_CODE", vo.getFactoryCode());
        qw.like(PubUtil.isNotEmpty(vo.getBigRollCode()), "BIG_ROLL_CODE", vo.getBigRollCode());
        qw.eq(PubUtil.isNotEmpty(vo.getMachineCode()), "MACHINE_CODE", vo.getMachineCode());
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
package com.zlt.aps.cd15.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd15.api.domain.entity.Cd15LossSetting;
import com.zlt.aps.cd15.mapper.Cd15LossSettingMapper;
import com.zlt.aps.cd15.service.ICd15LossSettingService;
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
 * 斜裁损耗率设定控制层。
 */
@Api(tags = "斜裁损耗率设定")
@RestController
@RequestMapping("/cd15LossSetting")
public class Cd15LossSettingController extends AbstractDocBizController<Cd15LossSetting> {

    @Resource
    private ICd15LossSettingService cd15LossSettingService;

    @Resource
    private Cd15LossSettingMapper cd15LossSettingMapper;

    @ApiOperation("查询斜裁损耗率列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd15LossSetting queryVO) {
        return super.list(queryVO);
    }

    @Log(title = "ui.data.column.lossSetting.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增斜裁损耗率")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Cd15LossSetting entity) {
        return super.save(entity);
    }

    @Log(title = "ui.data.column.lossSetting.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑斜裁损耗率")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd15LossSetting entity) {
        return super.save(entity);
    }

    @Log(title = "ui.data.column.lossSetting.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除斜裁损耗率")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取斜裁损耗率详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public Cd15LossSetting getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @ApiOperation("校验斜裁损耗率唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody Cd15LossSetting entity) {
        return cd15LossSettingService.checkUnique(entity);
    }

    @Log(title = "ui.data.column.lossSetting.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入斜裁损耗率")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "ui.data.column.lossSetting.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出斜裁损耗率")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody Cd15LossSetting queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<Cd15LossSetting> listExportData(Cd15LossSetting obj) {
        QueryWrapper<Cd15LossSetting> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<Cd15LossSetting> list = cd15LossSettingMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return cd15LossSettingService;
    }

    @Override
    protected void builderCondition(QueryWrapper<Cd15LossSetting> queryWrapper, Cd15LossSetting queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getSteelStripCode()), "STEEL_STRIP_CODE", queryVO.getSteelStripCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
    }

    @Override
    protected String getTypeCode() {
        return "CD15_LOSS_SETTING";
    }

    @Override
    protected String getOrderBy() {
        return "MACHINE_CODE asc, STEEL_STRIP_CODE asc, UPDATE_TIME desc";
    }
}

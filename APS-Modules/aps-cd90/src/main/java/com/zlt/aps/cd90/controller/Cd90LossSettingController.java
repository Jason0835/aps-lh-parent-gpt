package com.zlt.aps.cd90.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd90.api.domain.entity.Cd90LossSetting;
import com.zlt.aps.cd90.mapper.Cd90LossSettingMapper;
import com.zlt.aps.cd90.service.ICd90LossSettingService;
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
 * 直裁损耗率设定控制层。
 */
@Api(tags = "直裁损耗率设定")
@RestController
@RequestMapping("/cd90LossSetting")
public class Cd90LossSettingController extends AbstractDocBizController<Cd90LossSetting> {

    @Resource
    private ICd90LossSettingService cd90LossSettingService;

    @Resource
    private Cd90LossSettingMapper cd90LossSettingMapper;

    @ApiOperation("查询直裁损耗率列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd90LossSetting queryVO) {
        return super.list(queryVO);
    }

    @Log(title = "ui.data.column.lossSetting.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增直裁损耗率")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Cd90LossSetting entity) {
        return super.save(entity);
    }

    @Log(title = "ui.data.column.lossSetting.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑直裁损耗率")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd90LossSetting entity) {
        return super.save(entity);
    }

    @Log(title = "ui.data.column.lossSetting.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除直裁损耗率")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取直裁损耗率详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public Cd90LossSetting getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @ApiOperation("校验直裁损耗率唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody Cd90LossSetting entity) {
        return cd90LossSettingService.checkUnique(entity);
    }

    @Log(title = "ui.data.column.lossSetting.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入直裁损耗率")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "ui.data.column.lossSetting.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出直裁损耗率")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody Cd90LossSetting queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<Cd90LossSetting> listExportData(Cd90LossSetting obj) {
        QueryWrapper<Cd90LossSetting> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<Cd90LossSetting> list = cd90LossSettingMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return cd90LossSettingService;
    }

    @Override
    protected void builderCondition(QueryWrapper<Cd90LossSetting> queryWrapper, Cd90LossSetting queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getClothCode()), "CLOTH_CODE", queryVO.getClothCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
    }

    @Override
    protected String getTypeCode() {
        return "CD90_LOSS_SETTING";
    }

    @Override
    protected String getOrderBy() {
        return "CLOTH_CODE asc, MACHINE_CODE asc, UPDATE_TIME desc";
    }
}
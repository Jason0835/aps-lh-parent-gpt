package com.zlt.aps.cd15.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd15.api.domain.entity.Cd15DepthConfig;
import com.zlt.aps.cd15.mapper.Cd15DepthConfigMapper;
import com.zlt.aps.cd15.service.ICd15DepthConfigService;
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
 * 斜裁备库班数与供成型机数配置控制层。
 */
@Api(tags = "斜裁备库班数与供成型机数配置")
@RestController
@RequestMapping("/cd15DepthConfig")
public class Cd15DepthConfigController extends AbstractDocBizController<Cd15DepthConfig> {
    @Resource
    private ICd15DepthConfigService service;
    @Resource
    private Cd15DepthConfigMapper mapper;

    @ApiOperation("查询斜裁备库班数列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd15DepthConfig query) {
        return super.list(query);
    }

    @Log(title = "ui.data.column.cd15DepthConfig.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增斜裁备库班数")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Cd15DepthConfig entity) {
        if (UserConstants.NOT_UNIQUE.equals(service.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15DepthConfig.checkUnique"));
        }
        if (UserConstants.NOT_UNIQUE.equals(service.checkRangeCross(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15DepthConfig.rangeCross"));
        }
        return super.save(entity);
    }

    @Log(title = "ui.data.column.cd15DepthConfig.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑斜裁备库班数")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd15DepthConfig entity) {
        if (UserConstants.NOT_UNIQUE.equals(service.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15DepthConfig.checkUnique"));
        }
        if (UserConstants.NOT_UNIQUE.equals(service.checkRangeCross(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15DepthConfig.rangeCross"));
        }
        return super.save(entity);
    }

    @Log(title = "ui.data.column.cd15DepthConfig.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除斜裁备库班数")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取斜裁备库班数详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public Cd15DepthConfig getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @ApiOperation("校验斜裁备库班数唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody Cd15DepthConfig entity) {
        return service.checkUnique(entity);
    }

    @ApiOperation("校验斜裁备库班数范围交叉")
    @PostMapping("/checkRangeCross")
    public String checkRangeCross(@RequestBody Cd15DepthConfig entity) {
        return service.checkRangeCross(entity);
    }

    @Log(title = "ui.data.column.cd15DepthConfig.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入斜裁备库班数")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext,
                                 @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "ui.data.column.cd15DepthConfig.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出斜裁备库班数")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody Cd15DepthConfig query,
                             @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(query, fileName, response);
    }

    @Override
    protected List<Cd15DepthConfig> listExportData(Cd15DepthConfig output) {
        QueryWrapper<Cd15DepthConfig> queryWrapper = new QueryWrapper<>();
        builderCondition(queryWrapper, output);
        List<Cd15DepthConfig> list = mapper.selectList(queryWrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    public IDocService getDocService() {
        return service;
    }

    @Override
    protected void builderCondition(QueryWrapper<Cd15DepthConfig> qw, Cd15DepthConfig vo) {
        qw.eq(PubUtil.isNotEmpty(vo.getFactoryCode()), "FACTORY_CODE", vo.getFactoryCode());
        qw.eq(vo.getMachineQty() != null, "MACHINE_QTY", vo.getMachineQty());
        qw.eq(PubUtil.isNotEmpty(vo.getMachineRange()), "MACHINE_RANGE", vo.getMachineRange());
    }

    @Override
    protected String getTypeCode() {
        return "CD15_DEPTH_CONFIG";
    }

    @Override
    protected String getOrderBy() {
        return "MACHINE_QTY ASC";
    }
}

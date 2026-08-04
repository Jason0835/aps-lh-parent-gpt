package com.zlt.aps.cd90.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd90.api.domain.entity.Cd90DepthConfig;
import com.zlt.aps.cd90.mapper.Cd90DepthConfigMapper;
import com.zlt.aps.cd90.service.ICd90DepthConfigService;
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

@Api(tags = "直裁备库班数与供成型机数配置")
@RestController
@RequestMapping("/cd90DepthConfig")
public class Cd90DepthConfigController extends AbstractDocBizController<Cd90DepthConfig> {
    @Resource
    private ICd90DepthConfigService service;
    @Resource
    private Cd90DepthConfigMapper mapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd90DepthConfig query) {
        return super.list(query);
    }

    @Log(title = "ui.data.column.cd90DepthConfig.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Cd90DepthConfig entity) {
        // 唯一性校验
        if (UserConstants.NOT_UNIQUE.equals(service.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd90DepthConfig.checkUnique"));
        }
        // 范围交叉校验
        if (UserConstants.NOT_UNIQUE.equals(service.checkRangeCross(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd90DepthConfig.rangeCross"));
        }
        return super.save(entity);
    }

    @Log(title = "ui.data.column.cd90DepthConfig.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd90DepthConfig entity) {
        // 唯一性校验
        if (UserConstants.NOT_UNIQUE.equals(service.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd90DepthConfig.checkUnique"));
        }
        // 范围交叉校验
        if (UserConstants.NOT_UNIQUE.equals(service.checkRangeCross(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd90DepthConfig.rangeCross"));
        }
        return super.save(entity);
    }

    @Log(title = "ui.data.column.cd90DepthConfig.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public Cd90DepthConfig getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody Cd90DepthConfig entity) {
        return service.checkUnique(entity);
    }

    @ApiOperation("校验范围交叉")
    @PostMapping("/checkRangeCross")
    public String checkRangeCross(@RequestBody Cd90DepthConfig entity) {
        return service.checkRangeCross(entity);
    }

    @Log(title = "ui.data.column.cd90DepthConfig.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody com.ruoyi.api.gateway.system.domain.vo.ImportContext context,
                                 @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(context, updateSupport);
    }

    @Log(title = "ui.data.column.cd90DepthConfig.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody Cd90DepthConfig query,
                             @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(query, fileName, response);
    }

    @Override
    protected List<Cd90DepthConfig> listExportData(Cd90DepthConfig output) {
        QueryWrapper<Cd90DepthConfig> queryWrapper = new QueryWrapper<>();
        builderCondition(queryWrapper, output);
        List<Cd90DepthConfig> list = mapper.selectList(queryWrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    public IDocService getDocService() {
        return service;
    }

    @Override
    protected void builderCondition(QueryWrapper<Cd90DepthConfig> qw, Cd90DepthConfig vo) {
        qw.eq(PubUtil.isNotEmpty(vo.getFactoryCode()), "FACTORY_CODE", vo.getFactoryCode());
        qw.eq(vo.getMinMachineQty() != null, "MIN_MACHINE_QTY", vo.getMinMachineQty());
        qw.eq(vo.getMaxMachineQty() != null, "MAX_MACHINE_QTY", vo.getMaxMachineQty());
    }

    @Override
    protected String getTypeCode() {
        return "CD90_DEPTH_CONFIG";
    }

    @Override
    protected String getOrderBy() {
        return "MIN_MACHINE_QTY ASC";
    }
}

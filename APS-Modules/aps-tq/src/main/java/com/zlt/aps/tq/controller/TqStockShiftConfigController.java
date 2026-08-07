package com.zlt.aps.tq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.tq.api.domain.entity.TqStockShiftConfig;
import com.zlt.aps.tq.mapper.TqStockShiftConfigMapper;
import com.zlt.aps.tq.service.ITqStockShiftConfigService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * 胎圈备库班数配置 Controller
 *
 * @author zlt
 * @date 2026-06-25
 */
@Slf4j
@Api(tags = "胎圈备库班数配置")
@RestController
@RequestMapping("/tqStockShiftConfig")
public class TqStockShiftConfigController extends AbstractDocBizController<TqStockShiftConfig> {

    @Autowired
    private ITqStockShiftConfigService tqStockShiftConfigService;

    @Resource
    private TqStockShiftConfigMapper tqStockShiftConfigMapper;

    @ApiOperation("查询胎圈备库班数配置列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TqStockShiftConfig queryVO) {
        startPage();
        List<TqStockShiftConfig> list = tqStockShiftConfigMapper.selectStockShiftConfigList(queryVO);
        return getDataTable(list);
    }

    @Log(title = "胎圈备库班数配置", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TqStockShiftConfig billVO) {
        // 校验业务唯一约束（同一工厂下同区间起始机台数只能有一条）
        if (UserConstants.NOT_UNIQUE.equals(tqStockShiftConfigService.checkUnique(billVO))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.error.message.stockShiftConfig.unique"));
        }
        // 校验区间连续性（从1开始、连续不重叠、无上限仅末段）
        if (UserConstants.NOT_UNIQUE.equals(tqStockShiftConfigService.checkRangeCross(billVO))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.tq.depthConfig.rangeCross"));
        }
        return super.save(billVO);
    }

    @Log(title = "胎圈备库班数配置", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/delete/{ids}")
    public AjaxResult deleteByIds(@PathVariable("ids") List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @Override
    public TqStockShiftConfig getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @Log(title = "胎圈备库班数配置", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "胎圈备库班数配置", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TqStockShiftConfig queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        List<TqStockShiftConfig> list = getExportDataList(queryVO);
        ExcelUtil<TqStockShiftConfig> util = new ExcelUtil<>(TqStockShiftConfig.class);
        Workbook workbook = util.exportExcelFromList(list, fileName);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody TqStockShiftConfig config) {
        return tqStockShiftConfigService.checkUnique(config);
    }

    @ApiOperation("校验范围交叉")
    @PostMapping("/checkRangeCross")
    public String checkRangeCross(@RequestBody TqStockShiftConfig config) {
        return tqStockShiftConfigService.checkRangeCross(config);
    }

    @Log(title = "胎圈备库班数配置", businessType = BusinessType.EXPORT)
    @ApiOperation("导出胎圈备库班数配置列表")
    @PostMapping("/exportList")
    public List<TqStockShiftConfig> exportList(@RequestBody TqStockShiftConfig config) {
        startPage();
        return tqStockShiftConfigService.listStockShiftConfig(config);
    }

    @ApiOperation("删除全部(逻辑删)")
    @PostMapping("/deleteAll")
    public AjaxResult deleteAll() {
        tqStockShiftConfigService.deleteAll();
        return AjaxResult.success();
    }

    @Override
    protected IDocService getDocService() {
        return tqStockShiftConfigService;
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "CREATE_TIME desc";
    }

    @Override
    protected List<TqStockShiftConfig> listExportData(TqStockShiftConfig obj) {
        QueryWrapper<TqStockShiftConfig> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        wrapper.last("ORDER BY " + getOrderBy());
        return tqStockShiftConfigMapper.selectList(wrapper);
    }

    private List<TqStockShiftConfig> getExportDataList(TqStockShiftConfig obj) {
        QueryWrapper<TqStockShiftConfig> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        wrapper.last("ORDER BY " + getOrderBy());
        return tqStockShiftConfigMapper.selectList(wrapper);
    }

    @Override
    protected void builderCondition(QueryWrapper<TqStockShiftConfig> queryWrapper, TqStockShiftConfig queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.eq(queryVO.getMinMachineQty() != null, "MIN_MACHINE_QTY", queryVO.getMinMachineQty());
    }
}

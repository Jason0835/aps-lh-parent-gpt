package com.zlt.aps.cd90.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd90.api.domain.entity.Cd90StorageLaneLimit;
import com.zlt.aps.cd90.mapper.Cd90StorageLaneLimitMapper;
import com.zlt.aps.cd90.service.ICd90StorageLaneLimitService;
import com.zlt.aps.utils.AppUtils;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.ImportExcelUtils;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

@Api(tags = "直裁库排限制")
@RestController
@RequestMapping("/cd90StorageLaneLimit")
public class Cd90StorageLaneLimitController extends AbstractDocBizController<Cd90StorageLaneLimit> {
    @Resource
    private ICd90StorageLaneLimitService service;
    @Resource
    private Cd90StorageLaneLimitMapper mapper;



    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd90StorageLaneLimit query) {
        return super.list(query);
    }

    @Log(title = "ui.data.column.storageLaneLimit.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Cd90StorageLaneLimit entity) {
        return super.save(entity);
    }

    @Log(title = "ui.data.column.storageLaneLimit.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd90StorageLaneLimit entity) {
        return super.save(entity);
    }

    @Log(title = "ui.data.column.storageLaneLimit.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public Cd90StorageLaneLimit getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody Cd90StorageLaneLimit entity) {
        return service.checkUnique(entity);
    }

    @Log(title = "ui.data.column.storageLaneLimit.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext c, @RequestParam("updateSupport") boolean u) throws Exception {
        return super.importData(c, u);
    }

    /**
     * MES按工厂、日期和班次全量覆盖库排状态。
     */
    @ApiOperation("MES全量覆盖直裁库排状态")
    @PostMapping("/logicDeleteAndSaveMesBatch")
    public AjaxResult logicDeleteAndSaveMesBatch(@RequestParam("factoryCode") String factoryCode,
                                                  @RequestParam("laneDate") String laneDate,
                                                  @RequestParam("shiftCode") String shiftCode,
                                                  @RequestParam("updateBy") String updateBy,
                                                  @RequestBody List<Cd90StorageLaneLimit> list) {
        this.service.logicDeleteAndSaveBatch(factoryCode, DateUtil.parseDate(laneDate), shiftCode, updateBy, list);
        return AjaxResult.success();
    }

    @Log(title = "ui.data.column.storageLaneLimit.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody Cd90StorageLaneLimit query, @PathVariable("fileName") String fileName, HttpServletResponse r) throws IOException {
        return super.exportData(query, fileName, r);
    }

    @Override
    protected List<Cd90StorageLaneLimit> listExportData(Cd90StorageLaneLimit output) {
        QueryWrapper<Cd90StorageLaneLimit> w = new QueryWrapper<>();
        builderCondition(w, output);
        List<Cd90StorageLaneLimit> l = mapper.selectList(w);
        AppUtils.formatData(l, getQueryFormulas());
        return l;
    }


    @Override
    public IDocService getDocService() {
        return service;
    }





    @Override
    protected void builderCondition(QueryWrapper<Cd90StorageLaneLimit> qw, Cd90StorageLaneLimit vo) {
        qw.eq(PubUtil.isNotEmpty(vo.getFactoryCode()), "FACTORY_CODE", vo.getFactoryCode());
        qw.eq(vo.getLaneDate() != null, "LANE_DATE", vo.getLaneDate());
        qw.eq(PubUtil.isNotEmpty(vo.getShiftCode()), "SHIFT_CODE", vo.getShiftCode());
        qw.like(PubUtil.isNotEmpty(vo.getStorageLaneCode()), "STORAGE_LANE_CODE", vo.getStorageLaneCode());
    }

    @Override
    protected String getTypeCode() {
        return "CD90_STORAGE_LANE_LIMIT";
    }

    @Override
    protected String getOrderBy() {
        return "LANE_DATE desc, SHIFT_CODE asc, STORAGE_LANE_CODE asc";
    }
}

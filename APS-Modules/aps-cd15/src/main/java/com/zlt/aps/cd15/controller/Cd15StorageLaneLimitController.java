package com.zlt.aps.cd15.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.hutool.core.date.DateUtil;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd15.api.domain.entity.Cd15StorageLaneLimit;
import com.zlt.aps.cd15.mapper.Cd15StorageLaneLimitMapper;
import com.zlt.aps.cd15.service.ICd15StorageLaneLimitService;
import com.zlt.aps.utils.AppUtils;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 斜裁库排限制控制层。
 */
@Api(tags = "斜裁库排限制")
@RestController
@RequestMapping("/cd15StorageLaneLimit")
public class Cd15StorageLaneLimitController extends AbstractDocBizController<Cd15StorageLaneLimit> {

    @Resource
    private ICd15StorageLaneLimitService cd15StorageLaneLimitService;

    @Resource
    private Cd15StorageLaneLimitMapper cd15StorageLaneLimitMapper;

    /** 查询斜裁库排限制列表 */
    @ApiOperation("查询斜裁库排限制列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd15StorageLaneLimit queryVO) {
        return super.list(queryVO);
    }

    /** 新增斜裁库排限制 */
    @Log(title = "ui.data.column.cd15StorageLaneLimit.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增斜裁库排限制")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Cd15StorageLaneLimit entity) {
        String errorKey = cd15StorageLaneLimitService.validateBusiness(entity);
        if (StringUtils.isNotBlank(errorKey)) {
            return AjaxResult.error(I18nUtil.getMessage(errorKey));
        }
        return super.save(entity);
    }

    /** 编辑斜裁库排限制 */
    @Log(title = "ui.data.column.cd15StorageLaneLimit.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑斜裁库排限制")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd15StorageLaneLimit entity) {
        String errorKey = cd15StorageLaneLimitService.validateBusiness(entity);
        if (StringUtils.isNotBlank(errorKey)) {
            return AjaxResult.error(I18nUtil.getMessage(errorKey));
        }
        return super.save(entity);
    }

    /** 删除斜裁库排限制 */
    @Log(title = "ui.data.column.cd15StorageLaneLimit.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除斜裁库排限制")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /** 获取斜裁库排限制详情 */
    @ApiOperation("获取斜裁库排限制详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public Cd15StorageLaneLimit getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /** 校验斜裁库排限制唯一性 */
    @ApiOperation("校验斜裁库排限制唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody Cd15StorageLaneLimit entity) {
        return cd15StorageLaneLimitService.checkUnique(entity);
    }

    /** 导入斜裁库排限制 */
    @Log(title = "ui.data.column.cd15StorageLaneLimit.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入斜裁库排限制")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /** 替换斜裁MES库排快照。 */
    @ApiOperation("替换斜裁MES库排快照")
    @PostMapping("/logicDeleteAndSaveMesBatch")
    public AjaxResult logicDeleteAndSaveMesBatch(@RequestParam("factoryCode") String factoryCode,
                                                  @RequestParam("laneDate") String laneDate,
                                                  @RequestParam("shiftCode") String shiftCode,
                                                  @RequestParam("updateBy") String updateBy,
                                                  @RequestBody List<Cd15StorageLaneLimit> list) {
        this.cd15StorageLaneLimitService.logicDeleteAndSaveBatch(factoryCode,
                DateUtil.parseDate(laneDate), shiftCode, updateBy, list);
        return AjaxResult.success();
    }

    /** 导出斜裁库排限制 */
    @Log(title = "ui.data.column.cd15StorageLaneLimit.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出斜裁库排限制")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody Cd15StorageLaneLimit queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<Cd15StorageLaneLimit> listExportData(Cd15StorageLaneLimit obj) {
        QueryWrapper<Cd15StorageLaneLimit> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        wrapper.last("ORDER BY " + this.getOrderBy());
        List<Cd15StorageLaneLimit> list = cd15StorageLaneLimitMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return cd15StorageLaneLimitService;
    }

    @Override
    protected void builderCondition(QueryWrapper<Cd15StorageLaneLimit> queryWrapper, Cd15StorageLaneLimit queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.eq(queryVO.getLaneDate() != null, "LANE_DATE", queryVO.getLaneDate());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getShiftCode()), "SHIFT_CODE", queryVO.getShiftCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getStorageLaneCode()), "STORAGE_LANE_CODE", queryVO.getStorageLaneCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMaterialCode()), "MATERIAL_CODE", queryVO.getMaterialCode());
    }

    @Override
    protected String getTypeCode() {
        return "CD15_STORAGE_LANE_LIMIT";
    }

    @Override
    protected String getOrderBy() {
        return "LANE_DATE desc, SHIFT_CODE asc, STORAGE_LANE_CODE asc";
    }
}

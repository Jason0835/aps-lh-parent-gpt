package com.zlt.aps.cd15.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd15.api.domain.entity.Cd15CurlLength;
import com.zlt.aps.cd15.mapper.Cd15CurlLengthMapper;
import com.zlt.aps.cd15.service.ICd15CurlLengthService;
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
 * 斜裁卷曲长度控制层。
 */
@Api(tags = "斜裁卷曲长度")
@RestController
@RequestMapping("/curlLength")
public class Cd15CurlLengthController extends AbstractDocBizController<Cd15CurlLength> {

    @Resource
    private ICd15CurlLengthService cd15CurlLengthService;

    @Resource
    private Cd15CurlLengthMapper cd15CurlLengthMapper;

    /** 查询斜裁卷曲长度列表 */
    @ApiOperation("查询斜裁卷曲长度列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd15CurlLength queryVO) {
        return super.list(queryVO);
    }

    /** 新增斜裁卷曲长度 */
    @Log(title = "ui.data.column.cd15CurlLength.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增斜裁卷曲长度")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Cd15CurlLength entity) {
        return super.save(entity);
    }

    /** 编辑斜裁卷曲长度 */
    @Log(title = "ui.data.column.cd15CurlLength.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑斜裁卷曲长度")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd15CurlLength entity) {
        return super.save(entity);
    }

    /** 删除斜裁卷曲长度 */
    @Log(title = "ui.data.column.cd15CurlLength.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除斜裁卷曲长度")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /** 获取斜裁卷曲长度详情 */
    @ApiOperation("获取斜裁卷曲长度详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public Cd15CurlLength getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /** 校验斜裁卷曲长度唯一性 */
    @ApiOperation("校验斜裁卷曲长度唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody Cd15CurlLength entity) {
        return cd15CurlLengthService.checkUnique(entity);
    }

    /** 导入斜裁卷曲长度 */
    @Log(title = "ui.data.column.cd15CurlLength.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入斜裁卷曲长度")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /** 导出斜裁卷曲长度 */
    @Log(title = "ui.data.column.cd15CurlLength.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出斜裁卷曲长度")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody Cd15CurlLength queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<Cd15CurlLength> listExportData(Cd15CurlLength obj) {
        QueryWrapper<Cd15CurlLength> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<Cd15CurlLength> list = cd15CurlLengthMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return cd15CurlLengthService;
    }

    @Override
    protected void builderCondition(QueryWrapper<Cd15CurlLength> queryWrapper, Cd15CurlLength queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getSteelStripCode()), "STEEL_STRIP_CODE", queryVO.getSteelStripCode());
    }

    @Override
    protected String getTypeCode() {
        return "CD15_CURL_LENGTH";
    }

    @Override
    protected String getOrderBy() {
        return "STEEL_STRIP_CODE asc, UPDATE_TIME desc";
    }
}
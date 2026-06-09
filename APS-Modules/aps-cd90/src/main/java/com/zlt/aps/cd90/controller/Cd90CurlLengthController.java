package com.zlt.aps.cd90.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd90.api.domain.entity.Cd90CurlLength;
import com.zlt.aps.cd90.mapper.Cd90CurlLengthMapper;
import com.zlt.aps.cd90.service.ICd90CurlLengthService;
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
 * 直裁卷曲长度控制层。
 */
@Api(tags = "直裁卷曲长度")
@RestController
@RequestMapping("/cd90CurlLength")
public class Cd90CurlLengthController extends AbstractDocBizController<Cd90CurlLength> {

    @Resource
    private ICd90CurlLengthService cd90CurlLengthService;

    @Resource
    private Cd90CurlLengthMapper cd90CurlLengthMapper;

    /** 查询直裁卷曲长度列表 */
    @ApiOperation("查询直裁卷曲长度列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd90CurlLength queryVO) {
        return super.list(queryVO);
    }

    /** 新增直裁卷曲长度 */
    @Log(title = "ui.data.column.cd90CurlLength.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增直裁卷曲长度")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Cd90CurlLength entity) {
        return super.save(entity);
    }

    /** 编辑直裁卷曲长度 */
    @Log(title = "ui.data.column.cd90CurlLength.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑直裁卷曲长度")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd90CurlLength entity) {
        return super.save(entity);
    }

    /** 删除直裁卷曲长度 */
    @Log(title = "ui.data.column.cd90CurlLength.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除直裁卷曲长度")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /** 获取直裁卷曲长度详情 */
    @ApiOperation("获取直裁卷曲长度详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public Cd90CurlLength getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /** 校验直裁卷曲长度唯一性 */
    @ApiOperation("校验直裁卷曲长度唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody Cd90CurlLength entity) {
        return cd90CurlLengthService.checkUnique(entity);
    }

    /** 导入直裁卷曲长度 */
    @Log(title = "ui.data.column.cd90CurlLength.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入直裁卷曲长度")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /** 导出直裁卷曲长度 */
    @Log(title = "ui.data.column.cd90CurlLength.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出直裁卷曲长度")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody Cd90CurlLength queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<Cd90CurlLength> listExportData(Cd90CurlLength obj) {
        QueryWrapper<Cd90CurlLength> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<Cd90CurlLength> list = cd90CurlLengthMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return cd90CurlLengthService;
    }

    @Override
    protected void builderCondition(QueryWrapper<Cd90CurlLength> queryWrapper, Cd90CurlLength queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getClothCode()), "CLOTH_CODE", queryVO.getClothCode());
    }

    @Override
    protected String getTypeCode() {
        return "CD90_CURL_LENGTH";
    }

    @Override
    protected String getOrderBy() {
        return "CLOTH_CODE asc, UPDATE_TIME desc";
    }
}
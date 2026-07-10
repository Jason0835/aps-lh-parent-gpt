package com.zlt.aps.cd15.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd15.api.domain.entity.Cd15AngleWidthMapping;
import com.zlt.aps.cd15.mapper.Cd15AngleWidthMappingMapper;
import com.zlt.aps.cd15.service.ICd15AngleWidthMappingService;
import com.zlt.aps.utils.AppUtils;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * CD15角度宽度对应关系 Controller
 */
@Api(tags = "CD15角度宽度对应关系")
@RestController
@RequestMapping("/cd15AngleWidthMapping")
public class Cd15AngleWidthMappingController extends AbstractDocBizController<Cd15AngleWidthMapping> {

    @Resource
    private ICd15AngleWidthMappingService cd15AngleWidthMappingService;

    @Resource
    private Cd15AngleWidthMappingMapper cd15AngleWidthMappingMapper;

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd15AngleWidthMapping queryVO) {
        return super.list(queryVO);
    }

    /**
     * 新增
     */
    @Log(title = "ui.data.column.cd15AngleWidthMapping.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Cd15AngleWidthMapping entity) {
        return super.save(entity);
    }

    /**
     * 编辑
     */
    @Log(title = "ui.data.column.cd15AngleWidthMapping.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd15AngleWidthMapping entity) {
        return super.save(entity);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.cd15AngleWidthMapping.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /**
     * 获取详情
     */
    @ApiOperation("获取详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public Cd15AngleWidthMapping getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody Cd15AngleWidthMapping entity) {
        return cd15AngleWidthMappingService.checkUnique(entity);
    }

    /**
     * 导入
     */
    @Log(title = "ui.data.column.cd15AngleWidthMapping.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出
     */
    @Log(title = "ui.data.column.cd15AngleWidthMapping.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody Cd15AngleWidthMapping queryVO, @PathVariable("fileName") String fileName, HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<Cd15AngleWidthMapping> listExportData(Cd15AngleWidthMapping obj) {
        QueryWrapper<Cd15AngleWidthMapping> queryWrapper = new QueryWrapper<>();
        builderCondition(queryWrapper, obj);
        List<Cd15AngleWidthMapping> list = cd15AngleWidthMappingMapper.selectList(queryWrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return cd15AngleWidthMappingService;
    }

    @Override
    protected void builderCondition(QueryWrapper<Cd15AngleWidthMapping> queryWrapper, Cd15AngleWidthMapping queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getCutAngle()), "CUT_ANGLE", queryVO.getCutAngle());
    }

    @Override
    protected String getTypeCode() {
        return "CD15_ANGLE_WIDTH_MAPPING";
    }

    @Override
    protected String getOrderBy() {
        return "FACTORY_CODE asc, CUT_ANGLE asc, UPDATE_TIME desc";
    }
}

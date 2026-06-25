package com.zlt.aps.cd15.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd15.api.domain.entity.Cd15Params;
import com.zlt.aps.cd15.mapper.Cd15ParamsMapper;
import com.zlt.aps.cd15.service.ICd15ParamsService;
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
 * 15度裁��参数设置 Controller
 */
@Api(tags = "15度裁断参数设置")
@RestController
@RequestMapping("/cd15Params")
public class Cd15ParamsController extends AbstractDocBizController<Cd15Params> {

    @Resource
    private ICd15ParamsService cd15ParamsService;

    @Resource
    private Cd15ParamsMapper cd15ParamsMapper;

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd15Params queryVO) {
        return super.list(queryVO);
    }

    /**
     * 新增
     */
    @Log(title = "ui.data.column.cd15Params.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Cd15Params entity) {
        return super.save(entity);
    }

    /**
     * 编辑
     */
    @Log(title = "ui.data.column.cd15Params.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd15Params entity) {
        return super.save(entity);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.cd15Params.modelName", businessType = BusinessType.DELETE)
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
    public Cd15Params getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody Cd15Params entity) {
        return cd15ParamsService.checkUnique(entity);
    }

    /**
     * 导入
     */
    @Log(title = "ui.data.column.cd15Params.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出
     */
    @Log(title = "ui.data.column.cd15Params.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody Cd15Params queryVO, @PathVariable("fileName") String fileName, HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<Cd15Params> listExportData(Cd15Params obj) {
        QueryWrapper<Cd15Params> w = new QueryWrapper<>();
        builderCondition(w, obj);
        List<Cd15Params> list = cd15ParamsMapper.selectList(w);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return cd15ParamsService;
    }

    @Override
    protected void builderCondition(QueryWrapper<Cd15Params> qw, Cd15Params vo) {
        // 工厂精确查询
        qw.eq(PubUtil.isNotEmpty(vo.getFactoryCode()), "FACTORY_CODE", vo.getFactoryCode());
        // 参数编码模糊查询
        qw.like(PubUtil.isNotEmpty(vo.getParamCode()), "PARAM_CODE", vo.getParamCode());
        // 参数名称模糊查询
        qw.like(PubUtil.isNotEmpty(vo.getParamName()), "PARAM_NAME", vo.getParamName());
    }

    @Override
    protected String getTypeCode() {
        return "CD15_PARAMS";
    }

    @Override
    protected String getOrderBy() {
        return "FACTORY_CODE asc, PARAM_CODE asc, UPDATE_TIME desc";
    }
}
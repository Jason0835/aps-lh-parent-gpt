package com.zlt.aps.cx.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.entity.config.CxKeyProduct;
import com.zlt.aps.cx.mapper.CxKeyProductMapper;
import com.zlt.aps.cx.service.CxKeyProductService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * 关键产品配置控制器
 *
 * @author APS Team
 */
@Slf4j
@Api(tags = "关键产品配置")
@RestController
@RequestMapping("/cxKeyProduct")
public class CxKeyProductController extends AbstractDocBizController<CxKeyProduct> {

    @Autowired
    private CxKeyProductService cxKeyProductService;

    @Resource
    private CxKeyProductMapper cxKeyProductMapper;

    /**
     * 查询关键产品配置列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody CxKeyProduct queryVO) {
        return super.list(queryVO);
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.cxKeyProduct.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody CxKeyProduct entity) {
        return super.save(entity);
    }

    /**
     * 删除关键产品配置
     */
    @Log(title = "ui.data.column.cxKeyProduct.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /**
     * 获取关键产品配置详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public CxKeyProduct getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    /**
     * 根据集合导入关键产品配置数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.cxKeyProduct.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody com.ruoyi.api.gateway.system.domain.vo.ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出关键产品配置列表
     */
    @Log(title = "ui.data.column.cxKeyProduct.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody CxKeyProduct queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<CxKeyProduct> listExportData(CxKeyProduct obj) {
        QueryWrapper<CxKeyProduct> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return cxKeyProductMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return cxKeyProductService;
    }

    @Override
    protected void builderCondition(QueryWrapper<CxKeyProduct> queryWrapper, CxKeyProduct queryVO) {
        // 胎胚代码模糊查询
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getEmbryoCode()), "EMBRYO_CODE", queryVO.getEmbryoCode());
        // 结构名称模糊查询
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getStructureName()), "STRUCTURE_NAME", queryVO.getStructureName());
        // 是否启用精确查询
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getIsActive()), "IS_ACTIVE", queryVO.getIsActive());
    }

    @Override
    protected String getTypeCode() {
        return "CX_KEY_PRODUCT";
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }
}

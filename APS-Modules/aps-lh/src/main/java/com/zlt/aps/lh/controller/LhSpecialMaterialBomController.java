package com.zlt.aps.lh.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.entity.LhSpecialMaterialBom;
import com.zlt.aps.lh.mapper.LhSpecialMaterialBomEntityMapper;
import com.zlt.aps.lh.service.ILhSpecialMaterialBomService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 特殊物料清单配置控制器
 *
 * @author zlt
 * @date 2026-05-06
 */
@Slf4j
@Api(tags = "特殊物料清单配置")
@RestController
@RequestMapping("/lhSpecialMaterialBom")
public class LhSpecialMaterialBomController extends AbstractDocBizController<LhSpecialMaterialBom> {

    @Autowired
    private ILhSpecialMaterialBomService lhSpecialMaterialBomService;

    @Resource
    private LhSpecialMaterialBomEntityMapper lhSpecialMaterialBomEntityMapper;

    /**
     * 查询特殊物料清单配置列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody LhSpecialMaterialBom queryVO) {
        return super.list(queryVO);
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.lhSpecialMaterialBom.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody LhSpecialMaterialBom entity) {
        return super.save(entity);
    }

    /**
     * 删除特殊物料清单配置
     */
    @Log(title = "ui.data.column.lhSpecialMaterialBom.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        // 空值校验：防止MyBatis Plus deleteBatchIds空指针异常
        if (CollectionUtils.isEmpty(ids)) {
            log.warn("删除特殊物料清单配置时ID列表为空，跳过删除操作");
            return AjaxResult.success();
        }

        // 过滤掉null值
        List<Long> validIds = new ArrayList<>();
        for (Long id : ids) {
            if (id != null) {
                validIds.add(id);
            }
        }

        if (validIds.isEmpty()) {
            log.warn("删除特殊物料清单配置时没有有效的ID，跳过删除操作");
            return AjaxResult.success();
        }

        // 直接调用MyBatis Plus的deleteBatchIds
        int result = lhSpecialMaterialBomEntityMapper.deleteBatchIds(validIds);
        return result > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 获取特殊物料清单配置详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public LhSpecialMaterialBom getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    /**
     * 根据集合导入特殊物料清单配置数据
     *
     * @param importContext  导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.lhSpecialMaterialBom.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData/{updateSupport}")
    @Override
    public AjaxResult importData(@RequestBody com.ruoyi.api.gateway.system.domain.vo.ImportContext importContext,
                                 @PathVariable("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出特殊物料清单配置列表
     */
    @Log(title = "ui.data.column.lhSpecialMaterialBom.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody LhSpecialMaterialBom queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<LhSpecialMaterialBom> listExportData(LhSpecialMaterialBom obj) {
        QueryWrapper<LhSpecialMaterialBom> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return lhSpecialMaterialBomEntityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return lhSpecialMaterialBomService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper 查询条件构造器
     * @param queryVO      查询参数
     */
    @Override
    protected void builderCondition(QueryWrapper<LhSpecialMaterialBom> queryWrapper, LhSpecialMaterialBom queryVO) {
        // 分厂编号精确查询
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        // 结构编码模糊查询
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("structureCode")), "STRUCTURE_CODE", queryVO.getFieldValueByFieldName("structureCode"));
        // 结构名称模糊查询
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("structureName")), "STRUCTURE_NAME", queryVO.getFieldValueByFieldName("structureName"));
        // 物料编码模糊查询
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        // 物料描述模糊查询
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        // 分类精确查询
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("category")), "CATEGORY", queryVO.getFieldValueByFieldName("category"));
    }

    @Override
    protected String getTypeCode() {
        return "LH_SPECIAL_MATERIAL_BOM";
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }
}

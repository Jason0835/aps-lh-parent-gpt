package com.zlt.aps.lh.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.lh.api.domain.entity.LhSharedMouldPat;
import com.zlt.aps.lh.mapper.LhSharedMouldPatEntityMapper;
import com.zlt.aps.lh.service.ILhSharedMouldPatService;
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
 * 共用模具花纹配置控制器
 *
 * @author zlt
 * @date 2026-05-14
 */
@Slf4j
@Api(tags = "共用模具花纹配置")
@RestController
@RequestMapping("/lhSharedMouldPat")
public class LhSharedMouldPatController extends AbstractDocBizController<LhSharedMouldPat> {

    @Autowired
    private ILhSharedMouldPatService lhSharedMouldPatService;

    @Resource
    private LhSharedMouldPatEntityMapper lhSharedMouldPatEntityMapper;

    /**
     * 查询共用模具花纹配置列表
     */
    @ApiOperation("查询列表")
    @RequiresPermissions("lh:lhSharedMouldPat:list")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody LhSharedMouldPat queryVO) {
        return super.list(queryVO);
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.lhSharedMouldPat.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @RequiresPermissions({"lh:lhSharedMouldPat:edit", "lh:lhSharedMouldPat:add"})
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody LhSharedMouldPat entity) {
        return super.save(entity);
    }

    /**
     * 删除共用模具花纹配置
     */
    @Log(title = "ui.data.column.lhSharedMouldPat.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @RequiresPermissions("lh:lhSharedMouldPat:remove")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        // 空值校验：防止MyBatis Plus deleteBatchIds空指针异常
        if (CollectionUtils.isEmpty(ids)) {
            log.warn("删除共用模具花纹配置时ID列表为空，跳过删除操作");
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
            log.warn("删除共用模具花纹配置时没有有效的ID，跳过删除操作");
            return AjaxResult.success();
        }

        // 直接调用MyBatis Plus的deleteBatchIds
        int result = lhSharedMouldPatEntityMapper.deleteBatchIds(validIds);
        return result > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 获取共用模具花纹配置详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public LhSharedMouldPat getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    /**
     * 根据集合导入共用模具花纹配置数据
     *
     * @param importContext  导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.lhSharedMouldPat.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @RequiresPermissions("lh:lhSharedMouldPat:import")
    @PostMapping("/importData/{updateSupport}")
    @Override
    public AjaxResult importData(@RequestBody com.ruoyi.api.gateway.system.domain.vo.ImportContext importContext,
                                 @PathVariable("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出共用模具花纹配置列表
     */
    @Log(title = "ui.data.column.lhSharedMouldPat.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @RequiresPermissions("lh:lhSharedMouldPat:export")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody LhSharedMouldPat queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<LhSharedMouldPat> listExportData(LhSharedMouldPat obj) {
        QueryWrapper<LhSharedMouldPat> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return lhSharedMouldPatEntityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return lhSharedMouldPatService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper 查询条件构造器
     * @param queryVO      查询参数
     */
    @Override
    protected void builderCondition(QueryWrapper<LhSharedMouldPat> queryWrapper, LhSharedMouldPat queryVO) {
        // 工厂编号精确查询
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        // 物料编码模糊查询
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        // 物料描述模糊查询
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        // 主花纹模糊查询
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mainPattern")), "MAIN_PATTERN", queryVO.getFieldValueByFieldName("mainPattern"));
        // 模具类型精确查询
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldType")), "MOULD_TYPE", queryVO.getFieldValueByFieldName("mouldType"));
        // 模具号模糊查询
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldNo")), "MOULD_NO", queryVO.getFieldValueByFieldName("mouldNo"));
    }

    /**
     * 唯一性校验
     */
    @ApiOperation("唯一性校验")
    @PostMapping("/checkUniqueLhSharedMouldPat")
    public AjaxResult checkUniqueLhSharedMouldPat(@RequestBody LhSharedMouldPat entity) {
        String result = lhSharedMouldPatService.checkUnique(entity);
        return AjaxResult.success().put("exist", UserConstants.NOT_UNIQUE.equals(result));
    }

    @Override
    protected String getTypeCode() {
        return "LH_SHARED_MOULD_PAT";
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }
}

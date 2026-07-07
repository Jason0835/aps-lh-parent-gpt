package com.zlt.aps.cx.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.entity.config.CxEmbryoLhTime;
import com.zlt.aps.cx.mapper.CxEmbryoLhTimeMapper;
import com.zlt.aps.cx.service.CxEmbryoLhTimeService;
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
 * 胎胚最早可供硫化时间控制器
 *
 * @author APS Team
 */
@Slf4j
@Api(tags = "胎胚最早可供硫化时间")
@RestController
@RequestMapping("/cxEmbryoLhTime")
public class CxEmbryoLhTimeController extends AbstractDocBizController<CxEmbryoLhTime> {

    @Autowired
    private CxEmbryoLhTimeService cxEmbryoLhTimeService;

    @Resource
    private CxEmbryoLhTimeMapper cxEmbryoLhTimeMapper;

    /**
     * 查询胎胚最早可供硫化时间列表
     *
     * @param queryVO 查询条件
     * @return 列表数据
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody CxEmbryoLhTime queryVO) {
        return super.list(queryVO);
    }

    /**
     * 保存胎胚最早可供硫化时间
     *
     * @param entity 实体对象
     * @return 操作结果
     */
    @Log(title = "ui.data.column.cxEmbryoLhTime.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody CxEmbryoLhTime entity) {
        return super.save(entity);
    }

    /**
     * 删除胎胚最早可供硫化时间
     * 重写父类方法，修复MyBatis Plus deleteBatchIds空指针异常
     *
     * @param ids 主键ID列表
     * @return 操作结果
     */
    @Log(title = "ui.data.column.cxEmbryoLhTime.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            log.warn("删除胎胚最早可供硫化时间时ID列表为空，跳过删除操作");
            return AjaxResult.success();
        }

        List<Long> validIds = new ArrayList<>();
        for (Long id : ids) {
            if (id != null) {
                validIds.add(id);
            }
        }

        if (validIds.isEmpty()) {
            log.warn("删除胎胚最早可供硫化时间时没有有效的ID，跳过删除操作");
            return AjaxResult.success();
        }

        int result = cxEmbryoLhTimeMapper.deleteBatchIds(validIds);
        return result > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 获取胎胚最早可供硫化时间详细信息
     *
     * @param billId 主键ID
     * @return 实体对象
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public CxEmbryoLhTime getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    /**
     * 导入胎胚最早可供硫化时间数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 操作结果
     */
    @Log(title = "ui.data.column.cxEmbryoLhTime.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody com.ruoyi.api.gateway.system.domain.vo.ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出胎胚最早可供硫化时间列表
     *
     * @param queryVO  查询条件
     * @param fileName 文件名
     * @param response HTTP响应
     * @return Excel文件字节数组
     */
    @Log(title = "ui.data.column.cxEmbryoLhTime.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody CxEmbryoLhTime queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<CxEmbryoLhTime> listExportData(CxEmbryoLhTime obj) {
        QueryWrapper<CxEmbryoLhTime> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return cxEmbryoLhTimeMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return cxEmbryoLhTimeService;
    }

    @Override
    protected void builderCondition(QueryWrapper<CxEmbryoLhTime> queryWrapper, CxEmbryoLhTime queryVO) {
        // 结构名称模糊查询
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getStructureName()), "STRUCTURE_NAME", queryVO.getStructureName());
    }

    @Override
    protected String getTypeCode() {
        return "CX_EMBRYO_LH_TIME";
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }
}

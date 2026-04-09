package com.zlt.aps.cx.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.mapper.CxParamConfigMapper;
import com.zlt.aps.cx.service.CxParamConfigService;
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
 * 排程参数配置控制器
 *
 * @author APS Team
 */
@Slf4j
@Api(tags = "排程参数配置")
@RestController
@RequestMapping("/cxParamConfig")
public class CxParamConfigController extends AbstractDocBizController<CxParamConfig> {

    @Autowired
    private CxParamConfigService cxParamConfigService;

    @Resource
    private CxParamConfigMapper cxParamConfigMapper;

    /**
     * 查询排程参数配置列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody CxParamConfig queryVO) {
        return super.list(queryVO);
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.cxParamConfig.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody CxParamConfig entity) {
        return super.save(entity);
    }

    /**
     * 删除排程参数配置
     */
    @Log(title = "ui.data.column.cxParamConfig.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /**
     * 获取排程参数配置详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public CxParamConfig getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    /**
     * 根据集合导入排程参数配置数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.cxParamConfig.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody com.ruoyi.api.gateway.system.domain.vo.ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出排程参数配置列表
     */
    @Log(title = "ui.data.column.cxParamConfig.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody CxParamConfig queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<CxParamConfig> listExportData(CxParamConfig obj) {
        QueryWrapper<CxParamConfig> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return cxParamConfigMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return cxParamConfigService;
    }

    @Override
    protected void builderCondition(QueryWrapper<CxParamConfig> queryWrapper, CxParamConfig queryVO) {
        // 参数编码模糊查询
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getParamCode()), "PARAM_CODE", queryVO.getParamCode());
        // 参数名称模糊查询
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getParamName()), "PARAM_NAME", queryVO.getParamName());
        // 是否启用精确查询
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getIsActive()), "IS_ACTIVE", queryVO.getIsActive());
    }

    @Override
    protected String getTypeCode() {
        return "CX_PARAM_CONFIG";
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }
}

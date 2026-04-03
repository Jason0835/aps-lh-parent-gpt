package com.zlt.aps.lh.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.maindata.mapper.MdmMouldCleanPlanEntityMapper;
import com.zlt.aps.maindata.service.IMdmMouldCleanPlanService;
import com.zlt.aps.mdm.api.domain.entity.MdmMouldCleanPlan;
import com.zlt.aps.mdm.api.domain.vo.MdmDeviceMaintenancePlanVo;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Slf4j
@Api(tags = "模具清洗计划")
@RestController
@RequestMapping("/mouldCleanPlan")
public class MdmMouldCleanPlanController extends AbstractDocBizController<MdmMouldCleanPlan> {

    @Autowired
    private IMdmMouldCleanPlanService mdmMouldCleanPlanService;

    @Resource
    private MdmMouldCleanPlanEntityMapper mdmMouldCleanPlanEntityMapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmMouldCleanPlan queryVO) {
        return super.list(queryVO);
    }

    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmMouldCleanPlan getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mouldCleanPlan.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmMouldCleanPlan billVO){
        if (StringUtil.isBlank(billVO.getFactoryCode())) {
            billVO.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        int result = mdmMouldCleanPlanService.save(billVO);
        return result > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mouldCleanPlan.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 根据集合导入模具清洗计划数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     * @throws Exception 异常
     */
    @Log(title = "ui.data.column.mouldCleanPlan.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }


    public AjaxResult doImportData(List list, boolean updateSupport, long importLogId) {
        return mdmMouldCleanPlanService.importData(list, updateSupport, importLogId);
    }
    /**
     * 导出列表
     */
    @Log(title = "模具清洗计划", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmMouldCleanPlan queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmMouldCleanPlan> listExportData(MdmMouldCleanPlan obj) {
        QueryWrapper<MdmMouldCleanPlan> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return mdmMouldCleanPlanEntityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return mdmMouldCleanPlanService;
    }

    @Override
    protected void builderCondition(QueryWrapper<MdmMouldCleanPlan> queryWrapper, MdmMouldCleanPlan queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lhCode")), "LH_CODE", queryVO.getFieldValueByFieldName("lhCode"));
        
        String operTimeBegin = queryVO.getOperTimeBegin();
        String operTimeEnd = queryVO.getOperTimeEnd();
        if (PubUtil.isNotEmpty(operTimeBegin)) {
            queryWrapper.ge("OPER_TIME", DateUtil.beginOfDay(DateUtil.parse(operTimeBegin, "yyyy-MM-dd")));
        }
        if (PubUtil.isNotEmpty(operTimeEnd)) {
            queryWrapper.le("OPER_TIME", DateUtil.endOfDay(DateUtil.parse(operTimeEnd, "yyyy-MM-dd")));
        }
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "OPER_TIME desc, id desc";
    }
}

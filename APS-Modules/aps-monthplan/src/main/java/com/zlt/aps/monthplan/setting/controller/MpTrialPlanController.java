package com.zlt.aps.monthplan.setting.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.SysUser;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.ISysUserService;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.maindata.mapper.MdmSkuConstructionRefEntityMapper;
import com.zlt.aps.maindata.mapper.MpTrialPlanEntityMapper;
import com.zlt.aps.maindata.service.IMpTrialPlanService;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.aps.monthplan.api.domain.entity.MpTrialPlan;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpTrialPlanController.java
 * 描    述：试制量试计划 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-11
 */
@Slf4j
@Api(tags = "试制量试计划")
@RestController
@RequestMapping("/mpTrialPlan")
public class MpTrialPlanController extends AbstractDocBizController<MpTrialPlan> {

    @Autowired
    private IMpTrialPlanService mpTrialPlanService;

    @Autowired
    private MpTrialPlanEntityMapper entityMapper;
    @Autowired
    private MdmSkuConstructionRefEntityMapper skuConstructionRefEntityMapper;

    @Autowired
    private ISysUserService iSysUserService;

    /**
     * 查询试制量试计划列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MpTrialPlan queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mpTrialPlan.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MpTrialPlan billVO) {
        SysUser sysUser = iSysUserService.selectUserByName(SecurityUtils.getUsername());
        billVO.setDeptId(sysUser.getDeptId());
        // 查询SKU与施工关系，用于写入 制造示方、文字示方、硫化示方
        String materialCode = billVO.getMaterialCode();
        if (StringUtils.isNotBlank(materialCode)) {
            LambdaQueryWrapper<MdmSkuConstructionRef> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MdmSkuConstructionRef::getMaterialCode, materialCode);
            List<MdmSkuConstructionRef> mdmSkuConstructionRefList = skuConstructionRefEntityMapper.selectList(queryWrapper);
            Map<String, MdmSkuConstructionRef> skuConstructionRefMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(mdmSkuConstructionRefList)) {
                skuConstructionRefMap = mdmSkuConstructionRefList.stream().collect(Collectors.toMap(MdmSkuConstructionRef::getMaterialCode, Function.identity(), (old, now) -> old));
            }
            if (skuConstructionRefMap.containsKey(materialCode)) {
                MdmSkuConstructionRef mdmSkuConstructionRef = skuConstructionRefMap.get(materialCode);
                billVO.setEmbryoNo(mdmSkuConstructionRef.getEmbryoNo());
                billVO.setMadeInfo(mdmSkuConstructionRef.getEmbryoNo());
                billVO.setTextNo(mdmSkuConstructionRef.getTextNo());
                billVO.setLhNo(mdmSkuConstructionRef.getLhNo());
            }
        }
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mpTrialPlan.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }


    /**
     * 获取试制量试计划详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MpTrialPlan getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入试制量试计划数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mpTrialPlan.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "试制量试计划", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MpTrialPlan queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MpTrialPlan> listExportData(MpTrialPlan obj) {
        QueryWrapper<MpTrialPlan> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return mpTrialPlanService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MpTrialPlan> queryWrapper, MpTrialPlan queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("trialType")), "TRIAL_TYPE", queryVO.getFieldValueByFieldName("trialType"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specifications")), "SPECIFICATIONS", queryVO.getFieldValueByFieldName("specifications"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("pattern")), "PATTERN", queryVO.getFieldValueByFieldName("pattern"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("trialStatus")), "TRIAL_STATUS", queryVO.getFieldValueByFieldName("trialStatus"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("urgencyType")), "URGENCY_TYPE", queryVO.getFieldValueByFieldName("urgencyType"));

        queryWrapper.ge(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("planDateStartTime")), "PLAN_DATE", queryVO.getFieldValueByFieldName("planDateStartTime"));
        queryWrapper.le(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("planDateEndTime")), "PLAN_DATE", queryVO.getFieldValueByFieldName("planDateEndTime"));
        queryWrapper.ge(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("completeDateStartTime")), "COMPLETE_DATE", queryVO.getFieldValueByFieldName("completeDateStartTime"));
        queryWrapper.le(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("completeDateEndTime")), "COMPLETE_DATE", queryVO.getFieldValueByFieldName("completeDateEndTime"));
    }

    @Override
    protected String getTypeCode() {
        return "MP0210";
    }

    @Override
    protected String[] getQueryFormulas() {
        return new String[]{
                "updateByName->getcolvalue(SYS_USER, nick_name, user_name, updateBy)",
                "deptIdName->getcolvalue(SYS_DEPT, dept_name, dept_id, deptId)",
        };
    }
}

package com.zlt.aps.monthplan.setting.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.maindata.mapper.MdmMoldingMachineClsEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMoldingMachineEntityMapper;
import com.zlt.aps.maindata.service.IMdmMoldingMachineService;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachineCls;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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
* 文件名称：MdmMoldingMachineController.java
* 描    述：基础数据-成型机档案 控制层类：....
*@author zlt
*@date 2025-02-25
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "基础数据-成型机档案")
@RestController
@RequestMapping("/mdmMoldingMachine")
public class MdmMoldingMachineController extends AbstractDocBizController<MdmMoldingMachine> {

    @Autowired
    private IMdmMoldingMachineService mdmMoldingMachineService;

    @Autowired
    private MdmMoldingMachineEntityMapper entityMapper;
    @Autowired
    private MdmMoldingMachineClsEntityMapper clsEntityMapper;

    @Autowired
    private RedisService redisService;

    /**
     * 查询基础数据-成型机档案列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmMoldingMachine queryVO) {
        this.startPage(this.getOrderBy());
        QueryWrapper<MdmMoldingMachine> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, queryVO);
        List<MdmMoldingMachine> list = entityMapper.selectList(wrapper);

        Map<Long, MdmMoldingMachineCls> machineClsMap = new HashMap<>(16);
        QueryWrapper<MdmMoldingMachineCls> moldingMachineClsQueryWrapper = new QueryWrapper<>();
        List<MdmMoldingMachineCls> mdmMoldingMachineClsList = clsEntityMapper.selectList(moldingMachineClsQueryWrapper);
        if (CollectionUtils.isNotEmpty(mdmMoldingMachineClsList)) {
            machineClsMap = mdmMoldingMachineClsList.stream().collect(Collectors.toMap(BaseEntity::getId, Function.identity(), (s1, s2) -> s1));
        }
        if (list != null && !list.isEmpty()) {
            for (MdmMoldingMachine mdmMoldingMachine : list) {
                Long moldingMachineClassId = mdmMoldingMachine.getMoldingMachineClassId();
                if (machineClsMap.containsKey(moldingMachineClassId)) {
                    mdmMoldingMachine.setMouldMethod(machineClsMap.get(moldingMachineClassId).getMouldMethod());
                }
            }
        }
        return getDataTable(list);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mdmMoldingMachine.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmMoldingMachine billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mdmMoldingMachine.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions( "maindata:mdmMoldingMachine:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取基础数据-成型机档案详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmMoldingMachine getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入基础数据-成型机档案数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mdmMoldingMachine.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "基础数据-成型机档案", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmMoldingMachine queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmMoldingMachine> listExportData(MdmMoldingMachine obj) {
        QueryWrapper<MdmMoldingMachine> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return mdmMoldingMachineService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MdmMoldingMachine> queryWrapper, MdmMoldingMachine queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("moldingMachineCode")), "MOLDING_MACHINE_CODE", queryVO.getFieldValueByFieldName("moldingMachineCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("moldingMachineClassId")), "MOLDING_MACHINE_CLASS_ID", queryVO.getFieldValueByFieldName("moldingMachineClassId"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isRfSpecialUse")), "IS_RF_SPECIAL_USE", queryVO.getFieldValueByFieldName("isRfSpecialUse"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("carcassClothType")), "CARCASS_CLOTH_TYPE", queryVO.getFieldValueByFieldName("carcassClothType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("machineStatus")), "MACHINE_STATUS", queryVO.getFieldValueByFieldName("machineStatus"));
    }



    @Override
    protected String getTypeCode(){
        return "0116";
    }

    @Override
    protected String[] getQueryFormulas() {
        return new String[]{
                "mouldMethod->getcolvalue(T_MDM_MOLDING_MACHINE_CLS, MOULD_METHOD, ID, moldingMachineClassId)",
        };
    }
}

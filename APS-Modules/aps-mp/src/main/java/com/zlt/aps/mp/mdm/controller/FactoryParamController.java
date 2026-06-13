package com.zlt.aps.mp.mdm.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.mp.api.domain.entity.FactoryParam;
import com.zlt.aps.mp.api.domain.vo.FactoryParamVo;
import com.zlt.aps.mp.common.utils.ParamDataTypeUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryParamController.java
 * 描    述：系统参数（排产设定） 控制层类：....
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-20
 */
@Slf4j
@Api(tags = "系统参数（排产设定）")
@RestController
@RequestMapping("/factoryParam")
public class FactoryParamController extends BaseController<FactoryParam> {

    private final IFactoryParamService factoryParamService;

    public FactoryParamController(IFactoryParamService factoryParamService) {
        this.factoryParamService = factoryParamService;
    }

    /**
     * 查询分厂排产设定列表
     */
    @ApiOperation("查询分厂排产设定列表`")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody FactoryParam entity) {
        startPage("FACTORY_CODE,BUSINESS_GROUP,PARAM_CODE asc");
        List<FactoryParam> list = factoryParamService.getFacParamByList(entity);
        return getDataTable(list);
    }


    /**
     * 修改分厂排产设定
     */
    @Log(title = "ui.data.column.docFactoryParam.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改分厂排产设定")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody FactoryParam entity) {
        ParamDataTypeUtils.checkValidParams(entity.getParamCode(), entity.getParamName(), entity.getDataType(), entity.getParamValue());
        return toAjax(factoryParamService.updateById(entity));
    }

    /**
     * 复制分厂排产设定
     */
    @Log(title = "ui.data.column.docFactoryParam.modelName", businessType = BusinessType.OTHER)
    @ApiOperation("复制分厂排产设定")
    @PostMapping("/copy")
    @ResponseBody
    public AjaxResult copy(@RequestBody FactoryParamVo factoryParamVo) {
        return factoryParamService.copy(factoryParamVo);
    }

    /**
     * 根据工厂、产品品类和参数编码查询系统参数
     *
     * @param entity 查询条件，需包含工厂编码、产品品类和参数编码
     * @return 查询到的系统参数，不存在时返回空
     * @throws RuntimeException 查询系统参数异常时抛出
     */
    @ApiOperation("根据参数编码查询系统参数")
    @PostMapping("/getByParamCode")
    public FactoryParam getByParamCode(@RequestBody FactoryParam entity) {
        return factoryParamService.getFacParamSingle(entity);
    }

}

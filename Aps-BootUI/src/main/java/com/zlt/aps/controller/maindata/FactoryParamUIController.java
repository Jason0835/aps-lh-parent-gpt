package com.zlt.aps.controller.maindata;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.itf.vo.MesBrandDict;
import com.zlt.aps.mp.api.domain.entity.FactoryParam;
import com.zlt.aps.mp.api.domain.vo.FactoryParamVo;
import com.zlt.aps.mp.api.service.IFactoryParamRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryParamUIController.java
 * 描    述：系统参数（排产设定） UI控制层类：....
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
@Controller
@RequestMapping("/monthplan/factoryParam")
public class FactoryParamUIController extends BaseUIController<FactoryParam> {

    private final IFactoryParamRemoteService iFactoryParamService;

    public FactoryParamUIController(IFactoryParamRemoteService iFactoryParamService) {
        this.iFactoryParamService = iFactoryParamService;
    }


    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:factoryParam:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(FactoryParam factoryParam) {
        factoryParam.setBusinessGroup("02");
        return iFactoryParamService.list(factoryParam);
    }

    /**
     * 修改或新增分厂排产设定
     */
    @ApiOperation("修改或新增分厂排产设定")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(FactoryParam factoryParam) {
        AjaxResult ajaxResult = null;
        if (factoryParam.getId() != null) {
            ajaxResult = iFactoryParamService.edit(factoryParam);
        } else {
            return AjaxResult.error();
        }
        return ajaxResult;
    }

    /**
     * 复制分厂排产设定
     */
    @ApiOperation("复制分厂排产设定")
    @RequiresPermissions("fac:docFactoryParam:copy")
    @PostMapping("/copy")
    @ResponseBody
    public AjaxResult copy(FactoryParamVo vo) {
        return iFactoryParamService.copy(vo);
    }

    @Autowired
    private IMesItfService iMesItfService;

    /**
     * 查询MES品牌字典
     *
     * @return 结果
     */
    @ApiOperation("查询MES品牌字典")
    @PostMapping("/selectMesBrandDict")
    public List<MesBrandDict> selectMesBrandDict() {
        return iMesItfService.selectMesBrandDict();
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("dp:factoryParam:list")
    @PostMapping("/dpList")
    @ResponseBody
    public TableDataInfo dpList(FactoryParam factoryParam) {
        factoryParam.setBusinessGroup("01");
        return iFactoryParamService.list(factoryParam);
    }

    /**
     * 修改或新增分厂排产设定
     */
    @ApiOperation("修改或新增分厂排产设定")
    @RequiresPermissions("dp:factoryParam:edit")
    @PostMapping("/dpEdit")
    @ResponseBody
    public AjaxResult dpEdit(FactoryParam factoryParam) {
        AjaxResult ajaxResult = null;
        if (factoryParam.getId() != null) {
            ajaxResult = iFactoryParamService.edit(factoryParam);
        } else {
            return AjaxResult.error();
        }
        return ajaxResult;
    }
}

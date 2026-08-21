package com.zlt.aps.gsq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDiscMachine;
import com.zlt.aps.gsq.mapper.GsqTwiningDiscMachineMapper;
import com.zlt.aps.gsq.service.IGsqTwiningDiscMachineService;
import com.zlt.aps.utils.AppUtils;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 钢丝圈缠绕盘-机台关系Controller
 * 路径：/gsq/discMachine
 * <p>独立菜单页面维护缠绕盘可安装使用的机台清单（MES同步+手工维护）</p>
 *
 * @author zlt
 * @date 2026-08-20
 */
@Slf4j
@Api(tags = "钢丝圈缠绕盘-机台关系")
@RestController
@RequestMapping("/gsq/discMachine")
public class GsqTwiningDiscMachineController extends AbstractDocBizController<GsqTwiningDiscMachine> {

    @Autowired
    private IGsqTwiningDiscMachineService gsqTwiningDiscMachineService;

    @Resource
    private GsqTwiningDiscMachineMapper gsqTwiningDiscMachineMapper;

    /**
     * 查询缠绕盘-机台关系列表（含缠绕盘名称/英寸/排列方式/机台名称反显）
     */
    @ApiOperation("查询缠绕盘-机台关系列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody GsqTwiningDiscMachine queryVO) {
        startPage();
        List<GsqTwiningDiscMachine> list = gsqTwiningDiscMachineMapper.listDiscMachine(queryVO);
        return getDataTable(list);
    }

    /**
     * 获取缠绕盘-机台关系详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @Override
    public GsqTwiningDiscMachine getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /**
     * 保存缠绕盘-机台关系（带业务校验：缠绕盘/机台存在性、组合唯一性）
     */
    @Log(title = "缠绕盘机台关系", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody GsqTwiningDiscMachine billVO) {
        return gsqTwiningDiscMachineService.saveWithCheck(billVO);
    }

    /**
     * 删除缠绕盘-机台关系（逻辑删除）
     */
    @Log(title = "缠绕盘机台关系", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/delete/{ids}")
    public AjaxResult deleteByIds(@PathVariable("ids") List<Long> ids) {
        gsqTwiningDiscMachineService.removeByIds(ids);
        return AjaxResult.success();
    }

    /**
     * 校验缠绕盘+机台组合唯一性
     */
    @ApiOperation("校验缠绕盘+机台组合唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody GsqTwiningDiscMachine entity) {
        return gsqTwiningDiscMachineService.checkUnique(entity);
    }

    @Override
    protected IDocService getDocService() {
        return gsqTwiningDiscMachineService;
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "CREATE_TIME desc";
    }

    /**
     * 主表反显公式
     */
    @Override
    protected String[] getQueryFormulas() {
        return gsqTwiningDiscMachineService.getQueryFormulas();
    }

    /**
     * 构建查询条件（手动追加 IS_DELETE=0 过滤逻辑删除数据）
     */
    @Override
    protected void builderCondition(QueryWrapper<GsqTwiningDiscMachine> queryWrapper, GsqTwiningDiscMachine queryVO) {
        queryWrapper.eq("IS_DELETE", "0");
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getTwiningDiscCode()), "TWINING_DISC_CODE", queryVO.getTwiningDiscCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getStatus()), "STATUS", queryVO.getStatus());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getDataSource()), "DATA_SOURCE", queryVO.getDataSource());
    }

    /**
     * 获取导出数据列表，并补反显字段
     * <p>导出需含缠绕盘名称/英寸/排列方式/机台名称反显字段，复用listDiscMachine的join查询</p>
     */
    @Override
    protected List<GsqTwiningDiscMachine> listExportData(GsqTwiningDiscMachine obj) {
        obj.setOrderStr(getOrderBy());
        List<GsqTwiningDiscMachine> list = gsqTwiningDiscMachineMapper.listDiscMachine(obj);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }
}

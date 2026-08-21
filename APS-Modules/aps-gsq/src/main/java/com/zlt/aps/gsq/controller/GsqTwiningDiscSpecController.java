package com.zlt.aps.gsq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDiscSpec;
import com.zlt.aps.gsq.mapper.GsqTwiningDiscSpecMapper;
import com.zlt.aps.gsq.service.IGsqTwiningDiscService;
import com.zlt.aps.gsq.service.IGsqTwiningDiscSpecService;
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
 * 钢丝圈缠绕盘-规格关系Controller
 * 路径：/gsq/discSpec
 * <p>独立菜单页面维护缠绕盘与钢丝圈规格的对应关系（多对多，MES同步+手工维护），
 * 数据表T_GSQ_TWINING_DISC_SPEC（与机台关系表对称，统一TWINING_DISC_CODE编码关联）</p>
 *
 * @author zlt
 * @date 2026-08-21
 */
@Slf4j
@Api(tags = "钢丝圈缠绕盘-规格关系")
@RestController
@RequestMapping("/gsq/discSpec")
public class GsqTwiningDiscSpecController extends AbstractDocBizController<GsqTwiningDiscSpec> {

    @Autowired
    private IGsqTwiningDiscSpecService gsqTwiningDiscSpecService;

    /** 缠绕盘主表服务（提供钢丝圈下拉选项查询） */
    @Autowired
    private IGsqTwiningDiscService gsqTwiningDiscService;

    @Resource
    private GsqTwiningDiscSpecMapper gsqTwiningDiscSpecMapper;

    /**
     * 查询缠绕盘-规格关系列表（含缠绕盘名称/英寸/排列方式反显）
     */
    @ApiOperation("查询缠绕盘-规格关系列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody GsqTwiningDiscSpec queryVO) {
        startPage();
        List<GsqTwiningDiscSpec> list = gsqTwiningDiscSpecMapper.listDiscSpec(queryVO);
        return getDataTable(list);
    }

    /**
     * 获取缠绕盘-规格关系详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @Override
    public GsqTwiningDiscSpec getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /**
     * 保存缠绕盘-规格关系（带业务校验：缠绕盘/钢丝圈存在性、组合唯一性、名称反显）
     */
    @Log(title = "缠绕盘规格关系", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody GsqTwiningDiscSpec billVO) {
        return gsqTwiningDiscSpecService.saveWithCheck(billVO);
    }

    /**
     * 删除缠绕盘-规格关系（逻辑删除）
     */
    @Log(title = "缠绕盘规格关系", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/delete/{ids}")
    public AjaxResult deleteByIds(@PathVariable("ids") List<Long> ids) {
        gsqTwiningDiscSpecService.removeByIds(ids);
        return AjaxResult.success();
    }

    /**
     * 校验缠绕盘+钢丝圈规格组合唯一性
     */
    @ApiOperation("校验缠绕盘+钢丝圈规格组合唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody GsqTwiningDiscSpec entity) {
        return gsqTwiningDiscSpecService.checkUnique(entity);
    }

    /**
     * 查询施工信息表全部钢丝圈选项（编码+名称，去重），供页面下拉选择使用
     */
    @ApiOperation("查询钢丝圈下拉选项")
    @GetMapping("/listSteelRingOptions")
    public AjaxResult listSteelRingOptions() {
        return AjaxResult.success(gsqTwiningDiscService.listSteelRingOptions());
    }

    @Override
    protected IDocService getDocService() {
        return gsqTwiningDiscSpecService;
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
        return gsqTwiningDiscSpecService.getQueryFormulas();
    }

    /**
     * 构建查询条件（手动追加 IS_DELETE=0 过滤逻辑删除数据）
     */
    @Override
    protected void builderCondition(QueryWrapper<GsqTwiningDiscSpec> queryWrapper, GsqTwiningDiscSpec queryVO) {
        queryWrapper.eq("IS_DELETE", "0");
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getTwiningDiscCode()), "TWINING_DISC_CODE", queryVO.getTwiningDiscCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getSteelRingCode()), "STEEL_RING_CODE", queryVO.getSteelRingCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getStatus()), "STATUS", queryVO.getStatus());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
    }

    /**
     * 获取导出数据列表，并补反显字段
     * <p>导出需含缠绕盘名称/英寸/排列方式反显字段，复用listDiscSpec的join查询</p>
     */
    @Override
    protected List<GsqTwiningDiscSpec> listExportData(GsqTwiningDiscSpec obj) {
        obj.setOrderStr(getOrderBy());
        List<GsqTwiningDiscSpec> list = gsqTwiningDiscSpecMapper.listDiscSpec(obj);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }
}

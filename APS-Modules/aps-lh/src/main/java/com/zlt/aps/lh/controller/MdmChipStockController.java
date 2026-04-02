package com.zlt.aps.lh.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.mdm.api.domain.entity.MdmChipStock;
import com.zlt.aps.maindata.service.IMdmChipStockService;
import com.zlt.aps.maindata.mapper.MdmChipStockEntityMapper;
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
import java.util.List;

/**
 * 芯片库存 控制层类
 *
 * @author APS Team
 * @date 2026-04-02
 */
@Slf4j
@Api(tags = "芯片库存")
@RestController
@RequestMapping("/mdmChipStock")
public class MdmChipStockController extends AbstractDocBizController<MdmChipStock> {

    @Autowired
    private IMdmChipStockService mdmChipStockService;

    @Resource
    private MdmChipStockEntityMapper mdmChipStockEntityMapper;

    /**
     * 查询芯片库存列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmChipStock queryVO) {
        return super.list(queryVO);
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mdmChipStock.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmChipStock billVO){
        if (PubUtil.isEmpty(billVO.getFactoryCode())) {
            billVO.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        // 计算剩余可用量
        if (billVO.getStockNum() != null && billVO.getFinishQty() != null) {
            billVO.setRemainStockNum(billVO.getStockNum() - billVO.getFinishQty());
        }
        // 检查库存量 >= 完成量
        if (billVO.getStockNum() != null && billVO.getFinishQty() != null
                && billVO.getStockNum() < billVO.getFinishQty()) {
            return AjaxResult.error(com.ruoyi.common.i18n.utils.I18nUtil.getMessage("ui.data.alert.mdmChipStock.stockLessThanEditFinish"));
        }
        int result = mdmChipStockService.save(billVO);
        return result > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 合并保存 - 新增时检测到重复，将库存量和完成量累加到已有数据上
     */
    @Log(title = "ui.data.column.mdmChipStock.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("合并保存")
    @PostMapping("/mergeSave")
    @ResponseBody
    public AjaxResult mergeSave(@RequestBody MdmChipStock billVO) {
        if (PubUtil.isEmpty(billVO.getFactoryCode())) {
            billVO.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return mdmChipStockService.mergeSave(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mdmChipStock.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/remove")
    @ResponseBody
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids ) {
        // 检查是否有完成量，有则不允许删除
        for (Long id : ids) {
            MdmChipStock entity = mdmChipStockEntityMapper.selectById(id);
            if (entity != null && entity.getFinishQty() != null && entity.getFinishQty() > 0) {
                return AjaxResult.error(com.ruoyi.common.i18n.utils.I18nUtil.getMessage("ui.data.alert.mdmChipStock.hasFinishQtyCannotDelete"));
            }
        }
        int result = mdmChipStockService.removeByIds(ids);
        return result > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 获取芯片库存详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmChipStock getInfo(@PathVariable("billId") Long billId) {
        MdmChipStock entity = super.getInfo(billId);
        // 计算剩余可用量
        if (entity != null && entity.getStockNum() != null && entity.getFinishQty() != null) {
            entity.setRemainStockNum(entity.getStockNum() - entity.getFinishQty());
        }
        return entity;
    }

    /**
     * 根据集合导入芯片库存数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新（累加）
     * @return 结果
     */
    @Log(title = "ui.data.column.mdmChipStock.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "芯片库存", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmChipStock queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    /**
     * 更新完成量 - 供硫化排程回填调用
     */
    @ApiOperation("更新完成量")
    @PostMapping("/updateFinishQty")
    @ResponseBody
    public AjaxResult updateFinishQty(@RequestParam("factoryCode") String factoryCode,
                                     @RequestParam("chipCode") String chipCode,
                                     @RequestParam("finishQty") Integer finishQty) {
        int result = mdmChipStockService.updateFinishQty(factoryCode, chipCode, finishQty);
        return result > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    @Override
    protected List<MdmChipStock> listExportData(MdmChipStock obj) {
        QueryWrapper<MdmChipStock> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<MdmChipStock> list = mdmChipStockEntityMapper.selectList(wrapper);
        // 计算剩余可用量
        for (MdmChipStock item : list) {
            if (item.getStockNum() != null && item.getFinishQty() != null) {
                item.setRemainStockNum(item.getStockNum() - item.getFinishQty());
            }
        }
        return list;
    }

    @Override
    protected IDocService getDocService(){
        return mdmChipStockService;
    }

    @Override
    protected String[] getQueryFormulas() {
        return mdmChipStockService.getQueryFormulas();
    }

    /**
     * 条件拼接 - 所有数据库字段都支持查询
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MdmChipStock> queryWrapper, MdmChipStock queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getCompanyCode()), "COMPANY_CODE", queryVO.getCompanyCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getChipCode()), "CHIP_CODE", queryVO.getChipCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getStockNum()), "STOCK_NUM", queryVO.getStockNum());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFinishQty()), "FINISH_QTY", queryVO.getFinishQty());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getDataVersion()), "DATA_VERSION", queryVO.getDataVersion());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getRemark()), "REMARK", queryVO.getRemark());
    }

    @Override
    protected String getTypeCode(){
        return "0115";
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

}

package com.zlt.aps.cx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.dto.CxMonthStockDto;
import com.zlt.aps.cx.entity.CxMonthStock;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 成型月结库存Service接口
 *
 * @author chen
 * @date 2021-06-17
 */
public interface CxMonthStockService extends IService<CxMonthStock> {
    /**
     * 查询成型月结库存列表
     *
     * @param dto 成型月结库存
     * @return 成型月结库存集合
     */
    public List<CxMonthStockDto> selectCxMonthStockList(CxMonthStockDto dto);

    /**
     * 查询成型月结库存
     *
     * @param id 成型月结库存ID
     * @return 成型月结库存
     */
    public CxMonthStock selectCxMonthStockById(Long id);

    /**
     * 修改成型月结库存
     *
     * @param monthStock 成型月结库存
     */
    @Transactional
    public void saveCxMonthStock(CxMonthStock monthStock);

    /**
     * 批量删除成型月结库存
     *
     * @param ids 需要删除的成型月结库存ID
     */
    @Transactional
    public void deleteCxMonthStockByIds(Long[] ids);

    /**
     * 校验唯一性
     * @param stock 要校验的记录
     * @return 是否唯一
     */
    public String checkUnique(CxMonthStock stock);

    /**
     * 导入数据
     */
    AjaxResult importData(List<CxMonthStockDto> list, boolean updateSupport, Long importLogId);
}

package com.zlt.aps.gsq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gsq.api.domain.dto.GsqReserveStockDto;
import com.zlt.aps.gsq.entity.GsqReserveStock;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * 钢丝圈预生产库存倍数设定Service接口
 *
 * @author hak
 * @date 2025-02-11
 */
public interface GsqReserveStockService extends IService<GsqReserveStock> {

    /**
     * 查询钢丝圈预生产库存倍数设定列表
     *
     * @param reserveStock 钢丝圈预生产库存倍数设定
     * @return 钢丝圈预生产库存倍数设定集合
     */
    public List<GsqReserveStockDto> selectReserveStockList(GsqReserveStock reserveStock);

    /**
     * 查询钢丝圈预生产库存倍数设定
     *
     * @param id 钢丝圈预生产库存倍数设定ID
     * @return 钢丝圈预生产库存倍数设定
     */
    public GsqReserveStock selectReserveStockById(Long id);

    /**
     * 修改钢丝圈预生产库存倍数设定
     *
     * @param reserveStock 钢丝圈预生产库存倍数设定
     */
    @Transactional
    public AjaxResult saveReserveStock(GsqReserveStock reserveStock);

    /**
     * 批量删除钢丝圈预生产库存倍数设定
     *
     * @param ids 需要删除的钢丝圈预生产库存倍数设定ID
     */
    @Transactional
    public void deleteReserveStockByIds(Long[] ids);

    /**
     * 验证预生产库存倍数设定信息唯一性
     */
    public String checkUnique(GsqReserveStock reserveStock);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<GsqReserveStockDto> list, boolean updateSupport, Long importLogId);
}

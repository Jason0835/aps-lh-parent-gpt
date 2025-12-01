package com.zlt.aps.gdyy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gdyy.api.domain.dto.GdyyReserveStockDto;
import com.zlt.aps.gdyy.entity.GdyyReserveStock;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * 钢带压延预生产库存倍数设定Service接口
 *
 * @author hak
 * @date 2025-02-11
 */
public interface GdyyReserveStockService extends IService<GdyyReserveStock> {

    /**
     * 查询钢带压延预生产库存倍数设定列表
     *
     * @param reserveStock 钢带压延预生产库存倍数设定
     * @return 钢带压延预生产库存倍数设定集合
     */
    public List<GdyyReserveStockDto> selectReserveStockList(GdyyReserveStock reserveStock);

    /**
     * 查询钢带压延预生产库存倍数设定
     *
     * @param id 钢带压延预生产库存倍数设定ID
     * @return 钢带压延预生产库存倍数设定
     */
    public GdyyReserveStock selectReserveStockById(Long id);

    /**
     * 修改钢带压延预生产库存倍数设定
     *
     * @param reserveStock 钢带压延预生产库存倍数设定
     */
    @Transactional
    public AjaxResult saveReserveStock(GdyyReserveStock reserveStock);

    /**
     * 批量删除钢带压延预生产库存倍数设定
     *
     * @param ids 需要删除的钢带压延预生产库存倍数设定ID
     */
    @Transactional
    public void deleteReserveStockByIds(Long[] ids);

    /**
     * 验证预生产库存倍数设定信息唯一性
     */
    public String checkUnique(GdyyReserveStock reserveStock);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<GdyyReserveStockDto> list, boolean updateSupport, Long importLogId);
}

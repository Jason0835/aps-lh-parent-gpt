package com.zlt.aps.xwyy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.xwyy.api.domain.dto.XwyyReserveStockDto;
import com.zlt.aps.xwyy.entity.XwyyReserveStock;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * 纤维压延预生产库存倍数设定Service接口
 *
 * @author hak
 * @date 2025-02-11
 */
public interface XwyyReserveStockService extends IService<XwyyReserveStock> {

    /**
     * 查询纤维压延预生产库存倍数设定列表
     *
     * @param reserveStock 纤维压延预生产库存倍数设定
     * @return 纤维压延预生产库存倍数设定集合
     */
    public List<XwyyReserveStockDto> selectReserveStockList(XwyyReserveStock reserveStock);

    /**
     * 查询纤维压延预生产库存倍数设定
     *
     * @param id 纤维压延预生产库存倍数设定ID
     * @return 纤维压延预生产库存倍数设定
     */
    public XwyyReserveStock selectReserveStockById(Long id);

    /**
     * 修改纤维压延预生产库存倍数设定
     *
     * @param reserveStock 纤维压延预生产库存倍数设定
     */
    @Transactional
    public AjaxResult saveReserveStock(XwyyReserveStock reserveStock);

    /**
     * 批量删除纤维压延预生产库存倍数设定
     *
     * @param ids 需要删除的纤维压延预生产库存倍数设定ID
     */
    @Transactional
    public void deleteReserveStockByIds(Long[] ids);

    /**
     * 验证预生产库存倍数设定信息唯一性
     */
    public String checkUnique(XwyyReserveStock reserveStock);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<XwyyReserveStockDto> list, boolean updateSupport, Long importLogId);
}

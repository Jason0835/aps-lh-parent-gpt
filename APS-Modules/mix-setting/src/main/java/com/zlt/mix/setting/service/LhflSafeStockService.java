package com.zlt.mix.setting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.setting.api.domain.entity.LhflSafeStock;

import java.math.BigDecimal;
import java.util.List;

/**
 * 硫磺辅料安全库存Service接口
 * 
 * @author hakimryan
 *
 */
public interface LhflSafeStockService extends IService<LhflSafeStock> {
	/**
	 * 查询安全库存列表
	 * 
	 * @param lhflSafeStock 安全库存
	 * @return 安全库存集合
	 */
	List<LhflSafeStock> selectLhflSafeStockList(LhflSafeStock lhflSafeStock);

	/**
	 * 保存安全库存信息（id为空则新增，id不为空则修改）
	 *
	 * @param lhflSafeStock
	 */
	void saveLhflSafeStock(LhflSafeStock lhflSafeStock);

	/**
	 * 批量删除安全库存
	 * 
	 * @param ids 需要删除的安全库存ID
	 * @return 结果
	 */
	int deleteLhflSafeStockByIds(Long[] ids);

	/**
	 * 校验安全库存唯一性
	 */
	String checkLhflSafeStockUnique(LhflSafeStock lhflSafeStock);

	/**
	 * 导入安全库存数据
	 */
	AjaxResult importData(List<LhflSafeStock> list, boolean updateSupport, Long importLogId);

	/**
	 * 查询安全库存
	 *
	 * @param mixArea  密炼区
	 * @param material 胶料名称
	 * @return 安全库存
	 */
	BigDecimal selectLhflSafeStock(String mixArea, String material);

	/**
	 * 有则更新，无则插入
	 *
	 * @param mixArea   密炼区
	 * @param material  胶料名称
	 * @param safeStock 安全库存
	 */
	void saveOrUpdateLhflSafeStock(String mixArea, String material, BigDecimal safeStock);

	/**
	 * 根据密炼区和胶料名称更改安全库存
	 */
	void updateSafeStockByMixAreaAndLhfl(LhflSafeStock lhflSafeStock);
}

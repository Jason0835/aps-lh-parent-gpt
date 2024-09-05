package com.zlt.aps.cx.service;

import java.util.List;

import com.zlt.aps.cx.api.domain.dto.LhMachineInfoDto;

/**
 * 硫化机台-胎胚库存接口
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-12-17 10:03:21
 */
public interface LhMachineInfoEmbyroStockService {
	/**
	 * 查询硫化机台列表，根据胎胚库存顺序排序
	 * 
	 * @return
	 */
	List<LhMachineInfoDto> getList();

	/**
	 * 查询硫化机台列表，根据胎胚库存顺序排序
	 * 
	 * @param machineName 机台名称
	 * @return
	 */
	List<LhMachineInfoDto> getList(String machineName);
}

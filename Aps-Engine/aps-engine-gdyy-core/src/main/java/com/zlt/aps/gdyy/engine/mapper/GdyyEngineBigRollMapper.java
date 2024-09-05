package com.zlt.aps.gdyy.engine.mapper;

import java.util.List;

import com.zlt.aps.gdyy.engine.vo.GdyyBigRollVo;

/**
 * 钢压大卷信息数据Mapper
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-19 11:37:24
 * @Version 1.0
 */
public interface GdyyEngineBigRollMapper {
	/**
	 * 获取钢压大卷配置信息
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-19 10:01:43
	 * @return
	 */
	List<GdyyBigRollVo> listCd15BigRoll();
}

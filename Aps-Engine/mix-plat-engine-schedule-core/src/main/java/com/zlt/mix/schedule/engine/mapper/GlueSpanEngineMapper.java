package com.zlt.mix.schedule.engine.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.mix.schedule.engine.vo.GlueSpanReceiveVo;

/**
 * 引擎胶料跨区模块相关mapper
 */
public interface GlueSpanEngineMapper {
	/**
	 * 根据查询条件获取胶料跨区接收信息
	 * 
	 * @param glueSpanReceiveVo 查询条件
	 * @return
	 */
	List<GlueSpanReceiveVo> listGlueSpanReceive(@Param("params") GlueSpanReceiveVo glueSpanReceiveVo);
}

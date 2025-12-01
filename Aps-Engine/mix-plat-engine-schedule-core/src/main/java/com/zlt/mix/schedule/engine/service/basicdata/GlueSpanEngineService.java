package com.zlt.mix.schedule.engine.service.basicdata;

import java.util.List;

import com.zlt.mix.schedule.engine.vo.GlueSpanReceiveVo;

/**
 * 引擎胶料跨区服务
 * 
 * @author hakimryan
 *
 */
public interface GlueSpanEngineService {
	/**
	 * 根据查询条件获取胶料跨区接收信息
	 * 
	 * @param glueSpanReceiveVo 查询条件
	 * @return
	 */
	List<GlueSpanReceiveVo> listGlueSpanReceive(GlueSpanReceiveVo glueSpanReceiveVo);
}

package com.zlt.mix.schedule.engine.service.glueschedule;

import java.util.List;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.schedule.api.domain.entity.MaterialScheduleResult;
import com.zlt.mix.schedule.engine.vo.GlueScheduleResultVo;

/**
 * 排程发布服务
 * 
 * @author hakimryan
 *
 */
public interface GlueScheduleEnginePublishService {

	/**
	 * 向MES发布胶料排程记录
	 * @param scheduleList
	 */
	AjaxResult publishGlueScheduleResult(List<GlueScheduleResultVo> scheduleList);

	/**
	 * 向MES发布硫磺辅料排程记录
	 * @param scheduleList
	 */
	AjaxResult publishMaterialScheduleResult(List<MaterialScheduleResult> scheduleList);
}

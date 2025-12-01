package com.zlt.aps.monthplan.common.accept;


/**
 * 接受器(可以使用与所有可能需要有外部决定是否接受的机制)
 *
 */
public interface IAccept {

	/**
	 * 是否能够接受
	 * @param obj
	 * @return
	 */
	boolean isAccept(Object obj);

	/**
	 * 接受处理完毕后的收尾事件
	 */
	void afterAccept(Object object);

}

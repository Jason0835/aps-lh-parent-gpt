package com.zlt.aps.monthplan.common.hashlize;

public interface IHashKey {

	/**
	 * Hash化一组对象时，取得一个给定对象的Key的方法
	 * @throws Exception
	 */
	String getKey(Object o) ;
}

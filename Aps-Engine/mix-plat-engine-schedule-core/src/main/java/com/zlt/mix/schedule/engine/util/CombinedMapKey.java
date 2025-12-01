package com.zlt.mix.schedule.engine.util;

import java.util.Arrays;
import java.util.List;

/**
 * 组合Map键
 * 
 * @author hakimryan
 *
 */
public class CombinedMapKey {
	private List<Object> keys;

	private CombinedMapKey(Object... keys) {
		this.keys = Arrays.asList(keys);
	}

	/**
	 * 根据参数创建组合键
	 * 
	 * @param keys
	 * @return
	 */
	public final static CombinedMapKey createKey(Object... keys) {
		return new CombinedMapKey(keys);
	}

	/**
	 * 获取指定下标的key对象
	 * 
	 * @param index 选择下标
	 * @return
	 */
	public Object getKey(int index) {
		if (index >= keys.size()) {
			return null;
		}
		return keys.get(index);
	}

	@Override
	public int hashCode() {
		return keys.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof CombinedMapKey)) {
			return false;
		}
		return keys.equals(((CombinedMapKey) obj).keys);
	}
}

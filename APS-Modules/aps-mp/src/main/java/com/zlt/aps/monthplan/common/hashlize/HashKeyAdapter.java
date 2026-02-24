package com.zlt.aps.monthplan.common.hashlize;


import com.zlt.aps.monthplan.common.utils.PubUtil;
import org.apache.commons.beanutils.PropertyUtils;

import java.lang.reflect.InvocationTargetException;

/**
 * 该类实现了IHashKey接口。 构造时以String数组的形式接受一组AttributeNames，
 * getKey方法接受一个CircularlyAccessibleValueObject类型的参数
 * 返回构造时传入的AttributeNames所对应的值的组合。
 * 若构造时接受的AttributeNames为空，则对所有的CircularlyAccessibleValueObject
 * 对象getKey方法返回一个固定值"FIXKEY#$@"
 */
public class HashKeyAdapter implements IHashKey {
	String[] m_KeyFields;
	static final String m_FixKey = "FIXKEY#$@";

	/**
	 * VOHashKeyAdatper 构造子注解。
	 */
	public HashKeyAdapter(String[] keyFields) {
		super();
		m_KeyFields = keyFields;
	}

	/**
	 * getKey 方法注解。
	 */
	@Override
	public String getKey(Object o) {

		if (m_KeyFields == null || m_KeyFields.length == 0) {
            return m_FixKey;
        }

		String key = "";
		String value = null;
		for (int i = 0; i < m_KeyFields.length; i++) {
			try {
				if (PubUtil.isNotEmpty(PropertyUtils.getSimpleProperty(o, m_KeyFields[i]))) {
                    value = PropertyUtils.getSimpleProperty(o, m_KeyFields[i]).toString().trim();
                } else {
                    value = null;
                }

				key = key + value;
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e.getMessage(), e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e.getMessage(), e);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}

		return key;
	}
}

package com.zlt.mix.schedule.engine.util;

/**
 * ID生成器
 *
 */
public class IdGenerator {
	private Long id;
	
	private Long increment;
	
	private IdGenerator(Long id, Long increment) {
		this.id = id;
		this.increment = increment;
	}
	
	/**
	 * 正数
	 * @param id
	 * @return
	 */
	public static IdGenerator positive() {
		return new IdGenerator(1L, 1L);
	}
	
	/**
	 * 正数
	 * @param id
	 * @return
	 */
	public static IdGenerator negative() {
		return new IdGenerator(-1L, -1L);
	}
	
	/**
	 * 下一个ID
	 * @return
	 */
	public Long next() {
		return this.id = this.id + this.increment;
	}
}

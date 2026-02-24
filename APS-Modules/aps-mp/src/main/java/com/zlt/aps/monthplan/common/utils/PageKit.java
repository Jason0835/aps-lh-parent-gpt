package com.zlt.aps.monthplan.common.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 分页工具类
 * 
 * @author Yelq
 * 
 */
public class PageKit {

	/**
	 * 将页数和每页条目数转换为开始位置和结束位置<br>
	 * 此方法用于不包括结束位置的分页方法<br>
	 * 例如：<br>
	 * 页码：1，每页10 -> [0, 10]<br>
	 * 页码：2，每页10 -> [10, 20]<br>
	 * 。。。<br>
	 * 
	 * @param pageNo
	 *            页码（从1计数）
	 * @param countPerPage
	 *            每页条目数
	 * @return 第一个数为开始位置，第二个数为结束位置
	 */
	public static int[] transToStartEnd(int pageNo, int countPerPage) {
		if (pageNo < 1) {
			pageNo = 1;
		}

		if (countPerPage < 1) {
			  countPerPage = 0;
//			LogKit.warn("Count per page  [" + countPerPage + "] is not valid!");
		}

		int start = (pageNo - 1) * countPerPage;
		int end = start + countPerPage;

		return new int[] { start, end };
	}

	/**
	 * 合并所有集合
	 */
	public static <T> List<T> mergeCollections(Collection<? extends T>[] collections) {
		if (collections == null || collections.length == 0) {
			return new ArrayList<>();
		}

		// 预先计算总大小以提高性能
		int totalSize = Arrays.stream(collections)
				.filter(Objects::nonNull)
				.mapToInt(Collection::size)
				.sum();

		List<T> mergedList = new ArrayList<>(totalSize);
		for (Collection<? extends T> collection : collections) {
			if (collection != null && !collection.isEmpty()) {
				mergedList.addAll(collection);
			}
		}

		return mergedList;
	}

	/**
	 * 计算分页边界
	 */
	public static PaginationBounds calculatePaginationBounds(int pageNo, int pageSize, int totalSize) {
		int start = (pageNo - 1) * pageSize;
		int end = Math.min(start + pageSize, totalSize);

		// 确保开始位置不为负
		start = Math.max(0, start);

		return new PaginationBounds(start, end);
	}

	/**
	 * 分页边界封装类
	 */
	public static class PaginationBounds {
		final int start;
		final int end;

		PaginationBounds(int start, int end) {
			this.start = start;
			this.end = end;
		}
	}

	/**
	 * 替代原 transToStartEnd 的方法
	 * 更安全的边界计算，不会越界
	 */
	public static int[] calculateStartEnd(int pageNo, int pageSize, int totalSize) {
		if (pageNo < 1 || pageSize < 1) {
			throw new IllegalArgumentException("页码和每页大小必须大于等于1");
		}

		int start = (pageNo - 1) * pageSize;

		// 如果开始位置已经超过总数，返回[0,0]表示无数据
		if (start >= totalSize) {
			return new int[]{0, 0};
		}

		int end = Math.min(start + pageSize, totalSize);
		start = Math.max(0, start);

		return new int[]{start, end};
	}

	/**
	 * 根据总数计算总页数
	 * 
	 * @param totalCount
	 *            总数
	 * @param numPerPage
	 *            每页数
	 * @return 总页数
	 */
	public static int totalPage(int totalCount, int numPerPage) {
		if (numPerPage == 0) {
			return 0;
		}
		return totalCount % numPerPage == 0 ? (totalCount / numPerPage)
				: (totalCount / numPerPage + 1);
	}
}

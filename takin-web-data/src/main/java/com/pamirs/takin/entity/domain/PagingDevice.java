package com.pamirs.takin.entity.domain;

import java.util.ArrayList;
import java.util.List;

import io.shulie.takin.cloud.ext.content.trace.ContextExt;

/**
 * @author vernon
 * @date 2019/12/2 10:42
 */
public class PagingDevice extends ContextExt implements Cloneable {
    private int count;
    private int pageSize = 20;
    private int current = 0;

    public int getOffset() {
        int i = getCurrentPage() * pageSize;
        return Math.max(i, 0);
    }

    public int getTotalPage() {
        if (count <= 0 || pageSize <= 0) {
            return 0;
        }
        return count / pageSize + (count % pageSize > 0 ? 1 : 0);
    }

    public int getLastPage() {
        int totalPage = getTotalPage();
        return totalPage == 0 ? 0 : totalPage - 1;
    }

    public int getPrevPage() {
        return getCurrentPage() > 1 ? getCurrentPage() - 1 : 0;
    }

    public int getNextPage() {
        return getCurrentPage() >= getTotalPage() - 1 ? getCurrentPage() : getCurrentPage() + 1;
    }

    public List<Integer> getPages(int num) {
        int totalPage = getTotalPage();
        num = Math.min(num, totalPage);

        int beforeStart, endStart;
        int endNum, beforeNum;
        if (num < 1) {
            num = 1;
        }
        endNum = (int)(num * 0.4);
        beforeNum = num - endNum;
        endStart = totalPage - endNum;
        beforeStart = current - beforeNum / 2;
        if (beforeStart < 0) {
            beforeStart = 0;
        } else if (beforeStart > endStart - beforeNum) {
            beforeStart = endStart - beforeNum;
        }

        List<Integer> nums = new ArrayList<>();
        for (int i = 0; i < beforeNum; i++) {
            nums.add(i + beforeStart);
        }

        if (totalPage > num) {
            nums.add(1);
        }
        for (int i = 0; i < endNum; i++) {
            nums.add(i + endStart);
        }

        return nums;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCurrentPage() {
        return current;
    }

    public void setCurrentPage(Integer currentPage) {
        currentPage = currentPage == null ? 0 : currentPage;
        this.current = currentPage < 0 ? 0 : currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        pageSize = pageSize == null ? getPageSize() : pageSize;
        this.pageSize = pageSize;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public String getAjaxResponse() {
        return "";
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }
}
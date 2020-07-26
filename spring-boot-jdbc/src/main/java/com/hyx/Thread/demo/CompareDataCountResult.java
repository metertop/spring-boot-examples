package com.hyx.Thread.demo;

/**
 * @Description:
 * @Author: haoyuexun
 * @Date: 2020-07-26 20:57
 */
public class CompareDataCountResult {

    public Integer getPassCount() {
        return passCount;
    }

    public void setPassCount(Integer passCount) {
        this.passCount = passCount;
    }

    public Integer getFailCount() {
        return failCount;
    }

    public void setFailCount(Integer failCount) {
        this.failCount = failCount;
    }

    private Integer passCount;
    private Integer failCount;
}

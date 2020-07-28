package com.lyh.demo;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description:
 * @Author: haoyuexun
 * @Date: 2020-07-29 00:22
 */
public class Test1 {
    private  Logger logger = LoggerFactory.getLogger(getClass());
    @Test
    public void test1() {
        int totalDataRows = 110000;
        int pageSize = 10000;

        int pageCount = (int) Math.ceil(totalDataRows/pageSize);   // 需要的总页数
        int pageCountPerThread = (int) Math.ceil(pageCount/10);   // 每个线程的页数为

        logger.error("查询的数据量total={}", totalDataRows);
        if (totalDataRows <= pageSize) {
           logger.error("仅仅用1个线程即可");
        } else {
            logger.error("totalDataRows={},pageCount={},pageCountPerThread={}", totalDataRows, pageCount, pageCountPerThread);


            int pageNoStart = 1;
            int pageNoEnd = pageCountPerThread;
            for (int i = 1; i <= 10; i++) {
                if (i > pageCount){
                    break;
                }
                String threadName = "线程" + i;
                logger.error("线程[{}]->pageNoStart={},pageNoEnd={},该线程需要使用页数={}", threadName, pageNoStart, pageNoEnd, pageCount);
//                MydataThreadList.add(new MyDataThread("线程" + i, oldTablesqlString, pageNoStart, pageNoEnd, pageSize));
                pageNoStart += pageCountPerThread;
                pageNoEnd = pageNoStart + pageCountPerThread-1;


            }
        }
    }
}

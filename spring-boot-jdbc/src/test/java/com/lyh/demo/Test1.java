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
        int totalDataRows = 20000;
        int pageSize = 10000;
        int dataRowPerThread = totalDataRows;   // 10个线程数,1个线程的数量
        int pageCount = (int) Math.ceil(dataRowPerThread/pageSize);   // 需要的页数
        logger.error("查询的数据量total={}", totalDataRows);
        if (totalDataRows <= pageSize) {
           logger.error("仅仅用1个线程即可");
        } else {
            logger.error("dataRowPerThread={},pageCount={}", dataRowPerThread, pageCount);


            int pageNoStart = 1;
            int pageNoEnd = pageNoStart + 1;
            for (int i = 1; i <= 10; i++) {
                if (i > pageCount){
                    break;
                }
                String threadName = "线程" + i;
                logger.error("线程[{}]->pageNoStart={},pageNoEnd={},该线程需要使用页数={}", threadName, pageNoStart, pageNoEnd, pageCount);
//                MydataThreadList.add(new MyDataThread("线程" + i, oldTablesqlString, pageNoStart, pageNoEnd, pageSize));
                pageNoStart = (i - 1) * pageCount + 1;
                pageNoEnd = i * pageCount + 1;


            }
        }
    }
}

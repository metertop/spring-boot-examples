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
        Integer totalDataRows = 1200010;
        Integer pageSize = 10000;
        int threadNum = 10;



        int pageCount = (int) Math.ceil(totalDataRows.doubleValue()/pageSize.doubleValue());   // 需要的总页数

        int pageCountPerThread = pageCount/threadNum;   // 每个线程的页数为

        logger.error("查询的数据量total={}", totalDataRows);
        if (totalDataRows <= pageSize) {
           logger.error("仅仅用1个线程即可");
        } else {
            logger.error("totalDataRows={},pageCount={},pageCountPerThread={}", totalDataRows, pageCount, pageCountPerThread);


            int pageNoStart = 1;
            int pageNoEnd = pageCountPerThread;
            for (int i = 1; i <= threadNum; i++) {
                if (i > pageCount){
                    break;
                }
                if (i==threadNum) {    // 最后一个线程将多余数据进行查询
                    pageNoEnd = pageCount;
                }
                String threadName = "线程" + i;
                logger.error("线程[{}]->pageNoStart={},pageNoEnd={},该线程需要使用页数={}", threadName, pageNoStart, pageNoEnd, pageCount);
//                MydataThreadList.add(new MyDataThread("线程" + i, oldTablesqlString, pageNoStart, pageNoEnd, pageSize));
                pageNoStart += pageCountPerThread;
                pageNoEnd = pageNoStart + pageCountPerThread-1;


            }
        }
    }


    @Test
    public void test2() {
        String str1 = "id,stu_id,content,type";
        StringBuilder sb = new StringBuilder();
        String[] strArr = str1.split(",");

        for (String str: strArr) {
            sb.append(",a." + str);
        }

        logger.info("sb=={}", sb.toString().substring(1));

    }


    @Test
    public void test3() {
        String str1 = "id,stu_id,content,type";
        logger.info("sb-->{}", getRefTableFields(str1));
    }


    private String getRefTableFields(String tableFields) {
        StringBuilder sb = new StringBuilder();
        String[] strArr = tableFields.split(",");

        for (String str: strArr) {
            sb.append(",a." + str);
        }

        return sb.toString().substring(1);
    }


    @Test
    public void test4() {
        String str1 = "2455,2110,0.00,point";
        String str2 = str1.substring(0, str1.indexOf(","));
        logger.info("----->str2={}", str2);
    }


}

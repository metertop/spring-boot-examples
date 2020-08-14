package com.hyx.Thread.demo;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import com.google.common.net.InternetDomainName;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Description:
 * @Author: haoyuexun
 * @Date: 2020/7/24 14:49
 */
@Service("CheckPositionService")
public class CheckPositionServiceImpl implements CheckPositionService {

    private static Logger logger = LoggerFactory.getLogger(CheckPositionServiceImpl.class);

    @Autowired
    private ThreadPoolUtil threadPoolUtil;


    private static DruidDataSource dataSourceMDB = null;
    private static SqlSessionFactory sqlSessionFactory = null;

    AtomicInteger passNum = new AtomicInteger(0);
    AtomicInteger failNum = new AtomicInteger(0);
    AtomicInteger compareThreadNum = new AtomicInteger(0);

    Integer totalPassNum = 0;
    Integer totalFailNum = 0;

    Integer oldTableCounts = 0;
    int threadNum = 10;

    List<String> oldTableValuesAll = new LinkedList<>();

    //声明Connection对象
    Connection con;
    //驱动程序名
    String driver = "com.mysql.jdbc.Driver";
    //URL指向要访问的数据库名mydata
    String url = "jdbc:mysql://172.16.70.20:3306/point?useUnicode=true&amp;characterEncoding=utf8&amp;connectTimeout=5000&amp;socketTimeout=6000000&amp;autoReconnect=true&amp;failOverReadOnly=false&amp;allowMultiQueries=true";
    //MySQL配置时的用户名
    String user = "rd_user";
    //MySQL配置时的密码
    String password = "NTHXDF7czYwi";

    @Override
    public SqlSession getSqlSession() {
        if(dataSourceMDB == null || sqlSessionFactory == null){
            dataSourceMDB = new DruidDataSource();
            //设置连接参数
            dataSourceMDB.setUrl(url);
            dataSourceMDB.setDriverClassName(driver);
            dataSourceMDB.setUsername(user);
            dataSourceMDB.setPassword(password);
            //配置初始化大小、最小、最大
            dataSourceMDB.setInitialSize(10);
            dataSourceMDB.setMinIdle(10);
            dataSourceMDB.setMaxActive(5000);
            //连接泄漏监测
            //dataSourceMDB.setRemoveAbandoned(true);
            //dataSourceMDB.setRemoveAbandonedTimeout(30);
            //配置获取连接等待超时的时间
            dataSourceMDB.setMaxWait(500000);
            //配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒
            dataSourceMDB.setTimeBetweenEvictionRunsMillis(20000);
            //防止过期
            dataSourceMDB.setValidationQuery("SELECT 'x'");
            dataSourceMDB.setTestWhileIdle(true);
            dataSourceMDB.setTestOnBorrow(true);

            TransactionFactory transactionFactory = new JdbcTransactionFactory();
            Environment environment =  new Environment("development", transactionFactory, dataSourceMDB);
            Configuration configuration = new Configuration(environment);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
            return sqlSessionFactory.openSession();
        }else{
            return sqlSessionFactory.openSession();
        }
    }

    @Override
    public TableDto getOldTableDetail(CheckTableInfo checkTableInfo) {

        String oldTable = checkTableInfo.getOldTable();
        String oldRealtionField = checkTableInfo.getOldTableRelationField();
        String oldFileds = oldRealtionField + "," +checkTableInfo.getQueryOldTableFileds();
        String oldQueryCondition = checkTableInfo.getQueryOldTableWhereCondition();
        TableDto oldTableDto = new TableDto();

        String sqlCount = String.format("select count(1) as rowCount from %s where %s",  oldTable, oldQueryCondition);
        String sqlContent = String.format("select %s from %s where %s", oldFileds, oldTable, oldQueryCondition);

        logger.error("sqlCount-->{}", sqlCount);
        logger.error("sqlContent-->{}", sqlContent);
        ResultSet rsCount = null;
        ResultSet rsContent = null;
        List<String> tableColumnValues = new ArrayList<>();
        try {

            SqlSession session = getSqlSession();
            Connection con = session.getConnection();
            if(!con.isClosed()){
//                System.out.println("Succeeded connecting to the Database!");
            }
            PreparedStatement psCount = con.prepareStatement(sqlCount);
            PreparedStatement psContent = con.prepareStatement(sqlContent);
            rsCount = psCount.executeQuery();
            rsContent = psContent.executeQuery();

            oldTableDto.setTableName(oldTable);
            oldTableDto.setTableColumns(oldFileds);
            if(rsCount.next()) {
                oldTableCounts = rsCount.getInt(1);
            }
            logger.info("老表{}-->count={}", oldTable, oldTableCounts);
            oldTableDto.setTableLength(oldTableCounts);
            while(rsContent.next()){
                tableColumnValues.add(getResultByType(rsContent));
            }
//            logger.error("老表查询数据是：{}", JSON.toJSONString(tableColumnValues));
            oldTableDto.setTableColumnsValues(tableColumnValues);
            logger.error("老表设置数据完毕！");
            rsCount.close();
            rsContent.close();
            psCount.close();
            rsContent.close();
            con.close();
            session.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return oldTableDto;
    }

    @Override
    public void checkData2(CheckTableInfo checkTableInfo){


        String oldTable = checkTableInfo.getOldTable();
        String oldRealtionField = checkTableInfo.getOldTableRelationField();
        String oldFileds = oldRealtionField + "," +checkTableInfo.getQueryOldTableFileds();
        String whereCondition = checkTableInfo.getQueryOldTableWhereCondition();

        String newTable = checkTableInfo.getNewTable();
        String newTableRelationField = checkTableInfo.getNewTableRelationField();
        String newFields = checkTableInfo.getQueryNewTableFileds();

//        TableDto oldTableDto = new TableDto();

        String sqlCount = String.format("select count(1) as rowCount from %s where %s",  oldTable, whereCondition);
        String sqlContent = String.format("select %s from %s where %s", oldFileds, oldTable, whereCondition);

        logger.error("sqlCount-->{}", sqlCount);
        logger.error("sqlContent-->{}", sqlContent);

        String whereSql = "";
        Integer pageSize = 10000;
        Integer totalDataRows = getTableRows(oldTable, whereCondition)/100;
        if (!whereCondition.trim().equals("") && whereCondition != null) {
            whereSql = String.format("where %s", whereCondition);
        }
//        String oldTableSqlString = String.format("select %s from % %s", oldFileds, oldTable, whereSql);

        String oldRefFields = StringUtil.getRefTableFields(oldFileds);

        String oldTableSqlString = String.format("select %s from %s as a inner join (select id from %s %s limit ?,?) as b on a.id=b.id"
                                    ,oldRefFields, oldTable, oldTable, whereSql);

        class MyDataThread implements Runnable{
            String threadName;
            String querySql;
            Integer pageSize;
            Integer pageNoStart;
            Integer pageNoEnd;

            public MyDataThread(String threadName, String querySql, Integer pageNoStart, Integer pageNoEnd,
                            Integer pageSize) {
                this.threadName = threadName;
                this.pageNoStart = pageNoStart;
                this.pageNoEnd = pageNoEnd;
                this.pageSize = pageSize;
                this.querySql = querySql;
            }
            @Override
            public void run() {
                List<String> resultList;
                List<String> resultListAll = new LinkedList<>();
                for (int pageNo = pageNoStart; pageNo <= pageNoEnd; pageNo++) {
                    resultList = getTableResults(querySql, pageNo, pageSize);
                    resultListAll.addAll(resultList);
                }
                Integer dataTotal = resultListAll.size();
                logger.error("线程-[{}]旧表数据量为{}--->当前时间戳是:{}", threadName, dataTotal, System.currentTimeMillis());

                AtomicInteger passCount = new AtomicInteger(0);
                AtomicInteger failCount = new AtomicInteger(0);

                Iterator<String> it = resultListAll.iterator();

                while(it.hasNext()) {
                    String oldTableValue = it.next();
                    Boolean isPass = compareOldAndNewPass(newTable, newFields, newTableRelationField, oldTable, oldFileds, oldTableValue);
                    if (isPass) {
                        passCount.incrementAndGet();
                    }else {
                        failCount.incrementAndGet();
                    }
                }
                logger.error("线程-[{}]数据一致数量是：{}--数据不一致数量是：{}--->当前时间戳是:{}", threadName, passCount.get(), failCount.get(), System.currentTimeMillis());

            }

        }

        List<Runnable> myDataThreadList = new ArrayList<>();
        int threadNum = 10;
        int pageCount = (int) Math.ceil(totalDataRows.doubleValue()/pageSize.doubleValue());   // 需要的总页数
        int pageCountPerThread = pageCount/threadNum == 0 ? 1: pageCount/threadNum;   // 每个线程的页数为

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
                if (i == threadNum) {    // 最后一个线程将多余数据进行查询
                    pageNoEnd = pageCount;
                }
                String threadName = "线程" + i;
                logger.error("线程[{}]->pageNoStart={},pageNoEnd={},该线程需要使用页数={}", threadName, pageNoStart, pageNoEnd, pageCount);
                myDataThreadList.add(new MyDataThread("线程" + i, oldTableSqlString, pageNoStart, pageNoEnd, pageSize));
                pageNoStart += pageCountPerThread;
                pageNoEnd = pageNoStart + pageCountPerThread-1;
            }
        }

        threadPoolUtil.executeTasks(myDataThreadList);

    }

    @Override
    public void checkData3(CheckTableInfo checkTableInfo) {
        int threadNum = 10;
        String oldTable = checkTableInfo.getOldTable();
        String oldRealtionField = checkTableInfo.getOldTableRelationField();
        String oldFields = oldRealtionField + "," +checkTableInfo.getQueryOldTableFileds();    // id 在前
        String whereCondition = checkTableInfo.getQueryOldTableWhereCondition();

        String newTable = checkTableInfo.getNewTable();
        String newTableRelationField = checkTableInfo.getNewTableRelationField();
        String newFields = newTableRelationField + "," + checkTableInfo.getQueryNewTableFileds();

        String sqlCount = String.format("select count(1) as rowCount from %s where %s",  oldTable, whereCondition);


        logger.error("sqlCount-->{}", sqlCount);

        String whereSql = "";
        Integer pageSize = 5000;
        Integer totalDataRows = getTableRows(oldTable, whereCondition)/100;
        logger.error("旧表[{}]数据量为{}--->当前时间戳是:{}", oldTable, totalDataRows, System.currentTimeMillis());

        if (!whereCondition.trim().equals("") && whereCondition != null) {
            whereSql = String.format("where %s and id>? order by id asc", whereCondition);
        }

        String oldTableSqlString = String.format("select %s from %s %s limit ?,? "
                ,oldFields, oldTable, whereSql);
        List<String> oldTableResults;
        List<String> oldTableResultAll = new LinkedList<>();
        Long startId = 0L;
        while (true) {    // 根据id 一页取出 5000条
            oldTableResults = getTableResultsById(oldTableSqlString, startId, 1, pageSize);
            startId = Long.parseLong(oldTableResults.get(oldTableResults.size()-1).substring(0,1));   // 取出前面的id
            oldTableResultAll.addAll(oldTableResults);
            if (startId >= totalDataRows) {
                break;
            }
        }

        Integer dataTotal = oldTableResultAll.size();
        logger.error("旧表数据量为{}--->当前时间戳是:{}", dataTotal, System.currentTimeMillis());


        List<Runnable> myDataThreadList = new ArrayList<>();
        List<List<String>> oldTableResultsPartition = Lists.partition(oldTableResultAll, threadNum);

        for (int i=0; i<threadNum; i++) {

            List<String> partList = oldTableResultsPartition.get(i);
            logger.error("-----partList.size={}", partList.size());
            myDataThreadList.add(new CompareDataThread("线程-" + (i+1), oldTableSqlString,
                    partList, newTable, newFields, newTableRelationField, oldTable, oldFields));
        }


        threadPoolUtil.executeTasks(myDataThreadList);

    }

    class CompareDataThread implements Runnable{


        AtomicInteger passCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        String threadName;
        String querySql;
        List<String> oldTableResult;
        String newTable;
        String newFields;
        String newTableRelationField;
        String oldTable;
        String oldFields;


        public CompareDataThread(String threadName, String querySql, List<String> oldTableResult, String newTable, String newFields,
                                 String newTableRelationField, String oldTable,  String oldFields) {
            this.threadName = threadName;
            this.querySql = querySql;
            this.oldTableResult = oldTableResult;
            this.newTable = newTable;
            this.newFields = newFields;
            this.newTableRelationField = newTableRelationField;
            this.oldTable = oldTable;
            this.oldFields = oldFields;
        }

        @Override
        public void run() {
            Iterator<String> it = oldTableResult.iterator();
            while (it.hasNext()) {
                String oldTableValue = it.next();
                Boolean isPass = compareOldAndNewPass(newTable, newFields, newTableRelationField, oldTable, oldFields, oldTableValue);
                if (isPass) {
                    passCount.incrementAndGet();
                }else {
                    failCount.incrementAndGet();
                }
            }
            logger.error("线程-[{}]数据一致数量是：{}--数据不一致数量是：{}--->当前时间戳是:{}", threadName, passCount.get(), failCount.get(), System.currentTimeMillis());

        }
    }


    private List<String> getTableResultsById(String querySql, Long startId, Integer pageNo, Integer pageSize) {
        String sqlString = querySql;
//        logger.error("---旧表查询sql={}", sqlString);
        ResultSet rsContent = null;
        List<String> tableColumnValues = new LinkedList<>();
        List<String> tableColumnValuesAll = new LinkedList<>();
        try {

            SqlSession session = getSqlSession();
            Connection con = session.getConnection();
            if (!con.isClosed()) {
//                    System.out.println("Succeeded connecting to the Database!");
            }

            PreparedStatement psContent = con.prepareStatement(sqlString);

            psContent.setLong(1, startId);
            psContent.setInt(2, (pageNo - 1) * pageSize);
            psContent.setInt(3, pageSize);
//                psContent.setFetchSize(Integer.MIN_VALUE);
//                psContent.setFetchDirection(ResultSet.FETCH_REVERSE);

            rsContent = psContent.executeQuery();

            while (rsContent.next()) {
                tableColumnValues.add(getResultByType(rsContent));
            }
            tableColumnValuesAll.addAll(tableColumnValues);

            rsContent.close();
            con.close();
            session.close();
            tableColumnValues = null;
            rsContent = null;

        } catch (Exception e) {
            e.printStackTrace();
        }
//        logger.error("----tableColumnValuesAll={}", tableColumnValuesAll);
        return tableColumnValuesAll;
    }

    @Override
    public void checkData4(CheckTableInfo checkTableInfo) {
        String oldTable = checkTableInfo.getOldTable();
        String oldRealtionField = checkTableInfo.getOldTableRelationField();
        String oldFields = oldRealtionField + "," +checkTableInfo.getQueryOldTableFileds();    // id 在前
        String whereCondition = checkTableInfo.getQueryOldTableWhereCondition();

        String newTable = checkTableInfo.getNewTable();
        String newTableRelationField = checkTableInfo.getNewTableRelationField();
        String newFields = newTableRelationField + "," + checkTableInfo.getQueryNewTableFileds();


        String sqlCount = String.format("select count(1) as rowCount from %s where %s",  oldTable, whereCondition);
        String sqlContent = String.format("select %s from %s where %s", oldFields, oldTable, whereCondition);

        logger.error("sqlCount-->{}", sqlCount);
        logger.error("sqlContent-->{}", sqlContent);

        String whereSql = "";
        Integer pageSize = 10000;
        Integer totalDataRows = getTableRows(oldTable, whereCondition)/100;
        if (!whereCondition.trim().equals("") && whereCondition != null) {
            whereSql = String.format("where %s", whereCondition);
        }
//        String oldTableSqlString = String.format("select %s from % %s", oldFileds, oldTable, whereSql);

        String oldRefFields = StringUtil.getRefTableFields(oldFields);

        String oldTableSqlString = String.format("select %s from %s as a inner join (select id from %s %s limit ?,?) as b on a.id=b.id"
                ,oldRefFields, oldTable, oldTable, whereSql);

        List<Runnable> getOldTableThreadList = new ArrayList<>();
        List<Runnable> compareTablesThreadList = new ArrayList<>();


        class GetOldValueThread implements Runnable{
            String threadName;
            String querySql;
            Integer pageSize;
            Integer pageNoStart;
            Integer pageNoEnd;

            public GetOldValueThread(String threadName, String querySql, Integer pageNoStart, Integer pageNoEnd,
                                     Integer pageSize) {
                this.threadName = threadName;
                this.pageNoStart = pageNoStart;
                this.pageNoEnd = pageNoEnd;
                this.pageSize = pageSize;
                this.querySql = querySql;
            }
            @Override
            public void run() {
                synchronized (oldTableValuesAll) {
                    List<String> resultList;
                    List<String> resultListAll = new LinkedList<>();
                    for (int pageNo=pageNoStart; pageNo <= pageNoEnd; pageNo++) {
//                        try {
//                            while (oldTableValuesAll.size() == 10000) {
//                                this.wait();
//                            }
//                        }catch (Exception e){
//                            e.printStackTrace();
//                            logger.error("---旧表没有数据---");
////                            break;
//                        }

                        resultList = getTableResults(querySql, pageNo, pageSize);
                        oldTableValuesAll.addAll(resultList);
                        resultListAll.addAll(resultList);  // 仅仅是为了得到数量
                        oldTableValuesAll.notifyAll();
                    }
                    Integer dataTotal = resultListAll.size();
                    resultListAll = null; //gc
                    logger.error("线程-[{}]旧表数据量为{}--->当前时间戳是:{}", threadName, dataTotal, System.currentTimeMillis());
                }



            }

        }

        class OldTableVsNewTableThread implements Runnable {
            String threadName;
            //        String querySql;
            String newTable;
            String newFields;
            String newTableRelationField;
            String oldTable;
            String oldFields;


            public OldTableVsNewTableThread(String threadName,
//                                        String querySql,
                                            String newTable,
                                            String newFields,
                                            String newTableRelationField,
                                            String oldTable,
                                            String oldFields) {
                this.threadName = threadName;
//            this.querySql = querySql;
                this.newTable = newTable;
                this.newFields = newFields;
                this.newTableRelationField = newTableRelationField;
                this.oldTable = oldTable;
                this.oldFields = oldFields;
            }


            @Override
            public void run() {
                synchronized (oldTableValuesAll) {
    //                    try {
    //                        TimeUnit.MILLISECONDS.sleep(1000);
    //                    } catch (Exception e) {
    //                        e.printStackTrace();

                    AtomicInteger passCount = new AtomicInteger(0);
                    AtomicInteger failCount = new AtomicInteger(0);

                    logger.error("-----oldTableValuesAll={}", oldTableValuesAll.size());


                    Iterator<String> it = oldTableValuesAll.iterator();
                    int count = 0;
                    while(it.hasNext()) {
                        compareThreadNum.incrementAndGet();
                        while (compareThreadNum.get()%2000 == 0 ) {
                            try {
                                oldTableValuesAll.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
//                        count++;
//                        if (oldTableValuesAll.isEmpty()) {
//                            try {
//                                oldTableValuesAll.wait();
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        }
                        String oldTableValue = it.next();
                        it.remove();
//                    logger.error("---->oldTableValue={}", oldTableValue);
                        Boolean isPass = compareOldAndNewPass(newTable, newFields, newTableRelationField, oldTable, oldFields, oldTableValue);
                        if (isPass) {
                            passCount.incrementAndGet();
                        }else {
                            failCount.incrementAndGet();
                        }

                        oldTableValuesAll.notifyAll();

                    }



         /*           try {
                        while (oldTableValuesAll.size() == 10000) {
                            this.wait();
                        }

                        String oldTableValue = oldTableValuesAll.get(0);
                        oldTableValuesAll.remove(0);
                        Boolean isPass = compareOldAndNewPass(newTable, newFields, newTableRelationField, oldTable, oldFields, oldTableValue);
                        if (isPass) {
                            passCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }
                        this.notifyAll();

                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.error("----新表对比无数据----");
//                        break;
                    }
*/
                    logger.error("线程-[{}]数据一致数量是：{}--数据不一致数量是：{}--->当前时间戳是:{}", threadName, passCount.get(), failCount.get(), System.currentTimeMillis());

                }

            }
        }

        int pageCount = (int) Math.ceil(totalDataRows.doubleValue()/pageSize.doubleValue());   // 需要的总页数
        int pageCountPerThread = pageCount/threadNum == 0 ? 1: pageCount/threadNum;   // 每个线程的页数为

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
                if (i == threadNum) {    // 最后一个线程将多余数据进行查询
                    pageNoEnd = pageCount;
                }
                String threadName = "线程" + i;
                getOldTableThreadList.add(new GetOldValueThread("线程" + i, oldTableSqlString, pageNoStart, pageNoEnd, pageSize));
                logger.error("线程[{}]->pageNoStart={},pageNoEnd={},该线程需要使用页数={}", threadName, pageNoStart, pageNoEnd, pageCount);
                pageNoStart += pageCountPerThread;
                pageNoEnd = pageNoStart + pageCountPerThread-1;

            }
        }

        threadPoolUtil.executeTasks(getOldTableThreadList);
        logger.error("1---oldTableValuesAll.size={}", oldTableValuesAll.size());


        for (int j=1; j<=threadNum; j++){

            compareTablesThreadList.add(new OldTableVsNewTableThread("对比线程：" + j,  newTable, newFields, newTableRelationField, oldTable, oldFields));

        }

        logger.error("2---oldTableValuesAll.size={}", oldTableValuesAll.size());
        threadPoolUtil.executeCompareTask(compareTablesThreadList);


    }


    @Override
    public void checkData5(CheckTableInfo checkTableInfo){
        String oldTable = checkTableInfo.getOldTable();
        String oldRealtionField = checkTableInfo.getOldTableRelationField();
        String oldFields = oldRealtionField + "," +checkTableInfo.getQueryOldTableFileds();    // id 在前
        String whereCondition = checkTableInfo.getQueryOldTableWhereCondition();

        String newTable = checkTableInfo.getNewTable();
        String newTableRelationField = checkTableInfo.getNewTableRelationField();
        String newFields = newTableRelationField + "," + checkTableInfo.getQueryNewTableFileds();


        String sqlCount = String.format("select count(1) as rowCount from %s where %s",  oldTable, whereCondition);
        String sqlContent = String.format("select %s from %s where %s", oldFields, oldTable, whereCondition);

        logger.error("sqlCount-->{}", sqlCount);
        logger.error("sqlContent-->{}", sqlContent);

        String whereSql = "";
        Integer pageSize = 10000;
        Integer totalDataRows = getTableRows(oldTable, whereCondition);
        if (!whereCondition.trim().equals("") && whereCondition != null) {
            whereSql = String.format("where %s", whereCondition);
        }
//        String oldTableSqlString = String.format("select %s from % %s", oldFileds, oldTable, whereSql);

        String oldRefFields = StringUtil.getRefTableFields(oldFields);

        String oldTableSqlString = String.format("select %s from %s as a inner join (select id from %s %s limit ?,?) as b on a.id=b.id"
                ,oldRefFields, oldTable, oldTable, whereSql);

        List<Runnable> getOldTableThreadList = new ArrayList<>();
        List<Runnable> compareTablesThreadList = new ArrayList<>();

        int pageCount = (int) Math.ceil(totalDataRows.doubleValue()/pageSize.doubleValue());   // 需要的总页数
        int pageCountPerThread = pageCount/threadNum == 0 ? 1: pageCount/threadNum;   // 每个线程的页数为

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
                if (i == threadNum) {    // 最后一个线程将多余数据进行查询
                    pageNoEnd = pageCount;
                }
                String threadName = "线程" + i;
                getOldTableThreadList.add(new GetOldValueThread2("线程" + i, oldTableSqlString, pageNoStart, pageNoEnd, pageSize));
                logger.error("线程[{}]->pageNoStart={},pageNoEnd={},该线程需要使用页数={}", threadName, pageNoStart, pageNoEnd, pageCount);
                pageNoStart += pageCountPerThread;
                pageNoEnd = pageNoStart + pageCountPerThread-1;

            }
        }

        threadPoolUtil.executeTasks(getOldTableThreadList);
        logger.error("1---oldTableValuesAll.size={}", oldTableValuesAll.size());

        int partOneSize = (int) Math.ceil(totalDataRows.doubleValue()/ threadNum);
        List<List<String>> oldTableResultsPartition = Lists.partition(oldTableValuesAll, partOneSize);
//        logger.error("-----oldTableResultsPartition={}", oldTableResultsPartition);
        for (int i=0; i<threadNum; i++) {

            List<String> partList = oldTableResultsPartition.get(i);

//            logger.error("-----partList={}", partList);
            logger.error("-----partList.size={}", partList.size());
            compareTablesThreadList.add(new OldTableVsNewTableThread2("线程-" + (i+1), newTable, newFields, newTableRelationField, oldTable, oldFields, partList));
        }

        threadPoolUtil.executeTasks(compareTablesThreadList);

        logger.error("------总共----数据一致={}， 数据不一致={}", totalPassNum, totalFailNum);
    }


    class GetOldValueThread2 implements Runnable{
        String threadName;
        String querySql;
        Integer pageSize;
        Integer pageNoStart;
        Integer pageNoEnd;

        public GetOldValueThread2(String threadName, String querySql, Integer pageNoStart, Integer pageNoEnd,
                                 Integer pageSize) {
            this.threadName = threadName;
            this.pageNoStart = pageNoStart;
            this.pageNoEnd = pageNoEnd;
            this.pageSize = pageSize;
            this.querySql = querySql;
        }
        @Override
        public void run() {
            synchronized (oldTableValuesAll) {
                List<String> resultList;
                List<String> resultListAll = new LinkedList<>();
                for (int pageNo=pageNoStart; pageNo <= pageNoEnd; pageNo++) {
                    resultList = getTableResults(querySql, pageNo, pageSize);
                    oldTableValuesAll.addAll(resultList);
                    resultListAll.addAll(resultList);  // 仅仅是为了得到数量
                    oldTableValuesAll.notifyAll();
                }
                Integer dataTotal = resultListAll.size();
                resultListAll = null; //gc
                logger.error("线程-[{}]旧表数据量为{}--->当前时间戳是:{}", threadName, dataTotal, System.currentTimeMillis());
            }
        }

    }


    class OldTableVsNewTableThread2 implements Runnable {
        String threadName;
        String newTable;
        String newFields;
        String newTableRelationField;
        String oldTable;
        String oldFields;
        List<String> oldTableValues;


        public OldTableVsNewTableThread2(String threadName,
//                                        String querySql,
                                        String newTable,
                                        String newFields,
                                        String newTableRelationField,
                                        String oldTable,
                                        String oldFields,
                                        List<String> oldTableValues) {
            this.threadName = threadName;
            this.newTable = newTable;
            this.newFields = newFields;
            this.newTableRelationField = newTableRelationField;
            this.oldTable = oldTable;
            this.oldFields = oldFields;
            this.oldTableValues = oldTableValues;
        }


        @Override
        public void run() {
            AtomicInteger passCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            logger.error("---{} --oldTableValues={}", threadName, oldTableValues.size());

            Iterator<String> it = oldTableValues.iterator();
            while(it.hasNext()) {
                compareThreadNum.incrementAndGet();
                String oldTableValue = it.next();
                Boolean isPass = compareOldAndNewPass(newTable, newFields, newTableRelationField, oldTable, oldFields, oldTableValue);
                if (isPass) {
                    passCount.incrementAndGet();
                }else {
                    failCount.incrementAndGet();
                }
            }
            totalPassNum += passCount.get();
            totalFailNum += failCount.get();
            logger.error("线程-[{}]数据一致数量是：{}--数据不一致数量是：{}--->当前时间戳是:{}", threadName, passCount.get(), failCount.get(), System.currentTimeMillis());

        }
    }


    /**
     * @param querySql 查询sql
     * @param pageNo 查询的页数
     * @param pageSize 一次查询的数量
     */

    private List<String> getTableResults(String querySql, Integer pageNo, Integer pageSize) {
        String whereSql = "";
//        Integer totalDataRows = this.getTableRows(tableName, whereCondition);
//        Integer pageTotal = totalDataRows/pageSize + 1;
//        if (!whereCondition.trim().equals("") && whereCondition != null) {
//            whereSql = String.format(" where %s", whereCondition);
//        }
//        String sqlString = String.format("select %s from %s%s", tableFields, tableName, whereSql);
//        String sqlString = querySql + " limit ?,?";
        String sqlString = querySql;
//        logger.error("---旧表查询sql={}", sqlString);
        ResultSet rsContent = null;
        List<String> tableColumnValues = new LinkedList<>();
        List<String> tableColumnValuesAll = new LinkedList<>();
            try {

                SqlSession session = getSqlSession();
                Connection con = session.getConnection();
                if(!con.isClosed()){
//                    System.out.println("Succeeded connecting to the Database!");
                }

                PreparedStatement psContent = con.prepareStatement(sqlString);
                psContent.setInt(1, (pageNo-1)*pageSize);
                psContent.setInt(2, pageSize);
//                psContent.setFetchSize(Integer.MIN_VALUE);
//                psContent.setFetchDirection(ResultSet.FETCH_REVERSE);

                rsContent = psContent.executeQuery();

                while (rsContent.next()) {
                    tableColumnValues.add(getResultByType(rsContent));
                }
                tableColumnValuesAll.addAll(tableColumnValues);


                rsContent.close();
                con.close();
                session.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        return tableColumnValues;

    }


    private Integer getTableRows(String tableName, String whereCondition) {
        Integer tableRows = 0;
        String whereSql = "";
        ResultSet rsCount = null;
        if (!whereCondition.trim().equals("") && whereCondition != null) {
            whereSql = String.format(" where %s", whereCondition);
        }
        String sqlStr = String.format("select count(1) from %s%s", tableName, whereSql);
        try {

            SqlSession session = getSqlSession();
            Connection con = session.getConnection();
            if(!con.isClosed()){
//                System.out.println("Succeeded connecting to the Database!");
            }

            PreparedStatement psConut = con.prepareStatement(sqlStr);
            rsCount = psConut.executeQuery();

            if(rsCount.next()) {
                tableRows = rsCount.getInt(1);
            }

            rsCount.close();
            psConut.close();
            con.close();
            session.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return tableRows;

    }

    private List<String> queryResultByColumn(String sql) {
        List<String> resultList = new LinkedList<>();
        SqlSession session = getSqlSession();
        Connection con = session.getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            if(!con.isClosed()){
//                System.out.println("Succeeded connecting to the Database!");
            }
            ps = con.prepareStatement(sql);
//            ps.setFetchSize(Integer.MIN_VALUE);   // 流读取
//            ps.setFetchDirection(ResultSet.FETCH_REVERSE);
            rs = ps.executeQuery();
            while(rs.next()){
                String values = getResultByType(rs);
                resultList.add(values);
            }
//            rs.getStatement().close();
            rs.close();
            ps.close();
            con.close();
            session.close();
            return resultList;
        } catch (Exception e) {
            e.printStackTrace();
        }
        resultList.add("查询无结果");
        return resultList;
    }


    private Boolean compareOldAndNewPass(String newTableName, String queryNewTableFields, String newTableRelationField, String oldTableName, String queyOldTableFileds, String oldTableValue) {
        Boolean isPass = false;
//        logger.error("----->{}", oldTableValue);
        String stringWhere = oldTableValue.substring(0, oldTableValue.indexOf(","));    // 第1个值为老表主键id
        String newQueryString = String.format("select %s from %s where %s=%s", queryNewTableFields, newTableName, newTableRelationField, stringWhere);
//        logger.error("---查询语句是：{}", newQueryString);
        List<String> newTableResult = this.queryResultByColumn(newQueryString);
//      logger.error("---查询结果是：{}", JSON.toJSONString(newTableResult));
        if (newTableResult.size()>0 && newTableResult.get(0).equals(oldTableValue)) {  // 现在查询只能1v1
//            logger.info("新旧表数据一致");
//            this.passNum.incrementAndGet();
           isPass = true;
        }else {
//            logger.info("新旧表数据不一致");
//            this.failNum.incrementAndGet();
            isPass = false;
            logger.info("-数据不一致--->新表{}[{}]结果：{}-->旧表{}[{}]结果：{}", newTableName, queryNewTableFields, JSON.toJSONString(newTableResult.get(0)), oldTableName, queyOldTableFileds, oldTableValue);
        }
        return isPass;
    }

    @Override
    public void checkData(CheckTableInfo checkTableInfo) throws Exception{
        String newTableName = checkTableInfo.getNewTable();
        String oldTableName = checkTableInfo.getOldTable();
        String newTableRelationField = checkTableInfo.getNewTableRelationField();
        String queryNewTableFields = newTableRelationField + "," + checkTableInfo.getQueryNewTableFileds() ;

        TableDto oldTableDto = this.getOldTableDetail(checkTableInfo);
        String queyOldTableFileds = checkTableInfo.getOldTableRelationField() + "," + checkTableInfo.getQueryOldTableFileds();

        List<String> oldTableValues = oldTableDto.getTableColumnsValues();
        logger.error("老表数据大小为：{}", oldTableValues.size());

        class MyThread implements Runnable{
            CompareDataCountResult data;
            List<String> dataList;
            String threadName;

            public MyThread(String threadName, List<String> dataList, CompareDataCountResult data) {
                this.data = data;
                this.dataList = dataList;
                this.threadName = threadName;
            }
            @Override
            public void run() {
                AtomicInteger passCount = new AtomicInteger(0);
                AtomicInteger failCount = new AtomicInteger(0);
//                logger.info("线程-[{}]最后结果是：{}", threadName, JSON.toJSONString(dataList));

//                    for (String oldTableValue: threadList1){

                Iterator<String> it = dataList.iterator();
//
                while(it.hasNext()) {
                    String oldTableValue = it.next();
                    Boolean isPass = compareOldAndNewPass(newTableName, queryNewTableFields, newTableRelationField, oldTableName, queyOldTableFileds,  oldTableValue);
                    if (isPass) {
                        passCount.incrementAndGet();
                    }else {
                        failCount.incrementAndGet();
                    }
                }
                data.setPassCount(passCount.get());
                data.setFailCount(failCount.get());
                logger.error("线程-[{}]数据一致数量是：{}--数据不一致数量是：{}--->当前时间戳是:{}", threadName, passCount.get(), failCount.get(), System.currentTimeMillis());
            }

        }

        if(oldTableValues != null && oldTableValues.size() > 0) {
            List<String> threadList1 = new ArrayList<>();
            List<String> threadList2 = new ArrayList<>();
            List<String> threadList3 = new ArrayList<>();
            List<String> threadList4 = new ArrayList<>();
            List<String> threadList5 = new ArrayList<>();
            List<String> threadList6 = new ArrayList<>();
            List<String> threadList7 = new ArrayList<>();
            List<String> threadList8 = new ArrayList<>();
            List<String> threadList9 = new ArrayList<>();
            List<String> threadList10 = new ArrayList<>();

            if (oldTableValues.size() <= 10) {
                threadList1.addAll(oldTableValues);
            } else {
                int stepLen = oldTableValues.size() / 10;
                logger.error("stepLen={}", stepLen);
                int i = 1;
                for (String oldTableValue : oldTableValues) {
                    if (i <= stepLen) {
                        threadList1.add(oldTableValue);
                    } else if (i > stepLen && i <= stepLen * 2) {
                        threadList2.add(oldTableValue);
                    } else if (stepLen * 2> stepLen && i <= stepLen * 3) {
                        threadList3.add(oldTableValue);
                    } else if(stepLen * 3> stepLen && i <= stepLen * 4) {
                        threadList4.add(oldTableValue);
                    } else if (stepLen * 4> stepLen && i <= stepLen * 5) {
                        threadList5.add(oldTableValue);
                    } else if (stepLen * 5> stepLen && i <= stepLen * 6) {
                        threadList6.add(oldTableValue);
                    } else if (stepLen * 6> stepLen && i <= stepLen * 7) {
                        threadList7.add(oldTableValue);
                    } else if (stepLen * 7> stepLen && i <= stepLen * 8) {
                        threadList8.add(oldTableValue);
                    } else if (stepLen * 8> stepLen && i <= stepLen * 9) {
                        threadList9.add(oldTableValue);
                    } else {
                        threadList10.add(oldTableValue);
                    }
                    i += 1;
                }
            }

            logger.error("初始化10个线程完毕！--trSize1={},trSize2={},trSize3={},trSize4={},trSize5={},trSize6={},trSize7={},trSize8={},trSize9={},trSize10={}",
                    threadList1.size(), threadList2.size(), threadList3.size(),threadList4.size(), threadList5.size(),
                    threadList6.size(),threadList7.size(), threadList8.size(), threadList9.size(), threadList10.size());

            CompareDataCountResult data1 = new CompareDataCountResult();
            CompareDataCountResult data2 = new CompareDataCountResult();
            CompareDataCountResult data3 = new CompareDataCountResult();
            CompareDataCountResult data4 = new CompareDataCountResult();
            CompareDataCountResult data5 = new CompareDataCountResult();
            CompareDataCountResult data6 = new CompareDataCountResult();
            CompareDataCountResult data7 = new CompareDataCountResult();
            CompareDataCountResult data8 = new CompareDataCountResult();
            CompareDataCountResult data9 = new CompareDataCountResult();
            CompareDataCountResult data10 = new CompareDataCountResult();

            MyThread myThread1 = new MyThread("线程1", threadList1, data1);
            MyThread myThread2 = new MyThread("线程2", threadList2, data2);

            MyThread myThread3 = new MyThread("线程3", threadList3, data3);

            MyThread myThread4 = new MyThread("线程4", threadList4, data4);

            MyThread myThread5 = new MyThread("线程5", threadList5, data5);
            MyThread myThread6 = new MyThread("线程6", threadList6, data6);

            MyThread myThread7 = new MyThread("线程7", threadList7, data7);

            MyThread myThread8 = new MyThread("线程8", threadList8, data8);

            MyThread myThread9 = new MyThread("线程9", threadList9, data9);
            MyThread myThread10 = new MyThread("线程10", threadList10, data10);


/*

            threadPoolUtil.executeTask(myThread1);
            threadPoolUtil.executeTask(myThread2);
            threadPoolUtil.executeTask(myThread3);
            threadPoolUtil.executeTask(myThread4);
            threadPoolUtil.executeTask(myThread5);
            threadPoolUtil.executeTask(myThread6);
            threadPoolUtil.executeTask(myThread7);
            threadPoolUtil.executeTask(myThread8);
            threadPoolUtil.executeTask(myThread9);
            threadPoolUtil.executeTask(myThread10);
*/
        List<Runnable> listMyThread = new ArrayList<>();
        listMyThread.add(myThread1);
        listMyThread.add(myThread2);
        listMyThread.add(myThread3);
        listMyThread.add(myThread4);
        listMyThread.add(myThread5);
        listMyThread.add(myThread6);
        listMyThread.add(myThread7);
        listMyThread.add(myThread8);
        listMyThread.add(myThread9);
        listMyThread.add(myThread10);

        threadPoolUtil.executeTasks(listMyThread);






           /* class Thread1 implements Runnable{
                CompareDataCountResult data;

                public Thread1(CompareDataCountResult data) {
                    this.data = data;
                }
                @Override
                public void run() {
                    Integer passCount = 0;
                    Integer failCount = 0;
                    logger.info("[线程1]最后结果是：{}", JSON.toJSONString(threadList1));

//                    for (String oldTableValue: threadList1){

                    Iterator<String> it = threadList1.iterator();
//
                    while(it.hasNext()) {
                        String oldTableValue = it.next();
                        Boolean isPass = compareOldAndNewPass(newTableName, queryNewTableFields, newTableRelationField, oldTableName, queyOldTableFileds,  oldTableValue);
                        if (isPass) {
                            passCount += 1;
                        }else {
                            failCount += 1;
                        }
                    }
                    data.setPassCount(passCount);
                    data.setFailCount(failCount);
                    System.out.println("线程1-->数据一致数量是：" + passCount + "--数据不一致数量是：" + failCount);
                }

            }

            class Thread2 implements Runnable{
                CompareDataCountResult data;

                public Thread2(CompareDataCountResult data) {
                    this.data = data;
                }
                @Override
                public void run() {
                    Integer passCount = 0;
                    Integer failCount = 0;
                    logger.info("[线程2]最后结果是：{}", JSON.toJSONString(threadList2));

//                    for (String oldTableValue: threadList1){

                    Iterator<String> it = threadList1.iterator();
//
                    while(it.hasNext()) {
                        String oldTableValue = it.next();
                        Boolean isPass = compareOldAndNewPass(newTableName, queryNewTableFields, newTableRelationField, oldTableName, queyOldTableFileds,  oldTableValue);
                        if (isPass) {
                            passCount += 1;
                        }else {
                            failCount += 1;
                        }
                    }
                    data.setPassCount(passCount);
                    data.setFailCount(failCount);

                    System.out.println("线程2-->数据一致数量是：" + passCount + "--数据不一致数量是：" + failCount);

                }

            }


            class Thread3 implements Runnable{
                CompareDataCountResult data;

                public Thread3(CompareDataCountResult data) {
                    this.data = data;
                }
                @Override
                public void run() {
                    Integer passCount = 0;
                    Integer failCount = 0;
                    logger.info("[线程3]最后结果是：{}", JSON.toJSONString(threadList3));

//                    for (String oldTableValue: threadList1){

                    Iterator<String> it = threadList1.iterator();
//
                    while(it.hasNext()) {
                        String oldTableValue = it.next();
                        Boolean isPass = compareOldAndNewPass(newTableName, queryNewTableFields, newTableRelationField, oldTableName, queyOldTableFileds,  oldTableValue);
                        if (isPass) {
                            passCount += 1;
                        }else {
                            failCount += 1;
                        }
                    }
                    data.setPassCount(passCount);
                    data.setFailCount(failCount);

                    System.out.println("线程3-->数据一致数量是：" + passCount + "--数据不一致数量是：" + failCount);

                }
            }

            logger.error("初始化3个线程完毕！--trSize1={},trSize2={},trSize3={}", threadList1.size(), threadList2.size(), threadList3.size());

            CompareDataCountResult data1 = new CompareDataCountResult();
            threadPoolUtil.executeTask(new Thread1(data1));

            CompareDataCountResult data2 = new CompareDataCountResult();
            threadPoolUtil.executeTask(new Thread2(data2));

            CompareDataCountResult data3 = new CompareDataCountResult();
            threadPoolUtil.executeTask(new Thread3(data3));*/

           /* if (threadList1.size() > 0) {
                Runnable runnable = () -> {
                    CompareDataCountResult data = new CompareDataCountResult();
                    Integer passCount = 0;
                    Integer failCount = 0;
                    logger.info("[线程1]最后结果是：{}", JSON.toJSONString(threadList1));

//                    for (String oldTableValue: threadList1){

                    Iterator<String> it = threadList1.iterator();
//
                    while(it.hasNext()) {
                        String oldTableValue = it.next();
                        Boolean isPass = this.compareOldAndNewPass(newTableName, queryNewTableFields, newTableRelationField, oldTableName, queyOldTableFileds,  oldTableValue);
                        if (isPass) {
                            passCount += 1;
                        }else {
                            failCount += 1;
                        }
                    }
                    data.setPassCount(passCount);
                    data.setFailCount(failCount);
                };
                threadPoolUtil.executeTask(runnable, data);
            }

            if (threadList2.size() > 0) {
                Runnable runnable = () -> {
                    logger.info("[线程2]最后结果是：");

                    Iterator<String> it = threadList2.iterator();
                    while (it.hasNext()) {
                        String oldTableValue = it.next();
                        this.compareOldAndNew(newTableName, queryNewTableFields, newTableRelationField, oldTableName, queyOldTableFileds,  oldTableValue);
                    }
                };
                threadPoolUtil.executeTask(runnable);
            }


            if (threadList3.size() > 0) {
                Runnable runnable = () -> {
                    logger.info("[线程3]最后结果是：");

                    Iterator<String> it = threadList3.iterator();
                    while(it.hasNext()) {
                        String oldTableValue = it.next();
                        this.compareOldAndNew(newTableName, queryNewTableFields, newTableRelationField, oldTableName, queyOldTableFileds,  oldTableValue);
                    }
                };
                threadPoolUtil.executeTask(runnable);
            }*/
        }

        logger.error("旧表数据数量={},新表和旧表数据不一致数量为={}, 新表和旧表数据一致数量为={}", this.oldTableCounts, this.failNum, this.passNum);
    }


    private String getResultByType(ResultSet rs) {

        ResultSetMetaData resultMetaData = null;
        StringBuilder queryContent = new StringBuilder();
        try {
            resultMetaData = rs.getMetaData();

            int cols = resultMetaData.getColumnCount();
//            logger.error("resultMetaData={}, count={}", JSON.toJSONString(resultMetaData), cols);
            for (int i=1; i<=cols; i++) {
                String columnName = resultMetaData.getColumnName(i);
                switch (resultMetaData.getColumnType(i)) {
                    case Types.VARCHAR:
                        queryContent.append(","+ rs.getString(columnName));
                        break;
                    case Types.INTEGER:
                        queryContent.append("," + rs.getInt(columnName));
                        break;
                    case Types.TIMESTAMP:
                        queryContent.append("," + rs.getDate(columnName));
                        break;
                    case Types.DOUBLE:
                        queryContent.append("," + rs.getDouble(columnName));
                        break;
                    case Types.FLOAT:
                        queryContent.append("," + rs.getFloat(columnName));
                        break;
                    case Types.CLOB:
                        queryContent.append("," + rs.getBlob(columnName));
                        break;
                    case Types.BIGINT:
                        queryContent.append("," + rs.getLong(columnName));
                        break;
                    case Types.DECIMAL:
                        queryContent.append("," + rs.getBigDecimal(columnName));
                        break;

                    default:
                        queryContent.append(",error");
                }
            }
            return queryContent.toString().substring(1);   // 去掉,号
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "";

    }


}
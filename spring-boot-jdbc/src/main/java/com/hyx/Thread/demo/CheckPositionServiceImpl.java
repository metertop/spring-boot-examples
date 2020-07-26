package com.hyx.Thread.demo;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
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

import javax.websocket.Session;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Description:
 * @Author: haoyuexun
 * @Date: 2020/7/24 14:49
 */
@Service("CheckPositionService")
public class CheckPositionServiceImpl implements CheckPositionService {

    Logger  logger = LoggerFactory.getLogger(CheckPositionServiceImpl.class);

    @Autowired
    private ThreadPoolUtil threadPoolUtil;

    private static DruidDataSource dataSourceMDB = null;
    private static SqlSessionFactory sqlSessionFactory = null;

    AtomicInteger passNum = new AtomicInteger(0);
    AtomicInteger failNum = new AtomicInteger(0);
    Integer oldTableCounts = 0;


    //声明Connection对象
    Connection con;
    //驱动程序名
    String driver = "com.mysql.jdbc.Driver";
    //URL指向要访问的数据库名mydata
    String url = "jdbc:mysql://172.16.70.20:3306/point?useUnicode=true&amp;characterEncoding=utf8&amp;connectTimeout=5000&amp;socketTimeout=60000&amp;autoReconnect=true&amp;failOverReadOnly=false&amp;allowMultiQueries=true";
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
                System.out.println("Succeeded connecting to the Database!");
            }
            PreparedStatement psCount = con.prepareStatement(sqlCount);
            PreparedStatement psConcent = con.prepareStatement(sqlContent);
            rsCount = psCount.executeQuery();
            rsContent = psConcent.executeQuery();

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
            logger.error("老表查询数据是：{}", JSON.toJSONString(tableColumnValues));
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


    private List<String> queryResultByColumn(String sql) {
        List<String> resultList = new ArrayList<>();
        SqlSession session = getSqlSession();
        Connection con = session.getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            if(!con.isClosed()){
//                System.out.println("Succeeded connecting to the Database!");
            }
            ps = con.prepareStatement(sql);
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
        String stringWhere = oldTableValue.substring(0,1);
        String newQueryString = String.format("select %s from %s where %s=%s", queryNewTableFields, newTableName, newTableRelationField, stringWhere);
//        logger.error("---查询语句是：{}", newQueryString);
        List<String> newTableResult = this.queryResultByColumn(newQueryString);
    //  logger.error("---查询结果是：{}", JSON.toJSONString(newTableResult));
        if (newTableResult.size()>0 && newTableResult.get(0).equals(oldTableValue)) {  // 现在查询只能1v1
//            logger.info("新旧表数据一致");
//            this.passNum.incrementAndGet();
           isPass = true;
        }else {
//            logger.info("新旧表数据不一致");
//            this.failNum.incrementAndGet();
            isPass = false;
        }
//        logger.info("新表{}[{}]结果：{}-->旧表{}[{}]结果：{}", newTableName, queryNewTableFields, JSON.toJSONString(newTableResult), oldTableName, queyOldTableFileds, oldTableValue);
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



        if(oldTableValues != null && oldTableValues.size() > 0) {
            List<String> threadList1 = new ArrayList<>();
            List<String> threadList2 = new ArrayList<>();
            List<String> threadList3 = new ArrayList<>();
            if (oldTableValues.size() <= 3) {
                threadList1.addAll(oldTableValues);
            } else {
                int stepLen = oldTableValues.size() / 3;
                logger.error("stepLen={}", stepLen);
                int i = 1;
                for (String oldTableValue : oldTableValues) {
                    if (i <= stepLen) {
                        threadList1.add(oldTableValue);
                    } else if (i > stepLen && i <= stepLen * 2) {
                        threadList2.add(oldTableValue);
                    } else {
                        threadList3.add(oldTableValue);
                    }
                    i += 1;
                }
            }

            class Thread1 implements Runnable{
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
            threadPoolUtil.executeTask(new Thread3(data3));

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
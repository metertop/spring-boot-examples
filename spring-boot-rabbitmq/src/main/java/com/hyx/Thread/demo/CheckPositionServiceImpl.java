package com.hyx.Thread.demo;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSONArray;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;

/**
 * @Description:
 * @Author: haoyuexun
 * @Date: 2020/7/24 14:49
 */
public class CheckPositionServiceImpl implements CheckPositionService {

    @Autowired
    private ThreadPoolUtil threadPoolUtil;

    private static DruidDataSource dataSourceMDB = null;
    private static SqlSessionFactory sqlSessionFactory = null;

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
        Long oldCounts;
        TableDto oldTableDto = new TableDto();

        String sqlCount = String.format("select count(1) as rowCount from %s where %s",  oldTable, oldQueryCondition);
        String sqlContent = String.format("select %s from %s where %s", oldFileds, oldTable, oldQueryCondition);
        ResultSet rsCount = null;
        ResultSet rsContent = null;
        LinkedList<String> tableColumnValues = new LinkedList<>();
        try {

            SqlSession session = getSqlSession();
            con = session.getConnection();
            if(!con.isClosed()){
                System.out.println("Succeeded connecting to the Database!");
            }
            PreparedStatement psCount = con.prepareStatement(sqlCount);
            PreparedStatement psConcent = con.prepareStatement(sqlContent);
            rsCount = psCount.executeQuery();
            rsContent = psConcent.executeQuery();

            oldTableDto.setTableName(oldTable);
            oldTableDto.setTableColumns(oldFileds);
            oldCounts = rsCount.getLong("rowCount");
            oldTableDto.setTableLength(oldCounts);
            while(rsContent.next()){
                tableColumnValues.add(getResultByType(rsContent));
            }
            rsCount.close();
            rsContent.close();
            con.close();
            session.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return oldTableDto;
    }


    private String queryResultByColumn(String sql) {
        SqlSession session = getSqlSession();
        StringBuilder sb = new StringBuilder();
        String[] array= new String[];
        con = session.getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            if(!con.isClosed()){
                System.out.println("Succeeded connecting to the Database!");
            }
            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();
            while(rs.next()){
                String values = getResultByType(rs);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }





        return null;
    }

    @Override
    public void checkData(CheckTableInfo checkTableInfo) {
        String newTableName = checkTableInfo.getNewTable();
        String newTableRelationField = checkTableInfo.getNewTableRelationField();
        String queryNewTableFields = newTableRelationField + "," + checkTableInfo.getQueryNewTableFileds() ;

        TableDto oldTableDto = this.getOldTableDetail(checkTableInfo);

        LinkedList<String> oldTableValues = oldTableDto.getTableColumnsValues();

        if(oldTableValues != null && oldTableValues.size() > 0) {
            List<String> threadList1 = new LinkedList<>();
            List<String> threadList2 = new LinkedList<>();
            List<String> threadList3 = new LinkedList<>();
            if (oldTableValues.size() <= 3) {
                threadList1.addAll(oldTableValues);
            } else {
                int stepLen = oldTableValues.size() / 3;
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

            if (threadList1.size() > 0) {
                Runnable runnable = () -> {
                    System.out.println("[线程1]最后结果是：");

                    for(int i=0; i<threadList1.size(); i++){
                        String stringWhere = threadList1.get(i).substring(0,1);
                        String newQueryString = String.format("select %s from %s where %s=%s", queryNewTableFields, newTableName, newTableRelationField, stringWhere);

                    }


//

                };
                threadPoolUtil.executeTask(runnable);
            }
        }

        for (int i=0; i<oldValues.size(); i++) {
            String oldTableValie = oldValues.get(i);
        }

//        String newTableSql = String.format("select %s from %s where %=%s", queryNewTableFileds, newTableName, newTableRelationField, ;
//,      )


    }

    private String getResultByType(ResultSet rs) {

        ResultSetMetaData resultMetaData = null;
        StringBuilder queryContent = new StringBuilder();
        try {
            resultMetaData = rs.getMetaData();
            int cols = resultMetaData.getColumnCount();
            for (int i=0; i<cols; i++) {
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
                    default:
                        queryContent.append(",error");
                }
            }
            return queryContent.toString().substring(1);   // 去掉,
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "";

    }


}
package com.hyx.Thread.demo;

import java.io.Serializable;
import java.util.List;



/**
 * @Description:
 * @Author: haoyuexun
 * @Date: 2020/7/24 14:11
 */
public class TableDto implements Serializable {


    private static final long serialVersionUID = 8179076974725793611L;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Integer getTableLength() {
        return tableLength;
    }

    public void setTableLength(Integer tableLength) {
        this.tableLength = tableLength;
    }

    public String getTableColumns() {
        return tableColumns;
    }

    public void setTableColumns(String tableColumns) {
        this.tableColumns = tableColumns;
    }

    public List<String> getTableColumnsValues() {
        return tableColumnsValues;
    }

    public void setTableColumnsValues(List<String> tableColumnsValues) {
        this.tableColumnsValues = tableColumnsValues;
    }

    private String tableName;
    private Integer tableLength;
    private String tableColumns;
    private List<String> tableColumnsValues;

}

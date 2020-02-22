package cn.cl.detect.config;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory;

/**
 * Mybatis数据源
 */
public class DBCPDataSourceFactory extends UnpooledDataSourceFactory {

    public DBCPDataSourceFactory() {
        this.dataSource = new BasicDataSource();
    }
    
}
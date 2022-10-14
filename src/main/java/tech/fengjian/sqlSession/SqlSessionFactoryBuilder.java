package tech.fengjian.sqlSession;

import org.dom4j.DocumentException;
import tech.fengjian.config.XMLConfigBuilder;
import tech.fengjian.pojo.Configuration;

import java.beans.PropertyVetoException;
import java.io.InputStream;

public class SqlSessionFactoryBuilder {

    public SqlSessionFactory build(InputStream in) throws PropertyVetoException, DocumentException {

        // 第一：使用 dom4j 解析配置文件，将解析出来的内容封装到 Configuration中
        XMLConfigBuilder xmlConfigBuilder = new XMLConfigBuilder();
        Configuration configuration = xmlConfigBuilder.parseConfig(in);

        // 第二：创建 SqlSessionFactory 对象：工厂类：生产 SqlSession：会话对象
        SqlSessionFactory sqlSessionFactory = new DefaultSqlSessionFactory(configuration);

        return sqlSessionFactory;
    }
}

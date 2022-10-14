package tech.fengjian.sqlSession;

import tech.fengjian.pojo.BoundSql;
import tech.fengjian.pojo.Configuration;
import tech.fengjian.pojo.MappedStatement;
import tech.fengjian.utils.GenericTokenParser;
import tech.fengjian.utils.ParameterMapping;
import tech.fengjian.utils.ParameterMappingTokenHandler;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SimpleExecutor implements Executor {

    @Override
    public <E> List<E> query(Configuration configuration, MappedStatement mappedStatement, Object... params) throws SQLException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException, IntrospectionException, InvocationTargetException, InstantiationException {

        // 1.注册驱动，获取连接
        Connection connection = configuration.getDataSource().getConnection();

        // 2. 获取 sql 语句：select * from user where id=#{id} and username=#{username}
        //    转换 sql 语句：select * from user where id=? and username=?，转换过程中还需要对#{}里面的值进行解析存储
        String sql = mappedStatement.getSql();
        BoundSql boundSql = getBoundSql(sql);

        // 3. 获取预处理对象：PreparedStatement
        PreparedStatement preparedStatement = connection.prepareStatement(boundSql.getSqlText());

        // 4. 设置参数
        // 获取到参数全路径
        String parameterType = mappedStatement.getParameterType();
        Class<?> parameterClass = getClassType(parameterType);
        List<ParameterMapping> parameterMappingList = boundSql.getParameterMappingList();
        for (int i = 0; i < parameterMappingList.size(); i++) {
            ParameterMapping parameterMapping = parameterMappingList.get(i);
            String content = parameterMapping.getContent();

            // 反射
            Field declaredField = parameterClass.getDeclaredField(content);
            // 暴力访问
            declaredField.setAccessible(true);
            Object o = declaredField.get(params[0]);

            preparedStatement.setObject(i + 1, o);
        }

        // 5. 执行 sql
        ResultSet resultSet = preparedStatement.executeQuery();

        // 6. 封装返回结果集
        List<Object> objects = new ArrayList<>();
        String resultType = mappedStatement.getResultType();
        Class<?> resultClass = getClassType(resultType);
        while (resultSet.next()) {
            Object o = resultClass.newInstance();
            // 元数据
            ResultSetMetaData metaData = resultSet.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                // 字段名
                String columnName = metaData.getColumnName(i);
                // 字段的值
                Object value = resultSet.getObject(columnName);
                // 使用反射或内省根据数据库表和实体的对应关系，完成封装
                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(columnName, resultClass);
                Method writeMethod = propertyDescriptor.getWriteMethod();
                writeMethod.invoke(o, value);

            }
            objects.add(o);
        }
        return (List<E>) objects;
    }

    private Class<?> getClassType(String parameterType) throws ClassNotFoundException {
        if (parameterType != null) {
            return Class.forName(parameterType);
        }
        return null;
    }

    /**
     * 完成对 #{} 的解析工作：
     * 1. 将 #{} 用 ? 代替
     * 2. 将 #{} 里面的值进行存储
     */
    private BoundSql getBoundSql(String sql) {

        // 标记处理类：配合标记解析器来完成对占位符的解析处理工作
        ParameterMappingTokenHandler tokenHandler = new ParameterMappingTokenHandler();
        GenericTokenParser tokenParser = new GenericTokenParser("#{", "}", tokenHandler);
        // 解析出来的 sql
        String parseSql = tokenParser.parse(sql);
        // #{}解析出来的参数名称
        List<ParameterMapping> parameterMappings = tokenHandler.getParameterMappings();

        return new BoundSql(parseSql, parameterMappings);
    }
}

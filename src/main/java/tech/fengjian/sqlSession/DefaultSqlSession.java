package tech.fengjian.sqlSession;

import tech.fengjian.pojo.Configuration;

import java.beans.IntrospectionException;
import java.lang.reflect.*;
import java.sql.SQLException;
import java.util.List;

public class DefaultSqlSession implements SqlSession {

    private Configuration configuration;

    public DefaultSqlSession(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public <E> List<E> selectList(String statementId, Object... params) throws SQLException, IntrospectionException, NoSuchFieldException, ClassNotFoundException, InvocationTargetException, IllegalAccessException, InstantiationException {

        // 将要去完成对 SimpleExecutor 里的 query 方法的调用
        SimpleExecutor simpleExecutor = new SimpleExecutor();
        return simpleExecutor.query(configuration, configuration.getMappedStatementMap().get(statementId), params);
    }

    @Override
    public <T> T selectOne(String statementId, Object... params) throws SQLException, IntrospectionException, NoSuchFieldException, ClassNotFoundException, InvocationTargetException, IllegalAccessException, InstantiationException {
        List<Object> objects = selectList(statementId, params);
        if (objects.size() == 1) {
            return (T) objects.get(0);
        } else {
            throw new RuntimeException("查询结果为空或返回结果过多");
        }
    }

    @Override
    public <T> T getMapper(Class<?> mapperClass) {
        // 使用 jdk 动态代理为 Dao 接口生成代理对象并返回，使用代理对象调用接口中的任意方法，都是执行 invoke 方法
        Object proxyInstance = Proxy.newProxyInstance(DefaultSqlSession.class.getClassLoader(), new Class[]{mapperClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // 底层都是执行 jdbc 代码 // 根据不同情况调用 selectOne 或 selectList 方法
                // 准备参数 1: statementId：sql 语句唯一标识：namespace.id
                // 约定：namespace 为接口全路径，id 为方法名
                String methodName = method.getName();
                String className = method.getDeclaringClass().getName();
                String statementId = className + "." + methodName;

                // 准备参数 2： params ： args
                // 根据方法返回值判断是调用 selectList 还是 selectOne，selectList 返回值包含泛型化符号：<>
                // 获取被调用方法返回值类型
                Type genericReturnType = method.getGenericReturnType();
                // 判断是否进行了泛型类型参数化
                if (genericReturnType instanceof ParameterizedType) {
                    List<Object> objects = selectList(statementId, args);
                    return objects;
                }
                return selectOne(statementId, args);
            }
        });


        return (T) proxyInstance;
    }
}

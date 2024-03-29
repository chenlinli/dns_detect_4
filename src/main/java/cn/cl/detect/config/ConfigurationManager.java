package cn.cl.detect.config;

import java.io.InputStream;
import java.util.Properties;

/**
 *  * 配置管理组件
 *
 * 1、配置管理组件可以复杂，也可以很简单，对于简单的配置管理组件来说，
 *  只要开发一个类，可以在第一次访问它的时候，就从对应的properties文件中，
 *  读取配置项，并提供外界获取某个配置key对应的value的方法
 * 2、如果是特别复杂的配置管理组件，那么可能需要使用一些软件设计中的设计模式
 * ,比如单例模式、解释器模,可能需要管理多个不同的properties，甚至是xml类型的配置文件
 * 我们这里的话，就是开发一个简单的配置管理组件，就可以了
 */
public class ConfigurationManager {
    /**
     *  Properties对象使用private来修饰，就代表了其是类私有的
     *	那么外界的代码，就不能直接通过ConfigurationManager.prop这种方式获取到Properties对象
     *	之所以这么做，是为了避免外界的代码不小心错误的更新了Properties中某个key对应的value
     *	从而导致整个程序的状态错误，乃至崩溃
     */
    private static Properties prop = new Properties();

    static {
        try{
            InputStream in = ConfigurationManager.class.getClassLoader().getResourceAsStream("my.properties");
            // 调用Properties的load()方法，给它传入一个文件的InputStream输入流
            // 即可将文件中的符合“key=value”格式的配置项，都加载到Properties对象中
            // 加载过后，此时，Properties对象中就有了配置文件中所有的key-value对了
            // 然后外界其实就可以通过Properties对象获取指定key对应的value
            prop.load(in);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    /**
     * 获取指定key对应的value
     *
     * 第一次外界代码，调用ConfigurationManager类的getProperty静态方法时，JVM内部会发现
     * ConfigurationManager类还不在JVM的内存中
     *
     * 此时JVM，就会使用自己的ClassLoader（类加载器），去对应的类所在的磁盘文件（.class文件）中
     * 去加载ConfigurationManager类，到JVM内存中来，并根据类内部的信息，去创建一个Class对象
     * Class对象中，就包含了类的元信息，包括类有哪些field（Properties prop）；有哪些方法（getProperty）
     *
     * 加载ConfigurationManager类的时候，还会初始化这个类，那么此时就执行类的static静态代码块
     * 此时咱们自己编写的静态代码块中的代码，就会加载my.properites文件的内容，到Properties对象中来
     *
     * 下一次外界代码，再调用ConfigurationManager的getProperty()方法时，就不会再次加载类，不会再次初始化
     * 类，和执行静态代码块了，所以也印证了，我们上面所说的，类只会加载一次，配置文件也仅仅会加载一次
     *
     * @param key
     * @return value
     */
    public static String getProperty(String key){
        return prop.getProperty(key);
    }

    public static Integer getInteger(String key){
        String value = prop.getProperty(key);
        try{
            return Integer.valueOf(value);
        }catch (Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    public static Boolean getBoolean(String key) {
        String value = prop.getProperty(key);
        try{
            return Boolean.valueOf(value);
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
    /**
     * 获取Long类型的配置项
     * @param key
     * @return
     */
    public static Long getLong(String key) {
        String value = getProperty(key);
        try {
            return Long.valueOf(value);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0L;
    }
}

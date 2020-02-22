package cn.cl.detect.constant;
public interface Constants {
    /**
     * 项目配置相关的常量
     */
    public static String SPARK_LOCAL = "spark.local";

    /**
     * 监听的设备ip：
     *
     */

    String ONLINE_LISTEN_DEVICE_IP = "online.listen.device.ip";
    public static String BOOTSTRAP_SERVERS = "bootstrap.servers";
    public static String KAFKA_TOPICS = "kafka.topics";

    /**
     * Spark作业相关的常量
     */
    String SPARK_APP_NAME_1 = "DnsTunnelDetectByNameTrain";
    String SPARK_APP_NAME_2 = "DnsTunnelDetectByNamePredict";
    String SPARK_APP_NAME_3 = "DnsTunnelDetectByActionTrain";
    String SPARK_APP_NAME_4 = "DnsTunnelDetectByActionPredict";
    /**
     * redis相关参数
     */
    public static String REDIS_HOST = "redis.host";
    public static  String REDIS_PORT = "redis.port";
    public static String REDIS_TIMEOUT = "redis.timeout";
    public static String REDIS_DB_INDEX = "redis.db.index";
    public static String REDIS_UMBRELLA_TABLE_NAME="redis.umbrella.table.name";
    public static String REDIS_MODEL_KEY = "redis.model.key";
    String REDIS_DOMAIN_BLACKlIST_KEY = "redis.domain.blacklist.key";
    String REDIS_DOMAIN_WHITELIST_KEY = "redis.domain.whitelist.key";
    /**
     * hive相关参数
     */
    public static String HIVE_DATABASE = "hive.database";
    public static String HIVE_ONLINE_DOMIAN_TABLE = "hive.online.domain.table";

    /**
     * 模型相关常量
     */
    String ONLINE_DECISION_TREE_MODEL = "online.decision.tree.model";
    String ONLINE_RANDOM_FOREST_MODEL = "online.random.forest.model";
    String ONLINE_LR_MODEL = "online.lr.model";

    String HDFS_MODEL_PATH = "hdfs.model.path";
    String LOCAL_MODEL_PATH = "local.model.path";

    String ONLINE_DATA_LOCAL_FILE_PATH = "online.data.local.file.path";
    String ONLINE_DATA_REMOTE_FILE_PATH = "online.data.remote.file.path";

    String ACTION_DATA_LOCAL_FILE_PATH = "action.data.local.file.path";
    String ACTION_DATA_REMOTE_FILE_PATH = "action.data.remote.file.path";

    String ACTION_DECISION_TREE_MODEL = "action.decision.tree.model";
    String ACTION_RANDOM_FOREST_MODEL = "action.random.forest.model";
    String ACTION_LR_MODEL = "action.lr.model";
    String ACTION_NAIVEBAYES_MODEL = "action.naivebayes.model";

}

package com.km.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.km.data.common.util.Configuration;
import com.km.service.common.exception.serviceException;
import com.km.utils.FileUtil;
import com.km.utils.LoadConfigureUtil;
import com.km.utils.PropertiesUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

@Component
public class LoadConfigureRunner implements CommandLineRunner {
    @Override
    public void run(String... args) {
        try {
            String ETLPlugConfigurePath = PropertiesUtil.getProperty("ETLPlugConfigurePath");
            String readerConfigPath = PropertiesUtil.getProperty("readerConfigurePath");
            String writerConfigPath = PropertiesUtil.getProperty("writerConfigurePath");
            loadETLPluginConfiguration(ETLPlugConfigurePath);
            loadReaderAndWriterPluginConfiguration(LoadConfigureUtil.getReaderPlugNameToConf(), readerConfigPath);
            loadReaderAndWriterPluginConfiguration(LoadConfigureUtil.getWriterPlugNameToConf(), writerConfigPath);
        } catch (serviceException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 加载reader或者writer配置文件
     *
     * @param map  存储 插件名->插件配置 映射
     * @param path 配置文件路径
     */
    private void loadReaderAndWriterPluginConfiguration(Map<String, Configuration> map, String path) throws serviceException {
        Configuration configuration = Configuration.from(FileUtil.readFile(path));
        List<JSONObject> AllReaderOrWriter = configuration.getList("reader", JSONObject.class);
        if (AllReaderOrWriter == null) AllReaderOrWriter = configuration.getList("writer", JSONObject.class);

        for (JSONObject parameter : AllReaderOrWriter) {
            String name = parameter.getString("pluginName");
            if (map.get(name) != null) {
                throw new serviceException("DataETL平台不允许出现pluginName一样的Reader或者Writer插件！");
            } else {
                map.put(name, Configuration.from(parameter));
            }
        }
    }

    /**
     * 加载指定目录下所有关于ETL插件的配置文件
     *
     * @param fileName 目录路径
     * @throws serviceException
     * @throws ClassNotFoundException
     */
    public void loadETLPluginConfiguration(String fileName) throws serviceException, ClassNotFoundException {
        Configuration configuration = Configuration.from(FileUtil.readFile(fileName));
        List<JSONObject> plugList = configuration.getList("ETLplugins", JSONObject.class);
        for (JSONObject object : plugList) {
            String pluginName = object.getString("pluginName");
            if (LoadConfigureUtil.getEtlPlugNameToConf().get(pluginName) != null) {
                throw new serviceException("DataETL平台不允许出现name一样的ETL插件！");
            } else {
                LoadConfigureUtil.getEtlPlugNameToConf().put(pluginName, loadPlugParameter(object));
            }
        }
    }

    private Configuration loadPlugParameter(JSONObject object) throws ClassNotFoundException {
        String classPath = object.getString("classPath");

        Class clazz = Class.forName(classPath);

        Field[] fields = clazz.getDeclaredFields();
        JSONArray array = new JSONArray();
        for (Field field : fields) {
            boolean isannotation = field.isAnnotationPresent(com.km.data.common.annotations.Field.class);
            if (isannotation) {
                JSONObject temp = new JSONObject();

                temp.put("filedName", field.getAnnotation(com.km.data.common.annotations.Field.class).fieldName());
                temp.put("desc", field.getAnnotation(com.km.data.common.annotations.Field.class).desc());
                temp.put("necessary", field.getAnnotation(com.km.data.common.annotations.Field.class).necessary());
                temp.put("pluginTrueField", field.getName());
                array.add(temp);
            }
        }
        object.put("parameters", array);
        return Configuration.from(object);
    }

    public static void main(String[] args) throws serviceException, ClassNotFoundException {
        String readerConfigPath = PropertiesUtil.getProperty("readerConfigurePath");
        String writerConfigPath = "src/main/resources/static/config/writerConfiguration.json";
        LoadConfigureRunner test = new LoadConfigureRunner();
        test.run(args);
        Map<String, Configuration> reader = LoadConfigureUtil.getReaderPlugNameToConf();
        Map<String, Configuration> writer = LoadConfigureUtil.getWriterPlugNameToConf();
        Map<String, Configuration> etl = LoadConfigureUtil.getEtlPlugNameToConf();
        parseMap(etl);
    }

    private static String parseMap(Map<String, Configuration> configurationMap) {
        JSONArray array = new JSONArray();
        for (Map.Entry<String, Configuration> entry : configurationMap.entrySet()) {
            JSONObject object = new JSONObject();
            Configuration value = entry.getValue();
            array.add(JSONObject.parseObject(value.toJSON()));
        }
        return array.toJSONString();
    }
}

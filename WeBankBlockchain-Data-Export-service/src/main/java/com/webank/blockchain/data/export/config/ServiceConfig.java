package com.webank.blockchain.data.export.config;

import cn.hutool.core.io.FileUtil;
import com.webank.blockchain.data.export.common.entity.ContractInfo;
import com.webank.blockchain.data.export.common.entity.ESDataSource;
import com.webank.blockchain.data.export.common.entity.MysqlDataSource;
import com.webank.solc.plugin.compiler.CompileSolToJava;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.beetl.core.GroupTemplate;
import org.beetl.core.resource.ClasspathResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author wesleywang
 * @Description:
 * @date 2021/3/30
 */
@Configuration
@ConfigurationProperties("system")
@Data
@Slf4j
public class ServiceConfig {

    private String nodeStr;
    private String groupId;
    private String certPath;

    private List<Integer> groupIds;

    private int cryptoTypeConfig;
    private String rpcUrl;

    private String jdbcUrl;
    private String user;
    private String password;


    private int frequency = 5;

    private String tablePrefix = "";
    private String tablePostfix = "";
    private String namePrefix = "";
    private String namePostfix = "";

    private long startBlockHeight;
    private String startDate;

    @Value("${system.db.autoCreateTable}")
    private boolean autoCreateTable;

    @Value("${system.db.sharding}")
    private boolean sharding;

    private String abiPath;

    private String binaryPath;

    private String solPath;

    @Value("${system.db.shardingNumberPerDatasource}")
    private int shardingNumberPerDatasource;

    /** @Fields crawlBatchUnit : to cut gaint mission to small missions, whose size is this */
    private int crawlBatchUnit = 100;

    private List<MysqlDataSource> mysqlDataSources;

    private List<ContractInfo> contractInfos;

    @Value("${system.es.enabled}")
    private boolean esEnabled;
    @Value("${system.es.clusterName}")
    private String clusterName;
    @Value("${system.es.ip}")
    private String ip;
    @Value("${system.es.port}")
    private int port;

    private boolean multiLiving;

    private String zookeeperServiceLists;
    private String zookeeperNamespace;
    private String prepareTaskJobCron = "0/"+ frequency + " * * * * ?";
    private String dataFlowJobCron = "0/"+ frequency + " * * * * ?";
    private String dataFlowJobItemParameters = "0=A,1=B,2=C,3=D,4=E,5=F,6=G,7=H";
    private int dataFlowJobShardingTotalCount = 8;

    /**
     * ex: Map<contractName, methodName or eventName>
     */
    private Map<String,List<String>> generatedOff_SDK;

    /**
     * ex: Map<contractName, Map<methodName or eventName, List<javaNameParamName>>>
     */
    private Map<String, Map<String,List<String>>> ignoreParam_SDK;

    /**
     * ex: Map<contractName, Map<methodName or eventName, Map<solidityParamName,paramType>>>
     */
    private Map<String, Map<String,Map<String,String>>> paramSQLType_SDK;

    private ESDataSource esDataSource;

    private boolean grafanaEnable;

    @Autowired
    private com.webank.blockchain.data.export.utils.PropertiesUtils PropertiesUtils;

    @Bean
    public GroupTemplate getGroupTemplateInstance() throws IOException {
        ClasspathResourceLoader resourceLoader = new ClasspathResourceLoader("");
        org.beetl.core.Configuration cfg = org.beetl.core.Configuration.defaultConfiguration();
        GroupTemplate gt = new GroupTemplate(resourceLoader, cfg);
        return gt;
    }

    @PostConstruct
    private void init() {
        if(solPath != null) {
            CompileSolToJava compiler = new CompileSolToJava();
            File outputBaseDir = new File("./config/solidity");
            File abiOutputDir = new File(outputBaseDir, "abi");
            File binOutputDir = new File(outputBaseDir, "bin/ecc");
            File smbinOutputDir = new File(outputBaseDir, "bin/sm");
            File solDir = new File(solPath);
            try {
                compiler.compileSolToJava("*",null,
                        solDir, abiOutputDir,binOutputDir,smbinOutputDir,null);
            } catch (Exception e) {
                log.error("CompileSolToJava failed !!! ", e);
            }
            this.abiPath = "./config/solidity/abi";
            if (this.cryptoTypeConfig == 0) {
                this.binaryPath = "./config/solidity/bin/ecc";
            } else {
                this.binaryPath = "./config/solidity/bin/sm";
            }
        }
        mysqlDataSources = PropertiesUtils.getMysqlConfigs();
        contractInfos = PropertiesUtils.getContractInfos();
        generatedOff_SDK = PropertiesUtils.getGeneratedOff();
        ignoreParam_SDK = PropertiesUtils.getIgnoreParam();
        paramSQLType_SDK = PropertiesUtils.getparamSQLType();
        if (esEnabled) {
            esDataSource = new ESDataSource();
            esDataSource.setClusterName(clusterName);
            esDataSource.setIp(ip);
            esDataSource.setPort(port);
            esDataSource.setEnable(true);
        }
        groupIds = new ArrayList<>();
        if (groupId != null && groupId.contains(",")) {
            String[] ids = groupId.split(",");
            for (String id : ids) {
                groupIds.add(Integer.valueOf(id));
            }
        }else {
            if (StringUtils.isNotBlank(groupId)) {
                groupIds.add(Integer.valueOf(groupId));
            }
        }
    }


}

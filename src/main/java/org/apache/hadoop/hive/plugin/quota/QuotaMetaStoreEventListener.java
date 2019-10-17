package org.apache.hadoop.hive.plugin.quota;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.client.HdfsAdmin;
import org.apache.hadoop.hive.metastore.MetaStoreEventListener;
import org.apache.hadoop.hive.metastore.RawStore;
import org.apache.hadoop.hive.metastore.TableType;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.hadoop.hive.metastore.events.AlterDatabaseEvent;
import org.apache.hadoop.hive.metastore.events.AlterTableEvent;
import org.apache.hadoop.hive.metastore.events.CreateDatabaseEvent;
import org.apache.hadoop.hive.metastore.events.CreateTableEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * @author lynn
 * @ClassName org.apache.hive.plugin.quota.QuotaMetaStoreEventListener
 * @Description TODO
 * @Date 19-8-26 下午1:53
 * @Version 1.0
 **/
public class QuotaMetaStoreEventListener extends MetaStoreEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(QuotaMetaStoreEventListener.class);

    public QuotaMetaStoreEventListener(Configuration config) {
        super(config);
        LOG.info("Initialize QuotaMetaStoreEventListener...");
    }

    /**
     *
     * @param tableEvent
     * @throws MetaException
     */
    @Override
    public void onCreateTable(CreateTableEvent tableEvent) throws MetaException {
        LOG.info("create a {} table {} in db {}", tableEvent.getTable().getTableType(), tableEvent.getTable().getTableName(), tableEvent.getTable().getDbName());

        TableType tableType = TableType.valueOf(tableEvent.getTable().getTableType());
        if(TableType.MANAGED_TABLE == tableType || TableType.MATERIALIZED_VIEW == tableType){
            setHdfsQuota(tableEvent.getTable().getParameters(), tableEvent.getTable().getSd().getLocation());
        }
    }

    @Override
    public void onAlterTable(AlterTableEvent tableEvent) throws MetaException {
        LOG.info("alter table from {} to {}", tableEvent.getOldTable(), tableEvent.getNewTable());

        TableType tableType = TableType.valueOf(tableEvent.getNewTable().getTableType());
        if(TableType.MANAGED_TABLE == tableType || TableType.MATERIALIZED_VIEW == tableType){
            setHdfsQuota(tableEvent.getNewTable().getParameters(), tableEvent.getNewTable().getSd().getLocation());
        }
    }

    @Override
    public void onCreateDatabase(CreateDatabaseEvent dbEvent) throws MetaException {
        LOG.info("create database {}, catalog {}", dbEvent.getDatabase().getName(), dbEvent.getDatabase().getCatalogName());

        if(dbEvent.getDatabase().getParameters().containsKey(QuotaMetaStoreEventListenerConstants.TABLE_QUOTA)){
            Integer tableQuota = Integer.parseInt(dbEvent.getDatabase().getParameters().getOrDefault(QuotaMetaStoreEventListenerConstants.TABLE_QUOTA, "-1"));
            if(tableQuota < -1 || tableQuota == 0){
                throw new RuntimeException("The value of \"table.quota\" must greater than 0 or is -1");
            }
        }
        setHdfsQuota(dbEvent.getDatabase().getParameters(), dbEvent.getDatabase().getLocationUri());
    }

    @Override
    public void onAlterDatabase(AlterDatabaseEvent dbEvent) throws MetaException {
        LOG.info("alter database from {}, {} to {}, {}",
                dbEvent.getOldDatabase().getName(), dbEvent.getNewDatabase().getName(),
                dbEvent.getNewDatabase().getName(), dbEvent.getNewDatabase().getCatalogName());

        if(dbEvent.getNewDatabase().getParameters().containsKey(QuotaMetaStoreEventListenerConstants.TABLE_QUOTA)){
            Integer tableQuota = Integer.parseInt(dbEvent.getNewDatabase().getParameters().getOrDefault(QuotaMetaStoreEventListenerConstants.TABLE_QUOTA, "-1"));
            if(tableQuota < -1 || tableQuota == 0){
                throw new RuntimeException("The value of \"table.quota\" must greater than 0 or is -1");
            }
        }

        setHdfsQuota(dbEvent.getNewDatabase().getParameters(), dbEvent.getNewDatabase().getLocationUri());
    }

    /**
     * set name quota and space quota to database
     * @param parameters
     * @param locationUri
     */
    private void setHdfsQuota(Map<String, String> parameters, String locationUri){
        if(null != parameters && parameters.size() > 0){
            if(parameters.containsKey(QuotaMetaStoreEventListenerConstants.NAME_QUOTA)){
                Long nameQuota = Long.parseLong(parameters.get(QuotaMetaStoreEventListenerConstants.NAME_QUOTA));
                //add name quota to locationUri
                LOG.info("set name quota {} to locationUri {}", nameQuota, locationUri);
                try {
                    HdfsAdmin admin = new HdfsAdmin(new URI(locationUri), getConf());
                    if(nameQuota > 0){
                        admin.setQuota(new Path(locationUri), nameQuota);
                    }else{
                        admin.clearQuota(new Path(locationUri));
                    }
                }catch (IOException | URISyntaxException e){
                    LOG.error("set name quota exception: {}", e);
                }
            }
            if(parameters.containsKey(QuotaMetaStoreEventListenerConstants.SPACE_QUOTA)){
                Long spaceQuota = Long.parseLong(parameters.get(QuotaMetaStoreEventListenerConstants.SPACE_QUOTA));
                //add space quota to locationUri
                LOG.info("set space quota {} to locationUri {}", spaceQuota, locationUri);
                try {
                    HdfsAdmin admin = new HdfsAdmin(new URI(locationUri), getConf());
                    if(spaceQuota > 0){
                        admin.setSpaceQuota(new Path(locationUri), spaceQuota);
                    }else{
                        admin.clearSpaceQuota(new Path(locationUri));
                    }
                }catch (IOException | URISyntaxException e){
                    LOG.error("set space quota exception: {}", e);
                }
            }
        }
    }
}

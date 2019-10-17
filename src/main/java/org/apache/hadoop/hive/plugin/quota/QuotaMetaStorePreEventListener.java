package org.apache.hadoop.hive.plugin.quota;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.metastore.MetaStorePreEventListener;
import org.apache.hadoop.hive.metastore.RawStore;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.api.InvalidOperationException;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.hadoop.hive.metastore.events.PreCreateTableEvent;
import org.apache.hadoop.hive.metastore.events.PreEventContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @author lynn
 * @ClassName org.apache.hive.plugin.quota.QuotaMetaStorePreEventListener
 * @Description TODO
 * @Date 19-8-26 下午4:01
 * @Version 1.0
 **/
public class QuotaMetaStorePreEventListener extends MetaStorePreEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(QuotaMetaStorePreEventListener.class);

    public QuotaMetaStorePreEventListener(Configuration config) {
        super(config);
        LOG.info("Initialize QuotaMetaStorePreEventListener...");
    }

    @Override
    public void onEvent(PreEventContext context) throws MetaException, NoSuchObjectException, InvalidOperationException {
        if(context instanceof PreCreateTableEvent){
            switch (context.getEventType()){
                case CREATE_TABLE:
                    PreCreateTableEvent event = (PreCreateTableEvent)context;
                    LOG.info("eventType: {} table: {}, tableType: {}", context.getEventType().name(), event.getTable().getTableName(), event.getTable().getTableType());
                    RawStore rs = context.getHandler().getMS();
                    Database database = rs.getDatabase(event.getTable().getCatName(), event.getTable().getDbName());
                    Map<String, String> parameters = database.getParameters();
                    Integer tableQuota = Integer.parseInt(parameters.getOrDefault(QuotaMetaStoreEventListenerConstants.TABLE_QUOTA, "-1"));
                    if(tableQuota == -1) return;  //no limit
                    List<String> tables = rs.getAllTables(event.getTable().getCatName(), event.getTable().getDbName());
                    int tableNumber = null == tables ? 0: tables.size();
                    if(tableQuota <= tableNumber){
                        throw new MetaException("can't create table because already exceed to table_quota " + tableQuota);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}

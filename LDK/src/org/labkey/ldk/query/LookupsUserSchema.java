package org.labkey.ldk.query;

import org.labkey.api.cache.CacheManager;
import org.labkey.api.cache.StringKeyCache;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.collections.CaseInsensitiveTreeSet;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbSchemaType;
import org.labkey.api.data.SchemaTableInfo;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.ldk.table.ContainerScopedTable;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.SimpleUserSchema;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.ldk.LDKModule;
import org.labkey.ldk.LDKSchema;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Created by bimber on 5/2/2016.
 */
public class LookupsUserSchema extends SimpleUserSchema
{
    private final static StringKeyCache<Object> _cache;

    static
    {
        _cache = CacheManager.getStringKeyCache(1000, CacheManager.UNLIMITED, "LookupsUserSchema");
    }

    public static final String NAME = "lookups";

    public LookupsUserSchema(User user, Container container)
    {
        super(NAME, "This schema allows simple, configurable lookup tables to be created", user, container, DbSchema.get(LDKSchema.SCHEMA_NAME, DbSchemaType.Bare));
    }

    public static void register(Module m)
    {
        DefaultSchema.registerProvider(LookupsUserSchema.NAME, new DefaultSchema.SchemaProvider(m)
        {
            @Override
            public boolean isAvailable(DefaultSchema schema, Module module)
            {
                return schema.getContainer().getActiveModules().contains(ModuleLoader.getInstance().getModule(LDKModule.class));
            }

            @Override
            public QuerySchema createSchema(final DefaultSchema schema, Module module)
            {
                return new LookupsUserSchema(schema.getUser(), schema.getContainer());
            }
        });
    }

    @Override
    public Set<String> getTableNames()
    {
        Set<String> available = new CaseInsensitiveTreeSet();
        available.addAll(getPropertySetNames().keySet());

        return Collections.unmodifiableSet(available);
    }

    @Override
    public synchronized Set<String> getVisibleTableNames()
    {
        Set<String> available = new CaseInsensitiveTreeSet();
        available.addAll(getPropertySetNames().keySet());

        return Collections.unmodifiableSet(available);
    }

    private Map<String, Map<String, Object>> getPropertySetNames()
    {
        Map<String, Map<String, Object>> nameMap = (Map<String, Map<String, Object>>) _cache.get(LookupSetTable.getCacheKey(getContainer()));
        if (nameMap != null)
            return nameMap;

        nameMap = new CaseInsensitiveHashMap<>();

        TableSelector ts = new TableSelector(_dbSchema.getTable(LDKSchema.TABLE_LOOKUP_SETS), new SimpleFilter(FieldKey.fromString("container"), getContainer().getId()), null);
        Map<String, Object>[] rows = ts.getMapArray();
        if (rows.length > 0)
        {
            Set<String> existing = super.getTableNames();
            for (Map<String, Object> row : rows)
            {
                String setname = (String)row.get("setname");
                if (setname != null && !existing.contains(setname))
                    nameMap.put(setname, row);
            }
        }

        nameMap = Collections.unmodifiableMap(nameMap);
        _cache.put(LookupSetTable.getCacheKey(getContainer()), nameMap);

        return nameMap;
    }

    @Override
    public TableInfo createTable(String name)
    {
        Set<String> available = super.getTableNames();

        if (LDKSchema.TABLE_LOOKUP_SETS.equalsIgnoreCase(name))
        {
            ContainerScopedTable ret = new ContainerScopedTable<>(this, createSourceTable(name), "setname");
            ret.addPermissionMapping(InsertPermission.class, AdminPermission.class);
            ret.addPermissionMapping(UpdatePermission.class, AdminPermission.class);
            ret.addPermissionMapping(DeletePermission.class, AdminPermission.class);

            return ret.init();
        }

        if (available.contains(name))
            return super.createTable(name);

        //try to find it in propertySets
        Map<String, Map<String, Object>> nameMap = getPropertySetNames();
        if (nameMap.containsKey(name))
        {
            return createForPropertySet(this, name, nameMap.get(name));
        }

        return null;
    }

    private LookupSetTable createForPropertySet(UserSchema us, String setName, Map<String, Object> map)
    {
        SchemaTableInfo table = _dbSchema.getTable(LDKSchema.TABLE_LOOKUPS);

        return new LookupSetTable(us, table, setName, map).init();
    }

    public static void clearCacheForContainer(Container c)
    {
        _cache.remove(LookupSetTable.getCacheKey(c));
    }
}

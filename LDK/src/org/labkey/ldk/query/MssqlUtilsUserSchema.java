package org.labkey.ldk.query;

import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbSchemaType;
import org.labkey.api.data.TableInfo;
import org.labkey.api.module.Module;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.SimpleUserSchema;
import org.labkey.api.security.User;
import org.labkey.ldk.LDKSchema;

import java.util.Collections;
import java.util.Set;

/**

 */
public class MssqlUtilsUserSchema extends SimpleUserSchema
{
    public static final String NAME = "mssqlutils";

    public MssqlUtilsUserSchema(User user, Container container)
    {
        super(NAME, "This schema exposes tables of information from SQL Server", user, container, DbSchema.get("SYS", DbSchemaType.Bare));
    }

    public static void register(Module m)
    {
        DefaultSchema.registerProvider(MssqlUtilsUserSchema.NAME, new DefaultSchema.SchemaProvider(m)
        {
            @Override
            public boolean isAvailable(DefaultSchema schema, Module module)
            {
                return schema.getContainer().isRoot();
            }

            @Override
            public QuerySchema createSchema(final DefaultSchema schema, Module module)
            {
                return new MssqlUtilsUserSchema(schema.getUser(), schema.getContainer());
            }
        });
    }

    @Override
    public boolean isHidden()
    {
        return false;
    }

    @Override
    public TableInfo createTable(String name, ContainerFilter cf)
    {
        if (!getTableNames().contains(name))
            return null;

        if (IndexStatsTable.NAME.equalsIgnoreCase(name))
        {
            return new IndexStatsTable(LDKSchema.getInstance().getSchema(), this, cf);
        }

        throw new UnsupportedOperationException("No table configured for: " + name);
    }

    @Override
    public Set<String> getTableNames()
    {
        return Collections.singleton(IndexStatsTable.NAME);
    }

    @Override
    public Set<String> getVisibleTableNames()
    {
        return getTableNames();
    }
}
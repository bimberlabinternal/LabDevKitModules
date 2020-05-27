package org.labkey.ldk.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.VirtualTable;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.UserSchema;

/**

 */
public class IndexStatsTable extends VirtualTable
{
    public static final String NAME = "index_stats";

    public IndexStatsTable(DbSchema schema, UserSchema userSchema, ContainerFilter cf)
    {
        super(schema, NAME, userSchema, cf);
        setTitle("SQL Server Index Usage");
        setupColumns();
    }

    @Override
    @NotNull
    public SQLFragment getFromSQL()
    {
        SQLFragment ret = new SQLFragment("SELECT " +
            "OBJECT_NAME(S.OBJECT_ID) AS OBJECT_NAME, " +
            "I.NAME AS INDEX_NAME, " +
            "USER_SEEKS, " +
            "USER_SCANS, " +
            "USER_LOOKUPS, " +
            "USER_UPDATES, " +
            "CASE " +
            "WHEN USER_SEEKS >= USER_SCANS AND USER_SEEKS >= USER_LOOKUPS THEN USER_SEEKS " +
            "WHEN USER_SCANS >= USER_SEEKS AND USER_SCANS >= USER_LOOKUPS THEN USER_SCANS " +
            "WHEN USER_LOOKUPS >= USER_SEEKS AND USER_LOOKUPS >= USER_SCANS THEN USER_LOOKUPS " +
            "ELSE USER_SEEKS " +
            "END AS MAX_VALUE " +
        "FROM SYS.DM_DB_INDEX_USAGE_STATS S " +
        "INNER JOIN SYS.INDEXES I ON (I.OBJECT_ID = S.OBJECT_ID AND I.INDEX_ID = S.INDEX_ID) " +
        "WHERE OBJECTPROPERTY(S.OBJECT_ID, 'IsUserTable') = 1");

        return ret;
    }

    @Override
    public boolean isPublic()
    {
        return true;
    }

    protected void setupColumns()
    {
        addColumn(new ExprColumn(this, "objectName", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".OBJECT_NAME"), JdbcType.VARCHAR));
        addColumn(new ExprColumn(this, "indexName", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".INDEX_NAME"), JdbcType.VARCHAR));
        addColumn(new ExprColumn(this, "userSeeks", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".USER_SEEKS"), JdbcType.INTEGER));
        addColumn(new ExprColumn(this, "userScans", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".USER_SCANS"), JdbcType.INTEGER));
        addColumn(new ExprColumn(this, "userLookups", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".USER_LOOKUPS"), JdbcType.INTEGER));
        addColumn(new ExprColumn(this, "maxValue", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".MAX_VALUE"), JdbcType.INTEGER));
        addColumn(new ExprColumn(this, "userUpdates", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".USER_UPDATES"), JdbcType.INTEGER));
    }
}

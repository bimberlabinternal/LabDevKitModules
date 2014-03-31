package org.labkey.ldk.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CrosstabMeasure;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.VirtualTable;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.UserSchema;
import org.labkey.ldk.LDKSchema;

/**

 */
public class IndexStatsTable extends VirtualTable
{
    public static final String NAME = "index_stats";

    public IndexStatsTable(DbSchema schema, UserSchema userSchema)
    {
        super(schema, NAME, userSchema);
        setTitle("SQL Server Index Usage");
        setupColumns();
    }

    @NotNull
    public SQLFragment getFromSQL()
    {
        SQLFragment ret = new SQLFragment("SELECT " +
            "OBJECT_NAME(S.OBJECT_ID) AS OBJECT_NAME, " +
            "I.NAME AS INDEX_NAME, " +
            "USER_SEEKS, " +
            "USER_SCANS, " +
            "USER_LOOKUPS, " +
            "USER_UPDATES " +
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
        addColumn(new ExprColumn(this, "indexName", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".INDEX_NAME"), JdbcType.INTEGER));
        addColumn(new ExprColumn(this, "userSeeks", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".USER_SEEKS"), JdbcType.INTEGER));
        addColumn(new ExprColumn(this, "userScans", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".USER_SCANS"), JdbcType.INTEGER));
        addColumn(new ExprColumn(this, "userLookups", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".USER_LOOKUPS"), JdbcType.INTEGER));
        addColumn(new ExprColumn(this, "userUpdates", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".USER_UPDATES"), JdbcType.INTEGER));
    }
}

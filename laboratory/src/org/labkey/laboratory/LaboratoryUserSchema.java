package org.labkey.laboratory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.TableInfo;
import org.labkey.api.module.Module;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.SimpleUserSchema;
import org.labkey.api.security.User;
import org.labkey.api.laboratory.query.ContainerIncrementingTable;
import org.labkey.laboratory.query.LaboratoryWorkbooksTable;
import org.labkey.api.ldk.table.ContainerScopedTable;

/**
 * User: bimber
 * Date: 1/20/13
 * Time: 7:57 AM
 */
public class LaboratoryUserSchema extends SimpleUserSchema
{
    private LaboratoryUserSchema(User user, Container container, DbSchema schema)
    {
        super(LaboratoryModule.SCHEMA_NAME, null, user, container, schema);
    }

    public static void register(final Module m)
    {
        final DbSchema dbSchema = DbSchema.get(LaboratoryModule.SCHEMA_NAME);

        DefaultSchema.registerProvider(LaboratoryModule.SCHEMA_NAME, new DefaultSchema.SchemaProvider(m)
        {
            @Override
            public QuerySchema createSchema(final DefaultSchema schema, Module module)
            {
                return new LaboratoryUserSchema(schema.getUser(), schema.getContainer(), dbSchema);
            }
        });
    }

    @Override
    @Nullable
    protected TableInfo createWrappedTable(String name, @NotNull TableInfo sourceTable, ContainerFilter cf)
    {
        if (LaboratorySchema.TABLE_SUBJECTS.equalsIgnoreCase(name))
            return getSubjectsTable(name, sourceTable, cf);
        else if (LaboratorySchema.TABLE_FREEZERS.equalsIgnoreCase(name))
            return getContainerScopedTable(name, sourceTable, cf, "name");
        else if (LaboratorySchema.TABLE_SAMPLE_TYPE.equalsIgnoreCase(name))
            return getContainerScopedTable(name, sourceTable, cf, "type");
        else if (LaboratorySchema.TABLE_DNA_OLIGOS.equalsIgnoreCase(name))
            return getDnaOligosTable(name, sourceTable, cf);
        else if (LaboratorySchema.TABLE_PEPTIDES.equalsIgnoreCase(name))
            return getPeptideTable(name, sourceTable, cf);
        else if (LaboratorySchema.TABLE_ANTIBODIES.equalsIgnoreCase(name))
            return getAntibodiesTable(name, sourceTable, cf);
        else if (LaboratorySchema.TABLE_WORKBOOKS.equalsIgnoreCase(name))
            return getWorkbooksTable(name, sourceTable, cf);
        else if (LaboratorySchema.TABLE_SAMPLES.equalsIgnoreCase(name))
            return getSamplesTable(name, sourceTable, cf);
        else
            return super.createWrappedTable(name, sourceTable, cf);
    }

    private SimpleTable getSubjectsTable(String name, @NotNull TableInfo schematable, ContainerFilter cf)
    {
        return new ContainerScopedTable<>(this, schematable, cf, "subjectname").init();
    }

    private TableInfo getContainerScopedTable(String name, @NotNull TableInfo schematable, ContainerFilter cf, String pkCol)
    {
        return new ContainerScopedTable<>(this, schematable, cf, pkCol).init();
    }

    private SimpleTable getDnaOligosTable(String name, @NotNull TableInfo schematable, ContainerFilter cf)
    {
        return new ContainerIncrementingTable(this, schematable, cf, "oligo_id").init();
    }

    private SimpleTable getSamplesTable(String name, @NotNull TableInfo schematable, ContainerFilter cf)
    {
        return new ContainerIncrementingTable(this, schematable, cf, "freezerid").init();
    }

    private SimpleTable getPeptideTable(String name, @NotNull TableInfo schematable, ContainerFilter cf)
    {
        return new ContainerIncrementingTable(this, schematable, cf, "peptideId").init();
    }

    private SimpleTable getAntibodiesTable(String name, @NotNull TableInfo schematable, ContainerFilter cf)
    {
        return new ContainerIncrementingTable(this, schematable, cf, "antibodyId").init();
    }

    private SimpleTable getWorkbooksTable(String name, @NotNull TableInfo schematable, ContainerFilter cf)
    {
        return new LaboratoryWorkbooksTable(this, schematable, cf).init();
    }
}

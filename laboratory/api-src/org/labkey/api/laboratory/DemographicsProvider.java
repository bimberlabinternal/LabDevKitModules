package org.labkey.api.laboratory;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.module.Module;
import org.labkey.api.security.User;


public class DemographicsProvider
{
    private final Module _owningModule;
    private final String _schemaName;
    private final String _queryName;
    private final String _subjectField;

    public DemographicsProvider(Module owningModule, String schemaName, String queryName, String subjectField)
    {
        _owningModule = owningModule;
        _schemaName = schemaName;
        _queryName = queryName;
        _subjectField = subjectField;
    }

    public String getSchema()
    {
        return _schemaName;
    }

    public String getQuery()
    {
        return _queryName;
    }

    public String getSubjectField()
    {
        return _subjectField;
    }

    public @Nullable String getMotherField()
    {
        return null;
    }

    public @Nullable String getFatherField()
    {
        return null;
    }

    public @Nullable String getSexField()
    {
        return null;
    }

    public boolean isAvailable(Container c, User u)
    {
        return c.getActiveModules().contains(_owningModule);
    }

    public  String getLabel()
    {
        return getSchema() + "." + getQuery();
    }

    public  boolean isValidForPedigree()
    {
        return getMotherField() != null && getFatherField() != null && getSexField() != null;
    }
}
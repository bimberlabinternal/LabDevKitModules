package org.labkey.ldk.sql;

import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbSchemaType;
import org.labkey.api.data.bigiron.AbstractClrInstallationManager;
import org.labkey.api.view.template.Warnings;
import org.labkey.ldk.LDKController;
import org.labkey.ldk.LDKModule;

import static org.labkey.ldk.LDKSchema.SCHEMA_NAME;

public class LDKNaturalizeInstallationManager extends AbstractClrInstallationManager
{
    private static final String INITIAL_VERSION = "1.0.0";
    private static final String CURRENT_VERSION = "1.0.1";
    private static final String DESCRIPTION = "This function helps with applying more human-friendly sorting of mixed numeric/text values.";

    private static final LDKNaturalizeInstallationManager _instance = new LDKNaturalizeInstallationManager();

    private LDKNaturalizeInstallationManager()
    {
    }

    public static LDKNaturalizeInstallationManager get()
    {
        return _instance;
    }

    @Override
    protected DbSchema getSchema()
    {
        return DbSchema.get(SCHEMA_NAME, DbSchemaType.Module);
    }

    @Override
    protected String getModuleName()
    {
        return LDKModule.NAME;
    }

    @Override
    protected String getBaseScriptName()
    {
        return "Naturalize";
    }

    @Override
    protected String getInitialVersion()
    {
        return INITIAL_VERSION;
    }

    @Override
    protected String getCurrentVersion()
    {
        return CURRENT_VERSION;
    }

    @Override
    protected String getInstallationExceptionMsg()
    {
        return "Failure installing LDK naturalize function. " + DESCRIPTION + "Contact LabKey if you need assistance installing these functions.";
    }

    @Override
    protected String getUninstallationExceptionMsg()
    {
        return "Failure uninstalling the existing LDK naturalize function, which means they can't be upgraded to the latest version. Contact LabKey if you need assistance installing the newest version of this function.";
    }

    @Override
    protected String getInstallationCheckSql()
    {
        return "SELECT x.G, ldk.Naturalize('Foo') FROM (SELECT 1 AS G) x GROUP BY G;";
    }

    @Override
    protected String getVersionCheckSql()
    {
        return "SELECT ldk.NaturalizeVersion()";
    }

    @Override
    protected void addAdminWarningMessages(Warnings warnings)
    {
        addAdminWarningMessage(warnings, "The LDK naturalize function is not installed. " + DESCRIPTION, LDKController.DownloadNaturalizeInstallScriptAction.class, null);
    }
}

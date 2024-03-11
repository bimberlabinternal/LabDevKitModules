package org.labkey.laboratory.query;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.laboratory.DemographicsProvider;
import org.labkey.api.module.ModuleLoader;
import org.labkey.laboratory.LaboratoryModule;
import org.labkey.laboratory.LaboratorySchema;

public class LaboratoryDemographicsProvider extends DemographicsProvider
{
    public LaboratoryDemographicsProvider()
    {
        super(ModuleLoader.getInstance().getModule(LaboratoryModule.class), LaboratoryModule.SCHEMA_NAME, LaboratorySchema.TABLE_SUBJECTS, "subjectname");
    }

    @Nullable
    @Override
    public String getMotherField()
    {
        return "mother";
    }

    @Nullable
    @Override
    public String getFatherField()
    {
        return "father";
    }

    @Nullable
    @Override
    public String getSexField()
    {
        return "gender";
    }
}

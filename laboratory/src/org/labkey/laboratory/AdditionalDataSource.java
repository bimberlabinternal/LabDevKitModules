package org.labkey.laboratory;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.laboratory.LaboratoryService;
import org.labkey.api.query.QueryDefinition;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;

/**
 * User: bimber
 * Date: 1/19/13
 * Time: 9:26 AM
 */
public class AdditionalDataSource extends AbstractDataSource
{
    private LaboratoryService.NavItemCategory _itemType;
    private String _reportCategory;
    private boolean _importIntoWorkbooks;
    private String _subjectFieldKey;
    private String _sampleDateFieldKey;
    private static final Logger _log = LogManager.getLogger(AdditionalDataSource.class);

    public AdditionalDataSource(LaboratoryService.NavItemCategory itemType, String label, @Nullable String containerId, String schemaName, String queryName, String reportCategory, boolean importIntoWorkbooks, String subjectFieldKey, String sampleDateFieldKey)
    {
        super(label, containerId, schemaName, queryName);
        _itemType = itemType;
        _reportCategory = reportCategory;
        _importIntoWorkbooks = importIntoWorkbooks;
        _subjectFieldKey = subjectFieldKey;
        _sampleDateFieldKey = sampleDateFieldKey;
    }

    public static AdditionalDataSource getFromParts(Container c, User u, String itemType, String label, @Nullable String containerId, String schemaName, String queryName, String reportCategory, boolean importIntoWorkbooks, String subjectFieldKey, String sampleDateFieldKey) throws IllegalArgumentException
    {
        AdditionalDataSource.validateKey(c, u, containerId, schemaName, queryName, label, itemType, reportCategory, importIntoWorkbooks);

        LaboratoryService.NavItemCategory cat = LaboratoryService.NavItemCategory.valueOf(itemType);
        return new AdditionalDataSource(cat, label, containerId, schemaName, queryName, reportCategory, importIntoWorkbooks, subjectFieldKey, sampleDateFieldKey);
    }

    public static @Nullable AdditionalDataSource getFromPropertyManager(Container c, User u, String key, String value) throws IllegalArgumentException
    {
        if (value == null)
            return null;

        try
        {
            JSONObject json = new JSONObject(value);
            String schemaName = StringUtils.trimToNull(json.optString("schemaName"));
            String queryName = StringUtils.trimToNull(json.optString("queryName"));
            String containerId = StringUtils.trimToNull(json.optString("containerId"));
            String label = StringUtils.trimToNull(json.optString("label"));
            String itemType = StringUtils.trimToNull(json.optString("itemType"));
            String subjectFieldKey = StringUtils.trimToNull(json.optString("subjectFieldKey"));
            String sampleDateFieldKey = StringUtils.trimToNull(json.optString("sampleDateFieldKey"));

            //for legacy data:
            if (json.has("category") && !json.has("reportCategory"))
            {
                json.put("reportCategory", json.getString("category"));
            }

            String reportCategory = StringUtils.trimToNull(json.optString("reportCategory"));
            boolean importIntoWorkbooks = json.has("importIntoWorkbooks") && json.getBoolean("importIntoWorkbooks");

            validateKey(c, u, containerId, schemaName, queryName, label, itemType, reportCategory, importIntoWorkbooks);
            LaboratoryService.NavItemCategory cat = LaboratoryService.NavItemCategory.valueOf(itemType);

            return new AdditionalDataSource(cat, label, containerId, schemaName, queryName, reportCategory, importIntoWorkbooks, subjectFieldKey, sampleDateFieldKey);
        }
        catch (JSONException e)
        {
            _log.error("Malformed data source saved in " + c.getPath() + ": " + e.getMessage() + ".  was: " + value);
            return null;
        }
    }

    @Override
    public String getPropertyManagerKey()
    {
        return getForKey(getSchemaName()) + DELIM + getForKey(getQueryName()) + DELIM + getForKey(getContainerId()) + DELIM + getForKey(getLabel()) + DELIM + getForKey(getItemType().name())  + DELIM + getForKey(getReportCategory());
    }

    public String getPropertyManagerValue()
    {
        JSONObject json = new JSONObject();
        json.put("containerId", getContainerId());
        json.put("schemaName", getSchemaName());
        json.put("queryName", getQueryName());
        json.put("itemType", getItemType());
        json.put("reportCategory", getReportCategory());
        json.put("label", getLabel());
        json.put("subjectFieldKey", getSubjectFieldKey());
        json.put("sampleDateFieldKey", getSampleDateFieldKey());
        json.put("importIntoWorkbooks", isImportIntoWorkbooks());

        return json.toString();
    }

    public LaboratoryService.NavItemCategory getItemType()
    {
        return _itemType;
    }

    public String getReportCategory()
    {
        return _reportCategory;
    }

    public boolean isImportIntoWorkbooks()
    {
        return _importIntoWorkbooks;
    }

    public String getSubjectFieldKey()
    {
        return _subjectFieldKey;
    }

    public String getSampleDateFieldKey()
    {
        return _sampleDateFieldKey;
    }

    private static boolean validateKey(Container defaultContainer, User u, @Nullable String containerId, String schemaName, String queryName, String label, String navItemCategory, String reportCategory, boolean importIntoWorkbooks) throws IllegalArgumentException
    {
        Container target = null;
        if (containerId != null)
        {
            target = ContainerManager.getForId(containerId);
            if (target == null)
            {
                _log.error("Invalid containerId in saved data source: " + containerId);
            }
        }

        if (target == null)
        {
            target = defaultContainer;
        }

        UserSchema us = QueryService.get().getUserSchema(u, target, schemaName);
        if (us == null)
        {
            throw new IllegalArgumentException("Unknown schema in saved data source: " + schemaName + ", in container: " + target.getPath());
        }

        QueryDefinition qd = us.getQueryDefForTable(queryName);
        if (qd == null)
        {
            throw new IllegalArgumentException("Unknown query in saved data source: " + queryName);
        }

        if (StringUtils.trimToNull(label) == null)
            throw new IllegalArgumentException("Label must not be blank");

        if (StringUtils.trimToNull(reportCategory) == null)
            throw new IllegalArgumentException("Report category must not be blank");

        if (StringUtils.trimToNull(navItemCategory) == null)
            throw new IllegalArgumentException("Item type must not be blank");

        try
        {
            LaboratoryService.NavItemCategory.valueOf(navItemCategory);
        }
        catch (IllegalArgumentException e)
        {
            throw new IllegalArgumentException("Unknown value for item type: " + navItemCategory);
        }

        return true;
    }

    @Override
    public JSONObject toJSON(Container c, User u, boolean includeTotals)
    {
        JSONObject json = super.toJSON(c, u, includeTotals);
        json.put("reportCategory", getReportCategory());
        json.put("importIntoWorkbooks", isImportIntoWorkbooks());
        json.put("subjectFieldKey", getSubjectFieldKey());
        json.put("sampleDateFieldKey", getSampleDateFieldKey());
        if (getItemType() != null)
            json.put("itemType", getItemType().name());

        return json;
    }
}

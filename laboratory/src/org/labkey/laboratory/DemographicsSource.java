package org.labkey.laboratory;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.QueryDefinition;
import org.labkey.api.query.QueryException;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.study.Dataset;
import org.labkey.api.study.DatasetTable;

import java.util.ArrayList;
import java.util.List;

/**
 * User: bimber
 * Date: 1/11/13
 * Time: 5:42 PM
 */
public class DemographicsSource extends AbstractDataSource
{
    private final String _targetColumn;
    private static final Logger _log = LogManager.getLogger(DemographicsSource.class);
    
    public DemographicsSource(String label, String containerId, String schemaName, String queryName, String targetColumn)
    {
        super(label, containerId, schemaName, queryName);
        _targetColumn = targetColumn;
    }

    public static DemographicsSource getFromParts(Container c, User u, String label, String containerId, String schemaName, String queryName, String targetColumn) throws IllegalArgumentException
    {
        if (!isValidSource(c, u, containerId, schemaName, queryName, targetColumn, label))
        {
            return null;    
        }
        
        return new DemographicsSource(label, containerId, schemaName, queryName, targetColumn);
    }

    public static @Nullable DemographicsSource getFromPropertyManager(Container c, User u, String key, String value) throws IllegalArgumentException
    {
        if (value == null)
        {
            return null;
        }

        try
        {
            JSONObject json = new JSONObject(value);
            String schemaName = StringUtils.trimToNull(json.optString("schemaName"));
            String queryName = StringUtils.trimToNull(json.optString("queryName"));
            String containerId = StringUtils.trimToNull(json.optString("containerId"));
            String label = StringUtils.trimToNull(json.optString("label"));
            String targetColumn = StringUtils.trimToNull(json.optString("targetColumn"));

            if (!isValidSource(c, u, containerId, schemaName, queryName, targetColumn, label))
            {
                return null;
            }

            return new DemographicsSource(label, containerId, schemaName, queryName, targetColumn);
        }
        catch (JSONException e)
        {
            _log.error("Malformed demographics source saved in " + c.getPath() + ": " + e.getMessage() + ".  value was: " + value);
            return null;
        }
    }

    public String getTargetColumn()
    {
        return _targetColumn;
    }

    @Override
    public String getPropertyManagerKey()
    {
        return getForKey(getSchemaName()) + DELIM + getForKey(getQueryName()) + DELIM + getForKey(getContainerId()) + DELIM + getForKey(getTargetColumn()) + DELIM + getForKey(getLabel());
    }

    public String getPropertyManagerValue()
    {
        JSONObject json = new JSONObject();
        json.put("containerId", getContainerId());
        json.put("schemaName", getSchemaName());
        json.put("queryName", getQueryName());
        json.put("targetColumn", getTargetColumn());
        json.put("label", getLabel());

        return json.toString();
    }

    @Override
    public JSONObject toJSON(Container c, User u, boolean includeTotals)
    {
        JSONObject json = super.toJSON(c, u, includeTotals);
        json.put("targetColumn", getTargetColumn());
        
        return json;
    }

    private static boolean isValidSource(Container defaultContainer, User u, @Nullable String containerId, String schemaName, String queryName, String targetColumn, String label) throws IllegalArgumentException
    {
        Container target = null;
        if (containerId != null)
        {
            target = ContainerManager.getForId(containerId);
            if (target == null)
            {
                _log.error("Invalid containerId in saved demographics source: " + containerId);
            }
        }

        if (target == null)
        {
            target = defaultContainer;
        }

        if (!target.hasPermission(u, ReadPermission.class))
        {
            return false;
        }

        UserSchema us = QueryService.get().getUserSchema(u, target, schemaName);
        if (us == null)
        {
            throw new IllegalArgumentException("Unknown schema in saved demographics source: " + schemaName + ", in container: " + target.getPath());
        }

        if (!us.canReadSchema())
        {
            return false;
        }

        QueryDefinition qd = us.getQueryDefForTable(queryName);
        if (qd == null)
        {
            throw new IllegalArgumentException("Unknown query in saved data source: " + queryName);
        }

        if (targetColumn == null)
        {
            throw new IllegalArgumentException("Missing targetColumn");
        }

        List<QueryException> errors = new ArrayList<>();
        TableInfo ti = qd.getTable(errors,  true);

        if (!errors.isEmpty())
        {
            _log.error("Unable to create TableInfo for query: " + queryName + ". there were " + errors.size() + " errors");
            for (QueryException e : errors)
            {
                _log.error(e.getMessage());
            }

            throw new IllegalArgumentException("Unable to create table for query: " + queryName, errors.get(0));
        }

        if (ti == null)
        {
            throw new IllegalArgumentException("Unable to create table for query: " + queryName);
        }

        if (!ti.hasPermission(u, ReadPermission.class))
        {
            return false;
        }

        ColumnInfo col = ti.getColumn(targetColumn);
        if (col == null)
        {
            throw new IllegalArgumentException("Unable to find column: " + targetColumn);
        }
        targetColumn = col.getName(); //normalize string

        //NOTE: this is not an ideal solution.  for now, special-case demographics datasets.
        //It is problematic to flag the participant column as a keyfield, since other places in the code expect a single PK column
        if (!ti.getPkColumnNames().contains(targetColumn))
        {
            if (ti instanceof DatasetTable)
            {
                Dataset ds = ((DatasetTable)ti).getDataset();
                if (!ds.hasPermission(u, ReadPermission.class))
                {
                    return false;
                }

                if (!(ds.isDemographicData() && ds.getStudy().getSubjectColumnName().equalsIgnoreCase(col.getName())))
                {
                    throw new IllegalArgumentException("Target column is not a key field: " + targetColumn);
                }
            }
            else
            {
                throw new IllegalArgumentException("Target column is not a key field: " + targetColumn);
            }
        }

        if (!col.getJdbcType().equals(JdbcType.VARCHAR))
        {
            throw new IllegalArgumentException("The selected target column is not a string: " + targetColumn);
        }

        if (StringUtils.trimToNull(label) == null)
        {
            throw new IllegalArgumentException("Label must not be blank");
        }

        return true;
    }
}

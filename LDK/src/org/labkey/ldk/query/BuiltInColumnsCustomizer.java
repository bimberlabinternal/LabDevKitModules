package org.labkey.ldk.query;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.MutableColumnInfo;
import org.labkey.api.data.TableCustomizer;
import org.labkey.api.data.TableInfo;
import org.labkey.api.gwt.client.FacetingBehaviorType;

import java.sql.Timestamp;

/**
 * Created with IntelliJ IDEA.
 * User: bimber
 * Date: 11/12/12
 * Time: 7:54 AM
 */
public class BuiltInColumnsCustomizer implements TableCustomizer
{
    private static final Logger _log = LogManager.getLogger(TableCustomizer.class);
    private boolean _disableFacetingForNumericCols = true;

    public BuiltInColumnsCustomizer()
    {

    }

    @Override
    public void customize(TableInfo table)
    {
        for (ColumnInfo col : table.getColumns())
        {
            COL_ENUM.processColumn( (MutableColumnInfo)col );

            if (_disableFacetingForNumericCols && col.isNumericType() && col.getFk() == null)
            {
                ((MutableColumnInfo)col).setFacetingBehaviorType(FacetingBehaviorType.ALWAYS_OFF);
            }
        }

        table.setDefaultVisibleColumns(null);
    }

    private enum COL_ENUM
    {
        created(Timestamp.class){
            @Override
            public void customizeColumn(MutableColumnInfo col)
            {
                setNonEditable(col);
                col.setHidden(true);
                col.setShownInDetailsView(false);
                col.setLabel("Created");
            }
        },
        createdby(Integer.class){
            @Override
            public void customizeColumn(MutableColumnInfo col)
            {
                setNonEditable(col);
                col.setHidden(true);
                col.setShownInDetailsView(false);
                col.setLabel("Created By");
            }
        },
        modified(Timestamp.class){
            @Override
            public void customizeColumn(MutableColumnInfo col)
            {
                setNonEditable(col);
                col.setHidden(true);
                col.setShownInDetailsView(false);
                col.setLabel("Modified");
            }
        },
        modifiedby(Integer.class){
            @Override
            public void customizeColumn(MutableColumnInfo col)
            {
                setNonEditable(col);
                col.setHidden(true);
                col.setShownInDetailsView(false);
                col.setLabel("Modified By");
            }
        },
        container(String.class){
            @Override
            public void customizeColumn(MutableColumnInfo col)
            {
                setNonEditable(col);
                col.setLabel("Folder");
            }
        },
        rowid(Integer.class){
            @Override
            public void customizeColumn(MutableColumnInfo col)
            {
                setNonEditable(col);
                col.setAutoIncrement(true);
            }
        },
        entityid(String.class){
            @Override
            public void customizeColumn(MutableColumnInfo col)
            {
                setNonEditable(col);
                col.setShownInDetailsView(false);
                col.setHidden(true);
            }
        },
        objectid(String.class){
            @Override
            public void customizeColumn(MutableColumnInfo col)
            {
                setNonEditable(col);
                col.setShownInDetailsView(false);
                col.setHidden(true);
            }
        };

        private Class dataType;

        COL_ENUM(Class dataType){
            this.dataType = dataType;
        }

        private static void setNonEditable(MutableColumnInfo col)
        {
            col.setUserEditable(false);
            col.setShownInInsertView(false);
            col.setShownInUpdateView(false);
        }

        abstract public void customizeColumn(MutableColumnInfo col);

        public static void processColumn(MutableColumnInfo col)
        {
            for (COL_ENUM colEnum : COL_ENUM.values())
            {
                if (colEnum.name().equalsIgnoreCase(col.getName()))
                {
                    if (col.getJdbcType().getJavaClass() == colEnum.dataType)
                    {
                        colEnum.customizeColumn(col);
                    }

                    if (col.isAutoIncrement())
                    {
                        col.setUserEditable(false);
                        col.setShownInInsertView(false);
                        col.setShownInUpdateView(false);
                    }

                    break;
                }
            }
        }
    }

    public void setDisableFacetingForNumericCols(boolean disableFacetingForNumericCols)
    {
        _disableFacetingForNumericCols = disableFacetingForNumericCols;
    }
}
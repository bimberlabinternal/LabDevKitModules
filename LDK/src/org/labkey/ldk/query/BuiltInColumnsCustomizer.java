package org.labkey.ldk.query;

import org.apache.log4j.Logger;
import org.labkey.api.data.ColumnInfo;
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
    private static final Logger _log = Logger.getLogger(TableCustomizer.class);
    private boolean _disableFacetingForNumericCols = true;

    public BuiltInColumnsCustomizer()
    {

    }

    public void customize(TableInfo table)
    {
        for (ColumnInfo col : table.getColumns())
        {
            COL_ENUM.processColumn(col);

            if (_disableFacetingForNumericCols && col.isNumericType() && col.getFk() == null)
            {
                col.setFacetingBehaviorType(FacetingBehaviorType.ALWAYS_OFF);
            }
        }
    }

    private enum COL_ENUM
    {
        created(Timestamp.class){
            public void customizeColumn(ColumnInfo col)
            {
                setNonEditable(col);
                col.setHidden(true);
            }
        },
        createdby(Integer.class){
            public void customizeColumn(ColumnInfo col)
            {
                setNonEditable(col);
                col.setHidden(true);
            }
        },
        modified(Timestamp.class){
            public void customizeColumn(ColumnInfo col)
            {
                setNonEditable(col);
                col.setHidden(true);
            }
        },
        modifiedby(Integer.class){
            public void customizeColumn(ColumnInfo col)
            {
                setNonEditable(col);
                col.setHidden(true);
            }
        },
        container(String.class){
            public void customizeColumn(ColumnInfo col)
            {
                setNonEditable(col);
                col.setLabel("Folder");
            }
        },
        rowid(Integer.class){
            public void customizeColumn(ColumnInfo col)
            {
                setNonEditable(col);
                col.setAutoIncrement(true);
            }
        },
        entityid(String.class){
            public void customizeColumn(ColumnInfo col)
            {
                setNonEditable(col);
                col.setHidden(true);
            }
        };

        private Class dataType;

        COL_ENUM(Class dataType){
            this.dataType = dataType;
        }

        private static void setNonEditable(ColumnInfo col)
        {
            col.setUserEditable(false);
            col.setShownInInsertView(false);
            col.setShownInUpdateView(false);
        }

        abstract public void customizeColumn(ColumnInfo col);

        public static void processColumn(ColumnInfo col)
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
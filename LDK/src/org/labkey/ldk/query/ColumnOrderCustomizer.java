package org.labkey.ldk.query;

import org.apache.log4j.Logger;
import org.labkey.api.data.AbstractTableInfo;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.TableCustomizer;
import org.labkey.api.data.TableInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: bimber
 * Date: 2/1/13
 * Time: 7:11 AM
 */
public class ColumnOrderCustomizer implements TableCustomizer
{
    private static final Logger _log = Logger.getLogger(TableCustomizer.class);

    public ColumnOrderCustomizer()
    {

    }

    public void customize(TableInfo table)
    {
        if (table instanceof AbstractTableInfo)
        {
            sortColumns((AbstractTableInfo)table);

            setDefaultVisible((AbstractTableInfo)table);
        }
    }

    public void sortColumns(AbstractTableInfo table)
    {
        List<ColumnInfo> columns = new ArrayList<>();
        columns.addAll(table.getColumns());
        for (ColumnInfo c : columns)
            table.removeColumn(c);

        // sort the columns using the following rules:
        // put calculated columns at the end
        // respect the original sort order for non-calculated
        // alphabetize calculated columns
        Collections.sort(columns, new Comparator<ColumnInfo>()
        {
            @Override
            public int compare(ColumnInfo o1, ColumnInfo o2)
            {
                if (isReorderCandidate(o1) && !isReorderCandidate(o2))
                {
                    return 1;
                }
                else if (!isReorderCandidate(o1) && isReorderCandidate(o2))
                {
                    return -1;
                }
                else if (isReorderCandidate(o1))
                {
                    return o1.getLabel().compareTo(o2.getLabel());
                }

                return 0;
            }

            public boolean isReorderCandidate(ColumnInfo col)
            {
                return col.isCalculated() && col.isUnselectable() && col.getFk() != null;
            }
        });

        for (ColumnInfo c : columns)
            table.addColumn(c);
    }

    public void setDefaultVisible(AbstractTableInfo table)
    {
        //this will reset default visible and force recalculation next time they are requested
        table.setDefaultVisibleColumns(null);
    }
}

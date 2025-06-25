package org.labkey.api.laboratory.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.data.TableInfo;
import org.labkey.api.laboratory.TabbedReportItem;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.view.template.ClientDependency;

import java.util.Collection;
import java.util.Map;

public interface TabbedReportFilterProvider
{
    boolean isAvailable(Container c, User u);

    Collection<ClientDependency> getClientDependencies();

    String getXType();

    String getLabel();

    String getInputValue();

    @NotNull
    Map<String, FieldKey> getAdditionalFieldKeys(TableInfo ti, TabbedReportItem tri, Map<String, FieldKey> overrides);
}

<%
/*
* Copyright (c) 2010-2012 LabKey Corporation
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
%>
<%@ page import="org.json.JSONObject" %>
<%@ page import="org.labkey.api.laboratory.query.TabbedReportFilterProvider" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.laboratory.LaboratoryServiceImpl" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("laboratory.context");
    }

    private String getFilterConfig()
    {
        StringBuilder ret = new StringBuilder();

        for (TabbedReportFilterProvider p : LaboratoryServiceImpl.get().getTabbedReportFilterProviderProviders(getContainer(), getUser()))
        {
            if (!p.isVisible(getContainer(), getUser()))
            {
                continue;
            }

            JSONObject config = new JSONObject();
            config.put("xtype", h(p.getXType()));
            config.put("inputValue", h(p.getInputValue()));
            config.put("label", h(p.getLabel()));

            ret.append(config).append(",");
        }

        return ret.toString();
    }
%>
<%
    JspView<?> me = HttpView.currentView();
    String wpId = "wp_" + me.getWebPartRowId();
%>

<div id="<%=h(wpId)%>"></div>

<script type="text/javascript" nonce="<%=getScriptNonce()%>">
    Ext4.onReady(function(){
        var webpartId = <%=q(wpId)%>;

        Ext4.define('Laboratory.panel.TabbedReportPanel', {
            extend: 'LDK.panel.TabbedReportPanel',

            initComponent: function(){
                Ext4.ns('Laboratory.tabbedReports');

                Ext4.apply(this, {
                    //defaultReport: 'abstract',
                    reportNamespace: Laboratory.tabbedReports
                });

                Ext4.Msg.wait('Loading...');
                Laboratory.Utils.getDataItems({
                    types: ['tabbedReports'],
                    scope: this,
                    success: this.onDataLoad,
                    failure: LDK.Utils.getErrorCallback()
                });

                this.callParent();
            },

            onDataLoad: function(results){
                Ext4.Msg.hide();
                this.reports = [];
                Ext4.each(results.tabbedReports, function(report){
                    LDK.Assert.assertNotEmpty('Tabbed Report is null', report);
                    if (report && report.key){
                        report.id = report.key.replace(/:/g, '_');
                        report.category = report.reportCategory;

                        if (report.targetContainer){
                            report.containerPath = report.targetContainer;
                        }
                        this.reports.push(report);
                    }
                }, this);

                this.reports = LDK.Utils.sortByProperty(this.reports, 'name', false);
                this.reports = LDK.Utils.sortByProperty(this.reports, 'reportCategory', false);

                this.createTabPanel();
            },

            filterTypes: [{
                xtype: 'ldk-singlesubjectfiltertype',
                inputValue: LDK.panel.SingleSubjectFilterType.filterName,
                label: LDK.panel.SingleSubjectFilterType.DEFAULT_LABEL
            },{
                xtype: 'ldk-multisubjectfiltertype',
                inputValue: LDK.panel.MultiSubjectFilterType.filterName,
                label: LDK.panel.MultiSubjectFilterType.label
            },<%=unsafe(getFilterConfig())%>{
                xtype: 'ldk-nofiltersfiltertype',
                inputValue: LDK.panel.NoFiltersFilterType.filterName,
                label: LDK.panel.NoFiltersFilterType.label
            }]
        });

        Ext4.create('Laboratory.panel.TabbedReportPanel').render(webpartId);
    });
</script>
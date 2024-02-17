/*
 * Copyright (c) 2012 LabKey Corporation
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

package org.labkey.ldk;

import jakarta.mail.Address;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.api.action.ApiResponse;
import org.labkey.api.action.ApiSimpleResponse;
import org.labkey.api.action.ConfirmAction;
import org.labkey.api.action.ExportAction;
import org.labkey.api.action.HasAllowBindParameter;
import org.labkey.api.action.MutatingApiAction;
import org.labkey.api.action.ReadOnlyApiAction;
import org.labkey.api.action.SimpleErrorView;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.CoreSchema;
import org.labkey.api.data.DbSequenceManager;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlScriptRunner;
import org.labkey.api.data.TableInfo;
import org.labkey.api.ldk.LDKService;
import org.labkey.api.ldk.notification.Notification;
import org.labkey.api.ldk.notification.NotificationService;
import org.labkey.api.module.AllowedDuringUpgrade;
import org.labkey.api.module.ModuleHtmlView;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.QueryAction;
import org.labkey.api.query.QueryForm;
import org.labkey.api.query.QueryParseException;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.QueryWebPart;
import org.labkey.api.security.PrincipalType;
import org.labkey.api.security.RequiresNoPermission;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.SecurityManager;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.ValidEmail;
import org.labkey.api.security.permissions.AdminOperationsPermission;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.settings.AppProps;
import org.labkey.api.settings.LookAndFeelProperties;
import org.labkey.api.util.ConfigurationException;
import org.labkey.api.util.GUID;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Path;
import org.labkey.api.util.StringUtilsLabKey;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.Portal;
import org.labkey.api.view.RedirectException;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.api.view.WebPartFactory;
import org.labkey.api.view.WebPartView;
import org.labkey.ldk.notification.NotificationServiceImpl;
import org.labkey.ldk.sql.LDKNaturalizeInstallationManager;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class LDKController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(LDKController.class);
    private static final Logger _log = LogManager.getLogger(LDKController.class);

    public LDKController()
    {
        setActionResolver(_actionResolver);
    }

    @RequiresPermission(ReadPermission.class)
    public class GetNotificationsAction extends ReadOnlyApiAction<Object>
    {
        @Override
        public ApiResponse execute(Object form, BindException errors)
        {
            Map<String, Object> result = new HashMap<>();
            Set<Notification> set = NotificationServiceImpl.get().getNotifications(getContainer(), false);
            JSONArray notificationJson = new JSONArray();
            for (Notification n : set)
            {
                notificationJson.put(NotificationServiceImpl.get().getJson(n, getContainer(), getUser()));
            }
            result.put("notifications", notificationJson);
            User u = NotificationServiceImpl.get().getUser(getContainer());
            result.put("serviceEnabled", NotificationServiceImpl.get().isServiceEnabled());
            result.put("notificationUser", u == null ? null : u.getEmail());
            Address replyEmail = NotificationServiceImpl.get().getReturnEmail(getContainer());
            result.put("replyEmail", replyEmail == null ? null : replyEmail.toString());
            result.put("success", true);

            return new ApiSimpleResponse(result);
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class GetFileRootSizesAction extends ReadOnlyApiAction<FileRootSizeForm>
    {
        @Override
        public ApiResponse execute(FileRootSizeForm form, BindException errors) throws Exception
        {
            Map<String, Object> result = new HashMap<>();
            JSONArray ret = new JSONArray();
            Container c = getContainer();

            ret.put(LDKService.get().getContainerSizeJson(getContainer(), getUser(), form.getIncludeAllRoots(), form.getIncludeFileCounts()));

            if (form.getShowChildren())
            {
                for (Container child : c.getChildren())
                {
                    if (!child.isWorkbook() || form.getIncludeWorkbooks())
                        ret.put(LDKService.get().getContainerSizeJson(child, getUser(), form.getIncludeAllRoots(), form.getIncludeFileCounts()));
                }
            }

            result.put("fileRoots", ret);
            result.put("success", true);

            return new ApiSimpleResponse(result);
        }
    }

    public static class FileRootSizeForm
    {
        private Boolean _includeAllRoots = true;
        private Boolean _includeWorkbooks = false;
        private Boolean _includeFileCounts = false;
        private Boolean _showChildren = false;

        public Boolean getIncludeWorkbooks()
        {
            return _includeWorkbooks;
        }

        public void setIncludeWorkbooks(Boolean includeWorkbooks)
        {
            _includeWorkbooks = includeWorkbooks;
        }

        public Boolean getShowChildren()
        {
            return _showChildren;
        }

        public void setShowChildren(Boolean showChildren)
        {
            _showChildren = showChildren;
        }

        public Boolean getIncludeAllRoots()
        {
            return _includeAllRoots;
        }

        public void setIncludeAllRoots(Boolean includeAllRoots)
        {
            _includeAllRoots = includeAllRoots;
        }

        public Boolean getIncludeFileCounts()
        {
            return _includeFileCounts;
        }

        public void setIncludeFileCounts(Boolean includeFileCounts)
        {
            _includeFileCounts = includeFileCounts;
        }
    }

    @RequiresPermission(AdminOperationsPermission.class)
    public class GetSiteNotificationDetailsAction extends ReadOnlyApiAction<Object>
    {
        @Override
        public ApiResponse execute(Object form, BindException errors) throws Exception
        {
            Map<String, Object> result = new HashMap<>();

            result.put("serviceEnabled", NotificationServiceImpl.get().isServiceEnabled());

            Set<Notification> set = NotificationServiceImpl.get().getNotifications(ContainerManager.getRoot(), true);
            JSONArray notificationJson = new JSONArray();
            for (Notification n : set)
            {
                JSONObject json = NotificationServiceImpl.get().getJson(n, getContainer(), getUser());
                List<Map<String, Object>> containers = new ArrayList<>();
                for (Container c : NotificationServiceImpl.get().getActiveContainers(n))
                {
                    containers.add(c.toJSON(getUser()));
                }
                json.put("containers", containers);
                notificationJson.put(json);
            }
            result.put("notifications", notificationJson);
            result.put("success", true);

            return new ApiSimpleResponse(result);
        }
    }

    @RequiresPermission(UpdatePermission.class)
    public class UpdateNotificationSubscriptionsAction extends MutatingApiAction<UpdateNotificationSubscriptionsForm>
    {
        @Override
        public ApiResponse execute(UpdateNotificationSubscriptionsForm form, BindException errors) throws Exception
        {
            if (form.getKey() == null)
            {
                errors.reject(ERROR_MSG, "No notification provided");
                return null;
            }

            Notification n = NotificationService.get().getNotification(form.getKey());
            if (n == null)
            {
                errors.reject(ERROR_MSG, "Unknown notification: " + form.getKey());
                return null;
            }

            List<UserPrincipal> toAdd = new ArrayList<>();
            if (form.getToAdd() != null)
            {
                for (Integer userId : form.getToAdd())
                {
                    UserPrincipal up = SecurityManager.getPrincipal(userId);
                    if (up == null)
                    {
                        errors.reject(ERROR_MSG, "Unknown user or group: " + userId);
                        return null;
                    }

                    if (!getUser().equals(up) && !getContainer().hasPermission(getUser(), AdminPermission.class))
                    {
                        throw new UnauthorizedException("Only admins can modify subscriptions for users besides their own user");
                    }

                    toAdd.add(up);
                }
            }

            List<UserPrincipal> toRemove = new ArrayList<>();
            if (form.getToRemove() != null)
            {
                for (Integer userId : form.getToRemove())
                {
                    UserPrincipal up = SecurityManager.getPrincipal(userId);
                    if (up == null)
                    {
                        errors.reject(ERROR_MSG, "Unknown user or group: " + userId);
                        return null;
                    }

                    if (!getUser().equals(up) && !getContainer().hasPermission(getUser(), AdminPermission.class))
                    {
                        throw new UnauthorizedException("Only admins can modify subscriptions for users besides their own user");
                    }

                    toRemove.add(up);
                }
            }

            NotificationServiceImpl.get().updateSubscriptions(getContainer(), getUser(), n, toAdd, toRemove);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);

            return new ApiSimpleResponse(result);
        }
    }

    public static class UpdateNotificationSubscriptionsForm
    {
        private String _key;
        private Integer[] _toAdd;
        private Integer[] _toRemove;

        public String getKey()
        {
            return _key;
        }

        public void setKey(String key)
        {
            _key = key;
        }

        public Integer[] getToAdd()
        {
            return _toAdd;
        }

        public void setToAdd(Integer[] toAdd)
        {
            _toAdd = toAdd;
        }

        public Integer[] getToRemove()
        {
            return _toRemove;
        }

        public void setToRemove(Integer[] toRemove)
        {
            _toRemove = toRemove;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class RunNotificationAction extends SimpleViewAction<RunNotificationForm>
    {
        private String _title = null;

        @Override
        public ModelAndView getView(RunNotificationForm form, BindException errors) throws Exception
        {
            if (form.getKey() == null)
            {
                errors.reject(ERROR_MSG, "No notification provided");
                return null;
            }

            Notification n = NotificationService.get().getNotification(form.getKey());
            if (n == null)
            {
                errors.reject(ERROR_MSG, "Unknown notification: " + form.getKey());
                return null;
            }

            if (!n.isAvailable(getContainer()))
            {
                return new HtmlView("The notification " + form.getKey() + " is not available in this container");
            }

            _title = n.getName();

            StringBuilder sb = new StringBuilder();
            Date lastRun = NotificationServiceImpl.get().getLastRunDate(n);
            SimpleDateFormat df = new SimpleDateFormat(LookAndFeelProperties.getInstance(getContainer()).getDefaultDateTimeFormat());
            sb.append("The notification email was last sent on: " + (lastRun == null ? "never" : df.format(lastRun)) + "<p><hr><p>");

            User u = NotificationServiceImpl.get().getUser(getContainer());
            if (u == null)
            {
                sb.append("The notification service user is not configured in this container, so the notification cannot run");
            }
            else
            {
                String msg = NotificationServiceImpl.get().getMessage(n, getContainer());
                sb.append(msg == null ? "The notification did not produce a message" : msg);
            }

            return new HtmlView(sb.toString());
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild(_title == null ? "Notification" : _title);
        }
    }

    @RequiresPermission(AdminPermission.class)
    public class SendNotificationAction extends MutatingApiAction<RunNotificationForm>
    {
        @Override
        public ApiResponse execute(RunNotificationForm form, BindException errors) throws Exception
        {
            Map<String, Object> result = new HashMap<>();

            if (form.getKey() == null)
            {
                errors.reject(ERROR_MSG, "No notification provided");
                return null;
            }

            Notification n = NotificationService.get().getNotification(form.getKey());
            if (n == null)
            {
                errors.reject(ERROR_MSG, "Unknown notification: " + form.getKey());
                return null;
            }

            if (!n.isAvailable(getContainer()))
            {
                errors.reject(ERROR_MSG, "The notification " + form.getKey() + " is not available in this container");
                return null;
            }

            NotificationServiceImpl.get().runForContainer(n, getContainer());

            result.put("success", true);
            return new ApiSimpleResponse(result);
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ValidateContainerScopedTablesAction extends SimpleViewAction<Object>
    {
        @Override
        public ModelAndView getView(Object form, BindException errors) throws Exception
        {
            LDKServiceImpl service = (LDKServiceImpl)LDKServiceImpl.get();
            List<String> messages = service.validateContainerScopedTables(false);

            String sb = "This page is designed to inspect all registered container scoped tables and report any tables with duplicate keys in the same container.  This should be enforced by the user schema; however, direct DB inserts will bypass this check.<p>" +
                    StringUtils.join(messages, "<br>");

            return new HtmlView(sb);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Validate Container Scoped Tables");
        }
    }

    @RequiresPermission(AdminPermission.class)
    public class GetNotificationSubscriptionsAction extends ReadOnlyApiAction<RunNotificationForm>
    {
        @Override
        public ApiResponse execute(RunNotificationForm form, BindException errors) throws Exception
        {
            Map<String, Object> result = new HashMap<>();

            if (form.getKey() == null)
            {
                errors.reject(ERROR_MSG, "No notification provided");
                return null;
            }

            Notification n = NotificationService.get().getNotification(form.getKey());
            if (n == null)
            {
                errors.reject(ERROR_MSG, "Unknown notification: " + form.getKey());
                return null;
            }

            if (!n.isAvailable(getContainer()))
            {
                errors.reject(ERROR_MSG, "The notification " + form.getKey() + " is not available in this container");
                return null;
            }

            List<Map<String, String>> ret = new ArrayList<>();
            for (UserPrincipal up : NotificationServiceImpl.get().getRecipients(n, getContainer()))
            {
                Map<String, String> map = new HashMap<>();
                map.put("type", up.getType());
                map.put("userId", String.valueOf(up.getUserId()));
                map.put("name", up.getName());
                if (up.getPrincipalType().equals(PrincipalType.USER))
                {
                    map.put("email", ((User)up).getEmail());
                }

                ret.add(map);
            }

            ret.sort(Comparator.comparing(o -> o.get("name")));

            result.put("subscriptions", ret);
            result.put("success", true);
            return new ApiSimpleResponse(result);
        }
    }

    @RequiresPermission(AdminPermission.class)
    public class SetNotificationSettingsAction extends MutatingApiAction<NotificationSettingsForm>
    {
        @Override
        public ApiResponse execute(NotificationSettingsForm form, BindException errors)
        {
            try
            {
                if (getContainer().isRoot())
                {
                    if (form.getEnabled() != null)
                    {
                        NotificationServiceImpl.get().setServiceEnabled(form.getEnabled());
                    }
                }

                if (form.getReplyEmail() != null)
                {
                    try
                    {
                        ValidEmail e = new ValidEmail(form.getReplyEmail());
                        NotificationServiceImpl.get().setReturnEmail(getContainer(), e.getEmailAddress());
                    }
                    catch (ValidEmail.InvalidEmailException e)
                    {
                        errors.reject(ERROR_MSG, "Invalid email: " + form.getReplyEmail());
                        return null;
                    }
                }

                if (form.getUser() != null)
                {
                    try
                    {
                        User u = UserManager.getUser(new ValidEmail(form.getUser()));
                        if (u == null)
                        {
                            errors.reject(ERROR_MSG, "Unknown user: " + form.getUser());
                            return null;
                        }

                        if (!getContainer().hasPermission(u, AdminPermission.class))
                        {
                            errors.reject(ERROR_MSG, "User must have admin permission in this container");
                            return null;
                        }

                        NotificationServiceImpl.get().setUser(getContainer(), u.getUserId());
                    }
                    catch (ValidEmail.InvalidEmailException e)
                    {
                        errors.reject(ERROR_MSG, "Invalid email: " + form.getUser());
                        return null;
                    }
                }

                if (form.getNotifications() != null)
                {
                    JSONObject notifications = new JSONObject(form.getNotifications());
                    for (String key : notifications.keySet())
                    {
                        Notification n = NotificationServiceImpl.get().getNotification(key);
                        if (n == null)
                        {
                            errors.reject(ERROR_MSG, "Unknown notification: " + key);
                            return null;
                        }

                        NotificationServiceImpl.get().setActive(n, getContainer(), notifications.getBoolean(key));
                    }
                }
            }
            catch (ConfigurationException e)
            {
                errors.reject(ERROR_MSG, e.getMessage());
                return null;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return new ApiSimpleResponse(result);
        }
    }

    public static class NotificationSettingsForm implements HasAllowBindParameter
    {
        private String _replyEmail;
        private String _user;
        private String _notifications;
        private Boolean _enabled;

        public String getReplyEmail()
        {
            return _replyEmail;
        }

        public void setReplyEmail(String replyEmail)
        {
            _replyEmail = replyEmail;
        }

        public String getUser()
        {
            return _user;
        }

        public void setUser(String user)
        {
            _user = user;
        }

        public String getNotifications()
        {
            return _notifications;
        }

        public void setNotifications(String notifications)
        {
            _notifications = notifications;
        }

        public Boolean getEnabled()
        {
            return _enabled;
        }

        public void setEnabled(Boolean enabled)
        {
            _enabled = enabled;
        }

        @Override
        public Predicate<String> allowBindParameter()
        {
            return (name) -> HasAllowBindParameter.getDefaultPredicate().test(name) || "user".equals(name);
        }
    }


    @RequiresPermission(ReadPermission.class)
    public class LogMetricAction extends MutatingApiAction<LogMetricForm>
    {
        @Override
        public ApiResponse execute(LogMetricForm form, BindException errors) throws Exception
        {
            Map<String, Object> result = new HashMap<>();

            if(form.getMetricName() == null)
            {
                errors.reject("No metric name provided");
                return null;
            }

            LDKServiceImpl.PerfMetricModel model = new LDKServiceImpl.PerfMetricModel();
            model.setCategory(form.getCategory());
            model.setMetricName(form.getMetricName());
            model.setNumericValue1(form.getNumericValue2());
            model.setNumericValue2(form.getNumericValue2());
            model.setNumericValue3(form.getNumericValue3());
            model.setStringValue1(form.getStringValue1());
            model.setStringValue2(form.getStringValue2());
            model.setStringValue3(form.getStringValue3());
            model.setReferrerURL(form.getReferrerURL());
            model.setBrowser(form.getBrowser());
            model.setPlatform(form.getPlatform());

            ((LDKServiceImpl)LDKServiceImpl.get()).logPerfMetric(getContainer(), getUser(), model);

            result.put("success", true);
            return new ApiSimpleResponse(result);
        }
    }

    public static class LogMetricForm
    {
        String _category;
        String _metricName;
        String _stringValue1;
        String _stringValue2;
        String _stringValue3;
        Double _numericValue1;
        Double _numericValue2;
        Double _numericValue3;
        String _referrerURL;
        String _platform;
        String _browser;

        public String getCategory()
        {
            return _category;
        }

        public void setCategory(String category)
        {
            _category = category;
        }

        public String getMetricName()
        {
            return _metricName;
        }

        public void setMetricName(String metricName)
        {
            _metricName = metricName;
        }

        public String getStringValue1()
        {
            return _stringValue1;
        }

        public void setStringValue1(String stringValue1)
        {
            _stringValue1 = stringValue1;
        }

        public String getStringValue2()
        {
            return _stringValue2;
        }

        public void setStringValue2(String stringValue2)
        {
            _stringValue2 = stringValue2;
        }

        public String getStringValue3()
        {
            return _stringValue3;
        }

        public void setStringValue3(String stringValue3)
        {
            _stringValue3 = stringValue3;
        }

        public Double getNumericValue1()
        {
            return _numericValue1;
        }

        public void setNumericValue1(Double numericValue1)
        {
            _numericValue1 = numericValue1;
        }

        public Double getNumericValue2()
        {
            return _numericValue2;
        }

        public void setNumericValue2(Double numericValue2)
        {
            _numericValue2 = numericValue2;
        }

        public Double getNumericValue3()
        {
            return _numericValue3;
        }

        public void setNumericValue3(Double numericValue3)
        {
            _numericValue3 = numericValue3;
        }

        public String getReferrerURL()
        {
            return _referrerURL;
        }

        public void setReferrerURL(String referrerURL)
        {
            _referrerURL = referrerURL;
        }

        public String getPlatform()
        {
            return _platform;
        }

        public void setPlatform(String platform)
        {
            _platform = platform;
        }

        public String getBrowser()
        {
            return _browser;
        }

        public void setBrowser(String browser)
        {
            _browser = browser;
        }
    }

    public static class RunNotificationForm
    {
        private String _key;

        public String getKey()
        {
            return _key;
        }

        public void setKey(String key)
        {
            _key = key;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class UpdateQueryAction extends SimpleViewAction<QueryForm>
    {
        private QueryForm _form;

        @Override
        public ModelAndView getView(QueryForm form, BindException errors) throws Exception
        {
            form.ensureQueryExists();

            _form = form;

            String schemaName = form.getSchemaName();
            String queryName = form.getQueryName();

            QueryView queryView = QueryView.create(form, errors);
            TableInfo ti = queryView.getTable();
            List<String> pks = ti.getPkColumnNames();
            String keyField = null;
            if (pks.size() == 1)
                keyField = pks.get(0);

            ActionURL url = getViewContext().getActionURL().clone();

            if (keyField != null)
            {
                DetailsURL importUrl = DetailsURL.fromString("/query/importData.view?schemaName=" + schemaName + "&query.queryName=" + queryName + "&keyField=" + keyField);
                importUrl.setContainerContext(getContainer());

                DetailsURL updateUrl = DetailsURL.fromString("/ldk/manageRecord.view?schemaName=" + schemaName + "&query.queryName=" + queryName + "&keyField=" + keyField + "&key=${" + keyField + "}");
                updateUrl.setContainerContext(getContainer());

                DetailsURL deleteUrl = DetailsURL.fromString("/query/deleteQueryRows.view?schemaName=" + schemaName + "&query.queryName=" + queryName);
                deleteUrl.setContainerContext(getContainer());

                url.addParameter("importURL", importUrl.toString());
                url.addParameter("updateURL", updateUrl.toString());
                url.addParameter("deleteURL", deleteUrl.toString());
                url.addParameter("showInsertNewButton", false);
                url.addParameter("dataRegionName", "query");
            }

            url.addParameter("queryName", queryName);
            url.addParameter("allowChooseQuery", false);

            WebPartFactory factory = Portal.getPortalPartCaseInsensitive("Query");
            Portal.WebPart part = factory.createWebPart();
            part.setProperties(url.getQueryString());

            QueryWebPart qwp = new QueryWebPart(getViewContext(), part);
            qwp.setTitle(ti.getTitle());
            qwp.setFrame(WebPartView.FrameType.NONE);
            return qwp;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            TableInfo ti = null;
            try
            {
                ti = _form.getSchema() == null ? null : _form.getSchema().getTable(_form.getQueryName());
            }
            catch (QueryParseException x)
            {
                /* */
            }

            root.addChild(ti == null ? _form.getQueryName() : ti.getTitle(), _form.urlFor(QueryAction.executeQuery));
        }
    }

    @RequiresPermission(AdminPermission.class)
    public static class MoveWorkbookAction extends ConfirmAction<MoveWorkbookForm>
    {
        private Container _movedWb = null;

        @Override
        public void validateCommand(MoveWorkbookForm form, Errors errors)
        {

        }

        @Override
        public ModelAndView getConfirmView(MoveWorkbookForm form, BindException errors) throws Exception
        {
            if (!getContainer().isWorkbook())
            {
                errors.reject(ERROR_MSG, "This is only supported for workbooks");
                return new SimpleErrorView(errors);
            }

            String sb = "This will move this workbook to the selected folder, renaming this workbook to match the series in that container.  Note: there are many reasons this can be problematic, so please do this with great care<p>" +
                    "<input name=\"targetContainer\" type=\"text\"></input>";

            return new HtmlView(sb);
        }

        @Override
        public boolean handlePost(MoveWorkbookForm form, BindException errors) throws Exception
        {
            Container toMove = getContainer();
            if (!toMove.isWorkbook())
            {
                errors.reject(ERROR_MSG, "This is only supported for workbooks");
                return false;
            }

            if (StringUtils.trimToNull(form.getTargetContainer()) == null)
            {
                errors.reject(ERROR_MSG, "Must provide target container");
                return false;
            }

            Container target = ContainerManager.getForPath(StringUtils.trimToNull(form.getTargetContainer()));
            if (target == null)
            {
                target = ContainerManager.getForId(StringUtils.trimToNull(form.getTargetContainer()));
            }

            if (target == null)
            {
                errors.reject(ERROR_MSG, "Unknown container: " + form.getTargetContainer());
                return false;
            }

            if (target.isWorkbook())
            {
                errors.reject(ERROR_MSG, "Target cannot be a workbook: " + form.getTargetContainer());
                return false;
            }

            if (ContainerManager.isSystemContainer(target))
            {
                errors.reject(ERROR_MSG, "Cannot move to system containers: " + form.getTargetContainer());
                return false;
            }

            if (target.equals(toMove.getParent()))
            {
                errors.reject(ERROR_MSG, "Cannot move the workbook to its current parent: " + form.getTargetContainer());
                return false;
            }

            //NOTE: transaction causing problems for larger sites?
            //try (DbScope.Transaction transaction = CoreSchema.getInstance().getSchema().getScope().ensureTransaction())
            //{
                //first rename workbook to make unique
                String tempName = new GUID().toString();
                int sortOrder = (int)DbSequenceManager.get(target, ContainerManager.WORKBOOK_DBSEQUENCE_NAME).next();
                _log.info("renaming workbook to in preparation for move from: " + toMove.getPath() + "  to: " + tempName);
                ContainerManager.rename(toMove, getUser(), tempName);
                toMove = ContainerManager.getForId(toMove.getId());

                //then move parent
                _log.info("moving workbook from: " + toMove.getPath() + "  to: " + target.getPath());
                ContainerManager.move(toMove, target, getUser());
                toMove = ContainerManager.getForId(toMove.getId());

                //finally move to correct name
                _log.info("renaming workbook from: " + toMove.getPath() + "  to: " + sortOrder);
                ContainerManager.rename(toMove, getUser(), String.valueOf(sortOrder));
                toMove.setSortOrder(sortOrder);
                new SqlExecutor(CoreSchema.getInstance().getSchema()).execute("UPDATE core.containers SET SortOrder = ? WHERE EntityId = ?", toMove.getSortOrder(), toMove.getId());
                toMove = ContainerManager.getForId(toMove.getId());

                //transaction.commit();
                _log.info("workbook move finished");

                _movedWb = toMove;
            //}

            return true;
        }

        @NotNull
        @Override
        public URLHelper getSuccessURL(MoveWorkbookForm moveWorkbookForm)
        {
            if (_movedWb == null)
                return getContainer().getStartURL(getUser());
            else
                return _movedWb.getStartURL(getUser());
        }
    }

    public static class MoveWorkbookForm
    {
        private String _targetContainer;

        public String getTargetContainer()
        {
            return _targetContainer;
        }

        public void setTargetContainer(String targetContainer)
        {
            _targetContainer = targetContainer;
        }
    }

    @RequiresNoPermission
    public class RedirectStartAction extends SimpleViewAction<Object>
    {
        @Override
        public ModelAndView getView(Object form, BindException errors) throws Exception
        {
            if (getContainer().hasPermission(getUser(), AdminPermission.class))
            {
                return ModuleHtmlView.get(ModuleLoader.getInstance().getModule(LDKModule.class), "setRedirectUrl");
            }
            else
            {
                String urlString = PropertyManager.getProperties(getContainer(), REDIRECT_URL_DOMAIN).get(REDIRECT_URL_PROP);
                if (urlString == null)
                {
                    return new HtmlView("This folder is only visible to admins");
                }
                else
                {
                    try
                    {
                        URLHelper url = new URLHelper(urlString);
                        Path path = url.getParsedPath();
                        if (path.startsWith(AppProps.getInstance().getParsedContextPath()))
                            path = path.subpath(1, path.size());

                        throw new RedirectException(AppProps.getInstance().getParsedContextPath() + path.toString(null, null));
                    }
                    catch (URISyntaxException e)
                    {
                        return new HtmlView("Invalid redirect URL set: " + urlString);
                    }
                }
            }
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Home");
        }
    }

    public static final String REDIRECT_URL_DOMAIN = "org.labkey.ldk.redirectSettings";
    public static final String REDIRECT_URL_PROP = "redirectURL";

    @RequiresPermission(AdminPermission.class)
    public class SetRedirectUrlAction extends MutatingApiAction<SetRedirectUrlForm>
    {
        @Override
        public ApiResponse execute(SetRedirectUrlForm form, BindException errors) throws Exception
        {
            if (StringUtils.isEmpty(form.getUrl()))
            {
                errors.reject(ERROR_MSG, "No URL Provided");
                return null;
            }

            PropertyManager.PropertyMap props = PropertyManager.getWritableProperties(getContainer(), REDIRECT_URL_DOMAIN, true);
            props.put(REDIRECT_URL_PROP, StringUtils.trimToNull(form.getUrl()));
            props.save();

            return new ApiSimpleResponse("success", true);
        }
    }

    public static class SetRedirectUrlForm
    {
        String _url;

        public String getUrl()
        {
            return _url;
        }

        public void setUrl(String url)
        {
            _url = url;
        }
    }

    @RequiresPermission(AdminPermission.class)
    public class GetRedirectUrlAction extends ReadOnlyApiAction<Object>
    {
        @Override
        public ApiResponse execute(Object form, BindException errors) throws Exception
        {
            return new ApiSimpleResponse("url", PropertyManager.getProperties(getContainer(), REDIRECT_URL_DOMAIN).get(REDIRECT_URL_PROP));
        }
    }

    @RequiresPermission(AdminOperationsPermission.class)
    @AllowedDuringUpgrade
    public class DownloadNaturalizeInstallScriptAction extends ExportAction<Object>
    {
        @Override
        public void export(Object o, HttpServletResponse response, BindException errors) throws Exception
        {
            SqlScriptRunner.SqlScript installScript = LDKNaturalizeInstallationManager.get().getInstallScript();
            response.setCharacterEncoding(StringUtilsLabKey.DEFAULT_CHARSET.name());
            PageFlowUtil.streamFileBytes(response, "naturalizeInstall.sql", installScript.getContents().getBytes(StringUtilsLabKey.DEFAULT_CHARSET), true);
        }
    }
}

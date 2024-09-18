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
package org.labkey.laboratory.assay;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.vfs2.FileObject;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.assay.AssayProvider;
import org.labkey.api.assay.AssayRunUploadContext;
import org.labkey.api.assay.AssayService;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.Container;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.qc.DefaultTransformResult;
import org.labkey.api.qc.TransformResult;
import org.labkey.api.security.User;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This primariy serves as a shim between the laboratory assay import pathway and the core.
 * @param <ProviderType>
 */
public class RunUploadContext<ProviderType extends AssayProvider> implements AssayRunUploadContext<ProviderType>
{
    private final ExpProtocol _protocol;
    private final ProviderType _providerType;
    private final String _name;
    private final String _comments;
    private final Map<String, String> _runProperties;
    private final Map<String, String> _batchProperties;
    private final ViewContext _ctx;

    private TransformResult _transformResult;
    private final Map<String, FileObject> _uploadedData;

    public RunUploadContext(ExpProtocol protocol, ProviderType providerType, String name, String comments, Map<String, String> runProperties, Map<String, String> batchProperties, ViewContext ctx, Map<String, FileObject> uploadedData)
    {
        _protocol = protocol;
        _providerType = providerType;
        _name = name;
        _comments = comments;
        _runProperties = new CaseInsensitiveHashMap<>(runProperties);
        _batchProperties = new CaseInsensitiveHashMap<>(batchProperties);
        _ctx = ctx;
        _uploadedData = uploadedData;
    }

    @Override
    @NotNull
    public ExpProtocol getProtocol()
    {
        return _protocol;
    }

    @Override
    public Map<DomainProperty, String> getRunProperties()
    {
        if (_runProperties != null)
        {
            AssayProvider provider = AssayService.get().getProvider(getProtocol());
            Map<DomainProperty, String> props = new HashMap<DomainProperty, String>();


            for (DomainProperty dp : provider.getRunDomain(getProtocol()).getProperties())
            {
                if (_runProperties.containsKey(dp.getName()))
                {
                    //TODO: talk to josh about this conversion
                    Object value = _runProperties.get(dp.getName());
                    props.put(dp, value == null ? null : String.valueOf(value));
                }
            }
            return Collections.unmodifiableMap(props);
        }
        return null;
    }

    @Override
    public Map<DomainProperty, String> getBatchProperties()
    {
        if (_batchProperties != null)
        {
            AssayProvider provider = AssayService.get().getProvider(getProtocol());
            Map<DomainProperty, String> props = new HashMap<DomainProperty, String>();

            for (DomainProperty dp : provider.getBatchDomain(getProtocol()).getProperties())
            {
                if (_batchProperties.containsKey(dp.getName()))
                    props.put(dp, _batchProperties.get(dp.getName()));
            }
            return Collections.unmodifiableMap(props);
        }
        return null;
    }

    @Override
    public String getComments()
    {
        return _comments;
    }

    @Override
    public String getName()
    {
        return _name;
    }

    @Override
    public User getUser()
    {
        return _ctx.getUser();
    }

    @Override
    @NotNull
    public Container getContainer()
    {
        return _ctx.getContainer();
    }

    @Override
    public HttpServletRequest getRequest()
    {
        return _ctx.getRequest();
    }

    @Override
    public ActionURL getActionURL()
    {
        return _ctx.getActionURL();
    }

    @Override
    @NotNull
    public Map<String, FileObject> getUploadedData() throws ExperimentException
    {
        return _uploadedData;
    }

    @NotNull
    @Override
    public Map<Object, String> getInputDatas()
    {
        return Collections.emptyMap();
    }

    @Override
    public ProviderType getProvider()
    {
        return _providerType;
    }

    @Override
    public String getTargetStudy()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public TransformResult getTransformResult()
    {
        return _transformResult == null ? DefaultTransformResult.createEmptyResult() :_transformResult;
    }

    @Override
    public void setTransformResult(TransformResult result)
    {
        _transformResult = result;
    }

    /**
     * The RowId for the run that is being deleted and reuploaded, or null if this is a new run
     */
    @Override
    public Integer getReRunId()
    {
        return null;
    }

    @Override
    public void uploadComplete(ExpRun run)
    {
    }

	@Override
    public Logger getLogger()
	{
		return null;
    }
}

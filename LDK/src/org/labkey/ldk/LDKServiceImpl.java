package org.labkey.ldk;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.api.data.AbstractTableInfo;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.TableCustomizer;
import org.labkey.api.files.FileContentService;
import org.labkey.api.ldk.LDKService;
import org.labkey.api.security.User;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.ldk.query.BuiltInColumnsCustomizer;
import org.labkey.ldk.query.ColumnOrderCustomizer;
import org.labkey.ldk.query.DefaultTableCustomizer;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: bimber
 * Date: 11/4/12
 * Time: 3:47 PM
 */
public class LDKServiceImpl extends LDKService
{
    public LDKServiceImpl()
    {

    }

    public TableCustomizer getDefaultTableCustomizer()
    {
        return new DefaultTableCustomizer();
    }

    public TableCustomizer getBuiltInColumnsCustomizer(boolean disableFacetingForNumericCols)
    {
        BuiltInColumnsCustomizer ret = new BuiltInColumnsCustomizer();
        ret.setDisableFacetingForNumericCols(disableFacetingForNumericCols);

        return ret;
    }

    public TableCustomizer getColumnsOrderCustomizer()
    {
        return new ColumnOrderCustomizer();
    }

    public Map<String, Object> getContainerSizeJson(Container c, User u, boolean includeAllRootTypes, boolean includeFileCount)
    {
        FileContentService svc = ServiceRegistry.get().getService(FileContentService.class);
        Map<String, Object> json = c.toJSON(u);

        JSONArray fileRoots = new JSONArray();

        //primary root
        File root = svc.getFileRoot(c);
        if (root != null && root.exists())
        {
            fileRoots.put(getJSONForRoot(root, "root", includeFileCount));
        }

        //append children
        if (includeAllRootTypes)
        {
            Set<File> paths = new HashSet<File>();
            for (FileContentService.ContentType type : FileContentService.ContentType.values())
            {
                File fileRoot = svc.getFileRoot(c, type);
                if (fileRoot != null && fileRoot.exists())
                {
                    if (paths.contains(fileRoot))
                        continue;

                    paths.add(fileRoot);
                    fileRoots.put(getJSONForRoot(fileRoot, type.name(), includeFileCount));
                }
            }
        }

        json.put("fileRoots", fileRoots);

        return json;
    }

    private JSONObject getJSONForRoot(File fileRoot, String name, boolean includeFileCount)
    {
        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("rootPath", fileRoot.getPath());
        long size = FileUtils.sizeOfDirectory(fileRoot);
        obj.put("rootSizeInt", size);
        obj.put("rootSize", FileUtils.byteCountToDisplaySize(size));
        if (includeFileCount)
            obj.put("totalFiles", getFileCount(fileRoot));

        return obj;
    }

    private int getFileCount(File file)
    {
        if (!file.isDirectory())
        {
            return 1;
        }

        int count = 0;
        File[] files = file.listFiles();
        if (files != null)
        {
            for (File f: files)
            {
                count += getFileCount(f);
            }
        }
        return count;
    }

    public void applyNaturalSort(AbstractTableInfo ti, String colName)
    {
        DefaultTableCustomizer.applyNaturalSort(ti, colName);
    }
}

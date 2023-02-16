/*
 * Copyright (c) 2012-2016 LabKey Corporation
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
package org.labkey.test.tests.external.labModules;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.Sort;
import org.labkey.remoteapi.security.CreateContainerResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.External;
import org.labkey.test.categories.LabModule;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.ext4.ComboBox;
import org.labkey.test.components.ext4.RadioButton;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.AdvancedSqlTest;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExcelHelper;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.ext4cmp.Ext4CmpRef;
import org.labkey.test.util.ext4cmp.Ext4ComboRef;
import org.labkey.test.util.ext4cmp.Ext4FieldRef;
import org.labkey.test.util.external.labModules.LabModuleHelper;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Contains a series of tests designed to test the UI in the laboratory module.
 * Also contains considerable coverage of Ext4 components and the client API
 */
@Category({External.class, LabModule.class})
public class LabModulesTest extends BaseWebDriverTest implements AdvancedSqlTest
{
    protected LabModuleHelper _helper = new LabModuleHelper(this);
    protected APIContainerHelper _apiContainerHelper = new APIContainerHelper(this);

    protected String PROJECT_NAME = "LaboratoryVerifyProject" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    private final String GENOTYPING_ASSAYNAME = "Genotyping Assay Test";

    private final String DATA_SOURCE = "Source_" + replaceNonVisibleChars(getProjectName());
    private final String SUBJECT_LIST = "Subject List_" + replaceNonVisibleChars(getProjectName());
    private final String ELISPOT_SOURCE = "ELISPOT_" + replaceNonVisibleChars(getProjectName());
    private final String GROUP_SOURCE = "GroupsQuery_" + replaceNonVisibleChars(getProjectName());
    private final String REMOVED_SOURCE = "To Remove_" + replaceNonVisibleChars(getProjectName());

    private int _oligosTotal = 0;
    private int _samplesTotal = 0;
    private int _peptideTotal = 0;

    protected static final String IMPORT_DATA_TEXT = "Import Data";

    private static final String[][] SUBJECTS = new String[][]{
        {"Participant0001","m","02/03/2001", "02/03/2012"},
        {"Participant0002","f","02/04/2001", null},
        {"Participant0003","f","02/05/2001", null},
        {"Participant0004","f","02/06/2001", null},
        {"Participant0005","m","02/07/2001", null},
        {"Participant0006","m","02/08/2002", null},
        {"Participant0007","m","02/09/2001", null},
        {"Participant0008","f","02/10/2001", null},
        {"Participant0009","m","02/11/2001", null},
        {"Participant0010","m","02/12/2001", null},
        {"Participant0011","f","02/13/2001", null},
        {"Participant0012","f","02/14/2001", null},
        {"Participant0013","f","02/15/2001", null},
        {"Participant0014","f","02/16/2010", null},
        {"Participant0015","f","02/17/2001", null},
        {"Participant0016","f","02/18/2001", null},
        {"Participant0017","f","02/19/2001", null},
        {"Participant0018","m","02/20/2001", null},
        {"Participant0019","m","02/21/2001", null},
        {"Participant0020","m","02/22/2001", null}
    };

    private static final String[][] SAMPLE_DATA = new String[][]{
            {"Participant0001_RNA","Participant0001","RNA","06/23/2009","Freezer1"},
            {"Participant0002_DNA","Participant0002","DNA","06/23/2005","Freezer1"},
            {"Participant0003_gDNA","Participant0003","gDNA","08/21/2007","Freezer1"},
            {"Participant0004_RNA","Participant0004","RNA","","Freezer1"},
            {"Participant0005_DNA","Participant0005","DNA","08/21/2007","Freezer1"},
            {"Participant0006_gDNA","Participant0006","gDNA","06/23/2009","Freezer1"},
            {"Participant0007_RNA","Participant0007","RNA","","Freezer1"},
            {"Participant0008_DNA","Participant0008","DNA","06/23/2009","Freezer1"},
            {"Participant0009_gDNA","Participant0009","gDNA","","Freezer1"},
            {"Participant0010_RNA","Participant0010","RNA","08/21/2007","Freezer1"},
            {"Participant0011_DNA","Participant0011","DNA","","Freezer1"},
            {"Participant0012_gDNA","Participant0012","gDNA","08/21/2007","Freezer1"},
            {"Participant0013_RNA","Participant0013","RNA","","Freezer1"},
            {"OtherSample","","DNA","","Freezer1"},
            {"Participant0015_gDNA","Participant0015","gDNA","06/23/2009","Freezer1"},
            {"Participant0016_RNA","Participant0016","RNA","08/21/2007","Freezer1"},
            {"Participant0017_DNA","Participant0017","DNA","08/21/2007","Freezer1"},
            {"Participant0018_gDNA","Participant0018","gDNA","02/03/2012","Freezer1"},
            {"OtherSample2","","RNA","","Freezer1"},
            {"Participant0020_DNA","Participant0020","DNA","02/03/2012","Freezer1"},
            {"Participant0001_gDNA","Participant0001","gDNA","04/23/2008","Freezer1"},
            {"Participant0002_RNA","Participant0002","RNA","10/23/2009","Freezer1"},
            {"Participant0003_DNA","Participant0003","DNA","05/02/2001","Freezer1"},
            {"Participant0004_gDNA","Participant0004","gDNA","04/23/2008","Freezer1"}
    };

    private static final String[][] PROJECT_ENROLLMENT = new String[][]{
            {"Participant0001","Project1","Controls","01/02/2004","08/31/2007"},
            {"Participant0002","Project1","Controls","01/02/2004",""},
            {"Participant0003","Project1","Group A","01/02/2004","9/30/2007"},
            {"Participant0004","Project1","Group A","01/02/2004",""},
            {"Participant0005","Project1","Group A","01/02/2004",""},
            {"Participant0006","Project1","Group B","01/16/2004","9/30/2007"},
            {"Participant0007","Project1","Group B","01/16/2004","12/14/2006"},
            {"Participant0008","Project2","Controls","01/16/2004","12/14/2006"},
            {"Participant0009","Project2","Controls","07/21/2002",""},
            {"Participant0010","Project2","Group A","07/21/2002",""},
            {"Participant0011","Project2","Group A","09/09/2002","09/09/2003"},
            {"Participant0012","Project2","Group A","09/09/2002",""},
            {"Participant0013","Project2","Group B","03/25/2004",""},
            {"Participant0014","Project2","Group B","03/25/2004","03/25/2005"},
            {"Participant0015","Project22","","03/25/2004","03/25/2005"},
            {"Participant0016","Project22","","04/08/2004","04/08/2005"},
            {"Participant0017","Project13","","04/08/2004","04/08/2005"},
            {"Participant0018","Project13","","04/08/2004",""},
            {"Participant0019","Project13","","10/12/2002","10/12/2003"},
            {"Participant0020","Project13","","01/02/2004","01/01/2005"},
            {"Participant0001","Project3","","05/02/2002",""},
            {"Participant0002","Project3","","01/02/2003","08/31/2007"},
            {"Participant0003","Project3","","01/02/2003",""},
            {"Participant0004","Project3","","01/02/2003","9/30/2007"}
    };

    private static final String[][] MAJOR_EVENTS = new String[][]{
            {"Participant0001","09/26/2002","Appendectomy","Surgery"},
            {"Participant0002","09/27/2002","Appendectomy","Surgery"},
            {"Participant0003","09/28/2002","Gave Birth","Medical"},
            {"Participant0004","09/29/2002","Vaccination","Medical"},
            {"Participant0005","09/30/2002","Biopsy",""},
            {"Participant0001","11/04/2006","Influenza Infection",""},
            {"Participant0002","11/05/2006","Influenza Infection",""},
            {"Participant0003","11/06/2006","Influenza Infection",""},
            {"Participant0004","11/07/2006","Influenza Infection",""},
            {"Participant0005","11/08/2006","Influenza Infection",""}
    };

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    public LabModulesTest()
    {

    }

    @Override
    public void validateQueries(boolean validateSubfolders)
    {
        super.validateQueries(false); // too may subfolders
    }

//    @Override
//    protected void doCleanup(boolean afterTest)
//    {
//        super.doCleanup(afterTest);
//    }

    @Test
    public void testSteps() throws Exception
    {
        setUpTest();
        populateData();

        dateParseTest();
        overviewUITest();
        workbookNumberingTest();
        reportsTest();
        settingsTest();
        siteSettingsTest();
        defaultAssayImportMethodTest();
        calculatedColumnsTest();
        tabbedReportsTest();
        tabbedReportToggleTest();
        labToolsWebpartTest();

        dataSourcesTest();
        workbookCreationTest();
        dnaOligosTableTest();
        samplesTableTest();
        urlGenerationTest();
        peptideTableTest();
        searchPanelTest();
    }

    protected void setUpTest() throws Exception
    {
        goToHome();
        beginAt("/project/shared/begin.view");
        _containerHelper.enableModule("Laboratory");
        goToHome();

        _containerHelper.createProject(getProjectName(), "Laboratory Folder");
        _containerHelper.enableModules(getEnabledModules());

        setupAssays();
    }

    private void populateData() throws Exception
    {
        insertSubjects();
        insertProjectEnrollment();
        insertSamples();
        insertMajorEvents();
    }

    protected void setupAssays()
    {
        for(Pair<String, String> pair : getAssaysToCreate())
        {
            _helper.defineAssay(pair.getKey(), pair.getValue());
        }
    }

    protected List<Pair<String, String>> getAssaysToCreate()
    {
        List<Pair<String, String>> assays = new ArrayList<>();
        assays.add(Pair.of("Immunophenotyping", "TruCount Test"));
        assays.add(Pair.of("ICS", "ICS Test"));
        assays.add(Pair.of("SSP Typing", "SSP Test"));
        assays.add(Pair.of("ELISPOT_Assay", "ELISPOT Test"));
        assays.add(Pair.of("Genotype Assay", GENOTYPING_ASSAYNAME));
        assays.add(Pair.of("SNP Assay", "SNP Assay Test"));

        return assays;
    }

    private void dateParseTest() throws ParseException
    {
        _helper.goToLabHome();
        log("Validating client-side date parsing");
        log("Client timezone: " + executeScript("return Ext4.Date.getTimezone(new Date());") + " / " + executeScript("return Ext4.Date.getGMTOffset(new Date());"));
        TimeZone tz = Calendar.getInstance().getTimeZone();
        long now = System.currentTimeMillis();
        log("Server timezone: " + tz.getDisplayName(tz.inDaylightTime(new Date(now)), TimeZone.SHORT) + " / " + TimeUnit.MILLISECONDS.toHours(tz.getOffset(now)));

        String dateFormat1 = "yyyy-MM-dd";
        checkDate("2011-03-04", dateFormat1);
        checkDate("2011-12-31", dateFormat1);

        String dateFormat2 = "MM/dd/yyyy";
        checkDate("03/04/2011", dateFormat2);
        checkDate("9/4/1999", dateFormat2);

        String dateFormat3 = "MM/dd/yy";
        checkDate("02/20/11", dateFormat3);
        checkDate("3/5/99", dateFormat3);
    }

    private void checkDate(String dateStr, String javaFormatStr) throws ParseException
    {
        SimpleDateFormat stdFormat = new SimpleDateFormat("yyyy-MM-dd");

        SimpleDateFormat format = new SimpleDateFormat(javaFormatStr);
        Date expectedDate = format.parse(dateStr);
        String expectedString = stdFormat.format(expectedDate);

        String clientDateStr = (String)executeScript("return (new Date()).toString()");
        String clientFormattedString = (String)executeScript("return Ext4.Date.format(LDK.ConvertUtils.parseDate('" + dateStr + "'), 'Y-m-d');");

        String clientTimezone = (String)executeScript("return Ext4.Date.getTimezone(LDK.ConvertUtils.parseDate('" + dateStr + "'));");
        String serverTimezone = Calendar.getInstance().getTimeZone().getDisplayName(false, TimeZone.SHORT);
        Date now = new Date();

        assertEquals("Incorrect JS date parsing for date: " + dateStr + " at: " + now + ", client date was: " + clientDateStr + ", with timezone: " + clientTimezone + ", with server timezone: " + serverTimezone, expectedString, clientFormattedString);
    }

    private void workbookNumberingTest() throws Exception
    {
        Connection cn = WebTestHelper.getRemoteApiConnection();

        //do cleanup in case test is started in the middle
        Integer highestWorkbookId = deleteExistingWorkbooks();

        List<CreateContainerResponse> workbooks = new ArrayList<>();
        workbooks.add(_apiContainerHelper.createWorkbook(getProjectName(), "Workbook1", null));
        workbooks.add(_apiContainerHelper.createWorkbook(getProjectName(), "Workbook2", null));
        workbooks.add(_apiContainerHelper.createWorkbook(getProjectName(), "Workbook3", null));

        //select rows from laboratory.workbooks
        SelectRowsCommand select1 = new SelectRowsCommand("laboratory", "workbooks");
        select1.addSort("workbookId", Sort.Direction.ASCENDING);
        select1.setColumns(Arrays.asList("workbookId", "container", "container/name", "containerRowId", "parentContainer"));

        SelectRowsResponse resp1 = select1.execute(cn, getProjectName());

        assertEquals("Incorrect row number", workbooks.size(), resp1.getRowCount().intValue());
        int idx = 0;
        for (Map<String, Object> row : resp1.getRows())
        {
            Integer workbookId = (Integer)row.get("workbookId");
            String container = (String)row.get("container");
            String containerName = (String)row.get("container/name");

            CreateContainerResponse workbook = workbooks.get(idx);

            assertEquals("Incorrect container", workbook.getId(), container);
            assertEquals("Incorrect container name", workbook.getName(), containerName);

            Integer expectedId = 1 + highestWorkbookId + idx;
            assertEquals("Incorrect workbookId", expectedId, workbookId);
            idx++;
        }

        //delete workbook in middle of series
        CreateContainerResponse toDelete = workbooks.get(1);
        workbooks.remove(toDelete);
        _apiContainerHelper.deleteContainer(getProjectName() + "/" + toDelete.getName(), true, 90000);

        //create new workbook
        workbooks.add(_apiContainerHelper.createWorkbook(getProjectName(), "Workbook4", null));

        //re-select
        SelectRowsCommand select2 = new SelectRowsCommand("laboratory", "workbooks");
        select2.addSort("workbookId", Sort.Direction.ASCENDING);
        select2.setColumns(Arrays.asList("workbookId", "container", "container/name", "containerRowId", "parentContainer"));

        SelectRowsResponse resp2 = select2.execute(cn, getProjectName());

        assertEquals("Incorrect row number", workbooks.size(), resp2.getRowCount().intValue());
        idx = 0;
        int workbookIdOffset = 1 + highestWorkbookId;
        for (Map<String, Object> row : resp2.getRows())
        {
            Integer workbookId = (Integer)row.get("workbookId");
            String container = (String)row.get("container");
            String containerName = (String)row.get("container/name");

            CreateContainerResponse workbook = workbooks.get(idx);

            assertEquals("Incorrect container", workbook.getId(), container);
            assertEquals("Incorrect container name", workbook.getName(), containerName);

            Integer expectedId = workbookIdOffset + idx;
            assertEquals("Incorrect workbookId", expectedId, workbookId);
            idx++;

            //the second workbook was deleted, so increment the workbookId
            if (idx == 1)
                workbookIdOffset++;
        }

        deleteExistingWorkbooks();
    }

    private int deleteExistingWorkbooks() throws Exception
    {
        Connection cn = WebTestHelper.getRemoteApiConnection();
        SelectRowsCommand select0 = new SelectRowsCommand("laboratory", "workbooks");
        select0.setColumns(Arrays.asList("workbookId", "container", "container/rowid", "containerRowId", "parentContainer"));
        select0.addSort("workbookId", Sort.Direction.ASCENDING);
        SelectRowsResponse resp0 = select0.execute(cn, getProjectName());

        int highestWorkbookId = 0;
        for (Map<String, Object> row : resp0.getRows())
        {
            highestWorkbookId = (Integer)row.get("workbookId");
            _apiContainerHelper.deleteWorkbook(getProjectName(), highestWorkbookId, true, 90000);
        }

        return highestWorkbookId;
    }

    private void calculatedColumnsTest() throws Exception
    {
        log("Testing calculated columns on data sources");
        _helper.goToLabHome();

        //test expected values
        Connection cn = WebTestHelper.getRemoteApiConnection();
        SelectRowsCommand sr = new SelectRowsCommand("laboratory", "samples");
        List<String> columns = Arrays.asList("samplename", "subjectId", "gender", "sampletype", "sampleDate",
            "overlappingProjects/projects", "overlappingProjects/groups",
            "allProjects/projects", "allProjects/groups",
            "majorEvents/Appendectomy::DaysPostEvent", "majorEvents/Appendectomy::YearsPostEvent", "majorEvents/Gave Birth::DaysPostEvent", "majorEvents/Vaccination::YearsPostEventDecimal", "majorEvents/Biopsy::MonthsPostEvent", "majorEvents/Influenza Infection::DaysPostEvent",
            "relativeDates/Project1::DaysPostStart", "relativeDates/Project2::MonthsPostStart", "relativeDates/Project3::YearsPostStartDecimal");
        sr.setColumns(columns);
        sr.setSorts(Arrays.asList(new Sort("samplename"), new Sort("sampleDate"), new Sort("sampleType")));

        SelectRowsResponse resp = sr.execute(cn, getProjectName());
        assertEquals("Incorrect number of rows returned", SAMPLE_DATA.length, resp.getRowCount().intValue());

        List<String> colOrder = Arrays.asList("samplename","subjectId","sampledate","sampletype","overlappingProjects/projects","overlappingProjects/groups","allProjects/projects","allProjects/groups","majorEvents/Appendectomy::DaysPostEvent","majorEvents/Appendectomy::YearsPostEvent","majorEvents/Gave Birth::DaysPostEvent","majorEvents/Vaccination::YearsPostEventDecimal","majorEvents/Biopsy::MonthsPostEvent","majorEvents/Influenza Infection::DaysPostEvent","relativeDates/Project1::DaysPostStart","relativeDates/Project2::MonthsPostStart","relativeDates/Project3::YearsPostStartDecimal");

        List<String[]> expected = new ArrayList<>();
        expected.add(new String[]{"OtherSample",null,null,"DNA",null,null,null,null,null,null,null,null,null,null,null,null,null,null});
        expected.add(new String[]{"OtherSample2",null,null,"RNA",null,null,null,null,null,null,null,null,null,null,null,null,null,null});
        expected.add(new String[]{"Participant0001_gDNA","Participant0001","04/23/2008","gDNA","Project3",null,"Project1\nProject3","Project1 (Controls)","2036","5",null,null,null,"536",null,null,"5.9",null});
        expected.add(new String[]{"Participant0001_RNA","Participant0001","06/23/2009","RNA","Project3",null,"Project1\nProject3","Project1 (Controls)","2462","6",null,null,null,"962",null,null,"7.1",null});
        expected.add(new String[]{"Participant0002_DNA","Participant0002","06/23/2005","DNA","Project1\nProject3","Project1 (Controls)","Project1\nProject3","Project1 (Controls)","1000","2",null,null,null,"-500","538",null,"2.4",null});
        expected.add(new String[]{"Participant0002_RNA","Participant0002","10/23/2009","RNA","Project1","Project1 (Controls)","Project1\nProject3","Project1 (Controls)","2583","7",null,null,null,"1083","2121",null,null,null});
        expected.add(new String[]{"Participant0003_DNA","Participant0003","05/02/2001","DNA",null,null,"Project1\nProject3","Project1 (Group A)",null,null,"-514",null,null,"-2014",null,null,null,null});
        expected.add(new String[]{"Participant0003_gDNA","Participant0003","08/21/2007","gDNA","Project1\nProject3","Project1 (Group A)","Project1\nProject3","Project1 (Group A)",null,null,"1788",null,null,"288","1327",null,"4.6",null});
        expected.add(new String[]{"Participant0004_gDNA","Participant0004","04/23/2008","gDNA","Project1","Project1 (Group A)","Project1\nProject3","Project1 (Group A)",null,null,null,"5.5",null,"533","1573",null,null,null});
        expected.add(new String[]{"Participant0004_RNA","Participant0004",null,"RNA",null,null,"Project1\nProject3","Project1 (Group A)",null,null,null,null,null,null,null,null,null,null});
        expected.add(new String[]{"Participant0005_DNA","Participant0005","08/21/2007","DNA","Project1","Project1 (Group A)","Project1","Project1 (Group A)",null,null,null,null,"58","286","1327",null,null,null});
        expected.add(new String[]{"Participant0006_gDNA","Participant0006","06/23/2009","gDNA",null,null,"Project1","Project1 (Group B)",null,null,null,null,null,null,null,null,null,null});
        expected.add(new String[]{"Participant0007_RNA","Participant0007",null,"RNA",null,null,"Project1","Project1 (Group B)",null,null,null,null,null,null,null,null,null,null});
        expected.add(new String[]{"Participant0008_DNA","Participant0008","06/23/2009","DNA",null,null,"Project2","Project2 (Controls)",null,null,null,null,null,null,null,null,null,null});
        expected.add(new String[]{"Participant0009_gDNA","Participant0009",null,"gDNA",null,null,"Project2","Project2 (Controls)",null,null,null,null,null,null,null,null,null,null});
        expected.add(new String[]{"Participant0010_RNA","Participant0010","08/21/2007","RNA","Project2","Project2 (Group A)","Project2","Project2 (Group A)",null,null,null,null,null,null,null,"61",null,null});
        expected.add(new String[]{"Participant0011_DNA","Participant0011",null,"DNA",null,null,"Project2","Project2 (Group A)",null,null,null,null,null,null,null,null,null,null});
        expected.add(new String[]{"Participant0012_gDNA","Participant0012","08/21/2007","gDNA","Project2","Project2 (Group A)","Project2","Project2 (Group A)",null,null,null,null,null,null,null,"59",null,null});
        expected.add(new String[]{"Participant0013_RNA","Participant0013",null,"RNA",null,null,"Project2","Project2 (Group B)",null,null,null,null,null,null,null,null,null,null});
        expected.add(new String[]{"Participant0015_gDNA","Participant0015","06/23/2009","gDNA",null,null,"Project22",null,null,null,null,null,null,null,null,null,null,null});
        expected.add(new String[]{"Participant0016_RNA","Participant0016","08/21/2007","RNA",null,null,"Project22",null,null,null,null,null,null,null,null,null,null,null});
        expected.add(new String[]{"Participant0017_DNA","Participant0017","08/21/2007","DNA",null,null,"Project13",null,null,null,null,null,null,null,null,null,null,null});
        expected.add(new String[]{"Participant0018_gDNA","Participant0018","02/03/2012","gDNA","Project13",null,"Project13",null,null,null,null,null,null,null,null,null,null,null});
        expected.add(new String[]{"Participant0020_DNA","Participant0020","02/03/2012","DNA",null,null,"Project13",null,null,null,null,null,null,null,null,null,null,null});

        assertEquals("Test has bad values for expected data", SAMPLE_DATA.length, expected.size());

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        int i = 0;
        for (Map<String, Object> row : resp.getRows())
        {
            assertTrue("Insufficient column number", row.keySet().size() >= columns.size());

            String[] expectations = expected.get(i);
            int idx = 0;
            for (String col : colOrder)
            {
                assertTrue("Row " + i + " is missing value for column: " + col, row.containsKey(col));

                Object serverVal = row.get(col);

                //NOTE: the response value can be a List
                if (serverVal instanceof List<?> list)
                {
                    String value = list.isEmpty() ? null : StringUtils.trimToNull(StringUtils.join(list, "\n"));
                    assertEquals("Incorrect value for: " + col + " on row: " + i, expectations[idx], value);
                }
                else if (serverVal instanceof Date)
                {
                    Date d = dateFormat.parse(expectations[idx]);
                    assertEquals("Incorrect value for: " + col + " on row " + i, d, serverVal);
                }
                else if ((serverVal instanceof Integer || serverVal instanceof Double))
                {
                    double d = Double.parseDouble(expectations[idx]);
                    assertEquals("Incorrect value for: " + col + " on row " + i, d, Double.parseDouble(serverVal.toString()), DELTA);
                }
                else
                {
                    String value = serverVal == null ? null : StringUtils.trimToNull(serverVal.toString());
                    assertEquals("Incorrect value for: " + col + " on row " + i, expectations[idx], value);
                }

                idx++;
            }

            i++;
        }

        _helper.goToLabHome();
    }

    private void insertSubjects() throws Exception
    {
        log("Inserting sample subject records");
        Connection cn = WebTestHelper.getRemoteApiConnection();
        InsertRowsCommand insertCmd = new InsertRowsCommand("laboratory", "subjects");

        for (String[] arr : SUBJECTS)
        {
            Map<String,Object> rowMap = new HashMap<>();
            rowMap.put("subjectname", arr[0]);
            rowMap.put("gender", arr[1]);
            rowMap.put("birth", arr[2]);
            rowMap.put("death", arr[3]);
            insertCmd.addRow(rowMap);
        }

        SaveRowsResponse saveResp = insertCmd.execute(cn, getProjectName());
        assertEquals("Incorrect number of rows created", SUBJECTS.length, saveResp.getRowsAffected().intValue());
    }

    private void insertProjectEnrollment() throws Exception
    {
        log("Inserting enrollment records");
        Connection cn = WebTestHelper.getRemoteApiConnection();
        InsertRowsCommand insertCmd = new InsertRowsCommand("laboratory", "project_usage");

        for (String[] arr : PROJECT_ENROLLMENT)
        {
            Map<String,Object> rowMap = new HashMap<>();
            rowMap.put("subjectId", arr[0]);
            rowMap.put("project", arr[1]);
            //NOTE: this is deliberately inserting using empty string, not null.  LK is expected to convert that to NULL.  If this doesnt happen,
            //calculatedColumnsTest() will fail since groupname is not null on row 2
            rowMap.put("groupname", arr[2]);
            rowMap.put("startdate", arr[3]);
            rowMap.put("enddate", arr[4]);
            insertCmd.addRow(rowMap);
        }

        SaveRowsResponse saveResp = insertCmd.execute(cn, getProjectName());
        assertEquals("Incorrect number of rows created", PROJECT_ENROLLMENT.length, saveResp.getRowsAffected().intValue());

        //test expectations
        SelectRowsCommand sr = new SelectRowsCommand("laboratory", "project_usage");
        List<String> columns = Arrays.asList("subjectId", "project", "groupname", "startdate", "enddate");
        sr.setColumns(columns);
        sr.setSorts(List.of(new Sort("rowid")));

        SelectRowsResponse resp = sr.execute(cn, getProjectName());

        int i = 0;
        for (Map<String, Object> row : resp.getRows())
        {
            String[] expected = PROJECT_ENROLLMENT[i];
            i++;

            if ("".equals(expected[2]))
            {
                //even though we insert empty string, the client API should trim this to null
                assertNull("groupname column is not null", row.get("groupname"));
            }
        }
    }

    private void insertSamples() throws Exception
    {
        log("Inserting test samples");
        Connection cn = WebTestHelper.getRemoteApiConnection();
        InsertRowsCommand insertCmd = new InsertRowsCommand("laboratory", "samples");

        for (String[] arr : SAMPLE_DATA)
        {
            Map<String,Object> rowMap = new HashMap<>();
            rowMap.put("samplename", arr[0]);
            rowMap.put("subjectId", arr[1]);
            rowMap.put("sampleType", arr[2]);
            // NOTE: the client API should accept this as a string.  If we submit this as a hava date object,
            // The JSON will include a time portion, which also includes the time portion, inferred from the local timezone
            // Using a string should preserve 00:00 as time.
            rowMap.put("sampleDate", arr[3]);
            rowMap.put("location", arr[4]);
            insertCmd.addRow(rowMap);
        }

        SaveRowsResponse saveResp = insertCmd.execute(cn, getProjectName());
        assertEquals("Incorrect number of rows created", SAMPLE_DATA.length, saveResp.getRowsAffected().intValue());
    }

    private void insertMajorEvents() throws Exception
    {
        log("Inserting test major event records");
        Connection cn = WebTestHelper.getRemoteApiConnection();
        InsertRowsCommand insertCmd = new InsertRowsCommand("laboratory", "major_events");

        for (String[] arr : MAJOR_EVENTS)
        {
            Map<String,Object> rowMap = new HashMap<>();
            rowMap.put("subjectId", arr[0]);
            rowMap.put("date", arr[1]);
            rowMap.put("event", arr[2]);
            rowMap.put("category", arr[3]);
            insertCmd.addRow(rowMap);
        }

        SaveRowsResponse saveResp = insertCmd.execute(cn, getProjectName());
        assertEquals("Incorrect number of rows created", MAJOR_EVENTS.length, saveResp.getRowsAffected().intValue());
    }

    private void overviewUITest()
    {
        log("Testing Overview UI");
        goToProjectHome();

        //verify import not visible to reader
        impersonateRole("Reader");
        _helper.goToLabHome();

        waitForElement(LabModuleHelper.getNavPanelRow("Sequence:"), WAIT_FOR_PAGE);
        _helper.verifyNavPanelRowItemPresent("Sequence:");

        for(Pair<String, String> pair : getAssaysToCreate())
        {
            _helper.verifyNavPanelRowItemPresent(pair.getValue() + ":");
            assertElementNotPresent(LabModuleHelper.getNavPanelItem(pair.getValue() + ":", IMPORT_DATA_TEXT));
        }

        for (String item : getSampleItems())
        {
            _helper.verifyNavPanelRowItemPresent(item + ":");
            assertElementNotPresent(LabModuleHelper.getNavPanelItem(item + ":", IMPORT_DATA_TEXT));
        }

        //now try UI will normal permissions
        stopImpersonatingRole();
        _helper.goToLabHome();

        _helper.clickNavPanelItem("Sequence:", IMPORT_DATA_TEXT);
        assertElementPresent(Ext4Helper.Locators.menuItem("Plan Sequence Run (Create Readsets)"));
        assertElementPresent(Ext4Helper.Locators.menuItem("Upload/Import Data"));

        waitAndClick(Ext4Helper.Locators.menuItem("Plan Sequence Run (Create Readsets)"));
        new Window.WindowFinder(getDriver()).withTitle("Plan Sequence Run (Create Readsets)").waitFor();
        waitAndClick(Ext4Helper.Locators.ext4Button("Close"));

    }

    //Related to issue 36231: ensure we can switch tabs when no subject is entered without errors:
    private void tabbedReportToggleTest()
    {
        _helper.goToLabHome();
        clickTab("Data Browser");
        checkErrors();  //try to track down team city failure
        waitForElement(Locator.tagContainingText("p", "Type of Search"));

        //we expect to be able to toggle top-level tabs without errors, report tab will not auto-load
        waitAndClick(getCategoryTabElByName("General"));
        waitForElement(getReportTabElByName("Major Events"));
        assertElementNotPresent(Locator.tagContainingText("div", "Must enter at least one Subject ID"));

        waitAndClick(getCategoryTabElByName("Samples"));
        waitForElement(getReportTabElByName("Samples"));
        assertElementNotPresent(Locator.tagContainingText("div", "Must enter at least one Subject ID"));

        waitAndClick(getCategoryTabElByName("Sequence Data"));
        waitForElement(getReportTabElByName("Sequence Readsets"));
        assertElementNotPresent(Locator.tagContainingText("div", "Must enter at least one Subject ID"));

        //when we make an active choice by clicking a bottom tab, we should error:
        waitAndClick(getReportTabElByName("Sequence Readsets"));
        waitForElement(Locator.tagContainingText("div", "Must enter at least one Subject ID"));
        waitAndClick(Ext4Helper.Locators.ext4Button("OK"));

        //now load a report
        Ext4FieldRef ref = _ext4Helper.queryOne("textfield[itemId='subjArea']", Ext4FieldRef.class);
        ref.setValue("12345,23456");
        waitAndClick(Ext4Helper.Locators.ext4Button("Update Report"));
        waitForElement(getReportChildWebpart("Sequence Readsets - 12345, 23456"));

        //toggle to samples, this should load automatically, even though we already viewed it once w/o a filter:
        waitAndClick(getCategoryTabElByName("Samples"));
        waitForElement(getReportChildWebpart("Samples - 12345, 23456"));

        //this has never been loaded yet and should also load automatically.
        waitAndClick(getCategoryTabElByName("SNP Assay"));
        waitForElement(getReportTabElByName("SNP Assay Test: Raw Data"));
        waitForElement(getReportChildWebpart("SNP Assay Test: Raw Data - 12345, 23456"));

        //toggling tab should automatically load:
        waitAndClick(getReportTabElByName("SNP Assay Test: SNP Result Summary"));
        waitForElement(getReportChildWebpart("SNP Assay Test: SNP Result Summary - 12345, 23456"));

        //now change filters.  the previously cached reports should all reload
        ref.setValue("29382");

        waitAndClick(getCategoryTabElByName("Samples"));
        waitForElement(getReportChildWebpart("Samples - 29382"));

        //now clear filters, making them invalid.  toggling category tabs should result in no error:
        ref.setValue("");

        waitAndClick(getCategoryTabElByName("SNP Assay"));
        waitForElement(getReportTabElByName("SNP Assay Test: SNP Result Summary"));

        //restore value:
        ref.setValue("29382");
        waitAndClick(getCategoryTabElByName("Samples"));
        waitForElement(getReportChildWebpart("Samples - 29382"));

        waitAndClick(getCategoryTabElByName("SNP Assay"));
        waitForElement(getReportTabElByName("SNP Assay Test: SNP Result Summary"));
        waitForElement(getReportChildWebpart("SNP Assay Test: SNP Result Summary - 29382"));

        //now toggle filter type.  we should not get an error:
        new RadioButton.RadioButtonFinder().withLabel("Subject Groups").find(getDriver()).check();
        waitForElement(Locator.tagContainingText("div", "Choose Projects/Groups:"));

        Ext4ComboRef field = _ext4Helper.queryOne("#projectField", Ext4ComboRef.class);
        field.setComboByDisplayValue("Project1");
        assertNotNull(field.getValue());

        // we toggle back to a panel with a single tab, where that tab has already been loaded.
        // since the filter set has changed, this tab should update to reflect that
        waitAndClick(getCategoryTabElByName("Samples"));
        waitForElement(getReportChildWebpart("Samples - Project1"));

        waitAndClick(getCategoryTabElByName("SNP Assay"));
        waitForElement(getReportTabElByName("SNP Assay Test: SNP Result Summary"));
        waitForElement(Locator.tagContainingText("div", "This report cannot be used with the selected filter type, because the report does not contain a field with project information"));

        //now toggle filter type.  we should not get an error:
        new RadioButton.RadioButtonFinder().withLabel("Entire Database").find(getDriver()).check();
        waitForElementToDisappear(Locator.tagContainingText("div", "Choose Projects/Groups:"));

        //should reload without errors
        waitAndClick(Ext4Helper.Locators.ext4Button("Update Report"));
        waitForElement(getReportChildWebpart("SNP Assay Test: SNP Result Summary"));

        //also should load.  report should auto-refresh since it is the only one
        waitAndClick(getCategoryTabElByName("Samples"));
        waitForElement(getReportChildWebpart("Samples"));

        waitAndClick(getCategoryTabElByName("General"));
        waitAndClick(getReportTabElByName("Major Events"));
        waitForElement(getReportChildWebpart("Major Events"));
    }

    private Locator getReportChildWebpart(String title)
    {
        return Locator.tagWithText("span", title).withClass("labkey-wp-title-text");
    }

    /**
     * This is a fairly superficial test, but this code is shared w/ EHR which has a better test
     */
    private void tabbedReportsTest()
    {
        _helper.goToLabHome();
        clickTab("Data Browser");
        checkErrors();  //try to track down team city failure
        waitForText("Type of Search");

        waitForElement(getCategoryTabElByName("Samples"));

        Ext4FieldRef ref = _ext4Helper.queryOne("textfield[itemId='subjArea']", Ext4FieldRef.class);
        ref.setValue("12345,23456");

        waitAndClick(Ext4Helper.Locators.ext4Button("Update Report"));
        waitForText("ELISPOT Test: Raw Data - 12345, 23456");

        //this is no longer the behavior
        //waitForElement(Ext4Helper.Locators.window("Error"));
        //assertTextPresent("You must select a report to display by clicking the one of the 2nd tier tabs below");
        //waitAndClick(Ext4Helper.Locators.ext4Button("OK"));

        waitAndClick(getCategoryTabElByName("Sequence Data"));
        waitForElement(getReportTabElByName("Sequence Readsets"));
        waitAndClick(getReportTabElByName("Sequence Readsets"));
        waitForText("Sequence Readsets - 12345, 23456");

        //now walk assay tabs
        for (Pair<String, String> pair : getAssaysToCreate())
        {
            log("checking assay: " + pair.getKey());
            waitAndClick(getCategoryTabElByName(pair.getKey()));
            waitForElement(getReportTabElByName(pair.getValue() + ": Raw Data"));
        }
    }

    /**
     * requires that the subject/project info was populated by calculatedColumnsTest() in order to run
     */

    private final String manageDemographicsSources = "manageDemographicsSources";
    private final String manageDataSources = "manageDataSources";

    private void dataSourcesTest() throws Exception
    {
        goToAdminConsole().goToSettingsSection();
        waitAndClickAndWait(Locator.linkContainingText("discvr admin"));
        waitAndClickAndWait(Locator.linkContainingText("Manage Default Data and Demographics Sources"));
        waitForText("You are currently editing the data and demographics sources for the Shared project");  //proxy for data loading

        cleanupDataSources();

        //data sources first
        List<Ext4CmpRef> btns = getRemoveBtns(manageDataSources);
        int initialDataBtns = btns == null ? 0 : btns.size();

        _helper.addDataSource("misc", GROUP_SOURCE, "New Data Sources", null, "core", "Groups");
        _helper.addDataSource("data", DATA_SOURCE, "New Data Sources", "/home", "core", "Users");
        _helper.addDataSource("data", REMOVED_SOURCE, "Will Be Removed", "/" + getProjectName(), "laboratory", "subjects");
        assertEquals("Incorrect number of remove buttons", initialDataBtns + 3, getRemoveBtns(manageDataSources).size());

        deleteSourceByLabel(REMOVED_SOURCE, manageDataSources);
        assertEquals("Incorrect number of remove buttons", initialDataBtns + 2, getRemoveBtns(manageDataSources).size());

        //now add demographics sources
        btns = getRemoveBtns(manageDemographicsSources);
        int initialDemographicsBtns = btns == null ? 0 : btns.size();

        _helper.addDemographicsSource(SUBJECT_LIST, null, "laboratory", "subjects", "subjectname", true, false);
        _helper.addDemographicsSource(SUBJECT_LIST, null, "laboratory", "subjects", "subjectname", false, true);
        _helper.addDemographicsSource(ELISPOT_SOURCE, "/" + getProjectName(), "elispot_assay", "elispot_targets", "target", true, false);
        _helper.addDemographicsSource("Failure", null, "core", "containers", null, false, false);
        assertEquals("Incorrect number of remove buttons", initialDemographicsBtns + 2, getRemoveBtns(manageDemographicsSources).size());

        deleteSourceByLabel(ELISPOT_SOURCE, manageDemographicsSources);
        assertEquals("Incorrect number of remove buttons", initialDemographicsBtns + 1, getRemoveBtns(manageDemographicsSources).size());

        //now go to folder and make sure it worked
        _helper.goToLabHome();
        waitAndClickAndWait(_helper.toolIcon("Settings"));
        waitAndClickAndWait(Locator.linkContainingText("Manage Data and Demographics Sources"));
        waitForText("Below are the extra data and sample sources registered for this folder");  //proxy for data loading
        waitForElementToDisappear(Locator.xpath("//div[contains(text(), 'Loading...')]"), WAIT_FOR_JAVASCRIPT);

        //precaution mainly important to resuming a test in the middle
        cleanupDataSources();

        //add data source
        Ext4CmpRef addDataSourceBtn = _ext4Helper.queryOne("#" + manageDataSources + " button[text='Add Default Sources']", Ext4CmpRef.class);
        waitAndClick(Locator.id(addDataSourceBtn.getId()));
        waitAndClick(Ext4Helper.Locators.menuItem(DATA_SOURCE + " (\"/home\".core.Users)"));
        new Window.WindowFinder(getDriver()).withTitle("Success").waitFor();
        click(Ext4Helper.Locators.ext4Button("OK"));

        addDataSourceBtn = _ext4Helper.queryOne("#" + manageDataSources + " button[text='Add Default Sources']", Ext4CmpRef.class);
        waitAndClick(Locator.id(addDataSourceBtn.getId()));
        waitAndClick(Ext4Helper.Locators.menuItem(GROUP_SOURCE + " (core.Groups)"));
        new Window.WindowFinder(getDriver()).withTitle("Success").waitFor();
        click(Ext4Helper.Locators.ext4Button("OK"));

        waitForText("Item Type");
        assertEquals("Incorrect number of remove buttons", 2, getRemoveBtns(manageDataSources).size());

        //add demographics source
        Ext4CmpRef addDemographicsSourceBtn = _ext4Helper.queryOne("#" + manageDemographicsSources + " button[text='Add Default Sources']", Ext4CmpRef.class);
        waitAndClick(Locator.id(addDemographicsSourceBtn.getId()));
        waitAndClick(Ext4Helper.Locators.menuItem(SUBJECT_LIST + " (laboratory.subjects)"));
        new Window.WindowFinder(getDriver()).withTitle("Success").waitFor();
        click(Ext4Helper.Locators.ext4Button("OK"));

        waitForText("Table Name");
        waitForText("New Data Sources");
        assertEquals("Incorrect number of remove buttons", 1, getRemoveBtns(manageDemographicsSources).size());

        //query to make sure columns added:
        Connection cn = WebTestHelper.getRemoteApiConnection();
        SelectRowsCommand sr = new SelectRowsCommand("laboratory", "samples");
        String fieldKey = _helper.getLegalNameFromName(SUBJECT_LIST);
        List<String> columns = Arrays.asList("samplename", fieldKey + "/ageInYears", fieldKey + "/gender");
        sr.setColumns(columns);
        sr.setSorts(Arrays.asList(new Sort("samplename"), new Sort("sampleDate")));
        sr.addFilter(new Filter("subjectId", "Participant0001"));

        SelectRowsResponse resp = sr.execute(cn, getProjectName());

        List<String[]> expected = new ArrayList<>();
        expected.add(new String[]{"Participant0001_gDNA","11","m"});
        expected.add(new String[]{"Participant0001_RNA","11","m"});

        assertEquals("Incorrect number of rows returned", expected.size(), resp.getRowCount().intValue());

        int i = 0;
        for (Map<String, Object> row : resp.getRows())
        {
            assertTrue("Insufficient column number", row.keySet().size() >= columns.size());

            String[] expectations = expected.get(i);
            int idx = 0;
            for (String col : columns)
            {
                assertTrue("Row " + i + " is missing value for column: " + col, row.containsKey(col));

                Object serverVal = row.get(col);
                assertNotNull("Missing value for column: " + col, serverVal);
                assertEquals("Incorrect value for: " + col + " on row " + i, expectations[idx], String.valueOf(serverVal));

                idx++;
            }

            i++;
        }

        _helper.goToLabHome();

        //assert custom data element present.  core.users cannot be edited, so it should hide the import UI
        assertElementPresent(Locator.linkContainingText(GROUP_SOURCE));
        assertElementNotPresent(LabModuleHelper.getNavPanelItem("Users:", IMPORT_DATA_TEXT));

        //verify that the demographics source has been added
        _helper.clickNavPanelItem(GENOTYPING_ASSAYNAME + ":", "Browse All");
        waitForText(GENOTYPING_ASSAYNAME + " Results");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn(fieldKey + "/gender");
        _customizeViewsHelper.applyCustomView();
        assertTextPresent("Gender");

        _helper.goToLabHome();
        _helper.clickNavPanelItem("Samples:", "Browse All");
        String subj1 = "Participant0001";
        waitForText(subj1);
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn(fieldKey + "/gender");
        _customizeViewsHelper.applyCustomView();
        DataRegionTable dr = new DataRegionTable("query", this);
        //dr.setSort("samplename", SortDirection.ASC);
        assertEquals("Incorrect values for subject field", subj1, dr.getDataAsText(3, "Subject Id"));
        assertEquals("Incorrect values for gender field", "m", dr.getDataAsText(3, "Gender"));

        _customizeViewsHelper.revertUnsavedViewGridClosed();

        //now validate site-summary reports
        goToAdminConsole().goToSettingsSection();
        waitAndClickAndWait(Locator.linkContainingText("discvr admin"));
        waitAndClickAndWait(Locator.linkContainingText("Manage Default Data and Demographics Sources"));

        waitAndClick(Ext4Helper.Locators.ext4Button("View Summary of Data Sources"));
        waitForText("The following sources have been defined:");
        waitForText("/" + getProjectName());
        assertTextPresent(DATA_SOURCE + " (\"/home\".core.Users)");

        waitAndClick(Ext4Helper.Locators.ext4Button("View Summary of Demographics Sources"));
        waitForText("The following sources have been defined:");
        waitForText(SUBJECT_LIST);
        assertTextPresent("/" + getProjectName(), SUBJECT_LIST + " (laboratory.subjects)");

        cleanupDataSources();
    }

    private void cleanupDataSources()
    {
        deleteSourceByLabel(REMOVED_SOURCE, manageDataSources);
        deleteSourceByLabel(GROUP_SOURCE, manageDataSources);
        deleteSourceByLabel(DATA_SOURCE, manageDataSources);
        deleteSourceByLabel(ELISPOT_SOURCE, manageDemographicsSources);
        deleteSourceByLabel(SUBJECT_LIST, manageDemographicsSources);
    }

    private List<Ext4CmpRef> getRemoveBtns(String parentItemId)
    {
        return _ext4Helper.componentQuery("#" + parentItemId + " button[text='Remove']", Ext4CmpRef.class);
    }

    private String replaceNonVisibleChars(String value)
    {
        //the DB might not store unicode chars correctly, so we just eliminate them here
        String ret = value.replaceAll("[^\\p{Print}]", "?");
        ret = ret.replaceAll("\\$", ""); //fieldkeys encode these
        return ret;
    }

    private void deleteSourceByLabel(String label, String parentItemId)
    {
        Ext4CmpRef panel = _ext4Helper.queryOne("#" + parentItemId, Ext4CmpRef.class);
        Long idx = (Long)panel.getFnEval("return this.getSourceIdx(arguments[0])", label);
        if (idx == null)
            return;

        List<Ext4CmpRef> removeBtns = _ext4Helper.componentQuery("#" + parentItemId + " button[text='Remove']", Ext4CmpRef.class);
        click(Locator.id(removeBtns.get(idx.intValue()).getId()));
        new Window.WindowFinder(getDriver()).withTitle("Success").waitFor();
        click(Ext4Helper.Locators.ext4Button("OK"));

        waitForText("Loading...");
        waitForElementToDisappear(Locator.xpath("//div[" + Locator.NOT_HIDDEN + " and contains(text(), 'Loading...')]"), WAIT_FOR_JAVASCRIPT);
    }

    private void settingsTest()
    {
        log("Testing Settings");
        _helper.goToLabHome();
        waitAndClickAndWait(_helper.toolIcon("Settings"));

        waitForElement(Locator.linkContainingText("Control Item Visibility"));
        assertElementPresent(LabModuleHelper.getNavPanelRow("Manage Freezers:"));

        waitAndClickAndWait(Locator.linkContainingText("Control Item Visibility"));

        waitForText("Sequence"); //proxy for page load
        waitForText(WAIT_FOR_JAVASCRIPT * 3, "TruCount"); //proxy for page load

        log("Disabling items");

        int i = 1;
        for (Pair<String, String> pair : getAssaysToCreate())
        {
            Ext4FieldRef.getForBoxLabel(this, pair.getValue()).setValue(false);
            sleep(40); //wait for listener to act
            assertFalse("Radio was not toggled", (Boolean) Ext4FieldRef.getForBoxLabel(this, pair.getValue() + ": Raw Data").getValue());

            if (i == 1)
                Ext4FieldRef.getForBoxLabel(this, pair.getValue()).setValue(true);

            i++;
        }

        //sequence
        Ext4FieldRef.getForBoxLabel(this, "Sequence").setValue(false);
        sleep(40); //wait for listener to act
        assertFalse("Radio was not toggled", (Boolean) Ext4FieldRef.getForBoxLabel(this, "Browse Sequence Data").getValue());


        //samples
        Ext4FieldRef.getForBoxLabel(this, "Samples").setValue(false);
        sleep(40); //wait for listener to act
        assertFalse("Radio was not toggled", (Boolean) Ext4FieldRef.getForBoxLabel(this, "Freezer Summary").getValue());
        assertFalse("Radio was not toggled", (Boolean) Ext4FieldRef.getForBoxLabel(this, "View All Samples").getValue());

        //oligos
        Ext4FieldRef.getForBoxLabel(this, "DNA_Oligos").setValue(false);
        sleep(40); //wait for listener to act
        assertFalse("Radio was not toggled", (Boolean) Ext4FieldRef.getForBoxLabel(this, "View All DNA Oligos").getValue());

        //peptides
        Ext4FieldRef.getForBoxLabel(this, "Peptides").setValue(false);
        sleep(40); //wait for listener to act
        assertFalse("Radio was not toggled", (Boolean) Ext4FieldRef.getForBoxLabel(this, "View All Peptides").getValue());

        //antibodies
        Ext4FieldRef.getForBoxLabel(this, "Antibodies").setValue(false);
        sleep(40); //wait for listener to act
        assertFalse("Radio was not toggled", (Boolean) Ext4FieldRef.getForBoxLabel(this, "View All Antibodies").getValue());

        //major events
        Ext4FieldRef.getForBoxLabel(this, "Major Events").setValue(false);
        sleep(40); //wait for listener to act
        Assert.assertFalse("Radio was not toggled", (Boolean) Ext4FieldRef.getForBoxLabel(this, "View All Major Events").getValue());

        click(Ext4Helper.Locators.ext4Button("Submit"));
        new Window.WindowFinder(getDriver()).withTitle("Success").waitFor();
        click(Ext4Helper.Locators.ext4Button("OK"));

        //should redirect to lab home
        waitForText("Types of Data:");

        assertElementNotPresent(LabModuleHelper.getNavPanelRow("Sequence:"));
        assertElementNotPresent(LabModuleHelper.getNavPanelRow("Samples:"));
        assertElementNotPresent(LabModuleHelper.getNavPanelRow("DNA_Oligos:"));
        assertElementNotPresent(LabModuleHelper.getNavPanelRow("Peptides:"));
        assertElementNotPresent(LabModuleHelper.getNavPanelRow("Antibodies:"));

        i = 1;
        for (Pair<String, String> pair : getAssaysToCreate())
        {
            if (i == 1)
                assertElementPresent(LabModuleHelper.getNavPanelRow(pair.getValue() + ":"));
            else
                assertElementNotPresent(LabModuleHelper.getNavPanelRow(pair.getValue() + ":"));

            i++;
        }

        //also verify reports
        clickTab("Reports");
        waitForText("Raw Data");
        i = 1;
        for (Pair<String, String> pair : getAssaysToCreate())
        {
            if (i == 1)
                assertElementPresent(Locator.linkContainingText(pair.getValue() + ": Raw Data"));
            else
                assertElementNotPresent(Locator.linkContainingText(pair.getValue() + ": Raw Data"));

            i++;
        }
        assertElementPresent(Locator.linkContainingText("TruCount Test: Results By Run"));

        assertElementNotPresent(Locator.linkContainingText("View All")); //covers samples, peptides, oligos, antibodies
        assertElementNotPresent(Locator.linkContainingText("Browse Sequence Data"));

        //restore defaults
        _helper.goToLabHome();
        waitAndClickAndWait(_helper.toolIcon("Settings"));
        waitAndClickAndWait(Locator.linkContainingText("Control Item Visibility"));
        waitForText("Sequence"); //proxy for page load
        waitForText(WAIT_FOR_JAVASCRIPT * 2, "TruCount"); //proxy for page load

        for (Pair<String, String> pair : getAssaysToCreate())
        {
            Ext4FieldRef.getForBoxLabel(this, pair.getValue()).setValue(true);
        }
        Ext4FieldRef.getForBoxLabel(this, "Sequence").setValue(true);
        String samplesSelector = "container[itemCategory='samples'] > checkbox[boxLabel='Samples']";
        _ext4Helper.queryOne(samplesSelector, Ext4FieldRef.class).setValue(true);
        Ext4FieldRef.getForBoxLabel(this, "DNA_Oligos").setValue(true);
        Ext4FieldRef.getForBoxLabel(this, "Peptides").setValue(true);
        Ext4FieldRef.getForBoxLabel(this, "Antibodies").setValue(true);
        Ext4FieldRef.getForBoxLabel(this, "Major Events").setValue(true);

        sleep(100);
        assertTrue("Incorrect value for samples checkbox", (Boolean)_ext4Helper.queryOne(samplesSelector, Ext4FieldRef.class).getValue());

        click(Ext4Helper.Locators.ext4Button("Submit"));
        new Window.WindowFinder(getDriver()).withTitle("Success").waitFor();
        click(Ext4Helper.Locators.ext4Button("OK"));
    }

    private void siteSettingsTest()
    {
        log("Testing Site Settings");
        goToAdminConsole().goToSettingsSection();
        waitAndClickAndWait(Locator.linkContainingText("discvr admin"));

        waitForText("Reference Sequences"); //proxy for page load

        assertElementPresent(LabModuleHelper.getNavPanelRow("Assay Types:"));
        assertElementPresent(LabModuleHelper.getNavPanelRow("Instruments:"));
        assertElementPresent(LabModuleHelper.getNavPanelRow("Peptide Pools:"));

        assertElementPresent(LabModuleHelper.getNavPanelRow("Cell Populations:"));
        assertElementPresent(LabModuleHelper.getNavPanelRow("Units:"));

        assertElementPresent(LabModuleHelper.getNavPanelRow("Reference AA Features:"));
        assertElementPresent(LabModuleHelper.getNavPanelRow("Reference NT Features:"));
        assertElementPresent(LabModuleHelper.getNavPanelRow("Virus Strains:"));

        assertElementPresent(LabModuleHelper.getNavPanelRow("Allowable Cell Types:"));
        assertElementPresent(LabModuleHelper.getNavPanelRow("Allowable Genders:"));

        assertElementPresent(LabModuleHelper.getNavPanelRow("Allowable Barcodes:"));
        assertElementPresent(LabModuleHelper.getNavPanelRow("DNA Loci:"));
        assertElementPresent(LabModuleHelper.getNavPanelRow("Haplotype Definitions:"));
    }

    private void reportsTest()
    {
        _helper.goToLabHome();
        clickTab("Reports");

        //TODO: also verify link targets

        waitForElement(Locator.linkContainingText("Browse Sequence Data"), WAIT_FOR_JAVASCRIPT * 3); //proxy for page load

        for (Pair<String, String> pair : getAssaysToCreate())
        {
            assertElementPresent(Locator.linkContainingText(pair.getValue() + ": Raw Data"));
        }
        assertElementPresent(Locator.linkContainingText("TruCount Test: Results By Run"));
        assertElementPresent(Locator.linkContainingText("SSP Test: SSP Summary"));
        assertElementPresent(Locator.linkContainingText("SSP Test: Results By Run"));
        assertElementPresent(Locator.linkContainingText("ICS Test: Results By Run"));

        assertElementPresent(Locator.linkContainingText("View All DNA Oligos"));
        assertElementPresent(Locator.linkContainingText("View All Peptides"));
        assertElementPresent(Locator.linkContainingText("View All Antibodies"));
        assertElementPresent(Locator.linkContainingText("View All Samples"));
        assertElementPresent(Locator.linkContainingText("Freezer Summary"));

        assertElementPresent(Locator.linkContainingText("Browse Sequence Data"));
    }

    private void labToolsWebpartTest()
    {
        log("testing lab tools webpart");
        _helper.goToLabHome();

        waitAndClick(_helper.toolIcon("Import Data"));
        assertElementPresent(Ext4Helper.Locators.menuItem("Sequence"));
        for(Pair<String, String> pair : getAssaysToCreate())
        {
            assertElementPresent(Ext4Helper.Locators.menuItem(pair.getValue()));
        }

        _helper.goToLabHome();
        waitAndClick(_helper.toolIcon("Import Samples"));
        for (String s : getSampleItems())
        {
            assertElementPresent(Ext4Helper.Locators.menuItem(s));
        }

        assertElementPresent(_helper.toolIcon("Data Browser"));

        //verify settings hidden for readers
        Locator settings = _helper.toolIcon("Settings");
        assertElementPresent(settings);
        impersonateRole("Reader");
        _helper.goToLabHome();
        assertElementNotPresent(settings);
        stopImpersonatingRole();
        goToProjectHome();
    }

    private void workbookCreationTest()
    {
        _helper.goToLabHome();

        _helper.clickNavPanelItem("View and Edit Workbooks:", "Create New Workbook");
        new Window.WindowFinder(getDriver()).withTitle("Create Workbook").waitFor();
        assertElementNotPresent(Ext4Helper.Locators.ext4Radio("Add To Existing Workbook"));
        waitForElement(Ext4Helper.Locators.ext4Button("Close"));
        click(Ext4Helper.Locators.ext4Button("Close"));
        assertElementNotPresent(Ext4Helper.Locators.window("Create Workbook"));

        String workbookTitle = "NewWorkbook_" + INJECT_CHARS_1;
        String workbookDescription = "I am a workbook.  I am trying to inject javascript into your page.  " + INJECT_CHARS_1 + INJECT_CHARS_2;
        _helper.createWorkbook(getProjectName(), workbookTitle, workbookDescription);

        //verify correct name and correct webparts present
        assertElementPresent(LabModuleHelper.webpartTitle("Lab Tools"));
        assertElementPresent(LabModuleHelper.webpartTitle("Pipeline Files"));
        assertElementPresent(LabModuleHelper.webpartTitle("Workbook Summary"));

        //we expect insert from within the workbook to go straight to the import page (unlike the top-level folder, which gives a dialog)
        waitAndClick(_helper.toolIcon("Import Samples"));
        for (String s : getSampleItems())
        {
            assertElementPresent(Ext4Helper.Locators.menuItem(s));
        }
        //NOTE: we are in a workbook here
        waitAndClickAndWait(Ext4Helper.Locators.menuItem("DNA_Oligos"));
        waitForElement(Locator.name("name"));
        waitForElement(Locator.name("purification"));

        setFormElement(Locator.name("name"), "TestPrimer20");
        setFormElement(Locator.name("sequence"), "ATGATGATGGGGG");
        sleep(150); //there's a buffer when committing changes
        clickButton("Submit", 0);

        new Window.WindowFinder(getDriver()).withTitle("Success").waitFor();
        assertTextPresent("Your upload was successful");
        _oligosTotal++;
        clickButton("OK", 0);

        _helper.goToLabHome();
    }

    private void dnaOligosTableTest()
    {
        log("Testing DNA Oligos Table");
        _helper.goToLabHome();

        _helper.clickNavPanelItem("DNA_Oligos:", IMPORT_DATA_TEXT);
        new Window.WindowFinder(getDriver()).withTitle(IMPORT_DATA_TEXT).waitFor();
        waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Submit"));

        waitForElement(Locator.name("purification"));

        setFormElement(Locator.name("name"), "TestPrimer1");
        setFormElement(Locator.name("sequence"), "ABC DQ");
        setFormElement(Locator.name("oligo_type"), "Type1");
        sleep(150); //there's a buffer when committing changes
        clickButton("Submit", 0);
        _oligosTotal += 1;

        _helper.goToLabHome();
        _helper.clickNavPanelItem("DNA_Oligos:", IMPORT_DATA_TEXT);
        new Window.WindowFinder(getDriver()).withTitle(IMPORT_DATA_TEXT).waitFor();
        waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Submit"));
        waitForElement(Locator.name("purification"));
        _ext4Helper.clickTabContainingText("Import Spreadsheet");
        waitForText("Copy/Paste Data");

        String sequence = "tggGg gGAAAAgg";
        setFormElementJS(Locator.name("text"), "Name\tSequence\nTestPrimer1\tatg\nTestPrimer2\t" + sequence);
        clickButton("Upload", 0);
        _oligosTotal += 2;

        new Window.WindowFinder(getDriver()).withTitle("Success").waitFor();
        assertTextPresent("Success! 2 rows inserted.");
        clickButton("OK");

        //verify row imported
        _helper.goToLabHome();
        _helper.clickNavPanelItem("DNA_Oligos:", "Browse All");
        String text = sequence.replaceAll(" ", "");
        waitForText(text);
        assertTrue("Sequence was not formatted properly on import", isTextPresent(text));
        assertFalse("Sequence was not formatted properly on import", isTextPresent(sequence));

        DataRegionTable dr = new DataRegionTable("query", this);
        assertEquals("Incorrect Oligo ID", "1", dr.getDataAsText(0, "Oligo Id"));
        assertEquals("Incorrect Oligo ID", "2", dr.getDataAsText(1, "Oligo Id"));
    }

    private void samplesTableTest() throws Exception
    {
        log("Testing Samples Table");
        _helper.goToLabHome();

        _helper.clickNavPanelItem("Samples:", IMPORT_DATA_TEXT);
        new Window.WindowFinder(getDriver()).withTitle(IMPORT_DATA_TEXT).waitFor();
        waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Submit"));

        waitForElement(Locator.name("samplename"));

        verifyFreezerColOrder();

        _helper.setFormField("samplename", "SampleName");

        //verify drop down menus show correct text by spot checking several drop-downs
        //NOTE: trailing spaces are added by ext template
        Ext4ComboRef.getForLabel(this, "Sample Type").setValue("Cell Line");
        Ext4FieldRef.getForLabel(this, "Sample Source").setValue("DNA");
        Ext4ComboRef.getForLabel(this, "Additive").setValue("EDTA");
        Ext4ComboRef.getForLabel(this, "Molecule Type").setValue("vRNA");

        assertElementNotPresent(Ext4Helper.Locators.invalidField());
        clickButton("Submit", 0);

        //test error conditions in trigger script
        waitForElement(Ext4Helper.Locators.window("Error"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        assertTextPresent("Must enter either a location or freezer");
        clickButton("OK", 0);
        waitForElement(Ext4Helper.Locators.invalidField());

        _helper.setFormField("box_row", "-100");
        _helper.setFormField("location", "Location1");
        clickButton("Submit", 0);

        new Window.WindowFinder(getDriver()).withTitle("Error").waitFor();
        waitForText("Cannot have a negative value for box_row");
        clickButton("OK", 0);

        //test presence of UI to download multiple templates
        _ext4Helper.clickTabContainingText("Import Spreadsheet");
        waitForText("Copy/Paste Data");

        //we only care that these items are present
        _ext4Helper.selectComboBoxItem("Choose Template:", "DNA Samples Template");
        _ext4Helper.selectComboBoxItem("Choose Template:", "Default Template");

        //the import button has an override that should cause the workbook popup if loading from a folder
        _helper.goToLabHome();
        _helper.clickNavPanelItem("Samples:", "Browse All");
        DataRegionTable dr = new DataRegionTable.DataRegionFinder(getDriver()).withName("query").find();
        dr.openHeaderMenu("Insert"); //expand menu
        assertTextPresent("Insert New", "Import Bulk Data");
        dr.clickHeaderButton("Insert"); //collapse menu

        dr.clickHeaderMenu("Insert", false, "Insert New");
        new Window.WindowFinder(getDriver()).withTitle(IMPORT_DATA_TEXT).waitFor();
        waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Submit"));
        waitForElement(Locator.name("samplename"));  //ensure we're on the correct Ext4 override page
        verifyFreezerColOrder();
        String containerPath = getCurrentContainerPath();
        assertEquals("Container is not a workbook: " + containerPath + "(" + containerPath.split("/").length + " elements)", 3, containerPath.split("/").length);

        //now repeat from within this workbook
        clickButton("Cancel");
        waitForElement(Locator.tagWithText("span", "Workbook Summary"));
        waitForElement(Locator.tagWithText("span", "Samples and Materials:"));
        _helper.clickNavPanelItemAndWait("Samples:", 1);

        dr = new DataRegionTable.DataRegionFinder(getDriver()).withName("query").find();
        dr.clickHeaderMenu("Insert", "Import Bulk Data");
        waitForElement(Locator.tagWithText("label", "Copy/Paste Data"));  //ensure we're on the correct Ext4 override page.  This should load with the spreadsheet panel
        waitForElement(Locator.tagWithText("label", "Choose Template:")); //and full page load
        Assert.assertEquals("Should be in the same workbook", containerPath, getCurrentContainerPath());

        //test duplicate/derive:
        _helper.goToLabHome();
        _helper.clickNavPanelItem("Samples:", "Browse All");
        dr = new DataRegionTable.DataRegionFinder(getDriver()).withName("query").find();
        dr.uncheckAllOnPage();
        assertEquals("incorrect number of rows selected", 0, dr.getCheckedCount());

        dr.checkCheckbox(1);
        dr.checkCheckbox(2);
        dr.clickHeaderMenu("More Actions", false, "Duplicate/Derive Samples");
        new Window.WindowFinder(getDriver()).withTitle("Duplicate/Derive Samples").waitFor();
        Ext4FieldRef.getForLabel(this, "Copies Per Sample").setValue(2);
        File downloaded = clickAndWaitForDownload(Ext4Helper.Locators.ext4Button("Submit"));
        Assert.assertTrue(downloaded.exists());

        List<String> columnLabels = new ArrayList<>();
        List<String> columnNames = Arrays.asList("samplename","subjectid","sampledate","sampletype","samplesubtype","samplesource","location","freezer","cane","box","box_row","box_column","preparationmethod","processdate","additive","concentration","concentration_units","quantity","quantity_units","passage_number","ratio","molecule_type","DNA_vector","DNA_insert","sequence","labwareidentifier","comment","parentsample","dateremoved","removedby","remove_comment");
        String sampleId1 = dr.getDataAsText(1, dr.getColumnIndex("freezerid"));
        String sampleId2 = dr.getDataAsText(2, dr.getColumnIndex("freezerid"));

        SelectRowsCommand sr = new SelectRowsCommand("laboratory", "samples");
        sr.setColumns(columnNames);
        sr.setSorts(Arrays.asList(new Sort("samplename"), new Sort("subjectid")));  //match default view
        sr.setViewName("All Samples");  //required to return those w/ disabled dates
        sr.addFilter(new Filter("freezerid", sampleId1 + ";" + sampleId2, Filter.Operator.IN));
        Connection cn = WebTestHelper.getRemoteApiConnection();
        SelectRowsResponse srr = sr.execute(cn, getCurrentContainerPath());
        Assert.assertEquals(2, srr.getRowCount().intValue());

        for (String name : columnNames)
        {
            columnLabels.add(getColumnLabel(srr, name));
        }

        List<List<String>> rows = new ArrayList<>();
        for (Map<String, Object> row : srr.getRows())
        {
            row = new CaseInsensitiveHashMap<>(row);
            List<String> target = new ArrayList<>();
            for (String name : columnNames)
            {
                String val = row.get(name) == null ? "" : String.valueOf(row.get(name));
                if (name.toLowerCase().contains("date"))
                {
                    val = StringUtils.isEmpty(val) ? "" : ExcelHelper.getDateTimeFormat().format(new Date(val));
                }

                target.add(val);
            }

            rows.add(target);
        }

        try (Workbook w = ExcelHelper.create(downloaded))
        {
            Sheet sheet = w.getSheetAt(0);
            List<List<String>> lines = ExcelHelper.getFirstNRows(sheet, 5);

            Assert.assertEquals(columnLabels, lines.get(0));
            Assert.assertEquals(rows.get(0), lines.get(1));
            Assert.assertEquals(rows.get(0), lines.get(2));
            Assert.assertEquals(rows.get(1), lines.get(3));
            Assert.assertEquals(rows.get(1), lines.get(4));
        }

        refresh();
        dr = new DataRegionTable.DataRegionFinder(getDriver()).withName("query").find();
        dr.uncheckAllOnPage();
        assertEquals("incorrect number of rows selected", 0, dr.getCheckedCount());
        dr.checkCheckbox(1);
        dr.checkCheckbox(2);
        dr.clickHeaderMenu("More Actions", false, "Append Comment");

        new Window.WindowFinder(getDriver()).withTitle("Append Comment").waitFor();
        final String comment = "This is text I added";
        Ext4FieldRef.getForLabel(this, "Comment").setValue(comment);
        waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Submit"));
        dr = new DataRegionTable.DataRegionFinder(getDriver()).withName("query").find();
        int colIdx = dr.getColumnIndex("comment");
        String comment1 = dr.getDataAsText(1, colIdx);
        String comment2 = dr.getDataAsText(2, colIdx);
        Assert.assertEquals(comment1, comment);
        Assert.assertEquals(comment2, comment);

        dr.uncheckAllOnPage();
        assertEquals("incorrect number of rows selected", 0, dr.getCheckedCount(this));
        dr.checkCheckbox(1);
        dr.clickHeaderMenu("More Actions", false, "Append Comment");

        new Window.WindowFinder(getDriver()).withTitle("Append Comment").waitFor();
        Ext4FieldRef.getForLabel(this, "Comment").setValue("This should append to the end");
        waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Submit"));
        dr = new DataRegionTable.DataRegionFinder(getDriver()).withName("query").find();
        String comment1b = dr.getDataAsText(1, colIdx);
        String comment2b = dr.getDataAsText(2, colIdx);
        Assert.assertEquals(comment1b, comment + "\nThis should append to the end");
        Assert.assertEquals(comment2b, comment);

        refresh();
        dr = new DataRegionTable.DataRegionFinder(getDriver()).withName("query").find();
        int originalCount = dr.getDataRowCount();
        dr.uncheckAllOnPage();
        assertEquals("incorrect number of rows selected", 0, dr.getCheckedCount(this));
        dr.checkCheckbox(1);
        dr.checkCheckbox(2);
        sampleId1 = dr.getDataAsText(1, dr.getColumnIndex("freezerid"));
        sampleId2 = dr.getDataAsText(2, dr.getColumnIndex("freezerid"));
        dr.clickHeaderMenu("More Actions", false, "Mark Removed");
        new Window.WindowFinder(getDriver()).withTitle("Mark Removed").waitFor();
        Ext4FieldRef.getForLabel(this, "Date Removed").setValue("2017-01-02");
        Ext4FieldRef.getForLabel(this, "Comment").setValue("I removed these samples");
        waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Submit"));

        dr = new DataRegionTable.DataRegionFinder(getDriver()).withName("query").find();
        int newColumnCount = dr.getDataRowCount();
        Assert.assertEquals(originalCount - 2, newColumnCount);

        sr = new SelectRowsCommand("laboratory", "samples");
        sr.setColumns(Arrays.asList("freezerid", "remove_comment", "dateremoved", "removedby"));
        sr.setViewName("All Samples");  //required to return those w/ disabled dates
        sr.addFilter(new Filter("freezerid", sampleId1 + ";" + sampleId2, Filter.Operator.IN));
        srr = sr.execute(cn, getCurrentContainerPath());
        Assert.assertEquals(2, srr.getRowCount().intValue());
        for (Map<String, Object> row : srr.getRows())
        {
            Assert.assertEquals("I removed these samples", row.get("remove_comment"));
            Assert.assertEquals(getUserId(), row.get("removedby"));
            Assert.assertEquals("2017-01-02", dateFormat.format(row.get("dateremoved")));
        }
    }

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private String getColumnLabel(SelectRowsResponse srr, String name)
    {
        List<Map<String, Object>> columnModel = (List<Map<String, Object>>) srr.getMetaData().get("fields");
        for (Map<String, Object> column : columnModel)
        {
            if (name.equalsIgnoreCase((String) column.get("name")))
            {
                return (String)column.get("caption");
            }
        }

        throw new IllegalArgumentException("Column not found: " + name);
    }

    private Integer getUserId()
    {
        return ((Long)executeScript("return LABKEY.user.id")).intValue();
    }

    /**
     * For TabbedReportPanel top-level tabs
     */
    private Locator getReportTabElByName(String name)
    {
        return Locator.tagWithClass("div", "active").descendant(Locator.tagWithId("a", name.replace(" ", "") + "Tab"));
    }

    /**
     * For TabbedReportPanel second-level tabs
     */
    private Locator getCategoryTabElByName(String name)
    {
        return Locator.tagWithId("a", name.replace(" ", "") + "Tab");
    }

    /**
     * This is designed to be a general test of custom URLs, and also should verify that URLs in
     * dataRegions use the correct container when you display rows from multiple containers in the same grid.
     */
    private void urlGenerationTest() throws UnsupportedEncodingException
    {
        log("Testing DataRegion URL generation");
        _helper.goToLabHome();

        //insert dummy data:
        String[] workbookIds = new String[3];
        Integer i = 0;
        int max = 3;
        while (i < max)
        {
            String id = _helper.createWorkbook(getProjectName(), "Workbook" + i, "Description");
            workbookIds[i] = id;
            insertDummySampleRow(i.toString());
            i++;
        }

        _helper.goToLabHome();
        _helper.clickNavPanelItem("Samples:", "Browse All");

        DataRegionTable dr = new DataRegionTable("query", this);
        CustomizeView cv = dr.getCustomizeView();
        cv.openCustomizeViewPanel();
        cv.showHiddenItems();
        cv.addColumn("container");
        cv.applyCustomView();

        dr.setFilter("container", "Does Not Equal", getProjectName());
        dr.setSort("freezerid", SortDirection.DESC);

        i = max;
        while (i > 0)
        {
            i--;

            //first record for each workbook
            int rowNum = dr.getRow("Folder", "Workbook" + i);

            String containerPath = getProjectName() + "/" + workbookIds[i];

            // NOTE: These URLs should point to the workbook where the record was created, not the current folder
            // NOTE: URIUtil.encodePath(containerPath), used in buildRelativeUrl(), swaps + for space in the path.
            // As a hack, we put it back using replaceAll() to make the string comparisons work
            log("using container relative URLs: " + WebTestHelper.isUseContainerRelativeUrl());

            //details link
            String url = URLDecoder.decode(WebTestHelper.buildRelativeUrl("query", containerPath, "recordDetails"), StandardCharsets.UTF_8).replaceAll(" ", "+");
            String href = URLDecoder.decode(dr.detailsLink(rowNum).getAttribute("href"), StandardCharsets.UTF_8);
            assertTrue("Expected [details] link to go to the container: " + url + ", href was: " + href, href.contains(url));

            //update link
            url = URLDecoder.decode(WebTestHelper.buildRelativeUrl("ldk", containerPath, "manageRecord"), StandardCharsets.UTF_8).replaceAll(" ", "+");
            href = URLDecoder.decode(dr.getUpdateHref(rowNum), StandardCharsets.UTF_8);
            assertTrue("Expected [edit] link to go to the container: " + url + ", href was: " + href, href.contains(url));

            //sample type
            url = URLDecoder.decode(WebTestHelper.buildRelativeUrl("query", getProjectName(), "recordDetails"), StandardCharsets.UTF_8).replaceAll(" ", "+");
            href = URLDecoder.decode(getAttribute(Locator.linkWithText("DNA").index(rowNum), "href"), StandardCharsets.UTF_8);
            assertTrue("Expected sample type column URL to go to the container: " + url + ", href was: " + href, href.contains(url));
            assertTrue("Incorrect params in sample type URL: " + href, href.contains("schemaName=laboratory&query.queryName=sample_type&keyField=type&key=DNA"));

            //container column
            url = URLDecoder.decode(WebTestHelper.buildRelativeUrl("project", containerPath, "begin"), StandardCharsets.UTF_8).replaceAll(" ", "+");
            href = URLDecoder.decode(getAttribute(Locator.linkWithText("Workbook" + i), "href"), StandardCharsets.UTF_8);
            assertTrue("Expected container column to go to the container: " + url + ", href was:" + href, href.contains(url));
        }

        //Test DetailsPanel:
        log("Testing details panel");
        clickAndWait(dr.link(1, "Sample Id"));
        waitForText("Whole Blood");
        assertTextPresent("Sample1", "DNA", "Whole Blood", "Freezer");

        verifyFreezerColOrder();
    }

    private void verifyFreezerColOrder()
    {
        //NOTE: we expect these columns to respect the order defined in the schema XML file
        log("Verifying freezer column order");
        assertTextBefore("Location", "Freezer");
        assertTextBefore("Freezer", "Cane");
        assertTextBefore("Cane", "Box");
        assertTextBefore("Box", "Row");
        assertTextBefore("Row", "Column");
        assertTextBefore("Column", "Parent Sample");
    }

    private void insertDummySampleRow(String suffix)
    {
        Locator locator = _helper.toolIcon("Import Samples");
        waitAndClick(locator);
        //NOTE: we are in a workbook
        waitAndClickAndWait(Ext4Helper.Locators.menuItem("Samples"));

        waitForElementToDisappear(Locator.tagWithClass("div", "x4-window-body").notHidden().withText("Loading..."));
        waitForElement(Locator.name("freezer"));

        _helper.waitForField("Quantity");

        _helper.setFormField("samplename", "Sample" + suffix);
        _helper.setFormField("location", "location_" + _helper.getRandomInt());

        Ext4ComboRef.getForLabel(this, "Sample Type").setValue("DNA");
        Ext4FieldRef.getForLabel(this, "Sample Source").setValue("Whole Blood");

        sleep(150); //there's a buffer when committing changes
        waitAndClick(Ext4Helper.Locators.ext4ButtonEnabled("Submit"));

        new Window.WindowFinder(getDriver()).withTitle("Success").waitFor();
        assertTextPresent("Your upload was successful");
        _samplesTotal++;
        waitAndClickAndWait(Ext4Helper.Locators.ext4Button("OK"));

        _helper.goToLabHome();
    }

    private void peptideTableTest() throws Exception
    {
        log("Testing Peptide Table");
        _helper.goToLabHome();

        _helper.clickNavPanelItem("Peptides:", IMPORT_DATA_TEXT);
        new Window.WindowFinder(getDriver()).withTitle(IMPORT_DATA_TEXT).waitFor();
        waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Submit"));

        waitForElement(Locator.name("sequence"));

        String sequence = "Sv LFpT LLF";
        String name = "Peptide 1"; //spaces should get replaced with '_' on import

        _helper.setFormField("sequence", sequence + "123");
        _helper.setFormField("name", name);
        sleep(150); //there's a buffer when committing changes
        clickButton("Submit", 0);

        //test error conditions in trigger script
        String errorMsg = "Sequence can only contain valid amino acid characters: ARNDCQEGHILKMFPSTWYV*";
        new Window.WindowFinder(getDriver()).withTitle("Error").waitFor();
        assertTextPresent(errorMsg);
        clickButton("OK", 0);

        _helper.setFormField("sequence", sequence);
        sleep(150); //there's a buffer when committing changes
        clickButton("Submit", 0);

        new Window.WindowFinder(getDriver()).withTitle("Success").waitFor();
        assertTextPresent("Your upload was successful");
        _peptideTotal = 1;
        clickButton("OK", 0);

        _helper.goToLabHome();
        _helper.clickNavPanelItem("Peptides:", "Browse All");
        String text = sequence.toUpperCase().replaceAll(" ", "");
        waitForText(text);
        assertTrue("Sequence was not formatted properly on import", isTextPresent(text));
        assertFalse("Sequence was not formatted properly on import", isTextPresent(sequence));

        DataRegionTable dr = new DataRegionTable("query", this);
        assertEquals("Peptide Id not set correctly.", "1", dr.getDataAsText(0, "Peptide Id"));
        assertEquals("MW not set correctly.", "1036.1", dr.getDataAsText(0, "MW"));

        log("Attempting to double-insert the same peptide ID");
        try
        {
            Connection cn = WebTestHelper.getRemoteApiConnection();
            InsertRowsCommand insertCmd = new InsertRowsCommand("laboratory", "peptides");

            Map<String,Object> rowMap = new HashMap<>();
            rowMap.put("peptideId", 1);
            rowMap.put("sequence", "aaa");
            rowMap.put("_selfAssignedId_", "true");
            insertCmd.addRow(rowMap);

            SaveRowsResponse saveResp = insertCmd.execute(cn, getProjectName());
            throw new Exception("The saveRows call above should have thrown an exception");
        }
        catch (CommandException e)
        {
            assertEquals("A record is already present with ID: 1", e.getMessage());
        }
    }

    private void searchPanelTest()
    {
        _helper.goToLabHome();
        _helper.clickNavPanelItem("DNA_Oligos:", "Search");
        waitForTextToDisappear("Loading...");
        waitForElement(Locator.name("name"));
        sleep(50);
        _helper.setFormField("name", "TestPrimer");
        waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Submit"));
        DataRegionTable table = new DataRegionTable("query", this);
        assertEquals("Wrong number of rows found", _oligosTotal, table.getDataRowCount());

        //TODO: test different operators
        //also verify correct options show up on drop down menus
    }

    protected List<String> getEnabledModules()
    {
        List<String> modules = new ArrayList<>();
        modules.add("ELISPOT_Assay");
        modules.add("FlowAssays");
        modules.add("GenotypeAssays");
        modules.add("SequenceAnalysis");
        return modules;
    }

    private List<String> getSampleItems()
    {
        List<String> list = new ArrayList<>();
        list.add("Antibodies");
        list.add("DNA_Oligos");
        list.add("Peptides");
        list.add("Samples");
        return list;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    private void defaultAssayImportMethodTest()
    {
        log("verifying ability to set default import method");
        _helper.goToLabHome();
        waitAndClickAndWait(_helper.toolIcon("Settings"));
        waitForText("Set Assay Defaults");
        waitAndClickAndWait(Locator.linkContainingText("Set Assay Defaults"));
        String defaultVal = "UC Davis STR";
        _helper.waitForField(GENOTYPING_ASSAYNAME);
        ComboBox.ComboBox(getDriver()).withLabelContaining(GENOTYPING_ASSAYNAME)
                .find(getDriver())
                .selectComboBoxItem(defaultVal);
        waitAndClick(Ext4Helper.Locators.ext4Button("Submit"));

        new Window.WindowFinder(getDriver()).withTitle("Success").waitFor();
        waitAndClickAndWait(Ext4Helper.Locators.ext4Button("OK"));
        waitForText("Types of Data");
        _helper.goToAssayResultImport(GENOTYPING_ASSAYNAME, false);
        _helper.waitForField("Purpose");
        Ext4FieldRef.getForLabel(this, "Purpose").setValue("Testing");  //ensure form will be dirty to trigger alert
        Boolean state = (Boolean) Ext4FieldRef.getForBoxLabel(this, defaultVal).getValue();
        assertTrue("Default method not correct", state);

        waitForElement(Ext4Helper.Locators.ext4Button("Download Example Data"));
        _helper.waitForCmp("#upload");
        _ext4Helper.queryOne("#upload", Ext4CmpRef.class).waitForEnabled();
        boolean dirty = executeScript("return Ext4.ComponentQuery.query('laboratory-assayimportpanel')[0].isDirty()", Boolean.class);
        assertTrue("Assay form was not dirty", dirty);

        doAndWaitForPageToLoad(() ->
        {
            _ext4Helper.queryOne("#cancelBtn", Ext4CmpRef.class).waitForEnabled();
            waitAndClick(Ext4Helper.Locators.ext4Button("Cancel"));

            // note: even though when manually using FF this alert appears,
            // when run through selenium it does not show this onbeforeunload alert
            if (getBrowserType() != BrowserType.FIREFOX)
            {
                acceptAlert();
            }
        });
        waitForText(WAIT_FOR_PAGE, LabModuleHelper.LAB_HOME_TEXT);
    }

    @Override
    public void checkViews()
    {
        //the module contains an R report tied to a specific assay name, so view check fails when an assay of that name isnt present
        //when module-based assays can supply reports this should be corrected
    }
}

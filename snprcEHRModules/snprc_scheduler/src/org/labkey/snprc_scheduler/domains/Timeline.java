package org.labkey.snprc_scheduler.domains;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.labkey.api.collections.ArrayListMap;
import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.labkey.api.snd.Project;
import org.labkey.api.snd.SNDService;
import org.labkey.api.util.DateUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
/**
 * Created by thawkins on 9/13/2018.
 * <p>
 * Class for Timeline table data. Used when saving, updating, deleting and getting a Timeline table
 */

public class Timeline
{
    private Integer _timelineId;
    private String _description;
    private Date _startDate;
    private Date _endDate;
    private String _leadTechs; // comma separated list of technicians
    private String _objectId;
    private List<TimelineItem> _timelineItems = new ArrayList<>();
    private Project _project;

    public static final String TIMELINE_ID = "TimelineId";
    public static final String TIMELINE_DESCRIPTION = "Description";
    public static final String TIMELINE_STARTDATE = "StartDate";
    public static final String TIMELINE_ENDDATE = "EndDate";
    public static final String TIMELINE_OBJECTID = "ObjectId";
    public static final String TIMELINE_CONTAINER = "Container";
    public static final String TIMELINE_LEAD_TECHS = "LeadTechs";
    public static final String TIMELINE_PROJECT = "Project";


    public Timeline( Container c, User u,
                    @Nullable Integer timelineId,
                    @Nullable Integer projectId,
                    @Nullable Integer revNum,
                    String description,
                    Date startDate,
                    @Nullable Date endDate)
    {
        _timelineId = timelineId;
        _description = description;
        _startDate = startDate;
        _endDate = endDate;

        if (projectId != null && revNum != null)  {
            setProject(c, u, projectId, revNum);
        }
    }

    public Timeline()
    {
    }

    public Integer getTimelineId()
    {
        return _timelineId;
    }

    public void setTimelineId(Integer timelineId)
    {
        _timelineId = timelineId;
    }

    public String getDescription()
    {
        return _description;
    }

    public void setDescription(String description)
    {
        _description = description;
    }

    public Date getStartDate()
    {
        return _startDate;
    }

    @Nullable
    public String startDateToString()
    {

        return DateUtil.formatDateISO8601(getStartDate());
    }

    public void setStartDate(Date startDate)
    {
        _startDate = startDate;
    }

    public Date getEndDate()
    {
        return _endDate;
    }

    @Nullable
    public String endDateToString()
    {

        return DateUtil.formatDateISO8601(getEndDate());
    }

    public void setEndDate(Date endDate)
    {
        _endDate = endDate;
    }

    public String getObjectId()
    {
        return _objectId;
    }

    public void setObjectId(String objectId)
    {
        _objectId = objectId;
    }


    public Project getProject()
    {
        return _project;
    }

    public void setProject(Container c, User u, Integer projectId, Integer revNum)
    {
        if (projectId != null && revNum != null)  {
            _project = SNDService.get().getProject(c, u, projectId, revNum);
        }
    }
    public void setProject(Project project)
    {
        _project = project;
    }

    public String getLeadTechs()
    {
        return _leadTechs;
    }

    public void setLeadTechs(String leadTechs)
    {
        _leadTechs = leadTechs;
    }

    @NotNull
    public Map<String, Object> getTimelineRow(Container c)
    {
        Map<String, Object> timelineValues = new ArrayListMap<>();
        timelineValues.put(TIMELINE_ID, getTimelineId());
        timelineValues.put(TIMELINE_DESCRIPTION, getDescription());
        timelineValues.put(TIMELINE_STARTDATE, getStartDate());
        timelineValues.put(TIMELINE_ENDDATE, getEndDate());
        timelineValues.put(TIMELINE_OBJECTID, getObjectId());
        timelineValues.put(TIMELINE_CONTAINER, c.getId());
        timelineValues.put(TIMELINE_LEAD_TECHS, getLeadTechs());



        if (_project != null)
        {

            Map<String, Object> project = getProject().getProjectRow(c);
            for (String key : project.keySet())
            {
                timelineValues.put(key, project.get(key));
            }
        }
        return timelineValues;
    }

    @NotNull
    public JSONObject toJSON(Container c, User u)
    {
        JSONObject json = new JSONObject();
        json.put(TIMELINE_ID, getTimelineId());
        json.put(TIMELINE_DESCRIPTION, getDescription());
        json.put(TIMELINE_STARTDATE, getStartDate());
        json.put(TIMELINE_CONTAINER, c.getId());
        if (getTimelineId() != null)
            json.put(TIMELINE_ID, getTimelineId());
        if (getEndDate() != null)
            json.put(TIMELINE_ENDDATE, getEndDate());
        if (getObjectId() != null)
          json.put(TIMELINE_OBJECTID, getObjectId());

        // add timeline items to json object

//        if (getTimelineItems(getTimelineId()).size() > 0)
//        {
//            JSONArray jsonTimelineItems = new JSONArray();
//            for (TimelineItem tiemlineItem : getTimelineItems())
//            {
//                jsonTimelineItems.put(timelineItem.toJSON(c, u));
//            }
//            json.put(TIMELINE_ITEMS, jsonTimelineItems);
//        }
        if (getProject() != null)
            json.put(TIMELINE_PROJECT, getProject().toJSON(c, u));

        return json;
    }

}
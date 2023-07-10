package com.slotegrator.projects.TMHRV2

import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.worklog.WorklogManager
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.component.ComponentAccessor
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.tempoplugin.common.TempoDateTimeFormatter
import com.tempoplugin.core.datetime.api.TempoDate
import com.tempoplugin.worklog.v4.rest.InputWorklogsFactory
import com.tempoplugin.worklog.v4.rest.TimesheetWorklogBean
import com.tempoplugin.worklog.v4.services.WorklogService
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.worklog.Worklog
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.event.type.EventDispatchOption;
import java.sql.Timestamp
import java.util.Calendar
import org.joda.time.LocalDate
import com.slotegrator.projects.TMHRV2.HolidayThisYear





//Issue issue = ComponentAccessor.getIssueManager().getIssueObject('')

/*  JS-7086 - New Class HolidayThisYear
Вместе с датами сюда не забыть внести плавающие даты в темпо: https://jira.platform.live/secure/Tempo.jspa#/settings/holidays
в 2023 в 2023 плавающие даты были толкьо у пасхи 7-10 апреля
List<Timestamp> holidays2023 = [
        new Timestamp(123, 03, 07, 0, 0, 0, 0), //7 апреля
        new Timestamp(123, 03, 10, 0, 0, 0, 0), //10 апреля
        new Timestamp(123, 04, 01, 0, 0, 0, 0), //1 мая
        new Timestamp(123, 04, 8, 0, 0, 0, 0), //8 мая
        new Timestamp(123, 05, 05, 0, 0, 0, 0), //5 июля
        new Timestamp(123, 05, 06, 0, 0, 0, 0), //6 июля
        new Timestamp(123, 8, 28, 0, 0, 0, 0), //28 сентября
        new Timestamp(123, 10, 17, 0, 0, 0, 0), //17 ноября
        new Timestamp(123, 11, 25, 0, 0, 0, 0), //25 декабря
        new Timestamp(123, 11, 26, 0, 0, 0, 0), //26 декабря
        //new Timestamp(122, 11, 28, 0, 0, 0, 0) // test holiday
]
*/
List<Timestamp> holidays = new HolidayThisYear().holidays
ApplicationUser automationUser = ComponentAccessor.getUserManager().getUserByName("automation");
CustomFieldManager cfm = ComponentAccessor.getCustomFieldManager();



def currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
String currentUserName = currentUser.getUsername();
Issue tmhrIssue = issue;
ApplicationUser reporterUser = tmhrIssue.getReporter()

try{
    ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(automationUser);
    Timestamp start_date_timestamp = cfm.getCustomFieldObject(11016L).getValue(issue)


    def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
    def userCardSearchJQL = jqlQueryParser.parseQuery("project = AC and Username  ~ ${tmhrIssue.getReporter().getUsername()} and status = 'In Progress'")
    List<Issue> issues = ComponentAccessor.getComponent(SearchService).search(automationUser, userCardSearchJQL, PagerFilter.getUnlimitedFilter()).getResults()
    Issue cardIssue = issues.get(0);    
    String userGroupCheck = ComponentAccessor.groupManager.isUserInGroup(reporterUser, 'Support Position')
    BigDecimal hoursInWorkDay = 0
    def daysToDecrease = 0; 

    def duedate_minus_start_date =  issue.getDueDate() - start_date_timestamp
    Calendar calendar = Calendar.getInstance()

    // Get the day of the week as an integer (Sunday = 1, Monday = 2, etc.)
    String issue_type = issue.getIssueType().getName()

    //Home Office
    if(issue_type == 'Home Office') {
        for (int i = 0; i <= duedate_minus_start_date; i++) {
            daysToDecrease++;
            logTimeTempo(1, 'INT2-7', start_date_timestamp, i, issue)  // 1 - 1 sec,
            //INT2-7 - task for logging,
            //start_date_timestamp -  timestamp from customfiled Start Date (11016),
            //i - loop_day,
            //issue - transition issue
        }

        if(daysToDecrease > 0){
            CustomField vacationCountFieldObject =  cfm.getCustomFieldObject(12902);
            double vacationsLeft = (double) cardIssue.getCustomFieldValue(vacationCountFieldObject)
            vacationsLeft -= daysToDecrease;
            MutableIssue cardIssueToUpate = ComponentAccessor.getIssueManager().getIssueByCurrentKey(cardIssue.getKey());
            cardIssueToUpate.setCustomFieldValue(vacationCountFieldObject,vacationsLeft );
            ComponentAccessor.getIssueManager().updateIssue(automationUser, cardIssueToUpate, EventDispatchOption.ISSUE_UPDATED, false);
        }

    //Vacantion
    } else if(issue_type == 'Vacation') {
        if (userGroupCheck == "true"){
            hoursInWorkDay = 7.5
        } else {
            hoursInWorkDay = 8
        }
        calendar.setTime(start_date_timestamp)
        int dayOfWeeks = calendar.get(Calendar.DAY_OF_WEEK)        
        Double timeOfAbsence = cfm.getCustomFieldObject(11100L).getValue(issue) as Double;
           log.error( "timeOfAbsence--> " + timeOfAbsence + " hoursInWorkDay--> " + hoursInWorkDay )
        int timeOfAbsenceSec = (timeOfAbsence * 3600) as int
        if (duedate_minus_start_date == 0){
            if (!((dayOfWeeks == 1) || (dayOfWeeks == 7) || start_date_timestamp in holidays )) { // 1 - SUN; 7 SUT
                daysToDecrease = timeOfAbsence / hoursInWorkDay
                log.error("daysToDecrease--> " + daysToDecrease + " timeOfAbsence--> " + timeOfAbsence + " hoursInWorkDay--> " + hoursInWorkDay )
                logTimeTempo(timeOfAbsenceSec, 'INT-1', start_date_timestamp, 0, issue)
            }
        } else {
            for (int i = 0; i <= duedate_minus_start_date; i++) {
                calendar.setTime(start_date_timestamp + i)
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                //int x = dayOfWeek + i
                //log.warn 'bool: '+ (dayOfWeek + i == 7) + ' count: ' + x
                if ((dayOfWeek == 1) || (dayOfWeek == 7) || start_date_timestamp + i in holidays ) { // 1 - SUN; 7 SUT
                    continue;
                }
                daysToDecrease++;
                logTimeTempo(28800, 'INT-1', start_date_timestamp, i, issue)  // 28800 - 8 hours,
                //INT-1 - task for logging,
                //start_date_timestamp -  timestamp from customfiled Start Date (11016),
                //i - loop_day,
                //issue - transition issue
            }
        }
        Object remainingVacationDays = cfm.getCustomFieldObject(13302L)
        Object remainingFamilyDays = cfm.getCustomFieldObject(13304L)
        Double remainingVacationDaysValue = remainingVacationDays.getValue(cardIssue)
        Double remainingFamilyDaysValue = remainingFamilyDays.getValue(cardIssue)
        if(daysToDecrease > 0){
            log.error("daysToDecrease" + daysToDecrease )
            log.error ("remainingVacationDaysValue >= timeOfAbsence" + (remainingVacationDaysValue >= timeOfAbsence))
            if (remainingVacationDaysValue  >= timeOfAbsence ){            
                remainingVacationDaysValue -= timeOfAbsence
                MutableIssue cardIssueToUpate = ComponentAccessor.getIssueManager().getIssueByCurrentKey(cardIssue.getKey());
                cardIssueToUpate.setCustomFieldValue(remainingVacationDays, remainingVacationDaysValue );
                ComponentAccessor.getIssueManager().updateIssue(automationUser, cardIssueToUpate, EventDispatchOption.ISSUE_UPDATED, false);
            } else {
                remainingFamilyDaysValue = remainingFamilyDaysValue - (timeOfAbsence - remainingVacationDaysValue)
                remainingVacationDaysValue = 0
                MutableIssue cardIssueToUpate = ComponentAccessor.getIssueManager().getIssueByCurrentKey(cardIssue.getKey());

                cardIssueToUpate.setCustomFieldValue(remainingVacationDays, remainingVacationDaysValue );
                ComponentAccessor.getIssueManager().updateIssue(automationUser, cardIssueToUpate, EventDispatchOption.ISSUE_UPDATED, false);

                cardIssueToUpate.setCustomFieldValue(remainingFamilyDays, remainingFamilyDaysValue );
                ComponentAccessor.getIssueManager().updateIssue(automationUser, cardIssueToUpate, EventDispatchOption.ISSUE_UPDATED, false);
            }           
        }

    }else if(issue_type == 'At own expense') {
        for (int i = 0; i <= 6; i++) {

            logTimeTempo(1, 'INT2-3', start_date_timestamp, i, issue)  // 1 - 1 sec,
            //INT2-3 - task for logging,
            //start_date_timestamp -  timestamp from customfiled Start Date (11016),
            //i - loop_day,
            //issue - transition issue
        }

    } else if(issue_type == 'Paid time off') {
        for (int i = 0; i <= duedate_minus_start_date; i++) {

            logTimeTempo(1, 'INT2-1', start_date_timestamp, i, issue)  // 1 - 1 sec,
            //INT2-1 - task for logging,
            //start_date_timestamp -  timestamp from customfiled Start Date (11016),
            //i - loop_day,
            //issue - transition issue

        }

    } else if(issue_type == 'Sick day') {
        for (int i = 0; i <= duedate_minus_start_date; i++) {
            daysToDecrease++;

            logTimeTempo(1, 'INT-2', start_date_timestamp, i, issue)  // 1 - 1 sec,
            //INT-2 - task for logging,
            //start_date_timestamp -  timestamp from customfiled Start Date (11016),
            //i - loop_day,
            //issue - transition issue

        }
        //log.warn 'counter daysToDecrease = ' + daysToDecrease.toString()
        if(daysToDecrease > 0){
            // This block was added 2023-06-12 issue JS-7204 
            Double timeOfAbsence = cfm.getCustomFieldObject(11100L).getValue(issue) as Double;
            CustomField sickDayObject  = cfm.getCustomFieldObject(13306L);
            double sickDayLeft = (double) cardIssue.getCustomFieldValue(sickDayObject)
            sickDayLeft -= timeOfAbsence
            MutableIssue cardIssueToUpate = ComponentAccessor.getIssueManager().getIssueByCurrentKey(cardIssue.getKey())
            cardIssueToUpate.setCustomFieldValue(sickDayObject, sickDayLeft );
            ComponentAccessor.getIssueManager().updateIssue(automationUser, cardIssueToUpate, EventDispatchOption.ISSUE_UPDATED, false)            
            
            /* use old field
            CustomField sickDayObject  = cfm.getCustomFieldObject(12901L);
            double sickDayLeft = (double) cardIssue.getCustomFieldValue(sickDayObject)
            sickDayLeft -= daysToDecrease;
            MutableIssue cardIssueToUpate = ComponentAccessor.getIssueManager().getIssueByCurrentKey(cardIssue.getKey());
            cardIssueToUpate.setCustomFieldValue(sickDayObject, sickDayLeft );
            ComponentAccessor.getIssueManager().updateIssue(automationUser, cardIssueToUpate, EventDispatchOption.ISSUE_UPDATED, false);
            */
        }

    } else if(issue_type == 'Weekend Overtime') {
        for (int i = 0; i <= duedate_minus_start_date; i++) {

            logTimeTempo(1, 'INT2-10', start_date_timestamp, i, issue)  // 1 - 1 sec,
            //INT2-10 - task for logging,
            //start_date_timestamp -  timestamp from customfiled Start Date (11016),
            //i - loop_day,
            //issue - transition issue

        }

    } else {
        log.warn("Error ! Automation rules for this type are not set!")
    }

} catch (Exception e){
    //log.warn(e);
} finally {
    ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(currentUser);
}


def logTimeTempo(Long timeToLog, String issueKeyforLog, Timestamp startDate, int loopDay, Issue currentIssue) {
    @WithPlugin('is.origo.jira.tempo-plugin')

    @PluginModule
    WorklogService worklogService

    @PluginModule
    InputWorklogsFactory inputWorklogsFactory

    Issue issue_for_logging = ComponentAccessor.getIssueManager().getIssueObject(issueKeyforLog)

    WorklogManager worklogManager = ComponentAccessor.worklogManager
    ApplicationUser reporter = currentIssue.reporter

    //for logging
    LocalDate start_date_LocalDate = new LocalDate((startDate + loopDay).getYear() + 1900, (startDate + loopDay).getMonth() + 1, (startDate + loopDay).getDate())
    TempoDate start_date_TempoDate = TempoDate.ofLocalDate(start_date_LocalDate)
    String start_date_String = TempoDateTimeFormatter.formatTempoDate(start_date_TempoDate)

    //for comment
    LocalDate due_date_LocalDate = new LocalDate(currentIssue.getDueDate().getYear() + 1900, currentIssue.getDueDate().getMonth() + 1, currentIssue.getDueDate().getDate())
    TempoDate due_date_TempoDate = TempoDate.ofLocalDate(due_date_LocalDate)
    String due_date_String = TempoDateTimeFormatter.formatTempoDate(due_date_TempoDate)

    //for comment
    LocalDate start_date_LocalDate_currentIssue = new LocalDate(startDate.getYear() + 1900, startDate.getMonth() + 1, startDate.getDate())
    TempoDate start_date_TempoDate_currentIssue = TempoDate.ofLocalDate(start_date_LocalDate_currentIssue)
    String start_date_String_currentIssue = TempoDateTimeFormatter.formatTempoDate(start_date_TempoDate_currentIssue)

    String comment = "${currentIssue.getIssueType().getName()} from ${start_date_String_currentIssue} to ${due_date_String} https://jira.platform.live/browse/${currentIssue.key}"

    // Add all fields needed to create a new worklog
    TimesheetWorklogBean timesheetWorklogBean = new TimesheetWorklogBean.Builder()
            .issueIdOrKey(issue_for_logging.key)
            .startDate(start_date_String)
            .workerKey(reporter.key)
            .timeSpentSeconds(timeToLog)
            .comment(comment)
            .build()

    def inputWorklogs = inputWorklogsFactory.buildForCreate(timesheetWorklogBean)
    worklogService.createTempoWorklogs(inputWorklogs)

    // Update timeSpent value of the issue
    def worklogsTimeSpentTotal = worklogManager.getByIssue(issue_for_logging).sum { (it as Worklog).timeSpent } as Long
    issue_for_logging.timeSpent = worklogsTimeSpentTotal
}

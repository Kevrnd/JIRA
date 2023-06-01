//Скрипт перевод дней в часы исходя из рабочего дня, группы пользователя,  и запись значений в новые поля
import java.math.BigDecimal
import java.lang.Double
import com.atlassian.jira.issue.issuetype.IssueType
import com.atlassian.jira.bc.project.component.ProjectComponent
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.config.ConstantsManager
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.issue.IssueManager
import com.atlassian.query.Query
import com.atlassian.jira.issue.fields.FieldManager
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.project.Project
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption



Collection<MutableIssue> searchIssue (String jqlSearch) {
    IssueManager issueManager = ComponentAccessor.getIssueManager()
    ApplicationUser automationUser = ComponentAccessor.userManager.getUserByName('automation')
    def searchService = ComponentAccessor.getComponentOfType(SearchService.class)
    def parseResult = searchService.parseQuery(automationUser, jqlSearch)
    if (!parseResult.valid) {
        log.error("Invalid JQL: ${jqlSearch}")
        return
    }
    Object searchResults  = searchService.search(automationUser, parseResult.query, PagerFilter.unlimitedFilter) //new PagerFilter(50))  //PagerFilter.unlimitedFilter)
    Collection issues = searchResults.getResults().collect { is -> issueManager.getIssueObject(is.getId()) }
    return issues
}


IssueManager issueManager = ComponentAccessor.getIssueManager()
//MutableIssue issue = issueManager.getIssueObject("AC-14")
ApplicationUser automationUser = ComponentAccessor.userManager.getUserByName('automation')


String projectACJQL = "project=AC and status = 'In Progress'"
Collection cards = searchIssue (projectACJQL)

for (issue in cards){
    CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager()
    CustomField usernameField = customFieldManager.getCustomFieldObject(11014L)
    CustomField vacantionRemainingField = customFieldManager.getCustomFieldObject(12900L)
    CustomField vacantionRemainingHRField = customFieldManager.getCustomFieldObject(13204L)
    CustomField familyDaysField = customFieldManager.getCustomFieldObject(13100L)
    CustomField familyDaysHRField = customFieldManager.getCustomFieldObject(13203L)
    CustomField sickDaysField = customFieldManager.getCustomFieldObject(12901L)

    CustomField hoursvacantionRemainingField = customFieldManager.getCustomFieldObject(13301L)
    CustomField hoursvacantionRemainingHRField = customFieldManager.getCustomFieldObject(13302L)
    CustomField hoursfamilyDaysField = customFieldManager.getCustomFieldObject(13303L)
    CustomField hoursfamilyDaysHRField = customFieldManager.getCustomFieldObject(13304L)
    CustomField hourssickDaysField = customFieldManager.getCustomFieldObject(13305L)

    String usernameFieldValue = usernameField.getValue(issue)
    String userGroup = ComponentAccessor.groupManager.isUserInGroup(usernameFieldValue, 'Support Position')
    log.error ("userGroup  -- >" + userGroup)
    if (userGroup == "true"){
        BigDecimal newValueVacantionRemaining =(vacantionRemainingField.getValue(issue) ?: 0) * 7.50
        BigDecimal newValueVacantionRemainingHR = (vacantionRemainingHRField.getValue(issue)?: 0) * 7.50 
        BigDecimal newValuefamilyDays = (familyDaysField.getValue(issue)?: 0.0) * 7.50
        BigDecimal newValuefamilyDaysHR = (familyDaysHRField.getValue(issue)?: 0.0) * 7.50 
        BigDecimal newValuesickDays = (sickDaysField.getValue(issue) ?: 0.0) * 7.50   

        MutableIssue cardIssueToUpate = ComponentAccessor.getIssueManager().getIssueByCurrentKey(issue.getKey())
        cardIssueToUpate.setCustomFieldValue(hoursvacantionRemainingField, newValueVacantionRemaining as Double)
        cardIssueToUpate.setCustomFieldValue(hoursvacantionRemainingHRField, newValueVacantionRemainingHR as Double)
        cardIssueToUpate.setCustomFieldValue(hoursfamilyDaysField, newValuefamilyDays  as Double)
        cardIssueToUpate.setCustomFieldValue(hoursfamilyDaysHRField, newValuefamilyDaysHR as Double)
        cardIssueToUpate.setCustomFieldValue(hourssickDaysField, newValuesickDays as Double)
        ComponentAccessor.getIssueManager().updateIssue(automationUser, cardIssueToUpate, EventDispatchOption.ISSUE_UPDATED, false)       
        
    } else {
        Double newValueVacantionRemaining = (vacantionRemainingField.getValue(issue)?: 0) * 8
        Double newValueVacantionRemainingHR = (vacantionRemainingHRField.getValue(issue)?: 0) * 8 
        Double newValuefamilyDays = (familyDaysField.getValue(issue)?: 0) * 8 
        Double newValuefamilyDaysHR = (familyDaysHRField.getValue(issue)?: 0) * 8 
        Double newValuesickDays = (sickDaysField.getValue(issue)?: 0) * 8 

        MutableIssue cardIssueToUpate = ComponentAccessor.getIssueManager().getIssueByCurrentKey(issue.getKey())
        cardIssueToUpate.setCustomFieldValue(hoursvacantionRemainingField, newValueVacantionRemaining as Double)
        cardIssueToUpate.setCustomFieldValue(hoursvacantionRemainingHRField, newValueVacantionRemainingHR as Double)
        cardIssueToUpate.setCustomFieldValue(hoursfamilyDaysField, newValuefamilyDays  as Double)
        cardIssueToUpate.setCustomFieldValue(hoursfamilyDaysHRField, newValuefamilyDaysHR as Double)
        cardIssueToUpate.setCustomFieldValue(hourssickDaysField, newValuesickDays as Double)
        ComponentAccessor.getIssueManager().updateIssue(automationUser, cardIssueToUpate, EventDispatchOption.ISSUE_UPDATED, false)
    }
}    

//..............................................................................................................................................................................
//..............................................................................................................................................................................
//behaviours Инициализатор
import com.atlassian.jira.util.json.JSONObject
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.component.ComponentAccessor;

import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.user.ApplicationUser;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.event.type.EventDispatchOption;

import com.slotegrator.projects.TMHRV2.UserVacationReplacementUtil;

def timeOfAbsense = getFieldByName("Time of absence")
def reqType = getFieldByName("Issue Type").getFormValue()


//if sick day
if(reqType == 10704){
    //timeOfAbsense.setFormValue("8")
}

//renaming fields
getFieldByName("Due Date").setLabel("End Date")

def currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()

///calculate vacations for replacement
try{
    ApplicationUser automationUser = ComponentAccessor.getUserManager().getUserByName("automation");
    CustomField teamLeadFieldObject = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("Team Lead").iterator().next();
    CustomField projectManagerFieldObject = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("Project manager").iterator().next();
    CustomField replacementFieldObject = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("Replacement").iterator().next();

    

    String currentUserName = currentUser.getUsername();

    def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)

    def userCardSearchJQL = jqlQueryParser.parseQuery("project = AC and Username  ~ ${currentUserName} and status = 'In Progress'")
    List<Issue> issues = ComponentAccessor.getComponent(SearchService).search(automationUser, userCardSearchJQL, PagerFilter.getUnlimitedFilter()).getResults()

    Issue cardIssue = issues.get(0);

    try{
        ApplicationUser replacement = (ApplicationUser)cardIssue.getCustomFieldValue(replacementFieldObject);

        String repalcementUsername = replacement.getUsername();

        UserVacationReplacementUtil userVacationReplacementUtil = new UserVacationReplacementUtil();
        userVacationReplacementUtil.clearVacationsFromVacationMap(repalcementUsername);
        ArrayList<JSONObject> vacationsForUser = userVacationReplacementUtil.getVacationsForUser(repalcementUsername);
        String vacationsString = "Replacement vacations : <br>";
        for(JSONObject vacationBean in vacationsForUser ){
            if(( "" + vacationBean.get("summary")).contains("Vacation")){
                vacationsString += "" + vacationBean.get("summary") + " <br>"
            }
        }

        getFieldByName("Replacement").setFormValue(replacement.getUsername());
        getFieldByName("Replacement").setDescription("" + vacationsString);
        getFieldByName("Replacement").setReadOnly(true);


        /*getFieldByName("Issue Type").setDescription("Setting replacement $repalcementUsername"+ 
        " for user $currentUserName vacationsForUser $vacationsForUser");
        */

        //getFieldByName("Issue Type").setDescription("replacing is $repalcementUsername "+vacationsForUser)
        //ComponentAccessor.getUserPropertyManager().getPropertySet(currentUser).setString("jira.meta.replacementUsername", repalcementUsername);
        //ComponentAccessor.getUserPropertyManager().getPropertySet(currentUser).setString("jira.meta.replacementVacations", vacationsForUser?.toString());

        //getFieldByName("Issue Type").setDescription("replacing is $repalcementUsername "+vacationsForUser)

    }catch (Exception e){

    }


    
}catch (Exception e){
    //getFieldByName("Issue Type").setDescription("error is " + e )
} finally {
    ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(currentUser);
}



//..............................................................................................................................................................................
//behaviours Изменния в IssueType 
import java.lang.Double
import java.lang.Integer
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.component.ComponentAccessor;

import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.user.ApplicationUser;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.web.bean.PagerFilter;

def reqType = getFieldByName("Issue Type").getFormValue()
ApplicationUser automationUser = ComponentAccessor.getUserManager().getUserByName("automation");






def currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()

def remainingHomeOfficeDays = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12902L);
def remainingVacationDays = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(13301L);
def remainingFamilyDays = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(13303L);
def remainingSickDays = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(13305L);




String currentUserName = currentUser.getUsername();
String userGroupCheck = ComponentAccessor.groupManager.isUserInGroup(currentUserName, 'Support Position')

def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)

def userCardSearchJQL = jqlQueryParser.parseQuery("project = AC and Username  ~ ${currentUserName} and status = 'In Progress'")
List<Issue> issues = ComponentAccessor.getComponent(SearchService).search(automationUser, userCardSearchJQL, PagerFilter.getUnlimitedFilter()).getResults()
Issue cardIssue = issues.get(0);
def issuetype = getFieldByName("Issue Type")
//change double
Double remainingVacationDaysValue = remainingVacationDays.getValue(cardIssue)
Double remainingFamilyDaysValue = remainingFamilyDays.getValue(cardIssue)
Integer remainingHomeOfficeDaysValue = remainingHomeOfficeDays.getValue(cardIssue)
Double remainingSickDaysValue = remainingSickDays.getValue(cardIssue)
//Vacation
if(reqType == 10706){
    //У пользователей группы support рабочий день - 7.5 часов, у остальных - 8 часов
    if (userGroupCheck == "true"){
        if ((remainingVacationDaysValue != null ) || (remainingFamilyDaysValue != null) ) {
            issuetype.setDescription("You have ${remainingVacationDaysValue / 7.5} vacation days left and ${remainingFamilyDaysValue / 7.5 } Family days left")
        } 
        if (remainingVacationDaysValue == 0 && remainingFamilyDaysValue ==0) {
            issuetype.setDescription("You don't have vacation days or family days available")
        }
    } else {
        if ((remainingVacationDaysValue != null ) || (remainingFamilyDaysValue != null) ) {
            issuetype.setDescription("You have ${remainingVacationDaysValue / 8 } vacation days left and ${remainingFamilyDaysValue / 8 } Family days left")
        } 
        if (remainingVacationDaysValue == 0 && remainingFamilyDaysValue ==0) {
            issuetype.setDescription("You don't have vacation days or family days available")
        }

    }
}
//Home Office
if(reqType == 10703){
    if (remainingHomeOfficeDaysValue != null) {
        issuetype.setDescription("You have ${remainingHomeOfficeDaysValue} home office days left")
        }
}
//Sick Days
if(reqType == 10704){
    if (remainingSickDaysValue != null) {
        //У пользователей группы support рабочий день - 7.5 часов, у остальных - 8 часов
        if (userGroupCheck == "true"){
            issuetype.setDescription("You have ${remainingSickDaysValue / 7.5 } sick days left")
        } else {
            issuetype.setDescription("You have ${remainingSickDaysValue / 8 } sick days left")
        }
    }
}



//..............................................................................................................................................................................?
//Validator create Issue

package com.slotegrator.projects.TMHRV2
import java.lang.Double
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.component.ComponentAccessor
import com.opensymphony.workflow.InvalidInputException
import java.sql.Timestamp
import java.util.Calendar
import com.slotegrator.projects.TMHRV2.HolidayThisYear



if (issue.issueType.name != 'Vacation' ) {
    return
}
List<Timestamp> holidays = new HolidayThisYear().holidays
def daysToDecrease = 0
BigDecimal hoursInWorkDay = 0 
ApplicationUser automationUser = ComponentAccessor.getUserManager().getUserByName("automation")
ApplicationUser currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
String currentUserName = currentUser.getUsername()
String userGroupCheck = ComponentAccessor.groupManager.isUserInGroup(currentUserName, 'Support Position')


Timestamp dueDate_date_timestamp = issue.getDueDate()
CustomFieldManager cfm = ComponentAccessor.getCustomFieldManager()
Object customFieldStartDate = cfm.getCustomFieldObject(11016L)  // change
Object remainingVacationDays = cfm.getCustomFieldObject(13301L)   // change
Object remainingFamilyDays = cfm.getCustomFieldObject(13303L)     //change
Object customFieldTimeofAbsence = cfm.getCustomFieldObject(11100L)
Calendar calendar = Calendar.getInstance()
Timestamp start_date_timestamp = issue.getCustomFieldValue(customFieldStartDate)
def duedate_minus_start_date = issue.getDueDate() - start_date_timestamp

def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
def userCardSearchJQL = jqlQueryParser.parseQuery("project = AC and Username  ~ ${currentUserName} and status = 'In Progress'")
List<Issue> issues = ComponentAccessor.getComponent(SearchService).search(automationUser, userCardSearchJQL, PagerFilter.getUnlimitedFilter()).getResults()
Issue cardIssue = issues.get(0)
Double remainingVacationDaysValue = remainingVacationDays.getValue(cardIssue)
Double remainingFamilyDaysValue = remainingFamilyDays.getValue(cardIssue)

calendar.setTime(start_date_timestamp)
    int dayOfWeeks = calendar.get(Calendar.DAY_OF_WEEK)
    BigDecimal timeOfAbsence = issue.getCustomFieldValue(customFieldTimeofAbsence) as BigDecimal;// change double
    if (userGroupCheck == "true"){
        hoursInWorkDay = 7.5
    } else {
        hoursInWorkDay = 8
    }
    
    //BigDecimal timeOfAbsenceSec = timeOfAbsence*3600;     
    if (duedate_minus_start_date == 0){
        if (!((dayOfWeeks == 1) || (dayOfWeeks == 7) || start_date_timestamp in holidays )) { // 1 - SUN; 7 SUT
            if ((remainingVacationDaysValue + remainingFamilyDaysValue)  < timeOfAbsence ){
                throw new InvalidInputException("Количество дней отпуска в задаче превышает имеющейся у вас остаток.")
            }
            log.error("timeOfAbsence / hoursInWorkDay" + !((timeOfAbsence / hoursInWorkDay == 0.5) || (timeOfAbsence / hoursInWorkDay == 1)))
            if ( !((timeOfAbsence / hoursInWorkDay == 0.5) || (timeOfAbsence / hoursInWorkDay == 1))) {
                throw new InvalidInputException("Вы можете взять только половину дня или целый день отпуска.")
            }

        } else {
            throw new InvalidInputException("Вы хотите взять отпуск в выходной или праздничный день.")
        }
    } else {
        for (int i = 0; i <= duedate_minus_start_date; i++) {
            calendar.setTime(start_date_timestamp + i)
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            if ((dayOfWeek == 1) || (dayOfWeek == 7) || start_date_timestamp + i in holidays ) { // 1 - SUN; 7 SUT
                continue
            }
            daysToDecrease++
            }
    }


if (daysToDecrease > 0){
    if ((remainingVacationDaysValue + remainingFamilyDaysValue) <  (daysToDecrease * hoursInWorkDay/2)){
        throw new InvalidInputException("Количество дней отпуска в задаче превышает имеющейся у вас остаток.")
    }
    if ( !(timeOfAbsence == daysToDecrease * hoursInWorkDay || timeOfAbsence == daysToDecrease * hoursInWorkDay - hoursInWorkDay * 0.5) ){
        throw new InvalidInputException('Проверьте правильность заполнения поля "Time of absence in hours"!')
    }
}







//..........................................................................POST FUNCTION....................................................................................................?
//POST FUNCTION com/slotegrator/projects/TMHRV2/logTempoTimeFromTMHR.groovy






package com.slotegrator.projects.TMHRV2

import java.lang.Double
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





Issue issue = ComponentAccessor.getIssueManager().getIssueObject('TMHRV2-4164')
//Вместе с датами сюда не забыть внести плавающие даты в темпо: https://jira.platform.live/secure/Tempo.jspa#/settings/holidays
//в 2023 в 2023 плавающие даты были толкьо у пасхи 7-10 апреля
/*
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

try{
    ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(automationUser);
    Timestamp start_date_timestamp = cfm.getCustomFieldObject(11016L).getValue(issue)


    def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
    def userCardSearchJQL = jqlQueryParser.parseQuery("project = AC and Username  ~ ${tmhrIssue.getReporter().getUsername()} and status = 'In Progress'")
    List<Issue> issues = ComponentAccessor.getComponent(SearchService).search(automationUser, userCardSearchJQL, PagerFilter.getUnlimitedFilter()).getResults()
    Issue cardIssue = issues.get(0);    
    String userGroupCheck = ComponentAccessor.groupManager.isUserInGroup(currentUserName, 'Support Position')
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
        int timeOfAbsenceSec = timeOfAbsence as int * 3600
        if (duedate_minus_start_date == 0){
            if (!((dayOfWeeks == 1) || (dayOfWeeks == 7) || start_date_timestamp in holidays )) { // 1 - SUN; 7 SUT
                daysToDecrease = timeOfAbsence / hoursInWorkDay
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
        Object remainingVacationDays = cfm.getCustomFieldObject(13301L)
        Object remainingFamilyDays = cfm.getCustomFieldObject(13303L)
        Double remainingVacationDaysValue = remainingVacationDays.getValue(cardIssue)
        Double remainingFamilyDaysValue = remainingFamilyDays.getValue(cardIssue)
        if(daysToDecrease > 0){
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
            CustomField vacationCountFieldObject = cfm.getCustomFieldObject(12901);
            double vacationsLeft = (double) cardIssue.getCustomFieldValue(vacationCountFieldObject)
            vacationsLeft -= daysToDecrease;
            MutableIssue cardIssueToUpate = ComponentAccessor.getIssueManager().getIssueByCurrentKey(cardIssue.getKey());
            cardIssueToUpate.setCustomFieldValue(vacationCountFieldObject,vacationsLeft );
            ComponentAccessor.getIssueManager().updateIssue(automationUser, cardIssueToUpate, EventDispatchOption.ISSUE_UPDATED, false);
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

if (daysToDecrease > 0){
    if ((remainingVacationDaysValue + remainingFamilyDaysValue) <  daysToDecrease * hoursInWorkDay ){
        throw new InvalidInputException("Количество дней отпуска в задаче превышает имеющейся у вас остаток.")
    }
    if ( !(timeOfAbsence == daysToDecrease * hoursInWorkDay || timeOfAbsence == daysToDecrease * hoursInWorkDay - hoursInWorkDay * 0.5) ){
        throw new InvalidInputException('Проверьте правильность заполнения поля "Time of absence in hours"!')
    }
}


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


List<Timestamp> holidays = new HolidayThisYear().holidays
def daysToDecrease = 0
ApplicationUser automationUser = ComponentAccessor.getUserManager().getUserByName("automation")
ApplicationUser currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
String currentUserName = currentUser.getUsername()
String userGroupCheck = ComponentAccessor.groupManager.isUserInGroup(currentUserName, 'Support Position')


Timestamp dueDate_date_timestamp = issue.getDueDate()
CustomFieldManager cfm = ComponentAccessor.getCustomFieldManager()
Object customFieldStartDate = cfm.getCustomFieldObject(11016L)
Object remainingVacationDays = cfm.getCustomFieldObject(12900L)
Object remainingFamilyDays = cfm.getCustomFieldObject(13100L)
Object customFieldTimeofAbsence = cfm.getCustomFieldObject(11100L)
Calendar calendar = Calendar.getInstance()
Timestamp start_date_timestamp = issue.getCustomFieldValue(customFieldStartDate)
def duedate_minus_start_date = issue.getDueDate() - start_date_timestamp

def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
def userCardSearchJQL = jqlQueryParser.parseQuery("project = AC and Username  ~ ${currentUserName} and status = 'In Progress'")
List<Issue> issues = ComponentAccessor.getComponent(SearchService).search(automationUser, userCardSearchJQL, PagerFilter.getUnlimitedFilter()).getResults()
Issue cardIssue = issues.get(0)
Double remainingVacationDaysValue = remainingVacationDays.getValue(cardIssue)
Integer remainingFamilyDaysValue = remainingFamilyDays.getValue(cardIssue)

calendar.setTime(start_date_timestamp)
        int dayOfWeeks = calendar.get(Calendar.DAY_OF_WEEK)
        int timeOfAbsence = issue.getCustomFieldValue(customFieldTimeofAbsence) as int;
        int timeOfAbsenceSec = timeOfAbsence*3600;
        if (duedate_minus_start_date == 0){
            if (!((dayOfWeeks == 1) || (dayOfWeeks == 7) || start_date_timestamp in holidays )) { // 1 - SUN; 7 SUT
                daysToDecrease = timeOfAbsence/8
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

if (issue.issueType.name == 'Vacation') {
    if (daysToDecrease > 0){
        

if (issue.issueType.name == 'Vacation') {
    if (daysToDecrease > 0){
        if ((remainingVacationDaysValue + remainingFamilyDaysValue) <  daysToDecrease){
         throw new InvalidInputException("Количество дней отпуска в задаче превышает имеющейся у Вас остаток.")
        }
    }
}



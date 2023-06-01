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

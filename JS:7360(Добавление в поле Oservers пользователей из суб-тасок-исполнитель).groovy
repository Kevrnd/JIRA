/* Версия на проде на 15.06.2023г.
com/slotegrator/projects/JS/createSubtasksOnOffBoarding.groovy
package com.slotegrator.projects.js;
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



IssueManager issueManager = ComponentAccessor.getIssueManager()
//MutableIssue issue = issueManager.getIssueObject("JS-5524")

//def issue = issueManager.getIssueObject(issueKey)
//assert issue: "Could not find issue with key $issueKey"

CustomField cfType = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12001L)
boolean checkFieldAddUser = cfType.getValue(issue).toString().equals('Добавить пользователя')
boolean checkFieldBlockUser = cfType.getValue(issue).toString().equals('Заблокировать пользователя')
if ( !(checkFieldBlockUser || checkFieldAddUser)) {
    return 
}
// Components
Collection<ProjectComponent> listComponent = issue.getComponents()
//create subtask
for (component in listComponent){
    Object issueSummary = issue.getSummary()
    // the summary of the new issue
    String summary = component.name  +" "+ issueSummary
    IssueService issueService = ComponentAccessor.issueService
    ConstantsManager constantsManager = ComponentAccessor.constantsManager
    ApplicationUser loggedInUser = ComponentAccessor.userManager.getUserByName('automation')
    String assigneReporterUser = ComponentAccessor.userManager.getUserByKey(component.lead).name
    def issueInputParameters = issueService.newIssueInputParameters().with {
        setProjectId(issue.projectObject.id)
        setIssueTypeId("10003")
        setReporterId(assigneReporterUser)
        setSummary(summary)
        setAssigneeId(assigneReporterUser)
    }
    def validationResult = issueService.validateSubTaskCreate(loggedInUser, issue.id, issueInputParameters)
    assert validationResult.valid : validationResult.errorCollection
    String jqlSearch = "summary ~ '${summary}'"
    // Some components
    def searchService = ComponentAccessor.getComponentOfType(SearchService)
    def parseResult = searchService.parseQuery(loggedInUser, jqlSearch)
    Object searchResults  = searchService.search(loggedInUser, parseResult.query, PagerFilter.unlimitedFilter)
    Collection issues = searchResults.getResults().collect { is -> issueManager.getIssueObject(is.getId()) }
    //Double check 
    if  (!issues) {
      def issueResult = issueService.create(loggedInUser, validationResult)
      assert issueResult.valid : issueResult.errorCollection
      Issue subtask = issueResult.issue
      ComponentAccessor.subTaskManager.createSubTaskIssueLink(issue, subtask, loggedInUser)
    }
}



*/



import com.atlassian.jira.event.type.EventDispatchOption
import java.util.List
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


/*
ISSUE JS-7360 this script was modified on 06/08/2023
Adding to the assignee in subtasks in issue Observers field 
*/

IssueManager issueManager = ComponentAccessor.getIssueManager()
//MutableIssue issue = issueManager.getIssueObject("")

CustomField cfType = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12001L)
boolean checkFieldAddUser = cfType.getValue(issue).toString().equals('Добавить пользователя')
boolean checkFieldBlockUser = cfType.getValue(issue).toString().equals('Заблокировать пользователя')
ApplicationUser loggedInUser = ComponentAccessor.userManager.getUserByName('automation')
CustomField observersField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(10529L)  
def observers  = issue.getCustomFieldValue(observersField) ?: []
if ( !(checkFieldBlockUser || checkFieldAddUser)) {
    return 
}
// Components
Collection<ProjectComponent> listComponent = issue.getComponents()
//create subtask
for (component in listComponent){  

    Object issueSummary = issue.getSummary()
    // the summary of the new issue
    String summary = component.name  +" "+ issueSummary
    IssueService issueService = ComponentAccessor.issueService
    ConstantsManager constantsManager = ComponentAccessor.constantsManager
    String assigneReporterUser = ComponentAccessor.userManager.getUserByKey(component.lead).name
    def issueInputParameters = issueService.newIssueInputParameters().with {
        setProjectId(issue.projectObject.id)
        setIssueTypeId("10003")
        setReporterId(assigneReporterUser)
        setSummary(summary)
        setAssigneeId(assigneReporterUser)
    }
    // Add users to the field Observers 
    if (!observers.contains(ComponentAccessor.userManager.getUserByName(assigneReporterUser))){
    observers.add (ComponentAccessor.userManager.getUserByName(assigneReporterUser) )
    }
    def validationResult = issueService.validateSubTaskCreate(loggedInUser, issue.id, issueInputParameters)
    assert validationResult.valid : validationResult.errorCollection
    String jqlSearch = "summary ~ '${summary}'"
    // Some components
    def searchService = ComponentAccessor.getComponentOfType(SearchService)
    def parseResult = searchService.parseQuery(loggedInUser, jqlSearch)
    Object searchResults  = searchService.search(loggedInUser, parseResult.query, PagerFilter.unlimitedFilter)
    Collection issues = searchResults.getResults().collect { is -> issueManager.getIssueObject(is.getId()) }
    //Double check 
    if  (!issues) {
      def issueResult = issueService.create(loggedInUser, validationResult)
      assert issueResult.valid : issueResult.errorCollection
      Issue subtask = issueResult.issue
      ComponentAccessor.subTaskManager.createSubTaskIssueLink(issue, subtask, loggedInUser)
    }
}


log.warn ("For Issue--> " + issue.getKey() + " Users added as Observers --> " + observers)

issue.setCustomFieldValue(observersField, observers) 
issueManager.updateIssue(loggedInUser, issue, EventDispatchOption.DO_NOT_DISPATCH, false)

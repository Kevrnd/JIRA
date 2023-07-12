package com.slotegrator.postfunctions
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager
import com.atlassian.jira.security.JiraAuthenticationContext
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.event.type.EventDispatchOption

/*
JS-7486
Доработка автоматизации в проектах CCR и ROC
В проектах CCR и ROC при перетягивании задачи из To do в In progress
- заполнять responsible user тем, кто перевёл задачу в прогресс
*/

IssueManager issueManager = ComponentAccessor.getIssueManager()
//MutableIssue issue = issueManager.getIssueObject("")

String projectKey = issue.getProjectObject().getKey()
if (projectKey != "CCR" && projectKey != "ROC" ){
    return
}
String issueTypeId = issue.getIssueType().id

if (issueTypeId != "10004" && issueTypeId != "10002"){
    return
}

ChangeHistoryManager changeHistoryManager = ComponentAccessor.getChangeHistoryManager()
def changeItem = changeHistoryManager.getChangeItemsForField(issue, 'status')?.last()
if (changeItem?.fromString != "To Do"){
    return changeItem?.fromString
}
JiraAuthenticationContext authContext = ComponentAccessor.getJiraAuthenticationContext()
ApplicationUser  currentUser = authContext.getLoggedInUser()
ApplicationUser automationUser = ComponentAccessor.userManager.getUserByName('automation')
CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager()
CustomField customFieldResponsibleUser = customFieldManager.getCustomFieldObject(11803L)
ApplicationUser responsibleUserValue = currentUser
issue.setCustomFieldValue(customFieldResponsibleUser , responsibleUserValue)
issueManager.updateIssue(automationUser, issue, EventDispatchOption.ISSUE_UPDATED, false)
log.warn("Responsible User в задаче ${issue.getKey()} изменен на  '${currentUser.name}'")





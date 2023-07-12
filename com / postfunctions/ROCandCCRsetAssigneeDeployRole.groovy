package com.slotegrator.postfunctions
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.security.roles.ProjectRole
import com.atlassian.jira.project.Project
import com.atlassian.jira.project.ProjectManager
import com.atlassian.jira.security.roles.ProjectRole
import com.atlassian.jira.security.roles.ProjectRoleActors
import com.atlassian.jira.security.roles.ProjectRoleManager



/*
JS-7486
Доработка автоматизации в проектах CCR и ROC
В проектах CCR и ROC необходимо создать роль,
привязать к ней следующий флоу:
Все задачи, которые попадают в колонку Ready for deploy переводились
на того, кому добавили эту роль. Расшарить эту роль мне.
*/

IssueManager issueManager = ComponentAccessor.getIssueManager()
//MutableIssue issue = issueManager.getIssueObject("")
ApplicationUser automationUser = ComponentAccessor.userManager.getUserByName('automation')
String issueTypeId = issue.getIssueType().getId()
Project issueProject = issue.getProjectObject()
String issueProjectKey = issue.getProjectObject().getKey()

String projectKey = issue.getProjectObject().getKey()
if (projectKey != "CCR" && projectKey != "ROC" ){
    return
}

if (issueTypeId != "10004" && issueTypeId != "10002"){
    return
}

String projectRoleName = 'Deploy Role'
ProjectManager projectManager = ComponentAccessor.getProjectManager()
ProjectRoleManager projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager)
ProjectRole projectRole = projectRoleManager.getProjectRole(projectRoleName)
ProjectRoleActors actors = projectRoleManager.getProjectRoleActors(projectRole, issueProject)
ApplicationUser firstResultUser =  actors.getUsers()[0]
if (firstResultUser){
    issue.setAssignee(firstResultUser)
    issueManager.updateIssue(
            automationUser,
            issue,
            EventDispatchOption.ISSUE_UPDATED,
            false
    )
    log.warn ("Assignee в задаче ${issue.getKey()} изменен на '${firstResultUser.name}'")
}




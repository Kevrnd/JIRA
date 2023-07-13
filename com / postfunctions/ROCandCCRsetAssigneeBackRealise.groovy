package com.slotegrator.postfunctions
import com.atlassian.jira.project.version.Version
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
В проекте CCR необходимо создать роль для релизов бэка, привязать к ней следующий флоу:
При переводе задачи в колонку Review и UAT Deploy, переводить задачу на того, кому
выделили эту роль, если в задаче проставлена fixVersion"CCR-BACK".
Нужно учитывать то, что стандартный вид fixVersion это CCR-BACK_x.y.z,
поэтому нужно проверять версию только на вхождение "CCR-BACK".
Добавить эту роль Владу Хатсенко.
*/
IssueManager issueManager = ComponentAccessor.getIssueManager()
//MutableIssue issue = issueManager.getIssueObject("")
ApplicationUser automationUser = ComponentAccessor.userManager.getUserByName('automation')
String issueTypeId = issue.getIssueType().getId()
Project issueProject = issue.getProjectObject()
String issueProjectKey = issue.getProjectObject().getKey()
if (issueProjectKey != "CCR" && issueProjectKey != "ROC" ){
    return
}
if (issueTypeId != "10004" && issueTypeId != "10002"){
    return
}
Collection<Version> fixVersions = issue.getFixVersions()
def hasCCRBackVersion = fixVersions.any { it.name.contains("CCR-BACK") }
if (!hasCCRBackVersion){
    return
}
String projectRoleName = 'Back realise'
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
            EventDispatchOption.DO_NOT_DISPATCH,
            false
    )
}






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

IssueManager issueManager = ComponentAccessor.getIssueManager()
MutableIssue issue = issueManager.getIssueObject("CCR-2036")
ApplicationUser automationUser = ComponentAccessor.userManager.getUserByName('automation')
String issueTypeId = issue.getIssueType().getId()
Project issueProject = issue.getProjectObject()
String issueProjectKey = issue.getProjectObject().getKey()
if (! (issueTypeId == "10002" && (issueProjectKey == "CCR" || issueProjectKey == "ROC") ) ){ //Только для типа задач Task???
    return
}
Collection<Version> fixVersions = issue.getFixVersions()
def hasCCRBackVersion = fixVersions.any { it.name.contains("CCR-FRONT") || it.name.contains("ROC-FRONT") }
if (!hasCCRBackVersion){
    return 
}
String projectRoleName = 'Front realise'
ProjectManager projectManager = ComponentAccessor.getProjectManager()
ProjectRoleManager projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager)
ProjectRole projectRole = projectRoleManager.getProjectRole(projectRoleName)
ProjectRole backRealiseRole = projectRoleManager.getProjectRole(projectRoleName)
ProjectRoleActors actors = projectRoleManager.getProjectRoleActors(backRealiseRole, issueProject)
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

import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.issue.util.IssueChangeHolder
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.security.JiraAuthenticationContext
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.project.ProjectCategory
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.project.ProjectManager
import com.atlassian.jira.project.Project
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser


IssueManager issueManager = ComponentAccessor.getIssueManager()
ProjectManager projectManager = ComponentAccessor.getProjectManager()
MutableIssue issue = issueManager.getIssueObject("BF-1448")

// Change issue type
if (issue.issueType.name != "Business Feature"){
    log.warn (issue.issueType.name)
    return
}
CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager()

// Create epic 
// Кого нужно ставить Assignee в Эпике?????????
def createLinkedEpic(MutableIssue issue, String projectKey) {    
    IssueManager issueManager = ComponentAccessor.getIssueManager()
    CustomFieldManager customFieldManager = ComponentAccessor.customFieldManager
    IssueService issueService = ComponentAccessor.issueService
    Object authContext = ComponentAccessor.getJiraAuthenticationContext()
    ApplicationUser currentUser = authContext.getLoggedInUser() //От какого пользователя создаем Эпик?
    ProjectManager projectManager = ComponentAccessor.projectManager
    Project project = projectManager.getProjectObjByKey(projectKey)    
    String projectName = project ? project.getName() : null   
    //CustomField projectToChooseField = customFieldManager.getCustomFieldObject(10900L)
    CustomField parentLinkField = customFieldManager.getCustomFieldObject(10112L)
    //Project projectChoose =  projectToChooseField.getValue(issue)
    String assigneUser = issue.assignee ? issue.assignee.getName() : "automation"
    //String projectLead = project ? project.getProjectLead() : null
    String reporterUser = issue.reporter.getName()
    Object originalSummary = issue.getSummary()
    String epicNameFieldValue = issue.key + originalSummary
    String epicSummary = "[" + projectName + "] " + originalSummary    
    String originalDescription = issue.getDescription()
    String epicLink = issue.getCustomFieldValue(customFieldManager.getCustomFieldObject(10101L))
    //Input parametrs Epic
    def issueInputParameters = issueService.newIssueInputParameters().with {
    setProjectId(project.id)
    setIssueTypeId('10000')
    setAssigneeId(assigneUser)
    setReporterId(reporterUser)
    setSummary(originalSummary)
    setDescription(originalDescription)
    setPriorityId(priorityId)
    addCustomFieldValue(10103L, epicNameFieldValue)
    }
    //Create Epic
    Object validationResult = issueService.validateCreate(currentUser, issueInputParameters)
    assert validationResult.valid : validationResult.errorCollection
    def result = issueService.create(currentUser, validationResult)
    assert result.valid : result.errorCollection
    String newEpicKey = result.getIssue().getKey()
    MutableIssue newEpic = issueManager.getIssueObject(newEpicKey)
    def parentLinkFieldType = parentLinkField.getCustomFieldType()
    JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext();
    Object mutableParentIssue = parentLinkFieldType.getSingularObjectFromString(issue.key)
    Issue mutableIssue = issueManager.getIssueObject(newEpic.id)
    IssueChangeHolder changeHolder = new DefaultIssueChangeHolder()
    parentLinkField.updateValue(null, mutableIssue, new ModifiedValue(parentLinkField, mutableParentIssue), changeHolder)
    log.warn ( "New epic create: "+ newEpicKey + epicSummary)

}




CustomField  customFieldP2ProjectPicker= customFieldManager.getCustomFieldObject(13307L)
def p2ProjectPickerValue =customFieldP2ProjectPicker.getValue(issue)


if (p2ProjectPickerValue == null) {
    log.warn ("Поле P2 Projects не заполненно для ${issue.key}")
    return
} else if (p2ProjectPickerValue.first() == "All P2 Projects"){ // Поле изменим для выбоа только одного значения
    List<Project> projects = projectManager.getProjectObjects()
    List<Project> p2Projects = projects.findAll { project ->
        project.getProjectCategory()?.getId() ==  10300L && project.getKey() != "SAMPLEP2"   //категория "P2 projects" и исключаем проект "!_Sample_Platform_2 Project"
    }
    for (Project project : p2Projects) {
        createLinkedEpic(issue, project.getKey())
    }
    log.warn(p2Projects)
} else {
    Long p2ProjectPickerValueLong = p2ProjectPickerValue.first() as Long
    Project projectInFieldp2ProjectPicker = projectManager.getProjectObj(p2ProjectPickerValueLong) 
    
    //createLinkedEpic(issue, project.getKey())    
}







/*   JS-7530	[Jira] Автоматизация создания Epic  */
import com.atlassian.jira.issue.attachment.Attachment
import com.atlassian.jira.issue.AttachmentManager
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
//MutableIssue issue = issueManager.getIssueObject("BF-3200")
log.warn ("Issue Type-->" + issue.issueType.name + "  IssueKey-->" + issue.key)
// Check issue type
if (issue.issueType.name != "Product request"){

    return
}


// Create epic (IssueKey in which the epic is created, projectKey in which the epic is created)

def createLinkedEpic(MutableIssue issue, String projectKey) {
    IssueManager issueManager = ComponentAccessor.getIssueManager()
    ProjectManager projectManager = ComponentAccessor.projectManager
    AttachmentManager attachmentManager = ComponentAccessor.getAttachmentManager()
    CustomFieldManager customFieldManager = ComponentAccessor.customFieldManager
    IssueService issueService = ComponentAccessor.issueService
    ApplicationUser automationUser = ComponentAccessor.userManager.getUserByName('automation')
    ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(automationUser)
    Project project = projectManager.getProjectObjByKey(projectKey)
    String projectName = project ? project.getName() : null
    CustomField parentLinkField = customFieldManager.getCustomFieldObject(10112L)
    String projectLead = project ? project.getProjectLead().getName() : automationUser
    Object originalSummary = issue.getSummary()
    String epicNameFieldValue = issue.key + originalSummary
    String epicSummary = "[" + projectName + "] " + originalSummary
    String originalDescription = issue.getDescription()
    String epicLink = issue.getCustomFieldValue(customFieldManager.getCustomFieldObject(10101L))
    //Set parametrs Epic
    def issueInputParameters = issueService.newIssueInputParameters().with {
        setProjectId(project.id)
        setAssigneeId(projectLead)
        setIssueTypeId('10000')
        setReporterId(automationUser.getName())
        setSummary(epicSummary)
        setDescription(originalDescription)
        setPriorityId(priorityId)
        addCustomFieldValue(10103L, epicNameFieldValue)
    }
    //Create Epic
    Object validationResult = issueService.validateCreate(automationUser, issueInputParameters)
    assert validationResult.valid : validationResult.errorCollection
    def result = issueService.create(automationUser, validationResult)
    assert result.valid : result.errorCollection
    //Link
    String newEpicKey = result.getIssue().getKey()
    MutableIssue newEpic = issueManager.getIssueObject(newEpicKey)
    def parentLinkFieldType = parentLinkField.getCustomFieldType()
    Object mutableParentIssue = parentLinkFieldType.getSingularObjectFromString(issue.key)
    Issue mutableIssue = issueManager.getIssueObject(newEpic.id)
    IssueChangeHolder changeHolder = new DefaultIssueChangeHolder()
    parentLinkField.updateValue(null, mutableIssue, new ModifiedValue(parentLinkField, mutableParentIssue), changeHolder)
    //Attachments copy
    Collection<Attachment> issueObject = issue.getAttachments()
    attachmentManager.copyAttachments(issue, automationUser, newEpicKey)
    log.warn ( "New epic create: "+ newEpicKey + epicSummary)
}


CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager()
CustomField  customFieldP2ProjectPicker= customFieldManager.getCustomFieldObject(13307L)
def p2ProjectPickerValue = customFieldP2ProjectPicker.getValue(issue)

if (p2ProjectPickerValue == null) {
    log.warn ("Поле P2 Projects не заполненно для ${issue.key}")
    return
} else if (p2ProjectPickerValue.first() == "All P2 Projects"){
    List<Project> projects = projectManager.getProjectObjects()
    List<Project> p2Projects = projects.findAll { project ->
        project.getProjectCategory()?.getId() ==  10300L && project.name != "!_Sample_Platform_2 Project"   //категория "P2 projects" и исключаем проект "!_Sample_Platform_2 Project"
    }
    for (Project project : p2Projects) {
        createLinkedEpic(issue, project.getKey())
    }
    log.warn(p2Projects)
} else {
    Long p2ProjectPickerValueLong = p2ProjectPickerValue.first() as Long
    Project projectInFieldp2ProjectPicker = projectManager.getProjectObj(p2ProjectPickerValueLong)
    if (projectInFieldp2ProjectPicker.name != "!_Sample_Platform_2 Project"){
        createLinkedEpic(issue, projectInFieldp2ProjectPicker.getKey())
    }
}



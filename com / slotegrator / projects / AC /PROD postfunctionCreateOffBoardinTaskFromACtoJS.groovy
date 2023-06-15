/* 
PROD 15/06/2023 
postfunctionCreateOffBoardinTaskFromACtoJS.groovy
*/

package com.slotegrator.projects.ac

import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.link.IssueLinkManager
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.workflow.TransitionOptions
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.bc.issue.IssueService.TransitionValidationResult

CustomFieldManager cfm = ComponentAccessor.getCustomFieldManager()
IssueService issueService = ComponentAccessor.getIssueService()
CustomField cf_email = cfm.getCustomFieldObject(11020L)
CustomField cf_job_position = cfm.getCustomFieldObject(11015L)
CustomField cf_hr = cfm.getCustomFieldObject(12353L)
CustomField cf_type = cfm.getCustomFieldObject(12001L)
CustomField start_date = cfm.getCustomFieldObject(11016L)
CustomField user_teams = cfm.getCustomFieldObject(12354L)


ApplicationUser auto_user = ComponentAccessor.getUserManager().getUserByName("automation")

def userManager = ComponentAccessor.getUserManager();
String leadKey = ComponentAccessor.getProjectComponentManager().findByComponentName(10133, "Onboarding").getLead();

ApplicationUser jira_admin_user = userManager.getUserByKey(leadKey); // ComponentAccessor.userManager.getUserByName("k.tarabrin")


IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager()
MutableIssue mutableIssue = issue as MutableIssue
List<Long> componentIds = new ArrayList<Long>();
List<String> developers_list = ['PHP Developer', 'React Front-End', '.net developer', 'QA Automation', '.net TL', 'Java developer', 'Architect', 'React Front-End TL', 'Front End', 'Java TL', 'PHP TL', 'CTO', 'FullStack Developer']
boolean sales_check = cf_job_position.getValue(issue).toString().equals('Sales Manager') && user_teams.getValue(issue).toString().replaceAll("\\[","").replaceAll("\\]","").equals('Sales')

if (cf_hr.getValue(issue).toString().equals("Slot")) {
    componentIds.addAll([10301L, 10303L, 14445L, 14625L, 14626L, 15410L, 10210L]) // "Jira", "Gsuite",  "Rocket.Chat.Slot", "Equipment.Slot", "VPN.Slot" , "Producation Access", "Confluence"
        if(cf_job_position.getValue(issue).toString() in developers_list) {
            componentIds.add(10422L) // GitLab
        }
        if (sales_check) {
            componentIds.addAll([10816L, 15221L, 15222L]) // "Redmine", Monday.Slot, JiraSM.Slot
        }
 }
 
 else {
    componentIds.addAll([10210L, 10303L, 10301L, 10420L, 10302L,15410L]) // "Jira", "Confluence", "VPN", "Gsuite", "Rocket.Chat", "Producation Access"
        if(cf_job_position.getValue(issue).toString() in developers_list) {
            componentIds.add(10422L) // GitLab
        }
 }

IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
issueInputParameters
 .setProjectId(ComponentAccessor.getProjectManager().getProjectObjByKey("JS").getId()) 
 // JS-6984 change Summary Issue
 //.setSummary("Offboarding - " + issue.getSummary() + " - " + issue.getResolutionDate() )
 .setSummary("Offboarding - " + issue.getSummary() + " - " + issue.getResolutionDate().format("yyyy-MM-dd") + " 18:00:00.000" )
 .setDescription("*ФИО:* " + issue.getSummary() + "\n" + "*email:* " + cf_email.getValue(issue).toString() + "\n" + "*Job position:* " + cf_job_position.getValue(issue).toString() + "\n" + "*End Date:* " +  issue.getResolutionDate() + "\n")
 .setIssueTypeId("10002")
 .setPriorityId("3")
 .setReporterId(auto_user.getName())
 .setAssigneeId(auto_user.getName())
 .setComponentIds((Long[]) componentIds.toArray())
 //.addCustomFieldValue(12001L, "Заблокировать пользователя")

IssueService.CreateValidationResult createValidationResult = issueService.validateCreate(auto_user, issueInputParameters)
if (createValidationResult.isValid()) {
IssueService.IssueResult createResult = issueService.create(auto_user, createValidationResult)
issueLinkManager.createIssueLink((Long)createResult.getIssue().getId(), (Long)issue.getId(), Long.parseLong("10300"), Long.valueOf(2), auto_user)
def cfConfig = cf_type.getRelevantConfig(createResult.getIssue())
def value_block = ComponentAccessor.optionsManager.getOptions(cfConfig)?.find {
    it.toString() == 'Заблокировать пользователя'
}
createResult.getIssue().setCustomFieldValue(cf_type, value_block);
ComponentAccessor.getIssueManager().updateIssue(auto_user, createResult.getIssue(), EventDispatchOption.DO_NOT_DISPATCH, false);

TransitionOptions transitionOptions = new TransitionOptions.Builder()
			.skipConditions()
			.skipPermissions()
			.skipValidators()
			.build()  
			int actionId = 331 // transition «Quick task»
			IssueService.TransitionValidationResult transitionValidationResult = issueService.validateTransition(jira_admin_user, createResult.getIssue().id, actionId, issueService.newIssueInputParameters(), transitionOptions)
        		if (transitionValidationResult.isValid()) {
                    	issueService.transition(jira_admin_user, transitionValidationResult)
        }
}

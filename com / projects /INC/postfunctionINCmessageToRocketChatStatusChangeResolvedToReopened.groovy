package com.slotegrator.projects.INC
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.MutableIssue
import com.slotegrator.rocketchat.RocketChatConnector

/*
JS-7532
(В проекте INC если задача стоит в статусе RESOLVED и ее статус изменяется
(в текущем флоу либо на RCA или REOPENED) нужно отправлять в чат INC_check c сообщение
Если статус измененился на REOPENED то нужно отправить следующее сообщение:
{Jira ticket URL}
{Jira ticket name}
Проверен: {user name} (имя и фамилия того, кто изменил статус)
Status: {Jira ticket status} ❌
*/
IssueManager issueManager = ComponentAccessor.getIssueManager()
String issueStatus = issue.getStatus().name
if (issueStatus != "Resolved"){
    return
}
RocketChatConnector rocketChatConnector = new RocketChatConnector()
String JIRA_BASE_URL = ComponentAccessor.getApplicationProperties().getJiraBaseUrl()
String issueSummary = issue.getSummary()
String issueKey = issue.getKey()
def authContext = ComponentAccessor.getJiraAuthenticationContext()
String currentUser = authContext.getLoggedInUser().displayName
String linkUrl = "$JIRA_BASE_URL/browse/${issueKey}"
def messageToSent = "[${issueKey}](${linkUrl})"   + "  " +  issueSummary + " \r\nПроверен: " + currentUser + "\r\nStatus: REOPENED ❌"
log.warn (messageToSent)

rocketChatConnector.postMessage(messageToSent, "#INC_check") // #testNotify



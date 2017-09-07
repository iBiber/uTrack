package com.github.iBiber.juTrack;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.github.iBiber.juTrack.data.GetIssueResultItem;
import com.github.iBiber.juTrack.data.GetIssuesParmeter;
import com.github.iBiber.juTrack.data.jira.Issue;

@Component
public class JiraIssuesProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(JiraIssuesProcessor.class);

	public void getIssues(GetIssuesParmeter parameter) {
		LOGGER.info("Get issues: " + parameter);
		String userName = parameter.credentials.userName;

		// Query Jira
		List<Issue> issues = new JiraIssuesQueryExecutor(parameter.jiraRootUrl).getIssues(parameter.credentials,
		        parameter.startDate, parameter.endDate).issues;

		// Process result
		List<GetIssueResultItem> resultList = new IssueListToGetIssueResultItemListTransformer(userName, issues)
		        .execute();

		// Print result
		resultList.stream().sorted(new Comparator<GetIssueResultItem>() {
			@Override
			public int compare(GetIssueResultItem o1, GetIssueResultItem o2) {
				return o1.created.compareTo(o2.created);
			}
		}).forEach(item -> System.out
		        .println(item.getDateTime().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)) + "\t"
		                + item.key + "\t" + item.changeAction + "\t" + item.summary + "\t" + parameter.jiraRootUrl
		                + "/browse/" + item.key));
	}
}

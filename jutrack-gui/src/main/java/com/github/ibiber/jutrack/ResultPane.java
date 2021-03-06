package com.github.ibiber.jutrack;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.ibiber.jutrack.external.GetIssueResultItemPresenter;
import com.github.ibiber.jutrack.external.data.JiraQueryParmeter;
import com.github.ibiber.jutrack.external.data.JiraQueryResultItem;

import javafx.application.HostServices;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.stage.Screen;

@Component
public class ResultPane extends TabPane implements GetIssueResultItemPresenter {
	private TextArea resultArea;
	private TableView issuesTable;

	@Autowired
	private HostServices hostServices;

	public void init() {
		double preferredWidth = Screen.getPrimary().getVisualBounds().getWidth() * 0.5;

		TabPane tabPane = this;
		tabPane.setMaxWidth(Double.MAX_VALUE);
		tabPane.setMaxHeight(Double.MAX_VALUE);
		tabPane.setPrefSize(preferredWidth, 400);

		Tab tableTab = new Tab("Table View");
		issuesTable = new TableView();
		tableTab.setContent(issuesTable);

		Tab textTab = new Tab("Text View");
		resultArea = new TextArea();
		textTab.setContent(resultArea);
		tabPane.getTabs().addAll(tableTab, textTab);
	}

	@Override
	public void presentResults(JiraQueryParmeter parameter, Stream<JiraQueryResultItem> resultStream) {
		// Show result to GUI
		issuesTable.getColumns().clear();
		createHeaderColumns(parameter);

		// Fill table data and result text area
		Rows rows = new Rows();
		String resultText = resultStream//
		        .peek(item -> rows.markAction(item.key, item.created)) // Collect data for table
		        .map(item -> item.created.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)) + "\t"
		                + item.key + "\t" + item.changeAction + "\t" + item.summary + "\t" + parameter.jiraRootUrl
		                + "/browse/" + item.key)//
		        .collect(Collectors.joining("\n"));
		this.resultArea.setText(resultText);

		issuesTable.setItems(FXCollections.observableArrayList(rows.getRows()));
	}

	class Rows {
		private Map<String, Row> rowsByIssueKey = new TreeMap<>();

		public void markAction(String key, LocalDateTime dateTime) {
			Row row = rowsByIssueKey.get(key);
			if (row == null) {
				row = new Row(key);
				rowsByIssueKey.put(key, row);
			}
			row.markDate(dateTime);
		}

		public Collection<Row> getRows() {
			return rowsByIssueKey.values();
		}
	}

	class Row {
		private final String issueKey;
		private Map<String, String> columnValuesByColumnTitle = new HashMap<>();

		public Row(String issueKey) {
			this.issueKey = issueKey;
		}

		public void markDate(LocalDateTime dateTime) {
			columnValuesByColumnTitle.put(getColumnHeader(dateTime), "X");
		}

		public String getIssueKey() {
			return issueKey;
		}

		public String getValueByColumnTitle(String columnTitle) {
			return columnValuesByColumnTitle.get(columnTitle);
		}
	}

	private void createHeaderColumns(JiraQueryParmeter parameter) {
		// Build first column
		List<TableColumn> columns = new ArrayList<>();
		TableColumn<Row, Hyperlink> issuesColumn = new TableColumn<>("Issue");
		issuesColumn.setCellValueFactory((cellFeature) -> createIssueCell(parameter, cellFeature));
		columns.add(issuesColumn);

		// Create one column per day
		long daysBetween = ChronoUnit.DAYS.between(parameter.startDate, parameter.endDate);
		IntStream.range(0, (int) daysBetween + 1).forEach(index -> {
			LocalDate d = parameter.startDate.plusDays(index);
			String columnTitle = getColumnHeader(d);
			TableColumn<Row, String> col = new TableColumn<>(columnTitle);
			col.setCellValueFactory(
			        row -> new ReadOnlyObjectWrapper<>(row.getValue().getValueByColumnTitle(columnTitle)));
			columns.add(col);
		});
		issuesTable.getColumns().addAll(columns);
	}

	private ReadOnlyObjectWrapper<Hyperlink> createIssueCell(JiraQueryParmeter parameter,
	        CellDataFeatures<Row, Hyperlink> row) {
		Hyperlink hyperLink = new Hyperlink(row.getValue().getIssueKey());
		hyperLink.setOnAction((e) -> openBrowser(parameter, hyperLink));
		return new ReadOnlyObjectWrapper<>(hyperLink);
	}

	private void openBrowser(JiraQueryParmeter parameter, Hyperlink hyperLink) {
		String link = parameter.jiraRootUrl + "/browse/" + hyperLink.getText();
		hostServices.showDocument(link);
	}

	private String getColumnHeader(Temporal dateTime) {
		return DateTimeFormatter.ofPattern("dd.MM").format(dateTime);
	}
}

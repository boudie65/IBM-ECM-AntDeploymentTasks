package com.ibm.deploy.ant.task.icm;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.security.auth.Subject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.json.JSONArray;
import org.apache.commons.json.JSONException;
import org.apache.commons.json.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.tools.ant.BuildException;

import com.filenet.api.core.Connection;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.util.UserContext;
import com.ibm.casemgmt.api.context.CaseMgmtContext;
import com.ibm.casemgmt.api.context.P8ConnectionCache;
import com.ibm.casemgmt.api.context.SimpleP8ConnectionCache;
import com.ibm.casemgmt.api.context.SimpleVWSessionCache;
import com.ibm.deploy.ant.DeployTasks;

import lombok.Getter;
import lombok.Setter;

/**
 * The Class DeploySolutionTask.
 * 
 * @author pvdhorst
 */
public class SolutionImportTask extends DeployTasks {

	/** The url. */
	@Getter private String url;

	/** The username. */
	@Getter @Setter private String username;

	/** The password. */
	@Getter @Setter private String password;

	/** The object store name. */
	@Getter @Setter private String objectStoreName;

	/** The connection def name. */
	@Getter @Setter private String connectionDefName;

	/** The package file. */
	@Getter @Setter private String packageFile;

	/** The override existing. */
	@Getter @Setter private String overrideExisting;

	/** The desktop. */
	@Getter @Setter private String desktop;

	/** The repository. */
	@Getter @Setter private String repository;

	/** The icm url. */
	@Getter @Setter private String icmUrl;

	/** The token. */
	private String token;

	/**
	 * Sets the desktop.
	 *
	 * @param desktop
	 *            the new desktop
	 */
	public void setDesktop(String desktop) {
		this.desktop = desktop;
	}

	/**
	 * Sets the url.
	 * 
	 * @param url
	 *            the new url
	 */
	public void setURL(String url) {
		if (url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}
		this.url = url;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.tools.ant.Task#execute()
	 */
	@Override
	public void execute() throws BuildException {

		CloseableHttpClient httpclient = HttpClients.createDefault();
		try {
			this.log("Starting upload ");
			String filePath = doUpload(getPackageFile(), httpclient);
			this.log("Upload done");

			this.log("Extracting Solution Package");
			JSONObject result = extractSolution(httpclient, filePath);
			this.log("Extracting done");

			this.log("Importing Solution Package");

			doImport(httpclient, result);

		} catch (UnsupportedEncodingException e) {
			this.log(e, 0);
		} catch (ClientProtocolException e) {
			this.log(e, 0);
		} catch (IOException e) {
			this.log(e, 0);
		} catch (JSONException e) {
			this.log(e, 0);
		}
	}

	/**
	 * Uploads a solution ZIP file to the remote server.
	 *
	 * @param packageFile
	 *            the package file
	 * @param httpclient
	 *            the httpclient
	 * @return the string
	 * @throws UnsupportedEncodingException
	 *             the unsupported encoding exception
	 * @throws ClientProtocolException
	 *             the client protocol exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws JSONException
	 *             the JSON exception
	 */
	@SuppressWarnings("deprecation")
	private String doUpload(String packageFile, CloseableHttpClient httpclient)
			throws UnsupportedEncodingException, ClientProtocolException, IOException, JSONException {
		String token = getSecurityToken(httpclient);
		HttpPost httpPost = new HttpPost(this.url + "/plugin.do?" + "server=" + this.getRepository() + "&serverType=p8"
				+ "&plugin=ICMAdminClientPlugin" + "&action=ibmAccmUploadContentService" + "&desktop=" + this.desktop
				+ "&security_token=" + token);

		File file = new File(packageFile);
		MultipartEntityBuilder mpBuilder = MultipartEntityBuilder.create();
		mpBuilder.addBinaryBody("uploadFile", file, ContentType.create("application/x-zip-compressed"), file.getName());
		HttpEntity httpEntity = mpBuilder.build();
		httpPost.setEntity(httpEntity);

		this.log(httpPost.toString());
		CloseableHttpResponse response2 = httpclient.execute(httpPost);

		this.log(response2.getStatusLine().toString());
		if (response2.getStatusLine().getStatusCode() == 200) {
			try {
				HttpEntity entity2 = response2.getEntity();
				StringWriter writer = new StringWriter();
				IOUtils.copy(entity2.getContent(), writer, HTTP.UTF_8);
				String jsString = writer.toString();
				this.log("Response value is:" + jsString);
				JSONObject jsList = new JSONObject(jsString.substring(22).replace("</textarea></body></html>", ""));
				JSONObject jsPayload = jsList.getJSONObject("payload");
				String filePath = jsPayload.getString("uploadFile");
				EntityUtils.consume(entity2);
				this.log(filePath);
				return filePath;
			} finally {
				response2.close();
			}
		} else {
			throw new IOException("Upload failed");
		}
	}

	/**
	 * Extract solution on the remote filesystem.
	 *
	 * @param httpclient
	 *            the httpclient
	 * @param filePath
	 *            the file path
	 * @return the JSON object
	 * @throws ClientProtocolException
	 *             the client protocol exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws JSONException
	 *             the JSON exception
	 */
	@SuppressWarnings("deprecation")
	private JSONObject extractSolution(CloseableHttpClient httpclient, String filePath)
			throws ClientProtocolException, IOException, JSONException {
		String token = getSecurityToken(httpclient);
		HttpPost httpPost = new HttpPost(this.url + "/jaxrs/plugin");

		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("server", this.getRepository()));
		nvps.add(new BasicNameValuePair("designObjectStore", this.objectStoreName));
		nvps.add(new BasicNameValuePair("plugin", "ICMAdminClientPlugin"));
		nvps.add(new BasicNameValuePair("action", "ibmAccmExtractSolutionToImportService"));
		nvps.add(new BasicNameValuePair("desktop", this.desktop));
		nvps.add(new BasicNameValuePair("solutionPackageName", filePath));
		httpPost.setEntity(new UrlEncodedFormEntity(nvps));
		httpPost.setHeader("security_token", token);

		CloseableHttpResponse response2 = httpclient.execute(httpPost);
		this.log(response2.getStatusLine().toString());
		if (response2.getStatusLine().getStatusCode() == 200) {
			try {
				HttpEntity entity2 = response2.getEntity();
				StringWriter writer = new StringWriter();
				IOUtils.copy(entity2.getContent(), writer, HTTP.UTF_8);
				String jsString = writer.toString();
				this.log("Response value is:" + jsString);
				JSONObject js = new JSONObject(jsString);
				this.log("Service response is :" + js.getBoolean("success"));
				if (js.getBoolean("success")) {
					return js;
				} else {
					throw new IOException(
							"Failure " + js.getJSONArray("failureMessages").getJSONObject(0).getString("text"));
				}
			} finally {
			}
		} else {
			throw new IOException("Extract failed");
		}
	}

	/**
	 * Do import.
	 *
	 * @param httpclient
	 *            the httpclient
	 * @param result
	 *            the result
	 * @throws UnsupportedEncodingException
	 *             the unsupported encoding exception
	 * @throws ClientProtocolException
	 *             the client protocol exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws JSONException
	 *             the JSON exception
	 */
	@SuppressWarnings("deprecation")
	private void doImport(CloseableHttpClient httpclient, JSONObject result)
			throws UnsupportedEncodingException, ClientProtocolException, IOException, JSONException {

		String packageName = result.getJSONArray("payload").getJSONObject(0).getJSONObject("properties")
				.getString("SolutionName");
		String token = getSecurityToken(httpclient);
		String monitorId = getMonitorId(httpclient, token);
		String extractedPath = result.getJSONArray("payload").getJSONObject(0).getString("extractionPath");
		extractedPath = encodeString(extractedPath);
		this.log("ExtractedPath is : " + extractedPath);
		HttpPost httpPost = new HttpPost(
				this.url + "/jaxrs/plugin?" + "server=" + this.getRepository() + "&objectStore=" + this.objectStoreName
						+ "&dataSetPath=" + java.net.URI.create(extractedPath).toString() + "&packageName="
						+ packageName + "&packageType=solutionPackage" + "&replaceOption=" + this.overrideExisting
						+ "&projectAreaName=" + this.connectionDefName + "&plugin=ICMAdminClientPlugin"
						+ "&action=ibmAccmImportService" + "&desktop=" + this.desktop + "&monitorId=" + monitorId);

		httpPost.setHeader("security_token", token);
		JSONObject jsonPost = recreateImportJSON(result);
		// TODO in Java7 use StandardCharsets.UTF_8
		StringEntity entity = new StringEntity(jsonPost.toString(), HTTP.UTF_8);
		this.log("Sending json : " + jsonPost.toString());
		entity.setContentType("application/json");
		httpPost.setEntity(entity);

		this.log(httpPost.toString());
		CloseableHttpResponse response2 = httpclient.execute(httpPost);

		this.log(response2.getStatusLine().toString());
		if (response2.getStatusLine().getStatusCode() == 200) {
			try {
				HttpEntity entity2 = response2.getEntity();
				StringWriter writer = new StringWriter();
				IOUtils.copy(entity2.getContent(), writer, "UTF-8");
				String jsString = writer.toString();
				this.log("Response value is:" + jsString);
				JSONObject js = new JSONObject(jsString);
				this.log("Service response is :" + js.getBoolean("success"));
				if (js.getBoolean("success")) {
					this.log("Import succes!");
				} else {
					throw new IOException(
							"Failure " + js.getJSONArray("failureMessages").getJSONObject(0).getString("text"));
				}
			} finally {
				response2.close();
			}
		} else {
			throw new IOException("Upload failed");
		}
	}

	/**
	 * Encode string.
	 *
	 * @param extractedPath
	 *            the extracted path
	 * @return the string
	 * @throws UnsupportedEncodingException
	 *             the unsupported encoding exception
	 */
	private String encodeString(String extractedPath) throws UnsupportedEncodingException {

		this.log("string is" + extractedPath);
		String returnValue = URLEncoder.encode(extractedPath, "UTF-8");
		this.log("Encoded string is" + returnValue);
		returnValue = returnValue.replace("+", "%20");
		returnValue = returnValue.replace("%28", "(");
		returnValue = returnValue.replace("%29", ")");
		this.log("Replaced string is" + returnValue);
		return returnValue;
	}

	/**
	 * Gets the monitor id.
	 *
	 * @param httpclient
	 *            the httpclient
	 * @param token
	 *            the token
	 * @return the monitor id
	 * @throws ClientProtocolException
	 *             the client protocol exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws JSONException
	 *             the JSON exception
	 */
	private String getMonitorId(CloseableHttpClient httpclient, String token)
			throws ClientProtocolException, IOException, JSONException {
		HttpPost httpPost = new HttpPost(this.url + "/jaxrs/plugin");
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("type", "generateId"));
		nvps.add(new BasicNameValuePair("plugin", "ICMAdminClientPlugin"));
		nvps.add(new BasicNameValuePair("action", "ibmAccmProgressService"));
		nvps.add(new BasicNameValuePair("desktop", this.desktop));
		httpPost.setEntity(new UrlEncodedFormEntity(nvps));
		httpPost.setHeader("security_token", token);

		this.log("Getting monitorId");
		CloseableHttpResponse response2 = httpclient.execute(httpPost);
		this.log(response2.getStatusLine().toString());
		if (response2.getStatusLine().getStatusCode() == 200) {
			try {
				HttpEntity entity2 = response2.getEntity();
				StringWriter writer = new StringWriter();
				IOUtils.copy(entity2.getContent(), writer, "UTF-8");
				String jsString = writer.toString();
				this.log("Response value is:" + jsString);
				JSONObject js = new JSONObject(jsString);
				this.log("Service response is :" + js.getBoolean("success"));
				if (js.getBoolean("success")) {
					this.log("Received monitorId");
					return js.getJSONObject("payload").getString("monitorId");
				} else {
					throw new IOException(
							"Failure " + js.getJSONArray("failureMessages").getJSONObject(0).getString("text"));
				}
			} finally {
			}
		} else {
			throw new IOException("Extract failed");
		}

	}

	/**
	 * Recreate import json.
	 *
	 * @param result
	 *            the result
	 * @return the JSON object
	 * @throws JSONException
	 *             the JSON exception
	 */
	private JSONObject recreateImportJSON(JSONObject result) throws JSONException {
		JSONObject resultJSON = new JSONObject();

		resultJSON.put("solutionProps", result.getJSONArray("payload").getJSONObject(0).getJSONObject("properties"));
		resultJSON.put("serviceDataMaps", new JSONArray());

		JSONArray osMaps = new JSONArray();
		JSONObject osObj = new JSONObject();
		JSONObject osProps = result.getJSONArray("payload").getJSONArray(2).getJSONObject(0)
				.getJSONObject("properties");
		osObj.put("objectStore", "objectStore0");
		osObj.put("OS_dsp_name", osProps.getString("OS_dsp_name"));
		osObj.put("OS_ID", osProps.getString("OS_ID"));
		osObj.put("OS_LABEL", osProps.getString("OS_LABEL"));
		osObj.put("OS_sym_name", osProps.getString("OS_sym_name"));
		osObj.put("destObjectStoreValue", this.objectStoreName);
		osMaps.put(osObj);
		resultJSON.put("objectStoreMaps", osMaps);

		JSONArray userGroups = createUserInfo(result);
		resultJSON.put("userGroupMaps", userGroups);
		return resultJSON;
	}

	/**
	 * Creates the user info.
	 *
	 * @param result
	 *            the result
	 * @return the JSON array
	 * @throws JSONException
	 *             the JSON exception
	 */
	private JSONArray createUserInfo(JSONObject result) throws JSONException {
		JSONArray returnUserInfo = new JSONArray();
		JSONArray userInfoReceived = result.getJSONArray("payload").getJSONArray(3);

		@SuppressWarnings("unchecked")
		Iterator<JSONObject> infoIterator = userInfoReceived.iterator();

		while (infoIterator.hasNext()) {
			JSONObject infoObj = infoIterator.next();
			JSONObject props = infoObj.getJSONObject("properties");
			props.put("userGroup", infoObj.getString("PrincipalInfo"));

			JSONObject infoValue = new JSONObject();
			infoValue.put("displayName", props.getString("DSP_NAME"));
			infoValue.put("distinguishedName", props.getString("SHORT_NAME"));
			infoValue.put("sid", props.getString("SID"));
			infoValue.put("name", props.getString("NAME"));
			infoValue.put("shortName", props.getString("SHORT_NAME"));
			infoValue.put("FOUND", props.getString("FOUND"));
			JSONArray destUserGroupValues = new JSONArray();
			destUserGroupValues.put(infoValue);
			props.put("destUserGroupValues", destUserGroupValues);
			returnUserInfo.put(props);

		}

		return returnUserInfo;
	}

	/**
	 * Gets the object store by logging into icm CPE and retrieve the
	 * object.
	 *
	 * @return the object store
	 */
	protected ObjectStore getObjectStore() {
		// Create P8 Connection
		P8ConnectionCache connCache = new SimpleP8ConnectionCache();
		Connection connection = connCache.getP8Connection(this.getIcmUrl());

		Subject sub = UserContext.createSubject(connection, this.username, this.password, "icmP8WSI");
		UserContext uc = UserContext.get();
		uc.pushSubject(sub);
		Locale origLocale = uc.getLocale();
		uc.setLocale(origLocale);

		// Create ICM Connection
		@SuppressWarnings("unused")
		CaseMgmtContext origCmctx = CaseMgmtContext.set(new CaseMgmtContext(new SimpleVWSessionCache(), connCache));

		// Grab ObjectStore
		Domain domain = Factory.Domain.fetchInstance(connection, null, null);
		ObjectStore objectStore = Factory.ObjectStore.fetchInstance(domain, this.objectStoreName, null);

		return objectStore;
	}

	/**
	 * Gets the security token by logging into ICN.
	 *
	 * @param httpclient
	 *            the httpclient
	 * @return the security token
	 * @throws UnsupportedEncodingException
	 *             the unsupported encoding exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws ClientProtocolException
	 *             the client protocol exception
	 * @throws JSONException
	 *             the JSON exception
	 */
	private String getSecurityToken(CloseableHttpClient httpclient)
			throws UnsupportedEncodingException, IOException, ClientProtocolException, JSONException {
		if (this.token != null) {
			return this.token;
		}
		HttpPost httpPost = new HttpPost(url + "/jaxrs/logon");
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();

		nvps.add(new BasicNameValuePair("userid", this.username));
		nvps.add(new BasicNameValuePair("password", this.password));
		nvps.add(new BasicNameValuePair("desktop", this.desktop));
		httpPost.setEntity(new UrlEncodedFormEntity(nvps));
		this.log(httpPost.toString());
		CloseableHttpResponse response2 = httpclient.execute(httpPost);
		String securityToken = "";
		try {
			this.log(response2.getStatusLine().toString());
			HttpEntity entity2 = response2.getEntity();
			StringWriter writer = new StringWriter();
			IOUtils.copy(entity2.getContent(), writer, "UTF-8");
			String jsString = writer.toString();
			JSONObject js = new JSONObject(jsString.substring(4));
			securityToken = js.get("security_token").toString();
			this.log("securityToken:" + securityToken);
			EntityUtils.consume(entity2);
		} finally {
			response2.close();
		}
		this.token = securityToken;
		return securityToken;
	}

	/**
	 * Validate input.
	 */
	protected void validateInput() {
		checkForNull("username", this.username);
		checkForNull("password", this.password);
		checkForNull("url", this.url);
		checkForNull("desktop", this.desktop);
		checkForNull("repository", this.repository);
		checkForNull("objectStoreName", this.objectStoreName);
		checkForNull("connectionDefName", this.connectionDefName);
		checkForNull("overrideExisting", this.overrideExisting);
		checkForNull("icmUrl", this.icmUrl);
	}
}
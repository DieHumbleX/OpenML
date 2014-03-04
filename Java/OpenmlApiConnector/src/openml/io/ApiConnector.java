package openml.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import openml.algorithms.Conversion;
import openml.algorithms.Hashing;
import openml.settings.Settings;
import openml.xml.ApiError;
import openml.xml.Authenticate;
import openml.xml.DataSetDescription;
import openml.xml.Implementation;
import openml.xml.ImplementationExists;
import openml.xml.Task;
import openml.xml.UploadDataSet;
import openml.xml.UploadImplementation;
import openml.xml.UploadRun;
import openml.xstream.XstreamXmlMapping;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import openml.xml.ImplementationDelete;
import openml.xml.ImplementationOwned;

import com.thoughtworks.xstream.XStream;

import weka.core.Instances;

public class ApiConnector {
	
	private static final String API_URL = Settings.BASE_URL + "api/";
	private static XStream xstream = XstreamXmlMapping.getInstance();
	
	private static HttpClient httpclient;
	
	public static Authenticate openmlAuthenticate( String username, String password ) throws Exception {
		List<NameValuePair> params = new ArrayList<NameValuePair>(2);
		params.add(new BasicNameValuePair("username", username));
		params.add(new BasicNameValuePair("password", Hashing.md5( password )));
		
		Object apiResult = doApiRequest("openml.authenticate", "", new UrlEncodedFormEntity(params, "UTF-8"));
        if( apiResult instanceof Authenticate){
        	return (Authenticate) apiResult;
        } else {
        	throw new DataFormatException("Casting Api Object to Authenticate");
        }
	}
	
	public static DataSetDescription openmlDataDescription( int did ) throws Exception {
		Object apiResult = doApiRequest("openml.data.description", "&data_id=" + did );
        if( apiResult instanceof DataSetDescription){
        	return (DataSetDescription) apiResult;
        } else {
        	throw new DataFormatException("Casting Api Object to DataSetDescription");
        }
	}
	
	public static Implementation openmlImplementationGet(int implementation_id) throws Exception {
		Object apiResult = doApiRequest("openml.implementation.get", "&implementation_id=" + implementation_id );
        if( apiResult instanceof Implementation){
        	return (Implementation) apiResult;
        } else {
        	throw new DataFormatException("Casting Api Object to Implementation");
        }
	}
	
	public static ImplementationOwned openmlImplementationOwned( String session_hash ) throws Exception {
		MultipartEntity params = new MultipartEntity();
		params.addPart("session_hash",new StringBody(session_hash));
		
		Object apiResult = doApiRequest("openml.implementation.owned", "", params);
		if( apiResult instanceof ImplementationOwned){
        	return (ImplementationOwned) apiResult;
        } else {
        	throw new DataFormatException("Casting Api Object to ImplementationOwned");
        }
	}
	
	public static ImplementationDelete openmlImplementationDelete( int id, String session_hash ) throws Exception {
		MultipartEntity params = new MultipartEntity();
		params.addPart("implementation_id",new StringBody(""+id));
		params.addPart("session_hash",new StringBody(session_hash));
		
		Object apiResult = doApiRequest("openml.implementation.delete", "", params);
		if( apiResult instanceof ImplementationDelete){
        	return (ImplementationDelete) apiResult;
        } else {
        	throw new DataFormatException("Casting Api Object to ImplementationDelete");
        }
	}
	
	public static ImplementationExists openmlImplementationExists( String name, String external_version ) throws Exception {
		Object apiResult = doApiRequest("openml.implementation.exists", "&name=" + name + "&external_version=" + external_version );
        if( apiResult instanceof ImplementationExists){
        	return (ImplementationExists) apiResult;
        } else {
        	throw new DataFormatException("Casting Api Object to ImplementationExists");
        }
	}
	
	public static Task openmlTasksSearch( int task_id ) throws Exception {
		Object apiResult = doApiRequest("openml.tasks.search", "&task_id=" + task_id );
        if( apiResult instanceof Task){
        	return (Task) apiResult;
        } else {
        	throw new DataFormatException("Casting Api Object to Task");
        }
	}
	
	public static UploadDataSet openmlDataUpload( File description, File dataset, String session_hash ) throws Exception {
		MultipartEntity params = new MultipartEntity();
		params.addPart("description", new FileBody(description));
		params.addPart("dataset", new FileBody(dataset));
		params.addPart("session_hash",new StringBody(session_hash));
        
        Object apiResult = doApiRequest("openml.data.upload", "", params);
        if( apiResult instanceof UploadDataSet){
        	return (UploadDataSet) apiResult;
        } else {
        	throw new DataFormatException("Casting Api Object to UploadDataSet");
        }
	}
	
	public static UploadImplementation openmlImplementationUpload( File description, File binary, File source, String session_hash ) throws Exception {
		MultipartEntity params = new MultipartEntity();
		params.addPart("description", new FileBody(description));
		if(source != null)
			params.addPart("source", new FileBody(source));
		if(binary != null)
			params.addPart("binary", new FileBody(binary));
		params.addPart("session_hash",new StringBody(session_hash));
		
        Object apiResult = doApiRequest("openml.implementation.upload", "", params);
        if( apiResult instanceof UploadImplementation){
        	return (UploadImplementation) apiResult;
        } else {
        	throw new DataFormatException("Casting Api Object to UploadImplementation");
        }
	}
	
	public static UploadRun openmlRunUpload( File description, Map<String,File> output_files, String session_hash ) throws Exception {
		MultipartEntity params = new MultipartEntity();
		if(Settings.API_VERBOSE) {
			System.out.println( Conversion.fileToString(description) );
			System.out.println("\n==========\n"+Conversion.fileToString(output_files.get("predictions"))+"\n==========");
		}
		params.addPart("description", new FileBody(description));
		for( String s : output_files.keySet() ) {
			params.addPart(s,new FileBody(output_files.get(s)));
		}
		params.addPart("session_hash",new StringBody(session_hash));
		Object apiResult = doApiRequest("openml.run.upload", "", params);
        if( apiResult instanceof UploadRun){
        	return (UploadRun) apiResult;
        } else {
        	throw new DataFormatException("Casting Api Object to UploadRun");
        }
	}
	
	public static Instances getDatasetFromUrl( String url ) throws IOException {
		URL openml_url = new URL(url);
		URLConnection conn = openml_url.openConnection();
		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		Instances dataset = new Instances(br);
		return dataset;
	}
	
	public static String getStringFromUrl( String url ) throws IOException {
		return IOUtils.toString(  new URL( url ) );
	}
	
	private static Object doApiRequest(String function, String queryString) throws Exception {
		return doApiRequest(function, queryString, null);
	}
	
	private static Object doApiRequest(String function, String queryString, HttpEntity entity) throws Exception {
		String result = "";
		httpclient = new DefaultHttpClient();
		String requestUri = API_URL + "?f=" + function + queryString;
		long contentLength = 0;
		try {
            HttpPost httppost = new HttpPost( requestUri );
            
            if(entity != null) {
            	httppost.setEntity(entity);
            }
            
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity resEntity = response.getEntity();
            if (resEntity != null) {
            	result = httpEntitiToString(resEntity);
                contentLength = resEntity.getContentLength();
            } else {
            	throw new Exception("An exception has occured while reading data input stream. ");
            }
		} finally {
            try { httpclient.getConnectionManager().shutdown(); } catch (Exception ignore) {}
        }
		if(Settings.API_VERBOSE)
			System.out.println("===== REQUEST URI: " + requestUri + " (Content Length: "+contentLength+") =====\n" + result + "\n=====\n");
		
		Object apiResult = xstream.fromXML(result);
		if(apiResult instanceof ApiError) {
			ApiError apiError = (ApiError) apiResult;
			throw new ApiException( Integer.parseInt( apiError.getCode() ), apiError.getMessage() );
		}
		return apiResult;
	}
	
	private static String httpEntitiToString(HttpEntity resEntity) throws IOException {
		StringWriter writer = new StringWriter();
		IOUtils.copy(new InputStreamReader( resEntity.getContent() ), writer );
		return writer.toString();
	}
}
